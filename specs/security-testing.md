# spec: security-testing

> Status: draft | Date: 2026-10-14 | Evidence: doc/research-report.md §sec06、§sec07（调研依据 nexvault-rn_dim14_sectesting；MASTG 编号一手核对于 OWASP/mastg master commit `02ffd85`，tarball 抓取日 2026-10-10）
> 模块定位：把安全验证从「手工动作」工程化为 CI 流水线 + 回归测试。本文件全部为**防御性**表述——所有绕过/注入手段仅用于对自家应用检测机制的回归门禁，永不针对第三方目标。

## 1. 目标（Goal 与范围）

**Goal**：安全验证门禁化——① 静态扫描进 CI（源码级 mobsfscan + Semgrep 常驻门禁；产物级完整版 MobSF 阶段门禁）；② 自绕过回归包（每次修改检测代码必跑「已知绕过脚本集」，三态预期矩阵确定性比对）；③ MASTG v2 测试用例映射（本工程检测机制 ↔ 标准测试编号，缺口如实标注并自研补齐）。

范围内：
- 三个 CI job 的契约定义：`mobsfscan`（源码 SAST + SARIF）/ `mobsf-full`（APK/IPA 上传深度扫描，阶段门禁）/ `bypass-regression`（Frida/objection 已知绕过回归包）
- 测试资产目录约定（`tests/security/bypass-scripts/`）与结果报告格式（SARIF + 三态矩阵 markdown）
- 检测机制 → MASTG-TEST 编号 → MASVS 控制映射总表（§2.4）
- 本模块自身的威胁模型（绕过脚本泄露、测试资产误打包进 release，§3）

Out of scope：
- 检测机制本身（spec: security-android / security-ios 承接，本模块只做它们的回归门禁）
- 服务端风控/判决（spec: security-android §3 已定「判决永远在服务端」）
- 商用 MAST SaaS 采购（AppSweep 等仅作备选对比，Roadmap 评估）

**已知能力边界（如实标注，禁止粉饰）**：
1. **MobSF 对 React Native / Hermes 产物零专门支持**（官方 issue #2635，2026-07-20）：无 RN 检测、无 Hermes 字节码反编译、无 JS 层规则；对 RN APK 只能拿到 manifest/权限/导出组件/原生库/字符串与密钥等通用层结果 [^1^]。Hermes bundle 分析需 hermes-dec/hbctool 另设流水线（Roadmap #2）——这是**已知缺口**，mobsf-full 报告对 JS 层的「干净」结论不构成任何保证。
2. **mobsfscan 不支持 JavaScript/TypeScript**（仅 Java/Kotlin/Swift/ObjC）[^2^]；RN 业务代码源端扫描由 Semgrep 补齐。
3. **Frida 平台兼容边界**：Android 16 支持自 17.1.4 起（17.6.0 带来 Android 16 稳定性大改进）；**iOS 26 仅 preliminary**（强制 debugger mappings，gadget 注入需 `code_signing: required`，Interceptor 未完全支持）；iOS 18/26+ 上 inline hook 受限，可能仅剩 ObjC/Swift 方法 hook 路径可用 [^3^]。预期结果矩阵必须按平台标注可达性。
4. MobSF 动态分析环境受限（社区资料：rooted emulator 至 Android 11/API 30；iOS 需越狱设备或 Corellium），本工程动态回归**不依赖 MobSF 动态模块**，统一走自建 Frida harness [^4^]。

## 2. 接口定义

### 2.1 CI job 契约（GitHub Actions）

