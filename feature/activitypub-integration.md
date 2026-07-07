# ActivityPub Fediverse integration

**Feature version:** 1  
**Status:** done  
**Requested:** 2026-07-06

## Summary

Enable **authors** to participate in the **Fediverse** via [ActivityPub](https://www.w3.org/TR/activitypub/) **server-to-server** federation: Mastodon (and compatible apps) users can **Follow** an author; **published posts** on the author's **main blog** appear as **Note/Article** activities in followers' timelines. Contraponto implements **actor**, **WebFinger**, **inbox**, **outbox**, and signed **delivery** — not a full Mastodon clone.

**Depends on (Accepted ADRs):**

- [ADR-0006](../docs/adr/0006-activitypub-federation.md) — S2S federation scope  
- [ADR-0007](../docs/adr/0007-activitypub-http-signatures.md) — HTTP Signatures  
- [ADR-0008](../docs/adr/0008-activitypub-actor-identity.md) — Actor identity & WebFinger  

## Wireframe

| Field | Value |
|-------|-------|
| **Source** | ASCII below |
| **Last updated** | 2026-07-06 |

### Screen: Author appearance — Fediverse section

```
┌─ Fediverse (ActivityPub) ─────────────────────────────┐
│ [ ] Publish my main blog posts to the Fediverse       │
│                                                       │
│ Your handle: @alice@blog.example.com                  │
│ Actor URL: https://alice.blog.example.com/            │
│                                                       │
│ [ Regenerate keys ]  (destructive — confirm modal)    │
│                                                       │
│ Followers on the Fediverse: 42                        │
│ Pending follow requests: 3  [ Review ]                │
└───────────────────────────────────────────────────────┘
```

### Screen: Manage — Fediverse follow requests (modal or panel)

```
┌─ Fediverse follow requests ─────────────────────────────┐
│ @reader@mastodon.social                               │
│ [ Accept ]  [ Reject ]                                │
│ ─────────────────────────────────────────────────────│
│ @bot@pleroma.example                                  │
│ [ Accept ]  [ Reject ]                                │
└───────────────────────────────────────────────────────┘
```

### Screen: Author profile (public) — Fediverse badge

```
Alice Ferreira
@alice@blog.example.com  (copy handle)
[ Mastodon profile link if mastodonUrl set ]
```

### N/A

No change to post editor wireframe in MVP (publish triggers delivery automatically when opt-in enabled).

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | New **`activitypub`** under **Integration**; observes `post`, `user`, `blog` via events |
| Packages | `dev.vepo.contraponto.activitypub` |
| API / routes | `/.well-known/webfinger`, `/.well-known/host-meta`, `/{username}/inbox`, `/{username}/outbox`, actor JSON, followers collection, shared inbox POST |
| UI | Author appearance toggle; optional profile badge; follow-request review |
| Schema | `tb_activitypub_actors`, `tb_activitypub_remote_actors`, `tb_activitypub_follows`, `tb_activitypub_deliveries`, `tb_activitypub_inbox_activities` (names TBD in Architecture) |
| **`dev-import.sql`** | `alice` federation-enabled with key pair; sample remote follow |
| Tests | Unit (signature, JSON-LD), `@QuarkusTest` inbox/outbox, optional `@WebTest` for settings UI |
| Docs | domain-spec, feature-catalog, cdi-events, ARCHITECTURE § syndication, ADRs 0006–0008 |

### Risks

* **Spam follows** and inbox abuse — mitigate with signature verification + rate limits ([ADR-0007](../docs/adr/0007-activitypub-http-signatures.md)).
* **Delivery failures** to remote instances — retry queue; authors may not know unless we add dashboard later.
* **Key rotation** breaks remote caches — document regen as destructive.
* **Republish / slug change** — object ID stability (**AQ3**).
* **HTML vs plain text** in Note content — Mastodon sanitization limits (**FQ3**).

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Should **follow requests** require author **manual Accept** (Mastodon locked account model) or **auto-Accept** all verified Follow activities? | answered | **Manual Accept** — pending requests in appearance panel |
| FQ2 | Include **secondary blog** posts in the same actor outbox in MVP, or **main blog only**? | answered | **Main blog only** (ADR-0008) |
| FQ3 | Activity **content**: full sanitized HTML body, summary + link only, or title + link only? | answered | **title + link** |
| FQ4 | Show **public follower count** on profile / appearance panel? | answered | **yes** |
| FQ5 | Platform **admin** kill-switch to disable federation globally? | answered | **yes** |

**Gate:** phase 3 requires blocking **FQ*n*** answered or marked `not valid`.

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | Delivery: synchronous on `PostPublishedEvent` vs **async queue** worker? | answered | **Async queue** (DB table + scheduler) — avoid blocking publish transaction |
| AQ2 | Shared inbox URL pattern: per-actor inbox vs platform **sharedInbox**? | answered | **Per-actor inbox** on subdomain host per ADR-0008 |
| AQ3 | Activity **object id** for post: canonical post URL vs stable UUID path `/posts/{uuid}`? | answered | **stable object id** |
| AQ4 | Private key encryption: Quarkus **SmallRye JWT** secret vs dedicated `ACTIVITYPUB_KEY_ENCRYPTION_SECRET`? | answered | **yes** — dedicated `ACTIVITYPUB_KEY_ENCRYPTION_SECRET` |
| AQ5 | Mastodon **NodeInfo** / **FediWellKnown** endpoints for discovery? | open | Optional v1.1 — not blocking MVP |

## Architecture

### ADRs aplicáveis

| ADR | Status | Relevância |
|-----|--------|------------|
| [0002](../docs/adr/0002-backend-java-quarkus-jakarta-ee.md) | Accepted | JAX-RS JSON-LD endpoints |
| [0003](../docs/adr/0003-frontend-qute-htmx.md) | Accepted | Settings UI |
| [0005](../docs/adr/0005-postgresql-database.md) | Accepted | Queue + actor tables |
| [0006](../docs/adr/0006-activitypub-federation.md) | Accepted | S2S scope |
| [0007](../docs/adr/0007-activitypub-http-signatures.md) | Accepted | Signatures |
| [0008](../docs/adr/0008-activitypub-actor-identity.md) | Accepted | One **Person** per **User**; highlights/comments deferred |

### Design específico da feature

| Area | Design |
|------|--------|
| **Bounded context** | `activitypub` — Integration (may depend on: shared, user, blog, post, image) |
| **Layers** | `ActivityPubActorEndpoint`, `ActivityPubInboxEndpoint`, `ActivityPubWebFingerEndpoint` → `ActivityPubActorService`, `ActivityPubInboxService`, `ActivityPubDeliveryService` → `*Repository` |
| **Events** | `@Observes PostPublishedEvent` → enqueue Create; `@Observes PostUnpublishedEvent` → enqueue Delete; optional `ActivityPubFollowAcceptedEvent` internal |
| **Actor JSON** | ActivityStreams 2.0 + `publicKey`; served with `application/activity+json` |
| **Post object** | `type`: `Article`; content format = **title + canonical link** (FQ3); `published`, `updated`, `url`, `to`: `Public`, `cc`: followers collection |
| **Follow flow** | Inbound `Follow` → store pending → author Accept → `Accept` activity to remote + add to followers collection |
| **Delivery** | POST signed JSON to remote `inbox` from actor record; exponential backoff |
| **Paths** | New `ActivityPubPaths` — no hardcoded URLs in templates |
| **Manage auth** | Actor owner enables federation; **admin global kill-switch required** (FQ5=yes) |
| **Tests** | Signature verify/generate; inbox rejects unsigned; outbox paging; delivery job marks success/failure |

### Architecture questions (AQ*n*)

See table above.

## Changelog

### Fediverse integration MVP — 2026-07-06

**Version:** 1  
**Status:** done

**Description:** ActivityPub S2S federation so authors syndicate main-blog posts to Mastodon and compatible servers.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Publish / unpublish | Observers enqueue ActivityPub deliveries |
| Author appearance | New Fediverse section |
| Author profile | Optional `@handle` display |
| RSS / SEO | Same canonical URLs in Activity objects |
| Notifications | Optional future: Fediverse follow → in-app notification |
| Blog audience Follow | **Distinct** — in-app follow ≠ ActivityPub Follow |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | WebFinger resolves `@user@domain` to actor | ADR-0008 | ☑ |
| FC2 | Actor JSON valid; inbox/outbox URLs work | ADR-0006 | ☑ |
| FC3 | Mastodon Follow → Accept → follower receives Create on publish | FQ1 | ☑ |
| FC4 | Unpublish sends Delete (or Update tombstone) | ADR-0006 | ☑ |
| FC5 | Unsigned inbox POST rejected in prod | ADR-0007 | ☑ |
| FC6 | Author opt-in off → actor 404 | ADR-0008 | ☑ |
| FC7 | Appearance UI matches wireframe | Wireframe | ☑ |
| FCdev | `alice` federation-enabled in dev-import | dev-import | ☑ |
| FC8 | domain-spec + cdi-events + feature-catalog updated | Docs | ☑ |

#### Tasks (phase 4 — approved)

| ID | Task | Done |
|----|------|------|
| T1 | Flyway: activitypub tables (actor, keys, follows, deliveries, inbox log) | ☑ |
| T2 | `ActivityPubActor` entity + repository + key generation | ☑ |
| T3 | Actor JSON + WebFinger + host-meta endpoints | ☑ |
| T4 | HTTP Signatures: sign outbound + verify inbound | ☑ |
| T5 | Inbox: Follow / Undo / Accept / Reject handling | ☑ |
| T6 | Outbox: paged OrderedCollection; object endpoints for posts | ☑ |
| T7 | `ActivityPubDeliveryService` + scheduler retry | ☑ |
| T8 | Observers: PostPublished / PostUnpublished → queue | ☑ |
| T9 | Map post → Note/Article JSON-LD | ☑ |
| T10 | Author appearance: enable toggle + handle display + HTMX save | ☑ |
| T11 | Follow-request review UI (if FQ1 = manual Accept) | ☑ |
| T12 | ArchUnit: activitypub dependency rules | ☑ |
| T13 | Admin global ActivityPub kill-switch (config + admin UI/guardrails) | ☑ |
| Tdev | dev-import.sql: alice actor + sample follow | ☑ |

**Development approval:** approved 2026-07-06 — tasks: T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12; approved 2026-07-07 — task: T13

#### Test coverage (phase 5)

| ID | Test | Covers | Done |
|----|------|--------|------|
| TC1 | `ActivityPubSignatureTest` round-trip | T4 | ☑ |
| TC2 | `ActivityPubInboxEndpointTest` rejects unsigned Follow | T4, T5 | ☑ |
| TC3 | `ActivityPubWebFingerTest` acct resolution | T3 | ☑ |
| TC4 | `ActivityPubDeliveryServiceTest` enqueues on publish event | T7, T8 | ☑ |
| TC5 | `ActivityPubOutboxTest` Create appears after publish | T6, T9 | ☑ |
| TC6 | `ActivityPubWebTest` appearance toggle | T10 | ☑ |
| TC7 | `BoundedContextRulesTest` includes activitypub | T12 | ☑ |
| TC8 | `ActivityPubInboxFollowTest` signed Follow → Accept → Create delivery | T5, T7, T8 | ☑ |
| TC9 | `ActivityPubDeliveryServiceTest` Delete on unpublish | T8 | ☑ |

**Implementation notes:** Package `dev.vepo.contraponto.activitypub` (33 classes). Actor/post JSON via `ActivityPubJsonResourceFilter` + `ActivityPubJsonResponder` (`Accept: application/activity+json`) on canonical HTML paths. Manual follow Accept (FQ1). Main-blog-only outbox (FQ2). Title + link in Create activities (FQ3). Admin global kill-switch on Platform insights (FQ5/T13). Alice federation seeded in `%dev` via `ActivityPubDevSeed` + `dev-import.sql`. Config: `contraponto.activitypub.enabled` (`%dev`/`%test` true). Automated: signature, WebFinger, inbox (unsigned reject + signed follow→accept→Create), outbox/actor JSON, delivery publish/unpublish, appearance WebTest, ArchUnit. **Manual:** Mastodon interop checklist below.

---

## Mastodon interop checklist (manual)

See **[docs/mastodon-remote-account-resolution.md](../docs/mastodon-remote-account-resolution.md)** for the full Mastodon resolve flow, production smoke commands, and gap analysis.

After implementation, verify against a test Mastodon instance:

1. **While logged in**, search `@vepo@commit-mestre.dev` (not profile URL alone) → profile loads.
2. Follow → Accept (if manual) → follow shows **Following**.
3. Publish post on Contraponto → appears in Mastodon home timeline with link.
4. Unpublish → post removed or marked deleted on Mastodon (best-effort).
5. [test.activitypub.rocks](https://test.activitypub.rocks/) suite where applicable.

**If search returns 0 accounts:** check ursal.zone domain blocks, Sidekiq logs, and remove `sameAs` Mastodon URL from Contraponto profile when `@vepo` exists locally on ursal.zone.

### Fediverse follow backfill and outbox paging — 2026-07-07

**Version:** 1.1  
**Status:** done

**Description:** When an author accepts a Fediverse follow, deliver historical main-blog **Create** activities to the new follower; expose standard outbox collection paging links.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Fediverse follow Accept | Enqueues historical **Create** deliveries (oldest first) |
| Outbox GET | Main-blog-only query; `first`/`last`/`next`/`prev` when paginated |

#### Feature checklist

| ID | Criterion | Done |
|----|-----------|------|
| FC9 | Accept follow backfills published main-blog posts to remote inbox | ☑ |
| FC10 | Outbox exposes paging links across multiple pages | ☑ |

#### Tasks

| ID | Task | Done |
|----|------|------|
| T14 | `enqueueHistoricalPostsForAcceptedFollow` on follow Accept | ☑ |
| T15 | Main-blog outbox query + pagination links | ☑ |
| T16 | Tests for backfill and outbox paging | ☑ |

**Development approval:** approved 2026-07-07 — tasks: T14, T15, T16

#### Test coverage

| ID | Test | Covers | Done |
|----|------|--------|------|
| TC10 | `ActivityPubInboxFollowTest.acceptFollowBackfillsHistoricalMainBlogPosts` | T14 | ☑ |
| TC11 | `ActivityPubDeliveryServiceTest.enqueueHistoricalPostsForAcceptedFollow` | T14 | ☑ |
| TC12 | `ActivityPubOutboxTest.outboxExposesPaginationLinksAcrossPages` | T15 | ☑ |

**Implementation notes:** Backfill runs after **Accept** delivery is queued; uses existing async delivery queue. Outbox page size remains 20; page 2+ uses `OrderedCollectionPage` with `partOf`.

---

## References

* [Mastodon remote account resolution](../docs/mastodon-remote-account-resolution.md)
* [W3C ActivityPub Recommendation](https://www.w3.org/TR/activitypub/)
* [ActivityStreams 2.0](https://www.w3.org/TR/activitystreams-core/)
* Mastodon ActivityPub documentation (implementers' notes)
