package com.easy.common.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("health_guidance")
public class HealthGuidance {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long user_id;
    private Long health_record_id;
    private String type;
    private String content;
    private Integer is_read;
    private LocalDateTime create_time;
    private LocalDateTime update_time;

    @TableLogic
    private Integer deleted;
}