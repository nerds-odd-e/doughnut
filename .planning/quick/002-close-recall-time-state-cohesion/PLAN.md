# Close recall-time state cohesion (C1)

**Status:** In progress — Phases 1–10 complete; Phase 11 next
**Plan type:** Ad-hoc phased delivery
**Created:** 2026-08-13
**Refined:** 2026-08-13 for small, green commit boundaries
**Policy:** [ADR 0003](../../../docs/adrs/0003-spaced-repetition-scheduling-policy.md)
**Gap analysis:** [FSRS compatibility gap](../../research/FSRS-COMPATIBILITY-GAP.md)

## Goal

After C1, `MemoryTracker` is the cohesive persisted aggregate for a recall
transition. It computes whole elapsed hours from its own `lastRecalledAt`, and
every trustworthy, state-changing recall leaves that anchor at the grade time.
Legacy anchors are repaired from trustworthy Answer and Tutor Feedback
timestamps without changing `nextRecallAt`.

## Scope boundary

### Required

1. Correct scheduling consumes whole elapsed hours since `lastRecalledAt`, not
   deviation from persisted or recomputed due time.
2. Whole-hour precision discards the sub-hour remainder and does not depend on
   calendar-day boundaries.
3. Correct, incorrect, accidental match, and Tutor Feedback advance
   `lastRecalledAt` to their grade time.
4. Overlap and missing Tutor Feedback do not change tracker scheduling state.
5. Incorrect recall keeps its current strength penalty and 12-hour retry while
   becoming the anchor for the next recall.
6. Legacy anchors become the maximum of current `lastRecalledAt`, latest
   trustworthy non-overlap `Answer.createdAt`, and latest
   `SessionItem.feedbackRecordedAt`.
7. Backfill never moves an anchor backward and never changes `nextRecallAt`.

### Explicitly out of C1

- `RecallLog`
- Stability/Difficulty migration
- `lapses`
- renaming `nextRecallAt` or `recallCount`
- FSRS overdue reward (C2)
- strictly-future scheduling fallback (C3)
- a special same-day/short-term formula
- replaying history or rebuilding due projections

## Current-code discoveries

1. For early recalls, the current due-relative success formula is algebraically
   `standardIncrement × elapsedHours / currentIntervalHours`; it can be renamed
   and reshaped without changing behavior.
2. Correct scheduling already ignores persisted `nextRecallAt`.
3. `TimestampOperations.getDiffInHours` already discards a positive sub-hour
   remainder.
4. Correct, accidental-match, and commissioned-feedback paths already advance
   the anchor. Ordinary incorrect is the live code defect.
5. MCQ and spelling incorrect routes share `MemoryTracker.markAsRecalled`, and
   both currently contain tests asserting the old stale-anchor behavior. The
   production fix and those two test corrections must land together.
6. Overlap returns before tracker mutation. A report with no accepted Tutor
   Feedback creates no `SessionItem` and performs no commissioned transition.
7. Normal correct/incorrect Answers have null `outcome`; accidental matches use
   `ACCIDENTAL_MATCH`; `OVERLAP` must not be a migration input.
8. The current highest Flyway migration is `V300000247`. Recheck immediately
   before execution; the refined plan currently uses 248–250.

## Phase sizing and commit policy

1. Each numbered phase below is one intended green commit and push.
2. Each phase targets roughly five minutes of editing plus its required focused
   verification. The repository-mandated full backend test command may itself
   exceed that budget; test runtime alone is not a reason to combine commits.
3. Test-only regression phases are intentional: they lock already-shipped C1
   behavior before or after the shared transition changes.
4. Red is observe-only inside a Behavior phase. Never commit the failing test;
   finish its red→green cycle before the phase commit.
5. If a phase unexpectedly requires another production behavior or cannot turn
   green within ten minutes excluding an already-running required test command,
   stop, revert that phase's WIP, and split it again.

## Design decisions

1. `MemoryTracker` reads its own persisted anchor and computes elapsed hours.
   Callers supply grade time/outcome, not a precomputed timing interpretation.
