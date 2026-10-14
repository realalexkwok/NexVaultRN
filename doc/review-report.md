# NexVault RN/Expo 移植预研 — 独立交叉校验报告

- 校验日期：2026-10-14（与交付物决策基准日同日）
- 校验范围：`/mnt/agents/output/nexvault-rn-sdd/` 全部交付物（doc/research-report.md、AGENTS.md、specs/×6、plans/3-day-implementation-plan.md、doc/dependencies.md）
- 校验方式：全文逐条比对（R 引用、接口签名、成本数字、版本号、[P]/[C]/[S] 标注、红线落实、时效性），仅核查不改写
- 总体结论：**PASS 8 项 / WARN 5 项 / FAIL 8 项**。FAIL 均为跨文件一致性缺陷（引用错位、数字/包名/包管理器口径冲突、引用不存在的规格文件），无威胁模型或安全结论层面的方向性错误。

---

## 一、核对结果总表

| # | 核对项 | 结论 | 摘要 |
|---|---|---|---|
| A | 完备性（5 类交付物 + 报告 8 要素） | **PASS** | 交付物齐全；报告含选型对比表/威胁模型/攻击-防御映射表/EAS 三档成本表/来源标注/合规注记/MVP-roadmap 分级/demo-vs-真实防护表 |
| B1 | AGENTS.md R1–R8 ↔ specs §6 引用 | **FAIL** | 3 处 R8 引用错位（fail-closed 门控误挂 R8，R8 实为「禁远程调试」） |
| B2 | specs 接口 ↔ 报告 §3 ↔ plans 一致性 | **FAIL** | 四方法签名主体一致；但 native 模块名两套并存（`modules/vault-core` vs `modules/nexvault-vault`）；plans 的 `attest()` 挂错包 |
| B3 | 成本数字 $0/$54/$449 三处一致 | **PASS** | 报告 §5.4 与 specs/ota-update §7 一致；plans/AGENTS.md 无冲突数字 |
| B4 | 版本号抽查（7 项） | **WARN** | 版本号本体一致；但 libsecp256k1 v0.7.0 的「角色」（MVP 原语 vs roadmap）在报告内部矛盾 |
| B5 | [P]/[C]/[S] 标注纪律 | **WARN** | 检测类机制（Root/Frida/反调试/越狱）无 [P] 拔高，「fail-closed 唯一 [P] 级端上控制」口径各处一致；但 web3-adapter 把供应链门禁标为 [P]，与报告 6.4 的 [C] 矛盾 |
| B6 | plans 任务粒度/依赖/风险/验收 | **WARN** | 粒度≤半天、依赖图、风险矩阵、G1–G3 门槛齐备；但 T3.2（半天）与报告自述的安全 MVP 工作量（4.5–6 人日）未调和 |
| C | 三红线三处落实 | **PASS** | R1/R2/R7 在 AGENTS.md（可执行检查）、specs（AC/TC）、报告（论证）三处均落实 |
| D | 诚实性（无未标注来源数字、无矛盾残留） | **FAIL** | 残留 4 组互相矛盾：text-encoding polyfill、min-release-age 7 天 vs 30、npm ci vs pnpm、libsecp256k1 角色 |
| E | 时效性（5 个关键事实以 2026-10-14 为基准） | **PASS** | SDK 57 最新稳定/SDK 58 beta、Developer Verification 2026-09-30 生效、RN 0.86/React 19.2.3、react-native-keychain 停滞 ~19 个月、web3.js 2025-03-04 归档，各处口径一致且标注核实日期 |

---

## 二、FAIL 项明细（含最小修复建议）

### F1【D】TextEncoder polyfill 包名两套并存，且与被禁包名撞车

- **证据**：
  - plans/3-day-implementation-plan.md L19（定案基线表）：「`react-native-get-random-values` + `text-encoding-polyfill` 两个 polyfill」；L150（风险 e）同样写 `text-encoding-polyfill`。
  - doc/research-report.md L1182（§7.4 风险 e）：同样写 `text-encoding-polyfill`。
  - 但报告自身 §2.5.3（L265–272）与定案（2.5）（L279）定的第二个 polyfill 是 **`fastestsmallesttextencoderdecoder`**，且 L272 明确「禁止引入已废弃的 `text-encoding`（npm 官方 DEPRECATED 标记）」；specs/web3-adapter.md L28/L152 与 doc/dependencies.md L42/L86 同为 `fastestsmallesttextencoderdecoder` 并禁用 `text-encoding`。
