# Registrar decisões arquiteturais com ADR

> **Status**: Accepted
>
> **Updated**: 2026-07-06

## Summary

O Contraponto adota Architecture Decision Records (ADR) no formato GiG Cymru NHS Wales para documentar decisões técnicas transversais. Decisões locais a uma feature permanecem no feature doc; decisões que afetam stack, padrões ou múltiplos contextos viram ADR em `docs/adr/`.

## Drivers

* O projeto é desenvolvido com auxílio de agentes de IA — decisões precisam de formato estável e rastreável.
* Múltiplas features compartilham stack e convenções; duplicar justificativas em cada `feature/*.md` gera inconsistência.
* O processo de desenvolvimento em cinco fases exige uma fase dedicada de architecture design com artefatos verificáveis.

## Options

### ADR em `docs/adr/` (GiG Cymru template)

Um arquivo markdown por decisão, índice em `docs/adr/README.md`, template versionado.

### Seção Architecture apenas no feature doc

Toda decisão técnica inline em `feature/<slug>.md`.

### ADR + ARCHITECTURE.md monolítico

Decisões apenas em `ARCHITECTURE.md` sem arquivos separados.

## Options Analysis

### ADR em `docs/adr/` Assessment

* Pro: Histórico claro; fácil referenciar por número; agentes localizam decisões sem varrer feature docs.
* Pro: Template padronizado força análise explícita.
* Con: Mais arquivos para manter.

### Seção Architecture apenas no feature doc Assessment

* Pro: Menos arquivos.
* Con: Decisões transversais repetidas ou divergentes entre features.

### ADR + ARCHITECTURE.md monolítico Assessment

* Con: Arquivo cresce; difícil revisar trade-offs por decisão.

## Recommendation

Adotar **ADR em `docs/adr/`** com template GiG Cymru. A fase 2 do [development-process.mdc](../../.cursor/rules/development-process.mdc) cria ou atualiza ADRs para decisões transversais; cada `feature/<slug>.md` mantém **Architecture** com design específico e links aos ADRs.

### Confirmation

* Novos ADRs referenciados em `docs/adr/README.md`.
* Feature docs na fase 2 listam ADRs na seção Architecture.
* Aceitação e reabertura somente manual — [adr.mdc](../../.cursor/rules/adr.mdc).

## Changelog

| Data | Evento | Detalhe |
|------|--------|---------|
| 2026-07-06 | proposed | Importado do processo SauOn / vepo issues. |
| 2026-07-06 | accepted | Aceite manual — processo GiG Cymru adotado no Contraponto. |

## More Information

* Template: [template.md](template.md)
* Processo: [architecture-design.mdc](../../.cursor/rules/architecture-design.mdc)
