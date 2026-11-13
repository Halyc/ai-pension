package com.easy.user.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.easy.common.bean.AI_conversation;
import com.easy.user.dto.ChatHistoryVO;
import com.easy.user.dto.ChatSessionVO;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

public interface IAI_conversationService extends IService<AI_conversation> {

    List<Message> getMessages(String sessionId);

    void clear(String sessionId);

    List<ChatHistoryVO> getHistory(String sessionId);

    List<ChatSessionVO> listSessions(Long userId);  //查询某个用户的全部会话（按最后消息时间倒序）

    void deleteSession(Long userId, String sessionId);  //删除某个会话的全部消息

    void deleteSessions(Long userId, List<String> sessionIds);  //批量删除会话

    void deleteAllSessions(Long userId);  //清空某个用户的全部会话

    void updateTitle(Long userId, String sessionId, String title);  //修改会话标题

}