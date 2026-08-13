package com.yike.aftersaleagent.coupon;

import jakarta.validation.constraints.NotBlank;

public record CouponQuery(@NotBlank String couponCode) { }
