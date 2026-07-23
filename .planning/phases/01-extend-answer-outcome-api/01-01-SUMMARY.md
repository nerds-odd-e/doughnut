---
phase: 01-extend-answer-outcome-api
plan: 01
subsystem: api
tags: [openapi, springdoc, jpa, jackson, typescript, lombok, vue-tsc]

# Dependency graph
requires: []
provides:
  - "AnswerOutcome enum (CORRECT, WRONG, ACCIDENTAL_MATCH, OVERLAP) on the entities package"
  - "Answer.matchedNoteId (@Transient Long) and Answer.outcome (@Transient AnswerOutcome) — optional, non-persisted contract fields"
  - "AnsweredQuestion.overlap (Boolean) and AnsweredQuestion.matchedNotes (List<NoteTopology>) — optional contract fields, reusing the existing NoteTopology"
  - "Regenerated types.gen.ts / open_api_docs.yaml with the new optional fields (sdk.gen.ts unchanged — no endpoint signature changed)"
  - "No-behavior backend tests pinning the four new fields null on the correct-spelling, wrong-spelling, and previously-answered paths"
affects:
  - 02-accidental-match-grading
  - 03-reveal-both-notes
  - 04-offer-link
  - 05-alias-overlap-declaration
  - 06-overlap-try-again

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "A1 verified: a JPA @Transient (not @JsonIgnore) field on a controller-return entity IS serialized by Jackson and surfaced by springdoc into the OpenAPI schema as an optional TS field"
    - "Additive optional contract fields behind Jackson NON_NULL (ObjectMapperConfig) keep existing frontend/CLI/MCP consumers type-checking with zero source edits"

key-files:
  created:
    - backend/src/main/java/com/odde/doughnut/entities/AnswerOutcome.java
  modified:
    - backend/src/main/java/com/odde/doughnut/entities/Answer.java
    - backend/src/main/java/com/odde/doughnut/controllers/dto/AnsweredQuestion.java
    - packages/generated/doughnut-backend-api/types.gen.ts
    - open_api_docs.yaml
    - backend/src/test/java/com/odde/doughnut/controllers/RecallPromptControllerTests.java
    - backend/src/test/java/com/odde/doughnut/controllers/RecallsControllerTests.java

key-decisions:
  - "Locked Option A (D-05): @Transient matchedNoteId + AnswerOutcome enum on Answer; overlap + matchedNotes:List<NoteTopology> on AnsweredQuestion. A1 (the @Transient-surfaces-in-OpenAPI assumption) verified via regen-then-grep — Option B fallback NOT needed."
  - "Pure Structure: no production writer sets the new fields (grep invariant setMatchedNoteId|setOutcome|setOverlap|setMatchedNotes in backend/src/main/java = 0); AnsweredQuestion.from(RecallPrompt) unchanged; correct stays required/@NotNull and the sole SRS-credit signal."
  - "Reused the existing NoteTopology (id + title) for matchedNotes — no new note-ref DTO; no Flyway migration (matchedNoteId/outcome are @Transient, non-persisted)."

patterns-established:
  - "A1 (foundation for Phases 2–6): JPA @Transient (jakarta.persistence) fields on a controller-return entity ARE serialized by Jackson (Jackson honors com.fasterxml.jackson.annotation.@JsonIgnore, NOT jakarta.persistence.@Transient) and surfaced by springdoc into the OpenAPI schema as optional TS fields. Verified by regenerating types.gen.ts and grepping for matchedNoteId/outcome/matchedNotes/overlap."
  - "Additive optional contract fields (no @Schema(requiredMode=REQUIRED)) + Jackson NON_NULL omit unset fields → existing frontend/CLI/MCP consumers type-check with zero source edits; responses are byte-identical to pre-Phase-1 on existing paths."

requirements-completed: [API-01, API-02]

coverage:
  - id: D1
    description: "Answer contract carries an optional accidental-match state (matchedNoteId: Long) and an optional explicit outcome (AnswerOutcome enum: CORRECT/WRONG/ACCIDENTAL_MATCH/OVERLAP) alongside the unchanged required correct: Boolean, surfaced in the regenerated types.gen.ts"
    requirement: API-01
    verification:
      - kind: unit
        ref: "backend/src/test/java/com/odde/doughnut/controllers/RecallPromptControllerTests.java#shouldNotPopulateAccidentalMatchFieldsOnCorrectSpellingAnswer"
        status: pass
      - kind: unit
        ref: "backend/src/test/java/com/odde/doughnut/controllers/RecallPromptControllerTests.java#shouldNotPopulateAccidentalMatchFieldsOnWrongSpellingAnswer"
        status: pass
      - kind: other
        ref: "pnpm generateTypeScript then grep packages/generated/doughnut-backend-api/types.gen.ts for matchedNoteId + outcome (A1)"
        status: pass
    human_judgment: false
  - id: D2
    description: "AnsweredQuestion response carries an optional overlap: Boolean and an optional matchedNotes: List<NoteTopology> (reusing the existing NoteTopology); the OpenAPI client is regenerated and the existing frontend type-checks with zero source edits"
    requirement: API-02
    verification:
      - kind: unit
        ref: "backend/src/test/java/com/odde/doughnut/controllers/RecallsControllerTests.java#shouldReturnAnsweredRecallPromptsInCurrentWindow (null-field assertions on the previouslyAnswered history surface)"
        status: pass
      - kind: integration
        ref: "pnpm frontend:test (226 files / 1607 tests, incl. vue-tsc --noEmit type-check against the regenerated contract)"
        status: pass
      - kind: other
        ref: "pnpm generateTypeScript then grep types.gen.ts for matchedNotes + overlap (A1)"
        status: pass
    human_judgment: false

