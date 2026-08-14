# Accidental-match confusion adjustment — context

**Status:** Planned; implementation not started  
**Decision source:** Proposed [ADR 0003](../../../docs/adrs/0003-spaced-repetition-scheduling-policy.md), Decision → Accidental-match and overlap transitions

## Goal

When a spelling recall for note A is answered with the title or plain alias of
note B, Doughnut should represent both signals:

- the prompted spelling recall for A failed;
- when B is unambiguous and already tracked, the learner showed confusion
  involving B, but did not perform a recall of B.

Declared overlap remains a no-credit, no-penalty outcome with same-session
retry.

## Required outcomes

| Preconditions | Trigger | Prompted tracker A | Matched note B |
|---|---|---|---|
| Non-overlap answer matches exactly one accessible B with active spelling tracker | Submit B's title/alias for A | Ordinary incorrect transition | Weaker confusion adjustment on B's spelling tracker |
| Same, but B has no active spelling tracker and has an active note-level understanding tracker | Submit answer | Ordinary incorrect transition | Weaker confusion adjustment on B's understanding tracker |
| B has no eligible active tracker | Submit answer | Ordinary incorrect transition | No tracker created or changed |
| Answer matches multiple accessible notes | Submit answer | Ordinary incorrect transition | No matched tracker chosen or changed |
| A explicitly declares overlap with B and the answer is accepted by both | Submit answer | No state or schedule change; retry allowed | No state or schedule change |

## Scheduling invariants

### Prompted tracker

- Keep `ACCIDENTAL_MATCH` as the answer outcome so the reveal and resolution UX
  remain distinct from a plain incorrect spelling answer.
- Apply the same full memory-state failure and relearning projection as an
  ordinary incorrect recall.
- Advance `lastRecalledAt` and `recallCount` and include the answer in the
  prompted tracker's failed-recall reporting.

### Secondary confusion target

- Select only among the learner's active trackers for the one accessible
  matched note.
- Prefer the note-level spelling tracker. If absent or inactive, use the active
  note-level understanding tracker.
- Never select property, commissioned, removed, or deleted trackers.
- Never create a tracker as a side effect.
- Reduce memory strength by the weaker failure adjustment.
- Recompute the due projection from the existing `lastRecalledAt`; do not move
  an already scheduled recall later.
- Do not change `lastRecalledAt`, `recallCount`, or failed-recall statistics:
  B was not directly prompted.

## Persistence and atomicity

The answer currently stores `outcome`, but `matchedNoteId` is transient and the
resolver chooses the lowest note ID when several notes match. That is sufficient
for the immediate reveal but not for changing another tracker.

Persist the single tracker that actually received a confusion adjustment as an
optional internal relationship from `quiz_answer` to `memory_tracker`. Keep the
existing matched-notes response for the resolve dialog; do not expose the
internal target relationship as a new UI contract. The answer write, prompted
failure, secondary adjustment, and causal link must commit or roll back together
under the existing transactional answer endpoint.

Choose the next available Flyway version during execution because another
active workstream may add a migration first. Use an indexed nullable foreign key
whose delete rule does not block legitimate memory-tracker deletion, and
regenerate the database ERD.

## Existing behavior and useful boundaries

- `SpellingRecallGrading` detects overlap before accidental match and performs
  all grading inside the transactional controller request.
- `WikiLinkResolver.findAllAccidentalMatches` already returns readable,
  de-duplicated notes ordered by ID.
- `MemoryTracker.recallFailed` performs the full failure and fixed 12-hour
  relearning projection.
- `MemoryTracker.markAsAccidentalMatch` currently applies the weaker adjustment
  to A while advancing its recall anchor; it must not be reused for B because
  doing so would fabricate recall credit and could postpone a due recall.
- `RecallPromptAccidentalMatchGradingTests`,
  `RecallPromptAccidentalMatchEdgeTests`, and
  `RecallPromptOverlapTryAgainTests` already drive the HTTP-stable controller
  boundary with the real database.
- Existing E2E features cover accidental-match reveal and overlap retry. A
  capability-named scheduling feature can add cross-tracker schedule
  observability without changing the reveal UX.

## Out of scope

- MCQ accidental matching, fuzzy matching, or cross-notebook typed qualifiers.
- Creating memory trackers automatically.
- Penalizing every note in an ambiguous match set.
- Property or commissioned tracker fallback.
- Reinterpreting or backfilling historical accidental-match answers.
- A general `RecallLog` or scheduler replacement.
- Redesigning the accidental-match resolve dialog.

## Worktree coordination

Quick plan `001-skip-assimilation` is finished. This plan is queued independently.
