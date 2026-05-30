package com.louis.musix.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.louis.musix.player.cache.CacheManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val cacheManager: CacheManager
) : ViewModel() {

    private val _cacheSizeMb = MutableStateFlow(0f)
    val cacheSizeMb: StateFlow<Float> = _cacheSizeMb.asStateFlow()

    init {
        updateCacheSize()
    }

    fun updateCacheSize() {
        val bytes = cacheManager.cacheSizeBytes
        val mb = bytes.toFloat() / (1024f * 1024f)
        _cacheSizeMb.value = mb
    }

    fun clearCache() {
        viewModelScope.launch {
            cacheManager.clearCache()
            updateCacheSize()
        }
    }
}
