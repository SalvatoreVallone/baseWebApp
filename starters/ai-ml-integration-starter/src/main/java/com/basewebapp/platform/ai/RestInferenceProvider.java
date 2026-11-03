package com.basewebapp.platform.ai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adapter verso un servizio di inferenza HTTP (es. il {@code ml-service} FastAPI di skin-lesion).
 *
 * <p><strong>Contratto HTTP atteso.</strong> {@code POST <base-url><path>} con corpo JSON:</p>
 * <pre>{"task": "...", "inputs": { ... }, "metadata": { ... }}</pre>
 * <p>e risposta JSON che contenga un array {@code predictions} di oggetti con {@code label} e
 * {@code score}:</p>
 * <pre>{"predictions": [{"label": "melanoma", "score": 0.93}], "model_version": "1.2.0"}</pre>
 * <p>I campi fuori contratto (qui {@code model_version}) restano disponibili in
 * {@link InferenceResponse#raw()}. Un servizio che non rispetta questa forma si integra scrivendo
 * un {@link InferenceProvider} dedicato nell'applicazione, non piegando questo adapter.</p>
 */
public class RestInferenceProvider implements InferenceProvider {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON_OBJECT = new ParameterizedTypeReference<>() {};

    private static final String PREDICTIONS_FIELD = "predictions";

    private final String name;

    private final AiMlProperties.Rest config;

    private final RestClient restClient;

    public RestInferenceProvider(String name, AiMlProperties.Rest config, RestClient.Builder restClientBuilder) {
        if (!StringUtils.hasText(config.getBaseUrl())) {
            throw new IllegalArgumentException("platform.ai.rest.%s.base-url è obbligatorio".formatted(name));
        }
        this.name = name;
        this.config = config;
        RestClient.Builder builder = restClientBuilder.baseUrl(config.getBaseUrl());
        if (StringUtils.hasText(config.getApiKey())) {
            builder = builder.defaultHeader(config.getApiKeyHeader(), config.getApiKey());
        }
        this.restClient = builder.build();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean supports(String task) {
        return config.getTasks().isEmpty() || config.getTasks().contains(task);
    }

    @Override
    public InferenceResponse infer(InferenceRequest request) {
        Map<String, Object> payload = Map.of("task", request.task(), "inputs", request.inputs(), "metadata", request.metadata());
        Map<String, Object> body;
        try {
            body = restClient
                .post()
                .uri(config.getPath())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(JSON_OBJECT);
        } catch (RestClientException e) {
            // Il messaggio del provider può contenere dettagli del servizio remoto: non si espone
            // all'utente finale, ma serve nei log per diagnosticare.
            throw new InferenceException("Inferenza fallita sul provider '%s' (task '%s')".formatted(name, request.task()), e);
        }
        if (body == null) {
            throw new InferenceException("Il provider '%s' ha risposto con un corpo vuoto".formatted(name));
        }
        return new InferenceResponse(name, request.task(), parsePredictions(body), rawWithoutPredictions(body));
    }

    private List<Prediction> parsePredictions(Map<String, Object> body) {
        Object predictions = body.get(PREDICTIONS_FIELD);
        if (predictions == null) {
            throw new InferenceException("La risposta del provider '%s' non contiene il campo '%s'".formatted(name, PREDICTIONS_FIELD));
        }
        if (!(predictions instanceof List<?> entries)) {
            throw new InferenceException("Il campo '%s' del provider '%s' non è un array".formatted(PREDICTIONS_FIELD, name));
        }
        List<Prediction> parsed = new ArrayList<>(entries.size());
        for (Object entry : entries) {
            if (!(entry instanceof Map<?, ?> prediction)) {
                throw new InferenceException("Elemento di '%s' non valido dal provider '%s': %s".formatted(PREDICTIONS_FIELD, name, entry));
            }
            Object label = prediction.get("label");
            Object score = prediction.get("score");
            if (label == null || !(score instanceof Number numericScore)) {
                throw new InferenceException(
                    "Predizione senza 'label' o con 'score' non numerico dal provider '%s': %s".formatted(name, prediction)
                );
            }
            parsed.add(new Prediction(label.toString(), numericScore.doubleValue()));
        }
        return parsed;
    }

    private Map<String, Object> rawWithoutPredictions(Map<String, Object> body) {
        Map<String, Object> raw = new LinkedHashMap<>(body);
        raw.remove(PREDICTIONS_FIELD);
        return raw;
    }
}
