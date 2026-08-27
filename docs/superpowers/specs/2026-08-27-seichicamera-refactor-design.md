# SeichiCamera 全面重构设计文档

> **日期：** 2026-08-27
> **状态：** 待实施
> **协议：** MIT（App 代码）/ CC BY-NC-SA 4.0（Anitabi 数据）

## 1. 项目概述

### 1.1 背景

SeichiCamera 是一款专为动漫圣地巡礼设计的 Android 相机应用。当前版本（v1.0）使用 Java + XML 布局 + CameraX 构建，所有逻辑集中在单个 `MainActivity`（717 行），存在以下问题：

- 代码全部堆积在一个 Activity，无分层架构
- UI 基于 XML，不够现代化
- 手势操控未完整实现（缩放/旋转 TODO）
- 语言切换依赖系统设置
- 无地图功能，无法关联圣地点位

### 1.2 重构目标

将 SeichiCamera 从纯相机工具升级为**圣地巡礼一站式应用**：

1. **技术栈全面升级**：Java → Kotlin + Jetpack Compose + Material 3
2. **引入圣地巡礼地图**：接入 Anitabi API，展示圣地点位
3. **地图→相机无缝衔接**：从地图选参考图，一键进入相机叠加拍摄
4. **UI/UX 现代化**：Material You 风格，应用内语言切换
5. **架构规范化**：MVVM + Repository + Hilt + Room

### 1.3 发布计划

- **近期**：开源发布到 GitHub
- **远期**：可能发布到 Google Play（免费）

---

## 2. 技术栈

所有依赖均为 Apache 2.0 / BSD / MIT 协议，完全可商用。

| 类别 | 库 / 技术 | 版本 | 协议 | 说明 |
|---|---|---|---|---|
| 语言 | Kotlin | 2.x | Apache 2.0 | — |
| UI | Jetpack Compose + Material 3 | 最新稳定 | Apache 2.0 | 声明式 UI |
| 相机 | CameraX | 1.4.x | Apache 2.0 | Google 官方相机框架 |
| 地图 | osmdroid | 6.x | Apache 2.0 | OpenStreetMap 瓦片地图 |
| 图片加载 | Coil 3 | 3.x | Apache 2.0 | Compose 原生支持 |
| 网络 | Retrofit + OkHttp | 最新稳定 | Apache 2.0 | HTTP 请求 |
| JSON 解析 | Kotlinx Serialization | 最新稳定 | Apache 2.0 | — |
| 本地数据库 | Room | 最新稳定 | Apache 2.0 | 缓存点位数据 |
| 设置存储 | DataStore Preferences | 最新稳定 | Apache 2.0 | 替代 SharedPreferences |
| 依赖注入 | Hilt | 最新稳定 | Apache 2.0 | — |
| 页面路由 | Navigation Compose | 最新稳定 | Apache 2.0 | 多页面导航 |
| 语言切换 | AppCompatDelegate | — | Apache 2.0 | 应用内即时切换 |
| 协程 | Kotlinx Coroutines | 最新稳定 | Apache 2.0 | 异步处理 |

### 2.1 技术选型说明

**地图：osmdroid vs Mapbox**
- Mapbox SDK v10+ 已改为私有协议，不符合完全开源要求
- osmdroid 使用 OpenStreetMap 瓦片，完全免费，无 API Key 费用
- 对于标注点位的场景完全足够

**图片加载：Coil vs Glide**
- Coil 3 原生支持 Compose（提供 `AsyncImage` 组件）
- Glide 需要额外的 Compose 扩展库
- Coil 3 基于 Kotlin 协程，与项目技术栈一致

---

## 3. 架构设计

### 3.1 代码分层

```
app/
├── ui/                         ← Compose 界面层
│   ├── navigation/             ← 导航图定义、底部导航栏
│   ├── map/                    ← 地图页
│   │   ├── MapScreen.kt
│   │   └── MapViewModel.kt
│   ├── camera/                 ← 相机页
│   │   ├── CameraScreen.kt
│   │   └── CameraViewModel.kt
│   ├── settings/               ← 设置页
│   │   ├── SettingsScreen.kt
│   │   └── SettingsViewModel.kt
│   └── theme/                  ← Material 3 主题定义
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
├── data/                       ← 数据层
│   ├── remote/                 ← Anitabi API
│   │   ├── AnitabiApi.kt       ← Retrofit 接口定义
│   │   └── dto/                ← API 返回数据模型
│   ├── local/                  ← Room 数据库
│   │   ├── AppDatabase.kt
│   │   ├── dao/                ← DAO 接口
│   │   └── entity/             ← 数据库实体
│   └── repository/             ← 统一数据接口
│       ├── BangumiRepository.kt
│       └── CheckInRepository.kt
├── domain/                     ← 业务模型
│   └── model/                  ← 纯数据类（UI 层使用）
├── di/                         ← Hilt 依赖注入配置
│   ├── NetworkModule.kt
│   ├── DatabaseModule.kt
│   └── RepositoryModule.kt
└── SeichiCameraApp.kt          ← Application 类（@HiltAndroidApp）
```

