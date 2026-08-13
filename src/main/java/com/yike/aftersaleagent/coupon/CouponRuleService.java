package com.yike.aftersaleagent.coupon;

import com.yike.aftersaleagent.order.OrderSummary;

public interface CouponRuleService {
    CouponEligibility evaluate(OrderSummary order, CouponSummary coupon);
}
