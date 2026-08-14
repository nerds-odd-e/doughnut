# Accidental-match confusion adjustment

**Status:** in progress (Phase 2 next)  
**Type mix:** Behavior and Structure  
**Context:** [CONTEXT.md](CONTEXT.md)

Each phase is stop-safe and carries one observable behavior or one structure
change for the immediately following behavior. Execute test-first. Before
closing any phase: Jidoka, post-change-refactor, update this plan, run the
targeted checks, then commit and push as required by repository policy.

## Goal

A non-overlap accidental spelling match fully fails the prompted tracker and,
when exactly one matched note has an eligible active tracker, weakens that
tracker without recording a recall of it. Declared overlap changes neither
tracker and still permits retry.

## Phase index

| # | Type | Status | One outcome |
|---|---|---|---|
| 1 | Behavior | done | Accidental match fully fails the prompted spelling tracker |
| 2 | Structure | planned | An answer can durably identify one confusion-adjusted tracker |
| 3 | Behavior | planned | A unique matched spelling tracker receives the weaker adjustment without recall credit |
| 4 | Behavior | planned | A unique matched understanding tracker is used when no active spelling tracker exists |
| 5 | Behavior | planned | Ambiguous matches adjust none of the matched trackers |
| 6 | Behavior | planned | Declared overlap leaves both trackers unchanged |

## Phase 1 — Full failure for the prompted spelling tracker

- **Type:** Behavior
- **Status:** done
- **Shipped:** Accidental match still sets `ACCIDENTAL_MATCH` and returns
  matched notes for the reveal, then falls through to ordinary
  `markAsRecalled` / `recallFailed` for A (index 180, `recallCount + 1`,
  grade-time `lastRecalledAt`, 12-hour relearning). Removed unused
  `MemoryTracker.markAsAccidentalMatch` and `ForgettingCurve.partialFail`.

**Learning for Phase 3:** Do not resurrect `markAsAccidentalMatch`. The weaker
confusion adjustment must be a new tracker operation that does **not**
advance `lastRecalledAt` or `recallCount`.

## Phase 2 — Durable causal link to one adjusted tracker

- **Type:** Structure
- **Status:** planned
- **Enables:** Phase 3 only.
- **Structure change:** `quiz_answer` can optionally reference the one memory
  tracker that received a secondary confusion adjustment; the relationship is
  internal and nullable.

### Work

1. Use the next available Flyway version to add an indexed nullable
   `confusion_adjusted_memory_tracker_id` relationship. Select a delete rule
   that preserves existing tracker-deletion behavior rather than introducing a
   restricting FK.
2. Map the relationship on `Answer` without expanding the learner-facing JSON
   or generated API surface. Do not repurpose transient `matchedNoteId`, which
   serves the immediate accidental-match response.
3. Add one persistence-focused backend test proving the optional relationship
   survives flush/reload and remains absent for ordinary answers.
4. Regenerate `docs/database-erd.md` with the `database-erd` skill.

### Verification

- `CURSOR_DEV=true nix develop -c pnpm backend:verify`
- `CURSOR_DEV=true nix develop -c pnpm export:database-erd`

**Stop-safe value:** The nullable schema and mapping change no grading behavior
and are solely the immediate foundation for Phase 3.

## Phase 3 — Weaken a unique matched spelling tracker

- **Type:** Behavior
- **Status:** planned
- **Precondition:** A non-overlap accidental answer for A matches exactly one
  accessible B; the learner has an active note-level spelling tracker for B.
- **Trigger:** Submit the spelling answer for A.
- **Postcondition:** A receives Phase 1's full failure. B's spelling strength is
  reduced by the weaker adjustment and its due projection is no later, while
  B's `lastRecalledAt`, `recallCount`, and failed-recall count remain unchanged.
  The answer durably references B's adjusted tracker.

### Test-first work

1. Add a controller-boundary test with real A and B trackers. Capture B's
   strength, due time, recall anchor, count, and threshold count; confirm the
   test fails before production changes.
2. Add a capability-named E2E scheduling scenario that records B's visible
   Memory Tracker state, submits the accidental answer for A, and observes that
   B is brought forward without recall credit.
3. Add a cohesive memory-tracker operation for confusion adjustment: weaker
   strength reduction, due projection from the unchanged anchor, and an
   explicit no-later-than-existing-due invariant.
