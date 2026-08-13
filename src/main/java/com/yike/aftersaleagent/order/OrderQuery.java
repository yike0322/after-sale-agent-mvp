package com.yike.aftersaleagent.order;

import jakarta.validation.constraints.NotBlank;

public record OrderQuery(@NotBlank String orderNo) { }
