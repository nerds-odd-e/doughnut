# Phased review: `InteractiveShellDeps`

**Intent:** `InteractiveShellDeps` bundles recall state, slash-command processing, access-token I/O, placeholder context, and static command config into one object passed from `interactive.ts` into Ink + the TTY adapter. That boundary leaks **where** state lives and **which** domain concepts the UI must know about.

**Approach:** One **phase per interface member** (18 phases). Each phase:

1. **Usages** — grep / read call sites; note files (not line-perfect; refresh when executing the phase).
2. **Inline?** — Could the implementation live at the use site without duplicating logic or pulling `interactive.ts` into a worse dependency cycle?
3. **Idiomatic alternative** — e.g. direct import from a leaf module, React Context for a stable slice, explicit port type per concern, reducer-owned session state lifted from module globals.

**Outcome per phase:** Written decision (keep as-is / move / merge with another member); if code moves, keep **`pnpm cli:test`** green and prefer driving **`runInteractive`** (or existing Vitest surfaces) over new deps-only tests unless a member becomes a **deliberate pure contract** (see `.cursor/rules/planning.mdc` — observable behavior first).

**Suggested execution order:** Start with members that are already **thin re-exports** or **static config** (clearer wins, lower coupling risk), then **placeholder / recall read models**, then **mutators + `processInput`** (highest coupling to `interactive.ts`).

---

## Phase 1 — `listAccessTokens`

1. **Usages:** `buildInteractiveShellDeps` → `InteractiveApp` (token picker / listing). Slash handling uses it inside `processInput` in `interactive.ts` (not via deps there).
2. **Inline?** `InteractiveApp` could import `listAccessTokens` from `commands/accessToken.js` directly — **no** duplication if the function stays the single source of truth.
3. **Alternatives:** Direct import from `accessToken` module; keep injection only if tests need to swap storage (currently they do not).
4. **Outcome (done):** **Move** — `InteractiveApp` imports `listAccessTokens` from `accessToken.js`; removed from `InteractiveShellDeps` and `buildInteractiveShellDeps`. `interactive.ts` still imports it for `processInput` slash paths.

## Phase 2 — `removeAccessToken`

1. **Usages:** `InteractiveApp` (token picker remove path).
2. **Inline?** Same as phase 1 — direct `accessToken` import is viable.
3. **Alternatives:** Same.
4. **Outcome (done):** **Move** — `InteractiveApp` imports `removeAccessToken` from `accessToken.js`; removed from `InteractiveShellDeps` and `buildInteractiveShellDeps`. `interactive.ts` still imports it for `processInput` slash paths.

## Phase 3 — `removeAccessTokenCompletely`

1. **Usages:** `InteractiveApp` (async remove with fetch-wait line); string label in `interactiveFetchWait.ts` is separate.
2. **Inline?** Direct import from `accessToken` — viable.
3. **Alternatives:** If fetch-wait + token removal stay coupled, a small `tokenRemovalWithWait(adapter, …)` helper could live next to `InteractiveApp` or `interactiveFetchWait` without keeping it on `InteractiveShellDeps`.
4. **Outcome (done):** **Move** — `InteractiveApp` imports `removeAccessTokenCompletely` from `accessToken.js`; removed from `InteractiveShellDeps` and `buildInteractiveShellDeps`. `interactive.ts` still imports it for `processInput` slash paths.

## Phase 4 — `setDefaultTokenLabel`

1. **Usages:** `InteractiveApp` (token picker default action).
2. **Inline?** Direct `accessToken` import — viable.
3. **Alternatives:** Same.

## Phase 5 — `getDefaultTokenLabel`

1. **Usages:** `ShellSessionRoot` (default label for token picker UI).
2. **Inline?** `ShellSessionRoot` importing `accessToken` — viable; watch bundle boundaries if `ui/` should stay free of command modules (today it already depends on many `cli/src` modules).
3. **Alternatives:** Pass only `defaultLabel: AccessTokenLabel | undefined` as **derived props** from `InteractiveApp` if you want `ShellSessionRoot` free of storage knowledge.

## Phase 6 — `TOKEN_LIST_COMMANDS`

1. **Usages:** `ShellSessionRoot` (lookup by `session.tokenSelection.command`); `InteractiveApp` destructures for handlers (verify all uses).
2. **Inline?** Moving the record to e.g. `commands/` or `shell/tokenListCommands.ts` as **pure data + types** removes “interactive module owns config” leakage; consumers import one module.
3. **Alternatives:** Co-locate with `AccessTokenPickerCommandConfig` / slash registration so **one place** defines both user-visible command and TTY stage copy.

## Phase 7 — `getStopConfirmationYesOutcomeLines`

1. **Usages:** `InteractiveApp` — commits assistant lines after “yes” on stop recall confirm.
2. **Inline?** Trivial constant at call site risks drift if another path should show the same text; better a **named constant** in recall or `renderer` copy module.
3. **Alternatives:** Constant export `RECALL_STOP_CONFIRM_YES_LINES`; deps object drops this field.

## Phase 8 — `getRecallSessionYesNoInkGuidanceLines`

1. **Usages:** `ShellSessionRoot` (current guidance for recall session y/n).
2. **Inline?** If body is static lines, move to `recallYesNo` or `renderer` as exported lines; **single source** for TTY + any non-Ink path.
3. **Alternatives:** Same as phase 7 — data export instead of callback.

