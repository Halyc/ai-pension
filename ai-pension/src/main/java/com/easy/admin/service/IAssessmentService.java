package com.easy.admin.service;

import com.easy.common.util.Result;

public interface IAssessmentService {
    // 13.1 问卷管理
    Result listQuestionnaires();
    Result createQuestionnaire(Object body);
    Result updateQuestionnaire(Long id, Object body);
    Result deleteQuestionnaire(Long id);
    Result publishQuestionnaire(Long id);

    // 13.2 题目管理
    Result listQuestions(Long questionnaireId);
    Result createQuestion(Object body);
    Result updateQuestion(Long id, Object body);
    Result deleteQuestion(Long id);

    // 13.3 评测结果管理
    Result listResults();
    Result getResultDetail(Long id);
    Result deleteResult(Long id);
}