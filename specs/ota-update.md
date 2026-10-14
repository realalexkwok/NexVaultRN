# spec: ota-update

> Status: draft | Date: 2026-10-14 | Evidence: doc/research-report.md §5.1–5.4、§5.7（调研依据 nexvault-rn_dim11、dim01、cross_verification C1）

## 1. 目标（Goal 与范围）

**Goal**：以 EAS Update 为托管热更通道，落地「fingerprint runtimeVersion + 端到端代码签名 + channel/branch + 百分比灰度 + 秒级回滚」的安全热更体系；保留 xprem 自建迁移开关（仅改 `updates.url`）。冷钱包语境下 OTA 是供应链面的一部分，本规格同时是安全规格。

范围内：
- app.json/eas.json 的 expo-updates 配置定案
- 代码签名（rsa-v1_5-sha256）密钥管理与轮换流程
- rollout 策略与回滚 API 封装
- 热更禁区 CI 检查规则
- EAS 档位决策与 xprem 迁移开关

Out of scope：
- 自建 OTA 服务端实现（xprem 部署属 Roadmap 触发项）
- EAS Build/商店发布流水线（report §5.5/5.6 承接）
- Web 端更新（Web 即部署即更新，不走 expo-updates [^1^]）

## 2. 接口定义

### 2.1 配置定案（app.json 关键节）

```jsonc
{
  "expo": {
    "name": "NexVault",
    "slug": "nexvault",
    "android": { "package": "io.github.realalexkwok.nexvault" },        // 定案永久标识（tech-stack §4）
    "ios": { "bundleIdentifier": "io.github.realalexkwok.nexvault" },   // 同上，双端同值
    "runtimeVersion": { "policy": "fingerprint" },   // 定案：任何影响 native 运行时的变更自动 bump [^2^]
    "updates": {
      "url": "https://u.expo.dev/<project-id>",       // xprem 迁移开关：仅此一处改为自建 manifest 端点
      "enabled": true,
      "checkAutomatically": "ON_LOAD",
      "fallbackToCacheTimeout": 0,                    // 首屏零阻塞：立即加载缓存 bundle，更新下次冷启动生效 [^3^]
      "codeSigningCertificate": "./certs/code-signing-certificate.pem",
      "codeSigningMetadata": { "keyid": "main", "alg": "rsa-v1_5-sha256" }
    }
  }
}
```

```jsonc
// eas.json（channel=环境，branch 切指）
{ "build": {
  "development": { "channel": "development" },
  "preview":     { "channel": "preview" },
  "production":  { "channel": "production" }
} }
```

### 2.2 运行时 API 封装（`packages/ota/index.ts`）

```ts
import * as Updates from 'expo-updates';

export interface UpdateStatus {
  isUpdateAvailable: boolean; isCriticalUpdate: boolean;   // critical 由 manifest extra 标记
  currentlyRunningId: string; channel: string;
}
export function checkForCriticalUpdate(): Promise<UpdateStatus>;  // 启动时仅查 manifest（轻量）；critical 时门控进入钱包
export function applyUpdateAndReload(): Promise<void>;            // fetchUpdateAsync + reloadAsync，仅安全时机调用
/** 紧急修复通道：manifest extra.critical=true 的更新强制立即应用；普通更新永远等下次冷启动 */
export function enforceCriticalUpdateGate(): Promise<'ok' | 'updated' | 'unavailable'>;

/** 回滚封装（供发布 runbook 调用，非 App 内） */
export type RollbackPlan =
  | { kind: 'republish'; updateId: string }        // eas update:republish → 重发历史良好更新
  | { kind: 'to-embedded' };                        // eas update:roll-back-to-embedded → 回到内嵌 bundle
export function buildRollbackCommand(plan: RollbackPlan, branch: string): string;  // 生成 eas-cli 命令串
```

### 2.3 发布/灰度命令契约（runbook 引用）

```bash
eas update --branch production --message "..." --private-key-path <KMS 取出的私钥>   # 本地签名，私钥不出机 [^4^]
eas update --rollout-percentage 10                 # 原生百分比灰度 [^5^]
eas update:edit --rollout-percentage 50            # 随时调整
eas update:revert-update-rollout                   # 撤销灰度（重发 control update）
```

