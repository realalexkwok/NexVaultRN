# spec: ui-wallet

> Status: draft | Date: 2026-10-14 | Evidence: doc/research-report.md §2.3、§4.6、§6.3（调研依据 nexvault-rn_dim03、dim10、dim01、dim07）

## 1. 目标（Goal 与范围）

**Goal**：钱包 UI 层——Expo Router 57 路由表、NativeWind v4 设计令牌（低饱和暖色）、FlashList v2 长列表、Reanimated 4.7 动画（60/120fps 帧预算）、首屏性能预算与安全检测异步化挂载点、Web 端响应式只读界面。**安全核心条款：交易确认页的地址/金额显示为 native 渲染红线，防 UI 篡改钓鱼**（§6.3 残余风险「攻击者改道篡改收款地址 UI」的正面回应）。

范围内：三端共享的路由/组件/样式层；确认页 native 显示组件；性能预算门禁。
Out of scope：业务状态逻辑（TanStack Query/Zustand 仅定挂载纪律）；WalletConnect dApp 浏览器（v2）；原生键盘完整版（vault-core Roadmap）；商店截图/营销页。

## 2. 接口定义

### 2.1 路由表（Expo Router 57，typed routes 开启；版本随 SDK 57 对齐，非「v7」[^1^]）

```
app/
  _layout.tsx              # 根 Stack；第一行 import '../polyfills'（web3-adapter §2.1 契约）；
                           # QueryClientProvider + 安全检测异步挂载点（§2.5）
  index.tsx                # 按钱包状态 redirect → /onboarding 或 /(tabs)/home
  onboarding/
    index.tsx              # 欢迎
    import.tsx             # 助记词导入（词格输入；FLAG_SECURE 常驻 [^2^]）
    verify.tsx             # 备份验证
  unlock.tsx               # 生物识别/密码解锁（security-android/ios）
  (tabs)/
    _layout.tsx            # NativeTabs（2026-10-19 T2.1a 实装：Home/History/DApp/NFT/Settings；
                           # 容器 surface、选中 primary、未选中 onSurfaceVariant、indicator primaryContainer、
                           # label labelSmall、高 80（M3 NavigationBar 默认；NexVaultDimens.bottomNavHeight=64
                           # 在 MainScreen.kt 未被使用 —— 2026-10-19 评审更正）—— 原仓库 MainScreen.kt/MainTab.kt 证据）
    home.tsx               # 资产总览（T2.1a 视觉对齐：无 TopBar、链选择器行领起；数据接入 T2.2/T4.x）
    history.tsx            # 交易历史 FlashList v2（T2.1a："Not configured" 占位，R5）
    dapp.tsx               # DApp 浏览器（T2.1a："Not configured" 占位；WalletConnect v2 后置）
    nft.tsx                # NFT 画廊（T2.1a："Not configured" 占位）
    settings.tsx           # 安全设置（T2.1a："Not configured" 占位；L0–L5 指示 T4.x）
  receive/index.tsx        # 收款：地址 + QR（地址经 vault-core.deriveAddress 现取，不信路由参数）
  send/
    index.tsx              # 表单（地址/金额输入）
    confirm.tsx            # ★ native 确认页（§2.2 红线）
    result.tsx             # 三态：已签名→广播中→链上确认（不乐观显示成功 [^3^]）
  address-book/index.tsx
  +not-found.tsx
```

> **2026-10-19 修订（T2.1a 规划轮，owner D3）**：`(tabs)` 组在 T2.1a 实装五 tab（Home/History/DApp/NFT/Settings；DApp/NFT/Settings 先以 "Not configured" 占位，R5）；其余路由表页（unlock / send/confirm / address-book / token-detail / tx-detail / swap 等）维持后续条目（T4.x）承载。Home 无 TopBar（原仓库 HomeScreen.kt：链选择器领起）。

### 2.2 ★ 交易确认页 native 地址显示红线（接口契约）

```tsx
// modules/vault-core 内新增 native view（Kotlin ComposeView / SwiftUI UIViewRepresentable 薄封装）
// 红线：确认页展示的「己方地址/收款地址/金额」必须由 native 组件渲染，
// 且 native 组件的数据源 = 即将送入 signDigest 的同一份序列化交易字节，而非 JS 传入的显示参数。
export interface NativeTxConfirmViewProps {
  unsignedSerialized: ArrayBuffer;  // 同一份字节：native 侧解析出 to/value/chainId 渲染
  fromPath: string;                 // native 侧经 vault-core 现取己方地址显示
  onUserConfirmed(): void;          // 确认后才允许调用 Vault.signDigest
  onRejected(): void;
}
export const NativeTxConfirmView: React.ComponentType<NativeTxConfirmViewProps>;
```

