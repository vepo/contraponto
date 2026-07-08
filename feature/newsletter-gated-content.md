# Newsletter with subscription-gated content

**Feature version:** 1  
**Status:** planned  
**Production:** not deployed

## Changelog

### Newsletter with subscription-gated content — 2026-07-08

**Version:** 1  
**Status:** planned

**Description:** Authors compose **newsletter editions** — email broadcasts distinct from automatic **new-post alerts** ([blog-audience.md](blog-audience.md)). Each edition has a **public teaser** (visible to everyone on the web archive) and a **subscriber-only body** (full content delivered by email and shown on the web only to active **newsletter subscribers**). Subscribers hold an **assinatura** (paid or free membership — FQ2) scoped per **blog** (default) or per author (FQ1). Non-subscribers see the teaser plus a subscribe CTA; subscribers see the full edition on the web and in their inbox.

**Domain model:** pending phase 1b — extend ubiquitous language (Newsletter edition, Subscriber-only body, Newsletter subscription, Assinatura).

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| [blog-audience.md](blog-audience.md) | **Distinct** channel — post alerts remain automatic on publish; newsletter is author-composed on demand. May share email infrastructure and audience tables or add parallel subscription model (FQ3). |
| [post-publishing.md](post-publishing.md) | Optional link from edition to published posts; no change to publish workflow in v1 unless FQ11 answered yes. |
| [platform-support-pix.md](platform-support-pix.md) | If paid assinatura uses PIX, payment confirmation may grant newsletter access (FQ12). |
| [authentication.md](authentication.md) | Subscribe flow may require sign-in (FQ4); guest email-only subscribers possible if FQ4 allows. |
| [custom-pages.md](custom-pages.md) | Newsletter archive is **not** a custom page — dedicated routes and templates. |
| [seo.md](seo.md) | Public teaser indexable; subscriber-only body must not leak in HTML/meta for guests (FQ8). |
| [rss-syndication.md](rss-syndication.md) | RSS unchanged in v1 — newsletter content excluded from post feeds (FQ9). |
| [notification-retention.md](notification-retention.md) | Optional in-app notification when a new edition ships to followers (FQ10). |
| [user-administration.md](user-administration.md) | Admin may need subscriber list export or refund tooling if paid (FQ13). |
| Deployment | Mailer config; optional payment webhook env vars if paid tier ships in v1. |

## Summary

Contraponto today sends **transactional email on publish** when a reader toggles **Subscribe by email** on a blog ([blog-audience.md](blog-audience.md)). That is a **notification**, not a curated **newsletter**.

This feature adds:

1. **Newsletter editions** — author-composed Markdown messages with title, optional cover, **teaser** (public), and **subscriber body** (gated).
2. **Newsletter subscription (assinatura)** — relationship between a reader and a blog (or author) granting access to subscriber-only content and email delivery of full editions.
3. **Public archive** — paginated list of sent editions per blog; guest sees teaser only; subscriber sees full body on the web.
4. **Author workspace** — compose, preview, send, and review past editions from the blog **Manage** hub.

**Not in scope for v1 (unless FQs reopen):**

- Cross-blog platform newsletter operated by editors
- RSS/ActivityPub syndication of subscriber-only body
- Automated drip sequences or A/B testing
- Refund/chargeback automation beyond manual admin (FQ13)

**Depends on (Accepted ADRs):**

- [ADR-0002](../docs/adr/0002-backend-java-quarkus-jakarta-ee.md) — Quarkus mailer, scheduled send  
- [ADR-0003](../docs/adr/0003-frontend-qute-htmx.md) — Manage hub + public read templates  
- [ADR-0005](../docs/adr/0005-postgresql-database.md) — Edition + subscription schema  
- [ADR-0013](../docs/adr/0013-cdi-events-cross-context.md) — optional `NewsletterEditionSentEvent` for notifications  

**New ADR (phase 2):** likely **ADR-0017** — newsletter vs post-alert separation, subscriber-only content exposure rules, email + web parity (Architect draft → `Proposed`).

## Wireframe

| Field | Value |
|-------|--------|
| **Source** | ASCII below |
| **Last updated** | 2026-07-08 |

### Screen: Blog Manage — Newsletter (`GET /manage/blogs/{id}/newsletter`)

New hub section under **Manage → {blog}** (alongside audience, Git sync). Owner-only.

```
┌─ Newsletter ───────────────────────────────────────────────┐
│ Send curated email editions to your subscribers.           │
│ Post alerts (on publish) are separate — see Audience.      │
│                                                            │
│ [ + New edition ]                          [ Subscribers ] │
│                                                            │
│ ┌ Sent editions ─────────────────────────────────────────┐ │
│ │ Title              Sent at        Recipients   Status  │ │
│ │ Março 2026         2026-03-01     142          Sent    │ │
│ │ Boas-vindas        2026-01-15     89           Sent    │ │
│ │ Rascunho fev       —              —             Draft   │ │
│ └────────────────────────────────────────────────────────┘ │
│ manage-pagination                                          │
└────────────────────────────────────────────────────────────┘
```

