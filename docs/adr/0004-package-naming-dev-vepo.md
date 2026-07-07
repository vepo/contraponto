# Convenção de pacotes `dev.vepo`

> **Status**: Accepted
>
> **Updated**: 2026-07-06

## Summary

Todo código Java do Contraponto usa o prefixo **`dev.vepo`**, com subpacote de produto **`dev.vepo.contraponto`** para o modular monolith deste repositório.

## Drivers

* Namespace único da organização desenvolvedora (Vepo).
* Bounded contexts como subpacotes (`post`, `blog`, `readinglist`, …).
* ArchUnit (`BoundedContextRulesTest`) valida dependências entre contextos.

## Options

### `dev.vepo.contraponto`

Subpacote por produto; Maven `groupId` `dev.vepo`.

### `br.com.vepo.contraponto`

Padrão reverso de domínio brasileiro.

### Sem convenção fixa

Pacotes ad hoc por feature.

## Recommendation

Usar **`dev.vepo.contraponto`** para todo código de produção e teste deste produto. Bounded contexts conforme [docs/domain-specification.md](../../docs/domain-specification.md).

### Confirmation

* `BoundedContextRulesTest` verde em CI.
* Nenhum pacote de feature em `shared` importando outros contextos.

## Changelog

| Data | Evento | Detalhe |
|------|--------|---------|
| 2026-07-06 | proposed | Formalização da convenção existente. |
| 2026-07-06 | accepted | Aceite manual — baseline Contraponto. |

## More Information

* [contraponto-bounded-contexts.mdc](../../.cursor/rules/contraponto-bounded-contexts.mdc)
