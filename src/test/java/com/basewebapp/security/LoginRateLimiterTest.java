package com.basewebapp.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.basewebapp.config.ApplicationProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit test del {@link LoginRateLimiter}: tempo controllato da un {@link Clock} mutabile, nessun
 * contesto Spring.
 */
class LoginRateLimiterTest {

    private static final String CLIENT = "203.0.113.7";

    private static final Duration WINDOW = Duration.ofMinutes(5);

    private static final int MAX_ATTEMPTS = 5;

    private MutableClock clock;

    private ApplicationProperties applicationProperties;

    private ApplicationProperties.Security.LoginRateLimit config;

    private LoginRateLimiter rateLimiter;

    @BeforeEach
    void setup() {
        clock = new MutableClock(Instant.parse("2026-10-20T10:00:00Z"));
        applicationProperties = new ApplicationProperties();
        config = applicationProperties.getSecurity().getLoginRateLimit();
        config.setMaxAttempts(MAX_ATTEMPTS);
        config.setWindow(WINDOW);
        rateLimiter = new LoginRateLimiter(applicationProperties, clock);
    }

    @Test
    void shouldAllowAttemptsBelowThreshold() {
        for (int i = 0; i < MAX_ATTEMPTS - 1; i++) {
            rateLimiter.recordFailure(CLIENT);
            assertThat(rateLimiter.retryAfter(CLIENT)).isEmpty();
        }
    }

    @Test
    void shouldBlockOnceThresholdIsReached() {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            rateLimiter.recordFailure(CLIENT);
        }

        assertThat(rateLimiter.retryAfter(CLIENT)).hasValue(WINDOW);
    }

    @Test
    void shouldReportRemainingWindowAsRetryAfter() {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            rateLimiter.recordFailure(CLIENT);
        }
        clock.advance(Duration.ofMinutes(2));

        assertThat(rateLimiter.retryAfter(CLIENT)).hasValue(Duration.ofMinutes(3));
    }

    @Test
    void shouldTrackClientsIndependently() {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            rateLimiter.recordFailure(CLIENT);
        }

        assertThat(rateLimiter.retryAfter(CLIENT)).isPresent();
        assertThat(rateLimiter.retryAfter("198.51.100.4")).isEmpty();
    }

    @Test
    void shouldUnblockWhenWindowExpires() {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            rateLimiter.recordFailure(CLIENT);
        }
        clock.advance(WINDOW);

        assertThat(rateLimiter.retryAfter(CLIENT)).isEmpty();
        assertThat(rateLimiter.trackedClients()).isZero();
    }

    @Test
    void shouldStartANewWindowAfterExpiry() {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            rateLimiter.recordFailure(CLIENT);
        }
        clock.advance(WINDOW.plusSeconds(1));
        rateLimiter.recordFailure(CLIENT);

        assertThat(rateLimiter.retryAfter(CLIENT)).isEmpty();
    }

    @Test
    void shouldResetCounterOnSuccess() {
        for (int i = 0; i < MAX_ATTEMPTS - 1; i++) {
            rateLimiter.recordFailure(CLIENT);
        }
        rateLimiter.reset(CLIENT);

        for (int i = 0; i < MAX_ATTEMPTS - 1; i++) {
            rateLimiter.recordFailure(CLIENT);
        }

        assertThat(rateLimiter.retryAfter(CLIENT)).isEmpty();
        rateLimiter.recordFailure(CLIENT);
        assertThat(rateLimiter.retryAfter(CLIENT)).isPresent();
    }

    @Test
    void shouldNeverBlockWhenDisabled() {
        config.setEnabled(false);

        for (int i = 0; i < MAX_ATTEMPTS * 3; i++) {
            rateLimiter.recordFailure(CLIENT);
        }

        assertThat(rateLimiter.retryAfter(CLIENT)).isEmpty();
        assertThat(rateLimiter.trackedClients()).isZero();
    }

    @Test
    void shouldCapTheNumberOfTrackedClients() {
        config.setMaxTrackedClients(3);

        for (int i = 0; i < 50; i++) {
            clock.advance(Duration.ofSeconds(1));
            rateLimiter.recordFailure("198.51.100." + i);
        }

        assertThat(rateLimiter.trackedClients()).isLessThanOrEqualTo(3);
    }

    @Test
    void shouldKeepTheMostRecentClientsWhenEvicting() {
        config.setMaxTrackedClients(1);

        rateLimiter.recordFailure("198.51.100.1");
        clock.advance(Duration.ofSeconds(1));
        rateLimiter.recordFailure("198.51.100.2");

        assertThat(rateLimiter.trackedClients()).isEqualTo(1);
        config.setMaxAttempts(1);
        assertThat(rateLimiter.retryAfter("198.51.100.2")).isPresent();
        assertThat(rateLimiter.retryAfter("198.51.100.1")).isEmpty();
    }

    /** {@link Clock} avanzabile a mano, per rendere deterministici i test sulla finestra. */
    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
