# Make inbound wiki-link matching consistent with outgoing resolution

Status: done
Source: [SEED-021 story 1](../../seeds/SEED-021-inbound-wiki-reference-nfkc-mismatch.md#story-1),
refined 2026-09-17. Reported and diagnosed live against the running dev app/DB
(notebook "jp7", notes n102472/n102466). The owner authorized planning, not
execution.

## Goal and scope

A notebook owner who authors a wiki link/alias reference to another note can
trust the target note's References panel shows that incoming reference back
whenever outgoing resolution from the source note already resolves it,
regardless of which Unicode form the shared title text takes.

Included:

- fix the inbound wiki-link candidate lookup in
  `AuthoredNoteReferenceInboundFacade.wikiCandidateRowsForTarget` so its
  direct title-lookup key compares raw/lowercase (no NFKC folding), mirroring
  outgoing resolution's `LOWER(n.title) = LOWER(:noteTitle)` comparison in
  `NoteRepository.findByNotebookNameAndNoteTitleOrderByIdAsc`;
- a regression test reproducing the fullwidth-solidus case, added to the
  existing stable-boundary test class `AuthoredNoteReferenceInboundFacadeTest`.

Excluded:

- any change to alias lookup keys (`NoteAliasIndexRepository`), which stay
  NFKC-normalized on both the write-time index and the read-time lookup,
  mirroring `WikiLinkNoteCandidates.aliasTargets` — already symmetric and
  correct today;
- any change to `PathShapedTarget`/Portable-path suffix-matching semantics
  (Accepted ADR 0004) — the two `LIKE` suffix patterns in the same method
  already use plain lowercase, not NFKC, and are left untouched;
- any change to `FrontmatterAliases`'s existing prohibition on slash
  characters in explicit `aliases:` frontmatter entries;
- broader removal of slash-based title/path matching — investigated during
  refinement and confirmed to be current, ADR-0004-accepted, actively
  maintained architecture, not legacy (see the seed's Open Decisions); no
  removal work is in scope.

## Existing-solution and architecture assessment

Current code was inspected directly (no automated checks run during
planning): `AuthoredNoteReferenceInboundFacade.java`,
`AuthoredNoteReferenceRowRepository.java`, `WikiLinkNoteCandidates.java`,
`NoteRepository.java`, `FrontmatterAliases.java`.

| Responsibility | Existing solution and decision |
| --- | --- |
| Outgoing title resolution | `NoteRepository.findByNotebookNameAndNoteTitleOrderByIdAsc` compares `LOWER(n.title) = LOWER(:noteTitle)` — plain SQL lowercase, no NFKC. Unaffected; this is the reference behavior the inbound side must mirror. |
| Outgoing alias resolution | `WikiLinkNoteCandidates.aliasTargets` NFKC-normalizes the authored token via `FrontmatterAliases.normalizedLookupKey` before matching `NoteAliasIndex` rows, whose stored `aliasLookupKey` is written NFKC-normalized too (symmetric by construction). Unaffected. |
| Inbound candidate lookup (the bug) | `AuthoredNoteReferenceInboundFacade.wikiCandidateRowsForTarget` (line 168) currently adds `FrontmatterAliases.normalizedLookupKey(target.getTitle())` to `titleAndAliasLookupKeys`, NFKC-folding the note's own title before it is compared — via `LOWER(r.wikiNotePortion) IN :titleAndAliasLookupKeys` in `AuthoredNoteReferenceRowRepository.findWikiCandidatesForNotebookScope` — against the raw, never-normalized `wiki_note_portion` column. Fix: replace that entry with the already-computed `lowerCaseTitle` local variable (plain `.toLowerCase(Locale.ROOT)`, no NFKC), matching outgoing's raw comparison exactly. The alias entries already in the same list (`aliasRow.getAliasLookupKey()`) are left untouched, preserving today's correct, symmetric alias behavior. |
| Path-shaped suffix fallback | The same method already builds its two `LIKE` suffix patterns (`"%/" + lowerCaseTitle[.md]`) from plain lowercase, not NFKC — confirming the fix direction is consistent with an already-correct sibling comparison in the very same method, not a new convention. |

No Accepted-ADR conflict: ADR 0004's Portable-path/path-shaped matching is
untouched. This removes an inconsistency local to one lookup-key computation
for direct title matches; it is not a Proudly-Found-Elsewhere candidate (no
existing normalized-title comparison helper exists to reuse — the fix reuses
a local variable already present in the same method).

## Outside-in proof ownership

| Promise | Owning slice | Observable proof |
| --- | --- | --- |
| A referrer whose authored wiki reference targets a note by a title containing an NFKC-foldable character (e.g. fullwidth solidus `／`) is returned as an inbound referrer | 1 | New test in `AuthoredNoteReferenceInboundFacadeTest`: target note titled with a fullwidth solidus, source note authoring a matching wiki reference via `AuthoredNoteReference.WikiPortablePathTarget.fromAuthoredInner(...)`, `distinctReferrerNotesForViewer` contains the source. Fails today (empty list), passes after the fix. |
| Existing inbound, outgoing, alias, and path-shaped behavior is unchanged | 1 | Existing `AuthoredNoteReferenceInboundFacadeTest` and `WikiLinkResolver*Test` suites stay green as part of the full backend suite. |

No E2E or API-client generation is selected: this is a backend-only
persistence/query fix with no new HTTP contract shape or UI change (the
References panel already renders whatever the backend returns). No database
schema or ERD change.

## Ordered slices

### 1. Inbound wiki-link matching recognizes titles containing NFKC-foldable characters

Type: Behavior
Status: done
Proof: `CURSOR_DEV=true nix develop -c pnpm backend:verify` green (2469
tests, 0 failures), including the new
`matchesAWikiReferenceAuthoredAgainstATitleContainingAnNfkcFoldableCharacter`
case in `AuthoredNoteReferenceInboundFacadeTest`, confirmed failing before the
fix (empty referrer list) and passing after. Focused rerun:
`CURSOR_DEV=true nix develop -c ./backend/gradlew -p backend test
-Dspring.profiles.active=test --build-cache --parallel --tests
"*AuthoredNoteReferenceInboundFacadeTest*" --tests "*WikiLinkResolver*Test*"`
green.

Behavior: Given a target note whose title contains a character NFKC
compatibility normalization would fold (e.g. "how 手段／方法", fullwidth
solidus U+FF0F) and a source note authoring a wiki reference using the
identical title text, when the target's inbound references are computed via
`AuthoredNoteReferenceInboundFacade.distinctReferrerNotesForViewer`, then the
source note is included as a referrer — matching what outgoing resolution
from the source note already resolves today.
