package com.easy.user.dto;

import lombok.Data;

@Data
public class ChatSendRequest {   //用于接收前端发送新聊天消息时传来的请求体
    private String sessionId;
    private String message;

}
