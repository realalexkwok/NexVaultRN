# T3.1 — Keystore/Keychain wrap + biometric gate — requirements

> Feature：T3.1（roadmap 阶段 1 Day 2 上午，plans/3-day-implementation-plan.md 行 61；AGENTS Part A「下一步 = T3.1 启动」）
> Date：2026-10-22 | Status：规划轮完成（owner 批准）
> 上位文件：AGENTS.md（B.2 / B.4 / B.7、R1 / R4 / R5 / R6 / R7）；specs/security-android.md §2.1/§2.4；specs/security-ios.md §2.1/§2.2；specs/vault-core.md §1（at-rest 包裹属 security-android/ios）
> Skill：.agents/skills/nexvault-t3-1-keystore-wrap-biometric/SKILL.md（owner 2026-10-22 安装，commit `56e950f`）

## 1. Goal

Android Keystore AES-256-GCM wrap key + per-use BiometricPrompt（CryptoObject 绑定）包裹密钥材料；iOS 侧 KEK 经 expo-secure-store/Keychain（生物识别 ACL）+ CryptoKit AES.GCM。**wrap/unwrap 为 native-internal API**（JS 面仅 capabilities / ensure / delete）——T3.1 skill 的 R1 裁定：spec §2.1 草案的 ArrayBuffer JS 面待 T3.2 与 vault-core 耦合时落地，本项在 requirements 记录该偏差，无需 R1 例外。

成功判据（owner 门禁）：tsc 双形态 0；JVM 门禁 BUILD SUCCESSFUL；真机 demo（弹窗 + 3 次错误指纹 fail-closed，MAN 登记）；R1/R5 grep 干净；零未批准依赖。边界诚实：detection / attestation / E_VAULT_SECURITY_GATE 属 T3.2/T5.x；T3.1 不门控 signDigest。

## 2. Owner decisions（Q/A 表）

| # | 问题 | 裁定 | 备注 |
|---|---|---|---|
| D1 | R4 gate：minSdk 预检 = 24 < 28 → androidx.biometric | **批准 androidx.biometric:biometric:1.1.0**（maven-metadata 最新 stable；1.2.0/1.4.0 仅 alpha，R4 排除） | 2026-10-22 两轮 ask_user_question 超时（owner 不在场）后 owner 回复 All default 确认全部推荐项；计划经 exit_plan_mode 批准（2026-10-22）；安装前 C.1 Round B 复核 |
| D2 | iOS 范围 | **本期撰写 Swift + 安装 expo-secure-store ~57.0.4**（tech-stack 既有批准行执行）；Mac 侧编译/验证走 T1.5 MAC-HANDOFF 模式；状态 [~] | Linux 无 Xcode；iOS [P] 标注生效条件 = Mac/真机轮 |
| D3 | T2.2 H10 裁定（B.7-2） | **Android 腿在 T3.1 真机轮顺带实施**（MAN-018 TalkBack 走查）；iOS 腿 replan → T4.x | 其余 H1–H9 维持 T2.2 记录（见 §3） |
| D4 | 附加真机走查 | **纳入 MAN-020**（新增指纹 → E_SEC_KEY_INVALIDATED） | 证明 setInvalidatedByBiometricEnrollment(true) |
| D5 | plans 行 61 措辞 | **追加 v1.4 追记**：T3.1 = native-internal wrap/unwrap + 最小 JS 面；原「锁屏后调 signDigest 弹生物识别」门禁措辞与 E_VAULT_SECURITY_GATE 耦合移入 T3.2 | plans 只增不改 |
| D6（review M1） | 设备凭据成功路径回传 null cryptoObject | **裁定 a（owner 2026-10-22「all recommended」）：生物识别-only** | key spec 与 prompt 均移除 DEVICE_CREDENTIAL；spec §2.4 偏差注已加；实现 `setUserAuthenticationParameters(0, AUTH_BIOMETRIC_STRONG)` + 提示仅 BIOMETRIC_STRONG（negative button 常驻） |
| D7（review M4） | 新 TS 面不在 tsc 门禁 | **裁定 b：顶层模块 tsconfig** | 新增 `apps/mobile/tsconfig.modules.json`（extends app config；include `modules/*/src` + `modules/security-android/index.ts`；paths expo/expo-secure-store）+ `package.json` script `typecheck:modules`；门禁命令 `pnpm --filter mobile exec tsc -p tsconfig.modules.json` → **exit 0** |
| D8（review M6） | TC-SI-009 无 harness | **裁定 c：随 T3.2 JS 面** | AC-10 仅绑定 TC-SI-010/011；TC-SI-009 入 Handoff（H-T3.1-1，T3.2） |
| D9（review M7） | 安全级别语义（来源 vs 验证） | **裁定 a：接受生成路径来源模型 + 措辞修正** | KDoc/validation/spec 统一为「未验证下限（unverified floor），非 attestation」；真实软件后端设备的误标风险记录为 T5.x attestation Handoff |

