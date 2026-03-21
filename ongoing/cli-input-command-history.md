# Interactive CLI: command history (↑↓), cursor rules, token masking, persistence

Informal plan. Delete or trim when implemented.

## Current baseline

- **TTY** (`cli/src/adapters/ttyAdapter.ts`): `buffer` is the draft line; typing appends and backspace only deletes from the **end**. Cursor placement always uses the **end of the last line** (`positionCursorInInputBox`).
- **↑↓ today**: When the last line is an incomplete `/` command with matches, arrows **cycle command suggestions** (`highlightIndex`). Otherwise arrows do nothing.
- **Scrollback**: On submit, `chatHistory` gets `{ type: 'input', content: … }` and `renderPastInput` paints history input (grey block). No separate “input history” deque and no persistence.
- **Add token**: `/add-access-token <token>` is handled in `interactive.ts` via `PARAM_COMMANDS`; the full line (including the secret) would currently be stored verbatim if we only push `buffer`.

## Design choices (decide before / while implementing)

### 1. ↑↓ vs command suggestion cycling

New rules require ↑↓ to move a **logical cursor** and then walk **input history**. That **overlaps** with today’s “↑↓ cycles slash-command suggestions.”

**Recommendation:** When suggestion mode would apply (`isCommandPrefixWithSuggestions` and not dismissed), **stop using ↑↓ for suggestion highlight**; keep **Tab** (and existing prefix completion) for disambiguation. Optionally add a short hint in Current guidance (e.g. mention Tab). This matches common shells (history on arrows, completion on Tab) and avoids ambiguous “second ↑” at column 0.

If the team prefers to keep arrow-based suggestion browse, spell an explicit precedence (e.g. only when `cursorOffset === 0` **and** some “suggestion focus” flag) — higher complexity.

### 2. Multiline buffer

Requirements are phrased for a single “input box.” **Default:** treat the draft as one string with optional `\n`; **cursor** is a single integer offset `0 … buffer.length` (UTF-16 code units, same as the rest of the CLI). “Beginning” = `0`, “end” = `buffer.length`. If multiline is rare, no per-line home/end unless you later add readline-style line-local motion.

### 3. Masked `/add-access-token` when recalled

Store and persist **only** the masked form (e.g. `/add-access-token <redacted>` or fixed `****`) for history. Recalling that line puts the **masked** text in the box; the user must paste a real token again to re-run. **Do not** write the raw token to the history file or to `chatHistory` input entries.

Scope: **`/add-access-token`** only unless product asks to extend (e.g. other param commands with secrets).

### 4. OSC / `lineDraft`

`interactiveInputReadyOscSuffix` uses `lineDraft === ''` to emit “ready” OSC. After adding a real cursor, keep behavior correct when the draft is non-empty (including while standing at column 0 with non-empty text).

## Phased delivery (scenario-first)

### Phase A — Input history behavior (TTY, in-process) ✅

**User-visible:** In interactive TTY mode, ↑↓ walk previous submissions; first ↑ moves cursor to start if not already there; first ↓ moves cursor to end if not already there; after ↑ into history, cursor at **beginning**; after ↓ within history, cursor at **end**; stepping ↓ past the “newest” stored line restores the **pre-history draft** (what the user had typed before first ↑), or **empty** if there was none.

**Done:** Domain state + transitions in `cli/src/interactiveCommandInput.ts` (Vitest: `cli/tests/interactiveCommandInput.test.ts`). TTY wiring in `cli/src/adapters/ttyAdapter.ts` (caret row/column in box, left/right/home/end, backspace before cursor, insert at cursor). **↑↓ no longer cycle slash-command suggestions** (use **Tab** / Enter on first match); see `cli/tests/interactive/interactiveTtySession.test.ts` and `interactiveTtySuggestionScroll.test.ts`. TTY smoke: `cli/tests/interactive/interactiveTtyInputHistory.test.ts`.

**Implementation sketch (historical):**

