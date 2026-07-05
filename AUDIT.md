# Audit du projet Musix

**Date** : juillet 2026 — **Version auditée** : v1.0.0 (commit `149d2b5`)
**Périmètre** : architecture, qualité de code, données, sécurité, build, dépendances, tests, fonctionnalités.
**Contexte retenu** : app personnelle partagée avec des proches via APK sideloadé ; UI en anglais assumée.

---

## Synthèse

Le projet est **sain dans son ensemble** : MVVM propre et cohérent (~7 200 LOC, 60 fichiers Kotlin), séparation `domain / data / ui` respectée, DI Koin bien utilisée, lecture audio correctement portée par un `MediaSessionService`. Les problèmes relevés sont concentrés sur **trois bugs/risques critiques** (corrigés par ce commit), une **dépendance opérationnelle fragile** (NewPipeExtractor), et de la dette classique (duplication, gros fichiers, absence de CI).

| Sévérité | Constat | Statut |
|---|---|---|
| 🔴 Critique | « Clear cache » casse le cache de streaming jusqu'au redémarrage | ✅ Corrigé |
| 🔴 Critique | Migrations Room destructives : toutes les données utilisateur effacées à chaque évolution du schéma | ✅ Corrigé |
| 🔴 Critique | Tokens Spotify en clair inclus dans les backups cloud/adb | ✅ Corrigé |
| 🟠 Important | NewPipeExtractor v0.26.2 (~18 mois de retard) — risque n°1 de panne | À faire |
| 🟠 Important | Gradle wrapper absent du repo (clone frais impossible à builder) | ✅ Corrigé |
| 🟠 Important | Téléchargements fragiles (annulables, sans progression, sans quota) | À faire |
| 🟠 Important | Aucune CI, couverture de tests minimale | À faire |

---

## Points forts (à préserver)

- **Architecture MVVM cohérente** : 9 ViewModels avec `StateFlow<UiState>` + `collectAsStateWithLifecycle()`, repositories dédiés, UI qui ne touche jamais les DAOs.
- **Lecture audio bien conçue** : `MediaSessionService` + ExoPlayer, cache de streaming LRU plafonné à 300 Mo, audio focus réel.
- **OAuth Spotify PKCE correct** (`SpotifyAuthManager.kt`) : challenge S256, pas de client_secret, marge de refresh de 60 s. Le `CLIENT_ID` en dur est acceptable pour un client public PKCE.
- **Gestion du keystore exemplaire** : `keystore.properties` git-ignoré, template avec placeholders, fallback debug pour les clones frais. Aucun secret tracké (vérifié sur `git ls-files`).
- **Hygiène coroutines** : aucun `GlobalScope`/`runBlocking`/`Thread.sleep` ; tout l'I/O réseau dans `Dispatchers.IO` ; `CancellationException` correctement re-levée dans `SpotifyImportViewModel` (piège classique évité).
- **Permissions minimales et justifiées** ; stockage app-specific (pas de permission storage) ; pas de trafic cleartext.
- README détaillé et globalement fidèle au code.

---

## 🔴 Problèmes critiques — corrigés par ce commit

### 1. `CacheManager.clearCache()` cassait le cache jusqu'au redémarrage
`app/src/main/java/com/louis/musix/player/cache/CacheManager.kt`

