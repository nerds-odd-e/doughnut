---
phase: 05-alias-as-wiki-link-overlap-declaration
plan: 02
subsystem: api
tags: [aliases, wiki-link, overlap, note-alias-index, search]

requires:
  - phase: 05-alias-as-wiki-link-overlap-declaration
    provides: FrontmatterAliases plain-only from* choke point (05-01)
provides:
  - NoteAliasIndexServiceTest proofs that wiki-link overlap items never become note_alias_index rows
  - SearchControllerAliasTests proofs that overlap wiki-link tokens/inner titles are not alias search hits
affects:
  - Phase 6 OVL-01 (safe derived index / search for plain aliases only)

tech-stack:
  added: []
  patterns:
    - "OVL-03 index/search safety inherits FrontmatterAliases.fromNoteContent plain-only — no local [[ filter in NoteAliasIndexService"
    - "Capability-named regression tests for mixed / wiki-link-only / empty alias lists"

key-files:
  created: []
  modified:
    - backend/src/test/java/com/odde/doughnut/services/NoteAliasIndexServiceTest.java
    - backend/src/test/java/com/odde/doughnut/controllers/SearchControllerAliasTests.java

key-decisions:
  - "Zero production edits — NoteAliasIndexService / NoteAliasSearchService unchanged; safety from 05-01 plain-only from*"
  - "Skipped optional WikiTitleCacheServiceTest awareness (file already >250 lines; orchestration covered by existing refresh_populates_alias_index case)"

patterns-established:
  - "Pattern: createNoteWithAliases + assertNoteNotInSearchResults helpers in SearchControllerAliasTests"

requirements-completed: [OVL-03]

coverage:
  - id: D1
    description: Mixed plain+wiki-link aliases index only plain rows; no alias_display contains [[
    requirement: OVL-03
    verification:
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/services/NoteAliasIndexServiceTest.java#indexes_only_plain_aliases_when_wiki_link_overlap_declared
        status: pass
    human_judgment: false
  - id: D2
    description: Wiki-link-only and empty aliases leave zero note_alias_index rows; refresh replace-all removes plain when only overlap remains
    requirement: OVL-03
    verification:
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/services/NoteAliasIndexServiceTest.java#leaves_no_rows_when_aliases_are_wiki_link_overlap_only
        status: pass
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/services/NoteAliasIndexServiceTest.java#removes_plain_alias_row_when_only_wiki_link_overlap_remains
        status: pass
    human_judgment: false
  - id: D3
    description: Alias search does not surface notes via wiki-link overlap token or inner title; plain alias still searchable
    requirement: OVL-03
    verification:
      - kind: integration
        ref: backend/src/test/java/com/odde/doughnut/controllers/SearchControllerAliasTests.java#does_not_return_note_for_wiki_link_only_overlap_alias_token_or_inner_title
        status: pass
      - kind: integration
        ref: backend/src/test/java/com/odde/doughnut/controllers/SearchControllerAliasTests.java#mixed_aliases_remain_searchable_by_plain_alias_but_not_wiki_link_overlap
        status: pass
    human_judgment: false

duration: 9min
completed: 2026-07-24
status: complete
---

# Phase 5 Plan 02: OVL-03 index + alias search regressions Summary

**Regression proofs close the index-poisoning blast radius: note_alias_index and alias search stay plain-only when wiki-link overlap declarations are authored, with no production service changes.**

## Performance

- **Duration:** ~9 min
- **Started:** 2026-07-24T06:00:42Z
- **Completed:** 2026-07-24T06:10:07Z
- **Tasks:** 2/2
- **Files modified:** 2

## Accomplishments
- `NoteAliasIndexService.refreshForNote` proven to index only plain aliases under mixed / wiki-link-only / empty lists; replace-all still clears plain rows when only overlap remains.
- `SearchController` alias search proven not to hit on raw `[[Other Note]]` or inner title `Other Note`; plain `color` still matches.
- No Flyway / schema change; `NoteAliasIndexService.java` production logic untouched.

## Task Commits

Each task was committed atomically:

1. **Task 1: Alias index refresh indexes plain aliases only when overlap wiki-links present** - `324345dc51` (test)
2. **Task 2: Alias search ignores wiki-link overlap alias items** - `0613a03709` (test)

**Plan metadata:** (pending docs commit)

## Files Created/Modified
- `backend/src/test/java/com/odde/doughnut/services/NoteAliasIndexServiceTest.java` — mixed / wiki-link-only / empty / replace-all regressions
- `backend/src/test/java/com/odde/doughnut/controllers/SearchControllerAliasTests.java` — wiki-link-only + mixed search regressions + helpers

## Decisions Made
- Relied entirely on 05-01 `FrontmatterAliases.fromNoteContent` plain-only choke point (D-02); did not add a local `[[` filter in `NoteAliasIndexService`.
- Omitted optional `WikiTitleCacheServiceTest` awareness assertion to avoid growing an already >250-line file; existing orchestration test still covers alias index refresh via `WikiTitleCacheService`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Concurrent agent interference on working tree / commits**
- **Found during:** Task 1 commit
- **Issue:** Parallel 05-03 work overwrote uncommitted Task 1 edits once; Task 1 commit also scooped staged `.planning/REQUIREMENTS.md` / `ROADMAP.md` / `STATE.md` from concurrent 05-03 updates. Unrelated commit `3bfa318473` later claimed a 05-02 message while adding 05-03 SUMMARY / spa-routing plan artifacts.
- **Fix:** Re-applied `NoteAliasIndexServiceTest` and re-committed; Task 2 staged only `SearchControllerAliasTests.java`. Left spurious `3bfa318473` history alone (not our task payload).
- **Files modified:** planning docs incidentally in `324345dc51`; search tests clean in `0613a03709`
- **Verification:** `backend:test_only` / focused gradle tests green
- **Committed in:** `324345dc51`, `0613a03709`

### Skipped optional work

**2. WikiTitleCacheServiceTest awareness assertion**
- **Plan:** Optional thin assertion that plain alias rows remain after `WikiTitleCacheService.refreshForNote`.
- **Skip reason:** File already >250 lines; NoteAliasIndexServiceTest covers OVL-03; existing `refresh_populates_alias_index_and_resolves_unambiguous_alias_links` covers orchestration.

## Threat Flags

None — no new endpoints, auth paths, or schema; T-05-04 / T-05-05 mitigated by regression proofs on plain-only index inheritance.

## Known Stubs

None.

## Self-Check: PASSED

- SUMMARY.md present at `.planning/phases/05-alias-as-wiki-link-overlap-declaration/05-02-SUMMARY.md`
- Commits `324345dc51`, `0613a03709` present
- `indexes_only_plain_aliases_when_wiki_link_overlap_declared` present
- `does_not_return_note_for_wiki_link_only_overlap_alias_token_or_inner_title` present
- No Flyway migration in plan commits