```yaml
# .github/workflows/security-gate.yml（片段，钉版为准）
jobs:
  mobsfscan:                      # job-st-1：源码级 SAST，常驻门禁
    if: always()                  # 所有 PR 必跑
    steps:
      - uses: MobSF/mobsfscan@main              # 入口 action；工具版本钉 mobsfscan 1.0.1 [^2^]
        with: { args: '. --sarif --output results.sarif || true' }
      - uses: github/codeql-action/upload-sarif@v2
        with: { sarif_file: results.sarif }
    # 输入：源码树（Java/Kotlin/Swift/ObjC）；输出：results.sarif → GitHub code scanning
    # 门禁：相对 main 基线无新增 high 级 finding；.mobsf 配置 ignore-rules 白名单评审制

  semgrep-js:                     # job-st-1b：RN JS/TS 源端扫描（补 mobsfscan 盲区）
    steps:
      - run: semgrep scan --config auto apps/mobile packages --sarif -o semgrep.sarif
    # 门禁：同 SARIF high 级新增阻断；无 MASVS 映射属已知限制 [^5^]

  mobsf-full:                     # job-st-2：产物级深度扫描，阶段门禁（release 候选构建触发）
    services:
      mobsf:
        image: opensecurity/mobile-security-framework-mobsf:v4.5.3   # 钉 tag，禁 latest [^6^]
        ports: ['8000:8000']
    steps:
      - run: |
          HASH=$(curl -F file=@app-release.apk $MOBSF/api/v1/upload | jq -r .hash)
          curl -X POST $MOBSF/api/v1/scan -d "hash=$HASH"
          curl -X POST $MOBSF/api/v1/report_json -d "hash=$HASH" > mobsf-report.json
    # 输入：CI 构建的 release APK/IPA；输出：mobsf-report.json 存 artifact
    # 门禁：manifest/权限/网络安全配置/硬编码密钥零新增高危；RN/Hermes 层无结论（见 §1 边界 1）

  bypass-regression:              # job-st-3：自绕过回归包
    # 触发条件（必跑）：PR diff 触碰 modules/security-android/** | modules/security-ios/** |
    #                    tests/security/** | packages/vault-core/** 中门控相关路径
    runs-on: macos-15               # android emulator runner
    steps:
      - uses: reactivecircus/android-emulator-runner@v2   # rootable AVD（非 Google Play 镜像）
        with: { api-level: 34, target: default, arch: x86_64 }
      - run: tests/security/ci/setup-frida.sh   # frida-server 与 host frida 17.x 版本严格对齐断言 [^3^]
      - run: tests/security/ci/run-regression.sh # 逐用例 run.sh → 与 expected output.txt 确定性比对
    # 输入：debug-ability 测试构建（仅 CI 内部产物）+ 回归包清单 manifest
    # 输出：bypass-matrix.md（三态矩阵）+ 失败即红
```

iOS 侧 `bypass-regression` 不在 GitHub-hosted runner 执行（无越狱环境）；走 Roadmap #1 真机 farm / Corellium 之前，iOS 回归记为 MAN 手工用例衔接（见 §4 AC-6）。

### 2.2 测试资产目录约定

```
tests/security/
├── bypass-scripts/            # 已知绕过脚本集（红线：永不进 release 构建，见 §3 / §6）
│   ├── android/
│   │   ├── root-denylist-bypass.js        # 自研；参考 httptoolkit android-disable-root-detection.js [^7^]
│   │   ├── frida-detection-bypass.js      # 反 Frida 信号抑制
│   │   └── cert-unpinning.js              # httptoolkit android-certificate-unpinning.js 钉版快照 [^7^]
│   ├── ios/
│   │   └── jb-detection-bypass.js         # 参考 httptoolkit ios-disable-detection.js [^7^]
│   └── objection-notes.md                 # objection 1.12.5 交互式辅助命令（核心逻辑必须沉淀为自有脚本）
├── cases/                     # 每用例一目录，对齐 MASTG demo harness 模式 [^8^]
│   └── ST-ROOT-001/
│       ├── run.sh             # frida -U -f <pkg> -l <script> -o output.txt（spawn 模式）
│       └── expected-output.txt  # 确定性输出基线；diff 不一致即失败
├── manifest.yaml              # 用例 → 检测目标 → 预期态（detected/bypassable/gray）+ 平台可达性
└── ci/                        # setup-frida.sh / run-regression.sh / release-assert.sh
```

纪律：预期输出必须「concise and deterministic」（对齐 MASTG demo 编写规范 [^8^]）；脚本内禁止硬编码任何真实密钥/助记词（R5）。

### 2.3 结果报告格式