## 3. Handoff re-read（B.7-2；上一条目 T2.2 的 H1–H10 逐条裁定）

| Handoff | T2.2 状态 | 本规划轮裁定 | 落点 |
|---|---|---|---|
| H1 交易/签名工作流 | 待 | replan（不变） | T4.1 |
| H2 readVerified 主备交叉验证 | 待 | replan（不变） | T4.1 |
| H3 链 failover live-fire | 待 | replan（不变） | T4.x |
| H4 watch 地址持久化 | 待 | replan（不变） | 未来条目（R4 提案随行） |
| H5 行情/图表 | 待 | replan（不变） | T4.x |
| H6 adapter jest 单测 | 已关闭（T2.2 完成） | 确认关闭 | — |
| H7 WalletConnect | 待 | replan（不变） | v2 |
| H8 T2.1a H2/H3/H5/H6 | replan 记录 | replan（不变） | T4.x |
| H9 MAN-017 Web 走查 | 已关闭（2026-10-21） | 确认关闭 | — |
| H10 MAN-018 a11y 走查 | 待（Android 可执行；iOS 无设备） | **implement（Android 腿，本期真机轮）** + iOS 腿 replan | T3.1 真机轮 + T4.x |

## 4. 功能需求（FR）

- FR-1（Android wrap key 生命周期）：ensureWrapKey 生成 alias `nexvault.wrap.v1` 的 AES-256-GCM Keystore 密钥；flag 集按 security-android §2.4：setUserAuthenticationRequired(true)；**API ≥ 30** → setUserAuthenticationParameters(0, AUTH_BIOMETRIC_STRONG or AUTH_DEVICE_CREDENTIAL)（窗口 0，per-use），**API 24–29** → per-use 默认（不设 validity duration；androidx.biometric 兼容路径，实现期核对官方文档）；setInvalidatedByBiometricEnrollment(true)；setIsStrongBoxBacked(true) 且 StrongBoxUnavailableException → TEE 重试，绝不静默 SOFTWARE；setUnlockedDeviceRequired(true) 仅 API ≥ 35（28–34 官方明示 bug，默认关）；KeyInfo.isInsideSecureHardware() + securityLevel 校验；无锁屏 → E_SEC_NO_LOCKSCREEN（生成前失败）。
- FR-2（native-internal wrap/unwrap）：**交付 API = `suspend wrap(activity: FragmentActivity, plaintext, aad)` / `unwrap(activity, blob, aad)`**——manager 内部持有 CryptoObject 与 cipher，activity 由调用方注入（T3.1 为 instrumentation 宿主；T3.2 由 JS 面解析 currentActivity）。blob = version(1B)=1 ‖ IV(12B)（Keystore 生成）‖ ciphertext ‖ tag(16B)；AAD = 账户地址‖userId（UTF-8，仅公开材料）；GCM tag 失败 → E_SEC_BLOB_CORRUPT（零明文输出）；明文在成功与失败路径均清零（wrap 外层 finally；AAD 副本 finally）。**2026-10-22 交付偏差记录**（原措辞 `wrap(plain, aad, crypto)` 为 spec §2.4 示意签名）。
- FR-3（BiometricPrompt 门控）：每次 wrap/unwrap 需 CryptoObject 绑定的 BiometricPrompt；取消/负按钮/超时 → E_SEC_BIOMETRIC_CANCELLED；锁定 → E_SEC_BIOMETRIC_LOCKOUT；**activity 由调用方传入**（native-internal；T3.2 再解析 currentActivity）；finishing/destroyed/stateSaved 预检 → E_SEC_UNSUPPORTED_OPERATION，180s 超时 → E_SEC_BIOMETRIC_CANCELLED（不挂起）；KeyPermanentlyInvalidatedException → E_SEC_KEY_INVALIDATED；UserNotAuthenticatedException → E_SEC_BIOMETRIC_*。**2026-10-22 交付偏差记录**（原措辞的 currentActivity 解析属 T3.2 JS 面）。
- FR-4（最小 JS 面）：SecurityAndroidModule AsyncFunction 仅 getCapabilities()（裁剪形 { hasStrongBox, isDeviceSecure, securityLevel }；spec §2.1 全形 hasGms/isEmulator 与 opts.attestationChallenge 属 T3.2/T5.x，偏差记录）、ensureWrapKey()、deleteWrapKey()；wrap/unwrap 为纯 Kotlin 方法（// T3.2 wiring 注释）。web stub 恒抛 E_SEC_UNSUPPORTED_OPERATION（R7）。
- FR-5（iOS）：ensureKek/deleteKek 经 expo-secure-store（specs/security-ios.md §2.2 OPTS：requireAuthentication: true、WHEN_UNLOCKED_THIS_DEVICE_ONLY、synchronizable 关；随机 32B KEK，远低于 2048B 上限）；wrap/unwrap native-internal Swift 用 CryptoKit AES.GCM（同一 blob 格式 + AAD）；KEK 读取同一 Keychain 条目（service/account 与 expo-secure-store 一致，实现期按 v57 文档核实 keychainService）；Data 清零（CoW 注意项记 [C]）。
- FR-6（错误码）：E_SEC_NO_LOCKSCREEN / E_SEC_KEY_INVALIDATED / E_SEC_BIOMETRIC_CANCELLED / E_SEC_BIOMETRIC_LOCKOUT / E_SEC_BLOB_CORRUPT / E_SEC_SOFTWARE_KEYSTORE / E_SEC_UNSUPPORTED_OPERATION——固定模板、不回显密钥/AAD/密文片段（R5）。
- FR-7（测试）：JVM（blob 逻辑、零化、错误模板）+ instrumentation（设备级生成/roundtrip/AAD/删除/幂等）+ iOS XCTest 骨架与共享 parity 向量（Mac 执行）+ MAN-019/020 + MAN-018 Android 腿。

