package com.basewebapp.platform.ai;

/** Inferenza non completata: provider non raggiungibile, in errore o con payload inatteso. */
public class InferenceException extends RuntimeException {

    public InferenceException(String message) {
        super(message);
    }

    public InferenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
