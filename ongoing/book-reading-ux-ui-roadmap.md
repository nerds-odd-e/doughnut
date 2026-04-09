# Book reading — UX/UI roadmap

**Scope:** The **single** user-experience and interface direction for book reading in Doughnut: **Story 2** layout (drawer, book layout ↔ PDF sync, show/hide structure), **mobile-realistic** reading, and **reading record** surfaces (including the **Reading Control Panel**). **This file is guidance only** — apply it when implementing features; it does not replace delivery plans or tests.

**Related:** Architecture and vocabulary — [`ongoing/doughnut-book-reading-architecture-roadmap.md`](doughnut-book-reading-architecture-roadmap.md) (*Story 2 — Read a block*, **direct content**, `ReadingRecord`). Shipped milestones for the block reader — [`ongoing/book-reading-read-a-block-plan.md`](book-reading-read-a-block-plan.md). Reading record delivery — [`ongoing/book-reading-reading-record-plan.md`](book-reading-reading-record-plan.md).

---

## Goals

1. **Reading-first:** On the book reading route, the **PDF is the hero**. The **book layout** is **supporting navigation**, not competing for attention.
2. **Mobile-realistic:** Users often read on a **phone or small tablet** in the browser. Touch scrolling the document must feel **native and uninterrupted**.
3. **Space when needed:** The user can **reclaim width** for the book by hiding the book layout; reopening should **not reset** reading position or confuse “where I am” more than necessary.
4. **Responsive defaults:** **Small viewports:** book layout **closed by default** so the book uses almost the full screen. **Large viewports:** book layout **open by default** so structure is visible without an extra tap. The user can **override** via a single, obvious control (GlobalBar / book layout toggle; see drawer section below).
5. **Progress without clutter:** Actions that record **what happened with direct content** (read / skim / skip, per architecture) live in a **docked, dismissible** control region so they do not replace the PDF as the main surface.

---

## Layout model

| Region | Role |
|--------|------|
| **Main (center/right of layout)** | PDF viewer: **maximum usable area**, vertical scroll through pages, pinch/zoom per product/pdf.js choice — **do not** trap scroll in nested panes unless necessary for pdf.js. Hosts the **Reading Control Panel** (see below) as an overlay or bottom-anchored strip **inside** this region so it moves with the book, not the app chrome. |
| **Side panel (left)** | **`BookBlock` tree** (book layout) from `GET …/book`: hierarchical list of **book blocks**, scrollable if tall. |

**Direction:** DaisyUI drawer/sidebar patterns consistent with the rest of the app; **left** anchor matches the architecture roadmap “drawer sidebar” wording.

---

## Reading Control Panel (direct content disposition)

**Purpose:** Let the user **mark the direct content** of a **book block** (see **Direct content** in the architecture roadmap) as **read**, **skimmed**, or **skipped** — without modal stacks that break reading flow.

**Placement:** **Near the bottom** of the **PDF main pane** (above safe-area inset on notched devices). It is **not** in the global top bar: it stays visually tied to “this book / this viewport.”

**States**

| State | Behavior |
|--------|-----------|
| **Expanded** | Shows **short context** (e.g. which **book block**’s direct content is in question — title from the **selected block**) and **actions** (**Mark as read**, **Mark as skimmed**, **Mark as skipped**). Optional secondary control to **open the book layout** or focus the relevant **book block** if the drawer is closed. |
| **Minimized** | Collapses to a **small bar** with **one or two** controls only — e.g. **chevron to expand** + **single primary** action (quick “mark read”), or **expand** + **overflow** for less common actions. Minimized state must **not** block page text: prefer floating strip, rounded bar, or thin dock with **large enough touch targets** (same comfort as **book blocks** in the list). |

**Interaction rules**

