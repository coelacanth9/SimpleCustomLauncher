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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onPurchase: () -> Unit = {}
) {
    // 毎回評価されるように
    val isPremium = isPremiumProvider()
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
                        text = "アプリ設定",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
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
                    title = "ホームアプリの設定",
                    description = "このアプリを標準のホームアプリに設定",
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
                SettingsSelectItem(
                    title = "起動操作",
                    description = "アプリを起動するときの操作",
                    currentValue = when (tapMode) {
                        TapMode.SINGLE_TAP -> "タップ"
                        TapMode.LONG_TAP -> "長押し"
                    },
                    onClick = { showTapModeDialog = true }
                )
            }

            // 確認ダイアログ設定
            item {
                SettingsSwitchItem(
                    title = "起動時に確認",
                    description = "アプリを起動する前に確認ダイアログを表示",
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
                    title = "タップ時の振動",
                    description = "タップ時に振動でフィードバック",
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
                SettingsSelectItem(
                    title = "テーマ",
                    description = "画面の配色を変更",
                    currentValue = when (themeMode) {
                        ThemeMode.LIGHT -> "ライト"
                        ThemeMode.DARK -> "ダーク"
                        ThemeMode.SYSTEM -> "端末設定に合わせる"
                    },
                    onClick = { showThemeModeDialog = true }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // ページ設定
            item {
                SettingsPremiumSelectItem(
                    title = "ホーム画面のページ数",
                    description = if (isPremium) "スワイプで複数ページを切り替え" else "プレミアム機能：複数ページに対応",
                    currentValue = if (isPremium) "${pageCount}ページ" else null,
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
                    title = "ページのループ",
                    description = "最後のページから最初のページへ移動可能",
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
                    title = "レイアウト編集",
                    description = "アプリの配置を変更します",
                    onClick = onEnterEditMode
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // 初期状態に戻す
            item {
                SettingsDangerItem(
                    title = "初期状態にリセット",
                    description = "アプリの配置がデフォルトに戻ります",
                    onClick = { showResetDialog = true }
                )
            }

            // レイアウトをクリア
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            item {
                SettingsDangerItem(
                    title = "レイアウトをクリア",
                    description = "すべての行と配置が削除されます",
                    onClick = { showClearDialog = true }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // バージョン情報
            item {
                SettingsInfoItem(
                    title = "バージョン",
                    value = "1.0.0"
                )
            }
        }
    }

    // タップ操作選択ダイアログ
    if (showTapModeDialog) {
        AlertDialog(
            onDismissRequest = { showTapModeDialog = false },
            title = { Text("タップ操作を選択") },
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
                                    TapMode.SINGLE_TAP -> "タップ"
                                    TapMode.LONG_TAP -> "長押し"
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
                    Text("キャンセル")
                }
            }
        )
    }

    // テーマ選択ダイアログ
    if (showThemeModeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeModeDialog = false },
            title = { Text("テーマを選択") },
            text = {
                Column {
                    listOf(
                        ThemeMode.SYSTEM to "端末設定に合わせる",
                        ThemeMode.LIGHT to "ライト",
                        ThemeMode.DARK to "ダーク"
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
                    Text("キャンセル")
                }
            }
        )
    }

    // リセット確認ダイアログ
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("初期状態にリセット") },
            text = { Text("現在の配置がすべて消去され、デフォルトの配置に戻ります。この操作は取り消せません。よろしいですか？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetToDefault()
                        showResetDialog = false
                    }
                ) {
                    Text("初期状態に戻す", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // レイアウト削除確認ダイアログ
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("レイアウトをクリア") },
            text = { Text("すべての行とアプリ配置が削除されます。この操作は取り消せません。よろしいですか？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearLayout()
                        showClearDialog = false
                    }
                ) {
                    Text("削除する", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // ページ数選択ダイアログ
    if (showPageCountDialog) {
        AlertDialog(
            onDismissRequest = { showPageCountDialog = false },
            title = { Text("ページ数を選択") },
            text = {
                Column {
                    (1..SettingsRepository.MAX_PAGES).forEach { count ->
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
                                text = "${count}ページ",
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPageCountDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // プレミアム機能ダイアログ
    if (showPremiumDialog) {
        AlertDialog(
            onDismissRequest = { showPremiumDialog = false },
            title = { Text("プレミアム機能") },
            text = {
                Column {
                    Text("複数ページ機能はプレミアム限定です。")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("動画を見るか課金して解除できます。")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onWatchAd()
                        showPremiumDialog = false
                    }
                ) {
                    Text("動画を見て24時間解除")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showPremiumDialog = false }) {
                        Text("キャンセル")
                    }
                    TextButton(
                        onClick = {
                            onPurchase()
                            showPremiumDialog = false
                        }
                    ) {
                        Text("課金して永久解除")
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
