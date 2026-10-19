# T2.1a — mobile UI visual parity — validation

> Status: 规划轮完成（2026-10-19）；实施（Task 2–8）由下一会话执行（模型切换点，见 plan.md §11）
> 三档制（B.4）：本文件所有 `[x]` 均须命令原文 + 观察结果 + 构件绝对路径；双侧齐备。

## 规划轮（2026-10-19，owner 裁定）

| 步骤 | 内容 | 结果 |
|---|---|---|
| 访谈 | `ask_user_question` ×5（D1–D5） | 全部答复批准：D1 expo-font 行 / D2 §2.3 令牌块 / D3 §2.1 (tabs) / D4 H3–H9 / D5 APK 截图参考侧（Q/A 原文见 requirements.md §Owner decisions） |
| GATE-R4-EXPOFONT | tech-stack §2 行 + dependencies §6d 行 | owner 批准（D1）；行文本见 Task 1 |
| ui-wallet 修宪 | §2.1 路由表 + §2.3 令牌块 | owner 批准（D2/D3）；修订文本见 plan.md §4.1/§4.2 |
| Handoff 裁定 | T2.1-H3…H9 | D4 批准；T2.1 validation.md 行已更新（Task 1） |
| MAN-016 登记 | pending-manual-test-cases.md | 已登记（Task 1） |
| 分支 | feature/T2.1a-mobile-ui-parity rebase 到 main e2ac943 | Task 0 记录 |

## Task 0 — 分支（先于一切落盘；owner 硬性要求）

命令与观察结果：

- `git status --short --branch` → `## main...origin/main [ahead 1]`（工作树干净）
- `git switch feature/T2.1a-mobile-ui-parity` → `Switched to branch 'feature/T2.1a-mobile-ui-parity'`（exit 0）
- `git rebase main` → 冲突仅 AGENTS.md（公告板记录区）；plans/roadmap 自动合并（exit 1 = 冲突）
- 裁决：公告板两套记录**并存**（T2.1 复核重关/代码评审轮 + 2026-10-19 治理修订行），按日期倒序；冲突标记清零（`grep -n '<<<<<<<\|>>>>>>>\|^=======' AGENTS.md` → NO_MARKERS）
- `GIT_EDITOR=true git rebase --continue` → `Successfully rebased and updated refs/heads/feature/T2.1a-mobile-ui-parity.`（新 tip `63c73c5`，基线 `e2ac943`）
- 复核：`specs/roadmap.md` L20 T2.1a 修宪子弹 ✓；`plans/3-day-implementation-plan.md` L205 v1.3 ✓；AGENTS.md L25 治理行 ✓；`git status --short --branch` 干净 ✓

结论：**分支先于任何文件落盘成立**（本 feature 全部文件写于 rebase 之后；治理提交 `63c73c5` 位于 main `e2ac943` 之上）。

## Task 1 — 规划轮产物（2026-10-19）

| 产物 | 路径 |
|---|---|
| requirements / plan / SKILL（provenance 副本）/ validation | `specs/features/2026-10-19-T2.1a-mobile-ui-parity/` |
| ui-wallet §2.1/§2.3 修订 | `specs/ui-wallet.md`（D2/D3 文本见 plan.md §4） |
| expo-font 行 + expo-router 括注同步 | `specs/tech-stack.md` §2 + `doc/dependencies.md` §6d（D1/H9） |
| MAN-016 登记 | `specs/pending-manual-test-cases.md` |
| T2.1-H3…H9 行更新 | `specs/features/2026-10-18-T2.1-mobile-ui-shell/validation.md` §Handoff 清单 |

SKILL provenance：.agents 原件 sha256 `67892de8792e1e40815a198e81ebd5a213621b1b478f728af3ab286227b66e80`（提交 e2ac943）；feature 目录副本附 provenance 头（T2.1 模式）。

提交：`docs(T2.1a): planning round — feature three-piece + ui-wallet §2.1/§2.3 + expo-font row + MAN-016 + H3–H9 dispositions`；提交号以 `git log --oneline -1` 为准（本文件写于同次提交，提交号在后续提交补记，不阻塞）。

## Task 2 — expo-font + Inter 资产 [ ] 未开始（模型切换后执行）

- `cd apps/mobile && npx expo install expo-font`（D1 已批准；→ ~57.0.4）；记录 `git diff -- package.json pnpm-lock.yaml` = 仅 expo-font 行。
- 复制 4 TTF + OFL 许可证（记录源路径）→ `apps/mobile/assets/fonts/`；`useFonts` 挂载；web 端 FontFace 验证。

## Task 3 — tokens / tailwind.config / global.css [ ]

## Task 4 — 原语组件（NexVaultCard/Button/TextField/TopBar） [ ]

## Task 5 — chrome + (tabs) 导航 + 页面重皮 [ ]

## Task 6 — 自动门禁 [ ]（tsc / export / R1 / R5 / R7 / 漂移 / doctor / drift audit）

## Task 7 — 路由 smoke（Android + Web）+ MAN-016 + Handoff + 公告板 + git 关闭 [ ]

## 复现命令（同 T2.1 validation §复现命令 形态；实施期按分支实际路径补全）

app 侧（`apps/mobile`）：

```bash
pnpm --filter mobile exec tsc --noEmit
npx expo export --platform web -o /tmp/t21a-web
```

## Handoff 清单（T2.2 / T4.x 规划轮裁定；实施期逐条补充）

| 事项 | 递延理由 | 建议处置 | 关联 AC/TC |
|---|---|---|---|
| （实施期补充：真实数据、polyfill、其余路由表页、motion、web 字体回退等） | — | — | — |
