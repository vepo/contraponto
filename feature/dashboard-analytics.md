# Dashboard & platform analytics

**Feature version:** 2  
**Status:** review-ready  
**Production:** live (baseline) — referrer tracking not yet live

## Changelog

### Production baseline — 2026-07-07

**Version:** 1  
**Status:** done

**Production:** live — deployed capability

### Referrer tracking & top sources — 2026-08-05

**Version:** 2  
**Status:** review-ready

**Production:** not yet live

**Description:** Capture the HTTP `Referer` header on each recorded post **View** (normalized to host only, e.g. `google.com`, `t.co`) and surface a **top sources** ranking (raw top hosts by count, no bucketing) in both the author **Dashboard analytics** panel and **Platform insights**, scoped to the same month/year selector already used by each.

**Domain model:** updated 2026-08-05 (see domain-spec — `View` gains **Referrer host**; new **Top source** concept)

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| `dashboard-analytics` (this file) | Adds a "Top sources" section to both analytics fragments |
| `post-publishing` / `PostEndpoint` | `renderPost` reads one more request header at the existing view-recording call site |
| Privacy | Only host is stored (no path/query string); same recording gate as existing views (`PostEngagementService.shouldRecordReaderEngagement`, author self-access excluded) |

## Summary

**Authors** view per-blog **dashboard analytics** in Manage hub (`/manage/dashboard`): monthly views, reading time, notifications, audience metrics with month comparison. **Administrators** view **platform insights** (`/administration/insights`): site-wide views, unique visitors, highlights, comments. **View** records post reads in `tb_views`. *(v2, planned)* **View** also records the normalized **Referrer host** of the request (from the `Referer` HTTP header, host-only, blank when absent), and both analytics panels gain a **Top sources** ranking (raw top hosts by view count, no categorization).

## Wireframe

| Screen | Route | Notes |
|--------|-------|-------|
| Manage dashboard | `GET /manage`, `/manage/dashboard` | Default Manage panel |
| Analytics fragment | `GET /manage/dashboard/components/analytics` | Month/year selectors; *(v2)* adds **Top sources** list (host, view count), same month scope as views |
| Platform insights | `GET /administration/insights` | Admin only |
| Insights fragment | `GET /administration/insights/components/analytics` | Platform-wide series; *(v2)* adds **Top sources** list (host, view count) across all blogs |

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `dashboard`, `platforminsights`, `view` |
| Schema | `tb_views` — *(v2)* new nullable `referrer_host` column |
| Recording | `ViewRepository.recordView` from `PostEndpoint` — *(v2)* extra parameter |
| Tests | `DashboardTest`, `DashboardAnalyticsTest`, `PlatformInsightsTest`, `ViewTest` — *(v2)* extend with referrer-host cases |

### Risks

| Risk | Mitigation |
|------|------------|
| Referer header can be spoofed or absent (privacy-conscious browsers/extensions increasingly strip it) | Top sources is informational only, not used for any access-control or billing decision; rows with no Referer are simply excluded from the ranking (not shown as a fake "Direct" bucket per FQ2 scope) |
| Storing any part of a cross-site header could be seen as visitor tracking | Host-only normalization (FQ1) — no path, query string, or fragment ever persisted |
| Unbounded distinct-host growth (referrer spam / low-value crawlers) | Aggregation is `TOP N` by count per month, not a stored leaderboard — no separate table, so no unbounded row growth beyond one `tb_views` row per view (unchanged from today) |

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Real-time analytics? | answered | **No** — aggregated from `tb_views` and related tables |
| FQ2 | Storage granularity for captured `Referer`? | answered | **Host only** (e.g. `google.com`), stripped of path/query/fragment before persisting — no raw Referer string stored |
| FQ3 | Presentation of "top sources"? | answered | **Raw top hosts by count** — no bucketing into Search/Social/Direct/Other categories |

## Architecture

### ADRs aplicáveis

