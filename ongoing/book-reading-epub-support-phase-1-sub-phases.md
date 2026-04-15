# Phase 1 — sub-phases (EPUB upload & attach)

**Parent plan:** [book-reading-epub-support-plan.md](book-reading-epub-support-plan.md) (Phase 1 only).

**Intent:** Decompose Phase 1 into **small sub-phases** (target **≤ ~5 minutes** of focused work each) where **each sub-phase ends with one complete, green `git commit`** (no intentionally failing tests left on `main`).

**Rules (from `.cursor/rules/planning.mdc` + phased-planning skill):**

- **Observable behavior:** Each sub-phase states *pre / trigger / post* where it helps; prefer **user- or integrator-visible** outcomes (HTTP, persisted state, DOM).
- **E2E-led inside a sub-phase:** It is fine to **write a failing step locally first**, then implement until green, **but only push/commit once the slice is green** (one failing test at a time while driving).
- **Cohesion:** Keep EPUB-specific validation and branching next to the attach / file-serving seams described in the parent plan; avoid speculative abstractions.
- **Regression:** After refactors, **PDF attach and file serving** must stay correct; extend existing tests where they already cover the seam.
- **OpenAPI:** Regenerate TypeScript in the **same** sub-phase that changes the API surface (do not split “Java change” and “generated client” across commits if that leaves frontend inconsistent).

---

## Phase 1 recap (outcome to reach)

**Behavior:**

- *Pre:* Notebook exists, no attached book, user has a **supported** `.epub`.
- *Trigger:* User uploads `.epub` on the notebook page.
- *Post:* Notebook shows attached EPUB; reading page opens with **book name** and a **temporary main-pane placeholder**; structure drawer is **empty or title-only**; unsupported/invalid EPUB fails with a **clear user-visible error**. **PDF** books already on the notebook (attached via **CLI + MinerU**) still **read** in the browser; the **browser does not upload PDFs**.

**Proof:** Backend controller coverage for attach + file serving + errors; Cypress scenario for happy path + error path.

---

## Sub-phases (ordered)

Each row is **one commit**. Adjust naming to match actual packages/controllers when implementing.

### 1 — Fixtures only

- **Post:** Repo contains **one small supported** `.epub` (TOC or headings, short chapters, paragraphs, ≥1 image) and **one unsupported** `.epub` (DRM signal or invalid container) under the agreed fixtures tree used by backend / E2E.
- **Commit:** Binary fixtures + any **minimal** fixture index only if the repo already uses that pattern for other media.
- **Verify:** No production behavior change; optional quick checksum that files are non-empty.

### 2 — Backend: attach request surface accepts `format: "epub"` (schema + validation wiring)

- **Post:** Attach API / DTO accepts `epub` as a **declared** format; **validation** still rejects it with a **clear** error until the orchestration slice lands **or** (if you prefer) this commit only extends the OpenAPI/schema with implementation following in sub-phase 3 — choose **one** story: either “schema widened, server still returns 400 with explicit message for epub until wired” **or** “schema + minimal accept path in one commit.” Prefer **green**: widen contract and keep PDF-only behavior unchanged for PDF requests.
- **Commit:** Controller/DTO/OpenAPI + **`pnpm generateTypeScript`** if the attach contract is generated.
- **Verify:** `pnpm backend:verify` (or the narrowest Gradle test subset if the repo standard allows).

### 3 — Backend: format-dispatched attach orchestration (replace PDF-only entry)

- **Post:** `attachBookWithPdf`-style flow becomes **format-dispatched**; **PDF path identical** in behavior; **EPUB path** stores raw bytes and `format=epub` the same way persistence expects (no layout/contentList from client for EPUB).
- **Commit:** Rename/refactor + dispatch; keep call sites cohesive.
- **Verify:** Existing PDF attach tests still pass; add **one** focused controller (or service-through-controller) test: **attach fixture EPUB → persisted format + file record** (may still skip media-type serving until sub-phase 5 if you split — then assert storage only here).

### 4 — Backend: EPUB “fail fast” pre-checks

- **Post:** Unsupported EPUBs (**DRM / encryption.xml**, broken `container.xml` / package) return **stable, user-visible** error text / status consistent with project error style; supported fixture passes the gate.
- **Commit:** EPUB validator module colocated with attach or book services; wired only from EPUB branch.
- **Verify:** Controller test uses **unsupported fixture** → expected HTTP + message; supported fixture → still attaches.

### 5 — Backend: `GET …/book/file` for EPUB

