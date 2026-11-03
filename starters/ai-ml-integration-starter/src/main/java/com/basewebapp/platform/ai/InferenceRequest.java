package com.basewebapp.platform.ai;

import java.util.Map;
import java.util.Objects;

/**
 * Richiesta di inferenza, indipendente dal provider che la servirà.
 *
 * @param task identifica <em>cosa</em> si chiede (es. {@code "skin-lesion-classification"}); i
 *             provider dichiarano i task che sanno servire
 * @param inputs payload applicativo (es. {@code image} in base64, testo, feature numeriche)
 * @param metadata informazioni di contorno non interpretate dal contratto (es. id correlazione,
 *                 versione del modello richiesta)
 */
public record InferenceRequest(String task, Map<String, Object> inputs, Map<String, String> metadata) {
    public InferenceRequest {
        Objects.requireNonNull(task, "task");
        inputs = inputs == null ? Map.of() : Map.copyOf(inputs);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static InferenceRequest of(String task, Map<String, Object> inputs) {
        return new InferenceRequest(task, inputs, Map.of());
    }
}
