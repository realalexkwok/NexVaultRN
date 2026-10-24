# MAC-HANDOFF — T3.1 iOS half（撰写于 Linux，编译/验证在 Mac）

> 状态：**撰写完成，未编译**（本机无 Xcode）。T3.1 iOS 状态 `[~]`；`[P]` 标注生效条件 = 本文件 MS-4 真机走查完成。
>
> **T3.2 追加（2026-10-24）**：越狱/反调试/Frida 检测 + fail-closed gate + JS 面 + vault-core 耦合，**全部 `[~]`（从未编译）**；iOS 侧不产生 [P] 结论（T3.2 的 [P] 耦合仅 Android 真机验证，iOS 待 MS-6）。
> 依据：T1.5 MAC-HANDOFF 模式（`specs/features/2026-10-16-T1.5-vault-core-ios-port/MAC-HANDOFF.md`）、specs/security-ios.md §2.1/§2.2。

## 1. 本机已产出（文件清单）

```
modules/security-ios/
├── expo-module.config.json          # platforms ["apple"]；apple.modules ["SecurityIosModule"]；podspecPath ./ios/SecurityIos.podspec
├── ios/
│   ├── SecurityIos.podspec          # ExpoModulesCore 依赖；iOS 16.4；test_spec 'Tests'
│   ├── SecurityIosModule.swift      # T3.2 JS 面：wrapKey/unwrapKey/runDetection/getCachedRiskLevel/assertSigningAllowed（gate 先行）
│   ├── SecurityErrors.swift         # E_SEC_IOS_* 固定模板 + SecurityGateDenied（E_SEC_GATE_DENIED）
│   ├── SecurityGate.swift           # 【T3.2 新】RiskLevel / DetectionReport / DetectionPure / SecurityState / DetectionRun / SecurityGate
│   ├── JailbreakDetector.swift      # 【T3.2 新】自研向量（D1）：路径族 / dyld / sysctl TracerPid / KERN_BOOTTIME / PT_DENY_ATTACH(dlsym)
│   ├── AesGcmEnvelope.swift         # CryptoKit AES.GCM；version(1B)=1 ‖ IV(12B) ‖ ct ‖ tag(16B)；AAD 绑定
│   ├── KeychainKek.swift            # 读 Keychain KEK（service = "app:auth"，kSecAttrAccount/Generic = Data("nexvault.kek.v1")，值为 32B 的 hex；坐标可注入供测试）
│   ├── WrapKeyService.swift         # wrap/unwrap（读 KEK 时触发 Face ID）
│   └── Tests/SecurityIosTests.swift # 15 例：TC-SI-010/011 + TC-SI-009 + JailbreakDetector fixtures + gate 决策
└── src/index.ts                     # JS 面：ensureKek/deleteKek + wrapKey/unwrapKey/runDetection/getCachedRiskLevel/assertSigningAllowed

modules/vault-core/ios/（T3.2 变更）
├── VaultCoreService.swift           # 注入 signingGate()，importMnemonic/signDigest 在任何密钥操作前调用
├── VaultErrors.swift                # 新增 E_VAULT_SECURITY_GATE
├── VaultCoreModule.swift            # 注入 SecurityGate.assertSigningAllowed()；SecurityGateDenied → E_VAULT_SECURITY_GATE
└── VaultCore.podspec                # 新增 s.dependency 'SecurityIos'（两 pod 同为 iOS 16.4 楼层）
```

依赖已装：`expo-secure-store ~57.0.4`（tech-stack 既有批准行；`apps/mobile/package.json` + `pnpm-lock.yaml` 已更新），app.json plugins 已含 `expo-secure-store`（注入 `NSFaceIDUsageDescription`）。

## 2. 前置

- macOS + Xcode（T1.5 会话的 Xcode 26.5 / Swift 6.3.2 环境即可）、CocoaPods。
- 仓库工作树 = `feature/T3.1-keystore-wrap-biometric`（或已合并后的 main）。
- 首次 iOS 预构建（CNG，`ios/` 由 prebuild 生成，禁止手改）：

```bash
cd apps/mobile
npx expo prebuild --platform ios --clean
cd ios && pod install
```

> autolinking 会自动发现 `modules/security-ios`（`expo.autolinking.searchPaths`）。若 pod install 未出现 `SecurityIos`，检查 `expo-module.config.json` 的 `apple.podspecPath` 与目录名一致。

## 3. MS 步骤（逐条记录观察）

### MS-1 编译门禁

```bash
cd apps/mobile/ios
xcodebuild -workspace NexVault.xcworkspace -scheme NexVault -destination 'generic/platform=iOS' -configuration Debug -skipPackagePluginValidation -skipMacroValidation CODE_SIGNING_ALLOWED=NO build
```

