# Flow — Privacy Policy

_Last updated: 17 August 2026_

Flow does not collect, transmit, store or share any personal data.

## No data leaves your device

Flow has no `INTERNET` permission. It is not declared in the app's manifest, so
the operating system will not allow the app to open a network connection at
all. There is no analytics, no crash reporting, no advertising identifier, no
account, and no Google Play Services dependency.

You can verify this yourself: **Settings → Apps → Flow → Permissions**, or read
`app/src/main/AndroidManifest.xml` in the public source repository.

## What Flow reads, and why

| Permission | Why |
|---|---|
| `READ_MEDIA_AUDIO` (Android 13+), `READ_EXTERNAL_STORAGE` (Android 12 and below) | To find the music files already on your device. This is the only permission that touches your content. |
| `POST_NOTIFICATIONS` | To show the playback notification. Requested the first time you press play. |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `WAKE_LOCK` | To keep playing while the screen is off. |

## What Flow stores, and where

Everything Flow saves stays in its private app storage on your device:

- an index of your music library, rebuilt from the system media store
- your playlists and liked songs
- play counts and the last-played position
- your settings

Uninstalling Flow deletes all of it. If you have Android's system backup
enabled, your playlists, liked songs and settings may be included in that
backup — that is Android's own encrypted backup to your Google account, not
something Flow sends anywhere.

## Children

Flow contains no ads, no purchases, no user accounts and no communication
features, and collects nothing from anyone regardless of age.

## Changes

Any change to this policy will be committed to the public repository, so its
full history is visible in version control.

## Contact

Open an issue at https://github.com/miloszg/simple_music_player/issues