防篡改逻辑：重打包者篡改 JS 层「显示地址 A、实际签地址 B」的钓鱼攻击，要求显示与签名数据源同构——native 从 `unsignedSerialized` 自解析渲染，JS 无法让「显示」与「签名」分叉 [^4^]。确认页同时开启 `expo-screen-capture` 防截屏（幂等、统一 key，规避 iOS #48985 黑屏 bug [^2^]）。

### 2.3 设计令牌（NativeWind v4，原仓库暗色 M3 色板 + Inter）

```ts
// theme/tokens.ts —— tailwind.config 的 theme.extend 映射源
// 2026-10-19 T2.1a 修订（owner D2）：整块替换为原仓库设计系统（/home/superguo/Projects/NexVault @ 3c7487f，实现代码为准）。
// 每个值注释 = 证据（文件:符号 @3c7487f）；转录错误按 T1.3 向量转录发现同级处理。
export const tokens = {
  color: {
    // Color.kt（dark scheme，生产为纯暗色）
    bg:                '#0A0E1A',  // Color.kt:DarkBackground
    surface:           '#121829',  // Color.kt:DarkSurface
    surfaceVariant:    '#1A1F35',  // Color.kt:DarkSurfaceVariant
    surfaceVariant2:   '#232842',  // Color.kt:DarkSurfaceVariant2
    primary:           '#2D5AF0',  // Color.kt:DarkPrimary
    onPrimary:         '#FFFFFF',  // Color.kt:DarkOnPrimary
    primaryContainer:  '#1E3A8A',  // Color.kt:DarkPrimaryContainer
    onPrimaryContainer:'#D8E2FF',  // Color.kt:DarkOnPrimaryContainer
    secondary:         '#00D4AA',  // Color.kt:DarkSecondary
    onSecondary:       '#003730',  // Color.kt:DarkOnSecondary
    secondaryContainer:'#005045',  // Color.kt:DarkSecondaryContainer
    onSecondaryContainer:'#70F7DC',// Color.kt:DarkOnSecondaryContainer
    tertiary:          '#7B61FF',  // Color.kt:DarkTertiary
    onTertiary:        '#2E0080',  // Color.kt:DarkOnTertiary
    tertiaryContainer: '#4A00B8',  // Color.kt:DarkTertiaryContainer
    onTertiaryContainer:'#E8DDFF', // Color.kt:DarkOnTertiaryContainer
    error:             '#FF4C6E',  // Color.kt:DarkError
    onError:           '#FFFFFF',  // Color.kt:DarkOnError
    errorContainer:    '#93000A',  // Color.kt:DarkErrorContainer
    onErrorContainer:  '#FFDAD6',  // Color.kt:DarkOnErrorContainer
    onBackground:      '#FFFFFF',  // Color.kt:DarkOnBackground
    onSurface:         '#FFFFFF',  // Color.kt:DarkOnSurface
    onSurfaceVariant:  '#A0A8C8',  // Color.kt:DarkOnSurfaceVariant
    onSurfaceDisabled: '#5A6180',  // Color.kt:DarkOnSurfaceDisabled
    outline:           '#404759',  // Color.kt:DarkOutline
    outlineVariant:    '#252B3F',  // Color.kt:DarkOutlineVariant
    // NexVaultColors.kt（dark；语义/加密专用色）
    positive:          '#00E676',  // NexVaultColors.kt:positive
    negative:          '#FF5252',  // NexVaultColors.kt:negative
    warning:           '#FFB74D',  // NexVaultColors.kt:warning
    info:              '#2196F3',  // NexVaultColors.kt:info
    cardSurface:       '#141928',  // NexVaultColors.kt:cardSurface
    cardBorder:        '#1E2440',  // NexVaultColors.kt:cardBorder
    shimmer:           '#1E2440',  // NexVaultColors.kt:shimmer
    shimmerHighlight:  '#2A3148',  // NexVaultColors.kt:shimmerHighlight
    textHigh:          '#FFFFFF',  // NexVaultColors.kt:textHigh
    textMedium:        '#A0A8C8',  // NexVaultColors.kt:textMedium
    textDisabled:      '#5A6180',  // NexVaultColors.kt:textDisabled
    gradientStart:     '#0A0E1A',  // NexVaultColors.kt:gradientStart
    gradientEnd:       '#121829',  // NexVaultColors.kt:gradientEnd
  },
  radius: {
    // NexVaultDimens.kt:cornerRadius*；组件级用法（NexVaultCard/Button/TextField.kt）
    xs: 4, sm: 8, md: 12, lg: 16, xl: 24,
    card: 16, button: 16, input: 12, chip: 8,
  },
  spacing: {
    // NexVaultDimens.kt:spacing*（+ 扩展步 spacing6/10/12/18/64）
    xxs: 2, xs: 4, sm: 8, md: 16, lg: 24, xl: 32, xxl: 48,
    s6: 6, s10: 10, s12: 12, s18: 18, s64: 64,
    gutterLg: 24,   // onboarding/receive/swap 屏 gutter（原仓库盘点）
    gutterMd: 16,   // home/token/history/send/nft 屏 gutter（原仓库盘点）
  },
  size: {
    buttonHeight: 52,      // NexVaultDimens.kt:buttonHeight
    inputHeight: 56,       // NexVaultDimens.kt:inputHeight
    bottomNavHeight: 64,   // NexVaultDimens.kt:bottomNavHeight
    icon: 24, iconSmall: 20, tokenIcon: 40, actionButton: 56,
    chartHeight: 180,      // NexVaultDimens.kt:chartHeight
    borderWidth: 1, borderWidthEmphasis: 1.5, borderWidthStrong: 2,  // NexVaultDimens.kt
  },
  type: {
    // Type.kt:NexVaultTypography（size/lineHeight/weight/letterSpacing；family = Inter）
    displayLarge:   { size: 36, lineHeight: 44, weight: '700', letterSpacing: -0.25 },
    displayMedium:  { size: 32, lineHeight: 40, weight: '700', letterSpacing: 0 },
    displaySmall:   { size: 28, lineHeight: 36, weight: '700', letterSpacing: 0 },
    headlineLarge:  { size: 28, lineHeight: 36, weight: '700', letterSpacing: 0 },
    headlineMedium: { size: 24, lineHeight: 32, weight: '600', letterSpacing: 0 },
    headlineSmall:  { size: 20, lineHeight: 28, weight: '600', letterSpacing: 0 },
    titleLarge:     { size: 18, lineHeight: 26, weight: '600', letterSpacing: 0 },
    titleMedium:    { size: 16, lineHeight: 24, weight: '600', letterSpacing: 0.1 },
    titleSmall:     { size: 14, lineHeight: 20, weight: '600', letterSpacing: 0.1 },
    bodyLarge:      { size: 16, lineHeight: 24, weight: '400', letterSpacing: 0.5 },
    bodyMedium:     { size: 14, lineHeight: 20, weight: '400', letterSpacing: 0.25 },
    bodySmall:      { size: 12, lineHeight: 16, weight: '400', letterSpacing: 0.4 },
    labelLarge:     { size: 14, lineHeight: 20, weight: '500', letterSpacing: 0.1 },
    labelMedium:    { size: 12, lineHeight: 16, weight: '500', letterSpacing: 0.5 },
    labelSmall:     { size: 10, lineHeight: 14, weight: '500', letterSpacing: 0.5 },
  },
  font: {
    family: 'Inter',  // Type.kt:NexVaultFontFamily（4 权重 TTF，OFL，自原仓库 res/font 复制）
    weights: { regular: '400', medium: '500', semibold: '600', bold: '700' },
  },
} as const;
```

