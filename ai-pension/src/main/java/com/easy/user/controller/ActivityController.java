package com.easy.user.controller;

import com.easy.common.annotation.OperLog;
import com.easy.common.util.Result;
import com.easy.user.service.IActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/member/activity")
public class ActivityController {

    @Autowired
    IActivityService activityService;

    @GetMapping("/list")
    public Result listActivities(@RequestParam(required = false) String status){
        return activityService.listActivities(status);
    }

    @GetMapping("/detail/{activityId}")
    public Result getActivityDetail(@PathVariable("activityId") Long activityId){
        return activityService.getActivityDetail(activityId);
    }

    @OperLog(module = "社区活动", operation = "报名活动", operateType = 1)
    @PostMapping("/register/{activityId}")
    public Result registerActivity(@PathVariable("activityId") Long activityId){
        return activityService.registerActivity(activityId);
    }

    @GetMapping("/my")
    public Result listMyActivities(){
        return activityService.listMyActivities();
    }

    @GetMapping("/is-registered/{activityId}")
    public Result checkRegistered(@PathVariable("activityId") Long activityId){  //检查是否完成活动
        return activityService.checkRegistered(activityId);
    }

    @GetMapping("/check-in-status/{activityId}")
    public Result getCheckInStatus(@PathVariable("activityId")Long activityId){  //获取签到状态
        return activityService.getCheckInStatus(activityId);
    }

    @OperLog(module = "社区活动", operation = "会员签到", operateType = 4)
    @PostMapping("/check-in/{activityId}")
    public Result checkIn(@PathVariable("activityId") Long activityId){
        return activityService.checkIn(activityId);
    }

    @OperLog(module = "社区活动", operation = "取消报名", operateType = 2)
    @DeleteMapping("/cancel/{activityId}")
    public Result cancelRegistration(@PathVariable("activityId") Long activityId){
        return activityService.cancelRegistration(activityId);
    }

}