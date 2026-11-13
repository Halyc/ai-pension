package com.easy.common.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")

public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String phone;
    private String password;
    private String real_name;
    private String gender;
    private LocalDateTime birth_date;
    private String avatar;
    private String member_level;
    private int points;
    private String status;
    private String role;
    private LocalDateTime create_time;
    private LocalDateTime update_time;
    @TableLogic //标注状态(逻辑/软)删除  1代表已删除   0代表未删除
    private int deleted;
    //短信验证码：仅用于登录/注册/重置密码传参，不是数据库字段
    @TableField(exist = false)
    private String smsCode;

}