纪律：原生端 NativeWind v4 className；Web 端同一 className 直通 RN Web 的 Tailwind CSS [^5^]；禁止引入 NativeWind v5 RC（钉住带 CVE-2026-97724 的 worklets 0.10.1 [^6^]）。

**2026-10-19 修订（T2.1a 规划轮，owner D2）**：令牌块整体替换为原仓库设计系统（见上；证据注释 `// Color.kt:DarkPrimary @3c7487f` 格式，转录错误按 T1.3 向量转录发现同级处置）；暖色块（`#1B1815`/`#C79A6B` 等）作废；地址等宽 + 首尾高亮纪律随地址组件（T4.x）落地，原 `addressMono` 键不再保留（原仓库无 mono 字体）。

**2026-10-19 评审补全（Color.kt dark scheme 完整性）**：inverseSurface `#E6E1E5` · inverseOnSurface `#313033` · inversePrimary `#0A2472` · surfaceTint = primary `#2D5AF0`（当前无屏幕使用，仅补全方案色板）。

### 2.4 动画与列表接口纪律

```ts
// 动画：仅 Reanimated 4.7 worklet 跑 UI 线程；react-native-worklets >= 0.12.2（CVE-2026-97724）[^6^]
export const ANIM = {
  frameBudget60: 16.67,   // ms
  frameBudget120: 8.33,   // ms；iPhone 需 CADisableMinimumFrameDurationOnPhone（config plugin 写 Info.plist）[^7^]
  maxJsThreadFrameWork: 4, // ms；安全检测/签名/RPC 解析 0ms——不进帧路径
} as const;
// 列表：交易历史/地址簿用 FlashList v2（免 estimatedItemSize [^8^]）；<200 行静态页可用 FlatList
```

