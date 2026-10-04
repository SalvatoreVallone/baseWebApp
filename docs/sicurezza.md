# Sicurezza — postura della piattaforma base

> Deliverable dello step **B2 — Security review & hardening**
> ([`piattaforma-base.md`](piattaforma-base.md) §4.1). Documenta la postura di sicurezza della base
> JHipster, l'esito della review (OWASP Top-10/ASVS), gli hardening applicati in B2 e la checklist
> ricorrente che ogni step successivo deve rispettare (gate in [`sviluppo-autonomo.md`](sviluppo-autonomo.md) §4).
>
> La base è generica: i consumer (es. skin-lesion) ereditano questa postura e **aggiungono** i
> controlli specifici del dominio (vedi §7, dati sanitari).

---

## 1. Modello di minaccia (sintesi)

- **Attore**: client web/mobile non fidati su rete pubblica; utenti autenticati con ruoli diversi.
- **Asset**: credenziali, token JWT, dati applicativi (per i consumer sanitari: dati personali e
  clinici → categoria particolare GDPR art. 9).
- **Superficie**: API REST (`/api`, `/api/v1`), endpoint di management, SPA Angular, DB PostgreSQL.
- **Fuori ambito base**: sicurezza fisica dell'host, hardening del DB gestito (demandato al provider),
  sicurezza del modello ML (step della traccia skin-lesion).

---

## 2. Esito della review per categoria (OWASP Top-10)

| Categoria | Stato base | Note |
|-----------|-----------|------|
| A01 Broken Access Control | ✅ buono | `/api/**` default `authenticated()`; admin via `hasAuthority(ADMIN)`; endpoint pubblici solo con matcher esplicito; `@PreAuthorize` sui resource sensibili. Isolamento per-utente: responsabilità del dominio (vedi §7). |
| A02 Cryptographic Failures | ✅/⚠️ | Password con BCrypt; JWT firmato. TLS: demandato al reverse proxy/host (HSTS ora forzato, §4). Cifratura at-rest dati sensibili: a carico del consumer. |
| A03 Injection (SQLi/XSS) | ✅ buono | Solo Spring Data JPA parametrizzato (nessuna query nativa/concatenata). XSS: API JSON + auto-escaping Angular + CSP. Vedi §3. |
| A04 Insecure Design | ✅ | Stateless, separazione ruoli, principio del minimo privilegio sugli endpoint. |
| A05 Security Misconfiguration | ⚠️→✅ | Hardening B2: CSP prod più stretta, HSTS, esposizione actuator minima, prometheus non pubblico (§4). |
| A06 Vulnerable Components | ⚠️ gestito | Scansione dipendenze aggiunta (OWASP dependency-check + `npm audit` + CI, §5). Da eseguire con continuità. |
| A07 Auth Failures | ✅/⚠️ | JWT, BCrypt, account lockout non presente di default → **rate-limit login consigliato** (§6). |
| A08 Integrity Failures | ✅ | Dipendenze da repository ufficiali; lockfile npm; `enforce-dependencyConvergence` nel pom. |
| A09 Logging/Monitoring | ✅/⚠️ | Logging strutturato disponibile; audit degli accessi di dominio a carico del consumer (§7). |
| A10 SSRF | ✅ n/a | La base non effettua fetch verso URL forniti dall'utente. Attenzione negli adapter dello starter AI/ML (B3). |

---

## 3. Dettaglio injection / XSS / CSRF (verifica richiesta)

### 3.1 SQL injection — nessun rischio nella base
Tutto l'accesso dati è via **Spring Data JPA** (metodi derivati, binding dei parametri). Nessuna
`@Query` custom, nessuna query **nativa**, nessuna concatenazione di stringhe in SQL/JPQL.
**Regola per il futuro:** se serve una query custom, usare sempre parametri (`:param` / `@Param`),
mai concatenazione di input.

### 3.2 XSS — baseline buona
- Il backend espone **JSON**, non HTML templato.
- Angular applica **auto-escaping** contestuale; evitare `bypassSecurityTrust*`.
- Due `innerHTML` interni a JHipster (alert e direttiva i18n) sono alimentati da stringhe
  **controllate dallo sviluppatore** (messaggi/traduzioni), non da input utente → rischio basso.
  Non introdurne di nuovi legati a input utente.
- **CSP** attiva; in prod è stata irrigidita (§4.1).

### 3.3 CSRF — corretto by-design
`csrf.disable()` è appropriato **perché** l'autenticazione è JWT **stateless** con token in
`localStorage`, inviato come header `Authorization: Bearer` solo a URL same-origin
(`auth.interceptor.ts`). Il browser non invia il token automaticamente cross-site → CSRF non
applicabile.
> ⚠️ **Se** si passa ad auth basata su cookie, il CSRF **va riattivato** (token CSRF + `SameSite`).

