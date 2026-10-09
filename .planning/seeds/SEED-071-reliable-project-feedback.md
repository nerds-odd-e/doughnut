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
{"schemaVersion":1,"refinement":"refined","approach":"unselected"}
```

- **For / why:** The Donut owner and contributors can delegate a removal
  without paying for a test or assertion that another agent immediately deletes.
- **Goal:** A removal handed to Donut's refinement, planning, and delivery
  roles produces deletion, transitive cleanup, and the whole-product sweep
  with proof of the surviving behavior from the first handoff. Nobody writes,
  reviews, or deletes an observation of the removed thing. This contributes to
  cheap, trustworthy delegation of maintenance work.
- **Handoff gap (refinement finding):** Both DD-205 plans (`595e2eb5d9`,
  `cd1b2f1694`) were written after principle 7 landed and both cite it. The
  shared proof-ownership step then asks the planner to map every promise,
  including "the rewrite step is deleted" and "`dev.pid` is deleted", to an
  owning slice and observable proof. Principle 7 says what a removal must not
  leave; Donut guidance nowhere says what proof a removal promise owns, so each
  planner mapped the deletion promise to an observation of the removed thing
  ("no Responses API call is made"; "a leftover `dev.pid` is ignored"), and the
  implementer wrote the assertion the plan asked for instead of skipping and
  reporting it. The refactor agent and coordinator applied the rule afterwards.
  Plan 006's "Start writes no `dev.pid`" row already shows the intended shape:
  a one-time reading at acceptance, no test.
- **Scope:**
  - Donut-authored guidance states what proof a removal owns: the retained or
    replacement tests of the behavior that survives, and a one-time sweep
    reading at acceptance (a search for the removed names over the product
    returns nothing) recorded in the plan as a reading. Proof of a removal
    names nothing about the removed thing.
  - The statement reaches the roles that write proof: the planner mapping
    promises to proof, and the implementer or refactorer writing tests. It
    lives in `AGENTS.md` and `CLAUDE.md` principle 7, kept in sync, and in the
    always-on `unit-testing` skill's assertion rules. Slice planning decides
    the exact wording and whether the principle cites the skill as its
    detail home, as the other principles do.
  - The principle's existing "every role" bullet already tells an implementer
    to skip and report a step asking for a trace; this story keeps that and
    does not restate it elsewhere.
  - Deferred: changes to the shared `dough-*` skills, whose proof-ownership
    step stays as it is; any check that Donut artifacts contain no absence
    test; any new plan template or proof format.
  - Boundary assumption: a refusal the product actively enforces is not a
    removal, so its tests stay, as principle 7 already says.
- **Key examples:**
  - A planner is handed the recorded dictation story (delete the model-rewrite
    chain so the transcription's own text is written) with the corrected
    guidance → the plan's proof column names the surviving behavior
    (`AiAudioControllerTests` writes the transcription's text; the E2E feature
    saves the spoken sentence) and a sweep reading for the removed names;
    no row says that the Responses API is not called.
  - A planner is handed the recorded Development PID-file removal → the plan
    deletes `dev.pid` with everything only it used, proves the replacement
    start and stop behavior from the process table, and records a one-time
    `git grep` reading; no row prescribes a test that a leftover `dev.pid`
    is ignored.
  - An implementer receives a slice whose proof still asks for an absence
    assertion → the implementer skips it, reports it, and the surviving
    behavior's test is the slice's proof, as principle 7 already requires.
  - A planner is handed a story that keeps non-owners from editing a notebook
    → the refusal's tests stay in the proof column; the rule does not apply.
- **Evaluation:** Replay the two recorded shapes as a one-time demonstration:
  a fresh planning agent, given the story text recorded at `595e2eb5d9` and at
  `cd1b2f1694` and the corrected guidance, writes proof that matches the key
  examples. Inspect the produced plan text directly and discard it; add no
  permanent check to Donut.
- **Findings:** [DD-205](../../DonutRetrospectiveFindings.md#local-removal-proof).
  This is a recurrence after SEED-068#removal-rule-in-guidance, delivered in
  `18b99b6424` and `52460a4077`; two later executions contradicted its intended
  effect. The completed story is historical provenance, not a current backlog
  item or a reason to repeat its implementation.
- **Value / learning:** Eliminate repeated proof churn and keep the owner's
  local removal convention effective across agent roles.
- **Effort hypothesis:** S (30–60 minutes), medium confidence: a few lines of
  guidance in three files plus the one-time demonstration. The demonstration
  is the uncertain part of the budget.
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
