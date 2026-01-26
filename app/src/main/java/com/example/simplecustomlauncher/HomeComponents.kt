package com.example.simplecustomlauncher

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.simplecustomlauncher.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun HomeHeader(
    context: android.content.Context,
    isEditMode: Boolean = false,
    onEditDone: () -> Unit = {},
    onAddRow: () -> Unit = {},
    onAppSettings: () -> Unit = {}
) {
    // 現在時刻を保持するステート
    var currentDateTime by remember { mutableStateOf(LocalDateTime.now()) }

    // 祝日取得用のリポジトリ
    val repository = remember { CalendarRepository(context) }

    // 1秒ごとに時刻を更新する処理
    LaunchedEffect(Unit) {
        while (true) {
            currentDateTime = LocalDateTime.now()
            delay(1000) // 1秒待機
        }
    }

    // 表示用のフォーマット作成
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val datePattern = stringResource(R.string.date_format)
    val dateFormatter = DateTimeFormatter.ofPattern(datePattern, Locale.getDefault())
    // 今日の祝日名を取得
    val holidayName = remember(currentDateTime.toLocalDate()) {
        repository.getHolidayName(currentDateTime.toLocalDate())
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    ) {
        // 1. 設定ボタン行（編集モード中は無効）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.app_settings),
                fontSize = 14.sp,
                color = if (isEditMode) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .then(
                        if (!isEditMode) Modifier.clickable { onAppSettings() } else Modifier
                    )
                    .padding(8.dp)
            )
            Text(
                text = stringResource(R.string.phone_settings),
                fontSize = 14.sp,
                color = if (isEditMode) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .then(
                        if (!isEditMode) Modifier.clickable {
                            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            context.startActivity(intent)
                        } else Modifier
                    )
                    .padding(8.dp)
            )
        }

        // 2. 日付
        Text(
            text = currentDateTime.format(dateFormatter),
            fontSize = 28.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        // 祝日があってもなくても、日付の下に一定のスペースを作る
        Spacer(modifier = Modifier.height(8.dp))

        if (holidayName != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = holidayName,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        } else {
            // 祝日がない時は、透明な箱を置いて高さを維持する（間が詰まらない）
            Spacer(modifier = Modifier.height(28.dp))
        }

        // 3. 時計 or 編集ボタン群
        if (isEditMode) {
            // 編集モード時は「編集完了」「行追加」を横並び
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                // 編集完了ボタン
                Surface(
                    modifier = Modifier.clickable { onEditDone() },
                    color = MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.edit_complete),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
                Spacer(modifier = Modifier.size(16.dp))
                // 行追加ボタン
                Surface(
                    modifier = Modifier.clickable { onAddRow() },
                    color = MaterialTheme.colorScheme.tertiary,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.add_row),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiary,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
            }
        } else {
            // 通常時は時計
            Text(
                text = currentDateTime.format(timeFormatter),
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}