# Screenshot fixtures

Generates the demo library used for the Play Store screenshots, then composes
the store panels from real device captures.

The album and track names are **original**, written in the lofi / Japanese
hip-hop idiom rather than lifted from real releases. Putting actual artist or
album names in store screenshots implies a licensing relationship that does not
exist, and Play enforces that under its misleading-content policy. If you want
real titles for local listening, edit `lib.py` — just don't ship those captures.

Two albums are deliberately left untagged so the fallback cover plates and the
dashed "Untitled" treatment appear in the screenshots.

```sh
cd tools/fixtures
python3 gen_art.py                      # cover art, in Flow's tone palette
python3 gen.py                          # 27 tracks across 6 albums

adb push music/. /sdcard/Music/
adb shell content call --uri content://media --method scan_volume --arg external_primary

# capture shot-<screen>.png from the device, then:
python3 compose.py                      # 1080x1920 store panels
python3 assets.py                       # 512 icon + 1024x500 feature graphic
```

Requires `ffmpeg` and Pillow.
