# Piattaforma base riutilizzabile — architettura e roadmap (pivot su JHipster)

> **Bozza in revisione.** Nasce dalla decisione di avere una **base riutilizzabile** per più
> progetti (skin-lesion come primo consumer). Dopo valutazione build-vs-buy, la base **non si
> costruisce a mano**: si adotta **JHipster** come scaffold, e il codice custom si riduce ai pochi
> pezzi realmente differenzianti, estratti come **artefatti condivisi**. Questo documento sostituisce
> la parte "fondamenta" di [`analisi-tecnica.md`](analisi-tecnica.md) §14 (che resta come visione di
> dominio) e la precedente bozza "estrazione a librerie a mano" (superata).
>
> Il repo base non esiste ancora: quando l'utente lo crea, la base vi viene generata; qui resta il piano.

---

## 1. Decisioni prese con l'utente

| Tema | Scelta |
|------|--------|
| Come costruire la base | **Pivot su JHipster** + starter condivisi (non a mano). JHipster 9.3+ supporta lo stack (Spring Boot 4.1, Java 25, Angular 22). |
| Codice custom condiviso | Solo i pezzi non generati: **starter AI/ML** (Maven), eventuale **Angular library** per widget custom. Estratti come artefatti versionati (GitHub Packages), SemVer. |
| i18n | **Statico di JHipster** (file JSON gestiti dagli sviluppatori). **Abbandonato** il catalogo i18n editabile a runtime (ex Fase 0b). |
| Sorte del codice attuale | Estrazione → skin-lesion diventa consumer; ma di fatto viene **ri-scaffoldato come app JHipster** (vedi §7). |
| Portata mobile | Solo **contratto API mobile-ready** (OpenAPI, `/api/v1`, JWT, CORS); niente client nativi nella base. JHipster fornisce OpenAPI/JWT nativamente. |
| Publishing artefatti | **GitHub Packages** (Maven/npm) + **GHCR** (Docker), SemVer. *Da confermare al primo uso.* |
| Deploy costo-zero | JVM → valutare **GraalVM native** per il footprint; ML **fuori dal percorso always-on**. Host Compose gratis: Oracle Always Free / Postgres gestito free. *Da verificare al deploy.* |

---

## 2. Cosa dà JHipster (base "comprata") vs cosa resta custom

| Necessità | Fonte | Note |
|-----------|-------|------|
| Entità/DAO/REST/DTO/test da definizione | **JHipster (JDL)** | Sostituisce completamente `core-datagen` |
| Auth JWT, gestione utenti, ruoli, audit | **JHipster** | Cuore di JHipster (era la Fase 0c) |
| OpenAPI/Swagger, `/api`, error handling, paginazione | **JHipster** (springdoc) | Base mobile-ready; resta da fissare versioning `/api/v1` e CORS |
| Shell Angular: routing, admin, auth guard/interceptor, i18n statico | **JHipster** | Frontend generato |
| Migrazioni schema | **JHipster (Liquibase)** | Si adotta Liquibase invece di Flyway |
| Docker Compose, CI, config di produzione | **JHipster** | Generati |
| **Layer di integrazione AI/ML (provider pluggable)** | **Custom → starter** | *Il vero valore aggiunto.* Interfaccia + adapter REST + provider mock |
| Widget Angular custom (es. chat/inferenza AI) | **Custom → Angular lib** *(opzionale)* | Solo se emergono componenti condivisi tra app |

Tutto il resto delle vecchie fasi 0/0b/0c **non si scrive più a mano**.

---

## 3. Artefatti condivisi pubblicati dalla base

| Artefatto | Tipo | Contenuto |
|-----------|------|-----------|
| `ai-ml-integration-starter` | Spring Boot starter, Maven (GitHub Packages) | Contratto provider AI/ML pluggable, adapter REST verso servizi di inferenza, provider mock, config |
| `platform-ui` *(opzionale/dopo)* | Pacchetto npm (GitHub Packages) | Componenti Angular condivisi (es. widget AI) che le app JHipster importano |
| immagini base *(se servono)* | Docker (GHCR) | Solo se emergono immagini comuni oltre a quelle generate da JHipster |

