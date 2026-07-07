# Author profile & appearance

**Feature version:** 1  
**Status:** done  
**Production:** live

## Changelog

### Production baseline — 2026-07-07

**Version:** 1  
**Status:** done

**Production:** live — deployed capability

## Summary

**Public author profile** at `/authors/{username}` (bio, social links, blogs, recent posts, **Message** button). **Author appearance** in Writing hub (`/writing/appearance`) edits profile picture, default banner, Markdown bio, social URLs, and **Fediverse** settings (see [activitypub-integration.md](activitypub-integration.md)).

## Wireframe

| Screen | Route | Notes |
|--------|-------|-------|
| Public profile | `GET /authors/{username}` | Top tags, blog cards |
| Appearance | `GET /writing/appearance` | Self-service edit |
| Message CTA | Profile → compose | `?to={username}` |

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `directory` (public), `user` (fields), `components.AuthorAppearanceEndpoint` |
| Packages | `AuthorProfileEndpoint`, `AuthorProfilePaths`, `AuthorSocialUrls` |
| Tests | `AuthorProfileEndpointTest`, `AuthorProfileMessageWebTest`, `ProfileTest` |



### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Profile bio vs blog description? | answered | User `profileDescription` distinct from `Blog.description` |

## Architecture

### HTMX component model

| Component | Route | Notes |
|-----------|-------|-------|
| Appearance save | `POST /forms/writing/appearance` | Hub panel refresh |
| ActivityPub toggle | `POST /forms/writing/activitypub` | See activitypub feature |

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Profile bio vs blog description? | answered | User `profileDescription` distinct from `Blog.description` |

#### Feature checklist

| ID | Criterion | Done |
|----|-----------|------|
| FC1 | Public author profile | ☑ |
| FC2 | Appearance hub section | ☑ |
| FC3 | Message button on profile | ☑ |
| FCdev | Personas with bios/social links | ☑ |

**Review approval:** approved 2026-07-07 — production baseline
