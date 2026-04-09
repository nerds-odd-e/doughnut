# Plan: Reading record Phase 4 — skimmed and skipped disposition

**Status:** Not started — **plan only** (no implementation in this document).

**Parent:** [`ongoing/book-reading-reading-record-plan.md`](book-reading-reading-record-plan.md) — *Phase 4 — Mark a book block as skimmed or skipped*.

**Architecture:** [`ongoing/doughnut-book-reading-architecture-roadmap.md`](doughnut-book-reading-architecture-roadmap.md) — `DirectContentReadingState`, `ReadingRecord.status`.

**UX:** [`ongoing/book-reading-ux-ui-roadmap.md`](book-reading-ux-ui-roadmap.md) — Reading Control Panel expanded actions; Phase 4 adds skim/skip (read + auto-read already shipped).

**Discipline:** [`.cursor/rules/planning.mdc`](.cursor/rules/planning.mdc) — one intended failure at a time, smallest green, no dead code after each sub-phase. The parent phase explicitly uses **no Cypress E2E**; observable surfaces here are **Spring controller tests** and **mounted Vitest** (black-box HTTP / DOM), matching the parent plan’s testing table.

**After Phase 4 completes:** Fold status into [`book-reading-reading-record-plan.md`](book-reading-reading-record-plan.md); trim this file or mark it archived like Phase 3’s plan.

---

## Current baseline (Phase 2–3 shipped)

- **`PUT /api/notebooks/{notebook}/book/blocks/{bookBlock}/reading-record`** has **no body**; server always writes **`READ`** (`BookService.markBlockRead`).
- **`GET …/book/reading-records`** returns `bookBlockId`, `status`, `completedAt` (`BookBlockReadingRecordListItem`).
- Frontend: `useNotebookBookReadingRecords.submitMarkRead` → PUT without body; `readBlockIdsFromRecords` treats **`READ`** as “direct content read” for layout border (`data-direct-content-read`).
- Phase 3 auto-mark calls the same **`submitMarkRead`** path for predecessors with no direct content.

---

## Target end state (Phase 4)

- User can mark the **selected** block’s direct content as **skimmed** or **skipped** from the **Reading Control Panel** (same successor/predecessor gating as “mark read” — panel visible when viewport **current block** is the **immediate successor** of the **selected** block in preorder walk).
- **`status`** on the wire supports at least **`READ`**, **`SKIMMED`**, **`SKIPPED`** (align string values with `BookBlockReadingRecord` constants and OpenAPI enum).
- **Book layout** visually distinguishes **read** vs **skimmed** vs **skipped** at least as much as product needs (icons, borders, or labels — decide during implementation; tests should lock **observable** attributes/text).
- **Invalid `status`** → **400** with a **stable** public error body or message (controller test asserts contract).
- **DB:** forward-only migration if the column needs widening or check constraint; follow [`.cursor/rules/db-migration.mdc`](.cursor/rules/db-migration.mdc).
- **`pnpm generateTypeScript`** after OpenAPI changes.

---

## Auto-mark (Phase 3) interaction

**Default for Phase 4 unless product revises in implementation:** When the predecessor has **no direct content**, the auto-mark path continues to write **`READ`** only (same semantics as today). Skim/skip are **explicit** dispositions from the panel. If that rule changes, add a **dedicated** sub-phase with a failing test that describes the new rule (mounted or controller), not an implicit behavior change.

---

## Optional (out of core Phase 4 unless pulled in)

- **Persist Reading Control Panel** expanded/minimized preference ([`book-reading-ux-ui-roadmap.md`](book-reading-ux-ui-roadmap.md)) — separate sub-phase or later phase; do not leave unused flags in the codebase.
- **Minimized panel** quick actions for skim/skip — only if UX chooses it; each added control needs a mounted test and must not block PDF scroll hit targets.

---

## Sub-phases (maximal split)

Each sub-phase ends with **all tests green**, **no unused API/UI/constants**, and **no commented-out scenarios** left behind. Prefer **one new failing test** (or one extended assertion) before the minimal implementation that turns it green.

