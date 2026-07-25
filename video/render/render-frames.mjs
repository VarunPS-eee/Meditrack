/**
 * Frame-exact 4K renderer for the MediTrack demonstration film.
 *
 * Drives the animated master with Puppeteer, stepping its timeline one frame
 * at a time via window.seekFrame(t), and pipes each screenshot straight into
 * ffmpeg's stdin.
 *
 * Why not just screen-record it:
 *   - A recorder captures wall-clock time, so a busy machine drops frames.
 *     Here every frame is requested explicitly; the render is slower than
 *     real time but never lossy.
 *   - Frames never touch the disk. 9,000 uncompressed 4K frames would be
 *     ~90GB; piping them keeps peak usage at the size of the finished file.
 *   - It is reproducible. Same input, same bytes out, on any machine.
 *
 * Usage:
 *   node render-frames.mjs
 *   FPS=30 START=0 END=300 OUT=../out/video.mp4 node render-frames.mjs
 *   START=56 END=64 OUT=../out/smoke.mp4 node render-frames.mjs   # one shot
 */
import puppeteer from 'puppeteer-core';
import { spawn } from 'node:child_process';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { dirname, resolve } from 'node:path';
import { mkdirSync, existsSync } from 'node:fs';

const HERE = dirname(fileURLToPath(import.meta.url));

const FPS    = Number(process.env.FPS   ?? 30);
const START  = Number(process.env.START ?? 0);
const END    = Number(process.env.END   ?? 300);
const FMT    = process.env.FMT  ?? 'png';          // png (lossless) | jpeg (fast)
const QUAL   = Number(process.env.QUAL ?? 95);     // jpeg only
const CRF    = process.env.CRF  ?? '16';
const OUT    = resolve(HERE, process.env.OUT ?? '../out/video-only.mp4');
const PAGE   = resolve(HERE, '../scenes/meditrack-demo.html');

const CHROME = process.env.CHROME ??
  'C:/Program Files/Google/Chrome/Application/chrome.exe';
const FFMPEG = process.env.FFMPEG ??
  'C:/Users/skba2/AppData/Local/Microsoft/WinGet/Packages/Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe/ffmpeg-8.1.2-full_build/bin/ffmpeg.exe';

const W = 3840, H = 2160;

if (!existsSync(CHROME)) { console.error(`Chrome not found: ${CHROME}`); process.exit(1); }
if (!existsSync(FFMPEG)) { console.error(`ffmpeg not found: ${FFMPEG}`); process.exit(1); }
mkdirSync(dirname(OUT), { recursive: true });

const totalFrames = Math.round((END - START) * FPS);
console.log(`render  ${W}x${H} @ ${FPS}fps  ${START}s -> ${END}s  (${totalFrames} frames)`);
console.log(`format  ${FMT}${FMT === 'jpeg' ? ` q${QUAL}` : ''}  crf ${CRF}`);
console.log(`out     ${OUT}\n`);

/* ---------------------------------------------------------------- ffmpeg */
const ff = spawn(FFMPEG, [
  '-y',
  '-f', 'image2pipe',
  '-framerate', String(FPS),
  '-i', '-',
  '-c:v', 'libx264',
  '-preset', 'slow',
  '-crf', String(CRF),
  '-pix_fmt', 'yuv420p',        // without this some players refuse 4:4:4 output
  '-profile:v', 'high',
  '-level', '5.1',
  '-movflags', '+faststart',
  OUT,
], { stdio: ['pipe', 'ignore', 'pipe'] });

let ffErr = '';
ff.stderr.on('data', d => { ffErr += d.toString(); if (ffErr.length > 8000) ffErr = ffErr.slice(-4000); });
ff.on('exit', c => { if (c !== 0) { console.error('\nffmpeg failed:\n' + ffErr); process.exitCode = 1; } });

/** Write honouring backpressure — without this, memory grows unbounded. */
const write = buf => new Promise(res => ff.stdin.write(buf) ? res() : ff.stdin.once('drain', res));

/* ---------------------------------------------------------------- chrome */
const browser = await puppeteer.launch({
  executablePath: CHROME,
  headless: 'new',
  defaultViewport: { width: W, height: H, deviceScaleFactor: 1 },
  args: [
    `--window-size=${W},${H}`,
    '--hide-scrollbars',
    '--force-device-scale-factor=1',
    '--font-render-hinting=none',   // identical text metrics across machines
    '--disable-lcd-text',           // greyscale AA; subpixel fringes on video look like colour noise
    '--force-color-profile=srgb',
    '--disable-gpu-vsync',
    '--allow-file-access-from-files',
  ],
});

const page = await browser.newPage();
await page.goto(pathToFileURL(PAGE).href, { waitUntil: 'networkidle0' });

/* Pin the stage at 1:1 and drop the HUD before the first frame. */
await page.evaluate(() => {
  document.getElementById('hud').classList.add('hide');
  document.getElementById('stage').style.zoom = 1;
});

if (typeof await page.evaluate(() => window.seekFrame) === 'undefined') {
  console.error('seekFrame() missing — the render API is not present in the page.');
  await browser.close(); process.exit(1);
}

/* ---------------------------------------------------------------- loop */
const shotOpts = FMT === 'jpeg'
  ? { type: 'jpeg', quality: QUAL, optimizeForSpeed: true }
  : { type: 'png' };

const t0 = Date.now();
for (let f = 0; f < totalFrames; f++) {
  const t = START + f / FPS;
  await page.evaluate(x => window.seekFrame(x), t);
  await write(await page.screenshot(shotOpts));

  if (f % 60 === 0 || f === totalFrames - 1) {
    const done = f + 1;
    const el   = (Date.now() - t0) / 1000;
    const eta  = el / done * (totalFrames - done);
    process.stdout.write(
      `\r  ${String(done).padStart(5)}/${totalFrames}  ` +
      `${(done / totalFrames * 100).toFixed(1).padStart(5)}%  ` +
      `${(done / el).toFixed(1)} fps  ` +
      `elapsed ${(el / 60).toFixed(1)}m  eta ${(eta / 60).toFixed(1)}m   `);
  }
}

console.log('\n\nclosing…');
await browser.close();
ff.stdin.end();
await new Promise(r => ff.on('close', r));
console.log(`done -> ${OUT}`);
