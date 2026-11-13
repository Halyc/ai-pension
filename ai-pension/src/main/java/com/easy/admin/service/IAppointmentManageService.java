package com.easy.admin.service;

import com.easy.common.util.Result;

public interface IAppointmentManageService {
    // 12.1 获取套餐列表
    Result listPackages(Integer page, Integer size, String keyword);

    // 12.2 创建套餐
    Result createPackage(Object body);

    // 12.3 更新套餐
    Result updatePackage(Long id, Object body);

    // 12.4 删除套餐
    Result deletePackage(Long id);

    // 12.5 获取套餐时段列表
    Result listSlots(Long packageId);

    // 12.6 批量生成时段
    Result batchCreateSlots(Object body);

    // 12.7 删除时段
    Result deleteSlot(Long id);

    // 12.8 批量删除时段
    Result batchDeleteSlots(Object body);

    // 12.9 获取预约列表
    Result listAppointments(Integer page, Integer size, String status, String date);

    // 12.10 完成体检
    Result completeAppointment(Long id, Object body);

    // 12.11 管理员上传体检报告
    Result uploadReport(Long id, String url);

}