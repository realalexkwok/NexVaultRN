# T3.1 — Keystore/Keychain wrap + biometric gate（native-internal）— plan

> Date：2026-10-22 | Status：owner 批准（exit_plan_mode approved；D1–D5 = All default）
> Skill：.agents/skills/nexvault-t3-1-keystore-wrap-biometric/SKILL.md；Requirements：同目录 requirements.md

## 0. Owner rulings（2026-10-22）

- 两轮 ask_user_question 超时后 owner 回复 **All default** → D1–D5 全部按推荐裁定确认；本计划经 exit_plan_mode 批准。
- 实施轮在 C.1 Round B 复核后开工；Round B 超时 → 仅提交规划轮文档，不写代码、不装依赖。

## 1. Goal & 成功判据

按 T3.1 skill 与 owner 指令实现 wrap-key 子模块：Android Keystore AES-256-GCM + per-use BiometricPrompt；iOS KEK 经 expo-secure-store/Keychain + CryptoKit AES.GCM。wrap/unwrap native-internal；JS 面最小（capabilities / ensure / delete）。

成功 = owner 门禁全绿：tsc 双形态 0、JVM 门禁、真机 demo（弹窗 + 3 次错误指纹 fail-closed，MAN 登记）、R1/R5 grep、零未批准依赖；边界诚实（detection/attestation/E_VAULT_SECURITY_GATE/signDigest 门控 → T3.2/T5.x）；T2.2 Handoff 裁定；Part A 公告板 + B.3 git 收尾；三件套落盘 specs/features/2026-10-22-T3.1-keystore-wrap-biometric/。

## 2. Preflight 事实（已实证）

- SDK 57.0.0；解析 minSdkVersion = 24（app.json 未设，SDK 57 默认 rootProject.ext.minSdkVersion）→ < 28 → GATE-R4-ANDROIDX-BIOMETRIC 开启（D1 已裁定）。
- androidx.biometric maven-metadata（2026-10-22 读取）：stable 最新 = 1.1.0；1.2.0/1.4.0 仅 alpha。
- expo-secure-store：tech-stack/dependencies 既有批准行；SDK 57 解析 ~57.0.4（bundledNativeModules.json）；未安装。
- 真机在线：Pixel 6a `10.42.0.139:42585`（无线 adb）；指纹走查需 owner 在场。
- 门禁先例（T1.4 validation 行 392–393）：JVM `./gradlew :vault-core:testDebugUnitTest --rerun`；设备 `ANDROID_SERIAL=<serial> ./gradlew :vault-core:connectedDebugAndroidTest`。
- `main` 已 push（91bcfcc..56e950f）；分支 feature/T3.1-keystore-wrap-biometric @ 56e950f。
- autolinking：expo.autolinking.searchPaths = ["../../modules"] + expo-module.config.json（vault-core 无 package.json，新模块同构 → lockfile 零churn）。
- apps/mobile/AGENTS.md：Expo v57 文档先取后写（记录 URL + 访问日期）。

## 3. 裁定（owner All default）

| # | 裁定 |
|---|---|
| D1 | androidx.biometric:biometric:1.1.0（R4 修宪：tech-stack + dependencies §6e）；安装前 Round B 复核 |
| D2 | 本期撰写 iOS Swift + 安装 expo-secure-store ~57.0.4；Mac 验证走 T1.5 MAC-HANDOFF；状态 [~] |
| D3 | H10 Android 腿本期真机轮实施（MAN-018）；iOS 腿 replan → T4.x；H1–H9 维持 T2.2 记录 |
| D4 | 纳入 MAN-020（新增指纹 → E_SEC_KEY_INVALIDATED） |
| D5 | plans v1.4 追记：T3.1 = native-internal wrap/unwrap + 最小 JS 面；signDigest 门控措辞移 T3.2 |

## 4. 规划轮交付（先于代码；随后 STOP 换模型）

1. P1 push main + 建分支 feature/T3.1-keystore-wrap-biometric（已完成）。
2. P2 requirements.md（本目录；D1–D5 Q/A 表、B.7-2 Handoff re-read、FR-1..7、AC-1..11、§7 测试编号、§8 红线）。
3. P3 plans/3-day-implementation-plan.md 修订记录追加 v1.4。
4. P4 R4 落行：tech-stack.md（androidx.biometric 行 + expo-secure-store 安装注）+ doc/dependencies.md §6e。
5. P5 本 plan.md。
6. P6 Round B ask-user：D1–D5 复核 + AC/TC/MAN 编号确认 + 真机轮排期（owner 在场）。超时 → 仅提交规划文档并停止。
7. STOP：报告「规划轮完成，切换 V4.1-Flash 后说 continue T3.1」。

