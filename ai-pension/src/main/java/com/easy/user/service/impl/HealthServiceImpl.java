package com.easy.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.easy.common.bean.Health;
import com.easy.common.bean.HealthGuidance;
import com.easy.common.bean.SysConfig;
import com.easy.common.bean.User;
import com.easy.common.mapper.adminmapper.ISysConfigMapper;
import com.easy.common.mapper.membermapper.IHealthMapper;
import com.easy.common.mapper.membermapper.IUserMapper;
import com.easy.common.util.CurrentUser;
import com.easy.common.util.Result;
import com.easy.common.util.Sys;
import com.easy.user.service.IHealthGuidanceService;
import com.easy.user.service.IHealthService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class HealthServiceImpl extends ServiceImpl<IHealthMapper, Health> implements IHealthService {

    private final ChatClient chatClient;

    @Autowired
    private IHealthGuidanceService guidanceService;

    @Autowired
    private IUserMapper userMapper;

    @Autowired
    private ISysConfigMapper sysConfigMapper;

    private String resolveGuidancePrompt() {
        try {
            LambdaQueryWrapper<SysConfig> lqw = new LambdaQueryWrapper<>();
            lqw.eq(SysConfig::getConfig_key, "ai.health.guidance.prompt");
            SysConfig config = sysConfigMapper.selectOne(lqw);
            if (config != null && config.getConfig_value() != null && !config.getConfig_value().isBlank()) {
                return config.getConfig_value();
            }
        } catch (Exception ignored) {
        }
        return Sys.AI_HEALTH_GUIDANCE_SYSTEM_PROMPT;
    }

    private static final OpenAiChatOptions GUIDANCE_OPTIONS = OpenAiChatOptions.builder()
            .temperature(0.7)
            .build();

    public HealthServiceImpl(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem(Sys.AI_HEALTH_GUIDANCE_SYSTEM_PROMPT)
                .defaultOptions(GUIDANCE_OPTIONS)
                .build();
    }

    @Override
    @Transactional
    public Result record(Health health) {
        Long userID = CurrentUser.getCurrentUserID();
        health.setUser_id(userID);

        health.setBmi(calcBmi(health.getWeight(), health.getHeight()));

        LocalDateTime now = LocalDateTime.now();
        health.setRecorded_at(now);
        health.setCreate_time(now);
        health.setUpdate_time(now);

        if (!this.save(health)) {
            return Result.error(500, "保存失败");
        }

        CompletableFuture.runAsync(() -> generateAndSaveGuidance(health, userID, now));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("health", health);
        result.put("guidance", null);
        result.put("guidanceGenerating", true);

        return Result.success("记录成功，AI 健康指导正在生成中", result);
    }

    private void generateAndSaveGuidance(Health health, Long userID, LocalDateTime now) {  //作用：调用 AI 大模型生成健康指导并落库；异常时生成一条兜底的通用健康指导
        try {
            String userPrompt = buildGuidancePrompt(health, userID);

            String aiContent = chatClient.prompt()
                    .system(resolveGuidancePrompt())
                    .user(userPrompt)
                    .call()
                    .content();

            if (this.getById(health.getId()) == null) {
                return;
            }

            LocalDateTime savedAt = LocalDateTime.now();
            HealthGuidance g = new HealthGuidance();
            g.setUser_id(userID);
            g.setHealth_record_id(health.getId());
            g.setType(detectGuidanceType(health));
            g.setContent(aiContent);
            g.setIs_read(0);
            g.setCreate_time(savedAt);
            g.setUpdate_time(savedAt);
            guidanceService.save(g);
        } catch (Exception e) {
            if (this.getById(health.getId()) == null) {
                return;
            }

            LocalDateTime savedAt = LocalDateTime.now();
            HealthGuidance fallback = new HealthGuidance();
            fallback.setUser_id(userID);
            fallback.setHealth_record_id(health.getId());
            fallback.setType("通用指导");
            fallback.setContent("已收到您的健康记录，建议您继续保持规律作息、合理饮食，定期关注身体变化。如有不适请及时就医。");
            fallback.setIs_read(0);
            fallback.setCreate_time(savedAt);
            fallback.setUpdate_time(savedAt);
            guidanceService.save(fallback);
        }
    }

    private String buildGuidancePrompt(Health h, Long userID) {  //根据用户信息 + 体征数据，拼出一段发给 AI 的用户信息
        StringBuilder sb = new StringBuilder();

        User user = userMapper.selectById(userID);
        sb.append("【用户信息】\n");
        if (user != null) {
            String realName = user.getReal_name();
            String gender = user.getGender();
            String surname = (realName != null && !realName.isBlank()) ? realName.charAt(0) + "" : "";
            String salutation;
            if (surname.isBlank()) {
                salutation = "您";
            } else if ("男".equals(gender)) {
                salutation = surname + "叔叔";
            } else if ("女".equals(gender)) {
                salutation = surname + "阿姨";
            } else {
                salutation = surname + "先生/女士";
            }
            sb.append("姓名：").append(realName != null ? realName : "未知").append("\n");
            sb.append("性别：").append(gender != null ? gender : "未知").append("\n");
            sb.append("要求使用的称呼：").append(salutation).append("\n");
        } else {
            sb.append("姓名：未知\n性别：未知\n要求使用的称呼：您\n");
        }
        sb.append("（请在健康建议开头直接使用上述称呼，如\"张叔叔您好\"或\"李阿姨您好\"，严禁使用\"叔叔阿姨\"\"爷爷奶奶\"等泛指称呼）\n\n");

        sb.append("请根据以下老人刚刚录入的体征数据，生成健康指导：\n\n");
        sb.append("【本次体检数据】\n");
        if (h.getSystolic() != null) sb.append("- 收缩压：").append(h.getSystolic()).append(" mmHg\n");
        if (h.getDiastolic() != null) sb.append("- 舒张压：").append(h.getDiastolic()).append(" mmHg\n");
        if (h.getBlood_sugar() != null) sb.append("- 血糖：").append(h.getBlood_sugar()).append(" mmol/L\n");
        if (h.getHeart_rate() != null) sb.append("- 心率：").append(h.getHeart_rate()).append(" 次/分\n");
        if (h.getWeight() != null) sb.append("- 体重：").append(h.getWeight()).append(" kg\n");
        if (h.getHeight() != null) sb.append("- 身高：").append(h.getHeight()).append(" cm\n");
        if (h.getBmi() != null) sb.append("- BMI：").append(h.getBmi()).append("\n");
        if (h.getMemo() != null && !h.getMemo().isEmpty()) {
            sb.append("- 老人自述：").append(h.getMemo()).append("\n");
        }
        sb.append("\n请生成一段温暖、实用的健康指导。");
        return sb.toString();
    }

    private String detectGuidanceType(Health h) {
        if (h.getBmi() != null && (h.getBmi().compareTo(new BigDecimal("28")) >= 0 || h.getBmi().compareTo(new BigDecimal("18.5")) < 0)) {
            return "体重管理";
        }
        if (h.getSystolic() != null && h.getSystolic() >= 140) {
            return "血压关注";
        }
        if (h.getDiastolic() != null && h.getDiastolic() >= 90) {
            return "血压关注";
        }
        if (h.getBlood_sugar() != null && h.getBlood_sugar().compareTo(new BigDecimal("7.0")) >= 0) {
            return "血糖关注";
        }
        if (h.getHeart_rate() != null && (h.getHeart_rate() >= 100 || h.getHeart_rate() <= 60)) {
            return "心率关注";
        }
        return "日常健康";
    }

    private BigDecimal calcBmi(BigDecimal weight, BigDecimal height) {  //计算BMI
        if (weight == null || height == null || height.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        BigDecimal heightM = height.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        return weight.divide(heightM.multiply(heightM), 1, RoundingMode.HALF_UP);
    }

    @Override
    public Result history(Integer months) {
        if (months == null || months <= 0) {
            months = 12;
        }

        Long userID = CurrentUser.getCurrentUserID();
        LocalDateTime startTime = LocalDateTime.now().minusMonths(months);

        LambdaQueryWrapper<Health> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Health::getUser_id, userID)
                .ge(Health::getRecorded_at, startTime)
                .orderByDesc(Health::getRecorded_at);

        List<Health> list = this.list(lqw);

        return Result.success("success", list);
    }

    @Override
    public Result trend(Integer months) {   //作用：查询最近的健康记录，并加工成前端画折线图需要的"标签 + 序列"结构
        if (months == null || months <= 0) {
            months = 12;
        }

        Long userID = CurrentUser.getCurrentUserID();
        LocalDateTime startTime = LocalDateTime.now().minusMonths(months);

        LambdaQueryWrapper<Health> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Health::getUser_id, userID)
                .ge(Health::getRecorded_at, startTime)
                .orderByAsc(Health::getRecorded_at);

        List<Health> list = this.list(lqw);

        List<String> labels = new ArrayList<>();
        List<Integer> systolicList = new ArrayList<>();
        List<Integer> diastolicList = new ArrayList<>();
        List<BigDecimal> bloodSugarList = new ArrayList<>();
        List<Integer> heartRateList = new ArrayList<>();
        List<BigDecimal> weightList = new ArrayList<>();

        for (Health h : list) {
            String label = h.getRecorded_at() != null
                    ? String.format("%02d-%02d",
                    h.getRecorded_at().getMonthValue(),
                    h.getRecorded_at().getDayOfMonth())
                    : "";
            labels.add(label);

            systolicList.add(h.getSystolic() != null ? h.getSystolic() : 0);
            diastolicList.add(h.getDiastolic() != null ? h.getDiastolic() : 0);
            bloodSugarList.add(h.getBlood_sugar() != null ? h.getBlood_sugar() : BigDecimal.ZERO);
            heartRateList.add(h.getHeart_rate() != null ? h.getHeart_rate() : 0);
            weightList.add(h.getWeight() != null ? h.getWeight() : BigDecimal.ZERO);
        }

        Map<String, Object> bloodPressure = new LinkedHashMap<>();
        bloodPressure.put("labels", labels);
        bloodPressure.put("systolic", systolicList);
        bloodPressure.put("diastolic", diastolicList);

        Map<String, Object> bloodSugar = new LinkedHashMap<>();
        bloodSugar.put("labels", labels);
        bloodSugar.put("values", bloodSugarList);

        Map<String, Object> heartRate = new LinkedHashMap<>();
        heartRate.put("labels", labels);
        heartRate.put("values", heartRateList);

        Map<String, Object> weight = new LinkedHashMap<>();
        weight.put("labels", labels);
        weight.put("values", weightList);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("bloodPressure", bloodPressure);
        data.put("bloodSugar", bloodSugar);
        data.put("heartRate", heartRate);
        data.put("weight", weight);

        return Result.success("success", data);
    }

    @Override
    @Transactional
    public Result delete(Long id) {  //删除记录
        if (id == null) {
            return Result.error(400, "记录ID不能为空");
        }

        Long userID = CurrentUser.getCurrentUserID();
        Health health = this.getById(id);

        if (health == null) {
            return Result.error(404, "记录不存在");
        }

        if (!health.getUser_id().equals(userID)) {
            return Result.error(403, "无权删除此记录");
        }

        guidanceService.remove(new LambdaQueryWrapper<HealthGuidance>()
                .eq(HealthGuidance::getHealth_record_id, id));

        if (this.removeById(id)) {
            return Result.success("删除成功", null);
        }
        return Result.error(500, "删除失败");
    }

}