# T3.2 — validation（security detection + fail-closed gate）

> Feature：`specs/features/2026-10-24-T3.2-security-detection-gate` | Date：2026-10-24
> 证据纪律：AGENTS.md B.4（命令原文 + 观察结果 + 构件路径）；`[x]` 双侧齐备，`[~]` 已实现未验证。
> 分支：`feature/T3.2-security-detection-gate`（规划轮提交 `c90b418`；main `3862902`）
> 状态：**规划轮完成 → STOP（GATE-MODEL-SWITCH）**；实施 = V4.1-Flash 会话，Tasks 1–8。

## Task 0 — planning round（preflight + 三件套 + commit）

Status：`[x]`

```bash
git status --short                          # 干净（无输出）
git switch -c feature/T3.2-security-detection-gate   # Switched to a new branch 'feature/T3.2-security-detection-gate'
mkdir -p specs/features/2026-10-24-T3.2-security-detection-gate
cp .agents/skills/nexvault-t3-2-security-detection-gate/SKILL.md specs/features/2026-10-24-T3.2-security-detection-gate/SKILL.md
# write requirements.md / plan.md / validation.md（本文件）
git add specs/features/2026-10-24-T3.2-security-detection-gate
git commit -m 'docs(T3.2): planning round - requirements/plan/validation + SKILL copy (GATE-MODEL-SWITCH pending)'
```

观察：
- main = `3862902`（chore: install agent skill (T3.2 security detection + fail-closed gate)）；分支切出干净。
- Preflight 事实（2026-10-24 实证，详见 plan.md §2）：debug APK 无内嵌 bundle（`unzip -l apps/mobile/android/app/build/outputs/apk/debug/app-debug.apk` 仅 assets/app.config → BundleSelfCheck evaluated=false 路径成立）；autolinking 工程名 :vault-core/:security-android（T3.1 已证）；minSdk=24 → GET_SIGNING_CERTIFICATES 需 API≥28 降级分支；本机无 Xcode；JAVA_HOME=/home/superguo/tools/jdk-17；apps/mobile/android 为 git 未跟踪生成目录 → 构建定制走 config plugin（CNG）；plugins/ 已有 withVaultCoreTests.js 先例。
- Owner decisions：两轮 ask_user_question（2026-10-24 12:01 120s / 12:21 600s）超时 → D1–D5 按 Recommended 默认落盘（requirements §2）；计划经 exit_plan_mode **approved**（2026-10-24）。owner 可在 GATE-MODEL-SWITCH 前推翻任一裁定。
- 计划批准后未写任何生产代码（GATE-MODEL-SWITCH：owner 指令「Stop on implementation to allow me to change the model to V4.1-Flash」）。

## Task 1 — 检测核心（modules/security-android）

Status：`[x]`（2026-10-24 实施；JVM 门禁绿，设备侧见 Task 5/6）

D1–D5 复核：owner 本轮未推翻任何裁定（默认口径执行；D2 设备实况 = 仅干净 Pixel 6a 在线 → D2(b) 路径，见 Task 6）。

新增（`modules/security-android/android/src/main/java/expo/modules/securityandroid/`）：`HashHex.kt`（sha256/normalize/expected/match 纯函数）、`SignatureSelfCheck.kt`（GET_SIGNING_CERTIFICATES，API≥28；24–27 GET_SIGNATURES 降级；资源 `nexvault_expected_cert_sha256`）、`BundleSelfCheck.kt`（assets/index.android.bundle 流式 SHA-256；asset 缺失 → 未评估）、`RootDetector.kt`（su 路径 / Magisk 包名 / build.prop 属性，纯解析器）、`FridaDetector.kt`（端口 27042/27043 + maps 特征串）、`AntiDebugDetector.kt`（TracerPid 纯解析）、`DetectionAggregator.kt`（RiskLevel L0–L5、DetectionReport、DetectionPure L0/L4 子集、SecurityState 内存缓存 + debug-only 注入口、SecurityGate.assertSigningAllowed 自足 fast pass、`GateDeniedException`）。改动：`SecurityExceptions.kt`（+E_SEC_GATE_DENIED / E_SEC_SIG_MISMATCH / E_SEC_BUNDLE_MISMATCH）、`SecurityAndroidModule.kt`（+runDetection / getCachedRiskLevel 同步 / assertSigningAllowed / wrapKey / unwrapKey；ensureWrapKey 走 schema 重建）、`WrapKeyManager.kt`（H-T3.1-7：`ensureCurrentSchemaKey()`，SharedPreferences `wrap_key_schema_tag=tee-biometric-v1`，不符即无条件 delete+ensure；blob 格式不变）。

