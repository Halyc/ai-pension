package com.easy.common.aitools;

import com.easy.common.util.Result;
import com.easy.user.service.IActivityService;
import com.easy.user.service.IHealthService;
import com.easy.user.service.ITestService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

//定义AI对话中可供大模型调用的工具函数

@Component   //注册为Spring Bean  管理生命周期
public class MemberFunctionCalling {

    @Autowired   //依赖注入
    private ITestService testService;

    @Autowired
    private IHealthService healthService;

    @Autowired
    private IActivityService activityService;

    private final ObjectMapper objectMapper = new ObjectMapper();  // 创建Jackson ObjectMapper实例，用于JSON序列化/反序列化

    private String toJson(Object obj) {
        if (obj == null) return "null";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return String.valueOf(obj);
        }
    }

    private String unwrap(Result r) {
        if (r == null) {
            return "服务调用失败，Result为null";
        }
        if (r.getCode() != 200) {
            return "服务调用失败：code=" + r.getCode() + " message=" + r.getMessage();   //返回错误信息字符串
        }
        Object data = r.getData();
        if (data == null) {
            return "未查询到数据";
        }
        return toJson(data);
    }

    @Tool(description = "获取指定健康评测的详情，包含问卷标题、AI评分、AI建议、答题详情。resultId可从用户历史评测记录的id字段获取。")
    // @Tool注解：声明此方法为AI可调用工具，description描述功能供大模型理解何时该调用它
    public String getAssessmentDetail(
            @ToolParam(description = "评测结果ID，来自评测历史记录中的id字段") Long resultId) {
        if (resultId == null) {
            return "评测结果ID不能为空";
        }
        Result r = testService.detail(resultId);
        if (r == null || r.getCode() != 200) {
            return "未找到该评测记录（resultId=" + resultId + "）：" + (r != null ? r.getMessage() : "服务异常");
        }
        Object data = r.getData();
        return toJson(data);
    }

    @Tool(description = "查询用户的健康体征数据趋势，返回血压、血糖、心率、体重的时间序列数据。可指定查询月数和指标类型。")
    public String getHealthTrend(
            @ToolParam(description = "查询最近几个月，默认6个月") Integer months,
            @ToolParam(description = "指标类型：all(全部), blood_pressure(血压), blood_sugar(血糖), heart_rate(心率), weight(体重)") String metric) {
        if (months == null || months <= 0) {
            months = 6;
        }
        Result r = healthService.trend(months);
        String json = unwrap(r);
        if (metric == null || metric.equalsIgnoreCase("all") || metric.trim().isEmpty()) {
            return json;
        }
        try {
            java.util.Map<?, ?> map = objectMapper.readValue(json, java.util.Map.class);   //?为通配符，不确定返回的JSON是什么类型的字段
            java.util.Map<String, Object> filtered = new java.util.LinkedHashMap<>();  // 创建有序Map存放过滤结果
            String m = metric.trim().toLowerCase();
            if (m.contains("blood_pressure") || m.contains("pressure") || m.contains("血压")) {
                filtered.put("bloodPressure", map.get("bloodPressure"));
            }
            if (m.contains("blood_sugar") || m.contains("sugar") || m.contains("血糖")) {
                filtered.put("bloodSugar", map.get("bloodSugar"));
            }
            if (m.contains("heart_rate") || m.contains("rate") || m.contains("心率")) {
                filtered.put("heartRate", map.get("heartRate"));
            }
            if (m.contains("weight") || m.contains("体重")) {
                filtered.put("weight", map.get("weight"));
            }
            if (filtered.isEmpty()) return "未识别的指标类型：" + metric + "，可选：all, blood_pressure, blood_sugar, heart_rate, weight";
            return toJson(filtered);
        } catch (JsonProcessingException e) {
            return json;
        }
    }

    @Tool(description = "获取指定社区活动的详细信息，包括活动时间、地点、报名状态、剩余名额等。")
    public String getActivityDetail(
            @ToolParam(description = "活动ID") Long activityId) {
        if (activityId == null) return "活动ID不能为空";
        Result r = activityService.getActivityDetail(activityId);  //查询活动详情
        return unwrap(r);
    }
}