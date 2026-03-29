# Phased plan: revive access-token list selection (interactive CLI)

**Intent:** Bring back **↑ / ↓ to move highlight**, **Enter to confirm**, and **Esc to dismiss** for choosing a stored token label—without reintroducing the old split-brain between shell session state, duplicate keyboard paths, and scattered command logic.

**Note on naming:** The historical behavior was tied to **`/list-access-token`** (change persisted default) and bare **`/remove-access-token`** / **`/remove-access-token-completely`**. If “`/less-access-token`” was meant literally, there is no such command in recent history; treat it as **`/list-access-token`**.

**Testing (per request):** No new Cypress/E2E. Cover with **high-level Vitest** driving **`runInteractive` + real stdin** (and temp config dir), same style as `cli/tests/InteractiveCliApp.addAccessToken.test.tsx`—**no mocks** of Ink adapters or command internals. Prefer **one** focused describe block (or extend that file) so behavior stays grouped.

**Design note — no `TOKEN_LIST_COMMANDS`:** A central map from slash line → picker config (`TOKEN_LIST_COMMANDS` / `tokenListCommands.ts`) was **tried in phase 1 and removed**. It looked like DRY but added a **second source of truth** next to `interactiveSlashCommands`, hid which command owned which UX, and encouraged drift. **Keep stage copy and behavior next to the command** (constants beside the list stage, or a tiny wrapper that passes props into a shared `AccessTokenPickerStage` when phase 2 generalizes it)—not a global table.

**Architecture goals:**

- **One policy module** for “highlight-only list” keys: map Ink input → move highlight / submit index / abort. This is the old `cli/src/interactions/selectListInteraction.ts` (removed with `9664c3a87` / surrounding strip-down; consumer `accessTokenListStage.tsx` removed in `2e18a025a`). Restore the **highlight-only + `abort-list`** path and **`cycleListSelectionIndex`**; keep **`slash-and-number-or-highlight`** only if recall or another flow still needs it—otherwise omit dead branches to stay minimal.
- **One reusable Ink picker** for list selection when it pays off: shared component for highlight / Enter / Esc, with **action and copy supplied by the slash command** (wrapper or props), **not** a separate registry file that mirrors command names.
- **Dedup with `MainInteractivePrompt.tsx`:** today slash suggestions use local modulo logic for highlight (`ttyArrowKeyUsesSlashSuggestionCycle` + inline wrap). After restoring `cycleListSelectionIndex`, **call the shared helper** from `MainInteractivePrompt` so token list and slash command list do not diverge on wrap semantics. Do **not** force one mega-component: slash completion still needs **caret gating** and **Enter-to-fill-buffer**; only the **index math** (and optionally Ink→event mapping via `selectListKeyEventFromInk`) should be shared.

**Product behavior to match** (behavioral reference from `2e18a025a^` `accessTokenListStage.tsx` and old `tokenListCommands.ts`—**not** a call to bring that map back):

| Slash line | Tokens present | Behavior |
|------------|----------------|----------|
| `/list-access-token` | 0 | Immediate assistant line: “No access tokens stored.” (no picker). |
| `/list-access-token` | ≥1 | Stage: indicator “Access tokens”, prompt “Select and enter to change the default access token”; **Enter** → `setDefaultTokenLabel` + assistant confirmation. |
| `/remove-access-token` | 0 | Same immediate empty message. |
| `/remove-access-token` | ≥1 | Stage: “Remove access token”; **Enter** → local remove + confirmation. |
| `/remove-access-token-completely` | 0 | Same immediate empty message. |
| `/remove-access-token-completely` | ≥1 | Stage: “Remove access token completely”; **Enter** → revoke on server + local remove (async); surface errors like today’s `run()` path. |
| Esc (in picker) | — | Close picker with a **single user-visible cancelled / aborted line** (old code used `CLI_USER_ABORTED_WAIT_MESSAGE` from removed `fetchAbort` stack—pick **one** short string and reuse everywhere for this pattern). |

**With explicit label:** Keep current behavior: **`/remove-access-token <label>`** and **`/remove-access-token-completely <label>`** call existing `run` handlers **without** opening the picker.

**InteractiveCliApp routing change:** Today, `argumentName` + empty argument always shows “Missing …”. For remove commands, **if** `stageComponent` is set **and** argument is empty, **open the picker stage** instead of the usage error. (List command already has no `argumentName` and always uses stage.)

**Persistence:** **`setDefaultTokenLabel`** lives next to `getDefaultTokenLabel` / `loadAccessTokenConfig` in `accessTokenStorage.ts` (phase 1).

---

## Phase 1 — Default token: picker + `setDefaultTokenLabel` + tests

**Status:** Done (Vitest green: `pnpm cli:test`). Stage keyboard input is forwarded from `InteractiveCliApp` via `SetStageKeyHandlerContext` so Ink delivers keys after swapping off `MainInteractivePrompt`.

