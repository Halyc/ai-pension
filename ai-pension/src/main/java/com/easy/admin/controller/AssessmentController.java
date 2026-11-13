package com.easy.admin.controller;

import com.easy.admin.service.IAssessmentService;
import com.easy.common.annotation.OperLog;
import com.easy.common.util.Result;
import com.easy.common.util.Sys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/assessment")
public class AssessmentController {

    @Autowired
    private IAssessmentService assessmentService;

    @GetMapping("/questionnaires")
    public Result listQuestionnaires(    //查询问卷列表
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken) {
        return assessmentService.listQuestionnaires();
    }

    @OperLog(module = "健康评测", operation = "创建问卷", operateType = 1)
    @PostMapping("/questionnaires")
    public Result createQuestionnaire(     //创建问卷列表
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @RequestBody Object body) {
        return assessmentService.createQuestionnaire(body);
    }

    @OperLog(module = "健康评测", operation = "修改问卷", operateType = 2)
    @PutMapping("/questionnaires/{id}")
    public Result updateQuestionnaire(  //更新问卷的标题/描述（部分更新）
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @PathVariable Long id,
            @RequestBody Object body) {
        return assessmentService.updateQuestionnaire(id, body);
    }

    @OperLog(module = "健康评测", operation = "删除问卷", operateType = 3)
    @DeleteMapping("/questionnaires/{id}")
    public Result deleteQuestionnaire(   //	删除问卷
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @PathVariable Long id) {
        return assessmentService.deleteQuestionnaire(id);
    }

    @OperLog(module = "健康评测", operation = "发布问卷", operateType = 4)
    @PostMapping("/questionnaires/{id}/publish")
    public Result publishQuestionnaire(   //将问卷状态从草稿改为已发布
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @PathVariable Long id) {
        return assessmentService.publishQuestionnaire(id);
    }

    @GetMapping("/questions")
    public Result listQuestions(    //查询题目列表
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @RequestParam(required = false) Long questionnaireId) {
        return assessmentService.listQuestions(questionnaireId);
    }

    @OperLog(module = "健康评测", operation = "创建题目", operateType = 1)
    @PostMapping("/questions")
    public Result createQuestion(   //创建题目
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @RequestBody Object body) {
        return assessmentService.createQuestion(body);
    }

    @OperLog(module = "健康评测", operation = "修改题目", operateType = 2)
    @PutMapping("/questions/{id}")
    public Result updateQuestion(   //更新题目
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @PathVariable Long id,
            @RequestBody Object body) {
        return assessmentService.updateQuestion(id, body);
    }

    @OperLog(module = "健康评测", operation = "删除题目", operateType = 3)
    @DeleteMapping("/questions/{id}")
    public Result deleteQuestion(    //	删除题目
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @PathVariable Long id) {
        return assessmentService.deleteQuestion(id);
    }

    @GetMapping("/results")
    public Result listResults(  //	查询所有评测结果列表
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken) {
        return assessmentService.listResults();
    }

    @GetMapping("/results/{id}")
    public Result getResultDetail(  //查询单条评测结果详情
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @PathVariable Long id) {
        return assessmentService.getResultDetail(id);
    }

    @OperLog(module = "健康评测", operation = "删除评测结果", operateType = 3)
    @DeleteMapping("/results/{id}")
    public Result deleteResult( //删除评测结果
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @PathVariable Long id) {
        return assessmentService.deleteResult(id);
    }
}