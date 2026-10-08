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

Donut contributors repeatedly lose behavioral feedback to the repository's
frontend formatting gate and repeat removal-proof work that conflicts with
the owner's local deletion rule. Both problems recurred across executions.
Their active evidence and priority assessment live in
[Donut Retrospective Findings](../../DonutRetrospectiveFindings.md).

## Story Decomposition

<a id="e2e-proof-before-final-formatting"></a>

### Run Donut E2E proof before final formatting

**Identity:** SEED-071#e2e-proof-before-final-formatting
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** Donut contributors get behavioral feedback from the ordinary
  local Cypress wrapper while a frontend change is still being implemented.
- **Goal:** A valid frontend behavior change reaches its selected E2E scenarios
  even when its touched source files still need mechanical formatting.
- **Scope:** Correct Donut's coupling between the Vite/Biome checker and E2E
  startup through the repository-owned frontend configuration, SUT/Cypress
  entry points, and local command guidance as needed. Preserve the existing
  lint/typecheck quality gate and coordinator-owned final formatting. Keep
  genuine compilation, application-readiness, and scenario failures visible.
  The published Open Dough execution workflow stays the governing workflow;
  this story repairs Donut's test-startup behavior.
- **Evaluation:** With a behaviorally valid but unformatted touched frontend
  file, `pnpm cy:run --spec <selected mocked-service feature>` reaches and
  passes the applicable scenarios without rewriting source files. A real
  application or scenario error still fails visibly, and the ordinary quality
  gate still rejects its applicable violations until corrected.
- **Findings:** [ODF-215 / former DD-211](../../DonutRetrospectiveFindings.md#e2e-proof-formatting).
  Three executions on October 5–7 encountered the startup gate; the last
  repeated it after an earlier delegation-only workaround.
- **Value / learning:** Restore reliable early feedback for frontend work,
  including the near-future voice-input stories.
- **Effort hypothesis:** S–M (30–120 minutes), medium confidence; confirm the
  ordinary startup path during refinement before choosing a configuration fix.
- **Depends on:** None identified; the commit-hook performance story and
  spoken-title rename story can proceed independently.
- **Safe stopping point:** The ordinary Donut E2E entry point gives behavioral
  feedback during implementation and the existing delivery checks retain
  their authority.

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
- **Depends on:** None identified; independent of the E2E-startup story.
- **Safe stopping point:** The local handoffs consistently produce deletion,
  sweep, and surviving-behavior proof while preserving active refusal coverage.

## Ordering and Scope

The owner requested the highest-priority one or two project-specific finding
groups first. E2E startup ranks first because its three occurrences block all
selected behavioral scenarios before feedback begins. Removal-proof churn ranks
second: two post-correction executions plus the original finding establish
recurrence, but the measured effect is limited to proof written and deleted.
The MinerU environment and unreproduced stale-class findings remain unqueued.
The current Taken story and near-future direction retain their existing scope.

These are candidate stories for refinement and approach selection. This seed
provides no executable plan or implementation authorization.

## Breadcrumbs

- Owner's 2026-10-09 request to separate shared and project findings, confirm
  resolutions, group recurrence, and queue the top one or two local stories.
- [Project findings and ownership review](../../DonutRetrospectiveFindings.md).
- [Shared-process findings](../../DearDough.md).
