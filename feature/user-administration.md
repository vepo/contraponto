# User administration

**Feature version:** 1  
**Status:** done  
**Production:** live

## Changelog

### Production baseline — 2026-07-07

**Version:** 1  
**Status:** done

**Production:** live — deployed capability

## Summary

**User administrators** and **administrators** manage platform **users** and **roles** from the **Administration hub** (`/administration/users`). Includes create user, edit roles, activation email on create. Related admin sections: **message reports** ([user-messaging.md](user-messaging.md)), **ActivityPub kill-switch** ([activitypub-integration.md](activitypub-integration.md)), **platform insights** ([dashboard-analytics.md](dashboard-analytics.md)).

## Wireframe

| Screen | Route | Notes |
|--------|-------|-------|
| Administration hub | `GET /administration` | Users panel default |
| User list | `GET /administration/users` | Paginated |
| New user | `GET /users/new` | Roles checkboxes |
| Edit user | `GET /users/{id}/edit` | Cannot assign ADMIN without ADMIN |

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `user`, `admin` (hub) |
| Auth | `UserAccess.canManageUsers`, `canAssignRole` |
| Tests | `UserManageTest`, `UserServiceTest` |



### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Paths under `/administration` not `/admin`? | open | Intentional — document vs rename (conventions checklist) |
| FQ2 | USER_ADMINISTRATOR vs ADMIN for message reports? | answered | Reports **ADMIN** only |

## Architecture

### Design específico

| Area | Design |
|------|--------|
| Save | `UserSaveEndpoint` — create sends activation email |
| Roles | `Role` enum; `USER_ADMINISTRATOR` cannot assign `ADMIN` |

### HTMX component model

| Component | Pattern |
|-----------|---------|
| Hub panels | `NavigationHubPanelService` |
| User save | `POST /forms/users` → Toast + hub shell |

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Paths under `/administration` not `/admin`? | open | Intentional — document vs rename (conventions checklist) |
| FQ2 | USER_ADMINISTRATOR vs ADMIN for message reports? | answered | Reports **ADMIN** only |

#### Feature checklist

| ID | Criterion | Done |
|----|-----------|------|
| FC1 | User list in Administration hub | ☑ |
| FC2 | Create / edit users + roles | ☑ |
| FCdev | `admin` with ADMIN + USER_ADMINISTRATOR | ☑ |

**Review approval:** approved 2026-07-07 — production baseline
