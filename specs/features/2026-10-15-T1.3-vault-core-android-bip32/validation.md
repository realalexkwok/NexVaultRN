# T1.3 — vault-core Android BIP32/BIP44 deriveAddress (validation)

> Feature spec (validation record). Branch: `feature/T1.3-vault-core-android-bip32`.
> Status: **planning round complete 2026-10-15; implementation blocked on GATE-MODEL-SWITCH
> (owner switches model to V4.1-Flash) and GATE-R4-BC (owner approves bcprov-jdk18on:1.86).**
> No task is marked `[x]` without command + observed output below.
> Status scale (AGENTS.md B.4): `[x]` verified (both sides with evidence) · `[~]` implemented
> but unverified · `[ ]` not started.

## Planning-round evidence (2026-10-15, read-only / record-only actions)

### P0.1 — Constitution inputs

| Item | Observed result |
| --- | --- |
| Read `AGENTS.md` (Part A board, B.1–B.7, C.1/C.2) | T1.2 closed and merged to `main` (`6b9fec4` + board record `24a35e6`); roadmap order makes T1.3 the next item |
| Read `specs/roadmap.md`, `plans/3-day-implementation-plan.md` T1.3 row | T1.3 = Android BIP32 (secp256k1, packaged BouncyCastle) + BIP44 → `deriveAddress` (EVM keccak256(pub)[12:]); acceptance: BIP32 official vectors green + known-mnemonic address matches the original repo record |
| Read `specs/tech-stack.md` | **No BouncyCastle row** → R4 amendment is a hard prerequisite (GATE-R4-BC); `@noble/@scure` are approved as devDependency-only (vector comparison) |
| Read `specs/vault-core.md` | §2.1/§2.2/§2.3 still declare `VaultHandle = number` and `handle: Int` (H2 conflict); §2.4 still splits `E_VAULT_CHECKSUM_MISMATCH` (H2 conflict); AC-2/TC-VC-003 pins the canonical address |
| Read T1.2 three files + `specs/pending-manual-test-cases.md` | T1.2 Handoff H1/H2/H3/H6/H7 are the inputs adjudicated in `requirements.md` (owner dispositions D1–D4) |

### P0.2 — Original-repo anchor (GATE-REFERENCE-ORIGINAL-REPO)

| Source read | Observed result |
| --- | --- |
| `/home/superguo/Projects/NexVault/core/core-security/src/test/java/com/nexvault/wallet/core/security/wallet/HDKeyManagerTest.kt` | line 16 `// TC-SEC-005: the canonical BIP-39 vector derives the known BIP-44 address.`; line 24 `assertEquals("0x9858EfFD232B4033E47d90003D41EC34EcaEda94", address)` — the canonical anchor |
| `/home/superguo/Projects/NexVault/core/core-security/src/main/java/com/nexvault/wallet/core/security/wallet/HDKeyManager.kt` | web3j `Bip32ECKeyPair` implementation — **reference-only** (this repo packages BouncyCastle per `specs/vault-core.md` §2.2/§6; web3j would be a second R4 dependency) |
| Second-address scan | The original repo asserts distinctness/validity only — **no recorded value** for `m/44'/60'/0'/0/1` → pinned at implementation via `@scure/bip32` in `/tmp` (Q4) |

### P0.3 — Skill identity

| Command | Exit | Observed result |
| --- | --- | --- |
| `sha256sum .agents/skills/nexvault-t1-3-vault-core-android-bip32/SKILL.md specs/features/2026-10-15-T1.3-vault-core-android-bip32/SKILL.md` | 0 | both `1c9aec107f717a765e17a3c1da06fa3d62da0e20500c45b78842a2ca30e07ad7` (identical; the committed feature-dir copy is authoritative for this round) |

### P0.4 — Ask-user round (C.1)

Q1–Q6 asked via `ask_user_question` and **all confirmed by the owner** (2026-10-15):
Q1 H2 amendment scope · Q2 canonical anchor `0x9858EfFD…Eda94` · Q3 R4 proposal wording
(`bcprov-jdk18on:1.86`) · Q4 `@scure/bip32` /tmp cross-check · Q5 Pixel 6a + `ANDROID_SERIAL`
pin · Q6 GATE-MODEL-SWITCH stop. Answers are transcribed in `requirements.md` Owner decisions.

### P0.5 — Feature branch and autolinking

| Command | Exit | Observed result |
| --- | --- | --- |
| `git switch -c feature/T1.3-vault-core-android-bip32` (from `main` `2dbe7a5`) | 0 | `Switched to a new branch 'feature/T1.3-vault-core-android-bip32'` |
| `git branch --show-current` | 0 | `feature/T1.3-vault-core-android-bip32` |
| `cd apps/mobile && npx expo-modules-autolinking search --platform android --json` | 0 | `{"name":"vault-core","path":"/home/superguo/Projects/NexVaultRN/modules/vault-core",…}` — resolves |

