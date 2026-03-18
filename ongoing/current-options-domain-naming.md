# Plan: Current Guidance Domain Naming

**Status**: Phases 1–4 ✅ complete. Phases 5–6 pending.

## Context

The CLI interactive UI has an area below the input box that displays hints, help, or selection options for the current input (e.g. `/ commands`, command suggestions, access token list, recall questions, MCQ choices, y/n confirmation). This area currently lacks a consistent domain name across product code, E2E tests, and documentation.

**Domain term**: **Current guidance** – the UI area below the input box used for hint, help, or options for selection for the current input.

## Principles

- **One representation per concept** – Use "Current guidance" consistently.
- **Domain-driven test language** – Replace generic assertions like "in the CLI output" with domain-specific terms.
- **Phased delivery** – Each phase delivers user value and keeps tests passing.

---

## Domain Terminology

| Term | Definition |
|------|------------|
| **Current guidance** | All content below the input box. Used for hints, help, or options for selection for the current input. |
| **History output** | Past command results and output (unchanged). |
| **History input** | Past user input lines (unchanged). |
| **Input box** | The bordered area where the user types (unchanged). |

---

## Phased Plan

### Phase 1: E2E section parser – add Current guidance ✅

**Goal**: Define and extract "Current guidance" in the E2E parser.

**Done**:

1. In `cliSectionParser.ts`:
   - Added section type `'current-guidance'`.
   - Current guidance = all content after the last line of the input box (the line containing `└`).
   - Added `findLastInputBoxEnd(lines)` – scan from last `┌` to find the matching `└` line.
   - `getSectionContent(output, 'current-guidance')` returns lines after the input box.
   - Module comment documents domain sections.

2. Edge case: when no input box exists (piped non-interactive), Current guidance is empty.

3. Added `getCurrentGuidanceDebug(output)` for educational failure messages (parser state, raw tail).

---

### Phase 2: Step definition – "Current guidance" ✅

**Goal**: Add a step that asserts on Current guidance and deprecate the generic "CLI output" step for interactive scenarios.

**Done**:

1. In `cli.ts`:
   - Added `Then('I should see {string} in the Current guidance', ...)` using `getSectionContent(output, 'current-guidance')`.
   - Educational failure message via `buildCurrentGuidanceFailureMessage()` – parser state, Current guidance content, raw tail.

---

### Phase 3: Migrate feature files – replace "CLI output" with "Current guidance" ✅

**Goal**: Use domain language in all interactive scenarios.

**Done**:

1. Migrated `cli_access_token.feature` – ESC cancels remove-access-token selection uses Current guidance for token list and `/ commands` hint.

2. Removed `Then('I should see {string} in the CLI output', ...)` – no scenarios needed "anywhere in full stdout"; all use domain sections.

3. Fix for token list: "I press ESC" now sends only `\x1b` (no `\n`) to avoid accidental remove on Enter; ESC from token list clears buffer (`cliPtyRunner.ts`, `ttyAdapter.ts`).

---

### Phase 4: Align "current prompt" with Current guidance ✅

**Goal**: Use "Current guidance" where the existing "current prompt" refers to the same area.

**Done**:

1. Naming: Option A – one term "Current guidance" for step and feature assertions.

2. In `cliSectionParser.ts`:
   - `getRecallDisplaySections` returns `currentGuidanceAndHistory` (current-prompt + current-guidance + history-output). Recall content is between separator and box; hints below the box.

3. In feature files and step defs: Replaced all "current prompt" with "Current guidance". `I answer X to prompt Y` asserts against Current guidance.

4. Single step `I should see {string} in the Current guidance` uses `getRecallDisplaySections` for the combined region.

---

### Phase 5: Product code – use "Current guidance" terminology

**Goal**: Use the domain name in CLI product code (comments, variable names, docs).

**Work**:

1. In `cli/`:
   - Identify references to "hint", "suggestions", "prompt" that map to the area below the input box.
   - Update comments and non-public symbols to use "Current guidance" where it improves clarity (e.g. in `renderer.ts`, `ttyAdapter.ts`, `cliSectionParser.ts`).
   - Avoid invasive renames of stable APIs unless necessary.

2. In `cli/tests/interactive.test.ts`:
   - Update test descriptions: "below input box" → "in the Current guidance" where appropriate.
   - Example: `shows grey hint "  / commands" below input box` → `shows "  / commands" in the Current guidance`.

**Verification**: `pnpm cli:test` passes.

---

### Phase 6: Update cursor rule

**Goal**: Document domain terminology in `.cursor/rules/cli.mdc`.

**Work**:

1. Add a **Domain terminology** section to `cli.mdc`:
   - **Current guidance**: The UI area below the input box. Used for hints, help, or selection options for the current input. In E2E tests, Current guidance = all content below the input box.
   - **History output**: Past command results.
   - **History input**: Past user input lines.
   - **Input box**: The bordered area where the user types.

2. Add guidance:
   - E2E steps should use domain terms: `I should see X in the Current guidance` (not "in the CLI output") when asserting on content below the input box.
   - `cliSectionParser` exports `getSectionContent(output, 'current-guidance')` for Current guidance.

3. Update any existing CLI E2E documentation in the rule to reference Current guidance.

**Verification**: Rule is readable and consistent with implementation.

---

## Summary of Changes by Phase

| Phase | Status | Files | Changes |
|-------|--------|-------|---------|
| 1 | ✅ | `cliSectionParser.ts` | Add `current-guidance`, `findLastInputBoxEnd`, `getCurrentGuidanceDebug` |
| 2 | ✅ | `cli.ts` | Add step `I should see {string} in the Current guidance` with educational failure message |
| 3 | ✅ | `cli_access_token.feature`, `cli.ts`, `cliPtyRunner.ts`, `ttyAdapter.ts` | Migrate to Current guidance; remove "CLI output" step; fix ESC/Enter ordering |
| 4 | ✅ | `cliSectionParser.ts`, `cli.ts`, `cli_recall.feature` | Align "current prompt" with Current guidance; migrate recall assertions |
| 5 | pending | `cli/src/`, `cli/tests/interactive.test.ts` | Use "Current guidance" in comments and test descriptions |
| 6 | pending | `.cursor/rules/cli.mdc` | Add domain terminology section and E2E guidance |

---

## Open Questions

1. **Parser layout**: The current `current-prompt` extracts grey hint lines "between separator and box". The renderer places hints (suggestionLines) *below* the box. `current-guidance` captures content after the input box (`└`) – verified for token list, `/ commands` hint.
