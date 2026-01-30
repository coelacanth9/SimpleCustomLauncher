package com.example.simplecustomlauncher.data

import android.content.Intent
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShortcutItemTest {

    // --- A項目: APPタイプでACTION_MAINインテントが生成される ---

    @Test
    fun toIntent_app_generatesActionMainIntent() {
        val item = ShortcutItem(
            id = "test-app",
            type = ShortcutType.APP,
            label = "テストアプリ",
            packageName = "com.example.testapp"
        )
        val intent = item.toIntent()

        assertNotNull(intent)
        assertEquals(Intent.ACTION_MAIN, intent!!.action)
        assertEquals("com.example.testapp", intent.`package`)
        assertTrue(intent.categories.contains(Intent.CATEGORY_LAUNCHER))
    }

    @Test
    fun toIntent_app_withoutPackageName_returnsNull() {
        val item = ShortcutItem(
            id = "test-app-no-pkg",
            type = ShortcutType.APP,
            label = "パッケージなし"
        )
        assertNull(item.toIntent())
    }

    // --- A項目: PHONEタイプでACTION_DIALインテントが生成される ---

    @Test
    fun toIntent_phone_generatesActionDialIntent() {
        val item = ShortcutItem(
            id = "test-phone",
            type = ShortcutType.PHONE,
            label = "電話",
            phoneNumber = "09012345678"
        )
        val intent = item.toIntent()

        assertNotNull(intent)
        assertEquals(Intent.ACTION_DIAL, intent!!.action)
        assertEquals("tel:09012345678", intent.data.toString())
    }

    @Test
    fun toIntent_phone_withoutNumber_returnsNull() {
        val item = ShortcutItem(
            id = "test-phone-no-num",
            type = ShortcutType.PHONE,
            label = "電話番号なし"
        )
        assertNull(item.toIntent())
    }

    // --- A項目: SMSタイプでACTION_SENDTOインテントが生成される ---

    @Test
    fun toIntent_sms_generatesActionSendToIntent() {
        val item = ShortcutItem(
            id = "test-sms",
            type = ShortcutType.SMS,
            label = "SMS",
            phoneNumber = "09087654321"
        )
        val intent = item.toIntent()

        assertNotNull(intent)
        assertEquals(Intent.ACTION_SENDTO, intent!!.action)
        assertEquals("smsto:09087654321", intent.data.toString())
    }

    @Test
    fun toIntent_sms_withoutNumber_returnsNull() {
        val item = ShortcutItem(
            id = "test-sms-no-num",
            type = ShortcutType.SMS,
            label = "SMS番号なし"
        )
        assertNull(item.toIntent())
    }

    // --- 内部機能タイプはnullを返す ---

    @Test
    fun toIntent_internalTypes_returnNull() {
        val internalTypes = listOf(
            ShortcutType.CALENDAR,
            ShortcutType.MEMO,
            ShortcutType.SETTINGS,
            ShortcutType.ALL_APPS,
            ShortcutType.DATE_DISPLAY,
            ShortcutType.TIME_DISPLAY,
            ShortcutType.EMPTY
        )
        for (type in internalTypes) {
            val item = ShortcutItem(id = "test-$type", type = type, label = type.name)
            assertNull("$type should return null", item.toIntent())
        }
    }
}
