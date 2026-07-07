# Banco de dados PostgreSQL

> **Status**: Accepted
>
> **Updated**: 2026-07-06

## Summary

O Contraponto adota **PostgreSQL** como banco relacional único, com migrações incrementais via **Flyway** (`src/main/resources/db/migration/`) e acesso via **Hibernate ORM** (Jakarta Persistence).

## Drivers

* Dados editoriais exigem integridade referencial (usuários, blogs, publicações, audiência).
* Flyway versionado desde o início do projeto (`V0.0.1__initial_schema.sql` e sequência).
* PostgreSQL padrão em dev (`%dev`), testes e produção.

## Options

### PostgreSQL + Flyway incremental

Uma migration por mudança de schema em produção.

### PostgreSQL + baseline único (pré-produção)

Um único arquivo de schema (padrão SauOn pré-launch).

### Outro RDBMS

MySQL, etc.

## Recommendation

Manter **PostgreSQL + Flyway incremental**. Nunca reescrever migrations já aplicadas em produção; novas tabelas/colunas em `V*.sql` sequencial.

Dev seed: [`dev-import.sql`](../../src/main/resources/dev-import.sql) após Flyway em `%dev` — [dev-import-sql-safety.mdc](../../.cursor/rules/dev-import-sql-safety.mdc).

### Confirmation

* `quarkus.datasource` aponta para PostgreSQL em todos os perfis relevantes.
* CI executa testes contra banco compatível.

## Changelog

| Data | Evento | Detalhe |
|------|--------|---------|
| 2026-07-06 | proposed | Formalização da persistência existente. |
| 2026-07-06 | accepted | Aceite manual — baseline Contraponto. |

## More Information

* [ARCHITECTURE.md](../../ARCHITECTURE.md) schema sections
* [development-experience.mdc](../../.cursor/rules/development-experience.mdc)
