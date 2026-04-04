# Meta-plan: PDF attach + download (old Phase 1 + Phase 2)

**Parent intent:** [book-reading-read-a-range-plan.md](book-reading-read-a-range-plan.md) — combine **Phase 1** (CLI uploads PDF, browser can obtain file, dev/test storage) and **Phase 2** (production GCP bucket, **same** user-visible attach/download flow).

**Planning discipline:** [.cursor/rules/planning.mdc](../.cursor/rules/planning.mdc) — one **user-visible** (or **integrator-visible**, where noted) slice per sub-phase, scenario-first ordering, **all tests pass** at the end of each sub-phase, **no dead code**, **no historical comments**; prefer **observable** tests (HTTP, Cypress, CLI); **at most one intentionally failing test** while driving a given sub-phase.

**Architecture constants (all sub-phases):**

- **Single attach surface:** `POST /api/notebooks/{notebook}/attach-book` — PDF + outline **together** (multipart); no separate product “upload-only” API.
- **Opaque `Book.source_file_ref`:** Only persisted locator the domain uses; **`BookStorage`** **puts** bytes → ref, **gets** ref → bytes (or streams). The type is **format-agnostic**; today’s implementations and routes are **PDF**. **Non-`prod`** Spring profiles use DB (`AttachmentBlob`); **`prod`** uses GCS object key only — **same** interface, **no** DB fallback in production.
- **Download contract:** Authenticated **`GET`** on a **documented** path (e.g. `/api/notebooks/{notebook}/book/file`) that streams PDF with correct headers; browser and CLI both rely on **same** contract. Phase 2 may stream **from GCS server-side** first (keeps cookie/session model identical to Phase 1); redirects/signed URLs stay an optional later optimization **without** changing the path if desired.

**Explicitly not in this meta-plan:** pdf.js, drawer, range sync (Phase 3+ of the parent doc).

**Book file storage (PDF today) — testing split:**

- **E2E** (book reading / attach / download scenarios, including SP-B2) runs **only** against **DB** `BookStorage`. CI and local SUT use **non-`prod`** Spring profiles, so Cypress never asserts behavior against real GCS.
- **GCS** `BookStorage` is covered by **unit tests** only (e.g. mocked `com.google.cloud.storage.Storage` in [`GcsBookStorageTest`](backend/src/test/java/com/odde/doughnut/services/book/GcsBookStorageTest.java)); **no** Gherkin / Cypress variant for GCS.

---

## Sub-phase ordering (fine-grained)

Each row is a **merge gate**: backend verify, relevant frontend/CLI tests, and **any** E2E spec touched in that sub-phase must be green before starting the next. **Deploy gate** per [.cursor/rules/planning.mdc](../.cursor/rules/planning.mdc): commit/push/CD when the team’s usual cadence requires it between sub-phases.

### SP-A1 — Download route exists; empty books get a clear 404

- **Outcome (integrator-visible):** `GET .../book/file` is registered, **read-authorized** like `GET .../book`, returns **404** when the notebook has no book or `source_file_ref` is blank.
- **Tests:** Extend [NotebookBooksControllerTest](backend/src/test/java/com/odde/doughnut/controllers/NotebookBooksControllerTest.java) (or equivalent) for 404 + auth; no storage bean required yet beyond what already exists.
- **Deliverable cleanliness:** No unused beans; if a placeholder method exists on a future storage type, **do not** add it until SP-A2.

### SP-A2 — `BookStorage` (DB) + happy-path download

**Done.** `BookStorage` / `DbBookStorage`, `AttachmentBlobRepository`, `GET .../book/file` streams PDF with `Content-Disposition`; tests in `NotebookBooksControllerTest`.

- **Outcome (integrator-visible):** First **production** use of storage: **`BookStorage`** + DB-backed implementation (persist/load via `AttachmentBlob` pattern). Download handler resolves `source_file_ref` through **`get`** and returns **200** `application/pdf` with **Content-Disposition** (filename sanitization consistent with existing export endpoints).
- **Tests:** Controller-level test with **real** persistence (`makeMe` / test setup) so the **full** GET path is exercised; include **404** when ref is corrupt/unknown if the implementation can detect it cheaply.
- **Deliverable cleanliness:** Interface + **one** default `@Bean` implementation wired; **no** orphan implementations.

### SP-A3 — Multipart attach **alongside** existing JSON attach

**Done.** Second handler on `POST .../attach-book` with `consumes = MULTIPART_FORM_DATA` (`@RequestPart` **`metadata`** + **`file`**); **`BookService.attachBookWithPdf`** + shared **`persistNewBook`**; empty/missing **`file`** → **400**. OpenAPI has **one** `post` (spec limit for same path/method) with **`application/json`** and **`multipart/form-data`** in **`requestBody.content`**; **[`SwaggerConfig`](backend/src/main/java/com/odde/doughnut/configs/SwaggerConfig.java)** **`attachBookOperationIdCustomizer`** sets **`operationId: attachBook`** so **`pnpm generateTypeScript`** still emits **`NotebookBooksController.attachBook`** for JSON (CLI unchanged until SP-A4). Multipart callers use **`multipart/form-data`** on the same URL.

