---
phase: 05-alias-as-wiki-link-overlap-declaration
verified: 2026-07-24T06:13:39Z
status: passed
score: 8/8 must-haves verified
behavior_unverified: 0
overrides_applied: 0
re_verification: false
---

# Phase 5: Alias-as-wiki-link overlap declaration Verification Report

**Phase Goal:** As a note author, I want to declare overlap by putting well-formed wiki-link tokens in the aliases frontmatter list, so that overlapping notes are declared for later grading without breaking plain-alias wiki-resolve, search, or cloze masking.

**Verified:** 2026-07-24T06:13:39Z
**Status:** passed
**Re-verification:** No — initial verification
**Mode:** mvp

## User Flow Coverage

User story: «As a note author, I want to declare overlap by putting well-formed wiki-link tokens in the aliases frontmatter list, so that overlapping notes are declared for later grading without breaking plain-alias wiki-resolve, search, or cloze masking.»

| Step | Expected | Evidence | Status |
|------|----------|----------|--------|
| Edit aliases | Property editor / authored validation accepts well-formed `[[…]]` items alongside plain aliases | `authoredAliasesValidation.ts` + `RichFrontmatterPropertyValueDialog.vue` / `noteContentPropertyRows.ts` wiring; Vitest accept/reject | ✓ |
| Save note | Content PATCH accepts mixed plain+wiki-link aliases; rejects malformed | `TextContentControllerTests.accepts_well_formed_wiki_link_overlap_alias_list` / `rejects_malformed_wiki_link_alias_list_item` | ✓ |
| Declaration stored | Overlap wiki-link tokens extractable for later grading (Phase 6 seam) | `FrontmatterAliases.overlapWikiLinkTokensFrom*` + unit tests | ✓ |
| Plain alias resolve/search/cloze | Plain aliases still resolve, search, and cloze-mask; wiki-link items do not participate | Wiki-resolve / Search / RecallPrompt / NoteAliasIndex regression tests | ✓ |
| Outcome | Overlaps declared without breaking plain-alias consumers | All four ROADMAP success criteria verified below | ✓ |

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
| --- | ------- | ---------- | -------------- |
| 1 | A note's `aliases` frontmatter can contain a wiki-link value that declares overlap (ROADMAP SC1 / OVL-02) | ✓ VERIFIED | `FrontmatterAliases.isWikiLinkAliasItem` + `isAcceptableAuthoredAliasItem`; HTTP accept; frontend parity; `FrontmatterAliasesTest` + `TextContentControllerTests` |
| 2 | Existing wiki-link resolution by title/alias still works for plain aliases; wiki-link-only overlap items are not alias targets (ROADMAP SC2 / OVL-03) | ✓ VERIFIED | `WikiLinkResolver.aliasTargetCandidates` → `NoteAliasIndex` lookup; `does_not_resolve_alias_target_from_wiki_link_only_overlap_alias`; `resolves_plain_alias_and_ignores_wiki_link_overlap_item_in_mixed_list` |
| 3 | Search unchanged for plain aliases; wiki-link alias items are not searchable (ROADMAP SC3 / OVL-03) | ✓ VERIFIED | Index via `fromNoteContent` plain-only; `SearchControllerAliasTests` wiki-link-only + mixed cases; `NoteAliasIndexServiceTest.indexes_only_plain_aliases_*` (gradle PASS) |
| 4 | Cloze-masking unchanged; wiki-link alias does not leak/break cloze (ROADMAP SC4 / OVL-03) | ✓ VERIFIED | `Note.createMaskedContentForRecall` → `hideAliases(fromNoteContent)`; `spellingQuestionMasksPlainAliasButNotOverlapWikiLinkTargetTitle`; wiki-link-only stem keeps target title unmasked |
| 5 | `from*` / `matchesFromNoteContent` plain-only; `overlapWikiLinkTokensFrom*` returns raw `[[…]]` tokens (D-02/D-03) | ✓ VERIFIED | `FrontmatterAliases.java` lines 28–62, 107–128; `FrontmatterAliasesTest` segregation + overlap token order (gradle PASS) |
| 6 | Frontend `authoredAliasesValidation` lockstep with backend (accept well-formed; reject malformed) | ✓ VERIFIED | Identical `AUTHORED_ALIASES_MESSAGE`; Vitest `accepts well-formed wiki-link overlap alias items` PASS; wired into property editor |
| 7 | `matchAnswer` accepts plain aliases only; overlap target title / raw `[[…]]` not correct via alias | ✓ VERIFIED | `Note.matchAnswer` → `matchesFromNoteContent`; `answerDoesNotMatchOverlapWikiLinkAliasTargetOrRawToken` asserts incorrect + `overlap` null |
| 8 | Accidental-match alias leg ignores wiki-link overlap items | ✓ VERIFIED | `shouldNotAccidentalMatchViaWikiLinkOverlapAliasItem` — wrong answer, no ACCIDENTAL_MATCH / matched notes |

