# SeichiCamera Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rewrite SeichiCamera from Java/XML to Kotlin/Compose with MVVM architecture, integrating Anitabi API map, overlay camera, and all Phase 1 features.

**Architecture:** MVVM + Repository pattern with Hilt DI. Single-Activity Compose host with Navigation Compose managing three screens (Map, Camera, Settings). Room for local caching, Retrofit for Anitabi API, CameraX for camera, osmdroid for map.

**Tech Stack:** Kotlin 2.x, Jetpack Compose + Material 3, CameraX 1.4.x, osmdroid 6.x, Coil 3, Retrofit + OkHttp, Kotlinx Serialization, Room, DataStore, Hilt, Navigation Compose

**Spec:** `docs/superpowers/specs/2026-08-27-seichicamera-refactor-design.md`

## Global Constraints

- `applicationId` = `com.tnt.seichicamera` (must not change, for upgrade compatibility)
- `minSdk` = 24, `targetSdk` = 35, `compileSdk` = 35
- `namespace` = `com.tnt.seichicamera`
- All dependencies must be Apache 2.0 / MIT / BSD (no GPL, no proprietary)
- Kotlin source in `app/src/main/java/com/tnt/seichicamera/` (keep `java` dir name for Gradle convention)
- String resources: EN (default), zh-CN, zh-HK, zh-TW, ja-JP
- Anitabi images: always use `?plan=h360` for overlay/cache, `?plan=h160` for thumbnails
- Anitabi attribution required: "Data: Anitabi" with link to `originUrl`
- Java source files in `app/src/main/java/com/tnt/seichicamera/` will be deleted after full migration

---

### Task 1: Project Foundation & Theme

**Files:**
- Modify: `gradle/libs.versions.toml` (rewrite with all dependencies)
- Modify: `build.gradle` (root, add Kotlin + Hilt + KSP plugins)
- Modify: `app/build.gradle` (rewrite for Kotlin + Compose)
- Modify: `gradle.properties` (add Compose flag)
- Delete: `app/src/main/java/com/tnt/seichicamera/MainActivity.java`
- Delete: `app/src/main/java/com/tnt/seichicamera/MyApplication.java`
- Delete: `app/src/main/java/com/tnt/seichicamera/SettingsActivity.java`
- Delete: `app/src/main/java/com/tnt/seichicamera/GridView.java`
- Delete: all `res/layout/*.xml`, `res/xml/preferences.xml`
- Create: `app/src/main/java/com/tnt/seichicamera/SeichiCameraApp.kt`
- Create: `app/src/main/java/com/tnt/seichicamera/MainActivity.kt`
- Create: `app/src/main/java/com/tnt/seichicamera/ui/theme/Color.kt`
- Create: `app/src/main/java/com/tnt/seichicamera/ui/theme/Type.kt`
- Create: `app/src/main/java/com/tnt/seichicamera/ui/theme/Theme.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: nothing (first task)
- Produces: compilable Kotlin + Compose project with Material 3 theme, `SeichiCameraApp` (Hilt application), `MainActivity` (empty Compose host)

- [ ] **Step 1: Rewrite `gradle/libs.versions.toml`**

Replace entire file with:

```toml
[versions]
agp = "8.13.0"
kotlin = "2.1.21"
ksp = "2.1.21-2.0.1"
compose-bom = "2025.06.01"
camerax = "1.4.2"
hilt = "2.56.2"
hilt-navigation-compose = "1.2.0"
room = "2.7.1"
retrofit = "2.11.0"
okhttp = "4.12.0"
coil = "3.2.0"
datastore = "1.1.7"
navigation-compose = "2.9.0"
osmdroid = "6.1.20"
kotlinx-serialization = "1.8.1"
coroutines = "1.10.2"
appcompat = "1.7.0"
core-ktx = "1.16.0"
activity-compose = "1.10.1"
lifecycle = "2.9.0"
junit = "4.13.2"
junit-ext = "1.2.1"
espresso = "3.6.1"

[libraries]
# Compose BOM
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-material-icons = { group = "androidx.compose.material", name = "material-icons-extended" }

# AndroidX
core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "core-ktx" }
appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }
activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activity-compose" }
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation-compose" }

# CameraX
camerax-core = { group = "androidx.camera", name = "camera-core", version.ref = "camerax" }
camerax-camera2 = { group = "androidx.camera", name = "camera-camera2", version.ref = "camerax" }
camerax-lifecycle = { group = "androidx.camera", name = "camera-lifecycle", version.ref = "camerax" }
camerax-view = { group = "androidx.camera", name = "camera-view", version.ref = "camerax" }

# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hilt-navigation-compose" }

# Room
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

# Network
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-kotlinx-serialization = { group = "com.squareup.retrofit2", name = "converter-kotlinx-serialization", version.ref = "retrofit" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinx-serialization" }

# Coil (image loading)
coil-compose = { group = "io.coil-kt.coil3", name = "coil-compose", version.ref = "coil" }
coil-network-okhttp = { group = "io.coil-kt.coil3", name = "coil-network-okhttp", version.ref = "coil" }

# DataStore
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }

# Map
osmdroid = { group = "org.osmdroid", name = "osmdroid-android", version.ref = "osmdroid" }

# Coroutines
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }

# Testing
junit = { group = "junit", name = "junit", version.ref = "junit" }
junit-ext = { group = "androidx.test.ext", name = "junit", version.ref = "junit-ext" }
espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espresso" }
compose-ui-test = { group = "androidx.compose.ui", name = "ui-test-junit4" }
compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

- [ ] **Step 2: Rewrite root `build.gradle`**

Replace entire file with:

```groovy
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}
```

- [ ] **Step 3: Rewrite `app/build.gradle`**

Replace entire file with:

```groovy
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace 'com.tnt.seichicamera'
    compileSdk 35

    defaultConfig {
        applicationId "com.tnt.seichicamera"
        minSdk 24
        targetSdk 35
        versionCode 2
        versionName "2.0.0"
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = '17'
    }

    buildFeatures {
        compose true
    }
}

dependencies {
    // Compose BOM
    def composeBom = platform(libs.compose.bom)
    implementation composeBom
    androidTestImplementation composeBom

    implementation libs.compose.ui
    implementation libs.compose.ui.graphics
    implementation libs.compose.ui.tooling.preview
    implementation libs.compose.material3
    implementation libs.compose.material.icons
    debugImplementation libs.compose.ui.tooling

    // AndroidX
    implementation libs.core.ktx
    implementation libs.appcompat
    implementation libs.activity.compose
    implementation libs.lifecycle.runtime.compose
    implementation libs.lifecycle.viewmodel.compose
    implementation libs.navigation.compose

    // CameraX
    implementation libs.camerax.core
    implementation libs.camerax.camera2
    implementation libs.camerax.lifecycle
    implementation libs.camerax.view

    // Hilt
    implementation libs.hilt.android
    ksp libs.hilt.compiler
    implementation libs.hilt.navigation.compose

    // Room
    implementation libs.room.runtime
    implementation libs.room.ktx
    ksp libs.room.compiler

    // Network
    implementation libs.retrofit
    implementation libs.retrofit.kotlinx.serialization
    implementation libs.okhttp
    implementation libs.okhttp.logging
    implementation libs.kotlinx.serialization.json

    // Coil
    implementation libs.coil.compose
    implementation libs.coil.network.okhttp

    // DataStore
    implementation libs.datastore.preferences

    // Map
    implementation libs.osmdroid

    // Coroutines
    implementation libs.coroutines.android

    // Testing
    testImplementation libs.junit
    testImplementation libs.coroutines.test
    androidTestImplementation libs.junit.ext
    androidTestImplementation libs.espresso.core
    androidTestImplementation libs.compose.ui.test
    debugImplementation libs.compose.ui.test.manifest
}
```

- [ ] **Step 4: Create theme files**

Create `app/src/main/java/com/tnt/seichicamera/ui/theme/Color.kt`:

```kotlin
package com.tnt.seichicamera.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Camera UI colors
val SemiTransparentBlack = Color(0x80000000)
val OverlayBorder = Color(0xFFFFFFFF)
```

Create `app/src/main/java/com/tnt/seichicamera/ui/theme/Type.kt`:

```kotlin
package com.tnt.seichicamera.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)
```

Create `app/src/main/java/com/tnt/seichicamera/ui/theme/Theme.kt`:

```kotlin
package com.tnt.seichicamera.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun SeichiCameraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

- [ ] **Step 5: Create Application class and MainActivity**

Create `app/src/main/java/com/tnt/seichicamera/SeichiCameraApp.kt`:

```kotlin
package com.tnt.seichicamera

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SeichiCameraApp : Application()
```

Create `app/src/main/java/com/tnt/seichicamera/MainActivity.kt`:

```kotlin
package com.tnt.seichicamera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tnt.seichicamera.ui.theme.SeichiCameraTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SeichiCameraTheme {
                // Navigation will be added in Task 5
                androidx.compose.material3.Text("SeichiCamera v2")
            }
        }
    }
}
```

- [ ] **Step 6: Update AndroidManifest.xml**

Replace `AndroidManifest.xml` with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-feature android:name="android.hardware.camera.any" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />

    <application
        android:name=".SeichiCameraApp"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.SeichiCamera"
        tools:targetApi="31">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:configChanges="orientation|screenSize|screenLayout|keyboardHidden|locale|layoutDirection"
            android:screenOrientation="portrait"
            android:theme="@style/Theme.SeichiCamera"
            tools:ignore="LockedOrientationActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>

</manifest>
```

- [ ] **Step 7: Delete old Java source files and XML layouts**

Delete these files:
- `app/src/main/java/com/tnt/seichicamera/MainActivity.java`
- `app/src/main/java/com/tnt/seichicamera/MyApplication.java`
- `app/src/main/java/com/tnt/seichicamera/SettingsActivity.java`
- `app/src/main/java/com/tnt/seichicamera/GridView.java`
- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/res/layout/settings_activity.xml`
- `app/src/main/res/xml/preferences.xml`

- [ ] **Step 8: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL — the app compiles with Kotlin + Compose, displays "SeichiCamera v2" text.

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "refactor: migrate to Kotlin + Compose + Hilt foundation (Task 1)"
```

---

### Task 2: Domain Models & Room Database

