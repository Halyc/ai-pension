package com.easy.admin.controller;

import com.easy.admin.service.IDashboardService;
import com.easy.common.util.Result;
import com.easy.common.util.Sys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
public class DashboardController {

    @Autowired
    private IDashboardService dashboardService;

    @GetMapping("/summary")   //仪表盘数据汇总
    public Result getSummary(@RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken){  //要求请求头必须存在
        return dashboardService.getSummary();
    }
}
