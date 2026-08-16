# ArcSync 底部导航栏液态玻璃重构计划

## 1. 项目概述

### 1.1 背景

当前 ArcSync 的底部导航栏采用纯手工绘制实现，虽然有玻璃质感效果，但与业界主流的液态玻璃（Liquid Glass）效果相比仍有差距。参考 [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) 开源库的设计标准，我们需要对底部导航栏进行重构。

### 1.2 目标

- 集成 `io.github.kyant0:backdrop` 标准库实现液态玻璃效果
- 升级 Kotlin / AGP / Compose 到兼容版本
- **仅改动 UI 层，保留全部业务逻辑不变**
- 支持拖拽手势交互和弹性动画

### 1.3 原则

- **业务逻辑零改动**：`data/`、`util/` 层代码不碰
- **UI 层适配**：仅调整 Compose API 的废弃/变更用法
- **渐进式升级**：先升级工具链，再集成库，最后重构 UI

---

## 2. 版本升级矩阵

### 2.1 当前 vs 目标版本

| 组件 | 当前版本 | 目标版本 | 说明 |
|------|---------|---------|------|
| **Gradle** | 8.7 | 9.6.0 | AGP 9.x 要求 |
| **AGP** | 8.5.2 | 9.3.0 | Android Gradle Plugin |
| **Kotlin** | 1.9.25 | 2.4.10 | 含 Compose Compiler |
| **Compose BOM** | 2024.09.00 | — | 改用 JetBrains Compose |
| **Compose** | Jetpack (via BOM) | JetBrains 1.11.1 | Multiplatform 版本 |
| **Compose Compiler** | 1.5.15 | Kotlin 插件内置 | Kotlin 2.x 不再单独使用 |
| **compileSdk** | 36 | 37 | 最新稳定 |
| **Java Target** | 17 | 17 | 保持不变 |

### 2.2 新增依赖

| 依赖 | 版本 | 说明 |
|------|------|------|
| `io.github.kyant0:backdrop` | 2.0.0 | 液态玻璃效果库 |
| `io.github.kyant0:shapes` | 1.2.0 | 形状库（Backdrop 依赖） |

### 2.3 依赖变化对照

```toml
# 移除
# androidx.compose:compose-bom:2024.09.00
# androidx.compose.compiler:compiler:1.5.15

# 新增 / 替换
[versions]
agp = "9.3.0"
kotlin = "2.4.10"
compose = "1.11.1"
kyantShapes = "1.2.0"
activity = "1.13.0"
core = "1.19.0"

[libraries]
# Compose 改用 JetBrains 版本
compose-foundation = { group = "org.jetbrains.compose.foundation", name = "foundation", version.ref = "compose" }
compose-ui = { group = "org.jetbrains.compose.ui", name = "ui", version.ref = "compose" }
compose-ui-graphics = { group = "org.jetbrains.compose.ui", name = "ui-graphics", version.ref = "compose" }
compose-material3 = { group = "org.jetbrains.compose.material3", name = "material3", version.ref = "compose" }
compose-material-icons = { group = "org.jetbrains.compose.material", name = "material-icons-extended", version.ref = "compose" }

# Backdrop
kyant-backdrop = { group = "io.github.kyant0", name = "backdrop", version = "2.0.0" }
kyant-shapes = { group = "io.github.kyant0", name = "shapes", version.ref = "kyantShapes" }

[plugins]
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
jetbrains-compose = { id = "org.jetbrains.compose", version.ref = "compose" }
```

---

## 3. 项目结构分析

### 3.1 文件分层（改动范围标注）

