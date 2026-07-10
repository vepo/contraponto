# Decisões técnicas — backlog

Lista de decisões que **devem ser resolvidas** (ADR `Accepted` ou FQ/AQ `answered`) antes da fase 5 de uma feature.

**Legenda:** bloqueante · recomendado antes do código · pode decidir na fase 2 da feature

---

## Já decidido (baseline)

| ADR | Decisão |
|-----|---------|
| [0001](adr/0001-record-architecture-decisions.md) | Processo ADR |
| [0002](adr/0002-backend-java-quarkus-jakarta-ee.md) | Java + Quarkus + Jakarta EE |
| [0003](adr/0003-frontend-qute-htmx.md) | Qute + HTMX (server-rendered) |
| [0004](adr/0004-package-naming-dev-vepo.md) | `dev.vepo.contraponto` |
| [0005](adr/0005-postgresql-database.md) | PostgreSQL + Flyway incremental |
| [0006](adr/0006-activitypub-federation.md) | ActivityPub S2S federation (Fediverse) |
| [0007](adr/0007-activitypub-http-signatures.md) | ActivityPub HTTP Signatures |
| [0008](adr/0008-activitypub-actor-identity.md) | ActivityPub actor identity — one Person per User |
| [0009](adr/0009-user-messaging-retention.md) | User messaging flagged-thread retention |
| [0010](adr/0010-notification-retention.md) | In-app notification retention |

## Decisões transversais pendentes (ADR)

| ID | Tópico | Pergunta | Prioridade | ADR |
|----|--------|----------|------------|-----|
| ADR-0011 | Blog subdomain URLs | Aceitar modelo dual host já implementado? | recomendado | [0011](adr/0011-blog-subdomain-urls.md) |
| ADR-0012 | Post publication versioning | Aceitar snapshots imutáveis já implementados? | recomendado | [0012](adr/0012-post-publication-versioning.md) |
| ADR-0013 | CDI events | Aceitar integração cross-context via eventos? | recomendado | [0013](adr/0013-cdi-events-cross-context.md) |
| ADR-0014 | Session store | Aceitar SessionStore in-memory/Redis? | recomendado | [0014](adr/0014-session-store.md) |
| ADR-0017 | Per-blog Git credentials + SSH | Aceitar credenciais criptografadas por blog e remotes SSH? | bloqueante | [0017](adr/0017-per-blog-git-credentials-ssh.md) |

Adicione linhas quando uma **AQ*n*** de feature doc exigir ADR `Proposed`.

## Perguntas abertas por feature

Rastreie FQ/AQ nos respectivos `feature/<slug>.md`. Índice completo: [feature/README.md](../feature/README.md).

| Feature | Doc | Status | Open items |
|---------|-----|--------|------------|
| Authentication | [authentication.md](../feature/authentication.md) | done | FQ3, FQ4 |
| Multi-blog | [multi-blog.md](../feature/multi-blog.md) | done | FQ3 |
| Blog audience | [blog-audience.md](../feature/blog-audience.md) | done | FQ2 (auto-follow after login) |
| Git sync | [git-sync.md](../feature/git-sync.md) | architecture-ready (v2) | **ADR-0017** acceptance; AQ2–AQ5 confirm; FQ8–FQ9 informational |
| User administration | [user-administration.md](../feature/user-administration.md) | done | FQ1 (`/administration` naming) |
| ActivityPub | [activitypub-integration.md](../feature/activitypub-integration.md) | done | AQ5 (NodeInfo) optional |
| Bluesky platform syndication | [bluesky-platform-syndication.md](../feature/bluesky-platform-syndication.md) | planned | AQ1, AQ2, AQ4, AQ6; ADR-0016 draft |
| Reading list | [reading-list.md](../feature/reading-list.md) | done | — |
| Post text highlight | [post-text-highlight.md](../feature/post-text-highlight.md) | done | — |

---

## Pronto para codar (checklist)

Antes de entrar na fase 5 de uma nova feature:

- [ ] `feature/<slug>.md` com changelog `approved` e task IDs listados
- [ ] ADRs relevantes `Accepted` (ou escopo local só no feature doc)
- [ ] Blocking **FQ*n*** e **AQ*n*** `answered` ou `not valid`
- [ ] **Architecture**, **Tasks**, **Test coverage**, **FCdev** definidos
- [ ] Domain spec atualizado se vocabulário mudou

Processo completo: [development-process.mdc](../.cursor/rules/development-process.mdc).
