# Termux 基础环境基线

状态：源码已引入，构建与真机验收结果单独记录。本阶段没有 AI、Shizuku 桥接或无障碍功能。

## 上游来源

- 仓库：https://github.com/termux/termux-app
- 标签：v0.118.3（选择一个固定基线，不宣称它是最新版本）
- Commit：5b657c6adf4304e5198951ce815fe0205dcac29c
- 保留根目录与 termux-shared 的许可证，以及源码版权声明。
- 上游 README 保存于 upstream/termux-README.md；未引入上游 GitHub 自动化和公开开发签名密钥。

## 首轮决策

保留 applicationId `com.termux`、Java 包路径及 `/data/data/com.termux/files/usr` 软件包前缀。应用显示名称改为 DroidUse。先验证既有 bootstrap 和包环境，独立包名作为后续迁移工作，不与基础验证混在一起。

因此此原型与原版 Termux 占用同一包名，不能并存。若手机已有不同签名的 Termux，先备份并制定迁移方案，不自动卸载或覆盖数据。

## 固定构建参数

| 参数 | 值 |
| --- | --- |
| JDK | 11（构建阶段） |
| Gradle wrapper | 7.2 |
| Android Gradle Plugin | 4.2.2 |
| compile SDK / target SDK / min SDK | 30 / 28 / 24 |
| Build tools | 30.0.3 |
| NDK | 22.1.7171670 |
| Bootstrap | 2025.03.28-r1+apt-android-7，沿用上游各架构 SHA-256 校验 |

这些是所选源码的参数，不代表目标手机的 Android 版本。首轮不同时升级工具链或 target SDK。

安装 Android SDK 和上述组件，配置 ANDROID_HOME，使用 JDK 11 后运行：

```sh
./gradlew --no-daemon :terminal-emulator:testDebugUnitTest :app:assembleDebug
```

APK 位于 app/build/outputs/apk/debug/。CI 自动生成临时 debug 签名，仅用于基础验证；不同 CI 运行的签名可能不同，不承诺覆盖升级。持续使用前需配置稳定的个人签名，密钥不入库。

## 真机验收清单

目标：vivo X200 / Android 15 / OriginOS 6，Shizuku 无线调试。

- [ ] 安装并首次启动，bootstrap 解压成功。
- [ ] 终端执行 `printf 'DroidUse OK\n'`。
- [ ] 执行 `uname -m`、`getprop ro.product.cpu.abilist`，确认架构。
- [ ] 在 HOME 中创建临时文本，使用 shell 统计行数并读取结果。
- [ ] 退出并重新打开 App，工作文件仍存在。
- [ ] 后台切换及进程恢复行为记录实际结果。

编译成功不能替代以上真机验收。Shizuku 与手机界面控制将在 #3、#4 中实现。

## 本次验证记录

- DroidUse 自有修改的 `git diff --check` 通过；完整上游导入保留部分既有空白字符，Windows wrapper 在连接上传时采用 CRLF。
- 本地构建在下载 Gradle 7.2 时遇到 `Network is unreachable`，未进入编译；当前环境仅有 JDK 17 且缺少 Android SDK，不能据此判定源码构建成功。
- [GitHub Actions #2](https://github.com/lilong7676/DroidUse/actions/runs/34438037988) 构建成功，验证提交 `ca80ebd07245ce412e1ae2249687126b38ea1ad2`，日志记录 145 项终端测试 PASSED。
- 构建产物 `droiduse-foundation-debug` 已上传，包含各架构与 universal 的 debug APK，保留至 2026-09-17。
- 真机安装、bootstrap 和 Shell 执行仍待验证，本阶段不声明已在目标手机跑通。