`simpleCache` est un `val` initialisé à la construction. L'ancienne implémentation faisait `simpleCache.release()` + suppression du dossier, avec un commentaire prétendant que l'instance se réinitialiserait « en lazy » — faux. Après un « Clear Stream Cache » dans Settings, la `cacheDataSourceFactory` (déjà liée à l'instance par `MusixPlayerService`) pointait sur un cache *released* : caching mort et risque d'`IllegalStateException` jusqu'au kill du process.

**Correctif** : vidage du contenu via `simpleCache.keys → removeResource(key)` sans jamais release l'instance. La factory reste valide, la taille affichée retombe à 0.

### 2. Migrations Room destructives
`app/src/main/java/com/louis/musix/data/local/MusixDatabase.kt`

`version = 2` avec `fallbackToDestructiveMigration()`, `exportSchema = false`, aucune `Migration` : **chaque évolution du schéma effaçait favoris, historique, playlists et téléchargements**. Le passage v1→v2 (mode offline) a déjà détruit les données des installations existantes. Pour une app partagée avec des proches, c'est le risque de perte de données n°1.

**Correctif** :
- `MIGRATION_1_2` explicite (ajout de `isDownloaded INTEGER NOT NULL DEFAULT 0` et `localFilePath TEXT` sur `songs`), `.addMigrations()` remplace le fallback destructif ;
- `exportSchema = true` + `room.schemaLocation` configuré dans `app/build.gradle.kts` → les schémas versionnés sont committés sous `app/schemas/`, prérequis pour écrire/tester les futures migrations.

**Règle à suivre désormais** : à chaque bump de `version`, écrire la `Migration` correspondante — ne jamais réintroduire le fallback destructif.

### 3. Tokens Spotify exposés aux backups
`SpotifyAuthManager.kt` + `AndroidManifest.xml`

Les tokens (access + **refresh**) sont stockés en clair dans `spotify_prefs` (SharedPreferences `MODE_PRIVATE`). Combiné à `android:allowBackup="true"` sans aucune règle d'exclusion, le refresh token partait dans les backups Google cloud et `adb backup`.

**Correctif** : `res/xml/backup_rules.xml` (API ≤ 30) et `res/xml/data_extraction_rules.xml` (API 31+) excluent `spotify_prefs.xml` du backup cloud **et** du transfert appareil-à-appareil, référencés dans le manifest. La base Room (playlists/favoris) reste sauvegardée — voulu pour des utilisateurs proches.

*Amélioration possible plus tard : chiffrer les tokens (Android Keystore). Non bloquant une fois l'exclusion de backup en place.*

### 4. (Bonus) Gradle wrapper committé
Seul `gradle-wrapper.properties` était tracké — pas de `gradlew`, `gradlew.bat` ni `gradle-wrapper.jar` : un clone frais ne pouvait pas builder sans Android Studio, et aucune CI n'était possible. Le wrapper 8.10.2 complet est maintenant committé.

---

## 🟠 Problèmes importants — à traiter ensuite

### NewPipeExtractor v0.26.2 : le risque opérationnel n°1
`gradle/libs.versions.toml` épingle `v0.26.2` (fin 2024, ~18 mois de retard). NewPipeExtractor casse à chaque changement de l'API InnerTube de YouTube ; une version aussi ancienne signifie que recherche/lecture peuvent tomber en panne à tout moment (si ce n'est pas déjà le cas).
**Recommandation** : bump vers le dernier tag, tester la lecture, puis mettre en place Renovate/Dependabot pour être alerté des nouvelles versions. C'est LE point de maintenance récurrent de ce projet.

### Téléchargements fragiles
`data/download/DownloadManager.kt` :
- lancés depuis `PlayerViewModel.viewModelScope` → **quitter l'écran Player peut annuler un téléchargement en cours** ; rien ne survit au kill de l'app ;
- `downloadingIds` (StateFlow) exposé mais **jamais collecté par l'UI** : aucune progression visible, et un échec est simplement loggé — l'utilisateur ne voit rien ;
- aucun quota ni nettoyage du dossier `filesDir/downloads` (contrairement au cache de streaming, plafonné) ; fichiers orphelins possibles si une ligne `songs` disparaît.

**Recommandation** : migrer vers WorkManager (contrainte réseau, reprise, notification de progression), afficher l'état dans l'UI, ajouter un écran « stockage » (taille des téléchargements + purge).

### Aucune CI, tests quasi absents
- 13 tests JVM seulement (`YouTubeUrlTest`, `LrcParserTest`) — bien choisis (parsing fragile) mais rien sur ViewModels, repositories, DAOs, PlayerController.
- Aucun workflow GitHub Actions.

**Recommandation** : maintenant que le wrapper est committé, ajouter un workflow minimal (`./gradlew test lint assembleDebug`), puis des tests Room (migrations !) et PlayerController (la logique queue/shuffle/repeat est la plus à risque).

### Stack ~18 mois en retard (juillet 2026)
AGP 8.7.3, Kotlin 2.1.0, Compose BOM 2024.12.01, Media3 1.5.1, Room 2.6.1, Coil 2.7 (3.x dispo), navigation 2.8.5, coroutines 1.9.0. Rien de cassé, mais l'écart grandit et les migrations deviendront plus coûteuses. À faire par lots (AGP/Kotlin d'abord, puis Compose/Media3, puis le reste), avec test de lecture après chaque lot.

