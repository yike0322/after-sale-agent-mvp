package com.yike.aftersaleagent.ticket;

import com.yike.aftersaleagent.ticket.domain.RefundDecision;
import com.yike.aftersaleagent.ticket.domain.TicketTaskStatus;

public final class AgentStepLogPayload {
    private final String inputSummary;
    private final String outputSummary;
    private final String errorCode;

    private AgentStepLogPayload(String inputSummary, String outputSummary, String errorCode) {
        this.inputSummary = inputSummary;
        this.outputSummary = outputSummary;
        this.errorCode = errorCode;
    }

    public static AgentStepLogPayload queryOrderSucceeded() {
        return new AgentStepLogPayload("orderReference=validated", "order=found", null);
    }

    public static AgentStepLogPayload queryOrderFailed() {
        return new AgentStepLogPayload("orderReference=validated", null, "ORDER_QUERY_FAILED");
    }

    public static AgentStepLogPayload ruleQuerySucceeded(int evidenceCount) {
        return new AgentStepLogPayload("ruleQuery=fixed", "evidenceCount=" + Math.max(0, Math.min(3, evidenceCount)), null);
    }

    public static AgentStepLogPayload ruleQueryFailed() {
        return new AgentStepLogPayload("ruleQuery=fixed", null, "AFTER_SALE_RULE_QUERY_FAILED");
    }

    public static AgentStepLogPayload decision(RefundDecision decision) {
        return new AgentStepLogPayload("ruleEngine=java", "reasonCode=" + decision.reasonCode(), null);
    }

    public static AgentStepLogPayload ticketUpdated(TicketTaskStatus status) {
        return new AgentStepLogPayload("stateMachine=fixed", "taskStatus=" + status.name(), null);
    }

    public static AgentStepLogPayload summaryGenerated(boolean fallback) {
        return new AgentStepLogPayload("summary=controlled", "fallback=" + fallback, null);
    }

    String inputSummary() {
        return inputSummary;
    }

    String outputSummary() {
        return outputSummary;
    }

    String errorCode() {
        return errorCode;
    }
}
