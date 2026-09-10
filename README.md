# DroidUse

运行在安卓手机上的 AI Agent：理解自然语言任务，动态编写脚本，操作应用并用真实结果验证完成。

## 项目方向

- 一个一体化安卓 App，基于 Termux 二次开发，内置终端和脚本运行环境。
- Agent 在手机本地运行，模型通过用户配置的 provider API 接入。
- 结合安卓原生能力、Shizuku 和无障碍服务，动态完成手机操作。
- 面向个人自用，不需要另装原版 Termux 或 Tasker；Shizuku 使用已有安装。

## 当前进度

当前分支已引入 Termux v0.118.3 基础源码，尚未完成构建和真机验收，也尚未接入 AI 或手机控制能力。

- [上游基线、构建说明与验收清单](docs/termux-baseline.md)

- [项目文档](docs/DroidUse-project.md)
- [实现与迭代计划](docs/DroidUse-implementation-plan.md)
- [首轮任务](https://github.com/lilong7676/DroidUse/issues)

| 阶段 | 目标 |
| --- | --- |
| M0 | 验证 Termux 基础环境、Shizuku、界面操作和脚本能力桥接（#1–#5） |
| M1 | 接入模型与本地 Agent，完成首个 Mobile Phone Use 闭环 |
| M2 | 保存任务经验，逐步扩展通知与定时自动化 |

首轮先证明同一个 APK 能运行代码并操作手机，再完善 AI 聊天体验。真机验证前需确认手机型号、系统版本与 Shizuku 启动模式。

## 开发约定

功能使用短期分支和 PR，关联 Issue 与实际验收证据。引入上游代码时记录完整 commit SHA 并保留许可证。模型密钥、签名密钥和个人手机数据不进入仓库。
