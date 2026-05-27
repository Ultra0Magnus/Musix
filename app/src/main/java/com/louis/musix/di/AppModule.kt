package com.louis.musix.di

import com.louis.musix.data.SelectedSongHolder
import com.louis.musix.data.newpipe.YouTubeRepository
import com.louis.musix.player.PlayerController
import com.louis.musix.ui.screens.player.PlayerViewModel
import com.louis.musix.ui.screens.search.SearchViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Holders / état partagé
    single { SelectedSongHolder() }

    // Repositories
    single { YouTubeRepository() }

    // Phase 4 — lecteur background
    single { PlayerController(androidContext()) }

    // ViewModels
    viewModel { SearchViewModel(get()) }
    // get() #1 = YouTubeRepository, get() #2 = SelectedSongHolder, get() #3 = PlayerController
    viewModel { PlayerViewModel(get(), get(), get()) }

    // Phase 5 : single<MusixDatabase> { ... } / single<LibraryRepository> { ... }
}
