# T1.4 — vault-core Android signDigest/destroy (validation)

> Feature spec (validation). Branch: `feature/T1.4-vault-core-android-sign-digest`.
> Parent: `plans/3-day-implementation-plan.md` Day 1 T1.4（关键路径，四方法闭环 G1）.
> Skill: feature-dir `SKILL.md`, sha256
> `b0dbe67a1427adbf1a6a0add79dbbd049d3b045b484729e964666cbb7ea1f9b2`.
> Session status: **planning round 2026-10-16** wrote this skeleton. Every task below is
> filled with command + exit code + observed output during the implementation session (after
> `GATE-MODEL-SWITCH`, V4.1-Flash). No `[x]` without evidence (B.4-3).

## Gate checklist

| Gate | Status | Evidence |
| --- | --- | --- |
| GATE-MODEL-SWITCH | `[ ]` pending | Owner switches the model to V4.1-Flash; recorded here before Task 0 |
| No-new-dependency (R4) | `[ ]` pending | `build.gradle` diff empty in Task 0/Task 8 |
| H9-first ordering | `[ ]` pending | Task 5 diff recorded before the Task 6 suites ran |

## Task 0 — Preflight and baseline recording

Status: `[ ]`

```
# (implementation session fills: git status/branch, autolinking, jar greps + sha256,
#  RFC 6979 fetch + sha256 + A.2.5 trap note, SigningKeyProvider.kt read (reference-only),
#  noble/viem versions, command + exit code + observed output)
```

## Task 1 — Spec check (no amendment expected)

Status: `[ ]`

```
# (implementation session fills: the spec: vault-core §2.1/§2.2/§2.4 citation table and the
#  git diff evidence that specs/vault-core.md is unchanged)
```

## Task 2 — TS interface

Status: `[ ]`

```
# (implementation session fills: git diff of VaultCoreModule.ts + index.ts,
#  /tmp harness outputs for STUB_MODE=missing/present + digest-31 fast-fail,
#  grep -n 'signDigest|destroy' result)
```

## Task 3 — Ecdsa.kt core

Status: `[ ]`

```
# (implementation session fills: file diff, zeroization checklist for DRBG/sign path,
#  compile evidence, BigInteger [C] caveat note)
```

## Task 4 — Handle-store extraction + module wiring

Status: `[ ]`

```
# (implementation session fills: HandleStore.kt diff, VaultCoreModule.kt diff,
#  VaultExceptions.kt diff, race-note recording)
```

## Task 5 — H9 zeroization patch

Status: `[ ]`

```
# (implementation session fills: exact diff of Bip32.kt — data.fill(0) in finally +
#  EIP-55 temp wipe; both suites then run on this patched revision)
```

## Task 6 — Tests

### 6.1 JVM gate

Status: `[ ]`

```
# ./gradlew :vault-core:testDebugUnitTest --rerun
# observed: BUILD SUCCESSFUL, exit 0; Bip39Test 16 + Bip32Test 10 + EcdsaTest N +
# HandleStoreTest M, 0 failures; XML paths recorded
```

### 6.2 Device suite (TC-VC-025)

Status: `[ ]`

```
# ANDROID_SERIAL=<serial> ./gradlew :vault-core:connectedDebugAndroidTest
# observed: Starting N tests on Pixel 6a - 17, Finished N tests on Pixel 6a - 17,
# BUILD SUCCESSFUL in ...; XML tests="N" failures="0"; evidence copied to feature evidence/
```

## Task 7 — E2E cross-check (throwaway /tmp)

Status: `[ ]`

```
# (implementation session fills: full t14-e2e.mjs script text + node output —
#  noble compact equality PASS, viem recoverPublicKey → 0x9858EfFD...Eda94,
#  pinned r/s/recid transcribed into TC-VC-022)
```

## Task 8 — Verification, MAN-011, evidence, Handoff

Status: `[ ]`

```
# (implementation session fills: zero npm drift diff, R1/R5/Log greps, tsc exit 0,
#  zeroization review table, evidence/ copies, MAN-011 registration row,
#  final T1.4 Handoff list T1.4-H1..H5)
```

## Task 9 — Board + git closure + push

Status: `[ ]`

```
# (implementation session fills: AGENTS.md Part A board line text, commit hashes,
#  git merge --no-ff -F /tmp/t14-merge-msg.txt output, branch deletion,
#  git push origin main output)
```

## Status board

| Task | Status |
| --- | --- |
| Planning round (owner decisions + Q/A) | `[x]` verified 2026-10-16 — D1–D10 dispositions and Q1–Q6 answers recorded in requirements.md |
| Task 0 preflight | `[ ]` |
| Task 1 spec check | `[ ]` |
| Task 2 TS interface | `[ ]` |
| Task 3 Ecdsa.kt core | `[ ]` |
| Task 4 store extraction + wiring | `[ ]` |
| Task 5 H9 zeroization patch | `[ ]` |
| Task 6 JVM gate | `[ ]` |
| Task 6 device suite | `[ ]` |
| Task 7 E2E cross-check | `[ ]` |
| Task 8 verification + MAN-011 + Handoff | `[ ]` |
| Task 9 board + git closure + push | `[ ]` |
| T1.4 overall | `[ ]` |