2. C1 preserves existing early, on-time, overdue, effort, failure-penalty, and
   12-hour retry semantics except for repairing the failed-recall anchor.
3. Route-specific regression coverage is added in separate small commits after
   the shared ordinary behavior is green.
4. Legacy repair is split by persisted source/outcome so every migration is a
   complete, immutable, stop-safe behavior:
   - normal Answer (`outcome IS NULL`);
   - accidental-match Answer;
   - Tutor Feedback.
   Sequential “advance only” updates produce the required maximum.
5. Repair `UPDATE`s use a dedicated `${recall_anchor_repair}` Flyway placeholder
   that defaults to no-op and is enabled deliberately for the shipping deploy.

## Phase overview

| Phase | Type | Status | One commit outcome |
|------:|------|--------|--------------------|
| 1 | Behavior | Done | Correct recall is proven independent of persisted due projection |
| 2 | Behavior | Done | Correct recall is proven to use whole-hour precision |
| 3 | Structure | Done | Success transition speaks elapsed hours without schedule changes |
| 4 | Behavior | Done | Ordinary incorrect recall becomes the new anchor |
| 5 | Behavior | Done | A correct recall after failure is proven to use the failure anchor |
| 6 | Behavior | Done | Accidental match is protected as an anchor-moving outcome |
| 7 | Behavior | Done | Overlap is protected as a no-anchor-mutation outcome |
| 8 | Behavior | Done | Recorded Tutor Feedback is protected as an anchor-moving outcome |
| 9 | Behavior | Done | Missing Tutor Feedback is protected as a no-mutation outcome |
| 10 | Structure | Done | A default-off repair gate enables only the next repair behavior |
| 11 | Behavior | Planned | Legacy normal Answers repair stale anchors |
| 12 | Behavior | Planned | Legacy accidental-match Answers repair stale anchors; overlap does not |
| 13 | Behavior | Planned | Legacy Tutor Feedback repairs stale anchors |

## Phase 1 — Protect due-projection independence

**Type:** Behavior (existing-behavior regression)
**Status:** Done

- **Precondition:** Two trackers have identical anchor, strength, user spacing,
  and correct grade time, but different persisted `nextRecallAt` projections.
- **Trigger:** Both receive the same correct recall.
- **Postcondition:** Their resulting schedule intervals are equal.

**Commit scope:** Add one aggregate-boundary schedule test. Do not change
production code or assert an index value.

**Likely test:** capability-named aggregate scheduling test, reusing
`SpacedRepetitionEarlyRecallAdjustmentTest` only if it remains cohesive.

**Verification:** `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

**Learning:** An asymmetric pair of persisted projections is required so a
due-deviation implementation using an absolute value cannot satisfy the
regression accidentally. The existing transition passed that stronger test.

**Stop-safe:** A shipped invariant gains regression coverage; no behavior or
structure changes.

## Phase 2 — Protect whole-hour precision

**Type:** Behavior (existing-behavior regression)
**Status:** Done

- **Precondition:** Two otherwise identical trackers are recalled after the
  same number of whole hours, with one recall also carrying a sub-hour remainder.
- **Trigger:** Both receive a correct grade.
- **Postcondition:** Their resulting schedule intervals are equal; their exact
  `lastRecalledAt` timestamps still reflect their respective grade times.

**Commit scope:** Add one focused schedule test beside Phase 1's test. No
production change.

**Verification:** `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

**Learning:** The existing transition produced the same schedule after 300
hours and after 300 hours 30 minutes while retaining each exact grade timestamp.

**Stop-safe:** The C1 precision chosen in Proposed ADR 0003 is executable before
terminology and formula structure change.

## Phase 3 — Express success in elapsed hours

**Type:** Structure
**Status:** Done
**Enables only:** Phase 4

**Structural outcome:** `MemoryTracker` computes `elapsedInHours` from its
persisted `lastRecalledAt`; `ForgettingCurve.succeeded` consumes elapsed hours
and preserves the current early formula. Due-delay terminology disappears from
the touched production/test API.

**Commit scope:**

- `MemoryTracker.java`
- `ForgettingCurve.java`
- mechanical test naming/argument updates required to compile

