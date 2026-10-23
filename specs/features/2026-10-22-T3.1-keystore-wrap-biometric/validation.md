# T3.1 — validation（keystore/keychain wrap + biometric gate）

> Feature：`specs/features/2026-10-22-T3.1-keystore-wrap-biometric` | Date：2026-10-22
> 证据纪律：AGENTS.md B.4（命令原文 + 观察结果 + 构件路径）；`[x]` 双侧齐备，`[~]` 已实现未验证。
> 分支：`feature/T3.1-keystore-wrap-biometric`（规划轮提交 `dd992d6`；main `56e950f` 已 push）。

## Task 0 — preflight

Status：`[x]`

```bash
git switch -c feature/T3.1-keystore-wrap-biometric   # 从 main（56e950f）切出
git push origin main                                  # 91bcfcc..56e950f main -> main
grep -n minSdkVersion apps/mobile/android/app/build.gradle   # → 93: minSdkVersion rootProject.ext.minSdkVersion
```

观察：
- 解析 minSdkVersion = **24**（app.json 未设，Expo SDK 57 默认；`expo config --type introspect` android 段无覆盖项）。→ skill 的 <28 分支：GATE-R4-ANDROIDX-BIOMETRIC 开启，owner D1 裁定 androidx.biometric 1.1.0。
- autolinking 预检：`cd apps/mobile/android && ./gradlew -q projects` → `security-android (0.1.0)`、`vault-core (0.1.0)` 均被 expo autolinking 识别（searchPaths = `../../modules`）。
- 文档抓取（2026-10-22）：
  - `https://docs.expo.dev/versions/v57.0.0/sdk/securestore/` → HTTP 200，但正文为客户端渲染（关键词 requireAuthentication/keychainAccessible 0 命中）；**改用已安装包的权威 TS 面**：`apps/mobile/node_modules/expo-secure-store/src/SecureStore.ts`（`SecureStoreOptions { keychainService?, requireAuthentication?, keychainAccessible? }`；`WHEN_UNLOCKED_THIS_DEVICE_ONLY` 常量存在）。
  - `https://developer.android.com/reference/androidx/biometric/BiometricPrompt.PromptInfo.Builder` → HTTP 200、客户端渲染（setAllowedAuthenticators 等 0 命中）；**改用编译门禁 + AOSP 源码**：`androidx.biometric:biometric:1.1.0` 的 `setAllowedAuthenticators`/`setNegativeButtonText`/`CryptoObject` 均通过 `:security-android:compileDebugKotlin`；AOSP `AndroidKeyStoreKeyFactorySpi.java`（master，141 行）确认 KeyInfo 仅对私钥可用（见 Task 4 设备发现）。

## Task 1 — modules/security-android（新 local module）

Status：`[x]`（代码 + JVM 门禁；设备侧见 Task 4）

| 文件 | 作用 |
|---|---|
| `expo-module.config.json` | platforms [android, web]；模块类 `expo.modules.securityandroid.SecurityAndroidModule` |
| `android/build.gradle` | `androidx.biometric:biometric:1.1.0`（D1，R4 §6e）+ junit/androidx.test 既有行 |
| `AesGcmEnvelope.kt` | 纯 blob 编解码：version(1B)=1 ‖ IV(12B) ‖ ct ‖ tag(16B)；AAD 绑定；seal 清零入参；JVM 可测 |
| `SecurityExceptions.kt` | `SecurityErrorCodes`（7 个 E_SEC_*，spec §2.5）+ CodedException 子类（固定模板） |
| `WrapKeyManager.kt` | ensureKey（StrongBox→TEE→SOFTWARE_DEBUG 来源级）、deleteKey、encryptCipher/decryptCipher、wrap/unwrap（BiometricPrompt CryptoObject；3 次失败自动 cancel → fail-closed）、错误映射 |
| `SecurityAndroidModule.kt` | 最小 JS 面：getCapabilities / ensureWrapKey / deleteWrapKey（wrap/unwrap 为 native-internal，`// T3.2 wiring`） |
| `src/SecurityAndroidModule.ts` | TS 声明（无密钥语义标识符，R1） |
| `index.ts` | 公共 JS 面：typeof 守卫 + coded `E_SEC_UNSUPPORTED_OPERATION`（web 为 stub 时 fail-closed，镜像 vault-core） |
| `src/SecurityAndroidModule.web.ts` | web stub（R7；web 导出零该模块代码） |

编译修复记录（均为真实缺陷）：
1. `KeyGenerator.generateKey(KeyGenParameterSpec)` 不存在 → 改为 `generator.init(spec); generator.generateKey()`（Android 官方用法）。
2. 零参 `Coroutine { ... }` 与 `suspend (P0) -> R` 重载歧义 → 显式零参 lambda `Coroutine { -> ... }`（vault-core 的带参形式不受影响）。

