# Plan: Requested-retention leftover cohesion

**Status:** in-progress  
**Goal:** Same schedule as after 016. Drop redundant due pins. Put grade due and confusion projection back on `MemoryTracker`.

Locked: [CONTEXT.md](./CONTEXT.md). Capability names only.

Tests: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`. When Gherkin changes: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/recall/accidental_match_reveal.feature,e2e_test/features/recall/recall_quiz_ai_question.feature,e2e_test/features/recall/spaced_repetition.feature`.

Wrap-up per `planning.mdc`. Do not accept ADR 0003.

---

### 1. Drop redundant fail-due pins
Type: Structure  
Status: done

Enables slice 2: scheduling cohesion without leftover tests pinning a second due source.

Existing tests still pass; no schedule change.

Learnings: Unique claim on post-lapse incorrect is **S=17** only — also dropped leftover last-recall/count pins from that unit test. Canonical due/last-recall stay on the accidental-match controller pin and `nextRecallAtIsLastRecalledAtPlusStabilityHours`. Deleted the mismatched 24h Last Recall Time E2E step and accidental-match schedule scenario.

---

### 2. Grade due and confusion projection live on MemoryTracker
Type: Structure  
Status: planned

No further Behavior. Existing tests still pass.

- Move `JPA_WHERE_*` fragments off `MemoryTracker` (they must stay aligned with `isNoteLevelTracker` / understanding type — one query-fragment type, not scheduling).
- Inline `MemoryTrackerNextRecallScheduling.apply` into `scheduleNextRecallFromStability` and `MemoryTrackerConfusionAdjustment.apply` into `adjustForConfusion`. Delete those two classes.
- Strictly-future fallback hours: `Fsrs.intervalHours(ForgettingCurve.FIRST_SUCCESS_STABILITY_HOURS)`, not a second `Math.round`.
- `MemoryTracker` stays ≤ 250 lines. Do not extract new 17-line apply types to hit the cap.

---

## Stop-safe

| Stop after | User-visible |
|------------|----------------|
| 1 | Same schedule; fewer overlapping due assertions |
| 2 | Same schedule; one MemoryTracker due/confusion path |

## Not this plan

Accept ADR 0003. Settings / UI for `r`. Varying-`r` tests. Delete `intervalHours`. Drop just-review E2E 8-hour pin. Drop accidental-match controller due pin. Collapse commissioned score dispatch.
