package com.easy.user.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.easy.common.bean.Health;
import com.easy.common.util.Result;

public interface IHealthService extends IService<Health> {

    Result record(Health health);

    Result history(Integer months);

    Result trend(Integer months);

    Result delete(Long id);
}