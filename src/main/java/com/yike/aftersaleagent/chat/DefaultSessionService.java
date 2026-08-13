package com.yike.aftersaleagent.chat;

import com.yike.aftersaleagent.agent.Intent;
import com.yike.aftersaleagent.chat.domain.ChatMessage;
import com.yike.aftersaleagent.chat.domain.UserSession;
import com.yike.aftersaleagent.common.api.ErrorCode;
import com.yike.aftersaleagent.common.exception.BusinessException;
import com.yike.aftersaleagent.identity.CurrentDemoUser;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DefaultSessionService implements SessionService {
    private static final String DEFAULT_TITLE = "New session";
    private static final String ACTIVE_STATUS = "ACTIVE";

    private final SessionMapper sessionMapper;
    private final Clock clock;

    DefaultSessionService(SessionMapper sessionMapper, Clock clock) {
        this.sessionMapper = sessionMapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public String createSession(CurrentDemoUser user) {
        String sessionId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now(clock);
        sessionMapper.insertSession(
                new UserSession(sessionId, user.id(), DEFAULT_TITLE, ACTIVE_STATUS, now, now));
        return sessionId;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessage> listMessages(CurrentDemoUser user, String sessionId) {
        requireOwnedSession(user, sessionId);
        return sessionMapper.findMessages(sessionId, user.id());
    }

    @Override
    @Transactional
    public void appendUserMessage(CurrentDemoUser user, String sessionId, String content) {
        appendMessage(user, sessionId, content, "USER", null);
    }

    @Override
    @Transactional
    public void appendAssistantMessage(
            CurrentDemoUser user, String sessionId, String content, Intent intent) {
        appendMessage(user, sessionId, content, "ASSISTANT", intent);
    }

    private void appendMessage(
            CurrentDemoUser user, String sessionId, String content, String role, Intent intent) {
        requireOwnedSession(user, sessionId);
        LocalDateTime now = LocalDateTime.now(clock);
        sessionMapper.insertMessage(
                new ChatMessage(sessionId, user.id(), role, content, intent, now, now));
    }

    private void requireOwnedSession(CurrentDemoUser user, String sessionId) {
        if (sessionMapper.countOwnedSession(sessionId, user.id()) == 0) {
            throw new BusinessException(ErrorCode.SESSION_NOT_FOUND);
        }
    }
}
