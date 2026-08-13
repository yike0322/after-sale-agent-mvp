package com.yike.aftersaleagent.tool;

import com.yike.aftersaleagent.agent.Intent;
import com.yike.aftersaleagent.common.api.ErrorCode;
import com.yike.aftersaleagent.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToolRegistryTest {

    @Test
    void enforcesTheFixedIntentToToolPolicyBeforeExecutingARegisteredTool() {
        CountingTool orderTool = new CountingTool("orderQuery");
        CountingTool couponTool = new CountingTool("couponQuery");
        ToolRegistry registry = new ToolRegistry(List.of(orderTool, couponTool));

        assertAllowed(registry, Intent.COUPON_ANALYSIS, "orderQuery");
        assertAllowed(registry, Intent.COUPON_ANALYSIS, "couponQuery");
        assertAllowed(registry, Intent.REFUND_ELIGIBILITY, "orderQuery");
        assertAllowed(registry, Intent.REFUND_ELIGIBILITY, "afterSaleRuleQuery");
        assertAllowed(registry, Intent.REFUND_ELIGIBILITY, "ticketCreate");

        assertDenied(registry, Intent.FAQ_QUERY, "orderQuery");
        assertDenied(registry, Intent.FAQ_QUERY, "couponQuery");
        assertDenied(registry, Intent.FAQ_QUERY, "afterSaleRuleQuery");
        assertDenied(registry, Intent.FAQ_QUERY, "ticketCreate");
        assertDenied(registry, Intent.COUPON_ANALYSIS, "afterSaleRuleQuery");
        assertDenied(registry, Intent.COUPON_ANALYSIS, "ticketCreate");
        assertDenied(registry, Intent.REFUND_ELIGIBILITY, "couponQuery");
        assertDenied(registry, Intent.UNSUPPORTED, "orderQuery");
        assertDenied(registry, Intent.UNSUPPORTED, "couponQuery");
        assertDenied(registry, Intent.UNSUPPORTED, "afterSaleRuleQuery");
        assertDenied(registry, Intent.UNSUPPORTED, "ticketCreate");
        assertDenied(registry, Intent.COUPON_ANALYSIS, "unknownTool");

        assertThat(registry.execute(Intent.COUPON_ANALYSIS, orderTool, context(), "input"))
                .isEqualTo("orderQuery:input");
        assertThat(orderTool.executions).isEqualTo(1);

        assertThatThrownBy(() -> registry.execute(Intent.FAQ_QUERY, orderTool, context(), "input"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode().getCode())
                .isEqualTo(ErrorCode.TOOL_NOT_ALLOWED.getCode());
        assertThat(orderTool.executions).isEqualTo(1);

        CountingTool unregisteredReservedTool = new CountingTool("afterSaleRuleQuery");
        assertThatThrownBy(() -> registry.execute(
                Intent.REFUND_ELIGIBILITY, unregisteredReservedTool, context(), "input"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode().getCode())
                .isEqualTo(ErrorCode.TOOL_NOT_ALLOWED.getCode());
        assertThat(unregisteredReservedTool.executions).isZero();
    }

    @Test
    void rejectsDuplicateToolNamesDuringRegistration() {
        assertThatThrownBy(() -> new ToolRegistry(List.of(
                new CountingTool("orderQuery"), new CountingTool("orderQuery"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private void assertAllowed(ToolRegistry registry, Intent intent, String toolName) {
        assertThatNoException().isThrownBy(() -> registry.requireAllowed(intent, toolName));
    }

    private void assertDenied(ToolRegistry registry, Intent intent, String toolName) {
        assertThatThrownBy(() -> registry.requireAllowed(intent, toolName))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode().getCode())
                .isEqualTo(ErrorCode.TOOL_NOT_ALLOWED.getCode());
    }

    private AgentExecutionContext context() {
        return new AgentExecutionContext("request-6", 10001L, "session-6", "raw user message must not be audited");
    }

    private static final class CountingTool implements GovernedTool<String, String> {
        private final String name;
        private int executions;

        private CountingTool(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public ToolRisk risk() {
            return ToolRisk.READ_ONLY;
        }

        @Override
        public String execute(AgentExecutionContext context, String input) {
            executions++;
            return name + ":" + input;
        }
    }
}
