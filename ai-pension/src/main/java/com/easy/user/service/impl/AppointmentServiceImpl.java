package com.easy.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.easy.common.bean.Appointment;
import com.easy.common.bean.AppointmentPackage;
import com.easy.common.bean.AppointmentSlot;
import com.easy.common.bean.User;
import com.easy.common.mapper.membermapper.IAppointmentMapper;
import com.easy.common.mapper.membermapper.IAppointmentPackageMapper;
import com.easy.common.mapper.membermapper.IAppointmentSlotMapper;
import com.easy.common.mapper.membermapper.IUserMapper;
import com.easy.common.util.CurrentUser;
import com.easy.common.util.Result;
import com.easy.user.service.IAppointmentService;
import com.easy.user.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AppointmentServiceImpl extends ServiceImpl<IAppointmentMapper, Appointment>
        implements IAppointmentService {

    @Autowired
    IAppointmentPackageMapper appointmentPackageMapper;

    @Autowired
    IAppointmentSlotMapper appointmentSlotMapper;

    @Autowired
    IAppointmentMapper appointmentMapper;

    @Autowired
    IUserMapper userMapper;


    @Override
    public Result listPackages(String sortBy) {
        LambdaQueryWrapper<AppointmentPackage> lqw = new LambdaQueryWrapper<>();
        lqw.eq(AppointmentPackage::getStatus, "ENABLED");

        if ("price_asc".equals(sortBy)) {
            lqw.orderByAsc(AppointmentPackage::getPrice);
        } else if ("price_desc".equals(sortBy)) {
            lqw.orderByDesc(AppointmentPackage::getPrice);
        } else {
            lqw.orderByDesc(AppointmentPackage::getCreate_time);
        }
        List<AppointmentPackage> list = appointmentPackageMapper.selectList(lqw);

        return Result.success("success", list);
    }

    @Override
    public Result listSlots(Long packageId) {
        if (packageId == null) {
            return Result.error(400, "ID不能为空");
        }

        LambdaQueryWrapper<AppointmentSlot> lqw = new LambdaQueryWrapper<>();
        lqw.eq(AppointmentSlot::getPackage_id, packageId)
                .eq(AppointmentSlot::getStatus, "AVAILABLE")
                .ge(AppointmentSlot::getAppoint_date, LocalDateTime.now())
                .orderByAsc(AppointmentSlot::getAppoint_date)
                .orderByAsc(AppointmentSlot::getTime_range);

        List<AppointmentSlot> slots = appointmentSlotMapper.selectList(lqw);

        Map<String, List<AppointmentSlot>> groups = slots.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getAppoint_date().toString(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<AppointmentSlot>> entry : groups.entrySet()) {
            Map<String, Object> group = new LinkedHashMap<>();
            group.put("date", entry.getKey());

            List<Map<String, Object>> slotList = new ArrayList<>();
            for (AppointmentSlot s : entry.getValue()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", s.getId());
                item.put("packageId", s.getPackage_id());
                item.put("appointDate", s.getAppoint_date());
                item.put("timeRange", s.getTime_range());
                item.put("maxCount", s.getMax_count());
                item.put("currentCount", s.getCurrent_count());
                item.put("status", s.getStatus());

                boolean full = s.getCurrent_count() >= s.getMax_count();
                item.put("available", !full);

                slotList.add(item);
            }
            group.put("slots", slotList);
            result.add(group);
        }
        return Result.success("success", result);
    }

    @Override
    @Transactional
    public Result bookAppointment(Long slotId, Long packageId) {

        Long userID = CurrentUser.getCurrentUserID();
        if (userID == null) {
            return Result.error(401, "未登录");
        }
        if (slotId == null) {
            return Result.error(400, "参数错误");
        }

        AppointmentPackage pkg = appointmentPackageMapper.selectById(packageId);
        if (pkg == null || !"ENABLED".equals(pkg.getStatus())) {
            return Result.error(400, "不可预约");
        }

        AppointmentSlot slot = appointmentSlotMapper.selectById(slotId);
        if (slot == null) {
            return Result.error(400, "该时间段没有课程");
        }
        if (!slot.getPackage_id().equals(packageId)) {
            return Result.error(400, "时间段和课程套餐不匹配");
        }
        if (!"AVAILABLE".equals(slot.getStatus())) {
            return Result.error(400, "该时间段不可预约");
        }

        LambdaQueryWrapper<Appointment> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Appointment::getUser_id, userID)
                .eq(Appointment::getSlot_id, slotId)
                .eq(Appointment::getStatus, "CONFIRMED");

        Long count = appointmentMapper.selectCount(lqw);
        if (count > 0) {
            return Result.error(400, "您已经预约了该时间段");
        }

        int price = pkg.getPrice() == null ? 0 : pkg.getPrice();
        if (price > 0) {
            User user = userMapper.selectById(userID);
            if (user == null) {
                return Result.error(404, "用户不存在");
            }
            if (user.getPoints() < price) {
                return Result.error(400, "积分不足");
            }
        }

        LambdaUpdateWrapper<AppointmentSlot> slotUpdate = new LambdaUpdateWrapper<>();
        slotUpdate.eq(AppointmentSlot::getId, slotId)
                .lt(AppointmentSlot::getCurrent_count, slot.getMax_count())
                .setSql("current_count = current_count + 1")
                .setSql("status = CASE WHEN current_count + 1 >= max_count THEN 'FULL' ELSE status END");

        int rows = appointmentSlotMapper.update(null, slotUpdate);
        if (rows == 0) {
            return Result.error(400, "该时间段已经预约满");
        }

        if (price > 0) {
            updateUserPoints(userID, userMapper.selectById(userID).getPoints() - price);
        }

        Appointment appointment = new Appointment();
        appointment.setUser_id(userID);
        appointment.setSlot_id(slotId);
        appointment.setPackage_id(packageId);
        appointment.setStatus("CONFIRMED");
        appointmentMapper.insert(appointment);

        return Result.success("预约成功", appointment);
    }

    @Override
    public Result cancelAppointment(Long appointmentId) {

        Long userID = CurrentUser.getCurrentUserID();
        if (userID == null) {
            return Result.error(401, "未登录");
        }
        if (appointmentId == null) {
            return Result.error(400, "预约ID不能为空");
        }

        Appointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null) {
            return Result.error(404, "预约记录不存在");
        }
        if (!appointment.getUser_id().equals(userID)) {
            return Result.error(403, "只能本人来取消预约");
        }

        if (!"CONFIRMED".equals(appointment.getStatus())) {
            return Result.error(400, "当前状态不可取消");
        }

        appointment.setStatus("CANCELLED");
        appointmentMapper.updateById(appointment);

        AppointmentSlot slot = appointmentSlotMapper.selectById(appointment.getSlot_id());
        slot.setCurrent_count(Math.max(0, slot.getCurrent_count() - 1));
        if (slot != null) {
            if ("FULL".equals(slot.getStatus())) {
                slot.setStatus("AVAILABLE");
            }
            appointmentSlotMapper.updateById(slot);
        }

        AppointmentPackage pkg = appointmentPackageMapper.selectById(appointment.getPackage_id());
        if (pkg != null && pkg.getPrice() != null && pkg.getPrice() > 0) {
            User user = userMapper.selectById(userID);
            if (user != null) {
                updateUserPoints(userID, user.getPoints() + pkg.getPrice());
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("appointmentId", appointment.getId());
        data.put("refundPoints", pkg == null || pkg.getPrice() == null ? 0 : pkg.getPrice());

        return Result.success("取消成功,积分已经返还", null);
    }

    @Override
    public Result listAppointments() {

        Long userID = CurrentUser.getCurrentUserID();
        if (userID == null) {
            return Result.error(401, "未登录");
        }

        LambdaQueryWrapper<Appointment> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Appointment::getUser_id, userID)
                .orderByDesc(Appointment::getCreate_time);

        List<Appointment> appointments = appointmentMapper.selectList(lqw);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Appointment a : appointments) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", a.getId());
            item.put("status", a.getStatus());
            item.put("reportUrl", a.getReport_url());
            item.put("createTime", a.getCreate_time());

            AppointmentPackage pkg = appointmentPackageMapper.selectById(a.getPackage_id());
            if (pkg != null) {
                Map<String, Object> pkgMap = new LinkedHashMap<>();
                pkgMap.put("id", pkg.getId());
                pkgMap.put("name", pkg.getName());
                pkgMap.put("coverUrl", pkg.getCover_url());
                pkgMap.put("price", pkg.getPrice());
                item.put("package", pkgMap);
            }
            result.add(item);
        }
        return Result.success("success", result);
    }

    private void updateUserPoints(Long userId, int newPoints) {
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(User::getId, userId)
                .set(User::getPoints, newPoints)
                .setSql("update_time = NOW()");
        userMapper.update(null, wrapper);
    }
}