# EPUB Phase 9 — sub-phases plan

**Parent plan:** [book-reading-epub-support-plan.md](book-reading-epub-support-plan.md) — Phase 9 "Attach an EPUB from the CLI with no preprocessing".

**Goal of this plan:** break Phase 9 into small, each-a-complete-git-commit slices (~5 minutes of focused work each). Each slice either delivers externally observable CLI attach behavior or restructures the codebase specifically to enable the immediate next behavior slice — no speculative prep. Along the way, collapse the EPUB-vs-PDF duplication in the CLI attach path (upload helper, slash-command dispatcher) so the shared attach concept stays cohesive.

## Guiding principles for these sub-phases

- **Stop-safe:** after any sub-phase main is shippable. CLI PDF attach never regresses; EPUB attach is either absent or fully working.
- **One behavior per behavior-slice; one enabling refactor per structure-slice.** Each structure slice is justified by the **next** behavior slice, not a later one.
- **Cohesion over parallel stacks:** one upload helper, one `/attach` dispatcher, one CLI page-object attach helper — format is a parameter, not a duplicated module.
- **Reuse server-side EPUB path:** no second extraction pipeline. The CLI just sends raw bytes with `format: "epub"` to the same `attach-book` endpoint the browser already drives (Phase 1–2).
- **Observable tests first:** add tests from the CLI entry point (`InteractiveCliApp` via `renderInkWhenCommandLineReady`) for unit behavior, including assertions on `attachNotebookBookFile` arguments for EPUB (`format`, `bookName`, resolved path). **No dedicated CLI EPUB Cypress scenario:** the browser EPUB stack stays covered by existing `epub_book.feature` (upload); duplicating install-PTY → browser for CLI attach was dropped as redundant. Do not re-test the full EPUB browser matrix from the CLI side.
- **`@wip` for scenarios that outrun the code.** Keep the budget well under 5.

---

## The central refactor: one attach pipeline, format is a parameter

Today the CLI attach path is PDF-shaped end to end:

| Layer | Today (PDF-only) | After Phase 9 (format-aware) |
|---|---|---|
| Upload helper in `doughnutBackendClient.ts` | `attachNotebookBookWithPdf(notebookId, metadata, pdfAbsolutePath)` hardcodes `application/pdf` Blob type | One helper that takes the absolute file path and derives `Content-Type` from `metadata.format` (`application/pdf` vs `application/epub+zip`) |
| Slash-command entry in `notebookAttachSlashCommand.tsx` | `runNotebookAttachPdf(notebook, path)` rejects non-`.pdf`, runs MinerU unconditionally | `runNotebookAttach(notebook, path)` detects format by extension, dispatches to PDF branch (MinerU + layout) or EPUB branch (raw upload, no preprocessing) |
| `CommandDoc` + spinner | Usage `"/attach <path to pdf>"`; spinner `"Attaching PDF…"` | Usage `"/attach <path to .pdf or .epub>"`; spinner `"Attaching book…"` |

Nothing about the backend changes — `BookService.attachBook` and the `attach-book` controller are already format-aware from Phase 1. Phase 9 is pure CLI work; proof is **Vitest** through `InteractiveCliApp` (no dedicated CLI EPUB E2E).

---

## Architecture opportunities beyond Phase 9 (not scheduled)

Noted here so future work can pick them up; Phase 9 behavior does not need them:

- Generate the attach helper from the OpenAPI-described `attach-book` operation instead of hand-rolling `FormData` + `fetch` in `doughnutBackendClient.ts`. Today the generated SDK skips multipart; a future pass could replace the custom helper once the generator supports it.
- Factor `runMineruOutlineSubprocess` + `truncateForBookOutlineAssistant` out of `notebookAttachSlashCommand.tsx` into a PDF-specific preprocessing module so the slash-command stays a thin dispatcher. Defer until a third format or a second PDF call site appears.
- CLI error-mapping symmetry for EPUB-specific server errors (DRM, invalid container). The existing `readableClientErrorDetail` + `doughnutApiErrorFromThrowable` path already surfaces backend messages; keep a watching brief and only add CLI-specific copy if a real fixture shows a gap.

---

## Sub-phases

Numbering is **9.N** and is plan-only bookkeeping — commit messages, test names, and any permanent artifact are named by **capability** (e.g. `InteractiveCliApp.useNotebook`, `attachNotebookBookFile`), never by sub-phase number.

### 9.1 — Structure: generalize the upload helper to carry any book file

