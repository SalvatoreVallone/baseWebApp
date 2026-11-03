package com.basewebapp.web.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.basewebapp.config.ApplicationProperties;
import com.basewebapp.management.SecurityMetersService;
import com.basewebapp.security.LoginRateLimiter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Unit test del {@link LoginRateLimitFilter} con request/response finte: nessun contesto Spring,
 * nessun database.
 */
class LoginRateLimitFilterTest {

    private static final String LOGIN_PATH = "/api/authenticate";

    private static final String CLIENT = "203.0.113.7";

    private static final int MAX_ATTEMPTS = 5;

    private ApplicationProperties.Security.LoginRateLimit config;

    private MeterRegistry meterRegistry;

    private LoginRateLimitFilter filter;

    @BeforeEach
    void setup() {
        ApplicationProperties applicationProperties = new ApplicationProperties();
        config = applicationProperties.getSecurity().getLoginRateLimit();
        config.setMaxAttempts(MAX_ATTEMPTS);
        meterRegistry = new SimpleMeterRegistry();
        filter = new LoginRateLimitFilter(new LoginRateLimiter(applicationProperties), new SecurityMetersService(meterRegistry));
    }

    @Test
    void shouldLetFailedAttemptsBelowThresholdThrough() throws Exception {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            MockHttpServletResponse response = failingLogin();

            assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        }
    }

    @Test
    void shouldRejectWithProblemDetailsOnceThresholdIsReached() throws Exception {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            failingLogin();
        }

        MockHttpServletRequest request = loginRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingFilterChain chain = new RecordingFilterChain(HttpStatus.UNAUTHORIZED);
        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(chain.invoked).as("la catena non deve essere invocata quando si risponde 429").isFalse();
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(Integer.parseInt(response.getHeader(HttpHeaders.RETRY_AFTER))).isBetween(1, (int) config.getWindow().toSeconds());
        assertThat(response.getContentAsString())
            .contains("\"status\":429")
            .contains("/problem/too-many-requests")
            .contains("\"path\":\"" + LOGIN_PATH + "\"");
        assertThat(meterRegistry.get(SecurityMetersService.RATE_LIMITED_LOGINS_METER_NAME).counter().count()).isEqualTo(1);
    }

    @Test
    void shouldCountClientsSeparately() throws Exception {
        for (int i = 0; i < MAX_ATTEMPTS + 1; i++) {
            failingLogin();
        }

        MockHttpServletRequest otherClient = loginRequest();
        otherClient.setRemoteAddr("198.51.100.4");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(otherClient, response, new RecordingFilterChain(HttpStatus.UNAUTHORIZED));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void shouldForgetAttemptsAfterASuccessfulLogin() throws Exception {
        for (int i = 0; i < MAX_ATTEMPTS - 1; i++) {
            failingLogin();
        }

        MockHttpServletResponse success = new MockHttpServletResponse();
        filter.doFilter(loginRequest(), success, new RecordingFilterChain(HttpStatus.OK));
        assertThat(success.getStatus()).isEqualTo(HttpStatus.OK.value());

        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            assertThat(failingLogin().getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        }
    }

    @Test
    void shouldIgnoreOtherMethodsAndPaths() throws Exception {
        for (int i = 0; i < MAX_ATTEMPTS * 3; i++) {
            MockHttpServletRequest get = loginRequest();
            get.setMethod("GET");
            filter.doFilter(get, new MockHttpServletResponse(), new RecordingFilterChain(HttpStatus.UNAUTHORIZED));

            MockHttpServletRequest otherPath = loginRequest();
            otherPath.setRequestURI("/api/account");
            filter.doFilter(otherPath, new MockHttpServletResponse(), new RecordingFilterChain(HttpStatus.UNAUTHORIZED));
        }

        assertThat(failingLogin().getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void shouldBeInertWhenDisabled() throws Exception {
        config.setEnabled(false);

        for (int i = 0; i < MAX_ATTEMPTS * 3; i++) {
            assertThat(failingLogin().getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        }
    }

    @Test
    void shouldHonourConfiguredPaths() throws Exception {
        config.setPaths(List.of("/api/account/reset-password/init"));

        for (int i = 0; i < MAX_ATTEMPTS * 2; i++) {
            assertThat(failingLogin().getStatus())
                .as("il login non è più fra i percorsi protetti")
                .isEqualTo(HttpStatus.UNAUTHORIZED.value());
        }

        MockHttpServletResponse response = null;
        for (int i = 0; i <= MAX_ATTEMPTS; i++) {
            MockHttpServletRequest request = loginRequest();
            request.setRequestURI("/api/account/reset-password/init");
            response = new MockHttpServletResponse();
            filter.doFilter(request, response, new RecordingFilterChain(HttpStatus.BAD_REQUEST));
        }

        assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    private MockHttpServletResponse failingLogin() throws ServletException, IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(loginRequest(), response, new RecordingFilterChain(HttpStatus.UNAUTHORIZED));
        return response;
    }

    private MockHttpServletRequest loginRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", LOGIN_PATH);
        request.setRemoteAddr(CLIENT);
        return request;
    }

    /** Catena finta: registra se è stata invocata e impone lo status della risposta. */
    private static final class RecordingFilterChain extends MockFilterChain {

        private final HttpStatus status;

        private boolean invoked;

        private RecordingFilterChain(HttpStatus status) {
            this.status = status;
        }

        @Override
        public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response)
            throws IOException, ServletException {
            invoked = true;
            ((MockHttpServletResponse) response).setStatus(status.value());
        }
    }
}
