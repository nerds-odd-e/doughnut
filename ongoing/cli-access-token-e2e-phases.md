# CLI access token management — phased plan

**Scope:** Re-enable the ignored scenarios in `e2e_test/features/cli/cli_access_token.feature`, **one scenario per top-level phase**, with **sub-phases** small enough to commit often, keep **CI green**, and avoid **dead production code**.

**Planning discipline:** `.cursor/rules/planning.mdc` — scenario-first phases, **observable** E2E at the PTY boundary, **at most one** intentionally failing test while driving a change, phase-complete tests, no dead code justified only by unit tests on happy paths.

**Reference implementation:** `git show 8210c94` had the feature end-to-end; it was removed in a **Mar 2026 interactive strip-down** (see below). Reuse **observable behavior** and **persistence shape**, not the old structure.

**Architecture (guide, not a mandate to build everything):** `ongoing/cli-architecture-roadmap.md` — apply only what this feature needs; **challenge** heavy patterns before copying (same spirit as `.cursor/rules/cli.mdc` roadmap note). When you do apply it: interactive Ink/React session, **stage-local** flows, **thin** Cucumber steps → page objects, **centralized** terminal assertions with **actionable** failures (roadmap §8–10). Reuse **`doughnut-api`** / `UserController` for validate / generate / revoke (roadmap §7).

**CLI implementation habits:** `.cursor/rules/cli.mdc` — **Current guidance** vs **Current prompt** (numbered token lists belong in **Current guidance**, like other list UIs); **stable `useInput` handlers** (`useCallback`); prefer **`setImmediate`** over **`setTimeout(…, 0)`** when sequencing exit/unmount after Ink commits; **no wall-clock sleeps** in Vitest — observable `frames` / turn loops; **column-width** rules for wrapped lists (not raw string `.length`).

---

## Lessons from the Mar 2026 removal (do not repeat)

The following **`main`** commits removed or hollowed out access-token behavior during the **CLI interactive strip-down** (late Mar 2026). They are useful as **anti-patterns to avoid** when reviving the feature.

**What was removed or cut back**

- **`c5f9efa39`** — Dropped **`addAccessToken`**, **`listAccessTokens`**, **`removeAccessToken`**, **`createAccessToken`**, and related **`saveConfig`** wiring in one simplification pass → avoid reintroducing a **wide surface** in a single commit without tests per scenario.
- **`cc94684a7`** — Removed **`accessToken` from CLI execution** and **ignored** E2E scenarios → keep **@ignore** discipline (this plan § CI) so CI never carries half-wired behavior.
- **`a4c4be63a`** — Removed **removal** flow and **`removeToken`** page object pieces → when Phase 3 returns, restore **removal** only with **page-object + assertion layer** cohesion, not ad-hoc steps.
- **`ed578b226`** — Removed **`/list-access-token`** from **`processInput`** / interactive input processing → **slash-command handling** had become **entangled** with low-level input routing; revival should keep **one coherent registration path** (this plan § Command surface).
- **`6e4e7219c`**, **`cd06846ed`** — **`removeAccessToken`** / **`removeAccessTokenCompletely`** peeled out of **interactive shell** dependencies → avoid **scattering** token domain helpers across unrelated modules; keep **revoke vs local-only** in one domain-oriented place, called from slash handlers.
- **`08a0fc4b12` → `08a17aaaf`** — A **refactor of access token management in the interactive application** was **reverted** the same day → large **interactive-app state** refactors without green E2E per slice are high risk; prefer **small vertical slices** (this plan sub-phases).

**Earlier buildup (context only, not a template)**

- **`42c9383f5`**, **`a8925b3fb`**, **`12fb26027`**, **`6638ae631`** — Moved token UX toward **Ink live panels** and interactive commands → **Ink is fine**; the failure mode was **coupling** and **command/input sprawl**, not “lists on screen.” Prefer **small stages** and **clear input ownership** (roadmap §4, §11.3).

**Gmail (`cc7fa312b`)** was removed in the same strip-down wave; **API access tokens** in this feature file are **not** Gmail OAuth. Do not conflate **Bearer API tokens** with **Google account** flows when naming modules or commands.

---

## Architecture guardrails for revival

