# CLI interactive loading UI (current prompt + disabled input box)

Informal requirement; delete or shrink once implemented.

## Problem

- On TTY submit, `ttyAdapter` calls `clearLiveRegionForRepaint` then `await processInput(...)`. Until `processInput` resolves, nothing repaints the live region, so the **input box disappears** for the whole duration of network/async work (notably `recallNext` on `/recall`).
- Users want a clear **loading** state: **current prompt** shows an active wait message; **input box** stays visible but **disabled**, with placeholder `loading ...`.

## Design goals

1. **Current prompt** while waiting: e.g. `Loading recall questions` with **animated ellipsis** (cycle `.` / `..` / `...` or similar) at the end.
2. **One semantic text color** for “loading / in progress” in the terminal, defined once (e.g. in `cli/src/renderer.ts` or `ansi.ts` next to `GREY`), and reused for all loading current-prompt lines. **Use a blue-ish foreground** (e.g. standard blue `\x1b[34m` or bright blue `\x1b[94m`) — distinct from green separator, grey muted text, and the cyan **command** highlight (`COMMAND_HIGHLIGHT`). Pick the exact SGR in implementation; document in `.cursor/rules/cli.mdc` when done.
3. **Input box:** same **disabled** treatment as existing selection mode (grey border, no `→`, hidden cursor): **one code path**, not a third copy of “grey box + hide cursor” rules. Prefer extracting a small predicate, e.g. `isInputBoxDisabledPlaceholderContext(ctx): boolean`, true for `tokenList` and for the new loading context, and use it in:
   - `buildBoxLines` (no `PROMPT` prefix when disabled),
   - `buildLiveRegionLines` / `grayBoxLinesForSelectionMode` (whichever applies — today only `tokenList` gets gray box; extend so **loading** uses the same gray-box helper),
   - `ttyAdapter` `drawBox` / `doFullRedraw` (hide cursor when disabled).
4. **Placeholder:** add a `PlaceholderContext` value (e.g. `loading`) with `PLACEHOLDER_BY_CONTEXT.loading === 'loading ...'` (static ellipsis in the box; animation stays on the current prompt line above).

## Cohesive state model (avoid duplicate “am I loading?” logic)

- **Single source of truth** for “interactive session is waiting on async work” should live beside other recall/TTY-facing state in `interactive.ts` (or a tiny dedicated module imported by both `interactive.ts` and TTY deps), e.g.:
  - `setInteractiveLoading({ message: string } | null)` + getters `getInteractiveLoading()`, `getInteractiveLoadingDisplayMessage()` (base message; TTY appends animated dots).
- **`processInput`** (and any TTY-only path that awaits before redraw) calls `setInteractiveLoading` **immediately before** each backend/network `await` that can take noticeable time, and **`null` in `finally`** (or immediately after) so the flag never sticks on error.
- **TTY adapter:** after `clearLiveRegionForRepaint` and before `await processInput`, ensure **one** `drawBox()` runs once loading state is set (if the flag is set inside `processInput` at the very start of the slow branch, TTY may need a **micro-contract**: either set loading from TTY wrapper right before await, or have `processInput` invoke an optional `output.onLoadingChange?.()` so TTY can `drawBox()` without duplicating command lists). Prefer **OutputAdapter extension** with something like `notifyLoadingChanged?: () => void` called whenever loading state toggles, so TTY redraw stays in one place.
- **Animated ellipsis:** while loading is active, TTY starts a **short interval** (e.g. 300–500 ms) that only repaints the **live region** (reuse existing `drawBox` / live-region paint path), then **clears interval** when loading ends. Guard against overlapping timers and exit.

## Commands / code paths to cover

Audit **every** `await` in interactive `processInput` that can hit the network or run for a while, and the TTY-only await outside `processInput`:

| Area | Typical user-visible wait |
|------|---------------------------|
| `/recall` | `recallNext(0)` |
| Recall continuation | `continueRecallSession` → `recallNext` |
| After MCQ / spelling / y-n answer | `answerQuiz` / `answerSpelling` / `markAsRecalled` then `continueRecallSession` → `recallNext` |
| `/contest` | `contestAndRegenerate` |
| `/recall-status` | `recallStatus` |
| Param commands | `addAccessToken`, `createAccessToken`, `removeAccessTokenCompletely` |
| `/add gmail` | `addGmailAccount` |
| `/last email` | `getLastEmailSubject` |
| Token list “remove completely” | `removeAccessTokenCompletely` in `ttyAdapter` key handler (not `processInput`) — same loading API |

