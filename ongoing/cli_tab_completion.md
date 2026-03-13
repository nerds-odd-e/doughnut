# CLI Tab Completion for Commands

## Requirement

Pressing **Tab** in the input box should complete the current command prefix to a full command or the longest common prefix when multiple commands match.

## Context

- **Input box**: Interactive prompt in `cli/src/interactive.ts`. Key handling is in `stdin.on('keypress', ...)`.
- **Commands**: From `interactiveDocs` in `help.ts` (e.g. `/help`, `/exit`, `/add-access-token`, `/recall`, `/recall-status`, `/add gmail`, etc.).
- **Current behavior**: Enter inserts the highlighted command from suggestions; Up/Down cycle; Tab has no effect.
- **Related**: `filterCommandsByPrefix` (help.ts) filters by substring. Tab completion needs **prefix match**: `usage.startsWith(buffer)`.

## Scope

- **Unit tests only** – no E2E tests.
- Tab completes only when the last line of the buffer starts with `/` and does not already end with a space (same condition as showing suggestions).

## Completion Logic

1. **Filter**: Commands whose `usage` starts with the current last line (exact prefix match).
2. **0 matches**: No change to buffer.
3. **1 match**: Replace last line with `usage + ' '` (add trailing space like Enter does).
4. **2+ matches**: Replace last line with the longest common prefix of all matching usages. If prefix equals buffer, no change (user can press Tab again to show suggestions, or we leave as-is for simplicity).

## Phased Plan

### Phase 1: Tab completion helper (unit-testable)

Add `getTabCompletion(buffer: string, commands: CommandDoc[]): { completed: string; count: number }`:

- **Location**: Either `help.ts` (next to `filterCommandsByPrefix`) or `interactive.ts`. Prefer `help.ts` for cohesion with command filtering.
- **Input**: `buffer` = last line of input (e.g. `/he`); `commands` = interactive docs.
- **Logic**:
  - If buffer does not start with `/`, return `{ completed: buffer, count: 0 }`.
  - Filter: `commands.filter(c => c.usage.startsWith(buffer))`.
  - If 0 matches: return `{ completed: buffer, count: 0 }`.
  - If 1 match: return `{ completed: matches[0].usage + ' ', count: 1 }`.
  - If 2+ matches: compute longest common prefix of all `matches.map(m => m.usage)`; if prefix > buffer length, return `{ completed: prefix, count: matches.length }`, else `{ completed: buffer, count: matches.length }`.

**Unit tests**: In `help.test.ts` for the helper; in `interactive.test.ts` for Tab key handling.

- `getTabCompletion('/he', docs)` → `{ completed: '/help ', count: 1 }`
- `getTabCompletion('/help', docs)` → `{ completed: '/help ', count: 1 }`
- `getTabCompletion('/add-access-token', docs)` → `{ completed: '/add-access-token ', count: 1 }`
- `getTabCompletion('/rec', docs)` → `{ completed: '/recall', count: 2 }` (common prefix of `/recall` and `/recall-status`)
- `getTabCompletion('/recall', docs)` → `{ completed: '/recall', count: 2 }` (prefix same as buffer, no change)
- `getTabCompletion('/add', docs)` → `{ completed: '/add', count: 2 }` (common prefix `/add` for `/add gmail` and `/add-access-token`)
- `getTabCompletion('/unknown', docs)` → `{ completed: '/unknown', count: 0 }`
- `getTabCompletion('hello', docs)` → `{ completed: 'hello', count: 0 }` (no leading `/`)
- `getTabCompletion('/', docs)` → many matches, common prefix `/` → no extension

### Phase 2: Wire Tab key in keypress handler

- In `runInteractiveTTY` keypress handler, add branch for `key.name === 'tab'` (before the final `else if (str && !key.ctrl && !key.meta)`).
- Only handle when: not in recall substate, not in token list, not in MCQ choice, not in pending stop confirmation.
- Get `bufferLines`, `lastLine`, call `getTabCompletion(lastLine, interactiveDocs)`.
- If `count > 0` and `completed !== lastLine`: set `buffer = bufferLines.slice(0, -1).concat(completed).join('\n') || completed`; reset `highlightIndex = 0`, `suggestionsDismissed = false`; `drawBox()`.

**Unit tests** (TTY mode, `interactive.test.ts`):

- Tab with `/he` completes to `/help ` and updates displayed buffer.
- Tab with `/rec` completes to `/recall` (common prefix).
- Tab with `/unknown` does nothing.
- Tab when buffer is `hello` (no `/`) does nothing.
- Tab with single match shows completed command in next draw.

### Phase 3: Tidy

- Ensure `getTabCompletion` is exported if needed for tests.
- No documentation beyond this file unless required.

## Edge Cases

- Buffer `/` only: many matches, common prefix `/` → no change.
- Buffer with trailing space (e.g. `/help `): completion not triggered (same as suggestions – we only complete when `!lastLine.endsWith(' ')` in the suggestion logic). Decide: allow Tab after space? For consistency with Enter/suggestions, **don’t** complete when last line ends with space.
- Multi-line buffer: only complete the last line.

## Test Strategy

- **UT**: Helper `getTabCompletion` in isolation; TTY keypress Tab via `createMockTTYStdin`, assert buffer and output via `writeSpy`.
