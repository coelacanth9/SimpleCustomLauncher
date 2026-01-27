package com.example.simplecustomlauncher

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.simplecustomlauncher.billing.BillingManager
import com.example.simplecustomlauncher.data.DefaultPremiumManager
import com.example.simplecustomlauncher.data.SettingsRepository
import com.example.simplecustomlauncher.data.ShortcutRepository

/**
 * MainViewModel のファクトリー
 */
class MainViewModelFactory(
    private val context: Context,
    private val billingManager: BillingManager? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            val settingsRepository = SettingsRepository(context)
            return MainViewModel(
                shortcutRepository = ShortcutRepository(context),
                settingsRepository = settingsRepository,
                calendarRepository = CalendarRepository(context),
                premiumManager = DefaultPremiumManager(context, settingsRepository),
                billingManager = billingManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
