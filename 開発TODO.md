バグ；

  TODO:
  - 全体のフォントサイズ設定
   
　課金要素
  - バックアップ/リストア（設定とレイアウトの書き出し・読み込み）
  - 長押し時間の調整（高齢者向けに長めにするオプション）
  - 動画閲覧で２４時間フル機能使用可能。期限切れが来た時、再度動画見た後に、フル機能で使用してた状態が復元できるようにする。



❯ 購入する時ってどうやってデバッグするの？

● いくつかの方法があります。

  1. テスト用アカウント（推奨）

  Google Play Consoleで「ライセンステスター」を登録：
  - 実際の課金フローが動く
  - でも実際にはお金がかからない
  - 本番とほぼ同じ挙動を確認できる

  Play Console → 設定 → ライセンステスト → テスターのメールアドレス追加

  2. 静的レスポンス（簡易テスト）

  予約された商品IDを使うと決まった結果が返る：
  // 必ず成功する
  "android.test.purchased"

  // 必ずキャンセル
  "android.test.canceled"

  // 必ずエラー
  "android.test.item_unavailable"

  3. 内部テストトラック

  Play Consoleにアプリをアップロードして：
  - 内部テスト → 指定したユーザーのみ
  - 実際のストアと同じ環境でテスト

  ---
  開発中のおすすめ流れ:
  1. 最初は静的レスポンスで基本動作確認
  2. 次にライセンステスターで実フロー確認
  3. リリース前に内部テストで最終確認

  注意:
  - デバッグビルドでも課金テストは可能
  - ただしアプリがPlay Consoleに登録されている必要あり
  - 商品IDもPlay Consoleで事前に作成が必要

❯ ふむ。ちょっと課金絡みでストアも含めて必要な作業をリストアップして

● 課金機能リリースまでの作業リスト

  1. Google Play Console（ストア側）

  アプリ登録
  - デベロッパーアカウント作成（$25 一回のみ）
  - アプリの新規作成
  - ストア掲載情報（アイコン、説明文、スクリーンショット等）

  課金商品の作成
  - アプリ内アイテム → 「プレミアム解除（買い切り）」を作成
  - 商品ID、価格、説明を設定
  - 商品を「有効」にする

  テスト設定
  - ライセンステスターのメールアドレス登録
  - 内部テストトラックの設定（テスターを招待）

  ---
  2. アプリ側（実装）

  Billing Library導入
  - build.gradleに依存関係追加
  implementation 'com.android.billingclient:billing:6.0.1'

  課金処理の実装
  - BillingClientの初期化・接続
  - 商品情報の取得（queryProductDetailsAsync）
  - 購入フローの起動（launchBillingFlow）
  - 購入結果のハンドリング（成功/キャンセル/エラー）
  - 購入の確認処理（acknowledgePurchase）← 重要！3日以内に確認しないと自動返金

  購入状態の復元
  - アプリ起動時にqueryPurchasesAsyncで購入履歴確認
  - PremiumManager.recordPurchase()との連携

  UIの実装
  - 購入ボタンの配置
  - 価格表示（商品情報から取得）
  - 購入済み状態の表示
  - エラー時のハンドリング

  ---
  3. AdMob（動画広告）

  AdMobアカウント
  - AdMobアカウント作成
  - アプリ登録
  - リワード広告ユニット作成 → 広告ユニットID取得

  実装
  - build.gradleに依存関係追加
  implementation 'com.google.android.gms:play-services-ads:22.6.0'
  - AndroidManifest.xmlにAdMob App ID追加
  - リワード広告の読み込み
  - 広告表示と視聴完了コールバック
  - PremiumManager.recordAdWatch()との連携

  ---
  4. テスト

  - 静的レスポンスで基本動作確認
  - ライセンステスターで購入フロー確認
  - 購入復元の確認（アンインストール→再インストール）
  - 動画広告のテスト（テスト広告ID使用）
  - 24時間後の期限切れ確認

  ---
  5. リリース

  Play Console
  - 内部テスト → クローズドテスト → 製品版 の順で公開
  - 課金商品が「有効」になっているか確認

  AdMob
  - 本番用広告ユニットIDに差し替え
  - テスト広告IDを削除

  ---
  優先度
  ┌────────┬─────────────────────────────────────────┐
  │ 優先度 │                  作業                   │
  ├────────┼─────────────────────────────────────────┤
  │ 高     │ Play Console登録、Billing実装、購入復元 │
  ├────────┼─────────────────────────────────────────┤
  │ 中     │ AdMob実装、テスト                       │
  ├────────┼─────────────────────────────────────────┤
  │ 低     │ UI調整、エラーハンドリング強化        


  ❯   - 購入の確認処理（acknowledgePurchase）← 重要！3日以内に確認しないと自動返金
  についてもう少し教えて

● acknowledgePurchase とは

  購入が成功した後、「ちゃんと受け取りました」とGoogleに伝える処理です。

  なぜ必要？
  - ユーザー保護のため
  - アプリがクラッシュして特典付与できなかった場合などに自動返金される仕組み

  ---
  フロー

  ユーザーが購入
      ↓
  購入成功（PURCHASED状態）
      ↓
  アプリ側で特典付与（premiumManager.recordPurchase()）
      ↓
  acknowledgePurchase() を呼ぶ ← ここ重要
      ↓
  Googleが「確認済み」と記録

  ---
  コード例

  private fun handlePurchase(purchase: Purchase) {
      if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
          // 1. まず特典を付与
          premiumManager.recordPurchase()

          // 2. 未確認なら確認処理
          if (!purchase.isAcknowledged) {
              val params = AcknowledgePurchaseParams.newBuilder()
                  .setPurchaseToken(purchase.purchaseToken)
                  .build()

              billingClient.acknowledgePurchase(params) { billingResult ->
                  if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                      // 確認完了
                  }
              }
          }
      }
  }

  ---
  確認しないとどうなる？
  ┌──────────┬────────────────────────┐
  │ 経過時間 │          状態          │
  ├──────────┼────────────────────────┤
  │ 0〜3日   │ PURCHASED（使える）    │
  ├──────────┼────────────────────────┤
  │ 3日後    │ 自動返金、購入取り消し │
  └──────────┴────────────────────────┘
  ---
  よくあるミス

  1. 確認を忘れる → 3日後に返金されてクレーム
  2. 特典付与前に確認 → クラッシュすると特典なしで課金だけ取られる

  正しい順序:
  特典付与 → acknowledge
  間違った順序:
  acknowledge → 特典付与（この間にクラッシュすると最悪）

  ---
  消耗型 vs 非消耗型
  ┌──────────┬─────────────────────┬────────────────┐
  │  タイプ  │        処理         │       例       │
  ├──────────┼─────────────────────┼────────────────┤
  │ 非消耗型 │ acknowledgePurchase │ プレミアム解除 │
  ├──────────┼─────────────────────┼────────────────┤
  │ 消耗型   │ consumeAsync        │ ゲーム内コイン │
  └──────────┴─────────────────────┴────────────────┘
  今回の「プレミアム解除（買い切り）」は非消耗型なので acknowledgePurchase を使います。
