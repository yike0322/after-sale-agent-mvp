package com.yike.aftersaleagent.identity;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class DemoIdentityConfiguration {
    @Bean
    Clock applicationClock() {
        return Clock.systemUTC();
    }
}
