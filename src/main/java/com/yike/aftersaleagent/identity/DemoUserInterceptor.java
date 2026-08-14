package com.yike.aftersaleagent.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yike.aftersaleagent.common.api.ApiResponse;
import com.yike.aftersaleagent.common.api.ErrorCode;
import com.yike.aftersaleagent.common.exception.BusinessException;
import com.yike.aftersaleagent.common.trace.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class DemoUserInterceptor implements HandlerInterceptor {
    public static final String CURRENT_USER_ATTRIBUTE =
            DemoUserInterceptor.class.getName() + ".currentUser";
    private static final String DEMO_USER_HEADER = "X-Demo-User-Id";

    private final DemoUserMapper demoUserMapper;
    private final DemoTokenService demoTokenService;
    private final ObjectMapper objectMapper;

    public DemoUserInterceptor(
            DemoUserMapper demoUserMapper, DemoTokenService demoTokenService, ObjectMapper objectMapper) {
        this.demoUserMapper = demoUserMapper;
        this.demoTokenService = demoTokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        CurrentDemoUser user;
        try {
            user = findBearerUser(request.getHeader("Authorization"));
        } catch (BusinessException exception) {
            writeError(response, exception.getErrorCode());
            return false;
        }
        if (user == null) {
            String header = request.getHeader(DEMO_USER_HEADER);
            if (!StringUtils.hasText(header)) {
                writeError(response, ErrorCode.DEMO_USER_REQUIRED);
                return false;
            }
            user = findDemoUser(header.trim());
        }
        if (user == null) {
            writeError(response, ErrorCode.DEMO_USER_NOT_FOUND);
            return false;
        }

        request.setAttribute(CURRENT_USER_ATTRIBUTE, user);
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) {
        request.removeAttribute(CURRENT_USER_ATTRIBUTE);
    }

    private CurrentDemoUser findDemoUser(String header) {
        try {
            return demoUserMapper.findById(Long.parseLong(header));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private CurrentDemoUser findBearerUser(String authorization) {
        if (!StringUtils.hasText(authorization)) {
            return null;
        }
        if (!authorization.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID);
        }
        return demoTokenService.verify(authorization.substring(7).strip());
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<Void> body = ApiResponse.failure(
                errorCode.getCode(), errorCode.getMessage(), MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY));
        objectMapper.writeValue(response.getWriter(), body);
    }
}