**Files:**
- Create: `app/src/main/java/com/tnt/seichicamera/domain/model/Bangumi.kt`
- Create: `app/src/main/java/com/tnt/seichicamera/domain/model/SacredPoint.kt`
- Create: `app/src/main/java/com/tnt/seichicamera/domain/model/CheckIn.kt`
- Create: `app/src/main/java/com/tnt/seichicamera/data/local/entity/BangumiEntity.kt`
- Create: `app/src/main/java/com/tnt/seichicamera/data/local/entity/SacredPointEntity.kt`
- Create: `app/src/main/java/com/tnt/seichicamera/data/local/entity/CheckInEntity.kt`
- Create: `app/src/main/java/com/tnt/seichicamera/data/local/converter/Converters.kt`
- Create: `app/src/main/java/com/tnt/seichicamera/data/local/dao/BangumiDao.kt`
- Create: `app/src/main/java/com/tnt/seichicamera/data/local/dao/SacredPointDao.kt`
- Create: `app/src/main/java/com/tnt/seichicamera/data/local/dao/CheckInDao.kt`
- Create: `app/src/main/java/com/tnt/seichicamera/data/local/AppDatabase.kt`
- Test: `app/src/test/java/com/tnt/seichicamera/domain/model/ModelMappingTest.kt`

**Interfaces:**
- Consumes: nothing
- Produces:
  - `Bangumi(id: Int, title: String, coverUrl: String, region: String?, zoom: Float?)` — domain model
  - `SacredPoint(id: String, bangumiId: Int, name: String?, lat: Double, lng: Double, imageUrls: List<String>, originUrl: String?, ep: String?)` — domain model
  - `CheckIn(id: Long, pointId: String, photoUri: String, timestamp: Long, comparisonUri: String?)` — domain model
  - `BangumiDao.getById(id: Int): BangumiEntity?`
  - `BangumiDao.insert(bangumi: BangumiEntity)`
  - `SacredPointDao.getByBangumiId(bangumiId: Int): List<SacredPointEntity>`
  - `SacredPointDao.insertAll(points: List<SacredPointEntity>)`
  - `CheckInDao.getByPointId(pointId: String): CheckInEntity?`
  - `CheckInDao.insert(checkIn: CheckInEntity): Long`
  - `CheckInDao.getAllCheckedInPointIds(): Flow<List<String>>`
  - `AppDatabase` — Room database with all DAOs

- [ ] **Step 1: Create domain models**

Create `app/src/main/java/com/tnt/seichicamera/domain/model/Bangumi.kt`:

```kotlin
package com.tnt.seichicamera.domain.model

data class Bangumi(
    val id: Int,
    val title: String,
    val coverUrl: String,
    val region: String?,
    val zoom: Float?
)
```

Create `app/src/main/java/com/tnt/seichicamera/domain/model/SacredPoint.kt`:

```kotlin
package com.tnt.seichicamera.domain.model

data class SacredPoint(
    val id: String,
    val bangumiId: Int,
    val name: String?,
    val latitude: Double,
    val longitude: Double,
    val imageUrls: List<String>,
    val originUrl: String?,
    val ep: String?
)
```

Create `app/src/main/java/com/tnt/seichicamera/domain/model/CheckIn.kt`:

```kotlin
package com.tnt.seichicamera.domain.model

data class CheckIn(
    val id: Long = 0,
    val pointId: String,
    val photoUri: String,
    val timestamp: Long,
    val comparisonUri: String? = null
)
```

- [ ] **Step 2: Create Room entities**

Create `app/src/main/java/com/tnt/seichicamera/data/local/entity/BangumiEntity.kt`:

```kotlin
package com.tnt.seichicamera.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tnt.seichicamera.domain.model.Bangumi

@Entity(tableName = "bangumi")
data class BangumiEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val coverUrl: String,
    val region: String?,
    val zoom: Float?,
    val cachedAt: Long,
    val isCached: Boolean = false
) {
    fun toDomain() = Bangumi(
        id = id,
        title = title,
        coverUrl = coverUrl,
        region = region,
        zoom = zoom
    )

    companion object {
        fun fromDomain(domain: Bangumi, cachedAt: Long = System.currentTimeMillis()) = BangumiEntity(
            id = domain.id,
            title = domain.title,
            coverUrl = domain.coverUrl,
            region = domain.region,
            zoom = domain.zoom,
            cachedAt = cachedAt
        )
    }
}
```

Create `app/src/main/java/com/tnt/seichicamera/data/local/entity/SacredPointEntity.kt`:

```kotlin
package com.tnt.seichicamera.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tnt.seichicamera.domain.model.SacredPoint

@Entity(
    tableName = "sacred_point",
    foreignKeys = [ForeignKey(
        entity = BangumiEntity::class,
        parentColumns = ["id"],
        childColumns = ["bangumiId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("bangumiId")]
)
data class SacredPointEntity(
    @PrimaryKey val id: String,
    val bangumiId: Int,
    val name: String?,
    val latitude: Double,
    val longitude: Double,
    val imageUrls: List<String>,
    val originUrl: String?,
    val ep: String?
) {
    fun toDomain() = SacredPoint(
        id = id,
        bangumiId = bangumiId,
        name = name,
        latitude = latitude,
        longitude = longitude,
        imageUrls = imageUrls,
        originUrl = originUrl,
        ep = ep
    )

    companion object {
        fun fromDomain(domain: SacredPoint) = SacredPointEntity(
            id = domain.id,
            bangumiId = domain.bangumiId,
            name = domain.name,
            latitude = domain.latitude,
            longitude = domain.longitude,
            imageUrls = domain.imageUrls,
            originUrl = domain.originUrl,
            ep = domain.ep
        )
    }
}
```

Create `app/src/main/java/com/tnt/seichicamera/data/local/entity/CheckInEntity.kt`:

```kotlin
package com.tnt.seichicamera.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tnt.seichicamera.domain.model.CheckIn

@Entity(
    tableName = "check_in",
    foreignKeys = [ForeignKey(
        entity = SacredPointEntity::class,
        parentColumns = ["id"],
        childColumns = ["pointId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("pointId")]
)
data class CheckInEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pointId: String,
    val photoUri: String,
    val timestamp: Long,
    val comparisonUri: String?
) {
    fun toDomain() = CheckIn(
        id = id,
        pointId = pointId,
        photoUri = photoUri,
        timestamp = timestamp,
        comparisonUri = comparisonUri
    )

    companion object {
        fun fromDomain(domain: CheckIn) = CheckInEntity(
            id = domain.id,
            pointId = domain.pointId,
            photoUri = domain.photoUri,
            timestamp = domain.timestamp,
            comparisonUri = domain.comparisonUri
        )
    }
}
```

- [ ] **Step 3: Create TypeConverter and DAOs**

Create `app/src/main/java/com/tnt/seichicamera/data/local/converter/Converters.kt`:

```kotlin
package com.tnt.seichicamera.data.local.converter

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String = Json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> = Json.decodeFromString(value)
}
```

Create `app/src/main/java/com/tnt/seichicamera/data/local/dao/BangumiDao.kt`:

```kotlin
package com.tnt.seichicamera.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tnt.seichicamera.data.local.entity.BangumiEntity

@Dao
interface BangumiDao {
    @Query("SELECT * FROM bangumi WHERE id = :id")
    suspend fun getById(id: Int): BangumiEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bangumi: BangumiEntity)

    @Query("DELETE FROM bangumi WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT * FROM bangumi WHERE isCached = 1")
    suspend fun getAllCached(): List<BangumiEntity>

    @Query("UPDATE bangumi SET isCached = :isCached WHERE id = :id")
    suspend fun updateCachedStatus(id: Int, isCached: Boolean)

    @Query("DELETE FROM bangumi")
    suspend fun deleteAll()
}
```

Create `app/src/main/java/com/tnt/seichicamera/data/local/dao/SacredPointDao.kt`:

```kotlin
package com.tnt.seichicamera.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tnt.seichicamera.data.local.entity.SacredPointEntity

@Dao
interface SacredPointDao {
    @Query("SELECT * FROM sacred_point WHERE bangumiId = :bangumiId")
    suspend fun getByBangumiId(bangumiId: Int): List<SacredPointEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(points: List<SacredPointEntity>)

    @Query("DELETE FROM sacred_point WHERE bangumiId = :bangumiId")
    suspend fun deleteByBangumiId(bangumiId: Int)

    @Query("SELECT * FROM sacred_point WHERE id = :id")
    suspend fun getById(id: String): SacredPointEntity?
}
```

Create `app/src/main/java/com/tnt/seichicamera/data/local/dao/CheckInDao.kt`:

```kotlin
package com.tnt.seichicamera.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.tnt.seichicamera.data.local.entity.CheckInEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckInDao {
    @Insert
    suspend fun insert(checkIn: CheckInEntity): Long

    @Query("SELECT * FROM check_in WHERE pointId = :pointId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getByPointId(pointId: String): CheckInEntity?

    @Query("SELECT DISTINCT pointId FROM check_in")
    fun getAllCheckedInPointIds(): Flow<List<String>>

    @Query("SELECT * FROM check_in ORDER BY timestamp DESC")
    fun getAllCheckIns(): Flow<List<CheckInEntity>>
}
```

- [ ] **Step 4: Create AppDatabase**

Create `app/src/main/java/com/tnt/seichicamera/data/local/AppDatabase.kt`:

```kotlin
package com.tnt.seichicamera.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tnt.seichicamera.data.local.converter.Converters
import com.tnt.seichicamera.data.local.dao.BangumiDao
import com.tnt.seichicamera.data.local.dao.CheckInDao
import com.tnt.seichicamera.data.local.dao.SacredPointDao
import com.tnt.seichicamera.data.local.entity.BangumiEntity
import com.tnt.seichicamera.data.local.entity.CheckInEntity
import com.tnt.seichicamera.data.local.entity.SacredPointEntity

@Database(
    entities = [BangumiEntity::class, SacredPointEntity::class, CheckInEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bangumiDao(): BangumiDao
    abstract fun sacredPointDao(): SacredPointDao
    abstract fun checkInDao(): CheckInDao
}
```

- [ ] **Step 5: Write model mapping tests**

Create `app/src/test/java/com/tnt/seichicamera/domain/model/ModelMappingTest.kt`:

