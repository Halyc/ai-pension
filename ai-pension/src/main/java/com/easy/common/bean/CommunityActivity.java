package com.easy.common.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("community_activity")
public class CommunityActivity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String cover_url;
    private String content;
    private LocalDateTime registration_start;
    private LocalDateTime registration_end;
    private LocalDateTime activity_start;
    private LocalDateTime activity_end;
    private Integer max_participants;
    private Integer current_participants;
    private String status;
    private LocalDateTime create_time;
    private LocalDateTime update_time;

    @TableLogic
    private Integer deleted;

}