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
import com.example.simplecustomlauncher.ui.screens.AppSettingsScreen
import com.example.simplecustomlauncher.ui.screens.CalendarFullScreen
import com.example.simplecustomlauncher.ui.screens.HomeScreen
import com.example.simplecustomlauncher.ui.screens.MemoScreen
import com.example.simplecustomlauncher.ui.screens.ShortcutAddScreen
import com.example.simplecustomlauncher.ui.screens.SlotEditScreen
import java.time.LocalDate
import java.util.UUID

class MainActivity : ComponentActivity() {

    private lateinit var shortcutRepository: ShortcutRepository
    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shortcutRepository = ShortcutRepository(this)
        settingsRepository = SettingsRepository(this)

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
                    label = shortcutInfo.shortLabel?.toString() ?: "ショートカット",
                    packageName = shortcutInfo.`package`
                )

                shortcutRepository.savePinShortcutInfo(item.id, shortcutInfo.id, shortcutInfo.`package`)
                shortcutRepository.saveShortcut(item)
                request.accept()
                Toast.makeText(this, "「${item.label}」を一時保管に追加しました", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun MainLauncherScreen(
    onThemeChanged: (ThemeMode) -> Unit = {}
) {
    val context = LocalContext.current

    // ViewModel
    val viewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory(context)
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
                    viewModel.targetSlot?.let { (row, col) ->
                        viewModel.placeShortcut(shortcut, row, col)
                    }
                    viewModel.navigateToHome()
                },
                onSelectInternal = { feature ->
                    viewModel.targetSlot?.let { (row, col) ->
                        viewModel.placeInternalFeature(feature.type, feature.label, row, col)
                    }
                    viewModel.navigateToHome()
                },
                onSelectApp = { app ->
                    viewModel.targetSlot?.let { (row, col) ->
                        viewModel.placeApp(app.packageName, app.label, row, col)
                    }
                    viewModel.navigateToHome()
                },
                onSelectShortcut = { shortcut ->
                    viewModel.targetSlot?.let { (row, col) ->
                        viewModel.placeIntent(shortcut.shortLabel, shortcut.packageName, shortcut.id, row, col)
                    }
                    viewModel.navigateToHome()
                },
                onSelectContact = { name, phoneNumber, type ->
                    viewModel.targetSlot?.let { (row, col) ->
                        viewModel.placeContact(name, phoneNumber, type, row, col)
                    }
                    viewModel.navigateToHome()
                },
                onBack = { viewModel.navigateToHome() }
            )
        }

        is MainScreenState.SlotEdit -> {
            val otherPlacedShortcuts = viewModel.getPlacedShortcuts().filter { it.id != state.currentShortcut?.id }
            val currentColumns = viewModel.getLayoutConfig().getColumnsForRow(state.row)

            SlotEditScreen(
                currentShortcut = state.currentShortcut,
                currentColumns = currentColumns,
                unplacedShortcuts = viewModel.getUnplacedShortcuts(),
                placedShortcuts = otherPlacedShortcuts,
                onSelectUnplaced = { shortcut ->
                    viewModel.placeShortcut(shortcut, state.row, state.column)
                    viewModel.navigateToHome()
                },
                onSelectPlaced = { shortcut ->
                    viewModel.swapShortcuts(state.currentShortcut, shortcut, state.row, state.column)
                    viewModel.navigateToHome()
                },
                onSelectApp = { app ->
                    viewModel.placeApp(app.packageName, app.label, state.row, state.column)
                    viewModel.navigateToHome()
                },
                onSelectShortcut = { shortcut ->
                    viewModel.placeIntent(shortcut.shortLabel, shortcut.packageName, shortcut.id, state.row, state.column)
                    viewModel.navigateToHome()
                },
                onSelectInternal = { feature ->
                    viewModel.placeInternalFeature(feature.type, feature.label, state.row, state.column)
                    viewModel.navigateToHome()
                },
                onSelectContact = { name, phoneNumber, type ->
                    viewModel.placeContact(name, phoneNumber, type, state.row, state.column)
                    viewModel.navigateToHome()
                },
                onClear = {
                    state.currentShortcut?.let { viewModel.clearSlot(it) }
                    viewModel.navigateToHome()
                },
                onChangeColumns = { newColumns ->
                    viewModel.changeRowColumns(state.row, newColumns)
                    viewModel.navigateToHome()
                },
                onDeleteRow = {
                    viewModel.deleteRow(state.row)
                    viewModel.navigateToHome()
                },
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
                onThemeChanged = onThemeChanged
            )
        }
    }
}