- **Outcome (integrator-visible):** Second `POST .../attach-book` handler (or equivalent) with `consumes = MULTIPART_FORM_DATA`: parts **`metadata`** (`AttachBookRequest` JSON) + **`file`** (PDF). On success, **`BookStorage.put`**, set **`source_file_ref`**, same outline persistence as today. **Existing JSON-only attach** remains **unchanged** so current CLI and tests keep working.
- **Tests:** **`AttachBookMultipart`** nested class in [NotebookBooksControllerTest](backend/src/test/java/com/odde/doughnut/controllers/NotebookBooksControllerTest.java); **all** existing attach tests still green.
- **OpenAPI / codegen:** Regenerate with **`pnpm generateTypeScript`**; do not hand-edit **`open_api_docs.yaml`**.

### SP-A4 — CLI uses multipart attach

- **Outcome (user-visible for CLI):** `/attach` sends **FormData** (metadata + file) to the multipart endpoint; success message and outline behavior unchanged.
- **Tests:** [cli Vitest](cli/tests/) updates for spies/mocks; **existing** [book_reading.feature](e2e_test/features/book_reading/book_reading.feature) scenario still passes (still hits JSON? No — after this, CLI hits multipart; E2E uses real CLI → must pass end-to-end).
- **Deliverable cleanliness:** No client code path still sending **only** JSON for new attaches unless documented as deprecated internal test helper (prefer not — tests should use multipart too).

### SP-A5 — Remove JSON-only attach; single consumes

**Done.** JSON `attachBook` handler removed; single multipart `attachBook` on [`NotebookBooksController`](backend/src/main/java/com/odde/doughnut/controllers/NotebookBooksController.java); **`BookService.attachBook`** removed; OpenAPI + SDK are multipart-only; [`NotebookBooksControllerTest`](backend/src/test/java/com/odde/doughnut/controllers/NotebookBooksControllerTest.java) uses multipart throughout.

- **Outcome (integrator-visible):** **One** attach contract: **multipart only**. All controller tests and fixtures use multipart.
- **Tests:** Full backend + CLI + book_reading E2E green.
- **Deliverable cleanliness:** Delete dead JSON attach mapping and any helpers only used for it; **no** dual-path attach left.

### SP-B1 — API exposes “has file” + book reading download control

**Done.** [`Book`](backend/src/main/java/com/odde/doughnut/entities/Book.java) exposes **`hasSourceFile`** on `BookViews.Full`; **`sourceFileRef`** is not serialized (`@JsonIgnore` on getter). [BookReadingPage.vue](frontend/src/pages/BookReadingPage.vue) download link `data-testid="book-download-pdf"` → `/api/notebooks/{id}/book/file`. Vitest: [BookReadingPage.spec.ts](frontend/tests/pages/BookReadingPage.spec.ts). Controller tests for `getHasSourceFile()` in [NotebookBooksControllerTest](backend/src/test/java/com/odde/doughnut/controllers/NotebookBooksControllerTest.java).

- **Outcome (user-visible in browser):** `Book` JSON (appropriate `JsonView`) includes something like **`hasSourceFile`** derived from **`source_file_ref`**. [BookReadingPage.vue](frontend/src/pages/BookReadingPage.vue) shows a **Download** control (stable **`data-testid`**) when true; link targets the same-origin GET download URL with session cookie.
- **Tests:** Frontend unit test if there is an established pattern for this page; otherwise minimal component assertion — **do not** skip E2E in SP-B2 solely because of this test.
- **Deliverable cleanliness:** No feature flags dead in code; unused generated SDK types cleaned up if any.

### SP-B2 — E2E: CLI attach + browser download

- **Outcome (user-visible E2E):** New Cucumber scenario: attach fixture PDF via CLI → open notebook → Read → **Download** → assert PDF (magic bytes or fixture comparison) via **intercept**, **`cy.request`** with session, or reliable downloads-folder pattern — pick one style and **reuse** patterns from [e2e_test](e2e_test) (e.g. obsidian/audio lessons). **Storage:** scenario assumes **DB** `BookStorage` (non-`prod` profile); **do not** run this E2E against GCS.
- **Tests:** Single focused feature file/spec run in CI for this change set.
- **Deliverable cleanliness:** Page object methods colocated; step definitions stay thin.

### SP-C1 — GCS `BookStorage` implementation (prod only)

**Done.** [`GcsBookStorage`](backend/src/main/java/com/odde/doughnut/services/book/GcsBookStorage.java), [`BookStorageConfiguration`](backend/src/main/java/com/odde/doughnut/configs/BookStorageConfiguration.java) — **`@Profile("prod")`** for GCS `Storage` + `BookStorage`; **`@Profile("!prod")`** for [`DbBookStorage`](backend/src/main/java/com/odde/doughnut/services/book/DbBookStorage.java) (no property-based fallback). Prod requires **`doughnut.book-pdf.gcs.bucket`**. Note in [`application.yml`](backend/src/main/resources/application.yml); [`GcsBookStorageTest`](backend/src/test/java/com/odde/doughnut/services/book/GcsBookStorageTest.java).

