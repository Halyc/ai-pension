package com.easy.common.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_conversation")
public class AI_conversation {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long user_id;
    private String session_id;
    private String title;
    private String role;
    private String message;
    private LocalDateTime create_time;
    private LocalDateTime update_time;

    @TableLogic
    private Integer deleted;
}
