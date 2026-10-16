# AGENTS.md — NexVault RN/Expo 跨端工程治理宪法

> 本文件是 NexVault RN/Expo 三端工程（Android / iOS / Web，Expo SDK 57 + react-native-web）的最高工程纪律文件。
> 工程性质：移动端加密资产钱包的求职演示原型（3 天原型 + roadmap 演进）。措辞约定：**必须（must）**= 无条件约束；**禁止（never）**= 红线，违反即回炉；**应当（should）**= 偏离须在 validation.md 记录理由。
> 所有条款决策日期：**2026-10-14**；证据指向：`doc/research-report.md` 章节号（§n）与同目录调研裁决存档（`research/` 下 dim01–dim12、cross_verification、insight，只读）。

---

## Part A — 项目状态公告板（权威，agent 必读）

| 项 | 当前状态 | 证据 |
|---|---|---|
| 工程阶段 | **实施中**（阶段 1 Day 1）。骨架已提交（`088d84f`；存在 scaffold 债 H6）；T1.2、T1.3 均已实装并双侧验证关闭；下一步 T1.4（signDigest/destroy） | `specs/features/2026-10-14-T1.2-vault-core-android-bip39/validation.md`；`specs/features/2026-10-15-T1.3-vault-core-android-bip32/validation.md` |
| 选型定案 | Expo SDK 57（≥57.0.21）+ CNG + Development Build；pnpm workspace；viem；NativeWind v4；Web 端只读降级 | `doc/research-report.md` §sec02 |
| 应用身份 | Android applicationId = iOS bundleIdentifier = **`io.github.realalexkwok.nexvault`**（2026-10-14 owner 定案，上架后终身不可改；`com.nexvault.wallet` 已由原 Kotlin 工程占用，弃用） | `specs/tech-stack.md` §4 |
| 权威规格目录 | `specs/`（mission / roadmap / tech-stack / dev-environment / pending-manual-test-cases + features/） | 本文件 Part B §B.1 |
| 实施计划 | `plans/`（3 天小时级计划，粒度 ≤ 半天） | `plans/3-day-implementation-plan.md` |
| 调研存档 | `doc/` 与 `/mnt/agents/output/research/`：**只读存档**，禁止改写 | `doc/research-report.md` |
| 自动化验证 | T1.2：JVM 16/16（BIP-39 V1–V8）+ 真机 2/2 + JS wrapper 2/2。T1.3：JVM 26/26（BIP-39 16 + BIP-32 向量 1–4 全链 xprv/xpub、规范锚点 `0x9858EfFD…Eda94`、第二地址、keccak/EIP-55、10 条路径负例）；`./gradlew :vault-core:testDebugUnitTest` BUILD SUCCESSFUL；真机 instrumentation 4/4（Pixel 6a / Android 17，`BUILD SUCCESSFUL in 4m 22s`）。CI 尚未接线 | `specs/features/2026-10-14-T1.2-…/validation.md`；`specs/features/2026-10-15-T1.3-…/validation.md` §Task 3–7 |
| 手工验证 | 登记簿 `specs/pending-manual-test-cases.md` 已登记 MAN-001…MAN-010，0 条走查完成；T1.2 的真机侧是 instrumentation 复跑（非人眼走查），人眼侧的 MAN-009 仍待端到端流程 | 本文件 Part B §B.4；`specs/features/2026-10-14-T1.2-vault-core-android-bip39/validation.md` §5.4 |

