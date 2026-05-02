# Doughnut Wiki Migration Plan - Phase 7.13 Sub-Phases

## Parent Phase

Sub-phase 7.13 of `ongoing/doughnut_wiki_migration_plan-phase-7-sub-phases.md`: Remove Parent-Based Graph Relationships.

## Goal

GraphRAG context should describe the focus note through wiki links and folder placement only. Note parent/child edges no longer provide graph relationship meaning or expansion.

**Target end state by the end of 7.13:**

- `FocusNote` has no children list.
- `FocusNote.contextualPath` is a single folder-name crumb string for the focus note's folder path; it does not create related notes.
- `FocusNote.links` lists wiki links from the focus note to other notes.
- `FocusNote.inboundReferences` lists notes that wiki-link to the focus note.
- Related notes still include sibling notes from the same folder.
- `RelationshipToFocusNote` contains only currently meaningful graph roles.
- `BareNote.uri` is a wiki-link URI: `[[note title]]` for the focus note itself, and `[[notebook name: note title]]` when the URI appears on a note other than the focus note.
- `BareNote` no longer exposes `linkFromFocus` or `linkHop2` (serialized `target`, `parent`, `relation_type`, and `subject` are already gone in **5.24e**); relationship meaning lives in `details` / `detailsTruncated` and in the focus note's link lists.
- **`Note.isRelation()` is gone.** There is no replacement field or method on `Note` that duplicates the old split between “relationship-shaped” notes and everything else. Every previous caller either drops the branching (one behavior for all notes) or deletes the branch entirely. Tests and builders stop filtering with `Note::isRelation`; production code stops gating caches, formatting, GraphRAG, or question generation on it.

**`BareNote` as implemented today** (`backend/.../graphRAG/BareNote.java`): JSON property order is `uri`, `title`, `relationToFocusNote`, `linkFromFocus`, `linkHop2`, `details`, `detailsTruncated`, `createdAt`. Serialized `uri` is still `note.getUri()` (legacy note URI), not wiki-link text yet. `linkFromFocus` and `linkHop2` are still present as booleans on the wire and in `mergeIntoExisting(...)`. `equals` / `hashCode` delegate to `UriAndTitle.fromNote(note)` via `@JsonIgnore` `getUriAndTitle()`. Factories: `fromNote(note, relation)`, `fromNote(note, relation, linkFromFocus, linkHop2)`, `fromNoteWithoutTruncate(note)`. Sub-phases **7.13.8**–**7.13.9** carry the simplification and wiki-uri behavior above from target into code.

## Design Decisions

- **Children are gone, not replaced:** Child expansion was structural parent-note behavior. Removing it is the product behavior; do not rebuild it from folders or wiki links.
- **`Note.isRelation()` is gone, not replaced:** Relationship-shaped markdown was a legacy axis for branching. Removing it means no substitute boolean (e.g. “wiki relation note”) on `Note`; narrow the behavior or delete the specialized path rather than renaming the predicate.
- **Folder path is focus-note context only:** Folder crumbs help the AI situate the focus note, but folder ancestors are not related notes.
- **Siblings stay folder-based:** Same-folder sibling notes remain useful structural context and are already aligned with Phase 6 folder-first behavior.
- **Links own semantic relationships:** Outgoing links and inbound references should come from the Phase 5 wiki-title cache, not legacy relation rows or parent/target fields.
- **DTO shape is intentionally smaller:** Removed graph fields should not get compatibility shims unless a currently supported external consumer fails and the maintainer explicitly asks for a transitional contract.
- **Human-owned git and deploy:** Automated assistants must not commit, push, open pull requests, or trigger deploy unless explicitly asked.

## Verification gate

Every sub-phase closes with **`GraphRAGServiceTest` passing** (and any other suites touched by that slice). Do not merge intentionally failing tests or leave red CI for “later” phases; bundle adjacent implementation when needed so the contract tests stay green.

## Sub-Sub-Phase 7.13.1 - Lock the wiki-link GraphRAG contract

**Type:** Behavior.

**Pre-condition:** Phase 5.23 is complete enough that `WikiTitleCacheService` can provide authorized outgoing wiki targets and inbound referrers for a focus note.

**Trigger:** A GraphRAG request is made for a note with outgoing wiki links, inbound wiki references, same-folder siblings, and legacy parent/child data still present in test setup.

**Post-condition:** `GraphRAGServiceTest` cases lock the wiki-link GraphRAG JSON contract (and pass):

- focus note exposes `links` for outgoing wiki links;
- focus note exposes `inboundReferences` for wiki backlinks;
- focus note exposes a folder crumb path string;
- related notes include same-folder siblings (same tree parent within the folder scope);
- related notes do not treat legacy note-parent expansion as graph structure.

