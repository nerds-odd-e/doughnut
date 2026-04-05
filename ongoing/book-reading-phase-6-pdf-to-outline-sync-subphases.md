# Book reading — Phase 6 sub-phases: PDF viewport → active range in the layout

**Parent delivery plan:** [`ongoing/book-reading-read-a-range-plan.md`](book-reading-read-a-range-plan.md) — Phase 6 (*Scroll / navigate the PDF → active range highlight in the layout*).

**UX intent:** [`ongoing/book-reading-ux-ui-roadmap-phases-4-7.md`](book-reading-ux-ui-roadmap-phases-4-7.md) — *Phase 6 — Scroll / navigate the PDF → active range in the layout*.

**Architecture:** [`ongoing/doughnut-book-reading-architecture-roadmap.md`](doughnut-book-reading-architecture-roadmap.md) — two-way sync (this phase implements **PDF → outline**; Phase 5 already shipped **layout → PDF**).

**Planning rules:** `.cursor/rules/planning.mdc` — one **user-visible** behavior per slice where work is UI-shaped; observable tests first when adding behavior; **no dead code** (production paths exercised by that slice’s tests or clearly merged into the next slice before closing); **at most one intentionally failing test** while driving a given slice.

**Status:** **6.7** shipped — visually hidden `role="status"` / `aria-live="polite"` on [`BookReadingPage.vue`](../frontend/src/pages/BookReadingPage.vue) (`data-testid="book-reading-viewport-current-live"`), driven by debounced `viewportCurrentAnchorId` plus title deduplication in [`viewportCurrentLiveAnnouncement.ts`](../frontend/src/lib/book-reading/viewportCurrentLiveAnnouncement.ts); tests in [`viewportCurrentLiveAnnouncement.spec.ts`](../frontend/tests/lib/book-reading/viewportCurrentLiveAnnouncement.spec.ts). **6.8** shipped — [`PdfBookViewer.vue`](../frontend/src/components/book-reading/PdfBookViewer.vue) includes `pagesCount` on `viewportAnchorPage`; [`viewportCurrentRangeFromAnchorPage.ts`](../frontend/src/lib/book-reading/viewportCurrentRangeFromAnchorPage.ts) skips anchors whose `page_idx` is outside the loaded PDF and returns `null` for an out-of-range viewport page or invalid page count; tests in [`viewportCurrentRangeFromAnchorPage.spec.ts`](../frontend/tests/lib/book-reading/viewportCurrentRangeFromAnchorPage.spec.ts). **6.9** shipped — [`BookReadingPage.spec.ts`](../frontend/tests/pages/BookReadingPage.spec.ts) (*updates viewport-current outline row while outline drawer is closed*): with narrow viewport (`outlineOpened` false at mount), emitted `viewportAnchorPage` still updates `data-outline-current` after debounce. Sub-phases **6.1–6.6** match the current viewer + outline stack unless a slice is reopened.

---

## Phase 6 scope (what “done” means)

**User outcome:** As the user **scrolls or changes which part of the PDF is visible**, the layout shows which **`BookRange` best matches** the viewport (single **viewport-current** indication). Feedback stays **calm** (no flicker from tiny scroll jitter). **Phase 5** row activation remains **user’s explicit choice** (**selected**); Phase 6 adds **position-derived current** — see sub-phase **6.3** for how both coexist on screen and in ARIA.

**Out of scope for Phase 6:** Drawer toggle polish beyond what already exists (**Phase 7**), keyboard activation (**Phase 9**), nested expand/collapse vs go-to (**Phase 10**), optional **“you are here”** chip when the drawer is closed (UX open question — defer unless bundled with a later phase).

**E2E surface:** Same book-reading feature and fixture as Phase 5 ([`e2e_test/features/book_reading/book_reading.feature`](../e2e_test/features/book_reading/book_reading.feature)); extend with scroll → **observable** current row (stable `data-*` or class contract on outline buttons), reusing **OCR page markers** where needed.

---

## Sub-phase discipline (each slice)

Before closing a sub-phase and starting the next:

1. **Tests** — Failing test → pass → refactor; run the **relevant** Cypress `--spec` and any new unit spec(s) for this slice.
2. **No dead code** — Remove interim flags, unused emits, duplicate “current” helpers, and debug logging; one cohesive place computes **viewport → best matching range id** (extend it across slices rather than leaving parallel algorithms).
3. **Observable surface** — Prefer DOM, canvas/OCR, or HTTP; unit tests for **pure** “given viewport descriptor + outline rows → current id” I/O where that module is the deliberate contract **and** production calls it in the same slice.
4. **Deploy gate** — Commit, push, and let CD deploy before the next sub-phase unless the team agrees otherwise (same as parent plan).

---

## Sub-phases (maximum practical split)

Order is **scenario-first**: **page-level** sync before **within-page** refinement; **visible current** before **polish** (debounce, scroll-into-view, resize, live region).

