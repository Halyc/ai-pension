package com.easy.common.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("appointment_slot")
public class AppointmentSlot {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long package_id;
    private LocalDate appoint_date;
    private String time_range;
    private Integer max_count;
    private Integer current_count;
    private String status;
    private LocalDateTime create_time;
    private LocalDateTime update_time;

    @TableLogic
    private Integer deleted;
}
