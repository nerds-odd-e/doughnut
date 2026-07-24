---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
current_phase: 6
current_phase_name: Overlap try again no credit
status: ready_to_discuss
stopped_at: Phase 5 complete; next Phase 6 discuss
last_updated: "2026-07-24T06:20:00.000Z"
progress:
  total_phases: 6
  completed_phases: 5
  total_plans: 12
  completed_plans: 12
---

# State: Spelling Answer Match & Link

## Project Reference

- **Project:** Spelling Answer Match & Link
- **Core value:** During spelling recall, an answer that names a *different* note becomes a learning opportunity — penalized lightly, both notes revealed, and a link offered — turning recall confusion into connection-building; and overlapping-but-distinct notes are kept distinct by asking the user for a more specific answer.
- **Repo:** `/Users/terryyin/git/doughnut` (brownfield Spring Boot + Vue)
- **Current focus:** Phase 06 — overlap "try again, no credit"

## Current Position

- **Phase:** 6 — Overlap "try again, no credit"
- **Plan:** Not started
- **Status:** Ready to discuss/plan
- **Progress:** [█████████████░░░░░░░] 5/6 phases

```
[x][x][x][x][x][ ] 5/6 phases
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

- **Phases completed:** 5
- **Requirements delivered:** 8/9 (API-01, API-02, AM-01, AM-02, AM-03, AM-04, OVL-02, OVL-03)
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
| Phase 05 P01 | 6min | 2 tasks | 6 files |
| Phase 05 P03 | 6min | 2 tasks | 2 files |
| Phase 05 P02 | 9min | 2 tasks | 2 files |

## Accumulated Context

### Key decisions (from PROJECT.md)

- v1 covers all three problems (accidental match + link + overlap) — shipping a half-feature is not acceptable.
- Match scope: all notebooks the user can read (broader than notebook-scoped `WikiLinkResolver`).
- Accidental-match penalty is lighter than a plain wrong answer (third SRS outcome via `updateForgettingCurve`, no 12h override).
- Overlap is **declared** (alias-as-wiki-link), not auto-detected.
- Reuse `WikiLinkResolver`, `Note.matchAnswer`, `LinkInsertionChoice`, `updateForgettingCurve`.

### Integration points (from codebase map)

- Phase 4 shipped: `MatchedNoteLinkOffer` under matched rows; property via `updateTextField`; relationship via `AddRelationshipFinalize` with `navigateOnSuccess=false` (D-07).
- Phase 5 shipped: `FrontmatterAliases` plain-only `from*` + `overlapWikiLinkTokensFrom*`; frontend authoredAliasesValidation parity; OVL-03 consumer regressions green.

### Known risks / blockers

- None blocking Phase 6. Alias blast radius mitigated by plain-only segregation (Phase 5).

### Todos

- [x] Execute Phase 5 (05-01..05-03) — OVL-02/OVL-03 complete; VERIFICATION passed 8/8.
- [ ] Run `/gsd-discuss-phase 6` (or `--auto`) then plan/execute OVL-01.
- [ ] Quick plan (outside milestone): `.planning/quick/260724-spa-routing-consistency/` — LB SPA fallback + remove backend frontend-serving; Phase 1 (prod `/settings` deep-link fix) and Phase 2 (classic→EXTERNAL_MANAGED LB migration) done and live in prod, Phases 3-4 pending.
- [ ] Quick plan (outside milestone): `.planning/quick/260724-db-timezone-fix/` — pin JDBC session tz to UTC + repair +8h-skewed prod TIMESTAMPs (2026-06-20 → fix rollout); Phases 1-4 planned, nothing executed.

### Open questions

- Whether the overlap "try again" re-asks the same review immediately or re-queues (Phase 6).

## Session Continuity

**Last session:** 2026-07-24T06:20:00.000Z
**Stopped at:** Phase 5 complete; next Phase 6 discuss
**Resume file:** .planning/ROADMAP.md

- **Last action:** Phase 5 closed — VERIFICATION passed; OVL-02/OVL-03 complete.
- **Next action:** `/gsd-discuss-phase 6 --auto` (or discuss without auto).
- **Resume from:** Read this file + ROADMAP Phase 6.

## Decisions

- [Phase 1]: Locked Option A (D-05): @Transient matchedNoteId + AnswerOutcome enum on Answer; overlap + matchedNotes:List<NoteTopology> on AnsweredQuestion
- [Phase 2]: ACCIDENTAL_MATCH grading + lighter −10 penalty; findAccidentalMatch title then alias
- [Phase 3]: D-01 findAllAccidentalMatches title∪alias; D-02 populate matchedNotes; D-03–D-05 UI NoteShow stack + distinct alert; D-06 no add-link this phase
- [Phase 4]: D-01–D-07 MatchedNoteLinkOffer; property updateTextField; relationship navigateOnSuccess=false; human verify via E2E browser subagent
- [Phase 5]: Keep fromNoteContent/fromFrontmatter plain-only; add overlapWikiLinkTokensFrom* for Phase 6
- [Phase 5]: Whole-item WikiLinkMarkdown.INNER_LINK_PATTERN.matches() for wiki-link alias detection
- [Phase 5]: 05-03: No production edits — OVL-03 consumer safety from 05-01 plain-only parse/index
- [Phase 5]: 05-03: Regression gate only; MemoryTrackerService OVERLAP grading deferred to Phase 6
- [Phase 5]: OVL-03 index/search: zero production edits; inherit FrontmatterAliases plain-only from*
- [Phase 5]: Skipped optional WikiTitleCacheServiceTest awareness (file already >250 lines)