## 5. 范围与边界诚实

- 在范围内：Android getCapabilities（裁剪形）/ ensureWrapKey / deleteWrapKey；iOS ensureKek / deleteKek；native-internal wrap/unwrap；blob 格式；错误码；JVM + instrumentation + iOS 撰写侧测试；MAN-019/020 + MAN-018 Android 腿。
- 明确除外（逐项带指针）：JS wrapKey/unwrapKey ArrayBuffer 面 → T3.2（spec §2.1 偏差记录）；getCapabilities.hasGms/isEmulator、opts.attestationChallenge → T3.2/T5.x；runDetection/attestation/E_VAULT_SECURITY_GATE → T3.2/T5.x；signDigest 生物识别门控 → T3.2；StrongBox→TEE attestation 证明 → T5.x；SE P-256 v2 → roadmap；SOFTWARE_DEBUG 生产禁用策略 → security-testing。
- R6 标注：Android 硬件包裹 + per-use 生物识别 = [P] 仅在真机轮验证后生效；SOFTWARE_DEBUG 显式、永不 [P]；iOS Keychain ACL = [P]（spec T-SI-1）但状态 [~]，生效条件 = Mac/真机轮。

## 6. 实施任务（V4.1-Flash，Round B 之后）

### Task 0 — preflight
记录 minSdk=24；取文档并记录 URL+日期：docs.expo.dev/versions/v57.0.0/sdk/securestore/、developer.android.com KeyGenParameterSpec/BiometricPrompt、claude-dev-suite keystore-biometric quick-ref；`cd apps/mobile/android && ./gradlew -q projects` 应出现 :security-android。

### Task 1 — modules/security-android（新 local module）
create-expo-module --local 脚手架后对齐 vault-core 结构（无 package.json；expo-module.config.json platforms [apple, android, web]）。
- android/build.gradle：implementation androidx.biometric:biometric:1.1.0（Round B 后）；junit/androidx.test 行抄自 vault-core。
- WrapKeyManager.kt：alias `nexvault.wrap.v1`；flag 集见 requirements FR-1（API ≥30 setUserAuthenticationParameters(0, AUTH_BIOMETRIC_STRONG or AUTH_DEVICE_CREDENTIAL)；24–29 per-use 默认；StrongBox→TEE 重试；API≥35 unlockedDeviceRequired；无锁屏 → E_SEC_NO_LOCKSCREEN）；wrap/unwrap 绑定 BiometricPrompt.CryptoObject，finally Arrays.fill(0)；KeyPermanentlyInvalidatedException → E_SEC_KEY_INVALIDATED；UserNotAuthenticatedException → E_SEC_BIOMETRIC_*；deleteWrapKey 幂等；BiometricPrompt 经 appContext.currentActivity（FragmentActivity，主线程 executor），取消/负按钮/凭据回退/锁定全处理。
- AesGcmEnvelope.kt（纯 Kotlin，JVM 可测）：version(1B)=1 ‖ IV(12B SecureRandom) ‖ ct ‖ tag(16B)；AAD UTF-8（地址‖userId）；AEADBadTagException → E_SEC_BLOB_CORRUPT；入参 SecretKey（JVM 注入 SecretKeySpec）。
- SecurityAndroidModule.kt：AsyncFunction 仅 getCapabilities/ensureWrapKey/deleteWrapKey；wrap/unwrap 纯 Kotlin + // T3.2 wiring 注释。
- SecurityExceptions.kt：七个 E_SEC_* 固定模板（R5）。
- src/index.ts 最小 TS 面（无密钥语义标识符）；SecurityAndroidModule.web.ts stub 恒抛 E_SEC_UNSUPPORTED_OPERATION（R7）。

### Task 2 — modules/security-ios（仅撰写；Mac 验证）
- `npx expo install expo-secure-store`（~57.0.4；Round B 后）。
- SecurityIosModule.swift：ensureKek/deleteKek 经 expo-secure-store JS 面（§2.2 OPTS；32B 随机 KEK）；wrap/unwrap native-internal 用 CryptoKit AES.GCM（同一 blob + AAD），KEK 读同一 Keychain 条目（service/account 一致，keychainService 按 v57 文档核实）；Data 清零（CoW 记 [C]）。
- Tests/ XCTest 骨架 + 共享 parity 向量；MAC-HANDOFF.md（T1.5 模式：文件清单、Mac 构建/测试命令、TC-SI-009/010 步骤、状态 [~]）。

