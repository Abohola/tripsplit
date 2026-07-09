# TripSplit Web

This is a static PWA-style companion for iOS guests and desktop browsers.

Run locally:

```powershell
python -m http.server 5173 --directory web
```

Open `http://localhost:5173`.

The current web build stores trips in browser local storage and can copy a snapshot link for another device to import. Live multi-device syncing needs a hosted backend such as Firebase, Supabase, or a small API service.
