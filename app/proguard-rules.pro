# Règles ProGuard/R8.
# minifyEnabled = false en v1 — ce fichier est prêt pour activation future.

# ── NewPipeExtractor ──────────────────────────────────────────────────────────
# Utilise de la réflexion pour les extracteurs de services (YouTube, SoundCloud…)
-keep class org.schabi.newpipe.extractor.** { *; }
-keepclassmembers class org.schabi.newpipe.extractor.** { *; }
-dontwarn org.schabi.newpipe.extractor.**

# ── OkHttp ────────────────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ── Media3 / ExoPlayer ────────────────────────────────────────────────────────
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ── Kotlinx Serialization ─────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.louis.musix.**$$serializer { *; }
-keepclassmembers class com.louis.musix.** {
    *** Companion;
}
-keepclasseswithmembers class com.louis.musix.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Koin ──────────────────────────────────────────────────────────────────────
-keep class org.koin.** { *; }
-dontwarn org.koin.**
