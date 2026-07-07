# Frontend Qute + HTMX (server-rendered)

> **Status**: Accepted
>
> **Updated**: 2026-07-06

## Summary

A interface do Contraponto é **server-rendered** com **Qute** templates e **HTMX** para navegação e mutações parciais — sem SPA framework. JavaScript complementar vive em `META-INF/resources/js/` para APIs de browser (upload, editor, chrome).

## Drivers

* Plataforma editorial: SEO, links rastreáveis, fragmentos HTML em vez de JSON para a maior parte das interações.
* HTMX já integrado (auth refresh, scoped events, OOB SEO) — ver [htmx-events.md](../../docs/htmx-events.md).
* Evitar build Node/npm no pipeline; assets estáticos servidos pelo Quarkus.

## Options

### Qute + HTMX (atual)

Templates em `src/main/resources/templates/`; `hx-get`/`hx-post` com `href` crawlable; JS mínimo.

### React / Vue SPA

Frontend separado; API JSON para todas as telas.

### Quarkus Quinoa + Angular

Monorepo full-stack SPA (padrão SauOn).

## Options Analysis

### Qute + HTMX Assessment

* Pro: SEO e CLS controláveis no servidor; feature-catalog documenta click paths reais.
* Pro: Testes `@WebTest` com `App` DSL cobrem fluxos de usuário.
* Con: Interatividade rica (editor write) exige JS dedicado — aceito e isolado em `write.js`.

### SPA Assessment

* Con: Reescrever toda a superfície; piora SEO e duplica rotas já modeladas em Qute.

### Angular + Quinoa Assessment

* Con: Stack diferente do produto atual; incompatível com regras `contraponto-javascript.mdc` e templates existentes.

## Recommendation

Manter **Qute + HTMX** como modelo de apresentação. Novas superfícies: endpoint → template → HTMX; JS apenas quando browser API for necessária ([contraponto-javascript.mdc](../../.cursor/rules/contraponto-javascript.mdc)).

### Confirmation

* Nenhum `package.json` de build no repositório.
* Templates usam `*Paths` e `data-hx-get` + `href` ([contraponto-seo.mdc](../../.cursor/rules/contraponto-seo.mdc)).

## Changelog

| Data | Evento | Detalhe |
|------|--------|---------|
| 2026-07-06 | proposed | Formalização da stack existente. |
| 2026-07-06 | accepted | Aceite manual — baseline Contraponto. |

## More Information

* [docs/htmx-events.md](../../docs/htmx-events.md)
* [docs/ui-elements.md](../../docs/ui-elements.md)
