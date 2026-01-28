package com.example.simplecustomlauncher.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.simplecustomlauncher.AppInfo
import com.example.simplecustomlauncher.ShortcutData
import com.example.simplecustomlauncher.ShortcutHelper
import com.example.simplecustomlauncher.data.ShortcutItem
import com.example.simplecustomlauncher.data.ShortcutType
import com.example.simplecustomlauncher.R
import com.example.simplecustomlauncher.ui.components.ColumnOptionCard
import com.example.simplecustomlauncher.ui.components.ContactTypeDialog
import com.example.simplecustomlauncher.ui.components.CustomContentDialog
import com.example.simplecustomlauncher.ui.components.LargeDangerConfirmDialog
import com.example.simplecustomlauncher.ui.components.PremiumFeatureDialog
import com.example.simplecustomlauncher.ui.components.RowDeleteConfirmDialog

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
    val labelResId: Int,
    val icon: String
)

val internalFeatures = listOf(
    InternalFeature(ShortcutType.CALENDAR, R.string.shortcut_type_calendar, "📅"),
    InternalFeature(ShortcutType.MEMO, R.string.shortcut_type_memo, "📝"),
    InternalFeature(ShortcutType.DIALER, R.string.shortcut_type_phone, "📞"),
    InternalFeature(ShortcutType.ALL_APPS, R.string.shortcut_type_all_apps, "📱"),
    InternalFeature(ShortcutType.DATE_DISPLAY, R.string.shortcut_type_date, ""),
    InternalFeature(ShortcutType.TIME_DISPLAY, R.string.shortcut_type_time, "")
)

/**
 * 色セット（背景色 + 文字色）のパレット（プレミアム機能用）
 */
data class ColorSet(
    val backgroundColor: String,
    val textColor: String,
    val name: String
)

val slotColorPalette = listOf(
    ColorSet("#E57373", "#FFFFFF", "Red"),      // 赤 + 白文字
    ColorSet("#FFB74D", "#000000", "Orange"),   // オレンジ + 黒文字
    ColorSet("#FFF176", "#000000", "Yellow"),   // 黄 + 黒文字
    ColorSet("#81C784", "#000000", "Green"),    // 緑 + 黒文字
    ColorSet("#4FC3F7", "#000000", "Light Blue"), // 水色 + 黒文字
    ColorSet("#64B5F6", "#FFFFFF", "Blue"),     // 青 + 白文字
    ColorSet("#BA68C8", "#FFFFFF", "Purple"),   // 紫 + 白文字
    ColorSet("#F06292", "#FFFFFF", "Pink")      // ピンク + 白文字
)

/**
 * 連絡先情報
 */
data class ContactInfo(
    val name: String,
    val phoneNumber: String
)

/**
 * 連絡先ピッカーの状態
 */
data class ContactPickerState(
    val selectedContact: ContactInfo?,
    val showDialog: Boolean,
    val startPicker: () -> Unit,
    val dismissDialog: () -> Unit
)

/**
 * 連絡先ピッカーのカスタムフック
 */
