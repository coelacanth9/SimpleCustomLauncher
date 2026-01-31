package com.example.simplecustomlauncher.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
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
import com.example.simplecustomlauncher.AppInfo
import com.example.simplecustomlauncher.R
import com.example.simplecustomlauncher.ShortcutHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * すべてのアプリ画面
 * アプリ一覧を表示し、タップで直接起動
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllAppsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val helper = remember { ShortcutHelper(context) }
    var apps by remember { mutableStateOf<List<AppInfo>?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // バックグラウンドでアプリ一覧を取得
    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) {
            helper.getInstalledApps()
        }
    }

    // 優先アプリを上に、それ以外はそのまま
    val sortedApps = remember(apps) {
        apps?.let { list ->
            val priority = list.filter { isPriorityApp(it.packageName) }
                .sortedBy { getPriorityIndex(it.packageName) }
            val others = list.filter { !isPriorityApp(it.packageName) }
            priority + others
        }
    }

    val filteredApps = remember(searchQuery, sortedApps) {
        sortedApps?.let { sorted ->
            if (searchQuery.isBlank()) {
                sorted
            } else {
                sorted.filter {
                    it.label.contains(searchQuery, ignoreCase = true)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.shortcut_type_all_apps),
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
        if (filteredApps == null) {
            // 読み込み中
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
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
                        var menuExpanded by remember { mutableStateOf(false) }
                        val isSelf = app.packageName == context.packageName

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    helper.startApp(app.packageName)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                app.icon?.let { icon ->
                                    val bitmap = remember(icon) { icon.toBitmap(48, 48) }
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = app.label,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    text = app.label,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Box {
                                    IconButton(onClick = { menuExpanded = true }) {
                                        Icon(
                                            Icons.Default.MoreVert,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.app_info)) },
                                            onClick = {
                                                menuExpanded = false
                                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                    data = Uri.parse("package:${app.packageName}")
                                                }
                                                context.startActivity(intent)
                                            }
                                        )
                                        if (!isSelf) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.uninstall)) },
                                                onClick = {
                                                    menuExpanded = false
                                                    val intent = Intent(Intent.ACTION_DELETE).apply {
                                                        data = Uri.parse("package:${app.packageName}")
                                                    }
                                                    context.startActivity(intent)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
