# SeichiCamera

> アニメ聖地巡礼のための、地図からカメラまでをつなぐ Android アプリ。

[简体中文](README.md) · [English](README.en.md) · [日本語](README.ja.md) · [繁體中文](README.zh-TW.md)

SeichiCamera は、作品検索、聖地マップ、参照画像、CameraX による撮影を一つの流れにまとめます。地点を見つけ、鮮明な参照画像をカメラに読み込み、構図を合わせて比較画像を残せます。

## 主な機能

- アニメ作品を検索し、関連する聖地を地図で確認
- 地点詳細から参照画像付きでカメラを起動
- 参照画像の移動・拡大縮小・回転・反転・透明度調整
- 参照画像を削除して、純粋なカメラ画面に戻る
- 比較画像の生成、ローカルキャッシュ、言語設定

## 技術スタック

Kotlin · Jetpack Compose · Material 3 · CameraX · Hilt · Room · Retrofit · Coil · osmdroid / OpenStreetMap

## 開始方法

JDK 17、Android SDK `compileSdk` 35、Android 7.0（API 24）以降が必要です。

```bash
git clone <your-fork-or-repository-url>
cd SeichiCamera
./gradlew assembleDebug
```

ビルド前に、Android SDK を示す `sdk.dir=/path/to/Android/Sdk` を `local.properties` に設定してください。

## ライセンス

[MIT License](LICENSE) で公開しています。本プロジェクトは、データ提供元、作品、画像の権利者と公式な関係はありません。

詳細は [简体中文 README](README.md) を参照してください。