**Score:** 8/8 truths verified (0 present, behavior-unverified)

### Required Artifacts

| Artifact | Expected | Status | Details |
| -------- | ----------- | ------ | ------- |
| `backend/.../FrontmatterAliases.java` | Plain-only `from*` + `overlapWikiLinkTokensFrom*` + authored wiki-link accept | ✓ VERIFIED | Substantive; wired to index, Note, AuthoredNoteContent |
| `backend/.../FrontmatterAliasesTest.java` | Capability-named unit proofs | ✓ VERIFIED | Segregation, overlap tokens, authored accept/reject |
| `frontend/.../authoredAliasesValidation.ts` | Client parity | ✓ VERIFIED | Wired via `noteContentPropertyRows` + dialog |
| `frontend/tests/.../authoredAliasesValidation.spec.ts` | Vitest parity | ✓ VERIFIED | Accept/reject wiki-link cases present; spot-check PASS |
| `backend/.../NoteAliasIndexServiceTest.java` | Plain-only index proofs | ✓ VERIFIED | `indexes_only_plain_aliases_*`; suite PASS |
| `backend/.../SearchControllerAliasTests.java` | Search regressions | ✓ VERIFIED | wiki-link-only + mixed not searchable |
| `backend/.../WikiLinkResolverYamlAndBodyIntegrationTest.java` | Wiki-resolve regressions | ✓ VERIFIED | wiki-link-only + mixed cases |
| `backend/.../RecallPromptControllerTests.java` | Cloze / matchAnswer / AM | ✓ VERIFIED | Three OVL-03 fixtures present |

### Key Link Verification

(gsd-tools `verify.key-links` failed because PLAN `from:` values are conceptual symbols, not file paths — verified manually)

