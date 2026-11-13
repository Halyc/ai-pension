package com.easy.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.easy.admin.service.IAdminActivityService;
import com.easy.common.bean.ActivityRegistration;
import com.easy.common.bean.CommunityActivity;
import com.easy.common.bean.User;
import com.easy.common.mapper.membermapper.IActivityRegistrationMapper;
import com.easy.common.mapper.membermapper.ICommunityActivityMapper;
import com.easy.common.mapper.membermapper.IUserMapper;
import com.easy.common.util.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminActivityServiceImpl implements IAdminActivityService {

    @Autowired
    private ICommunityActivityMapper communityActivityMapper;

    @Autowired
    private IActivityRegistrationMapper activityRegistrationMapper;

    @Autowired
    private IUserMapper userMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ---------- 14.1 活动管理 ----------
    @Override
    public Result listActivities() {
        LambdaQueryWrapper<CommunityActivity> lqw = new LambdaQueryWrapper<>();
        lqw.orderByDesc(CommunityActivity::getCreate_time);
        List<CommunityActivity> list = communityActivityMapper.selectList(lqw);  //查询活动列表
        List<Map<String, Object>> result = new ArrayList<>();
        for (CommunityActivity a : list) {
            result.add(activityToMapWithRealCount(a));
        }
        return Result.success("success", result);
    }

    @Override
    public Result createActivity(Object body) {
        if (!(body instanceof Map)) {
            return Result.error(400, "请求参数格式错误");
        }
        Map<?, ?> map = (Map<?, ?>) body;
        String title = (String) map.get("title");
        if (title == null || title.trim().isEmpty()) {
            return Result.error(400, "活动标题不能为空");
        }
        CommunityActivity activity = mapToActivity(map);
        activity.setTitle(title.trim());
        activity.setCurrent_participants(0);
        activity.setStatus(map.get("status") == null ? null : (String) map.get("status"));
        activity.setCreate_time(LocalDateTime.now());
        activity.setUpdate_time(LocalDateTime.now());
        communityActivityMapper.insert(activity);
        return Result.success("创建成功", activity);
    }

    @Override
    public Result updateActivity(Long id, Object body) {
        if (id == null) {
            return Result.error(400, "活动ID不能为空");
        }
        CommunityActivity a = communityActivityMapper.selectById(id);
        if (a == null) {
            return Result.error(404, "活动不存在");
        }
        if (!(body instanceof Map)) {
            return Result.error(400, "请求参数格式错误");
        }
        Map<?, ?> map = (Map<?, ?>) body;
        if (map.containsKey("title")) a.setTitle((String) map.get("title"));
        if (map.containsKey("coverUrl")) a.setCover_url((String) map.get("coverUrl"));
        if (map.containsKey("content")) a.setContent((String) map.get("content"));
        if (map.containsKey("registrationStart"))
            a.setRegistration_start(parseDateTime(map.get("registrationStart")));
        if (map.containsKey("registrationEnd"))
            a.setRegistration_end(parseDateTime(map.get("registrationEnd")));
        if (map.containsKey("activityStart"))
            a.setActivity_start(parseDateTime(map.get("activityStart")));
        if (map.containsKey("activityEnd"))
            a.setActivity_end(parseDateTime(map.get("activityEnd")));
        if (map.containsKey("maxParticipants"))
            a.setMax_participants(Integer.valueOf(map.get("maxParticipants").toString()));
        if (map.containsKey("status")) a.setStatus((String) map.get("status"));
        a.setUpdate_time(LocalDateTime.now());
        communityActivityMapper.updateById(a);
        return Result.success("更新成功", a);
    }

    @Override
    public Result deleteActivity(Long id) {
        if (id == null) {
            return Result.error(400, "活动ID不能为空");
        }
        CommunityActivity activity = communityActivityMapper.selectById(id);
        if (activity == null) {
            return Result.error(404, "活动不存在");
        }
        communityActivityMapper.deleteById(id);
        return Result.success("删除成功", null);
    }

    // ---------- 14.2 报名管理 ----------
    @Override
    public Result listRegistrations(Long activityId) {
        LambdaQueryWrapper<ActivityRegistration> lqw = new LambdaQueryWrapper<>();
        if (activityId != null) {
            lqw.eq(ActivityRegistration::getActivity_id, activityId);  //双冒号::是Java8引入的方法引用运算符。
        }
        lqw.orderByDesc(ActivityRegistration::getCreate_time);
        List<ActivityRegistration> list = activityRegistrationMapper.selectList(lqw);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ActivityRegistration registration : list) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", registration.getId());
            item.put("userId", registration.getUser_id());
            item.put("activityId", registration.getActivity_id());
            item.put("checkInStatus", registration.getCheck_in_status());
            item.put("createTime", registration.getCreate_time());

            User user = userMapper.selectById(registration.getUser_id());
            if (user != null) {
                item.put("userName", user.getReal_name());
                item.put("userPhone", user.getPhone());
            }

            CommunityActivity activity = communityActivityMapper.selectById(registration.getActivity_id());
            if (activity != null) {
                item.put("activityTitle", activity.getTitle());
            }

            result.add(item);
        }
        return Result.success("success", result);
    }

    @Override
    public Result getRegistrationDetail(Long id) {
        if (id == null) {
            return Result.error(400, "报名ID不能为空");
        }
        ActivityRegistration r = activityRegistrationMapper.selectById(id);
        if (r == null) {
            return Result.error(404, "报名记录不存在");
        }
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", r.getId());
        item.put("userId", r.getUser_id());
        item.put("activityId", r.getActivity_id());
        item.put("checkInStatus", r.getCheck_in_status());
        item.put("createTime", r.getCreate_time());
        item.put("updateTime", r.getUpdate_time());

        User user = userMapper.selectById(r.getUser_id());
        if (user != null) {
            item.put("userName", user.getReal_name());
            item.put("userPhone", user.getPhone());
        }

        CommunityActivity activity = communityActivityMapper.selectById(r.getActivity_id());
        if (activity != null) {
            item.put("activityTitle", activity.getTitle());
        }

        return Result.success("success", item);
    }

    @Override
    public Result checkIn(Long id) {
        if (id == null) {
            return Result.error(400, "报名ID不能为空");
        }
        ActivityRegistration r = activityRegistrationMapper.selectById(id);
        if (r == null) {
            return Result.error(404, "报名记录不存在");
        }
        CommunityActivity activity = communityActivityMapper.selectById(r.getActivity_id());
        if (activity == null) {
            return Result.error(404, "活动不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getActivity_start()) || now.isAfter(activity.getActivity_end())) {
            return Result.error(400, "不在活动时间范围内，无法签到");
        }
        if ("CHECKED_IN".equals(r.getCheck_in_status())) {
            return Result.error(400, "该报名已签到");
        }
        r.setCheck_in_status("CHECKED_IN");
        r.setUpdate_time(LocalDateTime.now());
        activityRegistrationMapper.updateById(r);
        return Result.success("签到成功", r);
    }

    @Override
    @Transactional
    public Result cancelRegistration(Long id) {
        if (id == null) {
            return Result.error(400, "报名ID不能为空");
        }
        ActivityRegistration r = activityRegistrationMapper.selectById(id);
        if (r == null) {
            return Result.error(404, "报名记录不存在");
        }

        CommunityActivity activity = communityActivityMapper.selectById(r.getActivity_id());
        if (activity == null) {
            return Result.error(404, "活动不存在");
        }

        if ("CHECKED_IN".equals(r.getCheck_in_status())) {
            return Result.error(400, "该报名已签到，无法取消");
        }

        activityRegistrationMapper.deleteById(id);

        long realCountAfter = activityRegistrationMapper.selectCount(
                new LambdaQueryWrapper<ActivityRegistration>()
                        .eq(ActivityRegistration::getActivity_id, r.getActivity_id())
        );
        activity.setCurrent_participants((int) realCountAfter);
        communityActivityMapper.updateById(activity);

        return Result.success("取消报名成功", null);
    }

    private String computeDynamicStatus(CommunityActivity a) {   //根据活动时间动态计算"当前状态"字符串
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

    // ---------- 工具方法 ----------
    private Map<String, Object> activityToMapWithRealCount(CommunityActivity a) {
        long realCount = activityRegistrationMapper.selectCount(
                new LambdaQueryWrapper<ActivityRegistration>()
                        .eq(ActivityRegistration::getActivity_id, a.getId())
        );
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
        item.put("status", computeDynamicStatus(a));
        item.put("createTime", a.getCreate_time());
        item.put("updateTime", a.getUpdate_time());
        return item;
    }

    private CommunityActivity mapToActivity(Map<?, ?> map) {
        CommunityActivity a = new CommunityActivity();
        if (map.containsKey("coverUrl")) a.setCover_url((String) map.get("coverUrl"));
        if (map.containsKey("content")) a.setContent((String) map.get("content"));
        if (map.containsKey("registrationStart"))
            a.setRegistration_start(parseDateTime(map.get("registrationStart")));
        if (map.containsKey("registrationEnd"))
            a.setRegistration_end(parseDateTime(map.get("registrationEnd")));
        if (map.containsKey("activityStart"))
            a.setActivity_start(parseDateTime(map.get("activityStart")));
        if (map.containsKey("activityEnd"))
            a.setActivity_end(parseDateTime(map.get("activityEnd")));
        if (map.containsKey("maxParticipants"))
            a.setMax_participants(Integer.valueOf(map.get("maxParticipants").toString()));
        return a;
    }

    private LocalDateTime parseDateTime(Object o) {
        if (o == null) return null;
        try {
            return LocalDateTime.parse(o.toString());
        } catch (Exception e) {
            return null;
        }
    }
}