---

## 4. Hardening applicati in B2

| Area | Modifica | File |
|------|----------|------|
| HSTS | `Strict-Transport-Security` esplicito (1 anno, includeSubDomains); attivo su HTTPS | `config/SecurityConfiguration.java` |
| CSP prod | Rimossi `'unsafe-eval'` e allowlist `storage.googleapis.com` da `script-src`; aggiunti `object-src 'none'`, `base-uri 'self'`, `form-action 'self'`, `frame-ancestors 'self'` | `config/application-prod.yml` (`jhipster.security.content-security-policy`) |
| Actuator | In prod esposti solo `health,info,prometheus,metrics` (niente `env`/`configprops`/`liquibase`/`threaddump`) | `config/application-prod.yml` |
| Metriche | `/management/prometheus` non più pubblico: ricade sotto ADMIN | `config/SecurityConfiguration.java` |
| Dipendenze | Profilo Maven `security` (OWASP dependency-check) + script `npm audit` + workflow CI | `pom.xml`, `package.json`, `.github/workflows/security.yml` |

> **Nota di verifica onesta:** la presenza effettiva degli header (CSP/HSTS) e il corretto
> funzionamento della SPA con la CSP prod più stretta vanno confermati con uno **smoke test su
> istanza avviata** (dev/prod reale) — non riproducibile offline su questa macchina (serve DB +
> build prod + browser). Le modifiche compilano e il YAML è valido.

---

## 5. Scansione delle dipendenze

```bash
# Frontend: fallisce su vulnerabilità High/Critical
npm run security:audit:frontend          # = npm audit --omit=dev --audit-level=high

# Backend: OWASP dependency-check (primo run lento; una NVD_API_KEY velocizza molto)
NVD_API_KEY=... ./mvnw -Psecurity org.owasp:dependency-check-maven:check
# report in target/dependency-check-report.html
```

In **CI** ([`.github/workflows/security.yml`](../.github/workflows/security.yml)): `npm audit`,
dependency-check (con `secrets.NVD_API_KEY` se configurata) e **CodeQL** (query
`security-extended` per Java e TypeScript) su push/PR e con schedulazione settimanale.

> Per abilitare dependency-check veloce: creare una NVD API key gratuita
> (<https://nvd.nist.gov/developers/request-an-api-key>) e aggiungerla come secret `NVD_API_KEY`.

---

## 6. Raccomandazioni aperte (non ancora implementate)

- **Rate limiting / lockout su `/api/authenticate`**: non presente di default. Consigliato prima
  dell'esposizione pubblica (es. bucket4j in-memory o a livello di reverse proxy). Mitiga
  brute-force/credential stuffing.
- **Rotazione/scadenza token**: valutare refresh token e TTL più corto per i client sensibili.
- **TLS end-to-end**: garantire HTTPS al reverse proxy (HSTS ha effetto solo su HTTPS).
- **Secret management**: in prod usare variabili d'ambiente / vault, mai `application-secret*.yml`
  committati con valori reali.

---

## 7. Dati sanitari (per i consumer, es. skin-lesion)

Controlli **aggiuntivi** che il consumer deve implementare (non sono nella base generica):

- **Isolamento per paziente/utente**: ogni query di dominio filtrata sull'utente proprietario;
  test di accesso negato cross-utente. Prerequisito prima dell'AI sui dati (vincolo di roadmap).
- **Minimizzazione**: raccogliere solo i dati necessari; evitare PII nei log.
- **Cifratura at-rest** dei dati clinici dove previsto; cifratura in transito (TLS).
- **Audit degli accessi** ai dati sensibili (chi, cosa, quando).
- **Data retention / cancellazione** (GDPR art. 17); base giuridica del trattamento (art. 9).
- **Posizionamento non-diagnostico**: nessun claim clinico (vincolo di progetto).

---

## 8. Checklist ricorrente (gate Definition of Done)

Da verificare a **ogni** step (vedi [`sviluppo-autonomo.md`](sviluppo-autonomo.md) §4):

- [ ] Nessuna query nativa/concatenata non parametrizzata introdotta.
- [ ] Ogni nuovo endpoint ha access control esplicito (matcher o `@PreAuthorize`); default autenticato.
- [ ] Nessun `innerHTML` / `bypassSecurityTrust` alimentato da input utente.
- [ ] Nessun segreto committato (chiavi, password, token).
- [ ] `npm audit` e dependency-check senza vulnerabilità High/Critical nuove.
- [ ] Se si introduce auth via cookie → CSRF riattivato.
- [ ] Per dati sensibili: isolamento per-utente verificato con test.
