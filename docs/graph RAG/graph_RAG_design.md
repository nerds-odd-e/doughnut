# Graph RAG Design

## Concepts

In Doughnut, notes live in notebooks and optional folders. **GraphRAG** builds a bounded JSON snapshot for AI context: the **focus** note plus **related** notes that fit a token budget.

Semantic links come from the **wiki title cache** (Phase 5): outgoing wiki targets and inbound referrers authorized for the viewer. Structural neighborhood is **folder-scoped**: older and younger “siblings” are the ordered peer list from `NoteService.findStructuralPeerNotesInOrder` (same folder, or notebook root when the note has no folder), without further partitioning by `Note.parent`.

There are no relationship notes as a separate graph entity: meaning lives in markdown/details, frontmatter, and wiki links.

## Data structures

### `BareNote`

JSON shape (see `BareNote.java`):

- **`uri`** — Graph-local wiki-style string from `GraphNoteWikiUri`: `[[title]]` when serialized as the focus note, otherwise `[[notebook name: title]]`.
- **`title`**
- **`relationToFocusNote`** — `RelationshipToFocusNote` when relevant (`OlderSibling`, `YoungerSibling`; `Self` on the focus note). Inbound wiki referrers added as related notes may have a null relation label.
- **`details`** / **`detailsTruncated`** / **`createdAt`**

`equals` / `hashCode` use `UriAndTitle` (underlying `Note` identity), not the serialized wiki `uri` string.

### `FocusNote` (extends `BareNote`)

- **`contextualPath`** — Single string of folder-name crumbs (notebook root → note’s folder), not note ancestors.
- **`links`** — Wiki URIs for outgoing resolved link targets from the focus note.
- **`inboundReferences`** — Wiki URIs for notes that link to the focus (duplicates the list-driven related notes for string convenience on the focus object).
- **`olderSiblings`** / **`youngerSiblings`** — Wiki URIs of related structural peers, in display order.

There is no `children` list on the focus note for graph expansion.

### `RelationshipToFocusNote`

Enum is minimal: `Self`, `OlderSibling`, `YoungerSibling` (`RelationshipToFocusNote.java`).

### `GraphRAGResult`

- **`focusNote`**
- **`relatedNotes`** — Prioritized `BareNote` entries within the token budget.

### `UriAndTitle`

Internal helper for deduplication keyed by `Note` (still exposes legacy `/n{id}` style in its own JSON shape if ever serialized elsewhere); **GraphRAG** wire `uri` for `BareNote`/`FocusNote` comes from `GraphNoteWikiUri`, not from `UriAndTitle.getUri()`.

## Handlers and retrieval

`GraphRAGService.retrieve` builds one `PriorityLayer` (threshold: three notes per sweep before yielding, though only one layer is wired today):

1. **`ReferenceByRelationshipHandler`** — Emits inbound wiki referrer notes (from `WikiTitleCacheService.referencesNotesForViewer`). Updates `FocusNote.inboundReferences` with each added note’s wiki URI.
2. **`OlderSiblingRelationshipHandler`** / **`YoungerSiblingRelationshipHandler`** — Walk structural peer lists; append wiki URIs to `olderSiblings` / `youngerSiblings` on the focus note.

`GraphRAGResultBuilder` fills `links` and initial `inboundReferences` on the focus from `WikiTitleCacheService` before the layer runs, then consumes handlers until the token budget is exhausted.

There are no dynamic handler injections, no parent/child/ancestor/cousin handlers, and no removed DTO fields (`linkFromFocus`, `linkHop2`, legacy `target` / `parent` / `relation_type` / `subject` on graph notes).

## Algorithm (summary)

1. Hydrate focus note; compute folder-scoped structural peers sharing the same tree parent.
2. Construct builder → focus note with folder crumbs, wiki `links`, and `inboundReferences` strings.
3. Run `PriorityLayer` over the handler list; each successful add decrements the related-note token budget via `TokenCountingStrategy`.
4. Return `GraphRAGResult`.

## Usage

Graph JSON is consumed by services that need a compact, AI-oriented view of a note (e.g. question generation, assistants). Canonical product URLs for notes remain `/n{id}` (or routed equivalents); **only** GraphRAG serialization uses the `[[…]]` wiki-style `uri` strings.
