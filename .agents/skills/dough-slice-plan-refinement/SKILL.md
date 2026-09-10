---
name: dough-slice-plan-refinement
description: >-
  Refines an existing executable plan in place into smaller, proof-owned
  Behavior/Structure slices. Use after dough-slice-planning when slices are
  complex, cumulative design accumulates special cases, sizing confidence is low,
  or execution overruns. Creates no new plan
  and does not change the selected story outcome.
---

# Slice-plan refinement

Refine an existing executable plan in place. Do not create another plan,
implement product code, or change the selected story outcome.

## Require a refinable plan

Require an existing executable plan and this project's context required by
[dough-slice-planning](../dough-slice-planning/SKILL.md), especially any supplied
target, hard limit, exceptions, overrun policy, and the plan lifecycle.

- If no plan exists, use `dough-slice-planning`.
- If a plan is marked as awaiting story refinement after resplitting, use
  [dough-story-refinement](../dough-story-refinement/SKILL.md) on its mapped
  story and realign the plan before treating it as refinable or executable.
- If the selected story's goal, scope, or examples must change, use
  [dough-story-refinement](../dough-story-refinement/SKILL.md).
- If the parent problem, candidate selection, or sibling ordering must change,
  use [dough-story-decomposition](../dough-story-decomposition/SKILL.md).
- After the assessment below, if the cumulative design is supported and all
  remaining slices are cohesive and single-proof-loop, meet any supplied target,
  and have no unexplained path beyond a supplied hard limit,
  report `ready for direct execution`; no further refinement is required.
  Execution still requires separate authorization from the invoking workflow.

## Refine the plan

Read the plan and only the code and tests needed to judge execution boundaries.
Read and apply:

- [slice decomposition](../dough-story-decomposition/references/problem-decomposition.md#decompose-slices),
  including its cumulative design assessment, sizing, and escalation rules; and
- [active-plan refinement](../dough-story-refinement/references/planning.md#refine-the-active-plan),
  including executable proof ownership.

Preserve completed slices, applicable evidence, and the selected story's goal
and scope.

Assess the cumulative design, including how remaining examples build on
completed slices, then classify each remaining slice:

| Result | Decision |
| --- | --- |
| **Ready** | One Behavior/Structure gate, one proof loop, a cohesive path consistent with the cumulative model, and a plausible hypothesis under any supplied target |
| **Refine** | Same story, but the slice has multiple beats, unsupported special-case design, low confidence, or a supplied target or hard-limit concern |
| **Escalate** | Learning requires selected-story or parent-story review |

A suspected accidental contractual restriction uses the shared
[plan-conflict handoff](../dough-execute-plan/references/execution-decisions.md#resolve-a-disputed-plan-restriction)
before a conflicting plan edit; classify it as Escalate, not an automatically
correctable special case. Route other Escalate findings through the input gate.
Refine every remaining Refine slice.

After an overrun, confirm attempt-owned work is safely parked or reverted before
editing the plan. Stop for human judgment when ownership is unclear. Do not
commit, push, implement, or verify product behavior unless the invoking workflow
separately authorizes it.

Count the resulting plan's slices, including completed slices but excluding
obsolete replaced slices. If the count is greater than 15, report
`story resplit recommended: <count> slices; use dough-resplit-story` and link to
[dough-resplit-story](../dough-resplit-story/SKILL.md). This is a recommendation,
not an automatic invocation or a new execution-readiness gate. Do not resplit
the story or change its backlog placement during slice-plan refinement.

Report the plan path, replaced slices, resulting slice count, sizing exceptions,
any resplit recommendation, and whether execution can resume. End with:

`## SLICE PLAN REFINED`