```bash
cd apps/mobile/android && JAVA_HOME=/home/superguo/tools/jdk-17 ./gradlew :security-android:compileDebugKotlin
# BUILD SUCCESSFUL（修复后）
```

## Task 2 — modules/security-ios（撰写侧；Mac 验证待）

Status：`[~]`（本机无 Xcode；MAC-HANDOFF.md 已落盘）

- 安装（D2，tech-stack 既有批准行）：`cd apps/mobile && npx expo install expo-secure-store` → `expo-secure-store ~57.0.4`；`pnpm-lock.yaml` +12 行、`apps/mobile/package.json` +1 行；`expo install` 自动加入 config plugin（app.json `plugins` +1 行，保留最小 diff；plugin 注入 iOS `NSFaceIDUsageDescription`）。
- 文件：`ios/SecurityIos.podspec`（ExpoModulesCore，iOS 16.4）、`AesGcmEnvelope.swift`（CryptoKit AES.GCM，同一 blob 布局，含空明文 parity）、`KeychainKek.swift`（Keychain 条目读取：**service=`app:auth`**（= `(keychainService ?? "app") + ":auth"`）、**account/kSecAttrGeneric = `Data("nexvault.kek.v1".utf8)`**、值为 32B KEK 的十六进制；`kSecUseOperationPrompt` 触发 Face ID；坐标可注入供测试）、`WrapKeyService.swift`（native-internal wrap/unwrap）、`SecurityErrors.swift`（E_SEC_IOS_* 固定模板 + SecRandomCopyBytes 失败 fail-closed）、`SecurityIosModule.swift`（Expo 模块壳，T3.2 接管 JS 面）、`Tests/SecurityIosTests.swift`（**TC-SI-010** 5 例 + 空明文 parity + **TC-SI-011** 坐标契约；**TC-SI-009 完整生命周期属 D8 待裁定项**）、`src/index.ts`（ensureKek/deleteKek via expo-secure-store，spec §2.2 OPTS）。
- R1 记录：KEK 不是 k1（k1 明文永不过 JS）；ensureKek 在 JS 侧生成 KEK 字节是 spec §2.2 的既定调用面，Swift 侧读同一条目，wrap/unwrap 明文只在 native。
- 待 Mac：MS-1 编译、MS-2 XCTest、MS-3 Keychain 坐标核对、MS-4 真机 Face ID 走查（见 MAC-HANDOFF.md）。
- **静态验证（Linux 可做，2026-10-22）**：
  - `cd apps/mobile && npx expo config --type introspect` → `ios.infoPlist.NSFaceIDUsageDescription = "Allow $(PRODUCT_NAME) to access your Face ID biometric data."`（expo-secure-store config plugin 生效）；`bundleIdentifier = io.github.realalexkwok.nexvault`；`deploymentTarget = 18.0`；plugins 含 `expo-secure-store`。
  - `npx expo-modules-autolinking resolve --platform apple` → `packageName: 'security-ios'`，`podspecDir: <repo>/modules/security-ios/ios`；`search --platform apple` 同样发现 `security-ios` 与 `vault-core`（iOS autolinking 正确）。
  - 边界：本机无 ruby/cocoapods/swiftc（`which` 均未命中），podspec 语法与 Swift 编译只能在 Mac（MS-1）验证。

## Task 3 — JVM 门禁

Status：`[x]`

```bash
cd apps/mobile/android && JAVA_HOME=/home/superguo/tools/jdk-17 ./gradlew :security-android:testDebugUnitTest --rerun
```

观察：`BUILD SUCCESSFUL`；XML 计数：
- `TEST-expo.modules.securityandroid.AesGcmEnvelopeTest.xml`：tests=9 skipped=0 failures=0 errors=0（TC-SA-010 ×2、TC-SA-011 ×6：roundtrip / wrongAad / 篡改 / IV 唯一 / 密文不同 / 空明文 parity、TC-SA-012 ×1）
- `TEST-expo.modules.securityandroid.SecurityErrorCodesTest.xml`：tests=1 failures=0（TC-SA-013 JVM 半）
- `TEST-expo.modules.securityandroid.PromptFailureGateTest.xml`：tests=2 failures=0（TC-SA-014 的「第 3 次失败即 fail-closed」计数契约，JVM 半）

合计 **12/12**（AesGcmEnvelopeTest 9 含 N7 空明文 parity；SecurityErrorCodesTest 1；PromptFailureGateTest 2）。诚实边界：Keystore/BiometricPrompt 无法在 JVM 运行，设备侧见 Task 4。

## Task 4 — 真机

Status：`[~]`（自动侧 4/4 通过；MAN-019/020/018 走查待 owner 在场）

构件：`/home/superguo/Projects/NexVaultRN/modules/security-android/android/build/outputs/apk/androidTest/debug/security-android-debug-androidTest.apk`（自包含 test APK：package `expo.modules.securityandroid.test`，instrumentation targetPackage 同包 —— 与 T1.4 模式一致）。

