"""
Builds the narration track from video/narration/voiceover.txt.

Parses the timestamped blocks out of the script, synthesises each one with a
free Microsoft neural voice via edge-tts, then lays them onto a 300-second bed
at their scripted start times.

Each block is generated separately rather than as one long utterance for two
reasons: a block that overruns its slot can be reported precisely, and a
re-record of one line does not invalidate the rest of the track.

    python make-narration.py            # generate
    python make-narration.py --dry-run  # parse and report only, no synthesis

Output: video/out/narration.wav  (48kHz stereo, 300s)
"""
from __future__ import annotations
import argparse, asyncio, re, subprocess, sys, wave
from pathlib import Path

HERE = Path(__file__).resolve().parent
SCRIPT = HERE.parent / "narration" / "voiceover.txt"
OUTDIR = HERE.parent / "out"
SEGDIR = OUTDIR / "narration-segments"

VOICE = "en-GB-RyanNeural"   # British documentary register; matches the script's tone
RATE = "-8%"                 # engines read technical copy too fast
DURATION = 300.0

FFMPEG = ("C:/Users/skba2/AppData/Local/Microsoft/WinGet/Packages/"
          "Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe/"
          "ffmpeg-8.1.2-full_build/bin/ffmpeg.exe")
FFPROBE = FFMPEG.replace("ffmpeg.exe", "ffprobe.exe")

HEADER = re.compile(r"^(\d+):(\d{2})\s+[—-]\s+SHOT", re.M)


def parse_blocks(text: str) -> list[tuple[float, str]]:
    """Pull (start_seconds, spoken_text) out of the script."""
    blocks: list[tuple[float, str]] = []
    matches = list(HEADER.finditer(text))

    for i, m in enumerate(matches):
        start = int(m.group(1)) * 60 + int(m.group(2))
        # skip the remainder of the header line ("2 · THE CLAIM"), which is a
        # label for the reader and must never reach the synthesiser
        line_end = text.find("\n", m.end())
        body_start = line_end + 1 if line_end != -1 else m.end()
        body = text[body_start: matches[i + 1].start() if i + 1 < len(matches) else len(text)]

        # drop the rule lines that fence each section, and the trailing TTS notes
        body = re.sub(r"^[─═-]{10,}$", "", body, flags=re.M)
        body = re.split(r"NOTES FOR TEXT-TO-SPEECH", body)[0]
        # drop stage directions and inline shot markers
        body = re.sub(r"\([^)]*\)", "", body)
        body = re.sub(r"\[\s*\d+:\d+[^\]]*\]", "", body)
        # beats -> punctuation the voice already knows how to pause on
        body = body.replace("[///]", ". ").replace("[//]", ", ")
        body = body.replace("*", "")                      # stress marks
        body = body.replace("←", " ").replace("→", " ")   # editorial arrows
        body = re.sub(r"\s+", " ", body).strip()
        body = re.sub(r"\s+([,.])", r"\1", body)
        body = re.sub(r"[,.]{2,}", ".", body)

        if body and len(body) > 12:
            blocks.append((float(start), body))
    return blocks


def probe(path: Path) -> float:
    out = subprocess.run(
        [FFPROBE, "-v", "error", "-show_entries", "format=duration",
         "-of", "default=nw=1:nk=1", str(path)],
        capture_output=True, text=True, check=True)
    return float(out.stdout.strip())


async def synth(blocks: list[tuple[float, str]]) -> list[Path]:
    import edge_tts
    SEGDIR.mkdir(parents=True, exist_ok=True)
    paths = []
    for i, (start, text) in enumerate(blocks):
        dest = SEGDIR / f"seg{i:02d}.mp3"
        await edge_tts.Communicate(text, VOICE, rate=RATE).save(str(dest))
        paths.append(dest)
        print(f"  [{i:02d}] {int(start//60)}:{int(start%60):02d}  "
              f"{probe(dest):5.1f}s  {text[:58]}...")
    return paths


def assemble(blocks, paths):
    """Lay each segment onto a silent 300s bed at its scripted offset."""
    inputs, filters, labels = [], [], []
    for i, ((start, _), p) in enumerate(zip(blocks, paths)):
        inputs += ["-i", str(p)]
        filters.append(
            f"[{i}:a]aresample=48000,adelay={int(start*1000)}|{int(start*1000)},"
            f"apad[a{i}]")
        labels.append(f"[a{i}]")

    graph = ";".join(filters) + ";" + "".join(labels) + \
        f"amix=inputs={len(paths)}:duration=longest:normalize=0[mix];" \
        f"[mix]atrim=0:{DURATION},asetpts=N/SR/TB," \
        f"loudnorm=I=-16:TP=-1.5:LRA=11," \
        f"aresample=48000," \
        f"aformat=sample_fmts=s16:channel_layouts=stereo[out]"
    # aresample is not optional: loudnorm internally runs at 192kHz and, without
    # being brought back down, writes a 220MB wav instead of a 55MB one.

    dest = OUTDIR / "narration.wav"
    subprocess.run([FFMPEG, "-y", "-v", "error", *inputs,
                    "-filter_complex", graph, "-map", "[out]", str(dest)], check=True)
    return dest


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    blocks = parse_blocks(SCRIPT.read_text(encoding="utf-8"))
    print(f"parsed {len(blocks)} narration blocks from {SCRIPT.name}\n")

    if args.dry_run:
        for start, text in blocks:
            words = len(text.split())
            print(f"  {int(start//60)}:{int(start%60):02d}  {words:3d}w  "
                  f"~{words/135*60:4.1f}s  {text[:70]}")
        return

    OUTDIR.mkdir(parents=True, exist_ok=True)
    print(f"voice {VOICE} at rate {RATE}\n")
    paths = asyncio.run(synth(blocks))

    # a block that runs past the next block's start will collide with it
    print("\noverrun check:")
    clean = True
    for i, ((start, _), p) in enumerate(zip(blocks, paths)):
        nxt = blocks[i + 1][0] if i + 1 < len(blocks) else DURATION
        dur = probe(p)
        if start + dur > nxt + 0.35:
            print(f"  !! block {i} at {start:.0f}s runs {dur:.1f}s, "
                  f"overlapping the next at {nxt:.0f}s by {start+dur-nxt:.1f}s")
            clean = False
    print("  all blocks fit their slots" if clean else
          "  (tighten the copy, or shift the timeline in SCRIPT.md)")

    dest = assemble(blocks, paths)
    print(f"\nnarration -> {dest}  ({probe(dest):.1f}s)")


if __name__ == "__main__":
    main()
