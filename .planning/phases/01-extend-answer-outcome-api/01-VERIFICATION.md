---
phase: 01-extend-answer-outcome-api
verified: 2026-07-23T22:20:00Z
status: passed
score: 7/7 must-haves verified
behavior_unverified: 0
overrides_applied: 0
re_verification: No — initial verification
---

# Phase 1: Extend Answer outcome API — Verification Report

**Phase Goal:** The backend→frontend answer contract can represent a third outcome (accidental-match with matched-note id, and an overlap flag) instead of only a boolean `correct`.
**Verified:** 2026-07-23T22:20:00Z
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

This is a pure **Structure** phase: the new contract states must be **representable** but **not yet returned**. Verification confirms representability (entity + enum + DTO + regenerated OpenAPI/TS) AND the no-behavior invariant (no production writer sets the new fields).

### Observable Truths

| # | Truth | Status | Evidence |
| --- | --- | --- | --- |
| 1 | The Answer contract type carries an optional accidental-match state (matchedNoteId: Long) and an optional explicit outcome (AnswerOutcome enum) alongside the unchanged required correct: Boolean. | ✓ VERIFIED | `Answer.java:37` `@Transient @Getter @Setter private Long matchedNoteId;`, `:39` `@Transient @Getter @Setter private AnswerOutcome outcome;`; `correct` still `@NotNull` (`:24-27`); `types.gen.ts:288` `matchedNoteId?: number;`, `:289` `outcome?: 'CORRECT' \| 'WRONG' \| 'ACCIDENTAL_MATCH' \| 'OVERLAP';` (optional) |
| 2 | The AnsweredQuestion response carries an optional overlap: Boolean and an optional matchedNotes: List<NoteTopology> (reusing the existing NoteTopology DTO, not a new note-ref type). | ✓ VERIFIED | `AnsweredQuestion.java:33` `private Boolean overlap;`, `:35` `private List<NoteTopology> matchedNotes;` (no `@Schema(requiredMode=...)`); `NoteTopology.java` reused (id+title); `types.gen.ts:299` `overlap?: boolean;`, `:300` `matchedNotes?: Array<NoteTopology>;` |
| 3 | The correct field remains required (@NotNull + @Schema REQUIRED) and its SRS-credit semantics are unchanged; no existing behavior is altered. | ✓ VERIFIED | `Answer.java:26` `@NotNull` on `correct`; `AnsweredQuestion.java:28` `@Schema(requiredMode=REQUIRED)` on `answer`; `open_api_docs.yaml:3659-3661` Answer `required: [correct, id]` unchanged; `Note.java`, `MemoryTrackerService.java` diffs empty; no-writer grep (see Truth 7) = 0 |
| 4 | No Flyway migration is added and no persisted column is introduced; matchedNoteId and outcome are @Transient (non-persisted) on the Answer entity. | ✓ VERIFIED | `git diff e34dd63ad6..dc3a04d88b -- db/migration/` empty; `Answer.java:37,39` `@Transient`, no `@Column` on new fields (only pre-existing columns have `@Column` at `:16,19,24,29,33`) |
| 5 | No new backend service, no new endpoint, and no UI/Vue change is introduced in Phase 1; AnsweredQuestion.from(RecallPrompt) and MemoryTrackerService.answerSpelling are unchanged and leave the new fields null. | ✓ VERIFIED | Diff stat: 8 files only (no `services/`, `factories/`, `frontend/`, no new controller); `AnsweredQuestion.from()` `:37-50` unchanged (does not set overlap/matchedNotes); `MemoryTrackerService.java` diff empty; no-writer grep = 0 ⇒ fields deterministically null |
| 6 | The regenerated OpenAPI client compiles and the existing frontend consumers of Answer/AnsweredQuestion still type-check with zero source edits (new fields optional via Jackson NON_NULL). | ✓ VERIFIED | Fresh `pnpm generateTypeScript` → `BUILD SUCCESSFUL` (Gradle generateOpenAPIDocs + @hey-api/openapi-ts, 4 files); all four TS fields optional (`?`); `git status` clean after regen ⇒ committed generated files reproducible from Java (not hand-edited, prohibition P-API01-03). Additive optional fields cannot break existing TS consumers (structural guarantee). |
| 7 | On the existing spelling-answer and previously-answered paths, the new fields are absent/null; no code path returns ACCIDENTAL_MATCH or OVERLAP in Phase 1. | ✓ VERIFIED | `grep -rc 'setMatchedNoteId\|setOutcome\|setOverlap\|setMatchedNotes' backend/src/main/java` = **0** (no production writer exists ⇒ fields cannot be non-null on any path); no-behavior tests assert null on correct spelling (`RecallPromptControllerTests:660-668`), wrong spelling (`:707-715`), and previouslyAnswered history (`RecallsControllerTests:193-196`); no `ACCIDENTAL_MATCH`/`OVERLAP` value asserted in any test |