### 3.2 数据流

```
UI (Compose) ←→ ViewModel ←→ Repository ←→ Remote API / Local DB
                    ↑                            ↑
                StateFlow                   Coroutines
```

- UI 层只通过 ViewModel 的 StateFlow 获取数据
- ViewModel 不直接接触网络/数据库，通过 Repository 抽象
- Repository 实现"先本地缓存，无缓存再请求 API"的策略

---

## 4. 页面设计

### 4.1 页面导航结构

```
启动页 (SplashScreen)
└── 主页面 (MainScreen) ← 底部导航栏
    ├── 🗺️ 地图页 (MapScreen)
    │   ├── 搜索栏（输入作品名 / Bangumi ID）
    │   ├── 地图视图（osmdroid，显示圣地点位 Marker）
    │   └── 点位详情弹窗 (BottomSheet)
    │       ├── 点位名称、所属作品
    │       ├── 参考图列表（横向滚动，多图）
    │       ├── [导航] → 跳转 Google Maps 导航
    │       └── [用这张图拍摄] → 携带参考图 URI 跳转相机页
    │
    ├── 📷 相机页 (CameraScreen)
    │   ├── 相机预览（全屏）
    │   ├── 叠加参考图（可拖拽/缩放/旋转/镜像）
    │   ├── 顶部工具栏（宽高比选择、闪光灯、翻转摄像头、网格线）
    │   ├── 右侧：透明度滑条（垂直）
    │   ├── 底部：快速切换参考图（← 上一张 / 下一张 →）
    │   ├── 拍摄按钮
    │   └── 拍摄后弹出：[打卡此点位] + [生成对比图] + [直接分享]
    │
    └── ⚙️ 设置页 (SettingsScreen)
        ├── 语言切换（简体中文 / 繁体中文 / 日本語 / English）
        ├── 宽高比默认值
        ├── 离线缓存管理（查看/清空已缓存作品）
        └── 关于 / 版本 / 数据来源声明
```

### 4.2 核心交互流程

#### 流程 1：地图 → 相机（核心用户路径）

```
搜索作品 → 地图显示点位 → 点击 Marker
→ BottomSheet 展开（显示参考图列表）
→ 点击某张参考图 → [用这张图拍摄]
→ 相机页打开，参考图自动加载，透明度 50%，居中显示
→ 用户调整构图（手势操控）→ 拍摄
→ 弹出：[打卡 ✅] [生成对比图 🖼️] [取消]
```

#### 流程 2：手动选图拍摄（保留原有功能）

```
直接进入相机页 → 点击 [从相册选图]
→ 系统图片选择器 → 选中图片
→ 参考图加载到叠加层 → 拍摄
```

#### 流程 3：多参考图快速切换

```
底部显示缩略图横条（该点位的所有参考图）
← / → 滑动或点击切换
切换时叠加图平滑淡入淡出，位置/缩放保持不变（不重置）
```

#### 流程 4：对比图生成 & 分享

```
拍摄完成 → 点击 [生成对比图]
→ 左：参考图 | 右：实拍图（或上下布局，可选）
→ 底部水印："Data: Anitabi · Photo: SeichiCamera"
→ [保存到相册] + [系统分享菜单]
```

---

## 5. 功能详细设计

### 5.1 相机叠加（重写）

**手势操控**（Compose 原生手势 API）：

| 手势 | 功能 |
|---|---|
| 单指拖拽 | 平移叠加图 |
| 双指捏合 | 缩放叠加图 |
| 双指旋转 | 旋转叠加图 |
| 点击叠加图 | 进入编辑模式（显示变换控制框） |
| 点击预览区 | 退出编辑模式 / 触发对焦 |

**变换状态管理**（CameraViewModel）：