```
app/src/main/java/com/jipzeongit/arcsync/
├── data/                          ← ❌ 不动
│   ├── DriversViewModel.kt
│   ├── IntelArcRepository.kt
│   ├── Models.kt
│   └── SettingsRepository.kt
├── util/                          ← ❌ 不动
│   ├── AppLogger.kt
│   └── HtmlRenderer.kt
├── ui/                            ← ✅ 改动范围
│   ├── AppRoot.kt                 ← 主要改动：集成 Backdrop + 重构底栏
│   ├── components/
│   │   └── WaveLoadingIndicator.kt  ← 可能需适配 API 变更
│   ├── screens/
│   │   ├── DriverDetailScreen.kt  ← 可能需适配 API 变更
│   │   ├── DriversScreen.kt       ← 可能需适配 API 变更
│   │   └── SettingsScreen.kt      ← 可能需适配 API 变更
│   └── theme/
│       ├── Theme.kt               ← 可能需适配 API 变更
│       └── Type.kt                ← 可能需适配 API 变更
├── ArcSyncApp.kt                  ← ❌ 不动
└── MainActivity.kt                ← 小改动：适配 Compose API
```

### 3.2 改动量预估

| 文件 | 改动程度 | 说明 |
|------|---------|------|
| `AppRoot.kt` | 🔴 大改 | 集成 Backdrop，重构 GlassBottomBar |
| `MainActivity.kt` | 🟡 小改 | 适配 Kotlin 2.x 可能的 API 变更 |
| `Theme.kt` | 🟡 小改 | Material3 API 适配 |
| `*Screen.kt` | 🟢 微调 | Compose API 废弃方法替换 |
| `WaveLoadingIndicator.kt` | 🟢 微调 | 同上 |
| `data/*`, `util/*` | ⚪ 不动 | 纯 Kotlin，无 Compose 依赖 |

---

## 4. 升级步骤

### Phase 1：工具链升级

#### 1.1 升级 Gradle Wrapper

```bash
cd /home/jipzeongit/Code/Projects/ArcSync
./gradlew wrapper --gradle-version=9.6.0
```

#### 1.2 更新 `gradle/libs.versions.toml`

```toml
[versions]
agp = "9.3.0"
kotlin = "2.4.10"
compose = "1.11.1"
kyantShapes = "1.2.0"
activity = "1.13.0"
core = "1.19.0"
coroutines = "1.8.1"
jsoup = "1.17.2"
viewmodel = "2.8.6"
lifecycle-runtime = "2.8.6"
datastore = "1.1.1"
okhttp = "4.12.0"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "core" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activity" }

# Compose - JetBrains Multiplatform 版本
compose-foundation = { group = "org.jetbrains.compose.foundation", name = "foundation", version.ref = "compose" }
compose-ui = { group = "org.jetbrains.compose.ui", name = "ui", version.ref = "compose" }
compose-ui-graphics = { group = "org.jetbrains.compose.ui", name = "ui-graphics", version.ref = "compose" }
compose-material3 = { group = "org.jetbrains.compose.material3", name = "material3", version.ref = "compose" }
compose-material-icons = { group = "org.jetbrains.compose.material", name = "material-icons-extended", version.ref = "compose" }

# Backdrop
kyant-backdrop = { group = "io.github.kyant0", name = "backdrop", version = "2.0.0" }
kyant-shapes = { group = "io.github.kyant0", name = "shapes", version.ref = "kyantShapes" }

# 其他依赖保持不变
androidx-navigation-compose = { module = "androidx.navigation:navigation-compose", version = "2.7.7" }
androidx-lifecycle-viewmodel-ktx = { module = "androidx.lifecycle:lifecycle-viewmodel-ktx", version.ref = "viewmodel" }
androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle-runtime" }
androidx-datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
jsoup = { module = "org.jsoup:jsoup", version.ref = "jsoup" }
okhttp = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttp" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
jetbrains-compose = { id = "org.jetbrains.compose", version.ref = "compose" }
```

#### 1.3 更新根 `build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.jetbrains.compose) apply false
}
```

#### 1.4 更新 `app/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.compose)
}

