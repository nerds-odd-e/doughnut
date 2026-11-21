---
id: SEED-133
status: dormant
planted: 2026-11-21
planted_during: a request to add whole-list assimilation to the product backlog
trigger_when: this story is selected from the product backlog
scope: One story for whole-list assimilation and recall
---

# SEED-133: Remember the contents of a list property

## Why This Matters

Donut learners can assimilate individual items in a list property. When a
learner needs to remember the list as a whole, they need a memory tracker for
the property that helps them recall which items belong in the list.

## Story Decomposition

<a id="recall-a-whole-list-property"></a>

### Recall a whole list property

**Identity:** SEED-133#recall-a-whole-list-property
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why.** A Donut learner can create one memory tracker for a whole list
  property and use it to practice recalling the items that make up the list.
- **Evaluation.** Given a note with a list property named Packing list that
  contains a charger, an adapter, and headphones, the learner can choose to
  assimilate the whole property. In recall, the resulting tracker prompts the
  learner to recall all three items together.
- **Boundary.** The learner can choose whole-list assimilation directly from
  the property, independently of the regular Next assimilation sequence.
  Individual items remain available for assimilation.
