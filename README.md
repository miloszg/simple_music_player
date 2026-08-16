# Plainly Music

An offline music player for Android. Reads the music already on your phone, plays
it, and does nothing else.

- **No network permission.** The app cannot phone home because it cannot reach
  the network at all.
- **No ads, no analytics, no accounts, no Play Services.** The same APK runs on
  Google Play, F-Droid and GrapheneOS.
- **Follows your phone.** Light/dark from the system setting, colours from your
  wallpaper on Android 12+.

First app in the *Plainly* suite: open-source, single-purpose, no-nonsense.

## Status

Walking skeleton: it scans a real library, groups it correctly and plays it,
with the notification and lock-screen controls working. Not yet shippable.

| Phase | State |
|---|---|
| 0. Toolchain | done |
| 1. Project skeleton | done |
| 2. Design system | theme + core components done; fast scroller outstanding |
| 3. Library scanner (MediaStore → Room → index) | done, 51 unit tests |
| 4. Playback (Media3) | done: queue, shuffle/repeat, persistence, resumption, browse tree |
| 5. UI | Songs/Albums/Artists/Folders, mini player, now playing. **Missing: detail screens, search UI, settings UI, playlists, favourites UI, sort menus, fast scroller** |
| 6. Release engineering | R8 build verified (4.2 MB). **Missing: CI, signing, baseline profile, store metadata** |

Release APK is 4.2 MB and has been run minified end-to-end, so R8 is not
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

### Testing against a real library

The emulator ships with no music. To seed one that exercises the awkward
grouping cases (compilations, two albums sharing a title, accented artists):

```sh
adb push my-music/ /sdcard/Music/
adb shell content call --uri content://media/external --method scan_volume
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
