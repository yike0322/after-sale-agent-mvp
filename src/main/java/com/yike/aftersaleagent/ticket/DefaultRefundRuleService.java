package com.yike.aftersaleagent.ticket;

import com.yike.aftersaleagent.chat.api.SourceCitation;
import com.yike.aftersaleagent.order.OrderSummary;
import com.yike.aftersaleagent.ticket.domain.RefundDecision;
import com.yike.aftersaleagent.ticket.domain.TicketTaskStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class DefaultRefundRuleService implements RefundRuleService {
    private final Clock clock;

    public DefaultRefundRuleService(Clock clock) {
        this.clock = clock;
    }

    @Override
    public RefundDecision evaluate(OrderSummary order, List<SourceCitation> evidence) {
        Objects.requireNonNull(order, "order");
        if (!hasUsableEvidence(evidence)) {
            return humanReview(false, "INSUFFICIENT_RULE_EVIDENCE", "规则资料不足，仅为演示资格建议，最终以人工审核为准。");
        }
        if (!"RECEIVED".equals(normalize(order.orderStatus()))) {
            return finished("ORDER_NOT_RECEIVED", "订单尚未处于已签收状态，仅为演示资格建议，最终以人工审核为准。");
        }
        if (!"NORMAL".equals(normalize(order.productType()))) {
            return finished("PRODUCT_TYPE_NOT_SUPPORTED", "该商品类型不在演示退款资格范围内，最终以人工审核为准。");
        }
        LocalDateTime receivedAt = order.receivedAt();
        LocalDateTime now = LocalDateTime.now(clock);
        if (receivedAt == null || receivedAt.isAfter(now)) {
            return finished("RECEIPT_TIME_INVALID", "签收时间无效，无法给出退款资格建议，最终以人工审核为准。");
        }
        if (receivedAt.isBefore(now.minusDays(7))) {
            return finished("RECEIPT_TIME_OUT_OF_WINDOW", "签收时间已超过演示的七天资格窗口，最终以人工审核为准。");
        }
        return humanReview(true, "ELIGIBLE_HUMAN_REVIEW", "符合演示退款资格建议，已进入人工审核，最终以人工审核为准。");
    }

    private boolean hasUsableEvidence(List<SourceCitation> evidence) {
        return evidence != null && evidence.stream().anyMatch(citation -> citation != null
                && !normalize(citation.sourceTitle()).isEmpty()
                && !normalize(citation.sourcePath()).isEmpty()
                && !normalize(citation.excerpt()).isEmpty());
    }

    private RefundDecision humanReview(boolean eligible, String reasonCode, String reasonText) {
        return new RefundDecision(eligible, true, reasonCode, reasonText, TicketTaskStatus.WAIT_HUMAN);
    }

    private RefundDecision finished(String reasonCode, String reasonText) {
        return new RefundDecision(false, false, reasonCode, reasonText, TicketTaskStatus.FINISHED);
    }

    private String normalize(String value) {
        return value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
    }
}
