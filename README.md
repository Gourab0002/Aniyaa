<div align="center">

<img src="app/src/main/res/drawable/ic_launcher_image.png" width="96" alt="Aniyaa logo" />

# Aniyaa

Search [nyaa.si](https://nyaa.si) and [sukebei.nyaa.si](https://sukebei.nyaa.si) from your Android phone.

[Download the latest version](https://github.com/Gourab0002/Aniyaa/releases/latest) · Android 7.0 or newer

</div>

---

## What is Aniyaa?

Aniyaa is a simple Android app for browsing [nyaa.si](https://nyaa.si) and [sukebei.nyaa.si](https://sukebei.nyaa.si) without using a web browser. You can search, filter results, save favorites, and send a download to the torrent app you already use.

Nyaa is the default catalog (anime, manga, music, and more). Sukebei is adult content and stays off until you opt in.

It is **not** an official nyaa.si or sukebei.nyaa.si app, and it is **not** a torrent downloader. Aniyaa finds listings; your torrent app (such as LibreTorrent, Flud, or qBittorrent) does the actual downloading.

---

## Download and install

1. On your phone, open the **[latest release](https://github.com/Gourab0002/Aniyaa/releases/latest)**.
2. Tap **Aniyaa-v…apk** to download it.
3. Open the downloaded file. If Android warns that the app isn’t from the Play Store, choose **Install anyway**. You may need to allow your browser (or Files) to install apps — Android will show a switch for that.
4. Open **Aniyaa** from your app drawer.

To update later, download a newer APK from the same [Releases](https://github.com/Gourab0002/Aniyaa/releases) page and install it over the old one. You won’t lose bookmarks.

If you previously used **Sukiniyaa**, install Aniyaa separately and export/import a backup from Settings if you want to keep saved data. The two apps use different package names, so they cannot update in place.

---

## How to use it

**Catalogs**  
Use the **Nyaa** / **Sukebei 18+** chips under the search bar. The first time you open Sukebei, Aniyaa asks you to confirm you are 18 or older. Bookmarks, history, saved searches, and filters are kept per catalog.

**Search**  
Type in the search bar at the top and press search. More results load as you scroll.

**Filters**  
Tap the filter button next to the search bar to narrow by category, hide remakes, show trusted uploads only, or change how results are sorted. Categories change with the selected catalog.

**Open a listing**  
Tap a result to see the description, file list, and comments. From there you can:

- **Magnet** — start the download in your torrent app
- **Copy magnet** — copy the link to paste elsewhere
- **Download** — get the `.torrent` file
- **Share** — send the listing to someone else
- **View on Nyaa / Sukebei** — open the page in your browser

Bookmark a listing with the star in the top corner so you can find it later on the **Bookmarks** tab. Swipe a bookmark left to remove it.

**History**  
Your recent searches appear on the **History** tab. Tap one to search it again, or swipe to delete it.

**Saved searches**  
From Search, save the current query and filters. Saved searches live on the History tab and can notify you of new results.

**Look and feel**  
Open **Settings** to pick a color theme, light/dark mode, a default category and sort, and a preferred torrent app.

**Privacy**  
Turn on app lock (PIN or biometrics), hide screenshots, or hide the app from recents.

---

## What you need

- An Android phone or tablet running **Android 7.0** or later
- An internet connection
- A torrent app if you want to download files (Aniyaa only hands off the link)

Bookmarks, search history, and saved searches stay on your device. You can export or import them from Settings. Aniyaa does not create an account.

If nyaa.si or sukebei.nyaa.si is blocked, set a mirror URL for that catalog in Settings.

---

## License and disclaimer

Aniyaa is free and open source. See [LICENSE](LICENSE) for details.

Aniyaa is an unofficial project and is not affiliated with or endorsed by nyaa.si or sukebei.nyaa.si. Use it in line with the laws where you live.

---

<details>
<summary>For people who want to build the app from source</summary>

You will need JDK 17 and the Android SDK. Then:

```bash
git clone https://github.com/Gourab0002/Aniyaa.git
cd Aniyaa
./gradlew assembleDebug
```

Signed releases are published automatically when a `v*.*.*` tag is pushed. Local signed builds use a gitignored `keystore.properties` file; see `keystore.properties.example`.

</details>
