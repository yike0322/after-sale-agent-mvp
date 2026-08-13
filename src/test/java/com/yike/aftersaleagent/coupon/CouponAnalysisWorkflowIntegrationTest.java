package com.yike.aftersaleagent.coupon;

import com.yike.aftersaleagent.ai.AiGateway;
import com.yike.aftersaleagent.chat.api.ChatOutcome;
import com.yike.aftersaleagent.common.api.ErrorCode;
import com.yike.aftersaleagent.common.exception.BusinessException;
import com.yike.aftersaleagent.tool.AgentExecutionContext;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class CouponAnalysisWorkflowIntegrationTest {
    private static final String SUCCESS_REQUEST_ID = "req-coupon-audit-1";

    @Autowired
    CouponAnalysisWorkflow workflow;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @MockitoBean
    AiGateway aiGateway;

    @BeforeEach
    void clearAuditRows() {
        jdbcTemplate.update("DELETE FROM tool_call_log");
    }

    @Test
    void executesGovernedOwnedToolsInOrderAndPersistsOnlySafeAuditSummaries() {
        given(aiGateway.explain(anyString(), anyString())).willReturn("已按系统规则核验优惠券门槛。");
        AgentExecutionContext context = new AgentExecutionContext(
                SUCCESS_REQUEST_ID,
                10001L,
                "session-audit",
                "订单 O1001 的优惠券 C1001，隐私标记=DO_NOT_LOG");

        ChatOutcome outcome = workflow.execute(context);

        assertThat(outcome.taskStatus()).isEqualTo("ORDER_AMOUNT_BELOW_THRESHOLD");
        assertThat(outcome.ticketId()).isNull();
        assertThat(outcome.citations()).isEmpty();
        assertThat(outcome.reply()).startsWith("系统规则结论：订单金额 80.00 元，未达到优惠券使用门槛 100.00 元。");

        List<AuditRow> rows = auditRows(SUCCESS_REQUEST_ID);
        assertThat(rows).extracting(AuditRow::toolName).containsExactly("orderQuery", "couponQuery");
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.success()).isTrue();
            assertThat(row.ticketId()).isNull();
            assertThat(row.costTimeMs()).isGreaterThanOrEqualTo(0L);
            assertThat(row.errorMessage()).isNull();
            String storedSummary = row.requestSummary() + " " + row.responseSummary();
            assertThat(storedSummary).doesNotContain(
                    "DO_NOT_LOG", context.userMessage(), "10001", "session-audit", "reasonCode", "Exception");
        });
        assertThat(rows.get(0).requestSummary()).isEqualTo("orderNo=O1001");
        assertThat(rows.get(0).responseSummary()).contains("orderNo=O1001", "status=PAID", "amount=80.00");
        assertThat(rows.get(1).requestSummary()).isEqualTo("couponCode=C1001");
        assertThat(rows.get(1).responseSummary()).contains(
                "couponCode=C1001", "status=AVAILABLE", "thresholdAmount=100.00", "discountAmount=10.00");
        assertThat(rows).noneMatch(row -> row.toolName().equals("couponAnalysis"));

        ArgumentCaptor<String> facts = ArgumentCaptor.forClass(String.class);
        verify(aiGateway).explain(anyString(), facts.capture());
        assertThat(facts.getValue())
                .contains("reasonCode=ORDER_AMOUNT_BELOW_THRESHOLD", "orderAmount=80.00", "couponThresholdAmount=100.00")
                .doesNotContain("DO_NOT_LOG", "10001", "session-audit", context.userMessage());
        verify(aiGateway, never()).classifyIntent(any());
    }

    @Test
    void keepsForeignOrderExistencePrivateAndStopsBeforeCouponTool() {
        AgentExecutionContext foreignContext = new AgentExecutionContext(
                "req-coupon-idor-1",
                10002L,
                "session-foreign",
                "我要查询订单 O1001 和优惠券 C1001，隐私标记=DO_NOT_LOG");

        assertThatThrownBy(() -> workflow.execute(foreignContext))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_NOT_FOUND_OR_FORBIDDEN);

        List<AuditRow> rows = auditRows("req-coupon-idor-1");
        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.toolName()).isEqualTo("orderQuery");
            assertThat(row.success()).isFalse();
            assertThat(row.ticketId()).isNull();
            assertThat(row.requestSummary()).isEqualTo("orderNo=O1001");
            assertThat(row.responseSummary()).isNull();
            assertThat(row.errorMessage()).isEqualTo(ErrorCode.ORDER_NOT_FOUND_OR_FORBIDDEN.getCode());
            assertThat(row.requestSummary() + " " + row.errorMessage()).doesNotContain(
                    "10001", "session-foreign", "DO_NOT_LOG", "belongs");
        });
        verify(aiGateway, never()).explain(anyString(), anyString());
    }

    private List<AuditRow> auditRows(String requestId) {
        return jdbcTemplate.query("""
                SELECT tool_name, ticket_id, success, cost_time_ms,
                       request_summary, response_summary, error_message
                FROM tool_call_log
                WHERE request_id = ?
                ORDER BY id
                """, (resultSet, rowNumber) -> new AuditRow(
                resultSet.getString("tool_name"),
                resultSet.getObject("ticket_id"),
                resultSet.getBoolean("success"),
                resultSet.getLong("cost_time_ms"),
                resultSet.getString("request_summary"),
                resultSet.getString("response_summary"),
                resultSet.getString("error_message")), requestId);
    }

    private record AuditRow(
            String toolName,
            Object ticketId,
            boolean success,
            long costTimeMs,
            String requestSummary,
            String responseSummary,
            String errorMessage) { }
}