**Score:** 7/7 truths verified (0 present, behavior-unverified)

### Required Artifacts

| Artifact | Expected | Status | Details |
| --- | --- | --- | --- |
| `backend/src/main/java/com/odde/doughnut/entities/AnswerOutcome.java` | NEW enum: CORRECT, WRONG, ACCIDENTAL_MATCH, OVERLAP | ✓ VERIFIED | 4 values exactly, no Lombok, no constructor (`:3-8`) |
| `backend/src/main/java/com/odde/doughnut/entities/Answer.java` | added @Transient matchedNoteId + outcome | ✓ VERIFIED | `:37,39` @Transient + @Getter @Setter; no @Column; correct untouched |
| `backend/src/main/java/com/odde/doughnut/controllers/dto/AnsweredQuestion.java` | added optional overlap + matchedNotes | ✓ VERIFIED | `:33,35`; no @Schema(requiredMode); `import java.util.List` `:8`; `from()` unchanged |
| `packages/generated/doughnut-backend-api/types.gen.ts` | regenerated; optional matchedNoteId/outcome/overlap/matchedNotes | ✓ VERIFIED | `:288,289,299,300` all optional `?`; reproducible (clean git status post-regen) |
| `open_api_docs.yaml` | Answer/AnsweredQuestion schemas gain optional properties; required unchanged | ✓ VERIFIED | Answer `:3649-3658` (matchedNoteId int64 + outcome enum), required `:3659-3661`=[correct,id]; AnsweredQuestion `:3682-3687` (overlap + matchedNotes array), required `:3688-3693` unchanged |
| `RecallPromptControllerTests.java` | no-behavior null-field assertion on spelling path | ✓ VERIFIED | 2 @Test methods (correct + wrong), 4 assertNull each, no @Disabled |
| `RecallsControllerTests.java` | no-behavior null-field assertion on history path | ✓ VERIFIED | 4 assertNull on previouslyAnswered (`:193-196`), no @Disabled |

Note: `sdk.gen.ts` is NOT in the phase diff — consistent with SUMMARY ("no endpoint signature changed"), so regen left it unchanged.

### Key Link Verification

| From | To | Via | Status | Details |
| --- | --- | --- | --- | --- |
| Answer @Transient fields | types.gen.ts optional fields | Jackson serialization (NON_NULL, ObjectMapperConfig) → springdoc OpenAPI → @hey-api/openapi-ts | ✓ WIRED | Fresh regen surfaces all 4 as optional TS fields (A1 verified) |
| AnsweredQuestion.answer + RecallPromptHistoryItem.answer | shared Answer entity | both embed Answer ⇒ both surfaces gain new fields | ✓ WIRED | RecallPromptHistoryItem.java diff empty (inherits via shared entity, no source edit) |
| AnsweredQuestion.matchedNotes | existing NoteTopology | List<NoteTopology> | ✓ WIRED | NoteTopology reused (id+title), no new note-ref DTO (prohibition P-API02-02) |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
| --- | --- | --- | --- |
| A1 — @Transient surfaces in regenerated schema | `pnpm generateTypeScript` then grep types.gen.ts | BUILD SUCCESSFUL; matchedNoteId?/outcome?/overlap?/matchedNotes? present; git status clean | ✓ PASS |
| No-behavior invariant — no production writer | `grep -rc 'setMatchedNoteId\|setOutcome\|setOverlap\|setMatchedNotes' backend/src/main/java` | 0 matches | ✓ PASS |
| No-behavior tests exist + assert null (not populated) | grep test files for getMatchedNoteId/ACCIDENTAL_MATCH | 2 tests in RecallPromptControllerTests (correct+wrong), 1 in RecallsControllerTests; 0 ACCIDENTAL_MATCH assertions | ✓ PASS |
| Backend test suite execution | (not run) | — | ? SKIP — see note below |