1. **Slash commands are the shell’s message handlers, not a second app** — Register **`/add-access-token`**, **`/list-access-token`**, etc. through the **same mechanism** other slash commands use (`cli/src/commands/` aggregation + interactive dispatch). Do not re-add **special-case branches** in unrelated input paths “just for tokens.”
2. **Domain module vs Ink stage** — Keep **file I/O, backend validate/generate/revoke, and label/default rules** in a **small, testable module** (pure-ish or thin async). Keep **TTY prompts, ↑↓ selection, cancel keys** in **stage-local** components. Stages signal **done / cancel / error** upward; parents decide next transition (roadmap §4.2).
3. **Single source of truth for “what list commands exist”** — The strip-down had churn around **`TOKEN_LIST_COMMANDS`** and moving **`getDefaultTokenLabel` / `setDefaultTokenLabel`**. Prefer **one table or registry** owned next to slash-command docs so list/remove/create do not **drift** from help and from **`processInput`** handling.
4. **Keyboard ownership** — For Phase 4 (`q` cancel), document **who consumes** `q` (stage vs global) before coding; align with roadmap §11.3 so cancel does not fight **`useInput`** on the command line.
5. **Exports** — Keep **`export` surface small** (cli.mdc); do not export helpers “for E2E” — drive behavior through **PTY + page objects**.

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
3. **Un-ignore + Vitest (sub-phase final):** Remove `@ignore` for this scenario. Add or extend **high-level** `runInteractive` tests only where they reduce regression risk without mirroring internals (cli.mdc). For Ink timing, follow **observable** waits and **setImmediate** ordering notes in cli.mdc.

---

# Phase 1 — Scenario: *Add access token and list it*

**Outcome:** User can paste/add the API token from the test fixture into the interactive CLI and see the label under **Current guidance** after `/list-access-token`.

### 1a — E2E wiring + failing test (**done**)

- **Glue:** `e2e_test/step_definitions/cli.ts` — `When I add the saved access token in the interactive CLI using add-access-token` (`@savedAccessToken` → `/add-access-token <token>`); `Then I should see "…" in the Current guidance` → `cli.interactiveCli().currentGuidance().expectContains`.
- **Simulated screen:** `e2e_test/config/cliInteractivePtyGeometry.ts` (cols/rows, shared with `cliE2ePluginTasks` spawn) + `e2e_test/config/cliPtyTerminalReplay.ts` (`ptyTranscriptToVisiblePlaintext`, `extractCurrentGuidanceFromReplayedPlaintext` — region = lines after the last row containing `> `).
- **Assertion layer:** `e2e_test/start/pageObjects/cli/outputAssertions.ts` (`currentGuidance`) + `interactiveCli.ts` export.
- **Feature:** scenario 1 tagged `@cliAccessTokenP1` (still `@ignore` for default CI). **Local run:** `pnpm exec cypress run --config-file e2e_test/config/ci.ts --spec e2e_test/features/cli/cli_access_token.feature --env TAGS='@cliAccessTokenP1'` (SUT up). **Expected until 1b:** fails with missing `"E2E CLI Token"` in guidance (currently `Not supported` for slash commands).
- **Mid-step assertion:** the add-access-token step asserts **`Token added successfully`** in past CLI assistant messages (after the PTY line is sent).

### 1b — Product: `/add-access-token` + `/list-access-token` (**done**)

- **Persistence:** write `access-tokens.json` (historical shape). **`addAccessToken`:** validate with backend, then append `{ label, token }`.
- **Interactive UX:** On success, append assistant message **`Token added successfully`** (must match the step assertion).
- **`/list-access-token`:** Show stored tokens in **Current guidance** (numbered list; **`string-width`** + grapheme wrap in `cli/src/terminalColumns.ts`). **`InteractiveCliApp`** renders default **`/ commands`** and slash results’ optional **`currentGuidance`** on lines **below** the `> ` prompt so E2E `extractCurrentGuidanceFromReplayedPlaintext` sees them. Empty list: assistant **`No access tokens stored.`**, guidance reset to default.
- **Stage:** List is synchronous (no separate Ink stage) — enough for this scenario; dedicated selection stage remains for later remove/cancel phases.

### 1c — Close phase

- Remove `@ignore` from scenario 1 only.
- `pnpm cli:test` + targeted Cypress: `e2e_test/features/cli/cli_access_token.feature` (grep/tag to scenario 1 if supported).

**Downstream note:** `cli_recall.feature` uses the same “add saved token” step; it stays `@ignore` until recall work — the step is **not** dead if scenario 1 is active.

