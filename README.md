# YtdlClean — a clean YouTube / video downloader for Android

A modern, Material-3 Android app for downloading video and audio, built with
**Kotlin + Jetpack Compose** and powered by **yt-dlp** (via the
`youtubedl-android` library).

> ⚠️ **Legal & distribution notice**
> - **Google Play** prohibits apps that download YouTube content. This app is for
>   distribution via **GitHub Releases / F-Droid / sideloading** only — never the Play Store.
> - Use this for content you own or have permission to save (your own uploads,
>   Creative Commons, personal offline use). Respect YouTube's Terms of Service and
>   the copyright laws in your region. The authors take no responsibility for misuse.

---

## ✨ Features

- 🎬 **Video downloads** — pick resolution (up to source max), auto-merges DASH video + audio with ffmpeg.
- 🎵 **Audio extraction** — convert to MP3 (320 kbps), M4A, WAV, FLAC.
- 🔗 **Paste or Share-to-app** — paste a link or share from any app.
- 📊 **Format picker** — analyze a link, see available qualities, choose one.
- 🔔 **Foreground service** — downloads continue in the background with a live progress notification.
- 📁 **Public folders** — finished files are published to `Movies/YtdlClean` or `Music/YtdlClean` via MediaStore.
- 🎨 **Material 3 + dynamic color**, dark/light/system theme.
- ⚡ **Sequential queue** with cancel / retry / remove.

---

## 🧱 Tech stack & architecture

```
UI (Jetpack Compose, Material 3, Navigation-Compose)
        │  ViewModels (state via StateFlow)
        ▼
DownloadManager  ──►  DownloadRepository (StateFlow<List<DownloadTask>>)
   │  (orchestrator)        (single source of truth)
   ▼
VideoDownloader  ──►  YoutubedlAndroidDownloader  ──►  yt-dlp + ffmpeg (native)
        │
        ▼
DownloadService (foreground: keeps process alive + notification)
```

- **MVVM + unidirectional data flow**, manual DI (no Hilt — fast, simple builds).
- The `VideoDownloader` interface is the only backend-specific seam; swap it for
  NewPipe Extractor, Chaquopy, etc. without touching the UI or service.
- `DownloadTask` state lives in an in-memory `StateFlow`. To persist across
  reboots, back `DownloadRepository` with a **Room** database (see Roadmap).

### Project layout

```
app/src/main/java/app/ytdlclean/
├── MainActivity.kt, YtdlApp.kt
├── di/AppContainer.kt          # manual DI + yt-dlp init
├── domain/                     # DownloadTask, Format, VideoInfo, enums
├── data/
│   ├── downloader/             # VideoDownloader (interface) + yt-dlp impl
│   ├── DownloadManager.kt      # orchestration + queue
│   ├── DownloadRepository.kt
│   ├── OutputResolver.kt       # MediaStore publishing
│   └── Settings.kt
├── service/                    # DownloadService + Notifications
└── ui/
    ├── theme/  home/  downloads/  settings/  navigation/
```

---

## 🛠️ Build & run

> **No Android Studio needed!** This project is set up for **GitHub Actions CI**.
> Push to GitHub and the APK is built for you (see below). You only need a browser
> to download the finished APK.

### Option A — Build on GitHub (recommended for low-end PCs) 🚀

A workflow is included at `.github/workflows/build-apk.yml`. On every push it:

1. Installs JDK 17 + Gradle + the Android SDK on GitHub's runner,
2. Generates the Gradle wrapper (not committed, since it's a binary),
3. Builds the debug APK,
4. Uploads it as a downloadable artifact.

**To use it:**
1. Create a new (public or private) repo on GitHub and push this project:
   ```bash
   cd YtdlClean
   git init && git add -A && git commit -m "Initial commit"
   git remote add origin https://github.com/<you>/<repo>.git
   git push -u origin main
   ```
2. Open the repo → **Actions** tab → click the latest **"Build APK"** run → scroll to **Artifacts** → download `YtdlClean-debug-apk.zip`.
3. Unzip and install `YtdlClean-debug.apk` on your phone ("Install unknown apps" must be allowed for your browser/files app).

**To publish a versioned release** (creates a GitHub Release with the APK):
```bash
git tag v1.0
git push --tags
```

> 💡 The debug APK is auto-signed with a debug key, so it installs immediately.
> The build takes ~5–10 min the first time (downloads the yt-dlp + ffmpeg native libs).

### Option B — Build locally with Android Studio

If you do have a dev machine:

1. **Open** the `YtdlClean` folder in Android Studio Iguana+ (JDK 17).
2. **Run** `app` on a device/emulator (API 24+).

> The APK is large because it bundles the yt-dlp Python runtime and ffmpeg for
> two ARM ABIs. For per-device splits, enable `splits { abi { ... } }` in
> `app/build.gradle.kts`.

---

## 🔧 How the yt-dlp backend works

`YoutubedlAndroidDownloader` talks to yt-dlp through `youtubedl-android`:

- **Analyze** → runs `yt-dlp -j <url>` (`--dump-json`) and parses the JSON with
  `org.json` (stable across library versions) to extract title, thumbnail,
  duration, and every available format.
- **Download (video)** → `-o <dir>/%(title).100B.%(ext)s -f <id>+bestaudio/best
  --merge-output-format mp4`. For video-only DASH streams, best audio is muxed in.
- **Download (audio)** → `-x --audio-format mp3 --audio-quality 0`.
- **Progress** → the library's `(progress, eta, line)` callback is mapped to
  `DownloadProgress`; speed is parsed from yt-dlp's console line.

### ⚠️ Verify the library's `execute(...)` signature
This scaffold uses the current Kotlin API:

```kotlin
YoutubeDL.getInstance().execute(request, processId) { progress, eta, line -> /* ... */ }
```

Confirmed against the library source (`YoutubeDL.kt` on `master`): the signature is
`execute(request, processId = null, callback: ((Float, Long, String) -> Unit)?)`,
where the callback receives `(progress, etaInSeconds, consoleLine)`. If you pin an
older version of the library, adjust the call in `YoutubedlAndroidDownloader.download(...)`.

---

## 🗺️ Roadmap (easy wins)

- [ ] **Room** persistence for `DownloadTask` (survive reboots).
- [ ] **Parallel downloads** (currently sequential) — a semaphore in `DownloadManager`.
- [ ] **Subtitles** (`--write-subs --sub-langs`).
- [ ] **Playlist download** (analyze supports it; the UI can expose batch enqueue).
- [ ] **Scheduled / metered-network guards** (`WorkManager`).
- [ ] **yt-dlp self-update** (`YoutubeDL.getInstance().updateYoutubeDL(...)`).

---

## 🧯 Troubleshooting

| Symptom | Fix |
|---|---|
| Build fails resolving `youtubedl-android` | Check the version tag on JitPack; ensure `jitpack.io` repo is in `settings.gradle.kts`. |
| `failed to initialize youtubedl-android` | First launch unpacks native libs; retry. Ensure 64-bit ABI device. |
| Download finishes but "file could not be located" | Output resolution falls back to newest file in the scratch dir; check logcat. |
| `configuration-cache` errors | Set `org.gradle.configuration-cache=false` in `gradle.properties`. |

---

*Use responsibly. Happy downloading.* 🚀
