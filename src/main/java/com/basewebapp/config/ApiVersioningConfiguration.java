package com.basewebapp.config;

import com.basewebapp.web.rest.api.ApiV1;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Versioning dell'API di dominio.
 *
 * <p>Aggiunge in modo trasparente il prefisso {@code /api/v1} a tutti i controller annotati con
 * {@link ApiV1}, senza che questi debbano ripetere {@code /api/v1} nel proprio
 * {@code @RequestMapping}. Gli endpoint di framework generati da JHipster non sono annotati con
 * {@code @ApiV1} e restano quindi su {@code /api} invariati.</p>
 *
 * <p>Approccio scelto: additivo, nessuna modifica al codice generato, nessun impatto sul client
 * Angular generato. Vedi {@code docs/convenzioni-api.md}.</p>
 */
@Configuration
public class ApiVersioningConfiguration implements WebMvcConfigurer {

    /** Prefisso stabile dell'API di dominio versionata. */
    public static final String API_V1 = "/api/v1";

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(API_V1, HandlerTypePredicate.forAnnotation(ApiV1.class));
    }
}
