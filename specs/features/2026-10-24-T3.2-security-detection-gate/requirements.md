# T3.2 — 安全检测 + fail-closed 门控（security detection + E_VAULT_SECURITY_GATE）— requirements

> Feature：T3.2（roadmap 阶段 1 Day 2 下午，plans/3-day-implementation-plan.md 行 62；AGENTS Part A「下一步 = T3.2 启动」）
> Date：2026-10-24 | Status：规划轮完成（exit_plan_mode approved；D1–D5 按 Recommended 默认，两轮 ask_user_question 超时未获实时答复）
> 上位文件：AGENTS.md（B.2/B.4/B.7、R1/R5/R6/R7）；specs/security-android.md §2.2/§2.5/§3；specs/security-ios.md §2.1/§2.3/§2.4；specs/vault-core.md §2.4（E_VAULT_SECURITY_GATE 已登记）；specs/pending-manual-test-cases.md（MAN-004）
> Skill：.agents/skills/nexvault-t3-2-security-detection-gate/SKILL.md（owner 2026-10-24 安装，commit `3862902`）

## 1. Goal

落地六项检测信号（①native 签名校验 ②bundle 哈希 ③Root ④越狱 ⑤反调试 ⑥Frida 多信号；Android = ①②③⑤⑥，iOS = ④⑤⑥）→ risk score（T3.2 子集映射 L0/L4）→ `E_VAULT_SECURITY_GATE` 在 vault-core `importMnemonic`/`signDigest` 的 **fail-closed 耦合** + JS 面（含 H-T3.1-1/2/7 implement）。演示子集 = 干净机 L0 放行 + Magisk 机拒绝两段对照（MAN-021 + MAN-004，录屏）；完整多信号链 → roadmap 阶段 2。

成功判据（owner 门禁）：tsc 双形态 + modules gate 0；JVM parser 测试全绿；干净机 L0 + Magisk 拒绝演示（MAN + 录屏）；R1/R5 grep 干净；零未批准依赖；评审零发现轮。R6 口径：**全部检测信号 = [C]；唯一 [P] = 真机验证过的 fail-closed 耦合**。

## 2. Owner decisions（Q/A 表）

两轮 ask_user_question（2026-10-24 12:01 / 12:21，各 120s / 600s）owner 不在场超时；按 **Recommended 默认**落盘，本计划经 exit_plan_mode 批准（2026-10-24）。owner 可在 GATE-MODEL-SWITCH 前推翻任一裁定（plan.md Task 1 前置条件）。

| # | 问题 | 默认裁定（Recommended） | 备选 |
|---|---|---|---|
| D1 | GATE-R4-IOSSECURITYSUITE：security-ios.md §2.3 原写 IOSSecuritySuite（不在 tech-stack） | **自写基础向量**（/var/jb 路径族 + dyld 可疑库 + sysctl），零新依赖；§2.3 同步修订为自研；完整 Dopamine 对抗 → roadmap | b) R4 修宪引入该库；c) iOS 检测整体 defer |
| D2 | Magisk 测试机（MAN-004 拒绝演示）是否就位 | **若就位** → 真机真实 Root 信号拒绝演示 + 录屏；**若无** → [P] 耦合在干净 Pixel 6a 经文档化 debug-only 测试注入口验证（真实 assert→throw 链路），MAN-004 保持待走查、设备到位补录屏 | c) Magisk 模拟器录屏（B.4-5 仅辅助） |
| D3 | 期望哈希注入（签名证书 SHA-256 + bundle SHA-256） | **自写 CNG config plugin** apps/mobile/plugins/withSecurityHashes.js（gradle 任务从 variant signingConfig 读证书、hash bundle 产物 → string resource） | b) 硬编码常量 + 两段构建脚本 |
| D4 | iOS 侧交付形态（本机无 Xcode） | **撰写 + XCTest + MAC-HANDOFF [~]**（T1.5/T3.1 模式），不标 [P] | b) iOS 腿整体 defer（须 owner 改判 H-T3.1-1） |
| D5 | 演示子集口径 | **确认**：MAN-021（干净机 L0）+ MAN-004（Magisk 拒绝）两段录屏；反调试/bundle 篡改/重签名 → T5.4 视频；完整多信号链 → roadmap | b) 扩大演示范围 |

## 3. Handoff re-read（B.7-2；T3.1 validation.md §Task 7 表逐条裁定）