### 4A — Request body contract; `READ` unchanged

**User-visible / observable:** Callers can supply an explicit **read** disposition in the JSON body; behavior matches today’s **`READ`** write and list shape.

- **Red:** Controller test (`NotebookBooksControllerTest` or equivalent): `PUT` with body `{"status":"READ"}` → **200**, list item `status` **`READ`**, persistence unchanged from current `markBlockRead` behavior. Optionally assert omitting the body still works (**backward compatible**) if the product keeps that contract; if the contract becomes “body required,” one test documents that instead.
- **Green:** Introduce request DTO + OpenAPI request body on `putNotebookBookBlockReadingRecord`; `BookService` method replaces or generalizes `markBlockRead` for **`READ`** only; regenerate SDK; **no** SKIMMED/SKIPPED in the whitelist yet (tests must not reference them).
- **Cleanup:** Remove any duplicate “mark read” logic left unreachable; update OpenAPI summary text if it still says “mark as read” only.

### 4B — Persist **`SKIMMED`**

- **Red:** Controller test: `PUT` with `{"status":"SKIMMED"}` → **200**, `GET …/reading-records` (or returned list) shows **`SKIMMED`**, DB row matches.
- **Green:** Add constant `STATUS_SKIMMED`, whitelist in service validation, map in `BookBlockReadingRecordListItem` as today.
- **Cleanup:** No frontend or layout hooks until **4H+** unless this sub-phase would leave “API with no client” — the **controller test + OpenAPI enum** is the justified consumer; do not add SKIMMED to OpenAPI **example**-only without the test.

### 4C — Persist **`SKIPPED`**

- **Red:** Same pattern as **4B** for **`SKIPPED`**.
- **Green:** Constant + whitelist + test.
- **Cleanup:** Confirm migration/length still fits longest status string.

### 4D — Reject unknown status

- **Red:** Controller test: `PUT` with `{"status":"NOT_A_STATUS"}` → **400**, assert stable error message or `ApiError` field contract used elsewhere for invalid input.
- **Green:** Validation in controller or service; consistent with project patterns (`@Valid`, manual check, or shared validator).
- **Cleanup:** No extra error codes unused by tests.

### 4E — Status transitions (overwrite semantics)

- **Red:** Controller test: existing row **`SKIMMED`**, then `PUT` **`READ`** (or **`SKIPPED`**) for same block → row reflects latest status and updated `completedAt` behavior (define expected behavior once: always bump timestamp vs preserve — pick one and test it).
- **Green:** Minimal change so test passes (likely already true if upsert overwrites; test documents the contract).
- **Cleanup:** If behavior was already guaranteed, keep the test as regression only; remove duplicate tests that assert the same overwrite path.

### 4F — Frontend API wrapper: disposition parameter

- **Red:** Vitest (composable or thin wrapper test): calling the submit helper with **`SKIMMED`** issues `putNotebookBookBlockReadingRecord` including **body** `status: "SKIMMED"` (mock SDK).
- **Green:** Rename or extend `submitMarkRead` → e.g. `submitReadingDisposition(bookBlockId, status)`; **`markSelectedBlockAsRead`** and Phase 3 auto-mark call it with **`READ`**; generated types from **4A–4C** already include body.
- **Cleanup:** No dead exports; remove `submitMarkRead` if fully replaced.

### 4G — Derive layout state from all three statuses

- **Red:** Unit test on `readBlockIdsFromRecords` successor (new module or renamed helper): given list items with **`SKIMMED`** / **`SKIPPED`**, expose a **single** structure the tree can use (e.g. `Map<bookBlockId, status>` or three sets — **one** representation to avoid duplicate derivations).
- **Green:** Implement; keep **`READ`** semantics used by Phase 3 auto-mark and “read border” explicit (either `isDirectContentRead` narrows to `READ` only, or rename for clarity).
- **Cleanup:** Delete old helper if renamed; update all imports; ensure auto-mark still treats only **`READ`** as “already read” for suppression if that is the rule.

