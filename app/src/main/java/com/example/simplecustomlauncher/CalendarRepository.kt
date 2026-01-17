package com.example.simplecustomlauncher

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.collections.get

class CalendarRepository(private val context: Context) {

    fun getHolidaysForMonth(year: Int, month: Int): Map<Int, String> {
        val holidayMap = mutableMapOf<Int, String>()

        val startLocalDate = LocalDate.of(year, month, 1)
        val endLocalDate = startLocalDate.plusMonths(1)
        val startMillis = startLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = endLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val projection = arrayOf(
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.TITLE,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME // ← 追加
        )

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startMillis)
        ContentUris.appendId(builder, endMillis)

        try {
            val cursor = context.contentResolver.query(
                builder.build(),
                projection,
                null, // selectionを完全にnullにする（全取得）
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )

            cursor?.use {
                val beginCol = it.getColumnIndex(CalendarContract.Instances.BEGIN)
                val titleCol = it.getColumnIndex(CalendarContract.Instances.TITLE)
                val nameCol = it.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)

                while (it.moveToNext()) {
                    val title = it.getString(titleCol) ?: ""
                    val calName = it.getString(nameCol) ?: ""
                    val begin = it.getLong(beginCol)

                    val date = java.time.Instant.ofEpochMilli(begin)
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate()

                    // 判定条件を広げる：
                    // 「祝日」や「の日」が含まれるか、
                    // あるいはカレンダー名自体に "Holidays" と入っているイベントは全部表示する
                    if (title.contains("祝日") ||
                        title.contains("の日") ||
                        title.contains("振替") ||
                        calName.contains("Holidays", ignoreCase = true)) {

                        holidayMap[date.dayOfMonth] = title
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CalendarDebug", "クエリエラー", e)
        }
        return holidayMap
    }

    fun getHolidayName(date: LocalDate): String? {
        // 指定された日の祝日情報を取得するために、その月の祝日マップを呼び出す
        val holidays = getHolidaysForMonth(date.year, date.monthValue)

        // マップからその日の日付（dayOfMonth）に対応する名前を返す（なければnull）
        return holidays[date.dayOfMonth]
    }
}
