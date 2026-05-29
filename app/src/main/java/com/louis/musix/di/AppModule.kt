package com.louis.musix.di

import com.louis.musix.data.SelectedSongHolder
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
import com.louis.musix.ui.screens.spotify.SpotifyImportViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // ─── Holders ──────────────────────────────────────────────────────────────
    single { SelectedSongHolder() }

    // ─── Repositories ─────────────────────────────────────────────────────────
    single { YouTubeRepository() }

    // ─── Spotify ──────────────────────────────────────────────────────────────
    single { SpotifyAuthManager(androidContext()) }
    single { SpotifyRepository(get()) }

    // ─── Room database ────────────────────────────────────────────────────────
    single { MusixDatabase.create(androidContext()) }
    single { get<MusixDatabase>().songDao() }
    single { get<MusixDatabase>().favoriteDao() }
    single { get<MusixDatabase>().historyDao() }
    single { get<MusixDatabase>().playlistDao() }
    single { LibraryRepository(get(), get(), get(), get()) }

    // ─── Background player ────────────────────────────────────────────────────
    single { PlayerController(androidContext(), get()) }

    // ─── Lyrics ───────────────────────────────────────────────────────────────
    single { LyricsRepository() }

    // ─── ViewModels ───────────────────────────────────────────────────────────
    viewModel { SearchViewModel(get()) }
    viewModel { PlayerViewModel(get(), get(), get(), get(), get()) }
    viewModel { LibraryViewModel(get()) }
    viewModel { HomeViewModel(get(), get()) }
    viewModel { SpotifyImportViewModel(get(), get(), get(), get()) }
    viewModel { ArtistViewModel(get()) }
    viewModel { AlbumDetailViewModel(get()) }
    // PlaylistDetailViewModel takes the id as a parameter (koinViewModel { parametersOf(id) })
    viewModel { params -> PlaylistDetailViewModel(params.get(), get()) }
}
