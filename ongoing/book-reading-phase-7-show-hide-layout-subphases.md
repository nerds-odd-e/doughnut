# Phase 7 — Show / hide the layout drawer: sub-phases

**Parent intent:** [`ongoing/book-reading-read-a-range-plan.md`](book-reading-read-a-range-plan.md) — Phase 7. **UX/UI:** [`ongoing/book-reading-ux-ui-roadmap-phases-4-7.md`](book-reading-ux-ui-roadmap-phases-4-7.md) — *Phase 7*. **Architecture:** [`ongoing/doughnut-book-reading-architecture-roadmap.md`](doughnut-book-reading-architecture-roadmap.md) — drawer + PDF main pane.

**Planning rules:** `.cursor/rules/planning.mdc` — one **user-visible** slice per sub-phase, order by **value** (common cases first), phase-complete cleanup (**no dead code**).

**Testing (explicit exception for this track):** **No Cypress / E2E** for Phase 7. Cover behavior with **Vitest** only, prefer **mounted `BookReadingPage`** (same entry point as [`frontend/tests/pages/BookReadingPage.spec.ts`](../frontend/tests/pages/BookReadingPage.spec.ts)) so assertions stay **observable** (DOM, `data-testid`, ARIA) rather than pinning private helpers. Each sub-phase finishes with **tests green** and **no unused production code** introduced for that slice.

**Baseline:** [`frontend/src/pages/BookReadingPage.vue`](../frontend/src/pages/BookReadingPage.vue) already wires `outlineOpened`, a single **Outline** control (`data-testid="book-reading-outline-toggle"`), large-viewport **collapse** (`daisy-hidden` on the aside), small-viewport **overlay + backdrop**, and **open-by-default on first mount** when `innerWidth >= 768`. Sub-phases below assume we **lock that contract in tests** and add any small gaps (mostly **a11y** and **resize** edge cases).

**Execution order:** Implement in numeric order unless a later sub-phase discovers a bug; then fix forward and update this doc.

**Codebase sync (production vs this plan):** In `BookReadingPage.vue`, **7.1–7.6**, **7.8**, **7.9** (selection is `selectedOutlineRangeId` only; toggling the outline does not clear it), **7.10–7.11** (`resize` → `windowWidth` / `isMdOrLarger`, backdrop `v-if` is small-only) are **already implemented**. **7.7** is **not**: the toggle has no `aria-expanded` (or `aria-controls` / aside `id`). [`frontend/tests/pages/BookReadingPage.spec.ts`](../frontend/tests/pages/BookReadingPage.spec.ts) has **no** Phase 7 outline-toggle tests yet — per phase discipline below, treat **Vitest** as the gate for marking a sub-phase **done**; only then shrink this sync block.

---

## Phase 7.1 — Large viewport: toggle hides the outline column

**User outcome:** On a **wide** layout, the user can **hide** the outline so the PDF uses more horizontal space.

**Test (Vitest):** Mount with `window.innerWidth` (or equivalent stub) **≥ 768**, load book + PDF as in existing specs, assert `book-reading-outline-aside` is **not** laid out as visible (e.g. `daisy-hidden` / not displayed). Click `book-reading-outline-toggle`, assert aside is hidden.

**Production:** Matches (large + closed → aside `daisy-hidden`).

**Cleanup:** No stray flags or duplicate toggle handlers.

---

## Phase 7.2 — Large viewport: toggle shows the outline again

**User outcome:** After hiding, **one more** use of the same control **restores** the outline column (push sidebar, not a modal).

**Test:** From 7.1’s hidden state, click toggle again; assert aside is visible and outline list still present.

**Production:** Matches (same toggle flips `outlineOpened`).

**Cleanup:** Do not introduce a second “open outline” code path; keep a **single** source of truth for `outlineOpened`.

---

## Phase 7.3 — Small viewport: toggle opens overlay outline + backdrop

**User outcome:** On a **narrow** layout, with outline **closed**, the user opens it and sees the **sheet over** the PDF with a **dimmed backdrop** (when open).

**Test:** `innerWidth < 768`, initial closed (matches mount default on small). Click toggle; assert aside is on-screen and a **backdrop** exists (e.g. fixed full-screen layer behind the sheet — match current template structure).

**Production:** Matches (`v-if="!isMdOrLarger && outlineOpened"` backdrop; aside `daisy-translate-x-0` when open).

**Cleanup:** Backdrop must remain **small-only**; no unnecessary modal layer on large.

---

## Phase 7.4 — Small viewport: toggle closes overlay

**User outcome:** Closing returns to **full-width PDF** reading (no outline stealing width).

**Test:** From open state in 7.3, click toggle; assert backdrop gone and outline not obstructing (e.g. translated off / not visible).

**Production:** Matches (toggle sets `outlineOpened` false; aside `-daisy-translate-x-full`).

---

## Phase 7.5 — Small viewport: backdrop tap closes outline

**User outcome:** Tapping **outside** the outline (on the backdrop) **closes** the drawer, consistent with common mobile drawer patterns.

**Test:** Open outline on small, trigger backdrop click (same handler as production), assert `outlineOpened` false and backdrop absent.

**Production:** Matches (backdrop `@click="outlineOpened = false"`).

