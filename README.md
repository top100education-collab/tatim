# Brave Tab Timer (starter v0.1)

This is a beginner starter Android app for Android 8+ (tested target: Android 15).

## What it currently does
- Lets you manually register a tab name and URL.
- Records when you registered it (tab age).
- Provides Start/Stop controls for manually tracked active time.
- Can request Android Usage Access to show total Brave app foreground usage for the last 24 hours.
- Stores records locally on the phone.

## Important limitations
Android does not let an ordinary app read Brave's private tab list or the currently selected tab. Therefore this starter does NOT automatically detect tabs or automatically measure per-tab viewing time. The active timer is manual. The Usage Access figure is total Brave app usage, not per-tab usage.

## Build APK online with GitHub Actions
1. Create a GitHub account and a new empty repository named `BraveTabTimer`.
2. Upload the contents of this project (all files and folders) to the repository.
3. In GitHub, open Actions and enable workflows if asked.
4. Add the workflow file `.github/workflows/build-apk.yml` from this project.
5. Open Actions, select `Build Android APK`, and run the workflow.
6. When it finishes, open the run and download the `BraveTabTimer-debug-apk` artifact.
7. Extract the ZIP and install `app-debug.apk` on your Android phone. Allow installation from that source if Android asks.

## Local build (optional)
Open this folder in Android Studio, let Gradle sync, then Build > Build Bundle(s) / APK(s) > Build APK(s).

## Next phase
This starter is local-only. Cloud sync and the laptop dashboard are intentionally a separate next phase so that browsing history is not accidentally exposed through a public database. We will add authenticated Supabase sync after the phone app is tested.
