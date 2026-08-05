# Roadmap: Doughnut

## Milestones

- ✅ **v1.0 Notebook Lint & Auto-Fix** — Phases 1–7 ([archive](./milestones/v1.0-ROADMAP.md)) — shipped 2026-07-23
- ✅ **v1.1 Spelling Answer Match & Link** — Phases 1–6 ([archive](./milestones/v1.1-ROADMAP.md)) — shipped 2026-07-25
- 🚧 **v1.2 Accidental Match Resolve UX** — Phases 7–12 (in progress)

## Overview

v1.2 replaces stacked matched-note bodies on accidental-match spelling results with a compact, optional resolve dialog so the reviewed note keeps full-height focus. Delivery is stop-safe and Behavior/Structure-ordered: shell first, then path identity, Build a link, a small Structure util, Add as overlapped (no try-again / no reclaim), then navigate/reopen polish.

## Phases

<details>
<summary>✅ v1.1 Spelling Answer Match & Link (Phases 1–6) — SHIPPED 2026-07-25</summary>

- [x] Phase 1: Extend Answer outcome API (1/1 plans) — completed 2026-07-23
- [x] Phase 2: Accidental-match grading & penalty (2/2 plans) — completed 2026-07-24
- [x] Phase 3: Reveal both notes after accidental match (3/3 plans) — completed 2026-07-24
- [x] Phase 4: Offer link between notes (3/3 plans) — completed 2026-07-24
- [x] Phase 5: Alias-as-wiki-link overlap declaration (3/3 plans) — completed 2026-07-24
- [x] Phase 6: Overlap "try again, no credit" (4/4 plans) — completed 2026-07-24

</details>

<details>
<summary>✅ v1.0 Notebook Lint & Auto-Fix (Phases 1–7) — SHIPPED 2026-07-23</summary>

- [x] Phase 1: Health lint contract — completed 2026-07-22
- [x] Phase 2: Empty-folder findings — completed 2026-07-22
- [x] Phase 3: Readme-only folder findings — completed 2026-07-22
- [x] Phase 4: Dead-link findings — completed 2026-07-22
- [x] Phase 5: Health tab and Run — completed 2026-07-23
- [x] Phase 6: User-level defaults — completed 2026-07-23
- [x] Phase 7: Gated empty-folder purge — completed 2026-07-23

</details>

### 🚧 v1.2 Accidental Match Resolve UX (In Progress)

**Milestone Goal:** Compact optional resolve dialog for accidental-match results; reviewed note stays primary.

- [x] **Phase 7: Compact result + Resolve dialog shell** - Drop stacked matches; CTA opens dismissible dialog with title list (2/2 plans)
- [x] **Phase 8: Match path and clickable titles** - Per-row notebook breadcrumb and clickable match titles (no body peek)
- [x] **Phase 9: Build a link from resolve dialog** - Single-Modal link offer; stay on result; readonly/unload gates (2/2 plans)
- [x] **Phase 10: Overlap alias append util** - Structure: wiki-link overlap append helper for the next behavior
- [ ] **Phase 11: Add as overlapped note** - Declare overlap from dialog; no try-again / no SRS credit reclaim
- [ ] **Phase 12: Title navigate, reopen, E2E polish** - Return to result and reopen resolve with same matches

## Phase Details

### Phase 7: Compact result + Resolve dialog shell

**Goal**: On accidental-match results, the reviewed note stays primary and matches are revealed only via an optional resolve dialog
**Type**: Behavior
**Depends on**: Nothing (first v1.2 phase; builds on shipped v1.1)
**Requirements**: AMR-01, AMR-02, AMR-03
**Success Criteria** (what must be TRUE):

  1. On an accidental-match spelling result, the UI does not stack full matched-note bodies under the reviewed note
  2. When matches exist, user sees **Resolve accidental match** under the alert and opening it shows a dialog listing match titles (no note body)
  3. User can dismiss the resolve dialog anytime and continue without resolving any match
  4. OVERLAP try-again chrome remains outcome-gated and unchanged on overlap answers

**Plans**: 2/2 plans executed
Plans:

- [x] 07-01-PLAN.md — Tracer: compact result + Resolve CTA title Modal + Vitest edges
- [x] 07-02-PLAN.md — E2E: @wip links then reveal rewrite (CI=true); overlap uncoupled

**UI hint**: yes

### Phase 8: Match path and clickable titles

**Goal**: Each resolve-dialog match is identifiable by notebook path and reachable by title
**Type**: Behavior
**Depends on**: Phase 7
**Requirements**: AMR-04
**Success Criteria** (what must be TRUE):

  1. Each match row shows a notebook path/breadcrumb (identity only — no note body / peek)
  2. Each match title is clickable (navigates toward that note)
  3. Dialog still lists all current matches with title + path together

**Plans**: 2/2 plans executed

Plans:
**Wave 1**

