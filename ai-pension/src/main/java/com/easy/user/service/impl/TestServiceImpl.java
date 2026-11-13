package com.easy.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.easy.common.bean.AssessmentResult;
import com.easy.common.bean.Question;
import com.easy.common.bean.Questionnaire;
import com.easy.common.bean.User;
import com.easy.common.mapper.membermapper.IAssessmentResultMapper;
import com.easy.common.mapper.membermapper.IQuestionMapper;
import com.easy.common.mapper.membermapper.IQuestionnaireMapper;
import com.easy.common.mapper.membermapper.IUserMapper;
import com.easy.common.util.CurrentUser;
import com.easy.common.util.Result;
import com.easy.user.service.ITestService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

@Slf4j
@Service
public class TestServiceImpl implements ITestService {
    @Autowired
    IQuestionnaireMapper questionnaireMapper;
    @Autowired
    IQuestionMapper questionMapper;
    @Autowired
    IAssessmentResultMapper assessmentResultMapper;
    @Autowired
    IUserMapper userMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ChatClient chatClient;

    public TestServiceImpl(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("你是一位专业的老年健康评估专家，擅长根据老年人的生活方式、身体状况等问卷答案，结合给定的健康评分，输出50-150字的中文健康建议。语气亲切、专业，给出具体可操作的建议。评分等级参考：80-100生活方式优秀，60-79生活方式尚可，0-59生活方式有待改善。重要：开头必须用【用户信息】中给出的具体称呼（如'张叔叔您好'、'李阿姨您好'），绝对不能使用'叔叔阿姨'、'爷爷奶奶'等泛指性称呼。")
                .build();
    }

    @Override
    public Result questionnaires() {
        LambdaQueryWrapper<Questionnaire> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Questionnaire::getStatus, "PUBLISHED")
                .orderByDesc(Questionnaire::getCreate_time);
        List<Questionnaire> list = questionnaireMapper.selectList(lqw);

