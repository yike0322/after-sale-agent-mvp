package com.yike.aftersaleagent.coupon;

import com.yike.aftersaleagent.order.OrderSummary;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CouponRuleServiceTest {
    private final CouponRuleService service = new DefaultCouponRuleService();

    @Test
    void unavailableStatusTakesPrecedenceOverAmountThreshold() {
        CouponEligibility result = service.evaluate(order("80.00"), coupon("USED", "100.00"));

        assertThat(result.usable()).isFalse();
        assertThat(result.reasonCode()).isEqualTo("COUPON_NOT_AVAILABLE");
        assertThat(result.reasonText()).isEqualTo("当前优惠券不可用，请确认优惠券状态后重试。");
    }

    @Test
    void availableCouponBelowThresholdReturnsStableAmountsAndReasonCode() {
        CouponEligibility result = service.evaluate(order("80.00"), coupon("AVAILABLE", "100.00"));

        assertThat(result.usable()).isFalse();
        assertThat(result.reasonCode()).isEqualTo("ORDER_AMOUNT_BELOW_THRESHOLD");
        assertThat(result.reasonText()).isEqualTo("订单金额 80.00 元，未达到优惠券使用门槛 100.00 元。");
    }

    @Test
    void amountEqualToThresholdIsUsableAfterStatusNormalization() {
        CouponEligibility result = service.evaluate(order("100.0"), coupon(" available ", "100.00"));

        assertThat(result.usable()).isTrue();
        assertThat(result.reasonCode()).isEqualTo("USABLE");
        assertThat(result.reasonText()).isEqualTo("订单金额已满足优惠券使用门槛，优惠券可使用。");
    }

    @Test
    void nullCouponStatusIsNotAvailableWithoutEvaluatingAmounts() {
        CouponEligibility result = service.evaluate(order("80.00"), coupon(null, "100.00"));

        assertThat(result).isEqualTo(new CouponEligibility(
                false, "COUPON_NOT_AVAILABLE", "当前优惠券不可用，请确认优惠券状态后重试。"));
    }

    private OrderSummary order(String amount) {
        return new OrderSummary(
                "O1001", "Demo product", "NORMAL", new BigDecimal(amount), "PAID", null);
    }

    private CouponSummary coupon(String status, String thresholdAmount) {
        return new CouponSummary("C1001", new BigDecimal(thresholdAmount), new BigDecimal("10.00"), status);
    }
}