spec 修订：`specs/security-android.md` §2.2 DetectionReport 增 `signatureEvaluated`/`bundleEvaluated`/`riskLevel`；§3 增 T3.2 子集口径注（L0/L4 only，≥L3 拒绝，L1/L2/L3/L5 → T5.x）。

```bash
cd apps/mobile/android && JAVA_HOME=/home/superguo/tools/jdk-17 ./gradlew \
  :security-android:testDebugUnitTest :vault-core:testDebugUnitTest --rerun --console=plain
# BUILD SUCCESSFUL in 8s
```

观察（XML 计数）：`:security-android` **28 tests / 0 failures**（T3.1 既有 12 + 新增 16：RootDetectorTest 4、FridaDetectorTest 2、AntiDebugDetectorTest 2、DetectionAggregatorTest 4、HashHexTest 4）；`:vault-core` **37 tests / 0 failures**（T1.x 基线，零回归）。

边界（如实）：`SignatureSelfCheck`/`BundleSelfCheck` 的设备读取（PackageManager/AssetManager）不可 JVM 化，纯语义由 `HashHexTest` 覆盖，读取路径由 Task 5 设备用例断言（签名 evaluated=true 且 match=true；debug 包 bundle evaluated=false）。

## Task 2 — vault-core 耦合 + JS 面（H-T3.1-2）

Status：`[x]`（tsc 双形态 + modules gate 绿；设备侧耦合证据见 Task 5）

改动：`modules/vault-core/android/src/main/java/expo/modules/vaultcore/VaultCoreModule.kt`（`importMnemonicGuarded`/`signDigestGuarded` = AsyncFunction 实际执行的同一代码；gate 在任何密钥操作前；import 路径 `finally bytes.fill(0)` 覆盖「拒绝早于解码」；拒绝翻译为 `SecurityGateException` = E_VAULT_SECURITY_GATE，不回显信号细节）；`VaultExceptions.kt`（+SecurityGateException 固定模板）；`android/build.gradle`（`implementation project(':security-android')`，本地工程依赖，零新外部坐标）；`modules/vault-core/index.ts`（导出 `SECURITY_GATE_CODE` + `isSecurityGateError()`，JSDoc 调用序 gate → biometric → crypto，四方法签名冻结）；`modules/security-android/index.ts` + `src/SecurityAndroidModule.ts`（spec §2.1/§2.2 全 TS 面：DetectionReport/RiskLevel 类型、runDetection、getCachedRiskLevel、assertSigningAllowed、wrapKey/unwrapKey 输入 `finally fill(0)`、输出新拷贝；web stub 不变）；`apps/mobile/tsconfig.json`（paths 增 expo/expo-secure-store，理由：app 的 tsc 程序现在会跟随 import 进入 `modules/security-android`，需按 app 的 node_modules 解析这两项——与 tsconfig.modules.json 同构）。

```bash
pnpm --filter mobile exec tsc --noEmit          # exit 0（form-1）
pnpm -r exec tsc --noEmit                       # exit 0（form-2；"No projects matched the filters" 为 B.6-1 已记录的根目录提示）
pnpm --filter mobile run typecheck:modules      # exit 0（modules gate）
```

调用序（R1）：gate →（wrap 路径的 BiometricPrompt）→ crypto；拒绝路径不触碰 handleStore/BIP-39 推导；`destroy` 不门控（恢复流恒可用）。

## Task 3 — iOS 对应物（撰写侧 `[~]`，按 D1/D4）

