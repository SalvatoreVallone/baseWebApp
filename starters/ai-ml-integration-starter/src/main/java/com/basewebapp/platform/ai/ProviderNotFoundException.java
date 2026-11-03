package com.basewebapp.platform.ai;

/** Nessun provider configurato con quel nome, o nessuno capace di servire quel task. */
public class ProviderNotFoundException extends InferenceException {

    public ProviderNotFoundException(String message) {
        super(message);
    }
}
