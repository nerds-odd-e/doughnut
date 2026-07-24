---
phase: 02-accidental-match-grading-penalty
verified: 2026-07-24T00:58:10Z
status: passed
score: 9/12 must-haves verified
behavior_unverified: 1
overrides_applied: 0
mvp_goal_format: invalid
mvp_note: "ROADMAP mode is mvp but phase goal is not user-story form (gsd_run query user-story.validate → false). Technical goal-backward verification proceeded per orchestrator request; run /gsd mvp-phase 2 if User Flow UAT framing is required."
behavior_unverified_items:

  - truth: "The accidental-match lookup prefers a title match over an alias match (title-then-alias fallback)."
    test: "Create a wrong spelling answer that equals note A's title AND note B's alias (both readable, neither is the reviewed note). Answer via answerSpelling."
    expected: "outcome=ACCIDENTAL_MATCH and matchedNoteId=A (title), never B (alias-only)."
    why_human: "Code returns titleMatch before consulting the alias leg, but no controller test creates a conflicting title+alias pair to exercise the ordering invariant."
human_verification:

  - test: "Submit a blank or whitespace-only spelling answer when another readable note exists."
    expected: "Graded plain wrong — correct=false, outcome=null, matchedNoteId=null (no accidental match)."
    why_human: "PLAN backstop truth (insufficient_spec). findAccidentalMatch has no blank guard; no held-out test covers empty/blank answers."

  - test: "Create two readable notes with the same title (different ids); answer with that title (wrong for reviewed note)."
    expected: "matchedNoteId equals the lower note id (OrderByIdAsc + first readable)."
    why_human: "PLAN backstop truth (insufficient_spec). Repository orders by id ASC and firstReadableAccidentalCandidate returns first, but no test with multiple simultaneous matches."

  - test: "Create a wrong answer that equals note A's title AND note B's alias; answer via answerSpelling."
    expected: "matchedNoteId=A (title preferred); alias leg not used when a readable title match exists."
    why_human: "Title-then-alias ordering is wired but not behaviorally tested (PRESENT_BEHAVIOR_UNVERIFIED)."
---

# Phase 2: Accidental-match grading & penalty Verification Report

**Phase Goal:** When a user types a spelling answer that is wrong for the reviewed note but matches another note's title or alias (searched across all notebooks the user can read), the system grades it as an accidental match with a lighter-than-wrong spaced-repetition penalty.

**Verified:** 2026-07-24T00:58:10Z  
**Status:** human_needed  
**Re-verification:** No — initial verification

**MVP mode note:** ROADMAP lists `Mode: mvp`, but the phase goal is **not** in user-story form (`As a …, I want to …, so that …`). User Flow Coverage UAT framing was not applied. Technical goal-backward verification of roadmap success criteria and PLAN must_haves was performed instead.

## Goal Achievement

### Roadmap Success Criteria (contract)

| # | Success criterion | Status | Evidence |
| --- | --- | --- | --- |
| 1 | Spelling answer matching another note's title or alias (all readable notebooks) graded as accidental match, distinct from plain wrong | ✓ VERIFIED | Title test + alias test: `outcome=ACCIDENTAL_MATCH`, `matchedNoteId` set, `correct=false`; plain-wrong Phase 1 test keeps `outcome=null` |
| 2 | Accidental match applies lighter SRS penalty than plain wrong (third outcome, no 12h override) | ✓ VERIFIED | `shouldApplyLighterPenaltyThanWrongAnswer`: index 200→190; `nextRecallAt > now` and ≠ now+12h; `partialFail()` / `markAsAccidentalMatch` (not `recallFailed`) |
| 3 | When answer matches reviewed note, accidental-match search skipped | ✓ VERIFIED | `shouldSkipAccidentalMatchSearchWhenAnswerMatchesReviewedNoteEvenIfAnotherNoteSharesTitle`; service only searches under `if (!correct)` |

### Observable Truths

