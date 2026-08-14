package com.yike.aftersaleagent.identity;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
class DemoAuthConfiguration {
    @Bean
    PasswordEncoder demoPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
