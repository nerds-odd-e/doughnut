# Meta-plan: PDF attach + download (old Phase 1 + Phase 2)

**Parent intent:** [book-reading-read-a-range-plan.md](book-reading-read-a-range-plan.md) — combine **Phase 1** (CLI uploads PDF, browser can obtain file, dev/test storage) and **Phase 2** (production GCP bucket, **same** user-visible attach/download flow).

**Planning discipline:** [.cursor/rules/planning.mdc](../.cursor/rules/planning.mdc) — one **user-visible** (or **integrator-visible**, where noted) slice per sub-phase, scenario-first ordering, **all tests pass** at the end of each sub-phase, **no dead code**, **no historical comments**; prefer **observable** tests (HTTP, Cypress, CLI); **at most one intentionally failing test** while driving a given sub-phase.

**Architecture constants (all sub-phases):**

- **Single attach surface:** `POST /api/notebooks/{notebook}/attach-book` — PDF + outline **together** (multipart); no separate product “upload-only” API.
- **Opaque `Book.source_file_ref`:** Only persisted locator the domain uses; **`BookPdfStorage`** (name indicative) **puts** bytes → ref, **gets** ref → bytes (or streams). Phase 1 default implementation = DB (`AttachmentBlob`); Phase 2 = GCS object key (or equivalent) behind the **same** interface.
- **Download contract:** Authenticated **`GET`** on a **documented** path (e.g. `/api/notebooks/{notebook}/book/file`) that streams PDF with correct headers; browser and CLI both rely on **same** contract. Phase 2 may stream **from GCS server-side** first (keeps cookie/session model identical to Phase 1); redirects/signed URLs stay an optional later optimization **without** changing the path if desired.

**Explicitly not in this meta-plan:** pdf.js, drawer, range sync (Phase 3+ of the parent doc).

---

## Sub-phase ordering (fine-grained)

Each row is a **merge gate**: backend verify, relevant frontend/CLI tests, and **any** E2E spec touched in that sub-phase must be green before starting the next. **Deploy gate** per [.cursor/rules/planning.mdc](../.cursor/rules/planning.mdc): commit/push/CD when the team’s usual cadence requires it between sub-phases.

### SP-A1 — Download route exists; empty books get a clear 404

- **Outcome (integrator-visible):** `GET .../book/file` is registered, **read-authorized** like `GET .../book`, returns **404** when the notebook has no book or `source_file_ref` is blank.
- **Tests:** Extend [NotebookBooksControllerTest](backend/src/test/java/com/odde/doughnut/controllers/NotebookBooksControllerTest.java) (or equivalent) for 404 + auth; no storage bean required yet beyond what already exists.
- **Deliverable cleanliness:** No unused beans; if a placeholder method exists on a future storage type, **do not** add it until SP-A2.

### SP-A2 — `BookPdfStorage` (DB) + happy-path download

- **Outcome (integrator-visible):** First **production** use of storage: **`BookPdfStorage`** + DB-backed implementation (persist/load via `AttachmentBlob` pattern). Download handler resolves `source_file_ref` through **`get`** and returns **200** `application/pdf` with **Content-Disposition** (filename sanitization consistent with existing export endpoints).
- **Tests:** Controller-level test with **real** persistence (`makeMe` / test setup) so the **full** GET path is exercised; include **404** when ref is corrupt/unknown if the implementation can detect it cheaply.
- **Deliverable cleanliness:** Interface + **one** default `@Bean` implementation wired; **no** orphan implementations.

### SP-A3 — Multipart attach **alongside** existing JSON attach

- **Outcome (integrator-visible):** Second `POST .../attach-book` handler (or equivalent) with `consumes = MULTIPART_FORM_DATA`: parts **`metadata`** (`AttachBookRequest` JSON) + **`file`** (PDF). On success, **`BookPdfStorage.put`**, set **`source_file_ref`**, same outline persistence as today. **Existing JSON-only attach** remains **unchanged** so current CLI and tests keep working.
- **Tests:** New multipart cases in controller tests; **all** existing attach tests still green.
- **OpenAPI / codegen:** Springdoc documents multipart; run `pnpm generateTypeScript`; fix any compile breaks in packages that consume the spec (even if CLI does not call multipart yet).
- **Deliverable cleanliness:** Two consumes on the same resource is **interim**; tracked for removal in SP-A5.

### SP-A4 — CLI uses multipart attach

- **Outcome (user-visible for CLI):** `/attach` sends **FormData** (metadata + file) to the multipart endpoint; success message and outline behavior unchanged.
- **Tests:** [cli Vitest](cli/tests/) updates for spies/mocks; **existing** [book_reading.feature](e2e_test/features/book_reading/book_reading.feature) scenario still passes (still hits JSON? No — after this, CLI hits multipart; E2E uses real CLI → must pass end-to-end).
- **Deliverable cleanliness:** No client code path still sending **only** JSON for new attaches unless documented as deprecated internal test helper (prefer not — tests should use multipart too).

### SP-A5 — Remove JSON-only attach; single consumes

