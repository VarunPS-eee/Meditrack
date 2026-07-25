# Production Guide

How to turn the assets in this folder into the finished 4K film.

> ## There is an automated path — use it first
>
> ```bash
> winget install Gyan.FFmpeg          # once
> cd video/render && npm install       # once
> python -m pip install edge-tts       # once
>
> bash video/render/build.sh           # everything, ~45 min, unattended
> ```
>
> That renders the frames, synthesises the voice and score, mixes, and exports
> both a 4K master and a 1080p web cut. **No screen recorder and no microphone.**
> See [`README.md`](README.md#building-the-film) for the stage breakdown.
>
> The rest of this document is the **manual route** — worth reading if you want
> to shoot it by hand, record your own narration, or understand the choices the
> automated pipeline is making on your behalf.

---

## Why frame-stepping beats screen recording

The automated path does not record the screen. It asks the page to render one
exact frame at a time and pipes each into ffmpeg.

That matters because a screen recorder samples wall-clock time: if the machine
stalls for 40ms, that frame is simply gone, and no amount of re-encoding brings
it back. Stepping the timeline explicitly is slower than real time but cannot
drop anything, and produces identical output on any machine.

It works because [`scenes/meditrack-demo.html`](scenes/meditrack-demo.html)
exposes `window.seekFrame(t)`, which pauses every CSS animation and sets its
`currentTime` directly, and recomputes anything that was `setTimeout`-driven as
a pure function of scene-local time.

---

## What you need

| Tool | Why | Get it |
|---|---|---|
| **Chrome or Edge** | Plays the animated master | Already installed |
| **OBS Studio** | Screen-records at true 4K60 | <https://obsproject.com> — free |
| **ffmpeg** | Muxes audio and video, final encode | `winget install Gyan.FFmpeg` |
| A voice | Narration | Record it yourself, or synthesise — see [below](#3--narration) |

Optional but better: **Audacity** (free) for cleaning up a recorded voice track.

---

## 1 · Record the animated master

The master is [`scenes/meditrack-demo.html`](scenes/meditrack-demo.html) — a
self-contained 3840×2160 timeline with all twelve scenes, timed to match
[`SCRIPT.md`](SCRIPT.md) exactly. It fetches nothing from the network, so it
renders identically offline.

```
1. Open scenes/meditrack-demo.html in Chrome
2. Press F11              → fullscreen
3. Press H                → hides the timecode HUD (do not skip this)
4. Press R                → restarts from 0:00 and plays
```

**Controls:** `space` play/pause · `←` `→` step scenes · `R` restart · `H` toggle HUD

### OBS settings — these matter

| Setting | Value |
|---|---|
| Base + output resolution | `3840×2160` |
| FPS | `60` |
| Encoder | NVENC H.264 (or x264 `veryslow` if no GPU) |
| Rate control | CQP, **CQ 16** — visually lossless for the master |
| Colour | Rec.709, full range |
| Format | `.mkv` while recording, remux to `.mp4` after |

> Record to **MKV, not MP4.** If OBS or the machine dies mid-take an MP4 is
> unrecoverable; an MKV is still perfectly playable. OBS remuxes to MP4 in one
> click afterwards.

On a display smaller than 4K the page scales the stage down to fit — which means
you would record a *downscaled* image. To capture true 4K on a 1080p monitor,
use OBS's **Browser Source** instead of Display Capture: point it at the local
file, set the source to 3840×2160, and OBS renders it off-screen at full
resolution regardless of your monitor.

---

## 2 · Capture the terminal footage

Already done — [`capture/capture-all.sh`](capture/capture-all.sh) produced
everything in [`assets/`](assets/). Re-run it any time; because the demo seeder
uses a fixed random seed, the output is byte-identical:

```bash
bash video/capture/capture-all.sh
```

It ends by asserting that the two figures the film puts on screen — the
₹1,593.00 bill total and the 325 assertions — are actually present in the
captures. **If those checks fail, the film is out of date with the code.** Fix
the film, not the check.

To shoot the terminal live rather than using the text captures:

```bash
# Windows Terminal, 4K, JetBrains Mono 28pt, "One Half Dark" scheme
java -Dfile.encoding=UTF-8 -cp out com.airtribe.meditrack.Main --seedDemo
```

`-Dfile.encoding=UTF-8` is not optional — without it the ₹ renders as `?`.

---

## 3 · Narration

[`narration/voiceover.txt`](narration/voiceover.txt) is the script, marked up
with beats and stress. ≈520 words at ~135 wpm.

**Recording it yourself** gives a noticeably better result than synthesis for a
piece this restrained. A USB condenser mic, a quiet room, and a duvet behind you
is genuinely enough. Record at 48kHz/24-bit.

**Synthesising it** — the file ends with a TTS notes section covering the SSML
break markers and the pronunciations that engines reliably get wrong (`JVM`,
`MIT`, `SonarQube`). Any of ElevenLabs, Azure Neural TTS or Play.ht will do; set
rate to ~0.95, because they all read technical copy too fast.

Either way, normalise to **−16 LUFS integrated, −1.5 dBFS true peak**. That is
the loudness target every platform expects, and it stops the narration fighting
the pad.

---

## 4 · Music and sound design

[`SCRIPT.md`](SCRIPT.md#audio-bed) specifies the bed in full. The short version:

- One evolving synth pad, 70–75 BPM, minor key
- A soft cardiac *thump* on the two-beat — the film's spine, running almost throughout
- **Exactly two stingers.** A sub-drop at 0:56, a reversed cymbal into 2:47. No more
- **0:50–0:56 is completely silent.** Do not let anyone talk you out of this

Royalty-free sources that fit the tone: Epidemic Sound, Artlist, or
Kevin MacLeod's darker ambient sets (CC-BY, credit required).

Duck the music to **−18 dB** under narration. A sidechain compressor keyed to the
voice track is the clean way; manual envelopes are fine too.

---

## 5 · Assemble and export

Any NLE works — DaVinci Resolve is free and handles 4K well. Or, if the animated
master is a single clean take, ffmpeg alone is enough:

```bash
# mux the recorded visuals with the finished audio mix
ffmpeg -i master.mkv -i mix.wav \
       -map 0:v -map 1:a \
       -c:v libx264 -preset veryslow -crf 16 \
       -pix_fmt yuv420p -profile:v high -level 5.1 \
       -c:a aac -b:a 320k -ar 48000 \
       -movflags +faststart \
       meditrack-demo-4k.mp4

# burn in subtitles (optional — most platforms prefer a sidecar .srt)
ffmpeg -i meditrack-demo-4k.mp4 \
       -vf "subtitles=narration/captions.srt:force_style='FontName=Inter,FontSize=22,PrimaryColour=&H00F3EDE6,OutlineColour=&H00000000,BorderStyle=1,Outline=2'" \
       -c:a copy meditrack-demo-4k-subbed.mp4
```

`-pix_fmt yuv420p` looks redundant but is not — without it, some encoders emit
4:4:4, which Safari and several social platforms refuse to play.

`-movflags +faststart` moves the index to the front of the file so it starts
streaming immediately instead of after a full download.

### Delivery targets

| Where | Spec |
|---|---|
| **Master / archive** | 3840×2160, CRF 16, AAC 320k. Keep this |
| **YouTube** | Upload the master. YouTube re-encodes; give it the best source |
| **GitHub README** | 1920×1080, CRF 23, **under 10 MB** — or link out, don't embed |
| **LinkedIn** | 1920×1080, ≤10 min, ≤5 GB. Burn in subtitles: most people watch muted |

---

## 6 · Check before you publish

- [ ] Timecode HUD is hidden (press `H` — the commonest mistake)
- [ ] ₹ renders correctly everywhere, no `?` boxes
- [ ] Every number on screen matches [`assets/`](assets/) — re-run the capture script
- [ ] The 0:50–0:56 silence survived the audio mix
- [ ] Audio peaks below −1.5 dBFS, integrated ≈ −16 LUFS
- [ ] Runtime is 5:00 ± 3s
- [ ] Watch it once at 100% zoom on a 4K display, and once on a phone
- [ ] Watch it once **muted**, with subtitles only — it should still make sense

---

## If you would rather not record anything

The animated master is genuinely watchable on its own. Open
[`scenes/meditrack-demo.html`](scenes/meditrack-demo.html), press `H` then `R`,
and it plays the full five minutes silently. That is a legitimate deliverable for
a code review or a submission — and it costs nothing but a browser.
