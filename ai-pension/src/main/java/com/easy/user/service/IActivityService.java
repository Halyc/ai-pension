package com.easy.user.service;

import com.easy.common.util.Result;

public interface IActivityService {

    Result listActivities(String status);

    Result getActivityDetail(Long activityId);

    Result registerActivity(Long activityId);

    Result listMyActivities();

    Result checkRegistered(Long activityId);

    Result getCheckInStatus(Long activityId);

    Result checkIn(Long activityId);

    Result cancelRegistration(Long activityId);

}