```bash
SERIAL=$(adb devices | awk 'NR>1 && $2=="device" {print $1; exit}')   # 当前 10.42.0.139:33821
adb -s "$SERIAL" install -r -t modules/security-android/android/build/outputs/apk/androidTest/debug/security-android-debug-androidTest.apk
adb -s "$SERIAL" shell am instrument -w -e class expo.modules.securityandroid.WrapKeyManagerDeviceTest expo.modules.securityandroid.test/androidx.test.runner.AndroidJUnitRunner
```

观察（2026-10-22 19:47）：`Performing Streamed Install / Success`；`OK (4 tests)`（keyLifecycleIsHardwareBackedAndIdempotent / capabilitiesReportDeviceState / deletedKeyFailsClosedWithTypedError / errorTemplatesCarryNoMaterial）。

**复跑（review round 1 m2，2026-10-22 21:00，修订 `309cc3d`）**：修复后 test APK 重新构建（sha256 `5b7da988…`，见 Task 5）并重装（`Performing Streamed Install / Success`）→ `WrapKeyManagerDeviceTest` **OK (4 tests)**；转录归档 `evidence/device-install-r309cc3d.txt` + `evidence/device-lifecycle-r309cc3d.txt`（round 1）；**round 2 复核（修订 `a92e759`，21:19）**：`OK (4 tests)`，转录取 `evidence/device-install-final.txt` + `evidence/device-lifecycle-final.txt`。设备 = Pixel 6a（无线 adb，mDNS `adb-26111jEGR13989-MJzh3R._adb-tls-connect._tcp`；无线连接间歇掉线，脚本会自动重连重试）。

MAN-019/020 仍待 owner 在场（交互式指纹；用例已加 `-e t3_1_manual 1` 门控，默认 androidTest 集合会跳过而非挂起）。

### Task 4（续）— MAN-019 真机走查（进行中，2026-10-22 22:26–22:38）

1. **StrongBox 首跑失败（真实缺陷）**：`IllegalBlockSizeException ← KeyStoreException: Key user not authenticated`；citadel 日志 `UpdateOperation: KEY_USER_NOT_AUTHENTICATED`——StrongBox 密钥可生成，但每次带认证的使用都被 Titan M2 KeyMint 拒绝（StrongBox per-use 需要硬件 auth token）。**修复**：wrap key 改走 **TEE**（`TRUSTED_ENVIRONMENT`，仍硬件后端），记录为 spec §2.4 StrongBox-first 的偏差 + T5.x Handoff。
2. **TEE 复跑**：**STEP 1（wrap）成功**——提示出现、认证通过、blob 产出；日志 22:26:04 STEP 1 → 22:26:17 STEP 2。**STEP 2 的 per-use 提示确实再次创建**（BiometricService 22:27:09.87 新 AuthSession），说明 per-use 门禁链路正常；但操作者按了**已录入手指** → unwrap 成功、无 fail-closed 错误 → 测试在断言处失败（走查序列问题，非代码缺陷）。
3. 证据：`evidence/man019-run-c.txt` + `man019-logcat-c.txt`（TEE 首跑，STEP 1 通过）；`evidence/man019-run-d.txt` + `man019-logcat-d.txt`（复跑；STEP 2 被正确手指认证）；`man019-prompt-c.png` / `man019-prompt-d.png`（提示截图）。
4. **待完成**：STEP 2 需用**未录入的手指**连续 3 次（门禁自动取消）→ STEP 3/4/5；MAN-020（phase1 → owner 新增指纹 → phase2）与 MAN-018（TalkBack）同待 owner 在设备旁。
5. 门禁机制层面验证：每次 wrap/unwrap 均创建新的 AuthSession（per-use），提示/认证/错误链路正常。
6. **自动化 fail-closed 设备证据（无需人手）**：`BiometricCancelDeviceTest` 在提示出现 2.5s 后向焦点对话框注入 BACK，断言操作以 `E_SEC_BIOMETRIC_CANCELLED` 结束且被取消的 wrap 明文已清零。
   - 命令：`adb -t <tid> shell am instrument -w -e class expo.modules.securityandroid.BiometricCancelDeviceTest expo.modules.securityandroid.test/androidx.test.runner.AndroidJUnitRunner`
   - 观察（2026-10-22 23:20）：`OK (1 test)`（4.841s）；logcat `CANCEL FAIL-CLOSED OK code=E_SEC_BIOMETRIC_CANCELLED`。
   - 结论：取消路径的 fail-closed + 明文清零已在真机验证；**MAN-019 仍欠「3 次错误指纹」这一条**（需未录入手指），其余（提示、per-use、取消 fail-closed、清零）均已取证。
