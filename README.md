# 年会費決済アプリ（Android / Square POS SDK）

卒業生向け年会費の決済を行う Android アプリです。  
Square Point of Sale SDK を利用して、受付画面から決済フローを開始します。

## 主な機能

- 卒業期の選択（和暦/西暦表示）
- 氏名入力
- 金額選択（`3,000円` / `1,000円` / 開発用 `1円`）
- 決済前の確認ダイアログ
- Square アプリ連携による決済実行
- 開発用の決済バイパス（UI確認向け）

## 技術スタック

- Android Gradle Plugin `7.4.2`
- Kotlin `1.8.0`
- compileSdk `33`
- minSdk `24`
- 依存ライブラリ: `com.squareup.sdk:point-of-sale-sdk:2.1`

## 前提条件

- macOS / Linux / Windows で Android 開発環境があること
- Android Studio または Android SDK/ADB が利用可能であること
- Square Developer で発行した **本番用 Client ID** (`sq0idp-...`) があること  
  - Point of Sale API は Sandbox の clientId では動作しません
- 端末に Square Point of Sale アプリがインストール済みであること

## セットアップ

1. このリポジトリをクローン
2. ルートの `local.properties` に `squareClientId` を追加

```properties
squareClientId="sq0idp-xxxxxxxxxxxxxxxx"
```

任意（開発用）:

```properties
squareDevPaymentBypass=true
```

`squareDevPaymentBypass=true` は **debug ビルドのみ有効** です。  
有効時は Square を起動せず、成功/失敗画面を選んで表示確認できます。

## ビルドとインストール

```bash
./gradlew --no-daemon :app:build
./gradlew --no-daemon :app:installDebug
```

## アプリID / パッケージ名

- `applicationId`: `com.tokyotochikukai.annualfee.square`

## プロジェクト構成

- `app/` : Android アプリ本体
- `documents/` : 要件・ワイヤーフレームなどの補助資料
- `LICENSE.txt` : ライセンス情報

## 注意事項

- `local.properties` は機密情報を含むため、Git 管理に含めないでください
- 決済にはネットワーク接続が必要です
- カード情報はアプリ側で保持せず、Square SDK に委任されます

## ライセンス

このリポジトリのライセンスは [LICENSE.txt](LICENSE.txt) を参照してください。
