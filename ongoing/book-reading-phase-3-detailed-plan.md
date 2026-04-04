# Phase 3 (detailed): CLI `/attach` + E2E gate

**Parent plan:** [book-reading-story-1-plan.md](book-reading-story-1-plan.md) — Phase 3.

**Principles:** [.cursor/rules/planning.mdc](../.cursor/rules/planning.mdc) — scenario-first sub-phases, observable assertions, at most one intentionally failing test while driving a change.

**Completion gate:** With `@ignore` removed from [e2e_test/features/book_reading/book_reading.feature](../e2e_test/features/book_reading/book_reading.feature), the step

`When I attach book "top-maths.pdf" to the notebook "Top Maths" via the CLI`

passes by:

1. Using **`/use`** (exact title) so the CLI enters the **notebook stage** with **Top Maths** active.
2. Using **`/attach`** with a **resolved filesystem path** to the fixture PDF.
3. Asserting **past CLI assistant messages** contain an **excerpt of the book layout** derived from the **`Book` returned by `POST …/attach-book`** (same structural cues the UI would use later — e.g. range titles or a stable formatted tree), not from guessing MinerU internals.

**Explicitly out of scope for this gate (Phase 4):** The `Then I should see the book structure … in the browser` line stays **commented out**.

**Scope note vs main Phase 3 blurb:** Story plan Phase 3 also mentions a **separate PDF upload/bind** flow. For this detailed plan, treat **attach-book JSON + assistant excerpt** as the gate; **file bytes on the server** may land in the same phase sequence **after** the E2E gate or as a labeled follow-up sub-phase so the scenario does not depend on blob storage before the CLI/API path is proven.

---

## MinerU: one mocking strategy (local + GitHub Actions)

**Do not run real MinerU** in E2E or in CI.

**Approach:** Keep **`runMineruOutlineSubprocess`** unchanged, but in tagged E2E runs **prepend** [`e2e_test/python_stubs/mineru_site`](../e2e_test/python_stubs/mineru_site) to **`PYTHONPATH`** so a shadow package provides **`mineru.cli.common.read_fn`** and **`do_parse`** (writes deterministic **`{stem}_content_list.json`**). The subprocess still runs the **real** [`minerui-spike/spike_mineru_phase_a_outline.py`](../minerui-spike/spike_mineru_phase_a_outline.py) (same argv as [mineruOutlineSubprocess.ts](../cli/src/commands/read/mineruOutlineSubprocess.ts)), so outline → **`layout.roots`** logic in the spike is exercised.

**JSON contract for attach:** Stdout JSON includes **`outline`** (and optional **`note`**) plus **`layout`** with nested **`roots` / `children`** for **`attach-book`**.

**Wiring in E2E:** Tag **`@mockMineruLib`**: **`Before` (order 0)** stores the mock site path via **`getMineruE2eMockSitePath`** in **`Cypress.env('DOUGHNUT_E2E_MINERU_MOCK_SITE')`**; **`@interactiveCLI`** / **`@interactiveCLIGmail` (order 2)** call **`prependMineruMockToPythonPath`** and set **`PYTHONPATH`** on the PTY env for **`startRepoInteractive`**. **`After`** clears the Cypress env key. Do **not** set **`DOUGHNUT_MINERU_OUTLINE_SCRIPT`** for this flow.

**GitHub Actions:** **`python3`** on `PATH`; no MinerU pip install.

**Production CLI bundle:** Default script resolution and embedding — sub-phase **3.5**.

---

## PDF fixture

- Add **`e2e_test/fixtures/book_reading/top-maths.pdf`** (or equivalent path referenced by the step).
- **Content irrelevant** for parsing if the mock **`do_parse`** ignores bytes; file must be a **valid `.pdf` path** the CLI accepts (extension + readable file).
- The step resolves a path on disk where the interactive CLI process can read it (same patterns as other features that pass filesystem paths to subprocesses).

---

## Sub-phases (order matters for TDD)

Each sub-phase should end with **tests green** except the deliberate red step called out below.

### 3.1 — E2E shadow `mineru` + env wiring (no user-visible attach yet) — **done**