### 2.4 错误码表

| code | 含义 |
|---|---|
| `E_OTA_SIG_INVALID` | manifest 签名校验失败 → 客户端拒绝加载（expo-updates 内建行为，静默不崩溃）[^4^] |
| `E_OTA_RUNTIME_MISMATCH` | runtimeVersion 指纹不匹配，更新不送达（平台内建） |
| `E_OTA_CRITICAL_FAILED` | critical 更新拉取失败 → 进入只读降级页，不阻断只读 |
| `E_OTA_DEV_MODE` | `__DEV__` 下调用运行时 API（expo-updates 在 dev 下抛错） |
| `E_OTA_FORBIDDEN_CHANGE` | CI 热更禁区检查命中（见 AC-5），发布流水线中止 |

## 3. 威胁模型

**资产**：E1 更新通道完整性（推送到用户设备的 JS 必须是官方所签）；E2 签名私钥；E3 发布凭据（EAS token/RPC 凭据）；E4 用户资产（恶意热更 = 最大供应链面）。

| # | 攻击向量 | 防御机制 | 级别 |
|---|---|---|---|
| T-OTA-1 | 篡改传输/服务端存储中的更新包（含「EAS 自己被攻破」） | 端到端代码签名：CLI 本地签 manifest（rsa-v1_5-sha256），客户端用构建期内嵌 X.509 证书验签+逐 asset hash 校验，失败拒绝加载；信任锚=随商店审核下发的内嵌证书，非 CA 链 [^4^] | **[P]** |
| T-OTA-2 | 签名私钥泄露 | 私钥离线/KMS 保管、绝不入源码库与 CI 日志（R2）；泄露应急预案：新密钥对 + 新 keyid + 新 runtimeVersion + 新发 build（旧 build 此后拒绝一切新更新）[^4^] | [P]（流程控制） |
| T-OTA-3 | 热更下发 native 不兼容代码 → 大面积崩溃 | fingerprint 策略自动拦截（runtime 变化即指纹变化，更新不送达旧二进制）+ CI 禁区检查（AC-5）双保险 | [P] |
| T-OTA-4 | 恶意/误发更新全量推送 | 百分比灰度 1%×1h → 10%×4-8h → 50% 过夜 → 100%（70-80% 崩溃回归在 1% 首小时暴露 [^5^]）；秒级回滚（republish/roll-back-to-embedded）[^6^] | [C]（缩短爆炸半径，非消除） |
| T-OTA-5 | EAS 发布凭据/CI 被盗推恶意更新 | EAS token 最小权限 + CI 环境隔离 + 发布需双人 runbook；客户端签名校验兜底（攻击者无私钥则包被拒） | [C] |
| T-OTA-6 | iOS 审核合规风险（2.5.2 / DPLA 3.3.1(B)）| 热更仅限 bug 修复/UI/文案/性能；任何涉及资金/私钥流程的新功能走商店审核；OTA+feature flag 静默开启敏感功能 = 禁止（Rollout.io 事件先例）[^7^] | [P]（流程控制） |
| T-OTA-7 | 灰度分流被操纵/灰度组偏斜 | EAS 原生 rollout（服务端分流）；自建 xprem 时用 EAS-Client-ID + rollout 专属盐哈希确定性分流，无需设备态存储 [^8^] | [C] |

## 4. 验收标准

