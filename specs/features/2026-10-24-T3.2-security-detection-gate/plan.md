# T3.2 — security detection + fail-closed gate — plan

> Date：2026-10-24 | Status：owner 批准（exit_plan_mode approved；D1–D5 = Recommended 默认，两轮 ask_user_question 超时未获实时答复）
> Skill：.agents/skills/nexvault-t3-2-security-detection-gate/SKILL.md；Requirements：同目录 requirements.md

## 0. Owner rulings（2026-10-24）

- 两轮 ask_user_question（12:01 / 12:21，120s / 600s）超时 → D1–D5 按 Recommended 默认落盘（requirements §2）；本计划经 exit_plan_mode 批准。
- **GATE-MODEL-SWITCH**：本规划轮只提交三件套 + SKILL 副本（Task 0），随后 **STOP**；owner 切换到 V4.1-Flash 后开始 Task 1。实施轮开工前先复核 D1–D5（owner 未推翻即按默认执行）。
- GATE-R4-IOSSECURITYSUITE：按 D1 默认 = cancel 依赖意图 → 自研基础向量 + security-ios.md §2.3 同步修订（实施 Task 3 时落行）。

## 1. Goal & 成功判据

按 T3.2 skill 与 owner 指令实现六项检测信号 → risk score → E_VAULT_SECURITY_GATE fail-closed 耦合（vault-core importMnemonic/signDigest）+ JS 面（H-T3.1-1/2/7 implement）。演示子集 = 干净机 L0 放行 + Magisk 机拒绝两段对照；完整多信号链 → roadmap。

成功 = owner 门禁全绿：tsc 双形态 + modules gate 0、JVM parser 测试全绿、干净机 L0 + Magisk 拒绝演示（MAN + 录屏）、R1/R5 grep 干净、零未批准依赖、评审零发现轮；Part A 公告板 + B.3 git 收尾；三件套落盘 specs/features/2026-10-24-T3.2-security-detection-gate/。

## 2. Preflight 事实（已实证，2026-10-24）

- main = `3862902`（T3.2 skill 安装 commit）；分支 feature/T3.2-security-detection-gate 已建（Task 0）。
- autolinking 工程名 :vault-core / :security-android（T3.1 validation Task 0 已证）；vault-core 现无对 security-android 的依赖（本项加 project 依赖，无新坐标）。
- debug APK 不内嵌 bundle（unzip -l app-debug.apk 无 index.android.bundle，仅 assets/app.config）→ BundleSelfCheck 的 evaluated=false 路径成立；演示走 release APK（T1.x/T2.x 先例：46MB 级、内嵌 bundle）。
- minSdk = 24（T3.1 已解析）→ GET_SIGNING_CERTIFICATES 仅 API≥28，24–27 用 GET_SIGNATURES 降级。
- 本机无 Xcode（T3.1 同）→ iOS 撰写侧 + MAC-HANDOFF [~]。
- 干净真机 = Pixel 6a / Android 17；Magisk 机是否就位 = D2（实施时与 owner 确认）。
- JAVA_HOME=/home/superguo/tools/jdk-17（dev-environment §1）；adb 无线 transport（T3.1 先例）。
- apps/mobile/plugins/ 已有 withVaultCoreTests.js 先例；app.json plugins 数组注册（新插件排其后）。
- 既有 JVM 基线：security-android 12/12（AesGcmEnvelope 9 + PromptFailureGate 2 + SecurityErrorCodes 1）。
- apps/mobile/AGENTS.md：Expo v57 文档先取后写（记录 URL + 访问日期）；android/ 为生成目录（git 未跟踪）→ 原生构建定制一律走 config plugin（CNG）。

## 3. 裁定（D1–D5，Recommended 默认；requirements §2 全表）

| # | 裁定 |
|---|---|
| D1 | 自写 iOS 越狱基础向量（路径族 + dyld + sysctl），零新依赖；§2.3 修订 |
| D2 | Magisk 机就位 → 真实拒绝演示；否则 [P] 耦合在干净机经 debug-only 注入口验证 + MAN-004 挂起 |
| D3 | 自写 CNG config plugin 构建期注入期望哈希（string resource） |
| D4 | iOS 撰写 + XCTest + MAC-HANDOFF [~] |
| D5 | 演示 = MAN-021（L0）+ MAN-004（拒绝）两段录屏；其余场景 → T5.4 / roadmap |

## 4. 规划轮交付（本会话，先于代码；随后 STOP 换模型）