7. 走查辅助：`TestBiometricActivity` 顶部红色横幅在每次提示前显示该步动作（1/5…5/5），避免操作者需同时看聊天记录。曾尝试把提示语写入 BiometricPrompt subtitle（测试 hook），因设备在前台播放视频、提示被顺手按掉而无效，**hook 已回滚**（生产 prompt 恢复固定 subtitle）。
8. **2026-10-23 07:38–07:50 晨间复跑（3 次自动化 + 1 次带字幕）**：每次 step 1 通过、step 2 的 per-use 提示均出现，但操作者只按 1 次**已录入手指**（日志 `Succeeded(isStrongBiometric=true)`）→ unwrap 认证成功，**「3 次错误指纹」序列从未被执行**；截图 `man019-subtitle.png` 显示设备当时在前台播放 bilibili 视频（提示被顺手按掉）。
   - 结论：**MAN-019 仍缺这一条**，需要 owner 刻意用**未录入手指连续按 3 次**（每次应被拒）；其余链路（提示、per-use 新 AuthSession、取消 → fail-closed、明文清零）均已取证。
   - 若 owner 判定无法/不便执行该动作，可由 owner 书面同意改用「取消路径 + JVM 失败计数」替代（该替代证据已在手），届时 MAN-019 按 owner 裁定记录偏差。
9. **2026-10-23 08:41–08:49 追加三次复跑**（owner 选 A 后：常规指纹、置桌/另一只手、指关节三法）：每次 step 2 仍由**首触认证**（日志 `Succeeded(isStrongBiometric=true)`）→ unwrap 成功、断言失败。三次「错误指纹」拒绝序列始终未发生（本机仅录入 1 枚指纹，操作者未能以未录入手指/指关节产生拒绝事件）。
10. **owner 裁定（2026-10-23 09:23）：选项 B — MAN-019 的「3 次错误指纹」条款以书面偏差关闭**：替代证据 = 设备取消路径 fail-closed（`BiometricCancelDeviceTest`：`OK (1 test)`，`E_SEC_BIOMETRIC_CANCELLED` + 明文清零）+ JVM 三次失败计数契约（`PromptFailureGateTest`）。**MAN-019 状态 = 通过（偏差经 owner 批准）**；roundtrip / 错误 AAD 的设备半侧改由补充用例 `roundtripAndWrongAadWithRegisteredFinger`（3 次常规指纹提示）取证。
11. **补充设备用例通过（2026-10-23 09:28）**：`roundtripAndWrongAadWithRegisteredFinger` → `OK (1 test)`（9.757s）；logcat：`ROUNDTRIP OK`、`WRONG-AAD OK code=E_SEC_BLOB_CORRUPT`。即 **wrap、unwrap 字节级 roundtrip、错误 AAD → E_SEC_BLOB_CORRUPT** 均已在真机取证（3 次常规指纹）。证据：`evidence/supp-roundtrip-run.txt` + `supp-roundtrip-logcat.txt`。

### 设备发现（真实缺陷 → 修复，记录）

首跑 3/4 失败：`java.security.NoSuchAlgorithmException: no such algorithm: AES for provider AndroidKeyStore`（`KeyFactory.getInstance(key.algorithm, "AndroidKeyStore")`，WrapKeyManager.describe）。AOSP `AndroidKeyStoreKeyFactorySpi.java` 证实 keystore2 的 KeyInfo 只对私钥开放，AES 密钥的硬件归属无法事后查询。

修复（测试驱动）：安全级别改为「生成路径来源」——StrongBox 尝试成功 → STRONGBOX；TEE 回退或进程重启后重开 → TRUSTED_ENVIRONMENT，这是**未验证下限（unverified floor）而非 attestation**：软件后端的真实设备会被标为 TEE（D9 待裁定，T5.x attestation 验证）；模拟器 → SOFTWARE_DEBUG（非 debug 构建直接抛 E_SEC_SOFTWARE_KEYSTORE）。级别只缓存在进程内、不落盘（R5 落盘白名单仅地址与 SecureStore/Keychain 密文）。修复后 4/4 通过。

### MAN-020（生物特征变更失效）

- **phase 1（2026-10-23 09:28）**：`ManualEnrollmentInvalidationTest#phase1PrepareKeyForEnrollmentWalk` → `OK (1 test)`；logcat `PHASE 1 OK level=TRUSTED_ENVIRONMENT — now enroll a NEW fingerprint in Settings, then run phase2`。键已创建（TEE），`existingSecurityLevel()` 非空断言通过。
- **待办**：owner 在设置中新增 1 枚指纹 → 运行 phase2（期望 `E_SEC_KEY_INVALIDATED` + delete/ensure 恢复）。

### MAN-018（TalkBack）

状态：待 owner 在场（需人耳确认「恰好一次」播报）。

**预置 runbook（可在 owner 就绪时直接执行）**：
1. 设备当前安装的 T2.2 应用 = `io.github.realalexkwok.nexvault`（release APK，源码 `ff80939`）。
2. 开启 TalkBack（可逆）：
   ```bash
   adb shell settings put secure enabled_accessibility_services com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService
   adb shell settings put secure accessibility_enabled 1
   ```
