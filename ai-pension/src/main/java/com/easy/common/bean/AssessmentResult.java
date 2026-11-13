package com.easy.common.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("assessment_result")
public class AssessmentResult {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long user_id;
    private Long questionnaire_id;
    private String answers;
    private Integer ai_score;
    private String ai_suggestion;
    private LocalDateTime create_time;
    private LocalDateTime update_time;

    @TableLogic
    private Integer deleted;

}