**Work:** Add or rewrite focused `GraphRAGServiceTest` cases around the public `GraphRAGResult` shape.

**Verify:** `CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests com.odde.doughnut.services.GraphRAGServiceTest`.

## Sub-Sub-Phase 7.13.2 - Add focus wiki links and inbound references

**Type:** Behavior.

**Pre-condition:** 7.13.1 defines contract coverage for focus-note wiki links and inbound references.

**Trigger:** `GraphRAGResultBuilder` builds a `FocusNote`.

**Post-condition:** `FocusNote` carries both wiki-link directions:

- `links`: outgoing resolved wiki links from the focus note;
- `inboundReferences`: authorized notes that link to the focus note.

These lists are populated from `WikiTitleCacheService` using the same viewer rules as graph related-note retrieval.

**Work:** Pass the viewer/cache data needed to construct `FocusNote`, or populate the lists in the builder/service immediately after focus creation. Reuse existing cache APIs where possible; add a focused cache method only if the current API cannot return outgoing link notes cleanly.

**Verify:** `CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests com.odde.doughnut.services.GraphRAGServiceTest`.

## Sub-Sub-Phase 7.13.3 - Convert focus contextual path to folder crumbs

**Type:** Behavior.

**Pre-condition:** A focused graph test covers a note inside nested folders.

**Trigger:** `FocusNote` is created for a note with `folderId`.

**Post-condition:** `FocusNote.contextualPath` is a string of folder-name crumbs from notebook root to the focus note's folder. It no longer reads `Note.parent`, `Note.getAncestors()`, or note URIs. Folder ancestors are not emitted as related notes.

**Work:** Replace the current note-ancestor list with folder ancestry lookup. Keep the representation as a single string so the AI sees placement context without a structural graph edge. If no folder is set, use an empty or null value consistently with existing JSON omission rules.

**Verify:** `CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests com.odde.doughnut.services.GraphRAGServiceTest`.

## Sub-Sub-Phase 7.13.4 - Remove child graph expansion

**Type:** Structure.

**Pre-condition:** 7.13.1-7.13.3 cover the graph context that replaces parent/child assumptions: folder crumbs, wiki links, inbound references, and folder siblings.

**Trigger:** GraphRAG still enqueues or serializes child-note context through `ChildRelationshipHandler` or `FocusNote.children`.

**Post-condition:** Child-related graph behavior is removed outright:

- `ChildRelationshipHandler` is deleted or no longer used and then deleted in the same slice;
- `FocusNote.children` is removed;
- `RelationshipToFocusNote.Child` is removed if no remaining code uses it.

**Work:** Remove the child handler from `GraphRAGService` priority layers and delete child list mutations. Fix tests by deleting obsolete child assertions, not by converting children into another structural relationship.

**Verify:** `CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests com.odde.doughnut.services.GraphRAGServiceTest`.

## Sub-Sub-Phase 7.13.5 - Keep same-folder sibling expansion only

**Type:** Structure.

**Pre-condition:** Child expansion is gone and `NoteService.findStructuralPeerNotesInOrder(focusNote)` is folder-scoped from Phase 6.

**Trigger:** GraphRAG still mixes same-folder sibling handlers with parent-neighborhood handlers.

**Post-condition:** GraphRAG retains only the sibling handlers that return notes from the focus note's folder peer list (`OlderSiblingRelationshipHandler` / `YoungerSiblingRelationshipHandler` or their cohesive replacement). Sibling labels remain the structural `relationshipToFocusNote` values for now.

**Work:** Keep the current same-folder peer lookup and tests. Do not introduce parent fallback for root notes beyond the existing folder-first peer definition.

**Verify:** `CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests com.odde.doughnut.services.GraphRAGServiceTest`.

## Sub-Sub-Phase 7.13.6 - Remove parent, ancestor, and target-parent graph handlers

**Type:** Structure.

**Pre-condition:** 7.13.5 confirms folder siblings still provide structural neighborhood context.

**Trigger:** `GraphRAGService` still wires handlers that read `Note.parent`, `Note.getAncestors()`, `Note.getChildren()` for parent neighborhoods, or target parent paths.

**Post-condition:** Parent-derived graph expansion is gone:

- no `ParentRelationshipHandler`;
- no note-ancestor context handlers;
- no parent-sibling or parent-sibling-child handlers;
- no target-parent or target-context handlers;
- no sibling-of-referencing-note expansion that depends on a referrer's parent/children;
- no relationship-of-target-sibling handler that returns a parent note.

Wiki targets and wiki inbound references may still add related notes, but without parent-derived follow-up expansion.

**Work:** Remove handlers from `GraphRAGService` in small batches if needed, deleting the handler classes as soon as they have no callers. Prefer shrinking the priority-layer wiring over keeping dead handlers behind conditionals.

