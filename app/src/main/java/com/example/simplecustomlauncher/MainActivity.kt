package com.example.simplecustomlauncher

import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import com.example.simplecustomlauncher.ui.theme.SimpleCustomLauncherTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.simplecustomlauncher.PermissionManager.CALENDAR_PERMISSIONS
import com.example.simplecustomlauncher.data.SettingsRepository
import com.example.simplecustomlauncher.data.ShortcutItem
import com.example.simplecustomlauncher.data.ShortcutPlacement
import com.example.simplecustomlauncher.data.ShortcutRepository
import com.example.simplecustomlauncher.data.ShortcutType
import com.example.simplecustomlauncher.data.ThemeMode
import com.example.simplecustomlauncher.ui.screens.AllAppsScreen
import com.example.simplecustomlauncher.ui.screens.AppSettingsScreen
import com.example.simplecustomlauncher.ui.screens.CalendarFullScreen
import com.example.simplecustomlauncher.ui.screens.HomeScreen
import com.example.simplecustomlauncher.ui.screens.MemoScreen
import com.example.simplecustomlauncher.ui.screens.ShortcutAddScreen
import com.example.simplecustomlauncher.ui.screens.SlotEditScreen
import java.time.LocalDate
import java.util.UUID
import android.content.pm.ActivityInfo
import com.example.simplecustomlauncher.R
import com.example.simplecustomlauncher.billing.BillingManager
import com.example.simplecustomlauncher.data.DefaultPremiumManager
import com.example.simplecustomlauncher.data.PremiumManager

class MainActivity : ComponentActivity() {

    private lateinit var shortcutRepository: ShortcutRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var premiumManager: PremiumManager
    private lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isTablet = resources.configuration.smallestScreenWidthDp >= 600
        if (!isTablet) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        shortcutRepository = ShortcutRepository(this)
        settingsRepository = SettingsRepository(this)
        premiumManager = DefaultPremiumManager(this, settingsRepository)

        // BillingManager 初期化
        billingManager = BillingManager(
            context = this,
            onPurchaseComplete = {
                // 購入完了時にPremiumManagerを更新
                premiumManager.recordPurchase()
            }
        )
        billingManager.initialize()

        // 初回起動時にデフォルトレイアウトを適用
        if (shortcutRepository.isFirstLaunch()) {
            shortcutRepository.applyDefaultLayout()
        }

        // 起動時のIntentを処理
        handleIntent(intent)

