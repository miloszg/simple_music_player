#!/usr/bin/env bash
# Point Flow at a real music folder and capture the app with real artwork.
#
#   ./use-my-music.sh ~/Music/Nujabes
#
# This is for looking at the app on your own device with your own files —
# which is the only way to see how it really handles your tags and covers.
#
# Do NOT feed the captures into fastlane/metadata/. Those assets ship in a
# public repo and on a Play listing, and commercial album art in either is
# someone else's copyright. The committed screenshots use licensed photography
# for exactly that reason.
set -euo pipefail

SRC="${1:?usage: use-my-music.sh <folder-of-music>}"
PKG=app.flow.music.debug
OUT="${2:-captures}"

echo "→ pushing $SRC"
adb shell 'for d in /sdcard/Music/*; do rm -rf "$d"; done' 2>/dev/null || true
adb push "$SRC/." /sdcard/Music/ > /dev/null

echo "→ rescanning MediaStore"
adb shell content call --uri content://media --method scan_volume --arg external_primary > /dev/null
sleep 5
echo "  $(adb shell "content query --uri content://media/external/audio/media --projection _id --where \"is_music!=0\"" | wc -l) tracks indexed"

adb shell pm grant $PKG android.permission.READ_MEDIA_AUDIO 2>/dev/null || true
adb shell am force-stop $PKG
adb shell am start -n $PKG/app.flow.music.MainActivity > /dev/null
sleep 7

mkdir -p "$OUT"
shot() { adb shell screencap -p /sdcard/_s.png && adb pull /sdcard/_s.png "$OUT/$1.png" > /dev/null && echo "  $1"; }

shot home
adb shell input tap 900 2285; sleep 3; shot library
adb shell input tap 540 2285; sleep 3; shot search
adb shell input tap 150 2285; sleep 2
adb shell input tap 970 1242; sleep 4; shot player

echo "→ $OUT/"