3. 启动应用并进入 Home 地址输入（T2.2 走查路径：`am start -n io.github.realalexkwok.nexvault/.MainActivity`，必要时用 deep link `nexvault://home`）。
4. 输入非法地址 `0x123` → 失焦/提交（`adb shell input text 0x123`，随后 `input keyevent 66`）；owner 人耳确认 `Invalid address` 播报**恰好一次**（无重复、无静默）。
5. 恢复：`adb shell settings put secure accessibility_enabled 0`（并按需清空 `enabled_accessibility_services`）。

**当前状态（2026-10-23 09:28）**：已创建 MAN-020 的 wrap key（TEE）；已通知 owner 在设置中新增指纹，设备侧 `dumpsys fingerprint` 仍为 `count:1`（尚未新增）——新增完成后运行 phase2。

## R6 标注（T3.1；三级标注禁止拔高）

| 机制 | 标注 | 依据 / 生效条件 |
|---|---|---|
| Android Keystore AES-256-GCM wrap key（StrongBox→TEE 来源）+ per-use BiometricPrompt（CryptoObject，3 次失败 fail-closed） | **[P]**（生效条件：MAN-019/020 走查通过） | 验收设备 Pixel 6a 自动侧：密钥生成/级别/删除幂等 4/4；提示与 3 错 fail-closed 需 owner 真人指纹（MAN-019/020 待走查）。**未走查前不得对外宣称已验证** |
| SOFTWARE_DEBUG（模拟器/调试构建）路径 | **[C]** | 仅模拟器/debug 可得；非 debug 构建直接抛 `E_SEC_SOFTWARE_KEYSTORE`，永不静默降级、永不标 [P] |
| iOS Keychain KEK（.biometryCurrentSet ACL + WHEN_UNLOCKED_THIS_DEVICE_ONLY） | **[P]**（生效条件：MS-4 真机 + MS-2 XCTest 在 Mac 通过） | 本机无 Xcode，当前状态 `[~]`；实现依据 spec §2.2 与 expo-secure-store 57.0.4 源码坐标 |
| wrap blob 完整性（AES-GCM tag/AAD 绑定，`E_SEC_BLOB_CORRUPT` 零明文） | **[P]**（生效条件：MAN-019 第 4 步通过） | JVM 已证静态路径（AAD 篡改/ct/tag 翻转/截断/空明文全负例）；设备侧待 MAN-019 第 4 步复验 |
| 清零：wrap 入参（含认证失败路径）/ AAD 副本 / unwrap 输出（调用方） | **[C]** | JVM 断言成功路径清零；认证失败路径由 MAN-019 第 5 步断言；ART/Hermes GC 副本不可控（如实标注） |
| iOS Swift `Data`/`SymmetricKey` COW 零化 | **[C]** | 值类型复制不可控；best-effort 清零 |
| iOS JS 面 KEK 物化（ensureKek 读取 hex 字符串回 JS，不可清零） | **[C]**（+ T3.2 Handoff：改为 native 存在性检查/创建） | spec §2.2 的既定调用面；k1 明文永不过 JS（R1 边界不变） |
| 提示挂起保护（state saved/销毁预检 + 180s 超时 → E_SEC_BIOMETRIC_CANCELLED） | **[C]** | 防挂死，不构成认证边界 |

## Task 5 — 门禁

Status：`[x]`（自动侧；doctor 的两条告警为既有 SDK patch 建议，见行末）

