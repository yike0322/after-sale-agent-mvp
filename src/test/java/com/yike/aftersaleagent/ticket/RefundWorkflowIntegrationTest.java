package com.yike.aftersaleagent.ticket;

import com.yike.aftersaleagent.chat.api.ChatOutcome;
import com.yike.aftersaleagent.tool.AgentExecutionContext;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RefundWorkflowIntegrationTest {
    @Autowired RefundWorkflow refundWorkflow;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearRefundWorkflowTables() {
        jdbcTemplate.update("DELETE FROM tool_call_log");
        jdbcTemplate.update("DELETE FROM agent_step_log");
        jdbcTemplate.update("DELETE FROM ticket_task");
        jdbcTemplate.update("DELETE FROM customer_ticket");
    }

    @Test
    void submitsThenRunsFiveControlledStepsAndFallsBackToHumanReviewWhenEvidenceIsEmpty() throws Exception {
        List<String> statuses = new ArrayList<>();
        RefundSubmission submission = refundWorkflow.submit(
                new AgentExecutionContext("req-refund-1", 10002L, "refund-session-1", "我要退货，订单号 O2001"),
                statuses::add);
        ChatOutcome outcome = submission.completion().get(5, TimeUnit.SECONDS);

        assertThat(submission.newlySubmitted()).isTrue();
        assertThat(submission.ticketId()).isPositive();
        assertThat(statuses).contains("正在创建退款审核工单…", "正在查询订单…", "正在检索售后规则…", "正在判定退款资格…", "正在更新人工审核工单…", "正在生成处理摘要…");
        assertThat(outcome.ticketId()).isEqualTo(submission.ticketId());
        assertThat(outcome.taskStatus()).isEqualTo("WAIT_HUMAN");
        assertThat(outcome.citations()).isEmpty();
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM customer_ticket WHERE id = ?", String.class, submission.ticketId()))
                .isEqualTo("WAIT_HUMAN");
        assertThat(jdbcTemplate.queryForObject("SELECT current_step FROM ticket_task WHERE ticket_id = ?", Integer.class, submission.ticketId()))
                .isEqualTo(5);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM agent_step_log WHERE ticket_id = ?", Integer.class, submission.ticketId()))
                .isEqualTo(5);
    }
}
