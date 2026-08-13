package com.yike.aftersaleagent.chat.domain;

import com.yike.aftersaleagent.agent.Intent;
import java.time.LocalDateTime;

public class ChatMessage {
    private Long id;
    private String sessionId;
    private long userId;
    private String role;
    private String content;
    private Intent intent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ChatMessage() { }

    public ChatMessage(
            String sessionId,
            long userId,
            String role,
            String content,
            Intent intent,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.role = role;
        this.content = content;
        this.intent = intent;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Intent getIntent() {
        return intent;
    }

    public void setIntent(Intent intent) {
        this.intent = intent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
