  設定関連
  - ダークモード対応（ダーク/ライト/システム連動の3択）
  - タップ時の確認ダイアログの挙動実装（設定はあるけど動作確認）
  - レイアウト設定の挙動検討および実装

  編集画面の並び順変更
  現在の順番 → 希望の順番：
  1. アプリ一覧から選ぶ
  2. このスロットを空ける
  3. この行全体を消す
  4. アプリ内機能
  5. 未配置ショートカット
  6. 配置済みと入れ替え

  以前出てたやつ
  - ショートカット編集画面で全クリア機能
  - テンプレート機能（初期配置のプリセット）

  他に思いつくもの
  - ページ数設定（現在1ページ固定）
  - 全体のフォントサイズ設定
  - バックアップ/リストア（設定とレイアウトの書き出し・読み込み）
  - 長押し時間の調整（高齢者向けに長めにするオプション）


    改善検討点 🔧

  1. MainActivity.kt が大きい（約1200行）
  現状: MainActivity に UI ロジックが集中
  推奨: 以下の分離を検討
    - HomeScreen.kt（ホーム画面のComposable）
    - ShortcutLauncher.kt（起動ロジック）
    - NavigationState.kt（画面状態管理）

  2. 状態管理のスケーラビリティ
  現状: remember { mutableStateOf() } で直接管理
  検討: ViewModel導入（画面回転対応、テスト容易性）
  ただし: 現規模なら今のままでも十分シンプル

  3. SharedPreferences の分散
  現状: 複数の SharedPreferences ファイル
    - launcher_shortcuts
    - memo_prefs
    - launcher_settings
    - pin_shortcuts
  検討: 将来的にRoomへの移行、またはDataStore
  ただし: 現規模なら問題なし

  4. 未使用・重複コード
  - CalenderComponents.kt（typo + 使われていない可能性）
  - BottomNavigationBar（MainActivity内、現在未使用）
  - DraggableItem.kt, SlotEditSheet.kt（確認が必要）

  5. エラーハンドリング
  現状: try-catch で Toast 表示
  推奨: 統一的なエラー表示（Snackbar等）