package com.yike.aftersaleagent.agent;

public interface BusinessReferenceExtractor {
    String requireOrderNo(String message);

    String requireCouponCode(String message);
}
