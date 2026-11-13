package com.easy.admin.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.easy.common.bean.SysOperLog;
import com.easy.common.util.Result;

public interface IOperLogService extends com.baomidou.mybatisplus.spring.service.IService<SysOperLog> {

    /** 异步保存操作日志（给切面调用） */
    void saveLogAsync(SysOperLog log);

    /** 管理端：分页查询日志列表 */
    Result pageLogs(int pageNum, int pageSize, String module, Integer status,
                    String startTime, String endTime, String keyword, Integer operateType);

    /** 管理端：查询日志详情 */
    Result getLogDetail(Long id);
}
