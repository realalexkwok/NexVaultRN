# spec: web3-adapter

> Status: draft | Date: 2026-10-14 | Evidence: doc/research-report.md §2.5、§4.5（调研依据 nexvault-rn_dim04、dim06、dim10）

## 1. 目标（Goal 与范围）

**Goal**：以 viem 2.56.x 为唯一 EVM 链交互库，封装「构造 → 序列化 → keccak256 → vault-core native 签名 → 广播」工作流；提供链配置/RPC 抽象与恶意 RPC 返回校验；Web 端只读子集（架构裁剪，无钥可偷）。

范围内：
- viem 封装层（client 构造、交易准备、序列化、签名桥接、广播、回读校验）
- polyfill 加载顺序纪律（App 入口第一行）
- 链配置注册表 + 多 RPC failover
- EIP-155 防重放断言 + 恶意 RPC 返回校验（chainId/回执/余额交叉验证）
- Web 端接口裁剪（只读子集 + `.web.ts` 分叉）

Out of scope：
- 私钥管理/签名本身（spec: vault-core）；Keystore/Keychain 包裹（spec: security-android/ios）
- WalletConnect(Reown) 桥接（v2，dim06 已定方向）；BTC/Solana 多链；Account Abstraction/UserOperation（viem#4002 已知 RN typed-array 坑，不触碰 [^1^]）
- TLS pinning（网络层规格承接，dim10 定案 @bam.tech 双 SPKI pin）

## 2. 接口定义

### 2.1 Polyfill 入口（`apps/mobile/polyfills.ts`）——加载顺序为硬性契约

```ts
// polyfills.ts —— 必须是 App 入口第一行 import（expo-router/entry 之前）[^2^]
import 'react-native-get-random-values';      // 1. crypto.getRandomValues（native CSPRNG）
import 'fastestsmallesttextencoderdecoder';   // 2. TextEncoder 防御性 polyfill（RN≥0.74 已有原生实现）
// 3.（v2 引入 WalletConnect 时）import '@walletconnect/react-native-compat';
```

### 2.2 链配置与 RPC 抽象

```ts
// packages/web3-adapter/src/chains.ts
export interface ChainConfig {
  chainId: number;                  // EIP-155 签名域组成部分，校验硬断言对象
  name: string;
  rpcUrls: string[];                // ≥2 个独立运营商 endpoint（failover + 交叉验证源）
  explorerTxUrl: (hash: string) => string;
  nativeCurrency: { symbol: string; decimals: number };
}
export const SUPPORTED_CHAINS: readonly ChainConfig[];  // mainnet(1) / sepolia(11155111) 起步
export function getChainConfig(chainId: number): ChainConfig; // 未注册链抛 E_W3A_UNKNOWN_CHAIN
```

### 2.3 签名工作流（核心）

```ts
// packages/web3-adapter/src/transaction.ts
import type { VaultHandle } from 'vault-core';

export interface UnsignedTxIntent {
  chainId: number; to: `0x${string}`; value?: bigint; data?: `0x${string}`;
  // nonce/gas/fees 由 prepareTransactionRequest 填充
}
export interface PreparedTx {
  request: TransactionRequest;      // viem prepare 后的完整请求（含 nonce、EIP-1559 fees、chainId）
  unsignedSerialized: `0x${string}`;
  digest: `0x${string}`;            // keccak256(unsignedSerialized)，送 native 的 32 字节
}
/** 步骤 1–3：构造 → 序列化 → keccak256（JS 层，无私钥） */
export function prepareTransaction(intent: UnsignedTxIntent, from: `0x${string}`): Promise<PreparedTx>;
/** 步骤 4：native 签名（vault-core.signDigest(handle, path, digest)，返回 r‖s‖recid）→ 转为 viem Signature（EIP-155：1559 交易用 yParity=recid；legacy 用 v=recid+chainId*2+35）*/
export function signPrepared(handle: VaultHandle, path: string, prepared: PreparedTx): Promise<`0x${string}`>; // 返回 signedRaw
/** 步骤 5–6：广播前 parseTransaction 回读自检 → sendRawTransaction */
export function broadcastSigned(signedRaw: `0x${string}`, expected: PreparedTx): Promise<`0x${string}`>;        // 返回 txHash
/** 全链组合（默认走法；等价于上三步顺序调用） */
export function signAndBroadcast(handle: VaultHandle, path: string, intent: UnsignedTxIntent, from: `0x${string}`): Promise<`0x${string}`>;
```

