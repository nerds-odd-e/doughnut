---
gsd_state_version: 1.0
milestone: v1.2
milestone_name: Accidental Match Resolve UX
current_phase: 12
current_phase_name: Title navigate, reopen, E2E polish
status: ready_to_execute
stopped_at: Phase 12 planning complete
last_updated: "2026-08-05T14:08:40.231Z"
last_activity: 2026-08-05
last_activity_desc: docs(12) create phase plans
progress:
  total_phases: 6
  completed_phases: 5
  total_plans: 11
  completed_plans: 9
  percent: 82
---

# Project State

## Project Reference

See: `.planning/PROJECT.md` (updated 2026-08-05)

**Core value:** Healthy mainline for learning and knowledge work — reviewed note stays primary during accidental-match results.
**Current focus:** Phase 12 planned — execute AMR-05 E2E reopen (12-01); KeepAlive contingency (12-02) only if remount

## Current Position

Phase: 12 of 12 (Title navigate, reopen, E2E polish) — plans ready
Plan: 12-01 (E2E reopen) → optional 12-02 (KeepAlive name if remount)
Status: Phase 12 planned; execute E2E-first; skip 12-02 if KeepAlive already green
Last activity: 2026-08-05 — docs(12) create phase plans

Progress: [████████░░] 83% (5/6 v1.2 phases complete; 9/11 plans executed; Phase 12 0/2)

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

### Pending Todos

None.

### Blockers/Concerns

- Phase 12 research: KeepAlive live session preserves `matchedNotes`; `previouslyAnswered` omits them — enrich OpenAPI only if E2E proves KeepAlive fails
- Do not conflate dialog overlap declare with `AnswerOutcome.OVERLAP` try-again / SRS reclaim (ADR 0003) — Phase 11 locked

## Deferred Items

| Category | Item | Status | Deferred At |
|----------|------|--------|-------------|
| seed | SEED-001 MCQ/fuzzy/`Notebook:Title` | dormant | v1.2 scoping |
| polish | AMR-10..13 resolve polish | v2 | v1.2 scoping |

**Parked:** [SEED-001](./seeds/SEED-001-mcq-fuzzy-notebook-title-spelling-match.md)

## Session Continuity

Last session: 2026-08-05T14:08:40.224Z
Stopped at: Phase 12 planning complete
Resume file: .planning/phases/12-title-navigate-reopen-e2e-polish/12-01-PLAN.md
Next action: Execute Phase 12 — `/gsd-execute-phase 12`