| 门禁 | 命令 | 观察 |
|---|---|---|
| tsc form-1 | `pnpm --filter mobile exec tsc --noEmit` | exit 0 |
| tsc form-2 | `pnpm -r exec tsc --noEmit` | exit 0（首行 "No projects matched the filters …" 为根目录无工程，B.6-1 已证不影响） |
| R1 grep | 宪法原文命令（apps/mobile + packages + 新模块 `src`，`*.ts/*.tsx`） | exit 1（干净） |
| R5 grep | `grep -rnE 'Log\.|println|print\(|NSLog' modules/security-android modules/security-ios --include='*.kt' --include='*.swift' | grep -v Test` | exit 1（干净；build/ 二进制噪声用 `--include` 过滤） |
| 零未批准依赖 | `git diff --stat -- apps/mobile/package.json pnpm-lock.yaml apps/mobile/app.json` | package.json **+1**（`expo-secure-store ~57.0.4`，D2 既有批准行）；lockfile **+12**；app.json **+1**（`"expo-secure-store"` plugin，已还原 `npx expo install` 的无关格式重排，保留最小 diff）；gradle 侧仅 `+androidx.biometric:1.1.0`（D1） |
| jest 回归 | `pnpm --filter mobile exec jest` | **3 suites / 25 tests** 全通过（1.5s） |
| web export | `npx expo export --platform web --output-dir dist-web` | exit 0；`Exported: dist-web` |
| R7 | `grep -rE 'signDigest|importMnemonic|vault-core' dist-web/_expo/static/js/` → **0 行**；`grep -rE 'E_SEC_|nexvault\.wrap|SecurityAndroid|security-android'` → **0 行** | 干净（新模块未进 web bundle） |
| expo-doctor | `npx expo-doctor` | 20/21；唯一失败 = "Check that packages match versions required by installed Expo SDK"：`expo expected ~57.0.31 found 57.0.30`、`expo-router expected ~57.0.29 found 57.0.28`——**既有 patch 建议**（T2.2 时 21/21，SDK 元数据此后更新），升级属 R4 修宪，本 feature 不做 |
| androidx 传递版本 | `:security-android:dependencies`（debugCompileClasspath） | biometric 1.1.0 → `androidx.fragment:fragment:1.2.5` / `androidx.core:core:1.3.2`（模块内解析）；`:app:debugRuntimeClasspath` 实际解析 `androidx.core:core 1.18.0`（**无降级**） |
| OSV 复核 | `curl -X POST https://api.osv.dev/v1/query -d '{"package":{"name":"androidx.biometric:biometric","ecosystem":"Maven"},"version":"1.1.0"}'` | 响应 `{}`（2026-10-22；0 条已知漏洞） |
| modules TS 门禁（D7） | `pnpm --filter mobile exec tsc -p tsconfig.modules.json`（= `pnpm --filter mobile run typecheck:modules`） | **exit 0**（零类型错误；覆盖 modules/security-android 的 `index.ts` + `src/**` 与 modules/security-ios 的 `src/**`） |
| app 级构建（m8） | `cd apps/mobile/android && ./gradlew :app:assembleDebug` | **BUILD SUCCESSFUL in 4m 38s**（416 tasks）；构件 `apps/mobile/android/app/build/outputs/apk/debug/app-debug.apk` **237,537,841 B**，sha256 `982cf4ec7cdd6c8067668f12cf5f4d8c6a7f58648aa28345b7e00b405c1589d2`（20:38）——验证新模块 autolinking、manifest 合并与 expo-secure-store config plugin 在 app 级生效 |
| JVM 证据归档（m2） | 修复后重跑 `:security-android:testDebugUnitTest --rerun` | BUILD SUCCESSFUL；XML 归档 `evidence/jvm-TEST-expo.modules.securityandroid.{AesGcmEnvelopeTest,SecurityErrorCodesTest}-ra92e759.xml`（**10/10**，round 2 复核）；test APK sha256 `708da4df8fc941755bc1e0ab5ca29ca120a1fc2a22f44ca9d18400358e95a37b`（`evidence/test-apk-sha256-a92e759.txt`） |

`dist-web` 为构建产物（未入 git；已删除，保留命令与输出为证据）。

**post-review 复核（HEAD `0e1afd8`，2026-10-22 21:4x）**：`pnpm --filter mobile exec tsc --noEmit` **exit 0**；`pnpm -r exec tsc --noEmit` **exit 0**（根目录 filter 提示同 B.6-1）；R1 grep **exit 1（干净）**；R5 grep **exit 1（干净）**；jest **3 suites / 25 tests** 全通过。

**modules TS 探针（D7 证据，未落盘为门禁）**：临时 tsconfig（`extends ./tsconfig.json`；`include: ["../../modules/*/src/**/*.ts", "../../modules/security-android/index.ts"]`；`paths` 指向 `./node_modules/expo` 与 `./node_modules/expo-secure-store`）→ `npx tsc -p … --noEmit` **exit 0，零类型错误**。即 D7 option (b) 的模块 tsconfig 门禁可直接通过；待 owner 裁定后正式落盘（临时文件已删除，未入库）。

## Task 6 — review-fix 循环

Status：`[ ]`

## Review round 1（独立评审 2026-10-22；range 56e950f..607bfe7）

reviewer 子代理 `b75d6157-6d3b-4daa-90ac-edb389caa2cb`（持久 reviewer）：1 BLOCKER + 7 MAJOR + 8 MINOR + 2 NIT。

