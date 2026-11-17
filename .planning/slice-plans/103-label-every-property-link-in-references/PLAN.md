# Label every property link in references

**Identity:** SEED-131#label-every-property-link-in-references
**Source:** [selected story](../../seeds/SEED-131-reference-details.md#label-every-property-link-in-references)
**Source review:** [source-review.md](source-review.md)

## Outcome and boundaries

A note reader or connected assistant sees, for each reference, every property
of the referring note that holds the link. The label is read from the
referring note's own text when references are read.

Included: the rule for a reference's property types, as the story's Scope
states it, on the web References section (note page and note context) and on
the focus note's `inboundReferences` (`get_note_graph` and
`GET /notes/{id}/graph`); and the story's one restriction, that a relationship
note gives no label for its `source` and `target`.

Excluded, as the story defers them: the Markdown focus context, retrieval path
steps, outgoing links, attachment page references, grouping, filtering or
sorting by property type, and body-text evidence. Which references appear,
their order, the caps, the sampling, the card wording and both response shapes
do not change.

## Architecture and reuse

PFE: every part exists; the story changes where the types come from.

- `AuthoredNoteReferenceInboundFacade.distinctInboundReferencesForViewer`
  already finds each referring note and the reference rows that resolve to the
  target, and returns `InboundReference.propertyTypes`. Both surfaces read that
  field through `NoteReferenceService`, so one change serves both.
- `AuthoredNoteReferences` (package `algorithms`) already parses the links of
  a text fragment, and `AuthoredNoteReference.sourceLocalKey` is the identity
  the reference rows are distinct by. `Frontmatter.keys()` gives keys in
  authored order and `getPropertyValue` gives a scalar or list items.
  `PropertyKeyNaming.propertyType` and
  `isRelationshipNoteStructuralPropertyKey` are the existing naming rules.
- The rule belongs with the authored-reference parsing in `algorithms`: given
  a note's content and the identities of the links that resolved to the
  target, name the property types whose values hold one of them. The facade
  calls it for each referring note, whose content is already loaded.
- The lookup of property keys through the property index by reference row id
  (`AuthoredNoteReferenceRowRepository.findPropertyKeysByReferenceRowIds` and
  the row-id map that feeds it) is removed with its only caller. The property
  index, its schema and its other readers stay.