None — single-feature schema addition (one nullable column on `tb_views`) with no cross-bounded-context or transversal impact; per [adr.mdc](../.cursor/rules/adr.mdc) § "When to create ADR", this stays in the feature Architecture section rather than a new ADR.

### Design específico

| Area | Design |
|------|--------|
| Author | `DashboardAnalyticsService` — blog-scoped monthly series; *(v2)* + top referrer hosts for the blog's posts |
| Platform | `PlatformInsightsService` — cross-tenant aggregates; *(v2)* + top referrer hosts platform-wide |
| Anonymous dedup | `SessionIdProvider` on view records |
| *(v2)* Referrer capture | `PostEndpoint.renderPost` reads `headers.getHeaderString("Referer")`, normalizes to host (parse + `getHost()`, discard on parse failure or blank), passes to `ViewRepository.recordView` |
| *(v2)* Referrer schema | `tb_views.referrer_host VARCHAR(255)`, nullable |
| *(v2)* Top sources query | New `ViewRepository` aggregate: `GROUP BY referrer_host` where not null, `ORDER BY COUNT(*) DESC`, `LIMIT 10`, scoped by blog+month (dashboard) or platform+month (insights) — same month/year parameters already accepted by `countDailyByBlogId` / `countDailyPlatform` |

### HTMX component model

| Component | Route | Activator | Target/swap | Events out/in | JS companion | Auth allowlist |
|-----------|-------|-----------|--------------|----------------|---------------|----------------|
| Dashboard analytics (extended) | `GET /manage/dashboard/components/analytics` | Month change (existing) | existing fragment target, inline swap | none new | none | existing (author, blog-scoped) |
| Platform analytics (extended) | `GET /administration/insights/components/analytics` | Month change (existing) | existing fragment target, inline swap | none new | none | existing (admin only) |

No new fragment routes, no new custom events — "Top sources" is an additional `<section>` rendered inline in the two existing analytics fragments, refreshed by the month/year selector that already triggers those `hx-get` calls. No `htmx-events.md` delta.

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | Top-N cutoff for the "top sources" list? | answered | **Top 10** hosts per panel |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Author dashboard with analytics | FQ1 | ☑ |
| FC2 | Platform insights for admin | FQ1 | ☑ |
| FC3 | Post view recording | FQ1 | ☑ |
| FCdev | Sample views in seed | FQ1 | ☑ |
| FC4 | `tb_views` captures normalized referrer host, host-only, on every recorded view | FQ2 | ☑ |
| FC5 | Dashboard analytics shows top referrer hosts for the selected blog + month | FQ3 | ☑ |
| FC6 | Platform insights shows top referrer hosts platform-wide + month | FQ3 | ☑ |
| FCdev2 | `dev-import.sql` seeds sample views with varied `referrer_host` values | dev-import-sql-safety | ☑ |

#### Tasks (phase 3) — layer tags

