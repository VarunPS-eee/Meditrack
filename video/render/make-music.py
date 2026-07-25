"""
Synthesises the score for the MediTrack demonstration film.

Generates the three audio layers SCRIPT.md specifies, from scratch — no samples,
no downloads, nothing licensed:

  1. An evolving pad. Detuned sine partials over a slow chord progression,
     with a breathing amplitude LFO. Voiced low and wide so it sits under
     narration without competing for the same frequencies.

  2. A cardiac pulse. The film's spine — a lub-dub on the two-beat at 72 BPM,
     synthesised as a fast-decaying low sine. Present from the opening frame
     to the last.

  3. Two stingers, and only two. A sub-drop into the JVM reveal at 0:56, and
     a reversed-cymbal riser into the bill at 2:47. Stingers stop working if
     you use five.

And one absence: 0:50-0:56 is cut to true digital silence. That gap is the most
deliberate thing in the mix.

    python make-music.py

Output: video/out/music.wav  (48kHz stereo, 300s)
"""
from __future__ import annotations
import wave
from pathlib import Path
import numpy as np

SR = 48_000
DURATION = 300.0
OUT = Path(__file__).resolve().parent.parent / "out" / "music.wav"

BPM = 72.0
BEAT = 60.0 / BPM

# The silence, in seconds. Nothing survives inside this window.
SILENCE = (50.0, 56.0)

# Chord progression, as (start_second, [midi notes]). Voiced low; the pad's job
# is to hold the floor, not to be noticed.
PROGRESSION = [
    (0.0,   [45, 52, 57, 64]),   # A2  E3  A3  E4   — open fifths, unresolved
    (30.0,  [45, 52, 57, 64]),
    (56.0,  [41, 48, 53, 60]),   # F2  C3  F3  C4   — drops a third into the reveal
    (100.0, [38, 45, 50, 57]),   # D2  A2  D3  A3   — darker still for the architecture
    (167.0, [45, 52, 57, 64]),   # back to A for the payoff
    (215.0, [41, 48, 53, 60]),
    (245.0, [43, 50, 55, 62]),   # G2  D3  G3  D4   — lifts under the E->A reveal
    (275.0, [48, 55, 60, 67]),   # C3  G3  C4  G4   — resolves for the close
]


def midi_hz(n: float) -> float:
    return 440.0 * 2 ** ((n - 69) / 12.0)


def box_smooth(x: np.ndarray, width: int) -> np.ndarray:
    """Moving average in O(n), via a cumulative sum and a windowed difference."""
    c = np.cumsum(np.concatenate([[0.0], x]))
    half = width // 2
    i = np.arange(len(x))
    lo = np.clip(i - half, 0, len(x))
    hi = np.clip(i + half, 0, len(x))
    return (c[hi] - c[lo]) / np.maximum(hi - lo, 1)


def pad() -> np.ndarray:
    """
    Sustained chord bed with per-partial detune and a slow breathing LFO.

    Each chord is synthesised only over the span it actually occupies, plus the
    crossfade tails. Computing every chord across the full five minutes — which
    the first version did — meant 64 passes over 14.4 million float64 samples,
    and spent its entire time allocating temporaries it then multiplied by zero.
    float32 halves the memory traffic again.
    """
    n_total = int(DURATION * SR)
    left = np.zeros(n_total, dtype=np.float32)
    right = np.zeros(n_total, dtype=np.float32)
    fade = 4.0

    for i, (start, notes) in enumerate(PROGRESSION):
        end = PROGRESSION[i + 1][0] if i + 1 < len(PROGRESSION) else DURATION

        a = max(0, int((start - fade) * SR))
        b = min(n_total, int((end + fade) * SR))
        if b <= a:
            continue

        t = (np.arange(a, b, dtype=np.float32) / SR)
        env = (np.clip((t - start + fade) / fade, 0, 1)
               * np.clip((end + fade - t) / fade, 0, 1)).astype(np.float32)
        env = np.sin(env * (np.pi / 2), dtype=np.float32) ** 2      # equal power

        for j, note in enumerate(notes):
            f = midi_hz(note)
            voice_gain = env / (1 + j * 0.7)
            # each partial detuned a few cents and panned differently — this is
            # what stops four sines sounding like a church organ
            for detune, gain, pan in ((-0.06, 1.0, 0.35), (0.07, 0.9, 0.65)):
                fd = np.float32(f * (1 + detune / 100))
                phase = (2 * np.pi * fd) * t
                v = np.sin(phase, dtype=np.float32)
                v *= 1 + 0.05 * np.sin((2 * np.pi * (0.05 + 0.013 * j)) * t + j, dtype=np.float32)
                v += 0.28 * np.sin(2 * phase, dtype=np.float32)     # octave
                v += 0.10 * np.sin(3 * phase, dtype=np.float32)     # fifth above
                v *= voice_gain * np.float32(gain)
                left[a:b] += v * np.float32(1 - pan)
                right[a:b] += v * np.float32(pan)

    return np.stack([left, right]).astype(np.float64)


