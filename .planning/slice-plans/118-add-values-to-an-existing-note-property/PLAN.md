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
Status: done
Proof: Note Edit search, two reloads, and ordered values under the selected
preset. Chromium save checks cover a missing preset and a typed scalar,
and confirm tracker continuity, manual append, and recovery after a key
collision.

Behavior: With a saved unnamed link from rich-editor search, commit a preset
or typed key. The author sees the new or updated property on the note.
An existing list-capable property's previous values remain in order and
the link is last. An occupied scalar-only property keeps its value and the
author can correct the naming attempt.

## Execution context

- Story Branch Mode in the owned worktree
  `/Users/terryyin/git/doughnut/.worktrees/add-values-to-an-existing-note-property`,
  branch `codex/add-values-to-an-existing-note-property`, with `origin/main` as
  the integration target and its main worktree as the integration checkout.
- Publisher `dashboard-territory.local-doughnut`, agent `chaifeng-chan`.
  Start `9e98ebd518983459009fb7a14eeba74b6b2b50ed`.
  The claim `2a6b4821ad988c10ec36b57ac89a3d1fa62a8dda` was confirmed on
  `origin/main` and the execution branch. This is the first increment's base.
- Codex coordinator `codex-6839e935` observes GitHub Actions `ci.yml` on
  `nerds-odd-e/doughnut` for the execution branch. Stream cell `g4:7`,
  command session `50276`, PID `49231`, mailbox `/tmp/dough-ci-501/watch-3mSTno`.
  After a server restart, the previous stream at `/tmp/dough-ci-501/watch-3YaWp3`
  stopped with no unread evidence. Both remote branches still held the claim.

## Learnings and premise evidence

Observations used this owned checkout at `76f0c9119f` on 2026-11-24.

| Premise | Consuming operation and observation |
| --- | --- |
| Manual Add property already covers the `ingredients` examples. | The five pre-change Chromium probes recorded in the [seed](../../seeds/SEED-134-add-values-to-note-properties.md#current-behavior) used `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.notePropertyRefinementProbe.spec.ts`. The actual add control emitted one ordered property for each input. The same probes observed the wiki naming defect, an omitted occupied preset and a duplicate-key message on the typed key. The scratch spec was removed. |
| The editor can use the current save, key validation, and tracker follow paths. | The focused Chromium runs passed 29 tests across property presets, property entry, note property editing, and tracker following, and 13 across `InsertWikiLink.spec.ts`, property validation, rename guard, and occupied preset slots. The search action handed off the backend-authored Portable path. The note editor sent the unnamed link to the save API and manual append sent the tracker follow request for the original scalar. |
| The same note save and load route supports the planned starting data and outcome shape. | The isolated Note Edit command above passed 14 scenarios, including manual preset append followed by reload and a Markdown source check. A focused scratch scenario in the same feature ran that command again. It searched for `swim`, chose the property action, waited for the content save, reloaded, and observed `"": "[[swim]]"` in the Markdown with the original two `example of` items still in order. The scenario passed and its scratch edits were reverted. |

## Slice 1 proof

Observed against the working changes based on `2a6b4821ad` on 2026-11-24.
The command arguments, results, and implementation report are in the checkout's
`open-dough/evidence` Git directory.

| Observation | Evidence |
| --- | --- |
| Real search saves `[[swim]]` under an unnamed key. After reload and preset selection, a second reload shows one `example of` entry in order, and the Markdown retains the other property and body. | `refactor-e2e` passed all 27 scenarios in Note Edit and Note Property Layout. The new scenario uses the shared browser clock for search and asserts the authored `[[swim]]` token in the first reloaded editor. |
| The absent preset creates its first value. The exact trimmed key appends to a scalar, leaving the differently cased key intact. Each successful name sends one final update after the unnamed save. | `refactor-browser` passed all 64 tests. `NoteTextContent.propertyEditing` asserts the complete generated content SDK payloads. |
| The tracker follows the former scalar value to the first item. Manual Add property appends to scalar and list `ingredients`. Both the scalar-only assignment refusal and the named-row rename refusal preserve the original values. | The same Chromium run includes `NoteEditableContent.trackerFollowsValue`, `RichMarkdownEditor.propertyEntry`, and the note save and rename guard specs. The scalar-only collision test also verifies recovery by naming the same link with a valid key. |
| Existing property and preset readers retain their contracts. | `property-consumers` passed 70 tests and `property-ui-readers` passed 41. See `consumer-trace-disposition.md` for the affected entry points and observations. Both frontend typechecks passed. |

The independent pass kept the focus-time naming decision and used the current
row index to filter rename presets. It renewed the affected browser specs,
both Cypress features, and both frontend typechecks. Its reasoning and
consumer trace are recorded in `slice-1-refactor.md`. No generation applies.

## Plan review

One Behavior covers both ways to name the same saved link. They use the
same domain rule and the same save boundary. Sharing the existing add
behavior is local to the slice. Slice-plan refinement was unnecessary.
No remaining slice-specific concern was identified.
