package com.easy.common.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("appointment")
public class Appointment {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long user_id;
    private Long slot_id;
    private Long package_id;
    private String status;
    private String report_url;
    private LocalDateTime create_time;
    private LocalDateTime update_time;

    @TableLogic
    private Integer deleted;

}