| # | Truth | Status | Evidence |
| --- | --- | --- | --- |
| 1 | Title/alias match → ACCIDENTAL_MATCH (distinct from wrong) | ✓ VERIFIED | `shouldGradeAsAccidentalMatchWhenWrongAnswerMatchesAnotherReadableNoteTitle`, `…Alias`; AccidentalMatch nested suite exit 0 |
| 2 | Lighter penalty (−10 clamped) + no +12h nextRecallAt | ✓ VERIFIED | Index 190.0f; nextRecallAt assertions; `ForgettingCurve.partialFail` → `add(-INCREMENT)`; `MemoryTracker.markAsAccidentalMatch` bumps lastRecalledAt + `calculateNextRecallAt` |
| 3 | Skip accidental search when reviewed note matches | ✓ VERIFIED | `if (!correct)` in `answerSpelling` + skip-when-correct controller test |
| 4 | No readable match → plain wrong (outcome/matchedNoteId null) | ✓ VERIFIED | `shouldNotPopulateAccidentalMatchFieldsOnWrongSpellingAnswer` |
| 5 | Unreadable notebook match not leaked (IDOR) | ✓ VERIFIED | `shouldNotLeakMatchedNoteIdFromUnreadableNotebook`; `userMayReadNotebook` in `firstReadableAccidentalCandidate` |
| 6 | `AnsweredQuestion.matchedNotes` / `overlap` stay null on Phase 2 paths | ✓ VERIFIED | Asserted null in AccidentalMatch tests; service never sets them |
| 7 | Alias leg when no title match | ✓ VERIFIED | `shouldGradeAsAccidentalMatchWhenWrongAnswerMatchesAnotherReadableNoteAlias` + `findByAliasLookupKeyOrderByNoteIdAsc` |
| 8 | Floor clamp: index stays ≥ 100 on accidental match | ✓ VERIFIED | `shouldNotDropForgettingCurveIndexBelowFloorOnAccidentalMatch` |
| 9 | Accidental match counts toward wrong-answer threshold | ✓ VERIFIED | `shouldStillCountAccidentalMatchTowardWrongAnswerThreshold` (5th → `isThresholdExceeded` true) |
| 10 | Blank/empty answer is not an accidental match (backstop) | ⚠️ insufficient_spec | No blank guard; no held-out test — see Human Verification |
| 11 | Multiple matches → lowest id as matchedNoteId (backstop) | ⚠️ insufficient_spec | `ORDER BY n.id ASC` + first readable wired; no multi-match test — see Human Verification |
| 12 | Title preferred over alias when both match | ⚠️ PRESENT_BEHAVIOR_UNVERIFIED | `findAccidentalMatch` returns titleMatch before alias leg; no conflicting-pair test |

**Score:** 9/12 truths verified (1 present, behavior-unverified; 2 backstop insufficient_spec)

### Required Artifacts

| Artifact | Expected | Status | Details |
| -------- | -------- | ------ | ------- |
| `NoteRepository.java` | `findByNoteTitleOrderByIdAsc` | ✓ VERIFIED | Exists, JOIN FETCH notebook, deletedAt guards, ORDER BY id ASC |
| `NoteAliasIndexRepository.java` | `findByAliasLookupKeyOrderByNoteIdAsc` | ✓ VERIFIED | Reuses SELECT_ALIAS_WITH_NOTEBOOK + ACTIVE_NOTE_AND_NOTEBOOK |
| `WikiLinkResolver.java` | `findAccidentalMatch` title-then-alias + IDOR filter | ✓ VERIFIED | Substantive; wired from MemoryTrackerService |
| `ForgettingCurve.java` | `partialFail()` | ✓ VERIFIED | `add(-DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT)` |
| `MemoryTracker.java` | `markAsAccidentalMatch` | ✓ VERIFIED | partialFail + bump lastRecalledAt + calculateNextRecallAt; no recallFailed |
| `MemoryTrackerService.java` | accidental branch + WikiLinkResolver inject | ✓ VERIFIED | Constructor param + `!correct` → findAccidentalMatch → set outcome/matchedNoteId → markAsAccidentalMatch |
| `RecallPromptControllerTests.java` | `@Nested AccidentalMatch` (7 tests) | ✓ VERIFIED | Title, penalty, IDOR, skip, alias, floor, threshold |

`gsd_run query verify.artifacts`: Plan 01 6/6, Plan 02 3/3 passed.

### Key Link Verification

| From | To | Via | Status | Details |
| ---- | -- | --- | ------ | ------- |
| MemoryTrackerService | WikiLinkResolver | `findAccidentalMatch` when `!correct` | ✓ WIRED | Pattern verified |
| WikiLinkResolver | AuthorizationService | `userMayReadNotebook` on candidates | ✓ WIRED | Shared helper for title + alias |
| MemoryTracker | ForgettingCurve | `partialFail()` in markAsAccidentalMatch | ✓ WIRED | Not recallFailed |
| WikiLinkResolver | NoteAliasIndexRepository | `findByAliasLookupKeyOrderByNoteIdAsc` | ✓ WIRED | Alias fallback |
| WikiLinkResolver | FrontmatterAliases | `normalizedLookupKey(answer)` | ✓ WIRED | Alias lookup key |