**最近关闭记录**（倒序，每条附日期与证据路径）：
- 2026-10-16：**T1.3 代码评审轮（独立评审）**——独立复核通过（自写 Python 重算 BIP-32 V1–V4 全 17 链 xprv/xpub + 两地址 + keccak/ERC-55；JVM 门禁复跑 26/26、`BUILD SUCCESSFUL in 32s`；BC 1.86 jar sha256/Maven `<lastUpdated>`/OSV 复核；依赖图仅 1 个 BC 制品）。发现 1 MAJOR（FR-8/AC-9 的 bridge 端到端未实装——device test 直调 `Bip39`/`Bip32`，实为 T1.3-H8 = T1.2-H3，owner D3「Implement」未达成）、4 MINOR（V5/Base58 修正未传播 6 处；`specs/vault-core.md` §2.4 漏列 `E_VAULT_PBKDF2_UNAVAILABLE`；清零清单不完整（BC `HMac` pad 状态、`data.fill(0)` 不在 finally）；第二地址取证工具未记录）、6 NIT。规格已修订：`requirements.md`（Goal/D3/Handoff 重读/FR-2/FR-7/AC-1/AC-2/AC-3/AC-9 + 新增 T1.3-H8 行）、`plan.md`（Task 6.2 + V4 行）、`specs/vault-core.md` §2.4、`specs/tech-stack.md`、`doc/dependencies.md` §6b、`validation.md`（§Code review round + T1.3-H9 + 清零表 + JCE 行更正）。生产代码未改动，真机证据修订号保持一致。**Round-2（主 agent）：** H9 清零补丁已应用并复跑 JVM 门禁 26/26 后**回滚**（当时无设备可复跑真机，为保持真机证据与提交修订号一致）；H8 可行性判定——`AppContext` 构造需真实 `WeakReference<ReactApplicationContext>` + `ModulesProvider` + legacy `ModuleRegistry`（expo-modules-core@57.0.22 `AppContext.kt:50-54`），故 AsyncFunction/JS 桥接测试需 RN 宿主。**Owner 裁定 2026-10-16：H8 replan → T4.1（dev-build 页面 smoke：onboarding 导入 → `deriveAddress` 走 JS 桥接），由 T4.1 规划轮再裁定（B.7-2）**；真机证据已落盘 feature `evidence/`（device XML/pb/log + `device-info.md`，含 Android 17/API 37/fingerprint），回应 NIT-3/NIT-4。**Round-3（文档一致性，2026-10-16）：** FR-8 正文补「交付口径 / 延后 → H8 → T4.1」指针（MINOR-R2-1）；六处「待 owner 裁定」措辞统一改为已裁定 replan → T4.1（MINOR-R2-2；NIT-R3-1 指出其中一处漏改，Round-3.1 已补齐——现为 6/6）；`evidence/device-info.md` 标注为 2026-10-15 现场 `getprop` 记录的 2026-10-16 转写、`device-run-2026-10-15.log` 标注为终端 `tee` 捕获（MINOR-R2-3）；Task 6 增加指向 `evidence/` 的持久副本指针（NIT-R2-1）；证据表标注各 XML 所属运行（device 2026-10-15T18:51:49 / JVM 2026-10-16T04:56:24Z 于提交修订号上重跑）（NIT-R2-2/R2-3）。证据：`/home/superguo/Projects/NexVaultRN/specs/features/2026-10-15-T1.3-vault-core-android-bip32/validation.md` §Code review round（Round-2 actions + durable copies）。
- 2026-10-15：**T1.3 关闭**——vault-core Android `deriveAddress`（BIP32/BIP44 + Keccak-256 + EIP-55）实装并双侧验证：JVM 门禁 26/26（BIP-32 官方向量 1–4 全链 xprv/xpub、规范锚点 `0x9858EfFD232B4033E47d90003D41EC34EcaEda94`、第二地址 `0x6Fac4D18…`、keccak("")、4 组 ERC-55、10 条路径负例）、真机 instrumentation 4/4（Pixel 6a / Android 17，`BUILD SUCCESSFUL in 4m 22s`）、R1/R5/日志 grep 干净、零 npm 漂移、`tsc` exit 0；R4 修宪经 owner 批准（`bcprov-jdk18on:1.86` 写入 `specs/tech-stack.md` + `doc/dependencies.md` §6b）；H2 修订 `specs/vault-core.md`（handle→string、Int→String、checksum 错误码合并）。实现期发现并修正两处规格错误（V3/V4 链数、V5 语义 = 16 条畸形扩展密钥）与一处 BC API 误设（1.86 无 `HDKeyDerivation`：CKD 胶水自写、曲线运算仍由 BC 承担）；测试首跑抓出生产 bug（硬化上界按负 Int 比较）并修复生产代码。证据：`/home/superguo/Projects/NexVaultRN/specs/features/2026-10-15-T1.3-vault-core-android-bip32/validation.md`。Handoff 裁定：H3/H4 关闭，H1（→T1.4）/H2（→T1.5）/H5/H6/H7/H9 开放，H8 由 owner 裁定 replan → T4.1。合并：`main` `a92fb78`（`--no-ff`，2026-10-16），本地 feature 分支已删除，`main` 领先 `origin/main` 17 个提交（未 push）。
- 2026-10-15：**T1.2 关闭**（含独立代码评审修复轮）——vault-core Android BIP39 `importMnemonic` 实装并验证：JVM 向量门禁 16/16（V1–V8 覆盖 12/15/18/21/24 词；V5/V6 官方向量，V7/V8 派生并经独立 Python 实现互验，且每个向量的 entropyHex/校验和由测试侧独立解码断言）、真机 instrumentation 2/2（Pixel 6a / Android 17）、JS wrapper 便携校验 2/2（web stub fail-closed + R1 `finally fill(0)`）、R1/R5/日志 grep 干净、`tsc --noEmit` exit 0、零新增 npm 依赖。评审 8 项发现全部处置：MAJOR-1 修订 FR-9/AC-5；MINOR-1 补齐长度向量；MINOR-2/NIT-1 修复清零路径；MINOR-3 证据改绝对路径并明确手工侧口径；NIT-2 收紧断言；NIT-3 证明 SKILL.md sha256 同源；NIT-4 增平台守卫（error.code 机器可读）。第二轮评审：AC-7 三码口径修订、陈旧计数头同步、熵值/校验和 JUnit 锁定、守卫 code 属性、路径绝对化全部处置。R4 测试依赖修订经 owner 批准并已写入 `specs/tech-stack.md`/`doc/dependencies.md`。证据：`/home/superguo/Projects/NexVaultRN/specs/features/2026-10-14-T1.2-vault-core-android-bip39/validation.md`（§5.3/§5.4/Task 6/Review-fix round）。Handoff 裁定：H3/H4/H5 关闭，H1/H2/H6/H7 待后续条目。合并：`main` `6b9fec4`（`--no-ff`，2026-10-15），本地 feature 分支已删除，`main` 领先 `origin/main` 4 个提交（未 push）。
- 2026-10-14（治理修订）：AGENTS.md B.3 修订——owner 授权 Agent 执行任意 git 命令（含 commit/push/tag/stash/config/hooks）；分支纪律 B.3-1/B.3-2 与 `.gitignore` 安全忽略条目禁令不变。
- 2026-10-14：预研（Stage 0–6）完成并交叉验证通过；AGENTS.md 与 doc/dependencies.md 立宪。[^1^]
- 2026-10-14（补充任务）：JD 合规缺口补齐——报告新增第 8 章「合规边界与标准对标」（doc/research-report.md §8）；`specs/security-testing.md` 立档（MobSF 进 CI + 自绕过回归门禁）；plans 升 v1.1（新增 T5.4 篡改版 APK 对照演示）。

