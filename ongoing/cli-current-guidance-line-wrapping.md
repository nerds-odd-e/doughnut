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

## Phases

### Phase 1: Truncate command completion choices

**User value**: Command suggestions stay on one line in narrow terminals; no messy wrapping.

**Changes**:
1. Add `truncateToWidth(line: string, width: number): string` in `renderer.ts` (or a shared util). Use `visibleLength()` for ANSI-aware truncation; append "..." when truncating.
2. Pass terminal width into `buildSuggestionLines` (or the command-suggestion path). Apply truncation to each line from `formatCommandSuggestionLines` before passing to `formatHighlightedList`.

**Tests**:
- Unit: `truncateToWidth` – truncates at width, adds "..."; handles ANSI; no change if within width.
- Unit: Command suggestions truncated when width is small (mock `getTerminalWidth`).
- E2E: Narrow terminal; type `/` prefix; Current guidance shows truncated lines without wrap.

---

### Phase 2: Truncate access token list

**User value**: Token labels stay on one line in narrow terminals.

**Changes**:
1. In `ttyAdapter.getDisplayContent()`, when `tokenListItems` is set, apply truncation to each line from `formatTokenLines` before `formatHighlightedList`. Use `getTerminalWidth()`.

**Tests**:
- Unit: Token list lines truncated when width is small.
- E2E: Narrow terminal; `/remove-access-token` or similar; Current guidance shows truncated token list.

---

### Phase 3: MCQ choices – no truncation, line wrapping only

**User value**: Long MCQ choice text wraps to multiple lines; never truncated.

**Changes**:
1. Ensure MCQ path does NOT use truncation (already the case once Phase 1–2 are done; verify).
2. Add explicit word-wrap for MCQ choices so each choice wraps at terminal width. `formatMcqChoiceLines(choices, width)` should return multiple lines per choice when needed (e.g. using a wrap utility or `renderMarkdownToTerminal` which already supports width).
3. Adjust `formatHighlightedList` usage for MCQ: either (a) accept multi-line items and map highlight correctly, or (b) introduce `formatMcqChoiceLinesWithWrap` that returns flat lines with metadata for which lines belong to which choice, and handle highlight/scroll per “choice” not per “line”.

**Note**: MCQ behavior is still buggy (e.g. highlight/scroll across wrapped choices). This phase only guarantees: no truncation and proper line wrapping.

**Tests**:
- Unit: Long MCQ choice produces multiple wrapped lines; no truncation.
- E2E: Narrow terminal; recall with long choice; Current guidance shows wrapped choices, no "...".

---

## Implementation Notes

- `visibleLength()` in `renderer.ts` already strips ANSI for length; reuse for truncation.
- `getTerminalWidth()` is available via `process.stdout.columns || 80`; inject or use where width is needed.
- Consider a shared `wrapToWidth(str, width)` for MCQ if no existing helper (markdansi's `renderMarkdownToTerminal` wraps; may suffice for choice text).
- Keep high cohesion: truncation vs wrapping logic should live in one place (e.g. `renderer.ts` or `listDisplay.ts`).
