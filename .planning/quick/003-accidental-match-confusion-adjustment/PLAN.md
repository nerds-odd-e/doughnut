# Accidental-match confusion adjustment

**Status:** in progress (Phase 6 next)  
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
| 2 | Structure | done | An answer can durably identify one confusion-adjusted tracker |
| 3 | Behavior | done | A unique matched spelling tracker receives the weaker adjustment without recall credit |
| 4 | Behavior | done | A unique matched understanding tracker is used when no active spelling tracker exists |
| 5 | Behavior | done | Ambiguous matches adjust none of the matched trackers |
| 6 | Behavior | planned | Declared overlap leaves both trackers unchanged |

## Phase 1 — Full failure for the prompted spelling tracker

- **Type:** Behavior
- **Status:** done
- **Shipped:** Accidental match still sets `ACCIDENTAL_MATCH` and returns
  matched notes for the reveal, then falls through to ordinary
  `markAsRecalled` / `recallFailed` for A (index 180, `recallCount + 1`,
  grade-time `lastRecalledAt`, 12-hour relearning). Removed unused
  `MemoryTracker.markAsAccidentalMatch` and `ForgettingCurve.partialFail`.

## Phase 2 — Durable causal link to one adjusted tracker

- **Type:** Structure
- **Status:** done
- **Shipped:** `V300000256` adds indexed nullable
  `quiz_answer.confusion_adjusted_memory_tracker_id` → `memory_tracker`
  `ON DELETE SET NULL`. Mapped on `Answer` as `@JsonIgnore`;
  `matchedNoteId` stays transient. Persistence test covers flush/reload vs
  ordinary answers. ERD regenerated. No OpenAPI change.

## Phase 3 — Weaken a unique matched spelling tracker

- **Type:** Behavior
- **Status:** done
- **Shipped:** Unique accessible match with an active note-level spelling
  tracker on B: `adjustForConfusion` reduces index by 10 (floor 100),
  recomputes due from unchanged `lastRecalledAt`, and never postpones.
  Anchor, `recallCount`, and failed-recall count stay put. Answer links
  `confusionAdjustedMemoryTracker`. Selection only when `matches.size() == 1`.

## Phase 4 — Fall back to the understanding tracker

- **Type:** Behavior
- **Status:** done
- **Shipped:** `findConfusionAdjustmentTracker` prefers active note-level
  spelling, then active note-level understanding. Same `adjustForConfusion`.
  Property, commissioned, removed, and deleted trackers are never selected;
  no tracker is created when none is eligible.

## Phase 5 — Leave ambiguous matched trackers unchanged

- **Type:** Behavior
- **Status:** done
- **Shipped:** Two or more accessible matches stay listed for the reveal;
  neither eligible tracker is adjusted and the answer has no confusion
  link. ID ordering is presentation-only (`matches.size() == 1` remains
  the uniqueness gate).

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
