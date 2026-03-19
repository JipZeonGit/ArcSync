# ArcSync

[English](#english) | [简体中文](#简体中文)

## 简体中文

ArcSync 是一个面向 Intel Arc / Intel Graphics 用户的第三方 Android 开源工具，用于更方便地查看、筛选和打开 Intel 官方驱动下载页面。

### 项目目标

Intel 官方下载中心在手机端浏览时信息层级较深、跳转较多，切换语言和查找历史版本也不够直接。ArcSync 的目标是把这些信息收敛到一个更适合移动端的界面里，让用户可以更快地获取自己需要的驱动版本信息。

### 主要功能

- 从 Intel 官方本地化页面抓取驱动信息
- 支持简体中文、繁体中文、英文三语切换
- 语言切换时自动切换对应语言的数据源
- 在驱动主页展示最新 8 个版本的驱动卡片
- 显示版本号、发布日期、文件大小、SHA256、WHQL 标识等摘要信息
- 点击卡片进入驱动详情页，查看介绍、详细说明和适用产品
- 一键跳转浏览器打开官方链接或下载地址
- 支持下拉刷新驱动列表
- 支持浅色、深色、跟随系统、AMOLED 黑色主题、动态取色
- 支持导出应用日志，便于排查抓取或显示问题

### 技术栈

- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- ViewModel / Lifecycle
- DataStore
- OkHttp
- Jsoup

### 设计说明

本项目在 UI 设计上参考了 GitHub Store 项目的优秀思路，尤其是玻璃感底栏、加载动画和整体视觉节奏。ArcSync 并不是 GitHub Store 的移植版本，而是在其设计启发下，结合本项目需求重新实现的 Android 应用。

### 许可证

本项目当前使用 `AGPL-3.0` 许可证。使用、修改、分发本项目时，请遵循仓库中的许可证文本。

### 免责声明

ArcSync 是社区驱动的第三方非官方开源工具，与 Intel Corporation 没有任何关联、授权或背书。所有驱动内容、说明文本和下载资源均以 Intel 官方页面为准。

---

## English

ArcSync is a third-party open-source Android app for Intel Arc / Intel Graphics users. It provides a more convenient mobile interface for browsing Intel's official driver information and opening the official download pages.

### Project Goal

Intel Download Center is not always comfortable to browse on a phone: information is spread across multiple sections, localized pages are separated, and older versions are not surfaced clearly. ArcSync aims to condense that workflow into a cleaner mobile-first experience.

### Features

- Fetches driver information from Intel's official localized pages
- Supports Simplified Chinese, Traditional Chinese, and English
- Automatically switches the data source when the app language changes
- Shows the latest 8 driver versions as cards on the main drivers screen
- Displays version, release date, size, SHA256, WHQL status, and related summary data
- Opens a dedicated detail screen for each driver entry
- Lets users jump to the official browser/download link with one tap
- Pull-to-refresh support for the drivers list
- Light, dark, follow-system, AMOLED black, and dynamic color themes
- App log export for troubleshooting parsing or rendering issues

### Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- ViewModel / Lifecycle
- DataStore
- OkHttp
- Jsoup

### Design Note

This project takes visual inspiration from the excellent UI design of GitHub Store, especially its glass-like bottom bar, loading motion, and overall presentation rhythm. ArcSync is not a port of GitHub Store; it is a separate Android implementation built for this project's driver-focused use case.

### License

This project currently uses the `AGPL-3.0` license. Please review the repository license text before using, modifying, or redistributing the project.

### Disclaimer

ArcSync is a community-driven unofficial open-source tool and is not affiliated with, endorsed by, or authorized by Intel Corporation. Driver metadata, descriptions, and downloads remain subject to Intel's official pages.