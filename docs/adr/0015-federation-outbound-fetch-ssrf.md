# Federation outbound HTTPS fetch — SSRF controls

> **Status**: Accepted
>
> **Updated**: 2026-07-07
>
> **Aceitação / reabertura:** somente **manual** pelo usuário humano. O agente cria com `Proposed` e **não** muda para `Accepted` ou `Reopened` sem mensagem explícita (ex.: "Aceito o ADR-0015", "Reabro o ADR-0015 porque …"). Ver [development-process.mdc](../../.cursor/rules/development-process.mdc).

## Summary

Contraponto performs **outbound HTTPS GET** requests to remote Fediverse instances — first for **inbound HTTP Signature verification** (actor `publicKey` lookup per [ADR-0007](0007-activitypub-http-signatures.md)), and already for **signed delivery POST** in `ActivityPubDeliveryService`. Any server-controlled URL used for outbound fetch MUST pass shared **SSRF host blocking** and **per-domain rate limits** before the HTTP client runs.

## Drivers

* [ADR-0007](0007-activitypub-http-signatures.md) assumes remote `publicKey` is "fetched from actor document and cached" — v1.2 wires this on inbox verify; `keyId` embeds an HTTPS actor URI supplied by untrusted remotes.
* `GitRemoteUrlValidator` already blocks localhost, RFC1918, link-local, and metadata-style hosts for blog Git remotes — federation fetch needs the **same guarantees** without coupling `activitypub` to `git`.
* Burst of first-contact Follow activities can trigger many outbound GETs (fetch-storm / DoS amplification).
* Operators expect parity with Mastodon `FetchRemoteActorService` loopback checks ([mastodon-remote-account-resolution.md](../mastodon-remote-account-resolution.md)).

## Options

### A — Duplicate SSRF checks inside `activitypub` only

Copy `GitRemoteUrlValidator` logic into `ActivityPubRemoteActorService`.

### B — Extract shared `OutboundHttpsUrlValidator` in `shared.security`

Single validator used by `GitRemoteUrlValidator` (delegate) and `ActivityPubRemoteActorService`; HTTPS-only, DNS resolution to blocked ranges, reject missing host.

### C — Rely on egress firewall only

No application-layer URL validation.

## Options Analysis

### A Assessment

* Pro: No cross-package refactor.
* Con: Two copies drift when blocking rules change; violates DRY for a security control.

### B Assessment

* Pro: One place for SSRF rules; `git` and `activitypub` stay independent bounded contexts.
* Con: Small shared-kernel addition; `GitRemoteUrlValidator` becomes a thin wrapper.

### C Assessment

* Con: Unacceptable for multi-tenant SaaS; does not protect against `keyId` pointing at internal services.

## Recommendation

Adopt **option B**:

| Concern | Decision |
|---------|----------|
| URL scheme | **HTTPS only** for federation actor fetch (align with Git remote rule) |
| Host blocking | Reject localhost, `.local`, `.internal`, loopback, link-local, site-local, CGNAT `100.64.0.0/10`; treat DNS failure as blocked |
| `keyId` → fetch URL | Strip URI fragment (`#main-key`); remainder MUST equal actor document URL used for GET |
| HTTP method | **Unsigned GET** with `Accept: application/activity+json` (Mastodon public actor interop); authorized/signed fetch deferred |
| Rate limit | In-memory per **registrable domain** sliding window before outbound GET; configurable max per minute |
| Timeouts | Connect + request timeouts separate from delivery POST (shorter — inbox verify is synchronous) |
| Failure | Reject inbox verify → **401**; warn log with failure class; no response body enumeration |
| Kill-switch | `contraponto.activitypub.enabled=false` returns **404** on inbox **before** any outbound fetch |

Extract validator to `dev.vepo.contraponto.shared.security.OutboundHttpsUrlValidator`. `GitRemoteUrlValidator` delegates without behaviour change.

### Consequences

* Pro: Consistent SSRF posture across Git sync and Fediverse fetch.
* Pro: Rate limit caps fetch amplification from spam Follow waves.
* Con: In-memory per-domain limit is per JVM — acceptable for current single-node deploy; Redis-backed limiter is out of scope until [ADR-0014](0014-session-store.md) multi-node story matures.
* Other: Depends on [ADR-0006](0006-activitypub-federation.md), [ADR-0007](0007-activitypub-http-signatures.md).

### Confirmation

* Unit tests: blocked hosts rejected; valid public HTTPS allowed; `GitRemoteUrlValidator` regression green.
* Integration test: inbox verify triggers mock actor GET when no cached PEM.

## Changelog

| Data | Evento | Detalhe |
|------|--------|---------|
| 2026-07-07 | proposed | SSRF + per-domain rate limit for federation outbound GET (v1.2 remote actor fetch). |
| 2026-07-07 | accepted | Aceite manual do usuário. |

## More Information

* Parent: [ADR-0007](0007-activitypub-http-signatures.md)
* Feature: [activitypub-integration.md](../../feature/activitypub-integration.md) v1.2
* Reference: `GitRemoteUrlValidator`, Mastodon `fetch_remote_actor_service.rb`
