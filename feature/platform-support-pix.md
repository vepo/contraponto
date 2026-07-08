# Platform support page (PIX)

**Feature version:** 1  
**Status:** planned  
**Production:** not deployed

## Changelog

### Platform support page with PIX — 2026-07-08

**Version:** 1  
**Status:** planned

**Description:** A **platform-level support page** where visitors can **support Commit Mestre / Contraponto** via **PIX** (Brazil instant payment). The page explains why support matters, shows a **PIX QR code** and **copy-paste key**, and optionally confirms payment to unlock benefits (e.g. paid **newsletter assinatura** — FQ8). Operator configures PIX key and beneficiary name via **`%prod` env** (not author-managed). Page is reachable from the **global footer** and direct URL.

**Domain model:** pending phase 1b — extend ubiquitous language (Support page, PIX donation, PIX payment confirmation).

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| [custom-pages.md](custom-pages.md) | **Dedicated route** preferred over editable custom page — PIX QR must stay operator-controlled (FQ1). Footer link added alongside migration pages (`/sobre`, `/contato`, …). |
| [newsletter-gated-content.md](newsletter-gated-content.md) | **Paid assinatura** on newsletter blogs (FQ2) — PIX payment grants paid tier; webhook per newsletter FQ12 |
| [authentication.md](authentication.md) | Payment confirmation may require signed-in user to attach assinatura (FQ5). |
| [user-administration.md](user-administration.md) | Admin view of PIX confirmations / manual reconcile if no webhook (FQ4). |
| [seo.md](seo.md) | Public indexable support page; noindex on webhook/callback routes. |
| Deployment | New env vars for PIX key, beneficiary, optional PSP credentials; docker-smoke + `docs/deployment.md`. |

## Summary

Contraponto has migration-seeded footer pages (`/page/sobre`, `/page/contato`, …) but **no payment or support flow**. Brazilian users expect **PIX** for voluntary support.

This feature adds:

1. **Support page** — static narrative (i18n) + PIX payment block (QR + copia e cola).
2. **Operator configuration** — PIX key, display name, optional fixed suggested amounts (FQ3).
3. **Payment confirmation** — manual trust (user clicks “I paid”) vs PSP webhook (Mercado Pago, OpenPix, etc.) — FQ4.
4. **Footer discovery** — global footer link **Apoie** / **Support** (FQ2).

**Not in scope for v1 (unless FQs reopen):**

- Per-author tip jars on blog profiles
- Recurring PIX (Pix Automático) subscriptions
- Invoice/NF-e generation
- Multi-currency card payments

**Depends on (Accepted ADRs):**

- [ADR-0002](../docs/adr/0002-backend-java-quarkus-jakarta-ee.md) — HTTP client for PSP webhooks  
- [ADR-0003](../docs/adr/0003-frontend-qute-htmx.md) — Support page template + copy button  
- [ADR-0005](../docs/adr/0005-postgresql-database.md) — Optional payment intent log  

**New ADR (phase 2):** likely **ADR-0018** — PIX integration scope (static vs PSP), webhook trust, LGPD for payer metadata (Architect draft → `Proposed`).

## Wireframe

| Field | Value |
|-------|--------|
| **Source** | ASCII below |
| **Last updated** | 2026-07-08 |

### Screen: Support page (`GET /page/apoie` or `GET /support`)

Dedicated SSR page (not author-editable custom page body — FQ1). Matches public custom-page chrome (header/footer).

```
┌─ Apoie o Commit Mestre ─────────────────────────────────────┐
│                                                             │
│  [Intro copy — why support matters, what it funds]          │
│                                                             │
│  ┌─ Pague com PIX ────────────────────────────────────────┐ │
│  │  Beneficiário: Commit Mestre                           │ │
│  │                                                        │ │
│  │  [ QR code image ]     Chave PIX (copia e cola):       │ │
│  │                        ••••••••••••••••                 │ │
│  │                        [ Copiar chave ]                │ │
│  │                                                        │ │
│  │  Valor sugerido: R$ 10  R$ 25  R$ 50  (FQ3)            │ │
│  │  — or free amount in banking app —                     │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                             │
│  After paying (FQ4):                                        │
│  [ Já paguei — confirmar ]  → webhook or honor system       │
│                                                             │
│  Dúvidas? /page/contato                                     │
└─────────────────────────────────────────────────────────────┘
```

