package com.easy.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.easy.admin.service.IConfigService;
import com.easy.common.bean.SysConfig;

import com.easy.common.mapper.adminmapper.ISysConfigMapper;
import com.easy.common.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ConfigServiceImpl implements IConfigService {

    @Autowired
    private ISysConfigMapper sysConfigMapper;

    // 15.1 获取所有配置
    @Override
    public Result listConfigs() {
        LambdaQueryWrapper<SysConfig> lqw = new LambdaQueryWrapper<>(); //创建查询条件包装器
        lqw.orderByAsc(SysConfig::getConfig_key);
        List<SysConfig> list = sysConfigMapper.selectList(lqw);
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysConfig c : list) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", c.getId());
            item.put("configKey", c.getConfig_key());
            item.put("configValue", c.getConfig_value());
            item.put("description", c.getDescription());
            result.add(item);
        }
        return Result.success("success", result);
    }

    // 15.2 获取单个配置值
    @Override
    public Result getConfigByKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return Result.error(400, "配置键不能为空");
        }
        LambdaQueryWrapper<SysConfig> lqw = new LambdaQueryWrapper<>();
        lqw.eq(SysConfig::getConfig_key, key);
        SysConfig c = sysConfigMapper.selectOne(lqw);
        if (c == null) {
            return Result.error(404, "配置项不存在");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", c.getId());
        data.put("configKey", c.getConfig_key());
        data.put("configValue", c.getConfig_value());
        data.put("description", c.getDescription());
        return Result.success("success", data);
    }

    // 15.3 更新配置
    @Override
    public Result updateConfig(String key, Object body) {
        if (key == null || key.trim().isEmpty()) {
            return Result.error(400, "配置键不能为空");
        }
        if (!(body instanceof Map)) {
            return Result.error(400, "请求参数格式错误");
        }
        Map<?, ?> map = (Map<?, ?>) body;
        String value = (String) map.get("value");
        if (value == null) {
            return Result.error(400, "配置值不能为空");
        }
        LambdaQueryWrapper<SysConfig> lqw = new LambdaQueryWrapper<>();
        lqw.eq(SysConfig::getConfig_key, key);
        SysConfig c = sysConfigMapper.selectOne(lqw);
        if (c == null) {
            // 不存在则新建
            c = new SysConfig();
            c.setConfig_key(key.trim());
            c.setConfig_value(value);
            c.setDescription((String) map.get("description"));
            c.setCreate_time(LocalDateTime.now());
            c.setUpdate_time(LocalDateTime.now());
            sysConfigMapper.insert(c);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", c.getId());
            data.put("message", "创建成功");
            return Result.success("success", data);
        } else {
            c.setConfig_value(value);
            c.setDescription((String) map.get("description"));
            c.setUpdate_time(LocalDateTime.now());
            sysConfigMapper.updateById(c);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", c.getId());
            data.put("message", "更新成功");
            return Result.success("更新成功", data);
        }
    }
}