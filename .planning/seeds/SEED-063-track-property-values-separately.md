---
id: SEED-063
status: dormant
planted: 2026-09-30
planted_during: owner request to replace numbered property keys with separately tracked values
trigger_when: improving how multi-valued note properties are expressed and memory tracked
scope: medium
---

# SEED-063: Track multiple values of one property without numbered keys

## Why This Matters

An example sentence can exemplify several vocabulary items or grammar concepts.
According to the owner's report, a tracked property currently cannot be a list,
so authors use distinct keys such as `example of`, `example of two`, and
`example of three` to create a separate memory tracker for each association.
Those suffixes make one shared meaning look like different properties and are
confusing, even when the convention is explained in AI instructions.

## Story

<a id="story-1"></a>

### Track each value of a property separately without numbered keys

**Identity:** SEED-063#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/007-track-property-values-separately/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"81c907793e0b0da31a46189d45ac19b58e152b233d111a0a6319eb40affbbdb7","plan":"a4ffc4cb8bcc84a40074b505604c72445e770546c6a56ade9caad5051e2b07fe"}}
```

**Goal**

As a note author and learner, I can put several values under one property, such
as `example of`, and memory track each value separately, so an example sentence
can teach several vocabulary or grammar concepts without numbered keys that make
one meaning look like several properties. Donut itself also stops creating
numbered keys, so the confusion does not keep growing.

**Scope**

- **One key, a list of values.** Several values of a property are an ordinary
  YAML list under that key (`example of: ["[[run]]", "[[past tense]]"]`), the
  shape the Markdown format already accepts and indexes item by item. No new
  syntax.
- **One tracker per value.** A tracked list property has one memory tracker per
  value. A value is identified by its key and the value itself, not by its
  position: reordering the list, or adding or removing another value, keeps each
  remaining value's tracker, recall history and schedule. Changing a value's text
  makes it a different value (owner decision, 2026-09-30).
- **One rule for every list property.** Every property whose value is a list
  follows this rule; there is no per-key choice. Keys that must stay a single
  value (`image`, `wikidata_id`, `type`, `relation`, `source`, `target`, and the
  other structural keys) and keys already excluded from tracking (`tags`,
  `aliases`, …) are unchanged. A single (scalar) value keeps today's one tracker.
- **Assimilate each value on its own.** Each value is a separate item to
  assimilate. Assimilating one value does not assimilate the others.
- **Recall asks about that value.** A value's recall question is about that
  value, not about the whole list, and the recall screen shows which value is
  being recalled.
- **Reducing a relationship note adds a value.** When a relationship note is
  reduced to a property on its source note and the source already has that key,
  the target is added to the key's list instead of creating `key 2`; the
  relationship note's tracker becomes that value's tracker, keeping its history.
- **Assumption (owner, 2026-09-30):** no tracker exists today on a list-valued
  property, so no transition rule for one is needed.
- **Excluded:**
  - converting existing numbered keys (`example of 2`, `url 2`) into lists and
    moving their trackers: [story 2](#story-2);
  - skipping one value in the assimilation sequence: a skip keeps covering the
    whole key;
  - a per-key choice between one tracker for the whole list and one per value
    (the "synonyms" idea): not now, owner decision;
  - reifying one list value into a relationship note (SEED-062 excludes lists);
  - new list-editing controls beyond what showing and assimilating a value
    needs; adding a value from the add-property row belongs to
    [SEED-064#story-6](SEED-064-note-properties-fixes.md#story-6);
  - keeping a tracker when its value's text changes, including when a linked
    note's rename rewrites the link text.

**Key examples**

- A sentence note has `example of: ["[[run]]", "[[past tense]]"]` → the learner
  assimilates it → there are two items to assimilate, one per value; assimilating
  `[[run]]` creates only that value's tracker.
- Both values are tracked → the learner recalls the `[[run]]` one → its question
  is about the note being an example of `run`; only that tracker's history and
  next recall change.
- Both values are tracked → the author adds `[[irregular verb]]` → the two
  existing trackers are unchanged, and the new value can be assimilated on its
  own.
- Both values are tracked → the author reorders the list, or removes
  `[[past tense]]` → the `[[run]]` tracker keeps its history and schedule.
- A property `topic: grammar` (single value) → tracked as today, one tracker.
- Note `Sentence` already has `example of: "[[run]]"`, and a relationship note
  "Sentence an example of past tense" is tracked → the author reduces it →
  `Sentence` has `example of: ["[[run]]", "[[past tense]]"]`, no `example of 2`,
  and the `[[past tense]]` value's tracker has the relationship note's history.

<a id="story-2"></a>

### Existing numbered property keys become one list, and their trackers follow

**Identity:** SEED-063#story-2
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** notes written before story 1 (and by the reduce flow before it)
  still carry `example of`, `example of 2`, `example of 3`, each with its own
  tracker. Story 1 stops new ones; this story removes the existing ones, which
  are the notes that motivated the seed.
- **Evaluation:** a note with `example of: "[[run]]"` and `example of 2:
  "[[past tense]]"`, each tracked → after the migration → `example of:
  ["[[run]]", "[[past tense]]"]`, and each value's tracker keeps its history and
  next recall; no numbered key remains in any notebook's content.
- **Open questions for its refinement:** how many notes and trackers are
  affected (production count); whether the change is a one-time data migration or
  an author action; how it reaches notebooks bound to Git (an accepted change per
  notebook); what happens to `url 2` and other numbered keys that are not
  tracked; whether the numbered-key code can then be deleted.
- **Effort hypothesis:** M, low confidence until the production count is known.
- **Depends on:** story 1 (the per-value tracker it moves trackers to).
- **Safe stopping point:** story 1 stands without it; old notes keep working with
  numbered keys.

## When to Surface

When selecting note property or memory tracking improvements from the backlog.

## Breadcrumbs

- Owner's report and requirement, 2026-09-30: replace the confusing numbered-key
  convention with multiple values of one property, each tracked independently.
- Related: reifying a single-link property into a relationship note exists in the product; its
  remaining correction is in [SEED-062](SEED-062-reify-property.md). Neither is a prerequisite of the other.
- Related UAT fixes: [SEED-064](SEED-064-note-properties-fixes.md) unifies the read-only and editable rows
  before this story, and [SEED-064#story-6](SEED-064-note-properties-fixes.md#story-6) (add-property row) does not
  wait for this story: whichever is delivered second adapts the one add path to the other. The numbered-key code is `propertyKeyBaseAndSuffix` and
  `nextAvailablePropertyKeyFor*` in `frontend/src/utils/noteContentPropertyKeys.ts`.