### Screen: Payment pending / thank-you (FQ4, FQ5)

If PSP returns async confirmation or user must sign in to link payment:

```
┌─ Obrigado ──────────────────────────────────────────────────┐
│  Estamos confirmando seu PIX. Você receberá um email quando │
│  o pagamento for reconhecido.                               │
│  — or —                                                     │
│  Pagamento confirmado! [ Ver benefícios ] (FQ8)             │
└─────────────────────────────────────────────────────────────┘
```

### Screen: Footer link (delta)

Global footer gains **Apoie** / **Support** next to Sobre, Contato, Privacidade, Termos (FQ2).

```
Sobre · Contato · Apoie · Privacidade · Termos · RSS
```

### Screen: Platform admin — PIX settings (optional v1)

If kill-switch or key rotation without redeploy is needed (FQ6):

```
┌─ PIX (support) ─────────────────────────────────────────────┐
│  [ ] Show support page in footer                            │
│  Chave PIX (read-only from env) ••••@email.com              │
│  [ Save ]  — toggles visibility only if key from env        │
└─────────────────────────────────────────────────────────────┘
```

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | **`payment`** or **`platformsupport`** under Integration / Platform insights; minimal coupling to `newsletter` if FQ8 |
| Packages | `dev.vepo.contraponto.platformsupport` or `payment.pix` (TBD phase 2) |
| API / routes | `GET` support page; `POST /forms/support/pix/copy` (analytics optional); `POST /webhooks/pix/{provider}` internal; optional `GET /components/support/pix-qr` |
| UI | Support page template; footer link; copy-to-clipboard (JS companion likely — FQ7) |
| Schema | Optional `tb_pix_payment_intents`, `tb_pix_payment_confirmations` if webhook tracking |
| Config | `%prod`: `contraponto.support.pix.enabled`, `.key`, `.beneficiary-name`, `.suggested-amounts`, PSP secrets |
| **`dev-import.sql`** | Footer link visible; `%dev` may use mock PIX key + placeholder QR |
| Tests | Unit (QR payload EMV); REST webhook signature; `@WebTest` page renders + copy button |
| Docs | domain-spec, feature-catalog, deployment.md, privacy/terms (payer data) |

### Risks

| Risk | Mitigation |
|------|------------|
| **Fake “I paid”** without webhook | Do not grant paid benefits on honor click alone (FQ4); webhook or manual admin confirm |
| **PIX key leak** in HTML | Key is public by design for PIX; document rotation procedure |
| **Webhook forgery** | Verify PSP signature; internal route prefix `/__pix_webhook__/` |
| **LGPD** | Store minimal payer metadata; retention TTL (ADR-0018) |
| **PSP vendor lock-in** | Abstract `PixPaymentProvider` if multiple PSPs (AQ3) |
| **Accessibility** | QR alt text + copia e cola always available; not QR-only |

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | **Page type:** fixed **dedicated template** (operator-controlled) vs **custom page** editable by editors? | open | |
| FQ2 | **URL + label:** `/page/apoie` + footer **Apoie**, or `/support` English path? | open | |
| FQ3 | **Suggested amounts:** show preset BRL chips (R$ 10/25/50), free-form only, or none? | open | |
| FQ4 | **Confirmation:** **PSP webhook** (Mercado Pago / OpenPix / other), **manual admin reconcile**, or **honor system** (“Já paguei”) without verification? | open | |
| FQ5 | **Account link:** must payer **sign in** before/after PIX to attach benefit, or email-only? | open | |
| FQ6 | **Admin UI:** footer visibility toggle without redeploy, or env-only in v1? | open | |
| FQ7 | **Copy button:** HTMX-only vs small **JS** module (`navigator.clipboard`) — Architect decides | open | |
| FQ8 | **Benefit:** support is **donation only**, or unlocks **newsletter assinatura** / platform badge? | open | |
| FQ9 | **QR generation:** static image from env vs **dynamic EMV QR** per amount/session? | open | |
| FQ10 | **i18n:** Portuguese-only copy or full en/es like rest of site? | open | |

