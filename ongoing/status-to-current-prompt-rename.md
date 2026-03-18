# Rename Status to Current Prompt with Green Separator

## Summary

Rename "status" / "status line" to **Current Prompt**. Change identification from ANSI dim styling to a **green separator line** that delimits the Current Prompt area above the input box.

## Section Definitions

| Section | Identification |
|---------|-----------------|
| **Current Prompt** | From the green separator line down to (excluding) the input box. Empty if no separator exists. |
| **History** | The area before the Current Prompt separator (if it exists), or before the input box (if no separator). |
| **history-input** | Within History: lines with grey background (unchanged). |
| **history-output** | Within History: lines without grey bg and not in Current Prompt (unchanged logic, scoped to History). |

## Green Separator (Phase 1 – done)

- **ANSI**: `\x1b[32m` (green), `\x1b[0m` (reset)
- **Pattern**: `buildCurrentPromptSeparator(width)` in `renderer.ts` returns `GREEN + '─'.repeat(width) + RESET`
- **Parser detection**: `line.includes('\x1b[32m')` and `line.includes('─')`
- **Placement**: Before first `writeCurrentPrompt` call in each prompt turn (recall prompt, load more, stop confirm)

## Phase 1: Add green separator to CLI ✅

1. **renderer.ts**: `GREEN`, `buildCurrentPromptSeparator(width)`
2. **types.ts**: `OutputAdapter.writeCurrentPrompt`, `beginCurrentPrompt`; domain: Current Prompt
3. **ttyAdapter.ts**: `beginCurrentPrompt` writes separator before first prompt content in each turn; `writeCurrentPrompt` for hint lines
4. **interactive.ts**: Call `output.beginCurrentPrompt?.()` at start of `showRecallPrompt`, "Load more", "Stop recall?" flows
5. Piped adapter: no `beginCurrentPrompt` (optional, E2E uses PTY)

## Phase 2: Update E2E section parser

1. **cliSectionParser.ts** (use same `\x1b[32m` + `─` pattern as `renderer.ts`):
   - Add `isCurrentPromptSeparator(line)` (green + `─`)
   - Add `findLastSeparatorIndex(lines)`, `findLastInputBoxStart(lines)` (box = line starting with `┌`)
   - `getSectionContent('current-prompt')`: lines from (separator + 1) to (box start - 1); empty if no separator
   - History sections: scope to lines before separator (or before box if no separator)
   - Remove `isStatusLine`; update `getLastCommandOutput` to use new boundaries
2. Section type: `'status'` → `'current-prompt'`

## Phase 3: Rename E2E steps and feature language

1. **cli.ts**: `assertExpectedInStatus` → `assertExpectedInCurrentPrompt`; `getSectionContent(output, 'status')` → `getSectionContent(output, 'current-prompt')`
2. Steps: "in the status" → "in the current prompt", "styled in the status" → "styled in the current prompt"
3. **cli_recall.feature**: "in the status" → "in the current prompt"
4. `getLastCommandOutput` and steps that rely on boundaries

## Phase 4: Verify and fix failures

1. Run full E2E suite for CLI recall.
2. **STOP** if "Recall session - complete all due notes, see summary, then load more from future days" fails; report error and ask developer.
3. Fix other failures; remove any remaining dead code (e.g. dim-based helpers).

## Edge Cases

- **No separator**: Idle or after `/recall-status` (log only). Current Prompt empty; History extends to input box.
- **Multiple separators**: Use **last** separator before last input box.
- **Piped mode**: Separator only in TTY; piped `defaultOutput` omits `beginCurrentPrompt`.

## Risk Note

"Recall session - complete all due notes, see summary, then load more from future days" has multiple prompt turns and load-more transitions. If it fails after Phase 4, STOP and ask the developer.
