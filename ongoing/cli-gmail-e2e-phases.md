# CLI Gmail E2E — phased plan

Informal working plan for bringing back `e2e_test/features/cli/cli_gmail.feature` scenario by scenario. Update or delete when this work is done.

**Reference implementation:** `git show 8210c94` had the full flow (Mountebank Google imposter on port **5003**, `ServiceMocker`-style stubs, `cliGmailE2eConfig.ts`, hooks, OAuth callback simulation on the PTY stream, `createCliConfigDirWithGmail`, `GOOGLE_BASE_URL` + `DOUGHNUT_CONFIG_DIR` for the CLI process).

**Change from 8210c94:** Do **not** rely on a runtime “inject secrets” hook into an unbundled CLI. The **same** `cli/dist/doughnut-cli.bundle.mjs` that `runRepoCliInteractive` starts should already contain OAuth client id/secret (baked at bundle time with the same fixed E2E values). Config file under a temp dir still supplies `gmail.json` (and optional overrides); use **`DOUGHNUT_CONFIG_DIR`** so local `~/.config/doughnut` is untouched.

**Roadmap alignment** (`ongoing/cli-architecture-roadmap.md`): PTY E2E, thin steps → page objects / plugin tasks, centralized terminal assertions (`outputAssertions.ts`), Ink message flow for interactive commands — no new terminal framework.

**CI / `@ignore`:** Keep scenarios **`@ignore`** until the corresponding phase is complete; remove **`@ignore`** for a scenario only when that scenario is green locally (and you intend CI to run it). While **`@ignore`** is on, commits stay green even if the spec is mid-flight.

**TDD rhythm:** Within each phase, prefer **sub-phase A = E2E wiring + assertion that fails for the right reason** (clear message from the centralized assertion layer or Cypress task errors), then **sub-phase B = minimal product code** to go green.

---

## Phase 1 — Scenario: *add gmail adds account when OAuth callback is simulated*

**Outcome:** `/add gmail` in the interactive CLI ends with **past CLI assistant messages** containing `Added account e2e@gmail.com`, with Google HTTP mocked via Mountebank and OAuth completed without a real browser.

### 1.1 — E2E: Mountebank Google service + `Given` step (tokens + profile)

- Add `e2e_test/start/mock_services/googleService.ts` (port **5003**, same stub shapes as 8210c94: `POST /token`, `GET` path-only for `/gmail/v1/users/me/profile`).
- Register `google` on `mock_services` in `e2e_test/start/mock_services/index.ts`.
- `Before` / `After` for `@usingMockedGoogleService` mirroring OpenAI/Wikidata (install imposter, restore).
- Step: `Given the Google API mock returns tokens and profile for {string}` — chain `stubTokenExchange`, `stubGmailProfile` (use token values consistent with what the CLI will send; match 8210c94 if still valid).
- **Failure check:** Running the scenario (with temporary tags/hooks from 1.2–1.3) should fail later with **network/API errors**, not “step undefined” or Mountebank connection refused.

### 1.2 — E2E: Isolated config dir + `gmail.json` for “add account”

- Add `e2e_test/config/cliGmailE2eConfig.ts` with fixed **`GMAIL_E2E_GOOGLE_CLIENT_ID` / `GMAIL_E2E_GOOGLE_CLIENT_SECRET`** and `GMAIL_E2E_OAUTH_ADD_CONFIG` (`clientId`, `clientSecret`, `accounts: []`) — same idea as 8210c94.
- Cypress task: `createCliConfigDirWithGmail` → temp dir, write `gmail.json`, return path (8210c94 pattern).
- Tag **`@withCliGmailOAuthAddConfig`** (or reuse a single **`@withCliConfig`** variant) in **`Before` order** before interactive start: create dir, `.as('cliConfigDir')`.
- **Restore `withCliConfig` behavior** if still missing from `hook.ts` (features reference it but implementation may be absent): temp config dir + pass **`DOUGHNUT_CONFIG_DIR`** into CLI spawns so developer machines are not polluted.

### 1.3 — E2E: Bundle embeds E2E OAuth client + PTY env + OAuth callback simulation

- **Bundle:** `ensureCliBundleFresh` always rebuilds `cli/dist/doughnut-cli.bundle.mjs` with `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` from `cliGmailE2eConfig` when the bundle is missing, older than inputs (including `cliGmailE2eConfig.ts`), or does not contain the E2E client id string — no `@cliGmailBundledSecrets` tag.
- **`runRepoCliInteractive` env** for Gmail scenarios: `GOOGLE_BASE_URL=http://localhost:5003`, `DOUGHNUT_CONFIG_DIR=<temp>`, `DOUGHNUT_NO_BROWSER=1` (suppress `xdg-open` in CI/dev).
- **OAuth simulation:** Optional Cypress task **`cliInteractivePtyEnableGoogleOAuthSimulation`** (after **`runRepoCliInteractive`**): when PTY output contains a Google auth URL, issue **`fetch(`${redirect_uri}?code=...`)`** (`e2e_mock_auth_code`) so the CLI’s local callback server receives the code. Callers that do not run this task get no simulation.
- **Failure check:** After `/add gmail`, assertion fails with missing **`Added account e2e@gmail.com`** (or a stable assistant error string), not timeout on prompt or wrong host.

