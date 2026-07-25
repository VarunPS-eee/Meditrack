# Security Policy

## Supported versions

| Version | Supported |
|---|---|
| 1.0.x | ✅ Actively maintained |
| < 1.0 | ❌ Pre-release, not supported |

---

## Reporting a vulnerability

**Please do not open a public issue for a security problem.**

Report privately through either channel:

1. **GitHub Security Advisories** — [Report a vulnerability](../../security/advisories/new)
   (preferred; keeps the report private until a fix ships)
2. **Email** — <contact.me@sunilkumarba.com> with the subject `MediTrack security`

Please include:

- What the issue is and which class or file it lives in
- Steps to reproduce, ideally with the exact menu path or CLI flags
- What an attacker gains
- The version or commit SHA you tested

### What to expect

| Stage | Target |
|---|---|
| Acknowledgement | within 3 days |
| Initial assessment | within 7 days |
| Fix or documented mitigation | within 30 days for confirmed issues |

We will credit you in the release notes unless you ask us not to.

---

## Scope — what counts

MediTrack is a **single-user, offline console application** that stores data in
local CSV files. It has no network listener, no authentication layer and no
multi-tenant boundary. That shapes what is and is not a vulnerability here.

### In scope

- **Path traversal** via `MEDITRACK_DATA_DIR` or any file path reaching
  `CSVUtil` — a crafted value escaping the intended data directory
- **CSV injection** — a field such as a patient name that survives
  `CSVUtil` escaping and executes when the export is opened in a spreadsheet
- **Deserialisation or parser flaws** in the CSV read path that allow crashes
  or corruption from a malformed `data/*.csv`
- **Unsafe file permissions** on files created under `data/`
- **Denial of service** through unbounded input — for example a value that
  drives `DataStore` or `IdGenerator` into exhausting memory
- **Concurrency defects with a security consequence** in `IdGenerator`,
  `DataStore` or `NotificationService` — for instance duplicate IDs causing
  one patient's record to overwrite another's

### Out of scope

- Anything requiring an attacker to already have write access to `data/` or to
  the machine — that is the trust boundary, not a bypass of it
- The absence of authentication, authorisation, encryption at rest or audit
  integrity. MediTrack is a coursework demonstrator; these are **documented
  non-goals**, listed in [docs/Design_Decisions.md](docs/Design_Decisions.md)
- Using `double` for currency. A known, deliberate limitation, already recorded
  in the design docs. Report it as a bug if you like, but not as a vulnerability
- The keyword-matching triage in `AIHelper` giving clinically poor suggestions.
  It is a rule-based demo, **not a medical device**, and must never be used for
  real clinical decisions

---

## Security posture

Deliberate choices that reduce the attack surface:

- **Zero third-party runtime dependencies.** Nothing to inherit a CVE from,
  and no transitive supply chain
- **Checked exceptions** on every failure path, so no input can crash the app
  into an undefined state
- **`try-with-resources`** on every stream in `CSVUtil`, so no descriptor leaks
- **Non-root container user** in the Dockerfile
- **Tests gate the build** — `TestRunner` exits non-zero, and the Docker build
  runs it, so an image cannot be produced from failing code