- **触发与影响**：plans 是实施入口文档，执行者按 plans 会安装一个名称近似被禁包的 polyfill（`text-encoding-polyfill` ≠ 被禁的 `text-encoding`，但与禁用条目撞名易混淆，且与定案包不符），直接违反 R4 依赖准入。
- **最小修复**：plans L19、L150 与报告 L1182 的 `text-encoding-polyfill` 全部改为 `fastestsmallesttextencoderdecoder`。

### F2【D】供应链冷静期数字冲突：min-release-age 30（天） vs ≥7 天

- **证据**：
  - doc/research-report.md L923：「`.npmrc`: `ignore-scripts=true` + `allow-git=none` + `min-release-age=30`」；L1008（6.2 表第 9 行）重复 `min-release-age=30`。
  - AGENTS.md L130（R4 硬性门禁 3）：「新版本冷静期：`minimumReleaseAge`（min-release-age）**≥ 7 天**」；doc/dependencies.md L7 通用门禁同样写「min-release-age ≥7 天」。
- **影响**：R4 是 CI 可执行门禁，30 天与 7 天是两套阈值，CI 实现按哪一份都会与另一份「修宪级」文档冲突。
- **最小修复**：裁决一个阈值并三处统一（建议报告 §5.7/6.2 改为与 AGENTS.md 一致的 ≥7 天，或将 AGENTS.md/dependencies.md 改为 30 天并注明收紧理由）。

### F3【D】CI 安装命令冲突：`npm ci` vs `pnpm install --frozen-lockfile`

- **证据**：
  - doc/research-report.md L890（§5.6 流水线）：「unit test（Linux runner，**npm ci --ignore-scripts**）」；L924（§5.7 基线 2）：「CI 一律 `npm ci`（禁 `npm install` 现场解析）」；L1008（6.2 表）：「CI 一律 `npm ci`」。
  - AGENTS.md L128（R4 门禁 1）：「`pnpm-lock.yaml` 入库，CI 一律 `pnpm install --frozen-lockfile`」；doc/dependencies.md L7 同；且定案 2.4（报告 L223）、plans L20/L46 均为 pnpm workspace。
- **影响**：pnpm workspace 下 `npm ci` 读的是 package-lock.json（不存在），流水线无法执行；报告 §5.6/§5.7 的 CI 配置是不可运行的残留。
- **最小修复**：报告 L890/L924/L1008 的 `npm ci` 改为 `pnpm install --frozen-lockfile`（`--ignore-scripts` 已由根 `.npmrc` 的 `ignore-scripts=true` 覆盖，注明即可）。

### F4【B4/D】libsecp256k1 v0.7.0 的角色矛盾：MVP 签名原语 vs roadmap

- **证据**：
  - doc/research-report.md L416（§3.5 工程纪律）：「**签名原语用 libsecp256k1 v0.7.0**（bitcoin-core/secp256k1）……iOS 侧走 Blockstream 官方 Swift 绑定 swift-secp256k1」——按 MVP 定案措辞书写。
  - 同报告 L419（同节）：「C++ 化 roadmap：共享 C++ 核心（BIP32/39/44 + libsecp256k1）经 JSI 直连」；L1156（7.3 表）/L1194（7.5）「共享 C++ 核心（libsecp256k1 经 JSI 直连）」为 roadmap。
  - specs/vault-core.md L67：「secp256k1 点运算禁止自实现：**MVP 用打包新版 BouncyCastle，Roadmap 切 libsecp256k1 v0.7.0**」；doc/dependencies.md L62 标注 libsecp256k1 为「**roadmap**」；plans L127/L158 同（roadmap）。
- **影响**：3 天 MVP 到底在 Android 上用 BouncyCastle 还是 libsecp256k1 CMake 出签名，两说并存；按报告 L416 执行会把一个 CMake 交叉编译项塞进关键路径 Day1，与「C++ 化必须 roadmap」的工期裁决（报告 §2.2 能力②）直接冲突。
- **最小修复**：报告 L416 改为「签名原语 MVP 用 BouncyCastle（Android）/ swift-secp256k1（iOS）；libsecp256k1 v0.7.0 为 roadmap 的 C++ 共享核心原语」，与 spec/dependencies/plans 对齐。

### F5【B2】native 模块名两套并存：`modules/vault-core` vs `modules/nexvault-vault`