| AC | 内容 | 绑定用例 | 验证方式 |
|---|---|---|---|
| AC-1 | runtimeVersion=fingerprint；改动任一 native 依赖后指纹变化（更新不送达旧 build），纯 JS 改动指纹不变 | TC-OTA-001 | CI（fingerprint 前后比对） |
| AC-2 | 代码签名闭环：签名更新可加载；篡改 manifest 任一字节/用错私钥签名的更新被拒绝加载且不崩溃 | TC-OTA-002 | 真机走查 + CI（负例构造） |
| AC-3 | 私钥卫生：仓库与 CI 日志中不存在 private-key.pem；grep 命中即流水线失败 | TC-OTA-003 | CI grep |
| AC-4 | 首屏零阻塞：fallbackToCacheTimeout=0 下首帧不等待更新检查；critical 门控单独验证 | TC-OTA-004 | 真机走查 + trace |
| AC-5 | 热更禁区 CI 规则：diff 触及 native 依赖/权限/config plugin/expo-updates 版本/runtimeVersion → 阻断 `eas update` 发布 | TC-OTA-005 | CI grep（禁区清单匹配） |
| AC-6 | 灰度与回滚：10% rollout 创建/调整/撤销命令按契约执行；republish 与 roll-back-to-embedded 各演练一次 | TC-OTA-006 | 真机走查（preview channel） |
| AC-7 | xprem 迁移开关演练：updates.url 指向 xprem 本地实例后更新正常送达且签名验证通过；切回 EAS 同样正常 | TC-OTA-007 | 真机走查（Roadmap 触发前可用 docker 本地验证） |
| AC-8 | bytecode diffing 默认开启（SDK 56+），diff 更新可正常应用；bsdiff opt-out 开关存在但未启用 | TC-OTA-008 | CI（产物体积与 manifest 检查） |

## 5. 测试用例

### TC-OTA-001 fingerprint 语义
```gherkin
Given 基线 build 的 runtimeVersion 指纹 F0
When 仅修改一处 UI 文案并重新计算指纹
Then 指纹 == F0（可走 OTA）
When 升级任一含原生代码的依赖（如 expo-screen-capture 小版本）
Then 指纹 != F0，且 eas update 发布被 CI 以 E_OTA_FORBIDDEN_CHANGE 阻断
```

### TC-OTA-002 代码签名正负例
```gherkin
Given 内嵌正确证书的 build
When 发布用正确私钥签名的更新
Then 客户端校验通过并加载
When 构造 manifest 被翻转 1 字节 / 用另一私钥签名的更新
Then 客户端拒绝加载、无崩溃、继续运行当前 bundle（E_OTA_SIG_INVALID 记遥测）
```

### TC-OTA-003 私钥卫生（CI grep）
```gherkin
Given 仓库与 CI 构建日志
When 扫描 "BEGIN (RSA )?PRIVATE KEY" 与 private-key.pem 文件名
Then 零命中；命中即流水线失败
```

### TC-OTA-004 首屏无阻塞
```gherkin
Given 冷启动且服务端存在一个待拉取更新
When 观测首帧时刻与更新下载时刻
Then 首帧早于更新下载完成；更新于下次冷启动生效
Given manifest extra.critical=true 的更新
When enforceCriticalUpdateGate()
Then 拉取成功→reload；拉取失败→只读降级页，不出现「造假可用」界面（R5）
```

### TC-OTA-005 热更禁区（CI grep）
```gherkin
Given 发布流水线的变更 diff
When 命中以下任一：package.json 含原生代码依赖变化 / app.json permissions 或 config plugin 变化 / expo-updates 版本变化 / runtimeVersion 指纹变化 / 图标启动屏等编译期资源变化
Then eas update 发布步骤被阻断并输出禁区命中清单
```

### TC-OTA-006 灰度与回滚演练
```gherkin
Given preview channel 一次 10% rollout
When eas update:edit --rollout-percentage 50 → revert-update-rollout
Then 各步命令成功，rollout 状态可观测
When 分别执行 republish（历史良好更新）与 roll-back-to-embedded
Then 客户端分别回到对应 bundle；回滚后再发新更新所有客户端可收到 [^6^]
```

### TC-OTA-007 xprem 迁移开关
```gherkin
Given 本地 docker 起 xprem（自有签名私钥、R2/MinIO 存储）
When app.json updates.url 指向 xprem manifest 端点并重发 build
Then 更新经 xprem 送达且 expo-updates 原生层验签通过
When 切回 EAS url
Then 同样正常（迁移/回退当天可完成 [^8^]）
```

### TC-OTA-008 diffing 默认开启
```gherkin
Given 连续两次仅 JS 变更的更新
When 检查第二次更新的产物
Then 为 bsdiff diff bundle（体积显著小于全量，官方口径平均 -58% [^9^]）且客户端可正常应用
```

## 6. 依赖与红线

