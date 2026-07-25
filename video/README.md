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
| **[`render/`](render/)** | **The build pipeline.** Renders the film end to end — frames, voice, score, mux |
| **[`PRODUCTION.md`](PRODUCTION.md)** | Manual route: recording at 4K by hand, mixing, export targets |

---

## Building the film

```bash
bash video/render/build.sh          # everything  (~45 min)
bash video/render/build.sh audio    # voice + score only  (~2 min)
bash video/render/build.sh mux      # re-mux existing parts  (seconds)
```

Output lands in `video/out/` (gitignored — see [below](#why-the-mp4-is-not-in-the-repository)).

### The finished film

Built and verified on 26 Jul 2026. These are measured figures, not targets:

| Artefact | Size | Resolution | Runtime | Audio |
|---|---|---|---|---|
| `meditrack-demo-4k.mp4` | 595 MB | 3840×2160 @ 30fps, CRF 16 | 300.00s | AAC 320k, 48kHz stereo |
| `meditrack-demo-1080p.mp4` | 24 MB | 1920×1080, CRF 21 | 300.01s | AAC 192k, 48kHz stereo |

Verified after the mux, not merely intended:

- **Runtime 5:00.00** — exactly on the script, no drift across 9,000 frames
- **−15.0 LUFS integrated**, LRA 5.5 — within 1 LU of the −16 target, well placed
  for YouTube's −14 normalisation
- **0:50–0:56 measures −91.0 dB** in narration, score and final mix alike. The
  silence survived the loudnorm pass, which is the thing most likely to destroy it
- **₹1,593.00 and 325 assertions** are legible on screen and match `assets/`
- HUD hidden; ₹ renders as ₹ throughout, no `?` boxes

| Stage | Tool | What it does |
|---|---|---|
| **Frames** | [`render-frames.mjs`](render/render-frames.mjs) | Drives the animated master with Puppeteer, stepping `window.seekFrame(t)` one frame at a time, piping each screenshot straight into ffmpeg |
| **Voice** | [`make-narration.py`](render/make-narration.py) | Parses the timestamped blocks out of `voiceover.txt`, synthesises each with `edge-tts`, lays them on a 300s bed at their scripted offsets |
| **Score** | [`make-music.py`](render/make-music.py) | Synthesises the pad, the cardiac pulse and the two stingers from scratch — no samples, nothing licensed |
| **Mux** | [`build.sh`](render/build.sh) | Mixes voice over score, muxes to 4K, then renders a 1080p web cut |

### Why frame-stepping instead of screen recording

A screen recorder captures wall-clock time, so a busy machine silently drops
frames. The renderer instead asks the page for each frame explicitly and waits —
slower than real time, but never lossy, and identical on any machine.

That requires the animation to be seekable, which is what the render API at the
bottom of [`scenes/meditrack-demo.html`](scenes/meditrack-demo.html) provides:
every CSS animation is paused and its `currentTime` set explicitly, and anything
that was `setTimeout`-driven during playback is recomputed as a pure function of
scene-local time. Frame *N* is the same pixels no matter how long the machine
took to draw it.

Frames are piped to ffmpeg rather than written out — 9,000 uncompressed 4K
frames would be about 90 GB.

### Requirements

Node, Python and Chrome are the only hard ones, plus **ffmpeg**:

```powershell
winget install Gyan.FFmpeg
```

```bash
cd video/render && npm install     # puppeteer-core, drives your installed Chrome
python -m pip install edge-tts     # free Microsoft neural voices, no API key
```

### Swapping the voice or the music

Both are one re-mux, not a re-render:

- **Own voice:** record over [`narration/voiceover.txt`](narration/voiceover.txt),
  save as `video/out/narration.wav`, then `build.sh mux`
- **Own music:** drop a track at `video/out/music.wav`, then `build.sh mux`
- **Different TTS voice:** change `VOICE` in `make-narration.py` —
  `python -m edge_tts --list-voices` lists them all

### Why the .mp4 is not in the repository

The 4K master runs to several hundred megabytes, well past GitHub's 100 MB file
limit, so `video/out/` is gitignored. Everything needed to *regenerate* it is
committed. Distribute the artefact through GitHub Releases or YouTube.

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
