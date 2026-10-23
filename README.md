# 蜂蜜（muzi 修改版 影視TV）

> **这是 [FongMi/TV](https://github.com/FongMi/TV)（影視TV）的修改版，不是官方版本。**
> 原作者：FongMi 及上游贡献者。版权归原作者所有。本仓库沿用上游的 **GNU GPL v3** 许可证（见 [LICENSE.md](LICENSE.md)），修改部分同样以 GPL v3 发布。
> 有问题请在本仓库提 issue，不要打扰上游作者。

本版由 muzi（Muzili919）维护，供家里和亲友的电视使用，配合 [木子桌面 muzi-studio-tv](https://github.com/Muzili919/muzi-studio-tv)。App 本身不内置任何内容源。

## 版本

| 项目 | 值 |
|---|---|
| 分支 | `muzi`（基于上游 `fongmi` 分支 `f98a376ca`，上游版本 5.8.0 / 580） |
| versionName / versionCode | `5.8.0-muzi.1` / `580001` |
| 包名 | `com.fongmi.android.tv`（与上游相同） |
| 显示名 | 蜂蜜 |
| 最低系统 | Android 7.0（API 24，跟随上游） |
| 签名 | muzi 自己的签名，**与官方版、其他第三方版不同，不能互相覆盖安装** |

Android 6.0 的老电视装不了这一版，请继续使用 4.0.7 木子版（4.0.7-muzi 线）。

## 相对上游的改动

1. **点播页内切换站点**：点播页（VodActivity）新增站点按钮，可打开站点列表或切到下一个可见站点；新站点分类加载完成后再切换，保留分类行。
2. **显示名改为「蜂蜜」**（简体、繁体、默认语言）。
3. **签名配置**：签名不再强制读 `local.properties`，按以下顺序取，**都不入库**：环境变量 `MUZI_KEYSTORE` / `MUZI_KEYSTORE_PASSWORD` / `MUZI_KEY_ALIAS` / `MUZI_KEY_PASSWORD` → 根目录 `signing.env`（`STORE_FILE` / `STORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD`）→ `local.properties`。都没有时不签名。`.gitignore` 增加 `*.keystore`、`signing.env`。
4. **版本号**改为 `5.8.0-muzi.1` / `580001`。该版本名不是 `X.Y.Z` 格式，App 内置的「检查更新」（读 FongMi/Release）会跳过，不会把官方签名的包推给本版用户。本版更新由木子桌面或手动安装提供。

## 编译

环境（跟随上游）：JDK 21、Android SDK（平台版本见 `gradle/libs.versions.toml` 的 `compileSdk`）、Python 3.10。

### 配套 AAR（必需，未入库）

上游 `app/build.gradle` 依赖 `app/libs/*.aar`，其中 `lib-*.aar`（上游自扩展的 Media3 播放器、`lib-mpvplayer-release.aar`、`lib-decoder-ffmpeg-release.aar` 等）**不在上游 Git 里，上游也不公开提供**，本仓库同样没有。单纯 clone 无法编译。获取方式：

- 向上游作者获取与该提交匹配的 AAR；或
- 自行从 [androidx/media](https://github.com/androidx/media) 构建对应模块（`./gradlew :lib-exoplayer:assemble` 等），并补齐上游自扩展的类（mpv 播放器、FFmpeg 解码器接口）。没有这些类，`androidx.media3.mpvplayer.*` 等引用会编译失败。

把得到的 `lib-*.aar` 放进 `app/libs/`（已被 `.gitignore` 忽略，不要提交）。

### 构建命令

```bash
# 签名：任选上面一种方式提供，示例为环境变量（值换成你自己的，不要提交）
export MUZI_KEYSTORE=/path/to/your.jks MUZI_KEY_ALIAS=your-alias
export MUZI_KEYSTORE_PASSWORD=... MUZI_KEY_PASSWORD=...

./gradlew :app:assembleLeanbackRelease   # 电视版，按 ABI 输出 32 位 / 64 位
./gradlew :app:assembleMobileRelease     # 手机版
```

APK 按 ABI 分包输出到 `Release/apk/`。

---

以下为上游原 README：

# 影視TV

Android TV、手機、平板都能用，支援本地檔案與網址播放。

[使用指南](https://fongmi.github.io/TV) · [討論群組](https://t.me/fongmi_official)

## 主要功能

- **瀏覽**：分類篩選、跨站搜尋、收藏、播放記錄與無痕模式。
- **播放**：倍速、字幕、彈幕、片段跳過、畫中畫與背景音訊。
- **直播**：分組選台、節目表、頻道收藏與線路切換。
- **操作**：電視遙控器、手機手勢、DLNA 投放與接收。
- **互通**：網頁遙控、檔案傳輸、記錄同步與 Android Auto。

爬蟲開發說明與 Java、Python、QuickJS 範例見 [CatVodSpider](https://github.com/FongMi/CatVodSpider)。

## Star History

<a href="https://www.star-history.com/?repos=FongMi%2FTV&amp;type=date">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/chart?repos=FongMi/TV&amp;type=date&amp;theme=dark" />
    <img alt="FongMi/TV Star History" src="https://api.star-history.com/chart?repos=FongMi/TV&amp;type=date" />
  </picture>
</a>
