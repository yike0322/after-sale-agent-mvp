package com.yike.aftersaleagent.identity;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoDataInitializer implements ApplicationRunner {
    private final DemoUserMapper demoUserMapper;
    private final Clock clock;

    public DemoDataInitializer(DemoUserMapper demoUserMapper, Clock clock) {
        this.demoUserMapper = demoUserMapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (demoUserMapper.countDemoUsers() != 0) {
            return;
        }

        demoUserMapper.insertUser(10001L, "Demo User 10001");
        demoUserMapper.insertUser(10002L, "Demo User 10002");

        demoUserMapper.insertOrder(
                10001L, "O1001", "P1001", "Demo product 1001", "NORMAL",
                new BigDecimal("80.00"), "PAID", LocalDateTime.now(clock), null);
        demoUserMapper.insertCoupon(
                10001L, "C1001", "Demo coupon 1001", new BigDecimal("100.00"),
                new BigDecimal("10.00"), "AVAILABLE");

        LocalDateTime receivedAt = LocalDateTime.now(clock).minusDays(2);
        demoUserMapper.insertOrder(
                10002L, "O2001", "P2001", "Demo product 2001", "NORMAL",
                new BigDecimal("120.00"), "RECEIVED", receivedAt.minusDays(1), receivedAt);
    }
}