- Maintain a **deque/array** of submitted lines (trimmed non-empty, same eligibility as `isCommittedInteractiveInput`), cap ~100 in memory aligned with later persistence.
- Maintain **`historyBrowseIndex`**: `null` = editing fresh draft, `0` = most recent submitted, increasing = older (or the inverse — pick one convention and stick to it).
- Maintain **`historyDraftCache`**: snapshot of `buffer` when the user first presses ↑ from `historyBrowseIndex === null`; restored when leaving history forward past newest.
- **Cursor:** `cursorOffset`; update `positionCursorInInputBox` to map offset → row/column inside the bordered box (reuse `PROMPT` / continuation prefix widths from `buildBoxLines`). Insert printable characters **at** `cursorOffset`; backspace deletes **before** `cursorOffset` (readline-style).
- **Keys:** At minimum **left/right** (or equivalent) so a visible in-box cursor is usable; **Home/End** optional but cheap if already doing offset-based cursor.

**Tests:** Vitest on `interactiveCommandInput.ts` for transitions; focused **TTY** tests in `cli/tests/interactive/*.test.ts` where needed.

**Out of scope this phase:** Disk persistence; masking (can use plain test strings).

### Phase B — Mask `/add-access-token` in history input ✅

**User-visible:** After adding a token, the grey **history input** block and any **navigable** history entry show a redacted line, never the raw token.

**Done:** `maskInteractiveInputForHistory` in `cli/src/inputHistoryMask.ts`. TTY: all `chatHistory` input entries, `rememberCommittedLine` / `appendCommittedCommand`, and immediate `renderPastInput` before token-list mode use the masked string; `processInput` still receives the raw line. Piped interactive: `renderPastInput` uses the masked line. **Tests:** `cli/tests/inputHistoryMask.test.ts`, `cli/tests/interactive/interactiveTtyAddAccessTokenMask.test.ts`.

### Phase C — Persist last ~100 commands

**User-visible:** History survives CLI restart (same config dir as the rest of the app, `DOUGHNUT_CONFIG_DIR` / `configDir`).

**Implementation sketch:**

- New file under config dir, e.g. `cli-command-history.json` (name TBD), array of strings, **max length ~100** (truncate oldest when appending — exact number not critical).
- **Load** once when starting **TTY** interactive session; **append** on each successful commit of an input line (after masking).
- Avoid secrets on disk beyond masked lines (Phase B is a prerequisite).
- **Debounce or flush-on-exit:** simplest is synchronous append after each submit; if noisy, batch with `setImmediate` / short debounce still acceptable for ~100 strings.

**Tests:** Unit test persistence helper with temp config dir (pattern from `accessToken.test.ts` / `interactiveTestHelpers.ts`).

### Phase D — Hygiene

- Update **`cli.mdc`** terminology: document **input command history** vs **chat scrollback**, ↑↓ + cursor rules, masking for `/add-access-token`, persistence path/limit.
- **E2E:** Optional **one** `@interactiveCLI` scenario if stable; if flaky, rely on Vitest (per `cli.mdc` loading guidance). **Do not** delete existing tests without explicit approval.

## Files likely touched

| Area | Files |
|------|--------|
| TTY command line state | `cli/src/interactiveCommandInput.ts` |
| TTY key handling / cursor | `cli/src/adapters/ttyAdapter.ts` |
| Box painting / cursor column | `cli/src/renderer.ts` (`buildLiveRegionLines` / `buildBoxLines` callers, possibly helpers) |
| Masking | new small module or `renderer.ts`; call sites in `ttyAdapter` where input is committed |
| Persistence | new module + `configDir`; wire from `ttyAdapter` or interactive bootstrap |
| Docs | `.cursor/rules/cli.mdc`, this file |

## Non-goals (unless requested later)

- **Piped** interactive (`pipedAdapter.ts`): line-based `readline` without raw keypress — no arrow history unless switched to a richer input path.
- **Non-interactive** `-c` / install paths: no change.

## Checklist (phase discipline)

After each phase: tests green, no dead code, update this doc; then remove or archive when feature is done.
