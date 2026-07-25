# Anushtha Sharma — Task Sheet

**Role:** Experience & Interface Design
**SDLC stages:** Integration (Phase 13) · Documentation (Phase 16)

Everything a user actually touches: the menu flow, the wording of every prompt
and error, and the documentation that teaches the application.

---

## Scope

| Area | Files |
|---|---|
| Console interface | `Main.java` — menu tree, prompts, error presentation |
| User documentation | [USER_MANUAL.md](../USER_MANUAL.md) |
| Demonstration | [DEMO_OUTPUT.md](../DEMO_OUTPUT.md) |
| Phase records | [PHASE_13_CONSOLE_UI.md](../phases/PHASE_13_CONSOLE_UI.md) · [PHASE_16_DOCUMENTATION.md](../phases/PHASE_16_DOCUMENTATION.md) |

---

## Tasks

### Phase 13 — Console UI

- [x] Nine-section menu tree, each option one keystroke from the main menu
- [x] `0` reserved as back/exit everywhere — it never destroys anything
- [x] Every action returns to the main menu; no nested loops to get lost in
- [x] CLI flags: `--seedDemo`, `--loadData`, `--runTests`, `--help`
- [x] Every checked exception surfaced as a sentence, never a stack trace
- [x] EOF returns `"0"` so piped input terminates cleanly
- [x] Contact numbers masked at the point of display
- [x] Menu 9 — OOP demonstrations, making language behaviour observable
- [x] UTF-8 currency rendering verified on Windows and Alpine

### Phase 16 — Documentation

- [x] [User Manual](../USER_MANUAL.md) — every menu option documented
- [x] [Demonstration & Output](../DEMO_OUTPUT.md) — guided tour, real captured output
- [x] Input rules table — the validation contract, stated for users
- [x] Troubleshooting — the five failures people actually hit
- [ ] Screenshot capture for `Setup_Instructions.md` *(needs a human at a screen)*

---

## Design decisions

**The UI stays thin — deliberately.** `Main` reads input, calls a service, prints
the result. It performs no validation, holds no booking rules and computes no
prices. Without that discipline console applications reliably become god objects,
and every rule ends up implemented twice.

**Errors are sentences, not stack traces.** Because the domain exceptions are
*checked*, the compiler will not let a handler be forgotten — which is what makes
"no input can crash the app" a structural guarantee rather than a hope.

```
  Invalid value for 'status' [Pending] — only a COMPLETED appointment can be billed
```

That message names the field, shows the offending value, and states the rule.

**Sub-menus do not loop.** Returning to the main menu after each action costs one
extra keystroke on repeat operations, and buys never being lost three levels
deep with no idea which `0` goes where.

**Menu 9 has no business purpose.** It exists so that dynamic dispatch, deep vs
shallow copy, immutability and singleton identity can be *watched* during a
walkthrough instead of merely described. Keeping the deliberately-wrong shallow
copy is the point — watching it change alongside the original is what makes the
distinction land.

**EOF returns `"0"`.** A small decision with outsized value: it is what makes
`printf '9\n2\n0\n' | java -cp out ...Main --seedDemo` work, which is what makes
every capture in [DEMO_OUTPUT.md](../DEMO_OUTPUT.md) reproducible.

---

## Interfaces with other members

| From | Contract |
|---|---|
| **Zubair** — `service/` | Services return values and throw checked exceptions; the UI decides how to phrase them |
| **Varun** — `entity/` | Entities own their own formatting; the UI prints, it does not compose |
| **Sunil** — `util/` | `Validator` owns every input rule. The UI re-implements none of them |

**To Sunil — testing:** every menu path is a test path. The UI walkthrough
defined what Phase 14 had to cover, and the `--runTests` flag exists because the
UI provides it.

---

## Known gaps

- `Setup_Instructions.md` still has screenshot placeholders
- `Main` is at 0.6% test coverage — an interactive `Scanner` loop is not drivable
  from a headless runner. Documented honestly in
  [CODE_QUALITY.md](../CODE_QUALITY.md#coverage--the-honest-picture) rather than
  papered over
- No input history or tab completion — out of scope for a console demonstrator

---

*[Phase index](../phases/README.md) · [Sunil](./SUNIL.md) · [Varun](./VARUN.md) · [Zubair](./ZUBAIR.md)*
