package com.yike.aftersaleagent.ticket;

import com.yike.aftersaleagent.identity.CurrentDemoUser;
import com.yike.aftersaleagent.identity.DemoRole;
import com.yike.aftersaleagent.identity.DemoTokenService;
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
class TicketRoleControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired DemoTokenService demoTokenService;

    @BeforeEach
    void seedTickets() {
        jdbcTemplate.update("DELETE FROM ticket_task");
        jdbcTemplate.update("DELETE FROM customer_ticket");
        insertTicket(901L, 10001L, "FINISHED");
        insertTicket(902L, 10002L, "WAIT_HUMAN");
    }

    @Test
    void customerCannotUseSupervisorList() throws Exception {
        mockMvc.perform(get("/api/supervisor/tickets").header("Authorization", customerBearer()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_ROLE_FORBIDDEN"));
    }

    @Test
    void supervisorListsAllTicketsWhileCustomerListHidesOwnerIdentity() throws Exception {
        mockMvc.perform(get("/api/supervisor/tickets").header("Authorization", supervisorBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].userId").exists());

        mockMvc.perform(get("/api/tickets/mine").header("Authorization", customerBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].ticketId").value(901))
                .andExpect(jsonPath("$.data.items[0].userId").doesNotExist());
    }

    private void insertTicket(long ticketId, long userId, String status) {
        jdbcTemplate.update("""
                INSERT INTO customer_ticket (id, user_id, ticket_type, priority, status, title)
                VALUES (?, ?, 'REFUND_REVIEW', 'NORMAL', ?, 'Demo ticket')
                """, ticketId, userId, status);
        jdbcTemplate.update("""
                INSERT INTO ticket_task (ticket_id, idempotency_key, status, current_step, total_steps)
                VALUES (?, ?, ?, 5, 5)
                """, ticketId, "ticket-role-" + ticketId, status);
    }

    private String customerBearer() {
        return "Bearer " + demoTokenService.issue(new CurrentDemoUser(10001L, "Buyer Li", DemoRole.CUSTOMER));
    }

    private String supervisorBearer() {
        return "Bearer " + demoTokenService.issue(new CurrentDemoUser(10003L, "Supervisor Chen", DemoRole.SUPERVISOR));
    }
}
