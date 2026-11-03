package com.basewebapp.platform.ai;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Provider deterministico per sviluppo e test: non chiama nessun modello e restituisce le
 * predizioni configurate in {@code platform.ai.mock.predictions}.
 *
 * <p>Serve a sviluppare l'applicazione (e a scriverne i test) senza dipendere da un servizio di
 * inferenza avviato. In produzione va disattivato con {@code platform.ai.mock.enabled=false}.</p>
 */
public class MockInferenceProvider implements InferenceProvider {

    /** Usate quando non è configurata nessuna predizione. */
    public static final Map<String, Double> DEFAULT_PREDICTIONS = Map.of("mock-label", 1.0);

    private final AiMlProperties.Mock config;

    public MockInferenceProvider(AiMlProperties.Mock config) {
        this.config = config;
    }

    @Override
    public String name() {
        return config.getName();
    }

    @Override
    public boolean supports(String task) {
        return config.getTasks().isEmpty() || config.getTasks().contains(task);
    }

    @Override
    public InferenceResponse infer(InferenceRequest request) {
        if (!supports(request.task())) {
            throw new InferenceException(
                "Il provider '%s' non supporta il task '%s' (task configurati: %s)".formatted(name(), request.task(), config.getTasks())
            );
        }
        Map<String, Double> configured = config.getPredictions().isEmpty() ? DEFAULT_PREDICTIONS : config.getPredictions();
        List<Prediction> predictions = configured
            .entrySet()
            .stream()
            .map(entry -> new Prediction(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparingDouble(Prediction::score).reversed())
            .toList();
        return new InferenceResponse(name(), request.task(), predictions, Map.of("mock", true));
    }
}