| Handoff | 本规划轮裁定 | 落点 |
|---|---|---|
| H-T3.1-1 | **implement**：iOS JS 面 wrap/unwrap + TC-SI-009 完整 KEK 生命周期 | T3.2（撰写 + XCTest + MAC-HANDOFF，[~]） |
| H-T3.1-2 | **implement**：Android JS 面 wrapKey/unwrapKey（ArrayBuffer）+ gate 耦合 | T3.2 |
| H-T3.1-3 | replan（不变） | T5.x |
| H-T3.1-4 | replan（不变） | T5.x |
| H-T3.1-5 | replan（不变） | 设备到位后 |
| H-T3.1-6 | replan（不变，2026-10-23 已裁） | roadmap 阶段 3 末 |
| H-T3.1-7 | **implement**：持久化 schema 标记 + 无条件 delete+ensure 重建（禁止「检测」旧键） | T3.2 |
| H-T3.1-8 | **confirm closed**（MAN-020 2026-10-23 11:25 通过；表内销案） | — |
| GATE-R4-IOSSECURITYSUITE | 按 D1（默认 cancel 依赖意图 → 自研向量；§2.3 修订） | T3.2（D1） |

## 4. 功能需求（FR）

- FR-1（六信号，平台分叉；全部 [C]）：Android：`SignatureSelfCheck`（GET_SIGNING_CERTIFICATES，API≥28；24–27 GET_SIGNATURES 降级；首证书 SHA-256 vs 资源常量 nexvault_expected_cert_sha256）、`BundleSelfCheck`（assets/index.android.bundle SHA-256 vs 资源常量 nexvault_expected_bundle_sha256；asset 缺失（debug/Metro）→ bundleEvaluated=false 不计入风险）、`RootDetector`（su 路径 / Magisk 包名 / build.prop 属性，解析器纯函数）、`AntiDebugDetector`（/proc/self/status TracerPid 纯解析）、`FridaDetector`（端口 27042 + /proc/self/maps 特征串，纯解析器）。iOS（撰写侧）：`JailbreakDetector`（路径族 /var/jb、Cydia、sshd、MobileSubstrate、/etc/apt + dyld 可疑库 FridaGadget/systemhook/Ellekit + sysctl）、`denyDebuggerAttach`（dlsym ptrace PT_DENY_ATTACH=31，绝不直接链接）、`isDebuggerAttached`（P_TRACED）。
- FR-2（聚合与 TS 面，spec §2.2）：`runDetection('fast'|'full')`（fast=签名+bundle；full=+root/frida/debug）、`getCachedRiskLevel()`（同步、内存缓存 <1ms）、`assertSigningAllowed()`（≥L3 抛 E_SEC_GATE_DENIED）；`DetectionReport` 只携带向量 ID（ROOT_SU_BIN / FRIDA_PORT_27042 / DBG_TRACERPID…，不落明文细节）；T3.2 子集映射 = 任一硬信号 → L4，否则 L0（L1–L3/L5 attestation 语义 → T5.x）；`SecurityState` 仅内存（不落盘）；`debugApplyForTest()` = BuildConfig.DEBUG 守卫的测试注入口（KDoc 注明 test-only、release 无操作、不经 JS）。
- FR-3（fail-closed 耦合；[P] 条件 = 真机验证）：vault-core `importMnemonic`（decode 前）/`signDigest`（requireDigest32/store 前）调 `SecurityGate.assertSigningAllowed()`；拒绝抛 `E_VAULT_SECURITY_GATE`（固定模板，不回显信号）；拒绝路径不触碰密钥材料、既有 fill(0)/finally 清零照跑；deriveAddress/destroy 不门控（恢复流恒可用）。调用序 gate → (biometric) → crypto。
- FR-4（H-T3.1-2）：SecurityAndroidModule 新 AsyncFunction wrapKey(ByteArray,ByteArray)/unwrapKey(ByteArray,ByteArray)，先 assertSigningAllowed 再走 WrapKeyManager（appContext.currentActivity as? FragmentActivity，null → 固定错误）；TS 面（modules/security-android/index.ts）ArrayBuffer 输入 finally fill(0)、输出新拷贝；vault-core 签名路径仍走 native-internal unwrap（spec §6 R1 联动），明文不跨 JS 持久化。
- FR-5（H-T3.1-7）：WrapKeyManager 增 `ensureCurrentSchemaKey()`——SharedPreferences 标记 wrap_key_schema_tag ≠ tee-biometric-v1 → 无条件 deleteKey()+ensureKey()+写标记；blob 格式不变（version 字节=1）；不「检测」旧键类型（AES 无 KeyInfo）。
- FR-6（H-T3.1-1 + iOS 检测）：SecurityIosModule 接管 JS 面（wrapKey/unwrapKey/runDetection/getCachedRiskLevel/assertSigningAllowed）；Swift SecurityGate（L0/L4 子集）；SecurityErrors 补 E_SEC_GATE_DENIED；VaultCoreModule.swift/VaultCoreService.swift importMnemonic/signDigest 前置门控；VaultCore.podspec s.dependency 'SecurityIos'；Tests 增 TC-SI-009（完整 KEK 生命周期，test-only 坐标）+ 越狱 collector fixture 正负例 + gate deny fixture；MAC-HANDOFF.md 更新（状态 [~]）。
- FR-7（D3 期望哈希注入）：自写 CNG config plugin apps/mobile/plugins/withSecurityHashes.js 在 **prebuild 时**把 `apps/mobile/security-hashes.json` 的期望值写成 `android/app/src/main/res/values/nexvault_security_hashes.xml`（`nexvault_expected_cert_sha256` / `nexvault_expected_bundle_sha256`；值非法/缺失 → `NOT_EVALUATED`）。**2026-10-24 实现裁定（D3(b) 回退，plan Task 4/Task 8 明示）**：首版 Gradle 任务生成资源目录被 Gradle 9 任务依赖校验连续拒绝（mapSourceSetPaths / generateResources / extractDeepLinks），改用两段构建回填——`scripts/update-security-hashes.mjs <apk>` 从已构建 APK 取证书 SHA-256（apksigner；keytool 无 v1 签名不可用）与内嵌 `assets/index.android.bundle` SHA-256 写回 JSON，再 prebuild+重建（本地形态；CI 自动回填属 roadmap 阶段 2）。JSON 与产物不一致时自检 mismatch → gate fail-closed 拒绝签名（不是静默放行），演示前必须重跑脚本。
- FR-8（R5/R6 纪律 + 错误码）：SecurityErrorCodes 补 E_SEC_GATE_DENIED / E_SEC_SIG_MISMATCH / E_SEC_BUNDLE_MISMATCH（固定模板）；vault-core 补 E_VAULT_SECURITY_GATE；模块内无 Log./println/print(/NSLog（grep 门禁）；R6 表：签名 [C] · bundle [C] · Root [C] · 越狱 [C] · 反调试 [C] · Frida [C] · fail-closed 耦合 [P]（仅真机验证后）；debug-only 注入口文档化。
- FR-9（App 侧）：_layout.tsx 首帧后 InteractionManager.runAfterInteractions → runDetection('fast')（fire-and-forget，不阻塞首帧）；store/security.ts（Zustand {riskLevel, report}）；(tabs)/settings.tsx 诚实读数（risk 级别 + 向量 ID + 「检测=[C]，门控=[P]」标注）。
- FR-10（演示与 MAN）：MAN-021 新登记（干净机 L0 基线：Settings L0 + 同机 instrumentation，录屏）；MAN-004 按 D2 更新状态（a=Magisk 真机拒绝录屏关闭；b=挂起+handoff）。

## 5. Out of scope（边界诚实，逐项带指针）

- 完整多信号交叉全覆盖（mounts/mountinfo 交叉、PID namespace、Shamiko 对抗、inline hook 序言自检）→ roadmap 阶段 2 #1（4.5–6 人日）
- .so 内嵌签名块自解析（spec §2.4 SignatureSelfCheck 完整版）→ roadmap
- attestation（Play Integrity / Key Attestation / App Attest）与 L1–L3/L5 分级语义 → T5.x（H-T3.1-4）
- 反调试 self-attach TRACEME / task stat 组合 → roadmap（spec T-SA-6）
- 重打包 CI 流水线、apktool 篡改演示、T5.4 对照视频 → T5.4
- UI L0–L5 分级指示器打磨 → T4.x polish
- iOS 真机/Mac 验证（MS-1..MS-4、TC-SI-004/005 真机腿）→ 设备到位后（H-T3.1-5）
- SOFTWARE_DEBUG 生产禁用策略 → security-testing/T5.x

## 6. 验收标准（AC → TC/MAN → 门禁）

| AC | 内容 | 绑定用例 | 验证 |
|---|---|---|---|
| AC-1 | 六信号落代码（平台分叉）+ [C] 标注齐备 | TC-T32-001..003 | 代码评审 + grep |
| AC-2 | JVM parser/聚合测试全绿（12 旧 + 新增，注入 fixture） | TC-T32-001..005 | :security-android:testDebugUnitTest |
| AC-3 | 干净机 runDetection('full') → L0 全清、assertSigningAllowed 放行、import→derive→sign roundtrip OK | TC-T32-006 | Pixel 6a instrumentation |
| AC-4 | gate deny E2E：seed L4 → assertSigningAllowed 抛 E_SEC_GATE_DENIED → vault-core importMnemonic/signDigest 抛 E_VAULT_SECURITY_GATE；复位后 L0 放行（= [P] 耦合真机证据） | TC-T32-007 | Pixel 6a instrumentation |
| AC-5 | wrapKey/unwrapKey JS 面：gate 前置、roundtrip 字节一致、输入 fill(0) | TC-T32-008（+ MAN 弹窗腿） | instrumentation + 真机走查 |
| AC-6 | schema 标记 + 无条件重建（tag 缺失/不符 → delete+ensure） | TC-T32-009 | JVM/instrumentation |
| AC-7 | 首帧后 fast 扫描（InteractionManager 挂载点） | — | 评审 |
| AC-8 | R1/R5 grep 干净；日志无信号细节 | B.5 原文命令 | exit 1（干净） |
| AC-9 | tsc 双形态 + modules gate 三绿；jest 回归 25/25 | — | 命令+退出码 |
| AC-10 | release APK 装机：签名/bundle 自检通过、L0、roundtrip OK | TC-T32-006 release 腿 | 真机 |
| AC-11 | D2(a)：Magisk 机 Root 信号命中 → L4 → import/sign 拒绝 + 录屏（MAN-004 关闭）；D2(b)：TC-T32-007 证据 + MAN-004 挂起记录 | TC-SA-006 子集 + MAN-004 | 真机走查（B.4-5） |
| AC-12 | iOS 撰写 + XCTest（TC-SI-009 + fixtures）+ MAC-HANDOFF [~] | TC-SI-009/004 fixtures | 代码评审 + Mac 侧 |
| AC-13 | 零未批准依赖（diff 仅 project(':security-android') + podspec 依赖）；Handoff 清单；Part A 板行；B.3 git；评审零发现轮 | — | 评审 + git 记录 |

## 7. 测试编号登记

- 自动用例（新增，T3.2 feature 编号）：TC-T32-001（Root 解析器正负例，JVM）、TC-T32-002（Frida 解析器正负例，JVM）、TC-T32-003（parseTracerPid 0/非0/畸形，JVM）、TC-T32-004（L0/L4 映射 + 缓存 + assertSigningAllowed 抛/放 + seed 语义，JVM）、TC-T32-005（错误码集合 + bundle hash match/mismatch/not-evaluated，JVM）、TC-T32-006（干净机 L0 全清放行，instrumentation）、TC-T32-007（gate deny E2E = [P] 证据，instrumentation）、TC-T32-008（wrap/unwrap JS 面 + gate 前置，instrumentation）、TC-T32-009（schema tag 无条件重建，JVM + instrumentation）。
- 既有 spec 用例衔接：TC-SA-004（重打包负例，T3.2 只做签名正例 + fixture 负例；完整 CI 流水线 → roadmap）、TC-SA-005（bundle 篡改负例 fixture）、TC-SA-006（Magisk 命中子集）、TC-SI-009（KEK 生命周期）、TC-SI-004（越狱 collector fixtures）。
- 手工用例：MAN-021（新登记，干净机 L0 基线）；MAN-004（既有，Magisk 拒绝，按 D2）；MAN-002 保持开放（更宽生物识别走查）。

## 8. 红线与依赖

- R1：gate 前置「任何密钥操作前」；wrap/unwrap 明文不跨 JS 持久化（vault-core 走 native-internal）；TS 面 fill(0)。
- R5：向量 ID only、固定错误模板、模块零日志。
- R6：全部检测 [C]；fail-closed 耦合 [P] 仅真机验证后（AC-4/AC-11）；debug-only 注入口文档化。
- R7：web stub 恒抛 E_SEC_UNSUPPORTED_OPERATION（不改）。
- R4：零新增外部坐标——唯一构建面变化 = `implementation project(':security-android')` + `s.dependency 'SecurityIos'`（均为本地模块工程/pod）；config plugin 自写零依赖；D1 若改选 b 须先 R4 修宪。