# Frontend / CLI / MCP unit tests → "small test" style

**Status:** in progress (Phases 1–2 done)  
**Type:** test renovation (no product behavior change)  
**Resume:** this `PLAN.md` progress log only — **do not edit** trunk `.planning/STATE.md` (parallel trunk-based work).

### Verify per package (run the package(s) touched in the phase)

| Package | Command |
|---------|---------|
| MCP | `CURSOR_DEV=true nix develop -c pnpm mcp-server:test` |
| CLI | `CURSOR_DEV=true nix develop -c pnpm cli:test` |
| Frontend | `CURSOR_DEV=true nix develop -c pnpm frontend:test` |

While iterating a single large frontend file, `pnpm frontend:test tests/path/to/File.spec.ts` is OK; close the phase with the full package suite above.

**Style:** `.cursor/rules/unit-testing.mdc` + package rules (`frontend-testing.mdc`, `cli.mdc`, `mcp-server.mdc`).

---

## Rubric (apply every Behavior phase)

| Check | Action |
|-------|--------|
| **Stable boundary** | Drive the public surface: mounted Vue component/page (frontend), CLI `run` / `runInteractive` / Ink app entry (CLI), MCP tool / server surface (MCP). Do not add a test file per internal helper; do not widen exports only for tests. |
| **Keep when domain-stable** | Pure utils, schema validation, markdown/formatters, token budget helpers — renovate in place. |
| **Mocks** | Only true externals. Frontend: `mockSdkService()` for the backend HTTP API (allowed). CLI: spies on `doughnut-api` / network as package rules allow. MCP: mock Doughnut API / transport as package rules allow. Drop sibling-component stubs and internal-module mocks when real rendering/data can cover the path. |
| **Focused assertions** | One behavior per test. Canonical shape once; siblings assert **delta only**. |
| **Concise makeMe** | Prefer `doughnut-test-fixtures/makeMe` builder APIs (e.g. `.id(10)`) over post-construction mutation. Extend builders when the same verbose shape repeats (Structure sub-slice for the immediate next Behavior only). |
| **No production drive-bys** | No product refactors for “testability” beyond MakeMe/fixtures unless Jidoka. |

**Phase done when:** listed files comply; dead/redundant tests removed; package verify green; plan status updated.

**Sizing:** ~5 min fuzzy per slice; >10 min without converging → revert WIP, split remaining into a sub-phase in this PLAN, commit+push, continue. Prefer mid-commit over huge uncommitted WIP (learned from backend renovation stalls).

**Naming:** Capability names only — never encode phase numbers in test/product file names.

---

## Design decisions

1. **Package order: MCP → CLI → Frontend** — smaller suites first; earlier feedback; frontend dominates effort (~255 specs).
2. **Controller/page/component-first** — same spirit as backend controller-first: fewer, higher-boundary tests over polishing duplicate util/wrapper suites.
3. **Frontend large files = multiple phases** — e.g. `BookReadingPage.spec.ts` (~1.8k LOC), `NoteAudioTools`, `RecallPage` — split by `@describe` / capability if needed.
4. **Allowed mocks stay** — `mockSdkService`, CLI API spies, MCP API mocks; drop everything else that is not a true external.
5. **Stop-safe** — after any phase, renovated cohorts are better and that package’s suite is green.
6. **No trunk STATE hijack** — progress only in this PLAN.

---

## Phases

### Phase 1 — MCP server suite
- **Status:** done
- **Type:** Behavior
- **Observable:** all `mcp-server/tests/**` follow the rubric.
- **Files:** `mcp-server/tests/` (helpers, server, `tools/*`).
- **Verify:** `pnpm mcp-server:test` — green (5 files, 10 tests).

### Phase 2 — CLI: pure helpers and non-interactive commands
- **Status:** done
- **Type:** Behavior
- **Files** under `cli/tests/` (non-Ink / non-recall-interactive), including e.g.:
  - `version`, `update`, `index`, `gmail`, `markdown`, `welcomeBanner`
  - `sdkHttpErrorClassification`, `doughnutBackendClient.errors`, `userVisibleSlashCommandError`
  - `terminalColumnsTruncate`, `spellingAnswerLine`, `numberedMcqMarkdownLines`
  - `slashCommandCompletion`, `notebookStageSlashCommands`, `interactiveSlashCommandDispatch`
  - `mineruOutlineSubprocess*` (subprocess/external stubs only; suite split by capability)
  - `contestAndRegenerateMcq`, `recallStatus`, `recallSessionSummary`, `selectListInteraction*`
- Leave Ink/`InteractiveCliApp`/`*Interactive*.tsx` / just-review interactive for Phases 3–4.
- **Verify:** `pnpm cli:test` — green (46 files / 300 tests after splits).
- **Done when:** listed files renovated; suite green.

