package com.yike.aftersaleagent.chat;

import com.yike.aftersaleagent.agent.Intent;
import com.yike.aftersaleagent.chat.domain.ChatMessage;
import com.yike.aftersaleagent.identity.CurrentDemoUser;
import java.util.List;

public interface SessionService {
    String createSession(CurrentDemoUser user);
    List<ChatMessage> listMessages(CurrentDemoUser user, String sessionId);
    void appendUserMessage(CurrentDemoUser user, String sessionId, String content);
    void appendAssistantMessage(CurrentDemoUser user, String sessionId, String content, Intent intent);
}
