# Musix

Personal Android music streaming app powered by YouTube (NewPipeExtractor). Distributed as a sideloaded APK - no Play Store.

**Current version: v1.1.0** - « Sillon » redesign: vinyl look, round shapes, oxblood / cream / mustard palette, new app icon

---

## Features

### Playback
- YouTube audio streaming (audio-only, no video)
- **Background playback** with a persistent control notification (Media3 / MediaSession)
- **Mini-player**: floating cream pill above the tab bar, with a spinning record and a progress ring
- **Auto-queue**: after a track ends, similar songs by the same artist are loaded automatically
- **Shuffle**: randomises the queue while keeping the current track at the front
- **Repeat**: three modes (Off → All → One)
- Controls: play/pause, next, previous (< 3 s → restart, otherwise previous track)

### Lyrics
- Automatic retrieval via [LRCLIB](https://lrclib.net) (no API key required)
- **Synced lyrics**: real-time auto-scroll to the current line
- Plain-text fallback when no timestamps are available
- Instrumental track detection

### Search
- YouTube search via NewPipeExtractor
- **Paginated results**: "Load more" button for endless scrolling
- **Recent searches** shown as tappable pills
- Per-track options: favorites, **add to queue**, add to playlist
- Tap an artist name → Artist page

### Artist & Album Pages
- 15–20 top tracks for the artist
- Albums with artwork, name, and track count
- Tap an album → full track listing (YouTube Music)

### Library (Room / SQLite)
- **Favorites**: add/remove from any screen
- **Listening history**: with duplicate filtering
- **Playlists**: record grid; create, rename, delete (« ⋯ » menu), add/remove tracks, **drag-to-reorder**, Play all / Shuffle
- **Search**: filter favorites and history by title or artist
- **Downloads**: full offline support with swipe-to-delete

### Design - « Sillon » (v1.1.0)
- Vinyl look: every artwork becomes the label of a record that **spins while playing**
- Everything round: pill rows, pill tabs, round controls, rounded cards and sheets
- Oxblood / cream / mustard palette, **Fraunces** (titles) + **Figtree** (body)
- Adaptive **Vinyle** app icon with a monochrome layer for Android 13+ themed icons

### Home
- "Jump back in" card (last played track)
- "Recently played" and "Favorites" record shelves
- **Top 50 Suggestions**: smart recommendations based on your Spotify all-time top tracks

### Spotify Import
- **GDPR JSON export** (recommended): Streaming history → Top 50 all time + Top 20 by year, YourLibrary → liked tracks, Playlist files → playlists
- **Account connection** (OAuth2 PKCE): requires a Spotify client ID (see *Development setup*) and a Premium developer account
- Imported tracks are matched automatically on YouTube

### Offline & cache
- Offline banner when the network drops; downloaded music stays playable
- Stream cache (ExoPlayer SimpleCache, LRU eviction, 300 MB max)

### Settings
- Spotify status, import and disconnect
- Stream cache size and « Clear cache »
- App version and open-source licenses

### Player Screen
- **Spinning record** inside a progress ring, round prev / play / next controls
- Chip row: Shuffle, Repeat, Like, Download, Sleep (with live countdown)
- **Lyrics preview card** showing the previous / current / next synced lines
- **Sleep timer**: 15/30/45/60 min or end-of-track, with a live countdown
- Tap the artist name → Artist page
- "Queue" bottom sheet: view and remove tracks
- "Lyrics" bottom sheet: synced or plain-text lyrics
- Real system **audio focus**: pauses when a call or another app takes over

---

## Installation (sideload APK)

1. Download the latest APK from [GitHub Releases](../../releases)
2. On Android: *Settings* → *Apps* → *Install unknown apps* → allow your browser or file manager
3. Open the downloaded APK and confirm the installation
4. Launch **Musix**

---

## Development setup

### Prerequisites
- **Android Studio** (stable channel): https://developer.android.com/studio
- JDK 17 (bundled with Android Studio)
- Android SDK: compile/target SDK 35, min SDK 26 (downloaded automatically on first Gradle sync)

### Local configuration
Copy `local.properties.example` → `local.properties` (git-ignored) and set your Spotify client ID
(from https://developer.spotify.com/dashboard):

```
spotify.client.id=YOUR_CLIENT_ID
```

Without it the app still builds, but Spotify account connection is unavailable (the JSON export
import still works). A `-Pspotify.client.id=…` Gradle argument overrides the file.

### Running the project
1. *File* → *Open* → select the `Musix` folder
2. Wait for the **Gradle sync** (5–15 min on first run)
3. Select your device or AVD in the toolbar
4. ▶ *Run 'app'* (`Shift+F10`)

### Tests
```
./gradlew :app:testDebugUnitTest
```
Unit tests cover the LRC lyrics parser and YouTube URL handling.

### Troubleshooting
- **Gradle sync fails**: *File* → *Invalidate Caches* → *Invalidate and Restart*
- **NewPipeExtractor not found**: check `maven { url = uri("https://jitpack.io") }` in `settings.gradle.kts`
- **Device not detected**: enable *USB debugging* in Developer Options; try a different cable if needed

### Signed release build
The release build is signed with a dedicated keystore. Secrets live in `keystore.properties`
(git-ignored). To set it up once:

1. Copy `keystore.properties.template` → `keystore.properties`.
2. Generate the keystore from the repo root:
   ```
   keytool -genkeypair -v -keystore musix-release.jks \
     -keyalg RSA -keysize 2048 -validity 10000 -alias musix
   ```
3. Fill in the passwords in `keystore.properties`.
4. Build: *Build* → *Generate Signed Bundle / APK*, or assemble the `release` variant.

If `keystore.properties` is absent, the release build falls back to the debug key so the
project still compiles on a fresh clone. The debug key differs from one machine to another,
and Android refuses to update an app signed with a different key: always sign published
releases with the same release keystore so users can update without reinstalling.

---

## Architecture

```
com.louis.musix
├── MusixApp.kt                 Application: Koin + NewPipe initialisation
├── MainActivity.kt             Compose host: Scaffold + NavHost + MiniPlayer + floating tab bar
├── di/AppModule.kt             Koin module (all singletons and ViewModels)
│
├── data/
│   ├── newpipe/                YouTubeRepository (search, audio URL, artist, albums) + NewPipeDownloader
│   ├── lyrics/                 LyricsRepository (LRCLIB - synced + plain lyrics)
│   ├── local/                  Room: MusixDatabase, dao/ + entity/ (song, favorite, history, playlist)
│   ├── download/               DownloadManager (Background downloading + file management)
│   ├── repo/                   LibraryRepository (favorites, history, playlists, downloads)
│   ├── spotify/                SpotifyAuthManager + SpotifyRepository (OAuth2 PKCE)
│   └── SelectedSongHolder.kt   Song handoff to PlayerScreen (navigation)
│
├── domain/
│   ├── model/                  Song, LyricLine, ArtistAlbum, Playlist
│   └── util/                   NetworkMonitor (online / offline state)
│
├── player/
│   ├── MusixPlayerService.kt   MediaSessionService (background playback + notification)
│   ├── PlayerController.kt     Queue, shuffle, repeat, auto-advance, sleep timer
│   ├── RepeatMode.kt           Enum: OFF / ALL / ONE
│   └── cache/CacheManager.kt   ExoPlayer SimpleCache (300 MB, LRU)
│
└── ui/
    ├── theme/                  Sillon palette, Fraunces/Figtree type, round shapes (forced dark)
    ├── navigation/             Routes, NavHost, MusixBottomBar
    ├── components/             VinylDisc, SongRow, MiniPlayer, pills, sheets, …
    └── screens/
        ├── home/               HomeScreen + HomeViewModel
        ├── search/             SearchScreen + SearchViewModel
        ├── player/             PlayerScreen + PlayerViewModel
        ├── library/            LibraryScreen + LibraryViewModel
        ├── artist/             ArtistScreen, AlbumDetailScreen + ViewModels
        ├── playlist/           PlaylistDetailScreen + ViewModel
        ├── settings/           SettingsScreen + ViewModel, LicensesScreen
        └── spotify/            SpotifyImportScreen + ViewModel
```

---

## Roadmap

- ✅ **Phase 1** - Navigable skeleton (dark theme, navigation, BottomBar)
- ✅ **Phase 2** - YouTube search (NewPipeExtractor)
- ✅ **Phase 3** - Audio playback (PlayerScreen, SelectedSongHolder)
- ✅ **Phase 4** - Background service + notification + MiniPlayer (Media3)
- ✅ **Phase 5** - Room library (favorites, history, playlists)
- ✅ **Phase 6** - HomeScreen, skeletons, artwork gradient, animated MiniPlayer
- ✅ **Phase 7** - Spotify import (OAuth2 PKCE, YouTube matching)
- ✅ **Phase 8** - Auto-queue, auto-advance, Artist & Album pages
- ✅ **Phase 9** - Shuffle, Repeat, editable queue, synced lyrics (LRCLIB)
- ✅ **Phase 10** - Paginated search ("Load more"), Search history, clear button
- ✅ **Phase 12** - Offline Mode (v0.9.1): Download tracks, Local playback, Storage management
- ✅ **Phase 13** - Data Saving (v0.9.2): ExoPlayer SimpleCache integration, Auto-eviction (300 MB max)
- ✅ **Phase 14** - Robustness: Network monitoring, Auto-skip on stream error
- ✅ **Phase 15 (v1.0.0)** - Sleep timer, Playlist rename & drag-to-reorder, Library search, real system audio focus, cleaned-up Settings, signed release build, unit tests
- 🚀 **v1.0.0** - Official Release
- ✅ **v1.1.0** - « Sillon » redesign of every screen + new « Vinyle » app icon

### Post-1.0 ideas
- 🔄 Android Auto support
- 🔄 Equalizer
- 🔄 Material You dynamic colors & shared-element transitions

---

## License

MIT - personal use only. NewPipeExtractor is subject to its own GPL-3.0 license.
