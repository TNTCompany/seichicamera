# SeichiCamera

> 為動畫聖地巡禮而生，串連地圖與相機的 Android 應用程式。

[简体中文](README.md) · [English](README.en.md) · [日本語](README.ja.md) · [繁體中文](README.zh-TW.md)

SeichiCamera 將作品搜尋、聖地地圖、參考圖片與 CameraX 拍攝整合為一條流程。找到地點後，將更清晰的參考圖帶入相機、調整構圖，並保留實景對比圖。

## 核心功能

- 搜尋動畫作品，於地圖瀏覽相關聖地點位
- 從地點詳情直接進入帶有參考圖的相機
- 移動、縮放、旋轉、鏡像與調整參考圖透明度
- 移除目前參考圖，回到純相機畫面後重新載入
- 產生對比圖片、離線快取與語言設定

## 技術堆疊

Kotlin · Jetpack Compose · Material 3 · CameraX · Hilt · Room · Retrofit · Coil · osmdroid / OpenStreetMap

## 快速開始

需要 JDK 17、Android SDK `compileSdk` 35，以及 Android 7.0（API 24）以上的裝置或模擬器。

```bash
git clone <your-fork-or-repository-url>
cd SeichiCamera
./gradlew assembleDebug
```

建置前請在 `local.properties` 設定 Android SDK 路徑：`sdk.dir=/path/to/Android/Sdk`。

## 授權

本專案採用 [MIT License](LICENSE)。本專案與資料提供方、作品及參考圖片的權利人沒有官方從屬關係。

完整說明請參閱 [简体中文 README](README.md)。
