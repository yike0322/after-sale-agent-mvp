package com.yike.aftersaleagent.agent;

import com.yike.aftersaleagent.common.api.ErrorCode;
import com.yike.aftersaleagent.common.exception.BusinessException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class DefaultBusinessReferenceExtractor implements BusinessReferenceExtractor {
    private static final Pattern ORDER_NO_PATTERN = Pattern.compile("\\bO\\d{4,}\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern COUPON_CODE_PATTERN = Pattern.compile("\\bC\\d{4,}\\b", Pattern.CASE_INSENSITIVE);

    @Override
    public String requireOrderNo(String message) {
        return requireFirst(message, ORDER_NO_PATTERN);
    }

    @Override
    public String requireCouponCode(String message) {
        return requireFirst(message, COUPON_CODE_PATTERN);
    }

    private String requireFirst(String message, Pattern pattern) {
        if (message != null) {
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                return matcher.group().toUpperCase(Locale.ROOT);
            }
        }
        throw new BusinessException(ErrorCode.BUSINESS_REFERENCE_REQUIRED);
    }
}
