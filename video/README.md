# MediTrack — Demonstration Film

Everything needed to produce the five-minute film about this project: the
shooting script, a self-contained animated 4K master, the real captured footage,
the narration and the subtitles.

**Want to just watch it?** Open
[`scenes/meditrack-demo.html`](scenes/meditrack-demo.html) in Chrome, press `H`
to hide the HUD, then `R` to play. It runs the full 5:00 silently, no build step,
no network.

---

## What's here

| Path | What it is |
|---|---|
| **[`SCRIPT.md`](SCRIPT.md)** | The shooting script. Twelve shots, timed to 5:00 — visuals, narration, audio direction, and the design bible (palette, type, motion, sound) |
| **[`scenes/meditrack-demo.html`](scenes/meditrack-demo.html)** | **The animated master.** A self-contained 3840×2160 timeline of all twelve scenes. Screen-record this and you have the visual track |
| **[`narration/voiceover.txt`](narration/voiceover.txt)** | Narration marked up with beats and stress, ≈520 words, plus TTS pronunciation notes |
| **[`narration/captions.srt`](narration/captions.srt)** | 42 timed subtitle cues |
| **[`capture/capture-all.sh`](capture/capture-all.sh)** | Regenerates every piece of terminal footage from the live application |
| **[`assets/`](assets/)** | The captured output — bill, triage, JVM boot, tests, case sheets, reports |
| **[`PRODUCTION.md`](PRODUCTION.md)** | How to record at 4K, mix the audio, and export |

---

## Status — read this first

**Done and in the repository:** the script, the animated master, all captured
footage, the narration script, the subtitles, and the production guide.

**Not done:** the exported `.mp4` with voiceover. Rendering video and
synthesising speech need `ffmpeg` and a TTS engine, and neither is installed on
the machine this was built on. [`PRODUCTION.md`](PRODUCTION.md) is the remaining
hour of work — install two free tools, record one take, mux.

Nothing here is a placeholder or a mock-up. The animated master really plays; the
captures are real program output.

---

## The one rule

**Every number in the film is real and reproducible.**

`capture-all.sh` ends by asserting that the two figures the film puts on screen —
the ₹1,593.00 bill total and the 325 passing assertions — are actually present in
the captured output. If those checks fail, the film has drifted from the code.

That check has already earned its keep: it caught the bill total being wrong in
the first draft, because expanding the demo dataset moved Ravi Kumar from a
₹1,400 doctor to a ₹1,500 one. The script and the animation were corrected to
match the program, never the other way round.

```bash
bash video/capture/capture-all.sh
```

---

## Structure of the film

| Act | Time | Beat |
|---|---|---|
| I — Hook | 0:00–0:30 | A cardiac pulse. The claim. Title |
| II — Substance | 0:30–0:56 | The numbers, then **six seconds of total silence** |
| III — **The reveal** | 0:56–1:40 | Lazy class loading, made visible on screen |
| IV — Architecture | 1:40–2:47 | The dependency graph builds; the one rule that holds it up |
| V — **The payoff** | 2:47–3:35 | One bill, three design patterns, the arithmetic |
| — | 3:35–4:05 | AI triage |
| VI — Proof | 4:05–5:00 | Thirteen bugs to zero. E to A. Then the close |

Three long shots carry it — the JVM reveal, the bill, and the E→A rating. Cut
anything and cut from the connective tissue, never from those.

---

## Why it's built this way

**The centrepiece is the class loader, not a feature tour.** Feature tours are
forgettable. The one genuinely arresting thing this codebase can show you is four
lines of startup output appearing in an order that proves the JVM loads classes
lazily — an invisible runtime behaviour, made visible. Everything before it is
setup; everything after is consequence.

**The six seconds of silence at 0:50 are deliberate.** The audience has just been
shown four statistics, and statistics do not survive. Cutting the sound entirely
resets their attention immediately before the one thing worth remembering.

**The coverage number is in the film.** 50.6%, stated plainly at 4:28. Admitting
the weakest number is what makes the other numbers believable.

---

*Project documentation: [../README.md](../README.md) ·
[../docs/USER_MANUAL.md](../docs/USER_MANUAL.md) ·
[../docs/DEMO_OUTPUT.md](../docs/DEMO_OUTPUT.md)*
