package com.yike.aftersaleagent.identity;

public record DemoAccount(
        long userId,
        String account,
        String passwordHash,
        DemoRole role,
        boolean enabled,
        String displayName) {

    CurrentDemoUser toCurrentUser() {
        return new CurrentDemoUser(userId, displayName, role);
    }
}