公告板维护纪律：每次 feature validation 完成后，agent **必须**在本表追加一行关闭记录（日期 + 条目 + 证据绝对路径）。禁止只改规格不改公告板。

---

## Part B — 项目宪法（权威）

### B.1 目录约定（binding）

```
nexvault-rn/
├── apps/
│   └── mobile/               # 唯一 Expo app；经 Metro 同时产出 Android / iOS / Web 三端。
│                             # 禁止拆分为多个 app（React/RN 版本对齐成本，纯负收益）[§sec02-2.4]
├── packages/
│   ├── vault-core/           # 纯 TS 业务/密码学胶水核心；零原生依赖；唯一允许出现密钥语义标识符的 TS 包（见 R1）
│   ├── web3-adapter/         # 纯 TS 链交互适配层：viem 封装、交易构造/自检、Web 只读子集裁剪（specs/web3-adapter.md）
│   └── ota/                  # 纯 TS OTA 运行时封装：critical 更新门控、回滚命令契约（specs/ota-update.md §2.2）
├── modules/
│   └── vault-core/           # 自写 expo local module（Kotlin/Swift）：importMnemonic/deriveAddress/signDigest/destroy 四方法 [§sec03-3.5]
│                             # roadmap：libsecp256k1 v0.7.0 经 CMake 引入（C++ 化）
├── specs/                    # 宪法文件体系（唯一可写规格区）
│   ├── mission.md            # 项目使命与三端信任分级（Android/iOS 全功能、Web 只读）
│   ├── roadmap.md            # 严格顺序的条目列表；顺序权威
│   ├── tech-stack.md         # 批准的库/版本/质量门禁；doc/dependencies.md 的决策依据
│   ├── dev-environment.md    # Node ≥22.13、pnpm、EAS CLI、真机清单
│   ├── pending-manual-test-cases.md  # 全局手工测试登记表（MAN-NNN 编号）
│   ├── <module>.md           # 模块 SDD 规格 ×7：vault-core / security-android / security-ios /
│   │                         # security-testing（安全测试与回归门禁）/ web3-adapter / ota-update / ui-wallet
│   └── features/YYYY-MM-DD-<item>-<slug>/   # 一个 roadmap 条目 = 一个目录
├── doc/                      # 只读存档（调研报告、决策记录）。禁止在 doc/ 下写工作文件
└── plans/                    # 实施计划（只增不改；修订=新文件或文末追记）
```

