---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
current_phase: 01
current_phase_name: Extend Answer outcome API
status: verifying
stopped_at: Completed 01-01-PLAN.md (contract round-trip + no-behavior tests; A1 verified)
last_updated: "2026-07-23T14:14:05.316Z"
progress:
  total_phases: 1
  completed_phases: 0
  total_plans: 1
  completed_plans: 0
---

# State: Spelling Answer Match & Link

## Project Reference

- **Project:** Spelling Answer Match & Link
- **Core value:** During spelling recall, an answer that names a *different* note becomes a learning opportunity — penalized lightly, both notes revealed, and a link offered — turning recall confusion into connection-building; and overlapping-but-distinct notes are kept distinct by asking the user for a more specific answer.
- **Repo:** `/Users/terryyin/git/doughnut` (brownfield Spring Boot + Vue)
- **Current focus:** Phase 01 — Extend Answer outcome API

## Current Position

- **Phase:** 01 (Extend Answer outcome API) — EXECUTING
- **Plan:** 1 of 1
- **Status:** Phase complete — ready for verification
- **Progress:** [░░░░░░░░░░] 0%

```
[1][..........] 0/6 phases
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

- **Phases completed:** 0
- **Requirements delivered:** 0/9
- **Coverage:** 9/9 mapped (100%)

**Per-Plan Metrics:**

| Plan | Duration | Tasks | Files |
|------|----------|-------|-------|
| Phase 01 P01 | 23min | 2 tasks | 8 files |

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
- [ ] Run `/gsd-execute-phase 1` to execute 01-01-PLAN.md (contract round-trip + no-behavior tests).
- [ ] Confirm the accidental-match penalty value (e.g. `updateForgettingCurve(−10)`-style) during Phase 2 planning.

### Open questions

- Exact SRS penalty magnitude for the accidental-match outcome (Phase 2).
- Whether the overlap "try again" re-asks the same review immediately or re-queues (Phase 6).

## Session Continuity

**Last session:** 2026-07-23T14:14:05.310Z
**Stopped at:** Completed 01-01-PLAN.md (contract round-trip + no-behavior tests; A1 verified)
**Resume file:** None

- **Last action:** Phase 1 planned — `01-01-PLAN.md` written (1 plan, wave 1, autonomous; tracer = full contract round-trip, expansion = no-behavior tests on both contract surfaces). ROADMAP + STATE finalized.
- **Next action:** `/gsd-execute-phase 1` (Structure — API contract extension; no behavior, no service, no UI).
- **Resume from:** Read this file + `.planning/phases/01-extend-answer-outcome-api/01-01-PLAN.md`; execute the plan.

---
*Last updated: 2026-07-23 during roadmap creation*

## Decisions

- [Phase ?]: Locked Option A (D-05): @Transient matchedNoteId + AnswerOutcome enum on Answer; overlap + matchedNotes:List<NoteTopology> on AnsweredQuestion; A1 (@Transient surfaces in OpenAPI) verified via regen-then-grep
- [Phase ?]: Pure Structure phase: no production writer sets the new fields (grep invariant = 0); AnsweredQuestion.from(RecallPrompt) unchanged; correct stays required/@NotNull and sole SRS-credit signal
- [Phase ?]: Reused existing NoteTopology (id+title) for matchedNotes; no new note-ref DTO; no Flyway migration (fields are @Transient)
