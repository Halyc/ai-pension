package com.easy.user.controller;

import com.easy.common.annotation.OperLog;
import com.easy.common.bean.Health;
import com.easy.common.util.Result;
import com.easy.user.service.IHealthGuidanceService;
import com.easy.user.service.IHealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/member/health")
public class HealthController {

    @Autowired
    IHealthService healthService;

    @Autowired
    IHealthGuidanceService guidanceService;

    @OperLog(module = "健康管理", operation = "记录健康数据", operateType = 1)
    @PostMapping("/record")
    public Result record(@RequestBody Health health) {   //记录健康记录，异步AI生成指导
        return healthService.record(health);
    }

    @PostMapping("/history")
    public Result history(@RequestParam(required = false, defaultValue = "12") Integer months) {
        return healthService.history(months);
    }

    @GetMapping("/trend")
    public Result trend(@RequestParam(required = false, defaultValue = "12") Integer months) {
        return healthService.trend(months);
    }

    @OperLog(module = "健康管理", operation = "删除健康记录", operateType = 2)
    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Long id) {
        return healthService.delete(id);
    }

    @GetMapping("/guidances")
    public Result guidances() {
        return guidanceService.listGuidances();
    }

    @PostMapping("/guidance/read/{id}")
    public Result markGuidanceRead(@PathVariable Long id) {
        return guidanceService.markAsRead(id);
    }
}