- **Outcome (integrator-visible):** **One** attach contract: **multipart only**. All controller tests and fixtures use multipart.
- **Tests:** Full backend + CLI + book_reading E2E green.
- **Deliverable cleanliness:** Delete dead JSON attach mapping and any helpers only used for it; **no** dual-path attach left.

### SP-B1 — API exposes “has file” + book reading download control

- **Outcome (user-visible in browser):** `Book` JSON (appropriate `JsonView`) includes something like **`hasSourceFile`** derived from **`source_file_ref`**. [BookReadingPage.vue](frontend/src/pages/BookReadingPage.vue) shows a **Download** control (stable **`data-testid`**) when true; link targets the same-origin GET download URL with session cookie.
- **Tests:** Frontend unit test if there is an established pattern for this page; otherwise minimal component assertion — **do not** skip E2E in SP-B2 solely because of this test.
- **Deliverable cleanliness:** No feature flags dead in code; unused generated SDK types cleaned up if any.

### SP-B2 — E2E: CLI attach + browser download

- **Outcome (user-visible E2E):** New Cucumber scenario: attach fixture PDF via CLI → open notebook → Read → **Download** → assert PDF (magic bytes or fixture comparison) via **intercept**, **`cy.request`** with session, or reliable downloads-folder pattern — pick one style and **reuse** patterns from [e2e_test](e2e_test) (e.g. obsidian/audio lessons).
- **Tests:** Single focused feature file/spec run in CI for this change set.
- **Deliverable cleanliness:** Page object methods colocated; step definitions stay thin.

### SP-C1 — GCS `BookPdfStorage` implementation (not default in dev)

- **Outcome (integrator-visible in configured env):** Second implementation of **`BookPdfStorage`** using GCP **Storage** client: **put** uploads object, **get** reads bytes (or streams). Activated only when **explicit** config/profile says so (e.g. bucket name + credentials). **Default** local/test profile remains DB implementation from SP-A2.
- **Tests:** **Black-box** tests against the GCS adapter with **mocked** `Storage` (or fakes) — assert correct bucket/blob id derivation from `source_file_ref` **without** coupling tests to private helpers; **no** real network in unit tests.
- **Deliverable cleanliness:** If the bean is `@ConditionalOnProperty`, document required properties; **no** unreachable GCS code paths.

### SP-C2 — Wire profile selection + integration proof

- **Outcome (operator-visible):** `application-*.yml` (or equivalent) documents **local = DB**, **prod = GCS**. One **integration-style** test: Spring context with **test** properties selecting GCS bean + mock `Storage` **or** Testcontainers/fake GCS **if** the repo already standardizes on one — **choose the smallest** approach that proves wiring (constructor injection, `BookService` uses interface only).
- **Tests:** That integration test + full **backend:verify** green.
- **Deliverable cleanliness:** No duplicate storage selection logic in multiple packages.

### SP-C3 — CI / E2E against GCS (or emulator) — same Gherkin intent

- **Outcome (release confidence):** Same **user journey** as SP-B2 runs in CI **with** GCS-backed storage: **dedicated test bucket**, **emulator**, or **mocked** backend boundary — **document the chosen approach** in this file’s “CI notes” subsection (update when implemented).
- **Tests:** Either tag an E2E variant (e.g. `@gcsBookStorage`) or a **single** pipeline job with env vars; **avoid** running full suite twice on every PR unless the team agrees.
- **Deliverable cleanliness:** Remove interim skips; document how developers run locally.

### SP-D1 — Hardening and parent-doc alignment

- **Outcome:** Limits (max upload size), consistent error bodies on attach failure, logging without secrets, **delete/orphan** policy **documented** for GCS objects when book row is removed (actual delete behavior may belong to a later story — here: **explicit** “not done” or “done” in one sentence).
- **Tests:** Add only **regression** tests that failed to exist before (e.g. oversize file → 4xx); keep **minimal**.
- **Deliverable cleanliness:** Update [book-reading-read-a-range-plan.md](book-reading-read-a-range-plan.md) Phase 1/2 bullets to point at this meta-plan and mark completed sub-phases; remove obsolete interim notes from **this** file if any.

---

## CI notes (fill in when SP-C3 is executed)

- **Chosen strategy:** _TBD — test bucket vs emulator vs contract tests only._
- **Required secrets / env:** _TBD._

---

## Sub-phase checklist (mechanical)

After **each** SP-A* / SP-B* / SP-C* / SP-D*:

1. **Tests:** `pnpm backend:verify` (or scoped), `pnpm cli:test` if CLI touched, `pnpm frontend:test` if frontend touched, Cypress **spec** for book reading if E2E touched.
2. **Dead code:** No unused storage impls, no deprecated attach paths left after SP-A5.
3. **Codegen:** If controllers/DTOs changed, `pnpm generateTypeScript` and fix dependents.
4. **Plan:** Tick sub-phase in this doc or in parent roadmap; drop text that no longer helps the **current** snapshot.

---

## If you must pause mid-stream

Prefer finishing through **SP-A5** before any deploy so attach contract is **singular**. Prefer finishing **SP-B2** before starting GCS so the **E2E** baseline exists for comparison on GCS.
