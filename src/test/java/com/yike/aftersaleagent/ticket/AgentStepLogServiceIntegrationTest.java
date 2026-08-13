package com.yike.aftersaleagent.ticket;

import com.yike.aftersaleagent.ticket.domain.AgentStepStatus;
import com.yike.aftersaleagent.ticket.domain.RefundWorkflowStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AgentStepLogServiceIntegrationTest {
    @Autowired AgentStepLogService agentStepLogService;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearLogs() {
        jdbcTemplate.update("DELETE FROM agent_step_log");
        jdbcTemplate.update("DELETE FROM customer_ticket");
        jdbcTemplate.update("INSERT INTO customer_ticket (id, user_id, ticket_type, priority, status, title) VALUES (901, 10002, 'REFUND_REVIEW', 'NORMAL', 'RUNNING', '退款资格人工审核')");
    }

    @Test
    void appendsACompletedControlledStepWithoutMutableRunningRecord() {
        agentStepLogService.appendCompleted(901L, RefundWorkflowStep.QUERY_ORDER,
                AgentStepStatus.SUCCESS, AgentStepLogPayload.queryOrderSucceeded());

        assertThat(jdbcTemplate.queryForMap("SELECT step_no, step_name, status, input_summary, output_summary, error_message, started_at, ended_at FROM agent_step_log WHERE ticket_id = 901"))
                .containsEntry("STEP_NO", 1)
                .containsEntry("STEP_NAME", "QUERY_ORDER")
                .containsEntry("STATUS", "SUCCESS")
                .containsEntry("INPUT_SUMMARY", "orderReference=validated")
                .containsEntry("OUTPUT_SUMMARY", "order=found")
                .containsEntry("ERROR_MESSAGE", null);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM agent_step_log WHERE ticket_id = 901", Integer.class))
                .isEqualTo(1);
    }
}