### 6.1 — Viewport **page** drives a **viewport-current** outline row

**User-visible:** After scrolling the PDF so a **different page** is primary in view, the outline shows which range is **current** for reading (at minimum: match by **start page** in **`pdf.mineru_outline_v1`** using the same preorder flattening as today’s tree).

**Implementation hint:** `PdfBookViewer` (or the scroll container it owns) emits a small **viewport descriptor** on pdf.js-visible changes (e.g. visible page indices or a single “anchor” page derived from the viewer — choose one approach and keep it private to the viewer). `BookReadingPage` maps descriptor + `flatOutline` → `viewportCurrentRangeId` (name indicative). **First slice** may use a **simple** rule: e.g. **last** range in preorder whose **start page index ≤** anchor page (document the rule in one place).

**Visual (minimal in this slice):** A **distinct** marker from Phase 5 **selected** (e.g. dedicated class and `data-testid` / `data-outline-current` on the current row) so E2E does not confuse “clicked” with “in view.” Final ARIA policy lands in **6.3**; avoid leaving conflicting `aria-current` on two rows before then.

**Tests:** New Cypress scenario: start on page 1 → **scroll** until page 2 marker appears → **Then** the outline row for the range that starts on page 2 is **viewport-current** (and Phase 5 selection assertions stay valid where scenarios still click). **Unit:** pure function inputs/outputs for “page index + ordered outline nodes → current id” for a few cases (happy path + empty).

**Cleanup:** No orphan emits from the viewer if the parent does not consume; no second copy of preorder flattening — reuse `flatOutline` / one builder.

---

### 6.2 — **Same page**, different ranges: use **position** (bbox / scroll) to disambiguate

**User-visible:** When several ranges **start on the same PDF page**, scrolling **up and down** that page changes which one is **viewport-current** (not stuck on the first row for the whole page).

**Implementation hint:** Extend the viewport descriptor with **vertical position** within the page (or equivalent) and refine the pure matcher so bbox-backed starts order correctly relative to the visible region. Reuse **`parseMineruOutlineV1StartAnchor`** / bbox types already used for Phase 5.

**Tests:** Extend book-reading feature: reuse or narrow the existing “same page, different scroll positions” journey — add **Then** steps asserting **viewport-current** row changes when the PDF scroll position changes **without** changing page. **Unit:** minimal cases for tie-breaking and boundary behavior.

**Cleanup:** Fold into the single “viewport → current id” function; delete any page-only duplicate matcher.

---

### 6.3 — **Selected** (Phase 5) vs **viewport-current** (Phase 6): stable **visual + ARIA** contract

**User-visible:** User always understands **what they last clicked** vs **where they are reading now** (two styles **or** one merged style — **pick one product rule** and implement consistently; UX roadmap lists this as an open question).

**Implementation hint:** If two styles: e.g. **selected** keeps `daisy-btn-active` / `aria-selected` (or focus ring policy); **viewport-current** uses a non-conflicting emphasis and carries **`aria-current="location"`** only on the current row. If one merged style: document that **viewport-current overrides** decoration until the next explicit row activation — still must be **test-described**.

**Tests:** E2E: click range A → scroll until range B is current → assert **both** states (or the single merged rule) per the chosen contract.

**Cleanup:** Remove interim duplicate classes; no row with two conflicting “primary” semantics.

---

### 6.4 — **Debounced / hysteresis** so the current row does not flicker

**User-visible:** Small scroll jitter does not **rapidly swap** the current highlight; updates feel **calm** when the user stops or settles.

**Implementation hint:** Debounce or hysteresis **around the matcher output** (not around raw scroll if that loses responsiveness — tune in implementation). Prefer **unit tests** with fake timers for the scheduler; E2E only if a **reliable** non-flaky observation exists.

**Cleanup:** No unused timer handles; unsubscribe on unmount; do not leave both “immediate” and “debounced” paths both updating the DOM.

---

### 6.5 — **Scroll the outline panel** so the viewport-current row stays **visible**

**User-visible:** With a **long** outline, when the current range changes to a row **off-screen** inside the aside, the drawer’s list **scrolls** that row into view (without stealing focus from the PDF if the user is reading).

**Implementation hint:** `scrollIntoView({ block: 'nearest' })` (or equivalent) on **current id change** after debounce; guard when the drawer is **closed** so layout is not forced open.

**Tests:** Prefer **E2E** only if the fixture outline is tall enough or a test-only outline injection path exists; otherwise **thin** coverage via a focused unit/integration test on the “when id changes, call scrollIntoView” wrapper — but avoid **only** internal mocks if an observable DOM test is cheap.

**Cleanup:** Remove duplicate scroll logic from multiple watchers.

---

### 6.6 — **Resize, orientation, and pdf.js scale** keep **current** coherent

**User-visible:** After **window resize** or layout reflow (e.g. breakpoint crossing, `pagesinit` scale), the **viewport-current** row still matches what is on screen (no stale highlight until the next manual scroll).