1. **SARIF**（mobsfscan / semgrep-js）：上传 GitHub code scanning；PR 视图内联标注；`.mobsf` 的 ignore-rules 与行内 `// mobsf-ignore: rule_id` 抑制项须逐条带理由注释，评审可见 [^2^]。
2. **三态矩阵 markdown**（bypass-regression 产物 `bypass-matrix.md`）：

| 用例 | 检测目标（MASTG-TEST） | 预期态 | 实际 | 判定 |
|---|---|---|---|---|
| ST-ROOT-001 | root 检测（0324/0325） | 应被检出 | detected | ✅ PASS |
| ST-FRIDA-002 | hook 检测（0341/0354） | 承认可绕过（accepted risk） | bypassed | ✅ PASS（符合预期） |
| ST-DBG-003 | 反调试（0352/0353） | 灰区（false-negative-prone） | — | ⚠️ 人工复核 |

三态语义借用 MASTG v2 Evaluation 判据与 `false_negative_prone` 标注 [^9^]：① 应被检出（检出=PASS，未检出=回归失败）；② 已知可绕过（绕过成功=符合预期的 accepted risk，记录台账）；③ 灰区（矩阵固定标注，人工复核，不计红绿）。

### 2.4 MASTG v2 测试用例映射表（核心表）

主键 = MASVS 控制 + MASTG-TEST 编号；编号一手核对于 OWASP/mastg master（commit `02ffd85`，tarball 抓取日 2026-10-10），短链 `https://mas.owasp.org/MASTG-TEST-0xxx/` 于 2026-10-14 实测可达 [^10^]。**MASWE 编号近期有重编风险**（root 检测已从快照期 MASWE-0097 变为当前 MASWE-0051），引用一律锁 commit 版本，MASWE 仅作辅助列 [^10^]。

| 本工程检测机制（spec 出处） | MASTG-TEST（v2，static/dynamic） | MASVS 控制 | MASWE（锁 02ffd85） | 回归包衔接 |
|---|---|---|---|---|
| Android root 检测（security-android §3 T-SA-5） | **0324** / **0325** | MASVS-RESILIENCE-1 | MASWE-0051 | ST-ROOT-001 系列 |
| iOS 越狱检测（security-ios §3 T-SI-3） | **0240** / **0241**（0241 标 false_negative_prone） | MASVS-RESILIENCE-1 | MASWE-0051 | ST-JB-001 系列 |
| Android 反调试（T-SA-6） | **0352** / **0353** | MASVS-RESILIENCE-4 | MASWE-0064 | ST-DBG-003 系列 |
| iOS 反调试 ptrace/PT_DENY_ATTACH（T-SI-4） | **0401** / **0402** | MASVS-RESILIENCE-4 | MASWE-0064 | ST-DBG-004 系列 |
| hook/插桩检测（T-SA-7 / T-SI-2） | Android **0341**；iOS **0354** | MASVS-RESILIENCE-4 | MASWE-0058 | ST-FRIDA-002 系列 |
| 签名/重打包检测（T-SA-3） | **0224**（签名方案版本）/ **0225**（签名密钥强度） | MASVS-RESILIENCE-2 | MASWE-0056 | 静态项进 mobsf-full；运行时自校验无 v2 测试 → 自研用例待立项（演示衔接见 plans T5.4） |
| 防截屏（expo-screen-capture，ui-wallet 承接） | Android **0289–0294**；iOS **0290** | MASVS-PLATFORM-3 | MASWE-0038 | MAN-003 既有登记 |
| iOS 剪贴板（ui-wallet） | **0276–0280** | MASVS-PLATFORM-3 | MASWE-0036 | MAN 走查 |
| TLS pinning（@bam.tech/react-native-app-security） | **0242** / **0243** / **0244**；iOS **0385** | MASVS-NETWORK-2 | MASWE-0028 | ST-PIN-001（unpinning 脚本负例） |
| 更新机制完整性（spec: ota-update） | Android **0382** / **0392**；iOS **0383** / **0384** | MASVS-CODE-2 | MASWE-0043 | MAN-005 既有登记 |