- **Does not own document scroll:** The panel sits **over** or **above** the scrollable PDF viewport; vertical swipe on the page remains **scroll-through** except on the panel’s own hit targets.
- **Coexists with viewport sync:** The **current block** (and optional **current selection** from a **book block** tap) drives **which** block’s direct content the panel describes. Copy and timing align with [`ongoing/book-reading-reading-record-plan.md`](book-reading-reading-record-plan.md) (e.g. predecessor-block prompt vs current block — implement to match Gherkin and user stories).
- **Coexists with drawer:** Opening/closing the book layout **does not** reset panel expand/minimize preference for the session unless we later persist it (open UX question).
- **Accessibility:** Expand/collapse and primary actions need **visible labels** or **accessible names**; avoid relying only on iconography for “mark read.”

**Delivery mapping:** The reading-record plan’s **Phase 2** slice (**read** + persistence + read styling on **book blocks** via this panel) and **Phase 3** (**auto-mark when no direct content**, so the panel is not needed on that path) are **shipped** — see [`ongoing/book-reading-reading-record-plan.md`](book-reading-reading-record-plan.md) and [`e2e_test/features/book_reading/reading_record.feature`](../e2e_test/features/book_reading/reading_record.feature). **Phase 4** (**skim/skip** actions + layout `data-*` cues + **`PUT` disposition body**) is **shipped** — [`ongoing/book-reading-phase-4-skim-skip-plan.md`](book-reading-phase-4-skim-skip-plan.md). Optional **persistence of panel state** stays deferred unless product pulls it in.

---

## Breakpoints and default state (book layout drawer)

Define **one primary breakpoint** (exact pixel/Tailwind token to be chosen during implementation; e.g. `lg` or project standard).

| Viewport | Default drawer | Rationale |
|----------|----------------|-----------|
| **Below breakpoint** (“small”) | **Hidden** | Book fills the viewport; thumb reach and distraction-free reading. |
| **At/above breakpoint** (“large”) | **Visible** | Desktop/tablet landscape has room; structure visible at a glance. |

**Implementation note:** “Default” means **initial mount** for a route visit. Optional later enhancement: **remember last open/closed** per notebook or globally — see open UX questions below.

---

## Story 2 — Drawer, navigation, and PDF sync

The following sections describe **shipped or planned** Story 2 reader behavior from [`ongoing/book-reading-read-a-block-plan.md`](book-reading-read-a-block-plan.md). They remain the baseline for **book layout + PDF** chrome; the **Reading Control Panel** is an **additional** main-pane affordance for **reading record**, not a replacement for the book layout toggle.

### Book layout in a drawer; PDF remains main focus

**UX outcomes**

- User always understands **where the book is** (main area) vs **where the structure is** (left drawer).
- On **small screens**, first paint is **PDF-forward**; the book layout does not steal vertical space from the reading column.
- On **large screens**, book layout and PDF appear **together** without cramping the PDF below usability (use breakpoint and sensible min-widths for the drawer).

**UI elements**

- **Drawer shell:** width on large screens (fixed vs min/max), backdrop on small when open (tap-outside to close is standard on mobile).
- **Book layout presentation:** reuse existing tree semantics; ensure **long lists of book blocks** scroll inside the drawer, **not** the whole page (only the PDF main area should drive document scroll).
- **Chrome:** notebook/book context (title, back) should stay **compact** so vertical space stays with the PDF on mobile.

**E2E alignment:** Scenario should assert **tree + PDF** when the drawer is in the **open** state used for the test; on small breakpoints, steps may need to **open the book layout** first (see **Show / hide the book layout drawer** below).

### Book block selection jumps the PDF to the right place

**UX outcomes**

- Tapping a **book block** feels like **“go to this section”**: clear **current selection** state for the chosen block (distinct from the **current block** if both exist).
- After navigation, the **target page/region is visible** without the user hunting; respect **safe area** and any **fixed header** so the jump does not land under obscured UI.
- **Loading:** if navigation async, avoid jarring blank flashes; prefer **subtle pending** state on the **book block** or a **minimal** main-area indicator consistent with existing `apiCallWithLoading` patterns where applicable.