- **Outcome (integrator-visible):** Second implementation of **`BookStorage`** using GCP **Storage** client: **put** uploads object, **get** reads bytes. **`prod`** profile **only** — uses GCS; **all other profiles** use DB from SP-A2. **No** DB storage bean in production.
- **Tests:** **Black-box** unit tests with **mocked** `Storage` — **no** real network; **no** E2E against GCS (see testing split above).
- **Deliverable cleanliness:** Required prod properties documented; **no** unreachable GCS code paths.

### SP-C1b — GCP bucket + prod config (operator)

**Done.** Bucket **`doughnut-book-pdf-carbon-syntax-298809`** (global name **`books`** unavailable). Runbook: [docs/gcp/prod_env.md](../docs/gcp/prod_env.md) §7; prod bucket name: [`application.yml`](../backend/src/main/resources/application.yml) prod `doughnut.book-pdf.gcs.bucket`. IAM: **`roles/storage.objectAdmin`** for **`220715781008-compute@developer.gserviceaccount.com`**.

- **Outcome (operator-visible):** Use **`gcloud`** to create a GCS bucket in the target GCP project (document the exact command, including region / uniform bucket-level access if required by org policy). Update **production** configuration so **`doughnut.book-pdf.gcs.bucket`** matches that bucket (and **`object-prefix`** only if the team standardizes on one).
- **Tests:** None required for this sub-phase (infra / manual); backend tests and E2E remain on **non-`prod`** profiles (DB storage).
- **Deliverable cleanliness:** Bucket name matches deployed config; single source of truth in ops docs / this file.

### SP-C2 — Wire profile selection + integration proof

**Done.** Comments in [`application.yml`](../backend/src/main/resources/application.yml) (default + `prod` blocks) describe **`prod` → GCS** (bucket required) and **non-`prod` → DB**; integration proof: [`BookStorageProdWiringTest`](../backend/src/test/java/com/odde/doughnut/integration/BookStorageProdWiringTest.java) — `AnnotationConfigApplicationContext` with **`prod`** profile, **`BookStorageConfiguration`** + mock `Storage` (bean override), asserts a single **`GcsBookStorage`** bean and **`BookService`** wiring.

- **Outcome (operator-visible):** `application.yml` (or deployment env) documents **`prod` = GCS** (bucket required) and **non-`prod` = DB**. One **integration-style** test that proves **`BookStorageConfiguration`** with **`prod`** yields **`GcsBookStorage`** for **`BookService`** (no `DbBookStorage` bean in that context).
- **Tests:** That integration test + full **backend:verify** green.
- **Deliverable cleanliness:** No duplicate storage selection logic in multiple packages.

### SP-D1 — Hardening and parent-doc alignment

- **Outcome:** Limits (max upload size), consistent error bodies on attach failure, logging without secrets, **delete/orphan** policy **documented** for GCS objects when book row is removed (actual delete behavior may belong to a later story — here: **explicit** “not done” or “done” in one sentence).
- **Tests:** Add only **regression** tests that failed to exist before (e.g. oversize file → 4xx); keep **minimal**.
- **Deliverable cleanliness:** Update [book-reading-read-a-range-plan.md](book-reading-read-a-range-plan.md) Phase 1/2 bullets to point at this meta-plan and mark completed sub-phases; remove obsolete interim notes from **this** file if any.

---

## CI notes

- **Chosen strategy:** **GCS:** mocked-`Storage` unit tests only. **Book attach/download E2E:** **DB** storage (non-`prod` profile). **No** CI job runs book-reading Gherkin against real GCS.
- **Prod / GCP:** Bucket **`doughnut-book-pdf-carbon-syntax-298809`** and `doughnut.book-pdf.gcs.bucket` alignment — see **SP-C1b**; ADC on the MIG VM.

---

## Sub-phase checklist (mechanical)

After **each** SP-A* / SP-B* / SP-C* / SP-D*:

1. **Tests:** `pnpm backend:verify` (or scoped), `pnpm cli:test` if CLI touched, `pnpm frontend:test` if frontend touched, Cypress **spec** for book reading if E2E touched.
2. **Dead code:** No unused storage impls, no deprecated attach paths left after SP-A5.
3. **Codegen:** If controllers/DTOs changed, `pnpm generateTypeScript` and fix dependents.
4. **Plan:** Tick sub-phase in this doc or in parent roadmap; drop text that no longer helps the **current** snapshot.

---

## If you must pause mid-stream

Prefer finishing through **SP-A5** before any deploy so attach contract is **singular**. Prefer finishing **SP-B2** before hardening GCS so the **E2E** baseline exists on **DB** storage. Complete **SP-C1b** (GCS bucket + prod config) before relying on attach/download in **prod**.