@Composable
fun rememberContactPicker(
    onContactSelected: (ContactInfo) -> Unit
): ContactPickerState {
    val context = LocalContext.current
    var selectedContact by remember { mutableStateOf<ContactInfo?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri ->
        uri?.let {
            val contactInfo = getContactInfo(context, it)
            if (contactInfo != null) {
                selectedContact = contactInfo
                showDialog = true
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            contactPickerLauncher.launch(null)
        }
    }

    val startPicker: () -> Unit = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            == PackageManager.PERMISSION_GRANTED) {
            contactPickerLauncher.launch(null)
        } else {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    return ContactPickerState(
        selectedContact = selectedContact,
        showDialog = showDialog,
        startPicker = startPicker,
        dismissDialog = { showDialog = false }
    )
}

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
    onSelectContact: (name: String, phoneNumber: String, type: ShortcutType) -> Unit = { _, _, _ -> },
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val helper = remember { ShortcutHelper(context) }

    var screenState by remember { mutableStateOf<SelectScreenState>(SelectScreenState.Main) }
    var shortcuts by remember { mutableStateOf<List<ShortcutData>>(emptyList()) }

    // 連絡先ピッカー（カスタムフック使用）
    val contactPicker = rememberContactPicker { /* not used here */ }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (screenState) {
                            is SelectScreenState.Main -> stringResource(R.string.add_shortcut)
                            is SelectScreenState.AppList -> stringResource(R.string.app_list)
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
                    onContactPicker = contactPicker.startPicker,
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

    // 連絡先タイプ選択ダイアログ
    if (contactPicker.showDialog) {
        contactPicker.selectedContact?.let { contact ->
            ContactTypeDialog(
                contactName = contact.name,
                onSelectPhone = {
                    onSelectContact(contact.name, contact.phoneNumber, ShortcutType.PHONE)
                    contactPicker.dismissDialog()
                },
                onSelectSms = {
                    onSelectContact(contact.name, contact.phoneNumber, ShortcutType.SMS)
                    contactPicker.dismissDialog()
                },
                onDismiss = { contactPicker.dismissDialog() }
            )
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
    currentColumns: Int,
    currentTextOnly: Boolean = false,
    currentBackgroundColor: String? = null,
    currentTextColor: String? = null,
    unplacedShortcuts: List<ShortcutItem>,
    placedShortcuts: List<ShortcutItem>,
    onSelectUnplaced: (ShortcutItem) -> Unit,
    onSelectPlaced: (ShortcutItem) -> Unit,
    onSelectApp: (AppInfo) -> Unit,
    onSelectShortcut: (ShortcutData) -> Unit,
    onSelectInternal: (InternalFeature) -> Unit,
    onSelectContact: (name: String, phoneNumber: String, type: ShortcutType) -> Unit = { _, _, _ -> },
    onClear: () -> Unit,
    onChangeColumns: (Int) -> Unit,
    onChangeTextOnly: (Boolean) -> Unit = {},
    onChangeColors: (backgroundColor: String?, textColor: String?) -> Unit = { _, _ -> },
    onDeleteRow: () -> Unit,
    isPremium: Boolean = false,
    onWatchAd: () -> Unit = {},
    onPurchase: () -> Unit = {},
    formattedPrice: String? = null,
    isAdReady: Boolean = true,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val helper = remember { ShortcutHelper(context) }

    // 連絡先ピッカー（カスタムフック使用）
    val contactPicker = rememberContactPicker { /* not used here */ }
    var showColumnsDialog by remember { mutableStateOf(false) }
    var showDeleteRowDialog by remember { mutableStateOf(false) }
    var showClearSlotDialog by remember { mutableStateOf(false) }
    var showPremiumDialog by remember { mutableStateOf(false) }

    var screenState by remember { mutableStateOf<SelectScreenState>(SelectScreenState.Main) }
    var shortcuts by remember { mutableStateOf<List<ShortcutData>>(emptyList()) }

    // 行削除確認ダイアログ
    if (showDeleteRowDialog) {
        RowDeleteConfirmDialog(
            onConfirm = {
                showDeleteRowDialog = false
                onDeleteRow()
            },
            onDismiss = { showDeleteRowDialog = false }
        )
    }

    // スロットを空にする確認ダイアログ
    if (showClearSlotDialog) {
        LargeDangerConfirmDialog(
            title = stringResource(R.string.clear_slot),
            message = stringResource(R.string.clear_slot_warning),
            confirmText = stringResource(R.string.delete_action),
            onConfirm = {
                showClearSlotDialog = false
                onClear()
            },
            onDismiss = { showClearSlotDialog = false }
        )
    }

    // プレミアム機能ダイアログ
    if (showPremiumDialog) {
        PremiumFeatureDialog(
            description = stringResource(R.string.background_color_premium),
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (screenState) {
                            is SelectScreenState.Main -> stringResource(R.string.place_in_slot)
                            is SelectScreenState.AppList -> stringResource(R.string.app_list)
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        when (screenState) {
            is SelectScreenState.Main -> {
                SlotEditMainContent(
                    currentShortcut = currentShortcut,
                    currentColumns = currentColumns,
                    currentTextOnly = currentTextOnly,
                    currentBackgroundColor = currentBackgroundColor,
                    currentTextColor = currentTextColor,
                    unplacedShortcuts = unplacedShortcuts,
                    placedShortcuts = placedShortcuts,
                    onSelectUnplaced = onSelectUnplaced,
                    onSelectPlaced = onSelectPlaced,
                    onSelectInternal = onSelectInternal,
                    onGoToAppList = { screenState = SelectScreenState.AppList },
                    onContactPicker = contactPicker.startPicker,
                    onClear = { showClearSlotDialog = true },
                    onShowColumnsDialog = { showColumnsDialog = true },
                    onChangeTextOnly = onChangeTextOnly,
                    onChangeColors = onChangeColors,
                    onDeleteRow = { showDeleteRowDialog = true },
                    isPremium = isPremium,
                    onPremiumRequired = { showPremiumDialog = true },
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

    // 連絡先タイプ選択ダイアログ
    if (contactPicker.showDialog) {
        contactPicker.selectedContact?.let { contact ->
            ContactTypeDialog(
                contactName = contact.name,
                onSelectPhone = {
                    onSelectContact(contact.name, contact.phoneNumber, ShortcutType.PHONE)
                    contactPicker.dismissDialog()
                },
                onSelectSms = {
                    onSelectContact(contact.name, contact.phoneNumber, ShortcutType.SMS)
                    contactPicker.dismissDialog()
                },
                onDismiss = { contactPicker.dismissDialog() }
            )
        }
    }

    // 分割数変更ダイアログ
    if (showColumnsDialog) {
        CustomContentDialog(
            title = stringResource(R.string.change_column_count),
            onDismiss = { showColumnsDialog = false }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.select_row_column_count),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 各分割数オプション
                val descriptions = listOf(
                    R.string.column_1_desc,
                    R.string.column_2_desc,
                    R.string.column_3_desc
                )
                listOf(1, 2, 3).forEachIndexed { index, columns ->
                    ColumnOptionCard(
                        columns = columns,
                        description = stringResource(descriptions[index]),
                        isSelected = columns == currentColumns,
                        onClick = {
                            onChangeColumns(columns)
                            showColumnsDialog = false
                        }
                    )
                }

                Text(
                    text = stringResource(R.string.column_reduce_warning),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ============ 共通コンポーネント ============

/**
 * 共通のショートカット選択コンテンツ（LazyListScope拡張）
 */
private fun LazyListScope.commonSelectContent(
    unplacedShortcuts: List<ShortcutItem>,
    onSelectUnplaced: (ShortcutItem) -> Unit,
    onSelectInternal: (InternalFeature) -> Unit,
    onGoToAppList: () -> Unit,
    onContactPicker: () -> Unit
) {
    // アプリ一覧へ
    item {
        NavigationCard(
            icon = {
                Icon(
                    Icons.Default.Apps,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            text = stringResource(R.string.select_from_app_list),
            onClick = onGoToAppList
        )
    }

    // 連絡先から追加
    item {
        NavigationCard(
            icon = {
                val context = LocalContext.current
                val contactsIcon = remember {
                    // 連絡先アプリのアイコンを取得
                    val pm = context.packageManager
                    val contactsPackages = listOf(
                        "com.google.android.contacts",
                        "com.android.contacts"
                    )
                    contactsPackages.firstNotNullOfOrNull { pkg ->
                        try {
                            pm.getApplicationIcon(pkg)
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                if (contactsIcon != null) {
                    Image(
                        bitmap = contactsIcon.toBitmap(64, 64).asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            },
            text = stringResource(R.string.add_from_contact),
            onClick = onContactPicker
        )
    }

    // アプリ内機能（3列グリッド）
    item {
        SectionHeader(text = stringResource(R.string.internal_features))
    }
    item {
        InternalFeaturesGrid(
            features = internalFeatures,
            onSelectInternal = onSelectInternal
        )
    }

    // 未配置ショートカット
    if (unplacedShortcuts.isNotEmpty()) {
        item {
            SectionHeader(text = stringResource(R.string.unplaced_shortcuts))
        }
        items(unplacedShortcuts) { shortcut ->
            ShortcutCard(
                shortcut = shortcut,
                subtitleResId = R.string.tap_to_place,
                backgroundColor = MaterialTheme.colorScheme.surface,
                onClick = { onSelectUnplaced(shortcut) }
            )
        }
    }
}

@Composable
private fun MainSelectContent(
    unplacedShortcuts: List<ShortcutItem>,
    onSelectUnplaced: (ShortcutItem) -> Unit,
    onSelectInternal: (InternalFeature) -> Unit,
    onGoToAppList: () -> Unit,
    onContactPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        commonSelectContent(
            unplacedShortcuts = unplacedShortcuts,
            onSelectUnplaced = onSelectUnplaced,
            onSelectInternal = onSelectInternal,
            onGoToAppList = onGoToAppList,
            onContactPicker = onContactPicker
        )
    }
}

@Composable
private fun SlotEditMainContent(
    currentShortcut: ShortcutItem?,
    currentColumns: Int,
    currentTextOnly: Boolean = false,
    currentBackgroundColor: String? = null,
    currentTextColor: String? = null,
    unplacedShortcuts: List<ShortcutItem>,
    placedShortcuts: List<ShortcutItem>,
    onSelectUnplaced: (ShortcutItem) -> Unit,
    onSelectPlaced: (ShortcutItem) -> Unit,
    onSelectInternal: (InternalFeature) -> Unit,
    onGoToAppList: () -> Unit,
    onContactPicker: () -> Unit,
    onClear: () -> Unit,
    onShowColumnsDialog: () -> Unit,
    onChangeTextOnly: (Boolean) -> Unit = {},
    onChangeColors: (backgroundColor: String?, textColor: String?) -> Unit = { _, _ -> },
    onDeleteRow: () -> Unit,
    isPremium: Boolean = false,
    onPremiumRequired: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 共通コンテンツ
        commonSelectContent(
            unplacedShortcuts = unplacedShortcuts,
            onSelectUnplaced = onSelectUnplaced,
            onSelectInternal = onSelectInternal,
            onGoToAppList = onGoToAppList,
            onContactPicker = onContactPicker
        )

        // 配置済みと入れ替え
        if (placedShortcuts.isNotEmpty()) {
            item {
                SectionHeader(text = stringResource(R.string.swap_with_placed))
            }
            items(placedShortcuts) { shortcut ->
                ShortcutCard(
                    shortcut = shortcut,
                    subtitleResId = R.string.tap_to_swap,
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { onSelectPlaced(shortcut) }
                )
            }
        }

        // --- 行操作セクション ---
        item {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 表示モード切替（文字のみ/アイコン+文字）
        item {
            DisplayModeCard(
                textOnly = currentTextOnly,
                onToggle = { onChangeTextOnly(!currentTextOnly) }
            )
        }

        // 色セット選択（プレミアム機能）
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
        item {
            ColorSetCard(
                currentBackgroundColor = currentBackgroundColor,
                currentTextColor = currentTextColor,
                isPremium = isPremium,
                onSelectColors = onChangeColors,
                onPremiumRequired = onPremiumRequired
            )
        }

        // この行の分割数を変更
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
        item {
            ActionCard(
                text = stringResource(R.string.current_column_count, currentColumns),
                color = MaterialTheme.colorScheme.primary,
                onClick = onShowColumnsDialog
            )
        }

        // スロットを空にする
        if (currentShortcut != null && currentShortcut.type != ShortcutType.EMPTY) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            item {
                ActionCard(
                    text = stringResource(R.string.clear_slot),
                    color = MaterialTheme.colorScheme.error,
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
                text = stringResource(R.string.delete_row),
                color = MaterialTheme.colorScheme.error,
                onClick = onDeleteRow
            )
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

// 優先表示するアプリのパッケージ名（完全一致または先頭一致）
// 上から順に表示される
internal val priorityAppPackages = listOf(
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

internal fun isPriorityApp(packageName: String): Boolean {
    return priorityAppPackages.any {
        packageName == it || packageName.startsWith("$it.")
    }
}

/**
 * 優先アプリの配列内でのインデックスを取得（ソート用）
 * 一致しない場合はInt.MAX_VALUEを返す
 */
internal fun getPriorityIndex(packageName: String): Int {
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
            placeholder = { Text(stringResource(R.string.search_app_name)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
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

        Text(
            text = stringResource(R.string.app_count, filteredApps.size),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                SectionHeader(text = stringResource(R.string.shortcut_count, shortcuts.size))
            }
            items(shortcuts) { shortcut ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectShortcut(shortcut) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun NavigationCard(
    icon: @Composable () -> Unit,
    text: String,
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
            icon()
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ShortcutCard(
    shortcut: ShortcutItem,
    subtitleResId: Int,
    backgroundColor: Color,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val shortcutHelper = remember { ShortcutHelper(context) }

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
            // DATE_DISPLAY/TIME_DISPLAYはラベルのみ大きく表示
            if (shortcut.type == ShortcutType.DATE_DISPLAY || shortcut.type == ShortcutType.TIME_DISPLAY) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = getLocalizedLabel(shortcut),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        color = contentColor
                    )
                    Text(
                        text = stringResource(subtitleResId),
                        fontSize = 12.sp,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                }
            } else {
                // APP/INTENTは実際のアイコン、MEMO/CALENDARはDrawable、それ以外は絵文字
                when (shortcut.type) {
                    ShortcutType.APP, ShortcutType.INTENT -> {
                        val appIcon = remember(shortcut.packageName) {
                            shortcut.packageName?.let { shortcutHelper.getAppIcon(it) }
                        }
                        if (appIcon != null) {
                            Image(
                                bitmap = appIcon.toBitmap(64, 64).asImageBitmap(),
                                contentDescription = shortcut.label,
                                modifier = Modifier.size(32.dp)
                            )
                        } else {
                            Icon(
                                Icons.Default.Apps,
                                contentDescription = shortcut.label,
                                modifier = Modifier.size(32.dp),
                                tint = contentColor
                            )
                        }
                    }
                    ShortcutType.MEMO -> {
                        val memoIcon = remember {
                            ContextCompat.getDrawable(context, R.drawable.ic_memo)
                        }
                        if (memoIcon != null) {
                            val bitmap = remember(memoIcon) { memoIcon.toBitmap(64, 64) }
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = shortcut.label,
                                modifier = Modifier.size(32.dp),
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(contentColor)
                            )
                        }
                    }
                    ShortcutType.CALENDAR -> {
                        val calendarIcon = remember {
                            ContextCompat.getDrawable(context, R.drawable.ic_calendar)
                        }
                        if (calendarIcon != null) {
                            val bitmap = remember(calendarIcon) { calendarIcon.toBitmap(64, 64) }
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = shortcut.label,
                                modifier = Modifier.size(32.dp),
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(contentColor)
                            )
                        }
                    }
                    ShortcutType.ALL_APPS -> {
                        Icon(
                            Icons.Default.Apps,
                            contentDescription = shortcut.label,
                            modifier = Modifier.size(32.dp),
                            tint = contentColor
                        )
                    }
                    ShortcutType.DIALER -> {
                        val dialerIcon = remember {
                            ContextCompat.getDrawable(context, R.drawable.ic_phone_keypad)
                        }
                        if (dialerIcon != null) {
                            val bitmap = remember(dialerIcon) { dialerIcon.toBitmap(64, 64) }
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = shortcut.label,
                                modifier = Modifier.size(32.dp),
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(contentColor)
                            )
                        }
                    }
                    ShortcutType.PHONE -> {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = shortcut.label,
                            modifier = Modifier.size(32.dp),
                            tint = contentColor
                        )
                    }
                    ShortcutType.SMS -> {
                        val smsIcon = remember {
                            val pm = context.packageManager
                            listOf(
                                "com.google.android.apps.messaging",
                                "com.android.mms",
                                "com.samsung.android.messaging"
                            ).firstNotNullOfOrNull { pkg ->
                                try { pm.getApplicationIcon(pkg) } catch (e: Exception) { null }
                            }
                        }
                        if (smsIcon != null) {
                            val bitmap = remember(smsIcon) { smsIcon.toBitmap(64, 64) }
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = shortcut.label,
                                modifier = Modifier.size(32.dp)
                            )
                        } else {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = shortcut.label,
                                modifier = Modifier.size(32.dp),
                                tint = contentColor
                            )
                        }
                    }
                    ShortcutType.SETTINGS -> {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = shortcut.label,
                            modifier = Modifier.size(32.dp),
                            tint = contentColor
                        )
                    }
                    else -> {
                        // EMPTY or unknown
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = getLocalizedLabel(shortcut),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = contentColor
                    )
                    Text(
                        text = stringResource(subtitleResId),
                        fontSize = 12.sp,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun InternalFeaturesGrid(
    features: List<InternalFeature>,
    onSelectInternal: (InternalFeature) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 縦1列に並べる
        features.forEach { feature ->
            InternalFeatureGridItem(
                feature = feature,
                onClick = { onSelectInternal(feature) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun InternalFeatureGridItem(
    feature: InternalFeature,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val containerColor = MaterialTheme.colorScheme.surfaceVariant
    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    val label = stringResource(feature.labelResId)

    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (feature.icon.isEmpty()) {
                // アイコンなし（日付・時刻）：ラベルのみ大きく表示
                Text(
                    text = label,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                // アイコン表示
                when (feature.type) {
                    ShortcutType.DIALER -> {
                        val dialerIcon = remember {
                            ContextCompat.getDrawable(context, R.drawable.ic_phone_keypad)
                        }
                        if (dialerIcon != null) {
                            val bitmap = remember(dialerIcon) { dialerIcon.toBitmap(64, 64) }
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = label,
                                modifier = Modifier.size(32.dp),
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(contentColor)
                            )
                        }
                    }
                    ShortcutType.MEMO -> {
                        val memoIcon = remember {
                            ContextCompat.getDrawable(context, R.drawable.ic_memo)
                        }
                        if (memoIcon != null) {
                            val bitmap = remember(memoIcon) { memoIcon.toBitmap(64, 64) }
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = label,
                                modifier = Modifier.size(32.dp),
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(contentColor)
                            )
                        }
                    }
                    ShortcutType.CALENDAR -> {
                        val calendarIcon = remember {
                            ContextCompat.getDrawable(context, R.drawable.ic_calendar)
                        }
                        if (calendarIcon != null) {
                            val bitmap = remember(calendarIcon) { calendarIcon.toBitmap(64, 64) }
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = label,
                                modifier = Modifier.size(32.dp),
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(contentColor)
                            )
                        }
                    }
                    ShortcutType.ALL_APPS -> {
                        Icon(
                            Icons.Default.Apps,
                            contentDescription = label,
                            modifier = Modifier.size(32.dp),
                            tint = contentColor
                        )
                    }
                    else -> {}
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// 開発時のみパッケージ名を表示（本番はfalse）
private const val SHOW_PACKAGE_NAME = false

@Composable
private fun AppCard(
    app: AppInfo,
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun DisplayModeCard(
    textOnly: Boolean,
    onToggle: () -> Unit
) {
    val containerColor = MaterialTheme.colorScheme.surfaceVariant
    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.display_mode),
                fontSize = 14.sp,
                color = contentColor.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { if (textOnly) onToggle() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !textOnly,
                        onClick = { if (textOnly) onToggle() }
                    )
                    Text(
                        text = stringResource(R.string.display_mode_icon_text),
                        fontSize = 14.sp,
                        color = contentColor
                    )
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { if (!textOnly) onToggle() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = textOnly,
                        onClick = { if (!textOnly) onToggle() }
                    )
                    Text(
                        text = stringResource(R.string.display_mode_text_only),
                        fontSize = 14.sp,
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorSetCard(
    currentBackgroundColor: String?,
    currentTextColor: String?,
    isPremium: Boolean,
    onSelectColors: (backgroundColor: String?, textColor: String?) -> Unit,
    onPremiumRequired: () -> Unit = {}
) {
    val containerColor = if (isPremium) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isPremium) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!isPremium) {
                    Modifier.clickable(onClick = onPremiumRequired)
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.background_color),
                    fontSize = 16.sp,
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

            Spacer(modifier = Modifier.height(12.dp))

            if (isPremium) {
                // 色セットパレット（2行に分けて表示）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // デフォルト（リセット）
                    ColorSetButton(
                        colorSet = null,
                        isSelected = currentBackgroundColor == null,
                        onClick = { onSelectColors(null, null) }
                    )
                    // カラーパレット前半
                    slotColorPalette.take(4).forEach { colorSet ->
                        ColorSetButton(
                            colorSet = colorSet,
                            isSelected = currentBackgroundColor == colorSet.backgroundColor,
                            onClick = { onSelectColors(colorSet.backgroundColor, colorSet.textColor) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // カラーパレット後半
                    slotColorPalette.drop(4).forEach { colorSet ->
                        ColorSetButton(
                            colorSet = colorSet,
                            isSelected = currentBackgroundColor == colorSet.backgroundColor,
                            onClick = { onSelectColors(colorSet.backgroundColor, colorSet.textColor) }
                        )
                    }
                    // 空白で揃える
                    Spacer(modifier = Modifier.size(44.dp))
                }
            } else {
                // プレミアム未購入時
                Text(
                    text = stringResource(R.string.background_color_premium),
                    fontSize = 14.sp,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun ColorSetButton(
    colorSet: ColorSet?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (colorSet != null) {
        Color(android.graphics.Color.parseColor(colorSet.backgroundColor))
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val txtColor = if (colorSet != null) {
        Color(android.graphics.Color.parseColor(colorSet.textColor))
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (colorSet == null) "×" else "Aa",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = txtColor
        )
        // 選択中は赤丸表示
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
                    .size(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.Red)
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

/**
 * 連絡先URIから名前と電話番号を取得
 */
private fun getContactInfo(context: android.content.Context, contactUri: android.net.Uri): ContactInfo? {
    var name: String? = null
    var phoneNumber: String? = null

    // 連絡先の名前を取得
    context.contentResolver.query(
        contactUri,
        arrayOf(ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts._ID),
        null, null, null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))
            val contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))

            // 電話番号を取得
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(contactId),
                null
            )?.use { phoneCursor ->
                if (phoneCursor.moveToFirst()) {
                    phoneNumber = phoneCursor.getString(
                        phoneCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    )
                }
            }
        }
    }

    return if (name != null && phoneNumber != null) {
        ContactInfo(name!!, phoneNumber!!)
    } else {
        null
    }
}