Status：`[~]`（2026-10-24 撰写完成；本机无 Xcode/Swift 工具链 → 未编译、未运行，禁止标 [P]）

改动（撰写侧）：`modules/security-ios/ios/JailbreakDetector.swift`（新：路径族 `/var/jb`、Cydia、sshd、MobileSubstrate、/etc/apt；dyld 可疑库 FridaGadget/systemhook/Ellekit；`sysctl(KERN_PROC)` P_TRACED；`dlsym(RTLD_DEFAULT,"ptrace")` PT_DENY_ATTACH=31——绝不直接链接；boottime 异常；**纯 fixture 可测的 `collectSignals(pathExists:loadedImages:bootTimeAnomaly:)`，只产出稳定向量 ID** `JB_PATH_*`/`JB_DYLD_*`/`JB_BOOTTIME`）；`SecurityGate.swift`（新：RiskLevel L0–L5、DetectionReport、DetectionPure L0/L4、SecurityState 内存缓存 + `#if DEBUG` 注入口、`SecurityGate.assertSigningAllowed()` 自足 fast pass、`SecurityGateDenied`=E_SEC_GATE_DENIED）；`SecurityErrors.swift`（+E_SEC_GATE_DENIED）；`SecurityIosModule.swift`（接管 JS 面：wrapKey/unwrapKey、runDetection、getCachedRiskLevel、assertSigningAllowed；gate → Keychain auth → crypto）；`src/index.ts`（ArrayBuffer 面 + 懒加载 native require + E_SEC_UNSUPPORTED_OPERATION 守卫）；`Tests/SecurityIosTests.swift`（+8 → 15 例：TC-SI-009 完整 KEK 生命周期〔test-only 坐标〕、越狱 collector fixtures 正负例、gate deny fixture）；`SecurityIos.podspec`（description 更新）。

vault-core iOS 耦合：`VaultCoreService.swift`（`init(signingGate:)`，importMnemonic/signDigest 前置门控；默认空闭包仅用于 Expo-free XCTest，bridge 恒注入）、`VaultCoreModule.swift`（`import SecurityIos`；`try SecurityGate.assertSigningAllowed()`；`catch is SecurityGateDenied` → `E_VAULT_SECURITY_GATE`）、`VaultErrors.swift`（+securityGate）、`VaultCore.podspec`（`s.dependency 'SecurityIos'`，pod 名与 security-ios podspec 一致）。

spec 修订（D1 落行）：`specs/security-ios.md` §1 范围/§2.1 T3.2 JS 面注/§2.3 **IOSSecuritySuite → 自研向量**（D1 2026-10-24；完整 Dopamine 对抗 → roadmap）；`specs/features/2026-10-22-T3.1-keystore-wrap-biometric/MAC-HANDOFF.md` 增 T3.2 文件清单 + **MS-6 runbook**（prebuild/pod install → MS-1 构建 → XCTest 15/15 → TC-SI-009/gate/dyld 点跑 → `nm -u` 断言无 ptrace 直接链接 → 真机 Face ID roundtrip/detection L0 → 记录机型/OS/xcresult）。

静态核验（可做）：`pnpm --filter mobile exec tsc -p tsconfig.modules.json` → exit 0（含 security-ios/security-android/vault-core 三个 TS 面）；无日志语句；`ptrace` 仅出现在 dlsym 字符串（未见直接链接）。

**未验证项（[~] 的边界，Mac 轮必做）**：`_dyld_image_count/_dyld_get_image_name` 的 MachO 桥接（`import MachO`）、`sysctl` kinfo_proc 桥接、`Record` `@Field` 初始化、跨 pod `import SecurityIos`、XCTest 15/15、真机 Face ID roundtrip 与 detection L0。iOS 无越狱设备 → 拒绝路径在 iOS 上不可产出真机证据 → **iOS 不标 [P]**（Handoff H-T3.2-3）。

## Task 4 — App 侧（期望哈希注入 + 首帧后 fast 扫描 + settings 读数）

Status：`[x]`（资源已实证写出；构建与设备证据见 Task 6）

