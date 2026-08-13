package com.yike.aftersaleagent.tool;

import com.yike.aftersaleagent.common.api.ErrorCode;
import com.yike.aftersaleagent.common.exception.BusinessException;
import com.yike.aftersaleagent.coupon.CouponInfo;
import com.yike.aftersaleagent.coupon.CouponMapper;
import com.yike.aftersaleagent.coupon.CouponQuery;
import com.yike.aftersaleagent.coupon.CouponQueryTool;
import com.yike.aftersaleagent.coupon.CouponSummary;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class CouponQueryToolTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void returnsAnOwnedCouponWithOnlyDisplayFieldsAndWritesOneSuccessAudit() {
        CouponMapper mapper = mock(CouponMapper.class);
        RecordingAuditService audit = new RecordingAuditService();
        when(mapper.findOwnedByCouponCode(10001L, "C1001")).thenReturn(couponInfo());

        CouponSummary summary = tool(mapper, audit, new DirectExecutor())
                .execute(context(10001L), new CouponQuery(" c1001 "));

        assertThat(summary).isEqualTo(new CouponSummary(
                "C1001", new BigDecimal("100.00"), new BigDecimal("10.00"), "AVAILABLE"));
        verify(mapper).findOwnedByCouponCode(10001L, "C1001");
        verifyNoMoreInteractions(mapper);
        assertThat(audit.records).singleElement().satisfies(record -> {
            assertThat(record.success).isTrue();
            assertThat(record.audit.requestSummary()).isEqualTo("couponCode=C1001");
            assertThat(record.audit.responseSummary()).contains(
                    "couponCode=C1001", "status=AVAILABLE", "thresholdAmount=100.00", "discountAmount=10.00");
            assertThat(record.audit.requestSummary()).doesNotContain(context(10001L).userMessage());
            assertThat(record.audit.errorCode()).isNull();
            assertThat(record.costTimeMs).isGreaterThanOrEqualTo(0L);
        });
    }

    @Test
    void mergesForeignAndMissingCouponsIntoOneNotFoundErrorWithoutLeakingExistence() {
        CouponMapper mapper = mock(CouponMapper.class);
        RecordingAuditService audit = new RecordingAuditService();
        when(mapper.findOwnedByCouponCode(10002L, "C1001")).thenReturn(null);

        assertBusinessCode(() -> tool(mapper, audit, new DirectExecutor())
                        .execute(context(10002L), new CouponQuery("C1001")),
                ErrorCode.COUPON_NOT_FOUND_OR_FORBIDDEN);

        verify(mapper).findOwnedByCouponCode(10002L, "C1001");
        verifyNoMoreInteractions(mapper);
        assertFailureAudit(audit, ErrorCode.COUPON_NOT_FOUND_OR_FORBIDDEN);
    }

    @Test
    void rejectsBlankOrMalformedCouponNumbersWithoutCallingTheMapper() {
        CouponMapper mapper = mock(CouponMapper.class);
        RecordingAuditService audit = new RecordingAuditService();
        CouponQueryTool tool = tool(mapper, audit, new DirectExecutor());

        assertBusinessCode(() -> tool.execute(context(10001L), new CouponQuery(" ")),
                ErrorCode.BUSINESS_REFERENCE_REQUIRED);
        assertBusinessCode(() -> tool.execute(context(10001L), new CouponQuery("O1001")),
                ErrorCode.BUSINESS_REFERENCE_REQUIRED);

        verifyNoInteractions(mapper);
        assertThat(audit.records).hasSize(2);
        assertThat(audit.records).allSatisfy(record -> {
            assertThat(record.success).isFalse();
            assertThat(record.audit.requestSummary()).isEqualTo("couponCode=invalid");
            assertThat(record.audit.errorCode()).isEqualTo(ErrorCode.BUSINESS_REFERENCE_REQUIRED);
        });
    }

    @Test
    void doesNotRetryTimeoutOrUnexpectedMapperFailuresAndAuditsEachOnlyOnce() {
        CouponMapper timeoutMapper = mock(CouponMapper.class);
        RecordingAuditService timeoutAudit = new RecordingAuditService();
        when(timeoutMapper.findOwnedByCouponCode(10001L, "C1001")).thenReturn(couponInfo());

        assertBusinessCode(() -> tool(timeoutMapper, timeoutAudit, new TimeoutAfterOneAttemptExecutor())
                        .execute(context(10001L), new CouponQuery("C1001")),
                ErrorCode.TOOL_TIMEOUT);
        verify(timeoutMapper).findOwnedByCouponCode(10001L, "C1001");
        verifyNoMoreInteractions(timeoutMapper);
        assertFailureAudit(timeoutAudit, ErrorCode.TOOL_TIMEOUT);

        CouponMapper failingMapper = mock(CouponMapper.class);
        RecordingAuditService failingAudit = new RecordingAuditService();
        when(failingMapper.findOwnedByCouponCode(10001L, "C1001"))
                .thenThrow(new IllegalStateException("SELECT raw failure detail"));

        assertBusinessCode(() -> tool(failingMapper, failingAudit, new DirectExecutor())
                        .execute(context(10001L), new CouponQuery("C1001")),
                ErrorCode.TOOL_EXECUTION_FAILED);
        verify(failingMapper).findOwnedByCouponCode(10001L, "C1001");
        verifyNoMoreInteractions(failingMapper);
        assertFailureAudit(failingAudit, ErrorCode.TOOL_EXECUTION_FAILED);
        assertThat(failingAudit.records.getFirst().audit.errorCode().getCode()).doesNotContain("SELECT", "raw failure detail");
    }

    private CouponQueryTool tool(CouponMapper mapper, ToolAuditService audit, ToolOperationExecutor executor) {
        return new CouponQueryTool(mapper, audit, executor, validator);
    }

    private AgentExecutionContext context(long userId) {
        return new AgentExecutionContext("request-6", userId, "session-6", "raw user message");
    }

    private CouponInfo couponInfo() {
        CouponInfo coupon = new CouponInfo();
        coupon.setCouponCode("C1001");
        coupon.setThresholdAmount(new BigDecimal("100.00"));
        coupon.setDiscountAmount(new BigDecimal("10.00"));
        coupon.setStatus("AVAILABLE");
        return coupon;
    }

    private void assertBusinessCode(ThrowingCallable action, ErrorCode errorCode) {
        assertThatThrownBy(action::call)
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode().getCode())
                .isEqualTo(errorCode.getCode());
    }

    private void assertFailureAudit(RecordingAuditService audit, ErrorCode errorCode) {
        assertThat(audit.records).singleElement().satisfies(record -> {
            assertThat(record.success).isFalse();
            assertThat(record.audit.responseSummary()).isNull();
            assertThat(record.audit.errorCode()).isEqualTo(errorCode);
            assertThat(record.costTimeMs).isGreaterThanOrEqualTo(0L);
        });
    }

    @FunctionalInterface
    private interface ThrowingCallable {
        void call();
    }

    private static final class DirectExecutor implements ToolOperationExecutor {
        @Override
        public <T> T execute(Callable<T> operation) {
            try {
                return operation.call();
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new AssertionError(exception);
            }
        }
    }

    private static final class TimeoutAfterOneAttemptExecutor implements ToolOperationExecutor {
        @Override
        public <T> T execute(Callable<T> operation) {
            try {
                operation.call();
            } catch (Exception exception) {
                throw new AssertionError(exception);
            }
            throw new BusinessException(ErrorCode.TOOL_TIMEOUT);
        }
    }

    private static final class RecordingAuditService implements ToolAuditService {
        private final List<AuditRecord> records = new ArrayList<>();

        @Override
        public void recordSuccess(String toolName, ToolAuditRequest audit, long costTimeMs) {
            records.add(new AuditRecord(toolName, audit, true, costTimeMs));
        }

        @Override
        public void recordFailure(String toolName, ToolAuditRequest audit, long costTimeMs) {
            records.add(new AuditRecord(toolName, audit, false, costTimeMs));
        }
    }

    private record AuditRecord(String toolName, ToolAuditRequest audit, boolean success, long costTimeMs) { }
}