### Screen: Compose / edit edition (`GET /manage/blogs/{id}/newsletter/editions/{editionId}/edit`)

Reuse **write** toolbar patterns where sensible; two content regions.

```
┌─ New newsletter edition ───────────────────────────────────┐
│ Title: [________________________________]                    │
│                                                            │
│ Public teaser (visible to everyone on the archive)         │
│ ┌────────────────────────────────────────────────────────┐ │
│ │ Markdown editor — short hook, no spoilers              │ │
│ └────────────────────────────────────────────────────────┘ │
│                                                            │
│ Subscriber-only body (email + web for assinantes)          │
│ ┌────────────────────────────────────────────────────────┐ │
│ │ Markdown editor — full content                         │ │
│ └────────────────────────────────────────────────────────┘ │
│                                                            │
│ [ Preview ]  [ Save draft ]  [ Send now ▼ ]                │
│              confirm modal: recipient count + irreversible │
└────────────────────────────────────────────────────────────┘
```

### Screen: Public newsletter archive (`GET /{username}/newsletter` or `/{username}/{blogSlug}/newsletter`)

Linked from blog home sidebar/footer when at least one edition sent (FQ5).

```
┌─ Newsletter — @alice ──────────────────────────────────────┐
│ Teaser list (newest first)                                 │
│                                                            │
│ ┌ Março 2026 ────────────────────────────────────────────┐ │
│ │ Public teaser paragraph…                               │ │
│ │ [ Assine para ler o conteúdo completo ]  (guest)       │ │
│ │ — or full body if subscriber —                         │ │
│ └────────────────────────────────────────────────────────┘ │
│                                                            │
│ pagination                                                 │
└────────────────────────────────────────────────────────────┘
```

### Screen: Edition detail (`GET /{username}/newsletter/{editionSlug}`)

Single edition; SEO title from edition title; meta description from teaser plain text only (FQ8).

### Screen: Subscribe CTA (inline + modal)

On archive/detail when guest or signed-in non-subscriber:

```
┌─ Assine a newsletter ──────────────────────────────────────┐
│ Receba o conteúdo completo por email e desbloqueie aqui.    │
│                                                            │
│ [ Assinar ]  → sign-in gate if FQ4 requires account        │
│              → payment step if FQ2 = paid                  │
└────────────────────────────────────────────────────────────┘
```

### Screen: Account hub — My newsletter subscriptions (`GET /account/newsletter-subscriptions`)

Optional mirror of `/account/subscriptions` for assinaturas (FQ6).

```
┌─ Newsletter subscriptions ─────────────────────────────────┐
│ Blog                    Status        [ Unsubscribe ]      │
│ @alice / main           Active                               │
└────────────────────────────────────────────────────────────┘
```

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | **`newsletter`** (new) under Reader engagement + Author workspace; depends on `blog`, `user`, `notification`; may integrate `payment` if PIX linked |
| Packages | `dev.vepo.contraponto.newsletter` (TBD in Architecture) |
| API / routes | Manage CRUD + send; public archive/detail; subscribe/unsubscribe forms; optional admin subscriber export |
| UI | Manage hub section; public archive pages; subscribe CTA; Account hub list (FQ6) |
| Schema | `tb_newsletter_editions`, `tb_newsletter_subscriptions`, `tb_newsletter_edition_deliveries` (email log); optional `tb_newsletter_payments` if paid v1 |
| Email | Batch send with mock mailer in `%dev`; teaser vs full body templates |
| **`dev-import.sql`** | One blog with sent edition (teaser public + body for subscribers); `dave` or new persona as subscriber |
| Tests | Unit (render split, access gate); REST (subscribe); `@WebTest` (guest sees teaser, subscriber sees body) |
| Docs | domain-spec, feature-catalog, htmx-events, deployment.md |

### Risks

