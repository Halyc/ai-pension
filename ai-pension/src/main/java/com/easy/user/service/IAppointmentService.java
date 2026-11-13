package com.easy.user.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.easy.common.bean.Appointment;
import com.easy.common.util.Result;

public interface IAppointmentService extends IService<Appointment> {

    Result listPackages(String sortBy);

    Result listSlots(Long packageId);

    Result bookAppointment(Long slotId, Long packageId);

    Result cancelAppointment(Long appointmentId);

    Result listAppointments();

}