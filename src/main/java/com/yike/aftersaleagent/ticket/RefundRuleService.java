package com.yike.aftersaleagent.ticket;

import com.yike.aftersaleagent.chat.api.SourceCitation;
import com.yike.aftersaleagent.order.OrderSummary;
import com.yike.aftersaleagent.ticket.domain.RefundDecision;
import java.util.List;

public interface RefundRuleService {
    RefundDecision evaluate(OrderSummary order, List<SourceCitation> evidence);
}