android {
    namespace = "com.jipzeongit.arcsync"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.jipzeongit.arcsync"
        minSdk = 26
        targetSdk = 37
        versionCode = 3
        versionName = "1.0.2"
    }

    // ... signingConfigs 保持不变

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfigs.findByName("release")?.let {
                signingConfig = it
            }
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true  // 保留，JetBrains 插件兼容
    }

    // 移除 composeOptions，Kotlin 2.x 不再需要
    // composeOptions {
    //     kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    // }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    
    // Compose - JetBrains 版本
    implementation(compose.foundation)
    implementation(compose.ui)
    implementation(compose.ui.graphics)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    
    // Backdrop
    implementation(libs.kyant.backdrop)
    implementation(libs.kyant.shapes)
    
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.jsoup)
    implementation(libs.okhttp)

    debugImplementation(compose.uiTooling)
}
```

#### 1.5 更新 `settings.gradle.kts`

```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ArcSync"
include(":app")
```

#### 1.6 更新 `gradle.properties`

```properties
org.gradle.jvmargs=-Xmx3g -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
# android.defaults.buildfeatures.compose=true  # 移除，已在 build.gradle 中配置
android.suppressUnsupportedCompileSdk=37
```

---

### Phase 2：代码适配（Kotlin 2.x + Compose 变更）

#### 2.1 可能需要处理的 API 变更

| 变更类型 | 说明 | 处理方式 |
|---------|------|---------|
| `@Composable` 注解 | Kotlin 2.x 的 Compose 插件处理方式不同 | 通常无需改动 |
| `CompositionLocal` | API 基本兼容 | 无需改动 |
| `Modifier` 扩展 | API 稳定 | 无需改动 |
| `Material3` 组件 | JetBrains 版本 API 一致 | 无需改动 |
| `collectAsStateWithLifecycle` | 可能需要额外依赖 | 添加 lifecycle-runtime-compose |

#### 2.2 逐文件检查清单

**`data/` 目录 — 不动**
- `DriversViewModel.kt` — 纯 Kotlin + Coroutines，无 Compose 依赖
- `IntelArcRepository.kt` — 纯 Kotlin + OkHttp + Jsoup
- `Models.kt` — 数据类
- `SettingsRepository.kt` — DataStore，无 Compose 依赖

**`util/` 目录 — 不动**
- `AppLogger.kt` — 纯 Kotlin
- `HtmlRenderer.kt` — 纯 Kotlin

**`ui/` 目录 — 仅适配变更**
- 检查所有 `@Composable` 函数签名
- 替换任何已废弃的 Material3 API
- 确保 `collectAsStateWithLifecycle` 正常工作

---

### Phase 3：集成 Backdrop 库

#### 3.1 创建全局 Backdrop

在 `AppRoot.kt` 中：

```kotlin
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberBackdrop

@Composable
fun AppRoot(settingsRepository: SettingsRepository) {
    val backdrop = rememberBackdrop()  // 全局 backdrop 实例
    
    // ... 现有逻辑保持不变
}
```

#### 3.2 调整布局层级

Backdrop 需要"看穿"底层内容才能实现模糊效果：

```kotlin
Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets(0, 0, 0, 0)
) { padding ->
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .backdrop(backdrop)  // 根布局应用 backdrop
    ) {
        // 内容层（在 backdrop 之下）
        NavHost(
            navController = navController,
            startDestination = Routes.DRIVERS,
            modifier = Modifier.fillMaxSize()
        ) {
            // ... 路由配置保持不变
        }
        
        // UI 层（在 backdrop 之上）
        if (showMainChrome) {
            GlassTopBar(...)   // 保持现有顶栏
            GlassBottomBar(    // 重构底栏
                backdrop = backdrop,
                currentRoute = currentRoute,
                onNavigate = { route -> navController.navigate(route) }
            )
        }
    }
}
```

#### 3.3 重构 GlassBottomBar

使用 `LiquidBottomTabs` 替换现有实现：

```kotlin
import com.kyant.backdrop.catalog.components.LiquidBottomTabs
import com.kyant.backdrop.catalog.components.LiquidBottomTab

