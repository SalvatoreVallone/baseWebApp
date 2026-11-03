package com.basewebapp.security;

import com.basewebapp.config.ApplicationProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Rate limit dei tentativi di autenticazione falliti, per client (hardening B2b).
 *
 * <p>Conta i tentativi <em>falliti</em> in una finestra temporale a lunghezza fissa: superata la
 * soglia, il client resta bloccato fino alla scadenza della finestra. Un'autenticazione riuscita
 * azzera il contatore, così un utente legittimo non viene mai penalizzato dai propri accessi.</p>
 *
 * <p><strong>Limiti noti</strong> (volutamente accettati, vedi {@code docs/sicurezza.md}):</p>
 * <ul>
 *   <li>Stato <strong>in memoria</strong>: il conteggio è per-istanza e si perde al restart. Con più
 *       istanze dietro load balancer la soglia effettiva si moltiplica: in quello scenario va
 *       applicato un rate limit anche al reverse proxy o uno store condiviso.</li>
 *   <li>La chiave è l'<strong>indirizzo del client</strong>: client dietro lo stesso NAT condividono
 *       la soglia, e dietro reverse proxy serve {@code server.forward-headers-strategy} perché
 *       l'indirizzo visto non sia quello del proxy.</li>
 *   <li>Il tetto {@code max-tracked-clients} protegge l'heap, ma sotto flood di chiavi diverse può
 *       far evacuare anticipatamente finestre attive.</li>
 * </ul>
 */
@Service
public class LoginRateLimiter {

    private final ApplicationProperties.Security.LoginRateLimit config;

    private final Clock clock;

    private final ConcurrentMap<String, AttemptWindow> windows = new ConcurrentHashMap<>();

    // @Autowired esplicito: la classe ha due costruttori (l'altro inietta il Clock nei test) e
    // senza annotazione Spring non saprebbe quale usare.
    @Autowired
    public LoginRateLimiter(ApplicationProperties applicationProperties) {
        this(applicationProperties, Clock.systemUTC());
    }

    LoginRateLimiter(ApplicationProperties applicationProperties, Clock clock) {
        this.config = applicationProperties.getSecurity().getLoginRateLimit();
        this.clock = clock;
    }

    /** Se il rate limit è attivo. */
    public boolean isEnabled() {
        return config.isEnabled();
    }

    /** I percorsi (metodo POST) da sottoporre a rate limit. */
    public List<String> protectedPaths() {
        List<String> paths = config.getPaths();
        return paths == null ? List.of() : List.copyOf(paths);
    }

    /**
     * Quanto il client deve attendere prima di poter ritentare.
     *
     * @param clientId chiave del client (tipicamente il suo indirizzo IP)
     * @return {@link Optional#empty()} se la richiesta è consentita, altrimenti il tempo residuo
     *         della finestra da esporre come {@code Retry-After}
     */
    public Optional<Duration> retryAfter(String clientId) {
        if (!config.isEnabled()) {
            return Optional.empty();
        }
        AttemptWindow window = windows.get(clientId);
        if (window == null) {
            return Optional.empty();
        }
        Instant now = clock.instant();
        if (isExpired(window, now)) {
            windows.remove(clientId, window);
            return Optional.empty();
        }
        if (window.count() < config.getMaxAttempts()) {
            return Optional.empty();
        }
        Duration remaining = config.getWindow().minus(Duration.between(window.startedAt(), now));
        return Optional.of(remaining.isNegative() ? Duration.ZERO : remaining);
    }

    /** Registra un tentativo di autenticazione fallito per il client. */
    public void recordFailure(String clientId) {
        if (!config.isEnabled()) {
            return;
        }
        Instant now = clock.instant();
        windows.compute(clientId, (key, window) ->
            window == null || isExpired(window, now) ? new AttemptWindow(now, 1) : new AttemptWindow(window.startedAt(), window.count() + 1)
        );
        evictIfNeeded(now);
    }

    /** Azzera il conteggio del client, dopo un'autenticazione riuscita. */
    public void reset(String clientId) {
        windows.remove(clientId);
    }

    /** Numero di client attualmente tracciati (diagnostica e test). */
    int trackedClients() {
        return windows.size();
    }

    private boolean isExpired(AttemptWindow window, Instant now) {
        return !now.isBefore(window.startedAt().plus(config.getWindow()));
    }

    /**
     * Mantiene la mappa sotto il tetto configurato: prima scarta le finestre scadute, poi, se non
     * basta, quelle iniziate da più tempo (le più vicine alla scadenza).
     */
    private void evictIfNeeded(Instant now) {
        int maxTracked = config.getMaxTrackedClients();
        if (maxTracked <= 0 || windows.size() <= maxTracked) {
            return;
        }
        windows.entrySet().removeIf(entry -> isExpired(entry.getValue(), now));
        int excess = windows.size() - maxTracked;
        if (excess <= 0) {
            return;
        }
        windows
            .entrySet()
            .stream()
            .sorted(Comparator.comparing(entry -> entry.getValue().startedAt()))
            .limit(excess)
            .map(Map.Entry::getKey)
            .toList()
            .forEach(windows::remove);
    }

    /** Finestra di conteggio immutabile: istante di inizio e tentativi falliti accumulati. */
    private record AttemptWindow(Instant startedAt, int count) {}
}
