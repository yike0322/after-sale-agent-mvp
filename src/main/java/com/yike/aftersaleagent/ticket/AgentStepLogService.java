package com.yike.aftersaleagent.ticket;

import com.yike.aftersaleagent.common.api.ErrorCode;
import com.yike.aftersaleagent.common.exception.BusinessException;
import com.yike.aftersaleagent.ticket.domain.AgentStepStatus;
import com.yike.aftersaleagent.ticket.domain.RefundWorkflowStep;
import org.springframework.stereotype.Service;

@Service
public class AgentStepLogService {
    private final AgentStepLogMapper agentStepLogMapper;

    public AgentStepLogService(AgentStepLogMapper agentStepLogMapper) {
        this.agentStepLogMapper = agentStepLogMapper;
    }

    public void appendCompleted(
            long ticketId,
            RefundWorkflowStep step,
            AgentStepStatus status,
            AgentStepLogPayload payload) {
        if (ticketId <= 0 || step == null || status == null || payload == null
                || agentStepLogMapper.insertCompleted(ticketId, step.number(), step.name(), status.name(),
                payload.inputSummary(), payload.outputSummary(), payload.errorCode()) != 1) {
            throw new BusinessException(ErrorCode.TICKET_PERSISTENCE_FAILED);
        }
    }
}
