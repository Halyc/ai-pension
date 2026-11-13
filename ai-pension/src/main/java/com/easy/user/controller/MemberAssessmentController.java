package com.easy.user.controller;

import com.easy.common.util.Result;
import com.easy.user.service.ITestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/member/assessment")
public class MemberAssessmentController {

    @Autowired
    ITestService assessmentService;

    @GetMapping("/questionnaires")
    public Result questionnaires() {   //获取所有已发布的问卷列表
        return assessmentService.questionnaires();
    }

    @GetMapping("/questions/{questionnaireId}")
    public Result questions(@PathVariable Long questionnaireId) {  //获取问卷中的所有问题
        return assessmentService.questions(questionnaireId);
    }

    @PostMapping("/submit")
    public Result submit(@RequestBody Map<String, Object> body) {  //提交问卷答案
        return assessmentService.submit(body);
    }

    @GetMapping("/history")
    public Result history() {   //查询历史测评信息
        return assessmentService.history();
    }

    @GetMapping("/detail/{resultId}")
    public Result results(@PathVariable Long resultId) {  //查询某条测评结果的详细信息
        return assessmentService.detail(resultId);
    }

}