### 2.5 首屏预算与异步挂载点

```ts
// app/_layout.tsx 挂载纪律（dim10 冷热路径分级 [^9^]）
// H1 热路径：只读 SecurityStateCache 布尔值（<1ms）；C1+：首帧后 InteractionManager.runAfterInteractions
export const PERF_BUDGET = {
  coldStartToFirstFrameMs: 1500,   // Pixel 6a 实测口径
  timeToInteractiveMs: 2500,
  homeSkeletonFirst: true,          // 骨架屏先行，余额/行情异步合并
  securityScanMount: 'after-first-frame',  // root/越狱/Frida 扫描禁止进启动路径 [^9^]
  attestationMount: 'onboarding-or-sensitive-write',  // Play Integrity/App Attest 预热时机
} as const;
```

### 2.6 错误码表（UI 层）

| code | 含义 |
|---|---|
| `E_UI_SECURITY_PENDING` | 缓存缺失时显示「环境检查中」，不重复触发全量扫描、不阻塞只读渲染 |
| `E_UI_RISK_LIMITED` | L2–L4 下动作面被限（按钮禁用态 + 平实解释文案 + 修复指引，非技术性报错） |
| `E_UI_NOT_CONFIGURED` | 缺配置/能力不可用的如实显示（R5：禁止造假数据让界面看起来能用） |
| `E_UI_WEB_READONLY` | Web 端访问写路径 → 引导扫码回手机 |

## 3. 威胁模型

**资产**：F1 用户所见即所签（WYSIWYS）；F2 助记词展示/输入过程；F3 首屏可用性与检测可信度。

| # | 攻击向量 | 防御机制 | 级别 |
|---|---|---|---|
| T-UI-1 | 重打包篡改 JS：确认页显示收款地址 A、实际签名交易给地址 B（§6.3 残余风险改道） | **native 确认组件**（§2.2）：显示数据与签名输入同源同字节；己方地址 native 现取 | [C]+（native 渲染抬高门槛；重打包者仍可同时改 native——兜底是 security-android 签名校验 fail-closed + 用户核对地址首尾） |
| T-UI-2 | 截屏/录屏/最近任务页泄露助记词与地址 | expo-screen-capture：导入/备份/确认页 FLAG_SECURE + iOS app switcher 模糊；iOS 幂等调用规避 #48985 [^2^] | [P]（Android FLAG_SECURE）/[C]（iOS 截屏只能检测后告警） |
| T-UI-3 | 三方 IME 窃取助记词击键 | MVP：textNoSuggestions + textVisiblePassword + IME_FLAG_NO_PERSONALIZED_LEARNING（仅 hint，如实标注 [^10^]）；Roadmap：native 词格键盘 | [C]（MVP）→[P]（Roadmap 键盘后） |
| T-UI-4 | 剪贴板泄露 | 助记词/私钥路径完全禁止剪贴板；仅地址可复制；Android 13+ 复制地址可附加 EXTRA_IS_SENSITIVE 防预览浮层肩窥 [^11^] | [P]（禁复制面） |
| T-UI-5 | 恶意 overlay/无障碍覆盖确认页 | L3 级信号（appAccessRisk OVERLAYS/CONTROLLING）拦截 C/D/E 动作面（security-android §3 矩阵）+ 确认按钮需生物识别（CryptoObject 链路） | [C] |
| T-UI-6 | 安全检测阻塞首屏/卡帧被用户关闭 | 冷热路径分级（§2.5）；检测异步挂载、UI 只读缓存；Bugsnag 同款工程共识 [^9^] | [P]（工程控制） |
| T-UI-7 | Web 端供应链注入伪造 UI | Web 只读裁剪（web3-adapter §2.5）；Web 端一切写操作渲染「扫码回手机」引导页 | **[P]**（架构消除） |

