package com.example.simplecustomlauncher.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 行追加時の分割数選択ダイアログ
 */
@Composable
fun AddRowDialog(
    onAddRow: (columns: Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "行を追加",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "分割数を選んでください",
                    fontSize = 16.sp,
                    color = Color.Gray
                )

                // 1分割
                ColumnOptionCard(
                    columns = 1,
                    description = "横長ボタン1つ（文字大きめ）",
                    onClick = { onAddRow(1) }
                )

                // 2分割
                ColumnOptionCard(
                    columns = 2,
                    description = "ボタン2つ横並び",
                    onClick = { onAddRow(2) }
                )

                // 3分割
                ColumnOptionCard(
                    columns = 3,
                    description = "ボタン3つ横並び（アイコンのみ）",
                    onClick = { onAddRow(3) }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}

@Composable
private fun ColumnOptionCard(
    columns: Int,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 分割数のプレビュー
            Row(
                modifier = Modifier.width(80.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(columns) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1976D2)
                        ),
                        shape = RoundedCornerShape(4.dp)
                    ) {}
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "${columns}分割",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
