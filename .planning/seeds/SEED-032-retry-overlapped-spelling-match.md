---
id: SEED-032
status: dormant
planted: 2026-09-18
planted_during: product backlog capture request
trigger_when: now, as the first product backlog item
scope: small
---

# SEED-032: Retry an overlapped spelling match without revealing the answer

## Why This Matters

When a learner's spelling answer matches a note already declared as overlapping
the focus note, Donut currently reveals the focus note and then offers a “Try
again” button. The reveal gives away the expected answer, so retrying no longer
tests recall. The learner should instead remain on the same question, understand
why the submitted answer was not accepted, and be ready to try a more specific
answer immediately.

## Alternatives and Decision

Keep the learner in the unanswered question flow when the spelling answer
matches an overlapped note. Show an inline prompt explaining the overlap, clear
the submitted answer, and return focus to the answer input. The smaller
alternative is to keep the existing answer reveal and “Try again” button, but
that cannot preserve a meaningful retry because the expected answer has already
been shown.

This behavior implements the existing policy in ADR 0003: a declared Overlap
leaves both schedules unchanged and the learner retries with a more specific
answer. It does not change the scheduling policy.

## Story Decomposition

<a id="story-1"></a>

### 1. Retry an overlapped spelling match without revealing the answer

**Executable plan:**
[143-overlapped-spelling-inline-retry](../quick/143-overlapped-spelling-inline-retry/PLAN.md)
— status `planned`, one Behavior slice, not taken.

- **For / why:** A learner whose answer names an overlapped note should be able
  to continue recalling the focus note without having its answer revealed.
- **Evaluation:** Given an unanswered spelling question whose focus note has a
  declared overlap with another note, when the learner enters an answer that
  matches that overlapped note, the same question remains visible without the
  focus note's answer being revealed; Donut prompts “Your answer matches an
  overlapped note, but that’s different from the expected answer”; the previous
  answer is cleared; and focus is in the answer input for the next attempt.
- **Value / learning:** Preserves the value of the recall attempt while making
  the reason for rejecting an otherwise matching answer clear.
- **Effort hypothesis:** S — medium confidence; Donut already recognizes this
  overlap outcome, but the current answer-reveal flow may couple the UI behavior
  to answer submission.
- **Depends on:** none.
- **Safe stopping point:** The learner can retry the same unrevealed question,
  and neither the focus note nor the overlapped matched note receives a Grade,
  Confusion, or any other memory-state transition.

## Removal Constraint

Delete the answered-note reveal, “Try again” button, and button-driven retry
behavior for declared overlaps. Delete tests and assertions whose purpose was
to verify those removed elements or interactions. Do not replace them with
assertions that the removed UI is absent; prove the new flow through its
positive signals: the same question, explanatory prompt, cleared answer, and
focused input.

## Ordering and Scope Reduction

This is one independently useful correction to the overlapped spelling-answer
flow and is explicitly the first product backlog item. Ordinary incorrect
answers and accidental matches without a declared overlap retain their existing
behavior.

## Open Decisions

None.

## When to Surface

Now — the current reveal makes the offered retry meaningless.

## Breadcrumbs

- Requested on 2026-09-18: keep the same spelling question visible and
  unrevealed after an overlapped-note match, show the explanatory prompt, clear
  the previous answer, and focus the answer input.
- [ADR 0003: Spaced-repetition scheduling policy](../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md)
  defines declared Overlap as leaving both schedules unchanged while the learner
  retries with a more specific answer.