### P0.6 — Planning-round commit

| Command | Exit | Observed result |
| --- | --- | --- |
| `git add specs/features/2026-10-15-T1.3-vault-core-android-bip32 && git commit -m "docs(T1.3): planning-round specs — deriveAddress (BIP32/BIP44)"` | 0 | `[feature/T1.3-vault-core-android-bip32 10fbb64] 3 files changed, 632 insertions(+)`; `create mode 100644 …/{plan,requirements,validation}.md` |
| `git status --short --branch` | 0 | `## feature/T1.3-vault-core-android-bip32`, worktree clean — **zero code/dependency changes** in this round (R4 gate holds) |

## Gate checklist (blocking)

| Gate | Status | Action |
| --- | --- | --- |
| GATE-MODEL-SWITCH | **[ ] pending** | Owner switches the model to V4.1-Flash before any code write (FR-12); evidence recorded in Task 0 |
| GATE-R4-BC | **[ ] pending** | Proposal confirmed (Q3); owner approval + amendment edits happen in Task 3 before any BC usage |

## Implementation tasks (skeleton — every task below stays `[ ]` until its evidence lands)

| Task | Status | Blocked by |
| --- | --- | --- |
| Task 0 preflight (baseline, branch, autolinking, `bip-0032.mediawiki` sha256 + independent vector-1 verification, GATE-MODEL-SWITCH record) | `[ ]` | GATE-MODEL-SWITCH |
| Task 1 H2 `specs/vault-core.md` amendment (five edits, diff recorded) | `[ ]` | GATE-MODEL-SWITCH |
| Task 2 TS interface (`deriveAddress` declaration + guarded wrapper + /tmp harness run) | `[ ]` | GATE-MODEL-SWITCH |
| Task 3 R4 gate: BC evidence re-verify → owner approval → `tech-stack.md`/`dependencies.md`/`build.gradle` | `[ ]` | GATE-MODEL-SWITCH + owner approval |
| Task 4 `Bip32.kt` core | `[ ]` | GATE-R4-BC |
| Task 5 module wiring + `VaultExceptions.kt` | `[ ]` | GATE-R4-BC |
| Task 6 tests: `Bip32Test.kt` (BIP-32 vectors 1–5, canonical + second address, keccak, ERC-55, negatives) + `VaultCoreModuleTest.kt` H3 + Gradle runs | `[ ]` | GATE-R4-BC + device (Q5) |
| Task 7 verification (zero-npm-drift diff, R1/R5 greps, tsc, zeroization review) + Handoff | `[ ]` | Tasks 4–6 |

## Evidence templates (filled during implementation)

### BIP-32 vector pass/fail table

| Vector | Chains | xprv/xpub match | Result |
| --- | --- | --- | --- |
| V1 … V5 | per `requirements.md` | — | `[ ]` |

### Zeroization review (mandatory, file:line)

| Path | Evidence | Verdict |
| --- | --- | --- |
| seed copy (module) / `x‖y` pubkey buffer / keccak buffers / BC `BigInteger` intermediates [C] | — | `[ ]` |

### R4 approval record

Owner · date · exact coordinate `org.bouncycastle:bcprov-jdk18on:1.86` · evidence links:
(empty until Task 3)

## Handoff list (adjudicated by the next planning round)

| # | Item | Status 2026-10-15 | Disposal |
| --- | --- | --- | --- |
| T1.3-H1 | `destroy` absent (T1.4) | `[ ]` open | Implement in T1.4 |
| T1.3-H2 | iOS `deriveAddress` port (T1.5) | `[ ]` open | Replan to T1.5 |
| T1.3-H3 | Device instrumentation fallback | `[ ]` open | Only if no device at implementation; rerun pinned `connectedDebugAndroidTest`; JVM gate stays mandatory |
| T1.3-H4 | BIP-32 strings transcribed after Step-0 verification | `[ ]` open | Keep transcription evidence in this file |
| T1.3-H5 | JS wrapper evidence via /tmp harness | `[ ]` open | T1.1 closure absorbs it as committed jest tests |
| T1.3-H6 | BC pin 1.86 carried to T1.4 | `[ ]` open | Replan to T1.4 |

## Status board

| Task | Status |
| --- | --- |
| Planning round (P0.1–P0.6) | `[x]` verified — evidence above |
| Tasks 0–7 | `[ ]` blocked on GATE-MODEL-SWITCH + GATE-R4-BC |
| T1.3 overall | **`[ ]` not started (planning complete)** |
