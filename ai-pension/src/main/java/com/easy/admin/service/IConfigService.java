package com.easy.admin.service;

import com.easy.common.util.Result;

public interface IConfigService {
    // 15.1 获取所有配置
    Result listConfigs();

    // 15.2 获取单个配置值
    Result getConfigByKey(String key);

    // 15.3 更新配置
    Result updateConfig(String key, Object body);
}
