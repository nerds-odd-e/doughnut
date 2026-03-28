# CLI access token management — phased plan

**Scope:** Re-enable the ignored scenarios in `e2e_test/features/cli/cli_access_token.feature`, **one scenario per top-level phase**, with **sub-phases** small enough to commit often, keep **CI green**, and avoid **dead production code**.

**Reference implementation:** `git show 8210c94` had the feature end-to-end; it was removed for architectural reasons. Reuse **observable behavior** and **persistence shape**, not necessarily the same file split.

**Architecture (guide):** `ongoing/cli-architecture-roadmap.md` — interactive Ink/React session, **stage-local** token-list flows, **thin** Cucumber steps → page objects, **centralized** terminal assertions with **actionable** failures (§8–10). Reuse **`doughnut-api`** / `UserController` for validate / generate / revoke (§7).

---

## Shared facts (from current + historical code)

- **Storage:** `{ configDir }/access-tokens.json` — JSON with `tokens: { label, token }[]` and optional `defaultLabel`. Path via `getConfigDir()` (see `cli/src/commands/accessToken.ts`).
- **Validate add:** call backend with the bearer token (historically `UserController.getTokenInfo`); on failure, user sees a message consistent with **`Access token is invalid or expired`** (match Gherkin in scenario 2; may need a dedicated validation error string vs generic `http401StoredTokenRejected` if copy diverges).
- **Create:** authenticated as **current default** token → `UserController.generateToken` → append to config.
- **Remove local:** drop entry from config; adjust `defaultLabel` if needed.
- **Remove completely:** revoke on server then remove locally.
- **E2E harness today:** `@interactiveCLI` + `cliInteractiveWriteLine` only sends `line + '\r'`. Scenario 4 needs a **raw `q`** (historically `INTERACTIVE_CLI_PTY_KEYSTROKE_TASK` + `typeRawKey`). Plan sub-phases should restore a **minimal** keystroke path (one task, or `writeRaw` for single keys) without reintroducing the old monolith.

---

## CI and `@ignore` discipline

- **Default:** Leave `@ignore` on a scenario until that **phase’s** E2E **and** product work are done and the scenario **passes** locally; then remove `@ignore` for that scenario only so **CI stays green**.
- **Optional progress tag:** e.g. `@cliAccessTokenP1` on the active scenario so you can run `--tags '@cliAccessTokenP1'` locally without enabling CI until ready.
- **At most one intentionally failing test** while driving a change (planning rule): do not un-ignore multiple scenarios at once.

---

## Sub-phase pattern (repeat inside every phase)

1. **E2E first (sub-phase a):** Steps, page objects, plugin support if needed, and **assertions with clear failure text** (expected substring, section name, short transcript preview). Run the **single** feature/spec for this scenario; confirm failure is **missing behavior**, not typos or missing PTY session.
2. **Product (sub-phase b+):** Smallest CLI changes so the scenario passes — **only** what this scenario needs; avoid shipping unused slash commands (if a later scenario needs a new command, add it in that phase).
3. **Un-ignore + Vitest (sub-phase final):** Remove `@ignore` for this scenario. Add or extend **high-level** `runInteractive` tests only where they reduce regression risk without mirroring internals (cli.mdc).

---

# Phase 1 — Scenario: *Add access token and list it*

**Outcome:** User can paste/add the API token from the test fixture into the interactive CLI and see the label under **Current guidance** after `/list-access-token`.

### 1a — E2E wiring + failing test

- Implement missing glue: `When I add the saved access token in the interactive CLI using add-access-token` (read `@savedAccessToken` from existing user step, send `/add-access-token <token>` via interactive PTY).
- Add **`Then I should see "…" in the Current guidance`** (and exports on `cli` page object): implement via **centralized** assertion layer — **simulated visible PTY screen** (cursor/erase replay) or the same technique the project uses for recall “visible” checks; **do not** scatter one-off parsing in step defs. Failure message must state: expected text, that guidance uses simulated screen, and show a **tail preview** of plain replay.
- **Mid-step assertion (optional but matches old behavior):** after add, expect **`Token added`** in past CLI assistant messages so failures distinguish “command missing” vs “list wrong”.
- Remove `@ignore` only after 1b is ready, or use a **wip tag** for local runs.

### 1b — Product: `/add-access-token` + `/list-access-token` (minimal)

- **Persistence:** `saveConfig` / write `access-tokens.json` (historical shape). **`addAccessToken`:** validate with backend, then append `{ label, token }`.
- **Interactive UX:** On success, append assistant message **`Token added`** (or exact string E2E asserts).
- **`/list-access-token`:** Show stored tokens in **Current guidance** (numbered list consistent with other list UIs; label text must include the stored label e.g. `E2E CLI Token`). Default marker (e.g. `★`) optional in this phase if not asserted.
- **Stage:** Prefer a **small** dedicated Ink stage or composed component for the list — self-contained, no parent leakage (roadmap §4.2). Avoid copying the old `accessTokenListStage` structure if it caused coupling problems; match **behavior** first.