Do not change any schedule result or persisted field.

**Verification:** Phases 1–2 remain green; run
`CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

**Learning:** Rewriting the early adjustment as
`(elapsedInHours - currentIntervalInHours) / currentIntervalInHours` preserved
all 1,691 backend tests while letting `MemoryTracker` own elapsed-time
calculation from its persisted anchor.

**Stop-safe:** No external behavior changes. The structure is used immediately
by the next phase's failed-recall anchor behavior.

## Phase 4 — Make ordinary incorrect recall the anchor

**Type:** Behavior
**Status:** Done

- **Precondition:** An ordinary tracker has an earlier recall anchor.
- **Trigger:** The learner receives an incorrect MCQ or spelling grade at `t`.
- **Postcondition:** `lastRecalledAt = t`, the current strength penalty remains,
  and `nextRecallAt = t + 12 hours`.

**Red→green commit scope:**

1. Extend the existing incorrect-answer MCQ E2E so the learner observes the
   grade time as **Last Recall Time** on the tracker page.
2. Change `MemoryTracker.recallFailed` to set the anchor.
3. Replace both existing MCQ and spelling stale-anchor assertions in this same
   commit; they necessarily turn red together after the shared production line
   changes.

**Likely files:** `MemoryTracker.java`, the two existing recall-prompt controller
tests, `recall_quiz_ai_question.feature`, and its smallest existing
step/page-object support.

**Verification:**

- `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
- `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/recall/recall_quiz_ai_question.feature`

This is the only intentionally wider commit: the shared production line, both
contradictory backend assertions, and the required E2E postcondition must turn
green together to leave a phase-complete deployable boundary.

**Learning:** Both ordinary routes failed only on their stale-anchor assertions;
one aggregate mutation fixed MCQ and spelling while preserving the exact
12-hour retry. The E2E observes the grade anchor locale-independently as twelve
hours before the displayed next recall. Its new tracker steps required a
capability-named extraction to keep step files under 250 lines.

**Stop-safe:** All new ordinary grades have a correct anchor; legacy data remains
unchanged until later repair phases.

## Phase 5 — Protect the next correct recall after failure

**Type:** Behavior (new-behavior regression)
**Status:** Done

- **Precondition:** A tracker has an old anchor, receives an incorrect grade at
  `t`, then waits a known whole-hour duration.
- **Trigger:** It receives a correct grade.
- **Postcondition:** The resulting schedule reflects elapsed hours since `t`,
  not elapsed hours since the pre-failure anchor.

**Commit scope:** Add one aggregate-boundary incorrect→correct schedule test. No
production change unless Phase 4 failed to deliver the intended behavior.

**Verification:** `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

**Learning:** With the repaired failure anchor, a correct recall 24 hours after
failure schedules a 96-hour interval; removing the anchor mutation produced 144
hours from the stale pre-failure timestamp, confirming the regression's reach.

**Stop-safe:** The cross-recall reason for C1 is protected independently of
entry-point tests.

## Phase 6 — Protect accidental-match anchoring

**Type:** Behavior (existing-behavior regression)
**Status:** Done

- **Precondition:** A spelling tracker has an earlier anchor and the answer
  accidentally names another accessible note.
- **Trigger:** Doughnut grades `ACCIDENTAL_MATCH` at `t`.
- **Postcondition:** `lastRecalledAt = t` while the existing partial-failure
  schedule semantics remain unchanged.

**Commit scope:** Add the missing anchor assertion to the existing accidental
match controller capability. No production change unless the regression exposes
a real defect.

**Verification:**

- `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
- `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/recall/accidental_match_reveal.feature`

**Learning:** The existing accidental-match transition already advanced the
anchor; mutation-checking that line made only the new controller assertion fail,
while the targeted feature remained green at 5/5.

**Stop-safe:** A specialized state-changing outcome gains explicit C1 coverage.

## Phase 7 — Protect overlap as no anchor mutation

**Type:** Behavior (existing-behavior regression)
**Status:** Done

- **Precondition:** A spelling tracker has a declared overlap and existing
  scheduling fields.