> Rispetto alla bozza precedente, `core-datagen`/`backend-core`/`platform-ui` come *fondamenta*
> spariscono: JHipster le fornisce. Resta la logica **applicativa** condivisa (AI/ML).

---

## 4. Roadmap — Traccia BASE (nuovo repo)

Legenda: ✅ fatta · 🔜 prossimo · ⬜ da fare. Ogni step = un branch + una PR
([`sviluppo-autonomo.md`](sviluppo-autonomo.md)).

| Step | Obiettivo | Stato | Deliverable |
|------|-----------|-------|-------------|
| **B0 — Bootstrap base con JHipster** | Scheletro generato | ✅ | Repo base creato; app JHipster generata (monolite, Angular, JWT, PostgreSQL, OpenAPI); scelte JDL/opzioni documentate; build/run verificati. PR #1 mergiata. |
| **B1 — Convenzioni & API mobile-ready** | Contratto stabile | 🔜 in review | Versioning `/api/v1` (marker `@ApiV1`, additivo), CORS prod env-driven per client nativi, convenzioni error (RFC7807)/paginazione documentate, spec OpenAPI scaricabile per codegen; guida JDL. Vedi `docs/convenzioni-api.md` e `docs/guida-entita-jdl.md`. |
| **B2 — Security review & hardening** | Base sicura e verificabile | ⬜ | Review sistematica (OWASP Top-10/ASVS) di base e API: injection, XSS, CSRF, auth/JWT, access control, security headers, secrets, dipendenze, rate-limit, audit, privacy dati sanitari. Hardening concreti (CSP prod, HSTS, actuator minimo, rate-limit login) + `docs/sicurezza.md` con postura e checklist; scansione dipendenze in CI. Vedi §4.1 |
| **B3 — Starter AI/ML** | Il pezzo custom | ⬜ | `ai-ml-integration-starter` pubblicato (Maven/GitHub Packages): contratto provider + adapter REST + provider mock; SemVer + config publishing |
| **B4 — Blueprint deploy costo-zero** | Produzione a costo zero | ⬜ | Profilo GraalVM native (opzionale) documentato; target di deploy free-tier provato (es. Oracle Always Free + Postgres gestito); ML isolato/scale-to-zero |
| **B5 — Archetype/create-app** | Avvio nuovo progetto | ⬜ | Ricetta ripetibile per un nuovo consumer: JDL base + dipendenza starter AI/ML + convenzioni; eventuale Angular lib se necessaria |

> Molto più corta della bozza precedente: gli ex B1/B2/B4/B5 (generatore, backend-core, identità,
> shell) sono assorbiti da JHipster in **B0**.

### 4.1 B2 — Security review & hardening (dettaglio)

Step dedicato alla sicurezza, **più** un gate ricorrente nella Definition of Done
([`sviluppo-autonomo.md`](sviluppo-autonomo.md) §4) applicato a ogni step successivo. Motivato dalla
natura **sanitaria** dei dati del consumer skin-lesion.

**Ambito della review (OWASP Top-10 / ASVS come riferimento):**

- Injection (SQL/NoSQL/command), XSS, CSRF, SSRF.
- Autenticazione e sessione: JWT, scadenze, rotazione, brute-force su login.
- Broken access control: matcher e `@PreAuthorize`, isolamento per utente/paziente.
- Security headers: CSP, HSTS, `X-Content-Type-Options`, referrer/permissions policy.
- Gestione segreti e configurazione (niente segreti in repo; `application-secret`).
- Vulnerabilità delle dipendenze: OWASP Dependency-Check (Maven) + `npm audit`, in CI.
- Rate limiting / abuse, audit logging degli accessi, logging senza dati sensibili.
- Privacy dati sanitari: minimizzazione, cifratura at-rest dove previsto, data retention.

**Baseline già verificata (2026-10-04, su B0+B1):**

- **SQL injection:** nessun rischio nella base — solo Spring Data JPA parametrizzato, nessuna query
  nativa/concatenata.
- **CSRF:** corretto by-design — sessione stateless, JWT in `localStorage` inviato come header
  `Authorization: Bearer` (non cookie); `csrf.disable()` appropriato. *Caveat:* riattivare il CSRF se
  si passa ad auth via cookie.
