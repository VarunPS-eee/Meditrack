# MediTrack — The Heist

The full demonstration film for this project.

**▶ [`meditrack-the-heist.mp4`](meditrack-the-heist.mp4)**
1920×1080 · **32:12** · 66 MB · H.264 + AAC stereo

Download it and play it in anything. GitHub will not stream a file this size in
the browser, so use the download button rather than clicking through.

---

## What it covers

It is a complete pass over the project, not a highlight reel — told as a heist,
because that is genuinely the shape the work had: a brief, a set of constraints,
a crew with different specialities, a plan argued out on a board, and then the
execution.

| Act | Runtime | What happens |
|---|---|---|
| **0 · The problem** | 0:00 | What a clinic actually has to keep track of, and why losing the thread between any two of those things hurts people |
| **I · The brief** | 1:02 | What MediTrack is. The four constraints — no framework, no database, no build tool, no library — and why they are the point rather than a handicap. The crew, and what each person owns |
| **II · The plan** | 3:50 | The ten packages and how they layer. The three architectural decisions, each against the alternative it beat. The dependency graph. How the work split, and why the order was a topological sort rather than a preference |
| **III · The vaults** | 8:00 | Six things the JVM is doing that you normally only read about — lazy class loading caught on screen, stack versus heap, late binding, the five design patterns, shallow versus deep copy, and the collections and streams underneath |
| **IV · Running it** | 15:10 | The real application, driven through all nine menus and roughly fifty operations. Case sheets, practice sheets, the appointment state machine, billing, search, triage, analytics, persistence, and what happens when you type something wrong |
| **V · The proof** | 25:08 | 325 assertions passing, thirteen SonarQube bugs fixed from rating E to A — and the coverage number we are not proud of, stated plainly |
| **VI · After the job** | 28:10 | Who did what, how to contribute, and how to reach us |

---

## Every number in it is real

Nothing on screen was typed into a slide. The terminal output was captured by
running the program, and the figures are asserted against those captures before
the film is built — the bill total, the assertion count, the class-loading order.

If the code changes so that a number moves, the check fails and the film gets
corrected. It has already caught one drift: expanding the demo dataset moved a
patient to a more expensive doctor, and the bill total in the script was wrong
until the assertion said so.

Two things in the captures legitimately vary and are therefore never shown as
headline figures: timestamps, and emergency-bill totals — which carry a doubled
surcharge outside clinic hours, so they differ between a capture at midnight and
one at midday.

---

## Watching it

There is no soundtrack requirement — the narration carries the information and
the score sits underneath it. But it is mixed properly (−16 LUFS, stereo), so
headphones are better than laptop speakers.

The film assumes no prior knowledge of the codebase. If you want to follow along
in the application while you watch, the walkthrough act maps one-to-one onto
[`docs/USER_MANUAL.md`](../docs/USER_MANUAL.md).

---

## Why the production files are not here

The screenplay, the animated scene sources, the capture harness and the render
pipeline are development tooling, not part of MediTrack. A repository that exists
to be read as a Java project should not make you scroll past a video build system
to find the Java.

They are kept outside the repository. What ships here is the film.

---

*Project home: [../README.md](../README.md) ·
Manual: [../docs/USER_MANUAL.md](../docs/USER_MANUAL.md) ·
Captured output: [../docs/DEMO_OUTPUT.md](../docs/DEMO_OUTPUT.md)*