1. P1 建分支 feature/T3.2-security-detection-gate（已完成）。
2. P2 requirements.md（本目录；D1–D5 Q/A 表、B.7-2 Handoff re-read、FR-1..10、AC-1..13、§7 测试编号、§8 红线）。
3. P3 本 plan.md。
4. P4 SKILL.md 副本（cp .agents/skills/nexvault-t3-2-security-detection-gate/SKILL.md → 本目录，T1.x–T3.1 惯例）。
5. P5 validation.md（Task 0 记录：命令 + 观察结果 + 提交哈希）。
6. P6 commit → STOP：报告「规划轮完成，切换 V4.1-Flash 后说 continue T3.2」。

## 5. 范围与边界诚实

- 在范围内：Android 签名/bundle/Root/反调试/Frida 检测 + iOS 越狱/反调试/Frida（撰写侧）；L0/L4 子集聚合；vault-core 门控耦合；H-T3.1-1/2/7；config plugin；MAN-021/004；JVM + instrumentation + iOS XCTest（撰写侧）；门禁六项；评审循环；Part A + B.3 收尾。
- 明确除外（requirements §5 表）：完整多信号链 / .so 签名变体 / attestation 与 L1–L3/L5 / TRACEME 组合 / 重打包 CI 与 T5.4 视频 / UI 分级指示器 / iOS 真机验证 / SOFTWARE_DEBUG 策略。
- R6 标注：签名 [C] · bundle [C] · Root [C] · 越狱 [C] · 反调试 [C] · Frida [C] · **fail-closed 耦合 [P]（仅真机验证后，AC-4/AC-11）**；debug-only 注入口文档化、release 无操作。

## 6. 实施任务（V4.1-Flash；开工前复核 D1–D5）

### Task 1 — 检测核心（modules/security-android）

先 fetch 官方 API 文档并记录 URL + 访问日期（skill Step 0）：GET_SIGNING_CERTIFICATES / APK 签名、/proc/self/status TracerPid、Frida 特征、iOS ptrace/sysctl。

| 文件 | 作用 |
|---|---|
| SignatureSelfCheck.kt（新） | GET_SIGNING_CERTIFICATES（API≥28）/ GET_SIGNATURES（24–27）→ 首证书 SHA-256 vs 资源常量；轮换注记；[C]；.so 变体 → roadmap 注释 |
| BundleSelfCheck.kt（新） | assets/index.android.bundle SHA-256 vs 资源常量；缺失 → evaluated=false；[C] |
| RootDetector.kt（新） | su 路径 / Magisk 包名 / build.prop 属性；解析器纯函数（注入字符串） |
| FridaDetector.kt（新） | 端口 27042 + /proc/self/maps 特征串；解析器纯函数 |
| AntiDebugDetector.kt（新） | /proc/self/status TracerPid 纯解析；TRACEME → roadmap 注释 |
| DetectionAggregator.kt（新） | DetectionReport → evaluate() 纯函数（L0/L4）；SecurityState（内存 + debugApplyForTest() DEBUG 守卫）；SecurityGate.assertSigningAllowed()（≥L3 抛 E_SEC_GATE_DENIED） |
| SecurityExceptions.kt（改） | +E_SEC_GATE_DENIED / E_SEC_SIG_MISMATCH / E_SEC_BUNDLE_MISMATCH |
| SecurityAndroidModule.kt（改） | +runDetection / getCachedRiskLevel（同步）/ assertSigningAllowed / wrapKey / unwrapKey（先 gate 再 WrapKeyManager，currentActivity as? FragmentActivity） |
| WrapKeyManager.kt（改） | H-T3.1-7：ensureCurrentSchemaKey()（tag ≠ tee-biometric-v1 → 无条件 delete+ensure+写 tag；不「检测」旧键） |

### Task 2 — vault-core 耦合 + JS 面（H-T3.1-2）

| 文件 | 作用 |
|---|---|
| VaultCoreModule.kt（改） | importMnemonic 协程首行（decode 前）/ signDigest（requireDigest32/store 前）调 SecurityGate.assertSigningAllowed()；清零路径不变 |
| VaultExceptions.kt（改） | +SecurityGateException（E_VAULT_SECURITY_GATE，固定模板） |
| android/build.gradle（改） | implementation project(':security-android')（跨模块门控耦合；无新坐标） |
| index.ts（改） | 导出 E_VAULT_SECURITY_GATE 常量 + JSDoc 调用序 gate → biometric → crypto |
| modules/security-android/index.ts + src/SecurityAndroidModule.ts（改） | §2.1/§2.2 全 TS 面（RiskLevel/DetectionReport、wrapKey/unwrapKey 输入 finally fill(0)、输出新拷贝）；web stub 不变 |

### Task 3 — iOS 对应物（撰写侧 [~]，按 D1/D4）

