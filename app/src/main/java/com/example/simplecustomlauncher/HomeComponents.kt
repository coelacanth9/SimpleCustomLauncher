package com.example.simplecustomlauncher

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun HomeHeader(context: android.content.Context) {
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
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日(E)", Locale.JAPANESE)
    // 今日の祝日名を取得
    val holidayName = remember(currentDateTime.toLocalDate()) {
        repository.getHolidayName(currentDateTime.toLocalDate())
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. 日付
        Text(
            text = currentDateTime.format(dateFormatter),
            fontSize = 28.sp,
            color = Color.Black
        )

        // 祝日があってもなくても、日付の下に一定のスペースを作る
        Spacer(modifier = Modifier.height(8.dp))

        if (holidayName != null) {
            Surface(
                color = Color(0xFFFFEBEE),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = holidayName,
                    color = Color.Red,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        } else {
            // 祝日がない時は、透明な箱を置いて高さを維持する（間が詰まらない）
            Spacer(modifier = Modifier.height(28.dp))
        }

        // 2. 時計
        Text(
            text = currentDateTime.format(timeFormatter),
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold
        )
    }
}