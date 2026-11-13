package com.easy.admin.service;

import com.easy.common.util.Result;

public interface IAdminActivityService {
    // 14.1 活动管理
    Result listActivities();
    Result createActivity(Object body);
    Result updateActivity(Long id, Object body);
    Result deleteActivity(Long id);

    // 14.2 报名管理
    Result listRegistrations(Long activityId);
    Result getRegistrationDetail(Long id);
    Result checkIn(Long id);
    Result cancelRegistration(Long id);
}