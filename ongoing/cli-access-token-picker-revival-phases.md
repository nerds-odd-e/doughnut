# Phased plan: revive access-token list selection (interactive CLI)

**Intent:** Bring back **↑ / ↓ to move highlight**, **Enter to confirm**, and **Esc to dismiss** for choosing a stored token label—without reintroducing the old split-brain between shell session state, duplicate keyboard paths, and scattered command logic.

**Note on naming:** The historical behavior was tied to **`/list-access-token`** (change persisted default) and bare **`/remove-access-token`** / **`/remove-access-token-completely`**. If “`/less-access-token`” was meant literally, there is no such command in recent history; treat it as **`/list-access-token`**.

**Testing (per request):** No new Cypress/E2E. Cover with **high-level Vitest** driving **`runInteractive` + real stdin** (and temp config dir), same style as `cli/tests/InteractiveCliApp.addAccessToken.test.tsx`—**no mocks** of Ink adapters or command internals. Prefer **one** focused describe block (or extend that file) so behavior stays grouped.

**Architecture goals:**

- **One policy module** for “highlight-only list” keys: map Ink input → move highlight / submit index / abort. This is the old `cli/src/interactions/selectListInteraction.ts` (removed with `9664c3a87` / surrounding strip-down; consumer `accessTokenListStage.tsx` removed in `2e18a025a`). Restore the **highlight-only + `abort-list`** path and **`cycleListSelectionIndex`**; keep **`slash-and-number-or-highlight`** only if recall or another flow still needs it—otherwise omit dead branches to stay minimal.
- **One place for “which slash lines open which token action”:** revive the idea of `TOKEN_LIST_COMMANDS` (previously `cli/src/shell/tokenListCommands.ts`) as a **single map** from exact slash line → `{ action, stageIndicator?, currentPrompt? }`. Slash command **registration** (`interactiveSlashCommands`) should not duplicate that table.
- **One Ink stage** for the picker UI + input handling, parameterized by **action** (`set-default` | `remove` | `remove-completely`), not three copy-paste components.
- **Dedup with `MainInteractivePrompt.tsx`:** today slash suggestions use local modulo logic for highlight (`ttyArrowKeyUsesSlashSuggestionCycle` + inline wrap). After restoring `cycleListSelectionIndex`, **call the shared helper** from `MainInteractivePrompt` so token list and slash command list do not diverge on wrap semantics. Do **not** force one mega-component: slash completion still needs **caret gating** and **Enter-to-fill-buffer**; only the **index math** (and optionally Ink→event mapping via `selectListKeyEventFromInk`) should be shared.

**Product behavior to match (from `2e18a025a^` `accessTokenListStage.tsx` + `tokenListCommands.ts`):**

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

**Persistence:** Add **`setDefaultTokenLabel(label)`** (or equivalent) next to existing `getDefaultTokenLabel` / `loadAccessTokenConfig` in `accessTokenStorage.ts`—it existed in the pre-strip codebase but is missing now.

---

## Phase 1 — Default token: picker + `setDefaultTokenLabel` + tests

**Status:** Done (Vitest green: `pnpm cli:test`). Stage keyboard input is forwarded from `InteractiveCliApp` via `SetStageKeyHandlerContext` so Ink delivers keys after swapping off `MainInteractivePrompt`.

**User-visible slice:** With at least one stored token, **`/list-access-token`** opens a picker; **↑↓** moves selection (start at current default if still present); **Enter** persists default and shows confirmation; **Esc** cancels with the shared abort line.

**Implementation sketch:**

1. Restore **`selectListInteraction`** (+ focused **direct** unit tests for `cycleListSelectionIndex`, `dispatchSelectListKey` highlight-only, `selectListKeyEventFromInk` bare `\r`/`\n`—restore from `77ae2317f^:cli/tests/selectListInteraction.test.ts` trimmed if recall-only cases are dropped).
2. Add **`setDefaultTokenLabel`** to storage; export via `accessToken.ts` if that remains the façade.
3. Replace or evolve **`ListAccessTokenStage`** into the unified **`AccessTokenPickerStage`** (name flexible) driven by props or a small wrapper so **`listAccessTokenSlashCommand`** still points at one component with **`action: 'set-default'`** and copy from the old `TOKEN_LIST_COMMANDS['/list-access-token']`.
4. **Empty tokens:** keep immediate assistant message (no stage)—same as old `tryHandleTokenListSlashSubmit` empty branch.

**Tests:** Vitest via `runInteractive`: add token → `/list-access-token` → assert highlighted row / confirmation after Enter / default in `access-tokens.json`. **Update** the existing test that expects Esc to dump the **full numbered list** into the transcript (`InteractiveCliApp.addAccessToken.test.tsx`); revived behavior is **abort-style** dismiss for the picker, not “commit list on Esc.”

**Phase-complete when:** Phase 1 behavior is covered by Vitest; `pnpm cli:test` green.

---

## Phase 2 — Bare remove + remove-completely pickers + tests

**User-visible slice:** **`/remove-access-token`** and **`/remove-access-token-completely`** with **no** label open the same picker pattern; **Enter** runs local remove or full revoke+remove; errors match current non-interactive semantics.

**Implementation sketch:**

1. Introduce **`stageComponent`** on both remove slash commands (or one shared component with closure—avoid duplicating Ink trees).
2. **`InteractiveCliApp`:** when `command.stageComponent` exists and `argument` is empty, mount stage **even if** `argumentName` is set; when argument non-empty, use existing **`run`**.
3. Wire actions **`remove`** and **`remove-completely`** from the central **`TOKEN_LIST_COMMANDS`** map (stage titles as before).
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
2. No duplicate `TOKEN_LIST_COMMANDS` vs per-file magic strings for the same slash lines.
3. Update this doc: mark phases done, delete stale notes, then remove or archive when shipped.

**Reference commits (last ~3 days context):** feature build-up `77404c264`, `0fde172d1`, `af9f51054`, …; strip-down / removal `cc94684a7`, `a4c4be63a`, `c5f9efa39`, `2e18a025a`, `9664c3a87`; E2E/plan cleanup `bdd62004f`. **Source of truth for behavior:** tree at **`2e18a025a^`** for `cli/src/ui/accessTokenListStage.tsx`, `cli/src/shell/tokenListCommands.ts`, `cli/src/interactions/selectListInteraction.ts`.
