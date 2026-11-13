package com.easy.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.easy.common.bean.Health;
import com.easy.common.bean.HealthGuidance;
import com.easy.common.mapper.membermapper.IHealthGuidanceMapper;
import com.easy.common.mapper.membermapper.IHealthMapper;
import com.easy.common.util.CurrentUser;
import com.easy.common.util.Result;
import com.easy.user.service.IHealthGuidanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class HealthGuidanceServiceImpl extends ServiceImpl<IHealthGuidanceMapper, HealthGuidance> implements IHealthGuidanceService {

    @Autowired
    private IHealthMapper healthMapper;

    @Override
    public Result listGuidances() {
        Long userID = CurrentUser.getCurrentUserID();
        LambdaQueryWrapper<HealthGuidance> lqw = new LambdaQueryWrapper<>();
        lqw.eq(HealthGuidance::getUser_id, userID)
                .orderByDesc(HealthGuidance::getCreate_time)
                .last("limit 50");
        List<HealthGuidance> list = this.list(lqw);
        return Result.success("success", enrichWithHealthSummary(list, false));
    }

    @Override
    public Result markAsRead(Long id) {
        if (id == null) {
            return Result.error(400, "ID不能为空");
        }
        Long userID = CurrentUser.getCurrentUserID();
        HealthGuidance g = this.getById(id);
        if (g == null || !g.getUser_id().equals(userID)) {
            return Result.error(404, "指导记录不存在");
        }
        g.setIs_read(1);
        g.setUpdate_time(LocalDateTime.now());
        this.updateById(g);
        return Result.success("已标记为已读", null);
    }

    private List<Map<String, Object>> enrichWithHealthSummary(List<HealthGuidance> list, boolean skipOrphan) {
        if (list.isEmpty()) return new ArrayList<>();

        List<Long> recordIds = list.stream()
                .map(HealthGuidance::getHealth_record_id)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Health> healthMap = new HashMap<>();
        if (!recordIds.isEmpty()) {
            List<Health> healths = healthMapper.selectBatchIds(recordIds);
            healthMap = healths.stream()
                    .collect(Collectors.toMap(Health::getId, h -> h));
        }

        List<Long> userIds = list.stream()
                .map(HealthGuidance::getUser_id)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Map<Long, Integer>> recordSeqMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (Long uid : userIds) {
                List<Health> allRecords = healthMapper.selectList(
                        new LambdaQueryWrapper<Health>()
                                .eq(Health::getUser_id, uid)
                                .orderByAsc(Health::getRecorded_at)
                );
                Map<Long, Integer> seqMap = new HashMap<>();
                for (int i = 0; i < allRecords.size(); i++) {
                    seqMap.put(allRecords.get(i).getId(), i + 1);
                }
                recordSeqMap.put(uid, seqMap);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (HealthGuidance g : list) {
            Long recordId = g.getHealth_record_id();
            boolean recordMissing = recordId != null && !healthMap.containsKey(recordId);

            if (skipOrphan && recordMissing) {
                continue;
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", g.getId());
            item.put("user_id", g.getUser_id());
            item.put("health_record_id", recordId);
            item.put("type", g.getType());
            item.put("content", g.getContent());
            item.put("is_read", g.getIs_read());
            item.put("create_time", g.getCreate_time());
            item.put("update_time", g.getUpdate_time());
            item.put("record_deleted", recordMissing);

            Health h = recordId != null ? healthMap.get(recordId) : null;
            if (h != null) {
                Map<String, Object> summary = new LinkedHashMap<>();
                summary.put("recorded_at", h.getRecorded_at());
                summary.put("systolic", h.getSystolic());
                summary.put("diastolic", h.getDiastolic());
                summary.put("blood_sugar", h.getBlood_sugar());
                summary.put("heart_rate", h.getHeart_rate());
                summary.put("weight", h.getWeight());
                summary.put("height", h.getHeight());
                summary.put("bmi", h.getBmi());
                summary.put("memo", h.getMemo());
                Map<Long, Integer> seqMap = recordSeqMap.get(g.getUser_id());
                if (seqMap != null && seqMap.containsKey(recordId)) {
                    summary.put("record_sequence", seqMap.get(recordId));
                }
                item.put("health_record_summary", summary);
            } else {
                item.put("health_record_summary", null);
            }
            result.add(item);
        }
        return result;
    }
}