| ID | Layer | Task | Depends | Expected outcome | Tests | Done |
|----|-------|------|---------|-------------------|-------|------|
| T1-java | java | Flyway migration adding nullable `tb_views.referrer_host VARCHAR(255)` | — | Schema supports storing normalized referrer host | TC1 | ☑ |
| T2-java | java | `View` entity gains `referrerHost` field, constructor param, getter | T1-java | Entity maps new column | TC1 | ☑ |
| T3-java | java | `ViewRepository.recordView(...)` accepts and persists `referrerHost` | T2-java | Insert writes `referrer_host` | TC1 | ☑ |
| T4-java | java | `PostEndpoint.renderPost` reads `Referer` header, normalizes to host only (parse failure/absent → `null`), passes to `recordView` | T3-java | Every recorded view captures normalized host or `null`, gated by the existing `PostEngagementService.shouldRecordReaderEngagement` (author self-access still excluded) | TC2, TC3 | ☑ |
| T5-java | java | `ViewRepository` top-referrer aggregate: `GROUP BY referrer_host` (non-null only), `ORDER BY COUNT(*) DESC`, `LIMIT 10` — blog-scoped (`topReferrersByBlogId`) and platform-wide (`topReferrersPlatform`) variants, month/year scoped like `countDailyByBlogId`/`countDailyPlatform` | T4-java | Ranked host+count list, capped at 10, per blog+month or platform+month | TC4 | ☑ |
| T6-java | java | `DashboardAnalyticsService.load(...)` + `DashboardAnalytics` record gain `topReferrers` | T5-java | Dashboard analytics payload includes top-10 referrer hosts for the selected blog+month | TC5 | ☑ |
| T7-java | java | `PlatformInsightsService.load(...)` + `PlatformInsights` record gain `topReferrers` | T5-java | Platform insights payload includes top-10 referrer hosts platform-wide for the selected month | TC6 | ☑ |
| T8-htmx | htmx | `templates/DashboardEndpoint/analytics.html` — new "Top sources" section (host, view count) | T6-java | Author sees ranked referrer hosts in Manage dashboard | TC7 | ☑ |
| T9-htmx | htmx | `templates/PlatformInsightsEndpoint/analytics.html` — new "Top sources" section (host, view count) | T7-java | Admin sees ranked referrer hosts platform-wide | TC8 | ☑ |
| Tdev | java | `dev-import.sql` seeds `tb_views` rows with varied `referrer_host` (including some `null`) for the happy-path seed | T1-java | Dev environment shows non-empty "Top sources" out of the box | — | ☑ |

**Development approval:** approved 2026-08-05 — tasks: T1-java, T2-java, T3-java, T4-java, T5-java, T6-java, T7-java, T8-htmx, T9-htmx, Tdev

No `T*-js` tasks — no new custom events, no JS lifecycle coupling (existing month/year `hx-get` triggers the whole fragment, per HTMX component model above).

#### Test coverage (phase 3)

| ID | Kind | Test | Covers | Done |
|----|------|------|--------|------|
| TC1 | unit | `ViewTest` — insert/read round-trip with `referrer_host` set and with `null` | T1-java, T2-java, T3-java | ☑ |
| TC2 | unit | Referer→host normalization: valid URL → host; malformed value → `null`; header absent → `null` | T4-java | ☑ (covered by `PostEndpointReferrerTest`, rest/web level) |
| TC3 | rest/web | `PostEndpoint` view recording captures `referrer_host` from a request's `Referer` header; author self-access still excluded | T4-java | ☑ |
| TC4 | unit | Top-referrer aggregate: ranks by count desc, excludes `null` hosts, caps at 10, respects blog/month and platform/month scoping | T5-java | ☑ |
| TC5 | unit | `DashboardAnalyticsTest` — `topReferrers` present and correctly scoped | T6-java | ☑ |
| TC6 | unit | `PlatformInsightsTest` — `topReferrers` present and correctly scoped | T7-java | ☑ |
| TC7 | web | `DashboardTest` (`@WebTest`) — "Top sources" section renders in Manage dashboard analytics fragment | T8-htmx | ☑ |
| TC8 | web | Platform insights `@WebTest` — "Top sources" section renders in insights analytics fragment | T9-htmx | ☑ |

**Implementation notes:** All tasks T1–T9 + Tdev implemented and verified. `GITHUB_ACTIONS=true mvn -B verify` green (full suite incl. ArchUnit, Spotless, `@WebTest`). Two bugs caught during TDD and fixed: (1) new "Top sources" section needed an explicit English `aria-label` since `data-i18n` swap is client-side only — the server-rendered fallback text is Portuguese, so tests checking rendered content needed the aria-label like other chart sections already have; (2) none — `mvnw` wrapper referenced by CLAUDE.md is not present in this checkout (verified absent across all branches/history), system `mvn` 3.9.12 (Java 25) was used instead; flagging for the user to add the wrapper or update CLAUDE.md.

**Review approval:** approved 2026-07-07 — production baseline (v1 only; v2 awaiting phase 6 review + phase 7 approval)