**User-visible slice:** With at least one stored token, **`/list-access-token`** opens a picker; **↑↓** moves selection (start at current default if still present); **Enter** persists default and shows confirmation; **Esc** cancels with the shared abort line.

**Implementation sketch:**

1. Restore **`selectListInteraction`** (+ focused **direct** unit tests for `cycleListSelectionIndex`, `dispatchSelectListKey` highlight-only, `selectListKeyEventFromInk` bare `\r`/`\n`—restore from `77ae2317f^:cli/tests/selectListInteraction.test.ts` trimmed if recall-only cases are dropped).
2. Add **`setDefaultTokenLabel`** to storage; export via `accessToken.ts` if that remains the façade.
3. **`AccessTokenPickerStage`** (shared list UI + keys) with **set-default** behavior and **stage copy colocated** in that module (or the thin **`ListAccessTokenStage`** wrapper); **`listAccessTokenSlashCommand`** keeps pointing at the stage component. **Do not** reintroduce a `TOKEN_LIST_COMMANDS`-style map.
4. **Empty tokens:** keep immediate assistant message (no stage)—same as old `tryHandleTokenListSlashSubmit` empty branch.

**Tests:** Vitest via `runInteractive`: add token → `/list-access-token` → assert highlighted row / confirmation after Enter / default in `access-tokens.json`. **Update** the existing test that expects Esc to dump the **full numbered list** into the transcript (`InteractiveCliApp.addAccessToken.test.tsx`); revived behavior is **abort-style** dismiss for the picker, not “commit list on Esc.”

**Phase-complete when:** Phase 1 behavior is covered by Vitest; `pnpm cli:test` green.

---

## Phase 2 — Bare remove + remove-completely pickers + tests

**Status:** Done (`pnpm cli:test`). Shared UI is **`AccessTokenLabelPickerStage`** in `accessToken/`; **`RemoveAccessTokenPickerStage`** / **`RemoveAccessTokenCompletelyPickerStage`** colocate copy and outcomes; **`InteractiveCliApp`** opens stage when `stageComponent` is set and either there is no `argumentName` (list) or the argument is empty (remove commands).

**User-visible slice:** **`/remove-access-token`** and **`/remove-access-token-completely`** with **no** label open the same picker pattern; **Enter** runs local remove or full revoke+remove; errors match current non-interactive semantics.

**Implementation sketch:**

1. Introduce **`stageComponent`** on both remove slash commands (or one shared component with closure—avoid duplicating Ink trees).
2. **`InteractiveCliApp`:** when `command.stageComponent` exists and `argument` is empty, mount stage **even if** `argumentName` is set; when argument non-empty, use existing **`run`**.
3. **Parameterize** the shared picker (props or small per-command stage wrappers) for **`remove`** and **`remove-completely`**—stage titles and `onSettled` outcomes live **next to each slash command**, not in a shared line→config map.
4. **Async:** old code used `runInteractiveFetchWait`; that stack is gone. Use the same pattern as other async stages (e.g. **`AsyncAssistantFetchStage`** or a minimal `useEffect` + loading line) so the session stays responsive and failures surface as assistant text.

**Tests:** Vitest: two tokens → bare remove → pick second → file content; bare remove-completely with **mocked backend** if the suite already does that for labeled remove—**prefer reusing** existing test HTTP setup from `InteractiveCliApp.addAccessToken.test.tsx` / related tests **without** new mock layers on Ink.

**Phase-complete when:** Both flows covered; no regression on labeled `run` paths.

---

## Phase 3 — `MainInteractivePrompt` cohesion (shared list index math)

**User-visible slice:** None intended—**refactor only**; slash suggestion **↑↓** wrapping should behave identically before/after.

**Implementation sketch:** Replace inline modulo highlight updates with **`cycleListSelectionIndex`** from `selectListInteraction`. Run existing **`MainInteractivePrompt` tests** and add a regression case only if coverage gap appears.

**Phase-complete when:** Tests green; duplication of wrap logic removed.

---

## Phase discipline (checklist)

Before treating the revival as done:

1. Remove dead branches from `selectListInteraction` if nothing uses `slash-and-number-or-highlight`.
2. **No** revived `TOKEN_LIST_COMMANDS` (or similar global slash→picker tables); if two commands share a picker, share **code** (component + helpers), not a **third** list of slash lines.
3. Update this doc: mark phases done, delete stale notes, then remove or archive when shipped.

**Reference commits (last ~3 days context):** feature build-up `77404c264`, `0fde172d1`, `af9f51054`, …; strip-down / removal `cc94684a7`, `a4c4be63a`, `c5f9efa39`, `2e18a025a`, `9664c3a87`; E2E/plan cleanup `bdd62004f`. **Historical source for UX copy and flows:** tree at **`2e18a025a^`** (`accessTokenListStage.tsx`, `tokenListCommands.ts`, `selectListInteraction.ts`). **Do not** resurrect `tokenListCommands.ts` as a design; wire pickers from command-local code instead.
