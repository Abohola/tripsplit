const CACHE_NAME = "tripsplit-web-v2";
const ASSETS = [
  "./",
  "./index.html",
  "./styles.css",
  "./app.js",
  "./manifest.webmanifest",
  "./assets/trip-glass-bg.png",
  "./assets/app-logo.png",
  "./assets/app-logo-192.png",
  "./assets/app-logo-512.png",
  "./assets/favicon.png",
];

self.addEventListener("install", (event) => {
  event.waitUntil(caches.open(CACHE_NAME).then((cache) => cache.addAll(ASSETS)));
});

self.addEventListener("fetch", (event) => {
  event.respondWith(caches.match(event.request).then((cached) => cached || fetch(event.request)));
});
