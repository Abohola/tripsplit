# TripSplit

TripSplit is a dark glassmorphic trip-expense splitter for groups of friends. Android users can install the APK, and iOS guests can use the hosted web companion.

Itemized receipt expenses can be scanned on-device or entered manually. Every item defaults to the selected shopping group, then private items can be reassigned to one traveler or a smaller group before settlement.

## Android

Build a debug APK:

```powershell
.\gradlew.bat assembleDebug
```

The distributable signed APK is produced locally under `dist/` and uploaded to GitHub Releases when publishing.

## iOS And Web

The web companion lives in `web/` and deploys to GitHub Pages through `.github/workflows/pages.yml`.

Run locally:

```powershell
python -m http.server 5173 --directory web
```

Then open `http://127.0.0.1:5173/`.

## Sync Note

This version is local-first. The web app can copy a snapshot link for another browser to import. Real live multi-device syncing should be added with Firebase, Supabase, or a small hosted API.
