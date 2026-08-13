package com.yike.aftersaleagent.chat;

import com.yike.aftersaleagent.chat.domain.ChatMessage;
import com.yike.aftersaleagent.common.api.ApiResponse;
import com.yike.aftersaleagent.common.trace.RequestIdFilter;
import com.yike.aftersaleagent.identity.CurrentDemoUser;
import com.yike.aftersaleagent.identity.DemoUserContext;
import java.util.List;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {
    private final DemoUserContext demoUserContext;
    private final SessionService sessionService;

    public SessionController(DemoUserContext demoUserContext, SessionService sessionService) {
        this.demoUserContext = demoUserContext;
        this.sessionService = sessionService;
    }

    @PostMapping
    public ApiResponse<Map<String, String>> createSession() {
        CurrentDemoUser user = demoUserContext.requireCurrentUser();
        String sessionId = sessionService.createSession(user);
        return ApiResponse.success(
                Map.of("sessionId", sessionId), MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY));
    }

    @GetMapping("/{sessionId}/messages")
    public ApiResponse<List<ChatMessage>> listMessages(@PathVariable String sessionId) {
        CurrentDemoUser user = demoUserContext.requireCurrentUser();
        return ApiResponse.success(
                sessionService.listMessages(user, sessionId),
                MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY));
    }
}
