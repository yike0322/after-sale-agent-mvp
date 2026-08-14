package com.yike.aftersaleagent.identity;

public record CurrentDemoUser(long id, String displayName, DemoRole role) {
    public CurrentDemoUser(long id, String displayName) {
        this(id, displayName, DemoRole.CUSTOMER);
    }
}
