---
id: SEED-071
status: dormant
planted: 2026-10-09
planted_during: owner-requested ownership review and prioritization of Donut retrospective findings
trigger_when: selecting the highest-priority project-specific retrospective follow-ups
scope: medium
---

# SEED-071: Get useful Donut feedback without recurring project setup and removal rework

## Why This Matters

Donut contributors repeat removal-proof work that conflicts with the owner's
local deletion rule, and the problem recurred across executions. Its active
evidence and priority assessment live in
[Donut Retrospective Findings](../../DonutRetrospectiveFindings.md).

## Story Decomposition

<a id="removals-follow-project-rule"></a>

### Deliver Donut removals under the project's deletion rule

**Identity:** SEED-071#removals-follow-project-rule
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** The Donut owner and contributors can delegate a removal
  without paying for a test or assertion that another agent immediately deletes.
- **Goal:** Refinement, planning, and delivery apply Donut's existing deletion
  and whole-product sweep rule consistently from the first handoff.
- **Scope:** Correct the local context or handoff gap evidenced by DD-205 in
  Donut-authored guidance and conventions. `AGENTS.md` and `CLAUDE.md` already
  carry the rule, including every role; another copy of that text alone does
  not establish the outcome. Keep them in sync when changed. Scope and slices
  express deletion, transitive cleanup, and the whole-product sweep; retained
  proof covers surviving behavior and refusals the product actively enforces.
  Shared Open Dough skills continue to allow negative assertions for actual
  promises; this work applies Donut's local removal convention.
- **Evaluation:** A fresh refinement/planning/delivery handoff for the two
  recorded shapes—deleting the dictation model-rewrite chain and deleting the
  obsolete Development PID-file mechanism—produces deletion and sweep work
  with meaningful surviving-behavior proof from the outset. An active notebook
  authorization refusal retains its refusal proof. Evaluate the handoff's
  artifacts directly rather than adding permanent absence checks to Donut.
- **Findings:** [DD-205](../../DonutRetrospectiveFindings.md#local-removal-proof).
  This is a recurrence after SEED-068#removal-rule-in-guidance, delivered in
  `18b99b6424` and `52460a4077`; two later executions contradicted its intended
  effect. The completed story is historical provenance, not a current backlog
  item or a reason to repeat its implementation.
- **Value / learning:** Eliminate repeated proof churn and keep the owner's
  local removal convention effective across agent roles.
- **Effort hypothesis:** S–M (30–120 minutes), low confidence until refinement
  identifies the handoff gap; the known fix of adding the rule is already done.
- **Depends on:** None identified.
- **Safe stopping point:** The local handoffs consistently produce deletion,
  sweep, and surviving-behavior proof while preserving active refusal coverage.

## Ordering and Scope

The owner requested the highest-priority project-specific finding groups
first. Removal-proof churn qualifies: two post-correction executions plus the
original finding establish recurrence, though the measured effect is limited
to proof written and deleted.
The MinerU environment and unreproduced stale-class findings remain unqueued.
The current Taken story and near-future direction retain their existing scope.

This is a candidate story for refinement and approach selection. This seed
provides no executable plan or implementation authorization.

## Breadcrumbs

- Owner's 2026-10-09 request to separate shared and project findings, confirm
  resolutions, group recurrence, and queue the top one or two local stories.
- [Project findings and ownership review](../../DonutRetrospectiveFindings.md).
- [Shared-process findings](../../DearDough.md).
