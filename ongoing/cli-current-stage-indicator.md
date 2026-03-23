# CLI: Current Stage + Current Stage Indicator

Informal plan for domain language, layout, and styling. Delete or trim when shipped.

## Intent

- **Stage** — The CLI is in the middle of a multi-step or long-running **command** (e.g. `/recall` session, slow interactive network call). This is a conceptual **state**, not the HTTP `recalling` API name.
- **Current Stage** — Which stage the user is in **right now** (what the UI should reflect).
- **Current Stage Indicator** — A dedicated **UI line**: the first line of the **Current prompt** block when a stage should be surfaced (initially: “Recalling” while recall payload is loading). It uses a **full terminal width** **Current stage band** fill (pad with spaces using **visible column width**, not JS `.length` — see `cli/src/renderer.ts`).
- **Current stage band** — The shared background treatment for the Current Stage Indicator line and (when the indicator is shown) the **Current prompt separator**, so the top of the Current prompt reads as one continuous strip. In code, the SGR sequence must be named for this **domain meaning**, not for a color adjective (e.g. not `GREY_BG`).
- **Current prompt separator** — The green horizontal rule (`buildCurrentPromptSeparator`). When the Current Stage Indicator is shown, paint the separator with the **same Current stage band** background as the indicator.

Out of scope for wording: backend/OpenAPI identifiers such as `RecallsController.recalling` stay as API names.

## Relationship to existing terms

| Existing | After |
|----------|--------|
| Grey “Recalling” below the input box (`RECALLING_INDICATOR` / `recallingIndicator`) | **Current Stage Indicator** as **line 1 of Current prompt** (not Current guidance). |
| `cli.mdc`: Current guidance lists hints + MCQ choices | Remove any implication that recall-loading text lives in Current guidance; guidance stays **below** the input box. |
| **Interactive fetch wait** | Still a distinct **visual** pattern today (blue prompt + grey disabled box + ellipsis). In domain terms it is a **Current Stage** (waiting on a slow call). **Current Stage Indicator** line for fetch-wait is **deferred** after recall/MCQ layout ships; glossary should still say fetch-wait is a stage even before the indicator exists for it. |

## Layout (TTY live region, top to bottom)

When **no** Current Stage Indicator (and existing rules unchanged):

- Optional gap from history
- If there are Current prompt text lines: **separator** → wrapped Current prompt lines → **input box** → **Current guidance**

When **Current Stage Indicator** is present:

- Same gap rules as today
- **Current Stage Indicator** (full-width **Current stage band**, first line of Current prompt)
- **Current prompt separator** (same band; green `─` repeat still `width` columns inside that band)
- Remaining wrapped Current prompt lines (if any), with existing per-line `currentPromptSgr` behavior
- **input box**
- **Current guidance**

When the indicator is present but there are **no** wrapped prompt lines yet (recall loading before stem): still show **indicator + separator** (shared band) so the block is consistent, then the input box — unless a quick spike shows double rule is ugly; then document the chosen rule in this file.

## Implementation notes

- **`ansi.ts` (or single re-export from `renderer.ts`):** introduce one exported SGR constant named for the **Current stage band** (e.g. `CURRENT_STAGE_BAND_BACKGROUND` / `…_SGR` — pick one convention and use it only for this strip). JSDoc ties the name to **Current stage band**, not to “grey”. The escape value can stay the same as today’s `GREY_BG` initially; only the **name** is domain-driven.
- **`buildLiveRegionLines`** (`cli/src/renderer.ts`): stop appending the old `recallingIndicator` after the box; build **stage line + adjusted separator + prompt lines** in one place using the band constant.
- **`ttyAdapter`**: `LiveRegionLayout` / `currentPromptLines` / `inputRowFromTop` must count the extra line(s) so cursor positioning and incremental repaint stay correct (`cursorUpStepsToLiveRegionTop`, `clearLiveRegionForRepaint`).
- **`TTYDeps`**: rename `recallingIndicator` → something like `currentStageIndicatorLines` (or pass structured data). Rename `RECALLING_INDICATOR` export to a name that reflects **default recall-loading stage label** (user-visible text can remain “Recalling”).
- **Tests**: `cli/tests/**/*.ts` that assert order of “Recalling”, separator, or box; `recallMcqTtyCursorPosition.test.ts`; renderer tests for `buildLiveRegionLines` / `renderFullDisplay`. Prefer **observable** stdout/TTY bytes over internal helper names.
- **E2E**: Grep `e2e_test` for “Recalling” or live-region assertions; align step/page-object language with **Current Stage Indicator** where steps describe the UI.

## Planning (not a phase)

**Planning** is this document plus discussion while the design moves. Per `.cursor/rules/planning.mdc`, numbered **phases** should each ship a small **user-visible** slice — not a phase that only “prepares” or duplicates work you already did in the plan.

So: no separate “plan phase.” When implementation starts, **fold the domain contract into the first behavior slice** (glossary in `cli.mdc`, domain-named band SGR constant, and TTY layout ship together — the constant is required for the layout anyway).

## Phases (scenario-first)

1. **Recall loading + MCQ TTY layout** — **Done.** Current Stage Indicator above the box with full-width **Current stage band** + banded separator; `countPromptBlockLinesAboveInputBoxTop` keeps TTY cursor math aligned with `buildLiveRegionLines`; `CURRENT_STAGE_BAND_BACKGROUND_SGR` / `buildCurrentPromptSeparatorForStageBand` in `renderer.ts`; Vitest in `renderer.test.ts`, MCQ TTY test uses banded separator; `.cursor/rules/cli.mdc` glossary updated.
2. **Vocabulary pass** — Rename implementation identifiers (`recallingIndicator`, etc.), `types.ts` adapter comments, E2E/step strings, and remaining comments to match the glossary. *User-visible only if E2E wording changes.*
3. **(Optional) Interactive fetch wait + Current Stage Indicator** — Reuse the **Current stage band** + indicator line pattern for slow interactive calls (today: blue first prompt + grey box + ellipsis in `interactiveFetchWait.ts`). Unify or reconcile with the blue prompt styling; extend Vitest / TTY tests. *Skip this phase if the current fetch-wait UX is enough.*

**Deploy gate** between phases if the team requires CD before the next slice (see `.cursor/rules/planning.mdc`).

## Resolved decisions

- **Interactive fetch wait:** Treat as a **Current Stage** in the glossary; **do not** add a Current Stage Indicator line for fetch-wait in phase 1 — **phase 3 (optional)** if you want one band + label for slow calls.
- **Band fill:** Use one **Current stage band** SGR constant (domain-named). Start with the **same numeric color** as today’s `GREY_BG` (`48;5;236`); change the palette only if the band is confused with **history** or **input box** chrome in practice.
