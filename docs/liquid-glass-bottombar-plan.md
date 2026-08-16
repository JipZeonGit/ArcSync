# ArcSync 底部导航栏液态玻璃重构计划

## 1. 项目概述

### 1.1 背景

当前 ArcSync 的底部导航栏采用纯手工绘制实现，虽然有玻璃质感效果，但与业界主流的液态玻璃（Liquid Glass）效果相比仍有差距。参考 [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) 开源库的设计标准，我们需要对底部导航栏进行重构。

### 1.2 目标

- 实现符合 iOS 26 / iPadOS 26 设计语言的液态玻璃效果底部导航栏
- 支持拖拽手势交互和弹性动画
- 保持与现有应用架构的兼容性
- 提升整体 UI 品质和用户体验

---

## 2. 技术分析

### 2.1 参考库架构

`AndroidLiquidGlass` 库的核心组件：

| 组件 | 说明 |
|------|------|
| `Backdrop` | 接口，定义背景层绘制能力 |
| `drawBackdrop()` | Modifier 扩展，应用液态玻璃效果 |
| `LayerBackdrop` | 支持层级的背景层 |
| `effects.lens()` | 折射效果（核心液态玻璃效果） |
| `effects.blur()` | 模糊效果 |
| `effects.vibrancy()` | 色彩增强效果 |
| `Shadow` / `InnerShadow` | 外阴影和内阴影 |

### 2.2 LiquidBottomTabs 实现要点

```kotlin
// 核心结构
LiquidBottomTabs(
    selectedTabIndex = { currentIndex },
    onTabSelected = { /* 切换逻辑 */ },
    backdrop = backdrop,           // 全局背景层
    tabsCount = 2,                 // Tab 数量
    modifier = Modifier
) {
    // Tab 内容
    LiquidBottomTab(onClick = { /* ... */ }) {
        Icon(...)
        Text(...)
    }
}
```

### 2.3 关键效果参数

```kotlin
// 容器效果
.drawBackdrop(
    backdrop = backdrop,
    shape = { Capsule() },
    effects = {
        vibrancy()                    // 色彩增强
        blur(8f.dp.toPx())           // 背景模糊
        lens(24f.dp.toPx(), 24f.dp.toPx())  // 折射
    },
    onDrawSurface = { drawRect(containerColor) }  // 半透明底色
)

// 指示器效果（选中项滑块）
.drawBackdrop(
    backdrop = combinedBackdrop,
    shape = { Capsule() },
    effects = {
        lens(
            10f.dp.toPx() * progress,
            14f.dp.toPx() * progress,
            chromaticAberration = true  // 色散效果
        )
    },
    shadow = { Shadow(alpha = progress) },
    innerShadow = { InnerShadow(radius = 8f.dp * progress, alpha = progress) }
)
```

### 2.4 交互动画系统

库使用 `DampedDragAnimation` 实现阻尼拖拽：
- 支持按压缩放（pressedScale = 78/56）
- 拖拽停止时自动吸附到最近的 Tab
- 速度影响缩放比例，产生弹性视觉反馈

---

## 3. 当前实现分析

### 3.1 现有代码结构

文件：`app/src/main/java/com/jipzeongit/arcsync/ui/AppRoot.kt`

```
AppRoot
├── Scaffold
│   ├── Box (主内容)
│   │   ├── NavHost (页面内容)
│   │   └── GlassBottomBar (底部导航栏)  ← 需要重构
│   └── GlassTopBar (顶部导航栏)
```

### 3.2 现有实现问题

| 问题 | 说明 |
|------|------|
| 没有使用 Backdrop | 无法实现真正的液态玻璃效果 |
| 玻璃效果简陋 | 仅使用半透明背景 + 简单模糊 |
| 缺少折射效果 | 没有 lens 效果，视觉层次不足 |
| 无拖拽交互 | Tab 切换只有点击，没有手势反馈 |
| 动画生硬 | 使用简单动画，缺少弹性效果 |

---

## 4. 重构方案

### 4.1 方案选择

**方案 A：直接集成 Backdrop 库（推荐）**

优点：
- 开箱即用，效果与参考库一致
- 维护成本低，跟随上游更新
- API 设计成熟，扩展性好

缺点：
- 增加依赖体积
- 需要适配库的 API 设计

**方案 B：自研实现**

优点：
- 完全可控，可深度定制
- 无外部依赖

缺点：
- 开发成本高
- 需要理解复杂的着色器和渲染管线
- 维护成本高

**决策：采用方案 A**

### 4.2 集成步骤

#### 步骤 1：添加依赖

在 `app/build.gradle.kts` 中添加：

```kotlin
dependencies {
    // Backdrop 库
    implementation("io.github.kyant0:backdrop:1.0.0")  // 使用最新版本
    
    // Shapes 库（Backdrop 依赖）
    implementation("io.github.kyant0:shapes:1.0.0")
}
```

#### 步骤 2：设置 Backdrop

在 `MainActivity` 或 `AppRoot` 中创建全局 Backdrop：