**Verify:** `CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests com.odde.doughnut.services.GraphRAGServiceTest`.

## Sub-Sub-Phase 7.13.7 - Shrink `RelationshipToFocusNote`

**Type:** Structure.

**Pre-condition:** Parent/child/ancestor/target-parent handlers are gone.

**Trigger:** `RelationshipToFocusNote` still contains enum constants for removed graph paths.

**Post-condition:** `RelationshipToFocusNote` contains only meaningful remaining values, expected to be `Self`, `OlderSibling`, and `YoungerSibling` unless the implementation keeps a small wiki-related related-note label for non-focus related notes. Removed values do not appear in production code, tests, or generated OpenAPI.

**Work:** Delete obsolete enum constants and fix compile errors by removing stale assertions/callers. Do not keep enum constants only to preserve historical labels.

**Verify:** `CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests com.odde.doughnut.services.GraphRAGServiceTest`.

## Sub-Sub-Phase 7.13.8 - Simplify `BareNote` graph wire shape

**Type:** Behavior / API cleanup.

**Pre-condition:** Focus note has `links` and `inboundReferences`, and wiki-hop flags are no longer the graph contract.

**Trigger:** Consumers serialize `GraphRAGResult.relatedNotes`.

**Post-condition:** `BareNote` serializes only the current graph fields:

- `uri`;
- `title`;
- `relationToFocusNote` when relevant for remaining structural sibling labels;
- `details`;
- `detailsTruncated`;
- `createdAt`.

It no longer exposes `linkFromFocus` or `linkHop2` (see **5.24e** for `target`, `parent`, `relation_type`, and `subject`). **Today** those two booleans are still on the class, in `@JsonPropertyOrder`, the protected constructor, `mergeIntoExisting`, `fromNote(..., boolean, boolean)`, and `@JsonProperty` accessors — all of that goes away in this slice.

**Work:** Remove the getters, constructor parameters, merge flags, JSON property order entries, and tests for removed fields. Relationship structure remains available only through note details/frontmatter and truncation.

**Verify:** `CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests com.odde.doughnut.services.GraphRAGServiceTest`; run `CURSOR_DEV=true nix develop -c pnpm generateTypeScript` if OpenAPI output changes.

## Sub-Sub-Phase 7.13.9 - Emit wiki-link URIs for graph notes

**Type:** Behavior.

**Pre-condition:** `BareNote` has the simplified wire shape from 7.13.8.

**Trigger:** GraphRAG serializes the focus note or a related note URI.

**Post-condition:** `BareNote.getUri()` (JSON `uri`) uses wiki-link text instead of `note.getUri()`. **Today** `getUri()` returns `note.getUri()` unchanged.

- focus note: `[[note title]]`;
- note rendered from another note's graph context: `[[notebook name: note title]]`.

`UriAndTitle`, if still used elsewhere in the graph package, follows the same graph-local URI rule or is deleted when no longer useful.

**Work:** Add a graph-local wiki-link formatter near `BareNote` or in a small cohesive helper. Do not change canonical product URLs or note identity outside GraphRAG serialization.

**Verify:** `CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests com.odde.doughnut.services.GraphRAGServiceTest`.

## Sub-Sub-Phase 7.13.10 - Phase 7.13 cleanup and OpenAPI alignment

**Type:** Structure.

**Pre-condition:** 7.13.1-7.13.9 are green.

**Trigger:** Search still finds removed graph concepts or stale API/client output.

**Post-condition:** GraphRAG has no parent/child graph relationship handlers, no removed `BareNote` fields, no obsolete wiki-hop flags, and no stale enum assertions. **`Note.isRelation()` does not exist** and no code references `Note::isRelation`; the Phase 7.13 plan and parent Phase 7 plan reflect implementation status.

**Work:** Search for `ChildRelationshipHandler`, `ParentRelationshipHandler`, `ContextAncestorRelationshipHandler`, `ParentSibling`, `TargetParent`, `SiblingOfReferencingNote`, `RelationshipOfTargetSibling`, `linkFromFocus`, `linkHop2`, `subject`, `relation_type`, `target`, graph-local `parent` uses, and **`isRelation` / `Note::isRelation`**. Remove the method from `Note` once call sites are gone; delete dead code and update this plan with discoveries that affect later Phase 7 work.

**Verify:** `CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests com.odde.doughnut.services.GraphRAGServiceTest`; run generated-client/type checks only if API files changed.

## Notes for Implementation

- If a sub-sub-phase changes the graph JSON contract, prefer assertions at the `GraphRAGService` / serialized DTO boundary over handler-private tests.
- If folder ancestry lookup requires new repository/service support, keep it narrowly named around folder crumbs for note context, not around legacy note ancestors.
- If a removed handler still appears necessary for a user-visible behavior, pause and split that behavior explicitly before preserving the handler.
