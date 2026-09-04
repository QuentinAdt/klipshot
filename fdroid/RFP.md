# F-Droid submission — ready to paste

Open a new issue at <https://gitlab.com/fdroid/rfp/-/issues/new> (a GitLab account is required),
pick the **Request For Packaging** template, and paste the body below. Then attach
`fdroid/io.github.quentinadt.klipshot.yml` as a comment, so a packager only has to move it into
`metadata/`.

---

**Application name:** Klipshot

**Package ID:** `io.github.quentinadt.klipshot`

**Summary:** Take a screenshot, paste it. Straight to the clipboard, no extra taps.

**Description:**

Android takes a screenshot in one gesture, but putting that screenshot in the clipboard takes
three more: tap the preview, wait for the markup editor, find its copy button. Klipshot removes
those steps. The system gesture is untouched — the app watches MediaStore and writes each new
screenshot to the system clipboard as it appears.

This works because Android's clipboard restriction is asymmetric: only reading requires
foreground focus, while writing is still permitted from the background. A foreground service can
therefore do the job without flashing an invisible activity on every capture.

**License:** Apache-2.0

**Source code:** https://github.com/QuentinAdt/klipshot

**Issue tracker:** https://github.com/QuentinAdt/klipshot/issues

**I am the author of this application.**

**Notes for the packager:**

- No third-party dependencies at all. Plain Java against framework APIs; no AndroidX, no Kotlin.
- No `INTERNET` permission, no analytics, no advertising, no trackers. No anti-features apply.
- `assembleRelease` succeeds without any keystore: the signing config is only wired up when a
  properties file exists outside the repository, so the build produces an unsigned APK ready for
  F-Droid to sign. Verified.
- Fastlane metadata for en-US, fr-FR, es-ES, de-DE and it-IT is already in the repository, with
  phone screenshots.
- Release tag: `v1.0` (versionCode 1).
- minSdk 33, targetSdk 36, builds against compileSdk 36 with AGP 9.4.0 and Gradle 9.7.1.