- **R2（本模块核心）**：一切 OTA 更新包必须代码签名校验、失败拒绝加载；签名私钥离线保管、永不入库（TC-OTA-003 CI grep 强制）。**由此反推成本下限**：EAS 代码签名仅 Production（$199/月）/Enterprise 档可用 [^10^]——留在 EAS 则起步 $199/月；成本敏感则转 xprem 自建（签名不受档位限制，仅改 updates.url）。该决策必须在 AGENTS.md/成本表中作为约束条件呈现，不得作为可选项省略。
- **R3**：热更禁区即本文件 §AC-5 清单——native 依赖、系统权限、expo-updates 版本、runtimeVersion 指纹变化必须走商店发新版，禁止 OTA。
- **R5**：critical 更新失败时显示只读降级页，禁止造假「一切正常」；更新遥测不得包含用户地址/密钥语义。
- **R6**：T-OTA-4/T-OTA-5 标 [C] 不得拔高——签名是 [P]，灰度与凭据管理只是缩短爆炸半径。
- **R4**：expo-updates 随 SDK 钉版；xprem 引入（若触发）属修宪，注意其 license 为非标准 SPDX（open-core），法务评审前置 [^8^]。
- 合规注记：不面向中国大陆（geo-fence）；iOS 2.5.2/DPLA 3.3.1(B) 边界——资金/私钥流程新功能禁止热更（T-OTA-6）。
- 流程纪律：发布/回滚命令执行留 runbook 记录（命令+输出+更新 ID）；Agents 不得 commit/push。

## 7. Roadmap 项（3 天外）

1. **xprem 自建迁移**（触发条件：需要签名但拒绝 $199/月，或数据主权要求）：Docker/Helm 部署 + R2/自有 CDN + 自有签名私钥；注意 license 评审。
2. **EAS 档位随规模演进**：1K MAU Free($0) → 10K Starter($19+超额≈$54/月) → 100K Production($199+超额≈$449/月)；交叉点 ~39K MAU；但只要需要托管签名即直接 Production [^10^]。
3. 密钥轮换演练制度化（新 keyid + 新 runtimeVersion + 新 build，证书有效期策略：内部 1 年）。
4. 「对内嵌 bundle 也做 diff」的 SDK opt-in 跟踪（首更流量再省）。
5. expo-observe/自有 ClickHouse 更新遥测；配额与异常下载告警。
6. hot-updater 持续观察但默认不采用（替换客户端 SDK 需全量用户升级二进制，冷钱包用户升级惰性高 [^8^]）。

---
### 来源
[^1^]: docs.expo.dev — Web 不需要 prebuild，也不走 EAS Update（即部署即更新）。
[^2^]: docs.expo.dev/eas-update/runtime-versions — fingerprint 为 2026 官方推荐策略。
[^3^]: docs.expo.dev config 文档 — fallbackToCacheTimeout 默认 0，=0 时立即加载缓存 bundle、更新下次冷启动生效。
[^4^]: docs.expo.dev/eas-update/code-signing — rsa-v1_5-sha256、私钥本地签名不出机、验签失败拒绝加载、轮换需新 runtimeVersion、仅 Production/Enterprise 档。
[^5^]: docs.expo.dev/eas-update/rollouts + 72technologies 节奏建议（1%×1h→10%×4-8h→50% 过夜→100%）。
[^6^]: docs.expo.dev/eas-update/rollbacks — republish 与 roll-back-to-embedded 两种回滚。
[^7^]: developer.apple.com App Review Guideline 2.5.2 + DPLA §3.3.1(B)（2025-10 修订措辞）；2017 Rollout.io 警告信事件。
[^8^]: GitHub API + xprem.dev — mercuretechnologies/xprem 673★、2026-10-11 活跃；签名不受 EAS 档位限制；EAS-Client-ID+盐哈希确定性分流；仅改 updates.url。
[^9^]: expo.dev/changelog/sdk-56 — Hermes bytecode diffing 默认开启，实测平均小 58%。
[^10^]: expo.dev/pricing 一手核实（2026-10-14）：Free 1K MAU / Starter $19·3K MAU / Production $199·50K MAU；MAU 超额 $0.005。
