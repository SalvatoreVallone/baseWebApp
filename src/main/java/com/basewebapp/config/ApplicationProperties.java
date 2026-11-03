package com.basewebapp.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties specific to Base Web App.
 * <p>
 * Properties are configured in the {@code application.yml} file.
 * See {@link tech.jhipster.config.JHipsterProperties} for a good example.
 */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {

    private final Liquibase liquibase = new Liquibase();

    private final Security security = new Security();

    // jhipster-needle-application-properties-property

    public Liquibase getLiquibase() {
        return liquibase;
    }

    public Security getSecurity() {
        return security;
    }

    // jhipster-needle-application-properties-property-getter

    public static class Liquibase {

        private Boolean asyncStart = true;

        public Boolean getAsyncStart() {
            return asyncStart;
        }

        public void setAsyncStart(Boolean asyncStart) {
            this.asyncStart = asyncStart;
        }
    }

    /**
     * Proprietà di sicurezza proprie della piattaforma base (non coperte da
     * {@code jhipster.security.*}). Vedi {@code docs/sicurezza.md}.
     */
    public static class Security {

        private final LoginRateLimit loginRateLimit = new LoginRateLimit();

        public LoginRateLimit getLoginRateLimit() {
            return loginRateLimit;
        }

        /**
         * Rate limit sugli endpoint di autenticazione (hardening B2b): mitiga brute-force e
         * credential stuffing contando i tentativi falliti per client in una finestra temporale.
         */
        public static class LoginRateLimit {

            /** Attiva o disattiva il rate limit (utile per test e ambienti di sviluppo). */
            private boolean enabled = true;

            /** Tentativi falliti consentiti per client nella finestra, prima del 429. */
            private int maxAttempts = 5;

            /** Ampiezza della finestra di conteggio. */
            private Duration window = Duration.ofMinutes(5);

            /** Tetto ai client tracciati in memoria, a protezione dell'occupazione di heap. */
            private int maxTrackedClients = 10_000;

            /** Percorsi (metodo POST) protetti dal rate limit. */
            private List<String> paths = new ArrayList<>(List.of("/api/authenticate"));

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public int getMaxAttempts() {
                return maxAttempts;
            }

            public void setMaxAttempts(int maxAttempts) {
                this.maxAttempts = maxAttempts;
            }

            public Duration getWindow() {
                return window;
            }

            public void setWindow(Duration window) {
                this.window = window;
            }

            public int getMaxTrackedClients() {
                return maxTrackedClients;
            }

            public void setMaxTrackedClients(int maxTrackedClients) {
                this.maxTrackedClients = maxTrackedClients;
            }

            public List<String> getPaths() {
                return paths;
            }

            public void setPaths(List<String> paths) {
                this.paths = paths;
            }
        }
    }

    // jhipster-needle-application-properties-property-class
}
