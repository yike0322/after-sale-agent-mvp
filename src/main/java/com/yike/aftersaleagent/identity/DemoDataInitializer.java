package com.yike.aftersaleagent.identity;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoDataInitializer implements ApplicationRunner {
    private final DemoUserMapper demoUserMapper;
    private final DemoAccountMapper demoAccountMapper;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public DemoDataInitializer(
            DemoUserMapper demoUserMapper,
            DemoAccountMapper demoAccountMapper,
            PasswordEncoder passwordEncoder,
            Clock clock) {
        this.demoUserMapper = demoUserMapper;
        this.demoAccountMapper = demoAccountMapper;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean initializedDemoUsers = demoUserMapper.countDemoUsers() == 0;
        if (initializedDemoUsers) {
            demoUserMapper.insertUser(10001L, "Demo User 10001");
            demoUserMapper.insertUser(10002L, "Demo User 10002");
            demoUserMapper.insertUser(10003L, "Demo Supervisor 10003");

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
        if (initializedDemoUsers && demoAccountMapper.countAccounts() == 0) {
            insertAccount(10001L, "buyer_li", "Buyer#2026", DemoRole.CUSTOMER);
            insertAccount(10002L, "buyer_wang", "Buyer#2026", DemoRole.CUSTOMER);
            insertAccount(10003L, "supervisor_chen", "Supervisor#2026", DemoRole.SUPERVISOR);
        }
    }

    private void insertAccount(long userId, String account, String password, DemoRole role) {
        demoAccountMapper.insert(userId, account, passwordEncoder.encode(password), role.name(), true);
    }
}