**Mobile**

- Hit targets for **book blocks** meet **comfortable touch** size; nested rows remain **expand/collapse** without accidental “go to” (pattern: chevron vs row body, or delay — pick one consistent pattern).

**E2E alignment:** Observable **page/scroll** change after click, as in the plan.

### PDF scroll and navigation update the current block in the book layout

**Shipped** (see read-a-block plan — viewport sync): **current block** (`data-current-block`), debounced updates, aside scroll-into-view for long book layouts, **current selection** vs **current block** distinguished in UI and ARIA, polite live region when the current block title changes.

**UX outcomes**

- As the user **scrolls or flips pages**, the book layout shows **which `BookBlock` best matches** the viewport (single **current block** highlight).
- **Calm feedback:** highlight updates should not **flicker** on small scroll jitter; debounce or hysteresis is a **UX implementation detail** (cohesive **current block** logic per plan).
- If the drawer is **closed**, the user still **has a reading path**; optional **minimal** indicator (e.g. breadcrumb chip, chapter title in the toolbar) is an **open question** — not required if the book layout toggle always makes “open drawer” one tap away.

**Accessibility**

- When the drawer is open, the **current block** should be **visible** and, where supported, **announced** on meaningful navigation (avoid noisy live regions on every scroll tick).

**E2E alignment:** Scroll to a known region → **highlight** (or equivalent DOM) matches expected **book block**.

### Show / hide the book layout drawer

**UX outcomes**

- **One clear control** toggles the drawer: icon + accessible name (e.g. **“Book layout”**) on **small and large** screens; placement **does not cover** the primary PDF scroll area (typical: **top app bar** or **floating action** — choose one pattern and reuse).

**Responsive behavior**

- Toggling on **small** screen: opens **over** content with backdrop; closing returns to **full-width PDF**.
- Toggling on **large** screen: **collapses** the left column; PDF **expands**; no unnecessary modal layer if the pattern is a **push/sidebar** layout.

**State**

- **Reopening** after hide: **scroll position and zoom** in the PDF remain; **expanded nodes** in the tree ideally preserved for that session (implementation detail; avoid surprising collapse of the whole tree if cheap to keep).

**E2E alignment:** Toggle works; PDF remains **scrollable** and **usable** with drawer open and closed.

---

## Cross-cutting: reading and scrolling

- **Primary gesture:** vertical **scroll through the book** in the main pane must remain the **default** interaction; the **book layout** is **secondary**.
- **Zoom:** if pdf.js zoom is exposed, ensure **layout toggle**, **book layout**, and **Reading Control Panel** do not break pinch or scroll containment (test on mobile Safari/Chrome when implementing).
- **Orientation change:** PDF reflows; **current block** logic should remain coherent after resize.

---

## Open UX questions (decide during implementation)

1. **Persist drawer open/closed** across visits or devices, or session-only?
2. When the drawer is **closed**, show a **compact “you are here”** label (chapter/**book block** title) in the header?
3. **FAB vs top-bar** toggle on mobile — which matches Doughnut’s existing navigation patterns best?
4. **Current selection** (tap on a **book block**) vs **current block** (viewport-derived): two visual styles or one merged state?
5. **Reading Control Panel:** default **expanded vs minimized** on first visit / small viewport? Remember user preference per notebook or globally?
6. **Reading Control Panel:** when both **current selection** and **current block** differ, which drives the “direct content in question” copy — always reading-order rule from the plan, or explicit UI affordance to switch?

---

## Document maintenance

When behavior ships, **trim obsolete speculation** and link to **actual** components and breakpoint tokens. Keep [`ongoing/doughnut-book-reading-architecture-roadmap.md`](doughnut-book-reading-architecture-roadmap.md) updated for **architectural** changes (e.g. moving the panel or redefining direct content). Keep delivery plans updated for **milestone- or slice-scoped** acceptance criteria and tests.
