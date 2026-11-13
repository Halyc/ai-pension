package com.easy.user.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.easy.common.bean.HealthGuidance;
import com.easy.common.util.Result;

public interface IHealthGuidanceService extends IService<HealthGuidance> {

    Result listGuidances();

    Result markAsRead(Long id);
}