package com.yike.aftersaleagent.ai;

import com.yike.aftersaleagent.agent.Intent;
import java.util.List;
import java.util.Locale;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!dashscope & (default | mock | test)")
public class MockAiGateway implements AiGateway {
    private static final List<String> COUPON_KEYWORDS = List.of("优惠券", "满减", "C1001");
    private static final List<String> FAQ_KEYWORDS = List.of("规则", "七天", "售后");
    private static final List<String> REFUND_KEYWORDS = List.of("退款", "退货", "商品坏");

    @Override
    public Intent classifyIntent(String message) {
        String normalized = message == null ? "" : message.toUpperCase(Locale.ROOT);
        // This order is intentional: rule questions mentioning returns remain FAQ questions.
        if (containsAny(normalized, COUPON_KEYWORDS)) {
            return Intent.COUPON_ANALYSIS;
        }
        if (containsAny(normalized, FAQ_KEYWORDS)) {
            return Intent.FAQ_QUERY;
        }
        if (containsAny(normalized, REFUND_KEYWORDS)) {
            return Intent.REFUND_ELIGIBILITY;
        }
        return Intent.UNSUPPORTED;
    }

    @Override
    public String explain(String systemInstruction, String facts) {
        return "Mock explanation: " + facts;
    }

    private boolean containsAny(String message, List<String> keywords) {
        return keywords.stream().anyMatch(message::contains);
    }
}
