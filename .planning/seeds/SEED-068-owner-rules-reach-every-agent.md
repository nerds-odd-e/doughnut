---
id: SEED-068
status: dormant
planted: 2026-10-03
planted_during: owner review of project retrospective findings, queueing the highest-impact Donut-specific finding group first
trigger_when: refining the queued story that corrects the removal-rule finding in DonutRetrospectiveFindings.md
scope: small
---

# SEED-068: Owner rules reach every agent that plans or delivers Donut work

## Why This Matters

The owner's standing rule for removals (delete outright, then leave no
negation, absence check, or historical note anywhere in the product) is kept
only in one coordinator's memory. Planners, implementers and refactor agents
never see it, so removal plans keep prescribing traces and the owner pays for
a correction afterwards. See
[Donut retrospective findings](../../DonutRetrospectiveFindings.md#open-findings).

## Story Decomposition

<a id="removal-rule-in-guidance"></a>
### Removals leave no trace without the owner restating the rule

**Identity:** SEED-068#removal-rule-in-guidance
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** Let the owner rely on every agent, not only a coordinator with
  memory, to keep a removal free of traces, so no correction story follows.
- **Evaluation:** Donut's always-loaded guidance states the removal rule, so a
  fresh planner, implementer or refactor agent given a removal plans and
  delivers no absence assertion, negation test, or "no longer" note in docs,
  comments or guidance.
- **Key examples (from findings):**
  - DD-202: a removal plan asked for a docs note that dictation "no longer
    changes titles"; three agents accepted it and a correction story and plan
    followed delivery.
  - DD-142 (now part of shared ODF-187): a plan asked for a "there is no
    `[aria-label=…]` element" assertion after deleting that hint; the refactor
    agent removed it.
- **Boundary:** Donut guidance only. Shared Open Dough guidance on absence
  assertions stays as it is.
- **Effort hypothesis:** S — guidance text in `AGENTS.md` and `CLAUDE.md`
  (kept in sync) and the skill they cite.
- **Safe stopping point:** The rule is in the always-loaded guidance with its
  transitive-removal and whole-product cleanup parts.