| # | 发现 | 处置 |
|---|---|---|
| B1 | KeychainKek 坐标与 expo-secure-store 57.0.4 实际条目不符（service=`app:auth`、account=`Data(key.utf8)`） | **已修**：坐标改为安装依赖实测值 + TC-SI-011 可执行坐标断言（XCTest）+ MAC-HANDOFF MS-3 改为可执行命令 |
| M1 | 设备凭据成功路径回传 null cryptoObject → 被误报 CANCEL（DECISION scope） | 待 owner 裁定（见 D6） |
| M2 | wrap 在认证失败路径不擦除入参明文 | **已修**：wrap 外层 try/finally 擦除 + MAN-019 第 5 步回归断言 |
| M3 | web stub 不抛 E_SEC_UNSUPPORTED_OPERATION（FR-4/R7） | **已修**：新增 `modules/security-android/index.ts` 公共面（typeof 守卫 + coded error，镜像 vault-core） |
| M4 | 新 TS 面不在任何 tsc 门禁内（DECISION tooling） | 待 owner 裁定（见 D7） |
| M5 | MAN-020 在无密钥时假通过 | **已修**：拆 phase1（建键+断言存在）/ phase2（先断言键曾存在再断言失效）+ MAN-020 行更新 |
| M6 | TC-SI-009 无测试代码/无可运行入口（DECISION scope） | 待 owner 裁定（见 D8） |
| M7 | 安全级别是生成路径来源而非验证，措辞过度（DECISION scope） | 即时修正已落（KDoc/validation 措辞 + T5.x attestation Handoff）；深层选项待 owner 裁定（见 D9） |
| m1 | 交互用例混入默认 androidTest（会挂起/假失败） | **已修**：`-e t3_1_manual 1` Assume 门控；文档记录按类/方法 am instrument |
| m2 | 无持久证据（evidence/ 空、JVM XML 早于最终源码） | **已修**：修复后重跑 JVM（9/9）+ 设备 4/4，XML/转录/APK sha 归档为 `evidence/*-r309cc3d.*`（提交 `ce97988`） |
| m3 | spec 漂移（§2.1/§2.5/iOS §2.1 与实现不一致） | **已修**：security-android §2.1 偏差注 + §2.5 补登 E_SEC_BLOB_CORRUPT；security-ios §2.1 偏差注 |
| m4 | androidx.biometric 无 OSV 记录 | **已修**：OSV 查询 0 条（Task 5 行） |
| m5 | MAC-HANDOFF 缺 `-skipPackagePluginValidation -skipMacroValidation` 等 | **已修**：MS-1/MS-2/MS-3 命令补齐 + pinned simulator |
| m6 | JS 侧 KEK 物化未标 [C]、缺 R6 表 | **已修**：R6 标注表 + T3.2 Handoff |
| m7 | 提示在 state saved 时可能永不返回 | **已修**：isFinishing/isDestroyed/isStateSaved 预检 + 180s withTimeout |
| m8 | 未做 app 级 Android 构建 | **已修**：`:app:assembleDebug` BUILD SUCCESSFUL（4m38s，237,537,841 B，sha256 `982cf4ec…`）；APK dex 内 `SecurityAndroidModule` 命中 33 次（app 级 autolinking 生效） |
| n1 | JVM 计数 2+4+1=7 与 tests=8 矛盾 | **已修**：TC-SA-011 ×5，2+5+1=8 |
| n2 | iOS 空明文 blob 比 Android 严格 | **已修**：`body.count >= tagSize` |

## Review round 2（2026-10-22 21:16；range 607bfe7..66df97b）

reviewer 判定：无新 BLOCKER；round-1 非递延项全部确认修复且证据复核通过（JVM XML 8+1 / test APK sha / 设备 4/4 / app APK 237,537,841 B + 33 次 dex 命中 / OSV {} / spec 偏差注 / Xcode flags）。新增 1 MAJOR + 4 MINOR + 2 NIT；M1/M4/M6/M7（D6–D9）仍开放。

| # | 发现 | 处置 |
|---|---|---|
| N2 (MAJOR) | MAN-019 登记命令缺 `-e t3_1_manual 1`（照做会被 Assume 跳过、无证据）；行内仍写 4 步 | **已修**：命令补参数、行改为 **5 步**并加第 5 步期望（取消的 wrap 亦清零明文） |
| N1 | TC-SI-011 循环论证（用 KeychainKek.service/account 播种又读回） | **已修**：先断言字面量 `"app:auth"` / `"nexvault.kek.v1"`，再用 **test-only 坐标** + 可注入 `loadKek(service:account:)` 读回；不再触碰生产 KEK 条目 |
| N3 | m7 修复的线程问题（isStateSaved 在调用线程、取消在定时器线程、定时器后仍创建 prompt、外层超时被吞） | **已修**：状态检查/prompt 创建全部移入 UI 线程并检查 settled/isActive；取消一律 post 主线程；改用 `withTimeoutOrNull`（只吞本次超时，外层取消照常传播） |
| N4 | R6 blob 行过度标 [P]；validation/security-android 仍写「绝不抬高/保守下限」 | **已修**：blob 行改为 [P]（生效条件 MAN-019 第 4 步）；两处措辞改为「未验证下限（unverified floor），非 attestation；软件后端真实设备可能误标（D9）」 |
| N5 | 文档未随修复更新（Task 2 KeychainKek 描述、TC-SI 编号、Task 1 缺 index.ts、FR-2/FR-3 旧签名、TC-SI-011 未登记、D6–D9 无处定义） | **已修**：全部更新 + requirements 增 D6–D9 表 + security-ios §5 登记 TC-SI-011 |
| N6 | §2.5 新行 3 单元格 | **已修**：并入含义单元格 |
| N7 | 空明文 blob 无测试 | **已修**：iOS `testEmptyPlaintextBlobIsValid` + Android JVM `emptyPlaintextBlobIsValid`（JVM 10 例） |