规则：
1. **禁止**在 `apps/mobile` 之外创建第二个 Expo app。平台差异用 `Platform.OS` 与 `.web.ts/.tsx` 平台后缀分叉。[^2^]
2. `doc/` 为只读存档。任何「顺手修正调研结论」必须走新 feature spec，不得原地修改。
3. `packages/` 下全部包（vault-core / web3-adapter / ota）均为纯 TS，**禁止引入任何含原生代码的依赖**；原生能力一律经 `modules/` 的 local module 桥接。

### B.2 SDD 工作流（binding）

1. **一个 roadmap 条目 = 一个 feature spec 目录**：`specs/features/YYYY-MM-DD-<item>-<slug/`，内含三件套：
   - `requirements.md`：Goal / Owner decisions（Q/A 表）/ Handoff re-read / 功能需求（FR 编号）/ AC 映射 / Out of scope / 验收标准（每条 AC 绑定测试编号与门禁）。
   - `plan.md`：Task 0..N 编号步骤（含文件名、接口签名、依赖注入点）+ 收尾 verification 任务。
   - `validation.md`：每任务一节：命令代码块 + 退出码/观察结果 + 测量表 + 修复或接受债裁定 + Handoff 清单。
2. **Roadmap 顺序权威（Roadmap order is authoritative）**：不得跳项、不得并行开两个 feature。认为顺序有误时，先修 `specs/roadmap.md`（=修宪）再动工。
3. 测试编号体系：自动用例 `TC-XXX-NNN`（代码中以 `// TC-XXX-NNN` 注释溯源）；手工用例 `MAN-NNN`（Gherkin Given/When/Then，登记于 `specs/pending-manual-test-cases.md`）。
4. 决策日期：2026-10-14。证据：原 Kotlin 仓库 AGENTS.md 纪律（`research/nexvault-rn_repo_recon.md` ①）；`doc/research-report.md` §sec07。

### B.3 分支与提交纪律（binding）

1. `main` 是唯一主干。**禁止**长期分支；**禁止**直接从 `main` 之外的基线切分支。
2. 分支命名强制：`feature/<item>-<slug>`（与 feature spec 目录同名）。CI 必须校验分支名正则，不符即拒绝构建。
3. **Agent 被授权执行任意 git 命令（2026-10-14 owner 修订）**，包括但不限于 `git commit` / `git push` / `git tag` / `git stash` / `git checkout` / `git restore` / `git config` 及 hooks 操作。不再要求所有改动必须留在工作树由 owner 亲自提交；执行后仍须记录命令与观察结果（B.4）。
4. **禁止**修改 `.gitignore` 中与安全相关的条目（如密钥/证书忽略规则，R2/R5）——本禁令不受 git 授权影响；修改需按修宪流程处理。
5. 决策日期：2026-10-14（修订：2026-10-14，owner 授权 Agent 任意 git 命令）。证据：`research/nexvault-rn_repo_recon.md` ①-4 与 owner 修订指令。

