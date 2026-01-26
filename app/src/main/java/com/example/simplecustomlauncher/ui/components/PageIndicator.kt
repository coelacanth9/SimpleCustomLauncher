package com.example.simplecustomlauncher.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.simplecustomlauncher.R

/**
 * ページインジケーター
 * @param pageCount 総ページ数
 * @param currentPage 現在のページ（0始まり）
 * @param lockedFromPage このページ以降はロック表示（-1の場合はロックなし）
 * @param modifier Modifier
 */
@Composable
fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    lockedFromPage: Int = -1,
    modifier: Modifier = Modifier
) {
    // 1ページの場合は表示しない
    if (pageCount <= 1) return

    Row(
        modifier = modifier.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (page in 0 until pageCount) {
            val isCurrentPage = page == currentPage
            val isLocked = lockedFromPage in 0..page

            PageDot(
                isSelected = isCurrentPage,
                isLocked = isLocked
            )
        }
    }
}

@Composable
private fun PageDot(
    isSelected: Boolean,
    isLocked: Boolean
) {
    val size by animateDpAsState(
        targetValue = if (isSelected) 12.dp else 8.dp,
        animationSpec = tween(durationMillis = 200),
        label = "dotSize"
    )

    val color by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primary
            isLocked -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.outline
        },
        animationSpec = tween(durationMillis = 200),
        label = "dotColor"
    )

    Box(
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
        )

        // ロック時は小さな鍵アイコンを表示
        if (isLocked && !isSelected) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = stringResource(R.string.lock),
                modifier = Modifier
                    .size(10.dp)
                    .offset(x = 6.dp, y = (-4).dp),
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}
