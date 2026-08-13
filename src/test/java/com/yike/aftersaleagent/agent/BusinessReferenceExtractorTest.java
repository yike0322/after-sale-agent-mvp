package com.yike.aftersaleagent.agent;

import com.yike.aftersaleagent.common.api.ErrorCode;
import com.yike.aftersaleagent.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BusinessReferenceExtractorTest {
    private final BusinessReferenceExtractor extractor = new DefaultBusinessReferenceExtractor();

    @Test
    void extractsOrderNumbersUsingTheFixedCaseInsensitivePattern() {
        assertThat(extractor.requireOrderNo("订单 O1001 想查询"))
                .isEqualTo("O1001");
        assertThat(extractor.requireOrderNo("first o1001 then O2001"))
                .isEqualTo("O1001");
    }

    @Test
    void extractsCouponNumbersUsingTheFixedCaseInsensitivePattern() {
        assertThat(extractor.requireCouponCode("优惠券 c1001 为什么不能使用"))
                .isEqualTo("C1001");
        assertThat(extractor.requireCouponCode("first c1001 then C2001"))
                .isEqualTo("C1001");
    }

    @Test
    void rejectsMissingOrMalformedBusinessReferencesWithoutGuessing() {
        assertBusinessCode(() -> extractor.requireOrderNo("我想查询订单"));
        assertBusinessCode(() -> extractor.requireOrderNo("订单 O123"));
        assertBusinessCode(() -> extractor.requireCouponCode(null));
        assertBusinessCode(() -> extractor.requireCouponCode("优惠券 C123"));
    }

    private void assertBusinessCode(ThrowingCallable action) {
        assertThatThrownBy(action::call)
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode().getCode())
                .isEqualTo(ErrorCode.BUSINESS_REFERENCE_REQUIRED.getCode());
    }

    @FunctionalInterface
    private interface ThrowingCallable {
        void call();
    }
}
