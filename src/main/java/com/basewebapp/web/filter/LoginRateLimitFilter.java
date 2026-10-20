package com.basewebapp.web.filter;

import com.basewebapp.management.SecurityMetersService;
import com.basewebapp.security.LoginRateLimiter;
import com.basewebapp.web.rest.errors.ErrorConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Applica il {@link LoginRateLimiter} agli endpoint di autenticazione (hardening B2b).
 *
 * <p>Oltre la soglia risponde <strong>429 Too Many Requests</strong> con header
 * {@code Retry-After} e corpo {@code application/problem+json} (RFC 7807, coerente con
 * {@code docs/convenzioni-api.md} §2), senza invocare il resto della catena. Sotto soglia lascia
 * passare la richiesta e classifica l'esito: risposta 2xx → contatore azzerato, qualunque altro
 * esito → tentativo fallito.</p>
 *
 * <p>Protegge solo le richieste <strong>POST</strong> ai percorsi configurati
 * ({@code application.security.login-rate-limit.paths}): il {@code GET /api/authenticate}, che
 * verifica solo la sessione corrente, non è interessato.</p>
 */
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(LoginRateLimitFilter.class);

    private static final String UNKNOWN_CLIENT = "unknown";

    private final LoginRateLimiter loginRateLimiter;

    private final SecurityMetersService securityMetersService;

    public LoginRateLimitFilter(LoginRateLimiter loginRateLimiter, SecurityMetersService securityMetersService) {
        this.loginRateLimiter = loginRateLimiter;
        this.securityMetersService = securityMetersService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        Optional<String> protectedPath = protectedPath(request);
        if (protectedPath.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientId = clientId(request);
        Optional<Duration> retryAfter = loginRateLimiter.retryAfter(clientId);
        if (retryAfter.isPresent()) {
            securityMetersService.trackLoginRateLimited();
            LOG.warn("Rate limit di autenticazione superato dal client {} su {}: richiesta rifiutata", clientId, protectedPath.get());
            rejectWithTooManyRequests(response, protectedPath.get(), retryAfter.get());
            return;
        }

        try {
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException e) {
            // Una richiesta che fa esplodere la catena non è un'autenticazione riuscita.
            loginRateLimiter.recordFailure(clientId);
            throw e;
        }

        if (isSuccessful(response)) {
            loginRateLimiter.reset(clientId);
        } else {
            loginRateLimiter.recordFailure(clientId);
        }
    }

    /** Vero per uno status 2xx. Confronto sulla classe di status: accetta anche codici non standard. */
    private boolean isSuccessful(HttpServletResponse response) {
        return response.getStatus() / 100 == 2;
    }

    /** Il percorso configurato che questa richiesta colpisce, se protetto dal rate limit. */
    private Optional<String> protectedPath(HttpServletRequest request) {
        if (!loginRateLimiter.isEnabled() || !HttpMethod.POST.matches(request.getMethod())) {
            return Optional.empty();
        }
        List<String> paths = loginRateLimiter.protectedPaths();
        if (paths.isEmpty()) {
            return Optional.empty();
        }
        // Request URI includes the contextPath if any, removed it.
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return paths.stream().filter(path::equals).findFirst();
    }

    /**
     * Chiave del client: l'indirizzo remoto della richiesta. Dietro reverse proxy va configurato
     * {@code server.forward-headers-strategy}, altrimenti tutti i client condividono l'indirizzo
     * del proxy (vedi {@code docs/sicurezza.md}).
     */
    private String clientId(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr == null || remoteAddr.isBlank() ? UNKNOWN_CLIENT : remoteAddr;
    }

    private void rejectWithTooManyRequests(HttpServletResponse response, String path, Duration retryAfter) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds(retryAfter)));
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // Corpo costruito a mano con soli valori noti (nessun input utente): niente da escapare.
        response.getWriter().write(
            """
            {"type":"%s","title":"Too Many Requests","status":%d,\
            "detail":"Too many authentication attempts, please try again later.","path":"%s"}""".formatted(
                ErrorConstants.TOO_MANY_REQUESTS_TYPE,
                HttpStatus.TOO_MANY_REQUESTS.value(),
                path
            )
        );
    }

    /** {@code Retry-After} è in secondi interi: si arrotonda per eccesso, con un minimo di 1. */
    private long retryAfterSeconds(Duration retryAfter) {
        return Math.max(1, (retryAfter.toMillis() + 999) / 1000);
    }
}
