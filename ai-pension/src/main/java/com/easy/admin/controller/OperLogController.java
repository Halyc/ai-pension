package com.easy.admin.controller;

import com.easy.admin.service.IOperLogService;
import com.easy.common.util.Result;
import com.easy.common.util.Sys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端操作日志查询。
 * 访问权限由 /api/admin/** 管理员拦截器统一校验。
 */
@RestController
@RequestMapping("/api/admin/oper-logs")
public class OperLogController {

    @Autowired
    private IOperLogService operLogService;

    @GetMapping
    public Result list(
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer operateType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return operLogService.pageLogs(page, size, module, status, startTime, endTime, keyword, operateType);
    }

    @GetMapping("/{id}")
    public Result detail(
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @PathVariable Long id) {
        return operLogService.getLogDetail(id);
    }
}