**Why now:** The very next slice (9.2) extracts a format dispatcher inside the slash command; the slice after that (9.3) wires EPUB. Both depend on an upload helper that is not PDF-shaped. Doing this first keeps 9.3 small.

**Scope:**
- In `cli/src/backendApi/doughnutBackendClient.ts`, rename `attachNotebookBookWithPdf` → `attachNotebookBookFile`. Derive the multipart Blob `type` from `metadata.format` (`'pdf'` → `application/pdf`, `'epub'` → `application/epub+zip`). Signature stays `(notebookId, metadata: AttachBookRequestFull, absolutePath, signal?)`.
- Update the only caller (`notebookAttachSlashCommand.tsx`) and its test (`cli/tests/InteractiveCliApp.useNotebook.test.tsx`) to the new name. No behavior change for PDF.

**Tests:**
- Existing `notebook stage /attach` PDF tests stay green unchanged (import name updated). No new tests in this slice.

### 9.2 — Structure: extract a format dispatcher inside the slash command

**Why now:** Needed by 9.3's EPUB branch so the PDF-specific MinerU work does not leak into the EPUB code path. Keeps 9.3 additive.

**Scope:**
- In `notebookAttachSlashCommand.tsx`, rename `runNotebookAttachPdf` → `runNotebookAttach(notebook, bookPath)`.
- Introduce a small local helper `detectBookAttachFormat(path): 'pdf' | 'epub' | undefined` (lowercased extension match).
- Split the existing PDF logic into a private `runNotebookAttachPdfPipeline(notebook, absPath, baseName)` that takes an already-resolved absolute path and filename stem; `runNotebookAttach` handles path validation, format detection, dispatch, and the `"Attach supports .pdf or .epub files."` rejection message for unknown extensions. (EPUB branch still rejects in this slice — dispatcher shape only.)
- Update the stage's spinner label to `"Attaching book…"` (format-agnostic).

**Tests:**
- Update the existing "rejects attach when path is not a file" / "rejects attach when PDF path is missing" / spinner tests in `cli/tests/InteractiveCliApp.useNotebook.test.tsx` to the new wording (`"Attaching book"` instead of `"Attaching PDF"`, and the new unknown-extension error text if exercised).
- All other PDF-pipeline tests stay green unchanged.

### 9.3 — Behavior: `/attach <file.epub>` attaches an EPUB via the CLI

**Why now:** First user-visible Phase 9 behavior. Rides entirely on 9.1 + 9.2.

**Scope:**
- In `notebookAttachSlashCommand.tsx`, add the EPUB branch inside `runNotebookAttach`: when `detectBookAttachFormat(path) === 'epub'`, resolve to an absolute path, `stat()` it (same file-not-found / is-a-directory checks as PDF), build `AttachBookRequestFull` with `bookName = basename(path, extname(path))`, `format: 'epub'`, no `layout`/`contentList`, and call `attachNotebookBookFile(notebook.id, metadata, absPath)`. Reuse `bookBlocksTreeLines` + `truncateForBookOutlineAssistant` to format the assistant response.
- Update `CommandDoc` usage to `"/attach <path to .pdf or .epub>"` and refresh its description so it says `pdf` attach uses MinerU; `epub` attach is a raw upload (no Python, no preprocessing).
- Update `notebookAttachSlashCommand.tsx`'s `argument` label to `"path to book file"` (or similar) so `/` guidance doesn't imply PDF-only.
- Make sure MinerU is **not** invoked on the EPUB branch (no `runMineruOutlineSubprocess` call).

**Tests:** extend the `notebook stage /attach` describe in `cli/tests/InteractiveCliApp.useNotebook.test.tsx`:
- `attaches EPUB and shows structure excerpt from API book` — happy path: stub `attachNotebookBookFile` to return a book with `format: 'epub'` and a couple of EPUB blocks; assert the assistant shows `Attached "my-book" to this notebook.` and the block titles.
- `EPUB attach calls attachNotebookBookFile with epub metadata and resolved path` — after `/attach` on an `.epub` file, assert the spy was called with `{ bookName, format: 'epub' }` and the resolved absolute path (wires the EPUB branch to the upload helper).
- `EPUB attach does not invoke MinerU` — assert `runMineruOutlineSubprocess` is not called on `/attach foo.epub`.
- `rejects attach when extension is neither .pdf nor .epub` — assert the `"Attach supports .pdf or .epub files."` error.
- `rejects attach when EPUB path is missing` — symmetrical to the existing PDF missing-path case.

Existing PDF tests remain green.

### 9.4 — Dropped: CLI E2E attach page-object

