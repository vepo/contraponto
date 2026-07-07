# Dashboard & platform analytics

**Feature version:** 1  
**Status:** done  
**Production:** live

## Changelog

### Production baseline — 2026-07-07

**Version:** 1  
**Status:** done

**Production:** live — deployed capability

## Summary

**Authors** view per-blog **dashboard analytics** in Manage hub (`/manage/dashboard`): monthly views, reading time, notifications, audience metrics with month comparison. **Administrators** view **platform insights** (`/administration/insights`): site-wide views, unique visitors, highlights, comments. **View** records post reads in `tb_views`.

## Wireframe

| Screen | Route | Notes |
|--------|-------|-------|
| Manage dashboard | `GET /manage`, `/manage/dashboard` | Default Manage panel |
| Analytics fragment | `GET /manage/dashboard/components/analytics` | Month/year selectors |
| Platform insights | `GET /administration/insights` | Admin only |
| Insights fragment | `GET /administration/insights/components/analytics` | Platform-wide series |

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `dashboard`, `platforminsights`, `view` |
| Schema | `tb_views` |
| Recording | `ViewRepository.recordView` from `PostEndpoint` |
| Tests | `DashboardTest`, `DashboardAnalyticsTest`, `PlatformInsightsTest`, `ViewTest` |



### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Real-time analytics? | answered | **No** — aggregated from `tb_views` and related tables |

## Architecture

### Design específico

| Area | Design |
|------|--------|
| Author | `DashboardAnalyticsService` — blog-scoped monthly series |
| Platform | `PlatformInsightsService` — cross-tenant aggregates |
| Anonymous dedup | `SessionIdProvider` on view records |

### HTMX component model

| Component | Route | Activator |
|-----------|-------|-----------|
| Dashboard analytics | `GET /manage/dashboard/components/analytics` | Month change |
| Platform analytics | `GET /administration/insights/components/analytics` | Month change |

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Real-time analytics? | answered | **No** — aggregated from `tb_views` and related tables |

#### Feature checklist

| ID | Criterion | Done |
|----|-----------|------|
| FC1 | Author dashboard with analytics | ☑ |
| FC2 | Platform insights for admin | ☑ |
| FC3 | Post view recording | ☑ |
| FCdev | Sample views in seed | ☑ |

**Review approval:** approved 2026-07-07 — production baseline