```kotlin
package com.tnt.seichicamera.domain.model

import com.tnt.seichicamera.data.local.entity.BangumiEntity
import com.tnt.seichicamera.data.local.entity.CheckInEntity
import com.tnt.seichicamera.data.local.entity.SacredPointEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelMappingTest {

    @Test
    fun `BangumiEntity round-trips through domain model`() {
        val domain = Bangumi(id = 1, title = "Steins;Gate", coverUrl = "https://img.example.com/cover.jpg", region = "Tokyo", zoom = 15f)
        val entity = BangumiEntity.fromDomain(domain, cachedAt = 1000L)
        val result = entity.toDomain()
        assertEquals(domain, result)
    }

    @Test
    fun `SacredPointEntity round-trips through domain model`() {
        val domain = SacredPoint(
            id = "point-1", bangumiId = 1, name = "Akihabara Radio Kaikan",
            latitude = 35.6984, longitude = 139.7714,
            imageUrls = listOf("https://img.example.com/1.jpg", "https://img.example.com/2.jpg"),
            originUrl = "https://anitabi.cn/point/1", ep = "EP01"
        )
        val entity = SacredPointEntity.fromDomain(domain)
        val result = entity.toDomain()
        assertEquals(domain, result)
    }

    @Test
    fun `CheckInEntity round-trips through domain model`() {
        val domain = CheckIn(id = 0, pointId = "point-1", photoUri = "content://media/photo/1", timestamp = 1000L, comparisonUri = null)
        val entity = CheckInEntity.fromDomain(domain)
        val result = entity.toDomain()
        assertEquals(domain, result)
    }
}
```

- [ ] **Step 6: Run tests**

Run: `./gradlew test`
Expected: All 3 tests PASS.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: add domain models and Room database layer (Task 2)"
```

---

### Task 3: Anitabi API & Network Layer

**Files:**
- Create: `app/src/main/java/com/tnt/seichicamera/data/remote/dto/AnitabiDto.kt`
- Create: `app/src/main/java/com/tnt/seichicamera/data/remote/AnitabiApi.kt`
- Test: `app/src/test/java/com/tnt/seichicamera/data/remote/dto/AnitabiDtoTest.kt`

**Interfaces:**
- Consumes: `SacredPointEntity`, `BangumiEntity` from Task 2
- Produces:
  - `AnitabiApi.getBangumiPoints(subjectId: Int): BangumiResponse`
  - `BangumiResponse.toBangumiEntity(): BangumiEntity`
  - `BangumiResponse.toPointEntities(): List<SacredPointEntity>`

- [ ] **Step 1: Create Anitabi DTOs**

Create `app/src/main/java/com/tnt/seichicamera/data/remote/dto/AnitabiDto.kt`:

```kotlin
package com.tnt.seichicamera.data.remote.dto

import com.tnt.seichicamera.data.local.entity.BangumiEntity
import com.tnt.seichicamera.data.local.entity.SacredPointEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BangumiResponse(
    @SerialName("id") val id: Int,
    @SerialName("cn") val titleCn: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("cover") val cover: String? = null,
    @SerialName("zoom") val zoom: Float? = null,
    @SerialName("city") val city: String? = null,
    @SerialName("litePoints") val litePoints: List<LitePoint>? = null
) {
    fun toBangumiEntity(): BangumiEntity = BangumiEntity(
        id = id,
        title = titleCn ?: title ?: "Unknown",
        coverUrl = cover?.let { "https://image.anitabi.cn/bangumi/$it" } ?: "",
        region = city,
        zoom = zoom,
        cachedAt = System.currentTimeMillis()
    )

    fun toPointEntities(): List<SacredPointEntity> =
        litePoints?.mapIndexed { index, point ->
            SacredPointEntity(
                id = "${id}_$index",
                bangumiId = id,
                name = point.name,
                latitude = point.geo?.get(1) ?: 0.0,
                longitude = point.geo?.get(0) ?: 0.0,
                imageUrls = point.image?.let { img ->
                    listOf("https://image.anitabi.cn/point/$img?plan=h360")
                } ?: emptyList(),
                originUrl = point.origin,
                ep = point.ep
            )
        } ?: emptyList()
}

@Serializable
data class LitePoint(
    @SerialName("name") val name: String? = null,
    @SerialName("geo") val geo: List<Double>? = null,
    @SerialName("image") val image: String? = null,
    @SerialName("ep") val ep: String? = null,
    @SerialName("s") val s: String? = null,
    @SerialName("origin") val origin: String? = null
)
```

- [ ] **Step 2: Create Retrofit API interface**

Create `app/src/main/java/com/tnt/seichicamera/data/remote/AnitabiApi.kt`:

```kotlin
package com.tnt.seichicamera.data.remote

import com.tnt.seichicamera.data.remote.dto.BangumiResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface AnitabiApi {
    companion object {
        const val BASE_URL = "https://api.anitabi.cn/"
    }

    @GET("bangumi/{subjectId}/lite")
    suspend fun getBangumiPoints(@Path("subjectId") subjectId: Int): BangumiResponse
}
```

- [ ] **Step 3: Write DTO mapping tests**

Create `app/src/test/java/com/tnt/seichicamera/data/remote/dto/AnitabiDtoTest.kt`:

```kotlin
package com.tnt.seichicamera.data.remote.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnitabiDtoTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val sampleJson = """
        {
            "id": 204135,
            "cn": "摇曳露营△",
            "title": "ゆるキャン△",
            "cover": "abc123.jpg",
            "zoom": 10.0,
            "city": "山梨県",
            "litePoints": [
                {
                    "name": "本栖湖",
                    "geo": [138.5833, 35.4500],
                    "image": "img001.jpg",
                    "ep": "EP01",
                    "origin": "https://anitabi.cn/map?id=204135"
                },
                {
                    "name": "浩庵キャンプ場",
                    "geo": [138.5700, 35.4600],
                    "image": "img002.jpg",
                    "ep": "EP01"
                }
            ]
        }
    """.trimIndent()

    @Test
    fun `parse BangumiResponse from JSON`() {
        val response = json.decodeFromString<BangumiResponse>(sampleJson)
        assertEquals(204135, response.id)
        assertEquals("摇曳露营△", response.titleCn)
        assertEquals("ゆるキャン△", response.title)
        assertEquals(2, response.litePoints?.size)
    }

    @Test
    fun `BangumiResponse maps to BangumiEntity with CN title preferred`() {
        val response = json.decodeFromString<BangumiResponse>(sampleJson)
        val entity = response.toBangumiEntity()
        assertEquals(204135, entity.id)
        assertEquals("摇曳露营△", entity.title)
        assertEquals("山梨県", entity.region)
    }

    @Test
    fun `BangumiResponse maps litePoints to SacredPointEntities`() {
        val response = json.decodeFromString<BangumiResponse>(sampleJson)
        val points = response.toPointEntities()
        assertEquals(2, points.size)

        val first = points[0]
        assertEquals("204135_0", first.id)
        assertEquals("本栖湖", first.name)
        assertEquals(35.4500, first.latitude, 0.001)
        assertEquals(138.5833, first.longitude, 0.001)
        assertTrue(first.imageUrls[0].contains("?plan=h360"))
        assertEquals("EP01", first.ep)
    }

    @Test
    fun `BangumiResponse with no litePoints returns empty list`() {
        val response = json.decodeFromString<BangumiResponse>("""{"id": 1}""")
        assertEquals(0, response.toPointEntities().size)
    }
}
```

- [ ] **Step 4: Run tests**

Run: `./gradlew test`
Expected: All 4 new tests PASS.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add Anitabi API client and DTOs (Task 3)"
```

---

### Task 4: Repositories & Hilt DI Modules

**Files:**
- Create: `app/src/main/java/com/tnt/seichicamera/data/repository/BangumiRepository.kt`
- Create: `app/src/main/java/com/tnt/seichicamera/data/repository/CheckInRepository.kt`
- Create: `app/src/main/java/com/tnt/seichicamera/di/NetworkModule.kt`
- Create: `app/src/main/java/com/tnt/seichicamera/di/DatabaseModule.kt`
- Create: `app/src/main/java/com/tnt/seichicamera/di/RepositoryModule.kt`

**Interfaces:**
- Consumes: `AnitabiApi` (Task 3), `BangumiDao`, `SacredPointDao`, `CheckInDao`, `AppDatabase` (Task 2)
- Produces:
  - `BangumiRepository.getBangumiPoints(subjectId: Int): Result<Pair<Bangumi, List<SacredPoint>>>`
  - `BangumiRepository.cacheOffline(subjectId: Int): Result<Unit>`
  - `BangumiRepository.getCachedBangumis(): List<Bangumi>`
  - `BangumiRepository.clearCache(subjectId: Int)`
  - `BangumiRepository.clearAllCache()`
  - `CheckInRepository.checkIn(pointId: String, photoUri: String, comparisonUri: String?): Long`
  - `CheckInRepository.getCheckedInPointIds(): Flow<List<String>>`
  - `CheckInRepository.isCheckedIn(pointId: String): Boolean`

- [ ] **Step 1: Create BangumiRepository**

Create `app/src/main/java/com/tnt/seichicamera/data/repository/BangumiRepository.kt`:

```kotlin
package com.tnt.seichicamera.data.repository

import com.tnt.seichicamera.data.local.dao.BangumiDao
import com.tnt.seichicamera.data.local.dao.SacredPointDao
import com.tnt.seichicamera.data.remote.AnitabiApi
import com.tnt.seichicamera.domain.model.Bangumi
import com.tnt.seichicamera.domain.model.SacredPoint
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BangumiRepository @Inject constructor(
    private val api: AnitabiApi,
    private val bangumiDao: BangumiDao,
    private val pointDao: SacredPointDao
) {
    suspend fun getBangumiPoints(subjectId: Int): Result<Pair<Bangumi, List<SacredPoint>>> {
        // 1. Try local cache first
        val cachedBangumi = bangumiDao.getById(subjectId)
        if (cachedBangumi != null) {
            val cachedPoints = pointDao.getByBangumiId(subjectId)
            if (cachedPoints.isNotEmpty()) {
                return Result.success(
                    cachedBangumi.toDomain() to cachedPoints.map { it.toDomain() }
                )
            }
        }

        // 2. Fetch from API
        return try {
            val response = api.getBangumiPoints(subjectId)
            val bangumiEntity = response.toBangumiEntity()
            val pointEntities = response.toPointEntities()

            // 3. Save to local
            bangumiDao.insert(bangumiEntity)
            pointDao.insertAll(pointEntities)

            Result.success(
                bangumiEntity.toDomain() to pointEntities.map { it.toDomain() }
            )
        } catch (e: Exception) {
            // 4. If API fails, try cache even if empty
            val fallback = bangumiDao.getById(subjectId)
            if (fallback != null) {
                val fallbackPoints = pointDao.getByBangumiId(subjectId)
                Result.success(fallback.toDomain() to fallbackPoints.map { it.toDomain() })
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun cacheOffline(subjectId: Int): Result<Unit> {
        return try {
            val response = api.getBangumiPoints(subjectId)
            bangumiDao.insert(response.toBangumiEntity())
            pointDao.insertAll(response.toPointEntities())
            bangumiDao.updateCachedStatus(subjectId, true)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCachedBangumis(): List<Bangumi> =
        bangumiDao.getAllCached().map { it.toDomain() }

    suspend fun clearCache(subjectId: Int) {
        bangumiDao.deleteById(subjectId)
        // Points deleted by CASCADE
    }

    suspend fun clearAllCache() {
        bangumiDao.deleteAll()
    }
}
```

