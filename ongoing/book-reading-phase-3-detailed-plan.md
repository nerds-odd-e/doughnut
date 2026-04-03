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

**Recommended single approach:** Keep using **`runMineruOutlineSubprocess`** (or a thin wrapper shared with attach) but point **`DOUGHNUT_MINERU_OUTLINE_SCRIPT`** at a **checked-in stub script** that:

- Is invoked like the real spike (`python3`, same argv pattern including `--json-result`, PDF tmp dir args as today in [cli/src/commands/read/mineruOutlineSubprocess.ts](../cli/src/commands/read/mineruOutlineSubprocess.ts)).
- Validates the book path exists / is readable (mirrors real precondition failures).
- Prints **one JSON object on stdout** compatible with existing parsing, **extended** as needed for attach (see below).

**Why Python for the stub:** The production path already spawns **`python3`** with a script path. A **stdlib-only** stub avoids new native dependencies and works the same locally and on **`ubuntu-latest`** runners (Python 3 is present; no `actions/setup-python` required unless the team wants a pinned version — then add one step to the E2E job).

**Alternatives (avoid unless the stub approach blocks):** A separate env that **skips spawn** and reads a fixture from disk introduces a second behavioral path to maintain; prefer one subprocess-shaped stub unless subprocess timing/flakiness forces a bypass.

**JSON contract extension for attach:** Today the subprocess yields text **`outline`** (and optional **`note`**) for `/read`. **`/attach`** must build the Phase 2 **nested `layout.roots` / `children`** payload.

**Implemented (sub-phase 3.1):** Successful stdout JSON may include an optional top-level **`layout`** object with the same shape as **`attach-book`** request body **`layout`**: **`roots`** array of nodes with **`title`**, **`startAnchor`**, **`endAnchor`**, optional **`children`**. Anchors use **`anchorFormat`** **`pdf.mineru_outline_v1`** and opaque **`value`** (JSON string) in the E2E stub. The checked-in stub is [e2e_test/scripts/mineru_outline_e2e_stub.py](../e2e_test/scripts/mineru_outline_e2e_stub.py); production MinerU wiring should emit **`layout`** when ready, or a dedicated attach entrypoint can reuse the same spawn helper.

**Wiring stub in E2E:** Tag **`@stubMineruOutline`**: a **`Before` (order 0)** stores the absolute stub path from Cypress task **`getMineruOutlineE2eStubScriptPath`** in **`Cypress.env('DOUGHNUT_MINERU_OUTLINE_SCRIPT_E2E')`**; **`@interactiveCLI` (order 2)** merges **`DOUGHNUT_MINERU_OUTLINE_SCRIPT`** into the env passed to **`startRepoInteractive`** when that env value is set. An **`After`** on the same tag clears the Cypress env key. Apply **`@stubMineruOutline`** only to book-reading (or other features that need the stub) so other interactive CLI tests keep default MinerU script resolution.

**GitHub Actions:** Confirm once in the E2E job that **`python3`** is on `PATH` (document finding; add a trivial `python3 -c 'print(1)'` step only if a runner image regression is a concern). No MinerU pip install in CI.

---

## PDF fixture

- Add **`e2e_test/fixtures/book_reading/top-maths.pdf`** (or equivalent path referenced by the step).
- **Content irrelevant** for parsing if the stub ignores bytes; file must be a **valid `.pdf` path** the CLI accepts (extension + readable file).
- The step resolves a path on disk where the interactive CLI process can read it (same patterns as other features that pass filesystem paths to subprocesses).

---

## Sub-phases (order matters for TDD)

Each sub-phase should end with **tests green** except the deliberate red step called out below.

### 3.1 — Stub script + E2E env wiring (no user-visible attach yet) — **done**

