package com.example.simplecustomlauncher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.simplecustomlauncher.CalendarRepository
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarFullScreen(
    hasPermission: Boolean,
    holidayMap: Map<Int, String>,  // 互換性のため残すが使わない
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val calendarRepository = remember { CalendarRepository(context) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    // 表示中の月の祝日を取得（月が変わるたびに再取得）
    val currentHolidayMap = remember(currentMonth, hasPermission) {
        if (hasPermission) {
            calendarRepository.getHolidaysForMonth(currentMonth.year, currentMonth.monthValue)
        } else {
            emptyMap()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "カレンダー",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // 月の切り替え
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                    Icon(
                        Icons.Default.KeyboardArrowLeft,
                        contentDescription = "前の月",
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    text = "${currentMonth.year}年${currentMonth.monthValue}月",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = "次の月",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 曜日ヘッダー
            Row(modifier = Modifier.fillMaxWidth()) {
                val weekDays = listOf("日", "月", "火", "水", "木", "金", "土")
                weekDays.forEachIndexed { index, day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = when (index) {
                            0 -> MaterialTheme.colorScheme.error
                            6 -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onBackground
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // カレンダーグリッド（残りのスペースを使う）
            CalendarGrid(
                yearMonth = currentMonth,
                holidayMap = currentHolidayMap,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    yearMonth: YearMonth,
    holidayMap: Map<Int, String>,
    modifier: Modifier = Modifier
) {
    val firstDayOfMonth = yearMonth.atDay(1)
    val lastDayOfMonth = yearMonth.atEndOfMonth()
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // 日曜=0
    val daysInMonth = lastDayOfMonth.dayOfMonth
    val today = LocalDate.now()

    val totalCells = firstDayOfWeek + daysInMonth
    val rows = (totalCells + 6) / 7

    Column(modifier = modifier) {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)  // 各行が均等に高さを分配
            ) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val day = cellIndex - firstDayOfWeek + 1

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(1.dp)
                    ) {
                        if (day in 1..daysInMonth) {
                            val date = yearMonth.atDay(day)
                            val isToday = date == today
                            val isHoliday = holidayMap.containsKey(day)
                            val holidayName = holidayMap[day]
                            val isSunday = col == 0
                            val isSaturday = col == 6

                            val textColor = when {
                                isHoliday || isSunday -> MaterialTheme.colorScheme.error
                                isSaturday -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onBackground
                            }

                            // セルの背景色
                            val cellBackground = if (isToday) {
                                MaterialTheme.colorScheme.secondary  // 当日はオレンジ（secondary）
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant  // 通常
                            }

                            // セル全体
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(cellBackground, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 2.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // 日付（上部固定）
                                Text(
                                    text = day.toString(),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isToday) MaterialTheme.colorScheme.onSecondary else textColor
                                )

                                // 祝日名（日付のすぐ下、スペースがあれば全部表示）
                                if (holidayName != null) {
                                    Text(
                                        text = holidayName,
                                        fontSize = 8.sp,
                                        color = if (isToday) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.error,
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 9.sp
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
