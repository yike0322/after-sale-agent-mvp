package com.yike.aftersaleagent.agent;

import com.yike.aftersaleagent.ai.MockAiGateway;
import com.yike.aftersaleagent.tool.AgentExecutionContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IntentRouterTest {
    private final IntentRouter intentRouter = new IntentRouter(new MockAiGateway());

    @Test
    void routesTheBoundedSupportedIntentsWithDeterministicPriority() {
        assertThat(intentRouter.route(context("七天无理由退货规则是什么？")))
                .isEqualTo(Intent.FAQ_QUERY);
        assertThat(intentRouter.route(context("优惠券 C1001 为什么不能用？")))
                .isEqualTo(Intent.COUPON_ANALYSIS);
        assertThat(intentRouter.route(context("商品坏了，我要退款")))
                .isEqualTo(Intent.REFUND_ELIGIBILITY);
    }

    @Test
    void matchesEnglishMarkersCaseInsensitively() {
        assertThat(intentRouter.route(context("please inspect coupon c1001")))
                .isEqualTo(Intent.COUPON_ANALYSIS);
    }

    @Test
    void routesMessagesOutsideTheAllowListAsUnsupported() {
        assertThat(intentRouter.route(context("请帮我查物流")))
                .isEqualTo(Intent.UNSUPPORTED);
    }

    private AgentExecutionContext context(String message) {
        return new AgentExecutionContext("request-1", 10001L, "session-1", message);
    }
}