**缺口如实标注（MASTG v2 无正式测试用例，自研 TC 补齐并注明「超出 MASTG 覆盖」）** [^11^]：
- 敏感数据内存保护：官方已弃测（v1 0011/0060 deprecated 且 `covered_by: []`，理由「best addressed during the development process」）。
- Android 剪贴板：无专门 v2 测试（仅知识页 MASTG-KNOW-0020）。
- Keystore / Secure Enclave 硬件级密钥保护：无专项测试；以 CRYPTO-2 要求 + 0307/0308 + AUTH 组 0326–0330 组合覆盖。
- 运行时签名校验/反重打包的「动态」测试：无（仅 0224/0225 静态 + 0338/0387 存储完整性 API 引用）。
- RN OTA bundle（CodePush 类）签名/完整性：无 MASTG 覆盖；本工程由 R2 红线 + ota-update spec 自控。

### 2.5 工具备选对比（如实，选型结论：mobsfscan + 完整版 MobSF 双轨）

| 工具 | 形态 | RN/JS 覆盖 | MASVS 映射 | 部署/合规 | 结论 |
|---|---|---|---|---|---|
| mobsfscan 1.0.1 | CLI SAST（semgrep+libsast 规则） | ❌（无 JS/TS） | 有（规则 ID 对应） | 全自托管 | ✅ 常驻门禁 [^2^] |
| 完整版 MobSF v4.5.3 | Docker + REST API | ❌（Hermes 零支持，issue #2635） | 有 | 全自托管 GPL-3.0 | ✅ 阶段门禁 [^1^][^6^] |
| Semgrep | CLI，17k stars 活跃 | ✅（JS/TS 源码层） | 无官方移动规则包 | 自托管 | ✅ 补充 JS 源端扫描 [^5^] |
| AppSweep（Guardsquare） | SaaS 上传二进制，污点分析 | 二进制级 | 有（按 MASVS 分组） | **二进制离开内网，合规需评估**；免费层无自托管 | Roadmap 评估项 [^12^] |

## 3. 威胁模型

**资产**：D1 绕过脚本集（tests/security/bypass-scripts/）；D2 回归基线可信度（expected-output / manifest 三态）；D3 release 产物纯净性（不含测试/攻击面资产）；D4 CI 工具链可信度。

| # | 攻击/失效向量 | 防御机制 | 级别 |
|---|---|---|---|
| T-ST-1 | 绕过脚本泄露被外部直接复用（虽多为公开脚本改写，聚合后降低攻击者收集成本） | 脚本仅存于 `tests/security/`，仓库内公开可得性按「公开知识」定级处理；真正敏感的是**检测向量清单与预期矩阵**，manifest 与 expected-output 变更须 owner review（CODEOWNERS） | [C] |
| T-ST-2 | 测试资产误打包进 release（frida 脚本/测试 hook 进入商店包 = 直接送攻击面） | CI `release-assert.sh`：release APK/IPA 解包断言无 `tests/security` 路径、无 `frida`/`objection` 字样资源、无 debug-ability 构建标志；eas.json / Xcode 构建配置不含测试资产引用 | **[P]**（CI 强制检查，可证伪） |
| T-ST-3 | 预期矩阵被「改绿」放水（改 expected-output 使回归假通过） | 矩阵与 expected-output 与检测代码同 PR 强制双人 review；AGENTS.md B.6-5「禁止削弱生产代码修测试」同义适用于禁止放松测试基线；矩阵变更在 validation.md 留痕 | [C] |
| T-ST-4 | 安全工具自身漏洞/供应链（MobSF 2026 年自修 SSRF CVE-2026-68927 与 SQL 注入 CVE-2026-33545 [^6^]） | 镜像钉 tag（禁 latest）；MobSF service 仅在隔离 CI 网络运行、不接触生产凭证；工具升级走 R4 修宪流程并复核 changelog | [C] |
| T-ST-5 | frida-server 与 host frida 版本漂移导致假阴性 | setup-frida.sh 启动即断言两侧版本严格相等（17.x 对齐线），不等即 fail-fast [^3^] | [P] |
| T-ST-6 | 回归环境被当作生产信任依据（「CI 绿 = 抗 root 安全」的误读） | R6 口径：绕过回归通过**不提升**任何端上检测的 [C] 标注；文档与对外叙事禁止把回归绿说成抗攻击保证 | [C]（纪律项） |

