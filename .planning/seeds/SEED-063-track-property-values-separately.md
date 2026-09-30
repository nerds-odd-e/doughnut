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
According to the owner's report, a tracked property could not be a list, so
authors used distinct keys such as `example of`, `example of two`, and
`example of three` to create a separate memory tracker for each association.
Each value of a list property now has its own tracker, but those numbered keys
remain in existing notes. The suffixes make one shared meaning look like different properties and are
confusing, even when the convention is explained in AI instructions.

## Story

<a id="story-2"></a>

### Existing numbered property keys become one list, and their trackers follow

**Identity:** SEED-063#story-2
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** notes written before per-value tracking (including by the old
  reduce flow) still carry `example of`, `example of 2`, `example of 3`, each with
  its own tracker. The product no longer creates new ones; this story removes the
  existing ones, which are the notes that motivated the seed.
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
- **Depends on:** nothing open; per-value trackers (the destination for moved
  trackers) are in the product, and `PropertyMemoryTrackerService.followPropertyValue`
  already moves a single value's trackers onto its list value.
- **Safe stopping point:** old notes keep working with numbered keys.

## When to Surface

When selecting note property or memory tracking improvements from the backlog.

## Breadcrumbs

- Owner's report and requirement, 2026-09-30: replace the confusing numbered-key
  convention with multiple values of one property, each tracked independently.
- Related: reifying a single-link property into a relationship note exists in the product and refuses
  structural keys; it is not a prerequisite of this seed.
- Related UAT fixes: [SEED-064#story-6](SEED-064-note-properties-fixes.md#story-6) (add-property row) uses the
  per-value rule for an existing key. The remaining numbered-key code is `propertyKeyBaseAndSuffix` and
  `nextAvailablePropertyKeyFor*` in `frontend/src/utils/noteContentPropertyKeys.ts`; the backend no longer
  creates numbered keys.