@Composable
private fun GlassBottomBar(
    backdrop: Backdrop,
    currentRoute: String?,
    driversLabel: String,
    settingsLabel: String,
    onNavigate: (String) -> Unit
) {
    val navItems = remember { listOf(NavItem.Drivers, NavItem.Settings) }
    val selectedIndex = navItems.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    val isLightTheme = !isSystemInDarkTheme()
    
    val accentColor = if (isLightTheme) Color(0xFF0088FF) else Color(0xFF0091FF)
    val containerColor = if (isLightTheme) {
        Color(0xFFFAFAFA).copy(alpha = 0.4f)
    } else {
        Color(0xFF121212).copy(alpha = 0.4f)
    }
    
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding()
    ) {
        LiquidBottomTabs(
            selectedTabIndex = { selectedIndex },
            onTabSelected = { index -> onNavigate(navItems[index].route) },
            backdrop = backdrop,
            tabsCount = navItems.size,
            modifier = Modifier.fillMaxWidth()
        ) {
            navItems.forEachIndexed { index, item ->
                LiquidBottomTab(
                    onClick = { onNavigate(item.route) }
                ) {
                    val isSelected = index == selectedIndex
                    Icon(
                        imageVector = if (isSelected) item.iconFilled else item.iconOutlined,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = if (isSelected) accentColor else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        }
                    )
                    Text(
                        text = item.label(appLang),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        }
                    )
                }
            }
        }
    }
}
```

#### 3.4 NavItem 适配

保留现有的 `NavItem` 枚举，确保 `LiquidBottomTab` 的内容与现有图标/标签一致：

```kotlin
private enum class NavItem(
    val route: String,
    val iconFilled: ImageVector,
    val iconOutlined: ImageVector,
    val label: (AppLang) -> String
) {
    Drivers(
        route = Routes.DRIVERS,
        iconFilled = Icons.Filled.Build,
        iconOutlined = Icons.Outlined.Build,
        label = { lang -> when (lang) {
            AppLang.ZH_CN -> "驱动"
            AppLang.ZH_TW -> "驅動"
            AppLang.EN -> "Drivers"
        }}
    ),
    Settings(
        route = Routes.SETTINGS,
        iconFilled = Icons.Filled.Settings,
        iconOutlined = Icons.Outlined.Settings,
        label = { lang -> when (lang) {
            AppLang.ZH_CN -> "设置"
            AppLang.ZH_TW -> "設定"
            AppLang.EN -> "Settings"
        }}
    )
}
```

---

## 5. 风险与应对

| 风险 | 等级 | 应对措施 |
|------|------|---------|
| Kotlin 2.x 编译错误 | 🟡 中 | 逐文件排查，重点关注 `@Composable` 注解和泛型用法 |
| Compose API 废弃 | 🟡 中 | 参考 JetBrains Compose 1.11.1 文档替换 |
| Navigation Compose 兼容 | 🟢 低 | 保持 2.7.7 版本，API 稳定 |
| DataStore 兼容 | 🟢 低 | 纯 Kotlin 库，无 Compose 依赖 |
| Backdrop 库 API 变更 | 🟢 低 | 锁定 2.0.0 版本，参考源码实现 |
| compileSdk 37 编译警告 | 🟢 低 | 已有 `suppressUnsupportedCompileSdk` 配置 |

---

## 6. 测试计划

### 6.1 编译测试

```bash
./gradlew assembleDebug
```

### 6.2 功能测试

- [ ] 应用正常启动
- [ ] 驱动列表正常加载
- [ ] 语言切换正常工作
- [ ] 设置页面正常显示
- [ ] 导航切换流畅
- [ ] 底部导航栏显示液态玻璃效果

### 6.3 视觉测试

- [ ] 浅色主题下效果正常
- [ ] 深色主题下效果正常
- [ ] 按压缩放动画正常
- [ ] Tab 切换动画流畅
- [ ] 60fps 流畅度达标

### 6.4 设备测试

- [ ] API 26 (Android 8.0) 设备
- [ ] API 34 (Android 14) 设备
- [ ] 不同屏幕尺寸

---

## 7. 回滚方案

如果升级过程中遇到无法解决的问题：

1. **停止当前工作**，不要提交半成品
2. **回退到 `main` 分支**
3. **拆分升级**：先只升级 Kotlin + AGP，不集成 Backdrop
4. **简化方案**：使用自研的简化版液态玻璃效果

---

## 8. 执行清单

### Phase 1: 工具链升级
- [ ] `./gradlew wrapper --gradle-version=9.6.0`
- [ ] 更新 `gradle/libs.versions.toml`
- [ ] 更新根 `build.gradle.kts`
- [ ] 更新 `app/build.gradle.kts`
- [ ] 更新 `settings.gradle.kts`
- [ ] 更新 `gradle.properties`
- [ ] `./gradlew assembleDebug` 验证编译

### Phase 2: 代码适配
- [ ] 修复 Kotlin 2.x 编译错误
- [ ] 替换废弃的 Compose API
- [ ] `./gradlew assembleDebug` 验证编译

### Phase 3: 集成 Backdrop
- [ ] 添加 Backdrop + Shapes 依赖
- [ ] 创建全局 Backdrop 实例
- [ ] 调整 Scaffold 布局层级
- [ ] 重构 GlassBottomBar
- [ ] 测试液态玻璃效果
- [ ] 优化动画参数

### Phase 4: 测试收尾
- [ ] 功能回归测试
- [ ] 视觉效果验证
- [ ] 性能测试（60fps）
- [ ] 代码审查
- [ ] 合并到 main

---

## 附录：关键文件改动预览

### `gradle/libs.versions.toml` — 完整改动

```toml
[versions]
agp = "9.3.0"
kotlin = "2.4.10"
compose = "1.11.1"
kyantShapes = "1.2.0"
activity = "1.13.0"
core = "1.19.0"
coroutines = "1.8.1"
jsoup = "1.17.2"
viewmodel = "2.8.6"
lifecycle-runtime = "2.8.6"
datastore = "1.1.1"
okhttp = "4.12.0"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "core" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activity" }
compose-foundation = { group = "org.jetbrains.compose.foundation", name = "foundation", version.ref = "compose" }
compose-ui = { group = "org.jetbrains.compose.ui", name = "ui", version.ref = "compose" }
compose-ui-graphics = { group = "org.jetbrains.compose.ui", name = "ui-graphics", version.ref = "compose" }
compose-material3 = { group = "org.jetbrains.compose.material3", name = "material3", version.ref = "compose" }
compose-material-icons = { group = "org.jetbrains.compose.material", name = "material-icons-extended", version.ref = "compose" }
kyant-backdrop = { group = "io.github.kyant0", name = "backdrop", version = "2.0.0" }
kyant-shapes = { group = "io.github.kyant0", name = "shapes", version.ref = "kyantShapes" }
androidx-navigation-compose = { module = "androidx.navigation:navigation-compose", version = "2.7.7" }
androidx-lifecycle-viewmodel-ktx = { module = "androidx.lifecycle:lifecycle-viewmodel-ktx", version.ref = "viewmodel" }
androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle-runtime" }
androidx-datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
jsoup = { module = "org.jsoup:jsoup", version.ref = "jsoup" }
okhttp = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttp" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
jetbrains-compose = { id = "org.jetbrains.compose", version.ref = "compose" }
```

---

**预计工期**: 3-5 天（含调试）
**关键路径**: Phase 1 工具链升级 → Phase 2 编译通过 → Phase 3 效果实现