| From | To | Via | Status | Details |
| ---- | --- | --- | ------ | ------- |
| `FrontmatterAliases.authoredValidationError*` | `isAcceptableAuthoredAliasItem` | D-04 | ✓ WIRED | Uses plain ∪ wiki-link; returns `AUTHORED_ALIASES_MESSAGE` |
| `fromNoteContent` / `fromFrontmatter` | plain aliases only | `validAliasesFromRawItems` + `isValidPlainAliasText` | ✓ WIRED | Filters out `[[`/`]]` |
| `authoredAliasesValidation.ts` | backend authored rules | lockstep message + whole-item wiki pattern | ✓ WIRED | Message MATCH; used by property editor |
| `NoteAliasIndexService.refreshForNote` | `FrontmatterAliases.fromNoteContent` | plain-only choke point | ✓ WIRED | Line 38; no local `[[` filter |
| `NoteAliasSearchService` | `note_alias_index` | inherits plain-only rows | ✓ WIRED | Reads `NoteAliasIndex` rows only |
| `WikiLinkResolver.aliasTargetCandidates` | `NoteAliasIndex` lookup keys | `findByNotebookNameAndAliasLookupKey` | ✓ WIRED | Lines 184–188 |
| `Note` cloze / `matchAnswer` | `fromNoteContent` / `matchesFromNoteContent` | `hideAliases` | ✓ WIRED | Note.java ~106, 135–139 |
| `WikiLinkResolver.aliasAccidentalCandidates` | `NoteAliasIndex` | plain-only keys | ✓ WIRED | AM path uses same index |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
| -------- | ------------- | ------ | ------------------ | ------ |
| `NoteAliasIndexService` | `aliases` | `FrontmatterAliases.fromNoteContent(note.getContent())` | YAML frontmatter plain items only | ✓ FLOWING |
| `overlapWikiLinkTokensFrom*` | overlap tokens | same YAML list, wiki-link filter | Author-authored `[[…]]` strings | ✓ FLOWING |
| Search / wiki-resolve / AM | lookup keys | `note_alias_index.alias_lookup_key` | Derived from plain aliases after refresh | ✓ FLOWING |
| Cloze stem | masked content | `hideAliases(fromNoteContent(…))` | Plain aliases only | ✓ FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
| -------- | ------- | ------ | ------ |
| FrontmatterAliases segregation + overlap API | `gradlew test --tests …FrontmatterAliasesTest` | BUILD SUCCESSFUL (~2s) | ✓ PASS |
| Index plain-only under wiki-link overlap | `gradlew test --tests …NoteAliasIndexServiceTest` | BUILD SUCCESSFUL (~7s) | ✓ PASS |
| Frontend wiki-link accept | `vitest … authoredAliasesValidation -t "accepts well-formed wiki-link"` | 1 passed | ✓ PASS |
| Message lockstep BE/FE | string compare `AUTHORED_ALIASES_MESSAGE` | exact MATCH | ✓ PASS |

### Probe Execution

Step 7c: SKIPPED (not a migration/tooling phase; no `probe-*.sh` declared).

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
| ----------- | ---------- | ----------- | ------ | -------- |
| OVL-02 | 05-01 | Aliases accept wiki-link values declaring overlap | ✓ SATISFIED | Authored accept + HTTP + overlap extraction API |
| OVL-03 | 05-01 seam + 05-02 + 05-03 | Preserve wiki-resolve, search, cloze (and matchAnswer/AM) | ✓ SATISFIED | Index/search/resolve/cloze/matchAnswer/AM regressions |
| OVL-01 | — | Overlap "try again, no credit" grading | deferred | Phase 6 — correctly out of scope |

Orphaned requirements for Phase 5: none (OVL-02, OVL-03 both claimed).

### Prohibitions

| Prohibition | Status | Evidence |
| ----------- | ------ | -------- |
| Must NOT wire MemoryTrackerService OVERLAP grading / `AnsweredQuestion.overlap` | ✓ VERIFIED (static) | No `OVERLAP` / `overlapWikiLink` / `setOverlap` in `MemoryTrackerService`; only `ACCIDENTAL_MATCH`. Recall tests assert `getOverlap()` null on non-overlap paths |
| Must NOT index wiki-link alias strings into `note_alias_index` | ✓ VERIFIED | `fromNoteContent` plain filter + `indexes_only_plain_aliases_*` / wiki-link-only zero rows |
| Must NOT change Flyway schema | ✓ VERIFIED | Phase commits are feat/test/docs only; no new `V*.sql` for aliases/overlap |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
| ---- | ---- | ------- | -------- | ------ |
| — | — | No TBD/FIXME/XXX/TODO/HACK in phase production files | — | None |

### Human Verification Required

None. Authoring and consumer behaviors are covered by automated unit/controller/integration tests; no `<human-check>` blocks in PLANs; no PRESENT_BEHAVIOR_UNVERIFIED truths.

### Gaps Summary

No gaps. Phase goal achieved: wiki-link aliases are authorable and extractable; plain-alias wiki-resolve, search, cloze, matchAnswer, and accidental-match alias leg remain plain-only. Overlap grading (OVL-01) correctly deferred to Phase 6.

---

_Verified: 2026-07-24T06:13:39Z_
_Verifier: Claude (gsd-verifier)_
