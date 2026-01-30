package com.example.simplecustomlauncher.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.simplecustomlauncher.BuildConfig
import com.example.simplecustomlauncher.R
import com.example.simplecustomlauncher.data.BackupManager
import com.example.simplecustomlauncher.data.RestoreResult
import com.example.simplecustomlauncher.data.SettingsRepository
import com.example.simplecustomlauncher.data.TapMode
import com.example.simplecustomlauncher.data.ThemeMode
import com.example.simplecustomlauncher.ui.components.ConfirmDialog
import com.example.simplecustomlauncher.ui.components.DangerConfirmDialog
import com.example.simplecustomlauncher.ui.components.InfoDialog
import com.example.simplecustomlauncher.ui.components.PremiumFeatureDialog
import com.example.simplecustomlauncher.ui.components.SelectionDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    onBack: () -> Unit,
    onEnterEditMode: () -> Unit = {},
    onResetToDefault: () -> Unit = {},
    onClearLayout: () -> Unit = {},
    onThemeChanged: (ThemeMode) -> Unit = {},
    isPremiumProvider: () -> Boolean = { false },
    onWatchAd: () -> Unit = {},
    onPurchase: () -> Unit = {},
    formattedPriceProvider: () -> String? = { null },
    isAdReadyProvider: () -> Boolean = { true },
    onRestoreComplete: () -> Unit = {},
    debugPremiumEnabled: Boolean = false,
    onDebugPremiumChange: (Boolean) -> Unit = {},
    onDebugClearAllPremium: () -> Unit = {}
) {
    // 毎回評価されるように
    val isPremium = isPremiumProvider()
    val formattedPrice = formattedPriceProvider()
    val isAdReady = isAdReadyProvider()
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val backupManager = remember { BackupManager(context) }

    var tapMode by remember { mutableStateOf(settingsRepository.tapMode) }
    var showConfirmDialog by remember { mutableStateOf(settingsRepository.showConfirmDialog) }
    var tapFeedback by remember { mutableStateOf(settingsRepository.tapFeedback) }
    var themeMode by remember { mutableStateOf(settingsRepository.themeMode) }
    var pageCount by remember { mutableStateOf(settingsRepository.pageCount) }
    var loopPagingEnabled by remember { mutableStateOf(settingsRepository.loopPagingEnabled) }
    var showTapModeDialog by remember { mutableStateOf(false) }
    var showThemeModeDialog by remember { mutableStateOf(false) }
    var showPageCountDialog by remember { mutableStateOf(false) }
    var showPremiumDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    // バックアップ/リストア用
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var restoreResultMessage by remember { mutableStateOf<String?>(null) }
    var showRestoreResultDialog by remember { mutableStateOf(false) }

    // ファイルピッカー（ローカルファイル用）
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                pendingRestoreUri = uri
                showRestoreConfirmDialog = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_settings),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ホームアプリ設定
            item {
                SettingsActionItem(
                    title = stringResource(R.string.home_app_settings),
                    description = stringResource(R.string.set_as_default_home),
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                    }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // タップ操作設定
            item {
                val tapModeLabel = when (tapMode) {
                    TapMode.SINGLE_TAP -> stringResource(R.string.tap_mode_single)
                    TapMode.LONG_TAP -> stringResource(R.string.tap_mode_long)
                }
                SettingsSelectItem(
                    title = stringResource(R.string.tap_mode),
                    description = stringResource(R.string.tap_mode_desc),
                    currentValue = tapModeLabel,
                    onClick = { showTapModeDialog = true }
                )
            }

            // 確認ダイアログ設定
            item {
                SettingsSwitchItem(
                    title = stringResource(R.string.confirm_before_launch),
                    description = stringResource(R.string.confirm_before_launch_desc),
                    checked = showConfirmDialog,
                    onCheckedChange = {
                        showConfirmDialog = it
                        settingsRepository.showConfirmDialog = it
                    }
                )
            }

            // タップフィードバック設定
            item {
                SettingsSwitchItem(
                    title = stringResource(R.string.haptic_feedback),
                    description = stringResource(R.string.haptic_feedback_desc),
                    checked = tapFeedback,
                    onCheckedChange = {
                        tapFeedback = it
                        settingsRepository.tapFeedback = it
                    }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // テーマ設定
            item {
                val themeModeLabel = when (themeMode) {
                    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                    ThemeMode.DARK -> stringResource(R.string.theme_dark)
                    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                }
                SettingsSelectItem(
                    title = stringResource(R.string.theme),
                    description = stringResource(R.string.change_theme_desc),
                    currentValue = themeModeLabel,
                    onClick = { showThemeModeDialog = true }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // ページ設定
            item {
                val pageDescription = if (isPremium) stringResource(R.string.multi_page_swipe) else stringResource(R.string.premium_multi_page)
                val pageValue = if (isPremium) stringResource(R.string.page_count_format, pageCount) else null
                SettingsPremiumSelectItem(
                    title = stringResource(R.string.home_page_count),
                    description = pageDescription,
                    currentValue = pageValue,
                    isPremiumActive = isPremium,
                    onClick = {
                        if (isPremium) {
                            showPageCountDialog = true
                        } else {
                            showPremiumDialog = true
                        }
                    }
                )
            }

            // ループページング
            item {
                SettingsPremiumSwitchItem(
                    title = stringResource(R.string.page_loop),
                    description = stringResource(R.string.page_loop_desc),
                    checked = loopPagingEnabled,
                    enabled = isPremium,
                    isPremiumFeature = true,
                    onCheckedChange = {
                        loopPagingEnabled = it
                        settingsRepository.loopPagingEnabled = it
                    }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // レイアウト編集
            item {
                SettingsActionItem(
                    title = stringResource(R.string.layout_edit),
                    description = stringResource(R.string.edit_layout_desc),
                    onClick = onEnterEditMode
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // バックアップ/リストア（Premium機能）
            item {
                SettingsPremiumItem(
                    title = stringResource(R.string.export_backup),
                    description = stringResource(R.string.export_backup_desc),
                    isPremiumActive = isPremium,
                    onClick = {
                        if (isPremium) {
                            val shareIntent = backupManager.createShareIntent()
                            val chooser = Intent.createChooser(shareIntent, context.getString(R.string.backup_share_title))
                            context.startActivity(chooser)
                        } else {
                            showPremiumDialog = true
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                SettingsPremiumItem(
                    title = stringResource(R.string.import_backup),
                    description = stringResource(R.string.import_backup_desc),
                    isPremiumActive = isPremium,
                    onClick = {
                        if (isPremium) {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "*/*"
                            }
                            filePickerLauncher.launch(intent)
                        } else {
                            showPremiumDialog = true
                        }
                    }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // 初期状態に戻す
            item {
                SettingsDangerItem(
                    title = stringResource(R.string.reset_to_default),
                    description = stringResource(R.string.reset_layout_desc),
                    onClick = { showResetDialog = true }
                )
            }

            // 一旦機能削除のためコメントアウト。各ページでクリアできるため、一括削除が必要か疑問。
//            // レイアウトをクリア
//            item {
//                Spacer(modifier = Modifier.height(8.dp))
//            }
//            item {
//                SettingsDangerItem(
//                    title = stringResource(R.string.clear_layout),
//                    description = stringResource(R.string.clear_layout_desc),
//                    onClick = { showClearDialog = true }
//                )
//            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // 使い方
            item {
                SettingsLinkItem(
                    title = stringResource(R.string.how_to_use),
                    description = stringResource(R.string.how_to_use_desc),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://coelacanth9.github.io/SimpleCustomLauncher/"))
                        context.startActivity(intent)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // プライバシーポリシー
            item {
                SettingsLinkItem(
                    title = stringResource(R.string.privacy_policy),
                    description = stringResource(R.string.privacy_policy_desc),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://coelacanth9.github.io/SimpleCustomLauncher/PRIVACY_POLICY"))
                        context.startActivity(intent)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // バージョン情報
            item {
                val buildType = if (BuildConfig.DEBUG) "debug" else "release"
                SettingsInfoItem(
                    title = stringResource(R.string.version),
                    value = "${BuildConfig.VERSION_NAME} ($buildType)"
                )
            }

            // デバッグセクション（DEBUGビルドのみ表示）
            if (BuildConfig.DEBUG) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                item {
                    Text(
                        text = "Debug Options",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                item {
                    SettingsSwitchItem(
                        title = "Debug Premium",
                        description = "強制的にプレミアム状態にする",
                        checked = debugPremiumEnabled,
                        onCheckedChange = onDebugPremiumChange
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    SettingsDangerItem(
                        title = "Clear All Premium",
                        description = "全てのプレミアム状態をクリア",
                        onClick = onDebugClearAllPremium
                    )
                }
            }
        }
    }

    // タップ操作選択ダイアログ
    if (showTapModeDialog) {
        val tapModeSingleLabel = stringResource(R.string.tap_mode_single)
        val tapModeLongLabel = stringResource(R.string.tap_mode_long)
        SelectionDialog(
            title = stringResource(R.string.select_tap_mode),
            options = listOf(
                TapMode.SINGLE_TAP to tapModeSingleLabel,
                TapMode.LONG_TAP to tapModeLongLabel
            ),
            selectedOption = tapMode,
            onSelect = { mode ->
                tapMode = mode
                settingsRepository.tapMode = mode
                showTapModeDialog = false
            },
            onDismiss = { showTapModeDialog = false }
        )
    }

    // テーマ選択ダイアログ
    if (showThemeModeDialog) {
        val themeSystemLabel = stringResource(R.string.theme_system)
        val themeLightLabel = stringResource(R.string.theme_light)
        val themeDarkLabel = stringResource(R.string.theme_dark)
        SelectionDialog(
            title = stringResource(R.string.select_theme),
            options = listOf(
                ThemeMode.SYSTEM to themeSystemLabel,
                ThemeMode.LIGHT to themeLightLabel,
                ThemeMode.DARK to themeDarkLabel
            ),
            selectedOption = themeMode,
            onSelect = { mode ->
                themeMode = mode
                settingsRepository.themeMode = mode
                onThemeChanged(mode)
                showThemeModeDialog = false
            },
            onDismiss = { showThemeModeDialog = false }
        )
    }

    // リセット確認ダイアログ
    if (showResetDialog) {
        DangerConfirmDialog(
            title = stringResource(R.string.reset_to_default),
            message = stringResource(R.string.reset_confirm_message),
            confirmText = stringResource(R.string.reset_to_default_short),
            onConfirm = {
                onResetToDefault()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false }
        )
    }

    // レイアウト削除確認ダイアログ
    if (showClearDialog) {
        DangerConfirmDialog(
            title = stringResource(R.string.clear_layout),
            message = stringResource(R.string.clear_confirm_message),
            confirmText = stringResource(R.string.delete),
            onConfirm = {
                onClearLayout()
                showClearDialog = false
            },
            onDismiss = { showClearDialog = false }
        )
    }

    // ページ数選択ダイアログ
    if (showPageCountDialog) {
        val pageOptions = (1..SettingsRepository.MAX_PAGES).map { count ->
            count to stringResource(R.string.page_count_format, count)
        }
        SelectionDialog(
            title = stringResource(R.string.select_page_count),
            options = pageOptions,
            selectedOption = pageCount,
            onSelect = { count ->
                pageCount = count
                settingsRepository.pageCount = count
                showPageCountDialog = false
            },
            onDismiss = { showPageCountDialog = false }
        )
    }

    // プレミアム機能ダイアログ
    if (showPremiumDialog) {
        PremiumFeatureDialog(
            description = stringResource(R.string.premium_page_only),
            formattedPrice = formattedPrice,
            isAdReady = isAdReady,
            onWatchAd = {
                onWatchAd()
                showPremiumDialog = false
            },
            onPurchase = {
                onPurchase()
                showPremiumDialog = false
            },
            onDismiss = { showPremiumDialog = false }
        )
    }

    // 復元確認ダイアログ
    if (showRestoreConfirmDialog) {
        ConfirmDialog(
            title = stringResource(R.string.restore_confirm_title),
            message = stringResource(R.string.restore_confirm_message),
            confirmText = stringResource(R.string.restore),
            onConfirm = {
                pendingRestoreUri?.let { uri ->
                    val result = backupManager.restoreFromUri(uri)
                    restoreResultMessage = when (result) {
                        is RestoreResult.Success -> context.getString(
                            R.string.restore_success,
                            result.shortcutCount,
                            result.pageCount
                        )
                        is RestoreResult.Error -> "${context.getString(R.string.restore_error)}: ${result.message}"
                    }
                    showRestoreResultDialog = true
                    if (result is RestoreResult.Success) {
                        // 設定値を再読み込み
                        themeMode = settingsRepository.themeMode
                        tapMode = settingsRepository.tapMode
                        showConfirmDialog = settingsRepository.showConfirmDialog
                        tapFeedback = settingsRepository.tapFeedback
                        pageCount = settingsRepository.pageCount
                        loopPagingEnabled = settingsRepository.loopPagingEnabled
                        onRestoreComplete()
                    }
                }
                showRestoreConfirmDialog = false
                pendingRestoreUri = null
            },
            onDismiss = {
                showRestoreConfirmDialog = false
                pendingRestoreUri = null
            }
        )
    }

    // 復元結果ダイアログ
    if (showRestoreResultDialog) {
        InfoDialog(
            title = stringResource(R.string.restore_confirm_title),
            message = restoreResultMessage ?: "",
            onDismiss = { showRestoreResultDialog = false }
        )
    }
}

@Composable
private fun SettingsSelectItem(
    title: String,
    description: String,
    currentValue: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = currentValue,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val textColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (enabled) {
                    Modifier.clickable { onCheckedChange(!checked) }
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.outline
                    }
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    }
}

@Composable
private fun SettingsDisabledItem(
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun SettingsPremiumItem(
    title: String,
    description: String,
    isPremiumActive: Boolean = false,
    onClick: () -> Unit
) {
    val containerColor = if (isPremiumActive) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isPremiumActive) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = contentColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Premium",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800) // オレンジ（常に目立つ）
                    )
                }
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun SettingsPremiumSelectItem(
    title: String,
    description: String,
    currentValue: String?,
    isPremiumActive: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isPremiumActive) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isPremiumActive) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = contentColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Premium",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800) // オレンジ（常に目立つ）
                    )
                }
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
            if (currentValue != null) {
                Text(
                    text = currentValue,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SettingsPremiumSwitchItem(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean = true,
    isPremiumFeature: Boolean = false,
    onCheckedChange: (Boolean) -> Unit
) {
    val textColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (enabled) {
                    Modifier.clickable { onCheckedChange(!checked) }
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor
                    )
                    if (isPremiumFeature) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Premium",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9800) // オレンジ（常に目立つ）
                        )
                    }
                }
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.outline
                    }
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    }
}

@Composable
private fun SettingsInfoItem(
    title: String,
    value: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsActionItem(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun SettingsDangerItem(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun SettingsLinkItem(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
