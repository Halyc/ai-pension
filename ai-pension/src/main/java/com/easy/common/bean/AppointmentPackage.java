package com.easy.common.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("appointment_package")
public class AppointmentPackage {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String cover_url;
    private String description;
    private Integer price;
    private String suitable_people;
    private String items;
    private String status;
    private LocalDateTime create_time;
    private LocalDateTime update_time;

    @TableLogic
    private Integer deleted;
}