- **XSS:** baseline buona (API JSON, auto-escaping Angular, CSP presente). Da irrigidire in prod:
  CSP con `script-src 'unsafe-inline' 'unsafe-eval'` e assenza di HSTS.

**Hardening previsti:** CSP prod più stretta, HSTS, esposizione actuator minima, eventuale
rate-limit su `/api/authenticate`, documento `docs/sicurezza.md` con postura e checklist.

## 5. Roadmap — Traccia skin-lesion (questo repo → app JHipster)

| Step | Obiettivo | Stato | Deliverable |
|------|-----------|-------|-------------|
| **S0 — Ri-scaffold come app JHipster** | Diventa consumer | ⬜ | skin-lesion rigenerata come app JHipster che importa lo starter AI/ML; entità di dominio ridefinite in JDL. **Dipende da B0–B3.** Vedi §7 sul codice legacy |
| **S1 — ml-service come provider** | Integrazione | ⬜ | Modello CV skin (FastAPI/PyTorch) registrato dietro lo starter AI/ML (B3) |
| **S2 — Diario clinico** (ex Fase 1) | Dati clinici | ⬜ | Entità di dominio in JDL, timeline, grafici, upload referti |
| **S3 — AI multimodale** (ex Fase 2) | Interrogazione | ⬜ | Orchestratore AI sui dati paziente — **dopo** che l'isolamento utenti di JHipster è configurato/solido |
| **S4 — PWA / client mobile** (ex Fase 3) | Mobilità | ⬜ | PWA + eventuali client nativi sull'API mobile-ready (B1) |
| **S5 — Interoperabilità** (ex Fase 4) | Ecosistema | ⬜ | Export FHIR, mappatura LOINC/ATC |

> **Ordine:** prima BASE B0→B3 (fino allo starter AI/ML consumabile, sicurezza inclusa), poi S0.
> B4/B5 in parallelo o dopo, quando la forma del consumer è chiara.

---

## 6. Valutazione dello stack (sintesi)

| Componente | Verdetto |
|------------|----------|
| PostgreSQL | ✅ Tieni — free managed (Neon/Supabase), ottimo su tutti gli assi |
| Spring Boot 4.1 / Java 25 | ✅ Tieni — ora sicuro (tooling allineato); per costo-zero valutare GraalVM native (~105MB vs ~420MB RAM, avvio <100ms) |
| Angular | ✅ Tieni — generato da JHipster |
| FastAPI | ✅ Tieni — giusto per il serving ML |
| PyTorch (modello) | ⚠️ Isola — pesante per i free-tier; tienilo fuori dal percorso always-on (scale-to-zero, ONNX/quantizzato) |
| Docker Compose | ✅ Tieni — dev + singola VM in prod |
| `core-datagen` custom | ❌ Sostituito da JHipster |
| Flyway | 🔁 → Liquibase (default JHipster) |

**Rischio costo-zero dominante:** non il framework, ma ospitare JVM + Postgres + inferenza PyTorch
gratis insieme. Mitigazione: backend GraalVM-native, Postgres gestito free, ML isolato.

---

## 7. Sorte del codice attuale (onesto)

Il pivot rende **legacy** buona parte del lavoro delle Fasi 0/0b:
- `tools/core-datagen`, `tools/seeder`, il catalogo i18n runtime (label/app_message, `I18nController`,
  seeder, pipe/servizi Angular i18n), le entità/DTO generate a mano → **superati da JHipster**.
- Non è lavoro sprecato: ha validato requisiti, convenzioni e il dominio; l'**astrazione AI/ML** e la
  comprensione del dominio si portano avanti.

skin-lesion non viene "rifattorizzato" pezzo per pezzo ma **ri-scaffoldato** come app JHipster (S0),
riportando dentro solo il dominio (entità in JDL, pagina screening, `ml-service` come provider).
Il repo attuale resta intatto finché S0 non è pronto e mergiato.

---

## 8. Impatti sul workflow di sviluppo autonomo

- Workflow "uno step, un branch, una PR" ([`sviluppo-autonomo.md`](sviluppo-autonomo.md)) invariato,
  su **due repository**: step `B*` sul repo base, step `S*` su skin-lesion.
- Finché il repo base non esiste, niente branch/PR base: primo passo concreto = **B0** subito dopo che
  l'utente crea il repo e ne comunica nome/URL.
