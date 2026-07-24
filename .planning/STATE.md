---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
current_phase: 3
current_phase_name: Reveal both notes after accidental match
status: planned
stopped_at: Phase 3 planning complete; Wave 1 execute in progress
last_updated: "2026-07-24T04:13:24.542Z"
progress:
  total_phases: 3
  completed_phases: 2
  total_plans: 6
  completed_plans: 3
---

# State: Spelling Answer Match & Link

## Project Reference

- **Project:** Spelling Answer Match & Link
- **Core value:** During spelling recall, an answer that names a *different* note becomes a learning opportunity — penalized lightly, both notes revealed, and a link offered — turning recall confusion into connection-building; and overlapping-but-distinct notes are kept distinct by asking the user for a more specific answer.
- **Repo:** `/Users/terryyin/git/doughnut` (brownfield Spring Boot + Vue)
- **Current focus:** Phase 03 — reveal-both-notes-after-accidental-match

## Current Position

- **Phase:** 3 — Reveal both notes after accidental match
- **Plan:** 03-01 (next to execute)
- **Status:** Planned — 3 plans ready (waves 1→2→3)
- **Progress:** Phase 3 plans 0/3 executed; overall [████░░░░░░░░░░░░░░░░] ~33%

```
[x][x][ ][ ][ ][ ] 2/6 phases
```

## Roadmap Snapshot

| # | Phase | Type | Requirements | Depends on |
|---|-------|------|--------------|------------|
| 1 | Extend Answer outcome API | Structure | API-01, API-02 | — |
| 2 | Accidental-match grading & penalty | Behavior | AM-01, AM-02 | 1 |
| 3 | Reveal both notes after accidental match | Behavior | AM-03 | 2 |
| 4 | Offer link between notes | Behavior | AM-04 | 3 |
| 5 | Alias-as-wiki-link overlap declaration | Structure | OVL-02, OVL-03 | 1 |
| 6 | Overlap "try again, no credit" | Behavior | OVL-01 | 5 |

## Performance Metrics

- **Phases completed:** 2
- **Requirements delivered:** 4/9 (API-01, API-02, AM-01, AM-02)
- **Coverage:** 9/9 mapped (100%)

**Per-Plan Metrics:**

| Plan | Duration | Tasks | Files |
|------|----------|-------|-------|
| Phase 01 P01 | 23min | 2 tasks | 8 files |
| Phase 02 P01 | 6min | 2 tasks | 6 files |
| Phase 02 P02 | 8min | 2 tasks | 3 files |

## Accumulated Context

### Key decisions (from PROJECT.md)

- v1 covers all three problems (accidental match + link + overlap) — shipping a half-feature is not acceptable.
- Match scope: all notebooks the user can read (broader than notebook-scoped `WikiLinkResolver`).
- Accidental-match penalty is lighter than a plain wrong answer (third SRS outcome via `updateForgettingCurve`, no 12h override).
- Overlap is **declared** (alias-as-wiki-link), not auto-detected.
- Reuse `WikiLinkResolver`, `Note.matchAnswer`, `LinkInsertionChoice`, `updateForgettingCurve`.

### Integration points (from codebase map)

- Backend: `MemoryTrackerService.answerSpelling` (lines ~255–280), after `Note.matchAnswer`.
- Frontend: `AnsweredSpellingQuestion.vue`.
- Reuse: `WikiLinkResolver.resolveWikiLinkToken`, `Note.matchAnswer`, `LinkInsertionChoice` / `AddRelationshipFinalize`, `updateForgettingCurve`.

### Known risks / blockers

- **⚠️ Alias blast radius (Phase 5):** Extending `aliases` to accept wiki-link values affects wiki resolve, search, and cloze masking. The derived-index coherence path (`WikiTitleCacheService.refreshForNote` + backfills) is a known regression source (see `.planning/codebase/CONCERNS.md`). Phase 5 is treated as a design spike — enumerate every `aliases` consumer and gate on regression tests before changing the parser. Expect it to take longer than its neighbors.

### Todos

- [x] Run `/gsd-plan-phase 1` to plan the API contract extension.
- [x] Run `/gsd-execute-phase 1` to execute 01-01-PLAN.md (contract round-trip + no-behavior tests).
- [x] Confirm the accidental-match penalty value (D-03 locked at -10 = DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT, half of failed()'s -20, no 12h override) during Phase 2 planning.
- [x] Run `/gsd-execute-phase 2` to execute 02-01-PLAN.md then 02-02-PLAN.md.
- [x] Run `/gsd-plan-phase 3` to write 03-01..03-03 PLAN.md.
- [ ] Run `/gsd-execute-phase 3` (or local execute-plan) starting with 03-01-PLAN.md.

### Open questions

- Whether the overlap "try again" re-asks the same review immediately or re-queues (Phase 6).

## Session Continuity

**Last session:** 2026-07-24T04:13:24.533Z
**Stopped at:** Phase 3 planning complete; Wave 1 execute in progress
**Resume file:** .planning/phases/03-reveal-both-notes-after-accidental-match/03-01-PLAN.md

- **Last action:** Phase 3 executable plans written — `03-01-PLAN.md` (backend findAll + matchedNotes + IDOR), `03-02-PLAN.md` (UI alert + NoteShow stack), `03-03-PLAN.md` (E2E + human verify). Assumption-delta: promote matchedNotes list; matchedNoteId = first-of-list.
- **Next action:** `/gsd-execute-phase 3` (Wave 1 → 03-01).
- **Resume from:** Read this file + `.planning/phases/03-reveal-both-notes-after-accidental-match/03-01-PLAN.md`.

---
*Last updated: 2026-07-23 during roadmap creation*

## Decisions

- [Phase ?]: Locked Option A (D-05): @Transient matchedNoteId + AnswerOutcome enum on Answer; overlap + matchedNotes:List<NoteTopology> on AnsweredQuestion; A1 (@Transient surfaces in OpenAPI) verified via regen-then-grep
- [Phase ?]: Pure Structure phase: no production writer sets the new fields (grep invariant = 0); AnsweredQuestion.from(RecallPrompt) unchanged; correct stays required/@NotNull and sole SRS-credit signal
- [Phase ?]: Reused existing NoteTopology (id+title) for matchedNotes; no new note-ref DTO; no Flyway migration (fields are @Transient)
- [Phase ?]: Set ACCIDENTAL_MATCH @Transient fields on recallPrompt.getAnswer() after merge/save so managed Answer keeps outcome/matchedNoteId
- [Phase ?]: Plan 02-01 title leg only; alias fallback deferred to Plan 02-02
- [Phase ?]: Alias index fixture must call refreshForNote — makeMe does not auto-index aliases
- [Phase 2]: At floor index, nextRecallAt equals now (0 repeat hours); assert greaterThanOrEqualTo
- [Phase 3]: D-01 findAllAccidentalMatches title∪alias union; D-02 populate matchedNotes; matchedNoteId = first-of-list
- [Phase 3]: D-03–D-05 UI: full NoteShow stack + distinct ACCIDENTAL_MATCH alert; D-06 no add-link this phase
- [Phase 3]: Assumption-delta promote: matchedNotes list is primary plural surface
