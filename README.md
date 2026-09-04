# Klipshot

**Screenshots straight to the clipboard. The one thing your Pixel won't do.**

[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-13%2B-3DDC84.svg)](#requirements)
[![No internet permission](https://img.shields.io/badge/network%20access-none-success.svg)](#privacy)

On a Pixel, taking a screenshot is one gesture. Getting it into the clipboard is not:
the preview only ever offers *Share* and *Edit*. The shortest native path is to tap the
preview, wait for Markup to open, then hit its copy button — three deliberate steps for
something that should be free.

Klipshot removes those steps. **You keep the system gesture** (Power + Volume down, or Quick
Tap). The app watches for the screenshot Android just saved and puts it in the system
clipboard. Take the shot, paste it. That's the whole product.

## Why this can work at all

Android has restricted clipboard access since version 10, but the restriction is asymmetric:
only **reading** requires foreground focus or being the default keyboard. **Writing** is still
allowed from the background — `ClipboardService` says so in as many words: *"Writing is allowed
without focus."*

That asymmetry is what makes Klipshot possible without the usual workaround of flashing an
invisible activity on screen every time you take a screenshot. Nothing blinks, nothing steals
focus. Verified on Android 17: screenshots taken from another app — and from the lock screen —
land in the clipboard.

## What you get

- The **native screenshot gesture**, untouched.
- A short vibration and a confirmation toast, both optional.
- A quiet foreground notification (importance `MIN`, so no status bar icon) that keeps the
  watcher alive.
- A fallback mode for apps that refuse to paste a `MediaStore` URI owned by another app: the
  screenshot is copied into the app's own storage and served from its own provider instead.
- Five languages: English, French, Spanish, German, Italian.

## Requirements

Android 13 (API 33) or newer. Built and tested on a Pixel 10 Pro running Android 17.

## Install

Grab the APK from [Releases](../../releases), or build it yourself:

```sh
brew install openjdk@17
brew install --cask android-commandlinetools
./build.sh
```

Then, with USB debugging on:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

When Android asks for photo access, choose **Allow all**. "Select photos" grants only partial
access, and new screenshots would stay invisible to the app — the settings screen tells you if
you end up in that state.

## Permissions, and why each one is there

| Permission | Why |
|---|---|
| `READ_MEDIA_IMAGES` | The only way to see the screenshot Android just wrote to `Pictures/Screenshots`. This is the core function, not a side feature. |
| `POST_NOTIFICATIONS` | A foreground service must show a notification. It is set to minimum importance. |
| `FOREGROUND_SERVICE` + `..._SPECIAL_USE` | Keeps the `ContentObserver` alive so a screenshot is copied the instant it appears. |
| `RECEIVE_BOOT_COMPLETED` | Restarts the watcher after a reboot, so you don't have to think about it. |
| `VIBRATE` | The optional confirmation buzz. |

## Privacy

**Klipshot has no `INTERNET` permission.** It cannot send anything anywhere, by construction —
not telemetry, not crash reports, not your screenshots. Check the manifest; it is fifty lines.
Nothing is stored beyond a handful of settings and, in fallback mode, the last three screenshots
in the app's private cache.

## How it is put together

| File | Role |
|---|---|
| `WatcherService` | Foreground service: `ContentObserver` on MediaStore, debounce, quiet notification |
| `ScreenshotFinder` | MediaStore query: newest complete, non-pending image under a `Screenshots` folder |
| `ClipboardWriter` | Builds the `ClipData` and writes it |
| `ShotProvider` | Fallback mode: serves a local copy through the app's own provider |
| `MainActivity` | Permissions, options, and a check that reads the clipboard back and previews it |
| `BootReceiver` | Restarts the watcher on boot |

No third-party dependencies. Plain Java, plain framework APIs.

## Known limits

- Photo access must be **full**. Partial access ("Select photos") makes new screenshots
  invisible to the app.
- Pasting an image only works in apps that accept images — Gmail, Messages, WhatsApp, Slack,
  Docs. A plain text field will not take one.
- The watcher needs to stay alive; allowing unrestricted battery use is recommended.

## Debugging

```sh
adb logcat -s Klipshot          # app log
adb shell input keyevent 120    # fire a system screenshot
```

## License

[Apache License 2.0](LICENSE).