底层实现锚点（viem 一等 API）[^3^]：
```ts
const request = await prepareTransactionRequest(client, { account: from, ...intent });
const unsignedSerialized = serializeTransaction(request);
const digest = keccak256(unsignedSerialized);
const sig65 = await Vault.signDigest(handle, path, toArrayBuffer(digest)); // native，私钥不出
const signedRaw = serializeTransaction(request, toViemSignature(sig65, request));
assertDeepEqual(parseTransaction(signedRaw), request);   // 广播前自检（AC-4）
const hash = await client.sendRawTransaction({ serializedTransaction: signedRaw });
```

### 2.4 恶意 RPC 返回校验

```ts
// packages/web3-adapter/src/verify.ts
export interface VerifiedReceipt { txHash: string; blockNumber: bigint; status: 'success' | 'reverted'; crossChecked: boolean; }
/** 广播后校验：txHash == keccak256(signedRaw)；receipt 经第二独立 RPC 源交叉确认；chainId 匹配注册表 */
export function waitAndVerifyReceipt(txHash: `0x${string}`, chainId: number): Promise<VerifiedReceipt>;
/** 读路径校验：余额/nonce 等关键读数在主备 RPC 不一致超阈值时抛 E_W3A_RPC_DIVERGENCE */
export function readVerified<T>(fn: (client: PublicClient) => Promise<T>, chainId: number, cmp: (a: T, b: T) => boolean): Promise<T>;
```

### 2.5 Web 端只读子集（接口裁剪）

```ts
// packages/web3-adapter/src/index.web.ts —— Web 构建仅导出以下符号
export { SUPPORTED_CHAINS, getChainConfig };
export function getBalance(address: `0x${string}`, chainId: number): Promise<bigint>;
export function getTransactionStatus(txHash: `0x${string}`, chainId: number): Promise<VerifiedReceipt>;
export function buildUnsignedTxQrPayload(intent: UnsignedTxIntent, from: `0x${string}`): Promise<string>; // 二维码导出未签名交易，回手机签名
// 明确不导出：prepareTransaction 的签名后续步骤、signPrepared、signAndBroadcast、任何 VaultHandle 引用
```

### 2.6 错误码表

| code | 含义 |
|---|---|
| `E_W3A_UNKNOWN_CHAIN` | chainId 未在 SUPPORTED_CHAINS 注册（防签名域错链） |
| `E_W3A_CHAIN_MISMATCH` | RPC 返回 chainId 与配置不符（恶意/错配 RPC） |
| `E_W3A_TXHASH_MISMATCH` | RPC 返回 txHash ≠ keccak256(signedRaw) |
| `E_W3A_RPC_DIVERGENCE` | 主备 RPC 关键读数不一致超阈值 |
| `E_W3A_BROADCAST_REJECTED` | 全部 RPC endpoint 拒绝广播 |
| `E_W3A_REVERTED` | 回执 status=reverted |
| `E_W3A_VAULT` | vault-core 错误透传（code 保留，不得改写 message 注入密钥片段） |
| `E_W3A_READ_ONLY_PLATFORM` | Web 端调用写路径 |

## 3. 威胁模型

**资产**：D1 交易完整性（不签非用户所见交易）；D2 资金（防重放/错链）；D3 RPC 通道可信度；D4 Web 端供应链面。

| # | 攻击向量 | 防御机制 | 级别 |
|---|---|---|---|
| T-W3A-1 | 跨链重放（同一签名交易在 ETC 等链重放） | EIP-155：chainId 编入签名哈希；签名后断言 parsed.chainId === intent.chainId；未注册链直接拒绝（`E_W3A_UNKNOWN_CHAIN`）[^4^] | [P]（协议级） |
| T-W3A-2 | 恶意 RPC 篡改返回（假 nonce→抢跑/卡死、假余额误导、假回执谎称成功） | chainId 匹配断言；txHash 自算比对；回执与关键读数经第二独立 RPC 源交叉验证；分歧即 `E_W3A_RPC_DIVERGENCE` 并阻止后续签名 | [C]（双源 RPC 仍属少数派信任；标注诚实） |
| T-W3A-3 | 恶意 RPC 篡改交易内容（to/value） | 交易内容由 JS 层用户确认（ui-wallet native 地址显示红线）→ 序列化 → digest 送 native；RPC 只能提供 nonce/gas 建议，无法改写已序列化字段而不改变 digest | [P]（对序列化后篡改） |
| T-W3A-4 | Web 端供应链投毒窃钥（chalk/debug、Shai-Hulud、Trust Wallet 扩展实证 [^5^]） | 架构级：Web bundle 不含任何私钥处理路径（接口裁剪 §2.5 + CI grep），注入也「无钥可偷」；签名引导回手机 | **[P]**（架构消除面） |
| T-W3A-5 | Hermes 层注入伪造 digest 请求 native 签名 | 本层不防伪调用——靠 security-android/ios fail-closed 门控 + 用户逐笔确认（native 地址显示）兜底；如实标注 | [C] |
| T-W3A-6 | 随机数源污染（Chrome 远程调试下 getRandomValues 退化 Math.random [^6^]） | 真钱流程禁远程调试（R8）；polyfill 顺序 CI 断言 | [C]（流程控制，提高成本） |
| T-W3A-7 | 仿冒/投毒 npm 包（ethers-io、verify-contract-* typosquat 集群 [^7^]） | R4：lockfile 钉版 + 官方包名核对 + ignore-scripts + Socket 扫描；web3.js 已归档禁止选型 [^8^] | [C]（流程控制，提高成本） |

