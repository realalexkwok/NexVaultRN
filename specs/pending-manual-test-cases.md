# spec: pending-manual-test-cases — 手工测试登记表

> Status: active | Date: 2026-10-14 | Constitution: yes
> 用途：CI 无法自动化的真机/人工走查统一登记（AGENTS.md B.4-5：涉及生物识别、截屏、越狱/root 等结论**必须实体真机**）。每条用例走查后把结果（机型 + OS 版本 + 录屏证据链接 + 结论）补入 validation.md，并同步 `plans/` 对应 AC 的勾选。

> 走查记录格式（写进 feature 的 validation.md）：`MAN-NNN | 机型+OS | 日期 | 录屏证据链接 | 结论（通过/失败/阻塞）`。

## 登记表

| ID | 用例 | 预期 | 关联 spec 用例 | 依赖设备 | 状态 |
|---|---|---|---|---|---|
| MAN-001 | 真机签名全链路走查：导入助记词→派生地址→构造 EIP-1559→native 签名→回收站→广播→Etherscan 回执 | 签名成功；不通过则 abort（“演示假签名”=红线，AGENTS.md B.4-5） | TC-VC-001/003/007、TC-W3A-003 | Android 干净机 + iPhone 各一 | ☐ 待走查 |
| MAN-002 | 生物识别门禁：改生物信息→设备密码回退→连续失败→取消弹窗 | 依序触发 promptDeviceCredential 回退 / 硬失败 / 取消信号（E_VAULT_BIOMETRIC_*）；per-use 有效期=使用周期 | TC-SA-005、TC-SI-003 | 生物识别真机 ×2 | ☐ 待走查 |
| MAN-003 | 防截屏：敏感页截图/录屏、切后台缩略图、离开恢复 | Android 黑屏/空白 + 后台模糊；iOS 快照遮挡层；模拟器结论无效须真机复核 | TC-SA-004、TC-SI-005、TC-UI-004 | 双端真机 | ☐ 待走查 |
| MAN-004 | 越狱/Root 分级处置：干净机 vs Magisk 机对照 | 干净机 risk=0 放行；Root/Frida 机 signDigest 被 E_VAULT_SECURITY_GATE 拒绝（fail-closed）并录屏 | TC-SA-002/003/006、TC-SI-002、TC-VC-004 | 干净机 + Magisk 机 | ☐ 待走查 |
| MAN-005 | OTA 代码签名与回滚演练：签 bundle→发布→回滚→重放篡改 bundle | 验签失败包被拒载；rollback-to-embedded 命令真实生效 | TC-OTA-002/003/006 | staging 渠道真机 | ☐ 待走查 |
| MAN-006 | OTA critical 门控：强制更新不可跳过、非 critical 可跳过并可回滚 | 门控 UI 行为符合 spec | TC-OTA-005/007、TC-UI-006 | 双端真机 | ☐ 待走查 |
| MAN-007 | Web 端只读性：UI 无签名入口 + 无签名 bundle | Web 端不出现导入/签名/广播入口（R7） | TC-W3A-004、TC-UI-005 | 桌面浏览器 ×2 | ☐ 待走查 |
| MAN-008 | release build 远程调试隔离：Chrome DevTools / Metro 无法连接 | 连接失败；真钱流程调试不可用（R8） | TC-SA-007、TC-W3A-006 | Android release 真机 | ☐ 待走查 |
| MAN-009 | 助记词不落盘审查：断网环境下全流程 + 本地文件/日志检查 | 无 plaintext 助记词/私钥落盘；日志零泄露（R5） | TC-VC-002/008 | 双端真机 + adb 检查 | ☐ 待走查 |
| MAN-010 | 弱网/断链链交互：广播超时、回执超时、断网降级 | 提示可重试、无重复广播、无悬挂状态 | TC-W3A-002/005 | 真机 + 网络调节 | ☐ 待走查 |
| MAN-011 | 真机内存卫生走查（TC-VC-005 人工侧）：1000 轮 import→derive→sign→destroy 期间以 `adb shell dumpsys meminfo` 采样 PSS/TOTAL，观察驻留曲线 | PSS/RSS 无单调增长（±5% 容差）；无 OOM/崩溃；destroy 后旧 handle 调用抛 E_VAULT_HANDLE_NOT_FOUND | TC-VC-005、TC-VC-025 | Pixel 6a / Android 17 | ☑ **通过** 2026-10-16（Pixel 6a / Android 17；`am instrument` 两次 `OK (6 tests)`；PSS 曲线非单调 62.8k→112.7k→92.6k kB，无 OOM/崩溃；±5% 驻留带由测试内 post-GC PSS 前后对比断言承载）。证据：`specs/features/2026-10-16-T1.4-vault-core-android-sign-digest/validation.md` Task 6.2 |
| MAN-012 | iOS 真机内存卫生走查（TC-VC-005 iOS 侧）：1000 轮 import→derive→sign→destroy 期间以 Instruments / Profiler 采样驻留曲线 | 无单调增长（±5% 容差由测试内断言承载）；无 OOM/崩溃；destroy 后旧 handle 抛 E_VAULT_HANDLE_NOT_FOUND | TC-VC-005、TC-VC-024 | iPhone（实体真机，B.4-5） | ☐ 待走查（T1.5-H1：无**合格**真机——已接 `郭浩然的iPad`（iPad mini 4，iPad5,1，iOS 15.8.8/19H422），低于 iOS 18.0 下限，Xcode 判定为 ineligible destination）。**2026-10-17 自动化载体已就位**：`MemoryWalkTests`（1000 轮 import→derive→sign→destroy、每 100 轮 `phys_footprint` 采样、±5% 带内断言 + 每轮对拍 TC-VC-027 地址/签名）在 iPhone 17 / iOS 26.5 模拟器 **18/18 通过**，曲线 29.89 MB → 30.91 MB（+3.40%）非单调；真机 Instruments 曲线与机型/OS/结论记录仍待。证据：`specs/features/2026-10-16-T1.5-vault-core-ios-port/evidence/simulator-memory-walk-2026-10-17.txt`） |
| MAN-013 | iOS mlock 语义一行探针（cross_verification C6）：真机对测试页调用 mlock/munlock，记录返回值与 errno | 记录 mlock 在 iOS 沙盒中的实际行为（成功或 EPERM/ENOMEM 均可接受）；结果仅用于 **[C]** 标注复核，无功能依赖 | TC-VC-009 | iPhone（实体真机） | ☐ 待走查（T1.5-H1：无**合格**真机——iPad mini 4 / iOS 15.8.8 低于 iOS 18.0 下限）。**2026-10-17 载体已就位**：`MlockProbeTests`（页对齐 `posix_memalign` 页 → `mlock`/`munlock`，原始返回值 + errno 作为 xcresult attachment 归档，模拟器 19/19 通过时记录 `page 16 KiB, aligned, mlock → 0, munlock → 0`）；**该模拟器结果不构成 MAN-013 结论**（真机沙盒可能返回 EPERM/ENOMEM，行内均接受），真机一跑即可补机型/OS/结论。证据：`specs/features/2026-10-16-T1.5-vault-core-ios-port/evidence/simulator-mlock-probe-2026-10-17.txt`） |
| MAN-014 | Android 路由点走（T2.1 UI shell）：dev build 上依次走 onboarding（`/`）→ home → receive → send，含返回与未匹配路由（+not-found） | 四路由无 crash、无红屏；每页「Not configured」占位如 spec；暗色令牌渲染正常 | TC-UI-001 子集 | Pixel 6a / Android 17 | ☑ **通过** 2026-10-19（Pixel 6a / Android 17；owner 点走通过；自动侧 deep-link + UI dump + 截图见 `specs/features/2026-10-18-T2.1-mobile-ui-shell/evidence/`） |
| MAN-015 | Web 浏览器路由走查（T2.1 UI shell）：桌面 Chrome/Safari 走查 `/`、`/home`、`/receive`、`/send` | 四路由渲染正常、console 无报错；写操作入口呈「扫码回手机」降级由后续条目承载（本条目仅 shell） | TC-UI-001/009 子集 | 桌面浏览器 ×1 | ☑ **通过** 2026-10-19（桌面浏览器；owner 走查通过；自动侧静态导出 curl 四路由 200 见 `specs/features/2026-10-18-T2.1-mobile-ui-shell/validation.md` Task 6） |