> `-skipPackagePluginValidation -skipMacroValidation` 为强制项（T1.5 §6 追加：swift-secp256k1 的 SPM 插件校验会让无交互 xcodebuild 卡住）；`CODE_SIGNING_ALLOWED=NO` 用于无签名证书的 CLI 构建。
```

观察：`BUILD SUCCEEDED`。若失败，优先核对：
- `AesGcmEnvelope.swift` 的 `AES.GCM.Nonce/SealedBox` API 形状（CryptoKit，iOS 13+）；
- `KeychainKek.swift` 的 `kSecUseOperationPrompt` 键可用性（iOS 13+ 兼容）；
- Swift 5.9 并发/警告级别（podspec `swift_version = 5.9`）。

### MS-2 XCTest（codec；无需真机）

```bash
cd apps/mobile/ios
xcodebuild test -workspace NexVault.xcworkspace -scheme SecurityIos-Unit-Tests -destination 'platform=iOS Simulator,id=8A63EA78-9400-4324-A81F-58C2A499FAD9' -skipPackagePluginValidation -skipMacroValidation CODE_SIGNING_ALLOWED=NO
```

预期：**15 个用例**全绿（TC-SI-010 五例：布局 + roundtrip + 错误 AAD + 篡改 + 截断；空明文 parity 一例；TC-SI-011：KEK 坐标契约；**T3.2 新增 8 例**：TC-SI-009 生命周期、4 个 JailbreakDetector fixtures、3 个 gate fixtures）。测试须以 **Debug 配置**执行（`SecurityState.resetForTest/debugApplyForTest` 仅 `#if DEBUG` 编译）。记录 simulator/OS 版本与 `xcodebuild` 输出。

### MS-3 Keychain 坐标核对（可执行断言，TC-SI-011）

坐标不是假定值，而是从**已安装依赖**读出（`apps/mobile/node_modules/expo-secure-store/ios/SecureStoreModule.swift`，`query(with:options:requireAuthentication:)`）：

- `service = (keychainService ?? "app") + ":auth"`（本仓 JS 面不传 keychainService → **`app:auth`**）
- `kSecAttrAccount = kSecAttrGeneric = Data(key.utf8)`（→ `Data("nexvault.kek.v1".utf8)`）
- ACL = `SecAccessControlCreateWithFlags(WHEN_UNLOCKED_THIS_DEVICE_ONLY, .biometryCurrentSet)`

可执行断言已写入 `Tests/SecurityIosTests.swift`（TC-SI-011）：

```bash
cd apps/mobile/ios
xcodebuild test -workspace NexVault.xcworkspace -scheme SecurityIos-Unit-Tests -destination 'platform=iOS Simulator,id=8A63EA78-9400-4324-A81F-58C2A499FAD9' -skipPackagePluginValidation -skipMacroValidation CODE_SIGNING_ALLOWED=NO -only-testing:SecurityIos-Unit-Tests/SecurityIosTests/testKekCoordinatesMatchExpoSecureStore
```

该用例**先断言字面量契约**（`KeychainKek.service == "app:auth"`、`account == "nexvault.kek.v1"`），再在 **test-only 坐标**（`app:auth.t3-1-test`）写入同编码条目并经 `KeychainKek.loadKek(service:account:)` 读回比对——不触碰生产 KEK 条目，且服务名回归会先被字面量断言抓住（不再循环论证）。若 Mac 会话发现 `app.json` 配置了 `keychainService` 或依赖升级改了服务名，改 `KeychainKek.service`/`account` 并记录证据（唯一允许的适配点；blob 格式不变）。

### MS-4 真机走查（[P] 生效条件）

1. dev build 安装到合格 iPhone（T1.5 记录：iPad mini 4 / iOS 15.8.8 不合格，需 iOS 18+ 目标设备）。
2. 调用 `ensureKek()`（首次创建 KEK）→ 记录是否按预期弹出 Face ID/Touch ID（spec §2.2 的 `getItemAsync` 也会触发认证；若首次创建不弹窗属正常）。
3. `wrapKey` → `unwrapKey` roundtrip（T3.2 起经 JS 面；native 侧同一 `WrapKeyService` 路径）→ 断言明文回环一致、AAD 错配抛 `E_SEC_IOS_BLOB_CORRUPT`。
4. 系统设置新增一枚 Face ID/Touch ID → 下一次 `loadKek` 抛 `E_SEC_IOS_KEK_MISSING`（`kSecAccessControlBiometryCurrentSet` 语义；对应 MAN-021 候选，登记前先与 owner 确认编号）。
5. 记录：机型 / iOS 版本 / 每步观察 / 截图或日志 → 回写 `validation.md` Task 2 与 `specs/pending-manual-test-cases.md`。

### MS-5 回写

- `validation.md`：Task 2 状态 `[~] → [x]`（MS-1..MS-4 全绿时）；R6 标注：iOS Keychain ACL 的 `[P]` 自此生效（此前为 spec 级 `[P]` + 状态 `[~]`）。
- 若 MS-3 触发常量适配：记录 diff 与原因（条目布局以 expo-secure-store 实测为准）。
- 若有失败项：按 B.4-4 转 bug/Handoff，禁止删除记录。

