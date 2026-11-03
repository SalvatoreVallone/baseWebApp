package com.basewebapp.platform.ai;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Esito di un'inferenza.
 *
 * @param provider nome del provider che ha servito la richiesta
 * @param task task richiesto
 * @param predictions ipotesi restituite dal modello
 * @param raw risposta grezza del provider, per i campi fuori contratto (versione del modello,
 *            heatmap, tempi...) che le applicazioni possono leggere a proprio rischio
 */
public record InferenceResponse(String provider, String task, List<Prediction> predictions, Map<String, Object> raw) {
    public InferenceResponse {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(task, "task");
        predictions = predictions == null ? List.of() : List.copyOf(predictions);
        raw = raw == null ? Map.of() : Map.copyOf(raw);
    }

    /** L'ipotesi con il punteggio più alto, se presente. */
    public Optional<Prediction> best() {
        return predictions.stream().max(Comparator.comparingDouble(Prediction::score));
    }
}
