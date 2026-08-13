package com.yike.aftersaleagent.identity;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class DemoIdentityWebConfiguration implements WebMvcConfigurer {
    private final DemoUserInterceptor demoUserInterceptor;

    DemoIdentityWebConfiguration(DemoUserInterceptor demoUserInterceptor) {
        this.demoUserInterceptor = demoUserInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(demoUserInterceptor).addPathPatterns("/api/**");
    }
}
