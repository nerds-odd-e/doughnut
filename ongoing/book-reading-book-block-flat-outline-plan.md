# Plan: Book layout — flat outline storage (depth + order)

**Status:** Draft — not executed.

**Precondition:** **No production book data to migrate.** Schema changes may **drop and recreate** book-related tables or columns without backfill. Local and CI fixtures stay under test control.

**Architecture:** [`ongoing/doughnut-book-reading-architecture-roadmap.md`](doughnut-book-reading-architecture-roadmap.md) — update that file when this plan’s **persisted shape** and **attach contract** are final; link here instead of duplicating long prose.

**UX:** [`ongoing/book-reading-ux-ui-roadmap.md`](book-reading-ux-ui-roadmap.md) — preorder successor and Reading Control Panel logic assume **depth-first preorder**; this plan must preserve that **observable** ordering.

**Planning discipline:** [`.cursor/rules/planning.mdc`](.cursor/rules/planning.mdc) — one primary **user- or integrator-visible** outcome per phase where possible; tests at **observable** surfaces (controller, mounted UI, E2E where already used for book reading); no dead code at phase merge.

---

## Canonical layout model (applies after Phase 2)

- **Total order:** Each structural block has a single **`layout_sequence`** (or equivalent) so `GET …/book` can emit blocks in **preorder** deterministically. Do **not** rely on implicit JPA list order without an explicit sort key.
- **Depth:** Each block has integer **`depth` ≥ 0** (root-level sections = 0 unless product standardizes differently — pick one convention and keep it stable through Phase 4).
- **Outline rule:** For blocks sorted by `layout_sequence`, **`depth[i] ≤ depth[i-1] + 1`**. Importer normalizes raw MinerU `text_level` (and gaps) into this rule; **raw** levels may still live on `BookContentBlock` / payload where needed.
- **Reading successor:** Product logic continues to treat the **next row in preorder** as the structural successor of the current block (direct content, auto-mark, panel gating). Changing this rule is out of scope unless a phase explicitly rewrites those scenarios.

---

## Phase 1 — Attach path: raw MinerU stream in; server builds layout

**Outcome:** A **real** attach flow (CLI and/or multipart **attach-book**) can send **MinerU-shaped blocks** (e.g. `content_list` items with `text_level` preserved **as extracted**), and the **backend** performs outline construction, synthetic **`*beginning*`** handling, and **`BookContentBlock`** persistence. Users still get a working book: **PDF + layout + reading** for the exercised scenario.

**Scope notes:**

- Decide the **attach contract** (multipart JSON part shape, size limits, validation errors). CLI may **stop** duplicating `layout_roots_from_heading_records` / `layout_roots_with_content_blocks` logic once the server owns it — remove dead Python/TS paths in the same phase or leave a single follow-up commit called out in this plan.
- **Phase-complete tests:** Prefer **controller-level** attach + **GET book** assertions (block count, titles, order, anchors) for at least one fixture with headings and orphans; extend **existing** book-reading E2E only if that is already the thinnest proof for the attach path you change.

**Deploy gate:** As in planning.mdc — merge and deploy before Phase 2 if your team uses that rule.

---

## Phase 2 — Persistence: flat blocks with `depth` + `layout_sequence`; wire-compatible `GET …/book`

**Outcome:** **BookBlock** (or replacement entity) is stored as a **flat** set per book: **`layout_sequence`**, **`depth`**, no **`parent_block_id`** (column removed or unused). **`GET /api/notebooks/{notebook}/book`** still returns the **same JSON shape the frontend uses today** — in particular **flat `blocks` array** with **`parentBlockId`** and **`siblingOrder`** (or equivalent fields the client already depends on) **derived** from `(layout_sequence, depth)` so **no frontend change** is required in this phase.

**Scope notes:**

- Implement **deterministic** derivation of `parentBlockId` / `siblingOrder` for serialization only (O(n) preorder walk is fine).
- **Reading records** and **`BookContentBlock`** FKs remain keyed by stable block **ids** as today.
- **Phase-complete tests:** Controller or repository-level tests that **round-trip**: build from normalized outline → persist → GET JSON matches expected parent links and order; include at least one case where raw MinerU **skipped a level** and normalized depth satisfies **`depth[i] ≤ depth[i-1] + 1`**.

**Deploy gate:** Merge and deploy before Phase 3 if required.

---

## Phase 3 — API exposes canonical fields (`depth`, order); optional transitional dual fields

**Outcome:** **`GET …/book`** (and OpenAPI) expose **`depth`** and a clear **ordering** contract (document that **`blocks` array order is preorder** and matches `layout_sequence`). Either:

- **Option A:** Add **`depth`** (and document sort key) while **keeping** derived **`parentBlockId`** for one release, or  
- **Option B:** Replace consumer-facing shape in one step if no external clients depend on `parentBlockId` besides this repo.

**`pnpm generateTypeScript`** after OpenAPI changes.

**Phase-complete tests:** Assert new fields in **controller** responses; **frontend** may still **ignore** `depth` until Phase 4.

**Deploy gate:** As usual.

---

## Phase 4 — Frontend consumes API layout directly

**Outcome:** Remove or shrink **client-side** reconstruction (e.g. building preorder + depth from `parentBlockId` + `siblingOrder`) when the API already delivers **preorder-ordered** blocks and **`depth`**. **User-visible behavior** unchanged: same highlights, successor rules, Reading Control Panel, and reading-record borders.

**Phase-complete tests:** **Mounted** `BookReadingContent` / layout tests updated to use the new wire shape; run **targeted** book-reading **E2E** spec(s) touched by the change (not necessarily the full suite).

**Cleanup:** Delete obsolete helpers and dead types; ensure **no** duplicate outline logic in CLI if Phase 1 fully moved processing server-side.

---

## After all phases

- Trim this plan’s **draft** sections; point **architecture roadmap** at the final attach contract and persisted fields.
- Remove **interim** dual-field serialization if Phase 3 used Option A.

---

## Explicit non-goals (unless a new plan adds them)

- Migrating legacy production book rows (**empty DB** assumption).
- Changing **direct content** definition or **reading-record** semantics without a separate plan.
- Supporting **user-edited** layout trees in this file’s phases.