- **证据**：
  - `modules/vault-core`：AGENTS.md L40（B.1 目录树）、L103（R1 接口冻结条）；doc/dependencies.md L5/L61/L62。
  - `modules/nexvault-vault`：doc/research-report.md L403（§3.5 定案「`modules/nexvault-vault`」）、L406（`import * as Vault from 'nexvault-vault'`）；specs/vault-core.md L25（§2.1）、L58/L74（`Name("NexvaultVault")`）；specs/ui-wallet.md L43；specs/web3-adapter.md L51（`from 'nexvault-vault'`）。
- **影响**：模块名是 R1/R7 CI grep 与 EAS export 扫描的匹配目标（AGENTS.md L163 的 web bundle grep 模式含 `vault-core`），名字不统一会使 CI 检查规则指向不存在的路径。
- **最小修复**：二选一全文统一（建议保留语义更明确的 `nexvault-vault`，修改 AGENTS.md B.1/R1 与 dependencies.md 三处）。

### F6【A/B】AGENTS.md 引用的 specs/ 宪法文件体系不存在

- **证据**：AGENTS.md L15（Part A「权威规格目录 specs/（mission / roadmap / tech-stack / dev-environment / pending-manual-test-cases + features/）」）、L42–47（B.1 目录树同列 5 个文件）、L126（R4：「Never add a dependency outside specs/tech-stack.md」）、L19/L86/L195（`specs/pending-manual-test-cases.md` 为 MAN 用例登记簿）。实际 `specs/` 目录只有 6 份模块规格（vault-core / security-android / security-ios / web3-adapter / ota-update / ui-wallet），mission.md、roadmap.md、tech-stack.md、dev-environment.md、pending-manual-test-cases.md **均不存在**。doc/dependencies.md L4 也声明「本清单是 `specs/tech-stack.md` 的决策依据与附录」。
- **影响**：R4 依赖准入门禁的仲裁文件（tech-stack.md）、B.4 手工用例登记簿（pending-manual-test-cases.md）、B.2 roadmap 顺序权威文件全部悬空——宪法的可执行检查规则指向不存在的文件。
- **最小修复**（二选一）：① 补齐 specs/ 下 5 个宪法文件（tech-stack.md 可从 doc/dependencies.md 摘要生成）；② 或修宪：改 AGENTS.md Part A/B.1/R4/B.4 的引用指向实际存在的 6 份模块规格，并把「tech-stack.md」职责改挂到 doc/dependencies.md。

### F7【B1】R8 引用错位 ×3（fail-closed 门控误挂到「禁远程调试」）

- **证据**（AGENTS.md 权威定义：R8 = 「真钱流程禁止远程 JS 调试」，L168–171；「fail-closed 耦合为唯一 [P] 级端上控制」口径在 R6，L153）：
  1. specs/security-android.md L220：「**R8（本模块核心）**：安全判定 fail-closed——检测不通过则不释放签名材料……」——错位，应引 R6（或注明 fail-closed 口径出自 R6）。
  2. specs/security-ios.md L184：「**R8**：fail-closed 门控同 Android——」——同样错位。
  3. specs/vault-core.md L94（错误码表 `E_VAULT_SECURITY_GATE` 行）：「与 spec: security-android/ios 联动（AGENTS.md **R8** 前置校验）」——错位；且同文件 L206 又正确地以 R8 指「禁远程调试」，同一 spec 内 R8 两种语义并存，自相矛盾。
- **影响**：R 编号是 CI 检查与验收标准的锚点（B.4-5 要求安全 AC 与 R1–R8 对应），错位引用会让「禁远程调试」红线失去验收挂钩、fail-closed 门控找不到宪法依据。
- **最小修复**：三处 R8 → R6（fail-closed 唯一 [P] 口径）；vault-core.md L94 改为「（AGENTS.md R6 fail-closed 口径的前置校验）」。specs 其余 R 引用（R1/R2/R3/R4/R5/R7 及 vault-core L206 的 R8）经逐条比对均正确。

### F8【B2】plans 把 native 检测入口 `attest()` 挂进 `packages/vault-core`，违反目录宪法

