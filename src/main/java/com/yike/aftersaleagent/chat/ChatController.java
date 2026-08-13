package com.yike.aftersaleagent.chat;

import com.yike.aftersaleagent.chat.api.ChatRequest;
import com.yike.aftersaleagent.identity.CurrentDemoUser;
import com.yike.aftersaleagent.identity.DemoUserContext;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final DemoUserContext demoUserContext;
    private final ChatOrchestrator chatOrchestrator;

    public ChatController(DemoUserContext demoUserContext, ChatOrchestrator chatOrchestrator) {
        this.demoUserContext = demoUserContext;
        this.chatOrchestrator = chatOrchestrator;
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Valid @RequestBody ChatRequest request) {
        CurrentDemoUser user = demoUserContext.requireCurrentUser();
        return chatOrchestrator.stream(request, user);
    }
}