| 文件 | 作用 |
|---|---|
| JailbreakDetector.swift（新） | 路径族 + dyld 可疑库 + isDebuggerAttached（sysctl P_TRACED）+ denyDebuggerAttach（dlsym ptrace PT_DENY_ATTACH=31，绝不直接链接） |
| SecurityGate.swift（新） | L0/L4 子集 + E_SEC_GATE_DENIED |
| SecurityIosModule.swift（改） | 接管 JS 面（wrapKey/unwrapKey/runDetection/getCachedRiskLevel/assertSigningAllowed） |
| SecurityErrors.swift（改） | +E_SEC_GATE_DENIED |
| VaultCoreModule.swift / VaultCoreService.swift（改） | importMnemonic/signDigest 前置门控 |
| VaultCore.podspec（改） | s.dependency 'SecurityIos' |
| Tests/（改） | TC-SI-009 完整 KEK 生命周期 + 越狱 collector fixtures + gate deny fixture |
| specs/security-ios.md §2.3（改） | IOSSecuritySuite → 自研向量（D1 落行） |
| MAC-HANDOFF.md（改） | 文件清单、Mac 构建/测试命令、状态 [~] |

### Task 4 — App 侧（CNG 合规）

| 文件 | 作用 |
|---|---|
| apps/mobile/plugins/withSecurityHashes.js（新）+ app.json plugins（改） | 构建期注入期望哈希（string resource）；先 ./gradlew tasks --all \| grep -i bundle 核实任务名；不可行 → D3(b) 两段脚本（validation 记录） |
| apps/mobile/src/app/_layout.tsx（改） | 首帧后 InteractionManager.runAfterInteractions → runDetection('fast')（fire-and-forget，R5 无日志） |
| apps/mobile/src/store/security.ts（新） | Zustand {riskLevel, report} |
| apps/mobile/src/app/(tabs)/settings.tsx（改） | 诚实读数：risk 级别 + 向量 ID + 「检测=[C]，门控=[P]」标注 |

### Task 5 — 测试

- JVM（注入 fixture，不削弱断言）：RootDetectorTest、FridaDetectorTest、AntiDebugDetectorTest、DetectionAggregatorTest、BundleSelfCheckTest、SecurityErrorCodesTest 扩充（= TC-T32-001..005）。
- Instrumentation（干净 Pixel 6a）：DetectionDeviceTest（TC-T32-006）、GateDenyDeviceTest（TC-T32-007 = [P] 证据）、WrapKeyJsFaceDeviceTest（TC-T32-008）、SchemaRebuildDeviceTest（TC-T32-009）；T3.1 既有套件回归。
- iOS XCTest 撰写（Mac 执行）。

### Task 6 — 门禁与演示

```bash
pnpm --filter mobile exec tsc --noEmit                       # form-1
pnpm -r exec tsc --noEmit                                   # form-2
pnpm --filter mobile run typecheck:modules                  # modules gate
cd apps/mobile/android && JAVA_HOME=/home/superguo/tools/jdk-17 ./gradlew :security-android:testDebugUnitTest --rerun
# R1 grep（B.5 原文，apps/mobile + packages）；R5 grep（Log./println/print(/NSLog，模块内排除 Test）；git diff 依赖面审计
```

- 干净机：DetectionDeviceTest + GateDenyDeviceTest OK；release APK（assembleRelease）装机 → 签名/bundle 自检通过、L0、import→derive→sign roundtrip OK。
- D2(a)：Magisk 真机 → Root 信号 → L4 → 拒绝 + screenrecord 录屏（MAN-004 关闭）；D2(b)：GateDenyDeviceTest 真机证据 + MAN-004 挂起（handoff）。
- MAN-021 登记 + 走查（干净机 L0：Settings 读数 + 同机 instrumentation，录屏）。
- jest 回归 25/25。

### Task 7 — 评审循环 + 收尾

- review-fix-loop skill（持久 reviewer）至零发现轮；发现逐条入 validation。
- pending-manual-test-cases.md：MAN-021 新行、MAN-004 状态更新（不删行）。
- Handoff 清单：完整多信号链 → roadmap；.so 签名变体 → roadmap；attestation → T5.x；iOS Mac 验证 → 设备到位后；MAN-004（若 D2(b)）→ 设备到位后；UI 分级指示器 → T4.x。
- AGENTS.md Part A 关闭行（证据绝对路径）+ B.3 git（--no-ff 合并、push、分支删除，owner 显式口令）。

### Task 8 — 风险降级预案

| 风险 | 触发 | 降级 |
|---|---|---|
| config plugin 任务图不可行 | ./gradlew tasks 核实失败 | D3(b) 两段构建脚本 + validation 记录（不静默） |
| Magisk 机未就位 | 实施时 owner 确认 | D2(b) 口径：TC-T32-007 真机证据 + MAN-004 挂起 handoff |
| 评审 BLOCKER/MAJOR | 评审轮发现 | review-fix 纪律修复后重跑对应门禁（B.6-5 不削弱生产代码） |
| 设备离线 | adb 不可达 | 转 D2(b) 口径 / 待设备 handoff |