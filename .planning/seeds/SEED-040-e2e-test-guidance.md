---
id: SEED-040
status: dormant
planted: 2026-09-25
planted_during: owner request to queue an end-to-end test guidance revision
trigger_when: selecting the second-priority product backlog story for refinement
scope: medium
---

# SEED-040: Make end-to-end test authoring guidance usable and consistent

## Why This Matters

For contributors who write and review Donut end-to-end tests, the authoring
skill and repository rules should give one clear path for choosing coverage,
writing a scenario, and expressing its steps and assertions. The owner
requested a revision of the end-to-end test authoring conventions as the second
backlog priority. The exact rules that need changing remain to be established
during refinement.

## Alternatives and Decision

The queued direction is to review the guidance as a contributor workflow and
revise the parts that need it. A smaller correction to a single rule may prove
sufficient if refinement finds only one issue; it should not be ruled out before
the concrete examples are known. Leaving the guidance as it is would not act on
the owner's requested revision.

## Story Decomposition

S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours, including delivery. This is a
rough hypothesis; the specific rule changes may require a narrower story.
This seed is not an executable plan.

<a id="story-1"></a>

### Give end-to-end test contributors clear, consistent authoring rules

**Identity:** SEED-040#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** Contributors can author and review an end-to-end test using
  conventions that agree across the end-to-end skill and applicable repository
  rules.
- **Scope:** Revise `.agents/skills/e2e-authoring/SKILL.md` and the rules that
  direct or constrain test authoring, based on concrete contributor workflows.
  Resolve conflicting, unclear, or obsolete instructions found in that scope.
  Identify the exact cases during story refinement.
- **Evaluation:** A contributor can follow the revised conventions for a
  representative change from coverage choice through scenario, step, page
  object, and assertion design; a reviewer can trace the same rules without
  contradictory instructions. A focused test run can verify the example.
- **Value / learning:** Establish which guidance changes actually remove
  friction or ambiguity for E2E contributors.
- **Effort hypothesis:** M, low confidence until the specific rules and
  representative workflows are identified.
- **Depends on:** None identified.
- **Safe stopping point:** The revised authoring guidance is usable on its own.

## Ordering and Scope Reduction

The owner selected this as the second product backlog priority. There are no
sibling stories in this seed to order or defer.

## Open Decisions

- Which current rules cause the problem, and what contributor examples show it?

## When to Surface

Refine this story when it reaches the top of the product backlog.

## Breadcrumbs

- Owner request on 2026-09-25: capture a second-priority story to revise the
  end-to-end test skill and rules; owner clarified the focus is authoring
  conventions.
- `.agents/skills/e2e-authoring/SKILL.md` and `AGENTS.md` are the starting
  guidance; identify other applicable rules during refinement.
