package com.easy.common.scheduled;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.easy.common.bean.Appointment;
import com.easy.common.bean.AppointmentSlot;
import com.easy.common.bean.CommunityActivity;
import com.easy.common.mapper.membermapper.IAppointmentMapper;
import com.easy.common.mapper.membermapper.IAppointmentSlotMapper;
import com.easy.common.mapper.membermapper.ICommunityActivityMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class ScheduledService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledService.class);

    @Autowired
    private ICommunityActivityMapper communityActivityMapper;

    @Autowired
    private IAppointmentMapper appointmentMapper;

    @Autowired
    private IAppointmentSlotMapper appointmentSlotMapper;

    @Scheduled(cron = "0 */5 * * * ?")    //秒分时日月周
    public void updateActivityStatus() {
        LocalDateTime now = LocalDateTime.now();

        LambdaQueryWrapper<CommunityActivity> lqw = new LambdaQueryWrapper<>();
        lqw.ne(CommunityActivity::getStatus, "ENDED");
        List<CommunityActivity> list = communityActivityMapper.selectList(lqw);

        int updated = 0;
        for (CommunityActivity activity : list) {
            String newStatus = computeStatus(activity, now);
            if (newStatus != null && !newStatus.equals(activity.getStatus())) {
                activity.setStatus(newStatus);
                activity.setUpdate_time(now);
                communityActivityMapper.updateById(activity);
                updated++;
            }
        }

        if (updated > 0) {
            log.info("[定时任务] 自动更新了 {} 个活动的状态", updated);
        }
    }

    private String computeStatus(CommunityActivity activity, LocalDateTime now) {   //判断活动处于的时段
        LocalDateTime regEnd = activity.getRegistration_end();
        LocalDateTime actStart = activity.getActivity_start();
        LocalDateTime actEnd = activity.getActivity_end();
        if (actEnd != null && now.isAfter(actEnd)) {
            return "ENDED";
        }
        if (actStart != null && !now.isBefore(actStart) && (actEnd == null || now.isBefore(actEnd))) {
            return "ONGOING";
        }
        if (regEnd != null && !now.isAfter(regEnd)) {
            return "RECRUITING";
        }
        return null;
    }

    @Scheduled(cron = "0 */5 * * * ?")
    public void updateAppointmentStatus() {   //更新预约状态
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        LambdaQueryWrapper<Appointment> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Appointment::getStatus, "CONFIRMED");
        List<Appointment> list = appointmentMapper.selectList(lqw);

        int updated = 0;
        for (Appointment appointment : list) {
            AppointmentSlot slot = appointmentSlotMapper.selectById(appointment.getSlot_id());
            if (slot == null || slot.getAppoint_date() == null) {
                continue;
            }
            if (slot.getAppoint_date().isBefore(today)) {
                appointment.setStatus("COMPLETED");
                appointment.setUpdate_time(now);
                appointmentMapper.updateById(appointment);
                updated++;
            }
        }

        if (updated > 0) {
            log.info("[定时任务] 自动完成了 {} 个已过期的预约", updated);
        }
    }

    @Scheduled(cron = "0 */5 * * * ?")
    public void updateSlotStatus() {    //更新预约时段
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        LambdaQueryWrapper<AppointmentSlot> lqw = new LambdaQueryWrapper<>();
        lqw.eq(AppointmentSlot::getStatus, "AVAILABLE");
        List<AppointmentSlot> list = appointmentSlotMapper.selectList(lqw);

        int updated = 0;
        for (AppointmentSlot slot : list) {
            if (slot.getAppoint_date() != null && slot.getAppoint_date().isBefore(today)) {
                slot.setStatus("EXPIRED");
                slot.setUpdate_time(now);
                appointmentSlotMapper.updateById(slot);
                updated++;
            }
        }

        if (updated > 0) {
            log.info("[定时任务] 自动关闭了 {} 个已过期的预约时段", updated);
        }
    }
}