改动：`apps/mobile/plugins/withSecurityHashes.js`（新，自写，**prebuild 时**把 `apps/mobile/security-hashes.json` 里的期望值写成 `android/app/src/main/res/values/nexvault_security_hashes.xml`；值非法/缺失 → `NOT_EVALUATED`，native 侧报「未评估」而非「通过」）；`apps/mobile/security-hashes.json`（新，tracked：`certSha256` + `bundleSha256` + `generatedFrom`/`updatedAt` 元数据）；`scripts/update-security-hashes.mjs`（新，两段构建回填：从已构建 APK 取 `keytool -printcert` 的证书 SHA-256 + `unzip` 出的 `assets/index.android.bundle` SHA-256，写回 JSON）；`app.json`（plugins 注册，排 withVaultCoreTests 后）；`apps/mobile/src/store/security.ts`（Zustand：status idle/running/ready/unsupported/error + riskLevel + report；web 显式 unsupported，绝不伪造 L0）；`apps/mobile/src/app/_layout.tsx`（首帧后 `InteractionManager.runAfterInteractions` → `runDetection('fast')`，fire-and-forget，无日志）；`apps/mobile/src/app/(tabs)/settings.tsx`（诚实读数：风险级别 + 签名/bundle 评估与匹配状态 + R6 标注；向量 ID 不上屏——TC-SA-006 对抗性静默）。

**注入方式变更（D3(b) 降级，记录在案，不静默）**：首版按 D3(a) 用 Gradle 任务生成资源目录，连续三次被 Gradle 9 的任务依赖校验拒绝（`:app:mapDebugSourceSetPaths` → `:app:generateDebugResources` → `:app:extractDeepLinksDebug` 依次报 “uses this output … without declaring an explicit or implicit dependency”）。按 plan.md Task 4/Task 8 的明示回退，改为 **D3(b) 两段构建回填**：期望值来自已构建 APK（tracked JSON），plugin 在 prebuild 时落成 Android string resource——不再触碰 Gradle 任务图。自动回填流水线是 roadmap 阶段 2 既定条目（“bundle 哈希 CI 两段构建回填流水线”），本脚本即其本地形态。

```bash
cd apps/mobile && npx expo prebuild --platform android --clean --no-install
# exit 0；生成 android/app/src/main/res/values/nexvault_security_hashes.xml：
#   cert = fac61745dc0903786fb9ede62a962b399f7348f0bb6f899b8332667591033b9c（本机 debug.keystore 证书 SHA-256，两 variant 同签名）
#   bundle = NOT_EVALUATED（debug 包不内嵌 bundle；release 由两段回填脚本写入）
```

风险与护栏：JSON 与产物不一致 → 自检 mismatch → gate fail-closed 拒绝签名（不是静默放行）；因此 release 演示前必须重跑 `node scripts/update-security-hashes.mjs <apk>` + prebuild + 重建（Task 6 门禁步骤）。

## Task 5 — 测试（JVM / instrumentation / XCTest 撰写）

Status：`[x]`（Android 双侧已跑；iOS XCTest 撰写见 Task 3 `[~]`）

### 5.1 JVM（TC-T32-001..005；注入 fixture，不削弱断言）

```bash
cd apps/mobile/android && JAVA_HOME=/home/superguo/tools/jdk-17 ./gradlew \
  :security-android:testDebugUnitTest :vault-core:testDebugUnitTest --rerun
# BUILD SUCCESSFUL in 8s
```

| 模块 | tests | failures | 组成 |
|---|---|---|---|
| :security-android | **28** | 0 | T3.1 既有 12（AesGcmEnvelope 9 + PromptFailureGate 2 + SecurityErrorCodes 1）+ 新增 16（RootDetectorTest 4、FridaDetectorTest 2、AntiDebugDetectorTest 2、DetectionAggregatorTest 4、HashHexTest 4） |
| :vault-core | **37** | 0 | T1.x 基线（Bip39 16 + Bip32 10 + Ecdsa 8 + HandleStore 3），零回归 |

XML 证据：`modules/*/android/build/test-results/testDebugUnitTest/TEST-*.xml`（关闭前归档到 `evidence/jvm-*.xml`）。

