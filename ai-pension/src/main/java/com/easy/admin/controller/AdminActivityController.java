package com.easy.admin.controller;

import com.easy.admin.service.IAdminActivityService;
import com.easy.common.annotation.OperLog;
import com.easy.common.util.Result;
import com.easy.common.util.Sys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminActivityController {

    @Autowired
    private IAdminActivityService adminActivityService;

    @GetMapping("/activity/list")
    public Result listActivities(
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken) {
        return adminActivityService.listActivities();
    }

    @OperLog(module = "社区活动", operation = "创建活动", operateType = 1)
    @PostMapping("/activity")
    public Result createActivity(
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @RequestBody Object body) {
        return adminActivityService.createActivity(body);
    }

    @OperLog(module = "社区活动", operation = "修改活动", operateType = 2)
    @PutMapping("/activity/{id}")
    public Result updateActivity(
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @PathVariable Long id,
            @RequestBody Object body) {
        return adminActivityService.updateActivity(id, body);
    }

    @OperLog(module = "社区活动", operation = "删除活动", operateType = 3)
    @DeleteMapping("/activity/{id}")
    public Result deleteActivity(
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @PathVariable Long id) {
        return adminActivityService.deleteActivity(id);
    }

    @GetMapping("/activity/registrations")
    public Result listAllRegistrations(
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken) {
        return adminActivityService.listRegistrations(null);
    }

    @GetMapping("/activity/{activityId}/registrations")
    public Result listRegistrationsByActivity(
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @PathVariable Long activityId) {
        return adminActivityService.listRegistrations(activityId);
    }

    @GetMapping("/activity/registration/{id}")
    public Result getRegistrationDetail(
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @PathVariable Long id) {
        return adminActivityService.getRegistrationDetail(id);
    }

    @OperLog(module = "社区活动", operation = "活动签到", operateType = 4)
    @PostMapping("/activity/registration/check-in/{id}")
    public Result checkIn(
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @PathVariable Long id) {
        return adminActivityService.checkIn(id);
    }

    @OperLog(module = "社区活动", operation = "取消报名", operateType = 2)
    @DeleteMapping("/activity/registration/{id}")
    public Result cancelRegistration(
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @PathVariable Long id) {
        return adminActivityService.cancelRegistration(id);
    }
}