package com.yike.aftersaleagent.ticket;

import com.yike.aftersaleagent.chat.api.SourceCitation;
import com.yike.aftersaleagent.order.OrderSummary;
import com.yike.aftersaleagent.ticket.domain.RefundDecision;
import com.yike.aftersaleagent.ticket.domain.TicketTaskStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RefundRuleServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-13T00:00:00Z"), ZoneOffset.UTC);
    private final RefundRuleService ruleService = new DefaultRefundRuleService(CLOCK);

    @Test
    void eligibleNormalReceivedOrderWithEvidenceStillRequiresHumanReview() {
        RefundDecision decision = ruleService.evaluate(
                order("RECEIVED", "NORMAL", LocalDateTime.of(2026, 8, 11, 0, 0)), evidence());

        assertThat(decision).isEqualTo(new RefundDecision(
                true, true, "ELIGIBLE_HUMAN_REVIEW", decision.reasonText(), TicketTaskStatus.WAIT_HUMAN));
        assertThat(decision.reasonText()).contains("人工审核").doesNotContain("已退款", "保证退款");
    }

    @Test
    void missingEvidenceIsAnUncertainHumanReviewInsteadOfAnAutomaticRejection() {
        RefundDecision decision = ruleService.evaluate(
                order("RECEIVED", "NORMAL", LocalDateTime.of(2026, 8, 11, 0, 0)), List.of());

        assertThat(decision.eligible()).isFalse();
        assertThat(decision.requiresHumanReview()).isTrue();
        assertThat(decision.reasonCode()).isEqualTo("INSUFFICIENT_RULE_EVIDENCE");
        assertThat(decision.ticketStatus()).isEqualTo(TicketTaskStatus.WAIT_HUMAN);
    }

    @Test
    void appliesReceivedNormalAndSevenDayBoundariesDeterministically() {
        assertThat(ruleService.evaluate(order("DELIVERED", "NORMAL", LocalDateTime.of(2026, 8, 11, 0, 0)), evidence()).reasonCode())
                .isEqualTo("ORDER_NOT_RECEIVED");
        assertThat(ruleService.evaluate(order("RECEIVED", "DIGITAL", LocalDateTime.of(2026, 8, 11, 0, 0)), evidence()).reasonCode())
                .isEqualTo("PRODUCT_TYPE_NOT_SUPPORTED");
        assertThat(ruleService.evaluate(order("RECEIVED", "NORMAL", LocalDateTime.of(2026, 8, 6, 0, 0)), evidence()).reasonCode())
                .isEqualTo("ELIGIBLE_HUMAN_REVIEW");
        assertThat(ruleService.evaluate(order("RECEIVED", "NORMAL", LocalDateTime.of(2026, 8, 5, 23, 59, 59)), evidence()).reasonCode())
                .isEqualTo("RECEIPT_TIME_OUT_OF_WINDOW");
    }

    @Test
    void rejectsFutureOrMissingReceiptTimeWithoutHumanReview() {
        assertThat(ruleService.evaluate(order("RECEIVED", "NORMAL", null), evidence()).reasonCode())
                .isEqualTo("RECEIPT_TIME_INVALID");
        assertThat(ruleService.evaluate(order("RECEIVED", "NORMAL", LocalDateTime.of(2026, 8, 13, 0, 0, 1)), evidence()).reasonCode())
                .isEqualTo("RECEIPT_TIME_INVALID");
    }

    private OrderSummary order(String status, String productType, LocalDateTime receivedAt) {
        return new OrderSummary("O2001", "Demo normal product", productType,
                new BigDecimal("120.00"), status, receivedAt);
    }

    private List<SourceCitation> evidence() {
        return List.of(new SourceCitation("退款资格说明", "knowledge/refund-rule.md", "签收后 7 天内可提出资格建议。"));
    }
}
