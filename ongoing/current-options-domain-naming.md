# Plan: Current Guidance Domain Naming

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

### Phase 1: E2E section parser – add Current guidance

**Goal**: Define and extract "Current guidance" in the E2E parser.

**Work**:

1. In `cliSectionParser.ts`:
   - Add section type `'current-guidance'`.
   - Define Current guidance as all content after the last line of the input box (the line containing `└`).
   - Add `findLastInputBoxEnd(lines)` – scan from last `┌` to find the matching `└` line.
   - Add `getSectionContent(output, 'current-guidance')` that returns lines after the input box.
   - Update module comment: document `current-guidance` alongside existing sections.

2. Handle edge case: when no input box exists (e.g. piped non-interactive), Current guidance is empty.

**Verification**: Existing E2E tests continue to pass (no behavior change yet).

---

### Phase 2: Step definition – "Current guidance"

**Goal**: Add a step that asserts on Current guidance and deprecate the generic "CLI output" step for interactive scenarios.

**Work**:

1. In `cli.ts`:
   - Add `Then('I should see {string} in the Current guidance', ...)` that uses `getSectionContent(output, 'current-guidance')`.
   - Keep `Then('I should see {string} in the CLI output', ...)` for now (used by some scenarios) – Phase 3 will migrate.

**Verification**: Add a temporary scenario or adjust an existing one to use the new step; ensure it passes.

---

### Phase 3: Migrate feature files – replace "CLI output" with "Current guidance"

**Goal**: Use domain language in all interactive scenarios.

**Work**:

1. Replace in feature files:
   - `I should see "X" in the CLI output` → `I should see "X" in the Current guidance` (only when X is in the area below the input box).

2. Affected scenarios (from `cli_access_token.feature`):
   - Line 39: `E2E CLI Token` (token list in Current guidance)
   - Line 41: `/ commands` (hint in Current guidance)
   - Line 43: `E2E CLI Token` (token list in Current guidance)

3. Remove `Then('I should see {string} in the CLI output', ...)` if no longer used, or retain only for edge cases that truly mean "anywhere in full output" (and document).

**Verification**: `pnpm cypress run --spec "e2e_test/features/cli/*.feature"` passes.

---

### Phase 4: Align "current prompt" with Current guidance

**Goal**: Use "Current guidance" where the existing "current prompt" refers to the same area.

**Analysis**: The recall scenarios use "I should see X in the current prompt" for:
- Recall question text, MCQ choices, "Yes, I remember?", "Spell:", etc.
- These are displayed in the area below the input box (Current guidance).

**Work**:

1. Decide naming:
   - Option A: Rename step to `I should see {string} in the Current guidance` and migrate all "current prompt" assertions.
   - Option B: Keep "current prompt" as a semantic subtype when the content is explicitly a prompt (e.g. "Yes, I remember?") but ensure it still targets the same section (`current-guidance`).
   - Recommended: **Option A** – one term, less ambiguity.

2. In `cliSectionParser.ts`:
   - Deprecate or alias `'current-prompt'` to use the same physical region as `'current-guidance'` if they overlap. If the parser’s `current-prompt` (between separator and box) differs from content below the box, clarify and possibly merge logic.
   - Ensure recall content (MCQ, y/n, spelling) is included in Current guidance extraction.

3. In feature files and step defs:
   - Replace `I should see X in the current prompt` with `I should see X in the Current guidance` where applicable.
   - Replace `I should see X styled in the current prompt` with `I should see X styled in the Current guidance`.

4. Update `getRecallDisplaySections` if it depends on `current-prompt` – use `current-guidance` or equivalent.

**Verification**: All CLI E2E tests pass.

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

| Phase | Files | Changes |
|-------|-------|---------|
| 1 | `cliSectionParser.ts` | Add `current-guidance` section, `findLastInputBoxEnd`, extract content below box |
| 2 | `cli.ts` | Add step `I should see {string} in the Current guidance` |
| 3 | `cli_access_token.feature`, `cli.ts` | Replace "CLI output" with "Current guidance"; remove or narrow "CLI output" step |
| 4 | `cliSectionParser.ts`, `cli.ts`, `cli_recall.feature` | Align "current prompt" with Current guidance; migrate recall assertions |
| 5 | `cli/src/`, `cli/tests/interactive.test.ts` | Use "Current guidance" in comments and test descriptions |
| 6 | `.cursor/rules/cli.mdc` | Add domain terminology section and E2E guidance |

---

## Open Questions

1. **Parser layout**: The current `current-prompt` extracts grey hint lines "between separator and box". The renderer places hints (suggestionLines) *below* the box. Confirm the actual stdout order in interactive mode and ensure `current-guidance` correctly captures hints, MCQ, token list, recall prompts, etc.

2. **"CLI output" step**: After migration, is there any scenario that genuinely needs "anywhere in full stdout"? If not, remove the step. If yes, keep it and document when to use "CLI output" vs "Current guidance".