**Status:** Not pursued. A format-agnostic `bookReadingCli` page object was considered for a Cypress scenario; the project **chose not to keep** a dedicated CLI EPUB E2E. No `e2e_test/start/pageObjects/cli/bookReadingCli.ts` in tree; PDF+CLI flows continue to use `notebookInteractiveCliSession` / existing steps as before.

### 9.5 — Dropped: CLI attach-EPUB Cypress E2E

**Status:** Intentionally **not** implemented. Rationale: `InteractiveCliApp.useNotebook` Vitest coverage already exercises the CLI attach UX and EPUB branch; asserting `attachNotebookBookFile` closes the main gap versus a full E2E. Browser EPUB reading remains proven by `e2e_test/features/book_reading/epub_book.feature` (upload path). A separate `@bundleCliE2eInstall` scenario would mostly duplicate those concerns at higher cost.

If the team later wants proof on the **installed** binary specifically, reintroduce a thin Cypress spec rather than expanding the EPUB matrix.

### 9.6 — Cleanup, plan refresh, and interim-wording removal (done)

**Why now:** Phase-discipline gate for closing Phase 9.

**Scope (shipped):**
- CLI attach user-facing strings audited; remaining PDF-specific wording only where it is genuinely PDF-only (MinerU env vars and the MinerU pipeline error text).
- Upload-too-large error wording generalized now that the multipart endpoint serves both formats: backend `ControllerSetup.handleMultipartException` returns `"The uploaded file exceeds the maximum upload size (100 MB)."`; CLI `http413PayloadTooLarge` fallback reads `"The file exceeds the maximum upload size the server accepts. Try a smaller file."`; matching test updated.
- `notebookAttachSlashCommand.tsx` cleaned up: dead `.pdf` conditional removed (dispatcher already guarantees the extension in `runPdfAttach`); assistant-message formatting extracted to `attachedBookAssistantMessage`; book-name derivation unified via `bookNameFromPath`; `runEpubAttach` no longer takes the redundant trimmed path.
- `ongoing/book-reading-epub-support-plan.md`: Phase 9 flipped to "Scope (shipped)" / "Testing (shipped)" style matching Phases 4–7; phase summary table updated.
- `ongoing/book-reading-user-stories.md`: added "Attach an EPUB from the CLI (shipped)" sub-story under "Story: EPUB book".

**Tests:** no new tests; all prior CLI PDF E2E scenarios, EPUB browser E2E, and EPUB CLI Vitest cases stay green.

---

## Mapping back to parent Phase 9 scope

| Parent Phase 9 bullet | Sub-phase(s) |
|---|---|
| Extend `/attach` to accept `.epub` | 9.2, 9.3 |
| Route EPUB attach as raw file upload with `format: "epub"` only | 9.1, 9.3 |
| Keep PDF CLI behavior unchanged | 9.1, 9.2 (rename-only refactors), 9.6 (audit) |
| Reuse the same backend EPUB attach path already exercised by the frontend upload | 9.3 (no new backend call) |
| CLI test for `.epub` routing and multipart payload shape | 9.3 (unit tests through `InteractiveCliApp`, including `attachNotebookBookFile` args) |
| Optional E2E: CLI attach → browser (installed binary) | **Dropped** — see §9.5; browser EPUB covered by `epub_book.feature` |

## Stop-safety check per sub-phase

| After… | Main branch state |
|---|---|
| 9.1 | PDF attach unchanged; upload helper renamed and format-aware under the hood |
| 9.2 | PDF attach unchanged; `/attach` has a dispatcher; unknown extension gives a clearer error; EPUB still rejected |
| 9.3 | **PDF attach unchanged; `/attach file.epub` now works in the CLI** (covered by unit tests) |
| 9.4 | *Skipped* (no CLI EPUB E2E page object) |
| 9.5 | *Skipped* (Vitest + existing browser EPUB E2E instead of dedicated CLI EPUB Cypress) |
| 9.6 | Cleanup only: wording, user-stories, plan doc |

## Commit checklist per sub-phase

1. Tests written or extended alongside the slice, failing for the right reason before the code change (for behavior slices; structure slices must keep existing tests green).
2. `pnpm cli:test` for any `cli/` change; targeted Cypress `--spec` only when a CLI **browser** scenario is added or changed (not required for Phase 9 EPUB attach if only Vitest changes).
3. Lint / format: `pnpm lint:all` (scope to touched files is usually enough).
4. Commit with a capability-focused message (e.g. "CLI `/attach` supports .epub", "Unify CLI book attach upload helper"); no "Phase 9.x" in the message body.
5. Push; let CD deploy before starting the next sub-phase.
