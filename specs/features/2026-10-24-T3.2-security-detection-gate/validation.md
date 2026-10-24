# T3.2 — validation（security detection + fail-closed gate）

> Feature：`specs/features/2026-10-24-T3.2-security-detection-gate` | Date：2026-10-24
> 证据纪律：AGENTS.md B.4（命令原文 + 观察结果 + 构件路径）；`[x]` 双侧齐备，`[~]` 已实现未验证。
> 分支：`feature/T3.2-security-detection-gate`（规划轮提交 `c90b418`；main `3862902`）
> 状态：**规划轮完成 → STOP（GATE-MODEL-SWITCH）**；实施 = V4.1-Flash 会话，Tasks 1–8。

## Task 0 — planning round（preflight + 三件套 + commit）

Status：`[x]`

```bash
git status --short                          # 干净（无输出）
git switch -c feature/T3.2-security-detection-gate   # Switched to a new branch 'feature/T3.2-security-detection-gate'
mkdir -p specs/features/2026-10-24-T3.2-security-detection-gate
cp .agents/skills/nexvault-t3-2-security-detection-gate/SKILL.md specs/features/2026-10-24-T3.2-security-detection-gate/SKILL.md
# write requirements.md / plan.md / validation.md（本文件）
git add specs/features/2026-10-24-T3.2-security-detection-gate
git commit -m 'docs(T3.2): planning round - requirements/plan/validation + SKILL copy (GATE-MODEL-SWITCH pending)'
```

观察：
- main = `3862902`（chore: install agent skill (T3.2 security detection + fail-closed gate)）；分支切出干净。
- Preflight 事实（2026-10-24 实证，详见 plan.md §2）：debug APK 无内嵌 bundle（`unzip -l apps/mobile/android/app/build/outputs/apk/debug/app-debug.apk` 仅 assets/app.config → BundleSelfCheck evaluated=false 路径成立）；autolinking 工程名 :vault-core/:security-android（T3.1 已证）；minSdk=24 → GET_SIGNING_CERTIFICATES 需 API≥28 降级分支；本机无 Xcode；JAVA_HOME=/home/superguo/tools/jdk-17；apps/mobile/android 为 git 未跟踪生成目录 → 构建定制走 config plugin（CNG）；plugins/ 已有 withVaultCoreTests.js 先例。
- Owner decisions：两轮 ask_user_question（2026-10-24 12:01 120s / 12:21 600s）超时 → D1–D5 按 Recommended 默认落盘（requirements §2）；计划经 exit_plan_mode **approved**（2026-10-24）。owner 可在 GATE-MODEL-SWITCH 前推翻任一裁定。
- 计划批准后未写任何生产代码（GATE-MODEL-SWITCH：owner 指令「Stop on implementation to allow me to change the model to V4.1-Flash」）。

## Task 1 — 检测核心（modules/security-android）

Status：`[ ]`（V4.1-Flash 实施轮；开工前复核 D1–D5）

门禁：`cd apps/mobile/android && JAVA_HOME=/home/superguo/tools/jdk-17 ./gradlew :security-android:testDebugUnitTest --rerun` → 12 旧 + TC-T32-001..005 全绿（XML 归档 evidence/）。

## Task 2 — vault-core 耦合 + JS 面（H-T3.1-2）

Status：`[ ]`

## Task 3 — iOS 对应物（撰写侧 [~]，按 D1/D4）

Status：`[ ]`（含 security-ios.md §2.3 自研向量修订 + MAC-HANDOFF.md 更新）

## Task 4 — App 侧（config plugin + 首帧后 fast 扫描 + settings 读数）

Status：`[ ]`

## Task 5 — 测试（JVM / instrumentation / XCTest 撰写）

Status：`[ ]`

## Task 6 — 门禁与演示（干净机 L0 + Magisk 拒绝，MAN-021/004 + 录屏）

Status：`[ ]`

门禁命令块（plan.md Task 6）：tsc form-1 / form-2 / modules gate；R1 grep（B.5 原文）；R5 grep（Log./println/print(/NSLog，排除 Test）；`git diff` 依赖面审计（仅 project(':security-android') + podspec 依赖，零未批准坐标）；jest 回归 25/25。

## Task 7 — 评审循环 + 收尾（Part A 板行 + B.3 git）

Status：`[ ]`

## Task 8 — 风险降级预案

见 plan.md Task 8：config plugin 任务图不可行 → D3(b) 两段脚本（记录）；Magisk 机未就位 → D2(b) 口径（TC-T32-007 证据 + MAN-004 挂起 handoff）；评审 BLOCKER/MAJOR → review-fix 纪律；设备离线 → D2(b)/待设备 handoff。