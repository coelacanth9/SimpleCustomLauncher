package com.example.simplecustomlauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.example.simplecustomlauncher.BuildConfig
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import com.example.simplecustomlauncher.ui.theme.SimpleCustomLauncherTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.simplecustomlauncher.billing.PurchaseState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.example.simplecustomlauncher.ui.components.LargeConfirmDialog
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
import com.example.simplecustomlauncher.ads.AdManager
import com.example.simplecustomlauncher.billing.BillingManager
import com.example.simplecustomlauncher.data.DefaultPremiumManager
import com.example.simplecustomlauncher.data.PremiumManager

class MainActivity : ComponentActivity() {

    private lateinit var shortcutRepository: ShortcutRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var premiumManager: PremiumManager
    private lateinit var billingManager: BillingManager
    private lateinit var adManager: AdManager

    /** ホームジェスチャー（スワイプアップ等）でHOME intentを受けたことをCompose側に通知 */
    private val _homeIntent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val homeIntent = _homeIntent.asSharedFlow()

    /** アプリアンインストール通知をCompose側に送信 */
    private val _packageRemoved = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val packageRemoved = _packageRemoved.asSharedFlow()

    private val packageRemovedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != Intent.ACTION_PACKAGE_REMOVED) return
            // アプリ更新時は無視
            if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return
            val packageName = intent.data?.schemeSpecificPart ?: return
            Log.d("MainActivity", "Package removed: $packageName")
            _packageRemoved.tryEmit(packageName)
        }
    }

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
            },
            onPurchaseCleared = {
                // 払い戻し等で購入がクリアされた時
                premiumManager.clearPurchase()
            }
        )
        billingManager.initialize()
        Log.d("Billing", "MainActivity: v${BuildConfig.VERSION_NAME} 起動 Premium=${premiumManager.isPremiumActive()}")

        // AdManager 初期化
        adManager = AdManager(this)
        adManager.initialize()

        // アプリアンインストール検知
        val packageFilter = IntentFilter(Intent.ACTION_PACKAGE_REMOVED).apply {
            addDataScheme("package")
        }
        registerReceiver(packageRemovedReceiver, packageFilter)

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
                        adManager = adManager,
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

        // ホームジェスチャーでのintentを検知してCompose側に通知
        if (intent.action == Intent.ACTION_MAIN &&
            intent.categories?.contains(Intent.CATEGORY_HOME) == true) {
            _homeIntent.tryEmit(Unit)
        }
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
        unregisterReceiver(packageRemovedReceiver)
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
    adManager: AdManager,
    onThemeChanged: (ThemeMode) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    // ViewModel
    val viewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory(context, billingManager, adManager)
    )

    // 起動時に孤立ピンショートカットをクリーンアップ
    LaunchedEffect(Unit) {
        viewModel.cleanupOrphanedPinShortcuts(context)
    }

    // ホームジェスチャーでホーム画面に戻る
    val mainActivity = activity as? MainActivity
    LaunchedEffect(Unit) {
        mainActivity?.homeIntent?.collect {
            viewModel.navigateToHome()
        }
    }

    // アプリアンインストール検知
    LaunchedEffect(Unit) {
        mainActivity?.packageRemoved?.collect { packageName ->
            viewModel.onPackageRemoved(packageName)
        }
    }

    // 購入完了を監視
    val purchaseState by billingManager.purchaseState.collectAsState()
    LaunchedEffect(purchaseState) {
        if (purchaseState == PurchaseState.Purchased) {
            viewModel.onPurchaseCompleted()
        }
    }

    // ライフサイクル監視（onResumeでプレミアム状態を再チェック）
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPremiumStatus()
                viewModel.cleanupUninstalledPackages(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 権限状態
    var hasPermission by remember {
        mutableStateOf(PermissionManager.checkPermissions(context, CALENDAR_PERMISSIONS))
    }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    // 初回起動時の使い方案内
    val settingsRepository = remember { SettingsRepository(context) }
    var permissionHandled by remember { mutableStateOf(false) }
    var showWelcomeDialog by remember { mutableStateOf(false) }

    // 権限リクエスト
    RequestPermissions(
        context = context,
        permissions = CALENDAR_PERMISSIONS,
        onResult = { isGranted ->
            hasPermission = isGranted
            permissionHandled = true
        }
    )

    // 権限ダイアログ完了後に初回案内を表示
    LaunchedEffect(permissionHandled) {
        if (permissionHandled && !settingsRepository.onboardingShown) {
            showWelcomeDialog = true
        }
    }

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

    // 初回起動時の使い方案内ダイアログ
    if (showWelcomeDialog) {
        LargeConfirmDialog(
            title = context.getString(R.string.welcome_title),
            message = context.getString(R.string.welcome_message),
            confirmText = context.getString(R.string.open_how_to_use),
            cancelText = context.getString(R.string.close),
            onConfirm = {
                settingsRepository.onboardingShown = true
                showWelcomeDialog = false
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://coelacanth9.github.io/SimpleCustomLauncher/"))
                context.startActivity(intent)
            },
            onDismiss = {
                settingsRepository.onboardingShown = true
                showWelcomeDialog = false
            }
        )
    }

    // システムの戻るボタン/ジェスチャーの処理
    // ランチャーはルートなので常にバックを消費する
    BackHandler(enabled = true) {
        if (viewModel.screenState !is MainScreenState.Home) {
            viewModel.navigateToHome()
        } else if (viewModel.isEditMode) {
            viewModel.exitEditMode()
        } else if (viewModel.currentPageIndex > 0) {
            viewModel.navigateToPage(0)
        }
        // 1ページ目のホーム画面 → 何もしない（バックを消費して終了を防ぐ）
    }

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
                onDeleteUnplaced = { shortcut ->
                    viewModel.deleteUnplacedShortcut(context, shortcut.id)
                },
                onBack = { viewModel.navigateToHome() }
            )
        }

        is MainScreenState.SlotEdit -> {
            // refreshKeyを読み込んでデータ更新時に再計算をトリガー
            val currentColors = remember(viewModel.refreshKey, state.pageIndex, state.row, state.column) {
                viewModel.getPlacementColors(state.pageIndex, state.row, state.column)
            }
            val otherPlacedShortcuts = viewModel.getPlacedShortcuts().filter { it.id != state.currentShortcut?.id }
            val layoutConfig = viewModel.getLayoutConfig()
            val currentColumns = layoutConfig.getColumnsForRow(state.pageIndex, state.row)
            val currentTextOnly = layoutConfig.isTextOnlyForRow(state.pageIndex, state.row)

            SlotEditScreen(
                currentShortcut = state.currentShortcut,
                currentColumns = currentColumns,
                currentTextOnly = currentTextOnly,
                currentBackgroundColor = currentColors.first,
                currentTextColor = currentColors.second,
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
                onDeleteUnplaced = { shortcut ->
                    viewModel.deleteUnplacedShortcut(context, shortcut.id)
                },
                onClear = {
                    state.currentShortcut?.let { viewModel.clearSlot(it) }
                    viewModel.navigateToHome()
                },
                onChangeColumns = { newColumns ->
                    viewModel.changeRowColumns(state.pageIndex, state.row, newColumns)
                    viewModel.navigateToHome()
                },
                onChangeTextOnly = { textOnly ->
                    viewModel.changeRowTextOnly(state.pageIndex, state.row, textOnly)
                },
                onChangeColors = { bgColor, txtColor ->
                    viewModel.changeSlotColors(state.pageIndex, state.row, state.column, bgColor, txtColor)
                },
                onDeleteRow = {
                    viewModel.deleteRow(state.pageIndex, state.row)
                    viewModel.navigateToHome()
                },
                isPremium = viewModel.isPremium,
                onWatchAd = {
                    activity?.let { viewModel.showRewardedAd(it) }
                },
                onPurchase = {
                    activity?.let { viewModel.launchPurchase(it) }
                },
                formattedPrice = viewModel.getFormattedPrice(),
                isAdReady = viewModel.isAdReady(),
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
                isPremiumProvider = { viewModel.isPremium },
                onWatchAd = {
                    activity?.let { viewModel.showRewardedAd(it) }
                },
                onPurchase = {
                    activity?.let { viewModel.launchPurchase(it) }
                },
                formattedPriceProvider = { viewModel.getFormattedPrice() },
                isAdReadyProvider = { viewModel.isAdReady() },
                onRestoreComplete = { viewModel.refresh() },
                debugPremiumEnabled = viewModel.debugPremiumEnabled,
                onDebugPremiumChange = { viewModel.setDebugPremium(it) },
                onDebugClearAllPremium = { viewModel.clearAllPremiumStatus() }
            )
        }

        is MainScreenState.AllApps -> {
            AllAppsScreen(
                onBack = { viewModel.navigateToHome() }
            )
        }
    }
}
