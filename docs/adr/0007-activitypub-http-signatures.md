# ActivityPub HTTP Signatures and inbox security

> **Status**: Accepted
>
> **Updated**: 2026-07-06

## Summary

All **server-to-server** ActivityPub HTTP requests (inbound to Contraponto **inbox**, outbound **delivery** to remote inboxes) must use **HTTP Signatures** per the ActivityPub security model and [draft-cavage-http-signatures](https://datatracker.ietf.org/doc/html/draft-cavage-http-signatures). Contraponto stores each actor's **RSA key pair**; remote actors' public keys are fetched from their actor document and cached.

## Drivers

* ActivityPub § [Security Considerations](https://www.w3.org/TR/activitypub/#security-considerations) — verify sender identity on inbox POST.
* Fediverse implementations (Mastodon, Pleroma) **reject** unsigned or invalid deliveries.
* Federation exposes **spam and DoS** vectors; signature verification is the baseline gate before parsing Activity JSON.
* Contraponto already enforces **Content-Security-Policy** and HTML sanitization on reader surfaces; federation must not bypass those guarantees when embedding remote content (MVP: **outbound only** — no rendering of arbitrary inbound Activities in HTML).

## Options

### HTTP Signatures (RSA-SHA256) — ActivityPub default

Sign outbound requests; verify inbound with `Signature` and `Digest` headers; actor `publicKey` in actor JSON.

### Shared secret per remote instance

HMAC with pairwise secrets — non-standard for Fediverse.

### No verification (development only)

Accept unsigned POST in `%dev` — **never in production**.

## Options Analysis

### HTTP Signatures Assessment

* Pro: Required for Mastodon interop.
* Pro: Well-documented in Fediverse implementers' guides.
* Con: Key management, rotation, clock skew, header canonicalization bugs.

### Shared secret Assessment

* Con: Not supported by Mastodon; would break interop.

### No verification Assessment

* Con: Unacceptable in production; only as explicit `%dev` flag behind `activitypub.insecure-accept-unsigned=false` default.

## Recommendation

Implement **HTTP Signatures** with **RSA 2048-bit** keys:

| Concern | Decision |
|---------|----------|
| Key storage | `tb_activitypub_actor_keys` — private key encrypted at rest (app secret / env); public key in actor JSON |
| Inbound | Reject inbox POST if signature invalid → **401**; rate-limit per source IP + actor |
| Outbound | Sign every delivery POST; include `Date`, `Digest` (SHA-256 of body) |
| Key rotation | New key pair → Update actor JSON → grace period delivering with both keys (document in feature **AQ4**) |
| Inbox payload | Accept only whitelisted Activity types in MVP: `Follow`, `Undo` (Follow), `Accept`, `Reject` |
| Remote media | MVP outbound links to Contraponto images/posts; do not fetch arbitrary remote media on inbound |

**Forbidden in production:** processing inbox activities without verified signature; reflecting inbound object HTML in Qute templates without a future sanitization ADR.

### Consequences

* Pro: Interop with Mastodon follow + delivery flows.
* Con: Operators must protect private keys and rotation procedure.
* Other: Depends on [ADR-0006](0006-activitypub-federation.md) and [ADR-0008](0008-activitypub-actor-identity.md).

### Confirmation

* Unit tests: signature round-trip; reject tampered body.
* Integration test: mock remote inbox records `Signature` header on delivery.

## Changelog

| Data | Evento | Detalhe |
|------|--------|---------|
| 2026-07-06 | proposed | HTTP Signatures mandatory for S2S. |
| 2026-07-06 | accepted | Aceito pelo usuário — HTTP Signatures em vigor. |

## More Information

* [ActivityPub — Authentication and Authorization](https://www.w3.org/TR/activitypub/#authentication-and-authorization)
* Parent: [ADR-0006](0006-activitypub-federation.md)