## 4. 验收标准

| AC | 内容 | 绑定用例 | 验证方式 |
|---|---|---|---|
| AC-1 | polyfill 顺序：App 入口第一行为 polyfills.ts，且其内顺序为 get-random-values → textencoder | TC-W3A-001 | CI grep（入口文件断言） |
| AC-2 | 完整签名闭环在 sepolia 跑通：prepare→digest→native sign→broadcast→链上确认；digest 恰为 keccak256(unsignedSerialized) 的 32 字节 | TC-W3A-002 | detox E2E（dev build 真机）+ 链上 txHash 证据 |
| AC-3 | EIP-155：签名解析回读的 chainId 恒等于 intent.chainId；1559 交易 yParity∈{0,1}；未注册 chainId 抛错 | TC-W3A-003 | jest（parseTransaction 断言） |
| AC-4 | 广播前自检：`parseTransaction(signedRaw)` 逐字段等于 prepared.request；不等则拒绝广播 | TC-W3A-004 | jest（含篡改负例） |
| AC-5 | 恶意 RPC：mock RPC 返回错 chainId/错 txHash/分歧余额时分别抛 E_W3A_CHAIN_MISMATCH / E_W3A_TXHASH_MISMATCH / E_W3A_RPC_DIVERGENCE，且不发起签名 | TC-W3A-005 | jest（mock transport） |
| AC-6 | Web 裁剪：web 构建产物不含 signPrepared/signAndBroadcast/VaultHandle 符号与 secp256k1 签名路径 | TC-W3A-006 | CI grep（`expo export --platform web` 产物） |
| AC-7 | 三端一致：同一 intent 在 Android/iOS/Web（只读部分）产生相同 unsignedSerialized | TC-W3A-007 | jest 跨端对拍 |
| AC-8 | 首屏零 RPC 阻塞：首帧渲染不 await 任何 RPC；余额异步合并 | TC-W3A-008 | 真机走查 + trace |

## 5. 测试用例

### TC-W3A-001 polyfill 顺序（CI grep）
```gherkin
Given apps/mobile 入口文件
When 静态分析 import 顺序
Then polyfills.ts 为第一条 import；且 polyfills.ts 内 get-random-values 在 textencoder 之前
And 工程内不存在 text-encoding（已 DEPRECATED）依赖
```

### TC-W3A-002 sepolia 签名闭环（已知向量）
```gherkin
Given BIP-44 已知账户（abandon×23 about → 0x9858EfFD232B4033E47d90003D41EC34EcaEda94，sepolia 测试币）
When signAndBroadcast(handle, "m/44'/60'/0'/0/0", { chainId: 11155111, to: 自地址, value: 1 wei })
Then 返回 txHash 且 waitAndVerifyReceipt 得 status='success'、crossChecked=true
And native 层全程未向 JS 返回私钥（接口面审查）
And txHash == keccak256(signedRaw)
```

### TC-W3A-003 EIP-155 断言
```gherkin
Given intent.chainId = 11155111
When 完成签名并 parseTransaction(signedRaw)
Then parsed.chainId === 11155111（legacy 时 v = recid + 11155111*2 + 35）
When intent.chainId = 999999（未注册）
Then prepareTransaction 抛 E_W3A_UNKNOWN_CHAIN，不进入签名
```

### TC-W3A-004 广播前自检与篡改负例
```gherkin
Given 合法 signedRaw
When broadcastSigned 内部 parseTransaction 逐字段比对通过
Then 执行 sendRawTransaction
Given 构造时翻转 signedRaw 中 value 字段 1 字节（对照实验，不合法 RLP 或字段不符）
Then 拒绝广播并抛错
```

### TC-W3A-005 恶意 RPC 负例矩阵
```gherkin
Given mock transport 可编程返回
When RPC 返回 chainId=1（配置 11155111）→ 抛 E_W3A_CHAIN_MISMATCH
When 广播后 RPC 返回 txHash ≠ keccak256(signedRaw) → 抛 E_W3A_TXHASH_MISMATCH
When 主 RPC 余额=10 ETH、备 RPC=0.5 ETH → 抛 E_W3A_RPC_DIVERGENCE 且 UI 显示「RPC 数据源不一致」
```