```kotlin
data class OverlayState(
    val imageUri: Uri? = null,
    val alpha: Float = 0.5f,
    val translationX: Float = 0f,
    val translationY: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val isMirrored: Boolean = false,
    val isEditing: Boolean = false
)
```

### 5.2 宽高比预设

| 预设 | 比例 | 用途 |
|---|---|---|
| 16:9（默认）| 1.78:1 | 大多数现代动画 |
| 4:3 | 1.33:1 | 部分老动画 |
| 2.35:1 | 2.35:1 | 宽银幕剧场版 |
| 1:1 | 1:1 | 社交媒体用图 |
| 自由 | 无限制 | 不裁剪 |

实现方式：CameraX `setTargetAspectRatio()` + Compose 取景框 Modifier 裁剪。

### 5.3 应用内语言切换

支持语言：
- 简体中文 (zh-CN)
- 繁体中文-香港 (zh-HK)
- 繁体中文-台湾 (zh-TW)
- 日本語 (ja-JP)
- English (en，默认)

实现方式：
- `AppCompatDelegate.setApplicationLocales()` 即时切换，无需重启 Activity
- 语言偏好存储在 DataStore 中
- 字符串资源文件保持 `values-xx/strings.xml` 结构

### 5.4 打卡记录

- 拍摄后可选"打卡此点位"
- 打卡记录存入 Room `CheckIn` 表
- 地图 Marker 显示打卡状态（已打卡：绿色 ✅ / 未打卡：默认蓝色）
- 打卡记录包含：点位 ID、照片 URI、时间戳、对比图 URI（可选）

### 5.5 对比图生成

- 布局选项：左右并排 / 上下并排
- 上方/左侧：Anitabi 参考图（标注集数/场景）
- 下方/右侧：用户实拍图
- 底部水印：`Data: Anitabi · Photo: SeichiCamera`
- 输出格式：JPEG，保存到 `Pictures/SeichiCamera/Comparisons/`
- 生成后可直接调用系统分享 Intent

### 5.6 导航跳转

- 点位详情页提供"导航"按钮
- 构造 `geo:` URI 或 Google Maps Deep Link
- 优先跳转 Google Maps，未安装则跳转浏览器

### 5.7 离线缓存

- 按作品维度缓存：用户可在作品详情页或设置页触发"下载离线数据"
- 缓存内容：作品元数据 + 全部点位坐标 + 参考图（`?plan=h360` 缩略图，约 50-100KB/张）
- 设置页提供缓存管理：按作品查看/删除、一键清空
- 自动过期：缓存超过 30 天标记为"可更新"（不自动删除）

---

## 6. 数据层设计

### 6.1 Anitabi API 接口

基础地址：`https://api.anitabi.cn/`
图片地址：`https://image.anitabi.cn/`

```kotlin
interface AnitabiApi {
    // 获取作品的圣地点位（精简版）
    @GET("bangumi/{subjectId}/lite")
    suspend fun getBangumiPoints(
        @Path("subjectId") subjectId: Int
    ): BangumiResponse

    // 搜索作品（如 API 支持）
    @GET("search")
    suspend fun searchBangumi(
        @Query("q") query: String
    ): List<BangumiSearchResult>
}
```

图片 URL 参数：
- `?plan=h160` — 缩略图（列表用）
- `?plan=h360` — 标清（叠加用、离线缓存用）

### 6.2 Room 数据库

```kotlin
@Entity(tableName = "bangumi")
data class BangumiEntity(
    @PrimaryKey val id: Int,          // Bangumi Subject ID
    val title: String,
    val coverUrl: String,
    val region: String?,
    val zoom: Float?,
    val cachedAt: Long,               // 缓存时间戳
    val isCached: Boolean = false     // 是否已完整离线缓存
)

@Entity(
    tableName = "sacred_point",
    foreignKeys = [ForeignKey(
        entity = BangumiEntity::class,
        parentColumns = ["id"],
        childColumns = ["bangumiId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class SacredPointEntity(
    @PrimaryKey val id: String,
    val bangumiId: Int,
    val name: String?,
    val latitude: Double,
    val longitude: Double,
    val imageUrls: List<String>,      // JSON 序列化存储
    val originUrl: String?,           // Anitabi 来源链接
    val ep: String?                   // 集数
)

@Entity(
    tableName = "check_in",
    foreignKeys = [ForeignKey(
        entity = SacredPointEntity::class,
        parentColumns = ["id"],
        childColumns = ["pointId"]
    )]
)
data class CheckInEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pointId: String,
    val photoUri: String,
    val timestamp: Long,
    val comparisonUri: String?        // 对比图 URI（可选）
)
```