**Gate:** phase 1b / 2 requires blocking **FQ*n*** answered or marked `not valid`. **Blocking (recommended):** FQ1, FQ4, FQ8, FQ9.

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | Package: `platformsupport` vs `payment` shared with future paid newsletter? | open | |
| AQ2 | QR: Java library for EMV BR Code vs pre-rendered PNG in static resources? | open | |
| AQ3 | PSP abstraction vs single-provider v1 implementation? | open | |
| AQ4 | Webhook ingress: `InternalRoutePrefixes` pattern like ActivityPub? | open | Recommend **yes** |
| AQ5 | New ADR-0018 for PIX scope + payer data retention? | open | Recommend **yes** |

## Architecture

> **Phase 2 — Architect Agent.** Fill ADR-0018, HTMX model, and tables below; set status `architecture-ready`. No production code until phases 1–4 complete.

### ADRs aplicáveis

| ADR | Status | Relevância |
|-----|--------|------------|
| [0002](../docs/adr/0002-backend-java-quarkus-jakarta-ee.md) | Accepted | Webhook client, config |
| [0003](../docs/adr/0003-frontend-qute-htmx.md) | Accepted | Support page |
| [0005](../docs/adr/0005-postgresql-database.md) | Accepted | Payment log tables |
| ADR-0018 (TBD) | Proposed | PIX integration + payer data |

### Design específico da feature (draft)

_To be filled in phase 2._

### HTMX component model (draft)

_To be filled in phase 2._

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Support page renders PIX QR + copia e cola | FQ9 | ☐ |
| FC2 | Copy-to-clipboard works on supported browsers | FQ7 | ☐ |
| FC3 | Footer link reaches support page | FQ2 | ☐ |
| FC4 | PSP webhook records confirmed payment (if FQ4 webhook) | FQ4 | ☐ |
| FC5 | Optional benefit unlock after confirm (if FQ8) | FQ8 | ☐ |
| FC6 | `%prod` env configures PIX key + beneficiary | — | ☐ |
| FCdev | `dev-import.sql` / dev config — page reachable from footer with mock PIX | dev-import | ☐ |

#### Tasks (phase 3)

_To be filled by Task Modeller after architecture-ready and FQ gate._

#### Test coverage (phase 3)

_To be filled by Task Modeller._

## Related capabilities

| Capability | Relationship |
|------------|--------------|
| [custom-pages.md](custom-pages.md) | Footer sibling links; support page is not editor-managed if FQ1 = dedicated |
| [newsletter-gated-content.md](newsletter-gated-content.md) | **Paid assinatura** on newsletter blogs unlocks paid posts + edition bodies (FQ2 answered); PIX webhook per FQ12 |
| [dashboard-analytics.md](dashboard-analytics.md) | Future: support revenue metrics (out of v1) |

## Operator notes

Example production env (names TBD in Architecture / `application.properties`):

```bash
CONTRAPONTO_SUPPORT_PIX_ENABLED=true
CONTRAPONTO_SUPPORT_PIX_KEY=contato@commit-mestre.dev
CONTRAPONTO_SUPPORT_PIX_BENEFICIARY_NAME=Commit Mestre
# Optional PSP (FQ4):
# CONTRAPONTO_SUPPORT_PIX_WEBHOOK_SECRET=...
```
