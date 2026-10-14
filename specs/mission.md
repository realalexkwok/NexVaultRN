# spec: mission — NexVault RN/Expo 三端冷钱包

> Status: active | Date: 2026-10-14 | Constitution: yes
> 上位文件：AGENTS.md（工程治理宪法）；决策依据：doc/research-report.md（预研报告，2026-10-14）。

## 1. 使命（Mission）

将原纯 Kotlin 的 NexVault 冷钱包移植为 **Expo（React Native）三端工程**，交付一个**求职演示用途的可验证原型**：72 小时内产出 Android / iOS / Web 三端可演示版本，证明「私钥不出 native + 原生签名闭环 + 供应链/热更防线」这套架构在 RN 技术栈上成立。

- **产品形态**：EVM 冷钱包（BIP-39/32/44，secp256k1），三端信任分级——**Android 全功能 / iOS 全功能 / Web 只读观察钱包**（不持私钥，经 WalletConnect/二维码桥接回手机签名）。
- **目标用户**：**海外用户**。不面向中国大陆：不上架、不提供下载、不做可用性承诺（geo-fence；报告 §6.6）。
- **安全对标**：银行级移动金融安全口径——OWASP MASVS 对齐、诚实三级标注（[P] 已落地防护 / [C] 仅提高攻击成本 / [S] 占位接口，AGENTS.md R6），威胁模型与攻击-防御映射见报告第 6 章。

## 2. 范围（In scope）

1. vault-core native 闭环：`importMnemonic / deriveAddress / signDigest / destroy` 四方法（specs/vault-core.md）。
2. 密钥静态保护：Android Keystore AES-GCM wrap + per-use 生物识别；iOS Keychain ACL（specs/security-android.md / security-ios.md）。
3. 端上完整性/环境检测 + fail-closed 门控（demo 级子集，对照可演示）。
4. 链交互：viem 构造→native 签名→广播→回执校验（specs/web3-adapter.md）。
5. OTA：EAS Update 代码签名（红线 R2）、fingerprint runtimeVersion、灰度/回滚（specs/ota-update.md）。
6. 三端 UI 骨架与核心流程（specs/ui-wallet.md）。

## 3. 非目标（Out of scope / Non-goals）

- **不是生产产品**：3 天交付「骨架正确、降级诚实、roadmap 清晰的验证品」，非经审计的可托管真实资产的钱包。
- 多链（BTC/Solana）、多账户体系、法币出入金、内置兑换——一律不在范围。
- Web 端永不实现助记词导入/私钥签名/广播写操作（红线 R7，属架构性排除而非排期）。
- TEE/SE 内 secp256k1 签名（平台不支持，报告 §3.2.1 实证）；Passkey PRF 密钥托管（报告 §3.4 否决）。
- 面向中国大陆的任何分发、合规与可用性工作。

## 4. 成功判据

- 三道每日门槛 G1–G3 通过（plans/3-day-implementation-plan.md §7.2 口径：真机签名闭环、Root/Frida 对照拦截、签名 OTA 真实生效）。
- 全部演示宣称与 [P]/[C]/[S] 标注一致；**不演示假签名**（红线）。
- 每份交付物可溯源：决策指向 doc/research-report.md 章节，用例以 TC/MAN 编号登记。
