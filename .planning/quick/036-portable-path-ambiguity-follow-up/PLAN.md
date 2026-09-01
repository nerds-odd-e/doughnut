# Wiki-link ambiguity and Markdown URL conformance

**Status:** in progress (slices 1–8 done; next: slice 9)

## Goal

Make stored link syntax honest and deterministic: Portable paths use wiki
syntax, Markdown links keep URL semantics, canonical Donut note URLs contribute
semantic references by note ID, and wiki links remain correct across ambiguity,
viewer permissions, and note/folder location changes. Remove the retired
path-Markdown implementation and redundant tests rather than carrying
compatibility code for data that does not exist.

This plan follows:

- [ADR 0001 — Ubiquitous language](../../../docs/adrs/0001-ubiquitous-language.md)
- [ADR 0004 — OKF-compatible notebook Markdown](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
- [ADR 0005 — Web routes](../../../docs/adrs/0005-web-routes.md)

## Audit scope

- Baseline: `45ebb565437e2cf805f3ce9c9a68f5a52d7843e1` (parent of the retired
  plan's creation commit).
- End: `14bca6b2a8` (retirement of
  `.planning/quick/034-portable-path-ambiguity-behavior/`).
- Included all commits that updated the retired plan, the slice-26 changes
  bundled in `9a473a7e37`, and the in-scope lock follow-up `8927c77e4c`.
- Excluded interleaved note-level and question-generation changes except where
  Portable-path scope refresh now calls those concepts.
- Assumption supplied by the decision maker: no deployment stores semantic
  `[label](/n1234)` links or path-shaped Markdown links, so no data migration or
  compatibility reader is required.

## Findings

### Markdown URL conformance

1. Backend and frontend scanners reinterpret Markdown links such as
   `[Target](/folder/Target.md)` as Portable-path wiki links. They index,
   decorate, repair, and rewrite an href that belongs to Markdown URL
   semantics. Source-relative `[Target](folder/Target.md)` must remain equally
   ordinary.
2. Rich-editor serialization converts `/n1234` and any absolute URL whose path
   resembles a Donut note route into title-only wiki syntax. This discards the
   authoritative note ID and href; absolute-origin validation is absent.
3. The paste pipeline converts note-property URLs to Portable-path wiki links
   and contains now-obsolete identity lookup and backend-authoring plumbing.
   `ConvertPastedNotePropertyLinksContext.sourceNotebookId` is unused.
4. Backend extraction explicitly excludes `/n1234` from the path-Markdown
   branch and has no note-ID URL reference kind. The resolved index, outgoing
   graph, inbound references, and focus context therefore cannot consume the
   deterministic URL form chosen in ADRs 0001/0004/0005.
5. `path_markdown_link.feature` plus path-Markdown cases across relationship,
   parser, rewrite, property-field, and dead-link tests specify behavior that is
   no longer part of the product. With the stated no-data assumption, these
   tests and their production branches are dead rather than compatibility
   coverage.

### User-visible correctness

6. `SearchForm.vue` sends only `AMBIGUOUS` repair through backend authoring.
   Pointing an ordinary dead wiki link at a colliding title still calls
   `buildWikiLinkText`, recreating the ambiguity the user was repairing.
7. `AccidentalMatchResolveDialog.vue` detects an existing `overlaps` item by
   reconstructing a shorthand with `buildWikiLinkText`. A backend-authored
   full/qualified path is not recognized, so the same destination can be added
   twice with different spellings.
8. Moving a note between a folder and notebook root inside one notebook does
   not rewrite exact path-shaped wiki links. A stale resolved row can mask the
   incorrect stored Portable path.
9. Reparenting, merging, or dissolving a folder inside one notebook likewise
   changes descendant Portable paths without rewriting exact inbound wiki
   links.
10. Cross-notebook folder moves do not re-resolve shorthand cardinality in both
    source and destination scopes. Unrelated referrers can retain stale rows
    when the moved subtree removes or introduces a title/alias collision.
11. `wikiLinksForViewer` trusts a global resolved row written for the user who
    last refreshed the source. A later collision or a viewer with a different
    readable candidate set can receive the wrong resolution.

### Cohesion, dead code, and performance

12. `refreshNotebookScope` rebuilds property, alias, and note-level indexes for
    every live note when only Portable-path resolution changed. This expands
    write/lock fan-out and couples wiki-link correctness to unrelated concepts.
13. `sameNotebookWikiLinkAuthoring.ts` authors same- and cross-notebook
    insertion, repair, overlap, and paste paths; its filename is stale.
14. `buildWikiLinkText.ts` remains in production only at the two incorrect
    fallback sites in findings 6–7. Once those callers use backend authoring,
    the utility and its direct test are dead.
15. The resolver queries the same candidates twice for a missing wiki token
    (`resolveToken` then `isAmbiguousToken`), while
    `ResolvedWikiLinkService` separately combines cached resolved rows with
    `AmbiguousWikiLinks`. One tri-state classification should own
    `RESOLVED | UNRESOLVED | AMBIGUOUS`.
16. `8927c77e4c` removed the note-row lock from `NoteLevelIndexService` while it
    still performs read-then-insert for a shared primary key. Concurrent first
    refreshes can both see no row and make one request fail on the unique key.

### Redundant tests / file cap

17. `frontend/tests/notes/NoteEditableContent.paste.spec.ts` is over 250 lines.
    Its repeated note-property conversion cases disappear with the conversion
    pipeline; retain mounted coverage for generic paste and exact URL
    preservation.
18. Consolidate these pairs while their capability is edited:
    - the two authoring-path cases in `InsertWikiLink.spec.ts`;
    - the two property-write cases in
      `MatchedNoteWikiLinkOrRelationshipOffer.spec.ts`;
    - the basic and folder-qualified append cases in
      `appendOverlapWikiLinkToNoteContent.spec.ts`.
19. Keep the controller collision cases for title/title, title/alias, and
    alias/alias. They exercise materially different candidate construction at
    one stable boundary.

## Key design decisions

| Decision | Choice | Rationale |
|---|---|---|
| Portable links | Only wiki syntax carries a Portable path | Markdown hrefs retain normal URL meaning. |
| Donut note URLs | Recognize exact canonical `/n<ID>` and absolute HTTP(S) URLs on a configured Donut origin | The href's ID is authoritative; display text never resolves a destination. |
| Absolute origin | One configured canonical origin; production is `https://doughnut.odd-e.com` | Do not infer that an arbitrary host's `/n<ID>` is local. |
| URL persistence | Preserve authored relative/absolute spelling through paste, edit, render, and export | Full URLs work away from the authoring host; root-relative URLs retain their acknowledged host dependency. |
| Missing URL target | Leave the Markdown link ordinary | A missing note-ID URL is not a dead Portable path and does not open wiki repair UI. |
| URL rewrites | Never rewrite a canonical note URL for title, folder, or notebook changes | Server note ID is its destination identity. |
| Wiki authoring | Backend `authored-portable-path` remains the client authoring seam | It owns cardinality, qualification, folder paths, root fallback, and property suffixes. |
| Viewer display state | Classify current wiki content for the current viewer at the `wikiLinks` boundary | A viewer-dependent answer cannot safely come from one global cache row. |
| Persistent index | Keep the existing resolved-only table for the safe slices | Broadening it still requires the remaining Jidoka decision below. |
| Scope refresh | Refresh content-derived indexes only for the changed note; re-resolve outgoing rows for the affected notebook | Avoid unrelated property/note-level writes while preserving alias-before-resolution ordering. |
| Tests | One boundary test per observable; pure parser tests only for syntax/origin edge cases | Remove implementation-pinning and redundant coverage. |

## Slices

### 1. Markdown links round-trip as URLs

**Status:** done
**Type:** Behavior

Markdown links (relative, absolute Donut, note-property) paste/edit/serialize
as ordinary `[text](href)` — Turndown no longer wiki-ifies note-show anchors;
paste-to-wiki conversion and its `sourceNotebookId` plumbing removed.
`markdown_link.feature` covers the round-trip.

**Learning:** path-Markdown wiki-DOM upgrade paths remain for slice 2.

### 2. File-looking Markdown URLs keep ordinary link UI

**Status:** done
**Type:** Behavior

File-looking Markdown URLs render as ordinary anchors (no wiki styling/repair)
and serialize unchanged. Frontend path-Markdown extraction/DOM upgrades and
related tests removed; visible behavior in `markdown_link.feature`.
`path_markdown_link.feature` and obsolete relationship path-Markdown scenarios
deleted early (frontend no longer matches them).

**Learning:** frontend path-Markdown UI removed; backend extraction removed in
slice 3; `path_markdown_link.feature` deleted.

### 3. File-looking Markdown URLs do not create wiki references

**Status:** done
**Type:** Behavior

Backend no longer extracts/rewrites file-looking Markdown as wiki; overlaps
reject them. Boundary regression
`file_looking_markdown_href_is_not_indexed_as_wiki_link`; E2E coverage in
`markdown_link.feature`.

### 4. Frontend Portable-path authoring has a capability name

**Status:** done
**Type:** Structure

Renamed `sameNotebookWikiLinkAuthoring.ts` → `wikiLinkAuthoring.ts`;
consolidated InsertWikiLink authoring-path examples. Prepares slice 5.

### 5. Pointing a dead wiki link at a colliding note is unambiguous

**Status:** done
**Type:** Behavior

Missing and ambiguous “point at existing note” repairs use backend
`authoredWikiLinkTokenFromOriginalPath` (folder-qualified when colliding).
`buildWikiLinkText` remains only for AccidentalMatchResolveDialog (slice 6).

### 6. An authored overlap is recognized by destination

**Status:** done
**Type:** Behavior

Overlap detection uses resolved `wikiLinks` + `destinationNoteId` (not
reconstructed shorthand). `buildWikiLinkText` deleted. Finding-18 pairs
consolidated; E2E covers existing qualified overlap.

### 7. Authored note references have a syntax-neutral model

**Status:** done
**Type:** Structure

`AuthoredNoteReference` / `AuthoredNoteReferences` (backend) and
`authoredNoteReference.ts` (frontend) distinguish wiki Portable-path vs
note-ID URL kinds; extraction emits wiki only; public `WikiLink` unchanged.
Prepares slice 8.

### 8. A root-relative Donut note URL contributes one semantic reference

**Status:** done
**Type:** Behavior

Exact `[display](/nID)` indexes by note ID (missing IDs stay ordinary Markdown).
Public `WikiLink.portablePath` → `target`; TypeScript client regenerated.
Editor keeps ordinary URL anchors.

### 9. A full Donut note URL contributes the same semantic reference

**Status:** planned
**Type:** Behavior

**Pre-condition:** A source note contains
`[any display](https://doughnut.odd-e.com/n19921)` and that deployment's note
exists.

**Trigger:** The source is saved/indexed or a reference/graph consumer opens
either note.

**Post-condition:** The href's note ID supplies the semantic destination and
the absolute href remains unchanged and usable outside its authoring host.

- Introduce one configured canonical Donut origin, with production default
  `https://doughnut.odd-e.com`; tests override it rather than accepting every
  host whose path looks local.
- Accept exact HTTP(S) origin + canonical note path. A foreign-origin
  `/n<ID>` remains an ordinary external link.
- Apply the same reference behavior as slice 8 without duplicating the note-ID
  resolution path.
- Cover recognized vs foreign origins at the parser/controller boundary and
  the main inbound-reference behavior in `markdown_link.feature`.

Verification: full backend/frontend unit suites; focused
`markdown_link.feature`.

### 10. Notebook resolution refresh excludes unrelated derived indexes

**Status:** planned
**Type:** Structure

Split the operations currently conflated by `refreshNotebookScope`:

- note-local refresh rebuilds that note's property/alias/level indexes and
  outgoing semantic-link rows;
- notebook resolution-scope refresh rebuilds only outgoing resolution rows and
  existing property-link validity for live notes in that notebook.

Creation and alias-content updates refresh the changed note locally before the
notebook resolution pass. Title, move, delete, and restore mutations perform
only the affected resolution-scope pass. Remove the internal test that expects
notebook scope to repair an alias index created by bypassing production
boundaries. This directly prepares slice 11.

Verification: full backend unit suite.

### 11. Cross-notebook folder moves refresh both shorthand scopes

**Status:** planned
**Type:** Behavior

**Pre-condition:** A folder subtree contains a title or alias that is unique in
the source notebook and collides—or resolves a collision—in the destination.

**Trigger:** The learner moves or merges the subtree into another notebook.

**Post-condition:** Unrelated shorthand referrers in both notebooks immediately
change between `RESOLVED` and `AMBIGUOUS` as appropriate.

Use the narrowed scope operation from slice 10 after existing inbound/outgoing
wiki rewrite. Cover removal from the source and addition to the destination at
the controller boundary, with the main transition in
`folder_organization.feature`.

Verification: full backend unit suite; focused `folder_organization.feature`.

### 12. Moving a note preserves exact wiki Portable paths

**Status:** planned
**Type:** Behavior

**Pre-condition:** Another note contains an exact folder/root wiki link to the
note being moved.

**Trigger:** The learner moves the destination into a folder or back to the
notebook root.

**Post-condition:** Stored wiki syntax contains the new Portable path, keeps
display text, property selector, and optional `.md`, and resolves after a fresh
index rebuild. Canonical Donut note URLs remain unchanged.

Do not rewrite an unqualified shorthand whose destination remains unique. Add
controller cases for folder and root directions and extend the location-change
scenario in `wiki_link.feature`.

Verification: full backend unit suite; focused `wiki_link.feature`.

### 13. Reparenting a folder preserves exact descendant wiki links

**Status:** planned
**Type:** Behavior

**Pre-condition:** A note links by exact wiki Portable path to a descendant of
a folder.

**Trigger:** The learner reparents the folder inside the notebook.

**Post-condition:** Inbound wiki paths reflect the new folder trail and resolve
after rebuilding the index; note-ID URLs remain unchanged.

Reuse the location-rewrite seam from slice 12 for every live note in the moved
subtree, including referrers inside and outside it.

Verification: full backend unit suite; focused `folder_organization.feature`.

### 14. Dissolving or merging a folder preserves descendant wiki links

**Status:** planned
**Type:** Behavior

**Pre-condition:** Exact wiki links point to notes whose folder trail will
change because a folder is dissolved or merged.

**Trigger:** The learner confirms dissolve or merge.

**Post-condition:** Every affected wiki target uses the promoted/merged trail
and continues resolving; unrelated wiki links and note-ID URLs are unchanged.

Capture affected notes before structural rows are removed, then reuse the
location-rewrite seam. Treat dissolve and merge as examples of the same
observable rather than separate algorithms.

Verification: full backend unit suite; focused `folder_organization.feature`.

### 15. Wiki candidate cardinality has one tri-state result

**Status:** planned
**Type:** Structure

Make one resolver operation classify a wiki Portable-path reference for a
scope/viewer as `RESOLVED(destination)`, `UNRESOLVED`, or `AMBIGUOUS`. Use it
for row rebuild, notebook health, and viewer DTO mapping; remove
`AmbiguousWikiLinks` and the duplicate candidate query from
`missingWikiLinkTokens`. Note-ID URL references bypass candidate cardinality
and retain their deterministic target.

Keep `WikiLinkResolver.java` and `ResolvedWikiLinkService.java` below 250
lines; extract only a cohesive candidate-query concept if needed. This directly
prepares slice 16.

Verification: full backend unit suite.

### 16. Showing a note uses current viewer-specific wiki resolution

**Status:** planned
**Type:** Behavior

**Pre-condition:** A source note's cached wiki row predates target-cardinality
change or was produced by a user with a different readable candidate set.

**Trigger:** The current user opens the source or an outgoing graph consumer
asks for targets.

**Post-condition:** Every wiki token is reported from current content as
`RESOLVED`, `AMBIGUOUS`, or omitted-as-missing for that viewer; stale cached
state cannot navigate to the wrong note. Deterministic note-ID URL references
remain filtered by the current viewer's authorization.

- Drive the regression through `NoteController.showNote` with a qualified
  cross-notebook source, first unique then colliding.
- Add the different-viewer example at the same stable boundary.
- Keep outgoing focus-context traversal consuming this corrected boundary.
- Do not broaden persistent indexing in this slice.

Verification: full backend unit suite; focused `wiki_link.feature`.

### 17. Concurrent first note-level refreshes do not fail

**Status:** planned
**Type:** Behavior

**Pre-condition:** No `note_level_index` row exists for a note with a valid
`note_level`.

**Trigger:** Two transactions refresh that note concurrently.

**Post-condition:** Both operations complete and one correct index row remains;
no duplicate-key, lock-timeout, or deadlock escapes.

Use an atomic database operation or a narrowly scoped lock for the single row.
Do not restore whole-notebook note locking. Prove the behavior with two real
transactions/connections at the service boundary.

Verification: full backend unit suite.

## Remaining Jidoka decision

**Reverse references after a qualified ambiguous→unique transition.** A
resolved-only index has no row from which to discover a formerly ambiguous
referrer in another notebook. Complete persistent inbound/reference sampling
requires broadening the existing index to retain unresolved/ambiguous authored
tokens or adding another lookup. ADR 0004 still specifies one resolved index
and no second index, so expanding the persistent reverse-reference guarantee
requires human direction.

Slice 16 fixes current note display and outgoing traversal without claiming
persistent reverse-index consistency.

## Completion gates

- `scripts/check_diff_whitespace.sh`
- `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
- `CURSOR_DEV=true nix develop -c pnpm frontend:test`
- Focused changed E2E specs: `markdown_link.feature`, `wiki_link.feature`,
  `folder_organization.feature`, `accidental_match_reveal.feature`, and any
  relationship feature edited while removing obsolete cases.
- Delete `path_markdown_link.feature`; no skipped replacement scenarios.
- `CURSOR_DEV=true nix develop -c pnpm generateTypeScript` in slice 8 if the
  public `WikiLink` shape changes.
- Post-change-refactor after every slice: no path-Markdown branch, dead client
  authoring/conversion utility, duplicate boundary test, stale capability name,
  or touched file over 250 lines.
- Commit and push each completed slice only during later authorized execution.
