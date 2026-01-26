package com.example.simplecustomlauncher

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortcutPickerSheet(
    onDismiss: () -> Unit,
    onShortcutSelected: (ShortcutData) -> Unit,
    onAppSelected: (AppInfo) -> Unit // アプリ自体を選択した場合
) {
    val context = LocalContext.current
    val helper = remember { ShortcutHelper(context) }

    // 現在の画面状態
    var currentScreen by remember { mutableStateOf<PickerScreen>(PickerScreen.AppList) }

    // アプリ一覧
    val apps = remember { helper.getInstalledApps() }

    // 検索クエリ
    var searchQuery by remember { mutableStateOf("") }

    // フィルタされたアプリ一覧
    val filteredApps = remember(searchQuery, apps) {
        if (searchQuery.isBlank()) {
            apps
        } else {
            apps.filter {
                it.label.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // 選択中のアプリのショートカット
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var shortcuts by remember { mutableStateOf<List<ShortcutData>>(emptyList()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(16.dp)
        ) {
            when (currentScreen) {
                is PickerScreen.AppList -> {
                    Text(
                        text = stringResource(R.string.select_app),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // 検索フィールド
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        placeholder = { Text(stringResource(R.string.search_app_name)) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // 検索結果の件数
                    Text(
                        text = "${filteredApps.size}件のアプリ",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyColumn {
                        items(filteredApps) { app ->
                            AppListItem(
                                app = app,
                                onClick = {
                                    selectedApp = app
                                    shortcuts = helper.getShortcutsForApp(app.packageName)
                                    currentScreen = PickerScreen.ShortcutList
                                }
                            )
                        }
                    }
                }

                is PickerScreen.ShortcutList -> {
                    // 戻るボタン
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        IconButton(onClick = { currentScreen = PickerScreen.AppList }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                        Text(
                            text = selectedApp?.label ?: "",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    LazyColumn {
                        // アプリ自体を起動するオプション
                        item {
                            selectedApp?.let { app ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { onAppSelected(app) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        app.icon?.let { DrawableImage(it, 48) }
                                        Spacer(Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = stringResource(R.string.launch_app),
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = app.label,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ショートカット一覧
                        if (shortcuts.isEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.no_shortcuts_for_app),
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            item {
                                Text(
                                    text = stringResource(R.string.shortcut_count, shortcuts.size),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            items(shortcuts) { shortcut ->
                                ShortcutListItem(
                                    shortcut = shortcut,
                                    onClick = { onShortcutSelected(shortcut) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppListItem(app: AppInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            app.icon?.let { DrawableImage(it, 48) }
            Spacer(Modifier.width(16.dp))
            Text(
                text = app.label,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
private fun ShortcutListItem(shortcut: ShortcutData, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            shortcut.icon?.let { DrawableImage(it, 40) }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = shortcut.shortLabel,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                shortcut.longLabel?.let {
                    Text(
                        text = it,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawableImage(drawable: Drawable, size: Int) {
    val bitmap = remember(drawable) {
        drawable.toBitmap(size * 2, size * 2)
    }
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        modifier = Modifier.size(size.dp)
    )
}

private sealed class PickerScreen {
    data object AppList : PickerScreen()
    data object ShortcutList : PickerScreen()
}
