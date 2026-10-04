# Convenzioni API — piattaforma base

> Contratto stabile dell'API offerta dalla piattaforma ai client (web, mobile, integrazioni).
> Prodotto nello step **B1** ([`sviluppo-autonomo.md`](sviluppo-autonomo.md) §2). La maggior parte di
> queste convenzioni è fornita nativamente da JHipster; qui le fissiamo come **contratto** e
> documentiamo le poche aggiunte custom (versioning, CORS prod).

---

## 1. Versioning dell'API di dominio — `/api/v1`

**Regola:** l'API di **dominio** (business) vive sotto il prefisso stabile **`/api/v1`**. Gli
endpoint di **framework** generati da JHipster (autenticazione, account, gestione utenti, admin,
`/management`) restano su `/api` e `/management` e **non** sono versionati: non fanno parte del
contratto di dominio.

```
GET /api/authenticate        ← framework JHipster (invariato)
GET /api/account             ← framework JHipster (invariato)
GET /api/admin/users         ← framework JHipster (invariato)
GET /api/v1/version          ← dominio (versionato)
GET /api/v1/<entità>         ← dominio (versionato)
```

### 1.1 Come si espone un controller sotto `/api/v1`

Approccio **additivo**: nessuna modifica al codice generato, nessun impatto sul client Angular
generato. Un controller di dominio:

1. si annota con [`@ApiV1`](../src/main/java/com/basewebapp/web/rest/api/ApiV1.java) (è già un
   `@RestController`: non serve aggiungerlo);
2. dichiara un `@RequestMapping` **relativo**, senza ripetere `/api/v1`.

```java
package com.basewebapp.web.rest.api;

@ApiV1
@RequestMapping("/lesions")
public class LesionResource {

    @GetMapping
    public List<LesionDTO> getAll() { ... }   // → GET /api/v1/lesions
}
```

Il prefisso `/api/v1` è aggiunto in modo trasparente da
[`ApiVersioningConfiguration`](../src/main/java/com/basewebapp/config/ApiVersioningConfiguration.java)
(`PathMatchConfigurer.addPathPrefix` su `HandlerTypePredicate.forAnnotation(ApiV1.class)`).

**Endpoint di riferimento:**
[`VersionResource`](../src/main/java/com/basewebapp/web/rest/api/VersionResource.java) espone
`GET /api/v1/version` (pubblico) e funge da template. Il meccanismo è coperto dallo slice test
[`VersionResourceTest`](../src/test/java/com/basewebapp/web/rest/api/VersionResourceTest.java).

### 1.2 Entità generate da JHipster

I controller CRUD **generati** da JHipster via JDL nascono su `/api/<entità>` in
`com.basewebapp.web.rest` (non annotati `@ApiV1`). Per portarli sotto il contratto di dominio
`/api/v1` senza editarli a mano, la strategia consigliata è esporre l'API pubblica di dominio con
controller/facciate **scritti a mano** in `com.basewebapp.web.rest.api` annotati `@ApiV1`, che
riusano service/DTO generati. Vedi [`guida-entita-jdl.md`](guida-entita-jdl.md) §4.

### 1.3 Evoluzione a `/api/v2`

Quando servirà una rottura di contratto: si introduce una seconda annotazione/config analoga per
`/api/v2`, tenendo `/api/v1` in parallelo per i client esistenti. Nessuna rottura "big-bang".

---

## 2. Formato degli errori — RFC 7807 (Problem Details)

Fornito da JHipster ([`ExceptionTranslator`](../src/main/java/com/basewebapp/web/rest/errors/ExceptionTranslator.java)).
Gli errori sono `application/problem+json`:

```json
{
  "type": "https://www.jhipster.tech/problem/constraint-violation",
  "title": "Method argument not valid",
  "status": 400,
  "path": "/api/v1/lesions",
  "fieldErrors": [{ "objectName": "lesion", "field": "name", "message": "must not be null" }]
}
```

- Errori di business custom: estendere `BadRequestAlertException` (o lanciare una sottoclasse di
  `AbstractThrowableProblem`) con `type`/`title`/`status` coerenti.
- Non esporre stacktrace o dettagli interni in prod (default JHipster già conservativo).

---

## 3. Paginazione e ordinamento

Convenzione JHipster (Spring Data `Pageable`):

- Query param: `?page=0&size=20&sort=createdDate,desc` (ripetibile per più campi).
- Risposta: header **`X-Total-Count`** (totale elementi) e header **`Link`** (RFC 5988:
  `first`/`prev`/`next`/`last`), generati da `PaginationUtil`.
