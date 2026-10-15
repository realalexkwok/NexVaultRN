# T1.3 — vault-core Android BIP32/BIP44 deriveAddress (validation)

> Feature spec (validation record). Branch: `feature/T1.3-vault-core-android-bip32`.
> Status: **in progress 2026-10-15** — planning round + implementation Tasks 0–2 verified;
> **Task 3 (GATE-R4-BC) awaiting owner approval**; Tasks 4–7 blocked on that approval.
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
| Read `specs/vault-core.md` | §2.1/§2.2/§2.3 declared `VaultHandle = number` and `handle: Int` (H2 conflict); §2.4 split `E_VAULT_CHECKSUM_MISMATCH` (H2 conflict); AC-2/TC-VC-003 pins the canonical address |
| Read T1.2 three files + `specs/pending-manual-test-cases.md` | T1.2 Handoff H1/H2/H3/H6/H7 are the inputs adjudicated in `requirements.md` (owner dispositions D1–D4) |

### P0.2 — Original-repo anchor (GATE-REFERENCE-ORIGINAL-REPO)

| Source read | Observed result |
| --- | --- |
| `/home/superguo/Projects/NexVault/core/core-security/src/test/java/com/nexvault/wallet/core/security/wallet/HDKeyManagerTest.kt` | line 16 `// TC-SEC-005: the canonical BIP-39 vector derives the known BIP-44 address.`; line 24 `assertEquals("0x9858EfFD232B4033E47d90003D41EC34EcaEda94", address)` — the canonical anchor |
| `/home/superguo/Projects/NexVault/core/core-security/src/main/java/com/nexvault/wallet/core/security/wallet/HDKeyManager.kt` | web3j `Bip32ECKeyPair` implementation — **reference-only** (this repo packages BouncyCastle per `specs/vault-core.md` §2.2/§6; web3j would be a second R4 dependency) |
| Second-address scan | The original repo asserts distinctness/validity only — **no recorded value** for `m/44'/60'/0'/0/1` → pinned at implementation via an independent script (Q4) |

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

### P0.6 — Planning-round commits

