package com.easy.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.easy.common.annotation.OperLog;
import com.easy.common.aitools.MemberFunctionCalling;
import com.easy.common.bean.AI_conversation;
import com.easy.common.bean.SysConfig;
import com.easy.common.mapper.adminmapper.ISysConfigMapper;
import com.easy.common.memory.DBChatMemory;
import com.easy.common.util.CurrentUser;
import com.easy.common.util.Result;
import com.easy.common.util.Sys;
import com.easy.user.dto.ChatHistoryVO;
import com.easy.user.dto.ChatSendRequest;
import com.easy.user.service.IAI_conversationService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.openai.OpenAiChatOptions;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/member/chat")
public class ChatController {

    private ChatClient chatClient;

    private static ChatMemory chatMemory;

    private static final Map<String, Object> DISABLE_THINKING = new HashMap<>();
    static {
        Map<String, Object> thinking = new HashMap<>();
        thinking.put("type", "disabled");
        DISABLE_THINKING.put("thinking", thinking);
    }

    private static final OpenAiChatOptions CHAT_OPTIONS = OpenAiChatOptions.builder()
            .extraBody(DISABLE_THINKING)
            .build();

    @Autowired
    private MemberFunctionCalling memberFunctionCalling;

    @Autowired
    private IAI_conversationService conversationService;

    @Autowired
    private ISysConfigMapper sysConfigMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    private void saveConversation(String sessionId, Long userId, String role, String message) {
        try {
            AI_conversation con = new AI_conversation();
            con.setUser_id(userId);
            con.setSession_id(sessionId);
            con.setRole(role);
            con.setMessage(message);
            con.setCreate_time(LocalDateTime.now());
            con.setDeleted(0);
            //会话的第一条用户消息决定会话标题
            if ("user".equals(role) && conversationService.count(new LambdaQueryWrapper<AI_conversation>()
                    .eq(AI_conversation::getSession_id, sessionId)
                    .eq(AI_conversation::getUser_id, userId)) == 0) {
                con.setTitle(buildTitle(message));
            }
            conversationService.save(con);
        } catch (Exception ignored) {
        }
    }

    //用户消息前面拼接了上下文，标题只取真正的提问部分
    private String buildTitle(String message) {
        if (message == null || message.isBlank()) return "新会话";
        String text = message;
        int idx = text.lastIndexOf(Sys.CHAT_USER_QUESTION_MARK);
        if (idx >= 0) text = text.substring(idx + Sys.CHAT_USER_QUESTION_MARK.length());
        text = text.replaceAll("\\s+", " ").trim();
        if (text.isEmpty()) return "新会话";
        return text.length() > 20 ? text.substring(0, 20) + "..." : text;
    }

    private String resolveSystemPrompt() {
        try {
            LambdaQueryWrapper<SysConfig> lqw = new LambdaQueryWrapper<>();
            lqw.eq(SysConfig::getConfig_key, Sys.AI_HEALTH_ASSISTANT_PROMPT_KEY);
            SysConfig config = sysConfigMapper.selectOne(lqw);
            if (config != null && config.getConfig_value() != null && !config.getConfig_value().isBlank()) {
                return config.getConfig_value();
            }
        } catch (Exception ignored) {
        }
        return Sys.AI_CHAT_SYSTEM_PROMPT;
    }

    public ChatController(ChatClient.Builder builder, DBChatMemory chatMemory) {
        this.chatClient = builder
                .defaultSystem(Sys.AI_CHAT_SYSTEM_PROMPT)  //默认系统提示词
                .defaultOptions(CHAT_OPTIONS)   //关闭模型思考过程
                .build();
        this.chatMemory = chatMemory;
    }

    @OperLog(module = "AI助手", operation = "创建对话会话", operateType = 4)
    @PostMapping("/session")
    public Result createSession() {     //创建一个新会话，返回唯一 sessionId
        String sessionId = "sess_" + UUID.randomUUID().toString().replace("-", "");
        return Result.success("success", sessionId);
    }
    //把前端传来的 rawSessionId拼成"真正存数据库用的完整会话 ID
    private String buildConversationId(String rawSessionId) {
        Long userId = CurrentUser.getCurrentUserID();
        return Sys.MEMBER_CHAT_SESSION_ID_PREFIX + userId + "_" + rawSessionId;
    }
    //对 AI 返回的原始文本做美化
    private String fixFormat(String content) {

        if (content == null || content.isEmpty()) return content;
        String s = content;
        // 1. 去掉没有空格的 # 标题标记
        s = s.replaceAll("#{1,6}(?=[^\\s#])", "");
        // 2. 修复数字列表后没空格的情况（如 "1.2024年" -> "1. 2024年"）
        s = s.replaceAll("([1-9])\\.(?=\\d{4}年)", "$1. ");
        s = s.replaceAll("([^\\n])([1-9])\\.\\s+(?=\\d{4}年)", "$1\n$2. ");
        s = s.replaceAll("([：:])(\\s*)([1-9])\\.\\s+(?=\\d{4}年)", "$1\n$3. ");
        // 3. 修复不配对的 ** 加粗标记（奇数个就去掉所有）
        s = stripUnpairedBold(s);
        // 4. 修复无序列表："-xxx" / "*xxx" -> "- xxx"
        s = s.replaceAll("(?m)^([-*+·•])([^\\s-*+·•])", "$1 $2");
        // 5. 合并过多空行
        s = s.replaceAll("\\n{3,}", "\n\n");

        return s.trim();
    }

