# app_install

A lightweight Android app that reads APK assets directly from a GitHub release and lets the user tap to download and install them.

## Behavior
- Loads the latest release from `ChrisMartin00/app_install`
- Lists only `.apk` assets
- Downloads the selected APK asset
- Tries a silent install when the device has root
- Falls back to the system package installer when silent install is unavailable

## Notes
- This is a visible admin-style installer, not a hidden background installer.
- It is designed for older Android devices by keeping the implementation simple.
- On a rooted or privileged system build, the app can install without user confirmation.

## Configure the feed
The current source is hardcoded to:
- Owner: `ChrisMartin00`
- Repo: `app_install`

If you want to point it at a different GitHub release feed later, update the constants in `MainActivity.java`.