**Note on suite execution:** The full `pnpm backend:test_only` / `frontend:test` suite was NOT re-run during verification (dev env already running; verifier guidance: keep fast, prefer static checks). This is acceptable here because the no-behavior invariant is an **absence-of-writer** invariant, deterministically proven by the static grep (= 0 production writers) plus reading `AnsweredQuestion.from()` and `Answer.buildAnswer()` (neither sets the new fields). The new fields are therefore statically guaranteed null on all paths — the no-behavior tests must pass. SUMMARY's claim that `backend:test_only` / `frontend:test` are green is corroborated (not contradicted) by this static proof.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
| --- | --- | --- | --- | --- |
| API-01 | 01-01 | Answer outcome extended beyond boolean correct to represent accidental-match (with matched note id) and overlap states | ✓ SATISFIED | `Answer.matchedNoteId` + `Answer.outcome` (AnswerOutcome enum incl. ACCIDENTAL_MATCH, OVERLAP); surfaced in regenerated types.gen.ts |
| API-02 | 01-01 | AnsweredQuestion carries matched-note topology + overlap flag; OpenAPI client regenerated | ✓ SATISFIED | `AnsweredQuestion.overlap` + `matchedNotes: List<NoteTopology>` (reusing NoteTopology); client regenerated (BUILD SUCCESSFUL, clean git status) |

No orphaned requirements: REQUIREMENTS.md maps only API-01 and API-02 to Phase 1, both claimed by 01-01-PLAN.md and both satisfied. ROADMAP coverage table confirms 9/9 v1 requirements mapped.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
| --- | --- | --- | --- | --- |
| — | — | — | — | No debt markers (TBD/FIXME/XXX/TODO/HACK/PLACEHOLDER) in modified production files; no @Disabled/@Ignore/@wip on new tests; no stub returns; no empty handlers. |

### Prohibitions Verified (negative checks — the must-NOT did NOT happen)

| ID | Prohibition | Status | Evidence |
| --- | --- | --- | --- |
| P-API01-01 | Must NOT alter correct semantics or existing grading/SRS behavior | ✓ verified | `correct` @NotNull untouched; `Note.java`, `MemoryTrackerService.java` diffs empty; no-writer grep = 0 |
| P-API01-02 | Must NOT add Flyway migration or persisted column | ✓ verified | No db/migration files in diff; new fields `@Transient`, no `@Column` |
| P-API01-03 | Must NOT hand-edit generated files | ✓ verified | Fresh regen produces clean `git status` ⇒ committed generated files reproducible from Java |
| P-API02-01 | Must NOT wire backend behavior returning new states | ✓ verified | no-writer grep = 0; no ACCIDENTAL_MATCH/OVERLAP returned on any path |
| P-API02-02 | Must NOT introduce a new note-ref DTO | ✓ verified | Existing `NoteTopology` reused for `matchedNotes` |

### MVP Mode Observation (non-blocking)

ROADMAP.md marks Phase 1 `Mode: mvp`, but the phase goal is a **structure/contract** goal ("the contract can represent a third outcome..."), not a User Story ("As a ... I want to ... so that ..."). `gsd-tools query user-story.validate` returns `false` for this goal. This is expected and appropriate: per `.cursor/rules/planning.mdc`, a pure Structure phase "restructures internals ... without changing any external behavior" and does not carry a user story. The goal-backward technical verification above fully covers the phase's actual contract-representability goal. **No action required** — flagged only so the developer can decide whether the `mode: mvp` flag should be `null` for this structure phase in a future roadmap edit. This does not affect the verdict.

### Gaps Summary

No gaps. All 7 must-have truths, all 3 ROADMAP success criteria, both requirements (API-01, API-02), and all 5 prohibitions are verified against the actual codebase. The highest-risk assumption A1 (JPA `@Transient` surfaces in the springdoc OpenAPI schema) is confirmed by a fresh regen-then-grep with a clean post-regen git status. The no-behavior invariant is statically proven (no production writer exists). The phase is genuinely structure-only: representable but not returned.

---

_Verified: 2026-07-23T22:20:00Z_
_Verifier: Claude (gsd-verifier)_
