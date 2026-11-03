package com.basewebapp.platform.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Verifica l'adapter REST contro un servizio di inferenza finto ({@link MockRestServiceServer}):
 * nessun servizio reale, nessun container.
 */
class RestInferenceProviderTest {

    private static final String BASE_URL = "https://ml-service.test";

    private AiMlProperties.Rest config;

    private RestClient.Builder builder;

    private MockRestServiceServer server;

    @BeforeEach
    void setup() {
        config = new AiMlProperties.Rest();
        config.setBaseUrl(BASE_URL);
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
    }

    @Test
    void shouldSendTheContractPayloadAndParseThePredictions() {
        server
            .expect(requestTo(BASE_URL + "/infer"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.task").value("skin-lesion"))
            .andExpect(jsonPath("$.inputs.image").value("base64..."))
            .andExpect(jsonPath("$.metadata.correlationId").value("abc"))
            .andRespond(
                withSuccess(
                    """
                    {"predictions":[{"label":"melanoma","score":0.93},{"label":"nevus","score":0.07}],"model_version":"1.2.0"}""",
                    MediaType.APPLICATION_JSON
                )
            );

        InferenceResponse response = provider().infer(
            new InferenceRequest("skin-lesion", Map.of("image", "base64..."), Map.of("correlationId", "abc"))
        );

        server.verify();
        assertThat(response.provider()).isEqualTo("ml");
        assertThat(response.predictions()).containsExactly(new Prediction("melanoma", 0.93), new Prediction("nevus", 0.07));
        assertThat(response.best()).hasValue(new Prediction("melanoma", 0.93));
        assertThat(response.raw()).containsEntry("model_version", "1.2.0").doesNotContainKey("predictions");
    }

    @Test
    void shouldCallTheConfiguredPathAndSendTheApiKey() {
        config.setPath("/v1/classify");
        config.setApiKeyHeader("X-Model-Key");
        config.setApiKey("segreto");
        server
            .expect(requestTo(BASE_URL + "/v1/classify"))
            .andExpect(header("X-Model-Key", "segreto"))
            .andRespond(
                withSuccess(
                    """
                    {"predictions":[]}""",
                    MediaType.APPLICATION_JSON
                )
            );

        provider().infer(InferenceRequest.of("skin-lesion", Map.of()));

        server.verify();
    }

    @Test
    void shouldWrapTransportAndServerErrorsIntoInferenceException() {
        server.expect(requestTo(BASE_URL + "/infer")).andRespond(withServerError());

        assertThatThrownBy(() -> provider().infer(InferenceRequest.of("skin-lesion", Map.of())))
            .isInstanceOf(InferenceException.class)
            .hasMessageContaining("Inferenza fallita sul provider 'ml'");
    }

    @Test
    void shouldRejectAResponseWithoutPredictions() {
        server.expect(requestTo(BASE_URL + "/infer")).andRespond(
            withSuccess(
                """
                {"model_version":"1.2.0"}""",
                MediaType.APPLICATION_JSON
            )
        );

        assertThatThrownBy(() -> provider().infer(InferenceRequest.of("skin-lesion", Map.of())))
            .isInstanceOf(InferenceException.class)
            .hasMessageContaining("non contiene il campo 'predictions'");
    }

    @Test
    void shouldRejectMalformedPredictions() {
        server.expect(requestTo(BASE_URL + "/infer")).andRespond(
            withSuccess(
                """
                {"predictions":[{"label":"melanoma","score":"alto"}]}""",
                MediaType.APPLICATION_JSON
            )
        );

        assertThatThrownBy(() -> provider().infer(InferenceRequest.of("skin-lesion", Map.of())))
            .isInstanceOf(InferenceException.class)
            .hasMessageContaining("'score' non numerico");
    }

    @Test
    void shouldHonourTheConfiguredTasks() {
        config.setTasks(Set.of("skin-lesion"));

        assertThat(provider().supports("skin-lesion")).isTrue();
        assertThat(provider().supports("traduzione")).isFalse();
    }

    @Test
    void shouldRequireABaseUrl() {
        AiMlProperties.Rest incomplete = new AiMlProperties.Rest();

        assertThatThrownBy(() -> new RestInferenceProvider("ml", incomplete, RestClient.builder()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("base-url");
    }

    private RestInferenceProvider provider() {
        return new RestInferenceProvider("ml", config, builder);
    }
}