## 4. 验收标准

| AC | 内容 | 绑定用例 | 验证方式 |
|---|---|---|---|
| AC-1 | mobsfscan job 对所有 PR 必跑；SARIF 成功上传 code scanning；相对 main 基线无新增 high 级 finding | TC-ST-002 | CI（构造含已知坏模式的试验 PR 验证阻断） |
| AC-2 | PR 触碰 `modules/security-*/**` 或 `tests/security/**` 时 bypass-regression 必跑且全绿；不触碰时可跳过（路径过滤器证据） | TC-ST-001 | CI 触发日志 + 两次对照 PR |
| AC-3 | 回归用例输出与 expected-output.txt 确定性 diff；三态矩阵 markdown 生成且「应被检出」项未检出即红 | TC-ST-003 | CI |
| AC-4 | release 产物断言：解包无 tests/security 资产、无 frida/objection 字样、无 debug-ability 标志 | TC-ST-004 | CI（release-assert.sh）+ 真机走查（MAN 衔接） |
| AC-5 | mobsf-full 阶段门禁：release 候选构建上传扫描、报告存 artifact、manifest/硬编码密钥零新增高危；报告头部注明「RN/Hermes 层无结论」 | TC-ST-005 | CI（release 流水线） |
| AC-6 | MASTG 映射表（§2.4）与本工程检测机制一一对应无遗漏；缺口项均有自研 TC 或 MAN 登记（iOS 侧真机回归登记为 MAN-011 起追加至 pending-manual-test-cases.md） | TC-ST-006 | spec 评审 checklist |
| AC-7 | frida-server 与 host 版本对齐断言生效（故意错版即 fail-fast）；平台可达性标注完整（iOS 26 preliminary / Android 16 ≥17.1.4） | TC-ST-007 | CI 负例演练 |

## 5. 测试用例

### TC-ST-001 检测代码变更触发回归全绿
```gherkin
Given 一个修改 security-android root 检测向量的 PR
When CI 执行 bypass-regression job
Then 已知绕过脚本集（denylist-bypass / frida-detection-bypass）全部执行
And 三态矩阵中「应被检出」项全部 detected
And 任一项漏检则 job 红且 PR 不可合并
```

### TC-ST-002 mobsfscan SARIF 无 high 级新增
```gherkin
Given PR 引入一段含已知高危模式的 Kotlin 代码（试验性坏样本）
When mobsfscan job 执行
Then results.sarif 含对应 high 级 finding 并上传 code scanning
And 门禁判定为「新增 high」→ 合并阻断
And 移除坏样本后同一 job 转绿（无基线漂移）
```

### TC-ST-003 确定性输出比对
```gherkin
Given cases/ST-ROOT-001/ 的 run.sh 与 expected-output.txt
When 在 rootable AVD 上执行 frida -U -f <pkg> -l root-denylist-bypass.js -o output.txt
Then output.txt 与 expected-output.txt 逐字节一致（diff 为空）
And 不一致时报告差异行而非笼统失败
```

### TC-ST-004 release 产物纯净性
```gherkin
Given CI 产出的 release APK/IPA
When release-assert.sh 解包扫描
Then 产物内不存在 tests/security 路径资产与 frida/objection 字符串资源
And 故意向 release 构建注入一个测试脚本的对照组必被断言拦截
```

### TC-ST-005 MobSF 阶段门禁
```gherkin
Given release 候选构建
When mobsf-full job 以钉版镜像 v4.5.3 执行 upload → scan → report_json
Then mobsf-report.json 存为 artifact
And manifest/权限/网络安全配置/硬编码密钥项无新增高危
And 报告头自动附加「RN/Hermes 层未覆盖（issue #2635）」声明
```

### TC-ST-006 映射与缺口完整性
```gherkin
Given §2.4 映射表与 security-android/security-ios 的威胁模型表
When spec 评审执行逐行核对
Then 每个检测机制均有 MASTG-TEST 编号或显式「缺口+自研 TC/MAN」标注
And MASWE 列注明锁定 commit 02ffd85 与抓取日期
```

