package com.basewebapp.web.rest.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Endpoint di esempio dell'API di dominio versionata.
 *
 * <p>Serve a due scopi: (1) dimostrare e verificare il meccanismo di versioning
 * ({@link ApiV1} + {@link com.basewebapp.config.ApiVersioningConfiguration}); (2) fornire ai
 * consumer un piccolo endpoint utile che espone nome applicazione e versione dell'API.</p>
 *
 * <p>Nota: pur dichiarando {@code @RequestMapping("/version")}, risponde su
 * {@code GET /api/v1/version} grazie al prefisso aggiunto da {@code @ApiV1}.</p>
 */
@ApiV1
@RequestMapping("/version")
public class VersionResource {

    private final String applicationName;

    public VersionResource(@Value("${spring.application.name:baseWebApp}") String applicationName) {
        this.applicationName = applicationName;
    }

    /**
     * {@code GET /api/v1/version} : nome applicazione e versione dell'API di dominio.
     *
     * @return nome applicazione e label della versione API.
     */
    @GetMapping
    public ApiVersion getVersion() {
        return new ApiVersion(applicationName, "v1");
    }

    /** Payload della versione API. */
    public record ApiVersion(String application, String apiVersion) {}
}