`gsd_run query verify.key-links`: Plan 01 3/3, Plan 02 2/2 verified.

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
| -------- | ------------- | ------ | ------------------ | ------ |
| MemoryTrackerService.answerSpelling | `Answer.matchedNoteId` / `outcome` | `wikiLinkResolver.findAccidentalMatch` → DB title/alias repos + auth filter | Real note ids from repositories | ✓ FLOWING |
| MemoryTracker.markAsAccidentalMatch | `forgettingCurveIndex` / `nextRecallAt` | `partialFail()` + `calculateNextRecallAt()` | Computed from SRS state | ✓ FLOWING |
| AnsweredQuestion response | embeds Answer | `AnsweredQuestion.from(recallPrompt)` | Transient fields set in-memory after save | ✓ FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
| -------- | ------- | ------ | ------ |
| AccidentalMatch nested suite | `CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests 'RecallPromptControllerTests$AccidentalMatch'` | BUILD SUCCESSFUL (exit 0; FROM-CACHE) | ✓ PASS |

### Probe Execution

| Probe | Command | Result | Status |
| ----- | ------- | ------ | ------ |
| — | — | No phase-declared probes | SKIPPED |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
| ----------- | ---------- | ----------- | ------ | -------- |
| **AM-01** | 02-01, 02-02 | Detect accidental match (title or alias across readable notebooks) | ✓ SATISFIED | Title + alias controller tests; IDOR + skip semantics |
| **AM-02** | 02-01, 02-02 | Lighter-than-wrong SRS penalty, no 12h override | ✓ SATISFIED | Penalty (−10), floor-clamp, nextRecallAt assertions |

REQUIREMENTS.md maps AM-01/AM-02 → Phase 2 (Complete). No orphaned Phase 2 requirement IDs. AM-03/AM-04 belong to later phases (not claimed by Phase 2 plans).

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
| ---- | ---- | ------- | -------- | ------ |
| — | — | No TBD/FIXME/XXX in phase-touched production files | — | None |
| MemoryTrackerService accidental path | — | Does not call `recallFailed` | ℹ️ Info | Prohibition satisfied |
| db/migration | — | No Phase 2 Flyway for matchedNoteId/outcome | ℹ️ Info | Remains @Transient |

### Prohibitions (must-NOT)

| Prohibition | Status | Evidence |
| ----------- | ------ | -------- |
| Must NOT route through `recallFailed` | ✓ must-NOT confirmed | Accidental path uses `markAsAccidentalMatch` → `partialFail` only |
| Must NOT populate matchedNotes/overlap in Phase 2 | ✓ must-NOT confirmed | Service never sets; tests assert null |
| Must NOT add Flyway/@Column for matchedNoteId/outcome | ✓ must-NOT confirmed | Still `@Transient` on Answer; no phase migration |
| Must NOT auto-detect overlap on correct answers | ✓ must-NOT confirmed | Search only under `!correct` + skip test |
| Must NOT change `correct` semantics | ✓ must-NOT confirmed | Accidental path keeps `correct=false` |
| Must NOT add new wrong-answer counter | ✓ must-NOT confirmed | Reuses `hasExceededWrongAnswerThreshold` / `countWrongAnswersSinceForMemoryTracker` |
| Must NOT use DB-level readability for alias leg | ✓ must-NOT confirmed | Alias candidates filtered via Java `userMayReadNotebook` |

### Human Verification Required

### 1. Blank / empty spelling answer (backstop)

**Test:** Submit blank or whitespace-only spelling answer while another readable note exists.  
**Expected:** Plain wrong — `correct=false`, `outcome=null`, `matchedNoteId=null`.  
**Why human:** PLAN tagged `verification: backstop`; no held-out test; no explicit blank guard in `findAccidentalMatch`.

### 2. Deterministic lowest-id among multiple matches (backstop)

**Test:** Two readable notes share the same title; answer with that title (wrong for reviewed).  
**Expected:** `matchedNoteId` = lower id.  
**Why human:** Ordering is coded (`ORDER BY n.id ASC` + first readable) but not covered by a multi-match test.

### 3. Title preferred over alias

**Test:** Wrong answer equals note A's title and note B's alias.  
**Expected:** `matchedNoteId` = A.  
**Why human:** Title-then-alias fallback is present and wired; ordering invariant not exercised by a test.

### Gaps Summary

No **FAILED** roadmap success criteria or missing/stub artifacts. Phase goal (AM-01 / AM-02 grading + lighter penalty) is implemented and covered by passing AccidentalMatch controller tests.

Status is **human_needed** (not `passed`) because:

1. Two PLAN **backstop** truths lack held-out tests (`insufficient_spec`).
2. One ordering truth is present and wired but **behavior-unverified** (title-over-alias).

These do not block the three ROADMAP success criteria; they are edge-case confirmation items for human/held-out coverage.

---

_Verified: 2026-07-24T00:58:10Z_  
_Verifier: Claude (gsd-verifier)_
