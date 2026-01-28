package com.example.simplecustomlauncher

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun HomeHeader(
    context: android.content.Context,
    isEditMode: Boolean = false,
    onEditDone: () -> Unit = {},
    onAddRow: () -> Unit = {},
    onResetPage: () -> Unit = {},
    onDeletePage: (() -> Unit)? = null,
    onLayoutEdit: () -> Unit = {},
    onAppSettings: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
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
                text = stringResource(R.string.layout_edit),
                fontSize = 14.sp,
                color = if (isEditMode) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .then(
                        if (!isEditMode) Modifier.clickable { onLayoutEdit() } else Modifier
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

        // 2. 編集モード時のみ編集ボタン群を表示
        if (isEditMode) {
            // 上段: 編集完了 + 行追加
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                // 編集完了ボタン（緑：完了/成功）
                Surface(
                    modifier = Modifier.clickable { onEditDone() },
                    color = Color(0xFF4CAF50), // Green
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.edit_complete),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
                Spacer(modifier = Modifier.size(16.dp))
                // 行追加ボタン（青：アクション）
                Surface(
                    modifier = Modifier.clickable { onAddRow() },
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.add_row),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.size(8.dp))
            // 下段: ページリセット + ページ削除
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    modifier = Modifier.clickable { onResetPage() },
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.reset_page),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                if (onDeletePage != null) {
                    Spacer(modifier = Modifier.size(12.dp))
                    Surface(
                        modifier = Modifier.clickable { onDeletePage() },
                        color = MaterialTheme.colorScheme.error,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.delete_page),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}