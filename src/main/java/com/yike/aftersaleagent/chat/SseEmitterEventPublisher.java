package com.yike.aftersaleagent.chat;

import com.yike.aftersaleagent.chat.api.ChatOutcome;
import java.io.IOException;
import java.util.Map;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

final class SseEmitterEventPublisher implements SseEventPublisher {
    private final SseEmitter emitter;
    private boolean closed;

    SseEmitterEventPublisher(SseEmitter emitter) {
        this.emitter = emitter;
    }

    @Override
    public void status(String text) {
        send(SseEmitter.event().name("status").data(text));
    }

    @Override
    public void message(ChatOutcome outcome) {
        send(SseEmitter.event().name("message").data(outcome));
    }

    @Override
    public void error(String code, String text) {
        send(SseEmitter.event().name("error").data(Map.of("code", code, "message", text)));
    }

    @Override
    public void done() {
        if (closed) {
            return;
        }
        send(SseEmitter.event().name("done").data("[DONE]"));
        if (!closed) {
            closed = true;
            emitter.complete();
        }
    }

    private void send(SseEmitter.SseEventBuilder event) {
        if (closed) {
            return;
        }
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException exception) {
            closed = true;
            emitter.completeWithError(exception);
        }
    }
}
