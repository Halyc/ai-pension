package com.easy.admin.controller;

import com.easy.admin.service.IConfigService;
import com.easy.common.annotation.OperLog;
import com.easy.common.util.Result;
import com.easy.common.util.Sys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/config")
public class ConfigController {

    @Autowired
    private IConfigService configService;

    @GetMapping("/list")
    public Result listConfigs(  //查询全部配置
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken) {
        return configService.listConfigs();
    }

    @GetMapping("/{key}")
    public Result getConfigByKey(  //按 key 查询单个配置
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @PathVariable String key) {
        return configService.getConfigByKey(key);
    }

    @OperLog(module = "系统配置", operation = "修改配置", operateType = 2)
    @PutMapping("/{key}")
    public Result updateConfig(   //按 key 更新配置，如果该 key 不存在则自动新建
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @PathVariable String key,
            @RequestBody Object body) {
        return configService.updateConfig(key, body);
    }
}