package com.easy.user.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.easy.common.bean.Question;
import com.easy.common.bean.Questionnaire;
import com.easy.common.util.RegexUtil;
import com.easy.common.util.Result;

import java.util.Map;
import java.util.Objects;

public interface ITestService {

    Result questionnaires();

    Result questions(Long questionnaireId);

    Result submit(Map<String, Object> body);

    Result history();

    Result detail(Long resultId);
}
