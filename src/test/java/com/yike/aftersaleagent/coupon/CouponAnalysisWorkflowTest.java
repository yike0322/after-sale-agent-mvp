package com.yike.aftersaleagent.coupon;

import com.yike.aftersaleagent.agent.BusinessReferenceExtractor;
import com.yike.aftersaleagent.agent.DefaultBusinessReferenceExtractor;
import com.yike.aftersaleagent.agent.Intent;
import com.yike.aftersaleagent.ai.AiGateway;
import com.yike.aftersaleagent.chat.api.ChatOutcome;
import com.yike.aftersaleagent.common.api.ErrorCode;
import com.yike.aftersaleagent.common.exception.BusinessException;
import com.yike.aftersaleagent.order.OrderQuery;
import com.yike.aftersaleagent.order.OrderQueryTool;
import com.yike.aftersaleagent.order.OrderSummary;
import com.yike.aftersaleagent.tool.AgentExecutionContext;
import com.yike.aftersaleagent.tool.ToolRegistry;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class CouponAnalysisWorkflowTest {
    private static final AgentExecutionContext CONTEXT = new AgentExecutionContext(
            "req-coupon-1",
            10001L,
            "session-1",
            "订单 O1001 使用优惠券 C1001，原始隐私标记=DO_NOT_LOG");
    private static final OrderSummary ORDER = new OrderSummary(
            "O1001", "Demo product", "NORMAL", new BigDecimal("80.00"), "PAID", null);
    private static final CouponSummary COUPON = new CouponSummary(
            "C1001", new BigDecimal("100.00"), new BigDecimal("10.00"), "AVAILABLE");
    private static final CouponEligibility BELOW_THRESHOLD = new CouponEligibility(
            false,
            "ORDER_AMOUNT_BELOW_THRESHOLD",
            "订单金额 80.00 元，未达到优惠券使用门槛 100.00 元。");

    @Test
    void evaluatesControlledFactsInOrderAndKeepsTheJavaConclusionAuthoritative() {
        Fixture fixture = fixture();
        given(fixture.extractor.requireOrderNo(CONTEXT.userMessage())).willReturn("O1001");
        given(fixture.extractor.requireCouponCode(CONTEXT.userMessage())).willReturn("C1001");
        given(fixture.registry.execute(
                eq(Intent.COUPON_ANALYSIS), same(fixture.orderTool), same(CONTEXT), eq(new OrderQuery("O1001"))))
                .willReturn(ORDER);
        given(fixture.registry.execute(
                eq(Intent.COUPON_ANALYSIS), same(fixture.couponTool), same(CONTEXT), eq(new CouponQuery("C1001"))))
                .willReturn(COUPON);
        given(fixture.ruleService.evaluate(ORDER, COUPON)).willReturn(BELOW_THRESHOLD);
        given(fixture.aiGateway.explain(anyString(), anyString())).willReturn("已按系统规则核验。\r\n请以结论为准。");
        List<String> statuses = new ArrayList<>();

        ChatOutcome outcome = fixture.workflow.execute(CONTEXT, statuses::add);

        assertThat(statuses).containsExactly("正在查询订单…", "正在查询优惠券…", "正在校验优惠券规则…");
        assertThat(outcome.ticketId()).isNull();
        assertThat(outcome.taskStatus()).isEqualTo("ORDER_AMOUNT_BELOW_THRESHOLD");
        assertThat(outcome.citations()).isEmpty();
        assertThat(outcome.reply())
                .startsWith("系统规则结论：订单金额 80.00 元，未达到优惠券使用门槛 100.00 元。")
                .contains("客服说明：已按系统规则核验。 请以结论为准。");

        InOrder inOrder = inOrder(fixture.extractor, fixture.registry, fixture.ruleService, fixture.aiGateway);
        inOrder.verify(fixture.extractor).requireOrderNo(CONTEXT.userMessage());
        inOrder.verify(fixture.extractor).requireCouponCode(CONTEXT.userMessage());
        inOrder.verify(fixture.registry).execute(
                Intent.COUPON_ANALYSIS, fixture.orderTool, CONTEXT, new OrderQuery("O1001"));
        inOrder.verify(fixture.registry).execute(
                Intent.COUPON_ANALYSIS, fixture.couponTool, CONTEXT, new CouponQuery("C1001"));
        inOrder.verify(fixture.ruleService).evaluate(ORDER, COUPON);
        inOrder.verify(fixture.aiGateway).explain(anyString(), anyString());
        verifyNoInteractions(fixture.orderTool, fixture.couponTool);
        verify(fixture.aiGateway, never()).classifyIntent(any());

        ArgumentCaptor<String> facts = ArgumentCaptor.forClass(String.class);
        verify(fixture.aiGateway).explain(anyString(), facts.capture());
        assertThat(facts.getValue())
                .contains(
                        "reasonCode=ORDER_AMOUNT_BELOW_THRESHOLD",
                        "usable=false",
                        "orderNo=O1001",
                        "orderAmount=80.00",
                        "couponCode=C1001",
                        "couponThresholdAmount=100.00",
                        "couponStatus=AVAILABLE")
                .doesNotContain("10001", "session-1", "DO_NOT_LOG", CONTEXT.userMessage(), "Demo product");
    }

    @Test
    void propagatesMissingReferencesBeforeAnyStatusOrToolOperation() {
        Fixture fixture = fixture();
        given(fixture.extractor.requireOrderNo(CONTEXT.userMessage()))
                .willThrow(new BusinessException(ErrorCode.BUSINESS_REFERENCE_REQUIRED));
        List<String> statuses = new ArrayList<>();

        assertThatThrownBy(() -> fixture.workflow.execute(CONTEXT, statuses::add))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.BUSINESS_REFERENCE_REQUIRED);

        assertThat(statuses).isEmpty();
        verifyNoInteractions(fixture.registry, fixture.orderTool, fixture.couponTool, fixture.ruleService, fixture.aiGateway);
    }

    @Test
    void malformedCouponReferenceFailsBeforeAnyStatusOrToolOperation() {
        Fixture fixture = fixture();
        CouponAnalysisWorkflow workflow = new DefaultCouponAnalysisWorkflow(
                new DefaultBusinessReferenceExtractor(),
                fixture.registry,
                fixture.orderTool,
                fixture.couponTool,
                fixture.ruleService,
                fixture.aiGateway);
        AgentExecutionContext malformedContext = new AgentExecutionContext(
                "req-coupon-invalid",
                10001L,
                "session-invalid",
                "订单 O1001 使用优惠券 C12");
        List<String> statuses = new ArrayList<>();

        assertThatThrownBy(() -> workflow.execute(malformedContext, statuses::add))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.BUSINESS_REFERENCE_REQUIRED);

        assertThat(statuses).isEmpty();
        verifyNoInteractions(fixture.registry, fixture.orderTool, fixture.couponTool, fixture.ruleService, fixture.aiGateway);
    }

    @Test
    void stopsAfterOrderRegistryFailureWithoutQueryingCouponOrCallingTheModel() {
        Fixture fixture = fixture();
        configureReferences(fixture);
        given(fixture.registry.execute(
                eq(Intent.COUPON_ANALYSIS), same(fixture.orderTool), same(CONTEXT), eq(new OrderQuery("O1001"))))
                .willThrow(new BusinessException(ErrorCode.ORDER_NOT_FOUND_OR_FORBIDDEN));
        List<String> statuses = new ArrayList<>();

        assertThatThrownBy(() -> fixture.workflow.execute(CONTEXT, statuses::add))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_NOT_FOUND_OR_FORBIDDEN);

        assertThat(statuses).containsExactly("正在查询订单…");
        verify(fixture.registry, never()).execute(
                eq(Intent.COUPON_ANALYSIS), same(fixture.couponTool), same(CONTEXT), any(CouponQuery.class));
        verifyNoInteractions(fixture.ruleService, fixture.aiGateway, fixture.couponTool);
    }

    @Test
    void stopsAfterCouponRegistryFailureWithoutEvaluatingRulesOrCallingTheModel() {
        Fixture fixture = fixture();
        configureReferences(fixture);
        given(fixture.registry.execute(
                eq(Intent.COUPON_ANALYSIS), same(fixture.orderTool), same(CONTEXT), eq(new OrderQuery("O1001"))))
                .willReturn(ORDER);
        given(fixture.registry.execute(
                eq(Intent.COUPON_ANALYSIS), same(fixture.couponTool), same(CONTEXT), eq(new CouponQuery("C1001"))))
                .willThrow(new BusinessException(ErrorCode.COUPON_NOT_FOUND_OR_FORBIDDEN));
        List<String> statuses = new ArrayList<>();

        assertThatThrownBy(() -> fixture.workflow.execute(CONTEXT, statuses::add))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.COUPON_NOT_FOUND_OR_FORBIDDEN);

        assertThat(statuses).containsExactly("正在查询订单…", "正在查询优惠券…");
        verifyNoInteractions(fixture.ruleService, fixture.aiGateway);
    }

    @Test
    void usesTheCanonicalConclusionWhenModelWordingIsBlank() {
        Fixture fixture = successfulFixture();
        given(fixture.aiGateway.explain(anyString(), anyString())).willReturn(" \r\n ");

        ChatOutcome outcome = fixture.workflow.execute(CONTEXT);

        assertCanonicalBelowThreshold(outcome);
        verify(fixture.aiGateway).explain(anyString(), anyString());
    }

    @Test
    void usesTheCanonicalConclusionWhenModelWordingFails() {
        Fixture fixture = successfulFixture();
        given(fixture.aiGateway.explain(anyString(), anyString())).willThrow(new IllegalStateException("provider unavailable"));

        ChatOutcome outcome = fixture.workflow.execute(CONTEXT);

        assertCanonicalBelowThreshold(outcome);
        verify(fixture.aiGateway).explain(anyString(), anyString());
    }

    @Test
    void doesNotDowngradeInvalidControlledFactsAsAModelFailure() {
        Fixture fixture = fixture();
        OrderSummary invalidOrder = new OrderSummary(
                "O1001", "Demo product", "NORMAL", null, "PAID", null);
        configureReferences(fixture);
        given(fixture.registry.execute(
                eq(Intent.COUPON_ANALYSIS), same(fixture.orderTool), same(CONTEXT), eq(new OrderQuery("O1001"))))
                .willReturn(invalidOrder);
        given(fixture.registry.execute(
                eq(Intent.COUPON_ANALYSIS), same(fixture.couponTool), same(CONTEXT), eq(new CouponQuery("C1001"))))
                .willReturn(COUPON);
        given(fixture.ruleService.evaluate(invalidOrder, COUPON)).willReturn(BELOW_THRESHOLD);

        assertThatThrownBy(() -> fixture.workflow.execute(CONTEXT))
                .isInstanceOf(NullPointerException.class);

        verifyNoInteractions(fixture.aiGateway);
    }

    private Fixture successfulFixture() {
        Fixture fixture = fixture();
        configureReferences(fixture);
        given(fixture.registry.execute(
                eq(Intent.COUPON_ANALYSIS), same(fixture.orderTool), same(CONTEXT), eq(new OrderQuery("O1001"))))
                .willReturn(ORDER);
        given(fixture.registry.execute(
                eq(Intent.COUPON_ANALYSIS), same(fixture.couponTool), same(CONTEXT), eq(new CouponQuery("C1001"))))
                .willReturn(COUPON);
        given(fixture.ruleService.evaluate(ORDER, COUPON)).willReturn(BELOW_THRESHOLD);
        return fixture;
    }

    private void configureReferences(Fixture fixture) {
        given(fixture.extractor.requireOrderNo(CONTEXT.userMessage())).willReturn("O1001");
        given(fixture.extractor.requireCouponCode(CONTEXT.userMessage())).willReturn("C1001");
    }

    private void assertCanonicalBelowThreshold(ChatOutcome outcome) {
        assertThat(outcome.reply())
                .isEqualTo("系统规则结论：订单金额 80.00 元，未达到优惠券使用门槛 100.00 元。");
        assertThat(outcome.ticketId()).isNull();
        assertThat(outcome.taskStatus()).isEqualTo("ORDER_AMOUNT_BELOW_THRESHOLD");
        assertThat(outcome.citations()).isEmpty();
    }

    private Fixture fixture() {
        BusinessReferenceExtractor extractor = mock(BusinessReferenceExtractor.class);
        ToolRegistry registry = mock(ToolRegistry.class);
        OrderQueryTool orderTool = mock(OrderQueryTool.class);
        CouponQueryTool couponTool = mock(CouponQueryTool.class);
        CouponRuleService ruleService = mock(CouponRuleService.class);
        AiGateway aiGateway = mock(AiGateway.class);
        return new Fixture(
                extractor,
                registry,
                orderTool,
                couponTool,
                ruleService,
                aiGateway,
                new DefaultCouponAnalysisWorkflow(
                        extractor, registry, orderTool, couponTool, ruleService, aiGateway));
    }

    private record Fixture(
            BusinessReferenceExtractor extractor,
            ToolRegistry registry,
            OrderQueryTool orderTool,
            CouponQueryTool couponTool,
            CouponRuleService ruleService,
            AiGateway aiGateway,
            CouponAnalysisWorkflow workflow) { }
}
