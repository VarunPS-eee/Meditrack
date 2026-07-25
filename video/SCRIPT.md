# MediTrack — Demonstration Film
## Shooting Script · 5:00 · 3840×2160 @ 60fps

> **Title:** *MediTrack — 9,742 Lines, Zero Dependencies*
> **Runtime:** 5:00 · **Aspect:** 16:9 · **Master:** 3840×2160 (4K UHD), 60fps
> **Tone:** Technical thriller. Restrained, confident, precise. The tension comes
> from *revealing how something works*, never from hype.

---

## Design bible

Everything in the film obeys these. Consistency is what makes it read as one
piece rather than a slideshow.

### Palette — "Clinical Night"

| Token | Hex | Use |
|---|---|---|
| `--void` | `#05070A` | Backgrounds. Near-black, faintly blue — never pure `#000` |
| `--panel` | `#0D1219` | Cards, terminal chrome |
| `--line` | `#1B2530` | Hairlines, grid |
| `--ink` | `#E6EDF3` | Primary text |
| `--muted` | `#7D8C9E` | Secondary text, labels |
| `--vital` | `#22D3AA` | **Signature accent.** Success, the pulse, key reveals |
| `--pulse` | `#38BDF8` | Secondary accent — data, links, cyan glow |
| `--warn` | `#FBBF24` | Caution, the "before" state |
| `--critical` | `#F43F5E` | Urgency, bugs, the `E` rating |
| `--gold` | `#E9C46A` | Currency, the bill |

**Rule:** never more than two accents on screen at once. The eye must always
know where to look.

### Type

- **Display / titles:** Inter Tight or Söhne — 700/800 weight, tight tracking (`-0.03em`)
- **Body / narration cards:** Inter — 400/500
- **Code / terminal:** JetBrains Mono — 400/700, `ligatures: off`
- **Numbers that count up:** tabular figures (`font-variant-numeric: tabular-nums`) — otherwise digits jitter as they change

### Motion

| Rule | Value |
|---|---|
| Primary easing | `cubic-bezier(0.16, 1, 0.3, 1)` — fast out, long settle |
| Scene transition | 600ms crossfade + 2% scale drift |
| Text reveal | 40ms stagger per line, 12px rise |
| Terminal typing | 18–24ms per character, ±15% jitter so it feels human |
| Hold after reveal | **minimum 800ms.** The commonest mistake is cutting before the viewer has read it |

**Camera:** a permanent, almost imperceptible drift — 1.5% scale over 20s. The
frame is never mechanically still. It reads as "alive" without being noticed.

### Audio bed

| Layer | Direction |
|---|---|
| Music | Single evolving synth pad, 70–75 BPM, minor key. Sub-bass swell into each act. **Duck to −18dB under narration** |
| Pulse | A soft cardiac *thump* on the two-beat, 60ms low-pass. It is the film's spine — it starts in Act I and never fully stops |
| UI | Terminal keystrokes: dry, quiet, 4kHz click. No mechanical-keyboard cliché |
| Stingers | One sub-drop at 0:56 (the JVM reveal). One reversed cymbal into 2:47 (the bill). **Only two.** Stingers stop working if you use five |
| Silence | **0:52–0:56 is fully silent.** The single most effective four seconds in the film |

### Voice direction

Measured, low-register, unhurried — closer to a documentary narrator than a
product ad. Never exclamatory. The material is impressive on its own; selling it
makes it smaller. Leave a beat of air before every technical noun.

---

## ACT I — THE HOOK
### Shot 1 · 0:00–0:08 · Cold open

| | |
|---|---|
| **Visual** | Pure `--void`. A single horizontal hairline in `--vital` crosses screen centre, flat. At 0:03 it spikes once — a cardiac QRS complex — and settles. The spike leaves a 400ms phosphor trail |
| **Audio** | Room tone only. The pulse *thump* lands exactly on the spike |
| **Text** | — |
| **Narration** | *(silence)* |

> **Why:** open on a single moving object against black. No logo, no title. The
> spike is a promise: this is about something alive.

### Shot 2 · 0:08–0:20 · The claim

| | |
|---|---|
| **Visual** | The hairline becomes a terminal caret. Text types on, monospace, `--ink`, centred |
| **Text** | `A working clinic.` *(beat)* `72 patients. 20 doctors.` *(beat)* `Zero dependencies.` |
| **Audio** | Keystrokes. Pad fades in under |
| **Narration** | "This is a hospital management system written in nothing but Java." |

> Each line types, holds 700ms, then the next appears below. `Zero dependencies.`
> lands in `--vital` and holds for a full second.

### Shot 3 · 0:20–0:30 · Title

| | |
|---|---|
| **Visual** | Everything collapses to the centre line. Title resolves from 8px blur, letter-spacing tightening `0.4em → -0.02em` |
| **Text** | **MediTrack** · sub: `Clinic & Appointment Management · Core Java 21` |
| **Audio** | Low sub swell, then the pad opens up |
| **Narration** | *(silence — let the title breathe)* |

