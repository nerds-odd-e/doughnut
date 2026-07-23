---
phase: 01-extend-answer-outcome-api
reviewed: 2026-07-23T14:16:00Z
depth: standard
files_reviewed: 7
files_reviewed_list:
  - backend/src/main/java/com/odde/doughnut/entities/AnswerOutcome.java
  - backend/src/main/java/com/odde/doughnut/entities/Answer.java
  - backend/src/main/java/com/odde/doughnut/controllers/dto/AnsweredQuestion.java
  - backend/src/test/java/com/odde/doughnut/controllers/RecallPromptControllerTests.java
  - backend/src/test/java/com/odde/doughnut/controllers/RecallsControllerTests.java
  - packages/generated/doughnut-backend-api/types.gen.ts
  - open_api_docs.yaml
findings:
  critical: 0
  warning: 0
  info: 1
  total: 1
status: issues_found
---

# Phase 01: Code Review Report

**Reviewed:** 2026-07-23T14:16:00Z
**Depth:** standard
**Files Reviewed:** 7 (5 hand-written Java; 2 generated, sanity-checked only)
**Status:** issues_found

## Summary

Phase 01 is a pure **Structure** contract extension: additive optional fields on the answer surfaces, no new behavior, no DB migration, no production writer. The implementation is correct and the no-behavior invariant holds.

Verified against the phase's explicit checklist:

- **`@Transient` usage is correct.** `Answer.matchedNoteId` and `Answer.outcome` use `jakarta.persistence.Transient` (resolved via the existing `import jakarta.persistence.*;` at `Answer.java:6`). They are **not** `@JsonIgnore`, so they remain in the JSON contract; they add **no** `@Column`/`@NotNull`, so they are non-persisted and require no migration. Correct.
- **`correct` semantics untouched.** `Answer.buildAnswer` (`Answer.java:41-51`) and `MemoryTrackerService.answerSpelling` (`MemoryTrackerService.java:201-220`) still derive `correct` from `checkAnswer` / `matchAnswer` exactly as before. The new `AnswerOutcome` enum is declared but never written by any production code, so it cannot alter existing grading behavior.
- **Optional-field style is consistent with existing optionals.** `AnsweredQuestion.overlap` and `AnsweredQuestion.matchedNotes` carry no `@Schema` annotation, matching the existing optionals `predefinedQuestion` (`AnsweredQuestion.java:31`) and `RecalledNote.ancestorFolders` (`RecalledNote.java:19`). The regenerated OpenAPI correctly leaves them out of the `required` arrays for both `Answer` and `AnsweredQuestion`. `Boolean` (boxed) is the right choice for `overlap` — a primitive `boolean` would default to `false` and always serialize, which would be wrong for an optional, currently-unset field.
- **No-behavior invariant holds.** A repo-wide search of `backend/src` finds **zero** production callers of `setMatchedNoteId` / `setOutcome` / `setOverlap` / `setMatchedNotes`. `AnsweredQuestion.from(...)` (`AnsweredQuestion.java:37-50`) does not set the new fields, so they serialize as `null`.
- **Tests assert `null`, not populated values.** Both new test methods (`RecallPromptControllerTests.java:659-668`, `706-715`) and the extended `RecallsControllerTests` assertion (`RecallsControllerTests.java:193-196`) use `assertNull` on every new field, while still asserting the pre-existing `correct` true/false behavior. No behavior is asserted for which there is no writer — exactly right for a Structure phase.
- **No mass-assignment / information-disclosure risk.** `Answer` is response-only: it is never a `@RequestBody`. The only request bodies are `AnswerDTO` (`AnswerDTO.java:6-9`, only `choiceIndex` + `thinkingTimeMs`) and `AnswerSpellingDTO`, neither of which exposes the new fields. `Answer.buildAnswer` only reads `choiceIndex`/`thinkingTimeMs`. Clients therefore cannot inject `matchedNoteId`/`outcome`/`overlap`/`matchedNotes` via request binding, and the new `@Transient` fields expose no persisted data (they are always `null` today).

**Generated files** (`types.gen.ts`, `open_api_docs.yaml`) were sanity-checked for structural well-formedness only, not reviewed as hand-written content. Both diffs are well-formed: the `Answer` schema adds optional `matchedNoteId` (int64) and `outcome` (string enum `CORRECT|WRONG|ACCIDENTAL_MATCH|OVERLAP`); the `AnsweredQuestion` schema adds optional `overlap` (boolean) and `matchedNotes` (array of `NoteTopology`). The enum names match `AnswerOutcome.java` exactly, and no new entries were added to either schema's `required` list.

Only one minor Info-level observation, below. No Critical or Warning findings.

## Info

### IN-01: Redundant `@Getter` on new `Answer` fields

**File:** `backend/src/main/java/com/odde/doughnut/entities/Answer.java:37,39`
**Issue:** `Answer` already declares a class-level `@Getter` (`Answer.java:12`). The two new fields repeat `@Getter` at the field level — `@Transient @Getter @Setter private Long matchedNoteId;` and `@Transient @Getter @Setter private AnswerOutcome outcome;`. The field-level `@Getter` is redundant (Lombok already generates the getter via the class-level annotation); only `@Setter` is actually needed (there is no class-level `@Setter`). The compact single-line annotation form also diverges from the file's prevailing one-annotation-per-line style used by every other field. Neither point risks a bug, but the redundancy can mislead a future reader into thinking the class-level `@Getter` does not cover these fields.
**Fix:**
```java
  @Transient
  @Setter
  private Long matchedNoteId;

  @Transient
  @Setter
  private AnswerOutcome outcome;
```

---

_Reviewed: 2026-07-23T14:16:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
