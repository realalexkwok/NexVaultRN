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
- 文件：`ios/SecurityIos.podspec`（ExpoModulesCore，iOS 16.4）、`AesGcmEnvelope.swift`（CryptoKit AES.GCM，同一 blob 布局）、`KeychainKek.swift`（Keychain 条目读取：service=Bundle id、account=nexvault.kek.v1、值为 32B KEK 的十六进制；`kSecUseOperationPrompt` 触发 Face ID）、`WrapKeyService.swift`（native-internal wrap/unwrap）、`SecurityErrors.swift`（E_SEC_IOS_* 固定模板 + SecRandomCopyBytes 失败 fail-closed）、`SecurityIosModule.swift`（Expo 模块壳，T3.2 接管 JS 面）、`Tests/SecurityIosTests.swift`（TC-SI-009/010 骨架）、`src/index.ts`（ensureKek/deleteKek via expo-secure-store，spec §2.2 OPTS）。
- R1 记录：KEK 不是 k1（k1 明文永不过 JS）；ensureKek 在 JS 侧生成 KEK 字节是 spec §2.2 的既定调用面，Swift 侧读同一条目，wrap/unwrap 明文只在 native。
- 待 Mac：MS-1 编译、MS-2 XCTest、MS-3 Keychain 坐标核对、MS-4 真机 Face ID 走查（见 MAC-HANDOFF.md）。

## Task 3 — JVM 门禁

Status：`[x]`

```bash
cd apps/mobile/android && JAVA_HOME=/home/superguo/tools/jdk-17 ./gradlew :security-android:testDebugUnitTest --rerun
```

观察：`BUILD SUCCESSFUL`；XML 计数：
- `TEST-expo.modules.securityandroid.AesGcmEnvelopeTest.xml`：tests=8 skipped=0 failures=0 errors=0（TC-SA-010 ×2、TC-SA-011 ×4、TC-SA-012 ×1、AAD/篡改负例含于 TC-SA-011）
- `TEST-expo.modules.securityandroid.SecurityErrorCodesTest.xml`：tests=1 failures=0（TC-SA-013 JVM 半）

合计 **9/9**。诚实边界：Keystore/BiometricPrompt 无法在 JVM 运行，设备侧见 Task 4。

## Task 4 — 真机

Status：`[~]`（自动侧 4/4 通过；MAN-019/020/018 走查待 owner 在场）

构件：`/home/superguo/Projects/NexVaultRN/modules/security-android/android/build/outputs/apk/androidTest/debug/security-android-debug-androidTest.apk`（自包含 test APK：package `expo.modules.securityandroid.test`，instrumentation targetPackage 同包 —— 与 T1.4 模式一致）。

```bash
SERIAL=$(adb devices | awk 'NR>1 && $2=="device" {print $1; exit}')   # 当前 10.42.0.139:33821
adb -s "$SERIAL" install -r -t modules/security-android/android/build/outputs/apk/androidTest/debug/security-android-debug-androidTest.apk
adb -s "$SERIAL" shell am instrument -w -e class expo.modules.securityandroid.WrapKeyManagerDeviceTest expo.modules.securityandroid.test/androidx.test.runner.AndroidJUnitRunner
```

观察（2026-10-22 19:47）：`Performing Streamed Install / Success`；`OK (4 tests)`（keyLifecycleIsHardwareBackedAndIdempotent / capabilitiesReportDeviceState / deletedKeyFailsClosedWithTypedError / errorTemplatesCarryNoMaterial）。

### 设备发现（真实缺陷 → 修复，记录）

首跑 3/4 失败：`java.security.NoSuchAlgorithmException: no such algorithm: AES for provider AndroidKeyStore`（`KeyFactory.getInstance(key.algorithm, "AndroidKeyStore")`，WrapKeyManager.describe）。AOSP `AndroidKeyStoreKeyFactorySpi.java` 证实 keystore2 的 KeyInfo 只对私钥开放，AES 密钥的硬件归属无法事后查询。

修复（测试驱动）：安全级别改为「生成路径来源」——StrongBox 尝试成功 → STRONGBOX；TEE 回退或进程重启后重开 → TRUSTED_ENVIRONMENT（保守降级，绝不抬高，R6）；模拟器 → SOFTWARE_DEBUG（非 debug 构建直接抛 E_SEC_SOFTWARE_KEYSTORE）。级别只缓存在进程内、不落盘（R5 落盘白名单仅地址与 SecureStore/Keychain 密文）。修复后 4/4 通过。

### MAN-019 / MAN-020 / MAN-018

待 owner 在场走查（本轮暂停点）。走查完成后补本节。

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

`dist-web` 为构建产物（未入 git；已删除，保留命令与输出为证据）。

## Task 6 — review-fix 循环

Status：`[ ]`

## Task 7 — 收尾

Status：`[ ]`
