package com.example.simplecustomlauncher

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import android.os.Process
import android.util.Log

data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable?
)

data class ShortcutData(
    val id: String,
    val shortLabel: String,
    val longLabel: String?,
    val packageName: String,
    val icon: Drawable?,
    val shortcutInfo: ShortcutInfo // 起動時に必要
)

class ShortcutHelper(private val context: Context) {

    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val packageManager = context.packageManager

    /**
     * インストール済みのアプリ一覧を取得
     */
    fun getInstalledApps(): List<AppInfo> {
        val apps = mutableListOf<AppInfo>()

        val activityList = launcherApps.getActivityList(null, Process.myUserHandle())
        for (activity in activityList) {
            apps.add(
                AppInfo(
                    label = activity.label.toString(),
                    packageName = activity.applicationInfo.packageName,
                    icon = activity.getBadgedIcon(0)
                )
            )
        }

        return apps.sortedBy { it.label }
    }

    /**
     * 指定したアプリのショートカット一覧を取得
     */
    fun getShortcutsForApp(packageName: String): List<ShortcutData> {
        val shortcuts = mutableListOf<ShortcutData>()

        // ショートカット取得にはクエリを作成
        val query = LauncherApps.ShortcutQuery().apply {
            setPackage(packageName)
            setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
            )
        }

        try {
            val shortcutList = launcherApps.getShortcuts(query, Process.myUserHandle())
            shortcutList?.forEach { shortcut ->
                val icon = launcherApps.getShortcutIconDrawable(shortcut, 0)
                shortcuts.add(
                    ShortcutData(
                        id = shortcut.id,
                        shortLabel = shortcut.shortLabel?.toString() ?: "",
                        longLabel = shortcut.longLabel?.toString(),
                        packageName = shortcut.`package`,
                        icon = icon,
                        shortcutInfo = shortcut
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("ShortcutHelper", "Failed to get shortcuts for $packageName", e)
        }

        return shortcuts
    }

    /**
     * ショートカットを起動
     */
    fun startShortcut(shortcut: ShortcutData) {
        try {
            launcherApps.startShortcut(
                shortcut.packageName,
                shortcut.id,
                null, // sourceBounds
                null, // startActivityOptions
                Process.myUserHandle()
            )
        } catch (e: Exception) {
            Log.e("ShortcutHelper", "Failed to start shortcut", e)
        }
    }

    /**
     * アプリアイコンを取得
     */
    fun getAppIcon(packageName: String): Drawable? {
        return try {
            val activityList = launcherApps.getActivityList(packageName, Process.myUserHandle())
            activityList.firstOrNull()?.getBadgedIcon(0)
        } catch (e: Exception) {
            Log.e("ShortcutHelper", "Failed to get icon for $packageName", e)
            null
        }
    }

    /**
     * アプリを起動
     */
    fun startApp(packageName: String) {
        try {
            val activityList = launcherApps.getActivityList(packageName, Process.myUserHandle())
            if (activityList.isNotEmpty()) {
                launcherApps.startMainActivity(
                    activityList[0].componentName,
                    Process.myUserHandle(),
                    null,
                    null
                )
            }
        } catch (e: Exception) {
            Log.e("ShortcutHelper", "Failed to start app $packageName", e)
        }
    }

    /**
     * デバッグ用：全アプリのショートカットをログ出力
     */
    fun debugPrintAllShortcuts() {
        val apps = getInstalledApps()
        Log.d("ShortcutHelper", "=== Installed Apps: ${apps.size} ===")

        for (app in apps) {
            val shortcuts = getShortcutsForApp(app.packageName)
            if (shortcuts.isNotEmpty()) {
                Log.d("ShortcutHelper", "📱 ${app.label} (${app.packageName})")
                shortcuts.forEach { shortcut ->
                    Log.d("ShortcutHelper", "  └─ ${shortcut.shortLabel} (${shortcut.id})")
                }
            }
        }
    }
}