## Phase 9 — `getRecallStopConfirmInkModel`

1. **Usages:** `ShellSessionRoot` (builds stop-confirm prompt for Ink).
2. **Inline?** Implementation is recall-specific; **import from `interactions/recallYesNo.js` (or adjacent)** is cleaner than a closure on `interactive.ts` module state **if** the function can take explicit inputs instead of reading `pendingRecallAnswer` globals.
3. **Alternatives:** **Lift recall pending state** into a reducer or session object passed as props — larger refactor; only if other phases show globals are the real pain.

## Phase 10 — `getRecallCurrentPromptWrappedLines`

1. **Usages:** `ShellSessionRoot` (MCQ/spelling current prompt block).
2. **Inline?** Same as phase 9 — depends on recall pending state; inlining without moving state duplicates wrapping rules (`terminalLayout` / markdown) if done wrong.
3. **Alternatives:** Pure function `(pendingRecall, width) => lines | null` in a recall UI helper module; `interactive.ts` supplies `pending` snapshot into React state or a ref.

## Phase 11 — `getNumberedChoiceListChoices`

1. **Usages:** `ShellSessionRoot`, `InteractiveApp`, `interactiveTtySession` (`isAlternateLivePanel` / input-ready OSC), plus `ShellSessionRoot` export `isAlternateLivePanel`.
2. **Inline?** **High fan-out** — any change must stay consistent across **Ink layout** and **raw mode / OSC** policy. Inlining three copies is bad; **one exported function** (recall module) is better than three `deps` call sites.
3. **Alternatives:** Derive “is MCQ selection mode” from **session + explicit flag** in `ShellSessionState` updated synchronously when recall sets choices, so TTY layer does not call back into `interactive.ts` getters (requires recall writes to session).

## Phase 12 — `getPlaceholderContext`

1. **Usages:** `ShellSessionRoot`, `InteractiveApp` (several branches), `interactiveTtySession` (placeholder compare for recall y/n and alternate panel).
2. **Inline?** **Wide** — encodes “what kind of prompt is active.” Hard to inline without duplicating the `inTokenList` rules.
3. **Alternatives:** Replace with a small **discriminated union** on session or a dedicated `PromptMode` enum carried in `ShellSessionState` (updated from recall + token flows) so UI and TTY read **state** not **callbacks to interactive.ts**.

## Phase 13 — `shouldRecordCommittedLineInUserInputHistory`

1. **Usages:** `InteractiveApp` (`rememberCommittedLine`).
2. **Inline?** Logic is “no history while recall expects an answer” — duplicates recall rules if copy-pasted.
3. **Alternatives:** Same as phase 12 — **derive from session/recall phase enum**; or one pure helper `shouldRecordHistoryForShellPhase(phase)` in a neutral module.

## Phase 14 — `isPendingStopConfirmation`

1. **Usages:** `ShellSessionRoot`, `interactiveTtySession` / `isAlternateLivePanel`.
2. **Inline?** Do not duplicate; it is global recall flag today.
3. **Alternatives:** **Session state flag** `stopConfirmPending` set only from recall exit flow; TTY and Ink read the same ref/session.

## Phase 15 — `setPendingStopConfirmation`

1. **Usages:** `InteractiveApp` only (enter/exit stop confirm).
2. **Inline?** Coupled to recall implementation in `interactive.ts`.
3. **Alternatives:** `patch` session + recall reducer; or `dispatchRecall({ type: 'setStopConfirm', value })` from a narrow module imported by `InteractiveApp`.

## Phase 16 — `isInCommandSessionSubstate`

1. **Usages:** `ShellSessionRoot` (loading stage indicator branch).
2. **Inline?** Same global recall session notion.
3. **Alternatives:** Explicit `recallSessionActive` or `recallLoading` on session state.

## Phase 17 — `exitCommandSession`

1. **Usages:** `InteractiveApp` (confirm “yes” on stop recall).
2. **Inline?** Must remain **one** implementation of “leave recall”; today `exitRecallMode` in `interactive.ts`.
3. **Alternatives:** Import `exitRecallMode` from a **recall controller module** that `InteractiveApp` is allowed to depend on, or pass via a **typed callback** kept as the only recall “command” on props (rename to `onExitRecallSession` for clarity).

## Phase 18 — `processInput`

1. **Usages:** `InteractiveApp` (submit line with `ttyOutput`, `interactiveUi: true`). Vitest and non-TTY paths use `processInput` directly from `interactive.ts`.
2. **Inline?** **No** — moving the full slash engine into `InteractiveApp` worsens cohesion and risks cycles; it is the main **facade** for commands.
3. **Alternatives:** Keep as **injected function** or **static import** of `processInput` from a module that does **not** import Ink; ensure `interactive.ts` stays a thin composition root or split into `processInput` module + `interactiveRun` entry. **Rename** on deps to `submitInteractiveLine` if it clarifies “this is the slash engine entry” vs generic “process.”

---

## After all phases

- Revisit whether `InteractiveShellDeps` should exist as **one** interface or **2–3 ports** (e.g. `AccessTokenStore`, `RecallShellPort`, `SlashEngine`) **only if** the per-member review shows stable groupings.
- Update or remove this document when the review is done or parked.