### 6.3 Repository 策略

```kotlin
class BangumiRepository @Inject constructor(
    private val api: AnitabiApi,
    private val bangumiDao: BangumiDao,
    private val pointDao: SacredPointDao
) {
    // 获取作品点位：先查本地，无缓存再请求 API
    suspend fun getBangumiPoints(subjectId: Int): Result<BangumiWithPoints> {
        // 1. 查本地缓存
        val cached = bangumiDao.getWithPoints(subjectId)
        if (cached != null) return Result.success(cached)

        // 2. 无缓存，请求 API
        return try {
            val response = api.getBangumiPoints(subjectId)
            // 3. 写入本地
            bangumiDao.insert(response.toBangumiEntity())
            pointDao.insertAll(response.toPointEntities())
            Result.success(bangumiDao.getWithPoints(subjectId)!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

## 7. 合规要求

### 7.1 Anitabi 数据协议：CC BY-NC-SA 4.0

| 条款 | 要求 | App 中的实现 |
|---|---|---|
| **BY（署名）** | 标注数据来源 | 每个点位详情页底部标注 "数据来源：Anitabi"，附跳转链接 |
| **NC（非商业）** | 不得用于商业目的 | App 免费，不含广告。如未来商业化需单独联系 Anitabi 授权 |
| **SA（相同方式共享）** | 衍生作品使用兼容协议 | App 代码开源，GitHub 上使用 MIT 协议 |
| **对比图水印** | — | 生成的对比图底部加 "Data: Anitabi · Photo: SeichiCamera" |

### 7.2 App 代码协议

- 代码使用 **MIT License**（与当前 LICENSE 文件一致）
- 所有第三方依赖均为 Apache 2.0 / BSD / MIT，兼容商用

---

## 8. 竞品分析

| App | 相机叠加 | 地图点位 | 开源 | 现代 Android | 多语言 |
|---|---|---|---|---|---|
| 舞台めぐり | ✅（官方授权） | ✅ | ❌ | 部分 | 日文 |
| メモリーグラフ | ✅（通用） | ❌ | ❌ | ✅ | 日文 |
| 巡カメ | ✅ | ❌ | ❌ | 未知 | 日文 |
| 聖地巡礼カメラ | ✅ | ❌ | ❌ | ❌（年久失修） | 日文 |
| **SeichiCamera（重构后）** | **✅** | **✅（Anitabi）** | **✅** | **✅** | **✅（4语言）** |

**差异化优势：** 市场上唯一同时具备"相机叠加 + 圣地地图 + 开源 + 现代 Android + 多语言"的应用。

---

## 9. 下期规划（Phase 2）

> 以下功能不在本期实施范围内，但架构设计已预留扩展点。

### 9.1 附近点位提醒（GPS 围栏）
- 基于用户 GPS 位置，检测附近 500 米内的未打卡圣地点位
- 通过 Android Notification 推送提醒
- **预留点：** `SacredPointEntity` 已包含经纬度，`CheckInEntity` 已包含打卡状态

### 9.2 图层参数记忆
- 每个点位记住上次调整好的叠加图位置、缩放、透明度
- 下次打开同一个点位时自动恢复
- **预留点：** 可在 `CheckIn` 表或新建 `OverlayPreset` 表存储变换参数

### 9.3 社区功能
- 用户提交新点位（补充 Anitabi 没有的内容）
- 需要后端服务支持（可考虑 Firebase 或自建）
- **预留点：** Repository 层已抽象，新增数据源不影响现有代码

### 9.4 桌面 Widget
- 桌面小组件显示最近的未打卡点位
- Glance（Jetpack Compose for Widgets）实现
- **预留点：** 数据层查询接口可直接复用

---

## 10. 迁移策略

由于是全面重构（Java → Kotlin + Compose），采用**全量重写**而非渐进迁移：

1. 创建新的 Kotlin + Compose 项目结构
2. 保持 `applicationId` 不变（`com.tnt.seichicamera`），确保可覆盖安装
3. 迁移所有字符串资源文件（`values-*/strings.xml`）
4. 迁移图标资源（`mipmap-*`, `drawable`）
5. 原有 Java 代码仅作参考，不直接复用
