# baseWebApp

**Piattaforma base riutilizzabile** per web app full-stack con backend REST condivisibile tra web e
client mobile (iOS/Android), e integrazione AI/ML pluggable.

Non è un progetto di dominio: è lo **scheletro** da cui nascono progetti concreti (il primo consumer
è `skin-lesion-webapp`). La base viene **generata con JHipster** (Spring Boot 4 · Java 21 · Angular ·
PostgreSQL · JWT · OpenAPI); il codice custom si riduce ai pochi pezzi differenzianti, a partire da
uno **starter di integrazione AI/ML**.

## Stack

| Livello | Tecnologia |
|---------|------------|
| Backend | Java 21 · Spring Boot 4 · Maven |
| Frontend | Angular (generato da JHipster) |
| DB | PostgreSQL |
| Auth | JWT |
| API | REST + OpenAPI/Swagger (mobile-ready) |
| Orchestrazione | Docker Compose |

## Documentazione

- [`docs/piattaforma-base.md`](docs/piattaforma-base.md) — architettura, scelte build-vs-buy,
  valutazione dello stack e **roadmap** a step (traccia BASE `B*`).
- [`docs/sviluppo-autonomo.md`](docs/sviluppo-autonomo.md) — workflow di sviluppo: uno step per
  branch, Pull request revisionata e mergiata manualmente.

> Questo README verrà arricchito dallo scaffold JHipster nello step **B0**.
