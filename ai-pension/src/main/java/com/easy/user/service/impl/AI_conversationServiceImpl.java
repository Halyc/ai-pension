package com.easy.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.easy.common.bean.AI_conversation;
import com.easy.common.mapper.membermapper.IAI_conversationMapper;
import com.easy.common.util.Sys;
import com.easy.user.dto.ChatHistoryVO;
import com.easy.user.dto.ChatSessionVO;
import com.easy.user.service.IAI_conversationService;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AI_conversationServiceImpl extends ServiceImpl<IAI_conversationMapper, AI_conversation> implements IAI_conversationService {

    @Override
    public List<Message> getMessages(String sessionid) {  //从数据库中查询某个会话的历史消息
        LambdaQueryWrapper<AI_conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AI_conversation::getSession_id, sessionid);
        wrapper.orderByDesc(AI_conversation::getCreate_time);
        wrapper.last(" limit 10");
        List<AI_conversation> list = this.list(wrapper);
        List<Message> messages = new ArrayList<>();
        Message msg = null;
        for (AI_conversation conversation : list) {
            if (conversation.getRole().equals("user")) {
                msg = new UserMessage(conversation.getMessage());
            } else {
                msg = new AssistantMessage(conversation.getMessage());
            }
            messages.add(msg);
        }
        Collections.reverse(messages);
        return messages;
    }

    @Override
    public List<ChatHistoryVO> getHistory(String sessionId) {  //查询某个会话的完整聊天历史
        LambdaQueryWrapper<AI_conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AI_conversation::getSession_id, sessionId);
        wrapper.orderByAsc(AI_conversation::getCreate_time);
        List<AI_conversation> list = this.list(wrapper);
        List<ChatHistoryVO> history = new ArrayList<>();
        for (AI_conversation conversation : list) {
            ChatHistoryVO vo = new ChatHistoryVO();
            vo.setId(conversation.getId());
            vo.setRole(conversation.getRole());
            vo.setMessage(conversation.getMessage());
            vo.setCreateTime(conversation.getCreate_time());
            history.add(vo);
        }
        return history;
    }

    public void clear(String sessionId) {   //清空某个会话的所有历史消息
        LambdaQueryWrapper<AI_conversation> lqw = new LambdaQueryWrapper<>();
        lqw.eq(AI_conversation::getSession_id, sessionId);
        this.remove(lqw);

    }

    @Override
    public List<ChatSessionVO> listSessions(Long userId) {
        LambdaQueryWrapper<AI_conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AI_conversation::getUser_id, userId);
        wrapper.orderByAsc(AI_conversation::getCreate_time);
        List<AI_conversation> list = this.list(wrapper);

        //按 session_id 聚合成会话，取首条时间作为创建时间、最后一条时间作为更新时间
        Map<String, ChatSessionVO> sessionMap = new LinkedHashMap<>();
        for (AI_conversation conversation : list) {
            String sessionId = conversation.getSession_id();
            if (sessionId == null) continue;
            ChatSessionVO vo = sessionMap.get(sessionId);
            if (vo == null) {
                vo = new ChatSessionVO();
                vo.setSessionId(toRawSessionId(sessionId, userId));
                vo.setCreateTime(conversation.getCreate_time());
                vo.setMessageCount(0);
                sessionMap.put(sessionId, vo);
            }
            if (vo.getTitle() == null && conversation.getTitle() != null && !conversation.getTitle().isBlank()) {
                vo.setTitle(conversation.getTitle());
            }
            vo.setPreview(buildPreview(conversation));
            vo.setUpdateTime(conversation.getCreate_time());
            vo.setMessageCount(vo.getMessageCount() + 1);
        }

        List<ChatSessionVO> result = new ArrayList<>(sessionMap.values());
        result.sort((a, b) -> {
            LocalDateTime ta = a.getUpdateTime();
            LocalDateTime tb = b.getUpdateTime();
            if (ta == null) return 1;
            if (tb == null) return -1;
            return tb.compareTo(ta);
        });
        for (ChatSessionVO vo : result) {
            if (vo.getTitle() == null || vo.getTitle().isBlank()) vo.setTitle("新会话");
        }
        return result;
    }

    @Override
    public void deleteSession(Long userId, String sessionId) {
        LambdaQueryWrapper<AI_conversation> lqw = new LambdaQueryWrapper<>();
        lqw.eq(AI_conversation::getUser_id, userId);
        lqw.and(w -> w.eq(AI_conversation::getSession_id, buildFullSessionId(sessionId, userId))
                .or().eq(AI_conversation::getSession_id, sessionId));
        this.remove(lqw);
    }

    @Override
    public void deleteSessions(Long userId, List<String> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) return;
        List<String> ids = new ArrayList<>();
        for (String sessionId : sessionIds) {
            if (sessionId == null || sessionId.isBlank()) continue;
            ids.add(buildFullSessionId(sessionId, userId));
            ids.add(sessionId);
        }
        if (ids.isEmpty()) return;
        LambdaQueryWrapper<AI_conversation> lqw = new LambdaQueryWrapper<>();
        lqw.eq(AI_conversation::getUser_id, userId)
                .in(AI_conversation::getSession_id, ids);
        this.remove(lqw);
    }

    @Override
    public void deleteAllSessions(Long userId) {
        LambdaQueryWrapper<AI_conversation> lqw = new LambdaQueryWrapper<>();
        lqw.eq(AI_conversation::getUser_id, userId);
        this.remove(lqw);
    }

    @Override
    public void updateTitle(Long userId, String sessionId, String title) {
        LambdaUpdateWrapper<AI_conversation> luw = new LambdaUpdateWrapper<>();
        luw.eq(AI_conversation::getUser_id, userId);
        luw.and(w -> w.eq(AI_conversation::getSession_id, buildFullSessionId(sessionId, userId))
                .or().eq(AI_conversation::getSession_id, sessionId));
        luw.set(AI_conversation::getTitle, title);
        this.update(luw);
    }

    private String buildFullSessionId(String sessionId, Long userId) {
        return Sys.MEMBER_CHAT_SESSION_ID_PREFIX + userId + "_" + sessionId;
    }

    //数据库里存的是带用户前缀的完整 ID，返回给前端时还原成原始 ID
    private String toRawSessionId(String fullSessionId, Long userId) {
        String prefix = buildFullSessionId("", userId);
        return fullSessionId.startsWith(prefix) ? fullSessionId.substring(prefix.length()) : fullSessionId;
    }

    //用户消息前面拼接了上下文，预览时只取真正的提问部分
    private String buildPreview(AI_conversation conversation) {
        String message = conversation.getMessage();
        if (message == null) return "";
        if ("user".equals(conversation.getRole())) {
            int idx = message.lastIndexOf(Sys.CHAT_USER_QUESTION_MARK);
            if (idx >= 0) message = message.substring(idx + Sys.CHAT_USER_QUESTION_MARK.length());
        }
        message = message.replaceAll("\\s+", " ").trim();
        return message.length() > 30 ? message.substring(0, 30) + "..." : message;
    }

}