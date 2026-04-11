# Plan: Reading record

**User story:** [`ongoing/book-reading-user-stories.md`](book-reading-user-stories.md) — *Story: Reading record*.

**Architecture:** [`ongoing/doughnut-book-reading-architecture-roadmap.md`](doughnut-book-reading-architecture-roadmap.md).

**UX context:** [`ongoing/book-reading-ux-ui-roadmap.md`](book-reading-ux-ui-roadmap.md) and Story 2 reader work in [`ongoing/book-reading-read-a-block-plan.md`](book-reading-read-a-block-plan.md).

**Planning rule:** `.cursor/rules/planning.mdc` — one user-visible behavior per phase, scenario-first ordering, test-first workflow.

**This document is a delivery plan only** — update phases here before implementation, then trim obsolete detail after each shipped slice.

**Shipped baseline (do not regress):** Reading-record Phases **2–8** — same as **2–7**, plus **no timed auto-selection**: viewport **current** block does **not** become **selected** after idle; selection changes only via explicit actions (until Phase 9 default selection). Observable contract: [`e2e_test/features/book_reading/reading_record.feature`](../e2e_test/features/book_reading/reading_record.feature) and mounted reader tests (e.g. `BookReadingPage.spec.ts`). Deeper vocabulary: architecture + UX roadmaps above.

---

## Principles (unchanged)

- **Progress remains per block:** `ReadingRecord` attaches to **`BookBlock`**, not PDF coordinates.
- **Direct-content gating:** explicit disposition only when the block has **direct content** and no recorded disposition; auto-read for **no-direct-content** blocks stays.
- **Reading order:** depth-first preorder for predecessor/successor, **current block**, and direct-content boundaries.
- **Snap-back:** reminder only (two attempts per block); not a persisted lock.
- **Tests:** prefer mounted `BookReadingPage` / reader-flow tests and controller tests when HTTP changes; avoid tests that only pin private counters or watcher internals.

---

## Phase 1 — Remember book last read position

**User story scenario:** leave the book, return later, land on the same reading position.

**User outcome:** reopening the book restores the last reading position for the same user and book.

**Test shape:** no E2E required; controller tests for persistence and mounted reader/page tests for save + restore through observable contracts.

**Out of scope for this slice alone:** per-block disposition prompts, snap-back state, read/skim/skip styling.

**Note:** Phase **10** below extends this slice so **selected block** restores **together** with view position; implement Phase 10 in the same delivery stream as Phase 1 if Phase 1 is not yet shipped, or as a follow-up if position persistence already exists without selection.

---

## Phase 5a — Snap to block start when block fits on one page

**User story scenario:** the user scrolls past an unread block whose top and last direct-content bbox are on the same page — snap-back should show the full block from the beginning.

**User outcome:** when snap fires and the selected block's start anchor and last direct-content bbox are on the **same page**, scroll to the **block start** (same target as layout click); if on **different pages**, keep Phase 5 behavior (last content bbox bottom above the panel).

**Depends on:** shipped Phase 5.

**Implementation hints:** `lastBbox.pageIndex === startAnchor.pageIndex` from **`GET …/book`** `allBboxes`; reuse `scrollToPdfOutlineV1Target` / `applyBookBlockSelection` for the same-page case; no change to attempt counter or suppression window.

**Tests (no new E2E):** mounted same-page vs cross-page scroll target assertions.

---

## Phase 5b — Animated panel on snap

**User story scenario:** snap fires but the user did not notice the panel.

**User outcome:** when snap-back fires, **Reading Control Panel** plays a brief one-shot attention animation (transform/opacity/shadow; no layout shift).

**Depends on:** shipped Phase 5.

**Tests (no new E2E):** mounted — animation class or `data-*` appears on snap and is removed after `animationend` or timer.

---

## Phase 8 — No timed auto-selection of the viewport “current” block (shipped)

**Shipped:** removed dwell timer that promoted **current** → **selected**.

---

## Phase 9 — Always a selected book block; default to the first

**User story scenario:** open a book or land in a state where no block was explicitly chosen.

**User outcome:** there is **always** a **selected** block when the book has at least one block; if none is set, **select the first block in reading order** (same depth-first preorder as the rest of the reader).

**Depends on:** Phase 8 (no silent timer overriding this).

**Tests (no new E2E):** mounted/page — initial load and edge transitions assert a non-null selected id (first block when appropriate).

---

## Phase 10 — Persist and restore selected block with last view position

**User story scenario:** leave mid-chapter with a explicit **selected** block and scroll position; return later.

**User outcome:** restore **both** the **last view position** (scroll/zoom contract already used for “remember position”) **and** the **selected** `BookBlock` in one coherent session — no restore of position that implies a different block without updating selection (and vice versa). Align persistence with Phase **1** (single save/restore path or explicitly ordered restore).

**Depends on:** Phase 9 (stable “always selected” invariant).

**Tests (no new E2E):** controller + mounted tests proving round-trip for position + selected block id together.

---

## Phase 11 — Panel below last content bottom when it would float above the obstruction zone

**User story scenario:** the **mark as read** (disposition) panel is showing, and the **bottom** of the selected block’s **last direct-content** bbox sits **above** the default bottom-docked panel zone (i.e. shorter content than the usual “panel eats the bottom” layout).

**User outcome:** while that prompt is shown, place the panel **immediately below** that **last content bottom** (not fixed to the viewport bottom), and **update on scroll** so it **follows** that edge until the usual rules hide the panel or the geometry no longer applies — without breaking scroll-through on the PDF.

**Depends on:** shipped Phase 2+ panel behavior; coordinate with UX roadmap **Reading Control Panel** placement.

**Tests (no new E2E):** mounted geometry/position assertions where feasible.

---

## Phase 12 — Current ≠ selected: “read from here” and “back to selected”

**User story scenario:** the user scrolls so the viewport **current** block differs from the **selected** block (e.g. skimmed ahead while the panel still referred to an earlier section).

**User outcome:** a **bottom** affordance in the main pane shows **current** block context (title) with **Read from here** (makes **current** the **selected** block — product-precise: align selection and disposition target with the viewport block) and **Back to selected** (returns focus/scroll to the **selected** block’s reading position per existing navigation rules).

**Depends on:** Phases 8–9 (clear separation of current vs selected without timer; invariant selected block).

**Tests:** **add E2E** in [`reading_record.feature`](../e2e_test/features/book_reading/reading_record.feature) (or a tightly related feature) covering **current ≠ selected** and both buttons; keep the rest of the reading-record E2E suite green.

---

## Phase discipline

After each phase:

1. Add or update the focused high-level test first, and confirm the failure is for the right reason.
2. Implement the smallest change that makes that phase green while preserving existing `reading_record.feature` paths (except Phase 12, which **extends** E2E deliberately).
3. Remove temporary state or dead branches that were only useful during the phase.
4. Update this plan and, when behavior changes defaults, the architecture or UX roadmap in the same delivery stream.

---

## Document maintenance

Keep this file forward-looking. Shipped implementation detail lives in code, tests, and the architecture/UX roadmaps.
