package com.easy.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.easy.admin.service.IOperLogService;
import com.easy.common.bean.SysOperLog;
import com.easy.common.exception.ServiceException;
import com.easy.common.mapper.adminmapper.ISysOperLogMapper;
import com.easy.common.util.Result;
import com.easy.common.util.SensitiveDataMasker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OperLogServiceImpl extends ServiceImpl<ISysOperLogMapper, SysOperLog>
        implements IOperLogService {

    @Autowired
    ISysOperLogMapper sysOperLogMapper;

    /**
     * 异步写入日志，不阻塞业务接口
     */
    @Async("operLogExecutor")
    @Override
    public void saveLogAsync(SysOperLog log) {   //异步保存一条操作日志到数据库

        sysOperLogMapper.insert(log);
    }

    @Override
    //按条件分页查询操作日志，返回统一格式结果
    public Result pageLogs(int pageNum, int pageSize, String module, Integer status,
                           String startTime, String endTime, String keyword, Integer operateType) {
        int currentPage = Math.max(pageNum, 1);
        int currentSize = Math.min(Math.max(pageSize, 1), 100);

        LambdaQueryWrapper<SysOperLog> lqw = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(module)) {
            lqw.eq(SysOperLog::getModule, module.trim());
        }
        if (status != null) {
            lqw.eq(SysOperLog::getStatus, status);
        }
        if (operateType != null) {
            lqw.eq(SysOperLog::getOperate_type, operateType);
        }
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            lqw.and(w -> w.like(SysOperLog::getOperation, value)
                    .or().like(SysOperLog::getModule, value)
                    .or().like(SysOperLog::getUser_name, value)
                    .or().like(SysOperLog::getUser_real_name, value)
                    .or().like(SysOperLog::getRequest_url, value)
                    .or().like(SysOperLog::getIp, value));
        }
        if (StringUtils.hasText(startTime)) {
            lqw.ge(SysOperLog::getOperate_time, parseTime(startTime, "开始时间"));
        }
        if (StringUtils.hasText(endTime)) {
            lqw.le(SysOperLog::getOperate_time, parseTime(endTime, "结束时间"));
        }

        lqw.orderByDesc(SysOperLog::getOperate_time);

        Page<SysOperLog> page = new Page<>(currentPage, currentSize);
        Page<SysOperLog> result = sysOperLogMapper.selectPage(page, lqw);

        List<SysOperLog> records = result.getRecords();
        records.forEach(this::maskSensitiveData);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", records);
        data.put("total", result.getTotal());
        data.put("page", result.getCurrent());
        data.put("size", result.getSize());
        data.put("pages", result.getPages());
        return Result.success("success", data);
    }

    @Override
    public Result getLogDetail(Long id) {
        if (id == null) {
            return Result.error(400, "日志ID不能为空");
        }
        SysOperLog log = this.getById(id);
        if (log == null) {
            return Result.error(404, "操作日志不存在");
        }
        maskSensitiveData(log);
        return Result.success("success", log);
    }

    private LocalDateTime parseTime(String value, String label) {
        String normalized = value.trim().replace('T', ' ');
        DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        };
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(normalized, formatter);
            } catch (Exception ignored) {
                // 尝试下一种时间格式
            }
        }
        throw new ServiceException(400, label + "格式错误，应为 yyyy-MM-dd HH:mm:ss");
    }

    private void maskSensitiveData(SysOperLog log) {
        if (log != null && StringUtils.hasText(log.getRequest_params())) {
            log.setRequest_params(SensitiveDataMasker.maskJson(log.getRequest_params()));
        }
    }
}