```kotlin
@Composable
fun AppRoot(settingsRepository: SettingsRepository) {
    val backdrop = rememberBackdrop()  // 创建全局 backdrop
    
    Scaffold(
        modifier = Modifier.backdrop(backdrop)  // 应用到根布局
    ) { padding ->
        // ... 现有内容
    }
}
```

#### 步骤 3：重构 GlassBottomBar

```kotlin
@Composable
private fun GlassBottomBar(
    currentRoute: String?,
    driversLabel: String,
    settingsLabel: String,
    backdrop: Backdrop,  // 新增参数
    onNavigate: (String) -> Unit
) {
    val navItems = remember { listOf(NavItem.Drivers, NavItem.Settings) }
    val selectedIndex = navItems.indexOfFirst { 
        it.route == currentRoute 
    }.coerceAtLeast(0)
    
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
                        tint = if (isSelected) {
                            Color(0xFF0088FF)  // 亮色模式主题色
                        } else {
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

#### 步骤 4：调整布局层级

由于 Backdrop 需要访问底层内容进行模糊处理，需要调整布局：

```kotlin
Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets(0, 0, 0, 0)
) { padding ->
    val backdrop = rememberBackdrop()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .backdrop(backdrop)  // 在这里应用 backdrop
    ) {
        // NavHost（内容在 backdrop 之下）
        NavHost(...)
        
        // Top Bar（在 backdrop 之上）
        GlassTopBar(...)
        
        // Bottom Bar（在 backdrop 之上，应用液态玻璃效果）
        GlassBottomBar(
            backdrop = backdrop,
            ...
        )
    }
}
```

---

## 5. 深色模式适配

```kotlin
// 根据主题动态调整颜色
val isLightTheme = !isSystemInDarkTheme()

val accentColor = if (isLightTheme) {
    Color(0xFF0088FF)  // 亮色模式：蓝色
} else {
    Color(0xFF0091FF)  // 暗色模式：亮蓝色
}

val containerColor = if (isLightTheme) {
    Color(0xFFFAFAFA).copy(alpha = 0.4f)  // 亮色模式：浅灰半透明
} else {
    Color(0xFF121212).copy(alpha = 0.4f)  // 暗色模式：深灰半透明
}
```

---

## 6. 开发计划

### Phase 1：准备工作（1-2 天）

- [ ] 添加 Backdrop 库依赖
- [ ] 验证库在目标 API 级别（minSdk 26）的兼容性
- [ ] 创建新分支 `feature/liquid-glass-bottombar`

### Phase 2：基础集成（2-3 天）

- [ ] 在 AppRoot 中创建全局 Backdrop
- [ ] 调整 Scaffold 布局层级
- [ ] 实现基础的 LiquidBottomTabs
- [ ] 验证导航功能正常

### Phase 3：效果调优（2-3 天）

- [ ] 调整模糊、折射、阴影参数
- [ ] 适配深色模式
- [ ] 添加按压动画和拖拽交互
- [ ] 优化性能，确保 60fps 流畅度

### Phase 4：测试与收尾（1-2 天）

- [ ] 在不同设备上测试（特别是 API 26-33 的兼容性）
- [ ] 处理边缘情况（如页面切换动画）
- [ ] 代码审查和文档更新
- [ ] 合并到主分支

---

## 7. 风险与应对

| 风险 | 影响 | 应对措施 |
|------|------|----------|
| API 兼容性 | 高 | 库要求 API 26+，与项目 minSdk 一致，暂无风险 |
| 性能问题 | 中 | 监控帧率，必要时降低效果复杂度 |
| 主题冲突 | 低 | 使用 MaterialTheme 颜色系统，减少硬编码 |
| 上游更新 | 低 | 锁定版本号，定期评估更新 |

---

## 8. 参考资源

- [AndroidLiquidGlass 仓库](https://github.com/Kyant0/AndroidLiquidGlass)
- [Backdrop 文档](https://kyant.gitbook.io/backdrop)
- [Material 3 NavigationBar](https://m3.material.io/components/navigation-bar)
- [Jetpack Compose Animation](https://developer.android.com/develop/ui/compose/animation)

---

## 附录：关键 API 速查

### Backdrop 创建

```kotlin
// 全局 Backdrop
val backdrop = rememberBackdrop()

// Layer Backdrop（用于嵌套层）
val layerBackdrop = rememberLayerBackdrop()

// 组合 Backdrop
val combinedBackdrop = rememberCombinedBackdrop(backdrop, layerBackdrop)
```

### Modifier 扩展

```kotlin
Modifier
    .drawBackdrop(
        backdrop = backdrop,
        shape = { Capsule() },
        effects = { /* 效果配置 */ },
        highlight = { /* 高光配置 */ },
        shadow = { /* 阴影配置 */ },
        innerShadow = { /* 内阴影配置 */ },
        layerBlock = { /* 图层变换 */ },
        onDrawSurface = { /* 绘制底层表面 */ },
        onDrawFront = { /* 绘制前景 */ }
    )
```

### 效果函数

```kotlin
effects {
    blur(radius: Float)           // 模糊
    lens(height: Float, amount: Float, chromaticAberration: Boolean)  // 折射
    vibrancy()                    // 色彩增强
}
```
