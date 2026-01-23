package com.example.simplecustomlauncher

import android.app.Activity
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.simplecustomlauncher.data.ShortcutItem
import com.example.simplecustomlauncher.data.ShortcutRepository
import com.example.simplecustomlauncher.data.ShortcutType
import java.util.UUID

/**
 * 外部アプリからの「ホーム画面に追加」リクエストを受け取るActivity
 */
class ShortcutReceiverActivity : Activity() {

    private lateinit var repository: ShortcutRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = ShortcutRepository(this)

        Log.d(TAG, "Received intent: ${intent.action}")
        Log.d(TAG, "Intent extras: ${intent.extras?.keySet()?.joinToString()}")

        when {
            // Android 8.0+ の PinShortcut
            intent.hasExtra(LauncherApps.EXTRA_PIN_ITEM_REQUEST) -> {
                handlePinItemRequest()
            }
            // 古い INSTALL_SHORTCUT
            intent.action == ACTION_INSTALL_SHORTCUT -> {
                handleInstallShortcut()
            }
            else -> {
                Log.w(TAG, "Unknown intent action: ${intent.action}")
                finish()
            }
        }
    }

    /**
     * Android 8.0+ のピン留めショートカットを処理
     */
    private fun handlePinItemRequest() {
        val launcherApps = getSystemService(LAUNCHER_APPS_SERVICE) as LauncherApps
        val request = launcherApps.getPinItemRequest(intent)

        if (request == null) {
            Log.e(TAG, "PinItemRequest is null")
            finish()
            return
        }

        when (request.requestType) {
            LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT -> {
                val shortcutInfo = request.shortcutInfo
                if (shortcutInfo != null) {
                    Log.d(TAG, "Shortcut: ${shortcutInfo.shortLabel}, package: ${shortcutInfo.`package`}")

                    val item = ShortcutItem(
                        id = UUID.randomUUID().toString(),
                        type = ShortcutType.INTENT,
                        label = shortcutInfo.shortLabel?.toString() ?: "ショートカット",
                        packageName = shortcutInfo.`package`,
                        intentUri = null, // ShortcutInfoから直接Intentは取れないので、IDで起動する
                        iconUri = null
                    )

                    // ショートカット情報を別途保存（起動時に使う）
                    saveShortcutInfo(item.id, shortcutInfo.id, shortcutInfo.`package`)

                    if (repository.addShortcutToFirstEmpty(item)) {
                        request.accept()
                        Toast.makeText(this, "「${item.label}」を追加しました", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "空きスロットがありません", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            else -> {
                Log.w(TAG, "Unknown request type: ${request.requestType}")
            }
        }

        finish()
    }

    /**
     * 古い形式のショートカットを処理
     */
    private fun handleInstallShortcut() {
        val name = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME) ?: "ショートカット"
        val shortcutIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_SHORTCUT_INTENT)

        Log.d(TAG, "Install shortcut: $name")
        Log.d(TAG, "Shortcut intent: $shortcutIntent")

        if (shortcutIntent != null) {
            val item = ShortcutItem(
                id = UUID.randomUUID().toString(),
                type = ShortcutType.INTENT,
                label = name,
                intentUri = shortcutIntent.toUri(0)
            )

            if (repository.addShortcutToFirstEmpty(item)) {
                Toast.makeText(this, "「$name」を追加しました", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "空きスロットがありません", Toast.LENGTH_SHORT).show()
            }
        }

        finish()
    }

    /**
     * PinShortcutの情報を保存（起動時に必要）
     */
    private fun saveShortcutInfo(itemId: String, shortcutId: String, packageName: String) {
        val prefs = getSharedPreferences("pin_shortcuts", MODE_PRIVATE)
        prefs.edit()
            .putString("${itemId}_shortcut_id", shortcutId)
            .putString("${itemId}_package", packageName)
            .apply()
    }

    companion object {
        private const val TAG = "ShortcutReceiver"
        private const val ACTION_INSTALL_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT"
    }
}