## Review rounds 3–5（bookkeeping → 零发现）

| 轮 | range | 发现 | 处置 |
|---|---|---|---|
| round 3 | 66df97b..48d95bc | 4 项 bookkeeping（NEW-A MAC-HANDOFF 测试计数/方法名；NEW-B JVM bullet tests=9/TC-SA-011 ×6；NEW-C 设备复跑时间；NEW-D AC-10 绑定 + 空明文 spec TC） | 全部修复（`48e4620`） |
| round 4 | 48d95bc..48e4620 | 1 项残留（MAC-HANDOFF §1 inventory 仍写旧坐标/旧测试编号） | 修复（`e6b7489`，已 push） |
| round 5 | 48e4620..e6b7489 | **No findings.**（reviewer 逐行核对 inventory/MS-2/MS-3 与代码/测试一致，无新问题） | — |

**评审结论（非递延范围）**：持久 reviewer `b75d6157-6d3b-4daa-90ac-edb389caa2cb` 对最终 HEAD `e6b7489` 报告 **No findings.**；round 1 的 BLOCKER/MAJOR/MINOR/NIT 与 round 2–4 的后续发现全部处置完毕，证据（JVM 10/10、设备 4/4、APK sha、OSV、app 构建）在修复后复跑并复核通过。

**仍开放（关闭门禁差项）**：
1. **D6–D9**（review M1/M4/M6/M7）——owner 三轮问询超时未答，记录于 requirements §2；实现按裁定收敛。
2. **真机走查**：MAN-019（5 步，含取消的 wrap 明文清零）、MAN-020（两阶段）、MAN-018（TalkBack 顺带）——需 owner 在场；`-e t3_1_manual 1` 门控已就位。
3. **Task 7 收尾**：validation 定稿 → AGENTS Part A 关闭行 → B.3 merge/push/分支清理（待 1、2 完成后执行）。

## Owner decisions D6–D9（2026-10-22 裁定「all recommended」）

| # | 裁定 | 落地 |
|---|---|---|
| D6 | 生物识别-only（移除设备凭据回退） | `WrapKeyManager`：`setUserAuthenticationParameters(0, AUTH_BIOMETRIC_STRONG)`；`PromptInfo` 仅 BIOMETRIC_STRONG + negative button 常驻；spec §2.4 偏差注 |
| D7 | 顶层模块 tsconfig 门禁 | `apps/mobile/tsconfig.modules.json` + `typecheck:modules` script；门禁 exit 0（Task 5 行） |
| D8 | TC-SI-009 随 T3.2 JS 面 | AC-10 仅绑 TC-SI-010/011；Handoff H-T3.1-1 |
| D9 | 接受生成路径来源模型 + 措辞 | KDoc/validation/spec 统一「unverified floor」；T5.x attestation Handoff |

**D6 迁移注**：D6 之前创建的旧 wrap key 仍带 `AUTH_DEVICE_CREDENTIAL` 认证类型，无法原地改参数（AES 无 KeyInfo 可查）——测试/演示均显式 delete + ensure 重建；T3.2 需在启动流程做一次「旧键检测→重建」迁移（Handoff H-T3.1-7）。

## Task 7 — 收尾

Status：`[ ]`（待 MAN-019/020/018 完成后执行）

**Handoff 清单（T3.1 → T3.2 / T5.x）**

| # | 事项 | 递延理由 | 建议处置 | 关联 |
|---|---|---|---|---|
| H-T3.1-1 | iOS JS 面 wrap/unwrap + TC-SI-009 完整 KEK 生命周期 | D8 裁定 c；本机无 Xcode，仅撰写 + 静态验证 | implement @ T3.2 | AC-10 / TC-SI-009 |
| H-T3.1-2 | Android JS 面 wrapKey/unwrapKey（ArrayBuffer）+ E_VAULT_SECURITY_GATE 耦合 | T3.1 native-internal 裁定 | implement @ T3.2 | FR-2 / FR-4 |
| H-T3.1-3 | StrongBox per-use AES 复测（Titan M2 拒绝 auth token） | 2026-10-22 设备发现；TEE 为当前路径 | T5.x | AC-2 / spec §2.1 |
| H-T3.1-4 | attestation 验证安全级别（UNVERIFIED 语义） | D9 裁定 a | T5.x | AC-2 / R6 |
| H-T3.1-5 | iOS 真机/Mac 验证（MS-1..MS-4） | 无 Xcode/合格 iPhone | 设备到位后 | AC-10 |
| H-T3.1-6 | MAN-018 Android 腿（TalkBack）+ MAN-019 未录入手指腿 + MAN-020 | 需 owner 在场 | 本轮完成或顺延 | MAN-018/019/020 |
| H-T3.1-7 | D6 前创建的旧 wrap key（DEVICE_CREDENTIAL auth 类型）迁移 | AES 无 KeyInfo、无法原地改参数 | T3.2 启动时检测并重建 | AC-2 |
