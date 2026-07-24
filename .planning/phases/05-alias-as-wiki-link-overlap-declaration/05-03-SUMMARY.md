---
phase: 05-alias-as-wiki-link-overlap-declaration
plan: 03
subsystem: testing
tags: [aliases, wiki-link, overlap, wiki-resolve, cloze, matchAnswer, accidental-match]

requires:
  - phase: 05-01
    provides: FrontmatterAliases plain-only from* + overlapWikiLinkTokensFrom* segregation
provides:
  - Wiki-resolve alias-target OVL-03 regressions for wiki-link overlap items
  - Cloze, matchAnswer, and accidental-match alias-leg OVL-03 regressions
affects:
  - Phase 6 OVL-01 overlap grading (consumers stay plain-only; grading wires overlap accessors)

tech-stack:
  added: []
  patterns:
    - "OVL-03 consumer gates via existing index/plain-only accessors — no WikiLinkResolver or Note production edits"
    - "Capability-named regressions extend WikiLinkResolverYamlAndBodyIntegrationTest + RecallPromptControllerTests"

key-files:
  created: []
  modified:
    - backend/src/test/java/com/odde/doughnut/services/WikiLinkResolverYamlAndBodyIntegrationTest.java
    - backend/src/test/java/com/odde/doughnut/controllers/RecallPromptControllerTests.java

key-decisions:
  - "No production edits — safety inherited from 05-01 plain-only parse + plain-only index refresh"
  - "Regression proofs only; MemoryTrackerService OVERLAP grading left for Phase 6"

patterns-established:
  - "Pattern: wiki-resolve empty resolve when only wiki-link overlap alias exists"
  - "Pattern: cloze/matchAnswer/AM controller fixtures with mixed and wiki-link-only aliases"

requirements-completed: [OVL-03]

coverage:
  - id: D1
    description: Wiki-resolve by plain alias still works; wiki-link overlap items are not alias-resolution targets
    requirement: OVL-03
    verification:
      - kind: integration
        ref: backend/src/test/java/com/odde/doughnut/services/WikiLinkResolverYamlAndBodyIntegrationTest.java#does_not_resolve_alias_target_from_wiki_link_only_overlap_alias
        status: pass
      - kind: integration
        ref: backend/src/test/java/com/odde/doughnut/services/WikiLinkResolverYamlAndBodyIntegrationTest.java#resolves_plain_alias_and_ignores_wiki_link_overlap_item_in_mixed_list
        status: pass
    human_judgment: false
  - id: D2
    description: Cloze masks plain aliases only; wiki-link overlap target titles are not cloze-masked via alias
    requirement: OVL-03
    verification:
      - kind: integration
        ref: backend/src/test/java/com/odde/doughnut/controllers/RecallPromptControllerTests.java#spellingQuestionMasksPlainAliasButNotOverlapWikiLinkTargetTitle
        status: pass
      - kind: integration
        ref: backend/src/test/java/com/odde/doughnut/controllers/RecallPromptControllerTests.java#spellingQuestionDoesNotMaskOverlapTargetTitleFromWikiLinkOnlyAlias
        status: pass
    human_judgment: false
  - id: D3
    description: matchAnswer accepts plain aliases; overlap target title and raw token are not correct via alias
    requirement: OVL-03
    verification:
      - kind: integration
        ref: backend/src/test/java/com/odde/doughnut/controllers/RecallPromptControllerTests.java#answerDoesNotMatchOverlapWikiLinkAliasTargetOrRawToken
        status: pass
    human_judgment: false
  - id: D4
    description: Accidental-match alias leg ignores wiki-link overlap alias items
    requirement: OVL-03
    verification:
      - kind: integration
        ref: backend/src/test/java/com/odde/doughnut/controllers/RecallPromptControllerTests.java#shouldNotAccidentalMatchViaWikiLinkOverlapAliasItem
        status: pass
      - kind: other
        ref: "rg overlapWikiLinkTokensFrom MemoryTrackerService — no matches (Phase 6 not wired)"
        status: pass
    human_judgment: false

duration: 6min
completed: 2026-07-24
status: complete
---

# Phase 5 Plan 03: OVL-03 wiki-resolve + cloze + matchAnswer + AM alias leg Summary

**Capability-named regressions prove wiki-resolve, cloze, matchAnswer, and accidental-match alias leg ignore wiki-link overlap alias items while plain aliases keep today's behavior — with no production grading or resolver edits.**

## Performance

- **Duration:** 6 min
- **Started:** 2026-07-24T06:00:38Z
- **Completed:** 2026-07-24T06:06:30Z
- **Tasks:** 2/2
- **Files modified:** 2

## Accomplishments

- Wiki-resolve: wiki-link-only and mixed overlap alias lists do not create alias-resolution targets for the overlap inner title; plain alias resolve remains green.
- Cloze / matchAnswer: plain aliases still mask and match; overlap target title and raw `[[…]]` are not treated as aliases.
- Accidental-match alias leg ignores wiki-link overlap declarations; `MemoryTrackerService` stays free of `overlapWikiLinkTokensFrom` / OVERLAP wiring.

## Task Commits

1. **Task 1: Wiki-resolve alias targets ignore overlap wiki-link alias items** - `53ce5843f4` (test)
2. **Task 2: Cloze, matchAnswer, and accidental-match alias leg ignore wiki-link overlap items** - `7e4f8e29f2` (test)

**Plan metadata:** (pending docs commit)

## Files Created/Modified

- `backend/src/test/java/com/odde/doughnut/services/WikiLinkResolverYamlAndBodyIntegrationTest.java` — OVL-03 wiki-resolve alias-target regressions
- `backend/src/test/java/com/odde/doughnut/controllers/RecallPromptControllerTests.java` — cloze, matchAnswer, and AM alias-leg OVL-03 regressions

## Decisions Made

- No production edits to `WikiLinkResolver`, `Note`, or `MemoryTrackerService` — consumer safety comes from 05-01 plain-only accessors and plain-only index rows.
- Task-level TDD RED was not forced to fail first: behavior already exists from 05-01; this plan is an OVL-03 regression gate (tests pass without GREEN production work).

## Deviations from Plan

None - plan executed exactly as written (test-only regression gate; no production changes required).

### Pre-commit isolation note

Pre-commit `git add -u` briefly staged parallel 05-02 WIP (`NoteAliasIndexServiceTest` / `WikiTitleCacheServiceTest`). Those files were excluded from 05-03 commits and left unstaged for the 05-02 wave.

## Known Stubs

None.

## Threat Flags

None — no new trust-boundary surface; tests only.

## Self-Check: PASSED

- FOUND: `.planning/phases/05-alias-as-wiki-link-overlap-declaration/05-03-SUMMARY.md`
- FOUND: commits `53ce5843f4`, `7e4f8e29f2`
- FOUND: `WikiLinkResolverYamlAndBodyIntegrationTest` + `RecallPromptControllerTests` modifications
- VERIFIED: `rg overlapWikiLinkTokensFrom MemoryTrackerService` exits 1
- VERIFIED: `pnpm backend:test_only` exits 0