### Phase 3 — CLI: Ink shell and main interactive app
- **Status:** planned
- **Type:** Behavior
- **Files:**
  - `interactiveInkSession.test.ts`
  - `InteractiveCliApp*.test.tsx` (base, useNotebook, setAccessToken, addGmail, recallStatus)
  - `MainInteractivePrompt.test.tsx`, `mainInteractivePromptHistory.test.ts`
  - `YesNoStagePrompt`, `AsyncAssistantFetchStage`, past user/assistant blocks
  - `borderedSingleLinePromptInputInk`, `guidanceListWindowInk`
  - `useNotebookSlashCommand.test.tsx`
- **Verify:** `pnpm cli:test`
- **Done when:** rubric applied; suite green.

### Phase 4 — CLI: recall interactive
- **Status:** planned
- **Type:** Behavior
- **Files:** `recallMcqInteractive*`, `recallSpellingInteractive*`, `recallJustReviewInteractive*`, shared `recallInteractiveShared` / fixtures / waits / mocks (slim mocks to allowed externals only).
- **Verify:** `pnpm cli:test`
- **Done when:** CLI suite fully renovated; suite green.

### Phase 5 — Frontend: helpers, commons, utils, lib
- **Status:** planned
- **Type:** Behavior
- **Files:**
  - `frontend/tests/helpers/**` (support only — style if tests live here; otherwise skip)
  - `frontend/tests/commons/**`, `frontend/tests/common/**`
  - `frontend/tests/utils/**`
  - `frontend/tests/lib/**`
- Prefer pure-contract style; delete tests that only re-cover a mounted component path already asserted elsewhere when safe.
- **Verify:** `pnpm frontend:test`
- **Done when:** rubric applied; suite green.

### Phase 6 — Frontend: composables, models, store, managedApi, routes
- **Status:** planned
- **Type:** Behavior
- **Files:**
  - `frontend/tests/composables/**`
  - `frontend/tests/models/**`
  - `frontend/tests/store/**`
  - `frontend/tests/managedApi/**`
  - `frontend/tests/routes/**`
  - `frontend/tests/storybook/**` if present
  - `frontend/tests/DoughnutApp.loadingThinBar.spec.ts` if present
- **Verify:** `pnpm frontend:test`
- **Done when:** rubric applied; suite green.

### Phase 7 — Frontend: components — form + commons
- **Status:** planned
- **Type:** Behavior
- **Files:** `frontend/tests/components/form/**`, `frontend/tests/components/commons/**`
- **Verify:** `pnpm frontend:test`
- **Done when:** rubric applied; suite green.

### Phase 8 — Frontend: components — recall (+ recallStats)
- **Status:** planned
- **Type:** Behavior
- **Files:** `frontend/tests/components/recall/**`, `frontend/tests/components/recallStats/**`, `frontend/tests/recall/**`
- Expect capability splits for large files (e.g. NoteRefinement extract).
- **Verify:** `pnpm frontend:test`
- **Done when:** rubric applied; suite green.

### Phase 9 — Frontend: components — notebook, book-reading, search, conversation, admin, recent, notes
- **Status:** planned
- **Type:** Behavior
- **Files:** remaining under `frontend/tests/components/` not covered in Phases 7–8.
- **Verify:** `pnpm frontend:test`
- **Done when:** rubric applied; suite green.

### Phase 10 — Frontend: notes — show / edit / new / toolbar
- **Status:** planned
- **Type:** Behavior
- **Files** under `frontend/tests/notes/` (root-level note UI), e.g. NoteShow, NoteTextContent, NoteEditableContent*, NoteNew*, NoteToolbar*, Card, Questions, NoteInfo, NoteAddQuestion, NoteExport*, NoteMoreOptions*, NoteDeadLink*, FolderSelector*, etc. Leave `notes/sidebar/**` for Phase 11. Leave `NoteAudioTools` / Wikidata* for Phase 12 if oversized.
- **Verify:** `pnpm frontend:test`
- **Done when:** listed files renovated; suite green.

### Phase 11 — Frontend: notes — sidebar
- **Status:** planned
- **Type:** Behavior
- **Files:** `frontend/tests/notes/sidebar/**`
- **Verify:** `pnpm frontend:test`
- **Done when:** rubric applied; suite green.

### Phase 12 — Frontend: notes — audio + Wikidata
- **Status:** planned
- **Type:** Behavior
- **Files:** `NoteAudioTools.spec.ts` (~1k LOC — expect splits), `WikidataAssociationDialog.spec.ts`, `WikidataSearchByLabel.spec.ts`, related note Wikidata specs.
- **Verify:** `pnpm frontend:test`
- **Done when:** rubric applied; suite green.