### TC-ST-007 Frida 版本对齐负例
```gherkin
Given AVD 内安装与 host 不一致版本的 frida-server（故意错版）
When setup-frida.sh 执行
Then 断言失败、job fail-fast，且不执行任何回归用例（防止假阴性污染基线）
```

## 6. 依赖与红线

- **R4（本模块直接相关）**：mobsfscan / MobSF / Semgrep / frida / objection 均为工具链依赖，引入与升级视同修宪——先改 tech-stack.md + dependencies.md（版本、CVE 处置、信任依据）；镜像钉 tag、包钉精确版，「最新 ≠ 最安全」。MobSF 2026 年自身 CVE 记录（CVE-2026-68927 / CVE-2026-33545）是「安全工具也必须过供应链门禁」的实证 [^6^]。
- **R6（本模块核心口径）**：绕过回归通过**不**改变任何端上检测的 [C] 标注；三态矩阵中的「承认可绕过」态是 R6 诚实标注在测试工程上的落地形态。禁止把「CI 绿」叙事为「抗 root/抗 hook 保证」。
- **R5**：回归脚本与 expected-output 禁止含真实助记词/私钥/签名原文；CI 日志不得打印密钥语义字段；测试资产缺失时报告「Not configured」，禁止假绿。
- **R1 联动**：回归包验证的是「检测→门控 fail-closed」链路（`E_SEC_GATE_DENIED`），不得在测试中引入任何让密钥材料出 native 的旁路（含 debug 后门）。
- **本模块新增红线（绕过脚本，与 R4/R6 纪律同源）**：绕过脚本永不进 release 构建、仅存在于 `tests/security/` 测试资产目录（T-ST-2 的 CI 断言为此红线的可执行检查）；检测向量清单视为内部敏感资产。
- **R8 联动**：回归环境用 debug-ability 专用测试构建；release 构建远程调试隔离由 MAN-008 既有用例覆盖，本模块不重复。
- **R2 联动**：RN OTA 完整性属 MASTG 缺口（§2.4），门禁依赖 R2 代码签名红线自身，本模块仅负责其回归触发衔接。
- 流程纪律：AGENTS.md B.4 三档状态制；iOS 侧真机回归在 farm 落地前一律登记 MAN 用例（自 MAN-011 起顺序追加），模拟器结论不计入（B.4-5）；Agents 不得 commit/push（B.3-3）。

## 7. Roadmap 项（3 天外）

1. **真机 farm**：iOS 侧 palera1n 越狱真机（iPhone 6s/8 档）或 Corellium 虚拟设备接入 bypass-regression；Android 侧低版本 rooted 环境矩阵；接入前 iOS 回归维持 MAN 手工衔接 [^4^][^13^]。
2. **hermes-dec 流水线**：`index.android.bundle` 经 hermes-dec/hbctool 解包 → 通用 SAST/正则密钥扫描，补 MobSF 的 RN/Hermes 盲区（issue #2635 跟踪，官方支持落地即回迁）[^1^]。
3. **MASTG v2 跟进机制**：订阅 OWASP/mastg 变更（MASWE 重编、新测试发布），每季度重核 §2.4 映射表并更新锁定 commit；v1 deprecated 编号不得再引用 [^10^]。
4. **OWASP DevSecOps 指标化**：按「MASVS controls passing per level」逐版本跟踪回归覆盖率（Advanced 级目标 = MASVS L2 + Frida 运行时检查自动化覆盖）[^14^]。
5. **AppSweep 评估**：免费层 MASVS 映射与污点分析能力 vs 二进制出内网的合规评估；Gradle 插件已弃用（仅 Guardsquare CLI 路径有效）[^12^]。
6. 内存保护自研 TC 设计（官方弃测项，以 vault-core 短时驻留+显式清零的 trace 证据形态补齐）。

