package com.yike.aftersaleagent.ticket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seedOwnedTicketTrace() {
        jdbcTemplate.update("DELETE FROM tool_call_log");
        jdbcTemplate.update("DELETE FROM agent_step_log");
        jdbcTemplate.update("DELETE FROM ticket_task");
        jdbcTemplate.update("DELETE FROM customer_ticket");
        jdbcTemplate.update("INSERT INTO customer_ticket (id, user_id, ticket_type, priority, status, title, result_summary) VALUES (801, 10002, 'REFUND_REVIEW', 'NORMAL', 'WAIT_HUMAN', '退款资格人工审核', '规则资料不足，已转人工审核')");
        jdbcTemplate.update("INSERT INTO ticket_task (ticket_id, idempotency_key, status, current_step, total_steps) VALUES (801, 'refund:v1:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'WAIT_HUMAN', 5, 5)");
        jdbcTemplate.update("INSERT INTO agent_step_log (ticket_id, step_no, step_name, status, input_summary, output_summary, started_at, ended_at) VALUES (801, 1, 'QUERY_ORDER', 'SUCCESS', 'orderReference=validated', 'order=found', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
        jdbcTemplate.update("INSERT INTO tool_call_log (ticket_id, tool_name, request_summary, response_summary, success, cost_time_ms, request_id) VALUES (801, 'orderQuery', 'orderNo=O2001', 'orderNo=O2001,status=RECEIVED,amount=100.00', TRUE, 3, 'req-ticket-test')");
    }

    @Test
    void returnsOwnedTicketDetailAndSanitizedTrace() throws Exception {
        mockMvc.perform(get("/api/tickets/801").header("X-Demo-User-Id", "10002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticketId").value(801))
                .andExpect(jsonPath("$.data.status").value("WAIT_HUMAN"))
                .andExpect(jsonPath("$.data.currentStep").value(5));

        mockMvc.perform(get("/api/tickets/801/trace").header("X-Demo-User-Id", "10002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticketId").value(801))
                .andExpect(jsonPath("$.data.steps[0].stepName").value("QUERY_ORDER"))
                .andExpect(jsonPath("$.data.toolCalls[0].toolName").value("orderQuery"))
                .andExpect(jsonPath("$.data.toolCalls[0].requestId").doesNotExist());
    }

    @Test
    void hidesForeignTicketBehindTheUnifiedNotFoundCode() throws Exception {
        mockMvc.perform(get("/api/tickets/801").header("X-Demo-User-Id", "10001"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TICKET_NOT_FOUND_OR_FORBIDDEN"));
    }
}
