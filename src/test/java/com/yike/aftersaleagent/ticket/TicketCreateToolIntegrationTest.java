package com.yike.aftersaleagent.ticket;

import com.yike.aftersaleagent.agent.Intent;
import com.yike.aftersaleagent.tool.AgentExecutionContext;
import com.yike.aftersaleagent.tool.ToolRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class TicketCreateToolIntegrationTest {
    @Autowired
    ToolRegistry toolRegistry;

    @Autowired
    TicketCreateTool ticketCreateTool;

    @Autowired
    RefundIdempotencyKeyFactory keyFactory;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearTickets() {
        jdbcTemplate.update("DELETE FROM tool_call_log");
        jdbcTemplate.update("DELETE FROM agent_step_log");
        jdbcTemplate.update("DELETE FROM ticket_task");
        jdbcTemplate.update("DELETE FROM customer_ticket");
    }

    @Test
    void createsOneOwnedPendingTicketAndReusesItForTheSameIdempotencyKey() {
        AgentExecutionContext context = new AgentExecutionContext(
                "req-refund-create-1", 10002L, "session-refund-1", "DO_NOT_LOG O2001");
        String idempotencyKey = keyFactory.create(context.userId(), context.sessionId(), "O2001");
        TicketCreateRequest request = new TicketCreateRequest("O2001", idempotencyKey);

        TicketCreateResult created = toolRegistry.execute(
                Intent.REFUND_ELIGIBILITY, ticketCreateTool, context, request);
        TicketCreateResult reused = toolRegistry.execute(
                Intent.REFUND_ELIGIBILITY, ticketCreateTool, context, request);

        assertThat(created.newlySubmitted()).isTrue();
        assertThat(reused).isEqualTo(new TicketCreateResult(created.ticketId(), false, created.currentStatus()));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM customer_ticket", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ticket_task", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForList("""
                SELECT user_id, ticket_type, priority, status, title, description
                FROM customer_ticket WHERE id = ?
                """, created.ticketId()))
                .singleElement()
                .satisfies(row -> assertThat(row).containsEntry("USER_ID", 10002L)
                        .containsEntry("TICKET_TYPE", "REFUND_REVIEW")
                        .containsEntry("PRIORITY", "NORMAL")
                        .containsEntry("STATUS", "PENDING")
                        .containsEntry("TITLE", "退款资格人工审核")
                        .containsEntry("DESCRIPTION", "orderNo=O2001"));
        assertThat(jdbcTemplate.queryForList("SELECT request_summary, response_summary FROM tool_call_log"))
                .allSatisfy(row -> assertThat(row.values().toString()).doesNotContain("DO_NOT_LOG", context.sessionId()));
    }
}
