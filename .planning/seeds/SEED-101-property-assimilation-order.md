---
id: SEED-101
status: dormant
planted: 2026-11-04
planted_during: discussion of Git-derived property timestamps for assimilation
trigger_when: improving the order in which learners assimilate note properties
scope: medium
---

# SEED-101: Use property change times in assimilation

## Story Decomposition

<a id="story-2"></a>

### Date property values on every web path and keep one value identity

**Identity:** SEED-101#story-2
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/004-date-property-values-on-every-web-path/PLAN.md","assessment":"not-ready","reasons":["Decisive premises are unobserved in this checkout: that wrapping createNoteFromExtractedSuggestion in AcceptedWebChangeService.apply keeps the existing AI extraction tests and save-cost guards green, and that the rebuild query can join the notebook's Git binding. Settle them at slice planning before execution."],"basis":{"document":"9424d632efe0ec8eed87a09d6d304e83d1e08f036c4a5d292e73d742a514da44","plan":"9578e24b3da1bb77f8d028c60badd9acf69ed798db35bca09baf034de564f3d2"}}
```

Correction from the execution retrospective of
SEED-101#story-1 (commits `44fb2233e0`..`0beb46cd03`; story and plan at `aff3fe6ea556b0800f0a7cbc0c95f09eba144bd7` in `.planning/seeds/SEED-101-property-assimilation-order.md#story-1` and `.planning/slice-plans/003-order-property-assimilation-by-change-time/PLAN.md`); the plan is
[004](../slice-plans/004-date-property-values-on-every-web-path/PLAN.md).

#### Goal

A learner who adds or changes property values through AI note extraction meets
those values in the assimilation sequence at the time of that change, as they
already do for other web saves. The maintenance job keeps dating the remaining
notebooks when one notebook cannot be rebuilt. One value identity serves the
index refresh, the history dater and the rebuild.

#### Scope

- AI "create extracted note" commits its change through the accepted web
  change, so the values it adds or changes are dated at that commit and appear
  in Git history.
- The rebuild selects only notebooks it can rebuild, so one notebook without a
  Git binding cannot stall the job for the others.
- The (key, value) identity of a property value has one definition, used by the
  refresh carry-over, the dater and the rebuild.
- Tests that repeat the controller-level dating tests or check a state no
  learner sees are removed; the remaining tests keep every story-1 example
  covered.
- Not included: how a note moved to another notebook keeps or loses its values'
  dates (awaiting an owner decision).
