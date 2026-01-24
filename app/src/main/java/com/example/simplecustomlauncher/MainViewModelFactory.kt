package com.example.simplecustomlauncher

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.simplecustomlauncher.data.SettingsRepository
import com.example.simplecustomlauncher.data.ShortcutRepository

/**
 * MainViewModel のファクトリー
 */
class MainViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(
                shortcutRepository = ShortcutRepository(context),
                settingsRepository = SettingsRepository(context),
                calendarRepository = CalendarRepository(context)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
