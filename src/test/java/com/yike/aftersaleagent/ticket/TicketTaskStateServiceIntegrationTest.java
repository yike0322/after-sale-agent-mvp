package com.yike.aftersaleagent.ticket;

import com.yike.aftersaleagent.agent.Intent;
import com.yike.aftersaleagent.common.api.ErrorCode;
import com.yike.aftersaleagent.common.exception.BusinessException;
import com.yike.aftersaleagent.ticket.domain.TicketTaskStatus;
import com.yike.aftersaleagent.tool.AgentExecutionContext;
import com.yike.aftersaleagent.tool.ToolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class TicketTaskStateServiceIntegrationTest {
    @Autowired ToolRegistry toolRegistry;
    @Autowired TicketCreateTool ticketCreateTool;
    @Autowired RefundIdempotencyKeyFactory keyFactory;
    @Autowired TicketTaskStateService stateService;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearTickets() {
        jdbcTemplate.update("DELETE FROM tool_call_log");
        jdbcTemplate.update("DELETE FROM agent_step_log");
        jdbcTemplate.update("DELETE FROM ticket_task");
        jdbcTemplate.update("DELETE FROM customer_ticket");
    }

    @Test
    void transitionsOwnedTaskAndTicketTogetherWithCompareAndSetStates() {
        AgentExecutionContext context = new AgentExecutionContext("req-state-1", 10002L, "session-state", "O2001");
        TicketCreateResult created = toolRegistry.execute(Intent.REFUND_ELIGIBILITY, ticketCreateTool, context,
                new TicketCreateRequest("O2001", keyFactory.create(context.userId(), context.sessionId(), "O2001")));
        long taskId = jdbcTemplate.queryForObject("SELECT id FROM ticket_task WHERE ticket_id = ?", Long.class, created.ticketId());

        stateService.transition(context.userId(), created.ticketId(), taskId,
                TicketTaskStatus.PENDING, TicketTaskStatus.RUNNING, 0, "处理中");
        stateService.transition(context.userId(), created.ticketId(), taskId,
                TicketTaskStatus.RUNNING, TicketTaskStatus.WAIT_HUMAN, 5, "已进入人工审核");

        assertThat(jdbcTemplate.queryForMap("SELECT status, current_step FROM ticket_task WHERE id = ?", taskId))
                .containsEntry("STATUS", "WAIT_HUMAN").containsEntry("CURRENT_STEP", 5);
        assertThat(jdbcTemplate.queryForMap("SELECT status, result_summary FROM customer_ticket WHERE id = ?", created.ticketId()))
                .containsEntry("STATUS", "WAIT_HUMAN").containsEntry("RESULT_SUMMARY", "已进入人工审核");
    }

    @Test
    void rejectsTaskThatBelongsToAnotherTicketWithoutChangingEitherTicket() {
        AgentExecutionContext firstContext = new AgentExecutionContext("req-state-2a", 10002L, "session-state-a", "O2001");
        AgentExecutionContext secondContext = new AgentExecutionContext("req-state-2b", 10002L, "session-state-b", "O2001");
        TicketCreateResult first = toolRegistry.execute(Intent.REFUND_ELIGIBILITY, ticketCreateTool, firstContext,
                new TicketCreateRequest("O2001", keyFactory.create(firstContext.userId(), firstContext.sessionId(), "O2001")));
        TicketCreateResult second = toolRegistry.execute(Intent.REFUND_ELIGIBILITY, ticketCreateTool, secondContext,
                new TicketCreateRequest("O2001", keyFactory.create(secondContext.userId(), secondContext.sessionId(), "O2001")));
        long firstTaskId = jdbcTemplate.queryForObject("SELECT id FROM ticket_task WHERE ticket_id = ?", Long.class, first.ticketId());

        assertThatThrownBy(() -> stateService.transition(firstContext.userId(), second.ticketId(), firstTaskId,
                TicketTaskStatus.PENDING, TicketTaskStatus.RUNNING, 0, "处理中"))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.TICKET_PERSISTENCE_FAILED);

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM ticket_task WHERE id = ?", String.class, firstTaskId))
                .isEqualTo("PENDING");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM customer_ticket WHERE id = ?", String.class, second.ticketId()))
                .isEqualTo("PENDING");
    }
}