- [x] 08-01-PLAN.md — Tracer: AccidentalMatchResolveRow title link + path breadcrumb (Vitest)

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 08-02-PLAN.md — E2E: path identity + clickable title in resolve dialog (no AMR-05)

**UI hint**: yes

### Phase 9: Build a link from resolve dialog

**Goal**: User can build a property/relationship link to a match from the resolve dialog without leaving the result
**Type**: Behavior
**Depends on**: Phase 8
**Requirements**: AMR-06, AMR-07
**Success Criteria** (what must be TRUE):

  1. From a resolve-dialog row, user can **Build a link** using the existing property/relationship offer as a single Modal step (not a nested PopButton)
  2. After building a link, user remains on the accidental-match result
  3. Build-a-link (and Add-as-overlapped when present) are unavailable when the reviewed notebook is readonly or required note data is not loaded

**Plans**: 2/2 plans executed

Plans:

- [x] 09-01-PLAN.md — Vitest tracer: same-Modal Build a link + AMR-07 gates
- [x] 09-02-PLAN.md — E2E: page-object Resolve → Build a link; untag @wip link scenarios

**UI hint**: yes

### Phase 10: Overlap alias append util

**Goal**: Pure overlap wiki-link append helper exists so Phase 11 can declare overlap without bloating the dialog
**Type**: Structure
**Depends on**: Phase 9
**Requirements**: —
**Success Criteria** (what must be TRUE):

  1. Existing accidental-match and OVERLAP try-again user flows are observably unchanged
  2. A unit-tested helper appends an overlap wiki-link alias token (not a plain alias) suitable for frontmatter merge
  3. No new user-facing **Add as overlapped note** action yet

**Plans:** 1/1 plans executed

Plans:

- [x] 10-01-PLAN.md — Structure tracer: appendOverlapWikiLinkToNoteContent util + Vitest (D-01..D-09)

**UI hint**: yes

### Phase 11: Add as overlapped note

**Goal**: User can declare an overlap wiki-link from the resolve dialog without try-again or SRS credit reclaim on this result
**Type**: Behavior
**Depends on**: Phase 10
**Requirements**: AMR-08, AMR-09
**Success Criteria** (what must be TRUE):

  1. From a resolve-dialog row, user can **Add as overlapped note**, which declares an overlap wiki-link alias on the reviewed note toward that match
  2. After that action, the current result does not show try-again and does not reclaim SRS credit
  3. Answer outcome stays accidental-match and the schedule for this answer is unchanged

**Plans**: 1/2 plans executed

Plans:

**Wave 1**

- [x] 11-01-PLAN.md — Vitest tracer: Add as overlapped note + AMR-07 gates + no try-again

**Wave 2** *(unblocked — Wave 1 complete)*

- [ ] 11-02-PLAN.md — E2E: Resolve → Add as overlapped; stay without try-again; overlap uncoupled

**UI hint**: yes

### Phase 12: Title navigate, reopen, E2E polish

**Goal**: After leaving via a matched title, user can return and reopen the resolve dialog with the same matches; E2E coverage matches the new UX
**Type**: Behavior
**Depends on**: Phase 11
**Requirements**: AMR-05
**Success Criteria** (what must be TRUE):

  1. After navigating away via a matched title and returning to the accidental-match result, user can open the resolve dialog again and see the same matches
  2. Targeted E2E covers resolve dialog open/dismiss, multi-match identity, and reopen-after-navigate (capability-named; no phase numbers in product tests)
  3. Existing `overlap_try_again` coverage remains green

**Plans**: TBD
**UI hint**: yes

## Progress

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1–7 Health lint / purge | v1.0 | 13/13 | Complete | 2026-07-23 |
| 1–6 Spelling match & link | v1.1 | 16/16 | Complete | 2026-07-25 |
| 7. Compact result + Resolve dialog shell | v1.2 | 2/2 | Complete | 2026-08-05 |
| 8. Match path and clickable titles | v1.2 | 2/2 | Complete | 2026-08-05 |
| 9. Build a link from resolve dialog | v1.2 | 2/2 | Complete | 2026-08-05 |
| 10. Overlap alias append util | v1.2 | 1/1 | Complete | 2026-08-05 |
| 11. Add as overlapped note | v1.2 | 1/2 | In progress | - |
| 12. Title navigate, reopen, E2E polish | v1.2 | 0/? | Not started | - |

### Coverage

| Requirement | Phase |
|-------------|-------|
| AMR-01 | Phase 7 |
| AMR-02 | Phase 7 |
| AMR-03 | Phase 7 |
| AMR-04 | Phase 8 |
| AMR-05 | Phase 12 |
| AMR-06 | Phase 9 |
| AMR-07 | Phase 9 |
| AMR-08 | Phase 11 |
| AMR-09 | Phase 11 |

**v1 requirements mapped:** 9/9 ✓

---
*Last updated: 2026-08-05 — v1.2 roadmap created (phases 7–12)*
