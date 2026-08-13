package com.yike.aftersaleagent.tool;

import com.yike.aftersaleagent.common.api.ErrorCode;
import com.yike.aftersaleagent.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersistentToolAuditService implements ToolAuditService {
    private final ToolCallLogMapper toolCallLogMapper;

    public PersistentToolAuditService(ToolCallLogMapper toolCallLogMapper) {
        this.toolCallLogMapper = toolCallLogMapper;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(String toolName, ToolAuditRequest audit, long costTimeMs) {
        persist(toolName, audit, true, costTimeMs, audit.responseSummary(), null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String toolName, ToolAuditRequest audit, long costTimeMs) {
        String errorCode = audit.errorCode() == null ? ErrorCode.TOOL_EXECUTION_FAILED.getCode()
                : audit.errorCode().getCode();
        persist(toolName, audit, false, costTimeMs, null, errorCode);
    }

    private void persist(
            String toolName,
            ToolAuditRequest audit,
            boolean success,
            long costTimeMs,
            String responseSummary,
            String errorMessage) {
        try {
            toolCallLogMapper.insert(
                    toolName,
                    audit.requestSummary(),
                    responseSummary,
                    success,
                    Math.max(0L, costTimeMs),
                    errorMessage,
                    audit.requestId());
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.TOOL_AUDIT_FAILED);
        }
    }
}
