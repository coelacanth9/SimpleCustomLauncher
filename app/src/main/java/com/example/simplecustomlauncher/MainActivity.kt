package com.example.simplecustomlauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.simplecustomlauncher.ui.theme.SimpleCustomLauncherTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.simplecustomlauncher.PermissionManager.CALENDAR_PERMISSIONS

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(), // ← これを追加！
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainLauncherScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainLauncherScreen() {
    val context = LocalContext.current
    val pagerState = rememberPagerState { 2 }

    // 権限状態の管理
    var hasPermission by remember {
        mutableStateOf(PermissionManager.checkPermissions(context, CALENDAR_PERMISSIONS))
    }

    // 起動時に権限を要求
    RequestPermissions(
        context = context,
        permissions = CALENDAR_PERMISSIONS,
        onResult = { isGranted -> hasPermission = isGranted }
    )

    // 祝日データの取得（権限が許可されたら再実行）
    val repository = remember { CalendarRepository(context) }
    val holidayMap = remember(hasPermission) {
        if (hasPermission) {
            val now = LocalDate.now()
            repository.getHolidaysForMonth(now.year, now.monthValue)
        } else {
            emptyMap()
        }
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(pagerState)
        }
    ) { paddingValues ->
        // Columnでヘッダーを上に固定
        Column(modifier = Modifier.padding(paddingValues)) {
            // ヘッダー（時計・日付・今日の祝日）
            HomeHeader(context = context)

            // 下のコンテンツ部分だけが左右にスワイプ
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> HomeGridContent() // ホーム画面のボタン群
                    1 -> CalendarContent(
                        hasPermission = hasPermission,
                        holidayMap = holidayMap
                    )
                }
            }
        }
    }
}

@Composable
fun HomeGridContent() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2), // 2列配置
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(20) { index ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("ボタン ${index + 1}", color = Color.White, fontSize = 20.sp)
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(pagerState: PagerState) {
    val scope = rememberCoroutineScope()

    NavigationBar(
        modifier = Modifier.height(80.dp), // 少し高くして押しやすく
        containerColor = Color.White
    ) {
        // --- ホームボタン ---
        NavigationBarItem(
            selected = pagerState.currentPage == 0,
            onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
            icon = { /* アイコンは空にする */ },
            label = {
                Text(
                    text = "ホーム",
                    fontSize = 24.sp, // 特大文字
                    fontWeight = FontWeight.Bold,
                    color = if (pagerState.currentPage == 0) Color.Black else Color.Gray
                )
            },
            alwaysShowLabel = true,
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = Color(0xFFE0E0E0) // 選択時の背景（薄いグレー）
            )
        )

        // --- カレンダーボタン ---
        NavigationBarItem(
            selected = pagerState.currentPage == 1,
            onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
            icon = { /* アイコンは空にする */ },
            label = {
                Text(
                    text = "カレンダー",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (pagerState.currentPage == 1) Color.Black else Color.Gray
                )
            },
            alwaysShowLabel = true,
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = Color(0xFFE0E0E0)
            )
        )
    }
}

@Composable
fun NavButtonItem(text: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable { onClick() }
            .background(if (isSelected) Color(0xFF444444) else Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
@Preview(showBackground = true, device = "spec:width=1080px,height=2340px,dpi=440")
@Composable
fun MainLauncherPreview() {
    MaterialTheme {
        MainLauncherScreen()
    }
}