package com.easy.common.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("health_record")
public class Health {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long user_id;
    private Integer systolic;
    private Integer diastolic;
    private BigDecimal blood_sugar;
    private Integer heart_rate;
    private BigDecimal weight;
    private BigDecimal height;
    private BigDecimal bmi;
    private String memo;
    private LocalDateTime recorded_at;
    private LocalDateTime create_time;
    private LocalDateTime update_time;

}