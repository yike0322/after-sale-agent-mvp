package com.yike.aftersaleagent.ticket;

import com.yike.aftersaleagent.common.exception.BusinessException;
import com.yike.aftersaleagent.ticket.domain.TicketTaskStateMachine;
import com.yike.aftersaleagent.ticket.domain.TicketTaskStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketTaskStateMachineTest {
    private final TicketTaskStateMachine stateMachine = new TicketTaskStateMachine();

    @Test
    void allowsOnlyTheDeclaredRefundTaskTransitions() {
        assertThat(stateMachine.transition(TicketTaskStatus.PENDING, TicketTaskStatus.RUNNING))
                .isEqualTo(TicketTaskStatus.RUNNING);
        assertThat(stateMachine.transition(TicketTaskStatus.RUNNING, TicketTaskStatus.WAIT_HUMAN))
                .isEqualTo(TicketTaskStatus.WAIT_HUMAN);
        assertThat(stateMachine.transition(TicketTaskStatus.RUNNING, TicketTaskStatus.FINISHED))
                .isEqualTo(TicketTaskStatus.FINISHED);
        assertThat(stateMachine.transition(TicketTaskStatus.RUNNING, TicketTaskStatus.FAILED))
                .isEqualTo(TicketTaskStatus.FAILED);
    }

    @Test
    void rejectsSelfTerminalAndSkippedTransitions() {
        assertIllegal(TicketTaskStatus.PENDING, TicketTaskStatus.WAIT_HUMAN);
        assertIllegal(TicketTaskStatus.PENDING, TicketTaskStatus.PENDING);
        assertIllegal(TicketTaskStatus.RUNNING, TicketTaskStatus.RUNNING);
        assertIllegal(TicketTaskStatus.FINISHED, TicketTaskStatus.RUNNING);
        assertIllegal(TicketTaskStatus.WAIT_HUMAN, TicketTaskStatus.WAIT_HUMAN);
        assertIllegal(null, TicketTaskStatus.RUNNING);
        assertIllegal(TicketTaskStatus.RUNNING, null);
    }

    private void assertIllegal(TicketTaskStatus current, TicketTaskStatus target) {
        assertThatThrownBy(() -> stateMachine.transition(current, target))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode().getCode())
                .isEqualTo("ILLEGAL_TASK_TRANSITION");
    }
}
