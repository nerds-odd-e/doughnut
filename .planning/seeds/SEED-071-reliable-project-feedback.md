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
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/069-truthful-removal-handoffs/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"272b79ccb4ef076f4946fc3eb7ee0001739e95161d5fc5de1d9ed8bf0789db3e","plan":"cbe8e0928a072bfcff6958518a5a37f5479cfddd061fa8b575d2f95f4800d836"}}
```

- **For / why:** Donut’s owner and contributors can accept removal work with
  useful surviving-behavior proof and accurate observation records.
- **Goal:** Apply Donut’s specific removal convention consistently across a
  real planning-to-delivery handoff, including an explicit, truthful disposition
  for dated evidence encountered by the whole-product sweep.
- **Scope:** Correct the Donut-authored guidance at each proof-item handoff,
  using the evidenced gap between the October 10 rule and the later plan.
  Delete the dated observation material dependent on the retired mid-speech
  control and its dependent links; preserve independently meaningful dated
  evidence with its provenance and observed results unchanged. Preserve the local
  deletion-plus-sweep convention and the tests of surviving behavior; active
  product refusals retain their proof. Public Open Dough skills, generic
  readiness or proof-selection rules, and unrelated product fixes are outside
  this story.
- **Key examples:**
  1. A mixed UI change reaches planning with button-state behavior and a
     removal → its proof table, slice proof, and implementation brief carry
     the surviving behavior's proof plus a separate one-time sweep reading.
     An actively enforced read-only restriction retains its rejection proof.
  2. A sweep encounters the dated mid-speech journey → delete that observation
     and dependent material, follow incoming links and shared descriptions,
     and retain independent navigation and responsiveness evidence truthfully.
     Do not relabel the historical action as a newly observed action.
  3. That evidence cleanup goes through an actual implementation handoff →
     the implementer, fresh refactor review, and coordinator acceptance use
     the same surviving-evidence proof and recorded sweep reading. The
     retained dates, revisions, limitations, results, and usable links are
     the observable result.
- **Evaluation:** Use the historical status-line plan as a bounded proof-item
  review example, then this story's actual documentation removal as the
  planning-to-delivery demonstration. Record what each establishes; a replay
  alone does not demonstrate later real-work compliance. No permanent absence
  checker or new paid-service observation is requested.
- **Findings:** [DD-205, DD-225 and DD-227](../../DonutRetrospectiveFindings.md#local-removal-proof).
  DD-227’s plan `7d062d701f` was created after the correction `b96f234fb8`.
  DD-225 records the dated-evidence problem in a different later execution.
- **Predecessor:** SEED-071#removals-follow-project-rule completed on
  2026-10-10; its [done record](../done/SEED-071_removals-follow-project-rule.json)
  remains the record of that delivered correction. This recurrence has a new
  identity and does not repeat or reopen its completed implementation claim.
- **Value / learning:** Resolve recurring Donut-specific handoff rework and
  the risk of misleading observation evidence during voice-input changes.
- **Effort hypothesis:** S (roughly 30 minutes including delivery), medium
  confidence: two bounded guidance/documentation outcomes with direct reading
  proof; the observation's dependent descriptions require care.
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

## Current decision

The owner accepted the deletion recommendation on 2026-10-12 and requested
slice planning. Preserve independent dated evidence unchanged, repair the
dependent links, and let Git retain the deleted observation. No open scope
decision remains; this instruction authorizes preparation, not execution.

## Refinement evidence

Read on 2026-10-12 in the established preparation workspace at
`0a8878e98be76b8e92203515d8f4719489b24169`:

- `git show b96f234fb8:AGENTS.md` confirms that the October 10 correction
  already applied to every role and distinguished surviving-behavior tests
  from the one-time sweep. Its publication preceded the later plan.
- `git show 7d062d701f:.planning/slice-plans/065-unobtrusive-spoken-title/PLAN.md`
  shows the rule cited in scope, while both the outside-in proof table and
  slice 5's Proof prescribe a status-element absence assertion. The observed
  gap is propagation into individual proof items, not a missing overall
  removal instruction. The evidence does not establish why that agent
  overlooked the contradiction.
- The existing owners are principle 7 in `AGENTS.md` / `CLAUDE.md` and the
  Donut-authored `unit-testing` skill. Public refactor guidance explicitly
  defers to project test style. A proportionate correction belongs in those
  local owners: apply the rule to each proof item when planning and carrying
  it into an implementation brief, then preserve that classification through
  acceptance. Another policy owner or a permanent absence-checking test would
  not address the observed gap appropriately.
- `git show 7becda17c4 -- docs/voice-input-observations.md` shows five edits
  substituting a generic requested conversion for the original control name,
  including the heading. The current document retains its October 3 boundary,
  backend revision, incomplete frontend provenance, input, and observed
  results. This establishes wording and anchor churn, not falsified results.
- The affected incoming anchor is in SEED-066's bounded discovery findings.
  The navigation observation and the October 6 responsiveness measurements
  have separate evidence. Removing the passage requires following its
  dependent text (including shared input descriptions and comparisons), not
  merely deleting the heading and leaving dangling references.

## Breadcrumbs

- Owner’s 2026-10-12 request to separate project findings, remove confirmed
  resolutions, group recurrence, and queue the highest-priority local stories.
- [Ownership, chronology, and priority assessment](../../DonutRetrospectiveFindings.md).
- [Shared-process findings](../../DearDough.md).