| Risk | Mitigation |
|------|------------|
| **Subscriber-only body leaked** in HTML/JSON/RSS for guests | Server-side gate in template; no full body in DOM for non-subscribers; integration test asserts absence (FQ8) |
| **Email batch overload** on large lists | Async queue + rate limit; progress in manage UI (FQ7) |
| **Confusion with post email subscribe** | Clear copy on Audience vs Newsletter manage sections; separate toggles |
| **Unpaid access** if payment webhook delayed | Do not grant assinatura until payment confirmed (FQ12) |
| **LGPD / unsubscribe** | One-click unsubscribe link in every edition email; Account hub list (FQ6) |
| **SEO duplicate** teaser vs full | Canonical on edition URL; full body not in meta (FQ8) |

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | **Scope:** assinatura per **blog** (default) or per **author** across all blogs? | open | |
| FQ2 | **Pricing:** free assinatura only in v1, **paid** via PIX ([platform-support-pix.md](platform-support-pix.md)), or both tiers? | open | |
| FQ3 | **Reuse `tb_blog_audience.email_subscribed`** for newsletter delivery, or **separate** `tb_newsletter_subscriptions`? | open | |
| FQ4 | **Subscribe flow:** require **signed-in account**, or allow **email-only** guest subscribers (magic link)? | open | |
| FQ5 | **Discovery:** link archive from blog home **sidebar**, **footer**, both, or Manage-only URL? | open | |
| FQ6 | **Account hub:** dedicated **Newsletter subscriptions** page vs extend existing `/account/subscriptions`? | open | |
| FQ7 | **Send UX:** synchronous send vs **queued** batch with retry (large lists)? | open | |
| FQ8 | **Web exposure:** subscriber-only body never in HTML for guests (strict) vs blurred preview? | open | |
| FQ9 | **RSS / ActivityPub:** exclude newsletter entirely from feeds in v1? | open | |
| FQ10 | **In-app notification** to blog **followers** when a new edition sends (in addition to email)? | open | |
| FQ11 | **Republish post into edition:** one-click embed latest post excerpt in compose — v1 or later? | open | |
| FQ12 | **Payment:** if paid, grant assinatura only after PIX webhook confirms (ties to platform-support-pix)? | open | |
| FQ13 | **Admin:** export subscriber emails / manual revoke for abuse — v1 scope? | open | |

**Gate:** phase 1b / 2 requires blocking **FQ*n*** answered or marked `not valid`. **Blocking (recommended):** FQ1–FQ4, FQ8, FQ12 (if paid).

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | Separate bounded context package `newsletter` vs extend `notification`? | open | Recommend **`newsletter`** — distinct aggregate lifecycle |
| AQ2 | Edition slug: auto from title vs author-editable? | open | |
| AQ3 | Send pipeline: CDI event + async worker vs inline `@Transactional` loop? | open | Recommend **async queue** (parity with email log pattern) |
| AQ4 | Access check: `NewsletterAccess` vs extend `BlogAccess`? | open | |
| AQ5 | Paid tier: shared payment tables with platform-support-pix or newsletter-owned? | open | Depends on FQ2/FQ12 |

## Architecture

> **Phase 2 — Architect Agent.** Fill ADR-0017, HTMX model, and tables below; set status `architecture-ready`. No production code until phases 1–4 complete.

### ADRs aplicáveis

| ADR | Status | Relevância |
|-----|--------|------------|
| [0002](../docs/adr/0002-backend-java-quarkus-jakarta-ee.md) | Accepted | Mailer, scheduler |
| [0003](../docs/adr/0003-frontend-qute-htmx.md) | Accepted | Manage + public templates |
| [0005](../docs/adr/0005-postgresql-database.md) | Accepted | Schema |
| [0013](../docs/adr/0013-cdi-events-cross-context.md) | Accepted | Optional send event |
| ADR-0017 (TBD) | Proposed | Newsletter vs post-alert; gated content rules |

### Design específico da feature (draft)

_To be filled in phase 2._

### HTMX component model (draft)

_To be filled in phase 2._

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Author composes edition with public teaser + subscriber body | — | ☐ |
| FC2 | Send delivers full body by email to active assinantes | FQ3 | ☐ |
| FC3 | Public archive shows teaser only for guests | FQ8 | ☐ |
| FC4 | Subscriber sees full body on web archive/detail | FQ4 | ☐ |
| FC5 | Subscribe / unsubscribe flows | FQ4, FQ6 | ☐ |
| FC6 | Manage hub lists editions + draft/send states | — | ☐ |
| FC7 | Paid assinatura gated on payment confirm (if FQ2 paid) | FQ12 | ☐ |
| FCdev | `dev-import.sql` — sample edition + subscriber persona | dev-import | ☐ |

#### Tasks (phase 3)

_To be filled by Task Modeller after architecture-ready and FQ gate._

#### Test coverage (phase 3)

_To be filled by Task Modeller._

## Related capabilities

| Capability | Relationship |
|------------|--------------|
| [blog-audience.md](blog-audience.md) | Post alerts on publish — parallel email channel |
| [platform-support-pix.md](platform-support-pix.md) | Optional payment for paid assinatura |
| [post-publishing.md](post-publishing.md) | Content may reference posts; no workflow merge in v1 |
| [custom-pages.md](custom-pages.md) | Static pages unchanged; newsletter uses dedicated routes |
