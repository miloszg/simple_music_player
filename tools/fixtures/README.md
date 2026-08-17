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

## Cover art

The four covers are photographs from Lorem Picsum (Unsplash-sourced, free for
commercial use), cropped square and graded as one series. `gen_art.py` still
contains the original drawn covers if you prefer them.

Do not substitute real commercial album art here. These images end up in
`fastlane/metadata/`, which is a public repo and a Play listing — third-party
cover art in either is someone else's copyright and Play enforces it under its
intellectual-property policy. To see the app with your own music, use
`use-my-music.sh` instead; those captures stay on your machine.

## Looking at it with your own library

```sh
./use-my-music.sh ~/Music/SomeAlbum
```

Pushes the folder, rescans MediaStore, drives the app and pulls captures to
`captures/`. This is the honest way to check tag handling, odd durations,
multi-disc sets and missing artwork.

```sh
cd tools/fixtures
python3 gen_art.py                      # cover art, in Flow's tone palette
python3 gen.py                          # 27 tracks across 6 albums

adb push music/. /sdcard/Music/
adb shell content call --uri content://media --method scan_volume --arg external_primary

# capture shot-<screen>.png from the device, then:
python3 panels.py                       # 1080x1920 store panels (device frame + shadow)
python3 assets.py                       # 512 icon + 1024x500 feature graphic
```

Requires `ffmpeg` and Pillow.
