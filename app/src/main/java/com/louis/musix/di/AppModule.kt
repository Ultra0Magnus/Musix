package com.louis.musix.di

import com.louis.musix.data.SelectedSongHolder
import com.louis.musix.domain.util.NetworkMonitor
import com.louis.musix.data.download.DownloadManager
import com.louis.musix.player.cache.CacheManager
import com.louis.musix.data.local.MusixDatabase
import com.louis.musix.data.lyrics.LyricsRepository
import com.louis.musix.data.newpipe.YouTubeRepository
import com.louis.musix.data.repo.LibraryRepository
import com.louis.musix.data.spotify.SpotifyAuthManager
import com.louis.musix.data.spotify.SpotifyRepository
import com.louis.musix.player.PlayerController
import com.louis.musix.ui.screens.artist.AlbumDetailViewModel
import com.louis.musix.ui.screens.artist.ArtistViewModel
import com.louis.musix.ui.screens.home.HomeViewModel
import com.louis.musix.ui.screens.library.LibraryViewModel
import com.louis.musix.ui.screens.player.PlayerViewModel
import com.louis.musix.ui.screens.playlist.PlaylistDetailViewModel
import com.louis.musix.ui.screens.search.SearchViewModel
import com.louis.musix.ui.screens.settings.SettingsViewModel
import com.louis.musix.ui.screens.spotify.SpotifyImportViewModel
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val appModule = module {

    // ─── OkHttpClient partagé (pool de connexions unique pour toute l'app) ──────
    single {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // ─── Holders (conservé pour compatibilité, déprécié — voir PlayerViewModel) ─
    single { SelectedSongHolder() }

    // ─── Repositories ─────────────────────────────────────────────────────────
    single { YouTubeRepository() }

    // ─── Spotify (OkHttpClient injecté) ───────────────────────────────────────
    single { SpotifyAuthManager(androidContext(), get()) }
    single { SpotifyRepository(get()) }

    // ─── Room database ────────────────────────────────────────────────────────
    single { MusixDatabase.create(androidContext()) }
    single { get<MusixDatabase>().songDao() }
    single { get<MusixDatabase>().favoriteDao() }
    single { get<MusixDatabase>().historyDao() }
    single { get<MusixDatabase>().playlistDao() }
    single { LibraryRepository(get(), get(), get(), get()) }

    // ─── Utilities ────────────────────────────────────────────────────────────
    single { NetworkMonitor(androidContext()) }

    // ─── Downloads & Cache ───────────────────────────────────────────────────
    single { DownloadManager(androidContext(), get(), get()) }
    single { CacheManager(androidContext()) }

    // ─── Background player ────────────────────────────────────────────────────
    single { PlayerController(androidContext(), get(), get()) }

    // ─── Lyrics ───────────────────────────────────────────────────────────────
    single { LyricsRepository() }

    // ─── ViewModels ───────────────────────────────────────────────────────────
    viewModel { SearchViewModel(get()) }
    viewModel { PlayerViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { LibraryViewModel(get(), get()) }
    viewModel { HomeViewModel(get(), get()) }
    viewModel { SpotifyImportViewModel(get(), get(), get(), get()) }
    viewModel { ArtistViewModel(get()) }
    viewModel { AlbumDetailViewModel(get()) }
    viewModel { SettingsViewModel(get(), get()) }
    // PlaylistDetailViewModel takes the id as a parameter (koinViewModel { parametersOf(id) })
    viewModel { params -> PlaylistDetailViewModel(params.get(), get()) }
}