## 5. Out of scope（边界诚实，逐项带指针）

- JS wrapKey/unwrapKey ArrayBuffer 面 → T3.2（spec §2.1 偏差记录）
- getCapabilities.hasGms/isEmulator、ensureWrapKey(opts.attestationChallenge) → T3.2/T5.x
- runDetection / attestation / E_VAULT_SECURITY_GATE fail-closed 耦合 → T3.2/T5.x
- signDigest 生物识别门控（plans 行 61 原措辞）→ T3.2
- StrongBox→TEE attestation 证明、Key Attestation → T5.x
- SE P-256 wrap v2 → roadmap（security-ios §7-1）
- SOFTWARE_DEBUG 生产禁用策略 → security-testing / T3.2

## 6. 验收标准（AC → TC/MAN → 门禁）

| AC | 内容 | 绑定用例 | 验证 |
|---|---|---|---|
| AC-1 | minSdk 预检记录（=24）且 R4 门禁裁定落行（D1：tech-stack + dependencies §6e） | — | grep/评审 |
| AC-2 | flag 集密钥生成：securityLevel = **生成路径来源**（TEE 为未验证下限，非 attestation；D9）；模拟器 SOFTWARE_DEBUG 显式；API 分支正确（≥30 参数化 / 24–29 per-use）。**2026-10-22 设备发现**：Pixel 6a 的 StrongBox per-use 认证在 Titan M2 上不可用（KEY_USER_NOT_AUTHENTICATED），wrap key 走 TEE 路径（TRUSTED_ENVIRONMENT） | TC-SA-014 | 真机 instrumentation |
| AC-3 | blob 格式 + roundtrip 字节相等 + IV 唯一 + 错误 AAD/篡改 → E_SEC_BLOB_CORRUPT | TC-SA-010/011 | JVM + instrumentation |
| AC-4 | per-use：每次 unwrap 触发 BiometricPrompt；3 次错误指纹 fail-closed（无成功回调、无明文、缓冲清零、下次需重新弹窗） | MAN-019（+ TC-SA-014） | 真机走查（B.4-5） |
| AC-5 | 生物特征变更 → E_SEC_KEY_INVALIDATED | MAN-020 | 真机走查 |
| AC-6 | deleteWrapKey 幂等；删除后 unwrap 抛类型错误；ensure 幂等 | TC-SA-014 | instrumentation |
| AC-7 | 零化：wrap 入参封存后清零、unwrap 输出用后清零 | TC-SA-012 | JVM |
| AC-8 | R1/R5/R6/R7：grep 干净、web 导出无 wrap-key 符号、标注诚实 | TC-SA-013 + grep | CI 命令 + 评审 |
| AC-9 | 全部门禁绿：tsc 双形态、JVM、真机、零未批准依赖（lockfile 仅 +expo-secure-store ~57.0.4；gradle 仅 +androidx.biometric 1.1.0）、jest 回归、doctor、web export | — | 命令+退出码 |
| AC-10 | iOS 代码撰写 + MAC-HANDOFF.md；状态 [~]（Mac 验证前不标 [P]） | TC-SI-010/011（Mac 执行）；TC-SI-009 完整生命周期属 D8 待裁定 | 代码评审 + Mac 侧 |
| AC-11 | Handoff 裁定落表（§3）+ plans v1.4 + AGENTS Part A 关闭行 + B.3 git | — | 文档核对 |

