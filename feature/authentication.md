# Authentication & account access

**Feature version:** 1  
**Status:** done  
**Production:** live

## Changelog

### Production baseline — 2026-07-07

**Version:** 1  
**Status:** done

**Production:** live — deployed capability

**Description:** Authentication, session, roles, and account flows.

**Domain model:** documented in domain-spec (pre-existing)

## Summary

**Guests** sign up, activate via email, and sign in with username/password. **Users** manage account security (email change with verification, password change), recover passwords, and sign out. **Administrators** create and edit users and roles. Sessions use the `__session` cookie and pluggable `SessionStore` ([ADR-0014](../docs/adr/0014-session-store.md)).

## Wireframe

| Field | Value |
|-------|-------|
| **Source** | feature-catalog § Authentication |
| **Last updated** | 2026-07-07 |



### Screen: Sign In / Sign Up modal (`GET /auth/modal`)

| Region | Elements | Notes |
|--------|----------|-------|
| Modal | Username, password; mode toggle | HTMX into `#modal-container` |
| Footer link | Forgot password? | → `/password-recovery` |
| Errors | `#authError` inline | i18n on login; signup messages partly English |

### Screen: Account hub — Security (`GET /account/security`)

| Region | Elements | Notes |
|--------|----------|-------|
| Fields | Email, pending email status, password change | `POST /forms/account/security` |
| Language | Locale flags | Also on header/footer |

### Screen: Password recovery

| Route | Elements |
|-------|----------|
| `GET /password-recovery` | Email request form |
| `GET /password-recovery/reset?token=` | New password form |

Email-only (out of catalog step count): `GET /account/activate?token=`, `GET /account/verify-email?token=`.

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `auth`, `user` |
| Packages | `auth.*`, `user.*`, `components.AuthModal`, `components.forms.*`, `shared.security.SessionStore` |
| Routes | `/auth/modal`, `/forms/auth/*`, `/account/*`, `/password-recovery`, `/users/*` (admin) |
| Schema | `tb_users`, `tb_user_roles`, `tb_user_account_tokens`, `tb_account_email_outbox` |
| Tests | `LoginTest`, `SignupTest`, `AccountActivationTest`, `PasswordRecovery*`, `EmailVerificationTest`, `UserManageTest` |
| Docs | domain-spec § Platform & people, feature-catalog § Authentication |

### Risks

| Risk | Mitigation |
|------|------------|
| Signup without session until activation | By design; reduces spam accounts |
| Admin email change bypasses verification | Documented gap (FQ3) |
| No OAuth/MFA | Out of scope MVP |

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Should signup auto-login before email activation? | answered | **No** — activation link required |
| FQ2 | Self-service email change requires verification token? | answered | **Yes** via `EmailVerificationService` |
| FQ3 | Should admin user save require email verification? | open | Currently direct update |
| FQ4 | Invalidate all sessions on admin password reset? | open | Recovery path does; admin `UserSaveEndpoint` may not |

## Architecture

### ADRs aplicáveis

| ADR | Relevância |
|-----|------------|
| [0002](../docs/adr/0002-backend-java-quarkus-jakarta-ee.md) | JAX-RS forms |
| [0003](../docs/adr/0003-frontend-qute-htmx.md) | Auth modal HTMX |
| [0014](../docs/adr/0014-session-store.md) | Session backing store |

### Design específico da feature

| Area | Design |
|------|--------|
| Bounded contexts | `auth`, `user` → `shared` |
| Layers | `*Endpoint` → `UserService`, `AccountActivationService`, `PasswordRecoveryService`, `LoggedUserProvider` |
| Session | `__session` cookie; `InMemorySessionStore` / `RedisSessionStore` |
| Roles | `USER`, `EDITOR`, `USER_ADMINISTRATOR`, `ADMIN` via `tb_user_roles` |
| Registration | `UserService.createUser` auto-creates **main blog** |
| CDI events | None — email payloads are not CDI events |

### HTMX component model

| Component id | Route | Activator | Target/swap | Events out | Events in | JS | Auth allowlist |
|--------------|-------|-----------|-------------|------------|-----------|-----|----------------|
| `#modal-container` | `GET /auth/modal` | Header Sign In/Up | innerHTML | — | — | modal close (legacy onclick) | No |
| `#authError` | — | Login/signup submit | innerHTML | — | — | none | No |
| `#menu-container` | OOB | Login/logout | OOB replace | — | — | `header.js` | No |
| `body` | — | Login | — | `loggedIn` | `{authRefreshTrigger}` subscribers | `main.js` | Yes |
| `body` | — | Logout | — | `loggedOut` | same | `main.js` | Yes |
| Protected `@Logged` | — | HTMX without session | — | `loginRequired` (401) | opens login modal | — | Yes |

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | Session store for multi-node? | answered | Redis when configured — ADR-0014 |
| AQ2 | bcrypt for passwords? | answered | `PasswordService` |

### Screen: Sign In / Sign Up modal (`GET /auth/modal`)

| Region | Elements | Notes |
|--------|----------|-------|
| Modal | Username, password; mode toggle | HTMX into `#modal-container` |
| Footer link | Forgot password? | → `/password-recovery` |
| Errors | `#authError` inline | i18n on login; signup messages partly English |

### Screen: Account hub — Security (`GET /account/security`)

| Region | Elements | Notes |
|--------|----------|-------|
| Fields | Email, pending email status, password change | `POST /forms/account/security` |
| Language | Locale flags | Also on header/footer |

### Screen: Password recovery

| Route | Elements |
|-------|----------|
| `GET /password-recovery` | Email request form |
| `GET /password-recovery/reset?token=` | New password form |

Email-only (out of catalog step count): `GET /account/activate?token=`, `GET /account/verify-email?token=`.

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `auth`, `user` |
| Packages | `auth.*`, `user.*`, `components.AuthModal`, `components.forms.*`, `shared.security.SessionStore` |
| Routes | `/auth/modal`, `/forms/auth/*`, `/account/*`, `/password-recovery`, `/users/*` (admin) |
| Schema | `tb_users`, `tb_user_roles`, `tb_user_account_tokens`, `tb_account_email_outbox` |
| Tests | `LoginTest`, `SignupTest`, `AccountActivationTest`, `PasswordRecovery*`, `EmailVerificationTest`, `UserManageTest` |
| Docs | domain-spec § Platform & people, feature-catalog § Authentication |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Sign up with email activation | feature-catalog | ☑ |
| FC2 | Sign in / sign out with menu refresh | feature-catalog | ☑ |
| FC3 | Password recovery flow | feature-catalog | ☑ |
| FC4 | Account security (email/password) | feature-catalog | ☑ |
| FC5 | Admin user CRUD + roles | feature-catalog | ☑ |
| FCdev | `admin`, `ghost` (inactive), dev users sign-in ready | dev-import | ☑ |

**Development approval:** approved — production baseline (shipped)

**Review approval:** approved 2026-07-07 — production baseline

**Implementation notes:** Signup does not fire `loggedIn` (htmx-events doc nuance). `ghost` user tests inactive login.
