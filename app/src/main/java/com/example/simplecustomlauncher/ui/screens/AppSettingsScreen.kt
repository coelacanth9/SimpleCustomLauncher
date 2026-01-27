package com.example.simplecustomlauncher.ui.screens

import android.content.Intent
import android.provider.Settings
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
import com.example.simplecustomlauncher.R
import com.example.simplecustomlauncher.data.SettingsRepository
import com.example.simplecustomlauncher.data.TapMode
import com.example.simplecustomlauncher.data.ThemeMode

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
    isAdReadyProvider: () -> Boolean = { true }
) {
    // 毎回評価されるように
    val isPremium = isPremiumProvider()
    val formattedPrice = formattedPriceProvider()
    val isAdReady = isAdReadyProvider()
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }

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
                    enabled = isPremium && pageCount > 1,
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

            // 初期状態に戻す
            item {
                SettingsDangerItem(
                    title = stringResource(R.string.reset_to_default),
                    description = stringResource(R.string.reset_layout_desc),
                    onClick = { showResetDialog = true }
                )
            }

            // レイアウトをクリア
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            item {
                SettingsDangerItem(
                    title = stringResource(R.string.clear_layout),
                    description = stringResource(R.string.clear_layout_desc),
                    onClick = { showClearDialog = true }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // バージョン情報
            item {
                SettingsInfoItem(
                    title = stringResource(R.string.version),
                    value = "1.0.0"
                )
            }
        }
    }

    // タップ操作選択ダイアログ
    if (showTapModeDialog) {
        val tapModeSingleLabel = stringResource(R.string.tap_mode_single)
        val tapModeLongLabel = stringResource(R.string.tap_mode_long)
        val cancelLabel = stringResource(R.string.cancel)
        AlertDialog(
            onDismissRequest = { showTapModeDialog = false },
            title = { Text(stringResource(R.string.select_tap_mode)) },
            text = {
                Column {
                    TapMode.values().forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    tapMode = mode
                                    settingsRepository.tapMode = mode
                                    showTapModeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = tapMode == mode,
                                onClick = {
                                    tapMode = mode
                                    settingsRepository.tapMode = mode
                                    showTapModeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (mode) {
                                    TapMode.SINGLE_TAP -> tapModeSingleLabel
                                    TapMode.LONG_TAP -> tapModeLongLabel
                                },
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTapModeDialog = false }) {
                    Text(cancelLabel)
                }
            }
        )
    }

    // テーマ選択ダイアログ
    if (showThemeModeDialog) {
        val themeSystemLabel = stringResource(R.string.theme_system)
        val themeLightLabel = stringResource(R.string.theme_light)
        val themeDarkLabel = stringResource(R.string.theme_dark)
        val cancelLabel = stringResource(R.string.cancel)
        AlertDialog(
            onDismissRequest = { showThemeModeDialog = false },
            title = { Text(stringResource(R.string.select_theme)) },
            text = {
                Column {
                    listOf(
                        ThemeMode.SYSTEM to themeSystemLabel,
                        ThemeMode.LIGHT to themeLightLabel,
                        ThemeMode.DARK to themeDarkLabel
                    ).forEach { (mode, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    themeMode = mode
                                    settingsRepository.themeMode = mode
                                    onThemeChanged(mode)
                                    showThemeModeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = themeMode == mode,
                                onClick = {
                                    themeMode = mode
                                    settingsRepository.themeMode = mode
                                    onThemeChanged(mode)
                                    showThemeModeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showThemeModeDialog = false }) {
                    Text(cancelLabel)
                }
            }
        )
    }

    // リセット確認ダイアログ
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.reset_to_default)) },
            text = { Text(stringResource(R.string.reset_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetToDefault()
                        showResetDialog = false
                    }
                ) {
                    Text(stringResource(R.string.reset_to_default_short), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // レイアウト削除確認ダイアログ
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.clear_layout)) },
            text = { Text(stringResource(R.string.clear_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearLayout()
                        showClearDialog = false
                    }
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // ページ数選択ダイアログ
    if (showPageCountDialog) {
        AlertDialog(
            onDismissRequest = { showPageCountDialog = false },
            title = { Text(stringResource(R.string.select_page_count)) },
            text = {
                Column {
                    (1..SettingsRepository.MAX_PAGES).forEach { count ->
                        val pageLabel = stringResource(R.string.page_count_format, count)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    pageCount = count
                                    settingsRepository.pageCount = count
                                    showPageCountDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = pageCount == count,
                                onClick = {
                                    pageCount = count
                                    settingsRepository.pageCount = count
                                    showPageCountDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = pageLabel,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPageCountDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // プレミアム機能ダイアログ
    if (showPremiumDialog) {
        AlertDialog(
            onDismissRequest = { showPremiumDialog = false },
            title = { Text(stringResource(R.string.premium_feature)) },
            text = {
                Column {
                    Text(stringResource(R.string.premium_page_only))
                    Spacer(modifier = Modifier.height(8.dp))
                    if (formattedPrice != null) {
                        Text(stringResource(R.string.premium_price_format, formattedPrice))
                    } else {
                        Text(stringResource(R.string.premium_unlock_short))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onWatchAd()
                        showPremiumDialog = false
                    },
                    enabled = isAdReady
                ) {
                    Text(
                        if (isAdReady) {
                            stringResource(R.string.watch_ad_unlock)
                        } else {
                            stringResource(R.string.ad_loading)
                        }
                    )
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showPremiumDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(
                        onClick = {
                            onPurchase()
                            showPremiumDialog = false
                        }
                    ) {
                        Text(
                            if (formattedPrice != null) {
                                stringResource(R.string.purchase_with_price, formattedPrice)
                            } else {
                                stringResource(R.string.purchase_unlock)
                            }
                        )
                    }
                }
            }
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
        modifier = Modifier.fillMaxWidth(),
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
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
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
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Premium",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
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
        MaterialTheme.colorScheme.tertiaryContainer
    }
    val contentColor = if (isPremiumActive) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onTertiaryContainer
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
                        color = MaterialTheme.colorScheme.tertiary
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
        modifier = Modifier.fillMaxWidth(),
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
                            color = if (enabled) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.outline
                            }
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
