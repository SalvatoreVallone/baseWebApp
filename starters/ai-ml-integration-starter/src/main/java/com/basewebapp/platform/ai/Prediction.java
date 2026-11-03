package com.basewebapp.platform.ai;

import java.util.Objects;

/**
 * Singola ipotesi restituita da un modello.
 *
 * @param label etichetta predetta
 * @param score confidenza associata, tipicamente in {@code [0,1]} (non è un vincolo del contratto:
 *              alcuni modelli restituiscono logit o punteggi non normalizzati)
 */
public record Prediction(String label, double score) {
    public Prediction {
        Objects.requireNonNull(label, "label");
    }
}
