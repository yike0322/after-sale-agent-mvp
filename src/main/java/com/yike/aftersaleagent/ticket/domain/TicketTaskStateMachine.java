package com.yike.aftersaleagent.ticket.domain;

import com.yike.aftersaleagent.common.api.ErrorCode;
import com.yike.aftersaleagent.common.exception.BusinessException;

public class TicketTaskStateMachine {
    public TicketTaskStatus transition(TicketTaskStatus current, TicketTaskStatus target) {
        if ((current == TicketTaskStatus.PENDING && target == TicketTaskStatus.RUNNING)
                || (current == TicketTaskStatus.RUNNING && (target == TicketTaskStatus.WAIT_HUMAN
                || target == TicketTaskStatus.FINISHED || target == TicketTaskStatus.FAILED))) {
            return target;
        }
        throw new BusinessException(ErrorCode.ILLEGAL_TASK_TRANSITION);
    }
}
