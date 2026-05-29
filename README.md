# Musix

Personal Android music streaming app powered by YouTube (NewPipeExtractor). Distributed as a sideloaded APK — no Play Store.

**Current version: v0.6.4** — Paginated search / Artist & Album pages / Shuffle / Repeat / Queue / Synced lyrics / Top 50 Suggestions / Settings

---

## Features

### Playback
- YouTube audio streaming (audio-only, no video)
- **Background playback** with a persistent control notification (Media3 / MediaSession)
- **Mini-player** anchored at the bottom of the screen (animated slide-in)
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
- Per-track options: favorites, **add to queue**, add to playlist
- Tap an artist name → Artist page

### Artist & Album Pages
- 15–20 top tracks for the artist
- Albums with artwork, name, and track count
- Tap an album → full track listing (YouTube Music)

### Library (Room / SQLite)
- **Favorites**: add/remove from any screen
- **Listening history**
- **Playlists**: create, rename, delete, add/remove tracks

### Home
- "Continue Listening" carousel (recent history)
- "Your Favorites" carousel
- "YouTube Trending" carousel (with loading skeletons)

### Spotify Import
- OAuth2 PKCE connection
- Import Spotify playlists → Musix playlists (automatic YouTube matching)
- GDPR JSON export support (Streaming history, YourLibrary, Playlist files)

### Player Screen
- **Color gradient** extracted from the artwork via Palette (animated transition)
- Tap the artist name → Artist page
- "Queue" bottom sheet: view and remove tracks
- "Lyrics" bottom sheet: synced or plain-text lyrics

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
- Android SDK API 26+ (downloaded automatically on first Gradle sync)

### Running the project
1. *File* → *Open* → select the `Musix` folder
2. Wait for the **Gradle sync** (5–15 min on first run)
3. Select your device or AVD in the toolbar
4. ▶ *Run 'app'* (`Shift+F10`)

### Troubleshooting
- **Gradle sync fails**: *File* → *Invalidate Caches* → *Invalidate and Restart*
- **NewPipeExtractor not found**: check `maven { url = uri("https://jitpack.io") }` in `settings.gradle.kts`
- **Device not detected**: enable *USB debugging* in Developer Options; try a different cable if needed

---

## Architecture

```
com.louis.musix
├── MusixApp.kt                 Application: Koin + NewPipe initialisation
├── MainActivity.kt             Compose host: Scaffold + NavHost + BottomBar
├── di/AppModule.kt             Koin module (all singletons and ViewModels)
│
├── data/
│   ├── newpipe/                YouTubeRepository (search, audio URL, trending, artist, albums)
│   ├── lyrics/                 LyricsRepository (LRCLIB — synced + plain lyrics)
│   ├── local/                  Room: MusixDatabase, DAOs (song, favorite, history, playlist)
│   ├── repo/                   LibraryRepository (favorites, history, playlists)
│   ├── spotify/                SpotifyAuthManager + SpotifyRepository (OAuth2 PKCE)
│   └── SelectedSongHolder.kt   Song handoff to PlayerScreen (navigation)
│
├── domain/model/               Song, LyricLine, ArtistAlbum, Playlist, …
│
├── player/
│   ├── MusixPlayerService.kt   MediaSessionService (background playback + notification)
│   ├── PlayerController.kt     Queue, shuffle, repeat, auto-advance
│   └── RepeatMode.kt           Enum: OFF / ALL / ONE
│
└── ui/
    ├── theme/                  Color palette + forced dark theme
    ├── navigation/             Routes, NavHost, BottomBar
    ├── components/             SongRow, SongCard, ShimmerBox, MiniPlayer, …
    └── screens/
        ├── home/               HomeScreen + HomeViewModel
        ├── search/             SearchScreen + SearchViewModel
        ├── player/             PlayerScreen + PlayerViewModel
        ├── library/            LibraryScreen + LibraryViewModel
        ├── artist/             ArtistScreen, AlbumDetailScreen + ViewModels
        ├── playlist/           PlaylistDetailScreen + ViewModel
        └── spotify/            SpotifyImportScreen + ViewModel
```

---

## Roadmap

- ✅ **Phase 1** — Navigable skeleton (dark theme, navigation, BottomBar)
- ✅ **Phase 2** — YouTube search (NewPipeExtractor)
- ✅ **Phase 3** — Audio playback (PlayerScreen, SelectedSongHolder)
- ✅ **Phase 4** — Background service + notification + MiniPlayer (Media3)
- ✅ **Phase 5** — Room library (favorites, history, playlists)
- ✅ **Phase 6** — HomeScreen, skeletons, artwork gradient, animated MiniPlayer
- ✅ **Phase 7** — Spotify import (OAuth2 PKCE, YouTube matching)
- ✅ **Phase 8** — Auto-queue, auto-advance, Artist & Album pages
- ✅ **Phase 9** — Shuffle, Repeat, editable queue, synced lyrics (LRCLIB)
- ✅ **Phase 10** — Paginated search ("Load more"), Search history, clear button
- ✅ **Phase 11** — Full English translation

---

## License

MIT — personal use only. NewPipeExtractor is subject to its own GPL-3.0 license.
