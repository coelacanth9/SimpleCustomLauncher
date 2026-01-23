package com.example.simplecustomlauncher.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * メモの永続化を担当するリポジトリ
 */
class MemoRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    /**
     * 全メモを取得
     */
    fun getMemos(): List<MemoItem> {
        val json = prefs.getString(KEY_MEMOS, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                MemoItem(
                    id = obj.getString("id"),
                    text = obj.getString("text"),
                    isChecked = obj.getBoolean("isChecked"),
                    createdAt = obj.getLong("createdAt")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * メモを保存
     */
    fun saveMemos(memos: List<MemoItem>) {
        val array = JSONArray()
        memos.forEach { memo ->
            val obj = JSONObject().apply {
                put("id", memo.id)
                put("text", memo.text)
                put("isChecked", memo.isChecked)
                put("createdAt", memo.createdAt)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_MEMOS, array.toString()).apply()
    }

    /**
     * メモを追加
     */
    fun addMemo(text: String): MemoItem {
        val memo = MemoItem(text = text)
        val memos = getMemos().toMutableList()
        memos.add(0, memo)  // 先頭に追加
        saveMemos(memos)
        return memo
    }

    /**
     * メモを更新
     */
    fun updateMemo(memo: MemoItem) {
        val memos = getMemos().toMutableList()
        val index = memos.indexOfFirst { it.id == memo.id }
        if (index >= 0) {
            memos[index] = memo
            saveMemos(memos)
        }
    }

    /**
     * メモを削除
     */
    fun deleteMemo(id: String) {
        val memos = getMemos().filter { it.id != id }
        saveMemos(memos)
    }

    /**
     * チェック状態を切り替え
     */
    fun toggleCheck(id: String) {
        val memos = getMemos().toMutableList()
        val index = memos.indexOfFirst { it.id == id }
        if (index >= 0) {
            memos[index] = memos[index].copy(isChecked = !memos[index].isChecked)
            saveMemos(memos)
        }
    }

    /**
     * 文字サイズ設定を取得
     */
    fun getFontSize(): Int {
        return prefs.getInt(KEY_FONT_SIZE, DEFAULT_FONT_SIZE)
    }

    /**
     * 文字サイズ設定を保存
     */
    fun setFontSize(size: Int) {
        prefs.edit().putInt(KEY_FONT_SIZE, size).apply()
    }

    companion object {
        private const val PREFS_NAME = "memo_prefs"
        private const val KEY_MEMOS = "memos"
        private const val KEY_FONT_SIZE = "font_size"
        private const val DEFAULT_FONT_SIZE = 20
    }
}
