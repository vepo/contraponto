# Security policy

## Supported versions

Security fixes are applied on the default branch (`main`). Deploy from the latest commit or release tag you track in production.

## Reporting a vulnerability

**Please do not open a public GitHub issue for security vulnerabilities.**

Report privately by emailing the repository owner (see [GitHub profile](https://github.com/vepo)) or using [GitHub private vulnerability reporting](https://github.com/vepo/contraponto/security/advisories/new) if enabled.

Include:

- Description of the issue and impact
- Steps to reproduce
- Affected URLs or components (e.g. auth, upload, admin)
- Proof of concept if available

We will acknowledge receipt and work on a fix; coordinated disclosure is appreciated.

## Scope

In scope: this application (Quarkus server, Qute templates, auth, file upload, admin surfaces).

Out of scope: third-party dependencies unless exploitable through Contraponto’s default configuration; issues in forked themes or external Git remotes you configure yourself.