### Task 3 — JVM 测试（:security-android:testDebugUnitTest --rerun）
TC-SA-010 pack/unpack/version/截断负例；TC-SA-011 roundtrip 字节相等 + IV 唯一 + AAD 绑定 + 篡改负例（SecretKeySpec）；TC-SA-012 零化断言；TC-SA-013 错误模板白名单。边界诚实：Keystore/BiometricPrompt 路径仅真机。

### Task 4 — instrumentation + 真机 demo（owner 在场）
- androidTest：TestBiometricActivity（FragmentActivity）+ manifest；WrapKeyManagerDeviceTest：securityLevel ∈ {STRONGBOX, TRUSTED_ENVIRONMENT}、roundtrip、错误 AAD、删除后类型错误、ensure 幂等。
- MAN-019（门禁，B.4-5）：ANDROID_SERIAL=10.42.0.139:42585 ./gradlew :security-android:connectedDebugAndroidTest 交互测试——弹窗出现 → 3 次错误指纹（3× onAuthenticationFailed）→ 取消 → 断言 fail-closed（无成功、无明文、缓冲清零、下次重弹窗）→ 正确指纹 → 成功。证据：adb 转录、弹窗 screencap、XML → evidence/。
- MAN-020：新增指纹 → unwrap 抛 E_SEC_KEY_INVALIDATED → delete+re-ensure 记录。
- MAN-018 Android 腿：TalkBack 走查（设备上 T2.2 当前代码 APK，恰好一次播报）；H10 更新（iOS 腿 → T4.x）。
- MAN-019/020（+MAN-018 结果）登记 pending-manual-test-cases.md；MAN-002 保持开放。

### Task 5 — 门禁（命令+退出码入 validation.md）
1. tsc 双形态：pnpm --filter mobile exec tsc --noEmit 与 pnpm -r exec tsc --noEmit → 0。
2. JVM：cd apps/mobile/android && JAVA_HOME=/home/superguo/tools/jdk-17 ./gradlew :security-android:testDebugUnitTest --rerun → BUILD SUCCESSFUL + 计数；:vault-core:testDebugUnitTest 回归 37/37。
3. 真机：Task 4。
4. R1 grep（AGENTS 命令，apps/mobile+packages+新模块 TS）→ clean（exit 1）；R5 grep（Log.|println|print(|NSLog，除 Test）→ clean。
5. 零未批准依赖：git diff pnpm-lock.yaml + apps/mobile/package.json = 仅 expo-secure-store ~57.0.4；gradle diff = 仅 androidx.biometric 1.1.0 + 既有测试行。
6. 回归：jest 3 suites/25；npx expo-doctor；npx expo export --platform web --output-dir dist-web exit 0 + R7 grep（dist-web 无 wrap-key/security-android 符号）。

### Task 6 — review-fix 循环（review-fix-loop skill）
提交实现+证据；单一持久 reviewer 子代理审 main..HEAD（对照 T3.1 scope = skill + requirements.md）；逐轮修复；DECISION NEEDED → ask_user_question 升owner；至零发现轮。终报：commit hash、拒绝/递延项。

### Task 7 — 收尾（B.3 + Part A）
- validation.md 完整（逐任务证据、[P]/[C]/[S] 表、Handoff 清单：JS 面+门控耦合 → T3.2；attestation → T5.x；iOS 真机/Mac 轮 → 设备到位；SE P-256 → roadmap；SOFTWARE_DEBUG 策略 → security-testing）。
- AGENTS.md Part A：T3.1 关闭行 + 公告板更新（下一步 → T3.2）。
- Git：merge --no-ff → main，push，删本地+origin 分支；tag 视 owner 要求。

## 7. 边界情形与失败模式

取消 → E_SEC_BIOMETRIC_CANCELLED；5+ 失败 → E_SEC_BIOMETRIC_LOCKOUT；无锁屏 → E_SEC_NO_LOCKSCREEN（生成前）；StrongBox 缺失 → TEE 重试不静默；生物特征变更 → E_SEC_KEY_INVALIDATED（恢复流=重导入助记词，仅文档化）；AAD/篡改 → GCM 失败零明文；currentActivity null → 类型错误不崩溃；iOS CoW 清零注意项 [C]。

## 8. 验收标准（requirements §6 同表 AC-1..11）

## 9. 假设与风险

- 真机指纹腿需 owner 在场（排期检查点；实现暂停等待）。
- iOS [~] 直至 Mac 会话；T3.1 不主张 iOS [P]。
- Round B 复核 D1–D5；超时即停（沉默≠批准）。
- 56e950f 已随本次 push 上 origin。

## 10. 停点（owner 指令）

规划轮（P1–P6）→ STOP → owner 切换 V4.1-Flash → 「continue T3.1」→ Task 0–7。