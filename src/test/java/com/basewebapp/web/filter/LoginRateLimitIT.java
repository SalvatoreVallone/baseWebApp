package com.basewebapp.web.filter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.basewebapp.IntegrationTest;
import com.basewebapp.web.rest.vm.LoginVM;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration test del rate limit sull'autenticazione ({@link LoginRateLimitFilter} registrato nella
 * security chain). Soglia abbassata a 2 tentativi per non allungare il test.
 */
@AutoConfigureMockMvc
@IntegrationTest
@TestPropertySource(
    properties = {
        "application.security.login-rate-limit.enabled=true",
        "application.security.login-rate-limit.max-attempts=2",
        "application.security.login-rate-limit.window=5m",
    }
)
class LoginRateLimitIT {

    private static final int MAX_ATTEMPTS = 2;

    @Autowired
    private ObjectMapper om;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRejectAuthenticationAttemptsBeyondTheThreshold() throws Exception {
        LoginVM login = new LoginVM();
        login.setUsername("brute-force-victim");
        login.setPassword("wrong password");
        byte[] body = om.writeValueAsBytes(login);

        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            mockMvc
                .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        }

        mockMvc
            .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(429))
            .andExpect(jsonPath("$.path").value("/api/authenticate"))
            .andExpect(jsonPath("$.id_token").doesNotExist());

        // Il GET di verifica della sessione non è soggetto al rate limit.
        mockMvc.perform(get("/api/authenticate")).andExpect(status().isUnauthorized());
    }
}