def heartbeat() -> np.ndarray:
    """Lub-dub on the two-beat. Fast attack, exponential decay, deliberately dull."""
    out = np.zeros((2, int(DURATION * SR)))
    n = 0
    while True:
        beat_t = n * BEAT * 2
        if beat_t >= DURATION:
            break
        for offset, amp, f0 in ((0.0, 1.0, 54.0), (0.34, 0.62, 46.0)):   # lub, dub
            s = int((beat_t + offset) * SR)
            dur = int(0.34 * SR)
            if s + dur >= out.shape[1]:
                continue
            k = np.arange(dur) / SR
            env = np.exp(-k * 17)
            # slight downward pitch sweep is what makes it read as a thump
            body = np.sin(2 * np.pi * (f0 * (1 - 0.28 * k)) * k) * env * amp
            body += 0.18 * np.sin(2 * np.pi * f0 * 2 * (1 - 0.2 * k) * k) * env * amp
            out[0, s:s + dur] += body
            out[1, s:s + dur] += body
        n += 1
    return out


def stingers() -> np.ndarray:
    """One sub-drop into the reveal, one riser into the bill. No others."""
    out = np.zeros((2, int(DURATION * SR)))

    # sub-drop at 0:56 — the JVM reveal
    s, dur = int(56.0 * SR), int(2.6 * SR)
    k = np.arange(dur) / SR
    sweep = 92 * np.exp(-k * 1.5) + 26
    drop = np.sin(2 * np.pi * np.cumsum(sweep) / SR) * np.exp(-k * 0.85) * 1.15
    out[:, s:s + dur] += drop

    # reversed riser into 2:47 — the bill. Built forwards, then flipped.
    dur = int(3.2 * SR)
    s = int(167.0 * SR) - dur
    k = np.arange(dur) / SR
    noise = np.random.default_rng(20260726).normal(0, 1, dur)
    # cheap one-pole high-pass to thin it out; a full-band riser muddies the vocal
    hp = np.diff(np.concatenate([[0.0], noise]))
    riser = (hp * np.exp(-k * 2.2))[::-1] * 0.30
    out[0, s:s + dur] += riser
    out[1, s:s + dur] += riser * 0.92
    return out


def main():
    OUT.parent.mkdir(parents=True, exist_ok=True)
    print("synthesising pad…");        mix = pad() * 0.055
    print("synthesising heartbeat…");  mix += heartbeat() * 0.30
    print("synthesising stingers…");   mix += stingers() * 0.24

    t = np.arange(mix.shape[1]) / SR

    # duck under the narration windows so the voice never fights the bed
    duck = np.ones_like(t)
    for start, dur in ((8, 6), (30, 20), (57, 45), (102, 20), (132, 37),
                       (169, 49), (217, 32), (247, 34), (282, 17)):
        w = (t >= start) & (t <= start + dur)
        duck[w] = 0.34                       # ≈ -9dB under voice
    # Smooth the duck so it breathes rather than clicks. A box filter via
    # cumulative sums is O(n); np.convolve here is direct convolution, which at
    # 14.4M samples against a 16.8k kernel is ~2.4e11 operations and effectively
    # never finishes.
    duck = box_smooth(duck, int(0.35 * SR))
    mix *= duck

    # the silence — hard, with short ramps so it lands as a cut, not a fault
    a, b = int(SILENCE[0] * SR), int(SILENCE[1] * SR)
    ramp = int(0.22 * SR)
    mix[:, a:b] = 0.0
    mix[:, a - ramp:a] *= np.linspace(1, 0, ramp)
    mix[:, b:b + ramp] *= np.linspace(0, 1, ramp)

    # fade in from black, fade out to nothing
    fi, fo = int(2.0 * SR), int(4.0 * SR)
    mix[:, :fi] *= np.linspace(0, 1, fi)
    mix[:, -fo:] *= np.linspace(1, 0, fo)

    peak = np.abs(mix).max()
    mix = mix / peak * 0.72 if peak > 0 else mix
    print(f"peak {20*np.log10(np.abs(mix).max()):.1f} dBFS")

    data = (np.clip(mix.T, -1, 1) * 32767).astype("<i2")
    with wave.open(str(OUT), "wb") as w:
        w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes(data.tobytes())
    print(f"music -> {OUT}  ({mix.shape[1]/SR:.1f}s)")


if __name__ == "__main__":
    main()
