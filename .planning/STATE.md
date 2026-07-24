---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
current_phase: 5
current_phase_name: Alias-as-wiki-link overlap declaration
status: planning
stopped_at: Phase 5 context gathered
last_updated: "2026-07-24T05:35:34.499Z"
progress:
  total_phases: 5
  completed_phases: 4
  total_plans: 9
  completed_plans: 9
---

# State: Spelling Answer Match & Link

## Project Reference

- **Project:** Spelling Answer Match & Link
- **Core value:** During spelling recall, an answer that names a *different* note becomes a learning opportunity — penalized lightly, both notes revealed, and a link offered — turning recall confusion into connection-building; and overlapping-but-distinct notes are kept distinct by asking the user for a more specific answer.
- **Repo:** `/Users/terryyin/git/doughnut` (brownfield Spring Boot + Vue)
- **Current focus:** Phase 05 — alias-as-wiki-link overlap declaration

## Current Position

- **Phase:** 5 — Alias-as-wiki-link overlap declaration
- **Plan:** Not started
- **Status:** Ready to discuss/plan
- **Progress:** [█████████████░░░░░░░] 4/6 phases

```
[x][x][x][x][ ][ ] 4/6 phases
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

- **Phases completed:** 4
- **Requirements delivered:** 6/9 (API-01, API-02, AM-01, AM-02, AM-03, AM-04)
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
| Phase 04 P01 | 12min | 2 tasks | 10 files |
| Phase 04 P02 | 6min | 2 tasks | 5 files |
| Phase 04 P03 | 15min | 2 tasks | 3 files |

## Accumulated Context

### Key decisions (from PROJECT.md)

- v1 covers all three problems (accidental match + link + overlap) — shipping a half-feature is not acceptable.
- Match scope: all notebooks the user can read (broader than notebook-scoped `WikiLinkResolver`).
- Accidental-match penalty is lighter than a plain wrong answer (third SRS outcome via `updateForgettingCurve`, no 12h override).
- Overlap is **declared** (alias-as-wiki-link), not auto-detected.
- Reuse `WikiLinkResolver`, `Note.matchAnswer`, `LinkInsertionChoice`, `updateForgettingCurve`.

### Integration points (from codebase map)

- Phase 4 shipped: `MatchedNoteLinkOffer` under matched rows; property via `updateTextField`; relationship via `AddRelationshipFinalize` with `navigateOnSuccess=false` (D-07).
- Phase 5: extend `aliases` frontmatter for wiki-link values — alias blast radius (CONCERNS.md).

### Known risks / blockers

- **⚠️ Alias blast radius (Phase 5):** Extending `aliases` to accept wiki-link values affects wiki resolve, search, and cloze masking. See `.planning/codebase/CONCERNS.md`.

### Todos

- [x] Execute Phase 4 (04-01..04-03) — AM-04 offer-link complete; E2E green; human verify approved (E2E-env browser subagent).
- [ ] Run `/gsd-discuss-phase 5` (or `--auto`) then plan/execute OVL-02/OVL-03.

### Open questions

- Whether the overlap "try again" re-asks the same review immediately or re-queues (Phase 6).

## Session Continuity

**Last session:** 2026-07-24T05:35:34.490Z
**Stopped at:** Phase 5 context gathered
**Resume file:** .planning/phases/05-alias-as-wiki-link-overlap-declaration/05-CONTEXT.md

- **Last action:** Phase 4 closed — VERIFICATION passed; AM-04 complete; human spot-check approved via E2E-env subagent.
- **Next action:** `/gsd-discuss-phase 5 --auto` (or discuss without auto).
- **Resume from:** Read this file + ROADMAP Phase 5.

## Decisions

- [Phase 1]: Locked Option A (D-05): @Transient matchedNoteId + AnswerOutcome enum on Answer; overlap + matchedNotes:List<NoteTopology> on AnsweredQuestion
- [Phase 2]: ACCIDENTAL_MATCH grading + lighter −10 penalty; findAccidentalMatch title then alias
- [Phase 3]: D-01 findAllAccidentalMatches title∪alias; D-02 populate matchedNotes; D-03–D-05 UI NoteShow stack + distinct alert; D-06 no add-link this phase
- [Phase 4]: D-01–D-07 MatchedNoteLinkOffer; property updateTextField; relationship navigateOnSuccess=false; human verify via E2E browser subagent
