#!/usr/bin/env bash
#
# Builds the MediTrack demonstration film end to end, from source in this repo.
#
#   bash video/render/build.sh              # everything
#   bash video/render/build.sh audio        # audio only (fast — skips the render)
#   bash video/render/build.sh mux          # re-mux existing parts (seconds)
#
# The video render takes ~45 minutes; audio takes about two. If you only want to
# swap the voice or the music, run `audio` then `mux` and leave the frames alone.
#
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT="$HERE/../out"
STAGE="${1:-all}"

FFDIR="/c/Users/skba2/AppData/Local/Microsoft/WinGet/Packages/Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe/ffmpeg-8.1.2-full_build/bin"
FFMPEG="${FFMPEG:-$FFDIR/ffmpeg.exe}"
FFPROBE="${FFPROBE:-$FFDIR/ffprobe.exe}"
command -v ffmpeg >/dev/null 2>&1 && { FFMPEG=ffmpeg; FFPROBE=ffprobe; }

mkdir -p "$OUT"

render_video() {
  echo "==> rendering 9000 frames at 3840x2160 (this is the slow part)"
  ( cd "$HERE" && FPS=30 START=0 END=300 FMT=jpeg QUAL=96 CRF=16 \
      OUT=../out/video-only.mp4 node render-frames.mjs )
}

build_audio() {
  echo "==> narration (edge-tts, en-GB-RyanNeural)"
  ( cd "$HERE" && python make-narration.py )
  echo "==> score (synthesised — pad, heartbeat, two stingers)"
  ( cd "$HERE" && python make-music.py )

  echo "==> mixing voice over score"
  # The music was rendered with ducking envelopes already applied, so this is a
  # straight sum. Narration sits at unity; the bed has been cut to -9dB beneath it.
  "$FFMPEG" -y -v error \
    -i "$OUT/narration.wav" -i "$OUT/music.wav" \
    -filter_complex "[0:a]volume=1.0[v];[1:a]volume=0.85[m];[v][m]amix=inputs=2:duration=first:normalize=0,\
loudnorm=I=-16:TP=-1.5:LRA=11,aformat=sample_fmts=s16:channel_layouts=stereo[out]" \
    -map "[out]" "$OUT/mix.wav"
}

mux() {
  echo "==> muxing"
  "$FFMPEG" -y -v error \
    -i "$OUT/video-only.mp4" -i "$OUT/mix.wav" \
    -map 0:v -map 1:a -c:v copy \
    -c:a aac -b:a 320k -ar 48000 \
    -movflags +faststart -shortest \
    "$OUT/meditrack-demo-4k.mp4"

  echo "==> 1080p web cut"
  "$FFMPEG" -y -v error -i "$OUT/meditrack-demo-4k.mp4" \
    -vf "scale=1920:1080:flags=lanczos" \
    -c:v libx264 -preset slow -crf 21 -pix_fmt yuv420p \
    -c:a aac -b:a 192k -movflags +faststart \
    "$OUT/meditrack-demo-1080p.mp4"
}

case "$STAGE" in
  all)   render_video; build_audio; mux ;;
  video) render_video ;;
  audio) build_audio ;;
  mux)   mux ;;
  *) echo "unknown stage: $STAGE (use: all | video | audio | mux)"; exit 1 ;;
esac

echo
echo "=== output ==="
for f in "$OUT"/*.mp4; do
  [ -e "$f" ] || continue
  d=$("$FFPROBE" -v error -show_entries format=duration -of default=nw=1:nk=1 "$f" 2>/dev/null || echo "?")
  r=$("$FFPROBE" -v error -select_streams v:0 -show_entries stream=width,height \
        -of csv=p=0:s=x "$f" 2>/dev/null || echo "?")
  printf "  %-32s %8s  %-10s %s\n" "$(basename "$f")" \
         "$(du -h "$f" | cut -f1)" "$r" "${d%.*}s"
done