**Cleanup:** If backdrop is removed or refactored, **delete** obsolete tests; do not leave dead selectors.

---

## Phase 7.6 — Toggle reflects open/closed state for visual affordance

**User outcome:** The control **reads as** “outline open” vs “outline closed” (icon or styling already in the bar).

**Test:** Assert a **stable** observable: e.g. `sidebar-expanded` class on the toggle when open, or the **open** vs **closed** icon branch in the template — one clear assertion pair for open/closed on **one** viewport (large is enough if markup is shared).

**Production:** Matches (`sidebar-expanded` when `outlineOpened`; hamburger vs back-chevron SVG).

**Cleanup:** Do not add redundant duplicate classes; extend what exists.

---

## Phase 7.7 — Toggle exposes `aria-expanded` (and optional `aria-controls`)

**User outcome:** Screen reader users hear whether the outline is **expanded** or **collapsed**.

**Test:** `aria-expanded="true"` when open, `"false"` when closed; if `aria-controls` is added, point to a stable `id` on the aside.

**Production:** **Not done** — toggle only has `aria-label` / `title` today; add `aria-expanded` bound to `outlineOpened`, and optional `aria-controls` + aside `id`.

**Cleanup:** Remove any interim copy that duplicates `aria-label` without value.

---

## Phase 7.8 — PDF main stays mounted when toggling outline (large)

**User outcome:** Hiding/showing the outline **does not** reset reading position by **remounting** the PDF viewer for that session.

**Test:** On large, after PDF is mounted, record something stable (`PdfBookViewer` wrapper exists once; optional: stub scroll state / emit if you already have a hook). Toggle outline **closed → open**; assert **still a single** viewer instance (Vue Test Utils `findAllComponents(PdfBookViewer).length === 1` or equivalent). Avoid coupling to pdf.js internals.

**Production:** Matches (`PdfBookViewer` is `v-else-if="bookPdfBytes"` in `main`, not gated on `outlineOpened`).

**Cleanup:** No `v-if` on `PdfBookViewer` introduced for toggle; if refactor happens, tests must match the **intended** “no remount” contract.

---

## Phase 7.9 — Outline **selection** survives hide/show (large)

**User outcome:** After the user **selected** a range (Phase 5), collapsing the outline **does not clear** that selection when they reopen (session continuity).

**Test:** Large viewport, load outline, click a row (or set `selectedOutlineRangeId` only if no cheaper path — prefer **real click**), toggle closed, toggle open; assert same row still has **selected** marker (`data-outline-selected="true"` or active class per current implementation).

**Production:** Matches (selection is only set on row click; outline toggle does not reset `selectedOutlineRangeId`).

**Cleanup:** Remove any temporary test-only exports from production.

---

## Phase 7.10 — Resize: small → large with outline closed stays consistent

**User outcome:** No broken layout or **stuck** overlay when the window **crosses** the breakpoint.

**Test:** Start `innerWidth < 768`, outline **closed**, toggle if needed. Bump `innerWidth` to **≥ 768** and dispatch `resize`. Assert: **no** mobile backdrop, aside follows **large** rules (hidden if `outlineOpened` still false), main PDF still present.

**Note:** Per UX doc, **default open on large** applies to **initial mount**, not necessarily to every resize; **preserving** `outlineOpened` across resize is acceptable unless product decides otherwise — this sub-phase **documents** the chosen rule in code + test.

**Production:** Matches (`resize` updates `windowWidth`; backdrop is small-only; aside class branch follows `isMdOrLarger`).

**Cleanup:** If a `watch(isMdOrLarger, …)` is added, it must have **observable** effect covered here; remove unused watchers.

---

## Phase 7.11 — Resize: large → small with outline open

**User outcome:** With outline **open** on desktop, narrowing the viewport moves to **mobile** presentation without a broken double-layer or invisible trap.

**Test:** Start large, outline open, `innerWidth` to `< 768`, `resize`. Assert backdrop behavior matches **small-open** state and PDF remains usable (viewer still mounted).

**Production:** Matches (same reactive layout; open + small → backdrop + fixed aside).

**Cleanup:** Same as 7.10 — no dead resize branches.

---

## Phase discipline (per sub-phase)

1. Prefer **one** new failing test (or one focused `it` block) for the slice, then minimal production change. A sub-phase is **done** only when that Vitest coverage exists and is green — not when **Production: Matches** alone (see **Codebase sync**).
2. **Remove** dead code, duplicate handlers, and obsolete tests before marking the sub-phase done.
3. **No Cypress** for Phase 7; run **`pnpm frontend:test`** (or targeted spec path) under the usual nix wrapper.
4. When all sub-phases are done, update [`ongoing/book-reading-read-a-range-plan.md`](book-reading-read-a-range-plan.md) Phase 7 to point here and adjust the **E2E** line to match shipped policy (Vitest-only for this phase).

---

## Optional merge points (if the team wants fewer commits)

- **7.1 + 7.2** — single “large toggle round-trip” test file section.
- **7.3 + 7.4 + 7.5** — single `describe("small viewport outline toggle")`.
- **7.10 + 7.11** — one `describe("breakpoint crossing")` if resize setup is shared.

Keep **7.7** (a11y) separate unless merged with **7.6** deliberately.