### Fuites de ressources mineures
- `domain/util/NetworkMonitor.kt` : `registerNetworkCallback` dans `init`, jamais unregister — pas de teardown possible.
- `player/PlayerController.kt` : le `MediaController` construit dans `ensureConnected()` n'est jamais release, le listener jamais retiré.
Singletons app-scoped (contexte application), donc pas de fuite d'Activity — mais un chemin de teardown propre serait sain.

### Divers
- `SettingsViewModel` lit `cacheSizeBytes` (I/O disque SQLite du cache) **sur le main thread** (`init` → `updateCacheSize`). À passer sur `Dispatchers.IO`.
- Table `songs` sans purge : `LibraryRepository.cacheSong()` upsert à chaque recherche/lecture/import, rien ne supprime jamais → croissance illimitée (lente, mais réelle).
- Service `MusixPlayerService` exporté sans permission de garde — acceptable pour Media3 (contrôleurs externes), mais à garder à l'esprit.

---

## 🟡 Qualité de code / nettoyage

### À découper
- `ui/screens/player/PlayerScreen.kt` (614 LOC) : artwork + Palette, 3 bottom sheets (queue/lyrics/sleep timer), slider, gradient dans un seul fichier → extraire des sous-composables.
- `player/PlayerController.kt` (445 LOC) : god-object (connexion MediaController + polling position + queue + shuffle + repeat + sleep timer + auto-advance). L'état de la queue vit dans des **champs mutables « shadow »** (`_queue`, `queueIdx`, …) recopiés manuellement dans le `StateFlow` via `pushQueueState()` : chaque mutation doit penser à l'appeler, sinon l'UI diverge silencieusement. C'est la zone la plus propice aux bugs du projet. Extraire un `QueueManager` et un `SleepTimer`.

### Duplication
- Résolution « fichier local vs URL de stream » dupliquée ×3 (`PlayerController.autoAdvanceTo`, `PlayerViewModel.loadAndPlay`, `loadAndPlayQueue`).
- « Auto-skip après 2 s en cas d'échec » dupliqué ×4 — et si toute une portion de queue échoue, les skips s'empilent et défilent la queue en rafale.
- **`OkHttpClient` instancié ×5** (`DownloadManager`, `SpotifyAuthManager`, `SpotifyRepository`, `LyricsRepository`, `NewPipeDownloader`) : aucun pool de connexions partagé. En injecter un seul via Koin. Idem pour `Json { ignoreUnknownKeys = true }`.
- `LibraryRepository.history` re-mappe un `Song` à la main au lieu du `toDomain()` utilisé partout ailleurs ; `YouTubeRepository.search()`/`searchPaged()` reconstruisent la même requête.

### Code mort / scories
- `ui/components/PlaceholderScreen.kt` (45 LOC) : jamais référencé → **supprimer**.
- `ArtistViewModel.kt:50-52` : `catch (e) { throw e }` no-op → supprimer.
- 2 catch vides : `PlayerViewModel.kt:169` (tolérable, auto-queue en arrière-plan) et `SpotifyImportScreen.kt:69` (avale les erreurs de lecture de fichier pendant l'import — à remonter à l'utilisateur).
- Incohérence FR/EN dans commentaires, logs et messages d'exception (`SpotifyAuthManager`, `SpotifyRepository`, `HistoryDao`, `SongEntity`, manifest) ; `SpotifyRepository.kt:73` lève un message en français qui remonte jusqu'à l'UI anglaise. À uniformiser en anglais.
- `data/SelectedSongHolder.kt` : singleton mutable pour passer les arguments de navigation — fonctionne, mais fragile en cas de process death (le morceau sélectionné est perdu). L'idiome : argument de navigation / `SavedStateHandle`.

### README à rafraîchir
- « Duplicate filtering » de l'historique n'existe qu'en UI (`distinctBy` dans `HomeViewModel`) — la DB garde chaque lecture.
- L'import Spotify crée aussi « Top 20 \<année\> » (×3) et « Spotify — Liked Tracks » : non documenté.
- Écrans Settings et Licenses absents de la liste des features ; Phase 11 manquante dans la roadmap.
- Le commentaire « for Premium accounts » sur l'import API (`SpotifyImportViewModel.kt`) est inexact : les scopes utilisés fonctionnent sur un compte gratuit.

