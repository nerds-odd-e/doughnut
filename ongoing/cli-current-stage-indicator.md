# CLI: Current Stage + Current Stage Indicator

Informal plan for domain language, layout, and styling. Delete or trim when shipped.

## Intent

- **Stage** — The CLI is in the middle of a multi-step or long-running **command** (e.g. `/recall` session, slow interactive network call). This is a conceptual **state**, not the HTTP `recalling` API name.
- **Current Stage** — Which stage the user is in **right now** (what the UI should reflect).
- **Current Stage Indicator** — A dedicated **UI line**: the first line of the **Current prompt** block when a stage should be surfaced (initially: grey “Recalling” while recall payload is loading). It uses a **full terminal width** background (pad with spaces using **visible column width**, not JS `.length` — see `cli/src/renderer.ts`).
- **Current prompt separator** — The green horizontal rule (`buildCurrentPromptSeparator`). When the Current Stage Indicator is shown, paint the separator with the **same background** as the indicator so the prompt block reads as one band.

Out of scope for wording: backend/OpenAPI identifiers such as `RecallsController.recalling` stay as API names.

## Relationship to existing terms

| Existing | After |
|----------|--------|
| Grey “Recalling” below the input box (`RECALLING_INDICATOR` / `recallingIndicator`) | **Current Stage Indicator** as **line 1 of Current prompt** (not Current guidance). |
| `cli.mdc`: Current guidance lists hints + MCQ choices | Remove any implication that recall-loading text lives in Current guidance; guidance stays **below** the input box. |
| **Interactive fetch wait** | Still a distinct **visual** pattern (blue prompt + grey disabled box + ellipsis). In domain terms it is also a **stage** (waiting on a slow call). Decide in implementation whether it gets a **Current Stage Indicator** line later, or only recall-load uses the indicator first; glossary should state that **interactive fetch wait** is one kind of Current Stage even if the indicator line is not used for it yet. |

## Layout (TTY live region, top to bottom)

When **no** Current Stage Indicator (and existing rules unchanged):

- Optional gap from history
- If there are Current prompt text lines: **separator** → wrapped Current prompt lines → **input box** → **Current guidance**

When **Current Stage Indicator** is present:

- Same gap rules as today
- **Current Stage Indicator** (full-width background, first line of Current prompt)
- **Current prompt separator** (same background; green `─` repeat still `width` columns inside that band)
- Remaining wrapped Current prompt lines (if any), with existing per-line `currentPromptSgr` behavior
- **input box**
- **Current guidance**

When the indicator is present but there are **no** wrapped prompt lines yet (recall loading before stem): still show **indicator + separator** (shared background) so the block is consistent, then the input box — unless a quick spike shows double rule is ugly; then document the chosen rule in this file.

## Implementation notes

- **`buildLiveRegionLines`** (`cli/src/renderer.ts`): stop appending the old `recallingIndicator` after the box; build **stage line + adjusted separator + prompt lines** in one place. Reuse or extend `GREY_BG` (`\x1b[48;5;236m`) or a single new constant for “stage band” background so indicator and separator match.
- **`ttyAdapter`**: `LiveRegionLayout` / `currentPromptLines` / `inputRowFromTop` must count the extra line(s) so cursor positioning and incremental repaint stay correct (`cursorUpStepsToLiveRegionTop`, `clearLiveRegionForRepaint`).
- **`TTYDeps`**: rename `recallingIndicator` → something like `currentStageIndicatorLines` (or pass a structured `{ labelPlain: string }` if you want to avoid pre-styled strings in the adapter). Rename `RECALLING_INDICATOR` export to a name that reflects **default recall-loading stage label** (user-visible text can remain “Recalling”).
- **Tests**: `cli/tests/**/*.ts` that assert order of “Recalling”, separator, or box; `recallMcqTtyCursorPosition.test.ts` (separator vs recall row); renderer tests for `buildLiveRegionLines` / `renderFullDisplay`. Prefer **observable** stdout/TTY bytes over internal helper names.
- **E2E**: Grep `e2e_test` for “Recalling” or live-region assertions; align step/page-object language with **Current Stage Indicator** where steps describe the UI.
- **Docs**: Update **Domain terminology** table in `.cursor/rules/cli.mdc` (and any `e2e_test` CLI comments that duplicate the glossary).

## Phases (scenario-first, each phase complete with tests)

1. **Recall loading + MCQ TTY layout** — Implement new position, full-width background for the indicator, shared background on the separator; fix cursor/live-region counts; extend or add Vitest coverage for recall and MCQ TTY paths so layout regressions are caught. *User-visible: recall “Recalling” moves above the box with the new band styling.*
2. **Vocabulary pass** — Rename implementation identifiers (`recallingIndicator`, etc.), update `cli.mdc`, `types.ts` adapter comments, E2E/step strings, and in-repo comments to use **Stage / Current Stage / Current Stage Indicator** consistently. *User-visible only if E2E wording changes; main value is maintainability.*

**Deploy gate** between phases if the team requires CD before the next slice (see `.cursor/rules/planning.mdc`).

## Open decisions

- Whether **interactive fetch wait** should gain a Current Stage Indicator line in phase 1 or a later phase (blue text on the same grey band vs keeping today’s blue-only first prompt line).
- Exact background color (keep `GREY_BG` vs a slightly different shade for stage vs history).