# Metrics
duration: 23min
completed: 2026-07-23
status: complete
---

# Phase 01 Plan 01: Extend Answer outcome API Summary

**Answer/AnsweredQuestion OpenAPI contract extended with optional @Transient matchedNoteId + AnswerOutcome enum (CORRECT/WRONG/ACCIDENTAL_MATCH/OVERLAP) and optional overlap + matchedNotes (List<NoteTopology>); client regenerated, frontend type-checks green, and no-behavior tests pin the new fields null on both contract surfaces (representable but not returned).**

## Performance

- **Duration:** 23 min
- **Started:** 2026-07-23T13:49:58Z
- **Completed:** 2026-07-23T14:13:41Z
- **Tasks:** 2
- **Files modified:** 8 (1 new, 7 modified/regenerated)

## Accomplishments
- New `AnswerOutcome` enum (`CORRECT, WRONG, ACCIDENTAL_MATCH, OVERLAP`) mirroring `QuestionType`, placed in the `entities` package so `Answer` references it with no import.
- `Answer` entity gained `@Transient private Long matchedNoteId` and `@Transient private AnswerOutcome outcome` (field-local `@Getter @Setter` per `NoteEmbedding.embedding`; `jakarta.persistence.*` already imported) — non-persisted, optional in the contract. `correct` stays `@NotNull`/required and untouched.
- `AnsweredQuestion` DTO gained optional `private Boolean overlap` and `private List<NoteTopology> matchedNotes` (no `@Schema(requiredMode=...)` — absence makes them optional; `import java.util.List` added). `from(RecallPrompt)` is unchanged (leaves the new fields null). `RecallPromptHistoryItem` inherits the new `Answer` fields automatically via the shared embedded entity (second contract surface, no source edit).
- Regenerated `types.gen.ts` and `open_api_docs.yaml` via `pnpm generateTypeScript` — `Answer` gains optional `matchedNoteId?`/`outcome?`, `AnsweredQuestion` gains optional `overlap?`/`matchedNotes?`; `required` lists unchanged (`Answer.required: [correct, id]`). `sdk.gen.ts` unchanged (no endpoint signature changed).
- **A1 verified** (highest-risk assumption): after regen, `types.gen.ts` contains `matchedNoteId`, `outcome`, `matchedNotes`, AND `overlap` as optional fields — confirming a JPA `@Transient` field surfaces in the springdoc OpenAPI schema. Option B fallback was NOT needed.
- No-behavior backend tests pin the four new fields null on the correct-spelling, wrong-spelling, and previously-answered (history) paths — proving representable-but-not-returned. No test asserts a populated `AnswerOutcome` value (no writer returns one in Phase 1).

## Task Commits

Each task was committed atomically:

1. **Task 1 (tracer): Contract round-trip — Answer @Transient fields + AnswerOutcome enum + AnsweredQuestion optional fields, regen, frontend type-check** - `cdd40a3e7e` (feat)
2. **Task 2 (expansion): Pin representable-but-not-returned with no-behavior backend tests on both contract surfaces** - `dc3a04d88b` (test)

**Plan metadata:** pending final docs commit (docs: complete plan)

## Files Created/Modified
- `backend/src/main/java/com/odde/doughnut/entities/AnswerOutcome.java` - NEW enum: CORRECT, WRONG, ACCIDENTAL_MATCH, OVERLAP (mirrors QuestionType).
- `backend/src/main/java/com/odde/doughnut/entities/Answer.java` - added `@Transient` matchedNoteId + outcome (non-persisted, optional); correct untouched.
- `backend/src/main/java/com/odde/doughnut/controllers/dto/AnsweredQuestion.java` - added optional overlap + matchedNotes (List<NoteTopology>); from(RecallPrompt) unchanged.
- `packages/generated/doughnut-backend-api/types.gen.ts` - regenerated: Answer + matchedNoteId?/outcome?; AnsweredQuestion + overlap?/matchedNotes?.
- `open_api_docs.yaml` - regenerated: Answer/AnsweredQuestion schemas gain optional properties; required lists unchanged.
- `backend/src/test/java/com/odde/doughnut/controllers/RecallPromptControllerTests.java` - two no-behavior @Test methods in the existing AnswerSpelling @Nested class (correct + wrong path) asserting the four new fields are null.
- `backend/src/test/java/com/odde/doughnut/controllers/RecallsControllerTests.java` - extended shouldReturnAnsweredRecallPromptsInCurrentWindow with assertNull on overlap/matchedNotes/answer.matchedNoteId/answer.outcome (second contract surface).

