package com.basewebapp.platform.ai;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurazione dell'integrazione AI/ML ({@code platform.ai.*}).
 *
 * <p>I provider si dichiarano qui: zero o più adapter REST sotto {@code platform.ai.rest.<nome>} e
 * il provider mock sotto {@code platform.ai.mock}. Un'applicazione può in alternativa (o in
 * aggiunta) definire propri bean {@link InferenceProvider}.</p>
 */
@ConfigurationProperties(prefix = "platform.ai")
public class AiMlProperties {

    /** Disattiva l'intera integrazione: non viene creato nessun bean dello starter. */
    private boolean enabled = true;

    /** Nome del provider preferito nella ricerca per task. Vuoto = ordine di registrazione. */
    private String defaultProvider;

    /** Provider mock, per sviluppo e test senza un servizio di inferenza. */
    private final Mock mock = new Mock();

    /** Adapter REST verso servizi di inferenza, indicizzati per nome. */
    private Map<String, Rest> rest = new LinkedHashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDefaultProvider() {
        return defaultProvider;
    }

    public void setDefaultProvider(String defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    public Mock getMock() {
        return mock;
    }

    public Map<String, Rest> getRest() {
        return rest;
    }

    public void setRest(Map<String, Rest> rest) {
        this.rest = rest == null ? new LinkedHashMap<>() : rest;
    }

    /** Impostazioni comuni a tutti i provider. */
    public abstract static class ProviderConfig {

        /** Task supportati. Vuoto = il provider accetta qualunque task. */
        private Set<String> tasks = new LinkedHashSet<>();

        public Set<String> getTasks() {
            return tasks;
        }

        public void setTasks(Set<String> tasks) {
            this.tasks = tasks == null ? new LinkedHashSet<>() : tasks;
        }
    }

    public static class Mock extends ProviderConfig {

        /**
         * Il provider mock è attivo per default: la base non ha un servizio di inferenza, quindi
         * senza di esso lo starter non sarebbe utilizzabile appena aggiunto. <strong>Va disattivato
         * in produzione</strong> (vedi README).
         */
        private boolean enabled = true;

        /** Nome con cui il mock è selezionabile. */
        private String name = "mock";

        /**
         * Predizioni restituite (etichetta → punteggio). Vuoto = una predizione segnaposto
         * ({@link MockInferenceProvider#DEFAULT_PREDICTIONS}).
         *
         * <p>Resta volutamente vuoto per default: Spring Boot <em>fonde</em> la configurazione
         * nelle mappe già popolate, quindi un default qui non sarebbe sovrascrivibile e
         * inquinerebbe le predizioni dell'applicazione.</p>
         */
        private Map<String, Double> predictions = new LinkedHashMap<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Map<String, Double> getPredictions() {
            return predictions;
        }

        public void setPredictions(Map<String, Double> predictions) {
            this.predictions = predictions == null ? new LinkedHashMap<>() : predictions;
        }
    }

    public static class Rest extends ProviderConfig {

        /** URL base del servizio di inferenza (es. {@code http://ml-service:8000}). */
        private String baseUrl;

        /** Percorso dell'endpoint di inferenza, relativo a {@code base-url}. */
        private String path = "/infer";

        /** Timeout di connessione. */
        private Duration connectTimeout = Duration.ofSeconds(2);

        /** Timeout di lettura: l'inferenza può essere lenta, va tarata sul modello. */
        private Duration readTimeout = Duration.ofSeconds(30);

        /** Header con cui inviare l'eventuale chiave d'accesso. */
        private String apiKeyHeader = "X-API-Key";

        /** Chiave d'accesso al servizio. Va fornita via variabile d'ambiente, mai committata. */
        private String apiKey;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
        }

        public String getApiKeyHeader() {
            return apiKeyHeader;
        }

        public void setApiKeyHeader(String apiKeyHeader) {
            this.apiKeyHeader = apiKeyHeader;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }
}
