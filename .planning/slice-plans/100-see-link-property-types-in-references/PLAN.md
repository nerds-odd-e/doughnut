# See link property types in references

**Identity:** SEED-131#see-link-property-types-in-references
**Source:** [selected story](../../seeds/SEED-131-reference-details.md#see-link-property-types-in-references)
**Source review:** [source-review.md](source-review.md)

## Outcome and boundaries

A person reading a note on the web, or a connected assistant reading that
note's focus context, sees which property of each referring note holds the
link, and can tell the authored relationship without opening the referring
note.

Included: property types on each entry of the References section (note page
and note context) and on each entry of the focus note's `inboundReferences`
list in the structured focus context (`get_note_graph` and
`GET /notes/{id}/graph`). The story's Scope holds the rule for a property
type, the reading direction, and the cache constraint.

Excluded, as the story defers them: a label for the second or later link in
one property value, labels for keys the index leaves out, the Markdown focus
context, retrieval path steps, outgoing links, attachment page references, and
grouping, filtering or sorting by property type. Which references appear,
their order, caps and sampling do not change.

## Architecture and reuse

PFE: the lookup, the index and both surfaces exist; the story extends them.

- `AuthoredNoteReferenceInboundFacade.InboundReference` already pairs a
  referring note with its authored link texts for one target. It gains the
  property types. `NoteReferenceService` stays the only entry for application
  code. The row repository is package-private, so the join from the surviving
  reference rows to `note_property_index` belongs in that package.
- `note_property_index.authored_note_reference_id` and its key exist
  (migration `V300000315`). One query by the surviving reference row ids reads
  the keys; no referring note's content is read.
- `PropertyKeyNaming.propertyType` is the existing rule for a property type.
- `NoteReferences.vue` and `Card.vue` render the section; `NoteRealm.references`
  feeds it on the note page and in note context.
- `FocusContextRetrievalService` builds `inboundReferences` from
  `NoteReferenceService.sampledInboundReferencesForFocusContext`, which
  `FocusContextWikiBfsExpander` also calls for traversal.

Accepted decisions: [ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md)
vocabulary (Property, Wiki link, Note context, Focus context) is used as it
stands. [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
is untouched: no Markdown format changes. No conflict was found. No North Star
topic governs this work and none is added.

No expected edit touches a file over the file-size check: the largest are
`NoteReferenceService.java` (195 lines) and
`AuthoredNoteReferenceInboundFacade.java` (179 lines).

## Current decisions

- A References entry is a note topology plus its property types, as one flat
  entry, so every existing reader of an entry's id and title keeps working and
  `NoteTopology` itself, shared by all card lists, does not change.
- An `inboundReferences` entry becomes a structured entry: the link string it
  has today, unchanged, plus the property types. The field for the types is
  named as the referring note's property, and the `get_note_graph` description
  says so.
- Property types are omitted from an entry that has none.
- Tests author content with `makeMe.authorReferencingContent`, which does not
  rebuild the property index. A test that needs labels calls
  `NoteReferenceService.refreshDerivedIndexesForNote` or goes through a product
  write path.

## Decisive premises and observations

Preparation checkout:
`/Users/terryyin/git/doughnut/.worktrees/see-link-property-types-in-references`,
revision `7445adb85364114fa309698387a6426f66772e87`, 2026-11-17.

| Premise / consuming operation | Observation, result and limits | Owner |
| --- | --- | --- |
| The property index rows of a referring note point at the same cached reference row the inbound lookup selects for the target, for every indexed key | Scratch test in `com.odde.donut.entities.repositories`: Source authored with `prerequisite`, `prerequisite 2`, a `related` list item, `see also: "[[Other]] and [[Target]]"` and a body link, all to Target; `refreshDerivedIndexesForNote`; `findWikiCandidatesForNotebookScope` for Target returned one row; the index rows pointing at it had keys `prerequisite`, `prerequisite 2`, `related`. `CURSOR_DEV=true nix develop -c pnpm backend:test:worktree --tests '*ScratchInboundPropertyTypeTest'` exited 0. Test removed. `see also` did not point at it, which is the story's first deferred limit. | Slice 1 |
| A focused backend test runs in this worktree | The same command, exit 0, about 70 seconds. Slice 1 execution: each single-class run took 1 to 4 minutes; `backend:test:worktree` takes exactly one `--tests` pattern per run. | Slices 1, 3 |
| `authorReferencingContent` alone leaves the index empty | The same scratch test without the refresh call failed with no keys. | Slices 1, 3 |
| The label follows an accepted change | Read at this revision: every content write path calls `refreshDerivedIndexesForNote` (`AuthoredNoteDocumentPersistence`, `NoteConstructionService`, `NoteReferenceHandling`, `WikiLinkRewriteSupport`). Not run; slice 1 proves example 5 through the web content update. | Slice 1 |
| Readers of the changed contracts | Searched at this revision. `inboundReferences` as strings: `GraphNavigationMcpEndpointTest` (reads `/focusNote/inboundReferences/0` as text), `FocusContextRetrievalInboundSamplingTest`, `docs/focus-context/focus_context_retrieval_design.md` lines 32 and 38, the `get_note_graph` description, the generated client. No frontend or CLI source reads it. `NoteRealm.references`: `NoteRealmServiceTest`, `NoteControllerShowTests`, `NoteRealmBuilder` fixture, `NoteShow.vue`, `NoteContextReader.vue`, `ConversationNoteCorrectionReview.vue` and `useNoteRemovalFlow.ts` (length only). | Slices 1, 3 |
| The web journey can be observed end to end in this worktree | `SUT_TIMEOUT_MS=360000 CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_topology/markdown_link.feature` exited 0 on 2026-11-17: 9 passing, about 2 minutes, on its own worktree database. Its scenarios author a link with "I update note … content using markdown to become" and assert "I should see … in the References section", the steps slice 2 extends. A label assertion does not exist yet; slice 2 adds it. | Slice 2 |

## Outside-in proof ownership

| Promise / source example | Owning slice | Observable proof |
| --- | --- | --- |
| References entries carry property types (examples 1 to 4, 6) | 1 | Backend tests through `NoteRealmService.build` or the note show endpoint assert the entry's types for each example. |
| The label follows an accepted change (example 5) | 1 | A controller test updates the source's content through the web content update, then reads the target's realm. |
| No referring note's content is read for a label | 1 | Review of the lookup: one query by reference row ids. The test for example 6 fails if labels came from parsing content, since parsing would find the second link. |
| The reader sees the types on the card, worded as the referring note's property (examples 1 to 3, UI) | 2 | Mounted frontend test of the References section, and one E2E scenario for example 1 and example 2. |
| `inboundReferences` entries keep their link and carry the types (examples 1, 2) | 3 | `GraphNavigationMcpEndpointTest` reads the entry's link, follows it, and reads its types; `FocusContextRetrievalInboundSamplingTest` keeps its cap and sampling assertions. |
| Unchanged: which references appear, order, caps, sampling, visibility | 1, 3 | The existing reference, visibility and sampling tests stay green. |

## Ordered slices

### 1. Carry property types on a note's references
Type: Behavior
Status: done
Proof: `pnpm backend:test:worktree --tests '*NoteRealmServiceTest'` and the
controller test for example 5 pass; `AuthoredNoteReferenceInboundFacadeTest`
and `NoteControllerShowTests` stay green; the generated client is regenerated
and the frontend typecheck passes.

Behavior: Source links to Target from indexed properties → a viewer reads
Target's note realm → Source's References entry lists the distinct property
types in authored order (examples 1, 3, 4); an entry with no such property
lists none (examples 2, 6); after an accepted content change the entry follows
it (example 5).

The inbound reference gains its property types from the index rows that point
at its surviving reference rows. The realm's References entries carry them.
Regenerate the API client. The web page looks the same after this slice;
slice 2 shows the types.

Accepted (2026-11-17): `NoteReferenceEntry` (topology unwrapped plus
`propertyTypes`, omitted when empty) in `NoteRealm.references`;
`InboundReference.propertyTypes` from
`AuthoredNoteReferenceRowRepository.findPropertyKeysByReferenceRowIds`, ordered
by index row id. Proof: `NoteRealmServiceTest.ReferencePropertyTypes`
(examples 1, 2, 3, 4, 6) and
`NoteControllerShowTests.referencePropertyTypesFollowAcceptedContentChanges`
(example 5); `*AuthoredNoteReferenceInboundFacadeTest`, `*Frontmatter*`,
`*NotePropertyIndex*`, `*NoteContentMarkdown*`, `*TextContentControllerUpdate*`
green; client regenerated, `vue-tsc` and the NoteShow and NoteContextReader
specs pass. Learning: authored order needed `Frontmatter.keys()` to keep
insertion order (it returned `Set.copyOf`); the index is rebuilt in that order.
Test readers of referrer ids use `NoteRealmReferences.referrerIds`. Frontend
specs run through `pnpm -C frontend test <spec>`.

### 2. Show property types in the References section
Type: Behavior
Status: done
Proof: a mounted frontend test of the References section passes with the
frontend typecheck, and
`pnpm cy:run --spec e2e_test/features/note_topology/property_wiki_link.feature`
(or the feature the scenario is added to) passes.

Behavior: Target has a reference from Source through `prerequisite` and one
from Other through body text → a reader opens Target → Source's card shows
`prerequisite` as secondary text that reads as Source's property; Other's card
shows no label; a source with several types shows them together; a long key
does not break the card; the collapsed count is unchanged. Note context shows
the same.

Accepted (2026-11-17): `Card.vue` renders `via its <types>` as a `text-sm`,
dimmed, truncated line with a `title` tooltip under the referring note's title;
`NoteReferences` feeds the note page and note context. Proof:
`frontend/tests/notes/NoteReferences.spec.ts` (several types together, no
label, collapsed count) with `vue-tsc`, and the scenario "References show the
property types the referring note links through" in `property_wiki_link.feature`
(12 of 12, about 4.5 minutes). Test builder: `NoteRealmBuilder.referencing(target,
...propertyTypes)`. Named follow-up from the refactor pass: the References
section locators live in `e2e_test/step_definitions/wiki_link.ts`
(`referencesSection`, `referenceCard`) rather than a page object; moving them
touches three features.

### 3. Carry property types on focus context inbound references
Type: Behavior
Status: done
Proof: `pnpm backend:test:worktree --tests '*GraphNavigationMcpEndpointTest'`
and `--tests '*FocusContextRetrieval*'` pass; the generated client is
regenerated and the frontend typecheck passes.

Behavior: Source links to the focus note from `prerequisite`, Other from body
text → an assistant calls `get_note_graph`, or a client calls
`GET /notes/{id}/graph` → Source's `inboundReferences` entry holds the same
link string as today, which still selects Source, and the property type
`prerequisite`; Other's entry holds only its link. The cap of 20 and the
sampling are unchanged, and traversal through inbound references is unchanged.

Update the `get_note_graph` description and
`docs/focus-context/focus_context_retrieval_design.md` to the entry's shape,
naming the types as the referring note's property.

Accepted (2026-11-17): entries are `FocusContextInboundReference` (`link`,
`referrerPropertyTypes` omitted when none);
`NoteReferenceService.sampledInboundReferencesForFocusContext` samples inbound
references with the same exclusion, seed order and cap. Proof:
`GraphNavigationMcpEndpointTest.inboundReferenceCarriesTheReferringNotesPropertyTypesBesideItsLink`,
`NoteControllerGraphTests.inboundReferencesCarryTheReferringNotesPropertyTypes`,
`*FocusContext*` (cap and sampling unchanged),
`*AuthoredNoteReferenceInboundFacadeTest`; client regenerated, `vue-tsc`
passes. `backend:test:worktree` keeps only the last run's result XML.

## Concern evidence

Slice-plan refinement was not needed: three slices, each with one outcome and
one proof loop, and one shared rule (types come from the inbound reference).
This review identified no remaining slice-specific concern.