        setContent {
            // テーマモードを監視
            var themeMode by remember { mutableStateOf(settingsRepository.themeMode) }

            SimpleCustomLauncherTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainLauncherScreen(
                        billingManager = billingManager,
                        onThemeChanged = { newMode ->
                            themeMode = newMode
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        Log.d("MainActivity", "handleIntent: action=${intent.action}, extras=${intent.extras?.keySet()}")

        if (intent.hasExtra(LauncherApps.EXTRA_PIN_ITEM_REQUEST)) {
            handlePinShortcut(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        billingManager.endConnection()
    }

    private fun handlePinShortcut(intent: Intent) {
        val launcherApps = getSystemService(LAUNCHER_APPS_SERVICE) as LauncherApps
        val request = launcherApps.getPinItemRequest(intent)

        if (request == null) {
            Log.e("MainActivity", "PinItemRequest is null")
            return
        }

        if (request.requestType == LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT) {
            val shortcutInfo = request.shortcutInfo
            if (shortcutInfo != null) {
                Log.d("MainActivity", "Pin shortcut: ${shortcutInfo.shortLabel}, package: ${shortcutInfo.`package`}")

                val item = ShortcutItem(
                    id = UUID.randomUUID().toString(),
                    type = ShortcutType.INTENT,
                    label = shortcutInfo.shortLabel?.toString() ?: getString(R.string.shortcut),
                    packageName = shortcutInfo.`package`
                )

                shortcutRepository.savePinShortcutInfo(item.id, shortcutInfo.id, shortcutInfo.`package`)
                shortcutRepository.saveShortcut(item)
                request.accept()
                Toast.makeText(this, getString(R.string.item_added_to_storage, item.label), Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun MainLauncherScreen(
    billingManager: BillingManager,
    onThemeChanged: (ThemeMode) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    // ViewModel
    val viewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory(context, billingManager)
    )

    // 権限状態
    var hasPermission by remember {
        mutableStateOf(PermissionManager.checkPermissions(context, CALENDAR_PERMISSIONS))
    }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    // 権限リクエスト
    RequestPermissions(
        context = context,
        permissions = CALENDAR_PERMISSIONS,
        onResult = { isGranted -> hasPermission = isGranted }
    )

    // 祝日データ（カレンダー画面用）
    val holidayMap = remember(hasPermission) {
        if (hasPermission) {
            val now = LocalDate.now()
            viewModel.getHolidaysForMonth(now.year, now.monthValue, hasPermission)
        } else {
            emptyMap()
        }
    }

    // ShortcutHelper
    val shortcutHelper = remember { ShortcutHelper(context) }

    // 画面遷移
    when (val state = viewModel.screenState) {
        is MainScreenState.Home -> {
            HomeScreen(
                viewModel = viewModel,
                snackbarHostState = snackbarHostState
            )
        }

        is MainScreenState.ShortcutAdd -> {
            ShortcutAddScreen(
                unplacedShortcuts = viewModel.getUnplacedShortcuts(),
                onSelectUnplaced = { shortcut ->
                    viewModel.targetSlot?.let { (page, row, col) ->
                        viewModel.placeShortcut(shortcut, page, row, col)
                    }
                    viewModel.navigateToHome()
                },
                onSelectInternal = { feature ->
                    viewModel.targetSlot?.let { (page, row, col) ->
                        viewModel.placeInternalFeature(feature.type, context.getString(feature.labelResId), page, row, col)
                    }
                    viewModel.navigateToHome()
                },
                onSelectApp = { app ->
                    viewModel.targetSlot?.let { (page, row, col) ->
                        viewModel.placeApp(app.packageName, app.label, page, row, col)
                    }
                    viewModel.navigateToHome()
                },
                onSelectShortcut = { shortcut ->
                    viewModel.targetSlot?.let { (page, row, col) ->
                        viewModel.placeIntent(shortcut.shortLabel, shortcut.packageName, shortcut.id, page, row, col)
                    }
                    viewModel.navigateToHome()
                },
                onSelectContact = { name, phoneNumber, type ->
                    viewModel.targetSlot?.let { (page, row, col) ->
                        viewModel.placeContact(name, phoneNumber, type, page, row, col)
                    }
                    viewModel.navigateToHome()
                },
                onBack = { viewModel.navigateToHome() }
            )
        }

        is MainScreenState.SlotEdit -> {
            val otherPlacedShortcuts = viewModel.getPlacedShortcuts().filter { it.id != state.currentShortcut?.id }
            val currentColumns = viewModel.getLayoutConfig().getColumnsForRow(state.pageIndex, state.row)
            val totalPageCount = viewModel.getTotalPageCount()

            SlotEditScreen(
                currentShortcut = state.currentShortcut,
                currentColumns = currentColumns,
                unplacedShortcuts = viewModel.getUnplacedShortcuts(),
                placedShortcuts = otherPlacedShortcuts,
                onSelectUnplaced = { shortcut ->
                    viewModel.placeShortcut(shortcut, state.pageIndex, state.row, state.column)
                    viewModel.navigateToHome()
                },
                onSelectPlaced = { shortcut ->
                    viewModel.swapShortcuts(state.currentShortcut, shortcut, state.pageIndex, state.row, state.column)
                    viewModel.navigateToHome()
                },
                onSelectApp = { app ->
                    viewModel.placeApp(app.packageName, app.label, state.pageIndex, state.row, state.column)
                    viewModel.navigateToHome()
                },
                onSelectShortcut = { shortcut ->
                    viewModel.placeIntent(shortcut.shortLabel, shortcut.packageName, shortcut.id, state.pageIndex, state.row, state.column)
                    viewModel.navigateToHome()
                },
                onSelectInternal = { feature ->
                    viewModel.placeInternalFeature(feature.type, context.getString(feature.labelResId), state.pageIndex, state.row, state.column)
                    viewModel.navigateToHome()
                },
                onSelectContact = { name, phoneNumber, type ->
                    viewModel.placeContact(name, phoneNumber, type, state.pageIndex, state.row, state.column)
                    viewModel.navigateToHome()
                },
                onClear = {
                    state.currentShortcut?.let { viewModel.clearSlot(it) }
                    viewModel.navigateToHome()
                },
                onChangeColumns = { newColumns ->
                    viewModel.changeRowColumns(state.pageIndex, state.row, newColumns)
                    viewModel.navigateToHome()
                },
                onDeleteRow = {
                    viewModel.deleteRow(state.pageIndex, state.row)
                    viewModel.navigateToHome()
                },
                onDeletePage = if (totalPageCount > 1) {
                    {
                        viewModel.deletePage(state.pageIndex)
                        viewModel.navigateToHome()
                    }
                } else null,
                onBack = { viewModel.navigateToHome() }
            )
        }

        is MainScreenState.Calendar -> {
            CalendarFullScreen(
                hasPermission = hasPermission,
                holidayMap = holidayMap,
                onBack = { viewModel.navigateToHome() }
            )
        }

        is MainScreenState.Memo -> {
            MemoScreen(
                onBack = { viewModel.navigateToHome() }
            )
        }

        is MainScreenState.AppSettings -> {
            AppSettingsScreen(
                onBack = { viewModel.navigateToHome() },
                onEnterEditMode = { viewModel.enterEditMode() },
                onResetToDefault = { viewModel.resetToDefault() },
                onClearLayout = { viewModel.clearLayout() },
                onThemeChanged = onThemeChanged,
                isPremiumProvider = { viewModel.isPremiumActive() },
                onWatchAd = { viewModel.recordAdWatch() },
                onPurchase = {
                    activity?.let { viewModel.launchPurchase(it) }
                },
                formattedPriceProvider = { viewModel.getFormattedPrice() }
            )
        }

        is MainScreenState.AllApps -> {
            AllAppsScreen(
                onBack = { viewModel.navigateToHome() }
            )
        }
    }
}