---

## ACT II — THE SUBSTANCE
### Shot 4 · 0:30–0:50 · The numbers

| | |
|---|---|
| **Visual** | Four stat tiles fade up in sequence, 120ms apart. Each number **counts up** from 0 over 900ms with tabular figures |
| **Text** | `45` Java files · `9,742` lines · `0` dependencies · `325` tests passing |
| **Audio** | A soft tick per tile landing |
| **Narration** | "Forty-five source files. Nine and a half thousand lines. No Spring. No Maven. No Gradle. Not one third-party library — and three hundred and twenty-five tests that all pass." |

> `0` and `325` land in `--vital`. The other two stay `--ink`. Two accents, no more.

### Shot 5 · 0:50–0:56 · **THE SILENCE**

| | |
|---|---|
| **Visual** | Hard cut to black. Nothing. Then, at 0:54, one line of grey monospace, small, low in frame |
| **Text** | `[Constants] Static block executed` |
| **Audio** | **TOTAL SILENCE.** Even the pulse stops |
| **Narration** | *(none)* |

> **Why:** the audience has just been given numbers. Numbers are forgettable. The
> silence resets their attention completely before the one thing they should
> actually remember.

---

## ACT III — THE REVEAL *(the centrepiece)*
### Shot 6 · 0:56–1:40 · Lazy class loading, made visible

| | |
|---|---|
| **Visual** | The four startup lines appear **one at a time**, each held 900ms. As each lands, a node lights up in a small class-loader diagram at frame right |
| **Audio** | **Sub-drop on the first line.** Pulse returns, slower, heavier |

**On-screen, in order:**

```
[Constants]     Static block executed — data directory resolved to: data
[AppConfig]     Eager singleton constructed at 2026-07-25 22:13
[IdGenerator]   Lazy singleton constructed on first use.
[MedicalEntity] Class loaded and initialised at 2026-07-25T22:13:45.951
```

**Narration:**
> "Watch the order these four lines appear in.
> *(beat)*
> `Constants` initialises first — a static block, running before a single object
> exists.
> `AppConfig` is an eager singleton: built during class initialisation, thread-safe
> for free, because the JVM runs that exactly once under its own lock.
> `IdGenerator` is lazy — it waits until something actually asks for it.
> *(long beat)*
> And `MedicalEntity` — the root of the entire class hierarchy — loads **last.**"

| | |
|---|---|
| **Visual (1:30)** | The `MedicalEntity` line pulses `--vital`. An arrow whips from it back up to the top of the hierarchy diagram |
| **Narration** | "Not because it matters least. Because the JVM does not load a class until the moment it is first used. That ordering isn't documentation. It's the class loader, caught in the act." |

> **Why this is the centrepiece:** it is the one moment where an invisible runtime
> behaviour becomes visible on screen. Everything before it is setup; everything
> after is consequence.

---

## ACT IV — THE ARCHITECTURE
### Shot 7 · 1:40–2:10 · The graph builds

| | |
|---|---|
| **Visual** | Dependency graph assembles bottom-up. `interfaces/` + `exception/` first, then `constants/`+`util/`, then `entity/`, then `service/`, then `Main` on top. Each layer springs in with a 300ms overshoot; edges draw as animated strokes |
| **Audio** | A soft pluck per layer, rising in pitch |
| **Narration** | "The architecture is a directed acyclic graph, and it is enforced by the compiler. Contracts at the bottom, depending on nothing. The domain above them. Services orchestrating the domain. And the console — deliberately thin — on top." |

### Shot 8 · 2:10–2:47 · Ownership + the rule

| | |
|---|---|
| **Visual** | The same graph re-colours by owner — four colours washing across it. Then all but one edge dims, leaving `Main → service` glowing |
| **Narration** | "Four people built this, and ownership runs by package, not by file. Two engineers editing different methods in one file still collide. Two engineers editing different packages essentially never do. *(beat)* And one rule holds the whole thing up: `Main` reads input, calls a service, prints the result. It validates nothing. It stores nothing. It prices nothing. Without that discipline, console applications reliably become god objects." |

---

## ACT V — THE PAYOFF
### Shot 9 · 2:47–3:35 · One bill, three patterns

| | |
|---|---|
| **Visual** | Terminal centre-frame. A bill assembles **line by line**. As each line lands, a label flies in from the right naming the pattern responsible, then fades |
| **Audio** | Reversed cymbal into the shot. Then near-silence — just the pulse and typing |

```
              MediTrack — CONSULTATION BILL          ← FACTORY
  Patient    : Ravi Kumar (PAT-0005)
  Policy     : Senior Citizen                        ← STRATEGY
--------------------------------------------------------------
  Base amount                            ₹1,500.00
  Policy adjustment (Senior Citizen)      ₹-150.00
  GST @ 18%                                ₹243.00
--------------------------------------------------------------
  TOTAL                                  ₹1,593.00   ← TEMPLATE METHOD
```

