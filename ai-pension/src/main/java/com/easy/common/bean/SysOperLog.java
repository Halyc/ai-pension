package com.easy.common.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_oper_log")
public class SysOperLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    // ========== 操作人信息 ==========
    private Long user_id;
    private String user_name;
    private String user_real_name;
    private String role;

    // ========== 操作行为 ==========
    private String module;
    private String operation;
    private String method;
    private String request_url;
    private String request_method;
    private String request_params;
    private Integer operate_type;

    // ========== 操作结果 ==========
    private Integer status;
    private String error_msg;
    private Long cost_time;

    // ========== 环境信息 ==========
    private String ip;
    private String user_agent;

    // ========== 时间 & 逻辑删除 ==========
    private LocalDateTime operate_time;
    private LocalDateTime create_time;
    private LocalDateTime update_time;

    @TableLogic
    private Integer deleted;
}