---
gsd_state_version: 1.0
milestone: v1.2
milestone_name: Accidental Match Resolve UX
current_phase: 12
current_phase_name: Title navigate, reopen, E2E polish
status: gap_closure_in_progress
stopped_at: Quick 002 Phase 3 done (rich overlaps authoring); next Phase 4 grading from overlaps
last_updated: "2026-08-06T08:00:00Z"
last_activity: 2026-08-06
last_activity_desc: Quick 002 Phase 3 — rich-mode overlaps property (wiki-link list + FE/BE validation)
progress:
  total_phases: 6
  completed_phases: 6
  total_plans: 11
  completed_plans: 11
  percent: 100
---

# Project State

## Project Reference

See: `.planning/PROJECT.md` (updated 2026-08-05)

**Core value:** Healthy mainline for learning and knowledge work — reviewed note stays primary during accidental-match results.
**Current focus:** v1.2 Accidental Match Resolve UX — all phases 7–12 complete (AMR-01..09)

## Current Position

Phase: 12 of 12 (Title navigate, reopen, E2E polish) — complete
Plan: 12-01 executed; 12-02 skipped (KeepAlive sufficient)
Status: Gap closure quick/002 — Phase 3 (rich overlaps authoring) done; Phase 4 next (grading still ignores overlaps until then)
Last activity: 2026-08-06 — overlaps wiki-link list property + FE/BE validation + link display

Progress: [██████████] 100% (6/6 v1.2 phases; 11/11 plans accounted — 10 executed + 1 skipped)

## Performance Metrics

Preserved in `MILESTONES.md` for v1.0–v1.1. v1.2 metrics start after first plan completion.

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 07 | 2/2 | 8min | 4min |
| 08 | 2/2 | 6min | 3min |
| 09 | 2/2 | 7min | 3.5min |
| 10 | 1/1 | 4min | 4min |
| 11 | 2/2 | 14min | 7min |
| 12 | 1/2 (02 skip) | 2min | 2min |

**Per-Plan Metrics:**

| Plan | Duration | Tasks | Files |
|------|----------|-------|-------|
| Phase 07 P01 | 6min | 2 tasks | 5 files |
| Phase 07 P02 | 2min | 2 tasks | 2 files |
| Phase 08 P01 | 4min | 2 tasks | 5 files |
| Phase 08 P02 | 2min | 2 tasks | 2 files |
| Phase 09 P01 | 4min | 2 tasks | 5 files |
| Phase 09 P02 | 3min | 2 tasks | 2 files |
| Phase 10 P01 | 4min | 2 tasks | 2 files |
| Phase 11 P01 | 8min | 2 tasks | 4 files |
| Phase 11 P02 | 6min | 2 tasks | 4 files |
| Phase 12 P01 | 2min | 2 tasks | 3 files |

## Accumulated Context

### Decisions

