package com.basewebapp.management;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class SecurityMetersService {

    public static final String INVALID_TOKENS_METER_NAME = "security.authentication.invalid-tokens";
    public static final String INVALID_TOKENS_METER_DESCRIPTION =
        "Indicates validation error count of the tokens presented by the clients.";
    public static final String INVALID_TOKENS_METER_BASE_UNIT = "errors";
    public static final String INVALID_TOKENS_METER_CAUSE_DIMENSION = "cause";

    public static final String RATE_LIMITED_LOGINS_METER_NAME = "security.authentication.rate-limited";
    public static final String RATE_LIMITED_LOGINS_METER_DESCRIPTION =
        "Indicates the count of authentication requests rejected by the login rate limiter.";
    public static final String RATE_LIMITED_LOGINS_METER_BASE_UNIT = "requests";

    private final Counter tokenInvalidSignatureCounter;
    private final Counter tokenExpiredCounter;
    private final Counter tokenUnsupportedCounter;
    private final Counter tokenMalformedCounter;
    private final Counter rateLimitedLoginsCounter;

    public SecurityMetersService(MeterRegistry registry) {
        this.tokenInvalidSignatureCounter = invalidTokensCounterForCauseBuilder("invalid-signature").register(registry);
        this.tokenExpiredCounter = invalidTokensCounterForCauseBuilder("expired").register(registry);
        this.tokenUnsupportedCounter = invalidTokensCounterForCauseBuilder("unsupported").register(registry);
        this.tokenMalformedCounter = invalidTokensCounterForCauseBuilder("malformed").register(registry);
        this.rateLimitedLoginsCounter = Counter.builder(RATE_LIMITED_LOGINS_METER_NAME)
            .baseUnit(RATE_LIMITED_LOGINS_METER_BASE_UNIT)
            .description(RATE_LIMITED_LOGINS_METER_DESCRIPTION)
            .register(registry);
    }

    private Counter.Builder invalidTokensCounterForCauseBuilder(String cause) {
        return Counter.builder(INVALID_TOKENS_METER_NAME)
            .baseUnit(INVALID_TOKENS_METER_BASE_UNIT)
            .description(INVALID_TOKENS_METER_DESCRIPTION)
            .tag(INVALID_TOKENS_METER_CAUSE_DIMENSION, cause);
    }

    public void trackTokenInvalidSignature() {
        this.tokenInvalidSignatureCounter.increment();
    }

    public void trackTokenExpired() {
        this.tokenExpiredCounter.increment();
    }

    public void trackTokenUnsupported() {
        this.tokenUnsupportedCounter.increment();
    }

    public void trackTokenMalformed() {
        this.tokenMalformedCounter.increment();
    }

    /** Una richiesta di autenticazione respinta dal rate limit (hardening B2b). */
    public void trackLoginRateLimited() {
        this.rateLimitedLoginsCounter.increment();
    }
}
