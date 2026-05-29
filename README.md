# Musix

Ce readme n'est plus à jour, merci de ne pas en tenir compte

Application Android de streaming musical via YouTube (NewPipeExtractor). Usage personnel, sideload APK.

**Version actuelle : v0.6.0** — Shuffle / Repeat / File d'attente / Paroles synchronisées

---

## Fonctionnalités

### Lecture
- Streaming audio YouTube (pas de vidéo, juste l'audio)
- Lecture en **arrière-plan** avec notification de contrôle (Media3 / MediaSession)
- **Mini-player** persistant en bas de l'écran (slide animé à l'apparition)
- **File d'attente automatique** : après un morceau, les titres similaires de l'artiste sont chargés
- **Shuffle** : mélange la file d'attente, chanson courante conservée en tête
- **Repeat** : trois modes (OFF → Tout → Un seul)
- Contrôles : lecture/pause, suivant, précédent (< 3s → restart, sinon morceau précédent)

### Paroles
- Récupération automatique via [LRCLIB](https://lrclib.net) (sans clé API)
- **Paroles synchronisées** : auto-scroll sur la ligne courante en temps réel
- Fallback texte brut si pas de timestamps disponibles
- Détection morceaux instrumentaux

### Recherche
- Recherche YouTube via NewPipeExtractor
- Options par morceau : favoris, **ajouter à la file d'attente**, ajouter à une playlist
- Clic sur un artiste → page artiste

### Pages Artiste / Album
- 15–20 titres populaires de l'artiste
- Albums avec artwork, nom et nombre de pistes
- Clic sur un album → liste des pistes de l'album (YouTube Music)

### Bibliothèque (Room / SQLite)
- **Favoris** : ajouter/retirer depuis n'importe quel écran
- **Historique** d'écoute
- **Playlists** : créer, renommer, supprimer, ajouter/retirer des titres

### Accueil
- Carrousel "Continuer l'écoute" (historique récent)
- Carrousel "Tes favoris"
- Carrousel "Tendances YouTube" (avec skeletons de chargement)

### Import Spotify
- Connexion OAuth2 PKCE
- Import des playlists Spotify → playlists Musix (matching YouTube automatique)

### Player Screen
- **Gradient de couleur** extrait de l'artwork via Palette (transition animée)
- Tap sur l'artiste → page artiste
- Sheet "File d'attente" : visualiser et retirer des titres
- Sheet "Paroles" : paroles synchronisées ou texte brut

---

## Installation (sideload APK)

1. Télécharge le dernier APK depuis les [Releases GitHub](../../releases)
2. Sur Android : *Réglages* → *Applications* → *Installer des applis inconnues* → autorise ton navigateur/gestionnaire de fichiers
3. Ouvre l'APK téléchargé, accepte l'installation
4. Lance **Musix**

---

## Mise en route (développement)

### Prérequis
- **Android Studio** (canal stable) : https://developer.android.com/studio
- SDK Android API 26+ (téléchargé automatiquement au premier sync Gradle)

### Lancer le projet
1. *File* → *Open* → sélectionne le dossier `Musix`
2. Attends le **Gradle sync** (5–15 min au premier lancement)
3. Sélectionne ton téléphone ou un AVD dans la barre d'outils
4. ▶ *Run 'app'* (`Shift+F10`)

### Dépannage
- **Sync Gradle échoue** : *File* → *Invalidate Caches* → *Invalidate and Restart*
- **NewPipeExtractor introuvable** : vérifier `maven { url = uri("https://jitpack.io") }` dans `settings.gradle.kts`
- **Téléphone non détecté** : activer *Débogage USB* dans les Options développeur, changer de câble si besoin

---

## Architecture

```
com.louis.musix
├── MusixApp.kt                 Application : init Koin + NewPipe
├── MainActivity.kt             Hôte Compose : Scaffold + NavHost + BottomBar
├── di/AppModule.kt             Module Koin (tous les singletons et ViewModels)
│
├── data/
│   ├── newpipe/                YouTubeRepository (search, audio URL, trending, artiste, albums)
│   ├── lyrics/                 LyricsRepository (LRCLIB — paroles sync + plain)
│   ├── local/                  Room : MusixDatabase, DAOs (song, favorite, history, playlist)
│   ├── repo/                   LibraryRepository (favoris, historique, playlists)
│   ├── spotify/                SpotifyAuthManager + SpotifyRepository (OAuth2 PKCE)
│   └── SelectedSongHolder.kt   Passeur de chanson vers le PlayerScreen (nav)
│
├── domain/model/               Song, LyricLine, ArtistAlbum, Playlist, ...
│
├── player/
│   ├── MusixPlayerService.kt   MediaSessionService (lecture background + notification)
│   ├── PlayerController.kt     File d'attente, shuffle, repeat, auto-advance
│   └── RepeatMode.kt           Enum OFF / ALL / ONE
│
└── ui/
    ├── theme/                  Palette + thème sombre forcé
    ├── navigation/             Routes, NavHost, BottomBar
    ├── components/             SongRow, SongCard, ShimmerBox, MiniPlayer, ...
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

- ✅ **Phase 1** — Squelette navigable (thème sombre, navigation, BottomBar)
- ✅ **Phase 2** — Recherche YouTube (NewPipeExtractor)
- ✅ **Phase 3** — Lecture audio (PlayerScreen, SelectedSongHolder)
- ✅ **Phase 4** — Service background + notification + MiniPlayer (Media3)
- ✅ **Phase 5** — Bibliothèque Room (favoris, historique, playlists)
- ✅ **Phase 6** — HomeScreen, skeletons, gradient artwork, AnimatedVisibility MiniPlayer
- ✅ **Phase 7** — Import Spotify (OAuth2 PKCE, matching YouTube)
- ✅ **Phase 8** — File d'attente auto, avance automatique, pages Artiste + Album
- ✅ **Phase 9** — Shuffle, Repeat, file d'attente éditable, paroles synchronisées (LRCLIB)

---

## Licence

MIT — usage personnel uniquement. NewPipeExtractor est soumis à sa propre licence GPL-3.0.