- **Stub:** [e2e_test/scripts/mineru_outline_e2e_stub.py](../e2e_test/scripts/mineru_outline_e2e_stub.py) (stdlib **`python3`**).
- **Fixture:** [e2e_test/fixtures/book_reading/top-maths.pdf](../e2e_test/fixtures/book_reading/top-maths.pdf) (minimal valid PDF).
- **E2E:** Feature tag **`@stubMineruOutline`** + hooks in [e2e_test/step_definitions/hook.ts](../e2e_test/step_definitions/hook.ts); task **`getMineruOutlineE2eStubScriptPath`** in [e2e_test/config/cliE2ePluginTasks.ts](../e2e_test/config/cliE2ePluginTasks.ts).
- **CLI:** [cli/tests/mineruOutlineE2eStub.test.ts](../cli/tests/mineruOutlineE2eStub.test.ts) runs the stub via **`python3`** on a temp **`.pdf`** and asserts **`ok: true`** and **`layout.roots`**.

**Exit:** Local and GHA can run the Python stub the same way; **`@ignore`** unchanged on book-reading until 3.2.

### 3.2 — Cucumber step + page object + stable expected excerpt

- Implement **`When I attach book {string} to the notebook {string} via the CLI`** in thin [e2e_test/step_definitions/](../e2e_test/step_definitions/) glue; put behavior in **`e2e_test/start/pageObjects/cli/`** (and [outputAssertions.ts](../e2e_test/start/pageObjects/cli/outputAssertions.ts) if new assertion shape is needed).
- Flow: ensure interactive CLI session (**`@interactiveCLI`** + **`@withCliConfig`** already on feature), **`/use <exact notebook title>`**, wait for notebook stage, **`/attach <absolute path to fixture>`**, then assert **`pastCliAssistantMessages`** contain **known substrings** from the stub’s layout (titles agreed with stub output).
- **Remove `@ignore`** once the step exists.

**Expected first failure:** **`/attach`** is not a registered notebook-stage command → user-visible error or “unknown command” consistent with existing shell behavior. Assertion message should say that **attach** is missing or unrecognized (meaningful, not a timeout-only failure).

### 3.3 — Register `/attach` on the notebook stage (shell only)

- Add **`/attach`** to **`notebookStageSlashCommands`** (see [notebookStageSlashCommands.ts](../cli/src/commands/notebook/notebookStageSlashCommands.ts)) with **doc + usage**; implementation returns a **fixed placeholder** assistant line (e.g. “not implemented”) or empty success — enough to survive routing.

**Expected failure:** Assertion fails because assistant text does **not** contain the **layout excerpt** from the API.

### 3.4 — Implement `/attach` (happy path)

- Resolve **active notebook** from stage context (same notebook object **`/use`** selected).
- Run **outline/layout extraction** (shared module factored from **`/read`** spike — see parent plan): subprocess stub → **nested layout** → **`NotebookBooksController` / generated client** `POST /api/notebooks/{id}/attach-book` with **`bookName`** derived from filename (or documented rule) and **`format: "pdf"`**.
- Map **API response** (`Book` + **ranges**) to a **bounded, TTY-safe** assistant message (excerpt of structure — mirror rules like **`READ_OUTLINE_ASSISTANT_MAX_CHARS`** if needed).
- **Vitest:** prefer **`runInteractive`** + **`vi.spyOn`** on the API controller per [.cursor/rules/cli.mdc](../.cursor/rules/cli.mdc) for at least one success and one failure class; avoid duplicating E2E.

**Exit:** E2E scenario **green** with stub + real backend in **`pnpm run-p …`** stack.

### 3.5 — Exceptional cases for `/attach`

- **File:** missing path, non-`.pdf`, unreadable file — clear assistant or thrown **`userVisibleSlashCommandError`** mapping.
- **Subprocess:** stub returns **`ok: false`** or invalid JSON — message surfaces cleanly.
- **API:** **409** second attach, **403/404** notebook, validation **400** — user-visible text actionable for support.
- Keep **unit-level** tests for message shape where cheap; **one** E2E optional if a case is regressions-prone and not covered elsewhere.

### 3.6 — Remove `/read` and dead spike code

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
