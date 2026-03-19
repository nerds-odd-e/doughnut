# CLI Current Guidance: Truncation vs Line Wrapping

## Summary

In narrow terminals, the Current guidance area (below the input box) shows:
- **Command completion choices** – truncates with "..." ✓ (Phase 1 done)
- **Access token list** – currently line-wraps (ugly)
- **MCQ choices** – should use line wrapping; other MCQ bugs are out of scope

**Goal**: Command completion and access token list should truncate with "..." when lines exceed terminal width. MCQ choices should NOT be truncated and should use line wrapping only.

## Current Architecture

| Content Type       | Source Function                        | Wrapper                 | Truncation           | Output                      |
|--------------------|----------------------------------------|------------------------|---------------------|-----------------------------|
| Command completion | `formatCommandCompletionLines` (help.ts) | `formatHighlightedList` | `truncateToWidth` ✓ | `  /usage        description` |
| Access token list  | `formatTokenLines` (accessToken.ts)   | `formatHighlightedList` | `truncateToWidth` ✓ | `  ★ label` or `    label` |
| MCQ choices        | `formatMcqChoiceLines` (renderer.ts)  | `formatHighlightedList` | none                | `  1. choice text`         |

Command completion flow: `buildSuggestionLines(buffer, highlightIndex, width, options?)` → `formatHighlightedList(formatCommandCompletionLines(filtered), CURRENT_GUIDANCE_MAX_VISIBLE, highlightIndex)` → `.map(truncateToWidth(·, width))`. Hint path (`/ commands`) also truncated.

## Testing Strategy

**E2E tests are not needed** for this task.

**CLI-related E2E tests must pass** at each and every phase. Do not introduce regressions in existing CLI E2E scenarios.

**Unit tests only**, starting at the **top level**: exercise the public functions that produce Current guidance lines. Assert on observable output (line content, length, presence of "...")—never on internal state or helper functions. Avoid revealing implementation details.

Top-level functions: `buildSuggestionLines(buffer, highlightIndex, width)`, `buildTokenListLines(...)` (Phase 2), `formatMcqChoiceLines(choices, width)` (Phase 3). Tests call with narrow/wide widths and assert on line length and presence of "...". Phase 1: tests in `interactive.test.ts` (buildSuggestionLines) and `help.test.ts` (formatCommandCompletionLines, formatHighlightedList scroll).

## Phases

### Phase 1: Truncate command completion choices ✓

**User value**: Command suggestions stay on one line in narrow terminals; no messy wrapping.

**Done**:
- `buildSuggestionLines(buffer, highlightIndex, width, options?)` – width param, optional `forceCommandsHint` for dismissed case
- `truncateToWidth(str, width)` – ANSI-aware truncation, appends "..."
- `formatCommandCompletionLines` (help.ts) – renamed from formatCommandSuggestionLines
- Removed dead `formatCommandSuggestions`, inlined `formatCommandSuggestionsWithHighlight`
- `TerminalWidth` type alias

---

### Phase 2: Truncate access token list ✓

**User value**: Token labels stay on one line in narrow terminals.

**Done**:
- `buildTokenListLines(tokens, defaultLabel, width, highlightIndex)` in renderer.ts – formats via `formatTokenLines`, `formatHighlightedList`, then `truncateToWidth()` per line
- Used from `ttyAdapter` in `getDisplayContent()` instead of raw `formatTokenLines` + `formatHighlightedList`

**Tests** (accessToken.test.ts):
- Narrow width: each line ≤ width or ends with "..."; long label truncated with "..."
- Wide width: no truncation

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

- `visibleLength()` and `truncateToWidth()` in `renderer.ts` – ANSI-aware; truncateToWidth uses fresh RegExp per call to avoid global-state bugs.
- `getTerminalWidth()` via `process.stdout.columns || 80`.
- Phase 3: consider `wrapToWidth(str, width)` for MCQ (markdansi's `renderMarkdownToTerminal` may suffice).
- Truncation lives in `renderer.ts`; `formatHighlightedList` (listDisplay.ts) stays generic (scroll, highlight only); `CURRENT_GUIDANCE_MAX_VISIBLE` in listDisplay.ts names the scroll window height.