**Narration:**
> "One bill. Three design patterns, each doing real work.
> **Factory** chose the subclass — and its switch is exhaustive, so adding a bill
> type without handling it is a compile error, not a silently unpriced bill.
> **Strategy** chose the pricing policy. Nobody selected 'Senior Citizen' — Ravi
> is sixty-seven, and the policy was derived from his own record.
> **Template Method** fixed the order. `generateBill` is `final`. The sequence is a
> business invariant, so no subclass can discount *after* tax."

| | |
|---|---|
| **Visual (3:25)** | The three figures isolate and animate: `1500 → −150 → ×1.18 = 1593.00` |
| **Narration** | "Fifteen hundred. Less ten percent. Then eighteen percent tax on what remains. The discount lands before the tax — because the template will not permit anything else." |

### Shot 10 · 3:35–4:05 · AI triage

| | |
|---|---|
| **Visual** | Symptoms type into a prompt. Output resolves. The `** URGENT **` banner snaps in `--critical` with a 2px shake |
| **Text** | `chest pain and breathlessness` → Cardiology, 100% · 3 ranked doctors |
| **Narration** | "Type symptoms in plain English and it routes you. No machine learning, no network call — keyword rules declared on the enum itself, so the mapping lives next to the speciality it describes. It shows its work: which keywords matched, the confidence, and why each doctor ranked where they did. Experience is capped at twenty years, so a long-serving generalist can never outrank a well-matched specialist on longevity alone." |

---

## ACT VI — THE PROOF
### Shot 11 · 4:05–4:35 · Quality, honestly

| | |
|---|---|
| **Visual** | A large `E` in `--critical` centre-frame. It **cracks and shatters**, reassembling as `A` in `--vital`. Behind it, `13 → 0` counts down |
| **Audio** | Impact on the shatter. Pad resolves to major |
| **Narration** | "SonarQube's first pass came back with thirteen bugs and a reliability rating of E — the worst there is. A leaked thread pool. Uptime measured off a wall clock that could run backwards. Duration maths that would put reminders an hour out, twice a year. *(beat)* All thirteen are fixed. Zero bugs. Zero vulnerabilities. Rating A." |

| | |
|---|---|
| **Visual (4:28)** | Small, honest line fades up beneath: `Coverage 50.6% — and the docs say why` |
| **Narration** | "And where it isn't perfect, the documentation says so." |

> **Why include the flaw:** admitting the coverage number is what makes every
> other number in the film believable.

### Shot 12 · 4:35–5:00 · Close

| | |
|---|---|
| **Visual** | Graph, bill, triage and test results tile into a 2×2 grid, then pull back into the pulse line from Shot 1 — now beating steadily. Title returns. Four contributor names fade up |
| **Audio** | Full pad. Final pulse thump on the last frame, then silence |
| **Narration** | "MediTrack. Forty-five files, zero dependencies, three hundred and twenty-five tests. Open source under MIT. *(beat)* Read it, run it, or contribute to it." |
| **End card** | `github.com/VarunPS-eee/Meditrack` · MIT · Sunil Kumar B A · Varun P S · Zubair · Anushtha Sharma |

---

## Timing sheet

| # | In | Out | Dur | Beat |
|---:|---|---|---:|---|
| 1 | 0:00 | 0:08 | 0:08 | Cold open — the pulse |
| 2 | 0:08 | 0:20 | 0:12 | The claim |
| 3 | 0:20 | 0:30 | 0:10 | Title |
| 4 | 0:30 | 0:50 | 0:20 | The numbers |
| 5 | 0:50 | 0:56 | 0:06 | **Silence** |
| 6 | 0:56 | 1:40 | 0:44 | **JVM reveal** |
| 7 | 1:40 | 2:10 | 0:30 | Graph builds |
| 8 | 2:10 | 2:47 | 0:37 | Ownership + the rule |
| 9 | 2:47 | 3:35 | 0:48 | **The bill** |
| 10 | 3:35 | 4:05 | 0:30 | AI triage |
| 11 | 4:05 | 4:35 | 0:30 | E → A |
| 12 | 4:35 | 5:00 | 0:25 | Close |
| | | | **5:00** | |

**Pacing check:** three long shots (6, 9, 11) carry the film; the rest are ≤30s
connective tissue. Cut anything and cut from 7, 8 or 12 — never from 5 or 6.

---

## Production notes

- **Never fake a number.** Every figure on screen is reproducible from
  [`capture/shots.md`](capture/shots.md). If a value changes, re-capture — do not
  retouch the frame.
- **Terminal footage is real**, produced by [`capture/capture-all.sh`](capture/capture-all.sh).
  The demo seeder is deterministic, so the same run always yields the same data.
- **Hold longer than feels right.** In review, the note is always "too fast."
- Render the animated scenes from [`scenes/meditrack-demo.html`](scenes/meditrack-demo.html);
  see [`PRODUCTION.md`](PRODUCTION.md) for the 4K capture and mux workflow.