### 1c — Close phase

- Remove `@ignore` from scenario 1 only.
- `pnpm cli:test` + targeted Cypress: `e2e_test/features/cli/cli_access_token.feature` (grep/tag to scenario 1 if supported).

**Downstream note:** `cli_recall.feature` uses the same “add saved token” step; it stays `@ignore` until recall work — the step is **not** dead if scenario 1 is active.

---

# Phase 2 — Scenario: *Add invalid access token*

**Outcome:** `/add-access-token invalid-token-xxx` surfaces **`Access token is invalid or expired`** in **past CLI assistant messages**.

### 2a — E2E

- Scenario already phrased; ensure assertion path for past assistant messages is **clear** on failure (reuse existing `pastCliAssistantMessages` copy in `outputAssertions.ts`).

### 2b — Product

- Invalid token path on `/add-access-token`: no file write; assistant/past message with **exact** substring the feature expects (normalize with `authenticatedBackendCallFailureAdvice` or a single user-visible string used both here and for 401 paths).

### 2c — Close phase

- Remove `@ignore` for scenario 2 only; run CLI tests + this feature file.

---

# Phase 3 — Scenario outline: *Remove access token*

**Outcome:** After add, `/remove-access-token <label>` or `/remove-access-token-completely <label>` shows the right success copy; `/list-access-token` then shows **`No access tokens stored.`** in past assistant messages.

### 3a — E2E

- Implement `Then I should see the <removal_type> remove success message for "<label>"` in page objects (historically: **local** → `Token "<label>" removed.`; **complete** → substring like **`removed locally and from server`**). Centralize in e.g. `removeToken.ts` + re-export from `cli` index.
- Confirm failure messages include **removal type**, **label**, and transcript preview.

### 3b — Product: remove commands (no dead subcommands)

- **`/remove-access-token <label>`:** remove from config only; emit local success line.
- **`/remove-access-token-completely <label>`:** `revokeToken` then local remove; emit complete success line.
- **Empty list copy:** When there are no tokens, `/list-access-token` must put **`No access tokens stored.`** in the **past CLI assistant messages** region (as Gherkin specifies), not only in guidance.

### 3c — Close phase

- Remove `@ignore` for the outline scenario; both examples must pass.

---

# Phase 4 — Scenario: *Another key cancels remove-access-token selection*

**Outcome:** `/remove-access-token` opens selection; **`q`** cancels; guidance returns to **`/ commands`**; token remains listable.

### 4a — E2E infrastructure

- Restore **raw key** input for PTY: Cypress task + `When I cancel the token list with q in the interactive CLI` (writes **`q`** without a newline, then settle). Clear error if no session.
- Assertions: **Current guidance** contains token label → after `q`, contains **`/ commands`** → list still shows token.

### 4b — Product

- **Token list stage** for remove: **↑↓ Enter to select; other keys cancel** (or project-standard copy from cli.mdc). **`q`** must cancel and return to default interactive guidance.
- Ensure cancel does **not** mutate stored tokens.

### 4c — Close phase

- Remove `@ignore` for scenario 4; run tests.

---

# Phase 5 — Scenario: *Create access token via CLI*

**Outcome:** With a default token present, `/create-access-token New CLI Token` shows **`Token created`** in past assistant messages; list shows **New CLI Token**.

### 5a — E2E

- Past message + guidance assertions already covered by prior phases; confirm **`Token created`** failure text is explicit.

### 5b — Product

- **`/create-access-token <label>`:** `UserController.generateToken`, append to config, message **`Token created`**.
- **Duplicate label / API errors:** Handle with user-visible errors (only as much as this scenario needs; stricter validation can be a follow-up).

### 5c — Close phase

- Remove `@ignore` for scenario 5; run tests.

---

## Command registration and help

- Register slash commands in **one** place (aggregated help / command table) **as each phase** introduces a command — avoids dead command names in help with no implementation.

## After all phases

- Confirm `accessTokenCommandDocs` (or successor) lists all five commands for `/` help.
- Optionally align `authenticatedBackendCallFailureAdvice.noDefaultTokenInConfig` with interactive wording if `/add-access-token` is the primary path (historical 8210c94 did this); only when a scenario or existing CLI UX requires it.

## Suggested verification commands

- **Cloud VM:** `pnpm cli:test`; Cypress per `.cursor/rules/cloud-agent-setup.mdc` and `e2e_test.mdc` for single feature.
- **Nix dev shell:** prefix with `CURSOR_DEV=true nix develop -c` per `general.mdc`.

---

## Phase checklist (update as you go)

| Phase | Scenario                         | @ignore removed | Notes |
|------:|----------------------------------|-----------------|-------|
| 1     | Add access token and list it     | ☐               |       |
| 2     | Add invalid access token         | ☐               |       |
| 3     | Remove access token (outline)    | ☐               |       |
| 4     | `q` cancels remove selection     | ☐               |       |
| 5     | Create access token via CLI      | ☐               |       |