### Phase 13 — Frontend: links + toolbars
- **Status:** planned
- **Type:** Behavior
- **Files:** `frontend/tests/links/**`, `frontend/tests/toolbars/**` (MainMenu ~667 LOC — split if needed).
- **Verify:** `pnpm frontend:test`
- **Done when:** rubric applied; suite green.

### Phase 14 — Frontend: pages — notebooks / catalog / folder / circle / bazaar / home / settings / misc
- **Status:** planned
- **Type:** Behavior
- **Files** under `frontend/tests/pages/` and `frontend/tests/notebooks/` **except** BookReading, Recall, MemoryTracker, NoteShow (those are Phases 15–17).
- Include: NotebooksPage*, NotebookPage*, NotebookCatalog*, FolderPage*, CircleShow, Bazaar, HomePage, settings/*, FailureReport, NonproductionOnlyLogin, MessageCenter, etc.
- **Verify:** `pnpm frontend:test`
- **Done when:** rubric applied; suite green.

### Phase 15 — Frontend: pages — NoteShow + assimilation / conversation panels
- **Status:** planned
- **Type:** Behavior
- **Files:** `NoteShowPage.spec.ts`, `NoteShowPageAssimilationPanel.spec.ts`, `NoteShowPageConversation.spec.ts`
- **Verify:** `pnpm frontend:test`
- **Done when:** rubric applied; suite green.

### Phase 16 — Frontend: pages — Recall + MemoryTracker
- **Status:** planned
- **Type:** Behavior
- **Files:** `RecallPage.spec.ts` (~898 LOC), `RecallPageOverlap.spec.ts`, `MemoryTrackerPage.spec.ts`, `MemoryTrackerPageView.spec.ts`
- Expect capability splits.
- **Verify:** `pnpm frontend:test`
- **Done when:** rubric applied; suite green.

### Phase 17 — Frontend: pages — BookReading
- **Status:** planned
- **Type:** Behavior
- **Files:** `BookReadingPage.spec.ts` (~1819 LOC — must split into capability-named specs during/after renovation).
- **Verify:** `pnpm frontend:test`
- **Done when:** rubric applied; oversized file split; suite green.

### Phase 18 — Final anti-pattern sweep (all three packages)
- **Status:** planned
- **Type:** Behavior
- **Observable:** remaining smells cleared or explicitly excepted in this PLAN.
- **Method:**
  1. Grep for post-construction `makeMe` mutation / field soup; illicit `vi.mock` of internals; repeated full-payload asserts.
  2. Fix stragglers across `frontend/tests`, `cli/tests`, `mcp-server/tests`.
  3. Document permanent exceptions (and why) in this PLAN.
  4. Mark plan **complete**.
- **Verify:** run all three package test commands green.
- **Done when:** sweep clean (or exceptions documented); plan complete.

---

## Structure slices (insert when blocked)

If a Behavior phase cannot express fixtures concisely: add a **Structure** sub-phase that extends `doughnut-test-fixtures/makeMe` or package test helpers — only for the immediate next Behavior. Record it in this PLAN.

---

## Progress log

| Phase | Status | Notes |
|-------|--------|-------|
| 1 | done | MCP suite renovated; redundant schema suite + unused API-client setup removed |
| 2 | done | CLI non-Ink helpers/commands renovated; oversized selectList + mineru tests split by capability; suite green |
| 3–18 | planned | — |

---

## Discoveries (update while executing)

- Inventory 2026-08-06: MCP 6 / CLI 42 / Frontend ~255 specs; frontend dominates. Largest FE: BookReadingPage (~1819), NoteAudioTools (~1021), RecallPage (~898), SearchResults (~675), MainMenu (~667).
- ~124 frontend specs touch `mockSdkService` / `vi.mock` — many allowed; illicit internal mocks are the cleanup target.
- Trunk `STATE.md` must not claim exclusive focus for this quick task.
- Phase 1: `tool-schemas.test.ts` duplicated registry shape already covered by `server.test.ts` — deleted. `getApiConfig` lives in `doughnut-api`, not MCP — dropped from MCP helpers tests. With full `vi.mock` of SDK controllers, `setupMockApiClient` was dead — removed. No makeMe graph builder needed (single small fixture).
- Phase 2: Most Phase 2 files already drove stable pure helpers / `run` / command surfaces with allowed externals. Main gaps were makeMe for `recallStatus`, focused assertions (drop redundant full-payload / plural-count rechecks), table-driven siblings, and >250-line `selectListInteraction` + `mineruOutlineSubprocess` suites — split into capability-named modules under 250 lines. Files already rubric-compliant left largely untouched (`welcomeBanner`, `spellingAnswerLine`, `terminalColumnsTruncate`, `contestAndRegenerateMcq`, `slashCommandCompletion`, `notebookStageSlashCommands`, `doughnutBackendClient.errors`, `update`, mineru e2e stub).