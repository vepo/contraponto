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

---

## Decisões transversais pendentes (ADR)

| ID | Tópico | Pergunta | Prioridade | ADR |
|----|--------|----------|------------|-----|
| — | — | Nenhuma pendente | — | — |

Adicione linhas quando uma **AQ*n*** de feature doc exigir ADR `Proposed`.

---

## Perguntas abertas por feature

Rastreie FQ/AQ nos respectivos `feature/<slug>.md`. PRDs legados:

| Feature | Doc | Status |
|---------|-----|--------|
| Reading list | [feature/reading-list.md](../feature/reading-list.md) | done — PRD legado em [prd/reading-list.md](prd/reading-list.md) |
| Post text highlight | [feature/post-text-highlight.md](../feature/post-text-highlight.md) | done — PRD legado em [prd/post-text-highlight.md](prd/post-text-highlight.md) |
| ActivityPub integration | [feature/activitypub-integration.md](../feature/activitypub-integration.md) | **Done** — T1–T13; AQ5 (NodeInfo) optional follow-up |

---

## Pronto para codar (checklist)

Antes de entrar na fase 5 de uma nova feature:

- [ ] `feature/<slug>.md` com changelog `approved` e task IDs listados
- [ ] ADRs relevantes `Accepted` (ou escopo local só no feature doc)
- [ ] Blocking **FQ*n*** e **AQ*n*** `answered` ou `not valid`
- [ ] **Architecture**, **Tasks**, **Test coverage**, **FCdev** definidos
- [ ] Domain spec atualizado se vocabulário mudou

Processo completo: [development-process.mdc](../.cursor/rules/development-process.mdc).
