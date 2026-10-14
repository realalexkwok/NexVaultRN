# certs/

OTA update code-signing material (AGENTS.md R2).

Rule R2: the **public** code-signing certificate may be committed under `certs/`; the
**private key** stays offline (KMS / password manager) and is **never committed, never
stored in CI, never printed to logs**.

- Committed: public code-signing certificate (e.g. `code-signing-certificate.pem`).
- Never committed: `private-key.pem` and any `*.key` file (ignored by the root `.gitignore`).

All `production` channel EAS Updates must be signed (`rsa-v1_5-sha256`); the client must
silently reject updates whose signature fails validation.
