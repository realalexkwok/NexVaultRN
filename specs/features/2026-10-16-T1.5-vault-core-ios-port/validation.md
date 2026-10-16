# T1.5 — vault-core iOS four-method port (validation)

> Feature spec (validation). Branch: feature/T1.5-vault-core-ios-port.
> Parent: plans/3-day-implementation-plan.md Day 2 T1.5（关键路径：iOS Swift 薄封装移植）.
> Skill: feature-dir SKILL.md, sha256 2b0b26dc9b8540fbe35664d8e512f44863a7c5b4fa10e2b109f4ef4e00968bb8.
> Session status: **planning round 2026-10-16** wrote this skeleton. Every task below is
> filled with command + exit code + observed output during the implementation session (after
> GATE-MODEL-SWITCH). No [x] without evidence (B.4-3). Closure and push happen only on the
> owner's explicit calls (Q6).

## Gate checklist

| Gate | Status | Evidence |
| --- | --- | --- |
| GATE-MODEL-SWITCH | [ ] pending | Owner switches the model; recorded here before Task 0 |
| GATE-R4-SWIFTSECP | [x] owner-approved 2026-10-16 (planning round Q1) | swift-secp256k1 exact 0.23.2 via CocoaPods; amendments + approval record land in Task 1 before any podspec/app.json change |
| GATE-KECCAK | [x] owner-confirmed 2026-10-16 (planning round Q2) | self-written Keccak-f[1600]; two pinned vectors gate it |

## Task 0 — Preflight, R4 evidence, API inspection

Status: [ ]

```
# (implementation session fills: git baseline, Mac reachability probe + pinned iPhone UDID,
#  R4 evidence re-verification (trunk podspec / GitHub release / sha256), P256K API inspection
#  notes with consulted docs, oracle transcription provenance, autolinking output)
```

## Task 1 — R4 amendments + deployment target

Status: [ ]

```
# (implementation session fills: specs/tech-stack.md + doc/dependencies.md §6c diffs,
#  apps/mobile/app.json deploymentTarget diff, R3 note)
```

## Task 2 — Swift cores

Status: [ ]

```
# (implementation session fills: file diffs for Keccak/Bip39/Bip32/Ecdsa/VaultStore.swift,
#  compile evidence, zeroization checklist per file)
```

## Task 3 — Module wiring

Status: [ ]

```
# (implementation session fills: VaultCoreModule.swift + podspec diffs, wordlist copy sha256,
#  module registration evidence)
```

## Task 4 — XCTest suites

Status: [ ]

```
# xcodebuild test -scheme … -destination 'platform=iOS Simulator,…'   (auxiliary)
# xcodebuild test -scheme … -destination 'platform=iOS,id=<UDID>'      (authoritative)
# observed: vector suite results, counts, xcresult paths
```

## Task 5 — Manual side (MAN-012 / MAN-013)

Status: [ ]

```
# (implementation session fills: MAN-012 memory-walk record (机型+OS+结论+PSS curve),
#  MAN-013 mlock probe errno/result, registration rows in pending-manual-test-cases.md)
```

## Task 6 — Verification, evidence, handoffs

Status: [ ]

```
# (implementation session fills: zero TS diff, tsc exit 0, print/NSLog/os_log grep exit 1,
#  zeroization review table with [C] rows, evidence/ copies, final handoff list)
```

## Task 7 — Board + git (owner-gated)

Status: [ ]

```
# (runs ONLY after the owner's explicit close call: board row, git merge --no-ff,
#  branch deletion; push ONLY on the owner's explicit go-ahead)
```

## Status board

| Task | Status |
| --- | --- |
| Planning round (owner decisions + Q/A) | [x] verified 2026-10-16 — D1–D6 dispositions and Q1–Q6 answers recorded in requirements.md |
| Task 0 preflight | [ ] |
| Task 1 R4 amendments + deployment target | [ ] |
| Task 2 Swift cores | [ ] |
| Task 3 module wiring | [ ] |
| Task 4 XCTest suites | [ ] |
| Task 5 MAN-012/013 | [ ] |
| Task 6 verification + handoffs | [ ] |
| Task 7 board + git (owner-gated) | [ ] |
| T1.5 overall | [ ] |