| MAN-016 | 视觉对齐走查（T2.1a）：原仓库 APK（com.nexvault.wallet，debug，2026-10-18 构建，`/home/superguo/Projects/NexVault/app/build/outputs/apk/debug/app-debug.apk`）与 RN shell 在同一 Pixel 6a 上并排截图对照——5 tab（Home/History/DApp/NFT/Settings）+ onboarding + receive/send；核对 tokens（色板/Inter/形状/间距）、TopBar 透明居中（Home 无 bar）、卡片 1dp 描边无阴影、按钮 52/输入 56 | 逐屏并排一致，owner 逐条裁决；偏差逐条登记为 bug 或 Handoff（禁静默） | TC-UI-007（人工侧） | Pixel 6a / Android 17（两侧同机） | ☑ **通过** 2026-10-20（owner 复核 “All verified”；参考侧 = 原仓库 debug APK 同机截图 `evidence/orig-*.png`，shell = release APK `evidence/shell-release-*.png`）。**复核注**：参考 app 已初始化钱包 → 仅 Unlock 可见，**Welcome 参考截图不可得**（未清空其数据）；onboarding 行以代码对照 `WelcomeScreen.kt:77-125` 承载，其余 5 tab + unlock 有同机参考截图 |

| MAN-017 | 余额只读走查（T2.2）：Web 桌面浏览器 + Android 真机（release APK）分别输入两个 mainnet 锚点地址（`0x9858EfFD232B4033E47d90003D41EC34EcaEda94`、`0xd8dA6BF26964aF9D7eEd9e03E53415D37aA96045`），核对 Total Balance、区块号 caption、链切换与空态 | 两锚点均返回真实余额（anchor1 = 0 ETH 属实、anchor2 ≈ 5.77 ETH）且与 RPC curl 交叉计算一致；区块号与 RPC 同轮次；无地址时 "Not configured"；链 pill 可切 sepolia 并重查询 | TC-W3A-013、TC-UI-010 | Pixel 6a / Android 17 + 桌面浏览器 | ☐ 待走查（**review round 1 M1 更正**：Web 桌面浏览器人工侧未完成；Android 真机侧 agent 已完成——五张截图 + 四份 UI dump（`evidence/dump-home-*.xml`，含 Tokens 行文本）；Web 自动侧 curl/serve 完成。证据：`specs/features/2026-10-20-T2.2-web3-readonly/evidence/`） |

## 规则

1. 新增手工用例追加 MAN 编号（顺序分配，不复用），删除须注明原因。
2. 每条必须关联至少一个 specs/ 内的 TC 编号；无对应 TC 时先补 spec（=修宪）。
3. 走查结论只能为「通过 / 失败（附 issue）/ 阻塞（附原因）」，禁止无证据勾「通过」。
4. 走查环境以 spec: dev-environment §2 的真机基线为准；模拟器/Expo Go 结果不计入。
5. MAN 用例与 TC 用例互补：能自动化的进 TC + CI，本表只保留必须人工/真机的部分。
6. 阶段 1（三天原型）期间，G1–G3 门槛所需的 MAN-001/004/005 为最低走查集，其余可顺延至阶段 2 首轮。
