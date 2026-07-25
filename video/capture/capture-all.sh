#!/usr/bin/env bash
#
# Captures every piece of real terminal output the film uses.
#
# Nothing in the video is mocked up. Because DemoDataSeeder seeds a fixed
# Random, these captures are byte-identical between runs -- which is what makes
# the film reproducible, and what lets you re-shoot a single shot months later
# and have it still match the surrounding footage.
#
#   Usage:  bash video/capture/capture-all.sh
#   Output: video/assets/shot-*.txt
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUT="$ROOT/video/assets"
cd "$ROOT"

mkdir -p "$OUT"

echo "==> compiling"
rm -rf out
javac -encoding UTF-8 -d out $(find src/main/java -name "*.java")

# Every capture forces UTF-8 so the rupee symbol renders. On a POSIX-locale
# terminal it degrades to '?' -- the same WORA leak documented in JVM_Report 5.3.
run() { printf "$1" | java -Dfile.encoding=UTF-8 -cp out com.airtribe.meditrack.Main "${@:2}"; }

echo "==> shot 06 · JVM class-loading order"
run '0\n' --seedDemo 2>&1 | sed -n '1,20p' > "$OUT/shot-06-jvm-boot.txt"

echo "==> shot 09 · the senior-citizen bill (Factory + Strategy + Template Method)"
run '3\n3\nAPT-0005\n3\n5\nAPT-0005\nRoutine cardiac review\n4\n1\nAPT-0005\n1\n0\n' --seedDemo 2>&1 \
  | awk '/CONSULTATION BILL/{f=1} f' | sed -n '1,22p' > "$OUT/shot-09-bill.txt"

echo "==> shot 10 · AI triage"
run '6\n1\nchest pain and breathlessness\n0\n' --seedDemo 2>&1 \
  | awk '/AI TRIAGE/{f=1} f' | sed -n '1,26p' > "$OUT/shot-10-triage.txt"

echo "==> shot 11 · test suite"
java -cp out com.airtribe.meditrack.Main --runTests 2>&1 | tail -12 > "$OUT/shot-11-tests.txt"

echo "==> b-roll · patient case sheet"
run '1\n7\nPAT-0005\n0\n' --seedDemo 2>&1 \
  | awk '/PATIENT CASE SHEET/{f=1} f' | sed -n '1,28p' > "$OUT/broll-case-sheet.txt"

echo "==> b-roll · doctor practice sheet"
run '2\n7\nDOC-0002\n0\n' --seedDemo 2>&1 \
  | awk '/PRACTICE SHEET/{f=1} f' | sed -n '1,26p' > "$OUT/broll-practice-sheet.txt"

echo "==> b-roll · reports"
run '7\n3\n0\n' --seedDemo 2>&1 | sed -n '/status breakdown/,/rate/p' > "$OUT/broll-status.txt"
run '7\n6\n0\n' --seedDemo 2>&1 | sed -n '/Revenue summary/,/Procedure/p'  > "$OUT/broll-revenue.txt"

echo "==> b-roll · invalid input handling"
{ printf 'patients\n99\nquit\n0\n' | java -Dfile.encoding=UTF-8 -cp out com.airtribe.meditrack.Main 2>&1 \
    | grep -A3 -E 'is not a main menu option'; } > "$OUT/broll-errors.txt"

echo
echo "captured into video/assets:"
ls -1 "$OUT"
echo
echo "Sanity-check the two figures the film puts on screen:"
grep -c . "$OUT/shot-09-bill.txt"   >/dev/null && grep '1,593.00' "$OUT/shot-09-bill.txt" \
  && echo "  OK  bill total 1,593.00 present" || echo "  !!  bill total MISSING - re-check the capture"
grep -q '325' "$OUT/shot-11-tests.txt" \
  && echo "  OK  325 assertions present" || echo "  !!  test count MISSING"