### B.4 验证纪律（核心，never）

1. 状态三档制，全工程统一：
   - `[x]` 已验证（双侧验证齐备）；
   - `[~]` 已实现未验证；
   - `[ ]` 未开始。
2. `[x]` 必须双侧齐备，缺一不可：
   - **自动侧**：命令原文 + 观察到的结果（退出码 / BUILD SUCCESSFUL / 测试计数）+ 构件绝对路径（如 `/mnt/agents/output/.../app-release.apk`）。
   - **手工侧**：真机走查记录（机型、OS 版本、走查结论）。目标真机清单见 `specs/dev-environment.md`。
3. **Never write 'done' without a command and its observed result.** 禁止以「逻辑上应该可以」「和上次一样」充当证据。
4. 手工用例登记纪律：每个需要人眼/真机确认的验收点必须登记 `MAN-NNN` 于 `specs/pending-manual-test-cases.md`；完成一条划一条并附走查证据。**手工用例 never failed and never silently dropped**——失败必须转为 bug 或 Handoff 项，禁止删除登记行。
5. 安全相关 AC（R1–R8 对应项）的手工侧必须在**实体真机**完成，模拟器结果仅作辅助。
6. 决策日期：2026-10-14。证据：`research/nexvault-rn_repo_recon.md` ①-5；`doc/research-report.md` §sec07-7.2。

### B.5 安全红线 R1–R8（never；每条附可执行检查方法）

> 三端信任分级前提：Android 全功能 / iOS 全功能 / **Web 只读观察钱包**。「私钥不出 native + Web 不持私钥」是最高架构红线，同时消解 Hermes 字节码明文窃密面与 Web 供应链投毒面。[^3^]

#### R1 密钥材料不出 native（最高红线）
- JS/TS 层**禁止**以 `string` / `Uint8Array` 形式持有或传递私钥、助记词、seed、派生中间产物；JS 与 native 模块之间**仅传** `ArrayBuffer` 与不透明句柄（opaque handle）。助记词输入字节是唯一例外，进入 native 后 JS 侧必须立即 `fill(0)`。
- 可执行检查（CI 步骤）：
  ```bash
  # vault-core 之外的 TS 代码禁止出现密钥语义标识符
  grep -rnEi 'mnemonic|privateKey|secretKey|seedPhrase|xprv' \
    apps/mobile --include='*.ts' --include='*.tsx' \
    --exclude-dir=node_modules | grep -v 'packages/vault-core' && exit 1
  ```
- `modules/vault-core` 四方法接口冻结：`importMnemonic(bytes)→handle` / `deriveAddress(handle,path)` / `signDigest(handle,path,digest32)` / `destroy(handle)`；native 侧所有中间产物显式 `fill(0)`/`resetBytes`。
- 决策日期：2026-10-14。证据：`doc/research-report.md` §sec03-3.1/3.5/3.6；`research/nexvault-rn_dim07.md` §0/§4/§6。[^4^]

#### R2 OTA 更新包必须启用代码签名校验
- 所有 `production` channel 的 EAS Update 必须启用端到端代码签名（`rsa-v1_5-sha256`）；客户端校验失败**必须拒绝加载**（静默拒绝，不 crash，但记录事件）。签名私钥 `private-key.pem` **离线保管（KMS/密码管理器），永不入库、永不进 CI 环境变量与日志**。
- 可执行检查（CI 步骤）：
  ```bash
  # 1) 私钥不入库
  git ls-files | grep -E 'private-key\.pem|\.pem$' && exit 1
  # 2) eas.json production profile 必须配置代码签名证书字段
  grep -q '"codeSigningCertificatePath"' apps/mobile/eas.json || exit 1
  # 3) 预发环境用故意篡改的 manifest 验证客户端拒绝加载（MAN-NNN 真机用例）
  ```
- 代码签名为 EAS Production/Enterprise 档能力；若用 xprem 自建，签名红线不降级。[^5^]
- 决策日期：2026-10-14。证据：`doc/research-report.md` §sec05-5.2；`research/nexvault-rn_dim11.md` §2.2/§7。

