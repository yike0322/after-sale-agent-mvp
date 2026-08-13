package com.yike.aftersaleagent.chat;

import com.yike.aftersaleagent.chat.api.ChatOutcome;

public interface SseEventPublisher {
    void status(String text);
    void ticket(long ticketId, String taskStatus);
    void message(ChatOutcome outcome);
    void error(String code, String text);
    void done();
}
