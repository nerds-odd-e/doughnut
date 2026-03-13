# CLI Input Box: Bold & Colored Recognized Commands

## Requirement

In the input box, recognized commands should be displayed in bold and use a different color while the user is typing.

## Context

- **Input box**: The interactive prompt in `cli/src/interactive.ts` where users type commands. Content is built by `buildBoxLines(buffer, width)` and rendered via `renderBox()`.
- **Recognized commands**: From `interactiveDocs` in `help.ts` (e.g. `/help`, `/exit`, `/add-access-token`, `/recall`, etc.).
- **Current behavior**: Buffer text is displayed as plain; no styling on command-like input.

## Scope

1. **Live input** (primary): As the user types, the portion of the buffer that matches a recognized command is bold and colored.
2. **Past input** (optional): `renderPastInput()` displays submitted input above the box; could apply same styling when rendering history.

## Recognition Logic

- **Definition of "recognized"**: The buffer (or the last line in multi-line mode) is a prefix of, or exactly matches, a command from `interactiveDocs`.
- **Match strategy**: For each line, find the longest command that is a prefix of the line, or the line is a prefix of a command.
- **Parameters**: Commands like `/add-access-token <label>` – the command part (before the param) gets highlighted; param text stays normal.

## Styling

- **ANSI codes**: Bold = `\x1b[1m`, color = e.g. cyan `\x1b[36m` or green `\x1b[32m` (choose one that contrasts with placeholder grey).
- **Reset**: `\x1b[0m` after styled segment.
- **Placeholder**: Keep existing `GREY` for empty prompt; styled command appears only when buffer contains matching text.

## Phased Plan

### Phase 1: Helper for command highlighting

- Add `highlightRecognizedCommand(line: string): string` (or similar) that:
  - Takes a single line from the buffer.
  - Finds the best-matching command from `interactiveDocs` (exact or prefix match).
  - Returns the line with ANSI bold+color applied to the matched command part only.
- Unit tests in `interactive.test.ts` for various inputs: `/`, `/he`, `/help`, `/add-access-token x`, `hello` (no match), `/unknown` (no match).

### Phase 2: Wire into buildBoxLines

- In `buildBoxLines()`, when building each content line, call the highlight helper on the line before adding the prefix.
- Ensure multi-line buffer: only lines that look like commands (e.g. start with `/`) get highlighted; others stay plain.
- Test visually and via unit tests that styled output has correct ANSI sequences.

### Phase 3 (optional): Past input styling

- In `renderPastInput()`, apply the same highlighting to each line before wrapping with `GREY_BG`.
- Lower priority; can be deferred.

### Phase 4: Tidy and docs

- Reuse or centralize ANSI color constants if needed (e.g. `COMMAND_HIGHLIGHT`).
- Update any manual testing notes if relevant.

## Edge Cases

- Empty buffer: no change; placeholder remains grey.
- Buffer `/` only: could highlight `/` if we treat it as prefix of any `/command`; or leave plain until a letter is typed.
- Buffer with trailing space (e.g. `/help `): typically not a prefix of a command; no highlight or only highlight `/help`.
- Commands with params: highlight only the command token, not the param (e.g. `/add-access-token` bold+colored, ` mylabel` normal).

## Test Strategy

- **UT**: `interactive.test.ts` – highlight helper with various strings; `buildBoxLines` with commands, non-commands, multi-line.
- **Manual**: Run CLI, type `/he`, `/help`, `/add-access-token x` and verify bold+color in the input box.