        List<Map<String, Object>> data = new ArrayList<>();
        for (Questionnaire q : list) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", q.getId());
            item.put("title", q.getTitle());
            item.put("description", q.getDescription());
            item.put("createTime", q.getCreate_time());
            data.add(item);
        }
        return Result.success("success", data);
    }

    @Override
    public Result questions(Long questionnaireId) {
        if (questionnaireId == null) {
            return Result.error(400, "问卷ID不能为空");
        }

        LambdaQueryWrapper<Question> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Question::getQuestionnaire_id, questionnaireId)
                .orderByAsc(Question::getSort_order);
        List<Question> list = questionMapper.selectList(lqw);

        List<Map<String, Object>> data = new ArrayList<>();
        for (Question q : list) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", q.getId());
            item.put("questionnaireId", q.getQuestionnaire_id());
            item.put("content", q.getContent());
            item.put("type", q.getType());
            item.put("options", q.getOptions());
            item.put("sortOrder", q.getSort_order());
            data.add(item);
        }
        return Result.success("success", data);
    }

    @Override
    @Transactional
    public Result submit(Map<String, Object> body) {
        Long questionnaireId;
        if (body.get("questionnaireId") != null) {
            questionnaireId = ((Number) body.get("questionnaireId")).longValue();
        } else {
            questionnaireId = null;
        }
        Object answers = body.get("answers");

        if (questionnaireId == null) {
            return Result.error(400, "问卷不存在");
        }
        if (answers == null) {
            return Result.error(400, "答案不能为空");
        }

        Long userID = CurrentUser.getCurrentUserID();

        AssessmentResult result = new AssessmentResult();
        result.setUser_id(userID);
        result.setQuestionnaire_id(questionnaireId);
        result.setCreate_time(LocalDateTime.now());
        result.setUpdate_time(LocalDateTime.now());

        try {
            result.setAnswers(objectMapper.writeValueAsString(answers));
        } catch (JsonProcessingException e) {
            result.setAnswers(String.valueOf(answers));
        }

        assessmentResultMapper.insert(result);
        analyzeAndSave(result);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", result.getId());
        return Result.success("提交成功", data);
    }

    @Override
    public Result history() {
        Long userID = CurrentUser.getCurrentUserID();

        LambdaQueryWrapper<AssessmentResult> lqw = new LambdaQueryWrapper<>();
        lqw.eq(AssessmentResult::getUser_id, userID)
                .orderByDesc(AssessmentResult::getCreate_time);
        List<AssessmentResult> list = assessmentResultMapper.selectList(lqw);

        List<Map<String, Object>> data = new ArrayList<>();
        for (AssessmentResult r : list) {
            if (r.getAi_score() == null) {
                analyzeAndSave(r);
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", r.getId());
            item.put("questionnaireId", r.getQuestionnaire_id());

            Questionnaire q = questionnaireMapper.selectById(r.getQuestionnaire_id());
            item.put("questionnaireTitle", q != null ? q.getTitle() : "");
            item.put("aiScore", r.getAi_score());
            item.put("createTime", r.getCreate_time());
            data.add(item);
        }
        return Result.success("success", data);
    }

    @Override
    public Result detail(Long resultId) {
        if (resultId == null) {
            return Result.error(400, "测评结果ID不能为空");
        }
        AssessmentResult result = assessmentResultMapper.selectById(resultId);
        if (result == null) {
            return Result.error(404, "测评结果不存在");
        }
        if (result.getAi_score() == null) {
            analyzeAndSave(result);
        }
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("id", result.getId());
        data.put("questionnaireId", result.getQuestionnaire_id());

        Questionnaire q = questionnaireMapper.selectById(result.getQuestionnaire_id());
        data.put("questionnaireTitle", q != null ? q.getTitle() : "");

        LambdaQueryWrapper<Question> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Question::getQuestionnaire_id, result.getQuestionnaire_id())
                .orderByAsc(Question::getSort_order);
        List<Question> questions = questionMapper.selectList(lqw);

        List<Map<String, Object>> questionList = new LinkedList<>();
        for (Question qq : questions) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", qq.getId());
            item.put("content", qq.getContent());
            item.put("type", qq.getType());
            item.put("options", qq.getOptions());
            questionList.add(item);
        }
        data.put("questions", questionList);

        try {
            Object answers = objectMapper.readValue(result.getAnswers(), Object.class);
            data.put("answers", answers);
        } catch (JsonProcessingException e) {
            data.put("answers", result.getAnswers());
        }
        data.put("aiScore", result.getAi_score());
        data.put("aiSuggestion", result.getAi_suggestion());
        data.put("createTime", result.getCreate_time());

        return Result.success("success", data);
    }

    private void analyzeAndSave(AssessmentResult result) {  //先打分，再调用 AI 生成建议，最后写回数据库
        ScoreSuggestion rule = ruleBasedScore(result);
        result.setAi_score(rule.score);
        try {
            String prompt = buildPrompt(result, rule.score);
            String suggestion = chatClient.prompt().user(prompt).call().content();
            if (suggestion != null && !suggestion.isBlank()) {
                String cleaned = suggestion.trim();
                if (cleaned.startsWith("```")) {
                    int start = cleaned.indexOf('\n');
                    int end = cleaned.lastIndexOf("```");
                    if (start > 0 && end > start) {
                        cleaned = cleaned.substring(start + 1, end).trim();
                    }
                }
                result.setAi_suggestion(cleaned);
            } else {
                result.setAi_suggestion(rule.suggestion);
            }
        } catch (Exception ex) {
            log.warn("AI 健康建议生成失败，降级为规则建议: {}", ex.getMessage());
            result.setAi_suggestion(rule.suggestion);
        }
        assessmentResultMapper.updateById(result);
    }

    private String buildPrompt(AssessmentResult result, int score) {  //拼接AI调用所需的完整文本
        Questionnaire q = questionnaireMapper.selectById(result.getQuestionnaire_id());
        LambdaQueryWrapper<Question> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Question::getQuestionnaire_id, result.getQuestionnaire_id())
                .orderByAsc(Question::getSort_order);
        List<Question> questions = questionMapper.selectList(lqw);
        Map<Object, Object> answersMap = new LinkedHashMap<>();
        try {
            answersMap = objectMapper.readValue(result.getAnswers(), new com.fasterxml.jackson.core.type.TypeReference<>() {
            });
        } catch (Exception ignored) {
        }

        StringBuilder sb = new StringBuilder();

        User user = userMapper.selectById(result.getUser_id());
        sb.append("【用户信息】\n");
        if (user != null) {
            String realName = user.getReal_name();
            String gender = user.getGender();
            String surname = (realName != null && !realName.isBlank()) ? realName.charAt(0) + "" : "";
            String salutation;
            if (surname.isBlank()) {
                salutation = "您好";
            } else if ("男".equals(gender)) {
                salutation = surname + "叔叔";
            } else if ("女".equals(gender)) {
                salutation = surname + "阿姨";
            } else {
                salutation = surname + "先生/女士";
            }
            sb.append("姓名：").append(realName != null ? realName : "未知").append("\n");
            sb.append("性别：").append(gender != null ? gender : "未知").append("\n");
            sb.append("要求使用的称呼：").append(salutation).append("\n");
        } else {
            sb.append("姓名：未知\n性别：未知\n要求使用的称呼：您\n");
        }
        sb.append("（请在健康建议开头直接使用上述称呼，如\"张叔叔您好\"或\"李阿姨您好\"，严禁使用\"叔叔阿姨\"\"爷爷奶奶\"等泛指称呼）\n\n");

        sb.append("请根据以下老年健康评测问卷和用户答案，结合已计算好的健康评分，撰写一段50-150字的中文健康建议。\n\n");
        sb.append("问卷名称：").append(q != null ? q.getTitle() : "").append("\n");
        sb.append("健康评分：").append(score).append("分");
        if (score >= 80) sb.append("（生活方式优秀）");
        else if (score >= 60) sb.append("（生活方式尚可）");
        else sb.append("（生活方式有待改善）");
        sb.append("\n\n");
        sb.append("题目与选项：\n");
        for (int i = 0; i < questions.size(); i++) {
            Question qu = questions.get(i);
            sb.append("Q").append(i + 1).append(". ").append(qu.getContent()).append("\n");
            if (qu.getOptions() != null && !qu.getOptions().isBlank()) {
                for (String o : qu.getOptions().split("\n")) {
                    String opt = o.trim();
                    if (!opt.isEmpty()) sb.append("  - ").append(opt).append("\n");
                }
            }
        }
        sb.append("\n用户答案：\n");
        for (int i = 0; i < questions.size(); i++) {
            Question qu = questions.get(i);
            Object ans = answersMap.get(String.valueOf(qu.getId()));
            if (ans == null) ans = answersMap.get(String.valueOf(i + 1));
            String ansStr;
            if (ans == null) ansStr = "（未作答）";
            else if (ans instanceof List) ansStr = String.join("、", (List<CharSequence>) ans);
            else ansStr = ans.toString();
            sb.append("Q").append(i + 1).append(". ").append(ansStr).append("\n");
        }
        sb.append("\n请直接输出健康建议文本，不要输出JSON或其他格式。");
        return sb.toString();
    }

    private ScoreSuggestion ruleBasedScore(AssessmentResult result) {   //规则打分入口
        LambdaQueryWrapper<Question> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Question::getQuestionnaire_id, result.getQuestionnaire_id())
                .orderByAsc(Question::getSort_order);
        List<Question> questions = questionMapper.selectList(lqw);
        return calculateScore(questions, result.getAnswers());
    }

    public ScoreSuggestion calculateScore(List<Question> questions, String answersJson) {   //评分标准
        Map<Object, Object> answersMap = new LinkedHashMap<>();
        try {
            answersMap = objectMapper.readValue(answersJson, new com.fasterxml.jackson.core.type.TypeReference<>() {
            });
        } catch (Exception ignored) {
        }

        List<Question> scoreable = new ArrayList<>();
        for (Question q : questions) {
            if (q.getType() == null || !q.getType().equalsIgnoreCase("TEXT")) {
                scoreable.add(q);
            }
        }
        int totalQuestions = scoreable.size();
        if (totalQuestions == 0) {
            return new ScoreSuggestion(50, "暂无有效题目，无法评分。");
        }

        double perQuestionFull = 100.0 / totalQuestions;
        double total = 0;

        for (Question qu : scoreable) {
            List<String> opts = parseOptions(qu.getOptions());
            int N = opts.size();
            if (N < 2) continue;

            Object ans = answersMap.get(String.valueOf(qu.getId()));
            if (ans == null) ans = answersMap.get(String.valueOf(questions.indexOf(qu) + 1));
            if (ans == null) continue;

            int k = findRank(opts, ans, qu.getType());
            if (k < 0) continue;

            double ratio = (double) (N - k) / (N - 1);
            total += perQuestionFull * ratio;
        }

        int score = (int) Math.round(Math.max(0, Math.min(100, total)));
        String sug;
        if (score >= 80) sug = "您的生活方式优秀，请继续保持规律作息、均衡饮食和适度运动。";
        else if (score >= 60)
            sug = "您的生活方式尚可，整体不错，但仍有少量习惯可以调整优化，建议继续保持并适度增加运动量。";
        else sug = "您的生活方式有待改善，建议进行健康干预，逐步调整作息、饮食和运动习惯，必要时就医检查。";
        return new ScoreSuggestion(score, sug);
    }

    private List<String> parseOptions(String raw) {  //解析题目的 options 字段
        List<String> result = new ArrayList<>();
        if (raw == null || raw.isBlank()) return result;
        try {
            Object v = objectMapper.readValue(raw, Object.class);
            if (v instanceof List) {
                for (Object o : (List<?>) v) result.add(String.valueOf(o).trim());
                return result;
            }
        } catch (Exception ignored) {
        }
        for (String s : raw.split("\n")) {
            String t = s.trim();
            if (!t.isEmpty()) result.add(t);
        }
        return result;
    }

    private int findRank(List<String> opts, Object ans, String type) {   //查找用户答案在选项列表中的"排名"
        if (type != null && type.equalsIgnoreCase("MULTIPLE") && ans instanceof List) {
            int maxIdx = -1;
            for (Object o : (List<?>) ans) {
                int idx = opts.indexOf(String.valueOf(o));
                if (idx > maxIdx) maxIdx = idx;
            }
            return maxIdx >= 0 ? maxIdx + 1 : -1;
        }
        int idx = opts.indexOf(String.valueOf(ans));
        return idx >= 0 ? idx + 1 : -1;
    }

    public static class ScoreSuggestion {
        public int score;
        public String suggestion;

        public ScoreSuggestion(int score, String suggestion) {
            this.score = score;
            this.suggestion = suggestion;
        }
    }
}