- **证据**：plans/3-day-implementation-plan.md L62（T3.2 产出物）：「`packages/vault-core` 内 `attest()` 方法 + JS 门控」。但 AGENTS.md L38/L56（B.1 规则 3）规定「`packages/vault-core` **禁止引入任何含原生代码的依赖**；原生能力一律经 `modules/` 的 local module 桥接」；检测/门控接口的定案位置是 `modules/nexvault-security-android`（specs/security-android.md L11、L55–58 的 `runDetection`/`assertSigningAllowed`），specs/vault-core.md §2.4 也约定门控以 `E_VAULT_SECURITY_GATE` 错误码形式从 security 模块联动，vault-core spec 全文无 `attest()` 接口。
- **影响**：按 plans 执行会在纯 TS 包里长出 native 检测入口，既违反 B.1 规则 3，也与两份 spec 的接口面不一致（spec 里是 `assertSigningAllowed()`，plans 里是 `attest()`，名称也未对齐）。
- **最小修复**：plans L62 改为「`modules/nexvault-security-android` 内 `runDetection`/`assertSigningAllowed` + vault-core `E_VAULT_SECURITY_GATE` 门控 + JS 侧调用封装」。

---

## 三、WARN 项明细

### W1【B2】specs 引入 AGENTS.md 目录约定之外的包：`packages/web3-adapter`、`packages/ota`
- 证据：specs/web3-adapter.md L35/L50/L86/L97、specs/ota-update.md L50；AGENTS.md B.1（L37–38）只允许 `packages/vault-core` 一个包；plans L20 的仓库定案也只有两个包。dependencies.md L40 还把 viem 装进 `packages/vault-core`，与 web3-adapter spec 的 `packages/web3-adapter` 归口不一致。
- 建议：要么修宪扩充 B.1 包清单，要么把两个 spec 的实现位置改回 `packages/vault-core` 子目录/`apps/mobile`。

### W2【B2】`importMnemonic` 入参类型口径：Uint8Array vs ArrayBuffer
- 证据：Uint8Array——plans L21、report L407（代码注释 `wordsBytes /* Uint8Array */`）、L1194（§7.5）；ArrayBuffer——specs/vault-core.md L35（`importMnemonic(mnemonic: ArrayBuffer)`）、AGENTS.md L95/L103（「仅传 ArrayBuffer」）。四方法签名主体（名字/参数序/返回）一致，仅类型标注分叉。
- 建议：以 spec §2.1 的 `ArrayBuffer` 为准（R1 条文口径），统一 plans 与报告 §3.5/§7.5 的写法。另 report L256 示意代码用 `NativeSigner.sign(digest)` 返回 `{r,s,yParity}`，与 spec `signDigest` 返回 65B `r‖s‖recid` 不同，建议加注「示意图」或对齐。

### W3【B5】web3-adapter 把过程控制标为 [P]，与报告 6.4 的 [C] 矛盾
- 证据：specs/web3-adapter.md L130（T-W3A-7 供应链门禁「[P]（流程控制）」）、L129（T-W3A-6「[P]（流程控制）」）；doc/research-report.md L1058（6.4 总表）同一机制明确标「[C]（过程控制，无单点充分条件）」、L1008 标「[C]（过程控制）+ [P]（Web 无钥可偷的架构面）」。
- 说明：Root/Frida/反调试/越狱等检测类机制经全文检索**无 [P] 拔高**；「fail-closed 耦合是唯一 [P] 级端上控制」口径在 AGENTS.md L153、报告 L94/L495/L512/L728/L1152、specs/security-android L220、plans L123 各处一致（PASS 部分）。仅供应链门禁这一行两处打架。
- 建议：web3-adapter §3 两行降为 [C] 或改写为「[C]（过程控制）+ [P]（Web 架构面）」与报告同口径。

### W4【D】noble 库审计次数两处不一致
- 证据：report L272「经 Trail of Bits / Kudelski / Cure53 等 **4 次审计**」；dependencies.md L43「**7 轮**第三方审计：ToB/Kudelski/Cure53」。
- 建议：核实后统一（保守取「≥4 次」）。

### W5【B6】plans T3.2 半天装下「安全 MVP 六项」，与报告工作量估算未调和
- 证据：plans L62 T3.2 排在 Day2 下午（14:00–18:00，4 小时）；report L516/L1196 自述六项合计 4.5–6 人日、砍深度后 Root 检测 MVP 子集仍约 1–1.5 天、Frida 基础四信号 0.5 天、bundle 哈希 0.5+0.5 天——即使按砍后口径也远超 4 小时。报告 §7.5 的「砍深度不砍叙事」论证未在 plans 的时段分配中兑现。
- 建议：plans T3.2 明确标注「六项中仅落地 fail-closed 耦合 + 对照演示所需子集（签名校验 + Root/Frida 各 1–2 个信号）」，其余标注溢出至 Day 4/首个迭代，与报告 §4.2/§7.5 对齐。

