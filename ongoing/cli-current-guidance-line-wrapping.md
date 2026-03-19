# CLI Current Guidance: Truncation vs Line Wrapping

## Summary

In narrow terminals, the Current guidance area (below the input box) shows:
- **Command completion choices** – currently line-wraps (ugly)
- **Access token list** – currently line-wraps (ugly)  
- **MCQ choices** – should use line wrapping; other MCQ bugs are out of scope

**Goal**: Command completion and access token list should truncate with "..." when lines exceed terminal width. MCQ choices should NOT be truncated and should use line wrapping only.

## Current Architecture

| Content Type            | Source Function                         | Wrapper                    | Output |
|-------------------------|------------------------------------------|----------------------------|--------|
| Command completion      | `formatCommandSuggestionLines` (help.ts) | `formatHighlightedList`    | `  /usage        description` |
| Access token list       | `formatTokenLines` (accessToken.ts)      | `formatHighlightedList`    | `  ★ label` or `    label` |
| MCQ choices             | `formatMcqChoiceLines` (renderer.ts)     | `formatHighlightedList`    | `  1. choice text` |

All are rendered via `ttyAdapter`'s `drawBox()` / `doFullRedraw()`, which write each `suggestionLines` element with `process.stdout.write(`\x1b[2K${line}\n`)`. Lines longer than `process.stdout.columns` wrap at the terminal (no explicit truncation or wrapping).

## Testing Strategy

**E2E tests are not needed** for this task.

**CLI-related E2E tests must pass** at each and every phase. Do not introduce regressions in existing CLI E2E scenarios.

**Unit tests only**, starting at the **top level**: exercise the public functions that produce Current guidance lines. Assert on observable output (line content, length, presence of "...")—never on internal state or helper functions. Avoid revealing implementation details.

To enable this, expose top-level functions that take `(state, width)` and return the lines to display:
- Command suggestions: `buildSuggestionLines(buffer, highlightIndex, width)` (already exists; add `width`).
- Token list: extract `buildTokenListLines(tokens, defaultLabel, width)`.
- MCQ: `formatMcqChoiceLines(choices, width)` (add `width`).

Tests call these with narrow/wide widths and assert on the returned strings. Coverage is achieved by exercising all branches through these public entry points.

## Phases

### Phase 1: Truncate command completion choices ✓

**User value**: Command suggestions stay on one line in narrow terminals; no messy wrapping.

**Changes**:
1. Add width parameter to `buildSuggestionLines(buffer, highlightIndex, width)`. Apply truncation internally.
2. Use `visibleLength()` for ANSI-aware truncation; append "..." when truncating.

**Tests** (top-level only):
- Call `buildSuggestionLines` with buffer like `/rec`, narrow width (e.g. 40). Assert each returned line has visible length ≤ width or ends with "...". Wide width: no truncation.
- Various prefix + width combinations to cover edge cases.

---

### Phase 2: Truncate access token list

**User value**: Token labels stay on one line in narrow terminals.

**Changes**:
1. Extract `buildTokenListLines(tokens, defaultLabel, width)` (e.g. in `accessToken.ts` or `renderer.ts`). Returns raw lines; truncate when exceeding width.
2. Use from `ttyAdapter` in `getDisplayContent()`.

**Tests** (top-level only):
- Call `buildTokenListLines` with tokens, narrow width. Assert each line ≤ width or ends with "...". Long label: truncated with "...".
- Wide width: no truncation.

---

### Phase 3: MCQ choices – no truncation, line wrapping only

**User value**: Long MCQ choice text wraps to multiple lines; never truncated.

**Changes**:
1. Ensure MCQ path does NOT use truncation.
2. Add width to `formatMcqChoiceLines(choices, width)`. Word-wrap each choice at width; return multiple lines per choice when needed.
3. Adjust `formatHighlightedList` usage for MCQ if multi-line items require it.

**Note**: MCQ behavior is still buggy (e.g. highlight/scroll across wrapped choices). This phase only guarantees: no truncation and proper line wrapping.

**Tests** (top-level only):
- Call `formatMcqChoiceLines` with long choice, narrow width. Assert output has multiple lines, no "...", each line ≤ width.
- Short choices: single line per choice. Wide width: no wrapping.

---

## Implementation Notes

- `visibleLength()` in `renderer.ts` already strips ANSI for length; reuse for truncation.
- `getTerminalWidth()` is available via `process.stdout.columns || 80`; inject or use where width is needed.
- Consider a shared `wrapToWidth(str, width)` for MCQ if no existing helper (markdansi's `renderMarkdownToTerminal` wraps; may suffice for choice text).
- Keep high cohesion: truncation vs wrapping logic should live in one place (e.g. `renderer.ts` or `listDisplay.ts`).