- `X-Total-Count` e `Link` sono tra gli `exposed-headers` CORS (vedi §4), quindi leggibili dai
  client browser/mobile cross-origin.

---

## 4. CORS per client nativi / mobile

JHipster filtra il CORS via `jhipster.cors.*` ([`WebConfigurer.corsFilter`](../src/main/java/com/basewebapp/config/WebConfigurer.java)),
applicato a `/api/**` (quindi anche `/api/v1/**`), `/management/**`, `/v3/api-docs`, `/swagger-ui/**`.

- **dev** (`application-dev.yml`): già abilitato per `localhost:4200` (Angular) e `localhost:8100`
  (Ionic).
- **prod** (`application-prod.yml`): **disabilitato di default**, abilitabile senza rebuild via
  variabili d'ambiente (il filtro si attiva solo con origini non vuote):

  ```bash
  export CORS_ALLOWED_ORIGINS="https://app.example.com,capacitor://localhost,ionic://localhost"
  # oppure, per pattern:
  export CORS_ALLOWED_ORIGIN_PATTERNS="https://*.example.com"
  ```

  Metodi, header consentiti ed esposti (incl. `Authorization`, `Link`, `X-Total-Count`) e
  `allow-credentials` sono già impostati nel blocco `jhipster.cors` di `application-prod.yml`.

> Client nativi (Capacitor/Ionic) usano spesso origini `capacitor://localhost` / `ionic://localhost`
> / `http://localhost` — aggiungerle a `CORS_ALLOWED_ORIGINS` quando necessario.

---

## 5. OpenAPI — spec scaricabile e codegen client

La piattaforma espone la spec OpenAPI (springdoc) per generare client tipizzati (web/mobile).

### 5.1 Abilitare e scaricare la spec

La spec è attiva col profilo **`api-docs`** ed è protetta (ADMIN) — non è esposta pubblicamente.

```bash
# 1) avvia col profilo api-docs (in aggiunta a dev o prod)
SPRING_PROFILES_ACTIVE=dev,api-docs ./mvnw spring-boot:run

# 2) autentica come admin e ottieni il token
TOKEN=$(curl -s -X POST http://localhost:8080/api/authenticate \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin"}' | sed 's/.*"id_token":"\([^"]*\)".*/\1/')

# 3) scarica la spec dell'API applicativa (default-include-pattern: /api/**)
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/v3/api-docs > openapi.json

# (facoltativo) spec degli endpoint di management
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/v3/api-docs/management > openapi-management.json
```

Swagger UI interattiva: `http://localhost:8080/swagger-ui/index.html`.

### 5.2 Generare un client

```bash
# esempio: client TypeScript-Angular con openapi-generator
npx @openapitools/openapi-generator-cli generate \
  -i openapi.json -g typescript-angular -o ./generated-client
```

> La spec include l'API di dominio `/api/v1/**` (pattern `default-include-pattern: /api/**`).

---

## 6. Checklist per un nuovo endpoint di dominio

- [ ] Controller in `com.basewebapp.web.rest.api`, annotato `@ApiV1`, `@RequestMapping` relativo.
- [ ] DTO in/out (non esporre entità JPA direttamente).
- [ ] Errori via Problem Details (§2).
- [ ] Liste paginate con `Pageable` + `PaginationUtil` (§3).
- [ ] Se pubblico: aggiungere il matcher `permitAll()` in `SecurityConfiguration` prima di `/api/**`.
- [ ] Documentazione OpenAPI tramite annotazioni springdoc dove utile.

---

## 7. Sicurezza (postura attuale)

Sintesi verificata sulla base (2026-10-04); la review sistematica e l'hardening sono lo step **B2**
([`piattaforma-base.md`](piattaforma-base.md) §4.1), con un gate ricorrente nella Definition of Done.

- **SQL injection:** accesso dati solo via Spring Data JPA parametrizzato; **vietate** query
  native/concatenate non parametrizzate (usare `@Param`/binding).
- **CSRF:** disabilitato **perché** l'auth è JWT stateless con token in header `Authorization`
  (non cookie). Se si introduce auth via cookie, **riattivare** il CSRF.
- **XSS:** API JSON + auto-escaping Angular + CSP. Evitare `innerHTML`/`bypassSecurityTrust` su input
  utente. Irrigidimento CSP/HSTS in prod → step B2.
- **Access control:** default `authenticated()` su `/api/**`; endpoint pubblici solo con matcher
  esplicito; usare `@PreAuthorize` per regole fini.
