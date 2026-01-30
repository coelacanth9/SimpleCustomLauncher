package com.example.simplecustomlauncher.data

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MemoRepositoryTest {

    private lateinit var repo: MemoRepository

    @Before
    fun setUp() {
        repo = MemoRepository(ApplicationProvider.getApplicationContext())
    }

    // --- A: 保存したメモが正しくデシリアライズされる ---

    @Test
    fun getMemos_returnsDeserializedMemos() {
        repo.addMemo("メモ1")
        repo.addMemo("メモ2")

        val memos = repo.getMemos()
        assertEquals(2, memos.size)
        assertEquals("メモ2", memos[0].text)
        assertEquals("メモ1", memos[1].text)
        assertFalse(memos[0].isChecked)
    }

    // --- A: データ破損時に空リストが返る ---

    @Test
    fun getMemos_corruptedData_returnsEmptyList() {
        val prefs = ApplicationProvider.getApplicationContext<android.app.Application>()
            .getSharedPreferences("memo_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("memos", "invalid json{{{").commit()

        val memos = repo.getMemos()
        assertEquals(0, memos.size)
    }

    // --- A: 新規メモがリスト先頭に追加される ---

    @Test
    fun addMemo_insertsAtFront() {
        repo.addMemo("最初")
        repo.addMemo("あとから")

        val memos = repo.getMemos()
        assertEquals("あとから", memos[0].text)
        assertEquals("最初", memos[1].text)
    }

    // --- A: 指定IDのメモが削除される ---

    @Test
    fun deleteMemo_removesById() {
        val memo1 = repo.addMemo("残る")
        val memo2 = repo.addMemo("消える")

        repo.deleteMemo(memo2.id)

        val memos = repo.getMemos()
        assertEquals(1, memos.size)
        assertEquals(memo1.id, memos[0].id)
    }

    // --- A: チェック状態がトグルされる ---

    @Test
    fun toggleCheck_flipsCheckedState() {
        val memo = repo.addMemo("テスト")
        assertFalse(repo.getMemos()[0].isChecked)

        repo.toggleCheck(memo.id)
        assertTrue(repo.getMemos()[0].isChecked)

        repo.toggleCheck(memo.id)
        assertFalse(repo.getMemos()[0].isChecked)
    }

    // --- A: チェック済みメモのみ削除される ---

    @Test
    fun deleteCheckedMemos_removesOnlyChecked() {
        val keep = repo.addMemo("残す")
        val remove = repo.addMemo("消す")
        repo.toggleCheck(remove.id)

        repo.deleteCheckedMemos()

        val memos = repo.getMemos()
        assertEquals(1, memos.size)
        assertEquals(keep.id, memos[0].id)
    }
}