**Implementation hint:** Re-run the same viewport sampling on `resize` and on pdf.js events that change visible pages; coalesce with **6.4**’s debouncer where appropriate.

**Tests:** Cypress **viewport** change step + assertion on current row (or page marker + current row together). **Unit:** optional for pure “recompute when descriptor changes.”

**Cleanup:** Single subscription site for resize vs scattered duplicates.

---

### 6.7 — **Accessible notification** when the **current range** meaningfully changes — **shipped**

**User-visible:** When the debounced **viewport-current** row **changes** to a **new title**, assistive tech gets a **polite** update — **not** on every scroll tick.

**Implementation hint:** Small `aria-live="polite"` region (`daisy-visually-hidden`, `aria-atomic="true"`) fed after debounced `viewportCurrentAnchorId` updates; live text updates only when the resolved **structural title** changes (not on every distinct anchor id if the title repeats). Message = current range title or empty when none. No `aria-live` on outline rows (selection unchanged).

**Tests:** **Unit** — [`viewportCurrentLiveAnnouncement.spec.ts`](../frontend/tests/lib/book-reading/viewportCurrentLiveAnnouncement.spec.ts) (`structuralTitleForStartAnchorId`, `nextLiveAnnouncementText`). **E2E** not required for this slice.

**Cleanup:** Remove live region if product defers; no duplicate announcements from selection and current.

---

### 6.8 — **Edge cases: invalid anchors and empty / partial outline**

**User-visible:** Ranges with **missing** or **unparseable** anchors are **skipped** for “current” matching; the UI **never** throws; if nothing matches, **no** row is viewport-current (or a documented fallback, e.g. last known good).

**Tests:** **Unit** on the matcher with malformed / out-of-range page indices, unparseable anchor values, and empty / partial outline inputs. **E2E** not required — unit coverage is the contract for this slice.

**Cleanup:** No dead branches “for future formats” until a second anchor format exists; keep mineru-only until then.

---

### 6.9 — **Drawer closed: no broken state** — **shipped**

**User-visible:** With the outline **hidden** (small viewport or user toggled closed), scrolling the PDF still updates **viewport-current** in state so that **reopening** the drawer shows the **correct** row without requiring another scroll (**Phase 7** will deepen toggle UX).

**Implementation hint:** Ensure listeners stay attached to the PDF container regardless of `outlineOpened`; avoid `v-if` teardown of scroll sources when the aside hides.

**Tests:** **Unit** or **thin component/integration** coverage: assert PDF viewport listeners / debounced state stay active when the outline panel is hidden (`outlineOpened` false) and that `viewportCurrentAnchorId` (or equivalent) still updates from scroll. **E2E** not required — avoid flakiness from drawer timing and OCR; optional Cypress only if a later phase needs the full journey.

**Cleanup:** No conditional hooks that only register when the drawer is open unless required for performance (default: keep simple).

---

## Optional consolidation notes

- If the team wants **fewer deploys**, **6.6** can merge with **6.4** (one “viewport changed” pipeline). Keep the **test list** from both so behavior is not dropped.
- **6.7** can be deferred after **6.9** if a11y scheduling prefers; mark in parent plan if skipped.
- **6.8** can be folded into **6.1** if all edge cases are implemented up front — split only when it helps TDD discipline.

---

## Mapping to current codebase (starting point)

Expect cohesion around:

- [`frontend/src/pages/BookReadingPage.vue`](../frontend/src/pages/BookReadingPage.vue) — `flatOutline`, `selectedOutlineRangeId`, outline row chrome
- [`frontend/src/components/book-reading/PdfBookViewer.vue`](../frontend/src/components/book-reading/PdfBookViewer.vue) — pdf.js `EventBus` / scroll container
- [`frontend/src/lib/book-reading/mineruOutlineV1PageIndex.ts`](../frontend/src/lib/book-reading/mineruOutlineV1PageIndex.ts) — anchor parsing (reuse for viewport matching)
- [`frontend/src/lib/book-reading/viewportCurrentLiveAnnouncement.ts`](../frontend/src/lib/book-reading/viewportCurrentLiveAnnouncement.ts) — **6.7** live region text (title resolution + dedupe)
- [`e2e_test/features/book_reading/book_reading.feature`](../e2e_test/features/book_reading/book_reading.feature) — extend scenarios; [`e2e_test/step_definitions/book_reading.ts`](../e2e_test/step_definitions/book_reading.ts) and page objects as needed

---

## Suggested command reminders

- Single feature: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/book_reading/book_reading.feature`
- Frontend unit: `CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/...` (paths per slice)

---

## Maintenance

When sub-phases ship or merge, update this file and the **Completion hint** for Phase 6 in [`ongoing/book-reading-read-a-range-plan.md`](book-reading-read-a-range-plan.md) so links stay accurate.