- **Trigger:** Doughnut grades the answer as `OVERLAP`.
- **Postcondition:** `lastRecalledAt` remains unchanged alongside the already
  asserted unchanged count, strength, and due projection.

**Commit scope:** Add one assertion to the existing overlap controller scenario.
No production change unless the regression exposes a defect.

**Verification:**

- `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
- `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/recall/overlap_try_again.feature`

**Learning:** The existing overlap route already left the anchor unchanged;
forcing an anchor mutation made exactly the new assertion fail, and the targeted
feature remained green at 2/2.

**Stop-safe:** The primary no-grade exception gains explicit C1 coverage.

## Phase 8 — Protect Tutor Feedback anchoring

**Type:** Behavior (existing-behavior regression)
**Status:** Done

- **Precondition:** A commissioned tracker has an earlier anchor.
- **Trigger:** A Tutor report records a score at `t`.
- **Postcondition:** The tracker has `lastRecalledAt = t` with its existing
  score-dependent schedule behavior.

**Commit scope:** Add a focused anchor assertion to
`LearningSessionRecordTests`. Do not retest the full score table.

**Verification:**

- `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
- `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/learning_session/commissioned_learning_session.feature`

**Learning:** The canonical record test now proves accepted Feedback records
both its own timestamp and the commissioned tracker's anchor at the same grade
time; restoring only the old anchor made exactly this assertion fail.

**Stop-safe:** The second grading source gains explicit C1 coverage.

## Phase 9 — Protect missing Feedback as no mutation

**Type:** Behavior (existing-behavior regression)
**Status:** Done

- **Precondition:** A commissioned tracker has an existing anchor and a Tutor
  report contains no accepted Feedback for it.
- **Trigger:** Doughnut rejects/abandons the ungraded report entry.
- **Postcondition:** `lastRecalledAt`, count, strength, and due projection remain
  unchanged.

**Commit scope:** Extend the existing all-lines-rejected learning-session test
with one cohesive tracker-state assertion. No production change unless it finds
a defect.

**Verification:**

