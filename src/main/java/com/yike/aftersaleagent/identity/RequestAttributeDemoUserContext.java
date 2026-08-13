package com.yike.aftersaleagent.identity;

import com.yike.aftersaleagent.common.api.ErrorCode;
import com.yike.aftersaleagent.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
final class RequestAttributeDemoUserContext implements DemoUserContext {
    private final HttpServletRequest request;

    RequestAttributeDemoUserContext(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public CurrentDemoUser requireCurrentUser() {
        Object currentUser = request.getAttribute(DemoUserInterceptor.CURRENT_USER_ATTRIBUTE);
        if (currentUser instanceof CurrentDemoUser demoUser) {
            return demoUser;
        }
        throw new BusinessException(ErrorCode.DEMO_USER_REQUIRED);
    }
}
