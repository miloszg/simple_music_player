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

### Licensed third-party artwork

The library also contains **Nujabes — Metaphorical Music** (Hydeout Productions,
2003) with its real tracklist, durations and release artwork. It is included at
the repository owner's direction, on their statement that they hold permission
from the rights holder to use the artwork in Flow's store listing.

Anything else you add here needs the same footing. These files end up in
`fastlane/metadata/`, which is a public repo and a Play listing, and Play
enforces third-party cover art under its intellectual-property policy — the
strike lands on the developer account. If you just want to look at the app with
your own collection, use `use-my-music.sh`; those captures stay on your machine.

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
