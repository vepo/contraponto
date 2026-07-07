# Architecture Decision Records (ADR)

Decisões arquiteturais do **Contraponto** usam o template [GiG Cymru NHS Wales](https://github.com/architecture-decision-record/architecture-decision-record/tree/main/locales/en/templates/decision-record-template-by-gig-cymru-nhs-wales).

## Índice

| ADR | Título | Status |
|-----|--------|--------|
| [0001](0001-record-architecture-decisions.md) | Registrar decisões arquiteturais com ADR | Accepted |
| [0002](0002-backend-java-quarkus-jakarta-ee.md) | Backend Java com Quarkus e APIs Jakarta EE | Accepted |
| [0003](0003-frontend-qute-htmx.md) | Frontend Qute + HTMX (server-rendered) | Accepted |
| [0004](0004-package-naming-dev-vepo.md) | Convenção de pacotes `dev.vepo` | Accepted |
| [0005](0005-postgresql-database.md) | Banco de dados PostgreSQL + Flyway | Accepted |
| [0006](0006-activitypub-federation.md) | ActivityPub — federação S2S (Fediverse) | Accepted |
| [0007](0007-activitypub-http-signatures.md) | ActivityPub — HTTP Signatures (inbox/delivery) | Accepted |
| [0008](0008-activitypub-actor-identity.md) | ActivityPub — identidade do actor e WebFinger | Accepted |
| [0009](0009-user-messaging-retention.md) | User messaging — retenção de threads sinalizadas | Accepted |
| [0010](0010-notification-retention.md) | In-app notification retention (read vs unread) | Accepted |

## Quando criar um ADR

Na **fase 2 (architecture design)** quando a decisão:

- Afeta mais de uma feature ou bounded context
- Define stack, padrões estruturais ou integração entre camadas
- Tem trade-offs relevantes a preservar no tempo
- Responde a uma **AQ*n*** com impacto transversal

Decisões **locais a uma feature** (um endpoint, um template, um campo) ficam na seção **Architecture** do `feature/<slug>.md`.

## Processo

1. Copiar [template.md](template.md) → `docs/adr/NNNN-slug.md` com status **`Proposed`**
2. Usuário revisa; **aceitação manual** — [development-process.mdc](../../.cursor/rules/development-process.mdc)
3. Após "Aceito o ADR-NNNN": status `Accepted`, Changelog, atualizar este índice
4. Impact review em features e ARCHITECTURE.md

## Reabertura

ADR `Accepted` insuficiente → usuário reabre **com justificativa** → `Reopened` → emenda → nova aceitação. Mudança grande → novo ADR + anterior `Superseded`.

Detalhe: [adr.mdc](../../.cursor/rules/adr.mdc).
