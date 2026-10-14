# spec: dev-environment — 开发环境（宪法化）

> Status: active | Date: 2026-10-14 | Constitution: yes
> 用途：B.4 验证纪律中「真机走查」的环境基线；环境不达标产出的验证结果不计入 `[x]`。

## 1. 工具链基线（版本钉死）

| 工具 | 版本要求 | 说明 |
|---|---|---|
| Node.js | **≥22.13**（LTS） | CI 与本地一致；`node -v` 断言入 CI |
| pnpm | ≥9（isolated 安装） | 唯一包管理器；CI 一律 `pnpm install --frozen-lockfile`；**禁止** `npm ci`/`npm install` 现场解析 |
| EAS CLI | 按 specs/tech-stack.md 钉版 | Build / Update / Submit / Deploy；`EXPO_TOKEN` 只挂 production environment secrets |
| expo-dev-client | 随 SDK 57 | **Development Build 起步**，Expo Go 不可用（安全模块/生物识别在 Go 下不生效） |
| Xcode | 当前 stable（iOS 构建） | 仅 iOS 任务需要；EAS Build 云构建可兜底 |
| Android Studio + NDK | 当前 stable | SDK Platform、CMake/NDK（roadmap C++ 化用）；本地 `npx expo run:android` |
| watchman / jq 等 | 最新 stable | 辅助工具，不入关键路径 |

## 2. 真机要求（手工验证环境基线）

- **Android 真机**：至少一台干净机（Pixel 6a 级，GMS 完整）+ 一台 Root/隐藏环境测试机（Magisk+Zygisk+Shamiko）做对照演示；模拟器结果仅作辅助，**安全相关 AC 必须实体真机**（AGENTS.md B.4-5）。
- **iOS 真机**：一台支持生物识别的 iPhone（Secure Enclave）；模拟器无 SE，凡依赖 SE/Keychain ACL 的结论在模拟器上一律无效。
- **Web**：桌面 Chrome/Safari 最新稳定版各一。
- 所有真机走查须录屏归档（机型 + OS 版本 + 走查结论写入 validation.md）。

## 3. 本地密钥与配置管理（never 红线）

1. 本地敏感配置（RPC endpoint、API key、EAS token）一律走 **`.env` + `app.config.ts`（expo-constants extra）注入**，`.env` 必须入 `.gitignore`；**禁止**把任何密钥/凭据写进 `app.json` 或源码。
2. **禁止提交**：`.env*`、`private-key.pem`、任何 keystore/证书私钥、EAS/商店凭据——CI grep 命中即流水线失败（R2 / spec: ota-update TC-OTA-003）。
3. OTA 代码签名私钥**离线保管**（KMS/密码管理器），本地签名不出机；即使 EAS remote credentials 托管，也必须 `eas credentials` 导出并离线冷备份。
4. 缺少配置时界面必须显示「Not configured」，**禁止以假数据充数**（R5）。
5. 真钱流程禁止远程 JS 调试（R8）；release build 无法连接 Metro/Chrome DevTools 属验收项（登记为 MAN 用例）。