### 4H — Book layout: observable styling for **`SKIMMED`**

- **Red:** Mounted `BookReadingPage.spec.ts`: when records include **`SKIMMED`** for a visible block, DOM exposes an agreed **user-visible** hook (prefer label text or `aria-*`; use `data-*` only if the project already relies on it for the same panel — today read state uses `data-direct-content-read`).
- **Green:** Template + CSS for skimmed state; wire from **4G** output.
- **Cleanup:** No unused CSS classes.

### 4I — Book layout: observable styling for **`SKIPPED`**

- **Red / Green / Cleanup:** Same as **4H** for **`SKIPPED`**, distinct from read and skimmed.

### 4J — Reading Control Panel: **Skimmed** action

- **Red:** Mounted test: when panel is shown, user activates **Mark as skimmed** (or equivalent copy) → **`submitReadingDisposition(…, SKIMMED)`** (mock); layout updates per **4H**.
- **Green:** `ReadingControlPanel.vue` button + emit; `BookReadingPage.vue` handler; accessible name matches visible text.
- **Cleanup:** Remove any placeholder button without handler.

### 4K — Reading Control Panel: **Skipped** action

- **Red / Green / Cleanup:** Same as **4J** for **`SKIPPED`**, wired to **4I**.

### 4L — Panel layout and copy (expanded state)

- **Red:** Mounted test: panel lists **selected block** context and **three** dispositions without breaking small viewports (snapshot or assert all three actions exist and are visible when panel is expanded — match how Phase 2 tests assert “mark read”).
- **Green:** Order buttons (primary **read**, secondary skim/skip or overflow — match UX roadmap); ensure touch targets remain acceptable.
- **Cleanup:** No duplicate strings scattered; reuse copy constants if the file already uses that pattern.

### 4M — Phase 3 auto-mark regression

- **Red:** Mounted test: simulate no-direct-content predecessor auto-mark → mocked PUT must send **`READ`** only (not SKIMMED/SKIPPED); optional: predecessor already **`SKIMMED`** does not get silently overwritten by auto-mark unless product decides otherwise (then assert that rule explicitly).
- **Green:** Adjust `BookReadingPage.vue` guard conditions if needed.
- **Cleanup:** No duplicate auto-mark paths.

### 4N — Controller / OpenAPI parity

- **Red:** None if already covered — otherwise one test for **403/404** paths with **new body present** (same as today’s put errors).
- **Green:** Only if gaps found.
- **Cleanup:** OpenAPI `operationId` and descriptions mention disposition, not only “read.”

### 4O — User story and E2E placeholder (documentation only)

- **Green:** Complete Gherkin for *mark a book block as skimmed/skipped* in [`ongoing/book-reading-user-stories.md`](book-reading-user-stories.md) aligned with implemented copy and DOM hooks. **Do not** add Cypress scenarios in Phase 4 unless the parent plan is amended — this sub-phase is **story text + links** only.
- **Cleanup:** Remove “TBD” stubs if any.

### 4P — Plan fold-back

- **Green:** Mark Phase 4 done in [`book-reading-reading-record-plan.md`](book-reading-reading-record-plan.md); update [`book-reading-ux-ui-roadmap.md`](book-reading-ux-ui-roadmap.md) Phase 4 line if behavior differs from the one-line summary; update architecture roadmap only if a **default** changed.
- **Cleanup:** Archive or trim this document per team habit.

---

## Execution notes

- **Order:** Keep **4A → 4E** on the backend before **4F** so the generated client includes the body type; **4G** before **4H–4I** so layout work has one data source; **4J–4L** after **4F** and **4H–4I** so actions immediately reflect in the tree.
- **If a sub-phase proves too thin:** Merge **4B+4C**, **4H+4I**, or **4J+4K** in one PR while keeping a **single** red→green story inside the merge (still no dead code).
- **Deploy gate:** Per parent checklist, prefer deploy between phases when the team requires it; otherwise batch adjacent backend-only slices (**4A–4E**) if CD cost is high — still document each logical sub-phase completion in commits or PR description.
