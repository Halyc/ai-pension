package com.easy.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatHistoryVO {  //用于向前端返回聊天历史记录时的数据封装
    private Long id;
    private String role;
    private String message;
    private LocalDateTime createTime;
}
