package com.yike.aftersaleagent.order;

import com.yike.aftersaleagent.common.api.ErrorCode;
import com.yike.aftersaleagent.common.exception.BusinessException;
import com.yike.aftersaleagent.tool.AgentExecutionContext;
import com.yike.aftersaleagent.tool.GovernedTool;
import com.yike.aftersaleagent.tool.ToolAuditRequest;
import com.yike.aftersaleagent.tool.ToolAuditService;
import com.yike.aftersaleagent.tool.ToolOperationExecutor;
import com.yike.aftersaleagent.tool.ToolRisk;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class OrderQueryTool implements GovernedTool<OrderQuery, OrderSummary> {
    private static final Pattern ORDER_NO_PATTERN = Pattern.compile("O\\d{4,}");

    private final OrderMapper orderMapper;
    private final ToolAuditService toolAuditService;
    private final ToolOperationExecutor toolOperationExecutor;
    private final Validator validator;

    public OrderQueryTool(
            OrderMapper orderMapper,
            ToolAuditService toolAuditService,
            ToolOperationExecutor toolOperationExecutor,
            Validator validator) {
        this.orderMapper = orderMapper;
        this.toolAuditService = toolAuditService;
        this.toolOperationExecutor = toolOperationExecutor;
        this.validator = validator;
    }

    @Override
    public String name() {
        return "orderQuery";
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.READ_ONLY;
    }

    @Override
    public OrderSummary execute(AgentExecutionContext context, OrderQuery input) {
        long startedAt = System.nanoTime();
        String requestSummary = "orderNo=invalid";
        OrderSummary summary;
        try {
            String orderNo = normalizeOrderNo(input);
            requestSummary = "orderNo=" + orderNo;
            OrderInfo order = toolOperationExecutor.execute(
                    () -> orderMapper.findOwnedByOrderNo(context.userId(), orderNo));
            if (order == null) {
                throw new BusinessException(ErrorCode.ORDER_NOT_FOUND_OR_FORBIDDEN);
            }
            summary = new OrderSummary(
                    order.getOrderNo(),
                    order.getProductName(),
                    order.getProductType(),
                    order.getOrderAmount(),
                    order.getOrderStatus(),
                    order.getReceivedAt());
        } catch (BusinessException exception) {
            throw recordFailureAndReturn(exception, context, requestSummary, elapsedMilliseconds(startedAt));
        } catch (RuntimeException exception) {
            throw recordFailureAndReturn(
                    new BusinessException(ErrorCode.TOOL_EXECUTION_FAILED),
                    context,
                    requestSummary,
                    elapsedMilliseconds(startedAt));
        }

        try {
            toolAuditService.recordSuccess(
                    name(),
                    ToolAuditRequest.success(
                            context.requestId(), requestSummary, successSummary(summary)),
                    elapsedMilliseconds(startedAt));
        } catch (RuntimeException exception) {
            throw auditFailure(exception);
        }
        return summary;
    }

    private String normalizeOrderNo(OrderQuery input) {
        if (input == null || !validator.validate(input).isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_REFERENCE_REQUIRED);
        }
        String normalized = input.orderNo().strip().toUpperCase(Locale.ROOT);
        if (!ORDER_NO_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException(ErrorCode.BUSINESS_REFERENCE_REQUIRED);
        }
        return normalized;
    }

    private BusinessException recordFailureAndReturn(
            BusinessException failure,
            AgentExecutionContext context,
            String requestSummary,
            long costTimeMs) {
        try {
            toolAuditService.recordFailure(
                    name(),
                    ToolAuditRequest.failure(context.requestId(), requestSummary, failure.getErrorCode()),
                    costTimeMs);
            return failure;
        } catch (RuntimeException auditException) {
            return auditFailure(auditException);
        }
    }

    private BusinessException auditFailure(RuntimeException exception) {
        if (exception instanceof BusinessException businessException
                && businessException.getErrorCode() == ErrorCode.TOOL_AUDIT_FAILED) {
            return businessException;
        }
        return new BusinessException(ErrorCode.TOOL_AUDIT_FAILED);
    }

    private String successSummary(OrderSummary summary) {
        return "orderNo=" + summary.orderNo()
                + ",status=" + summary.orderStatus()
                + ",amount=" + plainAmount(summary.amount());
    }

    private String plainAmount(BigDecimal amount) {
        return amount.toPlainString();
    }

    private long elapsedMilliseconds(long startedAt) {
        return Math.max(0L, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
    }
}
