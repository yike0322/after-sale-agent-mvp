package com.yike.aftersaleagent.identity.api;

import java.time.Instant;

public record LoginResponse(
        String token,
        long userId,
        String displayName,
        String role,
        Instant expiresAt) { }
