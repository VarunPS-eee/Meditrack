# Pull Request Log

Every change to `main` lands through a PR. This directory records what each one contained, why,
and who reviewed it.

`main` is protected — no direct pushes.

---

## Index

| # | Branch | Author | Scope | Status | Record |
|---|---|---|---|---|---|
| 1 | `setup/initial-skeleton` | Varun | Project skeleton, packages, doc stubs | ✅ Merged | [PR-001](./PR-001-initial-skeleton.md) |
| 2 | *(direct)* | Varun | Core entities, `Specialization`, relocate `Main` | ✅ Merged | [PR-002](./PR-002-core-entities.md) |
| 3 | *(direct)* | Varun | `Bill`, immutable `BillSummary` | ✅ Merged | [PR-003](./PR-003-bill-classes.md) |
| 4 | *(direct)* | Varun | `Searchable`, `Payable`; reserved-keyword fix | ✅ Merged | [PR-004](./PR-004-interfaces.md) |
| 5 | `feature/complete-meditrack` | Team | Full build-out to submission-ready | 🟡 **Open** | [PR-005](./PR-005-complete-meditrack.md) |

---

## Commit history

```
71d61f0  Varun  2026-07-24  Add Searchable and Payable interfaces, fix reserved keyword package name
159e5bb  Varun  2026-07-24  Add Bill and immutable BillSummary classes
3ec86d0  Varun  2026-07-24  Add core entities, Specialization enum, and relocate Main.java
aa198c7  Varun  2026-07-23  Resolve merge conflicts by keeping skeleton files
edce362  Varun  2026-07-21  Initial commit: Set up package structure and documentation files
82c5179  Varun  2026-07-21  Initial commit
```

---

## Workflow

```bash
git checkout main && git pull origin main
git checkout -b feature/<name>
# ... work ...
git add -A && git commit -m "<what and why>"
git push -u origin feature/<name>
# Open a PR, get one approval, merge
```

### Branch naming

| Prefix | For |
|---|---|
| `feature/` | New functionality |
| `docs/` | Documentation only |
| `fix/` | Bug fixes |
| `setup/` | Project configuration |

### Review checklist

- [ ] Compiles clean — `javac -d out $(find src/main/java -name '*.java')`
- [ ] `TestRunner` passes 100%
- [ ] Public types and methods have JavaDoc
- [ ] No `System.out` debugging left behind
- [ ] Package structure matches `com.airtribe.meditrack.*`
- [ ] No unrelated files touched
- [ ] Design decisions documented if non-obvious

---

## PR description template

```markdown
## What
One or two sentences on what this changes.

## Why
The problem being solved, or the rubric item being met.

## How
Key implementation decisions, and alternatives rejected.

## Rubric impact
| Item | Points | Status |
|---|---|---|

## Testing
How this was verified. Test counts if applicable.

## Review notes
Anything the reviewer should look at closely.
```

---

*[Documentation index](../) · [Phase plan](../phases/README.md)*
