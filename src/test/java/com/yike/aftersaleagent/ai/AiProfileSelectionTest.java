package com.yike.aftersaleagent.ai;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AiProfileSelectionTest {

    @ParameterizedTest
    @MethodSource("gatewayProfileCases")
    void selectsExactlyOneGatewayForEverySupportedProfileCombination(
            String[] activeProfiles, Class<? extends AiGateway> expectedGatewayType) {
        try (AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles(activeProfiles);
            context.registerBean(ChatClient.Builder.class, () -> mock(ChatClient.Builder.class));
            context.register(MockAiGateway.class, DashScopeAiGateway.class);
            context.refresh();

            assertThat(context.getBeansOfType(AiGateway.class).values())
                    .singleElement()
                    .isInstanceOf(expectedGatewayType);
        }
    }

    @Test
    void dashscopeWinsOverMockConfigurationForCombinedProfiles() {
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.profiles.active=dashscope,mock")
                .run(context -> assertThat(context.getEnvironment()
                                .getProperty("spring.ai.dashscope.enabled", Boolean.class, true))
                        .isTrue());
    }

    @Test
    void dashscopeWinsOverTestProfileGroupConfiguration() {
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.profiles.active=dashscope,test")
                .run(context -> assertThat(context.getEnvironment()
                                .getProperty("spring.ai.dashscope.enabled", Boolean.class, true))
                        .isTrue());
    }

    private static Stream<Arguments> gatewayProfileCases() {
        return Stream.of(
                Arguments.of(new String[] {}, MockAiGateway.class),
                Arguments.of(new String[] {"mock"}, MockAiGateway.class),
                Arguments.of(new String[] {"test"}, MockAiGateway.class),
                Arguments.of(new String[] {"dashscope"}, DashScopeAiGateway.class),
                Arguments.of(new String[] {"dashscope", "mock"}, DashScopeAiGateway.class),
                Arguments.of(new String[] {"dashscope", "test"}, DashScopeAiGateway.class));
    }
}
