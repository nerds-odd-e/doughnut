---
id: SEED-063
status: dormant
planted: 2026-09-30
planted_during: owner request to replace numbered property keys with separately tracked values
trigger_when: improving how multi-valued note properties are expressed and memory tracked
scope: small
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
{"schemaVersion":1,"refinement":"refined","approach":"unselected"}
```

**Goal**

As a note author and learner, I can express several values for the same property
and memory track each separately, so an example sentence can teach several
vocabulary or grammar concepts without confusing numbered property keys.

**Scope**

- Express multiple values under the same meaningful property name, such as
  `example of`, without requiring a numeric or spelled-out numbered suffix.
- Let each property value have its own memory tracker. Tracking one value does
  not require tracking all the values together.
- Recall history and scheduling are independent for each tracked value.
- Adding another value or recalling one value preserves the learning state of
  the other values.
- Keep the outcome general to properties; `example of` is the motivating
  example, rather than a restriction to that key.
- The precise editing and storage representation, and how existing numbered
  entries transition to it, remain decisions for later refinement. This story
  does not prescribe a particular list syntax or an automatic migration.

**Key examples**

- A sentence exemplifies a vocabulary item and a grammar concept → its author
  records both under `example of` → both associations use the same property
  name, with no numbered keys.
- That property has two values → the learner tracks each → each value has a
  separate tracker with its own recall history and next recall.
- Two values are tracked → the learner recalls one → only that value's learning
  state changes; the other tracker's history and schedule are preserved.
- Two values are tracked → the author adds a third value → the existing trackers
  retain their learning state, and the third value can be tracked separately.
- A property has several values → the learner tracks only one → the other
  values remain available without being forced into the same tracker.

## When to Surface

When selecting note property or memory tracking improvements from the backlog.

## Breadcrumbs

- Owner's report and requirement, 2026-09-30: replace the confusing numbered-key
  convention with multiple values of one property, each tracked independently.
- Related story: [Reify a property](SEED-062-reify-property.md#story-1). Neither
  story is currently identified as a prerequisite of the other.
- Related UAT fixes: [SEED-064](SEED-064-note-properties-fixes.md#story-5) unifies the read-only and editable rows
  before this story, and [SEED-064#story-6](SEED-064-note-properties-fixes.md#story-6) (add-property row) does not
  wait for this story: whichever is delivered second adapts the one add path to the other. The numbered-key code is `propertyKeyBaseAndSuffix` and
  `nextAvailablePropertyKeyFor*` in `frontend/src/utils/noteContentPropertyKeys.ts`.
