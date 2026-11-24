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
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/118-add-values-to-an-existing-note-property/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"3b11bd46a18d82795f9b37240d64f977496830dfbfba399fa3976e8899151e8c","plan":"8e151a5a6552ebe4479ac9470a14b89066db8c2e24acd28d1f4795f43a28cfb7"}}
```

#### Goal

A note author who finds a target through the rich editor's wiki-link search
can add that link to a note property in the same task. Values about the same
subject stay together under one key, with the author's existing values in
their original order.

#### Scope

- Adding a wiki link as a property saves the unnamed link immediately and
  opens key selection. The author can leave the note and finish naming the
  saved link later. A successful naming commit puts the link under the
  chosen key in the same update that consumes the unnamed entry.
- Offer `example of` among the default key choices when adding a wiki link
  as a property, whether the note has that key already or is getting its first
  value. Let the author type a key as well.
- Committing an add to the exact authored key of a list-capable property
  appends the link to that entry. A single existing value becomes the first
  list item. A missing key creates a property through the ordinary add flow.
- Manual **Add property** already appends to a matching list-capable property.
  Keep that behavior. Editing the key of an already-named property is a rename
  and must preserve the distinction between the two authored properties.
- Keep scalar-only fields scalar because consumers use them as single values.
  For example, the note image points to one file. The add operation leaves
  its existing value intact when the author enters that occupied key.
- Destination-first insertion and broader changes to how authors discover
  custom keys are follow-up possibilities. This story covers the rich note
  editor. Markdown editing and the recall link offer have their own entry
  points.

#### Key examples

| Starting note | Author action | Result |
| --- | --- | --- |
| The author has inserted `[[swim]]` as a property and has yet to name it. | Reopen the note and resume choosing a key. | The saved link is available to name. |
| There is no `example of` entry. | Add `[[swim]]` as a property from wiki-link search and choose `example of`. | One `example of` entry contains `[[swim]]`. |
| `example of` contains `[[run]]`, then `[[walk]]`. | Add `[[swim]]` the same way and choose that preset. | After reload, one `example of` entry shows `[[run]]`, `[[walk]]`, `[[swim]]` in that order. |
| `related topics` contains `[[Strength]]`. | Add `[[Mobility]]` as a property and type `related topics`. | One entry shows `[[Strength]]`, then `[[Mobility]]`. |
| `ingredients` contains `flour` as a scalar or already contains an ordered list. | Enter `ingredients` and `salt` in manual **Add property**. | One entry keeps the existing values and adds `salt` at the end. |

#### Current behavior

On `76f0c9119f`, a local Chromium probe used the rich editor's link insertion
entry point and the visible key control. The available preset created
`example of` when absent. With an existing list, the preset was absent from the
choices. Typing an occupied `related topics` key showed the duplicate-key
message and left the original property and the unnamed link intact. Manual
**Add property** emitted ordered lists for both `ingredients` examples. All
five observations passed. The probe stopped at editor Markdown output and was
removed. [Rich editor property editing](../../frontend/src/composables/useRichFrontmatterPropertyEditing.ts)

Four existing Chromium specs also passed, covering 29 cases. At the note
save boundary, the rich and Markdown link actions both saved an unnamed
value at insertion. Manual rich append moved the existing scalar tracker to
the former value when it became a list item. These tests mocked the save API.
[Property editing](../../frontend/tests/notes/NoteTextContent.propertyEditing.spec.ts),
[tracker continuity](../../frontend/tests/notes/NoteEditableContent.trackerFollowsValue.spec.ts)

#### Approaches

| Approach | Effect on the author and system |
| --- | --- |
| Keep the existing list editor as the way to append. | The author can add a link to a known property today by opening its value editor and entering the link text. It leaves the link picked in search outside the add flow. |
| Finish the current link-first action at the key field. | The author's chosen link survives an interruption before naming. Once the key is committed, the rich editor can apply the existing exact-key append rule to the destination property. This retains the initial unnamed save and its separate final naming save. |
| Prefill the manual add draft from link search. | The author can correct the key or cancel while the link is still a draft. The two entry points can share confirmation, validation, and saving. Leaving the editor before confirmation discards the unsaved link. |
| Choose the destination before searching for the link. | As in filing a document into a named folder, the property is the destination, the picked link is the item being filed, and exact naming keeps separate destinations distinct. Choosing the row first makes the destination explicit and bypasses key collisions. It adds a second way to start link search. The analogy only covers routing to a destination. Here, a new destination is part of the same add flow and the order of its existing values matters. |

The story's benefit is carrying the link selected in search into the right
property. General append support in the value editor or manual add alone
would leave that part of the task unresolved. The current link-first action
can deliver the benefit with the least change to the author's existing flow.

#### Architecture

The exact authored key identifies the destination and stays stable as its
value takes the shape of an ordered YAML list. Keep wiki-link spelling and
unrelated frontmatter through the existing note Markdown save and publication
path. When a scalar becomes the first list item, use the existing tracker
continuity behavior so the learner's history follows that value.
[ADR 0001, Ubiquitous language](../../docs/adrs/0001-ubiquitous-language.md),
[ADR 0004, OKF-compatible notebook Markdown profile](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md),
[ADR 0002, Git-native Portable notebook tree synchronization](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)

#### Decision

The product owner confirmed the current link-first save on 2026-11-24.
The author can leave and finish naming the link later. The existing
unnamed-save proof applies alongside the new naming and append proof.

[Slice plan](../slice-plans/118-add-values-to-an-existing-note-property/PLAN.md)
