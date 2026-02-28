# Aniyaa

A native Android app for searching torrents on [nyaa.si](https://nyaa.si) — the premier tracker for anime, manga, audio, and more. Built with Jetpack Compose and Material 3 for a fast, fluid experience.

---

## Features

- **Full-text search** — find any torrent by title or keyword
- **Category filtering** — narrow results to Anime, Audio, Literature, Live Action, Pictures, Software, and their sub-categories
- **Quality filter** — show all results, exclude remakes, or display trusted uploads only
- **Flexible sorting** — sort by Date, Seeders, Leechers, Size, Downloads, or Comments in ascending or descending order
- **Torrent cards** — each result shows title, category badge, Trusted / Remake status, file size, publish date, seeder count, leecher count, and download count at a glance
- **Detail screen** — view full torrent metadata plus one-tap actions:
  - Open magnet link in any torrent client
  - Copy magnet link to clipboard
  - Download the `.torrent` file
  - Share the torrent page or magnet link
  - Open the torrent page on nyaa.si
- **Material You** — dynamic color support on Android 12+ adapts to your wallpaper; gracefully falls back to a purple-blue palette on older devices
- **Edge-to-edge UI** — content draws behind the system bars for a truly immersive layout

---

## Screenshots

> _Build the app and run it on a device or emulator to see the UI._

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM — `ViewModel` + `StateFlow` |
| Navigation | Navigation Compose |
| Networking | OkHttp 4.12 |
| XML parsing | Android `XmlPullParser` (no extra deps) |
| Async | Kotlin Coroutines |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 35 (Android 15) |

---

## Project Structure

```
app/src/main/
├── java/com/nyaa/aniyaa/
│   ├── MainActivity.kt              # Entry point, NavHost setup
│   ├── data/
│   │   ├── api/
│   │   │   └── NyaaRssParser.kt     # RSS XML parser + magnet-link builder
│   │   ├── model/
│   │   │   └── Torrent.kt           # Data models, enums, SearchParams
│   │   └── repository/
│   │       └── NyaaRepository.kt    # OkHttp calls, URL builder
│   └── ui/
│       ├── screens/
│       │   ├── SearchScreen.kt      # Search bar, results list, filter sheet
│       │   └── TorrentDetailScreen.kt # Full detail + action buttons
│       ├── theme/
│       │   ├── Color.kt
│       │   ├── Theme.kt             # Dynamic color / dark mode support
│       │   └── Type.kt
│       └── viewmodel/
│           └── SearchViewModel.kt   # StateFlow-backed UI state
└── res/
    ├── drawable/                    # Adaptive icon foreground
    ├── mipmap-anydpi-v26/           # Adaptive icon definitions
    └── values/                      # strings, colors, theme
```

---

## Building

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer **or** JDK 17 + Android SDK (API 35)
- Android SDK Build-Tools 35
- An internet connection for the first Gradle sync (downloads dependencies)

### Clone & build

```bash
git clone https://github.com/Gourab0002/Aniyaa.git
cd Aniyaa
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

### Install directly on a connected device

```bash
./gradlew installDebug
```

### Open in Android Studio

1. **File → Open** and select the `Aniyaa` directory.
2. Wait for the Gradle sync to finish.
3. Press **Run ▶** or use `Shift+F10`.

---

## How It Works

Aniyaa queries the nyaa.si **RSS feed** endpoint:

```
https://nyaa.si/?page=rss&q=<query>&c=<category>&f=<filter>&s=<sort>&o=<order>
```

The RSS feed returns standard `<item>` elements extended with `nyaa:` namespace fields (seeders, leechers, downloads, infoHash, size, trusted, remake). `NyaaRssParser` reads the stream with `XmlPullParser` and builds a list of `Torrent` data objects.

Magnet links are assembled from the `infoHash` field and a set of public trackers, so no additional torrent-client API is required.

---

## Search Parameters

| Parameter | Options |
|---|---|
| **Category** | All, Anime, Anime-AMV, Anime-English, Anime-Non-English, Anime-Raw, Audio, Literature, Live Action, Live Action-English, Live Action-Raw, Pictures, Software |
| **Filter** | No Filter · No Remakes · Trusted Only |
| **Sort by** | Date · Seeders · Leechers · Size · Downloads · Comments |
| **Order** | Descending · Ascending |

---

## Permissions

| Permission | Reason |
|---|---|
| `INTERNET` | Fetch search results from nyaa.si |

No other permissions are requested.

---

## Contributing

1. Fork the repository and create a feature branch.
2. Make your changes — keep PRs focused and small.
3. Open a pull request against `main` with a clear description.

---

## License

This project is open source. See [LICENSE](LICENSE) for details.

---

> **Disclaimer:** Aniyaa is an unofficial third-party client. It is not affiliated with or endorsed by nyaa.si.