#### R3 热更禁区（必须走商店发新版）
- 以下变更**禁止 OTA**，必须发新二进制走商店审核：① 新增/升级/删除含原生代码的依赖；② 升级 Expo SDK 或 `expo-updates` 本身；③ 新增/修改系统权限、Info.plist usage description、entitlements；④ 修改影响原生构建的 config plugin / app.json 字段；⑤ `runtimeVersion` 指纹变化（含代码签名证书轮换）；⑥ 编译进二进制的资源（图标、启动屏）。
- 可执行检查（CI 步骤）：`runtimeVersion` 采用 `{ policy: "fingerprint" }`；CI 在 `eas update` 前执行 `npx expo-updates fingerprint:generate`（或等效命令）对比当前 build 指纹，不一致即拒绝发布并提示「须发新 build」。发布前人工对照上述六项清单签字确认，记录于 validation.md。
- 合规边界：OTA 仅限 bug 修复/UI/文案/性能；**禁止** OTA + feature flag 组合静默开启涉及资金/私钥流程的新功能（Apple 2.5.2 / DPLA §3.3.1(B)）。
- 决策日期：2026-10-14。证据：`doc/research-report.md` §sec05-5.3；`research/nexvault-rn_dim11.md` §4/§5。[^6^]

#### R4 依赖准入（加依赖 = 修宪）
- **Never add a dependency outside specs/tech-stack.md.** 新增依赖必须先修改 `specs/tech-stack.md` + `doc/dependencies.md`（版本、用途、CVE 处置、信任依据），owner 批准后方可安装。
- 硬性门禁（CI 步骤）：
  1. lockfile 精确钉版：`pnpm-lock.yaml` 入库，CI 一律 `pnpm install --frozen-lockfile`；
  2. 安装脚本禁用：根 `.npmrc` 必含 `ignore-scripts=true`（有原生构建刚需的包在 `pnpm.onlyBuiltDependencies` 白名单逐一放行）；
  3. 新版本冷静期：`minimumReleaseAge`（min-release-age）≥ 7 天，CI 校验 lockfile 中无发布时间 < 7 天的包；
  4. 供应链扫描：CI 跑 `pnpm audit` + Socket 扫描，高危即阻断；
  5. 包名核对：引入 web3/crypto 生态包前人工核对官方包名（`ethers-io`、`verify-contract-*` 等 typosquat 集群实证）。[^7^]
- 准入静态标准：近 6 个月有提交或发布、无未修 CVE、dependents 规模合理、非仿冒包名。「最新 ≠ 最安全」——禁止因「有新版本」而升级。
- 决策日期：2026-10-14。证据：`doc/research-report.md` §sec05-5.7；`research/nexvault-rn_insight.md` Insight 6；`research/nexvault-rn_cross_verification.md` §四-4。

#### R5 日志与落盘红线
- **禁止**任何日志（console / crash reporter / 分析 SDK）输出助记词、私钥、seed、签名原文、完整地址簿之外的敏感字段；native 抛给 JS 的 error message 不得回显密钥片段。
- **禁止**密钥材料未加密落盘；允许落盘的仅限：地址（公开信息）、经 SecureStore/Keychain 保护的密文。
- 缺少配置（RPC endpoint、API key）时，界面**必须显示 "Not configured"**；**禁止以假数据充数让界面看起来能用**。
- 可执行检查（CI 步骤）：
  ```bash
  # 日志调用附近禁现密钥语义变量（启发式，命中须人工复核）
  grep -rnE 'console\.(log|warn|error|info)\(.*(mnemonic|privKey|secret|seed|sig)\)' \
    apps/mobile packages --include='*.ts' --include='*.tsx' && exit 1
  ```
- 决策日期：2026-10-14。证据：`research/nexvault-rn_repo_recon.md` ①-6；`research/nexvault-rn_dim07.md` §6-5。

#### R6 安全能力三级标注制度（[P]/[C]/[S]）
- 文档、spec、代码注释中**每一项**安全机制必须标注其一：
  - **[P] 已真实落地**（Implemented Protection）：已在真机验证、构成真实边界（如私钥不出 native、OTA 签名校验）；
  - **[C] 仅提高攻击成本**（Cost-raiser）：可被足够耐心的攻击者绕过（如 Root/Frida/反调试/端上签名校验在 root 设备上）；
  - **[S] 占位接口**（Stub）：仅有接口与契约，无防护语义。
