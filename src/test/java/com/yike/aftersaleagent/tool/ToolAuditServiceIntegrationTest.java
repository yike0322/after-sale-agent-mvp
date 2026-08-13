package com.yike.aftersaleagent.tool;

import com.yike.aftersaleagent.common.api.ErrorCode;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ToolAuditServiceIntegrationTest {

    @Autowired
    ToolAuditService toolAuditService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearAuditRows() {
        jdbcTemplate.update("DELETE FROM tool_call_log");
    }

    @Test
    void persistsOnlySafeAuditFieldsWithNoTicketAssociation() {
        toolAuditService.recordSuccess(
                "orderQuery",
                ToolAuditRequest.success(
                        "request-6-integration",
                        "orderNo=O1001",
                        "orderNo=O1001,status=PAID,amount=80.00"),
                5L);
        toolAuditService.recordFailure(
                "couponQuery",
                ToolAuditRequest.failure(
                        "request-6-failure",
                        "couponCode=invalid",
                        ErrorCode.BUSINESS_REFERENCE_REQUIRED),
                0L);

        Map<String, Object> success = jdbcTemplate.queryForMap("""
                SELECT ticket_id AS ticketId, request_id AS requestId, success,
                       cost_time_ms AS costTimeMs, request_summary AS requestSummary,
                       response_summary AS responseSummary, error_message AS errorMessage
                FROM tool_call_log
                WHERE tool_name = 'orderQuery'
                """);
        Map<String, Object> failure = jdbcTemplate.queryForMap("""
                SELECT ticket_id AS ticketId, request_id AS requestId, success,
                       cost_time_ms AS costTimeMs, request_summary AS requestSummary,
                       response_summary AS responseSummary, error_message AS errorMessage
                FROM tool_call_log
                WHERE tool_name = 'couponQuery'
                """);

        assertThat(success)
                .containsEntry("ticketId", null)
                .containsEntry("requestId", "request-6-integration")
                .containsEntry("requestSummary", "orderNo=O1001")
                .containsEntry("errorMessage", null);
        assertThat(success.get("success")).isEqualTo(true);
        assertThat(((Number) success.get("costTimeMs")).longValue()).isGreaterThanOrEqualTo(0L);
        assertThat((String) success.get("responseSummary"))
                .doesNotContain("raw user message", "SELECT", "Exception");

        assertThat(failure)
                .containsEntry("ticketId", null)
                .containsEntry("requestId", "request-6-failure")
                .containsEntry("requestSummary", "couponCode=invalid")
                .containsEntry("responseSummary", null)
                .containsEntry("errorMessage", ErrorCode.BUSINESS_REFERENCE_REQUIRED.getCode());
        assertThat(failure.get("success")).isEqualTo(false);
        assertThat(((Number) failure.get("costTimeMs")).longValue()).isGreaterThanOrEqualTo(0L);
    }
}
