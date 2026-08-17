# Publishing Flow to Google Play

Split into what a machine can do and what only you can do. The second list is
longer, and one item on it takes **at least two weeks of wall-clock time**.

---

## Automated (in this repo)

```sh
./tools/release/make-keystore.sh     # once — creates the upload key
./gradlew bundleRelease              # signed AAB for Play
./gradlew assembleRelease            # signed APK for GitHub/F-Droid
```

Output: `app/build/outputs/bundle/release/app-release.aab`

Play requires an **AAB** for new apps; the APK is only for direct distribution.
Store listing text and images already live in `fastlane/metadata/android/en-US/`
and can be pushed with `fastlane supply` once you have API credentials.

---

## Only you can do these

### 1. Developer account — blocking, costs money
$25 one-off, plus identity verification (government ID, and for an
organisation, a D-U-N-S number). Verification is not instant.

### 2. Accept the Developer Distribution Agreement
A contract. It has to be you.

### 3. The 12-testers rule — blocking, 14+ days
Personal accounts created **after 13 November 2023** cannot publish straight to
production. You must run a **closed test with at least 12 testers opted in for
14 continuous days**, then apply for production access (reviewed in ~7 days).

"Opted in" means they accepted the invite *and installed*. Invited-but-not-
installed does not count. Organisation accounts and personal accounts older
than that date are exempt.

Plan for roughly three weeks between first upload and public availability.

### 4. Host the privacy policy
`PRIVACY.md` is written. Play needs a public **URL**, not a file. Cheapest
route: enable GitHub Pages on the repo, or point at the rendered file on
GitHub. Paste that URL into the Play Console listing.

### 5. Content rating questionnaire
A legal declaration about your app's content. Flow should come out at the
lowest rating everywhere — no ads, no purchases, no user content, no comms.

### 6. Data safety form
Declare **"No data collected"** and **"No data shared."** This is the one place
the no-network design pays off: it is true and you can point at the manifest.

### 7. Target audience, ads declaration, news/COVID declarations
All "no" for this app, but they must be answered.

---

## Do these before you upload anything

- [ ] **Dead controls.** Playlist detail shows no tracks; the Settings toggles,
      Sleep and Speed do nothing. Shipping visible controls that do nothing
      invites one-star reviews and can fail review under broken functionality.
      Either finish them or hide them.
- [ ] **Remove the "Playlists you make yourself" line** from
      `full_description.txt` unless playlist detail is finished.
- [ ] **Confirm the artwork rights.** The listing screenshots contain a
      third-party album cover, included on your statement that you have
      permission. Play enforces this under its IP policy and the strike lands
      on your account. Have the permission in writing before you upload.
- [ ] `./gradlew test lint` clean, and install the signed release build and
      use it for a day. R8 breaks Room and Media3 reflection silently.
- [ ] Back up the keystore somewhere you will still have in five years.

---

## Version bumps

`versionCode` must increase on every upload. It lives in `app/build.gradle.kts`
and is currently `1` / `0.1.0`. Play rejects a re-used `versionCode` outright.
