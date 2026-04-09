# Plan: Reading record Phase 4 — skimmed and skipped disposition

**Status:** **Shipped** — implementation matches the checklist below; keep this file as a short reference or trim per team habit.

**Parent:** [`ongoing/book-reading-reading-record-plan.md`](book-reading-reading-record-plan.md) — *Phase 4 — Mark a book block as skimmed or skipped*.

**Architecture:** [`ongoing/doughnut-book-reading-architecture-roadmap.md`](doughnut-book-reading-architecture-roadmap.md) — `DirectContentReadingState`, `ReadingRecord.status`.

**UX:** [`ongoing/book-reading-ux-ui-roadmap.md`](book-reading-ux-ui-roadmap.md) — Reading Control Panel expanded actions; Phase 4 adds skim/skip (read + auto-read already shipped).

**Delivery shape:** Phase 4 ships as **one slice** — backend contract, persistence, generated client, layout derivation, panel actions, and regressions land together. Observable tests stay **Spring controller** + **mounted Vitest** (no Cypress unless the parent plan is amended). Follow [`.cursor/rules/planning.mdc`](.cursor/rules/planning.mdc) for black-box assertions and no dead code at merge; you do **not** need to drive this through sixteen micro red/green steps.

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
- **Book layout** visually distinguishes **read** vs **skimmed** vs **skipped** at least as much as product needs (icons, borders, or labels — decide during implementation; tests lock **observable** attributes/text).
- **Invalid `status`** → **400** with a **stable** public error body or message (controller test asserts contract).
- **DB:** forward-only migration if the column needs widening or check constraint; follow [`.cursor/rules/db-migration.mdc`](.cursor/rules/db-migration.mdc).
- **`pnpm generateTypeScript`** after OpenAPI changes.

---

## Auto-mark (Phase 3) interaction

**Default unless product revises in implementation:** When the predecessor has **no direct content**, the auto-mark path continues to write **`READ`** only. Skim/skip are **explicit** dispositions from the panel. If that rule changes, add a **failing test** (mounted or controller) that documents the new rule before changing behavior.

---

## Optional (defer past Phase 4)

- **Persist Reading Control Panel** expanded/minimized preference ([`book-reading-ux-ui-roadmap.md`](book-reading-ux-ui-roadmap.md)).
- **Minimized panel** quick actions for skim/skip — only if UX chooses it; each added control needs a mounted test and must not block PDF scroll hit targets.

Do not ship unused flags or dead UI for these.

---

## Single-slice implementation checklist

Implement and test in one cohesive change set (order within the branch is flexible; keep **OpenAPI + backend** ahead of **generated client** usage).

### Backend

- Request body on **`PUT …/reading-record`**: e.g. `{"status":"READ"|"SKIMMED"|"SKIPPED"}`; **backward compatible** if the product keeps **no body** meaning **`READ`** (document the chosen rule in controller tests).
- Whitelist statuses in service layer; **`NOT_A_STATUS`** → **400** with stable error contract.
- Persistence for all three statuses; **overwrite** semantics for the same `(user, book_block)` row documented in a controller test (timestamp behavior defined once).
- **`GET …/reading-records`** (and **`PUT`** response list) round-trip all statuses.
- **403/404** paths with body present match existing put behavior.
- OpenAPI request body + enum; descriptions mention **disposition**, not only “read.”

### Frontend

- Replace or extend **`submitMarkRead`** → e.g. **`submitReadingDisposition(bookBlockId, status)`**; Phase 3 auto-mark and “mark read” call it with **`READ`**.
- **One** derived structure from records for the tree (e.g. map or sets) so **read / skimmed / skipped** styling is not duplicated; clarify whether “already read” guards for auto-mark mean **`READ`** only (default) or any disposition.
- **Reading Control Panel:** expanded state shows context + **three** dispositions (order and primary/secondary per UX roadmap); accessible names; **Skimmed** / **Skipped** actions call the SDK with the right body.
- **Book layout:** observable hooks for **SKIMMED** and **SKIPPED** (prefer user-visible text or `aria-*`; align with how **`data-direct-content-read`** works today).
- **Regression:** mounted test that no-direct-content auto-mark still issues **`READ`** only (and does not silently overwrite **`SKIMMED`/`SKIPPED`** unless product explicitly changes that rule and tests it).

### Documentation

- Complete Gherkin for *mark a book block as skimmed/skipped* in [`ongoing/book-reading-user-stories.md`](book-reading-user-stories.md) aligned with shipped copy and DOM hooks. **Do not** add Cypress scenarios in Phase 4 unless the parent plan is amended.

### Plan fold-back

- Mark Phase 4 done in [`book-reading-reading-record-plan.md`](book-reading-reading-record-plan.md); refresh [`book-reading-ux-ui-roadmap.md`](book-reading-ux-ui-roadmap.md) Phase 4 line if behavior differs from the one-line summary; update the architecture roadmap only if a **default** changed.

---

## Execution notes

- **Deploy gate:** Per parent checklist — commit/push and let CD deploy before starting unrelated follow-up unless the team agrees otherwise.
- **No partial API:** Avoid merging **PUT** that accepts **`SKIMMED`** without the client and layout using it in the same slice (or the next immediate PR), so the codebase does not carry orphan endpoints.
