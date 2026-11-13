package com.easy.admin.controller;


import com.easy.admin.service.IAppointmentManageService;
import com.easy.common.annotation.OperLog;
import com.easy.common.service.IFileService;
import com.easy.common.util.Result;
import com.easy.common.util.Sys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/appointment")
public class AppointmentManageController {

    @Autowired
    private IAppointmentManageService appointmentManageService;

    @Autowired
    private IFileService fileService;

    @GetMapping("/packages")
    public Result listPackages(        //套餐列表分页查询
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String keyword) {
        return appointmentManageService.listPackages(page, size, keyword);
    }

    @OperLog(module = "预约服务", operation = "创建套餐", operateType = 1)
    @PostMapping("/package")
    public Result createPackage(     // 新建套餐
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @RequestBody Object body) {
        return appointmentManageService.createPackage(body);
    }

    @OperLog(module = "预约服务", operation = "修改套餐", operateType = 2)
    @PutMapping("/package/{id}")
    public Result updatePackage(   //更新套餐
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @PathVariable Long id,
            @RequestBody Object body) {
        return appointmentManageService.updatePackage(id, body);
    }

    @OperLog(module = "预约服务", operation = "删除套餐", operateType = 3)
    @DeleteMapping("/package/{id}")
    public Result deletePackage(   //删除套餐
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @PathVariable Long id) {
        return appointmentManageService.deletePackage(id);
    }

    @GetMapping("/slots/{packageId}")
    public Result listSlots(   //查询某套餐下的所有时段
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @PathVariable Long packageId) {
        return appointmentManageService.listSlots(packageId);
    }

    @OperLog(module = "预约服务", operation = "批量新增时段", operateType = 1)
    @PostMapping("/slots/batch")
    public Result batchCreateSlots(   //批量生成时段
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @RequestBody Object body) {
        return appointmentManageService.batchCreateSlots(body);
    }

    @OperLog(module = "预约服务", operation = "删除时段", operateType = 3)
    @DeleteMapping("/slot/{id}")
    public Result deleteSlot(    //删除单个时段
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @PathVariable Long id) {
        return appointmentManageService.deleteSlot(id);
    }

    @OperLog(module = "预约服务", operation = "批量删除时段", operateType = 3)
    @DeleteMapping("/slots/batch")
    public Result batchDeleteSlots(  //批量删除时段
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @RequestBody Object body) {
        return appointmentManageService.batchDeleteSlots(body);
    }

    @GetMapping("/list")
    public Result listAppointments(  // 预约记录分页查询
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String date) {
        return appointmentManageService.listAppointments(page, size, status, date);
    }

    @OperLog(module = "预约服务", operation = "上传体检报告", operateType = 1)
    @PostMapping("/uploadReport/{id}")
    public Result uploadReport(   //完成体检
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        Result uploadRes = fileService.uploadImage(file, "medical-reports");
        if (uploadRes.getCode() != 200) {
            return uploadRes;
        }
        String url = (String) ((Map<?, ?>) uploadRes.getData()).get("url");
        return appointmentManageService.uploadReport(id, url);
    }

    @OperLog(module = "预约服务", operation = "完成预约", operateType = 4)
    @PostMapping("/complete/{id}")
    public Result completeAppointment(  //上传体检报告
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @PathVariable Long id) {
        return appointmentManageService.completeAppointment(id, null);
    }
}