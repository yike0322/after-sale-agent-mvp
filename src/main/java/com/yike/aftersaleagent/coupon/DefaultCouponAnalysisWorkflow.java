package com.yike.aftersaleagent.coupon;

import com.yike.aftersaleagent.agent.BusinessReferenceExtractor;
import com.yike.aftersaleagent.agent.Intent;
import com.yike.aftersaleagent.ai.AiGateway;
import com.yike.aftersaleagent.chat.api.ChatOutcome;
import com.yike.aftersaleagent.order.OrderQuery;
import com.yike.aftersaleagent.order.OrderQueryTool;
import com.yike.aftersaleagent.order.OrderSummary;
import com.yike.aftersaleagent.tool.AgentExecutionContext;
import com.yike.aftersaleagent.tool.ToolRegistry;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class DefaultCouponAnalysisWorkflow implements CouponAnalysisWorkflow {
    private static final String ORDER_QUERY_STATUS = "正在查询订单…";
    private static final String COUPON_QUERY_STATUS = "正在查询优惠券…";
    private static final String RULE_EVALUATION_STATUS = "正在校验优惠券规则…";
    private static final String EXPLANATION_INSTRUCTION = """
            你是优惠券结果说明助手。只能把已给定的系统规则事实改写为简洁客服说明。
            不得新增、删除或反转 usable、reasonCode、订单金额、优惠券门槛或优惠券状态。
            不得承诺修改优惠券、创建工单、退款，也不得引用未提供的规则或用户信息。
            """;
    private static final String CANONICAL_PREFIX = "系统规则结论：";
    private static final String CUSTOMER_EXPLANATION_PREFIX = "客服说明：";
    private static final int MAX_MODEL_WORDING_LENGTH = 200;
    private static final Pattern LINE_BREAKS = Pattern.compile("[\\r\\n]+");

    private final BusinessReferenceExtractor businessReferenceExtractor;
    private final ToolRegistry toolRegistry;
    private final OrderQueryTool orderQueryTool;
    private final CouponQueryTool couponQueryTool;
    private final CouponRuleService couponRuleService;
    private final AiGateway aiGateway;

    public DefaultCouponAnalysisWorkflow(
            BusinessReferenceExtractor businessReferenceExtractor,
            ToolRegistry toolRegistry,
            OrderQueryTool orderQueryTool,
            CouponQueryTool couponQueryTool,
            CouponRuleService couponRuleService,
            AiGateway aiGateway) {
        this.businessReferenceExtractor = businessReferenceExtractor;
        this.toolRegistry = toolRegistry;
        this.orderQueryTool = orderQueryTool;
        this.couponQueryTool = couponQueryTool;
        this.couponRuleService = couponRuleService;
        this.aiGateway = aiGateway;
    }

    @Override
    public ChatOutcome execute(AgentExecutionContext context, Consumer<String> onStepStarted) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(onStepStarted, "onStepStarted");

        String orderNo = businessReferenceExtractor.requireOrderNo(context.userMessage());
        String couponCode = businessReferenceExtractor.requireCouponCode(context.userMessage());

        onStepStarted.accept(ORDER_QUERY_STATUS);
        OrderSummary order = toolRegistry.execute(
                Intent.COUPON_ANALYSIS, orderQueryTool, context, new OrderQuery(orderNo));
        onStepStarted.accept(COUPON_QUERY_STATUS);
        CouponSummary coupon = toolRegistry.execute(
                Intent.COUPON_ANALYSIS, couponQueryTool, context, new CouponQuery(couponCode));
        onStepStarted.accept(RULE_EVALUATION_STATUS);
        CouponEligibility eligibility = couponRuleService.evaluate(order, coupon);

        String canonicalReply = CANONICAL_PREFIX + eligibility.reasonText();
        String modelWording = safelyExplain(eligibility, order, coupon);
        String reply = modelWording.isEmpty()
                ? canonicalReply
                : canonicalReply + "\n" + CUSTOMER_EXPLANATION_PREFIX + modelWording;
        return new ChatOutcome(reply, null, eligibility.reasonCode(), List.of());
    }

    private String safelyExplain(CouponEligibility eligibility, OrderSummary order, CouponSummary coupon) {
        String controlledFacts = facts(eligibility, order, coupon);
        String wording;
        try {
            wording = aiGateway.explain(EXPLANATION_INSTRUCTION, controlledFacts);
        } catch (RuntimeException exception) {
            return "";
        }
        if (wording == null) {
            return "";
        }
        String normalized = LINE_BREAKS.matcher(wording.strip()).replaceAll(" ").strip();
        if (normalized.isEmpty()) {
            return "";
        }
        return normalized.length() <= MAX_MODEL_WORDING_LENGTH
                ? normalized
                : normalized.substring(0, MAX_MODEL_WORDING_LENGTH);
    }

    private String facts(CouponEligibility eligibility, OrderSummary order, CouponSummary coupon) {
        return "reasonCode=" + eligibility.reasonCode() + "\n"
                + "usable=" + eligibility.usable() + "\n"
                + "orderNo=" + order.orderNo() + "\n"
                + "orderAmount=" + displayAmount(order.amount()) + "\n"
                + "couponCode=" + coupon.couponCode() + "\n"
                + "couponThresholdAmount=" + displayAmount(coupon.thresholdAmount()) + "\n"
                + "couponStatus=" + coupon.status();
    }

    private String displayAmount(BigDecimal amount) {
        return amount.setScale(2).toPlainString();
    }
}