- **Mock:** [e2e_test/python_stubs/mineru_site](../e2e_test/python_stubs/mineru_site) (shadow **`mineru.cli.common`**; stdlib **`python3`** only).
- **Fixture:** [e2e_test/fixtures/book_reading/top-maths.pdf](../e2e_test/fixtures/book_reading/top-maths.pdf) (minimal valid PDF).
- **E2E:** Tag **`@mockMineruLib`** + hooks in [e2e_test/step_definitions/hook.ts](../e2e_test/step_definitions/hook.ts); tasks **`getMineruE2eMockSitePath`** / **`prependMineruMockToPythonPath`** in [e2e_test/config/cliE2ePluginTasks.ts](../e2e_test/config/cliE2ePluginTasks.ts).
- **CLI:** [cli/tests/mineruOutlineSpikeMineruMock.test.ts](../cli/tests/mineruOutlineSpikeMineruMock.test.ts) runs the **spike** with **`PYTHONPATH`** set and asserts **`ok: true`** and **`layout.roots`**.

**Exit:** Local and GHA exercise the real spike script with a fake MinerU; **`@ignore`** unchanged on book-reading until 3.2.

### 3.2 — Cucumber step + page object + stable expected excerpt

- Implement **`When I attach book {string} to the notebook {string} via the CLI`** in thin [e2e_test/step_definitions/](../e2e_test/step_definitions/) glue; put behavior in **`e2e_test/start/pageObjects/cli/`** (and [outputAssertions.ts](../e2e_test/start/pageObjects/cli/outputAssertions.ts) if new assertion shape is needed).
- Flow: ensure interactive CLI session (**`@interactiveCLI`** + **`@withCliConfig`** already on feature), **`/use <exact notebook title>`**, wait for notebook stage, **`/attach <absolute path to fixture>`**, then assert **`pastCliAssistantMessages`** contain **known substrings** from the E2E mock’s deterministic headings (e.g. **Stub Part A**).
- **Remove `@ignore`** once the step exists.

**Expected first failure:** **`/attach`** is not a registered notebook-stage command → user-visible error or “unknown command” consistent with existing shell behavior. Assertion message should say that **attach** is missing or unrecognized (meaningful, not a timeout-only failure).

### 3.3 — Register `/attach` on the notebook stage (shell only)

- Add **`/attach`** to **`notebookStageSlashCommands`** (see [notebookStageSlashCommands.ts](../cli/src/commands/notebook/notebookStageSlashCommands.ts)) with **doc + usage**; implementation returns a **fixed placeholder** assistant line (e.g. “not implemented”) or empty success — enough to survive routing.

**Expected failure:** Assertion fails because assistant text does **not** contain the **layout excerpt** from the API.

### 3.4 — Implement `/attach` (happy path)

- Resolve **active notebook** from stage context (same notebook object **`/use`** selected).
- Run **outline/layout extraction** (shared module factored from **`/read`** spike — see parent plan): subprocess (**real spike**; E2E uses shadow **`mineru`**) → **nested layout** → **`NotebookBooksController` / generated client** `POST /api/notebooks/{id}/attach-book` with **`bookName`** derived from filename (or documented rule) and **`format: "pdf"`**.
- Map **API response** (`Book` + **ranges**) to a **bounded, TTY-safe** assistant message (excerpt of structure — mirror rules like **`READ_OUTLINE_ASSISTANT_MAX_CHARS`** if needed).
- **Vitest:** prefer **`runInteractive`** + **`vi.spyOn`** on the API controller per [.cursor/rules/cli.mdc](../.cursor/rules/cli.mdc) for at least one success and one failure class; avoid duplicating E2E.

**Exit:** E2E scenario **green** with **`@mockMineruLib`** + real backend in **`pnpm run-p …`** stack. **Bundled** CLI can still use checkout-relative script paths until **3.5** proves the same Python invocation from **`cli/dist/doughnut-cli.bundle.mjs`** (or E2E install path).

### 3.5 — Python outline script in the bundle; one spawn path for bundle and source

