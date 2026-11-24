# Add values to an existing note property

**Identity:** SEED-134#add-values-to-an-existing-note-property
**Source:** [Add values to an existing note property](../../seeds/SEED-134-add-values-to-note-properties.md#add-values-to-an-existing-note-property)

## Goal and scope

Let a note author take the wiki link already picked in rich-editor search
and put it under their chosen key, including an existing list-capable key.
Keep the immediate unnamed save so the author can reload and finish naming.
When naming succeeds, persist one update that contains the chosen property
and consumes the unnamed entry. Preserve the order of existing values, the
link's authored destination, and other note content.

An absent key creates the property. Appending to a scalar keeps its first
value and associated learning history. Match the exact authored key after
the usual input trimming. Keep manual Add property and the rules for
renaming an already-named property, including the collision refusal.
Destination-first search, custom key discovery, Markdown entry, and the
recall offer are outside this story.

## Design and decisions

The point of reuse is the existing rich-editor rules for key eligibility,
exact-row lookup, scalar promotion, and ordered append. The saved unnamed row
is the incoming value, and a named row is the destination. Inspect the
starting key when deciding between assignment and ordinary rename. Allow the
preset in the assignment context and retain row exclusion for rename.
Resolve the add against the other rows and publish the validated result
through the existing properties-changed path. A rejected assignment leaves
the previously saved unnamed link and the destination recoverable.

The value dialog starts from a destination and needs a separately entered
value. Relationship-note reduction is a backend operation that consumes a
relationship note and rehomes its trackers. The wiki-link action already has
its authored token and needs neither operation. Keep the codec, save route,
and tracker follow behavior in their current owners. If extracting the
manual add transformation keeps the editing composable within the repository
file limit, put it with the existing property-row operations.

[ADR 0001, Ubiquitous language](../../../docs/adrs/0001-ubiquitous-language.md)
and [ADR 0004, OKF-compatible notebook Markdown profile](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
govern the exact property key and wiki links in one-level YAML lists. The
North Star's [One format boundary](../../NORTH-STAR.md#one-format-boundary)
and [One accepted-change boundary](../../NORTH-STAR.md#one-accepted-change-boundary)
apply here through the existing codec and web note publication. The naming
update saves the final frontmatter as one content change under
[ADR 0002, Git-native Portable notebook tree synchronization](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md).
The property route keeps its original key under
[ADR 0005, Web routes](../../../docs/adrs/0005-web-routes-accepted.md).

## Outside-in proof

| Promise | Boundary and observing check |
| --- | --- |
| Link selection survives a reload while unnamed. Choosing the occupied `example of` preset then appends in order. | Extend [Note Edit](../../../e2e_test/features/note_creation_and_update/note_edit.feature) with the real link search action, reload before naming, then choose the preset and reload again. Assert the unnamed entry is visible on the first reload, and one named entry contains `[[run]]`, `[[walk]]`, `[[swim]]` in order on the second. Check authored Markdown for the final single property and unchanged surrounding content. Reuse the [target search dialog](../../../e2e_test/start/pageObjects/noteTargetSearchDialog.ts), [property page object](../../../e2e_test/start/pageObjects/noteRichPropertyMethods.ts), and explicit named-page reload step. |
| An absent preset creates the first value. An exact typed key appends to a scalar. | Through the note editor's registered property inserter and visible key field in Chromium, choose `example of` when absent and type `related topics` where `[[Strength]]` already exists. Observe the saved Markdown payloads at the generated content SDK boundary. Confirm the old value plus `[[Mobility]]` in one entry. |
| A tracked scalar keeps its tracking history on the original list item. | In [tracker follow proof](../../../frontend/tests/notes/NoteEditableContent.trackerFollowsValue.spec.ts), append a picked wiki link to a tracked scalar and observe the existing tracker request for the former value. |
| Manual add still appends to an exact key, and a named-row rename or scalar-only collision preserves both authored values. | Use the existing property entry, preset, validation, and rename-guard specs, with the seed's manual `ingredients` scalar and list examples and a naming collision at an occupied scalar-only key as the changed-boundary cases. Observe saved note Markdown for successful adds and a visible recovery message with the original values retained on a refused naming. |

Use `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/note_edit.feature`
for the real save and reload path. Run the focused Chromium readers via
`CURSOR_DEV=true nix develop -c pnpm frontend:test <spec paths>`. Trace
any further readers reached by the change and verify both frontend
typechecks with `pnpm -C frontend exec vue-tsc --noEmit` and
`pnpm -C frontend exec tsc --noEmit` through the repository Nix shell.

## Ordered slices

### 1. File a picked wiki link under the chosen property
Type: Behavior
Status: planned
Proof: Note Edit search, two reloads, and ordered values under the selected
preset. Chromium save checks cover a missing preset and a typed scalar,
and confirm tracker continuity, manual append, and recovery after a key
collision.

Behavior: With a saved unnamed link from rich-editor search, commit a preset
or typed key. The author sees the new or updated property on the note.
An existing list-capable property's previous values remain in order and
the link is last. An occupied scalar-only property keeps its value and the
author can correct the naming attempt.

## Learnings and premise evidence

Observations used this owned checkout at `76f0c9119f` on 2026-11-24.

| Premise | Consuming operation and observation |
| --- | --- |
| Manual Add property already covers the `ingredients` examples. | The five pre-change Chromium probes recorded in the [seed](../../seeds/SEED-134-add-values-to-note-properties.md#current-behavior) used `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.notePropertyRefinementProbe.spec.ts`. The actual add control emitted one ordered property for each input. The same probes observed the wiki naming defect, an omitted occupied preset and a duplicate-key message on the typed key. The scratch spec was removed. |
| The editor can use the current save, key validation, and tracker follow paths. | The focused Chromium runs passed 29 tests across property presets, property entry, note property editing, and tracker following, and 13 across `InsertWikiLink.spec.ts`, property validation, rename guard, and occupied preset slots. The search action handed off the backend-authored Portable path. The note editor sent the unnamed link to the save API and manual append sent the tracker follow request for the original scalar. |
| The same note save and load route supports the planned starting data and outcome shape. | The isolated Note Edit command above passed 14 scenarios, including manual preset append followed by reload and a Markdown source check. A focused scratch scenario in the same feature ran that command again. It searched for `swim`, chose the property action, waited for the content save, reloaded, and observed `"": "[[swim]]"` in the Markdown with the original two `example of` items still in order. The scenario passed and its scratch edits were reverted. |

## Plan review

One Behavior covers both ways to name the same saved link. They use the
same domain rule and the same save boundary. Sharing the existing add
behavior is local to the slice. Slice-plan refinement was unnecessary.
No remaining slice-specific concern was identified.