---

## 四、PASS 项证据摘要

- **A 完备性**：5 类交付物齐全（`ls` 核实）。报告：选型对比表（L136/L189/L233/L364/L628/L746）、威胁模型（§6.1 L949–992）、攻击-防御映射总表（§6.2 L998–1012，13 行）、EAS 成本表三档（§5.4 L848–852：1K=$0 / 10K≈$54 / 100K≈$449）、脚注来源 183 条（L1204–1386，含一手核实日期）、合规注记不面向中国大陆（§6.6 L1079；报告 L65；ota-update L193）、MVP/roadmap 分级（§4.2 vs §4.3、§7.3）、demo 级 vs 真实防护级表（§7.3 L1154–1168 + plans §4 L125–138）。
- **B2 主体**：四方法签名 `importMnemonic/deriveAddress/signDigest(handle,path,digest)/destroy` 在 AGENTS.md L103、report L103/L403–410/L1194、specs/vault-core.md §2.1–2.3、plans L21 五处一致（除 W2 类型标注分叉）。
- **B3 成本**：$0/$54/$449 仅出现于 report §5.4 与 ota-update §7（L199），数字一致；plans 仅引用「$199/月档位决策点」（L158），AGENTS.md R2 只说「Production/Enterprise 档能力」（L116），无第二套定价；rnpush 错误定价已在报告 L842 显式裁决弃用。
- **B4 版本**：expo ≥57.0.21（report L154、AGENTS L14、dependencies L13、plans L15）、viem 2.56.x（report L75、web3-adapter L7、dependencies L40）、nativewind v4（report L74、ui-wallet L81、dependencies L31）、reanimated 4.7（report L203/L205、dependencies L32、ui-wallet L86）、expo-secure-store（report §3.3、security-ios §2.2、dependencies L50）、@bam.tech/react-native-app-security 1.0.0（report L657、dependencies L53、plans L26）、libsecp256k1 v0.7.0 版本号三处一致（角色矛盾另见 F4）。
- **B6 plans**：任务粒度均 ≤ 半天（每时段 1–2 任务）；依赖列 + mermaid 依赖图（L94–117）；风险预案 7 条含触发信号/降级路径/决策时限（L142–152）；G1–G3 每日验收门槛明确（L54/L68/L81）。
- **C 三红线**：①私钥不出 native——AGENTS R1（L94–104 含 CI grep）、vault-core AC-5/TC-VC-006、report §3.1/L301；②热更必须签名——AGENTS R2（L106–117 含 CI 步骤）、ota-update AC-2/AC-3/TC-OTA-002/003、report §5.2.2；③Web 不持私钥——AGENTS R7（L157–166 含 web bundle grep）、vault-core AC-7/TC-VC-008 + web3-adapter AC-6/TC-W3A-006、report §3.4/L302。
- **E 时效性**：Expo SDK 57 最新稳定（2026-06-30）/SDK 58 2026-09-15 beta（report L130，plans/AGENTS/dependencies 同口径）；Developer Verification 2026-09-30 四国生效、2027 全球（report L90/L647/L1083 三处一致）；RN 0.86/React 19.2.3（report L130/L154、dependencies L14–15）；react-native-keychain 10.0.0（2025-03-23）停滞 ~19 个月（report L367、security-ios L20/L203、dependencies L54 一致）；web3.js 2025-03-04 归档（report L276、web3-adapter L239、dependencies L44 一致）。

---

## 五、未覆盖/残留风险声明

1. 本次为文档间交叉校验，未复核外部事实本身的真实性（如 expo.dev/pricing 当前数值、CVE 编号真实性）——时效性 PASS 的含义是「各文档以 2026-10-14 为基准且口径一致」，不构成对外部来源的再核实。
2. report §5.4 成本测算的中间假设（月 4 次更新 × 2MiB、超额 MAU 赠 40MiB 带宽）来源标注完整但未重新演算。
3. `doc/research-report.agent.outline.md` 与根目录 `plan.md` 属过程文件，未纳入交付物核对清单（如需，可补一轮）。
