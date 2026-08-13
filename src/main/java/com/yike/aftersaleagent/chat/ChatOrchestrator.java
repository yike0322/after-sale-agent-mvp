package com.yike.aftersaleagent.chat;

import com.yike.aftersaleagent.chat.api.ChatRequest;
import com.yike.aftersaleagent.identity.CurrentDemoUser;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface ChatOrchestrator {
    SseEmitter stream(ChatRequest request, CurrentDemoUser user);
}
