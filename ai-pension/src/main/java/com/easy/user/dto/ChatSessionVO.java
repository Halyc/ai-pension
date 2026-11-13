package com.easy.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatSessionVO {  //用于向前端返回会话列表时的数据封装
    private String sessionId;   //前端使用的原始会话 ID（不含用户前缀）
    private String title;       //会话标题
    private String preview;     //最后一条消息的预览
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer messageCount;
}
