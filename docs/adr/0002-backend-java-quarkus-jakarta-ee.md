# Backend Java com Quarkus e APIs Jakarta EE

> **Status**: Accepted
>
> **Updated**: 2026-07-06

## Summary

O backend do Contraponto é implementado em **Java** com **Quarkus**, priorizando APIs **Jakarta EE** (REST, CDI, Persistence, Validation) em vez de alternativas proprietárias quando houver equivalente padrão.

## Drivers

* Plataforma de publicação e leitura com HTML server-rendered, HTMX e PostgreSQL.
* Ecossistema Java maduro; testes com `@QuarkusTest`, `@WebTest`, e CDI events entre contextos.
* Quarkus oferece tempo de inicialização adequado para desenvolvimento local e deploy em container.

## Options

### Java + Quarkus + Jakarta EE

Quarkus como runtime; JAX-RS, CDI, JPA, Bean Validation.

### Java + Spring Boot

Stack Spring Web, Spring Data, Spring Security.

### Node.js + framework SPA

Backend TypeScript com frontend separado.

## Options Analysis

### Java + Quarkus + Jakarta EE Assessment

* Pro: Stack já em produção neste repositório; regras e testes estabelecidos.
* Pro: CDI events para reações entre bounded contexts (publicação → notificação, RSS, SEO).
* Con: Curva de Quarkus/Hibernate para novos contribuidores.

### Spring Boot Assessment

* Con: Migração desnecessária; perda de investimento em Qute, HTMX e testes existentes.

### Node.js Assessment

* Con: Incompatível com modelo server-rendered + Qute adotado.

## Recommendation

Manter **Java + Quarkus + Jakarta EE** como stack backend. Testes: `GITHUB_ACTIONS=true ./mvnw -B verify` como gate principal.

### Confirmation

* [ARCHITECTURE.md](../../ARCHITECTURE.md) §1 reflete a stack.
* CI em `.github/workflows/ci.yml` executa verify com perfis de teste.

## Changelog

| Data | Evento | Detalhe |
|------|--------|---------|
| 2026-07-06 | proposed | Formalização da stack existente. |
| 2026-07-06 | accepted | Aceite manual — baseline Contraponto. |

## More Information

* Persistência: [ADR-0005](0005-postgresql-database.md)
* Apresentação: [ADR-0003](0003-frontend-qute-htmx.md)