Use **message strings** centralized in one map or small table (e.g. `LOADING_MESSAGES.recallNext`, `LOADING_MESSAGES.contest`, …) so TTY animation + prompt text stay DRY.

**Piped / non-TTY:** `defaultOutput` can no-op `notifyLoadingChanged`; optional one-line stderr/stdout behavior is out of scope unless needed for tests.

## `writeCurrentPrompt` vs live region

Today, during recall, `showRecallPrompt` uses `writeCurrentPrompt` for multi-line prompts; TTY also renders **current prompt** from token-list config. Loading lines should appear **in the same live-region current-prompt strip** (green separator + colored loading text), not as raw `stdout` lines interleaved with a blank live region — align implementation with how token-list `currentPrompt` is wired through `getDisplayContent`.

## Testing (all phases)

**No new Cypress / `@interactiveCLI` E2E for this feature** in any phase. Loading text appears only during short, repainted TTY frames; cumulative PTY capture and `waitForInputBoxReady` timing make assertions **flaky**.

- **Unit tests only:** `cli/tests/recallLoadingUi.test.ts` (`pnpm cli:test`). Tests use **public exports** from `cli/src/interactive.js` and `cli/src/renderer.js` only (`LOADING_MESSAGES`, `processInput`, `resetRecallStateForTesting`, renderer helpers). Assert **observable behavior** (e.g. `OutputAdapter.notifyLoadingChanged` call pattern, rendered lines) — not private loading getters or `interactiveLoading` internals.
- Keep **few tests** with **dense assertions** so coverage stays complete without duplication across files.

## Documentation

- Update `.cursor/rules/cli.mdc` **in the phase that first exposes** the documented behavior (expand in later phases if new commands gain loading UX).
- Remove or trim this ongoing file once the last **implemented** phase is done (phase 3 may stay as a stub until researched).

## Suggested delivery phases

Each phase below is **complete on its own**: implementation + **UT** in `recallLoadingUi.test.ts` (extend as new surfaces appear) + `cli.mdc` when user-visible. **No E2E** for loading (see above).

1. **Recall fetch loading (interim vs other commands):** `LOADING_*` ANSI constant; `PlaceholderContext` + `loading`; shared disabled-box predicate (`tokenList` + loading); gray box for loading; loading state + `OutputAdapter` hook; TTY redraw + ellipsis timer; wire **every path that awaits `recallNext`** (`/recall`, `continueRecallSession`, including after MCQ/spelling/y-n answers). **Interim:** other commands in the table stay without loading until phase 2.

2. **Remaining async commands:** contest, recall-status, param commands, gmail, last email, recall answer handlers that await before next question, TTY token-list `removeAccessTokenCompletely`. **In this phase:** extend UT for new loading messages / `notifyLoadingChanged` behavior; extend `cli.mdc` if new surfaces need documenting.

If phase 2 is too large for one deploy, split into additional phases (e.g. contest + recall-status first, then param/gmail/email, then token list), each with its own UT updates — still **no** loading E2E.

3. **Cancellable long-running commands (TBD):** Let the user **abandon or interrupt** work that is stuck in loading (e.g. slow network). **Not specified here** — needs **research** first: what can be cancelled safely (in-flight `fetch`, local state rollback, OAuth flows, recall session integrity), which keys or gestures apply, and how this interacts with `readline`/PTY input while `processInput` is awaiting. When this phase is picked up, rewrite it into concrete UX + technical tasks, with **UT** (no loading E2E). **Depends on** phases 1–2 (loading UI must exist so “cancel” has a clear visual context).

## Out of scope (unless product asks)

- Non-interactive `-c` loading UX  
- **Cypress E2E for loading UI** (any phase; flaky)  
- Progress percentage (fine-grained); **broad cancellation** is deferred to **phase 3** above, not dismissed  
- Changing colors of existing `RECALLING_INDICATOR` / `GREY` hints (unless unified with loading palette deliberately)

---

**Status:** **Phase 1 implemented** — recall `recallNext` paths only; phase 2+ remain as above.
