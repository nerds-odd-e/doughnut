# Book reading — UX/UI roadmap (Phases 4–7)

**Scope:** User experience and interface direction for **Story 2** phases **4 through 7** in [`ongoing/book-reading-read-a-range-plan.md`](book-reading-read-a-range-plan.md). **This file is guidance only** — apply it when implementing each phase; it does not replace the delivery plan or tests.

**Related:** Architecture and chrome rules — [`ongoing/doughnut-book-reading-architecture-roadmap.md`](doughnut-book-reading-architecture-roadmap.md) (*Story 2 — Read a range*). Product vocabulary and drawer/sync intent live there; this document spells out **how it should feel and behave** on screen, especially **mobile browser**.

---

## Goals

1. **Reading-first:** On the book reading route, the **PDF is the hero**. Outline/layout is **supporting navigation**, not competing for attention.
2. **Mobile-realistic:** Users often read on a **phone or small tablet** in the browser. Touch scrolling the document must feel **native and uninterrupted**.
3. **Space when needed:** The user can **reclaim width** for the book by hiding the layout; reopening should **not reset** reading position or confuse “where I am” more than necessary.
4. **Responsive defaults:** **Small viewports:** layout **closed by default** so the book uses almost the full screen. **Large viewports:** layout **open by default** so structure is visible without an extra tap. The user can **override** via a single, obvious control (aligned with Phase 7).

---

## Layout model

| Region | Role |
|--------|------|
| **Main (center/right of layout)** | PDF viewer: **maximum usable area**, vertical scroll through pages, pinch/zoom only if pdf.js/product choice allows — **do not** trap scroll in nested panes unless necessary for pdf.js. |
| **Side panel (left)** | Book **outline / BookRange tree** from `GET …/book`: hierarchical list, scrollable if tall. |

**Direction:** DaisyUI drawer/sidebar patterns consistent with the rest of the app; **left** anchor matches the architecture roadmap “drawer sidebar” wording.

---

## Breakpoints and default state

Define **one primary breakpoint** (exact pixel/Tailwind token to be chosen during implementation; e.g. `lg` or project standard).

| Viewport | Default drawer | Rationale |
|----------|----------------|-----------|
| **Below breakpoint** (“small”) | **Hidden** | Book fills the viewport; thumb reach and distraction-free reading. |
| **At/above breakpoint** (“large”) | **Visible** | Desktop/tablet landscape has room; structure visible at a glance. |

**Implementation note:** “Default” means **initial mount** for a route visit. Optional later enhancement: **remember last open/closed** per notebook or globally — call out as **open UX question** below; not required for Phase 7 minimum.

---

## Phase 4 — Book layout in a drawer; PDF remains main focus

**UX outcomes**

- User always understands **where the book is** (main area) vs **where the structure is** (left drawer).
- On **small screens**, first paint is **PDF-forward**; no outline stealing vertical space from the reading column.
- On **large screens**, outline and PDF appear **together** without cramping the PDF below usability (use breakpoint and sensible min-widths for the drawer).

**UI elements to plan**

- **Drawer shell:** width on large screens (fixed vs min/max), backdrop on small when open (tap-outside to close is standard on mobile).
- **Outline presentation:** reuse existing tree semantics; ensure **long outlines** scroll inside the drawer, **not** the whole page (only the PDF main area should drive document scroll).
- **Chrome:** notebook/book context (title, back) should stay **compact** so vertical space stays with the PDF on mobile.

**E2E alignment:** Scenario should assert **tree + PDF** when the drawer is in the **open** state used for the test; on small breakpoints, steps may need to **open the drawer** first (see Phase 7 control).

---

## Phase 5 — Click a range → PDF jumps to the right place

**UX outcomes**

