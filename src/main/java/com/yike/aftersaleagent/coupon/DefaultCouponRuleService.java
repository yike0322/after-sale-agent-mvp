package com.yike.aftersaleagent.coupon;

import com.yike.aftersaleagent.order.OrderSummary;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class DefaultCouponRuleService implements CouponRuleService {
    private static final String COUPON_NOT_AVAILABLE_TEXT = "当前优惠券不可用，请确认优惠券状态后重试。";
    private static final String USABLE_TEXT = "订单金额已满足优惠券使用门槛，优惠券可使用。";

    @Override
    public CouponEligibility evaluate(OrderSummary order, CouponSummary coupon) {
        Objects.requireNonNull(order, "order");
        Objects.requireNonNull(coupon, "coupon");

        String status = coupon.status() == null ? "" : coupon.status().strip().toUpperCase(Locale.ROOT);
        if (!"AVAILABLE".equals(status)) {
            return new CouponEligibility(false, "COUPON_NOT_AVAILABLE", COUPON_NOT_AVAILABLE_TEXT);
        }

        BigDecimal orderAmount = Objects.requireNonNull(order.amount(), "order.amount");
        BigDecimal thresholdAmount = Objects.requireNonNull(coupon.thresholdAmount(), "coupon.thresholdAmount");
        if (orderAmount.compareTo(thresholdAmount) < 0) {
            return new CouponEligibility(
                    false,
                    "ORDER_AMOUNT_BELOW_THRESHOLD",
                    "订单金额 " + displayAmount(orderAmount) + " 元，未达到优惠券使用门槛 "
                            + displayAmount(thresholdAmount) + " 元。");
        }
        return new CouponEligibility(true, "USABLE", USABLE_TEXT);
    }

    private String displayAmount(BigDecimal amount) {
        return amount.setScale(2).toPlainString();
    }
}
