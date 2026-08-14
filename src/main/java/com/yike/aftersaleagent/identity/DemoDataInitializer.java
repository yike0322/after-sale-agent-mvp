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
            LocalDateTime now = LocalDateTime.now(clock);
            demoUserMapper.insertUser(10001L, "Demo User 10001");
            demoUserMapper.insertUser(10002L, "Demo User 10002");
            demoUserMapper.insertUser(10003L, "Demo Supervisor 10003");

            demoUserMapper.insertOrder(
                    10001L, "O1001", "P1001", "Demo product 1001", "NORMAL",
                    new BigDecimal("80.00"), "PAID", now.minusDays(3), null);
            demoUserMapper.insertCouponWithValidity(
                    10001L, "C1001", "Demo coupon 1001", new BigDecimal("100.00"),
                    new BigDecimal("10.00"), "AVAILABLE", now.minusDays(7), now.plusDays(14));
            demoUserMapper.insertOrder(
                    10001L, "O1002", "P1002", "Demo product 1002", "NORMAL",
                    new BigDecimal("158.00"), "PAID", now.minusDays(20), null);
            demoUserMapper.insertCouponWithValidity(
                    10001L, "C1002", "Demo coupon 1002", new BigDecimal("99.00"),
                    new BigDecimal("15.00"), "EXPIRED", now.minusDays(40), now.minusDays(1));

            LocalDateTime receivedAt = now.minusDays(2);
            demoUserMapper.insertOrder(
                    10002L, "O2001", "P2001", "Demo product 2001", "NORMAL",
                    new BigDecimal("120.00"), "RECEIVED", receivedAt.minusDays(1), receivedAt);
            demoUserMapper.insertOrder(
                    10002L, "O2002", "P2002", "Demo product 2002", "NORMAL",
                    new BigDecimal("66.00"), "PAID", now.minusHours(6), null);
            demoUserMapper.insertOrder(
                    10002L, "O2003", "P2003", "Demo restricted product 2003", "RESTRICTED",
                    new BigDecimal("299.00"), "RECEIVED", now.minusDays(8), now.minusDays(3));
            demoUserMapper.insertOrder(
                    10002L, "O2004", "P2004", "Demo product 2004", "NORMAL",
                    new BigDecimal("88.00"), "LOGISTICS_EXCEPTION", now.minusDays(5), null);
            insertHistoricalTickets(now);
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

    private void insertHistoricalTickets(LocalDateTime now) {
        insertHistoricalTicket(9101L, 10001L, "COUPON_ANALYSIS", "LOW", "FINISHED",
                "Coupon threshold explanation", "Coupon C1001 on order O1001", "Order amount did not meet coupon threshold.",
                "coupon:O1001:C1001", 3, "COUPON_THRESHOLD_NOT_MET", now.minusDays(1));
        insertHistoricalTicket(9102L, 10001L, "COUPON_ANALYSIS", "LOW", "FINISHED",
                "Expired coupon explanation", "Coupon C1002 on order O1002", "Coupon had expired before evaluation.",
                "coupon:O1002:C1002", 3, "COUPON_EXPIRED", now.minusHours(20));
        insertHistoricalTicket(9103L, 10002L, "REFUND_REVIEW", "NORMAL", "WAIT_HUMAN",
                "Received order refund review", "Refund review for order O2001", "Eligible conditions require a human refund review.",
                "refund:O2001", 5, "ELIGIBLE_HUMAN_REVIEW", now.minusHours(8));
        insertHistoricalTicket(9104L, 10002L, "LOGISTICS_EXCEPTION", "HIGH", "WAIT_HUMAN",
                "Logistics exception escalation", "Logistics issue for order O2004", "Shipment exception was escalated for manual follow-up.",
                "logistics:O2004", 2, "LOGISTICS_ESCALATED", now.minusHours(3));
    }

    private void insertHistoricalTicket(
            long ticketId,
            long userId,
            String ticketType,
            String priority,
            String status,
            String title,
            String description,
            String resultSummary,
            String idempotencyKey,
            int totalSteps,
            String outcome,
            LocalDateTime occurredAt) {
        demoUserMapper.insertHistoricalTicket(
                ticketId, userId, ticketType, priority, status, title, description, resultSummary);
        demoUserMapper.insertHistoricalTicketTask(ticketId, idempotencyKey, status, totalSteps, totalSteps);
        demoUserMapper.insertHistoricalStep(ticketId, 1, "ROUTE", "SUCCESS", "reference=validated",
                "intent=" + ticketType, occurredAt);
        demoUserMapper.insertHistoricalStep(ticketId, totalSteps, "DECISION", "SUCCESS",
                "policy=demo-after-sale-v1", "outcome=" + outcome, occurredAt.plusMinutes(1));
        demoUserMapper.insertHistoricalToolCall(ticketId, "orderQuery", "orderReference=validated",
                "result=found", true, 8L, "seed-" + ticketId);
    }
}
