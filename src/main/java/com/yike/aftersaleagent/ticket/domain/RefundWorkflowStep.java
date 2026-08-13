package com.yike.aftersaleagent.ticket.domain;

public enum RefundWorkflowStep {
    QUERY_ORDER(1),
    RETRIEVE_AFTER_SALE_RULE(2),
    EVALUATE_REFUND_ELIGIBILITY(3),
    CREATE_OR_UPDATE_HUMAN_REVIEW_TICKET(4),
    GENERATE_USER_SUMMARY(5);

    private final int number;

    RefundWorkflowStep(int number) {
        this.number = number;
    }

    public int number() {
        return number;
    }
}