## Decisions Made
- Followed locked decision D-05 (Option A) verbatim; A1 verified via regen-then-grep, so the documented Option B contingency was not invoked.
- No Flyway migration and no persisted column — matchedNoteId/outcome are `@Transient` (prohibition P-API01-02).
- Did not edit `AnsweredQuestion.from(RecallPrompt)`, `RecallPromptHistoryItem.java`, `MemoryTrackerService.answerSpelling`, or `Note.matchAnswer` (locked decisions D-01/D-04) — the new fields stay null and existing grading/SRS behavior is unchanged.

## Deviations from Plan

### Auto-fixed Issues

None - plan executed exactly as written. Both tasks and all acceptance criteria passed on the first attempt.

### Verification deviations (documented, not code deviations)

**1. [Verification scope] Full `CI=true pnpm lint:all` was not run to completion**
- **Found during:** Phase-gate verification step 5 (Format/lint)
- **Issue:** `pnpm lint:all` includes `test:sut-restart` and `test:sut-start` (SUT-lifecycle `node --test` scripts) that restart/start the already-running SUT — which the phase constraints explicitly forbid ("The dev environment is ALREADY RUNNING ... Do NOT restart it"). Additionally, the `backend:lint` Gradle `lint` task hung on Gradle-daemon contention with the long-running `bootRunE2E` daemon (>5 min, no output).
- **Fix:** Ran the equivalent lint coverage through targeted commands that do not touch the SUT and avoid the daemon hang: `pnpm format:all` (backend spotlessApply UP-TO-DATE + redocly valid), pre-commit hook `spotlessApply` + `redocly lint` on both task commits, `CI=true` Biome lints for frontend/cli/test-fixtures/cy (all "No fixes applied") + `vue-tsc --noEmit` (clean), and `backend/gradlew -p backend spotlessCheck --no-daemon` (UP-TO-DATE, BUILD SUCCESSFUL). `scripts/check_diff_whitespace.sh` on the committed phase diff is clean.
- **Files modified:** none (verification-only deviation).
- **Verification:** spotlessCheck UP-TO-DATE; Biome CI lints exit 0; redocly "Your API description is valid"; check_diff_whitespace.sh exit 0.
- **Committed in:** N/A (no code change).

---

**Total deviations:** 0 auto-fixed code deviations; 1 documented verification-scope deviation (full `lint:all` substituted with equivalent targeted lint coverage due to SUT-restart prohibition + Gradle daemon hang).
**Impact on plan:** None on the contract or behavior. All phase-gate checks are green via the targeted equivalents; the full `lint:all` was blocked by environment constraints, not by any code issue.

## Issues Encountered
- The pre-commit hook (`scripts/git-hooks/pre-commit`) runs `git add -u` after formatting, which stages ALL modified tracked files — so the orchestrator's pending `.planning/STATE.md` setup change was bundled into the Task 1 feat commit. Harmless: STATE.md is updated again by the SDK at plan close and re-committed in the final docs commit.
- `backend:lint` (Gradle `lint` task) hung on daemon contention with the running `bootRunE2E` daemon; `spotlessCheck --no-daemon` confirmed Java formatting is clean (UP-TO-DATE) in 2s.
- The SUT backend healthcheck reported `backend (127.0.0.1:9081) ECONNREFUSED` during the run; the `bootRunE2E` process was still alive. Not investigated further and NOT restarted (per constraints), because no remaining step needed the SUT (`backend:test_only` uses its own test-profile DB; `frontend:test` uses vite on 5174). Flagged here for the developer's awareness only.

## TDD Gate Compliance
- Plan frontmatter is `type: execute` (not `type: tdd`), so the plan-level RED/GREEN/REFACTOR gate does not apply.
- Task 2 carried `tdd="true"` but its `<behavior>` is a no-behavior pin (assert the four new fields are null). After Task 1, the getters exist and no writer sets the fields, so the null-assertion tests PASS immediately (RED is green on first run) — the expected no-behavior state. There is no GREEN implementation to write because prohibition P-API02-01 forbids any writer in Phase 1. Task 2 was committed as a single `test(...)` commit. No test asserts a populated `AnswerOutcome` value (grep `ACCIDENTAL_MATCH` in the two test files = 0).

## User Setup Required
None - no external service configuration required. Phase 1 adds no packages, no endpoints, no services, no migrations, no UI.

## Next Phase Readiness
- Contract foundation is in place for Phases 2–6: `Answer.matchedNoteId`/`outcome` and `AnsweredQuestion.overlap`/`matchedNotes` are representable in the OpenAPI contract and surface as optional TS fields.
- Phase 2 (accidental-match grading) can now write `matchedNoteId` + `outcome = ACCIDENTAL_MATCH` behind the existing `assertReadAuthorization` gate and re-check OWASP ASVS V4 (Access Control) when it searches across all readable notebooks — that is when matched-note data first crosses the trust boundary.
- No blockers. The `AnswerOutcome` enum values are cosmetic pre-wire (A3); Phase 2/6 may rename cheaply before any writer is wired.

---
*Phase: 01-extend-answer-outcome-api*
*Completed: 2026-07-23*
