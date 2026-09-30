# Sviluppo autonomo — regole operative e stato della roadmap

> Documento di **governo dello sviluppo assistito**. Definisce come Claude porta avanti la
> roadmap **uno step alla volta**, ogni step su un branch dedicato, chiudendo con una Pull request
> che l'utente **revisiona e mergia manualmente** su `main`.
>
> **Modello a due repository (deciso):** la base è una **piattaforma generata con JHipster** (repo
> separato) più i pochi artefatti custom condivisi (starter AI/ML); `skin-lesion-webapp` ne diventa
> **consumer** (ri-scaffoldato come app JHipster). La roadmap revisionata, divisa in traccia **BASE**
> (`B*`) e traccia **skin-lesion** (`S*`), con la valutazione build-vs-buy e dello stack, è in
> [`piattaforma-base.md`](piattaforma-base.md). La roadmap di dominio originale
> ([`analisi-tecnica.md`](analisi-tecnica.md) §14) resta come **visione**.
>
> Ogni repository ha la **sua** copia di questo documento come fonte di verità sullo stato: la
> tabella §2 qui sotto tiene lo stato **combinato** finché il repo base non esiste; dopo, ciascun
> repo aggiorna la propria. Le regole tecniche di dettaglio restano in `analisi-tecnica.md`; qui c'è
> solo il *processo*.

---

## 1. Principi

