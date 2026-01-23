package com.example.simplecustomlauncher.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.simplecustomlauncher.AppInfo
import com.example.simplecustomlauncher.ShortcutData
import com.example.simplecustomlauncher.ShortcutHelper
import com.example.simplecustomlauncher.data.ShortcutItem
import com.example.simplecustomlauncher.data.ShortcutType

/**
 * 画面状態
 */
sealed class SelectScreenState {
    object Main : SelectScreenState()
    object AppList : SelectScreenState()
    data class AppShortcuts(val app: AppInfo) : SelectScreenState()
}

/**
 * アプリ内機能の定義
 */
data class InternalFeature(
    val type: ShortcutType,
    val label: String,
    val icon: String
)

val internalFeatures = listOf(
    InternalFeature(ShortcutType.CALENDAR, "シンプルカレンダー", "📅"),
    InternalFeature(ShortcutType.MEMO, "メモ帳", "📝")
)

/**
 * ショートカット追加画面（通常モードで＋タップ時）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortcutAddScreen(
    unplacedShortcuts: List<ShortcutItem>,
    onSelectUnplaced: (ShortcutItem) -> Unit,
    onSelectApp: (AppInfo) -> Unit,
    onSelectShortcut: (ShortcutData) -> Unit,
    onSelectInternal: (InternalFeature) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val helper = remember { ShortcutHelper(context) }

    var screenState by remember { mutableStateOf<SelectScreenState>(SelectScreenState.Main) }
    var shortcuts by remember { mutableStateOf<List<ShortcutData>>(emptyList()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (screenState) {
                            is SelectScreenState.Main -> "ショートカットを追加"
                            is SelectScreenState.AppList -> "アプリ一覧"
                            is SelectScreenState.AppShortcuts -> (screenState as SelectScreenState.AppShortcuts).app.label
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when (screenState) {
                            is SelectScreenState.Main -> onBack()
                            is SelectScreenState.AppList -> screenState = SelectScreenState.Main
                            is SelectScreenState.AppShortcuts -> screenState = SelectScreenState.AppList
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { paddingValues ->
        when (screenState) {
            is SelectScreenState.Main -> {
                MainSelectContent(
                    unplacedShortcuts = unplacedShortcuts,
                    onSelectUnplaced = onSelectUnplaced,
                    onSelectInternal = onSelectInternal,
                    onGoToAppList = { screenState = SelectScreenState.AppList },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is SelectScreenState.AppList -> {
                AppListContent(
                    helper = helper,
                    onSelectApp = { app ->
                        shortcuts = helper.getShortcutsForApp(app.packageName)
                        screenState = SelectScreenState.AppShortcuts(app)
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is SelectScreenState.AppShortcuts -> {
                val app = (screenState as SelectScreenState.AppShortcuts).app
                AppShortcutsContent(
                    app = app,
                    shortcuts = shortcuts,
                    onSelectApp = { onSelectApp(app) },
                    onSelectShortcut = onSelectShortcut,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

/**
 * スロット編集画面（編集モードでスロットタップ時）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlotEditScreen(
    currentShortcut: ShortcutItem?,
    unplacedShortcuts: List<ShortcutItem>,
    placedShortcuts: List<ShortcutItem>,
    onSelectUnplaced: (ShortcutItem) -> Unit,
    onSelectPlaced: (ShortcutItem) -> Unit,
    onSelectApp: (AppInfo) -> Unit,
    onSelectShortcut: (ShortcutData) -> Unit,
    onSelectInternal: (InternalFeature) -> Unit,
    onClear: () -> Unit,
    onDeleteRow: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val helper = remember { ShortcutHelper(context) }

    var screenState by remember { mutableStateOf<SelectScreenState>(SelectScreenState.Main) }
    var shortcuts by remember { mutableStateOf<List<ShortcutData>>(emptyList()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (screenState) {
                            is SelectScreenState.Main -> "このスロットに配置"
                            is SelectScreenState.AppList -> "アプリ一覧"
                            is SelectScreenState.AppShortcuts -> (screenState as SelectScreenState.AppShortcuts).app.label
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when (screenState) {
                            is SelectScreenState.Main -> onBack()
                            is SelectScreenState.AppList -> screenState = SelectScreenState.Main
                            is SelectScreenState.AppShortcuts -> screenState = SelectScreenState.AppList
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { paddingValues ->
        when (screenState) {
            is SelectScreenState.Main -> {
                SlotEditMainContent(
                    currentShortcut = currentShortcut,
                    unplacedShortcuts = unplacedShortcuts,
                    placedShortcuts = placedShortcuts,
                    onSelectUnplaced = onSelectUnplaced,
                    onSelectPlaced = onSelectPlaced,
                    onSelectInternal = onSelectInternal,
                    onGoToAppList = { screenState = SelectScreenState.AppList },
                    onClear = onClear,
                    onDeleteRow = onDeleteRow,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is SelectScreenState.AppList -> {
                AppListContent(
                    helper = helper,
                    onSelectApp = { app ->
                        shortcuts = helper.getShortcutsForApp(app.packageName)
                        screenState = SelectScreenState.AppShortcuts(app)
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is SelectScreenState.AppShortcuts -> {
                val app = (screenState as SelectScreenState.AppShortcuts).app
                AppShortcutsContent(
                    app = app,
                    shortcuts = shortcuts,
                    onSelectApp = { onSelectApp(app) },
                    onSelectShortcut = onSelectShortcut,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

// ============ 共通コンポーネント ============

@Composable
private fun MainSelectContent(
    unplacedShortcuts: List<ShortcutItem>,
    onSelectUnplaced: (ShortcutItem) -> Unit,
    onSelectInternal: (InternalFeature) -> Unit,
    onGoToAppList: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // アプリ一覧へ
        item {
            NavigationCard(
                icon = "📱",
                text = "アプリ一覧から選ぶ",
                onClick = onGoToAppList
            )
        }

        // アプリ内機能
        item {
            SectionHeader(text = "アプリ内機能")
        }
        items(internalFeatures) { feature ->
            InternalFeatureCard(
                feature = feature,
                onClick = { onSelectInternal(feature) }
            )
        }

        // 未配置ショートカット
        if (unplacedShortcuts.isNotEmpty()) {
            item {
                SectionHeader(text = "未配置ショートカット")
            }
            items(unplacedShortcuts) { shortcut ->
                ShortcutCard(
                    shortcut = shortcut,
                    subtitle = "タップで配置",
                    backgroundColor = Color(0xFFF5F5F5),
                    onClick = { onSelectUnplaced(shortcut) }
                )
            }
        }
    }
}

@Composable
private fun SlotEditMainContent(
    currentShortcut: ShortcutItem?,
    unplacedShortcuts: List<ShortcutItem>,
    placedShortcuts: List<ShortcutItem>,
    onSelectUnplaced: (ShortcutItem) -> Unit,
    onSelectPlaced: (ShortcutItem) -> Unit,
    onSelectInternal: (InternalFeature) -> Unit,
    onGoToAppList: () -> Unit,
    onClear: () -> Unit,
    onDeleteRow: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // アプリ一覧へ
        item {
            NavigationCard(
                icon = "📱",
                text = "アプリ一覧から選ぶ",
                onClick = onGoToAppList
            )
        }

        // アプリ内機能
        item {
            SectionHeader(text = "アプリ内機能")
        }
        items(internalFeatures) { feature ->
            InternalFeatureCard(
                feature = feature,
                onClick = { onSelectInternal(feature) }
            )
        }

        // 未配置ショートカット
        if (unplacedShortcuts.isNotEmpty()) {
            item {
                SectionHeader(text = "未配置ショートカット")
            }
            items(unplacedShortcuts) { shortcut ->
                ShortcutCard(
                    shortcut = shortcut,
                    subtitle = "タップで配置",
                    backgroundColor = Color(0xFFF5F5F5),
                    onClick = { onSelectUnplaced(shortcut) }
                )
            }
        }

        // 配置済みと入れ替え
        if (placedShortcuts.isNotEmpty()) {
            item {
                SectionHeader(text = "配置済みと入れ替え")
            }
            items(placedShortcuts) { shortcut ->
                ShortcutCard(
                    shortcut = shortcut,
                    subtitle = "タップで入れ替え",
                    backgroundColor = Color(0xFFFFF3E0),
                    onClick = { onSelectPlaced(shortcut) }
                )
            }
        }

        // スロットを空にする
        if (currentShortcut != null && currentShortcut.type != ShortcutType.EMPTY) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                ActionCard(
                    text = "このスロットを空にする",
                    color = Color(0xFFE53935),
                    onClick = onClear
                )
            }
        }

        // この行を削除
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
        item {
            ActionCard(
                text = "この行全体を削除する",
                color = Color(0xFF757575),
                onClick = onDeleteRow
            )
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

// 優先表示するアプリのパッケージ名（完全一致または先頭一致）
// 上から順に表示される
private val priorityAppPackages = listOf(
    // 1. 電話・連絡先（最重要）
    "com.android.dialer",
    "com.google.android.dialer",
    "com.android.contacts",
    "com.google.android.contacts",

    // 2. LINE（高齢者に人気）
    "jp.naver.line.android",

    // 3. SMS・メール
    "com.android.messaging",
    "com.google.android.apps.messaging",
    "com.google.android.gm",

    // 4. カメラ
    "com.android.camera",
    "com.android.camera2",
    "com.google.android.GoogleCamera",

    // 5. 写真・ギャラリー
    "com.google.android.apps.photos",
    "com.google.android.apps.nbu.files",
    "com.amazon.clouddrive.photos",

    // 6. 地図
    "com.google.android.apps.maps",

    // 7. ブラウザ・検索
    "com.android.chrome",
    "com.google.android.googlequicksearchbox",
    "com.microsoft.bing",

    // 8. 便利ツール
    "com.google.android.calendar",
    "com.google.android.calculator",

    // 9. 動画・SNS
    "com.google.android.youtube",
    "com.instagram.android",
    "com.twitter.android",

    // 10. ショッピング・その他
    "com.amazon.mShop.android.shopping",
    "com.android.vending",
    "com.google.android.apps.bard",
)

private fun isPriorityApp(packageName: String): Boolean {
    return priorityAppPackages.any {
        packageName == it || packageName.startsWith("$it.")
    }
}

/**
 * 優先アプリの配列内でのインデックスを取得（ソート用）
 * 一致しない場合はInt.MAX_VALUEを返す
 */