- Tapping a row feels like **“go to this section”**: clear **active/selected** state for the chosen range (distinct from “current position in viewport” in Phase 6 if both exist).
- After navigation, the **target page/region is visible** without the user hunting; respect **safe area** and any **fixed header** so the jump does not land under obscured UI.
- **Loading:** if navigation async, avoid jarring blank flashes; prefer **subtle pending** state on the row or a **minimal** main-area indicator consistent with existing `apiCallWithLoading` patterns where applicable.

**Mobile**

- Hit targets for tree rows meet **comfortable touch** size; nested rows remain **expand/collapse** without accidental “go to” (pattern: chevron vs row body, or delay — pick one consistent pattern).

**E2E alignment:** Observable **page/scroll** change after click, as in the plan.

---

## Phase 6 — Scroll / navigate the PDF → active range in the layout

**Shipped** (see [`ongoing/book-reading-read-a-range-plan.md`](book-reading-read-a-range-plan.md) Phase 6): viewport-current row (`data-outline-current`), debounced updates, aside scroll-into-view for long outlines, **selected** vs **viewport-current** distinguished in UI and ARIA, polite live region when the current range title changes.

**UX outcomes**

- As the user **scrolls or flips pages**, the outline shows **which BookRange best matches** the viewport (single **current** row highlight).
- **Calm feedback:** highlight updates should not **flicker** on small scroll jitter; debounce or hysteresis is a **UX implementation detail** (cohesive “current range” logic per plan).
- If the drawer is **closed**, the user still **has a reading path**; optional **minimal** indicator (e.g. breadcrumb chip, chapter title in the toolbar) is an **open question** — not required if Phase 7 always makes “open drawer” one tap away.

**Accessibility**

- When the drawer is open, **current range** should be **visible** and, where supported, **announced** on meaningful navigation (avoid noisy live regions on every scroll tick).

**E2E alignment:** Scroll to a known region → **highlight** (or equivalent DOM) matches expected range.

---

## Phase 7 — Show / hide the layout drawer

**UX outcomes**

- **One clear control** toggles the drawer: icon + accessible name (e.g. “Outline” / “Table of contents”) on **small and large** screens; placement **does not cover** the primary PDF scroll area (typical: **top app bar** or **floating action** — choose one pattern and reuse).

**Responsive behavior**

- Toggling on **small** screen: opens **over** content with backdrop; closing returns to **full-width PDF**.
- Toggling on **large** screen: **collapses** the left column; PDF **expands**; no unnecessary modal layer if the pattern is a **push/sidebar** layout.

**State**

- **Reopening** after hide: **scroll position and zoom** in the PDF remain; **expanded nodes** in the tree ideally preserved for that session (implementation detail; avoid surprising collapse of the whole tree if cheap to keep).

**E2E alignment:** Toggle works; PDF remains **scrollable** and **usable** with drawer open and closed.

---

## Cross-cutting: reading and scrolling

- **Primary gesture:** vertical **scroll through the book** in the main pane must remain the **default** interaction; the outline is **secondary**.
- **Zoom:** if pdf.js zoom is exposed, ensure **layout toggle** and **outline** do not break pinch or scroll containment (test on mobile Safari/Chrome when implementing).
- **Orientation change:** PDF reflows; **current range** logic (Phase 6) should remain coherent after resize.

---

## Open UX questions (decide during implementation)

1. **Persist drawer open/closed** across visits or devices, or session-only?
2. When the drawer is **closed**, show a **compact “you are here”** label (chapter/range title) in the header?
3. **FAB vs top-bar** toggle on mobile — which matches Doughnut’s existing navigation patterns best?
4. **Selected range** (Phase 5 tap) vs **viewport-derived current range** (Phase 6): two visual styles or one merged state?

---

## Document maintenance

When Phases 4–7 ship, **trim or archive** obsolete speculation here and link to the **actual** components and breakpoint tokens. Keep [`ongoing/doughnut-book-reading-architecture-roadmap.md`](doughnut-book-reading-architecture-roadmap.md) updated for any **architectural** change (e.g. moving panel to the right).