- **禁止拔高级别**（[C] 写成 [P] 属修宪级违规）。除「检测信号与密钥派生 fail-closed 耦合」外，一切端上检测均为 [C]——标注口径与 OWASP MASVS-R 一致。
- 可执行检查：spec/validation 评审 checklist 必含「本 feature 新增安全机制是否均已标注 [P]/[C]/[S]」；`doc/research-report.md` §sec06-6.3 总表为基线，新增机制须同步登记。
- 决策日期：2026-10-14。证据：`research/nexvault-rn_cross_verification.md` §四-1；`research/nexvault-rn_insight.md` Insight 5。

#### R7 Web 端红线
- Web 构建产物**禁止**包含任何私钥处理代码路径。Web 端定位 = 只读观察钱包 + WalletConnect(Reown) 桥接回手机签名。所有密钥路径必须平台门控（`Platform.OS !== 'web'` + `.web.ts` 分叉为只读实现）。
- 可执行检查（CI 步骤）：
  ```bash
  # Web bundle 不得含 vault-core 签名导出与密钥语义符号
  npx expo export --platform web -o dist-web
  grep -rE 'signDigest|importMnemonic|vault-core' dist-web/_expo/static/js/ && exit 1
  ```
- 背景依据：Web 无 attestation 能力、无法应用层 TLS pinning、供应链投毒实证（chalk/debug、Shai-Hulud、Trust Wallet 扩展事件）。
- 决策日期：2026-10-14。证据：`doc/research-report.md` §sec03-3.4；`research/nexvault-rn_cross_verification.md` H6。[^3^]

#### R8 真钱流程禁止远程 JS 调试
- 涉及真实资产的流程**禁止**开启 Chrome/远程 JS 调试：`react-native-get-random-values` 在远程调试下退化为 `Math.random`（随机数源质量风险）；调试通道本身暴露运行时。
- 可执行检查：release build 中 CI 断言 `__DEV__ === false` 路径无调试入口；真机走查 MAN 用例「release 包无法连接 Metro/Chrome DevTools」；staging 演示必须显式声明为测试网资产。
- 决策日期：2026-10-14。证据：`research/nexvault-rn_dim04.md` §2（E4/E5）。

### B.6 质量门禁（binding）

1. **TypeScript strict 模式**：根与全部 workspace 包 `tsconfig.json` 必须 `"strict": true`；CI `pnpm -r exec tsc --noEmit` 零错误。禁止以 `// @ts-ignore` 压制（`@ts-expect-error` 须附注释理由）。
2. **ESLint 严格规则集**：`eslint-config-expo` + 显式开启 `no-console`（warn 级，R5 配套）、`no-restricted-imports`（禁 `@react-navigation/*` 直接导入，SDK 56 起已解耦）[^8^]；lint 零 error 方可进入 validation。
3. **Jest 单测**：`jest-expo` 跑 `packages/vault-core` 与关键 hooks/util；密码学路径必须含 BIP-39/BIP-32 官方测试向量对拍（向量对拍仅允许在测试代码中引用 `@noble/curves` devDependency）。
4. **真机走查**：每个 feature 的手工侧按 B.4 执行；安全相关 AC 模拟器不算数。
5. 禁止「通过削弱生产代码来修复失败的测试」（Never 'fix' a failing test by weakening production code）。
6. 决策日期：2026-10-14。证据：`research/nexvault-rn_repo_recon.md` ①-8；`research/nexvault-rn_dim07.md` §7（测试向量回归）。

### B.7 Handoff 制度（binding）