private fun getPriorityIndex(packageName: String): Int {
    val index = priorityAppPackages.indexOfFirst {
        packageName == it || packageName.startsWith("$it.")
    }
    return if (index >= 0) index else Int.MAX_VALUE
}

@Composable
private fun AppListContent(
    helper: ShortcutHelper,
    onSelectApp: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val apps = remember { helper.getInstalledApps() }
    var searchQuery by remember { mutableStateOf("") }

    // 優先アプリを上に（配列の登録順）、それ以外はそのまま
    val sortedApps = remember(apps) {
        val priority = apps.filter { isPriorityApp(it.packageName) }
            .sortedBy { getPriorityIndex(it.packageName) }
        val others = apps.filter { !isPriorityApp(it.packageName) }
        priority + others
    }

    val filteredApps = remember(searchQuery, sortedApps) {
        if (searchQuery.isBlank()) {
            sortedApps
        } else {
            sortedApps.filter {
                it.label.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // 検索フィールド
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("アプリ名で検索") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "クリア")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Text(
            text = "${filteredApps.size}件のアプリ",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredApps) { app ->
                AppCard(
                    app = app,
                    onClick = { onSelectApp(app) }
                )
            }
        }
    }
}

@Composable
private fun AppShortcutsContent(
    app: AppInfo,
    shortcuts: List<ShortcutData>,
    onSelectApp: () -> Unit,
    onSelectShortcut: (ShortcutData) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // アプリ起動
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectApp() },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    app.icon?.let { DrawableImage(it, 48) }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "アプリを起動",
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

        // ショートカット一覧
        if (shortcuts.isEmpty()) {
            item {
                Text(
                    text = "このアプリにはショートカットがありません",
                    modifier = Modifier.padding(16.dp),
                    color = Color.Gray
                )
            }
        } else {
            item {
                SectionHeader(text = "ショートカット (${shortcuts.size}件)")
            }
            items(shortcuts) { shortcut ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectShortcut(shortcut) },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
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
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============ UI部品 ============

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = Color.Gray,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun NavigationCard(
    icon: String,
    text: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "→",
                fontSize = 20.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun ShortcutCard(
    shortcut: ShortcutItem,
    subtitle: String,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (shortcut.type) {
                    ShortcutType.APP -> "📱"
                    ShortcutType.PHONE -> "📞"
                    ShortcutType.SMS -> "💬"
                    ShortcutType.INTENT -> "🔗"
                    ShortcutType.CALENDAR -> "📅"
                    ShortcutType.MEMO -> "📝"
                    ShortcutType.SETTINGS -> "⚙️"
                    ShortcutType.EMPTY -> ""
                },
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = shortcut.label,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFFFF9800)
                )
            }
        }
    }
}

@Composable
private fun InternalFeatureCard(
    feature: InternalFeature,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = feature.icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = feature.label,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// 開発時のみパッケージ名を表示
private const val SHOW_PACKAGE_NAME = true

@Composable
private fun AppCard(
    app: AppInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            app.icon?.let { DrawableImage(it, 48) }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = app.label,
                    fontSize = 18.sp
                )
                if (SHOW_PACKAGE_NAME) {
                    Text(
                        text = app.packageName,
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionCard(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
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
