package com.yike.aftersaleagent.tool;

import com.yike.aftersaleagent.common.api.ErrorCode;
import com.yike.aftersaleagent.common.exception.BusinessException;
import com.yike.aftersaleagent.order.OrderInfo;
import com.yike.aftersaleagent.order.OrderMapper;
import com.yike.aftersaleagent.order.OrderQuery;
import com.yike.aftersaleagent.order.OrderQueryTool;
import com.yike.aftersaleagent.order.OrderSummary;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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

class OrderQueryToolTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void returnsAnOwnedOrderAndWritesOneBoundedSuccessAuditRecord() {
        OrderMapper mapper = mock(OrderMapper.class);
        RecordingAuditService audit = new RecordingAuditService();
        when(mapper.findOwnedByOrderNo(10001L, "O1001")).thenReturn(orderInfo());

        OrderSummary summary = tool(mapper, audit, new DirectExecutor())
                .execute(context(10001L), new OrderQuery(" o1001 "));

        assertThat(summary).isEqualTo(new OrderSummary(
                "O1001", "Demo product 1001", "NORMAL", new BigDecimal("80.00"),
                "PAID", LocalDateTime.of(2026, 8, 10, 9, 0)));
        verify(mapper).findOwnedByOrderNo(10001L, "O1001");
        verifyNoMoreInteractions(mapper);
        assertThat(audit.records).singleElement().satisfies(record -> {
            assertThat(record.success).isTrue();
            assertThat(record.audit.requestId()).isEqualTo("request-6");
            assertThat(record.audit.requestSummary()).isEqualTo("orderNo=O1001");
            assertThat(record.audit.responseSummary()).contains("orderNo=O1001", "status=PAID", "amount=80.00");
            assertThat(record.audit.requestSummary()).doesNotContain(context(10001L).userMessage());
            assertThat(record.audit.responseSummary()).doesNotContain(context(10001L).userMessage());
            assertThat(record.audit.errorCode()).isNull();
            assertThat(record.costTimeMs).isGreaterThanOrEqualTo(0L);
        });
    }

    @Test
    void mergesForeignAndMissingOrdersIntoTheSameNotFoundErrorAndAuditsOnce() {
        OrderMapper mapper = mock(OrderMapper.class);
        RecordingAuditService audit = new RecordingAuditService();
        when(mapper.findOwnedByOrderNo(10002L, "O1001")).thenReturn(null);

        assertBusinessCode(
                () -> tool(mapper, audit, new DirectExecutor())
                        .execute(context(10002L), new OrderQuery("O1001")),
                ErrorCode.ORDER_NOT_FOUND_OR_FORBIDDEN);

        verify(mapper).findOwnedByOrderNo(10002L, "O1001");
        verifyNoMoreInteractions(mapper);
        assertFailureAudit(audit, ErrorCode.ORDER_NOT_FOUND_OR_FORBIDDEN);
    }

    @Test
    void rejectsBlankOrMalformedOrderNumbersWithoutCallingTheMapper() {
        OrderMapper mapper = mock(OrderMapper.class);
        RecordingAuditService audit = new RecordingAuditService();
        OrderQueryTool tool = tool(mapper, audit, new DirectExecutor());

        assertBusinessCode(() -> tool.execute(context(10001L), new OrderQuery(" ")),
                ErrorCode.BUSINESS_REFERENCE_REQUIRED);
        assertBusinessCode(() -> tool.execute(context(10001L), new OrderQuery("X1001")),
                ErrorCode.BUSINESS_REFERENCE_REQUIRED);

        verifyNoInteractions(mapper);
        assertThat(audit.records).hasSize(2);
        assertThat(audit.records).allSatisfy(record -> {
            assertThat(record.success).isFalse();
            assertThat(record.audit.requestSummary()).isEqualTo("orderNo=invalid");
            assertThat(record.audit.errorCode()).isEqualTo(ErrorCode.BUSINESS_REFERENCE_REQUIRED);
        });
    }

    @Test
    void timesOutAfterOneMapperAttemptAndWritesOneFailureAuditRecord() {
        OrderMapper mapper = mock(OrderMapper.class);
        RecordingAuditService audit = new RecordingAuditService();
        when(mapper.findOwnedByOrderNo(10001L, "O1001")).thenReturn(orderInfo());

        assertBusinessCode(() -> tool(mapper, audit, new TimeoutAfterOneAttemptExecutor())
                        .execute(context(10001L), new OrderQuery("O1001")),
                ErrorCode.TOOL_TIMEOUT);

        verify(mapper).findOwnedByOrderNo(10001L, "O1001");
        verifyNoMoreInteractions(mapper);
        assertFailureAudit(audit, ErrorCode.TOOL_TIMEOUT);
    }

    @Test
    void mapsUnexpectedMapperErrorsToASafeCodeAndNeverAuditsRawFailureText() {
        OrderMapper mapper = mock(OrderMapper.class);
        RecordingAuditService audit = new RecordingAuditService();
        String rawMessage = "SELECT * FROM order_info WHERE secret = 'raw user message'";
        when(mapper.findOwnedByOrderNo(10001L, "O1001")).thenThrow(new IllegalStateException(rawMessage));

        assertBusinessCode(() -> tool(mapper, audit, new DirectExecutor())
                        .execute(context(10001L), new OrderQuery("O1001")),
                ErrorCode.TOOL_EXECUTION_FAILED);

        verify(mapper).findOwnedByOrderNo(10001L, "O1001");
        verifyNoMoreInteractions(mapper);
        assertFailureAudit(audit, ErrorCode.TOOL_EXECUTION_FAILED);
        assertThat(audit.records.getFirst().audit.errorCode().getCode()).doesNotContain("SELECT", "raw user message");
    }

    @Test
    void failsClosedWhenTheSuccessAuditCannotBePersisted() {
        OrderMapper mapper = mock(OrderMapper.class);
        when(mapper.findOwnedByOrderNo(10001L, "O1001")).thenReturn(orderInfo());

        assertBusinessCode(() -> tool(mapper, new FailingAuditService(), new DirectExecutor())
                        .execute(context(10001L), new OrderQuery("O1001")),
                ErrorCode.TOOL_AUDIT_FAILED);
    }

    private OrderQueryTool tool(OrderMapper mapper, ToolAuditService audit, ToolOperationExecutor executor) {
        return new OrderQueryTool(mapper, audit, executor, validator);
    }

    private AgentExecutionContext context(long userId) {
        return new AgentExecutionContext("request-6", userId, "session-6", "raw user message");
    }

    private OrderInfo orderInfo() {
        OrderInfo order = new OrderInfo();
        order.setOrderNo("O1001");
        order.setProductName("Demo product 1001");
        order.setProductType("NORMAL");
        order.setOrderAmount(new BigDecimal("80.00"));
        order.setOrderStatus("PAID");
        order.setReceivedAt(LocalDateTime.of(2026, 8, 10, 9, 0));
        return order;
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

    private static final class FailingAuditService implements ToolAuditService {
        @Override
        public void recordSuccess(String toolName, ToolAuditRequest audit, long costTimeMs) {
            throw new IllegalStateException("database down");
        }

        @Override
        public void recordFailure(String toolName, ToolAuditRequest audit, long costTimeMs) {
            throw new IllegalStateException("database down");
        }
    }

    private record AuditRecord(String toolName, ToolAuditRequest audit, boolean success, long costTimeMs) { }
}
