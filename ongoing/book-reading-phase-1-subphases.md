# Phase 1 sub-phases — `/use` notebook context in the CLI

**Parent:** [book-reading-story-1-plan.md](book-reading-story-1-plan.md) — **Phase 1** only.

**Architecture context:** [doughnut-book-reading-architecture-roadmap.md](doughnut-book-reading-architecture-roadmap.md) (no schema work in these sub-phases).

**Principles:** [.cursor/rules/planning.mdc](../.cursor/rules/planning.mdc) — one user-visible slice per sub-phase; observable assertions; generalize after repetition where it stays valuable.

---

## Scope and testing

- **In scope:** Interactive CLI only — `/use`, notebook **stage** (1.1 done for titled `/use`), recall **stage** `/exit`, stage-local `/` sub-command discovery, missing notebook errors, bare `/use` picker, typing filter on that list.
- **Out of scope for these sub-phases:** Backend book APIs, `/attach`, browser; those stay in later parent phases.
- **E2E:** Do **not** add or extend Cypress/Cucumber for 1.1–1.6. Rely on **Vitest** driving **`runInteractive`** (and existing Ink helpers in `cli/tests/inkTestHelpers.ts`) per [.cursor/rules/cli.mdc](../.cursor/rules/cli.mdc). Mock HTTP with **`vi.spyOn`** on **`doughnut-api`** controllers and **`makeMe`** from **`doughnut-test-fixtures/makeMe`** for response shapes.
- **Deploy gate:** After each sub-phase, follow the parent plan checklist (commit/push/CD) when the team treats a sub-phase as shippable; keep **`pnpm cli:test`** green.

---

## Sub-phase order and dependencies

Sub-phases are numbered in **delivery order**. Later items assume earlier shell/stage behavior exists.

| ID | Depends on | Status |
|----|------------|--------|
| 1.1 | — | Done |
| 1.2 | 1.1 (shared “leave stage” semantics; implementation may parallelize conceptually) | — |
| 1.3 | 1.1 + 1.2 — commonize after two stage surfaces need the same pattern | — |
| 1.4 | 1.1 — error path for titled `/use` | — |
| 1.5 | 1.1, ideally 1.3 — optional argument + stage or list UI | — |
| 1.6 | 1.5 — filter is a refinement of the picker | — |

Unknown-notebook handling and its test are **1.4** (can ship in the commit right after **1.1** if small).

---

## 1.1 — `/use <notebook title>` enters notebook stage; `/exit` returns to top

**Status:** Done.

**User outcome:** After `/use Some Notebook`, the CLI shows a **notebook** current stage (stage indicator + confirmation that this notebook is the active target for later book commands). From that stage, **`exit`** or **`/exit`** on the stage command line clears the stage and returns to the normal top-level prompt. **`/exit` from the main prompt** still quits the whole CLI (`exitSlashCommand`).

**Implemented:** `useNotebookSlashCommand` + `UseNotebookStage` in `cli/src/commands/notebook/useNotebookSlashCommand.tsx`, registered in `cli/src/commands/interactiveSlashCommands.ts`. Title is the required slash argument only (trimmed); it is **not** resolved against the backend yet and there is **no notebook id** or session persistence — that stays for **1.4+** / parent Phase 1. The stage uses its own single-line input (main prompt is inactive while the stage is open).

**Tests:** `cli/tests/InteractiveCliApp.useNotebook.test.tsx` — Vitest + Ink (`renderInkWhenCommandLineReady`), asserts stage copy, then `/exit` and return to the main prompt (same observable style as other `InteractiveCliApp.*` tests).

---

## 1.2 — Recall stage supports `/exit`

**User outcome:** While in the **recall** stage, committing **`/exit`** leaves recall and returns to the top-level interactive prompt (without requiring recall-specific keys only), consistent with notebook stage behavior.

**Implementation notes:** Today global slash handling may not reach stages the same way as the main prompt; route or handle `/exit` (and optionally `exit` alias if the resolver supports it) inside recall’s stage stack or via shared stage command dispatch introduced in **1.3**. Prefer one code path for “pop stage” used by notebook and recall.