| Command | Exit | Observed result |
| --- | --- | --- |
| `git add specs/features/2026-10-15-T1.3-vault-core-android-bip32 && git commit -m "docs(T1.3): planning-round specs — deriveAddress (BIP32/BIP44)"` | 0 | `[feature/T1.3-vault-core-android-bip32 10fbb64] 3 files changed, 632 insertions(+)` |
| `git commit -m "docs(T1.3): record planning commit evidence (P0.6)"` | 0 | `335b29f` (this file's P0.6 addendum) |

## Implementation evidence (2026-10-15)

### Task 0 — Preflight, vector provenance, spec correction

Status: `[x]`

```bash
git status --short --branch          # ## feature/T1.3-vault-core-android-bip32 (clean)
git log --oneline -2                 # 335b29f / 10fbb64
cd apps/mobile && npx expo-modules-autolinking search --platform android --json | grep vault-core
curl -fsSL https://raw.githubusercontent.com/bitcoin/bips/master/bip-0032.mediawiki -o /tmp/bip32.mediawiki
sha256sum /tmp/bip32.mediawiki       # e5e00a8289db2f681052cf24a745320afc225e66b25d1e489a7c884d2fc7f11f
python3 /tmp/bip32_verify.py         # independent BIP32 + keccak + secp256k1 implementation
```

Observed:
- `GATE-MODEL-SWITCH`: owner switched the model; this implementation session runs the
  V4.1-Flash model (session context) and the owner issued `Continue` — recorded as satisfied.
- `bip-0032.mediawiki` fetched (315 lines); sha256 as above; vectors at lines 218–309.
- Independent Python verification (own HMAC-SHA512/sha256/base58/HASH160, own secp256k1 point
  math, own Keccak-256) reproduced **every** listed chain of vectors 1–4:
  `V1 6 chains PASS · V2 6 chains PASS · V3 2 chains PASS · V4 3 chains PASS`.
- Same script: `keccak256("")` sanity OK, the four ERC-55 vectors OK,
  `abandon×23 about` @ `m/44'/60'/0'/0/0` → `0x9858EfFD232B4033E47d90003D41EC34EcaEda94`
  (**independent MATCH**), BIP-39 seed `5eb00bbd…9e38e4` (matches the T1.2 locked value).
- Second address pinned independently: `m/44'/60'/0'/0/1` → `0x6Fac4D18c912343BF86fa7049364Dd4E424Ab9C0`.
- **Spec correction (recorded):** the planning draft over-listed V3/V4 chains and mis-described
  V5. The fetched revision lists V3 = 2 chains, V4 = 3 chains, and V5 = **16 malformed extended
  keys** (serialization validity, not derivation). `requirements.md` + `plan.md` were corrected;
  V5 is now explicitly outside the T1.3 gate (Handoff T1.3-H7) rather than a fake gate.

### Task 1 — H2 spec amendment (`specs/vault-core.md`)

Status: `[x]`

```bash
git diff --stat -- specs/vault-core.md
```

Observed: `1 file changed, 17 insertions(+), 13 deletions(-)` — six edits:
1. header amendment note (date, owner-approved direction, evidence pointer);
2. §2.1 `VaultHandle` → `string & { readonly __brand: 'VaultHandle' }` (+ rationale comment);
3. §2.2 Kotlin: `importMnemonic … -> String`, `deriveAddress/signDigest/destroy handle: String`;
4. §2.3 iOS: same `Int → String` signature update (doc-only; implementation stays T1.5);
5. §2.4: `E_VAULT_CHECKSUM_MISMATCH` row deleted, folded into `E_VAULT_INVALID_MNEMONIC`;
6. TC-VC-002 `Then` clause now expects only `E_VAULT_INVALID_MNEMONIC`.

### Task 2 — TS interface and wrapper

Status: `[x]`

```bash
git diff -- modules/vault-core/src/VaultCoreModule.ts modules/vault-core/index.ts
cd apps/mobile && npx --no-install tsc --noEmit   # app graph (exit 0)
/home/superguo/Projects/NexVaultRN/apps/mobile/node_modules/.bin/tsc -p /tmp/t13-typecheck   # module files vs real Expo types (exit 0)
/home/superguo/Projects/NexVaultRN/apps/mobile/node_modules/.bin/tsc -p /tmp/t13-guard       # harness copy + stub (exit 0)
STUB_MODE=missing node /tmp/t13-guard/run.js
STUB_MODE=present node /tmp/t13-guard/run.js
```

Observed:
- `VaultCoreModule.ts` declares `deriveAddress(handle: string, path: string): Promise<string>`
  next to `importMnemonic`.
- `index.ts` exports `VaultHandle` (string-branded) and the guarded `deriveAddress`
  wrapper; `unsupportedPlatformError(operation)` now names the operation and keeps the
  machine-readable `error.code` (T1.2 review pattern).
- Harness (throwaway `/tmp/t13-guard`, not committed) — `tsc_exit=0`:
  - `STUB_MODE=missing`: `importMnemonic=throw:E_VAULT_UNSUPPORTED_PLATFORM | … is unavailable on this platform`,
    `bufferAfter=[0,0,0,0,0]`, `deriveAddress=throw:E_VAULT_UNSUPPORTED_PLATFORM | … deriveAddress …`
  - `STUB_MODE=present`: `importMnemonic=handle:handle-abc`, `bufferAfter=[0,0,0,0,0]`,
    `deriveAddress=0x9858EfFD232B4033E47d90003D41EC34EcaEda94`,
    `stubSaw={"handle":"handle-abc","path":"m/44'/60'/0'/0/0"}` (handle + path pass-through).

### Task 3 — GATE-R4-BC (BLOCKING)

Status: `[~]` — **evidence gathered; owner approval pending (ask-user timed out at 600 s;
re-asked in prose 2026-10-15)**

| Evidence | Observed result |
| --- | --- |
| `curl https://repo1.maven.org/maven2/org/bouncycastle/bcprov-jdk18on/maven-metadata.xml` | `<version>1.86</version>` present; `<lastUpdated>20260911045244</lastUpdated>` → 2026-09-11 |
| HEAD `…/1.86/bcprov-jdk18on-1.86.jar` | `HTTP/2 200`, `content-length: 7224011`, `last-modified: Fri, 11 Sep 2026 02:42:07 GMT` |
| jar hash cross-check | published `.sha256` = `2af190b300cbb0b35e248ccf5f4a06b6072030aeb3da7a98ec73abe5b4cb371f` == locally downloaded jar sha256 (byte-identical) |
| min-release-age | 2026-09-11 → 2026-10-15 = **34 days ≥ 7** |
| OSV `query` for `org.bouncycastle:bcprov-jdk18on` @ `1.86` | `vulns affecting 1.86: 0` |
| OSV `query` (all versions) | 12 advisories, every `fixed` event ≤ `1.85` (latest: `GHSA-9pwp-9qqc-pr26`, `GHSA-qp49-qgx5-5m26` at 1.85) → 1.86 is above all published fixes |
| Trust basis | BouncyCastle ecosystem (used by bitcoinj et al.); `specs/vault-core.md` §2.2/§6 mandates "packaged new BouncyCastle" for the Android MVP |

Not yet done (blocked on approval): the `specs/tech-stack.md` row, the `doc/dependencies.md`
section, the `modules/vault-core/android/build.gradle` coordinate.

### Tasks 4–7 — not started

| Task | Status | Blocked by |
| --- | --- | --- |
| Task 4 `Bip32.kt` core | `[ ]` | GATE-R4-BC |
| Task 5 module wiring + `VaultExceptions.kt` | `[ ]` | GATE-R4-BC |
| Task 6 `Bip32Test.kt` + instrumentation + Gradle gates | `[ ]` | GATE-R4-BC (+ device for H3) |
| Task 7 verification, zeroization review, Handoff | `[ ]` | Tasks 4–6 |

## Gate checklist

| Gate | Status | Action |
| --- | --- | --- |
| GATE-MODEL-SWITCH | `[x]` satisfied | Owner switched the model; this session runs V4.1-Flash |
| GATE-R4-BC | `[~]` evidence complete, **approval pending** | Owner approval → then amend `tech-stack.md` + `dependencies.md` + `build.gradle` and record it here |

## Evidence templates (filled during implementation)

### BIP-32 vector pass/fail table (independent Python verification — Task 0)

| Vector | Chains | Independent result |
| --- | --- | --- |
| V1 | 6 | PASS (xprv + xpub) |
| V2 | 6 | PASS (xprv + xpub) |
| V3 | 2 | PASS (xprv + xpub) |
| V4 | 3 | PASS (xprv + xpub) |
| V5 | 16 malformed extended keys | excluded (Handoff T1.3-H7) |

Kotlin gate table (to be filled by Task 6): | Vector | xprv/xpub match | Result | — `[ ]`

### Zeroization review (mandatory, file:line)

| Path | Evidence | Verdict |
| --- | --- | --- |
| seed copy (module) / `x‖y` pubkey buffer / keccak buffers / BC `BigInteger` intermediates [C] | — | `[ ]` |

### R4 approval record

Owner · date · exact coordinate `org.bouncycastle:bcprov-jdk18on:1.86`:
**pending** (evidence table above; ask-user timed out 2026-10-15, re-asked in prose).

## Handoff list (adjudicated by the next planning round)

| # | Item | Status 2026-10-15 | Disposal |
| --- | --- | --- | --- |
| T1.3-H1 | `destroy` absent (T1.4) | `[ ]` open | Implement in T1.4 |
| T1.3-H2 | iOS `deriveAddress` port (T1.5) | `[ ]` open | Replan to T1.5 |
| T1.3-H3 | Device instrumentation fallback | `[ ]` open | Only if no device at implementation; rerun pinned `connectedDebugAndroidTest`; JVM gate stays mandatory |
| T1.3-H4 | BIP-32 strings transcribed after Task-0 verification | `[x]` closed | Transcription source verified (see Task 0); exact strings embedded at Task 6 |
| T1.3-H5 | JS wrapper evidence via /tmp harness | `[ ]` open | T1.1 closure absorbs it as committed jest tests |
| T1.3-H6 | BC pin 1.86 carried to T1.4 | `[ ]` open | Replan to T1.4 |
| T1.3-H7 | BIP-32 vector 5 (16 invalid extended keys) not gated | `[ ]` open | No production extended-key parser; absorb with a future xpub/parser feature |

## Status board

| Task | Status |
| --- | --- |
| Planning round (P0.1–P0.6) | `[x]` verified — evidence above |
| Task 0 preflight + vector verification + spec correction | `[x]` verified |
| Task 1 H2 `specs/vault-core.md` amendment | `[x]` verified (diff recorded) |
| Task 2 TS interface + wrapper harness | `[x]` verified (harness both modes, tsc 0) |
| Task 3 GATE-R4-BC | `[~]` evidence complete, owner approval pending |
| Tasks 4–7 | `[ ]` blocked on GATE-R4-BC |
| T1.3 overall | **`[~]` in progress (T1.2 review discipline: no premature `[x]`)** |