### ⚖️ Licence
Le repo est sous **MIT**, mais NewPipeExtractor est **GPL-3.0** : l'APK distribué (même à des proches) est une œuvre dérivée qui devrait être GPL-compatible. Recommandation : passer le projet en GPL-3.0, ou a minima le documenter clairement.

---

## Choses à ajouter (priorisées pour l'usage réel)

1. **CI GitHub Actions** (build + tests + lint) — désormais possible grâce au wrapper committé.
2. **Renovate/Dependabot** — surtout pour NewPipeExtractor.
3. **Téléchargements via WorkManager** avec progression et gestion d'erreurs visibles.
4. **Export/import de la bibliothèque** (JSON des playlists/favoris) : vraie assurance-vie des données, complémentaire du backup Android.
5. **Bouton « Déconnecter Spotify » dans Settings** : `SettingsViewModel.disconnectSpotify()` existe déjà mais n'est jamais appelé par `SettingsScreen`.
6. **Fallback LRCLIB `/api/search`** quand `/api/get` (match exact artiste/titre/durée) échoue — augmenterait nettement le taux de paroles trouvées.
7. Plus tard, si l'envie : égaliseur, Android Auto (`MediaLibraryService`), widget, Material You, thème clair (actuellement forcé sombre).

## Choses à supprimer

- `ui/components/PlaceholderScreen.kt` (jamais utilisé).
- `catch (e) { throw e }` dans `ArtistViewModel`.
- `downloadingIds` dans `DownloadManager` **ou** le brancher à l'UI (état actuel : API publique morte).
- Le commentaire mensonger de `clearCache()` (fait dans ce commit).
- `buildConfig = true` dans `app/build.gradle.kts` si `BuildConfig` reste inutilisé (aucune référence trouvée).

---

## Plan d'action recommandé

| # | Action | Effort | Impact |
|---|---|---|---|
| 1 | ~~Fix `clearCache()`~~ | — | ✅ fait |
| 2 | ~~Migrations Room réelles + export schéma~~ | — | ✅ fait |
| 3 | ~~Exclure `spotify_prefs` des backups~~ | — | ✅ fait |
| 4 | ~~Committer le Gradle wrapper~~ | — | ✅ fait |
| 5 | Bump NewPipeExtractor + test lecture | S | 🔥 disponibilité de l'app |
| 6 | CI GitHub Actions (test + lint + assembleDebug) | S | Filet de sécurité permanent |
| 7 | Downloads → WorkManager + UI de progression | M | Fiabilité offline |
| 8 | Mutualiser `OkHttpClient`/`Json` via Koin | S | Perf + propreté |
| 9 | Dédupliquer résolution stream/local + auto-skip | S | Moins de bugs futurs |
| 10 | Mise à jour des dépendances par lots | M | Dette maîtrisée |
| 11 | Découpage `PlayerScreen` / `PlayerController` | M | Maintenabilité |
| 12 | Tests migrations Room + PlayerController | M | Protège les données et la lecture |
| 13 | Clarifier la licence (GPL-3.0) | S | Conformité |

*Effort : S < 1 j, M = 1-3 j.*

---

## Annexe — versions au moment de l'audit

| Composant | Version | État (07/2026) |
|---|---|---|
| AGP | 8.7.3 | En retard |
| Kotlin / KSP | 2.1.0 / 2.1.0-1.0.29 | En retard |
| Gradle (wrapper) | 8.10.2 | En retard |
| Compose BOM | 2024.12.01 | En retard |
| Media3 | 1.5.1 | En retard |
| Room | 2.6.1 | En retard |
| Coil | 2.7.0 | Version majeure en retard (3.x) |
| Koin | 4.0.0 | OK (mineures dispo) |
| OkHttp | 4.12.0 | OK (5.x GA dispo) |
| NewPipeExtractor | v0.26.2 (JitPack) | **Critique — à bumper** |
| coroutines / serialization | 1.9.0 / 1.7.3 | En retard |
| compileSdk / targetSdk / minSdk | 35 / 35 / 26 | OK |