## 7. 测试编号登记

- 自动用例（新增）：TC-SA-010（pack/unpack roundtrip + version 字节 + 截断负例，JVM）、TC-SA-011（roundtrip 字节相等 + IV 唯一 + AAD 绑定 + 篡改 tag/ct + 空明文 parity 负例，JVM）、TC-SA-012（零化断言，JVM）、TC-SA-013（错误模板白名单：不含密钥/AAD/密文片段，JVM）、TC-SA-014（设备：安全级别 / roundtrip / 删除 / 幂等 / per-use 弹窗，instrumentation）、TC-SA-015（设备：生物特征变更失效，MAN-020 自动侧）、TC-SI-010（跨平台 blob 结构 parity 共享向量 + 空明文 parity，Mac/simulator）、TC-SI-011（KEK 坐标契约：字面量 `service="app:auth"` + `Data` account 断言 + test-only 坐标 roundtrip，Mac/simulator）；TC-SI-009（iOS KEK 完整生命周期 roundtrip）**D8 裁定：随 T3.2 JS 面**（Handoff H-T3.1-1）。
- 手工用例（新增）：MAN-019（弹窗 + 3 错 fail-closed + 正确指纹成功）、MAN-020（新增指纹 → E_SEC_KEY_INVALIDATED）；MAN-018 Android 腿本期核销（D3），iOS 腿随 H10 → T4.x。
- 既有：MAN-002 保持开放（更宽生物识别走查 → T3.2）。

## 8. 红线与依赖

- R4：androidx.biometric 1.1.0（新行，D1）+ expo-secure-store ~57.0.4（既有行执行安装，D2）；安装仅在 Round B 复核后执行。KeyGenParameterSpec/BiometricPrompt/CryptoKit 均为系统 API，零新依赖。
- R1：wrap/unwrap 不出 native；TS 面无密钥语义标识符；AAD 仅公开材料。
- R5：新模块无 Log.*/println/print(/NSLog（grep 门禁）；错误模板固定。
- R6：硬件包裹 + per-use 生物识别 [P]（真机验证后）；SOFTWARE_DEBUG 永不 [P]；iOS [P] 标注生效条件 = Mac/真机轮。
- R7：web stub + export 扫描。