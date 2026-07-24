---
phase: 02-accidental-match-grading-penalty
reviewed: 2026-07-24T00:56:00Z
depth: standard
files_reviewed: 7
files_reviewed_list:
  - backend/src/main/java/com/odde/doughnut/entities/repositories/NoteRepository.java
  - backend/src/main/java/com/odde/doughnut/entities/repositories/NoteAliasIndexRepository.java
  - backend/src/main/java/com/odde/doughnut/services/WikiLinkResolver.java
  - backend/src/main/java/com/odde/doughnut/entities/ForgettingCurve.java
  - backend/src/main/java/com/odde/doughnut/entities/MemoryTracker.java
  - backend/src/main/java/com/odde/doughnut/services/MemoryTrackerService.java
  - backend/src/test/java/com/odde/doughnut/controllers/RecallPromptControllerTests.java
findings:
  critical: 0
  warning: 2
  info: 3
  total: 5
status: issues_found
---

# Phase 02: Code Review Report

**Reviewed:** 2026-07-24T00:56:00Z
**Depth:** standard
**Files Reviewed:** 7
**Status:** issues_found

## Summary

Phase 02’s accidental-match writer is mostly sound: parameterized title/alias queries, Java-side `userMayReadNotebook` IDOR filtering, reviewed-note exclusion, title-then-alias preference, clamped `partialFail()` (-10), and `markAsAccidentalMatch` (bump `lastRecalledAt`, no +12h) match the plan. Controller coverage for title, alias, IDOR (title), skip-when-correct, floor clamp, and threshold counting is solid.

Two correctness gaps remain: no blank/empty short-circuit (plan must-have), and alias lookup does not trim the answer the way `FrontmatterAliases.matchesFromNoteContent` does—so padded alias answers miss accidental-match and take the harsher plain-wrong path. No Critical/security blockers found in the IDOR or SQL seams.

## Warnings

### WR-01: Blank/empty answers are not excluded from accidental-match lookup

**File:** `backend/src/main/java/com/odde/doughnut/services/WikiLinkResolver.java:42-51`
**Also:** `backend/src/main/java/com/odde/doughnut/services/MemoryTrackerService.java:283-291`
**Issue:** Plan 02-01 must-have backstop requires: empty/blank spelling answers are **not** accidental matches and grade as plain wrong (`outcome=null`, `matchedNoteId=null`). `findAccidentalMatch` and the `answerSpelling` branch have no `null`/`isBlank()` guard. A blank answer still runs `findByNoteTitleOrderByIdAsc` / alias lookup. Notes are not `@NotBlank` on title, so an empty-title readable note would be returned as `ACCIDENTAL_MATCH` and get the lighter penalty instead of plain wrong.
**Fix:** Short-circuit before lookup (service or resolver):

```java
public Optional<Note> findAccidentalMatch(String answer, Note reviewedNote, User viewer) {
  if (answer == null || answer.isBlank()) {
    return Optional.empty();
  }
  // existing title-then-alias logic...
}
```

Prefer also guarding in `answerSpelling` so blank answers never hit the wider scan.

### WR-02: Alias accidental-match does not trim the answer (asymmetric with reviewed-note alias matching)

**File:** `backend/src/main/java/com/odde/doughnut/services/WikiLinkResolver.java:53-56`
**Issue:** Reviewed-note alias matching via `FrontmatterAliases.matchesFromNoteContent` trims the answer (`DisplayNamePathSeparators.trimSurroundingWhitespace`) before compare. Accidental alias lookup passes the raw answer into `normalizedLookupKey`, which does NFKC+lower but **does not trim**. Stored `alias_lookup_key` values are built from trimmed alias display text (`NoteAliasIndexService.refreshForNote`). An answer like `" AccidentalAliasMatch "` therefore: is wrong for the reviewed note (if that note lacks the alias), fails the alias-index lookup, and falls through to plain wrong (`failed()` / -20 / +12h) instead of `ACCIDENTAL_MATCH` / `partialFail()`.
**Fix:** Trim before normalizing (same helper as alias matching):

```java
private List<Note> aliasAccidentalCandidates(String answer) {
  String trimmed = DisplayNamePathSeparators.trimSurroundingWhitespace(answer);
  if (trimmed.isBlank()) {
    return List.of();
  }
  String lookupKey = FrontmatterAliases.normalizedLookupKey(trimmed);
  // existing repository + dedupe...
}
```

Consider applying the same trim to the title leg if product intent is “exact after trim”; today title matching is also untrimmed (consistent with wiki-link title SQL, but still asymmetric with alias *judge* behavior).

## Info

### IN-01: `hasExceededWrongAnswerThreshold` result discarded on accidental path

**File:** `backend/src/main/java/com/odde/doughnut/services/MemoryTrackerService.java:299-304`
**Issue:** `markAsAccidentalMatch` calls `hasExceededWrongAnswerThreshold` and ignores the boolean. The method has no side effects, so the call is dead. Threshold is correctly observable via `isThresholdExceeded` (and proven by the controller test), matching how `answerSpelling` also ignores `markAsRecalled`’s return value—but the accidental helper looks like it “runs” threshold logic when it does nothing.
**Fix:** Remove the unused call, or make `markAsAccidentalMatch` return the boolean for symmetry with `markAsRecalled` if a future caller needs it.

### IN-02: Unused `answerSpelling(MemoryTracker, …)` overload lacks accidental-match grading

**File:** `backend/src/main/java/com/odde/doughnut/services/MemoryTrackerService.java:206-225`
**Issue:** A second public `answerSpelling` overload (takes `MemoryTracker`, creates a new `RecallPrompt`) still always routes through `markAsRecalled` with no `findAccidentalMatch` branch. Production `RecallPromptController` uses only the `RecallPrompt` overload, so this is currently dead—but it is a footgun if revived.
**Fix:** Delete the unused overload, or wire the same accidental-match branch / mark it `@Deprecated` with a clear comment.

### IN-03: Near-duplicate alias candidate collection

**File:** `backend/src/main/java/com/odde/doughnut/services/WikiLinkResolver.java:53-69` vs `174-191`
**Issue:** `aliasAccidentalCandidates` duplicates the ArrayList/HashSet dedupe pattern of `aliasTargetCandidates` (notebook-scoped). Drift risk if one path changes filtering/dedupe rules.
**Fix:** Extract a shared `distinctNotesFromAliasRows(List<NoteAliasIndex> rows)` helper when convenient.

---

_Reviewed: 2026-07-24T00:56:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