4. Select B only when `matches.size() == 1`, prefer its active spelling tracker,
   apply the adjustment, and persist the answer relationship within the
   existing transaction.
5. Cover the strength floor and a B tracker that is already due so the
   adjustment cannot postpone it. Retain the existing unreadable-note boundary.

### Verification

- `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
- Targeted Cypress spec for accidental-match scheduling.

**Stop-safe value:** The most direct two-note confusion case is represented
without fabricating a recall of B.

## Phase 4 — Fall back to the understanding tracker

- **Type:** Behavior
- **Status:** planned
- **Precondition:** The accidental answer uniquely matches accessible B; B has
  no active spelling tracker and has an active note-level understanding tracker.
- **Trigger:** Submit the spelling answer for A.
- **Postcondition:** Apply the same secondary confusion adjustment to B's
  understanding tracker. A still receives full failure.

### Test-first work

1. Add the controller-boundary fallback case and confirm it fails while only
   spelling trackers are eligible.
2. Add the corresponding E2E scenario using an understanding-assimilated B and
   observe its earlier due projection with unchanged recall count/anchor.
3. Generalize target selection only enough to prefer active spelling, then
   active note-level understanding.
4. Cover eligibility deltas at the same boundary: active spelling wins when
   both exist; removed/deleted spelling falls back to active understanding;
   property and commissioned trackers are never selected; no eligible tracker
   means no tracker is created or linked.

### Verification

- `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
- Targeted Cypress spec for accidental-match scheduling.

**Stop-safe value:** Learners who track B's understanding but not its spelling
still receive the weaker confusion signal on the best existing tracker.

## Phase 5 — Leave ambiguous matched trackers unchanged

- **Type:** Behavior
- **Status:** planned
- **Precondition:** The accidental spelling answer matches two or more
  accessible notes with eligible trackers.
- **Trigger:** Submit the answer for A.
- **Postcondition:** A receives full failure and the resolve UI may list all
  matches, but no matched tracker receives a confusion adjustment and the
  answer has no adjusted-tracker relationship.

### Test-first work

1. Replace the controller test that treats the lowest note ID as the selected
   match with assertions that all matches remain visible but neither eligible
   tracker changes.
2. Add an E2E scenario with two visible matches and verify the learner can see
   both without either schedule changing.
3. Keep ordering only as presentation behavior; do not let ordering select a
   scheduling target.

### Verification

- `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
- Targeted Cypress spec for accidental-match scheduling/reveal.

**Stop-safe value:** Doughnut never penalizes an arbitrary note merely because
its database ID sorts first.

## Phase 6 — Preserve declared-overlap neutrality

- **Type:** Behavior
- **Status:** planned
- **Precondition:** A explicitly declares overlap with accessible B, and the
  submitted spelling answer is accepted by both; both have active trackers.
- **Trigger:** Submit the shared answer.
- **Postcondition:** The answer is `OVERLAP`; neither tracker changes strength,
  due time, recall anchor, recall count, or failure count; no confusion target
  is linked.

### Test-first work

1. Extend `RecallPromptOverlapTryAgainTests` to capture and assert both tracker
   states, not only A.
2. Extend the existing overlap E2E scenario to observe both schedules. Keep the
   existing distinguishing-answer retry scenario green as a regression
   guardrail, not as an additional phase outcome.
3. Keep overlap detection ahead of accidental-match adjustment. Make a
   production change only if the stronger regression tests expose leakage.

### Verification

- `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
- `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/recall/overlap_try_again.feature`

**Stop-safe value:** Explicitly related, non-distinguishing notes are protected
from both the primary failure and the new secondary adjustment.

## Cross-phase constraints

- Do not hand-edit generated OpenAPI/TypeScript artifacts. The planned internal
  relationship should be JSON-hidden; if the wire shape changes unexpectedly,
  use the `generate-api-client` skill and regenerate.
- Keep answer outcome, matched-note reveal, and resolution actions backward
  compatible.
- Keep one intentionally failing test at a time; do not end a phase with CI-red
  tests or committed red E2E.
- Backend controller tests are the stable boundary for schedule details; E2E
  asserts the main learner-visible scheduling behavior.
- After the schema phase, use the `database-erd` skill rather than editing the
  ERD by hand.
- At completion, update ADR-related research if implementation reveals a policy
  gap, then remove spent quick-plan history according to repository rules.
