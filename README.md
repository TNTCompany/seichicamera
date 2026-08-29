<div align="center">

# SeichiCamera

### 为动漫圣地巡礼而生的地图与相机

从地图中的取景地点，抵达现实中的同一视角。

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Language](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/License-MIT-0B2239.svg)](LICENSE)

[简体中文](README.md) · [English](README.en.md) · [日本語](README.ja.md) · [繁體中文](README.zh-TW.md)

</div>

## 项目简介

**SeichiCamera** 是一款面向动漫圣地巡礼的 Android 应用。它把动漫作品检索、圣地地图、参考画面与相机拍摄串成一条自然的创作路径：在地图中找到地点，以更清晰的参考图进入相机，在现实场景中调整叠加画面并完成对比拍摄。

本项目与任何数据提供方或作品版权方不存在官方隶属关系；作品、地点及参考图片的相关权利归各自权利人所有。

## 核心体验

- **作品到地点**：搜索动漫作品，在地图上浏览关联的圣地巡礼点位。
- **地点到相机**：从地点详情直接带入参考图，减少在地图与相机之间的操作断点。
- **参考图叠加**：移动、缩放、旋转、镜像与调节透明度，帮助匹配动漫中的构图。
- **一键清空重来**：移除当前参考图，立即回到纯相机画面；随后可重新从地图或相册载入。
- **对比留存**：将参考画面与实拍照片生成对比图，记录每一次巡礼。
- **离线与本地化**：支持地点数据缓存，并提供简体中文、繁體中文、English 与日本語界面。

## 使用流程

```text
搜索动漫作品 → 浏览圣地地图 → 选择地点与参考图
       → 进入相机 → 调整叠加画面 → 拍摄 / 生成对比图
```

## 技术概览

| 领域 | 采用技术 |
| --- | --- |
| UI | Jetpack Compose、Material 3、Navigation Compose |
| 架构 | MVVM、Repository、Hilt 依赖注入 |
| 数据 | Retrofit、Kotlinx Serialization、Room、DataStore |
| 图像与相机 | CameraX、Coil 3 |
| 地图 | osmdroid / OpenStreetMap |
| 异步 | Kotlin Coroutines、Flow |
| 构建 | Gradle、KSP、Kotlin、Java 17 |

## 项目结构

```text
app/src/main/java/com/tnt/seichicamera/
├── data/          # 网络接口、本地数据库与 Repository 实现
├── di/            # Hilt 模块
├── domain/        # 面向 UI 的领域模型
├── ui/
│   ├── map/       # 作品检索、地图与地点详情
│   ├── camera/    # CameraX、参考图编辑与对比图生成
│   ├── settings/  # 语言、缓存等设置
│   └── navigation/# 页面路由与底部导航
└── util/          # 语言、图片 URL 等通用工具
```

## 快速开始

### 环境要求

- Android Studio（建议使用稳定版）
- JDK 17
- Android SDK，`compileSdk` 为 35
- Android 7.0（API 24）及以上设备或模拟器

### 构建与运行

```bash
git clone <your-fork-or-repository-url>
cd SeichiCamera
```

在项目根目录创建 `local.properties`，指向本机 Android SDK：

```properties
sdk.dir=/path/to/Android/Sdk
```

随后可用 Android Studio 打开项目并运行，或执行：

```bash
./gradlew assembleDebug
```

Windows PowerShell：

```powershell
.\gradlew.bat assembleDebug
```

调试 APK 默认输出至 `app/build/outputs/apk/debug/`。

## 权限与数据

| 权限 | 用途 |
| --- | --- |
| 相机 | 取景、拍摄与生成巡礼照片 |
| 网络 | 获取作品、地点、地图瓦片与参考图 |
| 图片媒体读取 | 从相册选择自定义参考图 |
| 精确/粗略位置 | 支持与地图相关的位置能力 |

地点缓存、偏好设置和巡礼记录保存在设备本地。请在发布前根据实际接入的数据服务与隐私政策，补充或更新本节说明。

## 贡献

欢迎通过 Issue 提交问题、体验反馈或功能建议；也欢迎通过 Pull Request 改进代码与文档。提交前请确保：

```bash
./gradlew testDebugUnitTest
```

同时请避免提交 `local.properties`、构建产物、个人 API 凭据或未经授权的图片素材。

## 许可证

本项目以 [MIT License](LICENSE) 发布。

---

<div align="center">
  <sub>让动漫中的一帧，成为你抵达现场的下一步。</sub>
</div>