### 5.2 Instrumentation（Pixel 6a / Android 17；transcript：`evidence/device-tests-transcript.txt`）

```bash
adb -s 10.42.0.139:45791 shell am instrument -w -e class expo.modules.securityandroid.DetectionDeviceTest \
  expo.modules.securityandroid.test/androidx.test.runner.AndroidJUnitRunner      # OK (2 tests)
adb -s 10.42.0.139:45791 shell am instrument -w -e class expo.modules.vaultcore.SecurityGateCouplingDeviceTest \
  expo.modules.vaultcore.test/androidx.test.runner.AndroidJUnitRunner            # OK (1 test)
adb -s 10.42.0.139:45791 shell am instrument -w -e class expo.modules.securityandroid.WrapKeyManagerDeviceTest \
  expo.modules.securityandroid.test/androidx.test.runner.AndroidJUnitRunner      # OK (4 tests) = T3.1 回归
```

- **TC-T32-006**（干净机 L0）：真实 app context 上 runDetection('full') → 注入资源存在（getIdentifier != 0）、`signatureMatch=true`（debug 签名证书 = 注入常量）、`bundleHashMatch=null`（debug 包无内嵌 bundle，**显式断言未被当作通过**）、root/frida 空、debugger=false、riskLevel=L0、`assertSigningAllowed` 放行。
- **TC-T32-007**（[P] 耦合，D2(b) 口径）：app context 的 gate 在 L0 放行；seed L4（`ROOT_SU_BIN`）→ `SecurityGate` 抛 `E_SEC_GATE_DENIED`；vault-core `importMnemonicGuarded`/`signDigestGuarded` 抛 `E_VAULT_SECURITY_GATE`，且拒绝路径 **mnemonic 输入字节全清零**（R1）；`resetForTest()` 后签名恢复 65 字节（无粘滞锁死）。

**边界与修复记录（B.4 不静默）**：库模块的 androidTest APK 是 **self-targeted**（其 manifest `targetPackage` = 测试包本身，`aapt2 dump xmltree` 实证），因此测试进程不是 app 进程：(1) 首跑 `signatureMatch=null` —— 期望资源在 app 包内而测试上下文读不到 → 用 `createPackageContext(<applicationId>)` 取真实 app context；(2) 第二次 `NameNotFoundException` —— Android 11+ 包可见性 → 两个模块的 androidTest manifest 增 `<queries><package …/></queries>`；(3) 第三次 app context 下 `Bip39` 词表 `R.raw` id 属测试 APK 资源表 → mnemonic 路径用测试 context，gate/detection 断言用 app context。三轮失败均留档在 transcript，最终全绿。

### 5.3 JS 桥接边界（如实）

`runDetection` / `getCachedRiskLevel` / `assertSigningAllowed` / `wrapKey` / `unwrapKey` 的 **TS 面**由三处 tsc 门禁 + 代码评审覆盖；**JS 桥接调用本身未在设备上执行**（T1.2 先例：AsyncFunction 需 RN 宿主）。作用域口径：Android 侧生产行为由 `TC-T32-006/007` 以「AsyncFunction 实际执行的同一方法」真机验证；桥接端到端属 T4.1 的真实导入/签名流程（与 T1.3-H8 同源）。

### 5.3 iOS XCTest（撰写侧）

`modules/security-ios/ios/Tests/SecurityIosTests.swift` 新增 8 例（合计 15）：TC-SI-009 完整 KEK 生命周期（test-only 坐标）、越狱 collector fixtures 正负例、gate deny fixture。**本机无 Xcode 未运行** → 状态 `[~]`，Mac 轮必跑（MAC-HANDOFF §5 MS-6）。

## Task 6 — 门禁与演示

Status：`[x]`（静态/自动化门禁全绿；release 演示 + MAN-021 取证完成）

### 6.1 自动化门禁

