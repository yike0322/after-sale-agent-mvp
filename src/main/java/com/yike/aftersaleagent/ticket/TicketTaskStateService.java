package com.yike.aftersaleagent.ticket;

import com.yike.aftersaleagent.common.api.ErrorCode;
import com.yike.aftersaleagent.common.exception.BusinessException;
import com.yike.aftersaleagent.ticket.domain.TicketTaskStateMachine;
import com.yike.aftersaleagent.ticket.domain.TicketTaskStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketTaskStateService {
    private final TicketTaskStateMachine stateMachine = new TicketTaskStateMachine();
    private final TicketTaskMapper ticketTaskMapper;
    private final TicketMapper ticketMapper;

    public TicketTaskStateService(TicketTaskMapper ticketTaskMapper, TicketMapper ticketMapper) {
        this.ticketTaskMapper = ticketTaskMapper;
        this.ticketMapper = ticketMapper;
    }

    @Transactional
    public void transition(
            long userId,
            long ticketId,
            long taskId,
            TicketTaskStatus expected,
            TicketTaskStatus target,
            int currentStep,
            String safeResultSummary) {
        stateMachine.transition(expected, target);
        if (ticketTaskMapper.transition(taskId, ticketId, expected.name(), target.name(), currentStep) != 1
                || ticketMapper.updateOwnedStatusAndSummary(userId, ticketId, target.name(), safeResultSummary) != 1) {
            throw new BusinessException(ErrorCode.TICKET_PERSISTENCE_FAILED);
        }
    }
}
