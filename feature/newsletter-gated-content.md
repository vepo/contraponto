# Newsletter with subscription-gated content

**Feature version:** 1  
**Status:** planned  
**Production:** not deployed

## Changelog

### Newsletter with subscription-gated content — 2026-07-08

**Version:** 1  
**Status:** planned

**Description:** Authors compose **newsletter editions** — email broadcasts distinct from automatic **new-post alerts** ([blog-audience.md](blog-audience.md)). Each edition has a **public teaser** and a **subscriber-only body**. A blog may be configured as a **newsletter blog**: **published posts default to paid/subscriber-only**; the author may mark individual posts **open** (public). **Assinatura** is **per blog** (FQ1), stored in a **separate subscription table** (FQ3) — not `tb_blog_audience`. **Free** and **paid** assinatura tiers coexist (FQ2): paid content **blocks non-payers** from reading on the web. Subscribe requires **sign-in** (FQ4). Gated HTML never includes real subscriber body or post content for unauthorized viewers — only a **blurred Lorem ipsum placeholder** (FQ8); real content is rendered server-side only after access check.

**Domain model:** pending phase 1b — extend ubiquitous language (Newsletter edition, Newsletter blog, Open post, Paid post, Assinatura, Free assinatura, Paid assinatura, Content placeholder).

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| [blog-audience.md](blog-audience.md) | **Distinct** channel — post alerts remain automatic on publish; newsletter assinatura uses **`tb_newsletter_subscriptions`** (FQ3). Share mailer only. |
| [post-publishing.md](post-publishing.md) | **Newsletter blogs:** new post **visibility** flag — **paid** (default) vs **open** (FQ3). Post read template gates body; RSS/SEO must respect visibility (FQ9). |
| [platform-support-pix.md](platform-support-pix.md) | **Paid assinatura** unlock via PIX payment confirmation (FQ12 — still open). |
| [authentication.md](authentication.md) | Subscribe / assinatura flows require **signed-in account** (FQ4); sign-in modal before subscribe CTA. |
| [custom-pages.md](custom-pages.md) | Newsletter archive is **not** a custom page — dedicated routes and templates. |
| [seo.md](seo.md) | Teaser + **open** posts indexable; paid post body and subscriber-only edition body **never** in HTML/meta/JSON-LD for unauthorized clients (FQ8). |
| [rss-syndication.md](rss-syndication.md) | RSS unchanged in v1 — newsletter content excluded from post feeds (FQ9). |
| [notification-retention.md](notification-retention.md) | Optional in-app notification when a new edition ships to followers (FQ10). |
| [user-administration.md](user-administration.md) | Admin may need subscriber list export or refund tooling if paid (FQ13). |
| Deployment | Mailer config; optional payment webhook env vars if paid tier ships in v1. |

## Summary

Contraponto today sends **transactional email on publish** when a reader toggles **Subscribe by email** on a blog ([blog-audience.md](blog-audience.md)). That is a **notification**, not a curated **newsletter**.

This feature adds:

1. **Newsletter blog mode** — blog setting: when enabled, **new published posts default to paid** (subscriber/payer-only). Author may set a post **open** (public) per post in the editor (FQ3).
2. **Newsletter editions** — author-composed Markdown with **teaser** (public) and **subscriber body** (gated); distinct from post alerts.
3. **Assinatura (per blog)** — separate **`tb_newsletter_subscriptions`**: **free** tier (sign-in + subscribe) and **paid** tier (PIX — FQ12). Paid tier required to read **paid** posts and full edition bodies on the web (FQ2).
4. **Strict content gate (FQ8)** — unauthorized viewers never receive real gated HTML. Post/edition pages render a **blurred Lorem ipsum placeholder** + subscribe/pay CTA; real content only when server confirms access.
5. **Public archive** — paginated sent editions; teaser for everyone; full body for authorized assinantes.
6. **Author workspace** — newsletter hub (editions, subscribers) + post visibility control in write/publish flow.

**Assinatura tiers (FQ2):**

| Tier | How to obtain | Web access |
|------|---------------|------------|
| **None** | — | Teaser + **open** posts only; paid posts show placeholder + CTA |
| **Free** | Sign in + subscribe (no payment) | FQ14 — email editions? open posts; paid posts still blocked unless FQ14 says otherwise |
| **Paid** | Sign in + PIX payment confirmed | Full **paid** posts + subscriber edition bodies |

**Default post visibility on newsletter blogs (FQ3):**

| Post flag | Who reads full body on web |
|-----------|----------------------------|
| **Open** | Anyone (guest or signed-in) |
| **Paid** (default when blog is newsletter) | **Paid assinantes** only; others see Lorem ipsum blur |