1. **Uno step alla volta.** Si lavora su un solo step fino al merge. Non si apre il branch dello
   step successivo finché la PR corrente non è mergiata (o esplicitamente accantonata dall'utente).
2. **Un branch per step.** Ogni step vive su un branch dedicato che parte da `main` aggiornato.
   `main` non viene mai committato direttamente.
3. **Merge manuale dell'utente.** Claude **non** mergia mai su `main` e **non** abilita mai
   l'auto-merge. Alla fine di uno step apre/prepara la PR; l'utente controlla e mergia.
4. **Uno step = uno step della roadmap.** L'unità di lavoro è uno step della roadmap revisionata
   (`piattaforma-base.md`): traccia BASE (`B0`, `B1`, …) sul repo base, traccia skin-lesion (`S0`,
   `S1`, …) su questo repo. Step ampi possono essere spezzati in più commit sullo stesso branch, ma
   restano **una sola PR** (decisione presa: "una PR per step/fase").
5. **Ogni step è rilasciabile e verificato.** Niente PR con codice non compilato o non provato: vedi
   la Definition of Done (§4).
6. **Vincoli tecnici del progetto sempre validi** (§7): append-only su Flyway, override solo via
   `/* KEEP */`, posizionamento non-diagnostico, isolamento pazienti prima dell'AI sui dati.

---

## 2. Stato della roadmap

Legenda stato: ✅ completata · 🔜 prossimo step · ⬜ da fare · 🔒 bloccata da dipendenza

Stato di dettaglio e deliverable per ogni step: [`piattaforma-base.md`](piattaforma-base.md) §4–5.
Qui la vista combinata di avanzamento.

**Traccia BASE** — repo piattaforma base (da creare, scaffold **JHipster**):

| Step | Obiettivo | Stato | Branch | PR |
|------|-----------|-------|--------|----|
| B0 — Bootstrap base con JHipster | app JHipster generata (JWT/Angular/PostgreSQL/OpenAPI) | 🔜 | — | — |
| B1 — Convenzioni & API mobile-ready | `/api/v1`, CORS, OpenAPI scaricabile, guida JDL | ⬜ | — | — |
| B2 — Starter AI/ML | contratto provider pluggable + mock (Maven/Packages) | ⬜ | — | — |
| B3 — Blueprint deploy costo-zero | GraalVM native (opz.), free-tier, ML isolato | ⬜ | — | — |
| B4 — Archetype/create-app | ricetta nuovo consumer (JDL + starter) | ⬜ | — | — |

**Traccia skin-lesion** — questo repo (→ app JHipster):

| Step | Obiettivo | Stato | Branch | PR |
|------|-----------|-------|--------|----|
| Fase 0 — Fondamenta DB | core-datagen + pipeline | ✅ → legacy (sostituito da JHipster) | — | — |
| Fase 0b — Catalogo i18n | label/app_message, seeder | ✅ → legacy (i18n statico JHipster) | — | — |
| S0 — Ri-scaffold come app JHipster | rigenera + importa starter AI/ML, dominio in JDL | 🔒 B0–B2 | — | — |
| S1 — ml-service provider | modello CV dietro lo starter AI/ML | 🔒 B2 | — | — |
| S2 — Diario clinico (ex Fase 1) | entità dominio (JDL), timeline, grafici, referti | ⬜ | — | — |
| S3 — AI multimodale (ex Fase 2) | orchestratore sui dati paziente | 🔒 isolamento utenti | — | — |
| S4 — PWA / client mobile (ex Fase 3) | PWA + client sull'API mobile-ready | 🔒 B1 | — | — |
| S5 — Interoperabilità (ex Fase 4) | export FHIR, LOINC/ATC | ⬜ | — | — |

> **Regole di dipendenza:** S0 richiede B0–B2 (base + starter AI/ML consumabile); l'AI sui dati (S3)
> non parte prima che l'isolamento utenti sia configurato/solido.
> **Prossimo passo concreto:** l'utente crea il repo base e ne comunica nome/URL → si parte da **B0**.

---

## 3. Convenzioni

### 3.1 Branch

- Nome: `fase-<id>-<slug-descrittivo>` in kebab-case. Es.: `fase-0c-persona-sicurezza`.
- Parte sempre da `main` aggiornato (`git fetch origin && git switch -c <branch> origin/main`).

### 3.2 Commit

- Messaggi in italiano, imperativi e specifici, coerenti con lo storico del repo.
- Commit piccoli e coerenti (uno per unità logica), non un unico commit-monstre a fine step.
- Ogni messaggio di commit termina con la riga di attribuzione:

  ```
  Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>
  ```

### 3.3 Pull request

- **Creazione (meccanismo scelto):** su questa macchina **non** ci sono `gh` né un token GitHub,
  quindi Claude **non crea la PR via API**. Alla fine dello step:
  1. fa il push del branch (`git push -u origin <branch>`);
  2. fornisce all'utente l'**URL compare precompilato** del repo in cui si sta lavorando:
     `https://github.com/<owner>/<repo>/compare/main...<branch>?expand=1`
     (per questo repo: `.../salvatorevallone07/skin-lesion-webapp/compare/...`; per il repo base,
     l'URL corrispondente una volta noto owner/nome)
  3. fornisce **titolo e descrizione PR** già pronti da incollare (vedi template §5).
  L'utente apre la PR con un click, la revisiona e la mergia.
- **Descrizione PR:** termina sempre con:

  ```
  🤖 Generated with [Claude Code](https://claude.com/claude-code)
  ```

- Se in futuro l'utente installa `gh` o fornisce un token, si passa alla creazione automatica della
  PR (aggiornare questa sezione in tal caso).

---

## 4. Definition of Done di uno step

Uno step è "done" e pronto per la PR solo quando **tutte** queste condizioni valgono:

- [ ] Il deliverable della fase (colonna "Obiettivo" in §2) è realizzato.
- [ ] Il backend **compila** (`mvn -f backend/pom.xml -q -DskipTests package` o build Docker) senza
      errori.
- [ ] Il frontend **builda** (`npm run build` in `frontend/`) senza errori, se toccato.
- [ ] I test rilevanti passano *oppure* è documentato esplicitamente perché non sono eseguibili in
      locale (com'è già il caso di Testcontainers su questa macchina — vedi log Fase 0/0b).
- [ ] La verifica end-to-end sensata per lo step è stata fatta (pipeline Docker, chiamate API, ecc.)
      **oppure** i limiti di verifica sono dichiarati onestamente nella descrizione PR.
- [ ] Nessuna regressione evidente sulle funzionalità esistenti.
- [ ] La migrazione Flyway (se presente) è **append-only** e non riscrive migrazioni già applicate.
- [ ] Documentazione aggiornata: questo file (§2 stato + §6 registro) e, se serve, `README.md` e/o un
      log di implementazione della fase in `docs/`.

Se una casella non può essere spuntata, va **detto** nella PR: mai spacciare per fatto ciò che non è.

---

## 5. Procedura operativa (checklist per ogni step)

Claude segue questi passi a ogni step:

1. **Allineo `main`:** `git fetch origin && git switch main && git pull`.
2. **Creo il branch:** `git switch -c fase-<id>-<slug> origin/main`.
3. **Aggiorno lo stato:** in §2 porto la fase corrente a 🔜/in corso e annoto il branch.
4. **Implemento** in commit piccoli e coerenti (§3.2), rispettando i vincoli §7.
5. **Verifico** secondo la Definition of Done (§4).
6. **Aggiorno la documentazione:** §2 (stato → in review), §6 (registro), eventuale log di fase.
7. **Push:** `git push -u origin fase-<id>-<slug>`.
8. **Preparo la PR:** fornisco URL compare + titolo + descrizione (template sotto).
9. **Mi fermo e attendo** la revisione/merge dell'utente. Non apro il branch successivo prima.
10. **Dopo il merge** (l'utente conferma): in §2 porto la fase a ✅ e segno la PR; il prossimo step
    diventa 🔜.

### Template descrizione PR

```markdown
## Fase <id> — <titolo>

### Cosa fa
<sintesi dei deliverable realizzati, riferiti a analisi-tecnica.md §n>

### Modifiche principali
- <file/area> — <cosa>
- ...

### Come verificare
<comandi / passi per provarla>

### Verifica già eseguita
<cosa è stato provato e con quale esito>

### Limiti noti
<cosa NON è coperto / non verificabile in locale, onestamente>

🤖 Generated with [Claude Code](https://claude.com/claude-code)
```

---

## 6. Registro degli step

Cronologia degli step gestiti con questo workflow (il più recente in alto). Ogni voce: data, fase,
branch, esito.

| Data | Fase | Branch | Stato | Note |
|------|------|--------|-------|------|
| 2026-09-30 | — | — | revisione 2 | Pivot su **JHipster** per la base (build-vs-buy: JHipster 9.3 supporta Spring Boot 4.1/Java 25/Angular 22). Custom ridotto allo starter AI/ML; i18n statico (abbandonato catalogo runtime). Roadmap BASE accorciata a B0–B4. Vedi `piattaforma-base.md`. |
| 2026-09-30 | — | — | revisione 1 | Roadmap revisionata: fondamenta estratte in una piattaforma base a librerie; skin-lesion consumer. (Superata dalla revisione 2.) |
| 2026-09-30 | — | — | setup | Creato questo documento di governo dello sviluppo autonomo. |

> Fasi 0 e 0b sono state completate **prima** dell'adozione di questo workflow; il loro dettaglio è
> in [`docs/fase-0-0b-implementation-log.md`](fase-0-0b-implementation-log.md).

---

## 7. Vincoli tecnici sempre validi

Riassunto operativo; il dettaglio è in `analisi-tecnica.md` (§ indicati).

- **Flyway append-only** (§5.4): mai modificare una migrazione già applicata; solo nuove `V<n>__`.
- **Codice generato non si edita a mano** (§5.7): override puntuali solo con marker `/* KEEP */`,
  con la responsabilità di tenerli allineati allo schema.
- **Single source of truth = descriptor YAML** (§5): nuove entità si aggiungono con un descriptor +
  build, non scrivendo a mano entità/DAO/REST.
- **Isolamento pazienti prima dell'AI** (§14): la Fase 2 non parte finché la 0c non è solida.
- **Posizionamento non-diagnostico** (§12): niente claim clinici/auto-diagnosi; screening informativo.
- **Privacy dati sanitari** (§12): minimizzazione, cifratura dove previsto, audit degli accessi.
- **Onestà nella verifica**: dichiarare sempre cosa è stato provato davvero e cosa no.

---

## 8. Cosa fa e cosa non fa Claude in questo workflow

**Fa:** crea branch, implementa, verifica, aggiorna la doc, fa push, prepara la PR, si ferma.

**Non fa senza conferma esplicita dell'utente:** merge su `main`, push forzati che riscrivono la
storia condivisa, auto-merge, eliminazione di branch remoti, modifiche a segreti/credenziali,
apertura di più step in parallelo.
