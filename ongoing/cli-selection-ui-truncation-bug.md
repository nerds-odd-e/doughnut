# CLI Selection UI Truncation Bug (Phase Plan)

## Problem Summary

Since commits c81a70edb and 0b1718109, the CLI selection menu has rendering bugs on narrow screens when options are truncated:

1. **Gray background overlay** – Covers options and the entire input box area
2. **Missing selection highlight** – No white (REVERSE) background on the selected item when partial input is `/list`
3. **Gray area from input box to mid-list** – When partial input is `/`, a gray block extends from input box downward
4. **Highlight inconsistency** – White highlight not always shown when changing selections with arrow keys

## Root Cause Analysis

### Commits introduced

- **c81a70edb**: Added `truncateToWidth()` for command suggestions and applied it per line in `buildSuggestionLines`
- **0b1718109**: Refactored to `formatCommandCompletionLines` + `formatHighlightedList`; both `buildSuggestionLines` and `buildTokenListLines` now truncate each line via `truncateToWidth`

### Bug mechanism

`truncateToWidth` (renderer.ts:68-86) truncates ANSI-decorated strings (e.g. `${REVERSE}  /list-access-token   List stored a${RESET}`). When truncating, it returns `${result}...` **without appending RESET** (`\x1b[0m`). Result:

1. Output becomes e.g. `\x1b[7m  /list-access-to...` (REVERSE never reset)
2. Terminals persist SGR state until RESET; REVERSE or GREY bleeds to subsequent output
3. Cascading styles produce the gray block and obscure the intended highlight

### Affected flows

- `buildSuggestionLines` – command completion (e.g. `/`, `/list`)
- `buildTokenListLines` – access token selection
- Any truncated line with ANSI codes from `formatHighlightedList` (REVERSE for selected, GREY for unselected)

## Phase Plan

### Phase 1: Failing tests ✓ DONE

**Goal**: Add failing tests that reproduce the bug; confirm failures; then skip tests and wait for user commit before fixing.

**Tests to add** (top-level, no internal state inspection):

1. **`cli/tests/renderer.test.ts`** (or extend `interactive.test.ts`):
   - `truncateToWidth`: When truncating an ANSI-decorated string (REVERSE or GREY), the result must end with `\x1b[0m` before `...` to avoid state bleed.
   - Assertion: `expect(truncateToWidth('\x1b[7mhello world\x1b[0m', 8)).toMatch(/\x1b\[0m\.\.\.$/)`
   - This tests the direct bug; low risk of false positive.

2. **`cli/tests/interactive.test.ts`** (buildSuggestionLines describe block):
   - `buildSuggestionLines with narrow width: every line containing ANSI codes must end with RESET`
   - Use `buildSuggestionLines('/list', 0, 25)` and `buildSuggestionLines('/', 0, 30)` to force truncation.
   - Assertion: For each line, if `line.includes('\x1b')` then `line.match(/\x1b\[0m$/)` (line must end with RESET). Non-truncated lines already pass; truncated lines fail.
   - Top-level, exercises full pipeline (formatCommandCompletionLines → formatHighlightedList → truncateToWidth).

3. **`buildTokenListLines`** (optional; same bug):
   - If tokens with long labels are truncatable, add a similar test for `buildTokenListLines` with narrow width. Lower priority since command completion is the primary user-visible case.

**Verification steps** (when executing Phase 1):

1. Add tests; run `CURSOR_DEV=true nix develop -c pnpm cli:test`; confirm tests **fail**
2. Mark failing tests with `.skip` (e.g. `test.skip(...)`)
3. Run tests again; confirm they are skipped and suite passes
4. Commit Phase 1 and wait for user

### Phase 2: Fix (after user commits Phase 1)

**Change** (renderer.ts `truncateToWidth`):

- When truncating (`visibleCount + 1 > maxVisible`), return `${result}\x1b[0m...` instead of `${result}...`
- Import RESET from ansi.js and use it for consistency: `${result}${RESET}...`

**Verification**:

1. Remove `.skip` from Phase 1 tests
2. Run `pnpm cli:test`; all tests pass
3. Manual check: run `pnpm cli`, type `/` and `/list` in a narrow terminal (~40 cols); verify no gray overlay and selection highlight visible

## Avoiding False Positives

- Assert ANSI invariants (RESET before `...`, lines ending with RESET when they contain ANSI), not terminal pixels
- Use fixed widths (25, 30) that guarantee truncation with current command set
- Test `truncateToWidth` directly with known ANSI input for deterministic failure

## Notes

- The "white highlight not always shown" and "gray area from input to mid-list" are both explained by ANSI bleed; fixing `truncateToWidth` should resolve all symptoms
- If Phase 2 leaves remaining issues, add a follow-up phase based on new findings