- [ ] **Step 2: Create CheckInRepository**

Create `app/src/main/java/com/tnt/seichicamera/data/repository/CheckInRepository.kt`:

```kotlin
package com.tnt.seichicamera.data.repository

import com.tnt.seichicamera.data.local.dao.CheckInDao
import com.tnt.seichicamera.data.local.entity.CheckInEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckInRepository @Inject constructor(
    private val checkInDao: CheckInDao
) {
    suspend fun checkIn(pointId: String, photoUri: String, comparisonUri: String? = null): Long {
        return checkInDao.insert(
            CheckInEntity(
                pointId = pointId,
                photoUri = photoUri,
                timestamp = System.currentTimeMillis(),
                comparisonUri = comparisonUri
            )
        )
    }

    fun getCheckedInPointIds(): Flow<List<String>> =
        checkInDao.getAllCheckedInPointIds()

    suspend fun isCheckedIn(pointId: String): Boolean =
        checkInDao.getByPointId(pointId) != null
}
```

- [ ] **Step 3: Create DI modules**

Create `app/src/main/java/com/tnt/seichicamera/di/NetworkModule.kt`:

```kotlin
package com.tnt.seichicamera.di

import com.tnt.seichicamera.data.remote.AnitabiApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(AnitabiApi.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideAnitabiApi(retrofit: Retrofit): AnitabiApi =
        retrofit.create(AnitabiApi::class.java)
}
```

Create `app/src/main/java/com/tnt/seichicamera/di/DatabaseModule.kt`:

```kotlin
package com.tnt.seichicamera.di

import android.content.Context
import androidx.room.Room
import com.tnt.seichicamera.data.local.AppDatabase
import com.tnt.seichicamera.data.local.dao.BangumiDao
import com.tnt.seichicamera.data.local.dao.CheckInDao
import com.tnt.seichicamera.data.local.dao.SacredPointDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "seichi_camera.db"
        ).build()

    @Provides
    fun provideBangumiDao(db: AppDatabase): BangumiDao = db.bangumiDao()

    @Provides
    fun provideSacredPointDao(db: AppDatabase): SacredPointDao = db.sacredPointDao()

    @Provides
    fun provideCheckInDao(db: AppDatabase): CheckInDao = db.checkInDao()
}
```

Create `app/src/main/java/com/tnt/seichicamera/di/RepositoryModule.kt`:

```kotlin
package com.tnt.seichicamera.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    // Repositories use @Inject constructor, so no @Provides needed.
    // This module exists for future bindings (e.g., interface-to-impl).
}
```

- [ ] **Step 4: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL — Hilt generates all DI components.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add repositories and Hilt DI modules (Task 4)"
```

---

### Task 5: Navigation & App Shell

**Files:**
- Create: `app/src/main/java/com/tnt/seichicamera/ui/navigation/Screen.kt`
- Create: `app/src/main/java/com/tnt/seichicamera/ui/navigation/BottomNavBar.kt`
- Create: `app/src/main/java/com/tnt/seichicamera/ui/navigation/NavGraph.kt`
- Modify: `app/src/main/java/com/tnt/seichicamera/MainActivity.kt`

**Interfaces:**
- Consumes: `SeichiCameraTheme` (Task 1)
- Produces:
  - `Screen` sealed class — `Map`, `Camera(referenceImageUrls: String?, pointId: String?)`, `Settings`
  - `NavGraph(navController, modifier)` composable
  - `BottomNavBar(navController)` composable

- [ ] **Step 1: Define Screen routes**

Create `app/src/main/java/com/tnt/seichicamera/ui/navigation/Screen.kt`:

```kotlin
package com.tnt.seichicamera.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Map : Screen("map", "Map", Icons.Default.Map)
    data object Camera : Screen("camera?imageUrls={imageUrls}&pointId={pointId}", "Camera", Icons.Default.CameraAlt) {
        fun createRoute(imageUrls: String? = null, pointId: String? = null): String {
            return "camera?imageUrls=${imageUrls ?: ""}&pointId=${pointId ?: ""}"
        }
        const val BASE_ROUTE = "camera?imageUrls={imageUrls}&pointId={pointId}"
    }
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    companion object {
        val bottomNavItems = listOf(Map, Camera, Settings)
    }
}
```

- [ ] **Step 2: Create BottomNavBar**

Create `app/src/main/java/com/tnt/seichicamera/ui/navigation/BottomNavBar.kt`:

```kotlin
package com.tnt.seichicamera.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        Screen.bottomNavItems.forEach { screen ->
            val selected = when (screen) {
                is Screen.Camera -> currentRoute?.startsWith("camera") == true
                else -> currentRoute == screen.route
            }
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.label) },
                label = { Text(screen.label) },
                selected = selected,
                onClick = {
                    val targetRoute = when (screen) {
                        is Screen.Camera -> Screen.Camera.createRoute()
                        else -> screen.route
                    }
                    navController.navigate(targetRoute) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
```

- [ ] **Step 3: Create NavGraph with placeholder screens**

Create `app/src/main/java/com/tnt/seichicamera/ui/navigation/NavGraph.kt`:

```kotlin
package com.tnt.seichicamera.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Map.route,
        modifier = modifier
    ) {
        composable(Screen.Map.route) {
            // Placeholder — will be replaced in Task 8
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("🗺️ Map Screen")
            }
        }

        composable(
            route = Screen.Camera.BASE_ROUTE,
            arguments = listOf(
                navArgument("imageUrls") { type = NavType.StringType; defaultValue = "" },
                navArgument("pointId") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val imageUrls = backStackEntry.arguments?.getString("imageUrls") ?: ""
            val pointId = backStackEntry.arguments?.getString("pointId") ?: ""
            // Placeholder — will be replaced in Task 6
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("📷 Camera Screen\nimageUrls=$imageUrls\npointId=$pointId")
            }
        }

        composable(Screen.Settings.route) {
            // Placeholder — will be replaced in Task 10
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("⚙️ Settings Screen")
            }
        }
    }
}
```

- [ ] **Step 4: Update MainActivity with navigation**

Replace `app/src/main/java/com/tnt/seichicamera/MainActivity.kt` with:

```kotlin
package com.tnt.seichicamera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.tnt.seichicamera.ui.navigation.BottomNavBar
import com.tnt.seichicamera.ui.navigation.NavGraph
import com.tnt.seichicamera.ui.theme.SeichiCameraTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SeichiCameraTheme {
                val navController = rememberNavController()
                Scaffold(
                    bottomBar = { BottomNavBar(navController) }
                ) { innerPadding ->
                    NavGraph(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 5: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL — app displays 3-tab navigation with placeholder screens.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: add Navigation Compose with bottom nav shell (Task 5)"
```

---

### Task 6: Camera Core

**Files:**
- Create: `app/src/main/java/com/tnt/seichicamera/ui/camera/CameraScreen.kt`
- Create: `app/src/main/java/com/tnt/seichicamera/ui/camera/CameraViewModel.kt`
- Modify: `app/src/main/java/com/tnt/seichicamera/ui/navigation/NavGraph.kt` (replace camera placeholder)

**Interfaces:**
- Consumes: `NavGraph` (Task 5)
- Produces:
  - `CameraScreen(navController, imageUrls, pointId)` composable — full camera preview with capture, flash toggle, camera flip, aspect ratio selector
  - `CameraViewModel` — manages `CameraUiState(flashMode, lensFacing, aspectRatio, capturedPhotoUri)`
  - `CameraUiState` data class
  - `AspectRatioOption` enum — `RATIO_16_9, RATIO_4_3, RATIO_CINEMATIC, RATIO_1_1, FREE`

- [ ] **Step 1: Create CameraViewModel**

Create `app/src/main/java/com/tnt/seichicamera/ui/camera/CameraViewModel.kt`:

```kotlin
package com.tnt.seichicamera.ui.camera

import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

enum class AspectRatioOption(val label: String, val ratioFloat: Float?) {
    RATIO_16_9("16:9", 16f / 9f),
    RATIO_4_3("4:3", 4f / 3f),
    RATIO_CINEMATIC("2.35:1", 2.35f),
    RATIO_1_1("1:1", 1f),
    FREE("Free", null)
}

data class CameraUiState(
    val flashMode: Int = ImageCapture.FLASH_MODE_OFF,
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val aspectRatio: AspectRatioOption = AspectRatioOption.RATIO_16_9,
    val showGrid: Boolean = false,
    val capturedPhotoUri: Uri? = null,
    val hasFlash: Boolean = true
)

@HiltViewModel
class CameraViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    fun toggleFlash() {
        _uiState.update {
            val newMode = if (it.flashMode == ImageCapture.FLASH_MODE_OFF)
                ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
            it.copy(flashMode = newMode)
        }
    }

    fun flipCamera() {
        _uiState.update {
            val newFacing = if (it.lensFacing == CameraSelector.LENS_FACING_BACK)
                CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
            it.copy(lensFacing = newFacing)
        }
    }

    fun setAspectRatio(ratio: AspectRatioOption) {
        _uiState.update { it.copy(aspectRatio = ratio) }
    }

    fun toggleGrid() {
        _uiState.update { it.copy(showGrid = !it.showGrid) }
    }

    fun onPhotoCaptured(uri: Uri?) {
        _uiState.update { it.copy(capturedPhotoUri = uri) }
    }

    fun clearCapturedPhoto() {
        _uiState.update { it.copy(capturedPhotoUri = null) }
    }

    fun setHasFlash(hasFlash: Boolean) {
        _uiState.update { it.copy(hasFlash = hasFlash) }
    }
}
```

- [ ] **Step 2: Create CameraScreen composable**

Create `app/src/main/java/com/tnt/seichicamera/ui/camera/CameraScreen.kt`:

```kotlin
package com.tnt.seichicamera.ui.camera

import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.Locale

private const val TAG = "CameraScreen"

@Composable
fun CameraScreen(
    navController: NavController,
    imageUrls: String,
    pointId: String,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    // Camera setup
    val previewView = remember { PreviewView(context) }

    DisposableEffect(uiState.lensFacing) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            cameraProvider = provider
            provider.unbindAll()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val capture = ImageCapture.Builder()
                .setFlashMode(uiState.flashMode)
                .build()
            imageCapture = capture

            val selector = CameraSelector.Builder()
                .requireLensFacing(uiState.lensFacing)
                .build()

            try {
                val camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
                viewModel.setHasFlash(camera.cameraInfo.hasFlashUnit())
            } catch (e: Exception) {
                Log.e(TAG, "Camera bind failed", e)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose { cameraProvider?.unbindAll() }
    }

    // Update flash mode when state changes
    DisposableEffect(uiState.flashMode) {
        imageCapture?.flashMode = uiState.flashMode
        onDispose { }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview
        val aspectModifier = uiState.aspectRatio.ratioFloat?.let {
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f / it) // portrait: height > width
        } ?: Modifier.fillMaxSize()

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { previewView },
                modifier = aspectModifier
            )

            // Grid overlay
            if (uiState.showGrid) {
                GridOverlay(modifier = aspectModifier)
            }

            // Overlay image will be added in Task 7
        }

        // Top toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Aspect ratio selector
            AspectRatioSelector(
                current = uiState.aspectRatio,
                onSelect = { viewModel.setAspectRatio(it) }
            )

            Row {
                // Grid toggle
                IconButton(onClick = { viewModel.toggleGrid() }) {
                    Icon(
                        Icons.Default.GridOn,
                        contentDescription = "Grid",
                        tint = if (uiState.showGrid) Color.Yellow else Color.White
                    )
                }

                // Flash toggle
                if (uiState.hasFlash) {
                    IconButton(onClick = { viewModel.toggleFlash() }) {
                        Icon(
                            if (uiState.flashMode == ImageCapture.FLASH_MODE_ON)
                                Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flash",
                            tint = Color.White
                        )
                    }
                }

                // Flip camera
                IconButton(onClick = { viewModel.flipCamera() }) {
                    Icon(Icons.Default.Cameraswitch, contentDescription = "Flip", tint = Color.White)
                }
            }
        }

        // Bottom capture button
        FloatingActionButton(
            onClick = {
                val capture = imageCapture ?: return@FloatingActionButton
                val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                    .format(System.currentTimeMillis())
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SeichiCamera")
                    }
                }
                val outputOptions = ImageCapture.OutputFileOptions.Builder(
                    context.contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                ).build()

                capture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            viewModel.onPhotoCaptured(output.savedUri)
                            Toast.makeText(context, "Photo saved", Toast.LENGTH_SHORT).show()
                        }
                        override fun onError(exception: ImageCaptureException) {
                            Log.e(TAG, "Capture failed", exception)
                            Toast.makeText(context, "Capture failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .size(72.dp)
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = "Capture", modifier = Modifier.size(36.dp))
        }
    }
}

