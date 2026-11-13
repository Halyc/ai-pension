package com.easy.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.easy.admin.service.IAssessmentService;
import com.easy.common.bean.AssessmentResult;
import com.easy.common.bean.Question;
import com.easy.common.bean.Questionnaire;
import com.easy.common.mapper.membermapper.IAssessmentResultMapper;
import com.easy.common.mapper.membermapper.IQuestionMapper;
import com.easy.common.mapper.membermapper.IQuestionnaireMapper;
import com.easy.common.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AssessmentServiceImpl implements IAssessmentService {

    @Autowired
    private IQuestionnaireMapper questionnaireMapper;

    @Autowired
    private IQuestionMapper questionMapper;

    @Autowired
    private IAssessmentResultMapper assessmentResultMapper;

    // ---------- 13.1 问卷管理 ----------
    @Override
    public Result listQuestionnaires() {
        LambdaQueryWrapper<Questionnaire> lqw = new LambdaQueryWrapper<>();
        lqw.orderByDesc(Questionnaire::getCreate_time);
        List<Questionnaire> list = questionnaireMapper.selectList(lqw);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Questionnaire q : list) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", q.getId());
            item.put("title", q.getTitle());
            item.put("description", q.getDescription());
            item.put("status", q.getStatus());
            item.put("createTime", q.getCreate_time());
            item.put("updateTime", q.getUpdate_time());
            result.add(item);
        }
        return Result.success("success", result);
    }

    @Override
    public Result createQuestionnaire(Object body) {
        if (!(body instanceof Map)) {
            return Result.error(400, "请求参数格式错误");
        }
        Map<?, ?> map = (Map<?, ?>) body;
        String title = (String) map.get("title");
        if (title == null || title.trim().isEmpty()) {
            return Result.error(400, "问卷标题不能为空");
        }
        Questionnaire q = new Questionnaire();
        q.setTitle(title.trim());
        q.setDescription((String) map.get("description"));
        q.setStatus("DRAFT");
        q.setCreate_time(LocalDateTime.now());
        q.setUpdate_time(LocalDateTime.now());
        questionnaireMapper.insert(q);
        return Result.success("创建成功", q);
    }

    @Override
    public Result updateQuestionnaire(Long id, Object body) {
        if (id == null) {
            return Result.error(400, "问卷ID不能为空");
        }
        Questionnaire q = questionnaireMapper.selectById(id);
        if (q == null) {
            return Result.error(404, "问卷不存在");
        }
        if (!(body instanceof Map)) {
            return Result.error(400, "请求参数格式错误");
        }
        Map<?, ?> map = (Map<?, ?>) body;
        if (map.containsKey("title")) {
            String title = (String) map.get("title");
            if (title == null || title.trim().isEmpty()) {
                return Result.error(400, "问卷标题不能为空");
            }
            q.setTitle(title.trim());
        }
        if (map.containsKey("description")) {
            q.setDescription((String) map.get("description"));
        }
        q.setUpdate_time(LocalDateTime.now());
        questionnaireMapper.updateById(q);
        return Result.success("更新成功", q);
    }

    @Override
    public Result deleteQuestionnaire(Long id) {
        if (id == null) {
            return Result.error(400, "问卷ID不能为空");
        }
        Questionnaire q = questionnaireMapper.selectById(id);
        if (q == null) {
            return Result.error(404, "问卷不存在");
        }
        questionnaireMapper.deleteById(id);
        return Result.success("删除成功", null);
    }

    @Override
    public Result publishQuestionnaire(Long id) {
        if (id == null) {
            return Result.error(400, "问卷ID不能为空");
        }
        Questionnaire q = questionnaireMapper.selectById(id);
        if (q == null) {
            return Result.error(404, "问卷不存在");
        }
        q.setStatus("PUBLISHED");
        q.setUpdate_time(LocalDateTime.now());
        questionnaireMapper.updateById(q);
        return Result.success("发布成功", q);
    }

    // ---------- 13.2 题目管理 ----------
    @Override
    public Result listQuestions(Long questionnaireId) {
        LambdaQueryWrapper<Question> lqw = new LambdaQueryWrapper<>();
        if (questionnaireId != null) {
            lqw.eq(Question::getQuestionnaire_id, questionnaireId);
        }
        lqw.orderByDesc(Question::getQuestionnaire_id)
                .orderByAsc(Question::getSort_order);
        List<Question> list = questionMapper.selectList(lqw);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Question q : list) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", q.getId());
            item.put("questionnaireId", q.getQuestionnaire_id());
            item.put("content", q.getContent());
            item.put("type", q.getType());
            item.put("options", q.getOptions());
            item.put("sortOrder", q.getSort_order());
            Questionnaire qn = questionnaireMapper.selectById(q.getQuestionnaire_id());
            item.put("questionnaireTitle", qn != null ? qn.getTitle() : "未知问卷");
            result.add(item);
        }
        return Result.success("success", result);
    }

    @Override
    public Result createQuestion(Object body) {
        if (!(body instanceof Map)) {
            return Result.error(400, "请求参数格式错误");
        }
        Map<?, ?> map = (Map<?, ?>) body;
        Object questionnaireIdObj = map.get("questionnaireId");
        if (questionnaireIdObj == null) {
            return Result.error(400, "问卷ID不能为空");
        }
        Long questionnaireId = Long.valueOf(questionnaireIdObj.toString());
        Questionnaire questionnaire = questionnaireMapper.selectById(questionnaireId);
        if (questionnaire == null) {
            return Result.error(404, "问卷不存在");
        }
        Question q = new Question();
        q.setQuestionnaire_id(questionnaireId);
        q.setContent((String) map.get("content"));
        q.setType((String) map.get("type"));
        q.setOptions((String) map.get("options"));
        Object sortOrder = map.get("sortOrder");
        q.setSort_order(sortOrder == null ? 0 : Integer.valueOf(sortOrder.toString()));
        q.setCreate_time(LocalDateTime.now());
        q.setUpdate_time(LocalDateTime.now());
        questionMapper.insert(q);
        return Result.success("创建成功", q);
    }

    @Override
    public Result updateQuestion(Long id, Object body) {
        if (id == null) {
            return Result.error(400, "题目ID不能为空");
        }
        Question q = questionMapper.selectById(id);
        if (q == null) {
            return Result.error(404, "题目不存在");
        }
        if (!(body instanceof Map)) {
            return Result.error(400, "请求参数格式错误");
        }
        Map<?, ?> map = (Map<?, ?>) body;
        if (map.containsKey("content")) q.setContent((String) map.get("content"));
        if (map.containsKey("type")) q.setType((String) map.get("type"));
        if (map.containsKey("options")) q.setOptions((String) map.get("options"));
        if (map.containsKey("sortOrder")) {
            q.setSort_order(Integer.valueOf(map.get("sortOrder").toString()));
        }
        q.setUpdate_time(LocalDateTime.now());
        questionMapper.updateById(q);
        return Result.success("更新成功", q);
    }

    @Override
    public Result deleteQuestion(Long id) {
        if (id == null) {
            return Result.error(400, "题目ID不能为空");
        }
        Question q = questionMapper.selectById(id);
        if (q == null) {
            return Result.error(404, "题目不存在");
        }
        questionMapper.deleteById(id);
        return Result.success("删除成功", null);
    }

    // ---------- 13.3 评测结果管理 ----------
    @Override
    public Result listResults() {
        LambdaQueryWrapper<AssessmentResult> lqw = new LambdaQueryWrapper<>();
        lqw.orderByDesc(AssessmentResult::getCreate_time);
        List<AssessmentResult> list = assessmentResultMapper.selectList(lqw);
        List<Map<String, Object>> result = new ArrayList<>();
        for (AssessmentResult r : list) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", r.getId());
            item.put("userId", r.getUser_id());
            item.put("questionnaireId", r.getQuestionnaire_id());
            item.put("answers", r.getAnswers());
            item.put("aiScore", r.getAi_score());
            item.put("aiSuggestion", r.getAi_suggestion());
            item.put("createTime", r.getCreate_time());
            result.add(item);
        }
        return Result.success("success", result);
    }

    @Override
    public Result getResultDetail(Long id) {
        if (id == null) {
            return Result.error(400, "结果ID不能为空");
        }
        AssessmentResult r = assessmentResultMapper.selectById(id);
        if (r == null) {
            return Result.error(404, "评测结果不存在");
        }
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", r.getId());
        item.put("userId", r.getUser_id());
        item.put("questionnaireId", r.getQuestionnaire_id());
        item.put("answers", r.getAnswers());
        item.put("aiScore", r.getAi_score());
        item.put("aiSuggestion", r.getAi_suggestion());
        item.put("createTime", r.getCreate_time());
        item.put("updateTime", r.getUpdate_time());
        return Result.success("success", item);
    }

    @Override
    public Result deleteResult(Long id) {
        if (id == null) {
            return Result.error(400, "结果ID不能为空");
        }
        AssessmentResult r = assessmentResultMapper.selectById(id);
        if (r == null) {
            return Result.error(404, "评测结果不存在");
        }
        assessmentResultMapper.deleteById(id);
        return Result.success("删除成功", null);
    }
}