**Tests:** `runInteractive` — drive a short recall session (existing spy pattern for `RecallsController` / trackers), then `/exit`; assert recall UI/stage is cleared and main prompt returns.

**Done when:** Test passes; recall y/n and other recall semantics unchanged except for the new exit path.

---

## 1.3 — Common stage sub-commands; `/` in a stage lists choosable sub-commands

**User outcome:** When a **stage** has focus (notebook stage, recall stage, and future stages), typing **`/`** shows **stage-relevant** sub-commands the user can complete or pick (mirror **Current guidance** behavior for top-level slash hints in `MainInteractivePrompt` — reuse patterns from `slashCommandCompletion` / `effectiveSlashGuidance` where practical). Sub-command set is **declared per stage** (or per command family), not hard-coded only in the main prompt.

**Implementation notes:** Extract or introduce a small shared mechanism: e.g. “active stage contributes slash rows + completions” so notebook and recall do not duplicate ad hoc lists. Keep **high cohesion** — one representation of “what can I type here?” per stage.

**Tests:** `runInteractive` (or focused Ink render if the guidance contract is stable and tested indirectly) — in notebook stage and in recall, `/` shows expected hints including `/exit` (and any other stage commands defined for that phase).

**Done when:** No duplicate unmaintained hint lists; tests prove both stages show the right guidance.

---

## 1.4 — `/use <non-existing-notebook>` shows a user-visible error

**User outcome:** If the title does not match an accessible notebook, the user sees a **clear error** in the same family as existing CLI failures (`userVisibleSlashCommandError`, red assistant block / transcript pattern).

**Implementation notes:** After API returns empty or 404, call **`onAbortWithError`** from the stage or map to assistant error before entering a “success” stage — do not enter notebook stage with an invalid id.

**Tests:** `runInteractive` + spy returning no match; assert error text in scrollback/stdout.

**Done when:** Error path tested; happy path from **1.1** unchanged.

---

## 1.5 — `/use` with no notebook argument opens a chooser

**User outcome:** `/use` alone (argument optional on the command) opens an interactive **notebook list** stage: user picks a notebook (e.g. ↑↓ + Enter or number keys — match an existing list pattern in the CLI if one exists, e.g. recall MCQ / token flows).

**Implementation notes:** Set **`argument.optional: true`** on `/use` and branch in the stage: empty argument → fetch list → selection UI; non-empty → resolve by title as in **1.1**. Reuse **1.3** mechanism for stage sub-commands (`/exit`, etc.).

**Tests:** `runInteractive` — mock list of at least two notebooks; open `/use`, select one; assert active notebook confirmation matches choice.

**Done when:** Optional arg behavior documented in help; tests green.

---

## 1.6 — Notebook chooser filters while typing

**User outcome:** In the **1.5** list, as the user types (or narrows with a dedicated filter buffer — pick one UX consistent with the CLI), the visible choices **filter** to matching notebook titles (substring or prefix; document the rule in code/help).

**Implementation notes:** If the chooser reuses a generic “select list” component, extend filtering there so recall or other lists can reuse later only if it stays simple.

**Tests:** `runInteractive` — type filter characters; assert only matching rows remain selectable and Enter confirms the highlighted match.

**Done when:** Filtering observable in tests; no fixed-time sleeps (see cli.mdc).

---

## Phase-complete checklist (all of 1.1–1.6)

1. **Clean up** — Remove interim duplication; stage sub-commands live in one pattern.
2. **Parent Phase 1** — Satisfies [book-reading-story-1-plan.md](book-reading-story-1-plan.md) Phase 1 user outcome (active notebook + errors + persistence) with **CLI Vitest** coverage; **no new** `e2e_test/features/book_reading/` scenarios for these sub-phases.
3. **Update parent plan** — When this slice is done, shorten or check off Phase 1 in `book-reading-story-1-plan.md` and note any deviation (e.g. title-only vs id resolution).

---

## Explicit non-goals (here)

- Cypress book-reading feature extensions for `/use`.
- Changing global **`/exit`** semantics for quitting the entire CLI unless the product explicitly requires it — **stage-local** `/exit` means “leave current stage”.
