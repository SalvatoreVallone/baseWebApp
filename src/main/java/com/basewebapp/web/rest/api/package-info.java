/**
 * API REST di dominio <strong>versionata</strong> della piattaforma.
 *
 * <p>I controller in questo package (e sotto-package) che vogliono essere esposti sotto
 * {@code /api/v1} si annotano con {@link com.basewebapp.web.rest.api.ApiV1} e dichiarano un
 * {@code @RequestMapping} <em>relativo</em> (senza {@code /api/v1}). Qui vive l'API di business
 * stabile offerta ai client (web, mobile, integrazioni), distinta dagli endpoint di framework
 * generati da JHipster in {@code com.basewebapp.web.rest}.</p>
 *
 * <p>Convenzioni complete in {@code docs/convenzioni-api.md}.</p>
 */
package com.basewebapp.web.rest.api;