**Not in scope for v1 (unless FQs reopen):**

- Cross-blog platform newsletter operated by editors
- RSS/ActivityPub syndication of paid post body (FQ9)
- Automated drip sequences or A/B testing
- Refund/chargeback automation beyond manual admin (FQ13)
- Client-side decryption or “hidden in CSS” real content — **forbidden** (FQ8)

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
| **Last updated** | 2026-07-08 (FQ1–4, FQ8 impact) |

### Screen: Blog settings — Newsletter mode (`GET /blogs/{id}/edit` delta)

Fieldset alongside Git sync / Bluesky. Owner-only.

```
┌─ Newsletter ───────────────────────────────────────────────┐
│ [ ] This is a newsletter blog                                │
│     New posts default to paid (subscriber-only).             │
│     Mark individual posts as open in the editor.             │
│                                                            │
│ Free assinatura: sign-in + subscribe (no payment)          │
│ Paid assinatura: PIX payment — unlocks paid posts          │
└────────────────────────────────────────────────────────────┘
```

### Screen: Post editor — visibility (write toolbar delta)

When blog is newsletter mode, publish/save shows visibility control:

```
Visibility: (•) Paid — assinantes pagos   ( ) Open — público
```

Non-newsletter blogs: control hidden; all posts remain **open** (today's behaviour).

### Screen: Gated post read (`GET /{username}/post/{slug}` delta)

Unauthorized viewer (guest, signed-in non-payer, or free assinante on paid post):

```
┌─ Post title (public) ──────────────────────────────────────┐
│ Teaser / excerpt if configured                             │
│                                                            │
│ ┌─ [ blurred Lorem ipsum block — NOT real content ] ─────┐ │
│ │ Lorem ipsum dolor sit amet… (CSS blur)                 │ │
│ └────────────────────────────────────────────────────────┘ │
│                                                            │
│ [ Entrar ]  or  [ Assinar ]  or  [ Pagar com PIX ]         │
└────────────────────────────────────────────────────────────┘
```

**FQ8 rule:** response HTML contains **zero bytes** of real post body or edition body for unauthorized clients — only static placeholder text. No `display:none`, no commented-out content, no JSON-LD with body.

Authorized **paid** assinante: normal post render (unchanged layout).

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

On archive/detail/post gate when user lacks required tier:

```
┌─ Assine a newsletter ──────────────────────────────────────┐
│ Conteúdo exclusivo para assinantes.                        │
│                                                            │
│ Guest → [ Entrar ] (sign-in modal) then subscribe/pay      │
│ Signed-in free → [ Assinar grátis ] or [ Pagar com PIX ]   │
│ Signed-in, needs paid → [ Pagar com PIX ]                  │
└────────────────────────────────────────────────────────────┘
```

Sign-in **required** before any assinatura (FQ4).

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
| API / routes | Manage CRUD + send; public archive/detail; subscribe/unsubscribe + pay forms; gated post read; optional admin export |
| UI | Blog newsletter fieldset; post **open/paid** in write; gated post/edition **Lorem blur**; Manage newsletter hub; subscribe/pay CTAs; Account hub (FQ6) |
| Schema | `tb_blogs.newsletter_enabled`; post **visibility** (`OPEN`, `PAID`); `tb_newsletter_subscriptions` (tier `FREE`/`PAID`); editions + delivery log; PIX payment link |
| Email | Batch edition send; full body to assinantes only; mock mailer in `%dev` |
| **`dev-import.sql`** | One **newsletter blog** with paid default post + one **open** post; sample edition; `dave` free assinatura; persona with **paid** assinatura |
| Tests | Unit (`NewsletterAccess` tiers); REST (no body leak in response); `@WebTest` (DOM has no real gated text for guest; paid assinante sees body) |
| Docs | domain-spec, feature-catalog, htmx-events, deployment.md |

### Risks

| Risk | Mitigation |
|------|------------|
| **Gated content leaked** in HTML, HTMX fragments, RSS, or JSON | FQ8: server renders **Lorem ipsum placeholder only** for unauthorized; automated tests grep response for known post/edition phrases |
| **HTMX partial swap** exposes body | Fragment routes reuse same access gate; no separate unauthenticated content endpoint |
| **Email batch overload** on large lists | Async queue + rate limit (FQ7) |
| **Confusion with post email subscribe** | Separate tables and Manage copy; Audience vs Newsletter sections |
| **Unpaid access** if payment webhook delayed | Grant **paid** tier only after PIX confirm (FQ12) |
| **Free vs paid tier confusion** | Clear CTA copy; Account hub shows tier per blog (FQ6) |
| **SEO** | Paid posts: title/teaser indexable; body absent from meta and structured data |

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | **Scope:** assinatura per **blog** (default) or per **author** across all blogs? | answered | **Per blog** — one assinatura row per `(user, blog)`. |
| FQ2 | **Pricing:** free assinatura only in v1, **paid** via PIX, or **both** tiers? | answered | **Both.** **Paid** content **blocks non-payers** from reading full post/edition body on the web. |
| FQ3 | **Reuse `tb_blog_audience.email_subscribed`** for newsletter delivery, or **separate** table? Post visibility? | answered | **Separate `tb_newsletter_subscriptions`.** Blog may be **newsletter blog**; posts **default paid**; author may set a post **open** (public). |
| FQ4 | **Subscribe flow:** require **signed-in account**, or allow **email-only** guest subscribers? | answered | **Sign-in required** — no guest/email-only assinatura. |
| FQ5 | **Discovery:** link archive from blog home **sidebar**, **footer**, both, or Manage-only URL? | open | |
| FQ6 | **Account hub:** dedicated **Newsletter subscriptions** page vs extend existing `/account/subscriptions`? | open | |
| FQ7 | **Send UX:** synchronous send vs **queued** batch with retry (large lists)? | open | |
| FQ8 | **Web exposure:** strict gate vs blurred preview? | answered | **Strict server gate** — real content **never** in HTML for unauthorized clients. UI shows **blurred Lorem ipsum placeholder** + CTA; no DOM/CSS/comment leak vectors. |
| FQ9 | **RSS / ActivityPub:** exclude paid content from feeds in v1? | open | |
| FQ10 | **In-app notification** to blog **followers** when a new edition sends? | open | |
| FQ11 | **Republish post into edition:** one-click embed latest post excerpt — v1 or later? | open | |
| FQ12 | **Payment:** grant **paid** tier only after PIX webhook confirms? | open | |
| FQ13 | **Admin:** export subscriber emails / manual revoke — v1 scope? | open | |
| FQ14 | **Free tier access:** does **free** assinatura unlock **paid** posts, or only editions/email + **open** posts (paid still requires PIX)? | open | Recommend: free = editions + open posts; **paid posts require paid tier**. |

**Gate:** blocking FQs **FQ1–FQ4, FQ8** answered (2026-07-08). Remaining blocking for paid ship: **FQ12** (PIX confirm). **FQ14** recommended before architecture (tier rules).

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

| AQ5 | Paid tier: shared payment tables with platform-support-pix or newsletter-owned? | open | Depends on FQ12; FQ2 confirms paid tier ships |
| AQ6 | Post visibility: column on `Post` vs publication snapshot? | open | Recommend **publication snapshot** so republish does not change historical access |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Blog **newsletter mode** toggle; posts default **paid** when on | FQ3 | ☐ |
| FC2 | Post editor **open / paid** visibility on newsletter blogs | FQ3 | ☐ |
| FC3 | **Paid** post: unauthorized clients get Lorem blur only — no real body in HTML | FQ8 | ☐ |
| FC4 | **Open** post: readable by anyone (unchanged behaviour) | FQ3 | ☐ |
| FC5 | Separate **`tb_newsletter_subscriptions`**; per-blog assinatura | FQ1, FQ3 | ☐ |
| FC6 | Sign-in required to subscribe | FQ4 | ☐ |
| FC7 | Free + paid tiers; paid blocks non-payers from paid content | FQ2 | ☐ |
| FC8 | Author composes edition (teaser + subscriber body) + send | — | ☐ |
| FC9 | Paid assinatura granted only after PIX confirm | FQ12 | ☐ |
| FCdev | Newsletter blog seed: paid + open posts, free + paid personas | dev-import | ☐ |

#### Tasks (phase 3)

_To be filled by Task Modeller after architecture-ready and FQ gate._

#### Test coverage (phase 3)

_To be filled by Task Modeller._

## Related capabilities

| Capability | Relationship |
|------------|--------------|
| [blog-audience.md](blog-audience.md) | Post alerts on publish — parallel channel; separate assinatura table (FQ3) |
| [platform-support-pix.md](platform-support-pix.md) | **Paid assinatura** unlock via PIX (FQ2); webhook per FQ12 |
| [post-publishing.md](post-publishing.md) | **Newsletter blogs:** post visibility **open/paid**; default paid (FQ3) |
| [custom-pages.md](custom-pages.md) | Static pages unchanged; newsletter uses dedicated routes |
