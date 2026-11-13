package com.easy.common.memory;

import com.easy.user.service.IAI_conversationService;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component  // 注册为Spring Bean，Spring AI框架会自动发现并使用此ChatMemory实现
public class DBChatMemory implements ChatMemory {

    @Autowired
    private IAI_conversationService conversationService;

    @Override
    public void add(String conversationId, List<Message> messages) {
        // 消息落库统一由 ChatController 负责（用户消息在发起对话时保存，AI 回复在流式结束后保存）。
        // MessageChatMemoryAdvisor 也会回调这里，若在此再保存一次，同一个问题会在历史记录里出现两遍。
    }

    @Override
    public List<Message> get(String conversationId) {
        return conversationService.getMessages(conversationId);
    }

    @Override
    public void clear(String conversationId) {
        conversationService.clear(conversationId);
    }
}