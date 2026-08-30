<div align="center">

<img src="app/src/main/res/drawable/ic_launcher_image.png" width="96" alt="Aniyaa logo" />

# Aniyaa

**A native Android client for [nyaa.si](https://nyaa.si)**

[![Android](https://img.shields.io/badge/Android-7.0%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-Open%20Source-brightgreen)](LICENSE)

</div>

---

## Overview

Aniyaa lets you search, filter, and download torrents from nyaa.si — the premier tracker for anime, manga, audio, and more — directly from your Android device. Built with Jetpack Compose and Material 3 for a fast, fluid, and beautiful experience.

---

## Features

| | Feature |
|---|---|
| 🔍 | **Full-text search** across all of nyaa.si, with infinite scroll |
| 🗂️ | **Category filtering** — Anime, Audio, Literature, Live Action, Pictures, Software, and sub-categories |
| ✅ | **Quality filter** — All · No Remakes · Trusted Only |
| ↕️ | **Flexible sorting** — by Date, Seeders, Leechers, Size, Downloads, or Comments |
| 📄 | **Rich torrent cards** — title, category badge, trust status, size, date, seeds, leeches, downloads |
| 📝 | **Torrent details** — markdown description, nested file list, and comments |
| 🔗 | **One-tap actions** — open magnet, copy magnet, download `.torrent`, share, or open on nyaa.si |
| 🔖 | **Bookmarks** — save listings locally; swipe to remove |
| 🕘 | **Search history** — last 50 queries, with swipe-to-delete and clear-all |
| 🎨 | **Material You** — dynamic color on Android 12+, falls back to a purple-blue palette |
| 📱 | **Edge-to-edge UI** — content flows behind system bars for a fully immersive layout |
| ⚡ | **120 Hz+** — locks to the display's highest same-resolution refresh rate |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM · `ViewModel` + `StateFlow` |
| Navigation | Navigation Compose |
| Networking | OkHttp 4.12 |
| Parsing | `XmlPullParser` (RSS) · Jsoup (HTML) |
| Markdown | Markwon 4.6.2 |
| Async | Kotlin Coroutines |
| Min / Target SDK | 24 (Android 7.0) / 35 (Android 15) |

---

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1)+ **or** JDK 17 with Android SDK (API 35)
- An internet connection for the first Gradle sync

### Build & Run

```bash
git clone https://github.com/Gourab0002/Aniyaa.git
cd Aniyaa
./gradlew assembleDebug          # APK → app/build/outputs/apk/debug/
./gradlew installDebug           # build + install on a connected device
```

Or open the project in Android Studio (**File → Open**), let Gradle sync, and press **Run ▶**.

---

## How It Works

Aniyaa queries the nyaa.si RSS feed:

```
https://nyaa.si/?page=rss&q=<query>&c=<category>&f=<filter>&s=<sort>&o=<order>
```

The feed's `nyaa:` namespace fields (seeders, leechers, infoHash, trusted, etc.) are parsed with `XmlPullParser`. Magnet links are assembled from the `infoHash` plus a set of public trackers — no extra client API needed. Torrent detail pages and comments are fetched as HTML and parsed with Jsoup; markdown content is rendered via Markwon.

Search requests are cancelled when a new query starts, pages are merged with unique keys so infinite scroll cannot crash on duplicates, and HTTP responses are always closed. Dates include the year. Release builds enable R8 shrinking.

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

---

## Project Structure

```
app/src/main/java/com/nyaa/aniyaa/
├── MainActivity.kt              # Entry point, NavHost, high refresh rate
├── data/
│   ├── api/                     # RSS + HTML parsers
│   ├── model/                   # Torrent, SearchParams, enums
│   ├── network/                 # Shared OkHttp client + cancellable calls
│   └── repository/              # Search, bookmarks, history
├── ui/
│   ├── screens/                 # Search, history, bookmarks, settings, detail
│   ├── theme/                   # Color, Theme, Type
│   └── viewmodel/               # Shared ViewModels (StateFlow)
└── util/                        # Date formatting, 120 Hz display mode
```

---

## Contributing

1. Fork the repo and create a feature branch.
2. Keep PRs focused and small.
3. Open a pull request against `main` with a clear description.

---

## License

Open source — see [LICENSE](LICENSE) for details.

---

<div align="center">

> **Disclaimer:** Aniyaa is an unofficial third-party client and is not affiliated with or endorsed by nyaa.si.

</div>
