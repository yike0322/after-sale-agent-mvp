package com.yike.aftersaleagent.tool;

public interface ToolAuditService {
    void recordSuccess(String toolName, ToolAuditRequest audit, long costTimeMs);

    void recordFailure(String toolName, ToolAuditRequest audit, long costTimeMs);
}