### TC-W3A-006 Web 产物裁剪（CI grep）
```gherkin
Given npx expo export --platform web 产物
When 扫描符号与依赖图
Then 不含 signPrepared/signAndBroadcast；不含 vault-core 绑定；不含 @noble/curves 签名路径入 chunk
```

### TC-W3A-007 三端序列化一致性
```gherkin
Given 固定 intent（含 nonce/gas 全字段固定 mock）
When 分别在 Android/iOS/Web 执行 prepareTransaction
Then 三端 unsignedSerialized 与 digest 逐字节相等
```

### TC-W3A-008 首屏无阻塞（真机走查）
```gherkin
Given 冷启动
When 观测首帧渲染完成时刻
Then 此前无任何 await RPC；余额区域先骨架屏后异步合并
```

## 6. 依赖与红线

- **R7（本模块核心）**：Web 构建产物不得包含任何私钥处理代码路径——§2.5 接口裁剪 + TC-W3A-006 CI grep 双保险；Web 端 `platform=web, assurance=low` 标记随请求上送。
- **R1 联动**：digest 是唯一进入 native 的交易数据形态；本模块任何函数不得要求/返回私钥（接口面审查）。
- **R8**：真钱流程禁远程 JS 调试（getRandomValues 退化 Math.random 风险 [^6^]）。
- **R4**：链交互库唯一 = viem（pin 2.56.x）；ethers v6、web3.js（已归档 [^8^]）禁止；polyfill 仅 `react-native-get-random-values@^2.0.0` + `fastestsmallesttextencoderdecoder`；不引入 Buffer shim / quick-crypto（本原型无此需求 [^9^]）。
- **R5**：错误透传不得改写为含密钥语义的 message；RPC URL 中的 API key 只经 BuildConfig/extra 注入，不入库。
- **R6 联动**：T-W3A-2 的 RPC 交叉验证为 [C] 级，禁止在文档/UI 中宣称「已验证链上状态绝对正确」；回执三状态（已签名/广播接受/链上确认）分层呈现，不乐观显示「交易成功」[^10^]。
- 流程纪律：双侧验证（jest + detox 真机 + 链上 txHash 证据）；Agents 不得 commit/push。

## 7. Roadmap 项（3 天外）

1. **WalletConnect(Reown) 桥接**：Web（AppKit）发起 → RN（WalletKit）签名回执；v1 先用「二维码导出未签名交易 → 手机扫码签名 → 二维码回传」纯离线闭环 [^11^]。
2. 多链扩展（BTC：@scure/btc-signer，Cure53 审计过；注意其 UTXO 选择模块未审计）。
3. `toAccount` 自定义签名器包装 native 签名（接 wagmi 生态）。
4. RPC 健康度面板：多源延迟/分歧率监控、自动摘除分歧源。
5. EIP-712 typed data 签名（WalletConnect 场景需要；注意 EIP-712 本身不含防重放，应用层须处理 [^4^]）。
6. 行情/价格源接入（CoinGecko 等）与 TLS pinning 联动（仅 pin 自有 API）。

---
### 来源
[^1^]: GitHub wevm/viem#4002 — RN 中 prepareUserOperation 触发 typed-array `.buffer` 边界问题（本项目不用 AA）。
[^2^]: docs.ethers.org v5 cookbook 原则 + dim04 §2 — get-random-values 必须先于一切使用随机源的代码 import。
[^3^]: viem.sh/docs/chains/serializers + wevm/viem discussion #785 + QuickNode 2026-08 教程 — prepare→serialize→sign→sendRawTransaction 为一等流程。
[^4^]: EIP-155（chainid,0,0 编入签名哈希；v={0,1}+CHAIN_ID*2+35）；EIP-712 原文「does not include replay protection」。
[^5^]: The Hacker News 2025-09（chalk/debug 20 亿周下载投毒）、Shai-Hulud（$58M）、Trust Wallet 扩展 v2.68（$8.5M）。
[^6^]: openapps.pro — react-native-get-random-values 在 Chrome 远程调试下降级 Math.random 并告警。
[^7^]: GitHub Advisory GHSA-h5cf-hh6x-vm87 / GHSA-rmhg-qfp9-hvvm 等 — viem/ethers 生态仿冒包集群。
[^8^]: ChainSafe 官方博客 — web3.js 2025-03-04 归档，最终版 4.16.0。
[^9^]: dim04 §8 — 本原型无私钥派生/keystore 解密，不需 Buffer shim 与 quick-crypto。
[^10^]: dim10 §5.4 — 乐观 UI 正确粒度：本地签名完成只显示「已签名，广播中」。
[^11^]: Reown 官方文档 — AppKit RN 需 @walletconnect/react-native-compat 首行导入；WalletKit 支持 RN 钱包侧签名回执。