Accepted decisions: [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
makes stored note Markdown authoritative and the reference index one row per
distinct authored reference; reading the label from the Markdown follows it.
[ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md) vocabulary
(Property, Relationship note, Wiki link) is used as it stands. No conflict was
found. No North Star topic governs this work and none is added.

No expected edit touches a file over the file-size check: the largest are
`NoteReferenceService.java` (191 lines) and
`AuthoredNoteReferenceInboundFacade.java` (185 lines, shrinking).

## Current decisions

- A relationship note is a note whose frontmatter `type` is `Relationship`.
  Only on such a note do `type`, `relation`, `source` and `target` give no
  label. No other key is left out.
- Labels no longer depend on the property index, so a test that authors
  content for a label needs no index refresh. Tests whose setup calls
  `refreshDerivedIndexesForNote` only for labels (`NoteRealmServiceTest`
  `ReferencePropertyTypes`, `NoteControllerGraphTests`,
  `GraphNavigationMcpEndpointTest`) drop that call; setups that refresh for
  trackers or reification keep it.
- No end-to-end scenario is added. The card and its wording do not change, and
  the existing scenario "References show the property types the referring note
  links through" in `property_wiki_link.feature` keeps proving the journey from
  an accepted web change to the card.
- The response shapes and their OpenAPI descriptions stay, so no API client
  regeneration is expected. Comments that say "indexed property"
  (`NoteReferenceEntry`, `FocusContextInboundReference`, the facade) are
  reworded to the rule.

## Decisive premises and observations

Preparation checkout:
`/Users/terryyin/git/doughnut/.worktrees/label-every-property-link-in-references`,
revision `246f5a28450bc33b2a2d213b2cc8e7415ee45e88`, 2026-11-17.

| Premise / consuming operation | Observation, result and limits | Owner |
| --- | --- | --- |
| Reading the types from the referring note's frontmatter, matched by link identity to the rows that resolved to the target, gives the story's results through `NoteRealmService.build` | Scratch application of the rule in the facade and `AuthoredNoteReferences`, with the index lookup removed, then `CURSOR_DEV=true nix develop -c pnpm backend:test:worktree --tests '*NoteRealmServiceTest'`: 15 tests, 14 passed. The existing label tests (single property, distinct types in authored order with a body link, body-only link) passed unchanged. A scratch test passed: a relationship note linking through `target` gave no types; an ordinary note with `parent`, `overlaps` and `source` holding the link gave `parent, overlaps, source`. Scratch edits reverted. Example 3 (long list item) and example 6 were not run. | Slice 1 |
| `labels_only_the_link_the_property_index_points_at` observes the changed behavior | In the same run it failed with `Expected: null but: was <[see also]>`: it asserts the limit this story lifts. Slice 1 replaces it with example 1. | Slice 1 |
| Reading labels for the most references seen stays responsive | The same scratch test built the realm of a note with 125 referring notes: labels took about 10 ms of a 460 ms build on a cold JVM. One local run, test database. | Slice 1 |
| A focused backend test runs in this worktree | The same command ran in about 2 minutes. It takes exactly one `--tests` pattern per run. | Slice 1 |
| Readers of the changed effect | Searched at this revision. `propertyTypes` / `referrerPropertyTypes`: `NoteRealmServiceTest`, `NoteControllerShowTests`, `NoteControllerGraphTests`, `GraphNavigationMcpEndpointTest`, `Card.vue`, `NoteRealmBuilder` fixture, the E2E step "labelled … in the References section". `findPropertyKeysByReferenceRowIds`: the facade only. The three controller and MCP tests were not run against the scratch rule; slice 1 runs them. | Slice 1 |
| The web journey to the label can be observed end to end in this worktree | `SUT_TIMEOUT_MS=360000 CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_topology/property_wiki_link.feature` exited 0 at this revision: 12 passing in 24 seconds, including "References show the property types the referring note links through". | Slice 1 |

## Outside-in proof ownership

| Promise / source example | Owning slice | Observable proof |
| --- | --- | --- |
| Every link in a value is labelled (example 1) | 1 | `NoteRealmServiceTest`: Source with `see also: "[[Other]] and [[Target]]"` → both Other's and Target's entries carry `see also`. A graph test asserts Target's `inboundReferences` entry carries `referrerPropertyTypes: ["see also"]`. |
| Any key, any item length (examples 2, 3, 5) | 1 | `NoteRealmServiceTest`: `overlaps`, a list item over 255 characters, and `source` on an ordinary note each give their type. |
| Only the property that holds this link (example 6) | 1 | `NoteRealmServiceTest`: a property linking to Other plus a body link to Target → Target's entry has no types. |
| Distinct types in authored order (example 7) and body-only gives none | 1 | The existing `ReferencePropertyTypes` tests stay green. |
| A relationship note gives no label (example 4) | 1 | `NoteRealmServiceTest`: both ends' entries for the relationship note have no types; a graph test asserts the `inboundReferences` entry has no `referrerPropertyTypes`. |
| The label follows an accepted change | 1 | `NoteControllerShowTests.referencePropertyTypesFollowAcceptedContentChanges` stays green. |
| The card shows the types | 1 | `frontend/tests/notes/NoteReferences.spec.ts` is untouched and stays valid; the E2E scenario in `property_wiki_link.feature` runs once at this slice's boundary. |
| Unchanged: which references appear, order, caps, sampling, visibility | 1 | `AuthoredNoteReferenceInboundFacadeTest`, `FocusContextRetrievalInboundSamplingTest` and the other `NoteRealmServiceTest` tests stay green. |

Removal sweep at acceptance (a reading, not a test): a search of the product
for `findPropertyKeysByReferenceRowIds` returns nothing.

## Ordered slices

### 1. Label references from the referring note's own properties
Type: Behavior
Status: done
Proof: `pnpm backend:test:worktree --tests` for `*NoteRealmServiceTest`,
`*NoteControllerGraphTests`, `*GraphNavigationMcpEndpointTest`,
`*NoteControllerShowTests`, `*AuthoredNoteReferenceInboundFacadeTest` and
`*FocusContextRetrievalInboundSamplingTest` pass with the new examples; one
`pnpm cy:run --spec e2e_test/features/note_topology/property_wiki_link.feature`
passes; the removal sweep reads empty.

Behavior: a referring note holds a link to Target in one or more property
values, under any key and at any place in the value → a viewer reads Target's
note realm, or an assistant reads Target's focus context → the entry for the
referring note lists the distinct property types of those properties in
authored order (examples 1, 2, 3, 5, 7); a property that links elsewhere adds
nothing (example 6); a relationship note's `source` and `target` add nothing
(example 4).

One rule in `algorithms` names the property types of a note's content that
hold given link identities; the facade collects the identities of the rows
that resolved to the target for each referring note and calls it. Delete the
index lookup and the row-id map. Replace the test of the lifted limit with
example 1, add the other examples, drop label-only index refreshes from test
setups, and reword the three comments.

Accepted proof (2026-11-17): all six focused patterns passed
(`NoteRealmServiceTest` `ReferencePropertyTypes` now holds examples 1–7, the
example-of family and the relationship-note case; graph and MCP tests assert
example 1 `["see also"]` and example 4 no types); `property_wiki_link.feature`
12/12. Removal sweep: `git grep findPropertyKeysByReferenceRowIds` outside
`.planning` reads empty. Refactor pass added
`FrontmatterPropertyValue.strings()` and
`RelationshipNoteComposition.isRelationshipNote`, rerunning
`*algorithms.Frontmatter*`, `*NoteRealmServiceTest` and
`*RecallPromptAnswerSpellingControllerTest` green. Commitments observed in
[source-review.md](source-review.md).

## Execution complete

Product advice: no change. The labels follow the story; the next queued story
in this seed, See surrounding text in references, stays as ordered. The
retrospective found no correction. Process review is deferred into wrap-up
because the slice rewrote an existing test, which selects the intent-coverage
check.

## Refinement and concerns

Refinement was not needed: one rule, one proof loop, both surfaces fed by the
same field. This review identified no remaining slice-specific concern. The
slice's sizing rests on the scratch application, which took a few minutes of
editing; the six backend patterns run one at a time at about 2 minutes each,
inside the slice clock's command time.
