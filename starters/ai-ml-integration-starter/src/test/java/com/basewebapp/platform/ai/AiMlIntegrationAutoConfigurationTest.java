package com.basewebapp.platform.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class AiMlIntegrationAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner().withConfiguration(
        AutoConfigurations.of(AiMlIntegrationAutoConfiguration.class)
    );

    @Test
    void shouldRegisterTheMockProviderOutOfTheBox() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(InferenceProviderRegistry.class);
            InferenceProviderRegistry registry = context.getBean(InferenceProviderRegistry.class);
            assertThat(registry.names()).containsExactly("mock");
            assertThat(registry.infer(InferenceRequest.of("qualunque", Map.of())).provider()).isEqualTo("mock");
        });
    }

    @Test
    void shouldBuildRestProvidersFromTheConfiguration() {
        runner
            .withPropertyValues(
                "platform.ai.mock.enabled=false",
                "platform.ai.rest.ml.base-url=https://ml-service.test",
                "platform.ai.rest.ml.tasks=skin-lesion",
                "platform.ai.rest.ml.read-timeout=5s"
            )
            .run(context -> {
                InferenceProviderRegistry registry = context.getBean(InferenceProviderRegistry.class);
                assertThat(registry.names()).containsExactly("ml");
                assertThat(registry.require("ml").supports("skin-lesion")).isTrue();
                assertThat(registry.require("ml").supports("traduzione")).isFalse();
                assertThat(context.getBean(AiMlProperties.class).getRest().get("ml").getReadTimeout().toSeconds()).isEqualTo(5);
            });
    }

    @Test
    void shouldCollectProvidersDeclaredByTheApplication() {
        runner
            .withUserConfiguration(CustomProviderConfiguration.class)
            .withPropertyValues("platform.ai.default-provider=applicativo")
            .run(context -> {
                InferenceProviderRegistry registry = context.getBean(InferenceProviderRegistry.class);
                assertThat(registry.names()).containsExactly("applicativo", "mock");
                assertThat(registry.defaultProvider()).isPresent();
                assertThat(registry.forTask("skin-lesion").name()).isEqualTo("applicativo");
            });
    }

    @Test
    void shouldBackOffWhenDisabled() {
        runner
            .withPropertyValues("platform.ai.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(InferenceProviderRegistry.class));
    }

    @Test
    void shouldLetTheApplicationOverrideTheRegistry() {
        runner.withUserConfiguration(CustomRegistryConfiguration.class).run(context -> {
            assertThat(context).hasSingleBean(InferenceProviderRegistry.class);
            assertThat(context.getBean(InferenceProviderRegistry.class).names()).isEmpty();
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomProviderConfiguration {

        @Bean
        InferenceProvider applicationProvider() {
            return new InferenceProvider() {
                @Override
                public String name() {
                    return "applicativo";
                }

                @Override
                public boolean supports(String task) {
                    return true;
                }

                @Override
                public InferenceResponse infer(InferenceRequest request) {
                    return new InferenceResponse(name(), request.task(), List.of(), Map.of());
                }
            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomRegistryConfiguration {

        @Bean
        InferenceProviderRegistry inferenceProviderRegistry() {
            return new InferenceProviderRegistry(List.of(), null);
        }
    }
}
