# Per-blog Git credentials and SSH remotes

> **Status**: Proposed
>
> **Updated**: 2026-07-10
>
> **Aceitação / reabertura:** somente **manual** pelo usuário humano. O agente cria com `Proposed` e **não** muda para `Accepted` ou `Reopened` sem mensagem explícita (ex.: "Aceito o ADR-0017", "Reabro o ADR-0017 porque …"). Ver [development-process.mdc](../../.cursor/rules/development-process.mdc).

## Summary

Git sync authenticates with **per-blog credentials** supplied by the blog owner (HTTPS username + password/PAT, or SSH private key + optional passphrase). Secrets are **encrypted at rest** with a dedicated server secret. Remotes may be **HTTPS or SSH**. There is **no** server-wide `contraponto.git.username` / `password` fallback. Automatic sync and Sync now behaviour are product rules in [feature/git-sync.md](../../feature/git-sync.md); this ADR covers credential storage, transport, and SSRF/host-key posture.

## Drivers

* Multi-tenant blogs cannot share one operator PAT (FQ3, FQ5).
* Authors need SSH remotes (`git@…`) as well as HTTPS (FQ6).
* Secrets must be usable by JGit (reversible encryption, FQ10) and never re-displayed.
* Outbound remotes must not become an SSRF vector ([ADR-0015](0015-federation-outbound-fetch-ssrf.md) pattern).

## Options

### A — Keep server-only credentials; HTTPS only

Status quo (`contraponto.git.username` / `password`).

### B — Per-blog credentials (HTTPS + SSH), encrypted; no server fallback

Store ciphertext on `tb_blogs`; decrypt only in the Git integration service for clone/fetch/push; extend remote URL validation for SSH; JGit Apache SSH; pin host keys under the blog workspace.

### C — Per-user credentials shared across blogs

One credential set per author for all blogs.

## Options Analysis

### A Assessment

* Pro: Simple ops.
* Con: Rejected by FQ3/FQ5; fails multi-tenant private remotes.

### B Assessment

* Pro: Matches product FQs; isolates secrets per blog; supports SSH.
* Con: Encryption secret rotation invalidates stored credentials; SSH host-key and SSRF rules to maintain.

### C Assessment

* Pro: Fewer secrets to enter.
* Con: Rejected by FQ3 (per blog).

## Recommendation

Adopt **option B**.

| Concern | Decision |
|---------|----------|
| Scope | Credentials on **Blog** only |
| HTTPS | Username (plaintext, not a secret) + **password/PAT always encrypted** at rest (`git_auth_secret_encrypted`) |
| SSH | Private key PEM/OpenSSH (**encrypted**) + optional passphrase (**encrypted**) |
| What is encrypted | HTTPS password/PAT, SSH private key, SSH passphrase — same AES-GCM path; never stored or logged in plaintext |
| Crypto | AES-GCM; config `contraponto.git.credential-encryption-secret` (separate from ActivityPub) |
| Server env | Do **not** use `contraponto.git.username`/`password` for sync auth (deprecate for auth) |
| Remotes | HTTPS (existing `OutboundHttpsUrlValidator`) **or** SSH (`git@host:path` / `ssh://`); block private/link-local hosts for SSH too |
| JGit SSH | `org.eclipse.jgit.ssh.apache` with in-memory key from decrypted PEM |
| Host keys | Persist `known_hosts` under the blog Git workspace; accept-and-pin on first successful connect (**AQ2**) |
| UI | Never echo secrets; blank field = keep; clear control removes ciphertext |
| Failures | Missing/invalid credentials → failed **Git sync run** with auth remediation |

### Consequences

* Pro: Authors self-serve private remotes; no shared operator PAT.
* Pro: Aligns with domain invariants (credentials per blog; encrypted; no fallback).
* Con: Operators must set encryption secret before any blog can store credentials; rotation requires re-entry of secrets.
* Con: SSH adds dependency and host-key management.
* Other: Complements [ADR-0003](0003-frontend-qute-htmx.md) (settings form + Sync now), [ADR-0005](0005-postgresql-database.md) (Flyway columns), [ADR-0015](0015-federation-outbound-fetch-ssrf.md) (outbound host blocking spirit for SSH).

### Confirmation

* Unit: encrypt/decrypt round-trip; blank keep / clear; no plaintext in `settingsSnapshot`.
* Integration: HTTPS fetch with blog PAT; SSH fetch with deploy key; rejected private SSH host.
* Regression: auto sync off skips poll/export; Sync now schedules `MANUAL` run.

## Changelog

| Data | Evento | Detalhe |
|------|--------|---------|
| 2026-07-10 | proposed | Per-blog encrypted credentials + SSH remotes; no server auth fallback. |

## More Information

* [feature/git-sync.md](../../feature/git-sync.md) v2
* [docs/git-jekyll-convention.md](../git-jekyll-convention.md)
* ActivityPub key encryption pattern: `ActivityPubKeyPairService` (reference only; separate secret)