- Resolve via optional dialog (not stacked NoteShows) — PROJECT.md / v1.2
- Overlap from dialog skips try-again / credit reclaim — PROJECT.md / v1.2
- AMR-07 (readonly gating) lives with first mutating action (Phase 9 Build a link)
- Phase 10 is Structure-only (overlap alias append util) immediately before Phase 11
- SEED-001 and AMR-10..13 deferred to v2 — not on this roadmap
- [Phase 7]: Slot AccidentalMatchResolveDialog without closer — Modal dismiss covers AMR-03
- [Phase 7]: Keep MatchedNoteLinkOffer.vue unused until Phase 9 Build a link
- [Phase 7]: Keep link page-object helpers callable for Phase 9; only tag Gherkin scenarios @wip
- [Phase 7]: Prefer page-object behavior change over Gherkin step text changes
- [Phase 8]: Keep NoteTopology[]; hydrate via getNoteRealmRefAndLoadWhenNeeded (D-01, D-02)
- [Phase 8]: AccidentalMatchResolveRow — NoteTitleWithLink above BreadcrumbWithCircle (D-03..D-06)
- [Phase 8]: No AMR-05 reopen this phase; hydrate on dialog mount; Vitest then E2E (D-07..D-11)
- [Phase 8]: Assert router-link to under RenderingHelper stub for title navigation
- [Phase 8]: Distinct notebook names via accidentalMatchWithTwoMatchedNotes notebookNames option
- [Phase 08]: Same-notebook English practice path assert is enough for E2E AMR-04 (D-11)
- [Phase 08]: Assert visible title anchor without click-through; AMR-05 reopen deferred to Phase 12
- [Phase 9]: Single-Modal step swap to MatchedNoteLinkOffer — never nest PopButton (D-01)
- [Phase 9]: Step state in AccidentalMatchResolveDialog; pass reviewedNoteId (D-02)
- [Phase 9]: Per-row Build a link; reuse offer; closeDialog → return to list (D-03..D-05)
- [Phase 9]: Hide Build a link when readonly or realms unloaded (D-06, D-07)
- [Phase 9]: Vitest Wave 1 then E2E untag @wip Wave 2 (D-08, D-09)
- [Phase 9]: Single-Modal step swap hosts MatchedNoteLinkOffer; closeDialog returns to list
- [Phase 9]: canOfferBuildLink hides Build a link when readonly or realms unloaded
- [Phase 9]: Page-object-only Resolve → Build a link path; Gherkin unchanged (D-09)
- [Phase 9]: Stay-on-result asserts alert + Resolve CTA + dialog list (D-04); no matched-notes-section
- [Phase 10]: One-line appendOverlapWikiLinkToNoteContent composes buildWikiLinkText (no displayText) → appendAliasToNoteContent
- [Phase 10]: Structure-only util + Vitest; Phase 11 wires Add as overlapped note CTA
- [Phase 11]: Per-row Add as overlapped note → appendOverlapWikiLinkToNoteContent → updateTextField; stay on list (D-01..D-05)
- [Phase 11]: Content-only declare — no retry / no OVERLAP try-again / no SRS reclaim (D-06, D-07)
- [Phase 11]: Shared mutating gate for Build a link + Add as overlapped (D-08, D-09)
- [Phase 11]: Wave 1 Vitest then Wave 2 E2E (D-10, D-11); CTA testid add-as-overlapped-note-{id}
- [Phase 11 P01]: canOfferMutatingAction → canMutate; declare Vitest split to AddAsOverlapped.spec (250-line)
- [Phase 11 P02]: openAddAsOverlappedNote + no-try-again E2E; accidental_match.ts step extract; overlap_try_again uncoupled
- [Phase 12]: KeepAlive live matchedNotes sufficient for AMR-05 reopen; Plan 12-02 can be skipped
- [Phase 12]: AMR-05 return path is cy.go('back') only — not Resume or full remount
- [Quick 002 Phase 2]: Shared seam = `frontmatterStringList` + `authoredListPropertyValidation`; aliases remain the only authored list key until Phase 3

### Pending Todos

- Continue `.planning/quick/002-overlaps-property-and-resolve-polish/` from Phase 3 (OVL-02..07 remain)
- Confirm D-mig (legacy wiki-in-aliases migration) at Phase 7 Jidoka; D-copy draft shipped in Phase 1 (tweak OK)
- Process: still missing `*-VERIFICATION.md` for phases 7–12 (audit process gate)

### Blockers/Concerns

- Resolved (12-01): KeepAlive live session preserves `matchedNotes` after history back — no OpenAPI enrichment
- Do not conflate dialog overlap declare with `AnswerOutcome.OVERLAP` try-again / SRS reclaim (ADR 0003) — Phase 11 locked; OVL plan keeps that policy, changes storage only
- Product: overlap must leave `aliases` — dual-purpose wiki-in-aliases is the gap being closed

## Deferred Items

| Category | Item | Status | Deferred At |
|----------|------|--------|-------------|
| seed | SEED-001 MCQ/fuzzy/`Notebook:Title` | dormant | v1.2 scoping |
| polish | AMR-10..13 resolve polish | v2 | v1.2 scoping |

**Parked:** [SEED-001](./seeds/SEED-001-mcq-fuzzy-notebook-title-spelling-match.md)

## Session Continuity

Last session: 2026-08-06
Stopped at: Quick 002 Phase 2 done (shared string-list frontmatter helpers)
Resume file: .planning/quick/002-overlaps-property-and-resolve-polish/PLAN.md
Next action: `execute-plan` Phase 3 — Behavior: rich-mode `overlaps` property
