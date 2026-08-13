---
id: SEED-004
status: dormant
planted: 2026-08-12
planted_during: ad-hoc planning session 2026-08-12
trigger_when: when scheduling, recall intervals, or SRS behavior is in scope
scope: medium-large
---

# SEED-004: Close the spaced-repetition scheduling policy gap (ADR 0003)

## Why This Matters

ADR 0003 is still **Proposed**. The **late-success penalty** (overdue correct
weakening the tracker / trapping busy learners in immediate or daily loops)
was **removed 2026-08-05** (`735b96623a`). Remaining work is FSRS-compatible
policy: success still keys off queue deviation (`gradedAt − nextRecallAt`)
instead of elapsed since last graded recall, and Doughnut does not yet grant
FSRS's bounded overdue **reward**. The ADR separates memory evidence (graded
outcome + elapsed time) from scheduling metadata (the due-time projection)
and states safety properties for correct, incorrect, accidental-match,
overlap, commissioned feedback, and effort. Implementation and tests should
close the remaining gap against that policy, not invent parallel semantics.

## When to Surface

**Trigger:** when scheduling, recall intervals, backlog behavior, or SRS
algorithm work is in scope — including human review/approval of ADR 0003.

This seed will surface during `/gsd-new-milestone` when the milestone scope
matches.

## Scope Estimate

**Medium-large** — policy-aligned changes across scheduling logic, outcome
handling (spelling accidental match / overlap), commissioned feedback scoring,
and policy tests. ADR recommends applying the policy to the existing Doughnut
model first (not a full FSRS migration).

## Breadcrumbs

- `docs/adrs/0003-spaced-repetition-scheduling-policy.md` — **authoritative policy** to implement and verify against
- `.planning/research/FSRS-COMPATIBILITY-GAP.md` — Doughnut ↔ open FSRS gap + open issues to settle before finalizing ADR 0003
- `docs/adrs/0001-ubiquitous-language.md` — spaced-repetition schedule / space setting glossary
- `docs/adrs/0005-commissioned-learning-session-protocol.md` — Tutor score meaning (ADR 0003 § commissioned feedback)
- `backend/src/main/java/com/odde/doughnut/algorithms/` — spaced-repetition math
- `backend/src/main/java/com/odde/doughnut/services/MemoryTrackerService.java` — tracker scheduling entry points
- `.planning/STATE.md` — operator item: resolve FSRS gap open issues, then finalize ADR 0003

## Notes

- ADR status is Proposed; settle FSRS-COMPATIBILITY-GAP open issues before treating 0003 as final shape.
- Approval and implementation can proceed after the written policy is stable; do not drift from it.
- Policy tests must assert observable schedule behavior, not internal strength indexes.
- Tests that require late correct answers to lose strength solely for lateness
  must stay gone (penalty already shipped-removed).
- Rebuildable due-time projection from history alone is explicitly deferred; preserve transactional consistency with the existing `nextRecallAt` projection.