    private String stripUnpairedBold(String s) {
        // 统计所有 **，奇数则去掉
        int count = 0;
        for (int i = 0; i <= s.length() - 2; i++) {
            if (s.charAt(i) == '*' && s.charAt(i + 1) == '*') {
                count++;
                i++;
            }
        }
        if (count % 2 != 0) {
            s = s.replace("**", "");
        }
        // 同样处理单个 *
        int single = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '*') single++;
        }
        if (single % 2 != 0) {
            s = s.replace("*", "");
        }
        return s;
    }

    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamPost(@RequestBody ChatSendRequest request) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5分钟超时

        String fullSessionId = buildConversationId(request.getSessionId());
        // 异步线程中拿不到 CurrentUser 的 ThreadLocal，这里先取好 userId
        Long userId = CurrentUser.getCurrentUserID();

        // 先保存用户消息到数据库
        saveConversation(fullSessionId, userId, "user", request.getMessage());

        StringBuilder fullReply = new StringBuilder();

        sseExecutor.execute(() -> {
            try {
                Flux<String> flux = this.chatClient.prompt()
                        .system(resolveSystemPrompt())
                        .advisors(a -> a.param("chat_memory_conversation_id", fullSessionId))
                        .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                        .tools(memberFunctionCalling)
                        .user(request.getMessage())
                        .stream()
                        .content();

                flux.subscribe(
                        chunk -> {
                            try {
                                if (chunk != null && !chunk.isBlank()) {
                                    fullReply.append(chunk);
                                    emitter.send(SseEmitter.event().data(
                                            toJson(Map.of("type", "text", "content", chunk))));
                                }
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        },
                        error -> {
                            try {
                                emitter.send(SseEmitter.event().data(
                                        toJson(Map.of("type", "error", "content", "AI服务异常"))));
                            } catch (IOException ignored) {
                            }
                            emitter.completeWithError(error);
                        },
                        () -> {
                            // 完成：对完整回复做 fixFormat，保存到数据库，发送 done 事件
                            String finalReply = fixFormat(fullReply.toString());
                            if (finalReply == null || finalReply.isBlank()) {
                                finalReply = "抱歉，我暂时没有理解您的问题，请换个方式描述一下。";
                            }
                            saveConversation(fullSessionId, userId, "assistant", finalReply);
                            try {
                                emitter.send(SseEmitter.event().data(toJson(Map.of("type", "done"))));
                            } catch (IOException ignored) {
                            }
                            emitter.complete();
                        }
                );
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().data(
                            toJson(Map.of("type", "error", "content", "服务内部异常"))));
                    emitter.send(SseEmitter.event().data(toJson(Map.of("type", "done"))));
                } catch (IOException ignored) {
                }
                emitter.completeWithError(e);
            }
        });

        emitter.onCompletion(() -> {});
        emitter.onTimeout(() -> emitter.complete());
        emitter.onError(e -> {});

        return emitter;
    }

    @GetMapping("/history/{sessionId}")
    public Result getHistory(@PathVariable String sessionId) {  //查询并返回该会话的历史聊天记录列表
        String fullSessionId = buildConversationId(sessionId);
        List<ChatHistoryVO> history = conversationService.getHistory(fullSessionId);
        return Result.success("success", history);
    }

    @GetMapping("/sessions")
    public Result listSessions() {  //查询当前用户的会话列表
        Long userId = CurrentUser.getCurrentUserID();
        return Result.success("success", conversationService.listSessions(userId));
    }

    @DeleteMapping("/session/{sessionId}")
    public Result deleteSession(@PathVariable String sessionId) {  //删除某个会话及其全部消息
        Long userId = CurrentUser.getCurrentUserID();
        conversationService.deleteSession(userId, sessionId);
        return Result.success("success", null);
    }

    @DeleteMapping("/sessions")
    public Result clearSessions() {  //清空当前用户的全部会话
        Long userId = CurrentUser.getCurrentUserID();
        conversationService.deleteAllSessions(userId);
        return Result.success("success", null);
    }

    @DeleteMapping("/sessions/batch")
    public Result deleteSessions(@RequestBody List<String> sessionIds) {  //批量删除会话
        Long userId = CurrentUser.getCurrentUserID();
        conversationService.deleteSessions(userId, sessionIds);
        return Result.success("success", null);
    }

    @PutMapping("/session/{sessionId}/title")
    public Result renameSession(@PathVariable String sessionId, @RequestBody Map<String, String> body) {  //修改会话标题
        Long userId = CurrentUser.getCurrentUserID();
        String title = body == null ? null : body.get("title");
        if (title == null || title.isBlank()) {
            return Result.error(400, "标题不能为空");
        }
        conversationService.updateTitle(userId, sessionId, title.trim());
        return Result.success("success", null);
    }

}
