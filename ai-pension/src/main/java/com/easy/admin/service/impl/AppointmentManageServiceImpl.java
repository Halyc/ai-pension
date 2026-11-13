package com.easy.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.easy.admin.service.IAppointmentManageService;
import com.easy.common.bean.Appointment;
import com.easy.common.bean.AppointmentPackage;
import com.easy.common.bean.AppointmentSlot;
import com.easy.common.bean.User;
import com.easy.common.mapper.membermapper.IAppointmentMapper;
import com.easy.common.mapper.membermapper.IAppointmentPackageMapper;
import com.easy.common.mapper.membermapper.IAppointmentSlotMapper;
import com.easy.common.mapper.membermapper.IUserMapper;
import com.easy.common.util.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service  // 将该类注册为 Spring 容器中的 Service Bean
public class AppointmentManageServiceImpl implements IAppointmentManageService {

    @Autowired
    private IAppointmentPackageMapper appointmentPackageMapper;

    @Autowired
    private IAppointmentSlotMapper appointmentSlotMapper;

    @Autowired
    private IAppointmentMapper appointmentMapper;

    @Autowired
    private IUserMapper userMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();  // 定义一个 JSON 转换工具，final 表示不可替换，线程安全且可复用

    @Override
    public Result listPackages(Integer page, Integer size, String keyword) {
        int currentPage = page == null || page < 1 ? 1 : page;    //条件运算符
        int pageSize = size == null || size < 1 ? 10 : size;

        LambdaQueryWrapper<AppointmentPackage> lqw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            lqw.like(AppointmentPackage::getName, keyword.trim());
        }
        lqw.orderByDesc(AppointmentPackage::getCreate_time);
        // 调用 MyBatis-Plus 的分页查询方法，传入分页对象 + 条件构造器
        Page<AppointmentPackage> pageResult = appointmentPackageMapper.selectPage(
                new Page<>(currentPage, pageSize), lqw);

        Map<String, Object> data = new LinkedHashMap<>();  //组装返回数据
        data.put("total", pageResult.getTotal());
        data.put("page", currentPage);
        data.put("size", pageSize);
        data.put("list", pageResult.getRecords());

