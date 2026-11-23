---
id: SEED-134
status: dormant
planted: 2026-11-24
planted_during: a request to queue easier addition of note property values
trigger_when: this story is selected from the product backlog
scope: One story for adding values to note properties in the rich editor
---

# SEED-134: Add values to note properties

## Why This Matters

Note authors use properties to record related notes and other values together.
When adding a value for a key they already use, they should be able to keep
editing through the same add flow and have the value join the existing entry.

## Story Decomposition

<a id="add-values-to-an-existing-note-property"></a>

### Add values to an existing note property

**Identity:** SEED-134#add-values-to-an-existing-note-property
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why.** As a note author, I want to choose `example of` when adding
  a wiki link as a property and add more values to existing properties, so
  related values stay together under one key.
- **Evaluation.** When an author adds a wiki link as a property in the rich
  editor, `example of` appears among the default key choices. Choosing it on a
  note without that property creates the entry. Choosing it when the note
  already has `example of` values `[[run]]` and `[[walk]]` appends
  `[[swim]]` in the same entry and preserves the order after reload.
- **Evaluation.** The same merge works for other keys that support list
  values, including when the author types the key while adding a wiki link.
  For example, adding `[[Mobility]]` under an existing `related topics` key
  with `[[Strength]]` saves both links under that key.
- **Evaluation.** When the author manually adds `ingredients` with value
  `salt` and the note already has `ingredients` with value `flour`, the note
  saves one `ingredients` entry with `flour` followed by `salt`. An existing
  list keeps its items and gains the new value at the end.
