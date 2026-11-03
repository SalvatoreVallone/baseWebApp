package com.basewebapp.platform.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MockInferenceProviderTest {

    private final AiMlProperties.Mock config = new AiMlProperties.Mock();

    @Test
    void shouldReturnConfiguredPredictionsSortedByScore() {
        Map<String, Double> predictions = new LinkedHashMap<>();
        predictions.put("nevus", 0.2);
        predictions.put("melanoma", 0.8);
        config.setPredictions(predictions);

        InferenceResponse response = new MockInferenceProvider(config).infer(InferenceRequest.of("skin-lesion", Map.of()));

        assertThat(response.provider()).isEqualTo("mock");
        assertThat(response.task()).isEqualTo("skin-lesion");
        assertThat(response.predictions()).extracting(Prediction::label).containsExactly("melanoma", "nevus");
        assertThat(response.best()).hasValue(new Prediction("melanoma", 0.8));
        assertThat(response.raw()).containsEntry("mock", true);
    }

    @Test
    void shouldFallBackToAPlaceholderPredictionWhenNoneIsConfigured() {
        InferenceResponse response = new MockInferenceProvider(config).infer(InferenceRequest.of("skin-lesion", Map.of()));

        assertThat(response.predictions())
            .extracting(Prediction::label)
            .containsExactlyElementsOf(MockInferenceProvider.DEFAULT_PREDICTIONS.keySet());
    }

    @Test
    void shouldReplaceAndNotMergeTheDefaultPrediction() {
        config.setPredictions(Map.of("melanoma", 0.9));

        InferenceResponse response = new MockInferenceProvider(config).infer(InferenceRequest.of("skin-lesion", Map.of()));

        assertThat(response.predictions()).extracting(Prediction::label).containsExactly("melanoma");
    }

    @Test
    void shouldSupportEveryTaskWhenNoneIsConfigured() {
        assertThat(new MockInferenceProvider(config).supports("qualunque-task")).isTrue();
    }

    @Test
    void shouldSupportOnlyTheConfiguredTasks() {
        config.setTasks(Set.of("skin-lesion"));
        MockInferenceProvider provider = new MockInferenceProvider(config);

        assertThat(provider.supports("skin-lesion")).isTrue();
        assertThat(provider.supports("altro")).isFalse();
        assertThatThrownBy(() -> provider.infer(InferenceRequest.of("altro", Map.of())))
            .isInstanceOf(InferenceException.class)
            .hasMessageContaining("non supporta il task 'altro'");
    }

    @Test
    void shouldUseTheConfiguredName() {
        config.setName("finto");

        assertThat(new MockInferenceProvider(config).name()).isEqualTo("finto");
    }
}
