package com.yike.aftersaleagent.coupon;

import java.math.BigDecimal;

public record CouponSummary(
        String couponCode,
        BigDecimal thresholdAmount,
        BigDecimal discountAmount,
        String status) { }
