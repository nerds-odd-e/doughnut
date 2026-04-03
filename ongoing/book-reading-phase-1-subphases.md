# Phase 1 sub-phases — `/use` notebook context in the CLI

**Parent:** [book-reading-story-1-plan.md](book-reading-story-1-plan.md) — **Phase 1** only.

**Architecture context:** [doughnut-book-reading-architecture-roadmap.md](doughnut-book-reading-architecture-roadmap.md) (no schema work in these sub-phases).

**Principles:** [.cursor/rules/planning.mdc](../.cursor/rules/planning.mdc) — one user-visible slice per sub-phase; observable assertions; generalize after repetition where it stays valuable.

---

## Scope and testing

- **In scope:** Interactive CLI only — `/use`, notebook **stage** (1.1 done for titled `/use`), **notebook-stage** `/` sub-command discovery (**1.3** done), missing notebook errors, bare `/use` picker, typing filter on that list.
- **Out of scope for these sub-phases:** Backend book APIs, `/attach`, browser; those stay in later parent phases.
- **E2E:** Do **not** add or extend Cypress/Cucumber for 1.1–1.6. Rely on **Vitest** driving **`runInteractive`** (and existing Ink helpers in `cli/tests/inkTestHelpers.ts`) per [.cursor/rules/cli.mdc](../.cursor/rules/cli.mdc). Mock HTTP with **`vi.spyOn`** on **`doughnut-api`** controllers and **`makeMe`** from **`doughnut-test-fixtures/makeMe`** for response shapes.
- **Deploy gate:** After each sub-phase, follow the parent plan checklist (commit/push/CD) when the team treats a sub-phase as shippable; keep **`pnpm cli:test`** green.

---

## Sub-phase order and dependencies

Sub-phases are numbered in **delivery order**. Later items assume earlier shell/stage behavior exists.

| ID | Depends on | Status |
|----|------------|--------|
| 1.1 | — | Done |
| 1.2 | — | **Scrapped** — recall stage `/exit` removed from this track (recall unchanged; leave via existing Esc / y-n flows). |
| 1.3 | 1.1 — stage slash hints for **notebook stage only** (see section) | Done |
| 1.4 | 1.1 — error path for titled `/use` | Done |
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

## 1.2 — (scrapped)

Recall stage **`/exit`** is **not** part of this sub-phase track. Recall behavior stays as today (e.g. Esc and y/n to leave). Notebook stage **`/exit`** remains as shipped in **1.1**.

---

## 1.3 — Notebook stage: `/` lists choosable sub-commands

**Status:** Done.

**User outcome:** When the **notebook** stage (from **`/use`**) has focus, typing **`/`** shows **notebook-stage** sub-commands the user can complete or pick (mirror **Current guidance** behavior for top-level slash hints in `MainInteractivePrompt` — reuse patterns from `slashCommandCompletion` / `effectiveSlashGuidance` where practical). **Recall and other stages are explicitly out of scope for 1.3**; extending the same mechanism to them is a later follow-up if still wanted.

**Implemented:** `notebookStageSlashCommands` in `cli/src/commands/notebook/notebookStageSlashCommands.ts` (currently **`/exit`**). `UseNotebookStage` passes it to `SlashCommandShellLiveColumn` as `slashCommands` so Tab/`/` guidance matches the stage registry only. Recall unchanged.

**Tests:** `cli/tests/InteractiveCliApp.useNotebook.test.tsx` — `in notebook stage, / shows slash sub-command guidance` asserts `/exit` usage and description in the live column.

---

## 1.4 — `/use <non-existing-notebook>` shows a user-visible error

**Status:** Done.

**User outcome:** If the title does not match an accessible notebook, the user sees a **clear error** in the same family as existing CLI failures (`userVisibleSlashCommandError`, red assistant block / transcript pattern).

**Implementation notes:** After API returns empty or 404, call **`onAbortWithError`** from the stage or map to assistant error before entering a “success” stage — do not enter notebook stage with an invalid id.

**Implemented:** `NotebookController.myNotebooks` via `runDefaultBackendJson`; exact case-sensitive title match; duplicate titles → error; loading spinner + Esc abort (`Cancelled.`). `NotebookController` and `NotebooksViewedByUser` exported from `doughnut-api`.

**Tests:** `cli/tests/InteractiveCliApp.useNotebook.test.tsx` — `vi.spyOn(NotebookController, 'myNotebooks')`, not found, 401, Esc during load, duplicate titles.

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

## Phase-complete checklist (1.1, 1.3–1.6; 1.2 scrapped)

1. **Clean up** — Remove interim duplication; notebook-stage sub-commands (**1.3**) live in one clear pattern; recall unchanged.
2. **Parent Phase 1** — Satisfies [book-reading-story-1-plan.md](book-reading-story-1-plan.md) Phase 1 user outcome (active notebook + errors + persistence) with **CLI Vitest** coverage; **no new** `e2e_test/features/book_reading/` scenarios for these sub-phases.
3. **Update parent plan** — When this slice is done, shorten or check off Phase 1 in `book-reading-story-1-plan.md` and note any deviation (e.g. title-only vs id resolution).

---

## Explicit non-goals (here)

- Cypress book-reading feature extensions for `/use`.
- Changing global **`/exit`** semantics for quitting the entire CLI unless the product explicitly requires it — **notebook** stage-local **`/exit`** (from **1.1**) means “leave notebook stage” only.
- Recall stage **`/exit`** or recall slash-hint rows — **not** in **1.2** (scrapped) or **1.3** (notebook-only).
