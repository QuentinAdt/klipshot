# Privacy Policy — Klipshot

_Last updated: 4 September 2026_

## The short version

Klipshot collects nothing, stores nothing about you, and cannot transmit anything. It has no
`INTERNET` permission, so sending data anywhere is not a limitation of policy but of capability:
the operating system will not let it.

## What the app accesses

**Screenshots.** Klipshot needs the `READ_MEDIA_IMAGES` permission to see the screenshot Android
has just written to your device. It queries the media database for the newest image stored in a
`Screenshots` folder, reads its identifier, and places that identifier in the system clipboard.
This is the entire function of the app.

Screenshots are never uploaded, never analysed, never sent anywhere.

## What the app stores

- **Your settings** (whether the watcher is on, which copy mode, whether to vibrate) in the app's
  private storage.
- **The name of the last screenshot copied**, shown in the app's log panel, in that same private
  storage.
- **In fallback mode only**: a copy of the three most recent screenshots in the app's private
  cache, so applications that refuse a media-store URI can still paste them. Older copies are
  deleted automatically. This mode is off by default.

All of it disappears when you uninstall the app.

## What the app shares

Nothing. There are no analytics, no crash reporting, no advertising, no third-party SDKs of any
kind. The app has no network permission and no third-party dependencies.

The clipboard itself is a system-wide feature: once a screenshot is in it, any app you paste into
receives it. That is the point of the app, and it happens only when you paste.

## Children

The app has no content, no accounts and no data collection, and is suitable for any age.

## Contact

Questions or concerns: open an issue at https://github.com/QuentinAdt/klipshot/issues
