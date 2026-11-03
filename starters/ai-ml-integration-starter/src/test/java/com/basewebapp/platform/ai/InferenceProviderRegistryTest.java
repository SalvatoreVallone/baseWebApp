package com.basewebapp.platform.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InferenceProviderRegistryTest {

    private static final InferenceProvider SKIN = provider("skin", Set.of("skin-lesion"));

    private static final InferenceProvider ANY = provider("qualunque", Set.of());

    @Test
    void shouldFindProvidersByName() {
        InferenceProviderRegistry registry = new InferenceProviderRegistry(List.of(SKIN, ANY), null);

        assertThat(registry.names()).containsExactly("skin", "qualunque");
        assertThat(registry.find("skin")).hasValue(SKIN);
        assertThat(registry.find("inesistente")).isEmpty();
        assertThat(registry.require("skin")).isSameAs(SKIN);
    }

    @Test
    void shouldFailWithAClearMessageWhenTheNameIsUnknown() {
        InferenceProviderRegistry registry = new InferenceProviderRegistry(List.of(SKIN), null);

        assertThatThrownBy(() -> registry.require("inesistente"))
            .isInstanceOf(ProviderNotFoundException.class)
            .hasMessageContaining("inesistente")
            .hasMessageContaining("[skin]");
    }

    @Test
    void shouldSelectTheFirstProviderSupportingTheTask() {
        InferenceProviderRegistry registry = new InferenceProviderRegistry(List.of(SKIN, ANY), null);

        assertThat(registry.forTask("skin-lesion")).isSameAs(SKIN);
        assertThat(registry.forTask("altro")).isSameAs(ANY);
    }

    @Test
    void shouldPreferTheDefaultProviderForATask() {
        InferenceProviderRegistry registry = new InferenceProviderRegistry(List.of(SKIN, ANY), "qualunque");

        assertThat(registry.defaultProvider()).hasValue(ANY);
        assertThat(registry.forTask("skin-lesion")).as("il default supporta ogni task e viene valutato per primo").isSameAs(ANY);
    }

    @Test
    void shouldRejectADefaultProviderThatDoesNotExist() {
        assertThatThrownBy(() -> new InferenceProviderRegistry(List.of(SKIN), "assente"))
            .isInstanceOf(ProviderNotFoundException.class)
            .hasMessageContaining("default-provider");
    }

    @Test
    void shouldFailWhenNoProviderSupportsTheTask() {
        InferenceProviderRegistry registry = new InferenceProviderRegistry(List.of(SKIN), null);

        assertThatThrownBy(() -> registry.forTask("traduzione"))
            .isInstanceOf(ProviderNotFoundException.class)
            .hasMessageContaining("traduzione");
    }

    @Test
    void shouldKeepTheFirstProviderWhenNamesCollide() {
        InferenceProvider duplicate = provider("skin", Set.of("skin-lesion"));
        InferenceProviderRegistry registry = new InferenceProviderRegistry(List.of(SKIN, duplicate), null);

        assertThat(registry.names()).containsExactly("skin");
        assertThat(registry.require("skin")).isSameAs(SKIN);
    }

    @Test
    void shouldInferOnTheProviderMatchingTheRequestTask() {
        InferenceProviderRegistry registry = new InferenceProviderRegistry(List.of(SKIN, ANY), null);

        assertThat(registry.infer(InferenceRequest.of("skin-lesion", Map.of())).provider()).isEqualTo("skin");
    }

    private static InferenceProvider provider(String name, Set<String> tasks) {
        return new InferenceProvider() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public boolean supports(String task) {
                return tasks.isEmpty() || tasks.contains(task);
            }

            @Override
            public InferenceResponse infer(InferenceRequest request) {
                return new InferenceResponse(name, request.task(), List.of(new Prediction("ok", 1.0)), Map.of());
            }
        };
    }
}