- `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
- `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/learning_session/commissioned_learning_session.feature`

**Learning:** The all-lines-rejected controller path now snapshots anchor,
count, strength, and due projection together; a deliberately injected Feedback
transition changed all four and failed exactly this assertion.

**Stop-safe:** Both sides of the trustworthy-grade boundary are now covered.

## Phase 10 — Add the recall-anchor repair gate

**Type:** Structure
**Status:** Done
**Enables only:** Phase 11

**Structural outcome:** A dedicated `${recall_anchor_repair}` Flyway placeholder
resolves to `1=0` in every default/test profile and to `1=1` in the deliberate
production repair deployment. No migration consumes it yet.

**Commit scope:**

- add the placeholder beside existing Flyway placeholders in `application.yml`;
- add production enablement beside the existing repair property in
  `mig-zulu25-openai-app-instance-startup.sh`.

Before Phase 11's production push, Jidoka requires the affected-row count and an
export of `(id, last_recalled_at, next_recall_at)` for recovery.

**Production snapshot:** 130 affected normal-Answer trackers on 2026-08-13.
Recovery export stored outside the repository at
`/Users/terryyin/.codex/recovery/doughnut/recall-anchor/2026-08-13T103119Z/normal-answer-anchors-before-repair.tsv`
(mode `600`, SHA-256
`1ef465ced5d60a02b3951021bbe25e922d59f6648bbbdecf6dd0ec5b353df240`).

**Verification:** `CURSOR_DEV=true nix develop -c pnpm backend:verify`.

**Learning:** The new placeholder defaults off in every application profile and
is enabled only by the production MIG command, whose focused script test went
red→green. Extracting the production profile to `application-prod.yml` kept
configuration files below 250 lines without changing profile behavior.

**Stop-safe:** No behavior or data changes; this structure enables only the
immediately following normal-Answer repair.

## Phase 11 — Repair anchors from normal Answers

**Type:** Behavior
**Status:** Planned

- **Precondition:** A legacy tracker has one or more normal correct/incorrect
  Answers (`outcome IS NULL`) newer than its stored anchor.
- **Trigger:** The enabled normal-Answer repair migration runs.
- **Postcondition:** `last_recalled_at` advances to the latest normal Answer;
  a later current anchor and `next_recall_at` remain unchanged.

**Commit scope:** Recheck Flyway versions, then currently add:

- `V300000248__repair_memory_tracker_recall_anchor_from_answers.sql`
- a capability-named migration test covering default no-op, latest normal
  Answer, later-current preservation, unchanged due, and a second idempotent run

The SQL selects only `outcome IS NULL` and includes
`WHERE ${recall_anchor_repair}`.

**Verification:** `CURSOR_DEV=true nix develop -c pnpm backend:verify`. No ERD
regeneration because the schema is unchanged.

**Stop-safe:** The common legacy path is repaired. Accidental and commissioned
history remain unchanged but no worse than before.

## Phase 12 — Repair accidental-match anchors and exclude overlap

**Type:** Behavior
**Status:** Planned

- **Precondition:** A legacy tracker has an `ACCIDENTAL_MATCH` Answer newer than
  its current/Phase-11-repaired anchor; another may have only newer `OVERLAP`.
- **Trigger:** The enabled accidental-match repair migration runs.
- **Postcondition:** The accidental-match anchor advances; overlap-only history
  does not move an anchor; later current state and due remain unchanged.

**Commit scope:** Recheck Flyway versions, then currently add:

- `V300000249__repair_memory_tracker_recall_anchor_from_accidental_matches.sql`
- focused enabled/default/idempotence cases in the existing recall-anchor
  migration test capability

The SQL selects exactly `outcome = 'ACCIDENTAL_MATCH'` and uses the same gate.

**Verification:** `CURSOR_DEV=true nix develop -c pnpm backend:verify`.

**Stop-safe:** All currently trustworthy Answer outcomes are repaired; overlap
remains excluded.

## Phase 13 — Repair anchors from Tutor Feedback

**Type:** Behavior
**Status:** Planned

- **Precondition:** A legacy tracker has `SessionItem.feedbackRecordedAt` newer
  than its current/Answer-repaired anchor.
- **Trigger:** The enabled Tutor-Feedback repair migration runs.
- **Postcondition:** `last_recalled_at` advances to the latest Feedback; a later
  current anchor and `next_recall_at` remain unchanged.

**Commit scope:** Recheck Flyway versions, then currently add:

- `V300000250__repair_memory_tracker_recall_anchor_from_tutor_feedback.sql`
- focused default/enabled/latest/idempotence cases in the recall-anchor migration
  test capability

**Verification:**

- `CURSOR_DEV=true nix develop -c pnpm backend:verify`
- rerun the Phase-4 targeted recall E2E as the final C1 behavior check
- no ERD regeneration because the schema is unchanged

**Stop-safe:** All selected trustworthy legacy sources have been folded into the
persisted aggregate. C1 is complete without history replay or due rewriting.

## C1 completion checklist

- [ ] Due projection cannot influence a correct memory transition.
- [ ] Correct transition uses whole elapsed hours.
- [ ] Success API and formula use elapsed-time language.
- [ ] Ordinary incorrect recall advances the anchor and retains its 12-hour retry.
- [ ] Correct, accidental, and Tutor paths have anchor coverage.
- [ ] Overlap and missing Feedback have no-mutation coverage.
- [ ] Normal Answer, accidental-match Answer, and Tutor Feedback repairs each
      advance only to a newer trustworthy timestamp.
- [ ] All repair migrations are default no-ops, enabled deliberately,
      idempotent, and leave `nextRecallAt` unchanged.
- [ ] Backend verification and the targeted recall E2E pass.
- [ ] C2, C3, Stability/Difficulty, lapses, and `RecallLog` remain absent.

## Execution discipline

When execution is explicitly requested, use `execute-plan`. At every phase:
Jidoka before/after, complete one green cycle, run relevant tests,
`post-change-refactor`, update this plan, commit, push, and let the deploy gate
complete before starting the next phase. When C1 is fully represented in code,
migrations, tests, and permanent docs, clean up this spent quick-plan history.
