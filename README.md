# Musix

Ce readme n'est plus à jour, merci de ne pas en tenir compte

Application Android de streaming musical via YouTube (NewPipeExtractor). Usage personnel, sideload APK.

## État actuel : Phase 1 — squelette navigable

Ce que tu obtiens en lançant l'app maintenant :
- Écran d'accueil sombre type Spotify
- BottomNavigation avec 3 onglets (Accueil / Recherche / Bibliothèque)
- Navigation fonctionnelle entre 3 écrans placeholders

Les phases suivantes ajouteront la recherche YouTube, la lecture audio, le service background, et les playlists Room.

## Mise en route (débutant complet)

### 1. Installer Android Studio
- Télécharge **Android Studio** (canal stable) : https://developer.android.com/studio
- Lance l'installeur, accepte les options par défaut.
- Au premier lancement, accepte le téléchargement des SDK Android (compte ~5 Go).

### 2. Activer le débogage USB sur ton téléphone (recommandé)
- *Réglages* → *À propos du téléphone* → tape 7 fois sur **Numéro de build** (active les options dev).
- *Options développeur* → activer **Débogage USB**.
- Branche le téléphone en USB, accepte la clé RSA sur le téléphone.

> Alternative : créer un appareil virtuel (AVD) dans Android Studio (*Tools* → *Device Manager*). Lent au démarrage mais marche sans téléphone.

### 3. Ouvrir le projet
- Lance Android Studio → *File* → *Open* → sélectionne le dossier `C:\Users\louis\Desktop\projets\Musix`.
- Android Studio détecte le projet Gradle et lance un **sync** automatique (télécharge toutes les dépendances : Compose, Media3, NewPipeExtractor, etc.). **Compte 5-15 minutes au premier sync**.
- Si une popup propose "Update Gradle Plugin", refuse pour l'instant (les versions sont déjà alignées dans le projet).

### 4. Lancer l'app
- En haut, sélectionne ton téléphone (ou l'AVD) dans le menu déroulant à côté du bouton ▶.
- Clique sur ▶ (*Run 'app'*) ou `Shift+F10`.
- Au premier lancement, Gradle compile l'APK (1-3 min), puis l'installe et l'ouvre automatiquement.

### Tu dois voir
- Une appli avec fond noir et le titre "Accueil" centré.
- En bas, 3 onglets : Accueil / Recherche / Bibliothèque.
- Tapper sur chaque onglet change l'écran (chaque écran affiche son nom + une note "bientôt").

## Architecture (résumé)

```
com.louis.musix
├── MusixApp.kt            Application : init Koin (et plus tard NewPipe)
├── MainActivity.kt        Hôte Compose : Scaffold + NavHost + BottomBar
├── di/AppModule.kt        Module Koin (sera rempli aux prochaines phases)
├── ui/
│   ├── theme/             Palette + thème sombre forcé
│   ├── navigation/        Routes, NavHost, BottomBar
│   ├── components/        Composables réutilisables
│   └── screens/{home,search,library}/  Écrans placeholders
└── (à venir)
    ├── data/newpipe/      Phase 2 : YouTubeRepository
    ├── player/            Phase 4 : MediaSessionService + PlayerController
    └── data/local/        Phase 5 : Room (playlists, favoris, historique)
```

## Roadmap

- ✅ **Phase 1** : Squelette navigable (actuel)
- ⏳ **Phase 2** : Recherche YouTube via NewPipeExtractor
- ⏳ **Phase 3** : Lecture audio (player simple)
- ⏳ **Phase 4** : Service background + notification + mini-player
- ⏳ **Phase 5** : Playlists, favoris, historique (Room)
- ⏳ **Phase 6** : Accueil/découverte + polish UI

Détails complets dans le plan : `C:\Users\louis\.claude\plans\j-ai-besoin-que-tu-radiant-orbit.md`

## Si quelque chose casse

- **Sync Gradle échoue** : *File* → *Invalidate Caches* → *Invalidate and Restart*.
- **NewPipeExtractor pas trouvé** : vérifier que `maven { url = uri("https://jitpack.io") }` est bien dans `settings.gradle.kts`.
- **AVD ne démarre pas** : essayer une image système plus légère (API 34, sans Google Services).
- **Téléphone non détecté** : changer de câble USB (certains câbles ne sont que pour la charge), réactiver le débogage USB.

## Pour la suite

Dis-moi quand l'app affiche les 3 onglets sur ton téléphone — on enchaîne sur la Phase 2 (recherche YouTube).
