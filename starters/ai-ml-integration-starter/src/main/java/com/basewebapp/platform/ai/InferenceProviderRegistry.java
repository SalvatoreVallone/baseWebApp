package com.basewebapp.platform.ai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.util.StringUtils;

/**
 * Catalogo dei provider di inferenza disponibili, con selezione per nome o per task.
 *
 * <p>Il provider di default (se configurato in {@code platform.ai.default-provider}) viene
 * considerato per primo nella ricerca per task, così un'applicazione può cambiare modello senza
 * toccare il codice.</p>
 */
public class InferenceProviderRegistry {

    private final Map<String, InferenceProvider> providersByName = new LinkedHashMap<>();

    private final String defaultProviderName;

    /**
     * @param providers provider disponibili; a parità di nome vince il primo (i bean applicativi
     *                  hanno quindi la precedenza su quelli derivati dalla configurazione)
     * @param defaultProviderName nome del provider preferito, oppure {@code null}
     */
    public InferenceProviderRegistry(List<InferenceProvider> providers, String defaultProviderName) {
        for (InferenceProvider provider : providers) {
            providersByName.putIfAbsent(provider.name(), provider);
        }
        this.defaultProviderName = StringUtils.hasText(defaultProviderName) ? defaultProviderName : null;
        if (this.defaultProviderName != null && !providersByName.containsKey(this.defaultProviderName)) {
            throw new ProviderNotFoundException(
                "platform.ai.default-provider = '%s' non corrisponde a nessun provider configurato: %s".formatted(
                    this.defaultProviderName,
                    names()
                )
            );
        }
    }

    /** I nomi dei provider disponibili, nell'ordine di registrazione. */
    public List<String> names() {
        return List.copyOf(providersByName.keySet());
    }

    public Optional<InferenceProvider> find(String name) {
        return Optional.ofNullable(providersByName.get(name));
    }

    /** Il provider con quel nome. */
    public InferenceProvider require(String name) {
        return find(name).orElseThrow(() ->
            new ProviderNotFoundException("Nessun provider di inferenza chiamato '%s'; disponibili: %s".formatted(name, names()))
        );
    }

    /** Il provider di default, se configurato. */
    public Optional<InferenceProvider> defaultProvider() {
        return defaultProviderName == null ? Optional.empty() : find(defaultProviderName);
    }

    /**
     * Il primo provider capace di servire il task: si valuta il provider di default, poi gli altri
     * nell'ordine di registrazione.
     *
     * @throws ProviderNotFoundException se nessun provider dichiara di supportare il task
     */
    public InferenceProvider forTask(String task) {
        return candidates()
            .stream()
            .filter(provider -> provider.supports(task))
            .findFirst()
            .orElseThrow(() ->
                new ProviderNotFoundException(
                    "Nessun provider di inferenza supporta il task '%s'; disponibili: %s".formatted(task, names())
                )
            );
    }

    /** Scorciatoia: inferenza sul primo provider capace di servire il task della richiesta. */
    public InferenceResponse infer(InferenceRequest request) {
        return forTask(request.task()).infer(request);
    }

    private List<InferenceProvider> candidates() {
        List<InferenceProvider> candidates = new ArrayList<>();
        defaultProvider().ifPresent(candidates::add);
        providersByName
            .values()
            .stream()
            .filter(provider -> !candidates.contains(provider))
            .forEach(candidates::add);
        return candidates;
    }
}
