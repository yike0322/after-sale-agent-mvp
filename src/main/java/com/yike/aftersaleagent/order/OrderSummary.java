package com.yike.aftersaleagent.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummary(
        String orderNo,
        String productName,
        String productType,
        BigDecimal amount,
        String orderStatus,
        LocalDateTime receivedAt) { }
