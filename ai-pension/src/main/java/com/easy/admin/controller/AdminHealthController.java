package com.easy.admin.controller;

import com.easy.admin.service.IAdminHealthService;
import com.easy.common.util.Result;
import com.easy.common.util.Sys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/health")
public class AdminHealthController {

    @Autowired
    private IAdminHealthService adminHealthService;

    @GetMapping("/records")
    public Result listRecords(
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String keyword) {
        return adminHealthService.listRecords(page, size, userId, keyword);
    }

    @GetMapping("/guidances")
    public Result listGuidances(
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false) Long userId) {
        return adminHealthService.listGuidances(page, size, userId);
    }

    @GetMapping("/record/{id}")
    public Result getRecordDetail(
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @PathVariable Long id) {
        return adminHealthService.getRecordDetail(id);
    }

    @DeleteMapping("/record/{id}")
    public Result deleteRecord(
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @PathVariable Long id) {
        return adminHealthService.deleteRecord(id);
    }

    @DeleteMapping("/guidance/{id}")
    public Result deleteGuidance(
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @PathVariable Long id) {
        return adminHealthService.deleteGuidance(id);
    }
}