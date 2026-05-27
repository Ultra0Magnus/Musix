package com.louis.musix

import android.app.Application
import com.louis.musix.data.newpipe.NewPipeDownloader
import com.louis.musix.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.Localization

class MusixApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Koin — injection de dépendances
        startKoin {
            androidLogger(Level.INFO)
            androidContext(this@MusixApp)
            modules(appModule)
        }

        // NewPipeExtractor — localization EN obligatoire : NewPipeExtractor parse les
        // réponses YouTube en anglais. Si on met "FR", YT répond en français et
        // le parsing échoue avec "page needs to be reloaded".
        NewPipe.init(NewPipeDownloader, Localization("en", "US"))
    }
}
