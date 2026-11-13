package com.easy.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.easy.admin.service.IMemberManagementService;
import com.easy.common.bean.Appointment;
import com.easy.common.bean.AssessmentResult;
import com.easy.common.bean.User;
import com.easy.common.exception.ServiceException;
import com.easy.common.mapper.membermapper.IAppointmentMapper;
import com.easy.common.mapper.membermapper.IAssessmentResultMapper;
import com.easy.common.mapper.membermapper.IUserMapper;
import com.easy.common.util.CurrentUser;
import com.easy.common.util.EncodeUtil;
import com.easy.common.util.Result;
import com.easy.common.util.Sys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MemberManagementServiceImpl implements IMemberManagementService {

    @Autowired
    private IUserMapper userMapper;

    @Autowired
    private IAssessmentResultMapper assessmentResultMapper;

    @Autowired
    private IAppointmentMapper appointmentMapper;

    private static final List<Map<String, Object>> MEMBER_LEVELS = Arrays.asList(
            buildLevel("NORMAL", "普通会员"),
            buildLevel("SILVER", "银卡会员"),
            buildLevel("GOLD", "金卡会员"),
            buildLevel("PLATINUM", "铂金会员"),
            buildLevel("DIAMOND", "钻石会员")
    );

    private static Map<String, Object> buildLevel(String code, String name) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("code", code);
        map.put("name", name);
        return map;
    }

    private static final Map<String, Integer> LEVEL_PRIORITY = new LinkedHashMap<>();
    static {
        LEVEL_PRIORITY.put("DIAMOND", 5);
        LEVEL_PRIORITY.put("PLATINUM", 4);
        LEVEL_PRIORITY.put("GOLD", 3);
        LEVEL_PRIORITY.put("SILVER", 2);
        LEVEL_PRIORITY.put("NORMAL", 1);
    }

    private void checkCanOperate(Long targetUserId) {
        User target = userMapper.selectById(targetUserId);
        if (target == null) {
            throw new ServiceException(404, "用户不存在");
        }
    }

    @Override
    public Result getMemberList(Integer page, Integer size, String phone, String name, String status) {
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 5;

        LambdaQueryWrapper<User> lqw = new LambdaQueryWrapper<>();
        if (phone != null && !phone.trim().isEmpty()) {
            lqw.like(User::getPhone, phone.trim());
        }
        if (name != null && !name.trim().isEmpty()) {
            lqw.like(User::getReal_name, name.trim());
        }
        if (status != null && !status.trim().isEmpty()) {
            lqw.eq(User::getStatus, status.trim());
        }

        List<User> allUsers = userMapper.selectList(lqw);

        allUsers.sort((a, b) -> {
            boolean aIsAdmin = "ADMIN".equals(a.getRole());
            boolean bIsAdmin = "ADMIN".equals(b.getRole());
            if (aIsAdmin != bIsAdmin) {
                return aIsAdmin ? -1 : 1;
            }
            if (aIsAdmin && bIsAdmin) {
                return b.getCreate_time().compareTo(a.getCreate_time());
            }
            int aLevel = LEVEL_PRIORITY.getOrDefault(a.getMember_level(), 0);
            int bLevel = LEVEL_PRIORITY.getOrDefault(b.getMember_level(), 0);
            if (aLevel != bLevel) {
                return bLevel - aLevel;
            }
            return b.getPoints() - a.getPoints();
        });

        long total = allUsers.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, allUsers.size());
        List<User> pagedUsers = fromIndex >= allUsers.size() ? new ArrayList<>() : allUsers.subList(fromIndex, toIndex);

        List<Map<String, Object>> records = pagedUsers.stream().map(u -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", u.getId());
            item.put("phone", u.getPhone());
            item.put("real_name", u.getReal_name());
            item.put("role", u.getRole());
            item.put("member_level", u.getMember_level());
            item.put("points", u.getPoints());
            item.put("status", u.getStatus());
            item.put("create_time", u.getCreate_time());
            return item;
        }).collect(Collectors.toList());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", records);
        data.put("total", total);
        data.put("size", size);
        data.put("current", page);
        data.put("pages", (total + size - 1) / size);
        data.put("currentUserId", CurrentUser.getCurrentUserID());

        return Result.success("获取成功", data);
    }

    @Override
    public Result getMemberDetail(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException(404, "用户不存在");
        }
        checkCanOperate(userId);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twelveMonthsAgo = now.minusMonths(12);

        Long recentRecordsCount = assessmentResultMapper.selectCount(
                new LambdaQueryWrapper<AssessmentResult>()
                        .eq(AssessmentResult::getUser_id, userId)
                        .ge(AssessmentResult::getCreate_time, twelveMonthsAgo));

        Long totalAppointCount = appointmentMapper.selectCount(
                new LambdaQueryWrapper<Appointment>().eq(Appointment::getUser_id, userId)
        );

        Appointment latestAppointment = appointmentMapper.selectOne(
                new LambdaQueryWrapper<Appointment>()
                        .eq(Appointment::getUser_id, userId)
                        .orderByDesc(Appointment::getCreate_time)
                        .last("LIMIT 1")
        );

        Long completedCount = appointmentMapper.selectCount(
                new LambdaQueryWrapper<Appointment>()
                        .eq(Appointment::getUser_id, userId)
                        .eq(Appointment::getStatus, "COMPLETED")
        );

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", user.getId());
        item.put("phone", user.getPhone());
        item.put("real_name", user.getReal_name());
        item.put("gender", user.getGender());
        item.put("birthdate", user.getBirth_date());
        item.put("role", user.getRole());
        item.put("member_level", user.getMember_level());
        item.put("points", user.getPoints());
        item.put("status", user.getStatus());

        Map<String, Object> healthStats = new LinkedHashMap<>();
        healthStats.put("recentRecordsCount", recentRecordsCount);
        healthStats.put("lastRecordTime", latestAppointment != null ? latestAppointment.getCreate_time() : null);

        Map<String, Object> appointmentStats = new LinkedHashMap<>();
        appointmentStats.put("totalCount", totalAppointCount);
        appointmentStats.put("completedCount", completedCount);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("member", item);
        data.put("healthStats", healthStats);
        data.put("appointmentStats", appointmentStats);

        return Result.success("获取成功", data);
    }

    @Override
    @Transactional
    public Result enableMember(Long userId) {
        checkCanOperate(userId);
        LambdaUpdateWrapper<User> lqw = new LambdaUpdateWrapper<>();
        lqw.eq(User::getId, userId)
                .set(User::getStatus, Sys.USER_STATUS_ENABLED)
                .set(User::getUpdate_time, LocalDateTime.now());
        userMapper.update(null, lqw);
        return Result.success("启用成功", null);
    }

    @Override
    @Transactional
    public Result disableMember(Long userId) {
        checkCanOperate(userId);
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(User::getId, userId)
                .set(User::getStatus, Sys.USER_STATUS_DISABLED)
                .set(User::getUpdate_time, LocalDateTime.now());
        userMapper.update(null, wrapper);
        return Result.success("禁用成功", null);
    }

    @Override
    public Result getMemberLevels() {
        return Result.success("获取成功", MEMBER_LEVELS);
    }

    @Override
    @Transactional
    public Result updateMemberLevel(Long userId, String levelCode) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException(404, "用户不存在");
        }
        if ("ADMIN".equals(user.getRole())) {
            throw new ServiceException(400, "管理员无需设置会员等级");
        }
        boolean valid = MEMBER_LEVELS.stream().anyMatch(level -> levelCode.equals(level.get("code")));
        if (!valid) {
            throw new ServiceException(400, "无效的会员等级代码");
        }
        LambdaUpdateWrapper<User> lqw = new LambdaUpdateWrapper<>();
        lqw.eq(User::getId, userId)
                .set(User::getMember_level, levelCode)
                .set(User::getUpdate_time, LocalDateTime.now());
        userMapper.update(null, lqw);
        return Result.success("更新成功", null);
    }

    @Override
    @Transactional
    public Result updateMemberPoints(Long userId, Integer points) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException(404, "用户不存在");
        }
        if ("ADMIN".equals(user.getRole())) {
            throw new ServiceException(400, "管理员无积分");
        }
        if (points == 0) {
            throw new ServiceException(400, "积分变更量不能为0");
        }
        int newPoints = user.getPoints() + points;
        if (newPoints < 0) {
            throw new ServiceException(400, "积分不足，无法扣减");
        }
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(User::getId, userId)
                .set(User::getPoints, newPoints)
                .set(User::getUpdate_time, LocalDateTime.now());
        userMapper.update(null, wrapper);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", userId);
        data.put("points", newPoints);
        return Result.success("更新成功", data);
    }

    @Override
    @Transactional
    public Result resetMemberPassword(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException(404, "用户不存在");
        }
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(User::getId, userId)
                .set(User::getPassword, EncodeUtil.encode("123456"))
                .set(User::getUpdate_time, LocalDateTime.now());
        userMapper.update(null, wrapper);
        return Result.success("密码重置成功", null);
    }

    @Override
    @Transactional
    public Result addMember(String phone, String password, String role, Integer points) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new ServiceException(400, "手机号不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new ServiceException(400, "密码不能为空");
        }
        if (role == null || role.trim().isEmpty()) {
            throw new ServiceException(400, "用户身份不能为空");
        }
        if (!"ADMIN".equals(role) && !"MEMBER".equals(role)) {
            throw new ServiceException(400, "用户身份无效");
        }
        if (points != null && points < 0) {
            throw new ServiceException(400, "初始积分不能为负数");
        }

        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPhone, phone.trim())
        );
        if (count > 0) {
            throw new ServiceException(400, "该手机号已被注册");
        }

        User user = new User();
        user.setPhone(phone.trim());
        user.setPassword(EncodeUtil.encode(password.trim()));
        user.setRole(role);
        user.setStatus(Sys.USER_STATUS_ENABLED);
        if ("MEMBER".equals(role)) {
            user.setMember_level("NORMAL");
            user.setPoints(points != null ? points : 0);
        } else {
            user.setMember_level(null);
            user.setPoints(0);
        }
        user.setCreate_time(LocalDateTime.now());
        user.setUpdate_time(LocalDateTime.now());

        userMapper.insert(user);
        return Result.success("新增成功", null);
    }

    @Override
    @Transactional
    public Result deleteMember(Long userId) {
        checkCanOperate(userId);
        userMapper.deleteById(userId);
        return Result.success("删除成功", null);
    }
}