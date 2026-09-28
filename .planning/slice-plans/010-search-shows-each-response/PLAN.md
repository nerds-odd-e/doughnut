# Web search shows each response without accumulating earlier ones

**Identity:** SEED-051#story-2
**Source:** [correction story](../../seeds/SEED-051-decommission-note-embeddings.md#story-2), from the
execution retrospective of SEED-051#story-1 (plan `009-retire-note-embeddings`, original contract
`f35fa810f1:.planning/slice-plans/009-retire-note-embeddings/PLAN.md`; reviewed commits `5003fbecc8`
slice 4 through `428ed03bb3` slice 11, merged to main as `ab08d6d046`, `aa6ae01a05`, `a0ab5999db`).

## Finding and provenance

Plan 009 slice 4 promised to "remove semantic controls, query branching, result merging, cache/state
inputs". Commit `5003fbecc8` removed the semantic request but kept `SearchResultsModel.mergeAndCacheResults`
merging each response into the hits already cached for the same key and scope, recorded as
"literal-with-literal cache merge kept (pinned by the within-note merge test)".

That merge was introduced (`83c1b0280a`) so separately arriving literal and semantic batches could be
combined; its removed doc comment said so. With one response per search it now only:

- keeps hits from an earlier response for the same trimmed key and scope, so a note renamed or trashed
  between two searches (e.g. typing `x` then `x `) stays listed until reload;
- unions global results into within-note results when a mounted `SearchResults` gains a `noteId`, the
  only case pinned by `SearchResults.spec.ts` "searches again within the note context and merges unique
  results by ascending distance";
- carries a duplicated "exact literal distance wins" rule for three hit kinds that equals "lower distance
  wins", because literal distances are never negative (`NoteSearchService`: exact 0, partial 0.9, aliases
  0.95/0.99; containers likewise).

## Goal and scope

Each completed web search caches and shows the hits of that response, ranked by the existing frontend
ordering. Included: `searchResultsModel.ts`, `mergeRelationshipLiteralSearchHits.ts` (reduced to ordering
one response and keeping the lower-distance duplicate within it), `executeDebouncedSearch.ts`, and the
pinned spec. Excluded: server search and `RelationshipLiteralHitSorter`, MCP, the ranking order itself
(exact title first, distance, current notebook, shorter title, alphabetical, id), previous-result display
while a new search is pending, the empty-result shortcut, and recent notes.

Preserved promises: title/alias matching, ranking, scopes, folder/notebook hits, target selection
(SEED-051#story-1 scope).

## Decisive premises

| Premise | Observation | Result |
| --- | --- | --- |
| Merging happens only in `mergeAndCacheResults` | `git grep -n 'mergeRelationshipLiteralSearchHits\|mergeAndCacheResults' -- frontend/src frontend/tests` | Two calls in `executeDebouncedSearch.ts`, one definition; no direct tests of the helper |
| Only one spec pins accumulation | Read `frontend/tests/components/search/SearchResults.spec.ts` caching block | The within-note test expects `[1, 2, 3]` (global N2 kept); same-notebook and pending-result tests do not depend on accumulation |
| Literal distances are non-negative | Read `NoteSearchService` and `RelationshipLiteralSearchHit` distance producers | Exact-literal preference equals minimum distance |
| Real journeys cover web search | `ls e2e_test/features/note_view/search_note.feature e2e_test/features/relationships/add_relationship.feature e2e_test/features/note_topology/wiki_link_insert.feature` | Present; passed 16/16 in plan 009 slices 4 and 7 |

## Proof commands

- **F:** `CURSOR_DEV=true nix develop -c pnpm frontend:test`, then
  `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit` (report vue-tsc's own exit code;
  do not pipe it into `tail`).
- **W:** `CURSOR_DEV=true nix develop -c pnpm cy:run --spec 'e2e_test/features/note_view/search_note.feature,e2e_test/features/relationships/add_relationship.feature,e2e_test/features/note_topology/wiki_link_insert.feature'`.

## Ordered slices

### 1. A new search response replaces the hits cached for its key
Type: Behavior
Status: done
Size: about 5 minutes active; F/W runtime excepted.
Proof: F and W. Reshape the pinned spec to its observable: after the context changes to note 1 and the
within-note response returns N1 and N3, the dropdown lists `[1, 3]`. Keep the same-notebook tie-break and
pending-previous-result specs passing unchanged.

Behavior: a mounted search has shown hits for key `x` → a later response for the same trimmed key and
scope (or the within-note context) arrives → only that response's hits are listed, in the existing order.

Replace `mergeAndCacheResults` with caching the ranked response; reduce the merge helper to ranking one
response, keeping the lower-distance hit when a response repeats a key, and delete the exact-literal
special cases. No new tests for the absence of merging.

Accepted proof: F — `frontend:test` 1943 passed, `SearchResults.spec.ts` "searches again within the note
context and lists only the within-note results" asserts `[1, 3]`; vue-tsc exit 0; after refactor (folded
`setCachedResult` into `cacheResults`) focused search specs 110 passed and vue-tsc exit 0. W — 16/16.
Helper renamed to `rankRelationshipLiteralSearchHits`; model method is `cacheResults`.

## Current decisions

Frontend ordering stays where it is. The web ordering and `RelationshipLiteralHitSorter` differ in tie-breaks
(exact title first, global-search current-notebook preference, shorter title); choosing one ranking owner is
an owner decision outside this correction.
