package com.basewebapp.web.rest.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.basewebapp.config.ApiVersioningConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Slice test del meccanismo di versioning dell'API di dominio.
 *
 * <p>Verifica che un controller annotato con {@link ApiV1} e {@code @RequestMapping("/version")}
 * sia effettivamente esposto sotto il prefisso {@code /api/v1} grazie a
 * {@link ApiVersioningConfiguration}, senza bisogno di database (nessun contesto Spring completo).</p>
 */
@WebMvcTest(controllers = VersionResource.class)
@Import(ApiVersioningConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "spring.application.name=baseWebApp")
class VersionResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void domainControllerIsExposedUnderApiV1Prefix() throws Exception {
        mockMvc
            .perform(get("/api/v1/version"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.apiVersion").value("v1"))
            .andExpect(jsonPath("$.application").value("baseWebApp"));
    }

    @Test
    void controllerIsNotExposedAtUnversionedPath() throws Exception {
        mockMvc.perform(get("/version")).andExpect(status().isNotFound());
    }
}
