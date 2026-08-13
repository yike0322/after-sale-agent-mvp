package com.yike.aftersaleagent.chat.domain;

import java.time.LocalDateTime;

public record UserSession(
        String id,
        long userId,
        String title,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) { }
