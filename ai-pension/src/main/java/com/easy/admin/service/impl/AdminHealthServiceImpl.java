package com.easy.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.easy.admin.service.IAdminHealthService;
import com.easy.common.bean.Health;
import com.easy.common.bean.HealthGuidance;
import com.easy.common.bean.User;
import com.easy.common.mapper.membermapper.IHealthGuidanceMapper;
import com.easy.common.mapper.membermapper.IHealthMapper;
import com.easy.common.mapper.membermapper.IUserMapper;
import com.easy.common.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminHealthServiceImpl implements IAdminHealthService {

    @Autowired
    private IHealthMapper healthMapper;

    @Autowired
    private IHealthGuidanceMapper guidanceMapper;

    @Autowired
    private IUserMapper userMapper;

    @Override
    public Result listRecords(Integer page, Integer size, Long userId, String keyword) {  //查询用户健康记录表
        int currentPage = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : size;

        LambdaQueryWrapper<Health> lqw = new LambdaQueryWrapper<>();
        if (userId != null) {
            lqw.eq(Health::getUser_id, userId);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            List<Long> userIds = findUserIdsByKeyword(keyword.trim());
            if (!userIds.isEmpty()) {
                lqw.in(Health::getUser_id, userIds);
            } else {
                lqw.eq(Health::getId, keyword.trim());
            }
        }
        lqw.orderByDesc(Health::getRecorded_at);

        Page<Health> pageResult = healthMapper.selectPage(new Page<>(currentPage, pageSize), lqw);

        List<Long> userIds = pageResult.getRecords().stream()
                .map(Health::getUser_id).distinct().collect(Collectors.toList());
        final Map<Long, User> userMap = userIds.isEmpty()
                ? new HashMap<>()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));

        List<Long> recordIds = pageResult.getRecords().stream()
                .map(Health::getId).collect(Collectors.toList());
        final Map<Long, HealthGuidance> guidanceMap = recordIds.isEmpty()
                ? new HashMap<>()
                : guidanceMapper.selectList(new LambdaQueryWrapper<HealthGuidance>()
                        .in(HealthGuidance::getHealth_record_id, recordIds))
                        .stream().collect(Collectors.toMap(
                                HealthGuidance::getHealth_record_id, g -> g, (a, b) -> a));

        List<Map<String, Object>> records = pageResult.getRecords().stream().map(h -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", h.getId());
            item.put("user_id", h.getUser_id());
            item.put("systolic", h.getSystolic());
            item.put("diastolic", h.getDiastolic());
            item.put("blood_sugar", h.getBlood_sugar());
            item.put("heart_rate", h.getHeart_rate());
            item.put("weight", h.getWeight());
            item.put("height", h.getHeight());
            item.put("bmi", h.getBmi());
            item.put("memo", h.getMemo());
            item.put("recorded_at", h.getRecorded_at());

            User u = userMap.get(h.getUser_id());
            if (u != null) {
                Map<String, Object> user = new LinkedHashMap<>();
                user.put("id", u.getId());
                user.put("real_name", u.getReal_name());
                user.put("phone", u.getPhone());
                user.put("member_level", u.getMember_level());
                item.put("user", user);
            }

            HealthGuidance g = guidanceMap.get(h.getId());
            if (g != null) {
                Map<String, Object> gItem = new LinkedHashMap<>();
                gItem.put("id", g.getId());
                gItem.put("type", g.getType());
                gItem.put("content", g.getContent());
                gItem.put("is_read", g.getIs_read());
                gItem.put("create_time", g.getCreate_time());
                item.put("guidance", gItem);
            } else {
                item.put("guidance", null);
            }
            return item;
        }).collect(Collectors.toList());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", pageResult.getTotal());
        data.put("page", currentPage);
        data.put("size", pageSize);
        data.put("list", records);

        return Result.success("success", data);
    }

    @Override
    public Result listGuidances(Integer page, Integer size, Long userId) {  //查询用户的健康指导
        int currentPage = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : size;

        LambdaQueryWrapper<HealthGuidance> lqw = new LambdaQueryWrapper<>();
        if (userId != null) {
            lqw.eq(HealthGuidance::getUser_id, userId);
        }
        lqw.orderByDesc(HealthGuidance::getCreate_time);

        Page<HealthGuidance> pageResult = guidanceMapper.selectPage(new Page<>(currentPage, pageSize), lqw);

        List<Long> userIds = pageResult.getRecords().stream()
                .map(HealthGuidance::getUser_id).distinct().collect(Collectors.toList());
        final Map<Long, User> userMap = userIds.isEmpty()
                ? new HashMap<>()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));

        List<Long> recordIds = pageResult.getRecords().stream()
                .map(HealthGuidance::getHealth_record_id).filter(id -> id != null).distinct().collect(Collectors.toList());
        final Map<Long, Health> healthMap = recordIds.isEmpty()
                ? new HashMap<>()
                : healthMapper.selectBatchIds(recordIds).stream()
                        .collect(Collectors.toMap(Health::getId, h -> h));

        List<Map<String, Object>> records = pageResult.getRecords().stream().map(g -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", g.getId());
            item.put("user_id", g.getUser_id());
            item.put("health_record_id", g.getHealth_record_id());
            item.put("type", g.getType());
            item.put("content", g.getContent());
            item.put("is_read", g.getIs_read());
            item.put("create_time", g.getCreate_time());

            User u = userMap.get(g.getUser_id());
            if (u != null) {
                Map<String, Object> user = new LinkedHashMap<>();
                user.put("real_name", u.getReal_name());
                user.put("phone", u.getPhone());
                item.put("user", user);
            }

            Health h = healthMap.get(g.getHealth_record_id());
            if (h != null) {
                Map<String, Object> summary = new LinkedHashMap<>();
                summary.put("recorded_at", h.getRecorded_at());
                summary.put("systolic", h.getSystolic());
                summary.put("diastolic", h.getDiastolic());
                summary.put("blood_sugar", h.getBlood_sugar());
                summary.put("heart_rate", h.getHeart_rate());
                item.put("health_record_summary", summary);
            } else {
                item.put("health_record_summary", null);
            }
            return item;
        }).collect(Collectors.toList());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", pageResult.getTotal());
        data.put("page", currentPage);
        data.put("size", pageSize);
        data.put("list", records);

        return Result.success("success", data);
    }

    @Override
    public Result getRecordDetail(Long id) {      //查询健康记录的详情
        Health h = healthMapper.selectById(id);
        if (h == null) return Result.error(404, "记录不存在");

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", h.getId());
        item.put("user_id", h.getUser_id());
        item.put("systolic", h.getSystolic());
        item.put("diastolic", h.getDiastolic());
        item.put("blood_sugar", h.getBlood_sugar());
        item.put("heart_rate", h.getHeart_rate());
        item.put("weight", h.getWeight());
        item.put("height", h.getHeight());
        item.put("bmi", h.getBmi());
        item.put("memo", h.getMemo());
        item.put("recorded_at", h.getRecorded_at());

        User u = userMapper.selectById(h.getUser_id());
        if (u != null) {
            Map<String, Object> user = new LinkedHashMap<>();
            user.put("id", u.getId());
            user.put("real_name", u.getReal_name());
            user.put("phone", u.getPhone());
            user.put("member_level", u.getMember_level());
            item.put("user", user);
        } else {
            item.put("user", null);
        }

        LambdaQueryWrapper<HealthGuidance> gqw = new LambdaQueryWrapper<>();
        gqw.eq(HealthGuidance::getHealth_record_id, id);
        HealthGuidance g = guidanceMapper.selectOne(gqw);
        if (g != null) {
            Map<String, Object> gItem = new LinkedHashMap<>();
            gItem.put("id", g.getId());
            gItem.put("type", g.getType());
            gItem.put("content", g.getContent());
            gItem.put("is_read", g.getIs_read());
            gItem.put("create_time", g.getCreate_time());
            item.put("guidance", gItem);
        } else {
            item.put("guidance", null);
        }

        return Result.success("success", item);
    }

    @Override
    @Transactional
    public Result deleteRecord(Long id) {
        Health h = healthMapper.selectById(id);
        if (h == null) return Result.error(404, "记录不存在");

        LambdaQueryWrapper<HealthGuidance> gqw = new LambdaQueryWrapper<>();
        gqw.eq(HealthGuidance::getHealth_record_id, id);
        guidanceMapper.delete(gqw);

        healthMapper.deleteById(id);
        return Result.success("删除成功", null);
    }

    @Override
    public Result deleteGuidance(Long id) {
        guidanceMapper.deleteById(id);
        return Result.success("删除成功", null);
    }

    private List<Long> findUserIdsByKeyword(String keyword) {   //通过姓名/手机号模糊匹配用户，返回用户 ID 列表，供 listRecords() 内部使用
        LambdaQueryWrapper<User> uqw = new LambdaQueryWrapper<>();
        uqw.like(User::getReal_name, keyword).or().like(User::getPhone, keyword);
        List<User> users = userMapper.selectList(uqw);
        return users.stream().map(User::getId).collect(Collectors.toList());
    }
}