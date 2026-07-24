---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
current_phase: 4
current_phase_name: Offer link between notes
status: planning
stopped_at: Phase 4 planned; ready to execute
last_updated: "2026-07-24T04:56:08.789Z"
progress:
  total_phases: 4
  completed_phases: 3
  total_plans: 9
  completed_plans: 6
---

# State: Spelling Answer Match & Link

## Project Reference

- **Project:** Spelling Answer Match & Link
- **Core value:** During spelling recall, an answer that names a *different* note becomes a learning opportunity — penalized lightly, both notes revealed, and a link offered — turning recall confusion into connection-building; and overlapping-but-distinct notes are kept distinct by asking the user for a more specific answer.
- **Repo:** `/Users/terryyin/git/doughnut` (brownfield Spring Boot + Vue)
- **Current focus:** Phase 04 — offer-link-between-notes

## Current Position

- **Phase:** 4 — Offer link between notes
- **Plan:** 04-01 (next to execute)
- **Status:** Plans ready (3 plans / 3 waves)
- **Progress:** [████████████░░░░░░░░] 3/6 phases

```
[x][x][x][ ][ ][ ] 3/6 phases
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

- **Phases completed:** 3
- **Requirements delivered:** 5/9 (API-01, API-02, AM-01, AM-02, AM-03)
- **Coverage:** 9/9 mapped (100%)

**Per-Plan Metrics:**

| Plan | Duration | Tasks | Files |
|------|----------|-------|-------|
| Phase 01 P01 | 23min | 2 tasks | 8 files |
| Phase 02 P01 | 6min | 2 tasks | 6 files |
| Phase 02 P02 | 8min | 2 tasks | 3 files |
| Phase 03 P01 | 18min | 2 tasks | 5 files |
| Phase 03 P02 | 3min | 2 tasks | 3 files |
| Phase 03 P03 | 8min | 2 tasks | 3 files |

## Accumulated Context

### Key decisions (from PROJECT.md)

- v1 covers all three problems (accidental match + link + overlap) — shipping a half-feature is not acceptable.
- Match scope: all notebooks the user can read (broader than notebook-scoped `WikiLinkResolver`).
- Accidental-match penalty is lighter than a plain wrong answer (third SRS outcome via `updateForgettingCurve`, no 12h override).
- Overlap is **declared** (alias-as-wiki-link), not auto-detected.
- Reuse `WikiLinkResolver`, `Note.matchAnswer`, `LinkInsertionChoice`, `updateForgettingCurve`.

### Integration points (from codebase map)

- Backend: `findAllAccidentalMatches` + `AnsweredQuestion.matchedNotes` populated on accidental match.
- Frontend: `AnsweredSpellingQuestion.vue` ACCIDENTAL_MATCH alert + `matched-notes-section` NoteShow stack.
- Phase 4: `MatchedNoteLinkOffer` under matched rows; property via `updateTextField`; relationship via `AddRelationshipFinalize` with `navigateOnSuccess=false` (D-07).

### Known risks / blockers

- **⚠️ Alias blast radius (Phase 5):** Extending `aliases` to accept wiki-link values affects wiki resolve, search, and cloze masking. See `.planning/codebase/CONCERNS.md`.

### Todos

- [x] Execute Phase 3 (03-01..03-03) — AM-03 reveal complete; human verify approved.
- [x] Discuss + plan Phase 4 (04-01..04-03).
- [ ] Execute Phase 4 — `/gsd-execute-phase 4` (or local execute-plan).

### Open questions

- Whether the overlap "try again" re-asks the same review immediately or re-queues (Phase 6).

## Session Continuity

**Last session:** 2026-07-24T04:56:08.781Z
**Stopped at:** Phase 4 planned; ready to execute
**Resume file:** .planning/phases/04-offer-link-between-notes/04-01-PLAN.md

- **Last action:** Phase 4 PLAN.md files written (tracer property → relationship+D-07 → E2E/human).
- **Next action:** `/gsd-execute-phase 4` (or plan-check then execute).
- **Resume from:** Read 04-01-PLAN.md + 04-CONTEXT.md.

## Decisions

- [Phase 1]: Locked Option A (D-05): @Transient matchedNoteId + AnswerOutcome enum on Answer; overlap + matchedNotes:List<NoteTopology> on AnsweredQuestion
- [Phase 2]: ACCIDENTAL_MATCH grading + lighter −10 penalty; findAccidentalMatch title then alias
- [Phase 3]: D-01 findAllAccidentalMatches title∪alias; D-02 populate matchedNotes; D-03–D-05 UI NoteShow stack + distinct alert; D-06 no add-link this phase
- [Phase 3]: assumption_delta promote — matchedNotes list is primary; matchedNoteId = first-of-list for Phase 4
- [Phase 4]: D-01 per-row CTA; D-02 reviewed→matched; D-03 preselect past search; D-04 NoteStorage fetch; D-05 hide bare wiki; D-06 write gate; D-07 stay on page via skipNavigation/navigateOnSuccess=false on relationship path