1. 刻意递延的事项（demo 级实现、known limitation、待真机验证项）必须逐条登记在该 feature `validation.md` 末尾的 **Handoff 清单**，格式：事项 / 递延理由 / 建议处置 / 关联 AC 或 TC。
2. 下一 roadmap 条目的规划轮**必须**先逐条裁定上一条目的 Handoff：`implement`（纳入本期）/ `replan`（移入指定后续条目）/ `cancel`（须 owner 书面理由）。**禁止静默丢弃**。
3. 当前预研阶段已知的 Handoff 候选（进入首个规划轮裁定）：iOS mlock 无语义保证（已知限制，真机实测项）；native UI 自定义词格键盘完整版；vault-core C++ 化（libsecp256k1 经 CMake）；Secure Enclave P-256 包裹（v2）。
4. 决策日期：2026-10-14。证据：`research/nexvault-rn_repo_recon.md` ①-9；`research/nexvault-rn_cross_verification.md` C6。

---

## Part C — 操作指令（agent 工作流）

### C.1 开工顺序（每次会话必做）

1. 先读本文件 Part A 公告板与 Part B 宪法，再读 `specs/mission.md` → `specs/roadmap.md`（当前条目）→ 对应 feature 三件套 → `specs/pending-manual-test-cases.md`。
2. 动手写代码前，必须完成 ask-user 轮：需求确认（requirements Q/A 表）、计划确认（plan.md）、验收确认（AC 与测试编号）。**每次落盘前必须有 ask-user-question 轮。**
3. 实现中对照 B.5 红线自查；每完成一个 Task 立即在 validation.md 记录命令与观察结果（趁热，不补记）。
4. 收尾：更新状态三档 → 登记/核销 MAN 用例 → 写 Handoff 清单 → 更新 Part A 公告板 → 按 B.3 处理工作树（Agent 可执行任意 git 命令，含 commit；执行后记录命令与结果）。

### C.2 常见禁令速查

| 禁令 | 条款 |
|---|---|
| 修改 `.gitignore` 安全忽略条目 | B.3-4 |
| 无命令+观察结果写 done / [x] | B.4-3 |
| 跳 roadmap 条目 / 并行开两个 feature | B.2-2 |
| JS 层 string/Uint8Array 持有私钥助记词 | R1 |
| OTA 推 native 变更 / 权限变更 / runtimeVersion 变更 | R3 |
| 加 specs/tech-stack.md 之外的依赖 | R4 |
| 日志打印密钥材料 / 假数据冒充已配置 | R5 |
| [C] 机制标注为 [P] | R6 |
| Web bundle 含签名代码路径 | R7 |
| 真钱流程开远程调试 | R8 |
| 修改 doc/ 只读存档 | B.1-2 |
| 删除手工用例登记行 | B.4-4 |
| 削弱生产代码修测试 | B.6-5 |
| 静默丢弃 Handoff 项 | B.7-2 |

---

## 来源定义

[^1^]: `research/nexvault-rn_cross_verification.md`（2026-10-14 交叉验证与冲突裁决）与 `research/nexvault-rn_insight.md`（跨维度洞察）。
[^2^]: `research/nexvault-rn_dim02.md` §4（Monorepo 决策）；Expo 官方 monorepo 指南 https://docs.expo.dev/guides/monorepos/ 。
[^3^]: `research/nexvault-rn_insight.md` Insight 1；`research/nexvault-rn_cross_verification.md` H6/H7。
[^4^]: `research/nexvault-rn_dim07.md` §0/§4.2/§6（ArrayBuffer 边界与清零）；`research/nexvault-rn_dim05.md`（Keystore 不支持 secp256k1 实证，cross_verification H5）。
[^5^]: Expo 官方代码签名文档 https://docs.expo.dev/eas-update/code-signing/ ；`research/nexvault-rn_dim11.md` §2.2/§7（一手核实，2026-10-14）。
[^6^]: Expo 官方 runtime versions 文档 https://docs.expo.dev/eas-update/runtime-versions/ ；Apple App Review Guideline 2.5.2 https://developer.apple.com/app-store/review/guidelines/#2.5.2 ；`research/nexvault-rn_dim11.md` §4/§5。
[^7^]: `research/nexvault-rn_dim12.md`（供应链事件与 npm 加固实践）；`research/nexvault-rn_dim04.md` §5（仿冒包集群 GHSA-h5cf-hh6x-vm87 等）。
[^8^]: Expo 官方博客 "Expo Router v56: Decoupling from React Navigation" https://expo.dev/blog/expo-router-v56-decoupling-from-react-navigation ；`research/nexvault-rn_dim01.md` §3。