## 4. 已知注意项（如实）

- **Zeroization**：Swift `Data`/`SymmetricKey` 为 COW 值类型，hex/字节中间量仅 best-effort 清零 → 记 `[C]`（R6，禁止拔高）。
- **ensureKek 的读路径**：`SecureStore.getItemAsync(..., requireAuthentication: true)` 会触发生物识别；spec §2.2 如此设计。若 MS-4 认为 UX 不可接受，T3.2 可把 ensureKek 完全 native 化（记录为选项，不在 T3.1 改 spec）。
- **条目值编码**：SecureStore 只存字符串，KEK 以 hex 存储（64 字符，远低于 2048B 上限）；Swift 解码后再构造 `SymmetricKey`。若 MS-3 发现 expo-secure-store 对值做了额外编码，以实测为准调整解码。
- **Pod 平台**：podspec iOS 16.4（ExpoModulesCore 下限）；app `ios.deploymentTarget = 18.0` 不变。

## 5. T3.2 追加（2026-10-24，撰写侧；全部 `[~]`）

### MS-6 编译 + 测试 + 静态检查（Mac 执行）

```bash
cd apps/mobile
npx expo prebuild --platform ios --clean
cd ios && pod install          # 必须重跑：VaultCore.podspec 新增 SecurityIos 依赖
```

1. 构建（复用 MS-1 命令）→ `BUILD SUCCEEDED`。重点核对：
   - `JailbreakDetector.swift` 的 `_dyld_image_count` / `_dyld_get_image_name`（Darwin；若报 cannot find in scope，在文件头补 `import MachO` 并记录）；
   - `kinfo_proc()` / `sysctl(&mib, ...)` 指针桥接（Swift 5.9）；`timeval` 的 KERN_BOOTTIME 读取；
   - `DetectionReportRecord: Record` + `@Field` 的合成 `init()`；
   - `SecurityGateDenied` / `SecurityGate` 跨 pod 可见性（`VaultCoreModule.swift` 的 `import SecurityIos`）。
2. 全量 XCTest（Debug）：`xcodebuild test -workspace NexVault.xcworkspace -scheme SecurityIos-Unit-Tests -destination 'platform=iOS Simulator,id=<UDID>' -skipPackagePluginValidation -skipMacroValidation CODE_SIGNING_ALLOWED=NO` → 预期 **15/15**。
3. 逐用例核对：`-only-testing:SecurityIos-Unit-Tests/SecurityIosTests/testKekLifecycleEnsureWrapUnwrapDeleteTCSI009`、`.../testGateDeniesWhenReportHasSignals`、`.../testDyldFixturesMapToStableIds`。
4. 反调试符号红线（TC-SI-005，静态）：`grep -rn 'ptrace' modules/security-ios modules/vault-core --include='*.swift'` 只允许 `JailbreakDetector.swift` 的 `dlsym(..., "ptrace")` 与注释；产出的二进制 `nm -u SecurityIos.framework/SecurityIos | grep -i ptrace` 应为空（出现 `_ptrace` 即红线）。
5. 真机（合格 iPhone，iOS 18+）：`ensureKek()` → `wrapKey`/`unwrapKey` 触发 Face ID/Touch ID（roundtrip 一致、错误 AAD → `E_SEC_IOS_BLOB_CORRUPT`）；`runDetection('full')` 返回 `riskLevel`（干净机预期 L0；`signatureEvaluated=false`/`bundleEvaluated=false`/`rootSignals=[]` 的口径见 spec §2.1）；`assertSigningAllowed()` 在干净机放行，`importMnemonic`/`signDigest` 正常。**iOS 侧 deny 路径（[P] 证据）当前造不出**（无越狱机/无 debug 注入口）→ iOS 不得宣称 [P]，与 T3.2 plan 一致（[P] 耦合仅 Android 真机验证）。
6. 记录：机型 / iOS 版本 / 每步观察 / xcresult 与日志 → 回写 T3.2 validation。

### 静态风险（本主机无法消除）

- `SecurityState.cachedRiskLevel()` 首次扫描前返回 L0（占位，非判定）：门控路径会先跑 fast 自足检测；UI 热路径读数不构成放行依据。
- `VaultCoreService` 的 `signingGate` 存在空闭包默认值（供 Expo-free XCTest）：生产路径由 `VaultCoreModule` 注入；新增调用点必须显式注入 gate。
- `denyDebuggerAttach()` 暂无 App 调用点（T3.2 只交付检测/门控；调用时机属 App 侧接线，建议 T4.x 启动早期，需 owner 决策以免影响调试）。
- `_dyld_*`/`sysctl` 常量属 Darwin 半私有面，审核风险按 spec §2.3 的 dlsym 纪律处理；若 Mac 构建需要 `import MachO`，按 MS-6.1 处理并记录。