- **Post:** Raw `.epub` bytes served with **`application/epub+zip`** and **Content-Disposition filename** behavior aligned with PDF naming rules.
- **Commit:** Format branch on file serving; shared “load bytes from storage” unchanged.
- **Verify:** Controller test asserts **media type + filename** for EPUB fixture.

### 6 — Backend: book deletion / storage cleanup parity

- **Post:** Deleting a notebook’s attached EPUB uses the **same** deletion path as PDF (no orphaned object); if already covered by generic tests, extend with **one** EPUB case only where it adds observable proof.
- **Commit:** Only if Phase 1 exposes a real gap; otherwise fold into sub-phase 3 **or** skip with note in commit message referencing existing coverage.
- **Verify:** Targeted backend test.

### 7 — Frontend: generated client sync (if not already done)

- **Post:** Frontend imports match backend attach + book-file endpoints after prior sub-phases.
- **Commit:** Only if any API change landed without regen; otherwise **skip** this sub-phase.
- **Verify:** `pnpm frontend:test` smoke or `pnpm lint:all` per team habit.

### 8 — Frontend: attach control on notebook page (multipart, **`.epub` only**)

- **Post:** User can pick a **`.epub`**; request hits **`attach-book`** with **`metadata` + `file`**, `format: "epub"`, and **no** `layout` / `contentList`. **PDF** is **not** offered in the file picker: PDF attach remains **CLI-only** (MinerU / outline pipeline). Copy may tell users to use the CLI for PDF.
- **Commit:** UI + wiring with `apiCallWithLoading` (or existing helper).
- **Verify:** Vitest at **HTTP boundary** (mocked SDK): EPUB happy path; **no** `attachBook` when a non-EPUB file is forced through the handler (e.g. `.pdf`) if tested.

### 9 — Frontend: user-visible copy is format-aware

- **Post:** Toasts / labels / errors do not imply “PDF only”; EPUB errors surface **server message** (or mapped) clearly.
- **Commit:** i18n / template text only if copy changed in sub-phase 8.
- **Verify:** Snapshot or text assertion tests only if already used nearby; keep minimal.

### 10 — Frontend: reading page safe for `book.format === "epub"`

- **Post:** Opening reading view for EPUB shows **title**, **placeholder pane**, drawer **empty or title-only**; **no crash**; PDF unchanged.
- **Commit:** `BookReading*` / shell branch on `book.format`.
- **Verify:** Focused Vitest (mounted page) or extend an existing reader-shell test file.

### 11 — E2E: happy path (upload supported EPUB → attached → reading shows name)

- **Post:** Cypress + steps **only for** upload → visible attached state → navigate to reading → **see book name** (and placeholder if easy to assert without flakiness).
- **Commit:** Feature + step defs + page objects; uses **supported fixture**.
- **Verify:** `pnpm cypress run --spec …` for the single feature.

### 12 — E2E: unsupported EPUB error

- **Post:** Upload **unsupported fixture** → **same** attach entry point → user sees **clear error** (assert stable substring from backend contract).
- **Commit:** Extend same feature file or sibling feature per `e2e_test.mdc` conventions.
- **Verify:** Single-feature Cypress run.

### 13 — Polish pass (only if needed)

- **Post:** Remove dead branches, align naming with domain (`attachBook` vs format), ensure **no PDF regression** in CI slices you run locally.
- **Commit:** Small refactor **without** behavior change; or **omit** sub-phase if nothing surfaced.
- **Verify:** `pnpm backend:verify` + targeted `pnpm frontend:test` + the two Cypress specs from 11–12.

---

## Optional micro-splits (if a single sub-phase risks >5 minutes)

Use these **only** when the parent sub-phase feels too large; **each still ends green**.

- Split **sub-phase 3** into **3a** persistence/write path and **3b** controller/orchestration glue.
- Split **sub-phase 4** into **4a** DRM detection only and **4b** container/OPF shape validation.
- Split **sub-phase 8** into **8a** hidden file input + upload wiring and **8b** visible button / layout placement.

---

## Explicitly out of scope for Phase 1 (do not sneak into these commits)

- Chapter tree, `BookBlock` extraction, Readium, reading-position / reading-record behavior beyond “page loads safely.”
- **Browser PDF upload** (MinerU belongs in the existing **CLI** PDF attach path).
- **CLI EPUB** attach (roadmap **Phase 9** in the parent plan — separate from today’s **CLI PDF** attach).

---

## Suggested commit message pattern

`feat(book): phase1 epub attach — <sub-phase title>`

Use `test(book): …` / `chore(fixtures): …` when the commit is tests or binaries only.
