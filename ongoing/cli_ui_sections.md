# CLI UI Sections Plan

## Goal

Distinguish CLI UI sections with clear domain meanings so that:
1. Users understand where each piece of content belongs.
2. E2E tests can assert content in specific sections and report meaningful failures when expectations are not met.

## Current State

The CLI has two run modes:

- **TTY mode**: readline with cursor, redrawn input box, suggestions/choices below, output above.
- **Piped mode** (`-c` or stdin pipe): no cursor, sequential output; version → box → suggestions → for each line: past input → command output.

Current E2E uses `cy.get('@doughnutOutput').should('include', expected)` — a single blob assertion. Failures do not indicate which section lacked the content or what that section contained.

## Section Definitions

| Section | Domain meaning | Content |
|---------|----------------|---------|
| **input** | Current typing area | The editable buffer (prompt + text). Shown in a box. |
| **status** | Current command status / prompts | Hints, status, lists of choices for the *current* command. Below input in TTY; inline in piped. |
| **history-input** | User's submitted input | Each line/block the user entered. |
| **history-output** | Command response | Result of processing that input. |

When relevant, history can be split into:
- **last command output**: Most recent history-output.
- **earlier output**: Older history-output in the session.

### Visual Layout

```
┌─────────────────────────────────────────────────────────┐
│ [Earlier history-input / history-output pairs]           │
│ [Last history-input + history-output]                    │
├─────────────────────────────────────────────────────────┤
│ [input: prompt + buffer]                                 │
│ → /recall                                               │
├─────────────────────────────────────────────────────────┤
│ [status: hints / choices / prompts]                     │
│   Recalling                                              │
│   1. Yes, I remember                                     │
│   2. No, show me again                                   │
└─────────────────────────────────────────────────────────┘
```

## Styling (Section Distinction from Background)

Sections are distinguishable by background and foreground styling. E2E parses the raw stdout (which includes ANSI codes) to identify sections — no markers or env vars.

| Section | Styling |
|---------|---------|
| **input** | Grey background, `→` prompt (box) |
| **status** | Dim foreground (`\x1b[90m`), consistent indentation |
| **history-input** | Grey background (`\x1b[48;5;236m`), padded block |
| **history-output** | Default foreground, no background |

History-input and history-output are distinguishable from each other and from the terminal background by these styles. E2E splits output by detecting ANSI background sequences: blocks with grey background = history-input, blocks with default background = history-output. Status lines use dim foreground without special background.

## E2E Assertion Strategy

### Step Definitions

```gherkin
Then I should see "X" in the history output
Then I should see "X" in the history input
Then I should see "X" in the status
Then I should see "X" in the last command output
And I should not see "Y" in the history output
```

### Implementation

Parse stdout by ANSI styling:
- history-input: content inside grey-background blocks (`renderPastInput` output)
- history-output: content with default styling between history-input blocks
- status: content with dim foreground in the status area (TTY) or between history blocks (piped)

### Failure Reporting

When an assertion fails:
- "Expected 'X' in history-output, but it was not found."
- "history-output contains: <truncated content of that section>"
- Optionally: "Found 'X' in history-input" when it appears in the wrong section.

### Assertion Naming

| Section | Gherkin phrase |
|---------|----------------|
| Any (legacy) | `I should see "X"` |
| history-output | `I should see "X" in the history output` |
| history-input | `I should see "X" in the history input` |
| status | `I should see "X" in the status` |
| last command | `I should see "X" in the last command output` |

## Section Markers

Use section markers only when ANSI-based parsing is insufficient (e.g. if styling is stripped or ambiguous). Prefer styling as the primary way to distinguish sections.

## Phased Implementation

### Phase 1: Define and document section model
- Use section names: input, status, history-input, history-output.

### Phase 2: Ensure consistent styling
- history-input: grey background (existing `renderPastInput`)
- history-output: default styling, no background
- status: dim foreground, consistent indentation

### Phase 3: E2E parsing helper
- Parse raw stdout by ANSI sequences into section maps.
- Implement `getSectionContent(output, section)` for assertions.

### Phase 4: New E2E step definitions
- `I should see "X" in the history output`
- `I should see "X" in the history input`
- `I should see "X" in the status`
- On failure: report section content.

### Phase 5: Migrate existing E2E assertions
- Replace `I should see "X"` with section-specific steps where intent is clear.
- Leave generic `I should see` for cases where section is irrelevant.

## Files to Touch

- `cli/src/interactive.ts` — apply consistent styling for status, history-input, history-output
- `e2e_test/step_definitions/cli.ts` — parsing helper, new step definitions
- `e2e_test/features/cli/*.feature` — migrate assertions to section-specific steps