### 1.4 — Product: Interactive `/add gmail`

- Wire slash command **`/add gmail`** to **`addGmailAccount`** from `cli/src/commands/gmail.ts` (fetch-wait / stage strings consistent with existing interactive patterns, e.g. “Connecting Gmail” in `interactiveFetchWait` if used).
- Ensure success text visible in **past CLI assistant messages** matches the scenario: include **`Added account <email>`** (same wording as `console.log` in `gmail.ts` or adjust one place only).
- **Unit tests (only if E2E alone would leave dead paths):** Prefer **`runInteractive`**-style coverage; add **narrow** tests only for **pure** behavior (e.g. error messages) if needed — see Phase 3.

### 1.5 — Gate

- Remove **`@ignore`** from scenario 1 when stable.
- Run: `pnpm cypress run --spec e2e_test/features/cli/cli_gmail.feature` (with project config per `.cursor/rules/e2e_test.mdc`).

---

## Phase 2 — Scenario: *last email shows subject when account is configured*

**Outcome:** With a **preconfigured** `gmail.json` (mock tokens + non-expired `expiresAt`), `/last email` shows **`Welcome to Doughnut`** in past assistant messages; Google list + get message endpoints mocked.

### 2.1 — E2E: Second `Given` + mock account config tag

- Extend feature (as in 8210c94): add  
  `Given the Google API mock returns messages and message "msg-1" with subject "Welcome to Doughnut"`  
  for scenario 2 only (scenario 1 keeps the shorter Given).
- Step implementation: stub `messages` list + stub `GET .../messages/msg-1` with Subject header.
- Tag **`@withCliGmailMockAccountConfig`** + task payload **`GMAIL_E2E_MOCK_ACCOUNT_CONFIG`** (accounts entry with `accessToken` / `refreshToken` / `expiresAt` far in the future).
- Gmail-specific interactive start **without** calling **`cliInteractivePtyEnableGoogleOAuthSimulation`** (or reuse env flags only); align with Phase 1 PTY env (`GOOGLE_BASE_URL`, `DOUGHNUT_CONFIG_DIR`, `DOUGHNUT_NO_BROWSER`, same bundled secrets).

### 2.2 — E2E: Failure mode before product

- Run scenario 2: expect failure because **`/last email`** missing or no subject in transcript — assertion message should mention expected substring **`Welcome to Doughnut`**.

### 2.3 — Product: Interactive `/last email`

- Wire **`/last email`** to **`getLastEmailSubject`**; render subject (or user-facing error) into assistant / past messages consistently with other slash commands.
- Reuse existing **`cli/tests/gmail.test.ts`** for HTTP edge cases where already covered; do not duplicate E2E assertions.

### 2.4 — Gate

- Remove **`@ignore`** from scenario 2 when stable; run the same single-feature Cypress command.

---

## Phase 3+ — Lower-level behavior (Gmail scope only, high-level unit tests)

Add **only** what Phase 1–2 expose as gaps. Prefer **`runInteractive`** or public CLI entry points; use **direct** tests only for **stable pure I/O** (`planning.mdc`).

Candidates (pick if uncovered after E2E):

- **`addGmailAccount`:** user-visible errors (missing credentials, OAuth timeout, token exchange failure) via mocked `fetch` / temp config path — if not already exercised indirectly.
- **URL building:** `GOOGLE_BASE_URL` rewriting for token + Gmail paths (if logic grows beyond trivial).
- **Interactive copy / stage labels:** short Vitest checks for strings tied to new fetch-wait lines (only if required for regression; avoid 1:1 file-to-test mapping).

**Stop rule:** No new unit file solely to mirror a module; each test must justify a **user-observable** or **documented contract** gap left by E2E.

---

## Quick reference — env and files

| Piece | Role |
|--------|------|
| `GOOGLE_BASE_URL` | Points CLI at Mountebank (`http://localhost:5003`) for token + Gmail API paths under rewritten base. |
| `DOUGHNUT_CONFIG_DIR` | Temp dir per scenario; contains `gmail.json`. |
| `DOUGHNUT_NO_BROWSER` | Prevents opening a real browser during OAuth URL print. |
| Bundle build env | `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` baked into **default** E2E bundle when Gmail secrets task runs. |
| `cli/src/commands/gmail.ts` | Existing `addGmailAccount`, `getLastEmailSubject`, `loadConfig` / `saveConfig`. |

---

## Commands

- CLI unit: `pnpm cli:test` (or targeted `pnpm -C cli test tests/gmail.test.ts`).
- E2E single feature: see `.cursor/rules/e2e_test.mdc` and `cloud-agent-setup.mdc` (Cloud VM: no nix prefix).
