package com.easy.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.easy.common.bean.Appointment;
import com.easy.common.bean.CommunityActivity;
import com.easy.common.bean.User;
import com.easy.admin.service.IDashboardService;
import com.easy.common.mapper.membermapper.IAI_conversationMapper;
import com.easy.common.mapper.membermapper.IAppointmentMapper;
import com.easy.common.mapper.membermapper.ICommunityActivityMapper;
import com.easy.common.mapper.membermapper.IUserMapper;
import com.easy.common.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class DashboardServiceImpl implements IDashboardService {

    @Autowired
    private IUserMapper userMapper;

    @Autowired
    private IAppointmentMapper appointmentMapper;

    @Autowired
    private ICommunityActivityMapper communityActivityMapper;

    @Autowired
    private IAI_conversationMapper aiConversationMapper;

    @Override
    public Result getSummary() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        //会员总数
        Long totalMembers = userMapper
                .selectCount(new LambdaQueryWrapper<User>()
                        .eq(User::getRole, "MEMBER"));

        //今日新增会员数
        Long todayNewMembers = userMapper
                .selectCount(new LambdaQueryWrapper<User>()
                        .eq(User::getRole, "MEMBER")
                        .ge(User::getCreate_time, todayStart));

        //预约总数
        Long totalAppointments = appointmentMapper.selectCount(null);

        //今日预约数
        Long todayAppointments = appointmentMapper
                .selectCount(new LambdaQueryWrapper<Appointment>()
                        .ge(Appointment::getCreate_time, todayStart));

        //活动总数
        Long totalActivities = communityActivityMapper.selectCount(null);

        //进行中活动数
        Long ongoingActivities = communityActivityMapper
                .selectCount(new LambdaQueryWrapper<CommunityActivity>()
                        .in(CommunityActivity::getStatus, "ONGOING", "ACTIVE"));

        //对话总次数

        Long aiChatCount = aiConversationMapper.countSessions();

        Map<String, Object> data = new HashMap<>();
        data.put("totalMembers", totalMembers);
        data.put("todayNewMembers", todayNewMembers);
        data.put("totalAppointments", totalAppointments);
        data.put("todayAppointments", todayAppointments);
        data.put("totalActivities", totalActivities);
        data.put("ongoingActivities", ongoingActivities);
        data.put("aiChatCount", aiChatCount);

        return Result.success("获取成功", data);
    }
}