## 4. 验收标准

| AC | 内容 | 绑定用例 | 验证方式 |
|---|---|---|---|
| AC-1 | 路由表与 §2.1 一致；typed routes 开启；Web 端 send/unlock/onboarding 路由替换为只读引导页 | TC-UI-001 | jest（路由快照）+ Web 静态导出 grep |
| AC-2 | native 确认组件：构造「显示地址≠签名地址」的对抗性 JS 状态，确认页仍渲染签名交易内的真实地址 | TC-UI-002 | detox（对抗 fixture）+ 真机走查 |
| AC-3 | 助记词页 FLAG_SECURE：Android 截屏被拒/最近任务页空白；iOS 截屏触发审计提示；连续进出不黑屏 | TC-UI-003 | 真机走查（Pixel 6a + iPhone） |
| AC-4 | 动画预算：home/history/confirm 交互在 120Hz 设备帧耗时 P95 ≤ 8.33ms；无安全检测函数出现在帧回调 | TC-UI-004 | 真机 trace（Perfetto/Instruments）+ CI grep（帧回调禁扫描调用） |
| AC-5 | 首屏预算：冷启动首帧 ≤1500ms、TTI ≤2500ms（Pixel 6a）；首帧前无 RPC/文件扫描/attestation | TC-UI-005 | 真机走查 + trace |
| AC-6 | FlashList v2：1000 条交易历史滚动无白闪；Web 端降级 FlatList 可用 | TC-UI-006 | 真机走查 + Web 走查 |
| AC-7 | 设计令牌：tokens 表与 §2.3 一致；地址组件等宽+首尾高亮；L0–L5 风险色使用低饱和语义色 | TC-UI-007 | jest（token 快照）+ 走查 |
| AC-8 | 剪贴板：全 App 助记词/私钥路径无 Clipboard set 调用；地址复制可用 | TC-UI-008 | CI grep + 走查 |
| AC-9 | Web 响应式：sm/md/lg 断点布局正确；Web 端不含写路径组件 chunk | TC-UI-009 | Web 走查 + CI grep |

## 5. 测试用例

### TC-UI-001 路由快照
```gherkin
Given typed routes 生成物与路由目录
When 比对 §2.1 路由表
Then 一致；且 expo export --platform web 产物中 send/confirm、unlock、onboarding/import 路由渲染为只读引导页
```

### TC-UI-002 ★ native 确认页防钓鱼
```gherkin
Given 对抗 fixture：JS 层构造 intent.to = 攻击者地址，同时注入 UI 状态使「看上去」收款地址为受害者地址
When 进入 send/confirm
Then NativeTxConfirmView 渲染的收款地址 = unsignedSerialized 内解析出的真实 to（攻击者地址被如实显示）
And 显示的己方地址 = vault-core.deriveAddress(handle, path) 现取值（不信路由参数）
And 用户确认前不调用 signDigest（调用序列断言）
```

### TC-UI-003 FLAG_SECURE
```gherkin
Given onboarding/import 与 send/confirm 页
When Android 真机截屏 / 打开最近任务页
Then 截屏被拒 / 预览空白
When iOS 真机截屏
Then 触发截屏检测审计提示；反复进出页面不因 #48985 黑屏（统一 key 幂等调用）
```

### TC-UI-004 帧预算（trace）
```gherkin
Given 120Hz 设备 Perfetto/Instruments trace（history 快速滚动 + confirm 打开动画）
When 统计帧耗时 P95
Then ≤8.33ms；帧回调栈中无 security runDetection / RPC / 签名调用
```

### TC-UI-005 首屏预算
```gherkin
Given Pixel 6a 冷启动 release build
When 测量首帧与 TTI
Then ≤1500ms / ≤2500ms；首帧前 trace 中无网络 attestation、无文件扫描
And 余额区骨架屏先行，数据异步合并
```

### TC-UI-006 长列表
```gherkin
Given 1000 条 mock 交易记录
When 快速滚动 history 页
Then 无明显白闪（FlashList v2）；Web 端同数据量降级列表可用
```

### TC-UI-007 设计令牌快照
```gherkin
Given theme/tokens.ts
When jest 快照比对
Then 与 §2.3 一致；地址渲染组件为等宽字体且首 6/末 4 字符高亮色 != 正文色
```

