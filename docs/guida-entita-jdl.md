# Guida — aggiungere un'entità via JDL

> Come si aggiunge un'entità di dominio alla piattaforma (o a un consumer) usando **JDL**, il
> single source of truth di JHipster. Prodotta nello step **B1**. Sostituisce il vecchio flusso
> "descriptor YAML + core-datagen" (ora legacy, vedi [`piattaforma-base.md`](piattaforma-base.md) §7).

---

## 1. Principio

Le entità **non si scrivono a mano**: si descrivono in un file `.jdl` e si generano con JHipster
(entità JPA, repository, service, DTO/mapper, REST, changelog Liquibase, schermate Angular, test).
Modificare a mano i file generati è contro le convenzioni: si cambia il JDL e si **rigenera**.

Prerequisiti ambiente (già presenti su questa macchina, vedi log di sessione):
`node` (via nvm), `generator-jhipster` globale, Java 21.

```bash
export NVM_DIR="$HOME/.nvm"; . "$NVM_DIR/nvm.sh"; nvm use --lts
jhipster --version   # 9.x
```

---

## 2. Scrivere il JDL

Crea un file, es. `entities.jdl` (separato da `app.jdl`, che descrive l'applicazione):

```jdl
entity Patient {
  firstName String required
  lastName String required
  birthDate LocalDate
}

entity Lesion {
  takenAt Instant required
  bodySite String
  notes TextBlob
}

relationship ManyToOne {
  Lesion{patient(lastName)} to Patient
}

// opzioni: DTO, service layer, paginazione
dto Patient, Lesion with mapstruct
service Patient, Lesion with serviceClass
paginate Lesion with infinite-scroll
```

Riferimento sintassi: <https://www.jhipster.tech/jdl/>.

---

## 3. Generare

Dalla root del progetto:

```bash
# importa SOLO le entità (non ri-scaffolda l'app)
jhipster jdl entities.jdl

# in CI / rigenerazioni non interattive
jhipster jdl entities.jdl --force --skip-install --no-insight
```

Genera/aggiorna: `domain/`, `repository/`, `service/` (+`dto`,`mapper`), `web/rest/`, un
**nuovo** changelog Liquibase in `src/main/resources/config/liquibase/changelog/`, le pagine
Angular e i test.

> **Vincolo Liquibase — append-only.** Ogni `jhipster jdl` aggiunge **nuovi** changelog con
> timestamp; non modificare né cancellare changelog già applicati. Per cambiare un'entità esistente
> si rigenera: JHipster crea un changelog incrementale. Non riscrivere la storia dello schema.

---

## 4. Esporre l'entità sotto `/api/v1` (contratto di dominio)

I controller generati nascono su `/api/<entità>` (non versionati). Per includerli nel contratto di
dominio `/api/v1` (vedi [`convenzioni-api.md`](convenzioni-api.md) §1) **senza editare il generato**,
scrivi una facciata a mano in `com.basewebapp.web.rest.api` annotata `@ApiV1` che riusa il service
generato:

```java
package com.basewebapp.web.rest.api;

@ApiV1
@RequestMapping("/lesions")
public class LesionApi {

    private final LesionService lesionService;   // generato da JHipster

    public LesionApi(LesionService lesionService) { this.lesionService = lesionService; }

    @GetMapping
    public List<LesionDTO> list() { return lesionService.findAll(); }   // → GET /api/v1/lesions
}
```

In alternativa, per CRUD interni/admin si può lasciare l'endpoint generato su `/api/<entità>`
(coperto dai matcher di sicurezza esistenti) ed esporre via `/api/v1` solo l'API pubblica curata.

---

## 5. Verifica (Definition of Done)

Come da [`sviluppo-autonomo.md`](sviluppo-autonomo.md) §4:

```bash
./mvnw -DskipTests compile          # backend compila
./npmw run build                    # frontend builda (se toccato)
./mvnw test                         # test backend (richiedono DB/Testcontainers)
```

- Verifica che i nuovi changelog Liquibase siano **append** e applichino puliti su un DB vuoto.
- Verifica le nuove rotte (`/api/<entità>` o la facciata `/api/v1/...`).
- Aggiorna la documentazione se l'entità introduce convenzioni nuove.

> Limite noto su questa macchina: i test JHipster usano **Testcontainers** (Docker) e potrebbero non
> girare in locale; dichiararlo onestamente nella PR (vedi log Fase 0/0b).
