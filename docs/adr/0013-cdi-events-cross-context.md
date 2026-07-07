# Cross-context side effects via CDI events

> **Status**: Proposed
>
> **Updated**: 2026-07-07
>
> **Aceitação / reabertura:** somente **manual** pelo usuário humano.

## Summary

Bounded contexts react to domain changes through **CDI application events** (`PostPublishedEvent`, `PostUnpublishedEvent`, `CustomPageChangedEvent`, `PostGitSyncRequestedEvent`, …) observed by `@Observes` methods in the owning or integration context. Publishing code does **not** call notification, RSS, SEO, Git, or ActivityPub services directly.

## Drivers

* Modular monolith must respect bounded-context dependency rules.
* Side effects (email, federation, cache invalidation) should not block HTTP transactions unnecessarily.
* Event catalog enables onboarding and impact analysis.

## Options

### CDI events + observers (implemented)

Producers fire after successful domain mutation; observers in target contexts.

### Direct service calls from publisher

Simpler call graph but violates context boundaries.

### Message broker (Kafka, etc.)

Overkill for current scale.

## Options Analysis

### CDI events + observers

* Pro: Documented in [cdi-events.md](../cdi-events.md); ArchUnit enforces package deps.
* Con: Ordering and transactional boundaries require discipline (`AFTER_SUCCESS` where needed).

## Recommendation

Keep **CDI events** as the integration mechanism for cross-context reactions. New side effects add an event or extend an existing one with impact review.

### Consequences

* Pro: `post` package stays free of `notification` imports in publish service (observer in `notification`).
* Con: Debugging requires tracing event → observer chain.

### Confirmation

* [cdi-events.md](../cdi-events.md) matches producers/observers in code.
* `BoundedContextRulesTest` green.

## Changelog

| Data | Evento | Detalhe |
|------|--------|---------|
| 2026-07-07 | proposed | Retroactive ADR documenting shipped event-driven integration. |

## More Information

* [cdi-events.md](../cdi-events.md)
* [contraponto-bounded-contexts.mdc](../../.cursor/rules/contraponto-bounded-contexts.mdc)
