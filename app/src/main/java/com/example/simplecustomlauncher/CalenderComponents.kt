package com.example.simplecustomlauncher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import java.util.Calendar


// --- 1. メイン画面：カレンダー全体のレイアウト ---
@Composable
fun CalendarContent(hasPermission: Boolean, holidayMap: Map<Int, String>) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 曜日ラベル（日〜土）
        CalendarHeader()

        // カレンダーグリッド
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            CalendarGrid(holidayMap = holidayMap)
        }
    }
}

@Composable
fun CalendarGrid(holidayMap: Map<Int, String>) {
    val calendar = Calendar.getInstance()
    val today = calendar.get(Calendar.DAY_OF_MONTH)
    val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    calendar.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1

    Column(modifier = Modifier.fillMaxSize()) {
        var currentDay = 1 - firstDayOfWeek

        for (week in 0 until 6) {
            Row(modifier = Modifier.weight(1f)) {
                for (dayInWeek in 0 until 7) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        if (currentDay in 1..maxDay) {
                            val displayDay = currentDay
                            val cellCalendar = Calendar.getInstance()
                            cellCalendar.set(Calendar.DAY_OF_MONTH, displayDay)
                            val dayOfWeek = cellCalendar.get(Calendar.DAY_OF_WEEK)
                            val holidayName = holidayMap[displayDay]

                            CalendarDayCell(
                                day = displayDay,
                                isToday = (displayDay == today),
                                holidayName = holidayName,
                                dayOfWeek = dayOfWeek
                            )
                        }
                    }
                    currentDay++
                }
            }
        }
    }
}

@Composable
fun CalendarDayCell(day: Int, isToday: Boolean, holidayName: String?, dayOfWeek: Int) {
    Box(
        modifier = Modifier
            .padding(2.dp)
            .fillMaxSize()
            .border(
                width = if (isToday) 3.dp else 1.dp,
                color = if (isToday) Color.Red else Color.LightGray,
                shape = RoundedCornerShape(4.dp)
            )
            .background(if (isToday) Color(0xFFFFF9C4) else Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.toString(),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    holidayName != null || dayOfWeek == Calendar.SUNDAY -> Color.Red
                    dayOfWeek == Calendar.SATURDAY -> Color.Blue
                    else -> Color.Black
                }
            )

            Box(
                modifier = Modifier.heightIn(min = 32.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                if (holidayName != null) {
                    Text(
                        text = holidayName,
                        color = Color.Red,
                        fontSize = 11.sp,
                        lineHeight = 12.sp,
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CalendarHeader() {
    val days = listOf("日", "月", "火", "水", "木", "金", "土")
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        days.forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = when (day) {
                    "日" -> Color.Red
                    "土" -> Color.Blue
                    else -> Color.Gray
                }
            )
        }
    }
}