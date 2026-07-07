# ActivityPub server-to-server federation

> **Status**: Accepted
>
> **Updated**: 2026-07-06

## Summary

Contraponto will implement the **ActivityPub server-to-server (S2S) federation profile** so **authors** can syndicate **published posts** to the Fediverse (Mastodon, Pleroma, Misskey, and other ActivityPub servers). Each opted-in **author** is exposed as an **ActivityPub actor** with **inbox**, **outbox**, and **followers** collections. **Publish** and **unpublish** events drive **Create**, **Update**, and **Delete** activities delivered to remote followers.

Client-to-server (C2S) ActivityPub and full bidirectional social graph replication are **out of scope** for the first release.

## Drivers

* Authors already store a **Mastodon profile URL** on their account; readers expect to **follow on Mastodon** and receive new posts without RSS-only workflows.
* ActivityPub is the **W3C Recommendation** for decentralized social networking ([ActivityPub](https://www.w3.org/TR/activitypub/)); Mastodon and most Fediverse apps speak it natively.
* Contraponto already reacts to **`PostPublishedEvent`** for RSS, notifications, and SEO — federation fits the same **CDI observer** pattern as [RSS cache invalidation](../cdi-events.md).
* Platform **blog subdomains** and **canonical URLs** must map cleanly to stable **actor IDs** and **object IDs** for interop.

## Options

### ActivityPub S2S (recommended scope)

Implement actor document, WebFinger, shared inbox, outbox paging, Follow/Accept/Reject, Create/Update/Delete delivery on publish lifecycle. Authenticate delivery with **HTTP Signatures**.

### RSS + manual “Share on Mastodon” link only

Keep syndication as RSS; authors copy post URL into Mastodon manually. `User.mastodonUrl` remains a profile link only.

### Bridgy Fed (third-party bridge)

Delegate federation to an external bridge service; Contraponto posts webhooks or feeds to the bridge.

### ActivityPub C2S + S2S full profile

Also expose C2S endpoints so Fediverse clients could post directly to Contraponto as if it were a Mastodon instance.

## Options Analysis

### ActivityPub S2S Assessment

* Pro: Native Mastodon **Follow** from `@user@contraponto.example`; automatic **Note/Article** on publish.
* Pro: Aligns with existing event-driven publish side effects.
* Pro: Differentiator vs generic blogging platforms.
* Con: Operational complexity (signatures, spam, delivery retries, key rotation).
* Con: Requires public HTTPS, stable hostnames, and careful **object ID** stability across republish.

### RSS + manual share Assessment

* Pro: Zero federation infrastructure.
* Con: Does not satisfy “integrate with Mastodon and other Fediverse applications” as first-class follow/delivery.

### Bridgy Fed Assessment

* Pro: Less in-repo crypto/delivery code.
* Con: External dependency; author identity still split; not self-hosted federation.

### Full C2S + S2S Assessment

* Pro: Maximum interop.
* Con: Large scope (inbox moderation UI, client auth, media upload protocol); duplicates Contraponto’s Qute write flow.

## Recommendation

Adopt **ActivityPub S2S** in a new bounded-context package **`dev.vepo.contraponto.activitypub`** under **Integration**, depending on `shared`, `user`, `blog`, `post`, and `image` (read-only via services/events).

**MVP delivery:**

| Capability | MVP | Later |
|------------|-----|-------|
| Actor (`Person`) per user (1:1) | Yes — main blog posts in outbox | Secondary blog actors; highlights/comments on same actor |
| WebFinger + `.well-known/host-meta` | Yes | — |
| Outbox + public Create/Update/Delete | Yes | Announce (boost) |
| Inbox + Follow/Accept | Yes | Block, Like |
| HTTP Signatures on inbound POST | Yes | — |
| Delivery queue + retries | Yes (DB-backed) | Dead-letter admin UI |
| Author opt-in + public key in DB | Yes | Per-blog toggle |
| ActivityPub C2S | No | Evaluate separately |
| Federated comments | No | Same **User** actor — future ADR |
| Federated highlights / notes | No | Same **User** actor — future ADR |

**Object mapping:** published posts → ActivityStreams **`Note`** (short) or **`Article`** (long-form); `attributedTo` → author actor; `url` / `canonical` → `BlogPublicUrlService` subdomain URL; content → sanitized HTML or summary + link (see feature doc **FQ3**).

**Side effects:** new observer `ActivityPubDeliveryObserver` on `PostPublishedEvent` / `PostUnpublishedEvent`; no direct calls from `PostPublicationService`.

### Consequences

* Pro: Authors gain Fediverse reach without leaving Contraponto as primary CMS.
* Pro: Reuses canonical URL and branding infrastructure.
* Con: New security surface (inbox spam, signature verification) — see [ADR-0007](0007-activitypub-http-signatures.md).
* Con: Dev seed must include at least one **federation-enabled** author for manual interop testing.
* Other: Feature work tracked in [feature/activitypub-integration.md](../../feature/activitypub-integration.md).

### Confirmation

* ArchUnit: `activitypub` may depend on documented contexts only.
* Mastodon interop checklist in feature **FC*n*** (Follow from Mastodon test instance; Create appears in home timeline).
* [test.activitypub.rocks](https://test.activitypub.rocks/) or documented manual test against `mastodon.social` / local Mastodon in dev.

## Changelog

| Data | Evento | Detalhe |
|------|--------|---------|
| 2026-07-06 | proposed | Rascunho inicial — S2S federation for author posts. |
| 2026-07-06 | amended | Actor model: one Person per **User**; highlights/comments deferred (same actor later). |
| 2026-07-06 | accepted | Aceito pelo usuário — S2S federation em vigor. |

## More Information

* [ActivityPub W3C Recommendation](https://www.w3.org/TR/activitypub/)
* Feature spec: [feature/activitypub-integration.md](../../feature/activitypub-integration.md)
* Security: [ADR-0007](0007-activitypub-http-signatures.md)
* Actors: [ADR-0008](0008-activitypub-actor-identity.md)
