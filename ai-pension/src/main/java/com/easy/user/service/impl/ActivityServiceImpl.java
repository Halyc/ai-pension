package com.easy.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.easy.common.bean.ActivityRegistration;
import com.easy.common.bean.CommunityActivity;
import com.easy.common.mapper.membermapper.IActivityRegistrationMapper;
import com.easy.common.mapper.membermapper.ICommunityActivityMapper;
import com.easy.common.util.CurrentUser;
import com.easy.common.util.Result;
import com.easy.user.service.IActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ActivityServiceImpl implements IActivityService {

    @Autowired
    IActivityRegistrationMapper activityRegistrationMapper;

    @Autowired
    ICommunityActivityMapper communityActivityMapper;

    private String computeDynamicStatus(CommunityActivity a) {  //计算活动的时间字段，计算活动的当前状态
        LocalDateTime now = LocalDateTime.now();
        if (a.getActivity_end() != null && now.isAfter(a.getActivity_end())) {
            return "ENDED";
        }
        if (a.getActivity_start() != null && !now.isBefore(a.getActivity_start())
                && (a.getActivity_end() == null || !now.isAfter(a.getActivity_end()))) {
            return "ONGOING";
        }
        if (a.getRegistration_end() != null && now.isAfter(a.getRegistration_end())
                && (a.getActivity_start() == null || now.isBefore(a.getActivity_start()))) {
            return "REGISTRATION_CLOSED";
        }
        if (a.getRegistration_start() != null && !now.isBefore(a.getRegistration_start())
                && (a.getRegistration_end() == null || !now.isAfter(a.getRegistration_end()))) {
            return "RECRUITING";
        }
        if (a.getRegistration_start() != null && now.isBefore(a.getRegistration_start())) {
            return "NOT_STARTED";
        }
        return a.getStatus();
    }

    @Override
    public Result listActivities(String status) {
        LambdaQueryWrapper<CommunityActivity> lqw = new LambdaQueryWrapper<>();
        lqw.orderByDesc(CommunityActivity::getCreate_time);

        List<CommunityActivity> list = communityActivityMapper.selectList(lqw);

        List<Map<String, Object>> result = new ArrayList<>();
        for (CommunityActivity a : list) {
            long realCount = activityRegistrationMapper.selectCount(
                    new LambdaQueryWrapper<ActivityRegistration>()
                            .eq(ActivityRegistration::getActivity_id, a.getId())
            );

            String dynamicStatus = computeDynamicStatus(a);
            if (status != null && !status.isEmpty() && !status.equals(dynamicStatus)) {
                continue;
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", a.getId());
            item.put("title", a.getTitle());
            item.put("coverUrl", a.getCover_url());
            item.put("content", a.getContent());
            item.put("registrationStart", a.getRegistration_start());
            item.put("registrationEnd", a.getRegistration_end());
            item.put("activityStart", a.getActivity_start());
            item.put("activityEnd", a.getActivity_end());
            item.put("maxParticipants", a.getMax_participants());
            item.put("currentParticipants", realCount);
            item.put("status", dynamicStatus);
            result.add(item);
        }
        return Result.success("success", result);
    }

    @Override
    public Result getActivityDetail(Long activityId) {

        if (activityId == null) {
            return Result.error(400, "活动ID不能为空");
        }
        CommunityActivity activity = communityActivityMapper.selectById(activityId);
        if (activity == null) {
            return Result.error(404, "活动不存在");
        }

        long realCount = activityRegistrationMapper.selectCount(
                new LambdaQueryWrapper<ActivityRegistration>()
                        .eq(ActivityRegistration::getActivity_id, activityId)
        );

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", activity.getId());
        item.put("title", activity.getTitle());
        item.put("coverUrl", activity.getCover_url());
        item.put("content", activity.getContent());
        item.put("registrationStart", activity.getRegistration_start());
        item.put("registrationEnd", activity.getRegistration_end());
        item.put("activityStart", activity.getActivity_start());
        item.put("activityEnd", activity.getActivity_end());
        item.put("maxParticipants", activity.getMax_participants());
        item.put("currentParticipants", realCount);
        item.put("status", computeDynamicStatus(activity));
        return Result.success("success", item);
    }

    @Override
    @Transactional
    public Result registerActivity(Long activityId) {

        Long userID = CurrentUser.getCurrentUserID();
        if (userID == null) {
            return Result.error(401, "未登录");
        }
        if (activityId == null) {
            return Result.error(400, "活动ID不能为空");
        }

        CommunityActivity activity = communityActivityMapper.selectById(activityId);
        if (activity == null) {
            return Result.error(404, "活动不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getRegistration_start())) {
            return Result.error(400, "活动报名尚未开始");
        }
        if (now.isAfter(activity.getRegistration_end())) {
            return Result.error(400, "活动报名已结束");
        }

        LambdaQueryWrapper<ActivityRegistration> dupCheck = new LambdaQueryWrapper<>();
        dupCheck.eq(ActivityRegistration::getUser_id, userID)
                .eq(ActivityRegistration::getActivity_id, activityId);

        Long existingUser = activityRegistrationMapper.selectCount(dupCheck);
        if (existingUser > 0) {
            return Result.error(400, "该活动您已报名");
        }

        LambdaUpdateWrapper<CommunityActivity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CommunityActivity::getId, activityId)
                .lt(CommunityActivity::getCurrent_participants, activity.getMax_participants())
                .setSql("current_participants = current_participants + 1");

        int rows = communityActivityMapper.update(null, updateWrapper);
        if (rows == 0) {
            return Result.error(400, "报名人数已满");
        }

        ActivityRegistration registration = new ActivityRegistration();
        registration.setUser_id(userID);
        registration.setActivity_id(activityId);
        registration.setCheck_in_status("NOT_CHECKED_IN");
        registration.setCreate_time(LocalDateTime.now());
        registration.setUpdate_time(LocalDateTime.now());

        try {
            activityRegistrationMapper.insert(registration);
        } catch (DuplicateKeyException e) {
            return Result.error(400, "该活动您已报名");
        }

        return Result.success("报名成功", registration);
    }

    @Override
    public Result listMyActivities() {

        Long userID = CurrentUser.getCurrentUserID();
        if (userID == null) {
            return Result.error(401, "未登录");
        }

        LambdaQueryWrapper<ActivityRegistration> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ActivityRegistration::getUser_id, userID)
                .orderByDesc(ActivityRegistration::getCreate_time);

        List<ActivityRegistration> registrations = activityRegistrationMapper.selectList(lqw);

        List<Map<String, Object>> result = new ArrayList<>();
        for (ActivityRegistration r : registrations) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("registrationId", r.getId());
            item.put("activityId", r.getActivity_id());
            item.put("checkInStatus", r.getCheck_in_status());
            item.put("createTime", r.getCreate_time());

            CommunityActivity activity = communityActivityMapper.selectById(r.getActivity_id());
            if (activity != null) {
                Map<String, Object> act = new LinkedHashMap<>();
                act.put("id", activity.getId());
                act.put("title", activity);
                act.put("coverUrl", activity.getCover_url());
                act.put("activityStart", activity.getActivity_start());
                act.put("activityEnd", activity.getActivity_end());
                act.put("status", computeDynamicStatus(activity));
                item.put("activity", act);
            }
            result.add(item);

        }

        return Result.success("success", result);
    }

    @Override
    public Result checkRegistered(Long activityId) {
        Long userID = CurrentUser.getCurrentUserID();
        if (userID == null) {
            return Result.error(401, "未登录");
        }
        if (activityId == null) {
            return Result.error(400, "活动ID不能为空");
        }

        LambdaQueryWrapper<ActivityRegistration> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ActivityRegistration::getUser_id, userID)
                .eq(ActivityRegistration::getActivity_id, activityId);

        ActivityRegistration registration = activityRegistrationMapper.selectOne(lqw);

        Map<String, Object> data = new LinkedHashMap<>();
        if (registration != null) {
            data.put("registered", true);
            data.put("registrationId", registration.getId());
            data.put("checkInStatus", registration.getCheck_in_status());
        } else {
            data.put("registered", false);
        }
        return Result.success("success", data);
    }

    @Override
    public Result getCheckInStatus(Long activityId) {
        Long userID = CurrentUser.getCurrentUserID();
        if (userID == null) {
            return Result.error(401, "未登录");
        }
        if (activityId == null) {
            return Result.error(400, "活动ID不能为空");
        }

        LambdaQueryWrapper<ActivityRegistration> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ActivityRegistration::getUser_id, userID)
                .eq(ActivityRegistration::getActivity_id, activityId);

        ActivityRegistration registration = activityRegistrationMapper.selectOne(lqw);
        if (registration == null) {
            return Result.error(404, "未报名该活动");
        }

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("registrationId", registration.getId());
        item.put("checkInStatus", registration.getCheck_in_status());
        item.put("activityId", registration.getActivity_id());

        return Result.success("success", item);
    }

    @Override
    @Transactional
    public Result checkIn(Long activityId) {

        Long userID = CurrentUser.getCurrentUserID();
        if (userID == null){
            return Result.error(401,"未登录");
        }
        if (activityId == null){
            return Result.error(400,"活动ID不能未空");
        }

        CommunityActivity activity = communityActivityMapper.selectById(activityId);
        if (activity == null){
            return Result.error(404,"活动不存在");
        }

        LambdaQueryWrapper<ActivityRegistration> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ActivityRegistration::getUser_id,userID)
                .eq(ActivityRegistration::getActivity_id,activityId);
        ActivityRegistration registration = activityRegistrationMapper.selectOne(lqw);
        if (registration == null){
            return Result.error(400,"未报名该活动，无法签到");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getActivity_start()) || now.isAfter(activity.getActivity_end())){
            return Result.error(400,"不在活动范围内，无法签到");
        }

        if ("CHECKED_IN".equals(registration.getCheck_in_status())) {
            return Result.error(400, "您已签到");
        }

        registration.setCheck_in_status("CHECKED_IN");
        activityRegistrationMapper.updateById(registration);

        return Result.success("签到成功", registration);
    }

    @Override
    @Transactional
    public Result cancelRegistration(Long activityId) {

        Long userID = CurrentUser.getCurrentUserID();
        if (userID == null) {
            return Result.error(401, "未登录");
        }
        if (activityId == null) {
            return Result.error(400, "活动ID不能为空");
        }

        CommunityActivity activity = communityActivityMapper.selectById(activityId);
        if (activity == null) {
            return Result.error(404, "活动不存在");
        }

        LambdaQueryWrapper<ActivityRegistration> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ActivityRegistration::getUser_id, userID)
                .eq(ActivityRegistration::getActivity_id, activityId);
        ActivityRegistration registration = activityRegistrationMapper.selectOne(lqw);
        if (registration == null) {
            return Result.error(400, "您未报名该活动，无法取消");
        }

        if ("CHECKED_IN".equals(registration.getCheck_in_status())) {
            return Result.error(400, "已签到的活动无法取消报名");
        }

        activityRegistrationMapper.deleteById(registration.getId());

        long realCountAfter = activityRegistrationMapper.selectCount(
                new LambdaQueryWrapper<ActivityRegistration>()
                        .eq(ActivityRegistration::getActivity_id, activityId)
        );
        activity.setCurrent_participants((int) realCountAfter);
        communityActivityMapper.updateById(activity);

        return Result.success("取消报名成功", null);
    }
}