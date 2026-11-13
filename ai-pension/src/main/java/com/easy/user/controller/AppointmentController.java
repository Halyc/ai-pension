package com.easy.user.controller;

import com.easy.common.annotation.OperLog;
import com.easy.common.util.Result;
import com.easy.user.service.IAppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/member/appointment")
public class AppointmentController {

    @Autowired
    IAppointmentService appointmentService;

    @GetMapping("/packages")
    public Result listPackages(@RequestParam(required = false) String sortBy) {  //查询体检套餐
        return appointmentService.listPackages(sortBy);
    }

    @GetMapping("/slots/{packageId}")
    public Result listSlots(@PathVariable Long packageId) {   //查询套餐的时段
        return appointmentService.listSlots(packageId);
    }

    @OperLog(module = "预约服务", operation = "预约套餐", operateType = 1)
    @PostMapping("/book")
    public Result bookAppointment(@RequestParam Long slotId, @RequestParam Long packageId) { //预约套餐时段
        return appointmentService.bookAppointment(slotId, packageId);
    }

    @OperLog(module = "预约服务", operation = "取消预约", operateType = 3)
    @PostMapping("/cancel")
    public Result cancleAppointment(@RequestParam Long appointmentId) {  //取消预约
        return appointmentService.cancelAppointment(appointmentId);
    }

    @GetMapping("/list")
    public Result listAppointments() {   //查询预约记录
        return appointmentService.listAppointments();
    }
}