---
### 来源
[^1^]: MobSF GitHub issue #2635（[FEATURE] React Native Security Analysis Support，2026-07-20）—「no React Native/Hermes support at all」；实现被评估为「meaningfully large, new analysis pipeline」。https://github.com/MobSF/Mobile-Security-Framework-MobSF/issues/2635
[^2^]: mobsfscan README / GitHub Marketplace（最新 release 1.0.1 @ 2026-09-21，API 实测 2026-10-14）— semgrep+libsast 引擎、SARIF/GitLab-SAST/SonarQube 输出、`.mobsf` ignore-rules、行内 `// mobsf-ignore`；仅支持 Java/Kotlin/Swift/ObjC。https://github.com/MobSF/mobsfscan
[^3^]: frida.re/news — 17.1.4（2025-06-10）Android 16 支持（frida-java-bridge 7.0.3）；17.6.0（2026-01-20）Android 16 稳定性大改进；17.2.9（2025-07-02）iOS 26 仅 preliminary（debugger mappings / Interceptor 未完全支持）；issue #3621（jailed 注入崩溃）已修；iOS 18/26+ inline hook 受限。最新 17.23.3（2026-10-09）。https://frida.re/news/releases/
[^4^]: ostorlab vs MobSF 对比（事实核对日 2026-10-07，竞品视角、置信中）+ MobSF v4.5.1 changelog — 动态分析：rooted emulator 至 Android 11/API 30；jailbroken iOS（checkra1n/palera1n，Early Beta）或 Corellium。
[^5^]: semgrep/semgrep（17k stars，2026-10-14 仍活跃提交）；移动端规则主要靠 mobsfscan 规则集、mindedsecurity/semgrep-rules-android-security、OWASP/mastg `rules/` 演示规则；可扫 JS/TS 但无 MASVS 映射。
[^6^]: MobSF Releases（API 实测 2026-10-14）— v4.5.3（2026-09-21）；v4.5.1 修 SSRF（GHSA-95px-34x5-p37h / CVE-2026-68927）；v4.4.6 修 SQLite viewer SQL 注入（CVE-2026-33545）。REST API：/api/v1/upload、/scan、/report_json、/download_pdf、/scans。https://github.com/MobSF/Mobile-Security-Framework-MobSF/releases
[^7^]: httptoolkit/frida-interception-and-unpinning（持续维护）— android-disable-root-detection.js（伪造 ro.secure/ro.debuggable 等）、android-certificate-unpinning.js、ios-disable-detection.js；作回归包最低绕过门槛基线。
[^8^]: OWASP/mastg `.github/instructions/mastg-frida-scripts.instructions.md` + demos/（157 个 MASTG-DEMO）— run.sh 典型 spawn 用法 `frida -U -f <pkg> -l script.js -o output.txt`；「Keep output concise and deterministic for Evaluation parsing」。
[^9^]: MASTG-TEST-0325 / 0241（master 一手抓取 2026-10-14）— Evaluation 判据「fails if root/jailbreak detection is not implemented」与 `false_negative_prone: true` 标注；TECH-0144/0152 主动绕过观察法。
[^10^]: OWASP/mastg master tarball（commit `02ffd85`，2026-10-10 抓取）front matter 全量解析 + mas.owasp.org 短链实测（0324/0352/0341/0401/0244/0382 均 200）；MASVS v2.1.0（2024-01-18）；MASWE 近期重编实证（root 检测 0097→0051）；MASTG v2 已退出 beta（#3295）。
[^11^]: dim14 §3.2 缺口汇总（一手核对）— 内存保护官方弃测（covered_by 空，理由见 MASTG-KNOW-0051/0103）；Android 剪贴板无专门 v2 测试；Keystore/Secure Enclave 无专项；运行时签名校验动态测试无；RN OTA 完整性无 MASTG 覆盖。
[^12^]: Guardsquare AppSweep 官网/GitHub Marketplace — 免费 MAST（上传 APK/IPA）、MASVS 分组、污点/数据流分析；「The Gradle plugin is DEPRECATED, and superseded by the Guardsquare CLI」；SaaS 模式合规需评估。
[^13^]: sensepost/objection（API 实测 2026-10-14）— **未归档**，1.12.5（2026-06-02）恢复活跃维护（9.4k stars）；核心绕过逻辑仍须沉淀为自有 Frida 脚本，objection 仅作交互式辅助。
[^14^]: OWASP DevSecOps Guideline — Mobile Application Security Testing — Advanced 级「Full MASVS L2 automated coverage with Frida scripts for runtime checks」；指标「MASVS controls passing per level, track regression per release」。
