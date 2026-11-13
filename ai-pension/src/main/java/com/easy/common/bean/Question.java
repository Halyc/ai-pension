package com.easy.common.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("question")
public class Question {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long questionnaire_id;
    private String content;
    private String type;
    private String options;
    private Integer sort_order;
    private LocalDateTime create_time;
    private LocalDateTime update_time;

    @TableLogic
    private Integer deleted;

}