---

# Phase 2 — Scenario: *Add invalid access token*

**Outcome:** `/add-access-token invalid-token-xxx` surfaces **`Access token is invalid or expired`** in **past CLI assistant messages**.

### 2a — E2E (**done**)

- Scenario already phrased; `assertPastCliAssistantMessagesContains` uses an explicit **past CLI assistant messages** section label plus head/tail transcript previews on failure.

### 2b — Product (**done**)

- Invalid token path on `/add-access-token`: `getTokenInfo` failure → `withBackendClient` → `userVisibleMessageForSdkThrowable` (401 → `http401StoredTokenRejected`, substring **Access token is invalid or expired**); no `saveConfig` (no file write).

### 2c — Close phase (**done**)

- `@ignore` removed for scenario 2 only; `pnpm cli:test` + Cypress `e2e_test/features/cli/cli_access_token.feature`.

---

# Phase 3 — Scenario outline: *Remove access token*

**Outcome:** After add, `/remove-access-token <label>` or `/remove-access-token-completely <label>` shows the right success copy; `/list-access-token` then shows **`No access tokens stored.`** in past assistant messages.

### 3a — E2E (**done**)

- `e2e_test/start/pageObjects/cli/removeToken.ts` — `expectedLocalRemoveSuccessMessage` / `expectedCompleteRemoveSuccessMessage`, `removeToken().expectRemoveSuccess(type, label)` + `cy.log` context; `cli` index re-exports `removeToken`.
- `e2e_test/step_definitions/cli.ts` — `Then I should see the {word} remove success message for {string}`.

### 3b — Product (**done**)

- **`/remove-access-token <label>`:** `removeAccessTokenLocal` → `Token "<label>" removed.`
- **`/remove-access-token-completely <label>`:** `UserController.revokeToken` with that entry’s bearer, then local remove → `Token "<label>" removed locally and from server.`
- **Empty list:** `ListAccessTokenStage` `useEffect` auto-`onSettled('No access tokens stored.')` so `/list-access-token` commits to transcript without Esc.

### 3c — Close phase (**done**)

- `@ignore` removed from the outline scenario; Vitest + Cypress `cli_access_token.feature` green.

---

# Phase 4 — Scenario: *Another key cancels remove-access-token selection*

**Outcome:** `/remove-access-token` opens selection; **`q`** cancels; guidance returns to **`/ commands`**; token remains listable.

### 4a — E2E infrastructure

- Restore **raw key** input for PTY: Cypress task + `When I cancel the token list with q in the interactive CLI` (writes **`q`** without a newline, then settle). Clear error if no session.
- Assertions: **Current guidance** contains token label → after `q`, contains **`/ commands`** → list still shows token.

### 4b — Product

- **Token list stage** for remove: **↑↓ Enter to select; other keys cancel** (or project-standard copy from cli.mdc). **`q`** must cancel and return to default interactive guidance. Define **input ownership** explicitly so `q` is handled in the **stage** without breaking global shortcuts (roadmap §11.3).
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

- Register slash commands in **one** place (aggregated help / command table) **as each phase** introduces a command — avoids dead command names in help with no implementation and repeats the **single registration path** lesson from the strip-down.

---

## After all phases

- Confirm `accessTokenCommandDocs` (or successor) lists all five commands for `/` help.
- Optionally align `authenticatedBackendCallFailureAdvice.noDefaultTokenInConfig` with interactive wording if `/add-access-token` is the primary path (historical 8210c94 did this); only when a scenario or existing CLI UX requires it.

---

## Suggested verification commands

- **Cloud VM:** `pnpm cli:test`; Cypress per `.cursor/rules/cloud-agent-setup.mdc` and `e2e_test.mdc` for single feature.
- **Nix dev shell:** prefix with `CURSOR_DEV=true nix develop -c` per `general.mdc`.

---

## Phase checklist (update as you go)

| Phase | Scenario                         | @ignore removed | Notes |
|------:|----------------------------------|-----------------|-------|
| 1     | Add access token and list it     | ☐               |       |
| 2     | Add invalid access token         | ☑               |       |
| 3     | Remove access token (outline)    | ☑               |       |
| 4     | `q` cancels remove selection     | ☐               |       |
| 5     | Create access token via CLI      | ☐               |       |
