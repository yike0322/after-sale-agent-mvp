package com.yike.aftersaleagent.coupon;

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
public class CouponQueryTool implements GovernedTool<CouponQuery, CouponSummary> {
    private static final Pattern COUPON_CODE_PATTERN = Pattern.compile("C\\d{4,}");

    private final CouponMapper couponMapper;
    private final ToolAuditService toolAuditService;
    private final ToolOperationExecutor toolOperationExecutor;
    private final Validator validator;

    public CouponQueryTool(
            CouponMapper couponMapper,
            ToolAuditService toolAuditService,
            ToolOperationExecutor toolOperationExecutor,
            Validator validator) {
        this.couponMapper = couponMapper;
        this.toolAuditService = toolAuditService;
        this.toolOperationExecutor = toolOperationExecutor;
        this.validator = validator;
    }

    @Override
    public String name() {
        return "couponQuery";
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.READ_ONLY;
    }

    @Override
    public CouponSummary execute(AgentExecutionContext context, CouponQuery input) {
        long startedAt = System.nanoTime();
        String requestSummary = "couponCode=invalid";
        CouponSummary summary;
        try {
            String couponCode = normalizeCouponCode(input);
            requestSummary = "couponCode=" + couponCode;
            CouponInfo coupon = toolOperationExecutor.execute(
                    () -> couponMapper.findOwnedByCouponCode(context.userId(), couponCode));
            if (coupon == null) {
                throw new BusinessException(ErrorCode.COUPON_NOT_FOUND_OR_FORBIDDEN);
            }
            summary = new CouponSummary(
                    coupon.getCouponCode(),
                    coupon.getThresholdAmount(),
                    coupon.getDiscountAmount(),
                    coupon.getStatus());
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

    private String normalizeCouponCode(CouponQuery input) {
        if (input == null || !validator.validate(input).isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_REFERENCE_REQUIRED);
        }
        String normalized = input.couponCode().strip().toUpperCase(Locale.ROOT);
        if (!COUPON_CODE_PATTERN.matcher(normalized).matches()) {
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

    private String successSummary(CouponSummary summary) {
        return "couponCode=" + summary.couponCode()
                + ",status=" + summary.status()
                + ",thresholdAmount=" + plainAmount(summary.thresholdAmount())
                + ",discountAmount=" + plainAmount(summary.discountAmount());
    }

    private String plainAmount(BigDecimal amount) {
        return amount.toPlainString();
    }

    private long elapsedMilliseconds(long startedAt) {
        return Math.max(0L, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
    }
}
