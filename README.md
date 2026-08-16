# Flow

An offline music player for Android. Reads the music already on your phone, plays
it, and does nothing else.

- **No network permission.** The app cannot phone home because it cannot reach
  the network at all.
- **No ads, no analytics, no accounts, no Play Services.** The same APK runs on
  Google Play, F-Droid and GrapheneOS.
- **Follows your phone.** Light and dark from the system setting. The palette is
  Flow's own — one accent, deliberately not wallpaper-derived.

Open-source, single-purpose, no-nonsense.

The idea behind the name and the mark: not standing up to the current, becoming
part of it. The icon is three measured bars dissolving into water.

## Status

Walking skeleton: it scans a real library, groups it correctly and plays it,
with the notification and lock-screen controls working. Not yet shippable.

| Area | State |
|---|---|
| Toolchain, Gradle, CI-less build | done |
| Library: MediaStore scan → Room → in-memory index | done, 51 unit tests |
| Playback: Media3 session, queue, resumption, browse tree | done |
| FLOW design: palette, type, cover plates, mark | done |
| Screens: Home, Search, Library, player, detail, settings | done |
| Playlists + liked songs | data layer done; some UI still stubbed |
| Release engineering: CI, signing, baseline profile, store metadata | not started |

Release APK is 4.7 MB and has been run minified end-to-end, so R8 is not
silently breaking Room or Media3 reflection.

## Building

Requires JDK 21 and the Android SDK (platform 37.1, build-tools 37).

```sh
./gradlew :app:assembleDebug        # build
./gradlew :app:installDebug         # build + install on the attached device
./gradlew test lint                 # unit tests and lint
./gradlew assembleRelease           # minified build
```

`local.properties` must point at your SDK; it is not checked in.

### Design

The visual language — Cherry `#DE1043`, Instrument Serif for titles and cover
plates, Instrument Sans for everything else, four rotating plate treatments for
untagged albums — is ported from the Flow design project. Both typefaces are SIL
Open Font License; the licence ships in `app/src/main/assets/licenses/`.

### Testing against a real library

The emulator ships with no music. To seed one that exercises the awkward
grouping cases (compilations, two albums sharing a title, accented artists):

```sh
adb push my-music/ /sdcard/Music/
adb shell content call --uri content://media --method scan_volume --arg external_primary
```

### Toolchain notes

Worth knowing before you touch `gradle/libs.versions.toml`:

- **`compileSdk` is 37, `targetSdk` is 36.** Compose 1.12 and lifecycle 2.11
  refuse to be consumed below 37. Target stays at 36 — the level Play mandates
  from 2026-08-31 — so we don't silently opt into Android 17 behaviour changes.
- **AGP 9 has built-in Kotlin** and pins the compiler to the KGP it was built
  against. The root `build.gradle.kts` raises it via a buildscript constraint,
  because several dependencies now ship Kotlin 2.4 metadata.
- **KSP must use the new independent versioning** (2.3.0+). The older
  `<kotlin>-<ksp>` builds use the `kotlin.sourceSets` DSL, which built-in Kotlin
  rejects outright.
- **Hilt must be 2.60+**, otherwise its bundled `kotlin-metadata-jvm` cannot read
  androidx.activity 1.13.

## Licence

GPL-3.0-or-later. See [LICENSE](LICENSE).