| 门禁 | 命令 | 结果 |
|---|---|---|
| tsc form-1 | `pnpm --filter mobile exec tsc --noEmit` | exit 0 |
| tsc form-2 | `pnpm -r exec tsc --noEmit` | exit 0（根 filter 提示同 B.6-1） |
| modules gate | `pnpm --filter mobile run typecheck:modules` | exit 0 |
| jest 回归 | `pnpm --filter mobile exec jest` | 3 suites / **25 tests** 全绿（71.7s） |
| JVM | `:security-android:testDebugUnitTest :vault-core:testDebugUnitTest --rerun` | BUILD SUCCESSFUL；**28/28 + 37/37** |
| 设备 L0 | `DetectionDeviceTest`（app context） | **OK (2 tests)** |
| 设备耦合 | `SecurityGateCouplingDeviceTest` | **OK (1 test)**（E_SEC_GATE_DENIED + E_VAULT_SECURITY_GATE + 输入清零 + 复位放行） |
| T3.1 回归 | `WrapKeyManagerDeviceTest` | **OK (4 tests)** |
| R1 | B.5 原文 grep（apps/mobile + packages） | exit 1（干净）——`evidence/r1-grep.txt` |
| R5 | B.5 原文 console grep + 模块源文件 Log./println/print(/NSLog（排除 Test） | 均 exit 1（干净）——`evidence/r5-grep.txt`、`evidence/r5-module-logs.txt` |
| 依赖面 | `git status/diff apps/mobile/package.json pnpm-lock.yaml` | 零改动（唯一构建面变化 = `implementation project(':security-android')` + `s.dependency 'SecurityIos'`，均为本地模块） |

### 6.2 Release 构建与两段哈希回填（D3(b)）

- 首次 `:app:assembleRelease`（全 ABI）：Gradle build daemon 崩溃（`Gradle build daemon disappeared unexpectedly`，系统内存 9/15 GB 已用、6 个 java 进程）→ 停止 daemon、`-PreactNativeArchitectures=arm64-v8a --max-workers=2` 重跑（演示设备为 arm64；构建范围注记于证据）。
- **Stage 1**（`/tmp/t32-release2.log`，arm64）：`:app:assembleRelease` **BUILD SUCCESSFUL in 5m 48s**；`app-release.apk` 内嵌 `assets/index.android.bundle`（4,139,416 B 条目）。
- 回填：`node scripts/update-security-hashes.mjs apps/mobile/android/app/build/outputs/apk/release/app-release.apk` → `certSha256=fac61745…3b9c`、`bundleSha256=7b64b3b5…dbfe`（`apksigner verify --print-certs` + `unzip` 取 bundle；keytool 无 v1 签名的解析失败已记录并改用 apksigner）。
- **Stage 2**：`expo prebuild --clean` 写入资源（`nexvault_expected_bundle_sha256 = 7b64b3b5…`）→ `:app:assembleRelease -PreactNativeArchitectures=arm64-v8a --max-workers=1 --no-parallel` **BUILD SUCCESSFUL in 8m 29s**（第 2 次 daemon 崩溃后按 `-Dkotlin.daemon.jvmargs=-Xmx1280m` 收紧内存重跑成功）。
- 产物：`app-release.apk` **46,957,479 B**，sha256 `dc707008fe41dce1aec849c3d618ce16353d2a77d7e1cd5d16e99197ca0ae957`（最终构建，含自检异常加固 + 检测快照原子化；`evidence/release-apk-sha256.txt`）；`aapt2 dump resources` 确认两枚注入字符串值。
- **真机（Pixel 6a / Android 17）release 装机**：`adb install -r` → Success；`DetectionDeviceTest` 对 release app context **OK (2 tests)**（签名 match + **bundle evaluated+match**）；`SecurityGateCouplingDeviceTest` **OK (1 test)**。
- **MAN-021 走查**：`nexvault://settings` 深链 → Settings 显示 **L0** + `Signature check: match · Bundle check: match` + R6 标注；截图/UI dump/14s 录屏归档 `evidence/man021-*`（agent 走查、owner 未在场，录屏可复核）。

### 6.3 R6 标注表（禁止拔高）

| 机制 | 级别 | 证据/边界 |
|---|---|---|
| native 签名校验（GET_SIGNING_CERTIFICATES + 常量比对） | **[C]** | TC-T32-006 真机 evaluated+match；重打包负例与 .so 自解析 → roadmap/T5.4 |
| bundle 哈希自检 | **[C]** | 评估路径 + release 两段回填（Task 6.2）；CI 自动回填流水线 → roadmap 阶段 2 |
| Root 检测（su 路径 / Magisk 包名 / root props） | **[C]** | JVM fixtures（TC-T32-001）+ 干净机无命中；Magisk+Shamiko 真机 → MAN-004（H-T3.2-2） |
| Frida 检测（端口 27042/27043 + maps 特征串） | **[C]** | JVM fixtures（TC-T32-002） |
| 反调试（TracerPid） | **[C]** | JVM fixtures（TC-T32-003）+ 干净机 false |
| iOS 越狱/反调试/Frida（自研向量，D1） | **[C]** | 撰写侧 `[~]`；无越狱真机 → iOS 不标 [P]（H-T3.2-3） |
| **fail-closed 耦合**（gate → E_VAULT_SECURITY_GATE，importMnemonic/signDigest 前置） | **[P]**（真机验证后生效） | TC-T32-007 真机 OK：seed L4 → 拒绝 + 清零 + 复位放行；信号由文档化 debug-only 注入口注入（`SecurityState.debugApplyForTest`，`BuildConfig.DEBUG` 守卫、release 无操作、JS 不可达）——真实 Root 设备对照仍待 MAN-004 |
| debug-only 注入口 | 文档化 test seam（非生产面） | KDoc + BuildConfig 守卫；review checklist 项 |

## Task 7 — 评审循环 + 收尾

Status：`[~]`（**评审轮阻塞**：reviewer 基础设施 4 次尝试超时无输出，见 §7.2；未合并/未 push）

### 7.1 Handoff 清单（B.7-1；下一条目 T3.3 规划轮必须逐条裁定）

| # | 事项 | 递延理由 | 建议处置 | 关联 |
|---|---|---|---|---|
| H-T3.2-1 | 完整多信号链（mounts/mountinfo 交叉、PID namespace、Shamiko/SUSFS 对抗、inline-hook 序言自检、TRACEME + task stat、iOS Dopamine hidejb） | plan 明示的 demo 子集范围 | roadmap 阶段 2 #1（4.5–6 人日） | AC-1 / AC-11 |
| H-T3.2-2 | Magisk+Zygisk 真机 MAN-004 对照拒绝演示与录屏 | 设备未就位（在线仅干净 Pixel 6a；D2(b)） | 设备到位后补录；耦合自动侧已真机验证（TC-T32-007） | MAN-004 / AC-11 |
| H-T3.2-3 | iOS 全量验证（MAC-HANDOFF §5 MS-6：构建 → XCTest 15/15 → 真机 Face ID roundtrip + detection L0） | 本机无 Xcode/合格 iPhone；撰写侧 [~] | 设备到位后执行；iOS 不标 [P] | AC-12 |
| H-T3.2-4 | bundle 哈希 CI 两段回填流水线 + `.so` 内嵌签名自解析（不经 PackageManager） | 本地两段脚本已是 MVP；CI 化与 .so 变体属阶段 2 | roadmap 阶段 2 #1/#3 | AC-1 / AC-10 |
| H-T3.2-5 | Play Integrity / Key Attestation / App Attest 与 L1–L3/L5 分级矩阵 | attestation 属 T5.x | T5.x（关联 H-T3.1-4） | AC-3 / R6 |
| H-T3.2-6 | UI 安全分级指示器打磨（当前仅诚实读数：级别 + 评估状态） | 属 UI 打磨条目 | T4.x | AC-7 |
| H-T3.2-7 | iOS `denyDebuggerAttach()` 的 App 调用点 | 需 T4.x 启动 wiring 决策 | T4.x | AC-12 |
| H-T3.2-8 | 旧 wrap blob 重包裹在真实流程中的接入（T3.2 只做 key schema 无条件重建；blob 格式未变） | 真实 wrap 流程属 T4.1 | T4.1 | AC-6 / H-T3.1-7 |

关闭备注：H-T3.1-7 已由本条目实现（schema tag + 无条件重建）；H-T3.1-8 保持已关闭（MAN-020）；H-T3.1-1/2 已实现（iOS 撰写 `[~]` / Android JS 面 + 同名 gate，设备用例 TC-T32-007/008 覆盖 Android 侧）。

### 7.2 Review-fix loop（**未完成 → 阻塞**）

2026-10-24 尝试 4 次独立评审，均未产出任何 finding 结论（reviewer 子代理运行 20–60 分钟无输出，随后被中断；最后一次为「内联代码 + 无工具」的 5–10 分钟限制评审，同样 7 分钟无输出）：

| # | reviewer | 形式 | 结果 |
|---|---|---|---|
| 1 | `09876d6d` | 全量范围 `3862902..bd4db7a`，可读全仓 | ~60 min 无输出 → 中断 |
| 2 | `03112054` | 同上，限 ~30 工具调用 | ~25 min 无输出 → 中断 |
| 3 | `da82c174` | 仅 6 个文件、≤6 工具调用 | ~6 min 无输出 → 中断 |
| 4 | `9ad76493` | 内联关键代码、禁止工具 | ~7 min 无输出 → 中断 |

**裁定**：这是 reviewer 基础设施（子代理模型执行）不可用的环境阻塞，不是代码或发现问题的结论。按 review-fix-loop 纪律**不得**以「开发者自审」冒充独立评审、**不得**记为 `No findings.`。因此：Task 7 状态 `[~]`（实现完成，评审轮待执行）；**未合并 `main`、未 push**（B.3 收尾在评审零发现轮之后）。恢复评审后的动作：同一分支重跑 reviewer → 修复 → 零发现轮 → 执行 §7.3 收尾清单。

开发者自查（**不作为独立评审**，仅记录已做核对）：三处 tsc 门禁、JVM 28+37、设备 3 套件、R1/R5 grep、依赖面、release 两段回填与 L0 演示均绿；`SecurityState.debugApplyForTest` 为 `BuildConfig.DEBUG` 守卫且 JS 不可达；gate 在 importMnemonic/signDigest 的首行执行且拒绝路径输入清零（设备用例断言）。

### 7.3 收尾清单（B.3 / Part A）

- [ ] AGENTS.md Part A 追加 T3.2 关闭行 + 阶段行更新（下一步 = T3.3 / 并行项）
- [ ] `--no-ff` 合并入 main + push + 删除 feature 分支
- [ ] validation 状态 `[x]`、Handoff 清单定稿

## Task 8 — 风险降级预案

见 plan.md Task 8：config plugin 任务图不可行 → D3(b) 两段脚本（记录）；Magisk 机未就位 → D2(b) 口径（TC-T32-007 证据 + MAN-004 挂起 handoff）；评审 BLOCKER/MAJOR → review-fix 纪律；设备离线 → D2(b)/待设备 handoff。
### 6.4 最终修订证据（最终构建，2026-10-24 15:11–15:18）

| 门禁 | 命令/动作 | 结果 |
|---|---|---|
| release 重建 | `:app:assembleRelease -PreactNativeArchitectures=arm64-v8a --max-workers=1 --no-parallel`（含 `SignatureSelfCheck`/`BundleSelfCheck` 异常加固提交后） | **BUILD SUCCESSFUL in 2m 11s**；APK 46,957,479 B，sha256 `dc707008…`（`evidence/release-apk-sha256.txt`） |
| 设备 L0（release app context） | `DetectionDeviceTest` | **OK (2 tests)** |
| 设备耦合（[P] 证据） | `SecurityGateCouplingDeviceTest` | **OK (1 test)** |
| T3.1 回归 | `WrapKeyManagerDeviceTest` | **OK (4 tests)** |
| MAN-021 复走 | 深链 `nexvault://settings` → Settings | **L0** + `Signature check: match · Bundle check: match`；`evidence/man021-settings-l0.{png,xml}`（png sha256 `3e4ab2cb…`）、`evidence/man021-l0.mp4`（sha256 `b4bec1bb…`） |