@Composable
private fun GridOverlay(modifier: Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeColor = Color.White.copy(alpha = 0.5f)
        val strokeWidth = 1.dp.toPx()

        // Vertical lines (rule of thirds)
        drawLine(strokeColor, Offset(w / 3, 0f), Offset(w / 3, h), strokeWidth)
        drawLine(strokeColor, Offset(2 * w / 3, 0f), Offset(2 * w / 3, h), strokeWidth)
        // Horizontal lines
        drawLine(strokeColor, Offset(0f, h / 3), Offset(w, h / 3), strokeWidth)
        drawLine(strokeColor, Offset(0f, 2 * h / 3), Offset(w, 2 * h / 3), strokeWidth)
    }
}

@Composable
private fun AspectRatioSelector(
    current: AspectRatioOption,
    onSelect: (AspectRatioOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(current.label, color = Color.White)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AspectRatioOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
```

- [ ] **Step 3: Wire CameraScreen into NavGraph**

In `NavGraph.kt`, replace the camera placeholder composable body:

```kotlin
composable(
    route = Screen.Camera.BASE_ROUTE,
    arguments = listOf(
        navArgument("imageUrls") { type = NavType.StringType; defaultValue = "" },
        navArgument("pointId") { type = NavType.StringType; defaultValue = "" }
    )
) { backStackEntry ->
    val imageUrls = backStackEntry.arguments?.getString("imageUrls") ?: ""
    val pointId = backStackEntry.arguments?.getString("pointId") ?: ""
    CameraScreen(
        navController = navController,
        imageUrls = imageUrls,
        pointId = pointId
    )
}
```

Add import: `import com.tnt.seichicamera.ui.camera.CameraScreen`

- [ ] **Step 4: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL — Camera tab shows preview with capture, flash, flip, grid, aspect ratio controls.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add CameraScreen with CameraX preview and controls (Task 6)"
```

---

### Task 7: Overlay System (Gestures & Image Overlay)

**Files:**
- Create: `app/src/main/java/com/tnt/seichicamera/ui/camera/OverlayState.kt`
- Create: `app/src/main/java/com/tnt/seichicamera/ui/camera/ImageOverlay.kt`
- Modify: `app/src/main/java/com/tnt/seichicamera/ui/camera/CameraViewModel.kt` (add overlay state)
- Modify: `app/src/main/java/com/tnt/seichicamera/ui/camera/CameraScreen.kt` (integrate overlay)

**Interfaces:**
- Consumes: `CameraScreen`, `CameraViewModel` (Task 6)
- Produces:
  - `OverlayState(imageUri, alpha, translationX, translationY, scale, rotation, isMirrored, isEditing)` — data class
  - `ImageOverlay(overlayState, onStateChange, onPickImage)` composable — draggable/zoomable/rotatable image overlay
  - `CameraViewModel.overlayState: StateFlow<OverlayState>` — manages overlay transforms

- [ ] **Step 1: Create OverlayState**

Create `app/src/main/java/com/tnt/seichicamera/ui/camera/OverlayState.kt`:

```kotlin
package com.tnt.seichicamera.ui.camera

import android.net.Uri

data class OverlayState(
    val imageUri: Uri? = null,
    val alpha: Float = 0.5f,
    val translationX: Float = 0f,
    val translationY: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val isMirrored: Boolean = false,
    val isEditing: Boolean = false,
    // For multi-image switching from map
    val imageUrls: List<String> = emptyList(),
    val currentImageIndex: Int = 0
) {
    val currentImageUrl: String?
        get() = imageUrls.getOrNull(currentImageIndex)
}
```

- [ ] **Step 2: Create ImageOverlay composable**

Create `app/src/main/java/com/tnt/seichicamera/ui/camera/ImageOverlay.kt`:

```kotlin
package com.tnt.seichicamera.ui.camera

import android.net.Uri
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun ImageOverlay(
    state: OverlayState,
    onTransform: (translationX: Float, translationY: Float, scale: Float, rotation: Float) -> Unit,
    onAlphaChange: (Float) -> Unit,
    onMirror: () -> Unit,
    onReset: () -> Unit,
    onPickImage: () -> Unit,
    onNextImage: () -> Unit,
    onPrevImage: () -> Unit,
    onTapOverlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transformState = rememberTransformableState { zoomChange, panChange, rotationChange ->
        onTransform(
            state.translationX + panChange.x,
            state.translationY + panChange.y,
            (state.scale * zoomChange).coerceIn(0.1f, 10f),
            state.rotation + rotationChange
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Overlay image
        val imageModel: Any? = state.imageUri ?: state.currentImageUrl
        if (imageModel != null) {
            AsyncImage(
                model = imageModel,
                contentDescription = "Overlay reference image",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = state.translationX
                        translationY = state.translationY
                        scaleX = state.scale * if (state.isMirrored) -1f else 1f
                        scaleY = state.scale
                        rotationZ = state.rotation
                    }
                    .alpha(state.alpha)
                    .transformable(transformState)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { onTapOverlay() })
                    }
            )
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 120.dp, start = 16.dp, end = 16.dp)
        ) {
            // Multi-image navigation (only if multiple images)
            if (state.imageUrls.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPrevImage) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous", tint = Color.White)
                    }
                    Text(
                        "${state.currentImageIndex + 1} / ${state.imageUrls.size}",
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onNextImage) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next", tint = Color.White)
                    }
                }
            }

            // Transparency slider
            Slider(
                value = state.alpha,
                onValueChange = onAlphaChange,
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(4.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPickImage) {
                    Icon(Icons.Default.Image, "Load image", tint = Color.White)
                }
                IconButton(onClick = onMirror) {
                    Icon(Icons.Default.Flip, "Mirror", tint = Color.White)
                }
                IconButton(onClick = onReset) {
                    Icon(Icons.Default.RestartAlt, "Reset", tint = Color.White)
                }
            }
        }
    }
}
```

- [ ] **Step 3: Add overlay state to CameraViewModel**

Add the following fields and methods to `CameraViewModel`:

```kotlin
// Add to CameraViewModel class body:

private val _overlayState = MutableStateFlow(OverlayState())
val overlayState: StateFlow<OverlayState> = _overlayState.asStateFlow()

fun setOverlayImage(uri: Uri) {
    _overlayState.update {
        it.copy(imageUri = uri, isEditing = false)
    }
}

fun setOverlayImageUrls(urls: List<String>, startIndex: Int = 0) {
    _overlayState.update {
        it.copy(imageUrls = urls, currentImageIndex = startIndex, imageUri = null)
    }
}

fun updateOverlayTransform(translationX: Float, translationY: Float, scale: Float, rotation: Float) {
    _overlayState.update {
        it.copy(translationX = translationX, translationY = translationY, scale = scale, rotation = rotation)
    }
}

fun setOverlayAlpha(alpha: Float) {
    _overlayState.update { it.copy(alpha = alpha) }
}

fun toggleMirror() {
    _overlayState.update { it.copy(isMirrored = !it.isMirrored) }
}

fun resetOverlay() {
    _overlayState.update {
        it.copy(
            alpha = 0.5f, translationX = 0f, translationY = 0f,
            scale = 1f, rotation = 0f, isMirrored = false
        )
    }
}

fun nextImage() {
    _overlayState.update {
        val next = (it.currentImageIndex + 1).coerceAtMost(it.imageUrls.size - 1)
        it.copy(currentImageIndex = next)
    }
}

fun prevImage() {
    _overlayState.update {
        val prev = (it.currentImageIndex - 1).coerceAtLeast(0)
        it.copy(currentImageIndex = prev)
    }
}

fun toggleEditing() {
    _overlayState.update { it.copy(isEditing = !it.isEditing) }
}
```

Add import: `import android.net.Uri`

- [ ] **Step 4: Integrate ImageOverlay into CameraScreen**

In `CameraScreen.kt`, add after the camera preview `AndroidView` and grid overlay, inside the same `Box`:

```kotlin
// Add these at top of CameraScreen function:
val overlayState by viewModel.overlayState.collectAsStateWithLifecycle()

// Image picker launcher
val pickMediaLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
) { uri ->
    uri?.let { viewModel.setOverlayImage(it) }
}

// Initialize overlay from nav args
LaunchedEffect(imageUrls) {
    if (imageUrls.isNotBlank()) {
        val urls = imageUrls.split(",").filter { it.isNotBlank() }
        if (urls.isNotEmpty()) {
            viewModel.setOverlayImageUrls(urls)
        }
    }
}
```

Add the `ImageOverlay` composable after the grid overlay inside the center `Box`:

```kotlin
// Image overlay
ImageOverlay(
    state = overlayState,
    onTransform = { tx, ty, s, r -> viewModel.updateOverlayTransform(tx, ty, s, r) },
    onAlphaChange = { viewModel.setOverlayAlpha(it) },
    onMirror = { viewModel.toggleMirror() },
    onReset = { viewModel.resetOverlay() },
    onPickImage = {
        pickMediaLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    },
    onNextImage = { viewModel.nextImage() },
    onPrevImage = { viewModel.prevImage() },
    onTapOverlay = { viewModel.toggleEditing() }
)
```

Add imports:

```kotlin
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import com.tnt.seichicamera.ui.camera.ImageOverlay
```

- [ ] **Step 5: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL — Camera screen supports image overlay with drag/scale/rotate gestures, transparency slider, mirror, reset, and multi-image switching.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: add image overlay with gesture controls (Task 7)"
```

---

### Task 8: Map Screen & Anitabi Search

**Files:**
- Create: `app/src/main/java/com/tnt/seichicamera/ui/map/MapScreen.kt`
- Create: `app/src/main/java/com/tnt/seichicamera/ui/map/MapViewModel.kt`
- Create: `app/src/main/java/com/tnt/seichicamera/ui/map/PointDetailSheet.kt`
- Modify: `app/src/main/java/com/tnt/seichicamera/ui/navigation/NavGraph.kt` (replace map placeholder)

**Interfaces:**
- Consumes: `BangumiRepository` (Task 4), `CheckInRepository` (Task 4), `Screen.Camera.createRoute()` (Task 5)
- Produces:
  - `MapScreen(navController)` composable — osmdroid map with search bar, markers, BottomSheet
  - `MapViewModel` — manages `MapUiState(searchQuery, bangumi, points, selectedPoint, checkedInIds, isLoading, error)`
  - `PointDetailSheet(point, isCheckedIn, onNavigate, onShootWithImage)` composable

- [ ] **Step 1: Create MapViewModel**

Create `app/src/main/java/com/tnt/seichicamera/ui/map/MapViewModel.kt`:

```kotlin
package com.tnt.seichicamera.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tnt.seichicamera.data.repository.BangumiRepository
import com.tnt.seichicamera.data.repository.CheckInRepository
import com.tnt.seichicamera.domain.model.Bangumi
import com.tnt.seichicamera.domain.model.SacredPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val searchQuery: String = "",
    val bangumi: Bangumi? = null,
    val points: List<SacredPoint> = emptyList(),
    val selectedPoint: SacredPoint? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val bangumiRepository: BangumiRepository,
    private val checkInRepository: CheckInRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    val checkedInPointIds: StateFlow<List<String>> =
        checkInRepository.getCheckedInPointIds()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun searchBangumi() {
        val query = _uiState.value.searchQuery.trim()
        if (query.isBlank()) return

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            // Try parsing as Bangumi ID (number)
            val subjectId = query.toIntOrNull()
            if (subjectId != null) {
                val result = bangumiRepository.getBangumiPoints(subjectId)
                result.fold(
                    onSuccess = { (bangumi, points) ->
                        _uiState.update {
                            it.copy(
                                bangumi = bangumi,
                                points = points,
                                isLoading = false,
                                selectedPoint = null
                            )
                        }
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(isLoading = false, error = e.message ?: "Unknown error")
                        }
                    }
                )
            } else {
                _uiState.update {
                    it.copy(isLoading = false, error = "Please enter a Bangumi Subject ID (number)")
                }
            }
        }
    }

    fun selectPoint(point: SacredPoint?) {
        _uiState.update { it.copy(selectedPoint = point) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
```

- [ ] **Step 2: Create PointDetailSheet**

Create `app/src/main/java/com/tnt/seichicamera/ui/map/PointDetailSheet.kt`:

```kotlin
package com.tnt.seichicamera.ui.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.tnt.seichicamera.domain.model.SacredPoint

@Composable
fun PointDetailSheet(
    point: SacredPoint,
    isCheckedIn: Boolean,
    onNavigate: () -> Unit,
    onShootWithImage: (imageIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Title row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = point.name ?: "Unknown Point",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            if (isCheckedIn) {
                Icon(Icons.Default.CheckCircle, "Checked in", tint = Color(0xFF4CAF50))
            }
        }

        if (point.ep != null) {
            Text(
                text = point.ep,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(12.dp))

        // Reference images
        if (point.imageUrls.isNotEmpty()) {
            Text("Reference Images", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 8.dp)
            ) {
                itemsIndexed(point.imageUrls) { index, url ->
                    AsyncImage(
                        model = url.replace("h360", "h160"),
                        contentDescription = "Reference image ${index + 1}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(120.dp, 80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onShootWithImage(index) }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Action buttons
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onNavigate,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Navigation, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Navigate")
            }

            Spacer(Modifier.width(8.dp))

            Button(
                onClick = { onShootWithImage(0) },
                modifier = Modifier.weight(1f),
                enabled = point.imageUrls.isNotEmpty()
            ) {
                Text("Shoot with Image")
            }
        }

        // Attribution
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Data: Anitabi",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

- [ ] **Step 3: Create MapScreen**

Create `app/src/main/java/com/tnt/seichicamera/ui/map/MapScreen.kt`:

```kotlin
package com.tnt.seichicamera.ui.map

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tnt.seichicamera.domain.model.SacredPoint
import com.tnt.seichicamera.ui.navigation.Screen
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavController,
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val checkedInIds by viewModel.checkedInPointIds.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Show errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("Enter Bangumi Subject ID") },
                trailingIcon = {
                    IconButton(onClick = { viewModel.searchBangumi() }) {
                        Icon(Icons.Default.Search, "Search")
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(onSearch = { viewModel.searchBangumi() }),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Title
            uiState.bangumi?.let {
                Text(
                    text = it.title,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                )
            }

            // Map
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(5.0)
                        controller.setCenter(GeoPoint(36.0, 138.0)) // Japan center
                    }
                },
                update = { mapView ->
                    mapView.overlays.clear()

                    uiState.points.forEach { point ->
                        val marker = Marker(mapView).apply {
                            position = GeoPoint(point.latitude, point.longitude)
                            title = point.name ?: "Point"
                            snippet = point.ep ?: ""
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                            // Green if checked in, default otherwise
                            if (point.id in checkedInIds) {
                                // Use default marker (tinted via icon in future)
                            }

                            setOnMarkerClickListener { _, _ ->
                                viewModel.selectPoint(point)
                                true
                            }
                        }
                        mapView.overlays.add(marker)
                    }

                    // Zoom to fit points
                    if (uiState.points.isNotEmpty()) {
                        val avgLat = uiState.points.map { it.latitude }.average()
                        val avgLng = uiState.points.map { it.longitude }.average()
                        val zoom = uiState.bangumi?.zoom?.toDouble() ?: 12.0
                        mapView.controller.setCenter(GeoPoint(avgLat, avgLng))
                        mapView.controller.setZoom(zoom)
                    }

                    mapView.invalidate()
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Loading indicator
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Point detail bottom sheet
        val sheetState = rememberModalBottomSheetState()
        uiState.selectedPoint?.let { point ->
            ModalBottomSheet(
                onDismissRequest = { viewModel.selectPoint(null) },
                sheetState = sheetState
            ) {
                PointDetailSheet(
                    point = point,
                    isCheckedIn = point.id in checkedInIds,
                    onNavigate = {
                        val geoUri = Uri.parse("geo:${point.latitude},${point.longitude}?q=${point.latitude},${point.longitude}(${Uri.encode(point.name ?: "Point")})")
                        val intent = Intent(Intent.ACTION_VIEW, geoUri)
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            // Fallback to browser
                            val webUri = Uri.parse("https://www.google.com/maps?q=${point.latitude},${point.longitude}")
                            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                        }
                    },
                    onShootWithImage = { imageIndex ->
                        viewModel.selectPoint(null)
                        val urls = point.imageUrls.joinToString(",")
                        navController.navigate(Screen.Camera.createRoute(imageUrls = urls, pointId = point.id))
                    }
                )
            }
        }
    }
}
```

- [ ] **Step 4: Wire MapScreen into NavGraph**

In `NavGraph.kt`, replace the map placeholder:

```kotlin
composable(Screen.Map.route) {
    MapScreen(navController = navController)
}
```

Add import: `import com.tnt.seichicamera.ui.map.MapScreen`

- [ ] **Step 5: Initialize osmdroid in SeichiCameraApp**

Add to `SeichiCameraApp.kt`:

```kotlin
import org.osmdroid.config.Configuration

@HiltAndroidApp
class SeichiCameraApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // osmdroid configuration
        Configuration.getInstance().userAgentValue = packageName
    }
}
```

- [ ] **Step 6: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL — Map tab shows search bar + osmdroid map, searching a Bangumi ID loads markers, clicking a marker shows BottomSheet with reference images.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: add MapScreen with Anitabi search and point details (Task 8)"
```

---

### Task 9: Map → Camera Bridge

**Files:**
- Modify: `app/src/main/java/com/tnt/seichicamera/ui/camera/CameraScreen.kt` (handle pointId for post-capture)
- Modify: `app/src/main/java/com/tnt/seichicamera/ui/camera/CameraViewModel.kt` (add pointId tracking)
- Create: `app/src/main/java/com/tnt/seichicamera/ui/camera/PostCaptureSheet.kt`

**Interfaces:**
- Consumes: `CameraScreen` (Task 6/7), `Screen.Camera.createRoute(imageUrls, pointId)` (Task 5), `CheckInRepository` (Task 4)
- Produces:
  - `PostCaptureSheet(photoUri, pointId, onCheckIn, onGenerateComparison, onDismiss)` composable
  - `CameraViewModel.pointId: String` — current point ID for check-in

- [ ] **Step 1: Create PostCaptureSheet**

Create `app/src/main/java/com/tnt/seichicamera/ui/camera/PostCaptureSheet.kt`:

```kotlin
package com.tnt.seichicamera.ui.camera

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun PostCaptureSheet(
    photoUri: Uri,
    pointId: String?,
    onCheckIn: () -> Unit,
    onGenerateComparison: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text("Photo Saved!", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        AsyncImage(
            model = photoUri,
            contentDescription = "Captured photo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )

        Spacer(Modifier.height(16.dp))

        // Check-in button (only if from map with pointId)
        if (!pointId.isNullOrBlank()) {
            Button(
                onClick = onCheckIn,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Check In ✅")
            }
            Spacer(Modifier.height(8.dp))
        }

        // Generate comparison
        OutlinedButton(
            onClick = onGenerateComparison,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Compare, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Generate Comparison 🖼️")
        }

        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.End)
        ) {
            Icon(Icons.Default.Close, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Close")
        }
    }
}
```

- [ ] **Step 2: Add pointId tracking to CameraViewModel**

Add to `CameraViewModel`:

```kotlin
private var _pointId: String = ""
val pointId: String get() = _pointId

fun setPointId(id: String) {
    _pointId = id
}
```

- [ ] **Step 3: Integrate PostCaptureSheet into CameraScreen**

In `CameraScreen.kt`, add after the `FloatingActionButton`:

```kotlin
// Initialize pointId from nav args
LaunchedEffect(pointId) {
    if (pointId.isNotBlank()) {
        viewModel.setPointId(pointId)
    }
}

// Post-capture bottom sheet
if (uiState.capturedPhotoUri != null) {
    ModalBottomSheet(
        onDismissRequest = { viewModel.clearCapturedPhoto() }
    ) {
        PostCaptureSheet(
            photoUri = uiState.capturedPhotoUri!!,
            pointId = viewModel.pointId.ifBlank { null },
            onCheckIn = {
                // Will be implemented in Task 11
                viewModel.clearCapturedPhoto()
            },
            onGenerateComparison = {
                // Will be implemented in Task 11
                viewModel.clearCapturedPhoto()
            },
            onDismiss = { viewModel.clearCapturedPhoto() }
        )
    }
}
```

Add imports:

```kotlin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
```

Add `@OptIn(ExperimentalMaterial3Api::class)` annotation to `CameraScreen`.

- [ ] **Step 4: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL — Tapping "Shoot with Image" from map navigates to camera with overlay images loaded. After capture, PostCaptureSheet shows.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add map-to-camera bridge and post-capture sheet (Task 9)"
```

---

### Task 10: Settings & Language Switch

**Files:**
- Create: `app/src/main/java/com/tnt/seichicamera/ui/settings/SettingsScreen.kt`
- Create: `app/src/main/java/com/tnt/seichicamera/ui/settings/SettingsViewModel.kt`
- Create: `app/src/main/java/com/tnt/seichicamera/util/LocaleHelper.kt`
- Modify: `app/src/main/java/com/tnt/seichicamera/ui/navigation/NavGraph.kt` (replace settings placeholder)
- Modify: `app/src/main/res/values/strings.xml` (add new strings)
- Modify: `app/src/main/res/values-zh-rCN/strings.xml` (add new strings)
- Modify: `app/src/main/res/values-ja-rJP/strings.xml` (add new strings)

**Interfaces:**
- Consumes: `BangumiRepository.getCachedBangumis()`, `BangumiRepository.clearCache()`, `BangumiRepository.clearAllCache()` (Task 4)
- Produces:
  - `SettingsScreen(navController)` composable
  - `SettingsViewModel` — manages settings state
  - `LocaleHelper.setLocale(context, localeTag)` — applies language change

- [ ] **Step 1: Create LocaleHelper**

Create `app/src/main/java/com/tnt/seichicamera/util/LocaleHelper.kt`:

```kotlin
package com.tnt.seichicamera.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LocaleHelper {
    data class LanguageOption(val tag: String, val displayName: String)

    val languages = listOf(
        LanguageOption("", "System Default"),
        LanguageOption("en", "English"),
        LanguageOption("zh-CN", "简体中文"),
        LanguageOption("zh-HK", "繁體中文（香港）"),
        LanguageOption("zh-TW", "繁體中文（台灣）"),
        LanguageOption("ja", "日本語")
    )

    fun setLocale(tag: String) {
        val localeList = if (tag.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    fun getCurrentLocaleTag(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) "" else locales.toLanguageTags()
    }
}
```

- [ ] **Step 2: Create SettingsViewModel**

Create `app/src/main/java/com/tnt/seichicamera/ui/settings/SettingsViewModel.kt`:

```kotlin
package com.tnt.seichicamera.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tnt.seichicamera.data.repository.BangumiRepository
import com.tnt.seichicamera.domain.model.Bangumi
import com.tnt.seichicamera.util.LocaleHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val currentLocaleTag: String = "",
    val cachedBangumis: List<Bangumi> = emptyList(),
    val isLoadingCache: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val bangumiRepository: BangumiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(
        currentLocaleTag = LocaleHelper.getCurrentLocaleTag()
    ))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadCachedBangumis()
    }

    fun setLanguage(tag: String) {
        LocaleHelper.setLocale(tag)
        _uiState.update { it.copy(currentLocaleTag = tag) }
    }

    fun loadCachedBangumis() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCache = true) }
            val cached = bangumiRepository.getCachedBangumis()
            _uiState.update { it.copy(cachedBangumis = cached, isLoadingCache = false) }
        }
    }

    fun clearCache(subjectId: Int) {
        viewModelScope.launch {
            bangumiRepository.clearCache(subjectId)
            loadCachedBangumis()
        }
    }

    fun clearAllCache() {
        viewModelScope.launch {
            bangumiRepository.clearAllCache()
            loadCachedBangumis()
        }
    }
}
```

- [ ] **Step 3: Create SettingsScreen**

Create `app/src/main/java/com/tnt/seichicamera/ui/settings/SettingsScreen.kt`:

```kotlin
package com.tnt.seichicamera.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tnt.seichicamera.util.LocaleHelper

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp)
    ) {
        // Language section
        item {
            Text(
                "General",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        item {
            val currentLang = LocaleHelper.languages.find { it.tag == uiState.currentLocaleTag }
            ListItem(
                headlineContent = { Text("Language") },
                supportingContent = { Text(currentLang?.displayName ?: "System Default") },
                modifier = Modifier.clickable { showLanguageDialog = true }
            )
            HorizontalDivider()
        }

        // Cache section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Offline Cache",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                if (uiState.cachedBangumis.isNotEmpty()) {
                    IconButton(onClick = { showClearAllDialog = true }) {
                        Icon(Icons.Default.DeleteSweep, "Clear all")
                    }
                }
            }
        }

        if (uiState.cachedBangumis.isEmpty()) {
            item {
                Text(
                    "No cached data",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else {
            items(uiState.cachedBangumis) { bangumi ->
                ListItem(
                    headlineContent = { Text(bangumi.title) },
                    supportingContent = { Text("ID: ${bangumi.id}") },
                    trailingContent = {
                        IconButton(onClick = { viewModel.clearCache(bangumi.id) }) {
                            Icon(Icons.Default.Delete, "Delete cache")
                        }
                    }
                )
            }
        }

        // About section
        item {
            Spacer(Modifier.height(16.dp))
            Text(
                "About",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        item { ListItem(headlineContent = { Text("Version") }, supportingContent = { Text("2.0.0") }) }
        item {
            ListItem(
                headlineContent = { Text("Data Source") },
                supportingContent = { Text("Anitabi (CC BY-NC-SA 4.0)") }
            )
        }
    }

    // Language picker dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Choose Language") },
            text = {
                Column {
                    LocaleHelper.languages.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setLanguage(lang.tag)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = lang.tag == uiState.currentLocaleTag,
                                onClick = {
                                    viewModel.setLanguage(lang.tag)
                                    showLanguageDialog = false
                                }
                            )
                            Text(lang.displayName, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLanguageDialog = false }) { Text("Cancel") } }
        )
    }

    // Clear all confirmation
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Clear All Cache?") },
            text = { Text("This will delete all offline data.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearAllCache(); showClearAllDialog = false }) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) { Text("Cancel") }
            }
        )
    }
}
```

- [ ] **Step 4: Wire SettingsScreen into NavGraph**

In `NavGraph.kt`, replace the settings placeholder:

```kotlin
composable(Screen.Settings.route) {
    SettingsScreen(navController = navController)
}
```

Add import: `import com.tnt.seichicamera.ui.settings.SettingsScreen`

- [ ] **Step 5: Update string resources with new entries**

Add to `app/src/main/res/values/strings.xml` (keep existing, add new):

```xml
<!-- New strings for v2 -->
<string name="nav_map">Map</string>
<string name="nav_camera">Camera</string>
<string name="nav_settings">Settings</string>
<string name="search_bangumi">Search Bangumi ID</string>
<string name="check_in">Check In</string>
<string name="generate_comparison">Generate Comparison</string>
<string name="navigate_to">Navigate</string>
<string name="shoot_with_image">Shoot with Image</string>
<string name="data_source_anitabi">Data: Anitabi</string>
<string name="offline_cache">Offline Cache</string>
<string name="clear_all_cache">Clear All Cache</string>
<string name="no_cached_data">No cached data</string>
```

Add corresponding entries to `values-zh-rCN/strings.xml`:

```xml
<string name="nav_map">地图</string>
<string name="nav_camera">相机</string>
<string name="nav_settings">设置</string>
<string name="search_bangumi">搜索 Bangumi ID</string>
<string name="check_in">打卡</string>
<string name="generate_comparison">生成对比图</string>
<string name="navigate_to">导航</string>
<string name="shoot_with_image">用这张图拍摄</string>
<string name="data_source_anitabi">数据来源：Anitabi</string>
<string name="offline_cache">离线缓存</string>
<string name="clear_all_cache">清除所有缓存</string>
<string name="no_cached_data">暂无缓存数据</string>
```

Add corresponding entries to `values-ja-rJP/strings.xml`:

```xml
<string name="nav_map">マップ</string>
<string name="nav_camera">カメラ</string>
<string name="nav_settings">設定</string>
<string name="search_bangumi">Bangumi IDで検索</string>
<string name="check_in">チェックイン</string>
<string name="generate_comparison">比較画像を生成</string>
<string name="navigate_to">ナビ</string>
<string name="shoot_with_image">この画像で撮影</string>
<string name="data_source_anitabi">データ：Anitabi</string>
<string name="offline_cache">オフラインキャッシュ</string>
<string name="clear_all_cache">キャッシュをすべて削除</string>
<string name="no_cached_data">キャッシュデータなし</string>
```

- [ ] **Step 6: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL — Settings tab shows language picker (switching works immediately), cache management, and about section.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: add SettingsScreen with language switch and cache management (Task 10)"
```

---

### Task 11: Check-in & Comparison Image Generation

**Files:**
- Create: `app/src/main/java/com/tnt/seichicamera/ui/camera/ComparisonGenerator.kt`
- Modify: `app/src/main/java/com/tnt/seichicamera/ui/camera/CameraViewModel.kt` (add check-in and comparison logic)
- Modify: `app/src/main/java/com/tnt/seichicamera/ui/camera/CameraScreen.kt` (wire up PostCaptureSheet actions)

**Interfaces:**
- Consumes: `CheckInRepository.checkIn()` (Task 4), `PostCaptureSheet` (Task 9), `OverlayState.currentImageUrl` (Task 7)
- Produces:
  - `ComparisonGenerator.generate(context, referenceUri, photoUri): Uri` — creates side-by-side comparison image
  - Check-in and comparison flows fully wired in CameraScreen

- [ ] **Step 1: Create ComparisonGenerator**

Create `app/src/main/java/com/tnt/seichicamera/ui/camera/ComparisonGenerator.kt`:

```kotlin
package com.tnt.seichicamera.ui.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.graphics.drawable.toBitmap
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

object ComparisonGenerator {

    suspend fun generate(
        context: Context,
        referenceImageSource: Any, // Uri or URL String
        photoUri: Uri
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val imageLoader = ImageLoader(context)

            // Load reference image
            val refRequest = ImageRequest.Builder(context).data(referenceImageSource).build()
            val refResult = imageLoader.execute(refRequest)
            val refBitmap = (refResult as? SuccessResult)?.image?.toBitmap() ?: return@withContext null

            // Load photo
            val photoRequest = ImageRequest.Builder(context).data(photoUri).build()
            val photoResult = imageLoader.execute(photoRequest)
            val photoBitmap = (photoResult as? SuccessResult)?.image?.toBitmap() ?: return@withContext null

            // Create side-by-side comparison
            val width = maxOf(refBitmap.width, photoBitmap.width)
            val height = maxOf(refBitmap.height, photoBitmap.height)
            val watermarkHeight = 48
            val totalWidth = width * 2
            val totalHeight = height + watermarkHeight

            val comparison = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(comparison)
            canvas.drawColor(Color.BLACK)

            // Draw reference image (left)
            val refScaled = Bitmap.createScaledBitmap(refBitmap, width, height, true)
            canvas.drawBitmap(refScaled, 0f, 0f, null)

            // Draw photo (right)
            val photoScaled = Bitmap.createScaledBitmap(photoBitmap, width, height, true)
            canvas.drawBitmap(photoScaled, width.toFloat(), 0f, null)

            // Draw watermark
            val paint = Paint().apply {
                color = Color.WHITE
                textSize = 24f
                typeface = Typeface.DEFAULT
                isAntiAlias = true
            }
            canvas.drawText(
                "Data: Anitabi · Photo: SeichiCamera",
                16f,
                height + watermarkHeight - 12f,
                paint
            )

            // Save to MediaStore
            val name = "comparison_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())}"
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SeichiCamera/Comparisons")
                }
            }

            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: return@withContext null

            context.contentResolver.openOutputStream(uri)?.use { stream ->
                comparison.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            }

            // Cleanup
            refScaled.recycle()
            photoScaled.recycle()
            comparison.recycle()

            uri
        } catch (e: Exception) {
            null
        }
    }
}
```

- [ ] **Step 2: Add check-in and comparison to CameraViewModel**

Add to `CameraViewModel`:

```kotlin
// Add constructor parameter:
// @Inject constructor(private val checkInRepository: CheckInRepository)

private val _comparisonUri = MutableStateFlow<Uri?>(null)
val comparisonUri: StateFlow<Uri?> = _comparisonUri.asStateFlow()

fun checkIn(context: android.content.Context) {
    val photoUri = _uiState.value.capturedPhotoUri ?: return
    val pid = _pointId.ifBlank { return }
    viewModelScope.launch {
        checkInRepository.checkIn(
            pointId = pid,
            photoUri = photoUri.toString(),
            comparisonUri = _comparisonUri.value?.toString()
        )
    }
}

fun generateComparison(context: android.content.Context) {
    val photoUri = _uiState.value.capturedPhotoUri ?: return
    val refSource: Any = _overlayState.value.imageUri
        ?: _overlayState.value.currentImageUrl
        ?: return

    viewModelScope.launch {
        val uri = ComparisonGenerator.generate(context, refSource, photoUri)
        _comparisonUri.value = uri
        if (uri != null) {
            // Share intent
            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share comparison"))
        }
    }
}
```

Add import to constructor: inject `CheckInRepository`:

```kotlin
@HiltViewModel
class CameraViewModel @Inject constructor(
    private val checkInRepository: CheckInRepository
) : ViewModel() {
```

Add import: `import com.tnt.seichicamera.data.repository.CheckInRepository`

- [ ] **Step 3: Wire PostCaptureSheet actions in CameraScreen**

In `CameraScreen.kt`, update the `PostCaptureSheet` callbacks:

```kotlin
PostCaptureSheet(
    photoUri = uiState.capturedPhotoUri!!,
    pointId = viewModel.pointId.ifBlank { null },
    onCheckIn = {
        viewModel.checkIn(context)
        Toast.makeText(context, "Checked in! ✅", Toast.LENGTH_SHORT).show()
        viewModel.clearCapturedPhoto()
    },
    onGenerateComparison = {
        viewModel.generateComparison(context)
        viewModel.clearCapturedPhoto()
    },
    onDismiss = { viewModel.clearCapturedPhoto() }
)
```

- [ ] **Step 4: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL — After capture, check-in saves to Room, comparison generates side-by-side image with watermark and launches share sheet.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add check-in and comparison image generation (Task 11)"
```

---

### Task 12: Offline Cache Download & Resource Migration

**Files:**
- Modify: `app/src/main/java/com/tnt/seichicamera/ui/map/MapViewModel.kt` (add cache download)
- Modify: `app/src/main/java/com/tnt/seichicamera/ui/map/MapScreen.kt` (add cache download button)
- Modify: `app/src/main/res/values/strings.xml` (complete migration of old strings + add new)
- Modify: `app/src/main/res/values-zh-rCN/strings.xml` (same)
- Modify: `app/src/main/res/values-zh-rHK/strings.xml` (same)
- Modify: `app/src/main/res/values-zh-rTW/strings.xml` (same)
- Modify: `app/src/main/res/values-ja-rJP/strings.xml` (same)
- Delete: `app/src/main/res/values/arrays.xml` (old preference arrays, no longer needed)
- Delete: `app/src/main/res/values/styles.xml` (if only used for old XML views)

**Interfaces:**
- Consumes: `BangumiRepository.cacheOffline()` (Task 4), `MapUiState` (Task 8)
- Produces: cache download button on map screen, completed string resource migration

- [ ] **Step 1: Add cache download to MapViewModel**

Add to `MapViewModel`:

```kotlin
fun downloadOfflineCache() {
    val subjectId = _uiState.value.bangumi?.id ?: return
    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        val result = bangumiRepository.cacheOffline(subjectId)
        result.fold(
            onSuccess = {
                _uiState.update { it.copy(isLoading = false, error = null) }
            },
            onFailure = { e ->
                _uiState.update { it.copy(isLoading = false, error = "Cache failed: ${e.message}") }
            }
        )
    }
}
```

- [ ] **Step 2: Add cache download button to MapScreen**

In `MapScreen.kt`, add a download button after the title text:

```kotlin
uiState.bangumi?.let {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = it.title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = { viewModel.downloadOfflineCache() }) {
            Icon(Icons.Default.Download, "Download for offline")
        }
    }
}
```

Add import: `import androidx.compose.material.icons.filled.Download`

- [ ] **Step 3: Complete string resource migration**

Ensure `values/strings.xml` includes all necessary strings for v2. Keep old camera-related strings that are still used. Add any missing v2 strings. The complete file should have entries for: `app_name`, `photo_saved`, `photo_failed`, `permission_denied`, `camera_start_failed`, `flash_on`, `flash_off`, `nav_map`, `nav_camera`, `nav_settings`, `search_bangumi`, `check_in`, `generate_comparison`, `navigate_to`, `shoot_with_image`, `data_source_anitabi`, `offline_cache`, `clear_all_cache`, `no_cached_data`, `download_offline`, `version_number`.

Do the same for all locale variants: `values-zh-rCN`, `values-zh-rHK`, `values-zh-rTW`, `values-ja-rJP`.

Delete `app/src/main/res/values/arrays.xml` (old language preference arrays, no longer needed with Compose settings).

- [ ] **Step 4: Delete remaining old resource files**

Delete files that are no longer used:
- `app/src/main/res/values/styles.xml` (if it only contains old XML view styles; keep if it has `Theme.SeichiCamera`)
- Old drawable resources only used in XML layouts (evaluate which to keep for the icon set)

Keep all `mipmap-*` launcher icons and useful drawable icons (flash, camera, grid, etc.).

- [ ] **Step 5: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL — Full app builds cleanly, map has download button, all strings resolve in all locales.

- [ ] **Step 6: Run all tests**

Run: `./gradlew test`
Expected: All tests PASS.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: add offline cache download and complete resource migration (Task 12)"
```

---

## Post-Implementation Checklist

After all 12 tasks are complete:

- [ ] Run full build: `./gradlew assembleDebug`
- [ ] Run all tests: `./gradlew test`
- [ ] Install on device/emulator and manually verify:
  - Map: search Bangumi ID, see markers, tap marker → BottomSheet
  - Camera: overlay image from map, gestures work, capture works
  - Camera: manual image pick from gallery works
  - Post-capture: check-in saves, comparison generates
  - Settings: language switch works immediately
  - Settings: cache management shows/deletes cached bangumis
- [ ] Verify Anitabi attribution visible in PointDetailSheet and comparison watermark
- [ ] Update `README.md` with v2 description
- [ ] Tag release: `git tag v2.0.0`
