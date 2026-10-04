package com.basewebapp.web.rest.api;

import java.lang.annotation.*;
import org.springframework.web.bind.annotation.RestController;

/**
 * Marca un controller REST come parte dell'<strong>API di dominio versionata</strong> della
 * piattaforma.
 *
 * <p>I controller annotati con {@code @ApiV1} vengono esposti automaticamente sotto il prefisso
 * {@code /api/v1} (vedi {@link com.basewebapp.config.ApiVersioningConfiguration}). Il percorso
 * dichiarato dal controller è quindi <em>relativo</em> alla versione: un controller con
 * {@code @RequestMapping("/lesions")} risponde su {@code /api/v1/lesions}.</p>
 *
 * <p>È un meta-stereotipo {@link RestController}: una classe annotata con {@code @ApiV1} è già un
 * controller REST, non serve aggiungere anche {@code @RestController}.</p>
 *
 * <p>Gli endpoint di framework generati da JHipster (autenticazione, account, admin, management)
 * <strong>non</strong> usano questa annotazione e restano su {@code /api} / {@code /management}:
 * il versioning riguarda solo l'API di dominio. Vedi {@code docs/convenzioni-api.md}.</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RestController
public @interface ApiV1 {
}