        return Result.success("success", data);
    }

    @Override
    @Transactional      // 开启事务，方法执行完毕若无异常则提交，异常则回滚
    public Result createPackage(Object body) {
        Map<String, Object> map = toMap(body);  // 调用私有方法将 body 转为 Map
        if (map == null) {
            return Result.error(400, "参数格式错误");
        }

        String name = getString(map, "name");
        if (name == null || name.trim().isEmpty()) {
            return Result.error(400, "套餐名称不能为空");
        }

        AppointmentPackage pkg = new AppointmentPackage();
        pkg.setName(name.trim());
        pkg.setCover_url(getString(map, "coverUrl"));
        pkg.setDescription(getString(map, "description"));

        Object price = map.get("price");
        if (price != null) {
            pkg.setPrice(((Number) price).intValue());
        }

        pkg.setSuitable_people(getString(map, "suitablePeople"));  //设置合适人群

        Object items = map.get("items");     //套餐列表
        if (items != null) {
            try {
                pkg.setItems(objectMapper.writeValueAsString(items));
            } catch (Exception e) {
                pkg.setItems(String.valueOf(items));
            }
        }

        pkg.setStatus(getString(map, "status") != null ? getString(map, "status") : "ENABLED");
        pkg.setCreate_time(LocalDateTime.now());
        pkg.setUpdate_time(LocalDateTime.now());        //设置创建和更新时间为当前时间

        appointmentPackageMapper.insert(pkg);           //插入数据库
        return Result.success("创建成功", pkg);
    }

    @Override
    @Transactional
    public Result updatePackage(Long id, Object body) {
        if (id == null) {
            return Result.error(400, "套餐ID不能为空");
        }
        AppointmentPackage pkg = appointmentPackageMapper.selectById(id);  //ID查询套餐
        if (pkg == null) {
            return Result.error(404, "套餐不存在");
        }

        Map<String, Object> map = toMap(body);
        if (map == null) {
            return Result.error(400, "参数格式错误");
        }

        if (map.containsKey("name")) {
            String name = getString(map, "name");
            if (name == null || name.trim().isEmpty()) {
                return Result.error(400, "套餐名称不能为空");
            }
            pkg.setName(name.trim());
        }
        if (map.containsKey("coverUrl")) pkg.setCover_url(getString(map, "coverUrl"));
        if (map.containsKey("description")) pkg.setDescription(getString(map, "description"));
        if (map.containsKey("price")) {
            Object price = map.get("price");        //取价格字段
            if (price != null) {
                pkg.setPrice(((Number) price).intValue());
            }
        }
        if (map.containsKey("suitablePeople")) pkg.setSuitable_people(getString(map, "suitablePeople"));
        if (map.containsKey("items")) {
            try {
                pkg.setItems(objectMapper.writeValueAsString(map.get("items")));  //序列化为JSON
            } catch (Exception e) {
                pkg.setItems(String.valueOf(map.get("items")));
            }
        }
        if (map.containsKey("status")) pkg.setStatus(getString(map, "status"));

        pkg.setUpdate_time(LocalDateTime.now());
        appointmentPackageMapper.updateById(pkg);
        return Result.success("更新成功", pkg);
    }

    @Override
    @Transactional
    public Result deletePackage(Long id) {
        if (id == null) {
            return Result.error(400, "套餐ID不能为空");
        }
        AppointmentPackage pkg = appointmentPackageMapper.selectById(id);  //先检查套餐是否存在
        if (pkg == null) {
            return Result.error(404, "套餐不存在");
        }
        appointmentPackageMapper.deleteById(id);
        return Result.success("删除成功", null);
    }

    @Override
    public Result listSlots(Long packageId) {
        if (packageId == null) {
            return Result.error(400, "套餐ID不能为空");
        }
        LambdaQueryWrapper<AppointmentSlot> lqw = new LambdaQueryWrapper<>();
        lqw.eq(AppointmentSlot::getPackage_id, packageId)
                .orderByAsc(AppointmentSlot::getAppoint_date)
                .orderByAsc(AppointmentSlot::getTime_range);

        List<AppointmentSlot> slots = appointmentSlotMapper.selectList(lqw);  //创建查询列表

        List<Map<String, Object>> result = new ArrayList<>();
        for (AppointmentSlot s : slots) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", s.getId());
            item.put("packageId", s.getPackage_id());
            item.put("appointDate", s.getAppoint_date());
            item.put("timeRange", s.getTime_range());
            item.put("maxCount", s.getMax_count());
            item.put("currentCount", s.getCurrent_count());
            item.put("status", s.getStatus());
            item.put("createTime", s.getCreate_time());
            result.add(item);
        }
        return Result.success("success", result);
    }

    @Override
    @Transactional
    public Result batchCreateSlots(Object body) {
        Map<String, Object> map = toMap(body);
        if (map == null) {
            return Result.error(400, "参数格式错误");
        }

        Long packageId = getLong(map, "packageId");
        String startDateStr = getString(map, "startDate");
        String endDateStr = getString(map, "endDate");
        Object timeRangesObj = map.get("timeRanges");
        Integer maxCount = getInteger(map, "maxCount");

        if (packageId == null) {
            return Result.error(400, "套餐ID不能为空");
        }
        if (startDateStr == null || startDateStr.isEmpty()) {
            return Result.error(400, "开始日期不能为空");
        }
        if (endDateStr == null || endDateStr.isEmpty()) {
            return Result.error(400, "结束日期不能为空");
        }
        if (timeRangesObj == null) {
            return Result.error(400, "时间段列表不能为空");
        }

        AppointmentPackage pkg = appointmentPackageMapper.selectById(packageId);
        if (pkg == null) {
            return Result.error(404, "套餐不存在");
        }

        LocalDate startDate;
        LocalDate endDate;
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            startDate = LocalDate.parse(startDateStr, fmt);
            endDate = LocalDate.parse(endDateStr, fmt);
        } catch (Exception e) {
            return Result.error(400, "日期格式错误，应为 yyyy-MM-dd");
        }

        if (startDate.isAfter(endDate)) {
            return Result.error(400, "开始日期不能大于结束日期");
        }

        LocalDate today = LocalDate.now();
        if (startDate.isBefore(today)) {
            return Result.error(400, "开始日期不能早于今天");
        }

        List<String> timeRanges;
        if (timeRangesObj instanceof List) {
            timeRanges = ((List<?>) timeRangesObj).stream()
                    .map(Object::toString).collect(Collectors.toList());
        } else {
            timeRanges = Arrays.asList(timeRangesObj.toString());
        }

        int currentHour = LocalDateTime.now().getHour();
        int finalMaxCount = maxCount == null ? 10 : maxCount;

        int total = 0;
        int skippedPast = 0;
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            boolean isToday = cursor.equals(today);
            for (String tr : timeRanges) {
                if (isToday) {
                    int slotStartHour;
                    try {
                        slotStartHour = Integer.parseInt(tr.split("-")[0].split(":")[0]);
                    } catch (Exception e) {
                        continue;
                    }
                    if (slotStartHour <= currentHour) {
                        skippedPast++;
                        continue;
                    }
                }
                AppointmentSlot slot = new AppointmentSlot();
                slot.setPackage_id(packageId);
                slot.setAppoint_date(cursor);
                slot.setTime_range(tr);
                slot.setMax_count(finalMaxCount);
                slot.setCurrent_count(0);
                slot.setStatus("AVAILABLE");
                slot.setCreate_time(LocalDateTime.now());
                slot.setUpdate_time(LocalDateTime.now());
                appointmentSlotMapper.insert(slot);
                total++;
            }
            cursor = cursor.plusDays(1);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", total);
        data.put("skippedPast", skippedPast);
        String msg = "成功创建 " + total + " 个时段";
        if (skippedPast > 0) {
            msg += "，已跳过 " + skippedPast + " 个过期时段";
        }
        data.put("message", msg);

        return Result.success(200, msg, data);
    }

    @Override
    @Transactional
    public Result deleteSlot(Long id) {
        if (id == null) {
            return Result.error(400, "时段ID不能为空");
        }
        AppointmentSlot slot = appointmentSlotMapper.selectById(id);
        if (slot == null) {
            return Result.error(404, "时段不存在");
        }

        // 校验时段下是否存在已预约的记录
        LambdaQueryWrapper<Appointment> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Appointment::getSlot_id, id)
                .in(Appointment::getStatus, Arrays.asList("CONFIRMED", "COMPLETED"));
        Long count = appointmentMapper.selectCount(lqw);
        if (count != null && count > 0) {
            return Result.error(400, "该时段存在预约记录，无法删除");
        }

        appointmentSlotMapper.deleteById(id);
        return Result.success("删除成功", null);
    }

    @Override
    @Transactional
    public Result batchDeleteSlots(Object body) {
        Map<String, Object> map = toMap(body);
        if (map == null) {
            return Result.error(400, "参数格式错误");
        }
        Object idsObj = map.get("ids");
        if (idsObj == null || !(idsObj instanceof List)) {
            return Result.error(400, "时段ID列表不能为空");
        }
        List<?> ids = (List<?>) idsObj;
        if (ids.isEmpty()) {
            return Result.error(400, "时段ID列表不能为空");
        }

        int successCount = 0;
        int failCount = 0;
        List<String> failMessages = new ArrayList<>();

        for (Object idObj : ids) {
            Long id = ((Number) idObj).longValue();
            AppointmentSlot slot = appointmentSlotMapper.selectById(id);
            if (slot == null) {
                failCount++;
                failMessages.add("时段ID " + id + " 不存在");
                continue;
            }
            LambdaQueryWrapper<Appointment> lqw = new LambdaQueryWrapper<>();
            lqw.eq(Appointment::getSlot_id, id)
                    .in(Appointment::getStatus, Arrays.asList("CONFIRMED", "COMPLETED"));
            Long count = appointmentMapper.selectCount(lqw);
            if (count != null && count > 0) {
                failCount++;
                failMessages.add("时段ID " + id + " 存在预约记录，无法删除");
                continue;
            }
            appointmentSlotMapper.deleteById(id);
            successCount++;
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("successCount", successCount);
        data.put("failCount", failCount);
        data.put("failMessages", failMessages);

        return Result.success("批量删除完成", data);
    }

    @Override
    public Result listAppointments(Integer page, Integer size, String status, String date) {
        int currentPage = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : size;

        LambdaQueryWrapper<Appointment> lqw = new LambdaQueryWrapper<>();
        if (status != null && !status.trim().isEmpty()) {
            lqw.eq(Appointment::getStatus, status.trim());
        }
        if (date != null && !date.trim().isEmpty()) {
            LocalDate d;
            try {
                d = LocalDate.parse(date.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                lqw.apply("DATE(create_time) = {0}", d);
            } catch (Exception e) {
                // 若日期格式不合法则忽略过滤
            }
        }
        lqw.orderByDesc(Appointment::getCreate_time);

        Page<Appointment> pageResult = appointmentMapper.selectPage(
                new Page<>(currentPage, pageSize), lqw);

        List<Map<String, Object>> list = new ArrayList<>();
        for (Appointment a : pageResult.getRecords()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", a.getId());
            item.put("userId", a.getUser_id());
            item.put("slotId", a.getSlot_id());
            item.put("packageId", a.getPackage_id());
            item.put("status", a.getStatus());
            item.put("reportUrl", a.getReport_url());
            item.put("createTime", a.getCreate_time());
            item.put("updateTime", a.getUpdate_time());

            User u = userMapper.selectById(a.getUser_id());
            item.put("userName", u != null ? u.getReal_name() : null);

            AppointmentPackage pkg = appointmentPackageMapper.selectById(a.getPackage_id());
            item.put("packageName", pkg != null ? pkg.getName() : null);

            AppointmentSlot slot = appointmentSlotMapper.selectById(a.getSlot_id());
            if (slot != null) {
                item.put("appointDate", slot.getAppoint_date());
                item.put("timeRange", slot.getTime_range());
            }

            list.add(item);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", pageResult.getTotal());
        data.put("page", currentPage);
        data.put("size", pageSize);
        data.put("list", list);

        return Result.success("success", data);
    }

    @Override
    @Transactional
    public Result completeAppointment(Long id, Object body) {
        if (id == null) {
            return Result.error(400, "预约ID不能为空");
        }
        Appointment appointment = appointmentMapper.selectById(id);
        if (appointment == null) {
            return Result.error(404, "预约不存在");
        }
        if ("COMPLETED".equals(appointment.getStatus())) {
            return Result.error(400, "该预约已完成");
        }
        if (!"CONFIRMED".equals(appointment.getStatus())) {
            return Result.error(400, "当前状态不可完成体检");
        }

        appointment.setStatus("COMPLETED");
        appointment.setUpdate_time(LocalDateTime.now());
        appointmentMapper.updateById(appointment);

        return Result.success("体检已完成", appointment);
    }

    @Override
    @Transactional
    public Result uploadReport(Long id, String url) {
        if (id == null) {
            return Result.error(400, "预约ID不能为空");
        }
        if (url == null || url.trim().isEmpty()) {
            return Result.error(400, "报告URL不能为空");
        }
        Appointment appointment = appointmentMapper.selectById(id);
        if (appointment == null) {
            return Result.error(404, "预约不存在");
        }
        if ("CANCELLED".equals(appointment.getStatus())) {
            return Result.error(400, "已取消的预约不能上传报告");
        }
        appointment.setReport_url(url.trim());
        appointment.setUpdate_time(LocalDateTime.now());
        appointmentMapper.updateById(appointment);
        return Result.success("体检报告上传成功", appointment);
    }

    //将 Object（可能是 LinkedHashMap、也可能是 Jackson 反序列化后的对象）安全转为 Map
    private Map<String, Object> toMap(Object body) {
        if (body == null) return null;
        if (body instanceof Map) {       // 如果本身就是 Map，直接强转返回
            return (Map<String, Object>) body;
        }
        try {
            return objectMapper.convertValue(body, Map.class);   // 否则用 Jackson 反射转换
        } catch (Exception e) {
            return null;
        }
    }


    private String getString(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v == null ? null : v.toString();
    }

    private Long getLong(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        try {
            return Long.parseLong(v.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Integer getInteger(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return Integer.parseInt(v.toString());
        } catch (Exception e) {
            return null;
        }
    }
}