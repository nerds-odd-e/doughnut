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

Donut’s deletion-and-sweep convention still reaches real removal plans
inconsistently after two local guidance corrections. A later sweep also reworded
dated voice-input observations to make search terms disappear and broke a link,
leaving the records’ intended disposition unresolved.
The Donut owner and contributors need trustworthy proof and evidence when
delegating removals. The grouped evidence lives in
[Donut Retrospective Findings](../../DonutRetrospectiveFindings.md#local-removal-proof).

## Story Decomposition

<a id="removal-proof-and-evidence-recurrence"></a>

### Make Donut removal handoffs preserve valid proof and truthful evidence

**Identity:** SEED-071#removal-proof-and-evidence-recurrence
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** Donut’s owner and contributors can accept removal work with
  useful surviving-behavior proof and accurate observation records.
- **Goal:** Apply Donut’s specific removal convention consistently across a
  real planning-to-delivery handoff, including an explicit, truthful disposition
  for dated evidence encountered by the whole-product sweep.
- **Scope:** Investigate why the October 10 local correction did not reach
  the later status-line proof list, and settle the dated observation handling
  exposed by the voice-control removal. Correct the Donut-authored guidance
  and affected evidence or links needed for that outcome. Preserve the local
  deletion-plus-sweep convention and the tests of surviving behavior; active
  product refusals retain their proof. Public Open Dough skills, generic
  readiness or proof-selection rules, and unrelated product fixes are outside
  this story.
- **Evaluation:** Inspect a removal handoff under Donut’s local convention:
  the proof describes surviving behavior and a one-time sweep reading; dated
  evidence has an explicit disposition that keeps retained observations true
  and their links usable. The exact demonstration and evidence disposition
  belong to refinement; no permanent absence check is requested.
- **Findings:** [DD-205, DD-225 and DD-227](../../DonutRetrospectiveFindings.md#local-removal-proof).
  DD-227’s plan `7d062d701f` was created after the correction `b96f234fb8`.
  DD-225 records the dated-evidence problem in a different later execution.
- **Predecessor:** SEED-071#removals-follow-project-rule completed on
  2026-10-10; its [done record](../done/SEED-071_removals-follow-project-rule.json)
  remains the record of that delivered correction. This recurrence has a new
  identity and does not repeat or reopen its completed implementation claim.
- **Value / learning:** Resolve recurring Donut-specific handoff rework and
  the risk of misleading observation evidence during voice-input changes.
- **Effort hypothesis:** S–M (30 minutes to two hours), low confidence until
  refinement establishes why the existing local handoff failed and how the
  affected dated evidence should be handled.
- **Depends on:** None identified.
- **Safe stopping point:** One Donut removal handoff produces usable proof
  and truthful retained evidence without changing the public workflow.

## Ordering and Scope

Queue this story first: four distinct executions after the first local
correction include two after the latest correction, and one altered dated
observations. The MinerU recovery and unreproduced stale-class incidents remain
unqueued because neither establishes comparable recurrence or ongoing impact.
The product backlog’s voice-input direction remains unchanged.

This seed provides candidate scope for refinement, not an executable plan or
authority to implement the follow-up.

## Open Decisions

- What made the October 10 local proof correction ineffective in the later
  status-line plan, and what proportionate Donut-local change addresses it?
- Should the affected dated voice-input observation be deleted or replaced by
  a fresh observation under the local removal convention, or does a truthful
  description of the dated action satisfy the rule? The document’s explicit
  historical boundary must inform this decision; the recorded concern does
  not establish that its observations were falsified.

## Breadcrumbs

- Owner’s 2026-10-12 request to separate project findings, remove confirmed
  resolutions, group recurrence, and queue the highest-priority local stories.
- [Ownership, chronology, and priority assessment](../../DonutRetrospectiveFindings.md).
- [Shared-process findings](../../DearDough.md).