### TC-UI-008 剪贴板（CI grep）
```gherkin
Given 全代码库
When grep Clipboard.setString / setStringAsync 调用点
Then 仅地址/txHash 复制路径存在；助记词、私钥、签名 hex 相关路径零命中
```

### TC-UI-009 Web 响应式
```gherkin
Given Web 静态导出
When 在 375px / 768px / 1280px 视口走查 home/history/address-book
Then 布局按断点收敛（单列→双列→侧边栏）；任何写操作入口呈现「扫码回手机」引导
```

## 6. 依赖与红线

- **T-UI-1 native 确认红线（本模块特有）**：涉及资金展示-签名一致性的页面，显示数据源必须 native 同源（§2.2）；JS 路由参数/全局状态不得作为确认页地址的最终来源。改动确认页数据流 = 修宪。
- **R5**：检测不可用/能力缺失显示「Not configured」或平实降级文案；禁止造假数据（假余额、假「安全」绿标）。
- **R6**：T-UI-1 标 [C]+、T-UI-2 iOS 侧标 [C]、T-UI-3 MVP 标 [C]，禁止在 UI 文案中宣称「绝对防钓鱼/防截屏」。
- **R7 联动**：Web 端写路径不渲染、不进 chunk（TC-UI-001/009）。
- **R4**：样式层仅 NativeWind v4（禁 v5 RC [^6^]）；列表仅 FlashList v2 / FlatList；动画仅 Reanimated 4.7 + worklets ≥0.12.2；**排除** gluestack-ui（2026-06 供应链 RAT 事件 + 弃 Web [^12^]）、TanStack Router（官方不支持 RN [^1^]）。
- **R8/R1 联动**：助记词输入页禁远程调试提示；词字节直送 native（vault-core）。
- 流程纪律：性能 AC（AC-4/5/6）必须真机 trace 证据，模拟器数据无效；Agents 不得 commit/push。

## 7. Roadmap 项（3 天外）

1. **native 词格键盘**（Compose/SwiftUI，2048 词前缀匹配，直送 native buffer）——T-UI-3 升 [P]（与 vault-core Roadmap 同项）。
2. NativeWind v5 转正后迁移评估（`styled` API 替代 cssInterop 属破坏性变更；先确认其 worklets 依赖 ≥0.12.2）。
3. Tamagui v3 评估（若演示需更强设计系统且有人力承担配置成本）。
4. LegendList 升级项（FlashList 遇白闪/可变高度极限时）。
5. 高刷矩阵扩展：Android adaptive refresh 目标机型 tracing；ProMotion 设备回归。
6. WalletConnect dApp 浏览器页（v2，与 web3-adapter Roadmap 联动）。
7. 收款地址本机语音/振动读数辅助核对（防「首尾字符相同」碰撞钓鱼的可用性研究项）。

---
### 来源
[^1^]: expo.dev 官方博客「Expo Router v56」+ npm 实测 expo-router 57.0.26（2026-10-12）；TanStack Router 官方安装文档明示不支持 RN。
[^2^]: docs.expo.dev screen-capture — FLAG_SECURE 含最近任务页空白；GitHub expo/expo#48985（iOS 重复 prevent 黑屏，统一 key 幂等规避）。
[^3^]: dim10 §5.4 — 乐观 UI 粒度：「已签名，广播中」≠ 链上确认。
[^4^]: research-report §6.3 — 重打包木马失效后攻击者改道篡改收款地址 UI 的残余风险论证。
[^5^]: dim03 §3 — NativeWind v4 编译期、className 直通 RN Web；Tamagui 备选、gluestack 排除。
[^6^]: OpenCVE CVE-2026-97724 — worklets <0.12.2 原型链污染 DoS；NativeWind v5 RC 钉 0.10.1。
[^7^]: Apple 官方文档 — ProMotion 不能强制固定 120Hz；iPhone 需 CADisableMinimumFrameDurationOnPhone。
[^8^]: Shopify FlashList v2 文档 — 新架构重写、免 estimatedItemSize。
[^9^]: Bugsnag changelog 2026-10-06 — root/jailbreak 检测移出启动路径并缓存；dim10 §5 冷热路径分级表。
[^10^]: termai.sh — IME_FLAG_NO_PERSONALIZED_LEARNING 是 hint 非强制边界。
[^11^]: developer.android.com copy-paste — Android 13+ 敏感内容须 ClipDescription.EXTRA_IS_SENSITIVE。
[^12^]: gluestack 官方博客 v5 弃 Next.js/Web；2026-06-06 @react-native-aria 供应链 RAT 事件汇编。