- The script the CLI passes to **`python3`** (embedded / default **`spike_mineru_phase_a_outline.py`**, or override via **`DOUGHNUT_MINERU_OUTLINE_SCRIPT`**) must be **reachable when the user runs the shipped artifact** (`cli/dist/doughnut-cli.bundle.mjs`), not only via **`pnpm -C cli exec tsx src/index.ts`** or dev layouts.
- Use **one** resolution and spawn approach for **bundled** and **from-source** runs: e.g. embed the script in the bundle (esbuild `loader` / `import` of file contents + write to a temp file at runtime, or equivalent) and pass that path to **`python3`**, or another single mechanism documented in this plan once chosen — avoid a large behavioral fork (`if (bundled) … else …`) except where the runtime truly differs (e.g. only the script bytes source changes).
- **Vitest:** observable check that the bundled entry (or the same helper **`runMineruOutlineSubprocess`** uses) resolves an executable script path and that **`python3`** can run it with **`PYTHONPATH`** shadowing **`mineru`** where practical; do not rely on “works in dev folder layout” alone.
- **CI / E2E:** **`@mockMineruLib`** + **`PYTHONPATH`** remains valid for book-reading; confirm the **default** production resolution still works when the CLI is **`node cli/dist/doughnut-cli.bundle.mjs`** (or the E2E-installed bundle path) without requiring a checkout-relative path that the bundle cannot see.

**Exit:** Same subprocess argv pattern and env knobs as today; bundle and source runs do not diverge in how the Python script is obtained or invoked.

### 3.6 — Exceptional cases for `/attach` (file, subprocess contract, API)

- **File:** missing path, non-`.pdf`, unreadable file — clear assistant or thrown **`userVisibleSlashCommandError`** mapping.
- **Subprocess:** process exits successfully but returns **`ok: false`** or invalid JSON — message surfaces cleanly (contract failure after a **running** interpreter).
- **API:** **409** second attach, **403/404** notebook, validation **400** — user-visible text actionable for support.
- Keep **unit-level** tests for message shape where cheap; **one** E2E optional if a case is regressions-prone and not covered elsewhere.

### 3.7 — Exceptional cases on the Python side (environment and MinerU)

- **`python3` missing or not on `PATH`:** user-visible message (install Python / fix PATH), mapped via **`userVisibleSlashCommandError`** or assistant text consistent with [.cursor/rules/cli.mdc](../.cursor/rules/cli.mdc).
- **MinerU (or required deps) not installed / import fails:** clear, actionable copy (how to install or which env var points at a custom script).
- **MinerU cannot run:** non-zero exit, stderr with useful excerpt (bounded for TTY), timeouts for huge PDFs — honest, support-friendly messages without leaking stack dumps.
- **Optional:** wrong Python version if the team documents a minimum; surface as a single class of error if detectable.
- Prefer **unit** tests with controlled subprocess mocks or fixture stderr where the behavior is message mapping; **E2E** only if a regression slipped through and needs a tag-scoped scenario.

**Exit:** Attach failures from the **host Python/MinerU stack** are categorized and user-visible separately from file/API issues in **3.6**.

### 3.8 — Remove `/read` and dead spike code

- Remove **`readSlashCommand`** from the main registry, **help** text, and any **guidance** references.
- Delete or archive **unused** modules under **`cli/src/commands/read/`** only after **attach** uses the **factored** outline/layout pipeline (no duplicate MinerU wiring).
- Remove **`minerui-spike`** or spike-only assets if nothing references them; update [spike-mineru-read-layout.md](spike-mineru-read-layout.md) or parent plan with a one-line “superseded by attach” note if the team keeps the doc.

**Exit:** `pnpm cli:test` green; no dead `/read` entry points.

---

## After Phase 3

- **Deploy gate:** Per planning checklist, ship before starting Phase 4 unless agreed otherwise.
- Update **[book-reading-story-1-plan.md](book-reading-story-1-plan.md)** Phase 3 with a short “done” summary and any **JSON contract** / upload/bind decisions.
- **OpenAPI / `pnpm generateTypeScript`:** If attach path or `Book` schema changed for the CLI, regenerate as required by [.cursor/rules/generated-backend-api-code-for-frontend.mdc](../.cursor/rules/generated-backend-api-code-for-frontend.mdc).

---

## CI matrix note

**Book reading** lives under **`e2e_test/features/book_reading/`** (letter **b**), so it runs in the existing E2E matrix bucket that already includes **`e2e_test/features/b*/**`** ([ci.yml](../.github/workflows/ci.yml)). No matrix change required unless the team splits specs further.
