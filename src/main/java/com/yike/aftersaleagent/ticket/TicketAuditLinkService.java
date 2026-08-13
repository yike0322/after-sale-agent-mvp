package com.yike.aftersaleagent.ticket;

import com.yike.aftersaleagent.common.api.ErrorCode;
import com.yike.aftersaleagent.common.exception.BusinessException;
import com.yike.aftersaleagent.tool.ToolCallLogMapper;
import org.springframework.stereotype.Service;

@Service
public class TicketAuditLinkService {
    private final ToolCallLogMapper toolCallLogMapper;

    public TicketAuditLinkService(ToolCallLogMapper toolCallLogMapper) {
        this.toolCallLogMapper = toolCallLogMapper;
    }

    public void linkRefundAuditAttempts(long userId, long ticketId, String requestId) {
        if (userId <= 0 || ticketId <= 0 || requestId == null || requestId.isBlank()) {
            throw new BusinessException(ErrorCode.TICKET_PERSISTENCE_FAILED);
        }
        try {
            toolCallLogMapper.linkRefundAuditsToOwnedTicket(userId, ticketId, requestId);
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.TICKET_PERSISTENCE_FAILED);
        }
    }
}
