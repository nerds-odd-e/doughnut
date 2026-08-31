# Portable path ambiguity behavior

**Status:** in progress (authorized via execute-plan)

Absorbs retired `.planning/seeds/SEED-009-portable-path-ambiguity-resolution.md`.
Prerequisite on `main`: Portable-path / Wiki-link vocabulary from retired
`.planning/quick/032-portable-path-domain-model/` plus review fix-ups from
retired `.planning/quick/035-portable-path-review-fixes/`.

## Goal

A shorthand Portable path resolves only when it identifies one destination
under the documented resolution scope. Otherwise it is unresolved/ambiguous,
and Donut asks for a longer path.

([ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md),
[ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md).)

## Live system (today)

- `WikiLinkResolver.titleOrAliasCandidates` returns the title∪alias union
  (dedupe by note id). `uniqueReadableNotebookMatch` skips unreadable then
  keeps a candidate only when cardinality is 1. `uniqueNotebookMatch` applies
  the same cardinality without a viewer. Two notes sharing an alias do not
  resolve. One remaining readable alias still does.
- Characterization:
  `ResolvedWikiLinkTitleResolutionTest.unqualified_link_does_not_resolve_when_same_title_in_different_folders`,
  `WikiLinkResolverYamlAndBodyIntegrationTest.wikiLinkResolver_doesNotResolveWhenTitleCollidesWithAlias`,
  `WikiLinkResolverYamlAndBodyIntegrationTest.wikiLinkResolver_doesNotResolveWhenTwoNotesShareAnAlias`,
  `WikiLinkResolverYamlAndBodyIntegrationTest.wikiLinkResolver_resolvesWhenOneNoteMatchesAsBothTitleAndAlias`,
  `WikiLinkResolverYamlAndBodyIntegrationTest.wikiLinkResolver_doesNotResolveWhenSeveralReadableNotesMatchQualifiedShorthand`.
- `NoteRealm.wikiLinks` includes resolved-index `RESOLVED` rows and
  `AMBIGUOUS` rows (`destinationNoteId` null) for cardinality `> 1`. Missing
  stays inferred from markup. Clicking ambiguous reuses
  `NoteUnresolvedWikiLinkModal` (guidance + “Point at an existing note”; no
  create-note). Confirming a destination calls
  `GET /api/notes/{note}/authored-portable-path` and replaces the shorthand
  with the returned full folder path, or `/Title` for a notebook-root
  destination (display text and `#prop:` preserved). Same-notebook body
  insert uses that authoring operation (unique → shorthand; colliding →
  lengthened path). Cross-notebook, property, overlap, accidental-match, and
  paste still use `buildWikiLinkText`.
- `GET /api/notes/{note}/authored-portable-path` authorizes `{note}` (source)
  but `PortablePathAuthoring` uniqueness uses the destination note as focus
  and `resolveAnyTargetWikiLinkToken` (no viewer). Cross-notebook repair
  therefore omits notebook qualification. Unreadable namesakes can force a
  longer path.
- Notebook health “Dead wiki links” uses `unresolvedWikiLinkTokens`, which
  treats ambiguous shorthands as missing.
- Exact path-shaped Portable paths already match folder trail plus display
  name (`ResolvedWikiLinkTitleResolutionTest` path-markdown / folder-path
  cases). Property validity is already checked after note resolution.

## Requirements

- Combine display-name and recognized-alias matches in one notebook scope,
  deduplicated by note id. One resolves, zero is unresolved, more than one is
  ambiguous.
- Notebook qualification changes scope but does not break ambiguity by
  database order. Viewer-unreadable candidates are skipped first; remaining
  readable cardinality decides. Do not invent a new same-named-notebook rule.
- Exact path-shaped paths keep matching folder trail plus display name. They
  must not fall back to shorthand. Property validity stays after note
  resolution.
- Ambiguous is an explicit non-navigable result, distinct from missing in
  user guidance. Both remain unresolved for navigation and the resolved-link
  index.
- Donut-authored repair, insertion, rewrite, and pasted-link conversion
  choose display-name shorthand only when unique; otherwise the complete
  normalized path. An exact root note uses `/Title` when `Title` is
  ambiguous. Cross-notebook output qualifies the same note portion.
- Resolution-dependent indexes and graph consumers follow later title, alias,
  location, deletion/restoration, and notebook changes.
- No compatibility mode, feature flag, or old/new API fields.

## Out of scope

- Accidental spelling-match first-match (`findAccidentalMatch` /
  `findAllAccidentalMatches` ordered by note id) — spelling recall, not
  Portable-path resolution.
- Source-relative and partial-folder-suffix Portable paths.
- Splitting `wikiLinkMarkup.ts` (left unsplit in 032/035).
- Duplicated native queries on `ResolvedWikiLinkRepository`.
- A dedicated rename of leftover `target` methods
  (`resolveAnyTargetToken`, `aliasTargetCandidates`, …). When a slice already
  edits those methods, finish destination vocabulary on the edited surface
  (032/035: old and new names must not coexist in one method).

## Current names (032/035)

Use these; do not revive retired nouns:

| Use | Name |
|---|---|
| In-memory resolver cache result | `WikiLinkResolution` (`authoredLink`, `destinationNote`) |
| JPA resolved-link row | `ResolvedWikiLink` |
| Wiki-link DTO | `authoredLink`, `portablePath`, `displayText`, `destinationNoteId` |
| Path parse / focus fallback | `PortablePath.resolve` (already has direct unit tests) |
| In-editor DOM markers | `data-portable-path`, `data-display-text` (`wikiLinkDomMarkers.ts`) |
| Href classifiers | `isPortablePathHref`, `hrefLooksLikePortablePath`, `authoredHrefLooksLikePortablePath` |

Gone: `WikiLinkTargetReference`, `WikiTitleCacheTitleResolutionTest`,
`targetToken`, `data-wiki-title` / `data-wiki-display`, camelCase
`*Concept*Path*`.

## Key design decisions

| Decision | Choice | Rationale |
|---|---|---|
| First honesty | Cardinality `!= 1` means no resolved row (dead-looking until slice 7) | Stopping after slice 1 already ends database-order navigation. Distinguishing ambiguous from missing waits until the click UX needs it. |
| Named three-state | Not until the public `WikiLink` contract (slice 6) | A `PortablePathResolution` type before any caller needs `AMBIGUOUS` is speculative structure. |
| Candidate set | Union title and alias, dedupe by note id | A title match must not hide an alias collision; two aliases on one note are one destination. |
| Authoring | Backend owns source-scoped Portable-path selection for the viewer | Uniqueness is readable cardinality in the notebook where the unqualified note portion would resolve; qualify when the source notebook differs. Slices 8–10 shipped destination-focus uniqueness without qualification — fix in slices 11–12. |
| Longer path | Full normalized folder path; exact root is `/Title` | Smallest already-readable exact form for the otherwise unlengthenable root collision. |
| Mutation consistency | One affected-scope re-resolution updates the derived resolved-link index | Cache history must not decide current Portable notebook tree semantics. |
| ADR closeout | Do not add execution-status notes to ADR 0001/0004 | The ADRs already state the live rule; a short window of code inconsistency until this plan ships is accepted. |

## Learnings from 032 / 035 (apply while executing)

- One observable behavior per slice; extra preconditions are later slices, not
  "and also cover…" bullets. 032's DOM-attribute rename was skipped because it
  was bundled and then misclassified.
- Completion greps must match identifiers and camelCase, not only spaced
  phrases.
- Do not run the full frontend suite for a backend-only slice, or the full
  backend suite for a frontend-only slice. Focused E2E is
  `wiki_link.feature` / `wiki_link_insert.feature` /
  `property_wiki_link.feature` / `path_markdown_link.feature` as listed per
  slice.
- When `WikiLink` (or another controller/DTO signature) changes, run
  `generate-api-client` before commit. No old-field adapters.
- `pnpm sut:healthcheck` can pass TCP while 9081 serves
  `No static resource api/…`. If wiki-link E2E 404s API routes, restart SUT;
  do not treat that as a product failure.
- Touch-set overlap (`wikiLinkMarkup.ts`, `replaceWikiLinksInHtml.ts`,
  `SearchForm.vue`, `WikiLinkResolver.java`) forces sequential wrap-up.

## Learnings from slices 1–10 (apply to remaining work)

- Cardinality belongs after skipping unreadable (`uniqueReadableNotebookMatch`).
  Applying `uniqueIfExactlyOne` at candidate construction zeros mixed-readability
  qualified aliases. Authoring still uses `resolveAnyTargetWikiLinkToken` — fix
  in slice 12, do not regress slice 3.
- `GET …/authored-portable-path` already has the source note in the path.
  Passing only `destinationNote` into `PortablePathAuthoring` is how repair
  dropped notebook qualification. Use the path variable.
- Reuse the existing dead-link modal + search for “choose”; do not add a
  candidate-list API. Relationship `targetSearchResult` stays (ADR 0001).
- `WikiLink.UNRESOLVED` is reserved; missing still inferred from markup. Do
  not emit `UNRESOLVED` rows until a slice needs them. Do not remove the enum.
- Collision cardinality is visible on `NoteRealm.wikiLinks` (`AMBIGUOUS` /
  empty `destinationNoteId`). Do not add a second empty-cache resolver test
  for the same scenario. Slice 14 deletes the copies already added.
- `SearchForm.vue` is ~224 lines; `replaceWikiLinksInHtml.ts` ~245. Extract
  spelling before more insert callers (slice 15). Do not grow either past 250.
- Vue `data` after `if (error) return` is still possibly undefined — narrow
  with `if (error || !data) return` in the same change, not a later fixup.
- `wiki_link_insert.feature` colliding insert asserts a `/Title]]` suffix, not
  a specific folder path. Tighten when that scenario is touched; do not treat
  the suffix as proof of exact-root authoring.
- Finish destination vocabulary on methods this plan already edits
  (`wikiLinkAnchorHtml` / `WikiLinkToken` still use leftover `target`). Do not
  sweep `authoredLinkMarkup.splitWikiLinkInner` or relationship `target*` in
  the same slice. `WikiLinkRewriteSupport` first-match stays slice 28.

## Slices

### 1. Duplicate display-name shorthand does not resolve

**Status:** done
**Type:** Behavior

Title-candidate cardinality `== 1` only. Duplicate display-name shorthand
has no resolved-link row; E2E `wiki_link.feature` treats `[[WikiDup Shared]]`
as a dead wiki link (missing-style UI until slice 7). Path-shaped cases
unchanged. Do not extract a cardinality helper until slice 2 unions aliases.

### 2. A title and an alias collision is unresolved

**Status:** done
**Type:** Behavior

Title∪alias then `uniqueIfExactlyOne`. `[[color]]` vs alias `color` has no
resolved row (resolver + `NoteControllerShowTests`). Alias-only first-match
unchanged. Shared `distinctByNoteId` for alias index and union.

### 3. Two notes sharing an alias make the shorthand ambiguous

**Status:** done
**Type:** Behavior

Alias-only collisions follow the same cardinality rule. Uniqueness runs
after skipping unreadable (`uniqueReadableNotebookMatch`). Skip-unreadable
qualified alias still resolves when one readable remains.

### 4. Two aliases on one note still identify one destination

**Status:** done
**Type:** Behavior

Pinned via title∪alias of the same token on one note (reachable equivalent
of duplicate alias rows). `distinctByNoteId` already kept cardinality at 1;
no production change.

### 5. Notebook-qualified shorthand does not break ties by note id

**Status:** done
**Type:** Behavior

`Notebook:shorthand` with several readable candidates has no resolved row.
Production already did this via `uniqueReadableNotebookMatch`; pinned in
`WikiLinkResolverYamlAndBodyIntegrationTest`. Skip-unreadable still green.

### 6. Wiki-link contract can name resolution states

**Status:** done
**Type:** Structure

`WikiLink.resolution` is `RESOLVED | UNRESOLVED | AMBIGUOUS`;
`destinationNoteId` optional iff `RESOLVED`. Emitted rows are still only
resolved-index `RESOLVED`. Frontend uses DTO fields directly (`isResolvedWikiLink`);
`wikiLinkParts` adapter removed. OpenAPI/TS regenerated.

### 7. Following an ambiguous link asks for a longer Portable path

**Status:** done
**Type:** Behavior

`AMBIGUOUS` rows emitted for cardinality `> 1`. Click stays on the source
note, explains several notes match, offers “Point at an existing note”
(`NoteUnresolvedWikiLinkModal`); create-note only for missing. E2E
`wiki_link.feature` duplicate-title scenario follows through the click.
`AmbiguousWikiLinks` owns listing; `wikiLinkClick.ts` owns click handling.

### 8. Choosing a destination writes the full normalized Portable path

**Status:** done
**Type:** Behavior

`PortablePathAuthoring.authoredPortablePath` / `GET …/authored-portable-path`
returns the full folder path. SearchForm uses it only for AMBIGUOUS repair.
Display text and `#prop:` preserved. Insert still uses `buildWikiLinkText`.

### 9. A root-note collision is authored as `/Title`

**Status:** done
**Type:** Behavior

Empty folder trail → `/Title` from `PortablePathAuthoring` (lengthened
exact-root spelling). Uniqueness/shorthand is slice 10. ADR 0004 records
the exact-root fallback. Controller test for colliding root display name.

### 10. Inserting a same-notebook Wiki link uses the shortest unambiguous path

**Status:** done
**Type:** Behavior

`PortablePathAuthoring` returns shortest unambiguous path. SearchForm body
insert uses `authoredPortablePathFor`. Insert E2E lives in
`wiki_link_insert.feature`. Cross-notebook still `buildWikiLinkText`.

### 11. Repairing an ambiguous link across notebooks qualifies the path

**Status:** done
**Type:** Behavior

`PortablePathAuthoring.authoredPortablePath` takes the source note; when
`sourceNote.getNotebook().getId()` differs from the destination's, the
result is qualified via `PortablePath.withNotebookName(destinationNotebook)`.
Note-portion selection (`shortestUnambiguousNotePortion`) is unchanged.
Pinned by `NoteControllerAuthoredPortablePathTests
.shouldQualifyPortablePathWithNotebookNameWhenDestinationNotebookDiffersFromSource`.
No other caller of `authoredPortablePath` existed. Viewer filtering
(unreadable namesakes) is still slice 12.

### 12. Authoring uniqueness skips unreadable namesakes

**Status:** done
**Type:** Behavior

`PortablePathAuthoring` injects `AuthorizationService` and
`displayNameUniquelyIdentifies` now calls the current user through
`WikiLinkResolver.readableNotebookMatchUniquelyIdentifies` (new public
wrapper around the existing private `uniqueReadableNotebookMatch`), instead
of the viewer-blind `resolveAnyTargetWikiLinkToken`. Pinned by
`NoteControllerAuthoredPortablePathTests
.shouldAuthorDisplayNameShorthandWhenOnlyUnreadableNamesakeShares`.

Stop-safe: authoring agrees with resolution’s readable cardinality.

### 13. Notebook health does not report ambiguous shorthands as dead

**Status:** done
**Type:** Behavior

`unresolvedWikiLinkTokens` renamed to `missingWikiLinkTokens`; a token is
reported only when unresolved and not ambiguous (`isAmbiguousToken`,
readable cardinality `> 1`). `DeadWikiLinkHealthRule` calls the renamed
method. Pinned by `DeadWikiLinkHealthRuleTest
.doesNotReportAmbiguousShorthandAsDeadButStillReportsMissingElsewhere`.
Post-change-refactor consolidated ambiguity detection with the existing
`AmbiguousWikiLinks` check (deleted its duplicate
`severalReadableDestinations`, both now call `WikiLinkResolver
.isAmbiguousToken`) and split the unrelated accidental-match lookup out of
`WikiLinkResolver.java` into `AccidentalWikiLinkMatches.java` to stay under
the 250-line file cap.

### 14. Collision tests live at the wikiLinks HTTP boundary

**Status:** done
**Type:** Structure

Deleted the three resolver-level empty-cache copies from
`WikiLinkResolverYamlAndBodyIntegrationTest`; `NoteControllerShowTests`
already pins the same scenarios as `AMBIGUOUS`. Kept the resolved-index
empty-row pin, the same-note title∪alias pin, and the qualified
several-readable pin untouched. No production code changed; no new
controller tests added.

### 15. SearchForm wiki-link spelling has one owner

**Status:** done
**Type:** Structure

Extracted `frontend/src/utils/sameNotebookWikiLinkAuthoring.ts`
(`authoredWikiLinkTokenForInsert`, `authoredWikiLinkTokenForAmbiguousRepair`)
owning the `authoredPortablePathFor` API call and `[[…]]` wrap.
`SearchForm.vue` 224→204 lines. Cross-notebook branch (`buildWikiLinkText`)
and the path-shaped/fallback branches of `wikiLinkSpellingForDestination`
unchanged. `InsertWikiLink.spec.ts` / `SearchDialog.deadWikiLink.spec.ts`
green, no behavior change.

### 16. Edited wiki-link markup uses destination names

**Status:** done
**Type:** Structure

Renamed `target` → `portablePath` in `wikiLinkAnchorHtml`,
`wikiAnchorToMarkdownToken`, `WikiLinkToken.vue`'s `resolved` computed, and
`replaceWikiLinksInHtml.ts` (including `authoredTokenFromWikiAnchor`, found
by post-change-refactor). `authoredLinkMarkup.ts`'s own `target` return
field (`splitWikiLinkInner`/`splitAuthoredToken`/`parseWholeWikiLinkItem`)
and backend `target*` names untouched, as scoped. `wikiLinkMarkup.spec.ts`
/ `propertyValueField.spec.ts` / `replaceWikiLinksInHtml.spec.ts` green.

### 17. Inserting a cross-notebook Wiki link qualifies that path

**Status:** done
**Type:** Behavior

`SearchForm.vue`'s `onInsertWikiLink` dropped the `otherNotebook` /
`buildWikiLinkText` special case; it always calls
`authoredWikiLinkTokenForInsert` (slice 15), which already qualifies
cross-notebook via slice 11's backend authoring. `wiki_link_insert.feature`
"Insert a qualified wiki link to a note in another notebook" and
`InsertWikiLink.spec.ts` green, no assertion changes needed (unique title,
same output via the new path). Same-notebook colliding scenario untouched.
`sameNotebookWikiLinkAuthoring.ts` kept its name (exported function names
don't claim same-notebook-only scope); its JSDoc comments updated to
reflect it now authors both same- and cross-notebook inserts.

### 18. Inserting a Wiki link as a property uses that path

**Status:** done
**Type:** Behavior

`onInsertWikiLinkAsProperty` calls `authoredWikiLinkTokenForInsert` instead
of `buildWikiLinkText`. Post-change-refactor extracted a shared
`insertAuthoredWikiLink(insertFn)` helper since it and `onInsertWikiLink`
had become structurally identical. `InsertWikiLink.spec.ts`'s property-insert
case now proves the backend-authored (folder-qualified) path is used, not
client-side title reconstruction. No E2E existed for the insert-as-property
UI action itself; `property_wiki_link.feature` only covers rendering.

### 19. Overlap "build a link" uses that path

**Status:** done
**Type:** Behavior

`appendOverlapWikiLinkToNoteContent` is now async, taking
`sourceNoteId`/`destinationNoteId` and calling `authoredWikiLinkTokenForInsert`
instead of `buildWikiLinkText`. `AccidentalMatchResolveDialog.vue`'s
`addAsOverlappedNote` awaits it. Spec rewritten with a folder-qualified
colliding-path case proving the switch. `isOverlapAlreadyDeclared`
deliberately still uses `buildWikiLinkText` for its search-token check
(converting it would need N concurrent API calls or a batched API — a
separate design decision, not this slice's scope).

### 20. Accidental-match "build a link" uses that path

**Status:** done
**Type:** Behavior

`MatchedNoteWikiLinkOrRelationshipOffer.vue`'s `onInsertWikiLinkAsProperty`
calls `authoredWikiLinkTokenForInsert` instead of `buildWikiLinkText`;
removed now-dead `buildWikiLinkText` import and `sourceNotebookId`
computed. `MatchedNoteWikiLinkOrRelationshipOffer.spec.ts` extended with a
folder-qualified-path test proving the switch, and a shared
`clickInsertWikiLinkAsProperty` test helper. Candidate ordering untouched.

### 21. Affected-scope re-resolution has one owner

**Status:** done
**Type:** Structure

`ResolvedWikiLinkService.refreshNotebookScope(Notebook, User)` delegates
to `ResolvedWikiLinkRefresh.refreshNotebookScope`, which loads all live
notes in the notebook (`noteRepository.findLiveNotesByNotebookIdOrderByIdAsc`,
same finder `DeadWikiLinkHealthRule` uses) and re-runs `refreshForNote` on
each. Pinned by `ResolvedWikiLinkServiceTest` proving a *different* note's
stale resolved row gets corrected (cross-note re-resolution, not just the
existing single-note refresh). Not wired into any mutation trigger yet.
Cross-notebook qualified referrers from other notebooks are out of scope
here — slices 22+ use the existing per-target-id
`findRowsReferringToNonDeletedNotesForTarget` for that.

### 22. Renaming a note updates shorthand cardinality

**Status:** done
**Type:** Behavior

`TextContentController.updateNoteTitle` calls
`resolvedWikiLinkService.refreshNotebookScope(note.getNotebook(), viewer)`
once after the if/else, guarded by `titleChanged` — covers both the
reference-handling rewrite branch and the plain-rename branch, closing the
gap where an unrelated referrer's shorthand cardinality wouldn't update.
Pinned by `TextContentControllerUpdateNoteTitleTests
.shouldReresolveNotebookShorthandsWhenRenameIntroducesOrRemovesACollision`.
No E2E needed; controller test was fast enough. Full backend suite green.

### 23. Creating a namesake updates shorthand cardinality

**Status:** done
**Type:** Behavior

Both `NoteConstructionService` creation entry points
(`createRootNoteWithWikidataService`, `createNoteFromExtractedSuggestion`)
now call `resolvedWikiLinkService.refreshNotebookScope(notebook, user)`
unconditionally after creation, replacing the narrower individual
`refreshForNote` calls (strictly broader, still covers the created note
itself). Pinned by `NotebookNoteCreateControllerTest
.creatingANamesakeMakesAnExistingUniqueShorthandAmbiguous`. Full backend
suite green.

### 24. Moving a note updates shorthand cardinality

**Status:** planned
**Type:** Behavior

**Precondition:** A move changes whether a shorthand is unique in its
scope (folder or notebook).
**Trigger:** Existing move runs.
**Postcondition:** Index/rendering follow the new tree.

Test first: one move collision at the move controller/cache boundary.

Verification: `pnpm backend:test_only`.

Stop-safe: location changes participate in uniqueness.

### 25. Deleting or restoring a note updates shorthand cardinality

**Status:** planned
**Type:** Behavior

**Precondition:** Delete or restore changes candidate cardinality.
**Trigger:** Existing delete/restore runs.
**Postcondition:** A previously ambiguous shorthand can resolve again
after the extra candidate is gone; a unique shorthand becomes unresolved
if its destination is deleted.

Test first: one delete and one restore at that boundary; fixture
completeness for delete FKs (`unit-testing.mdc`).

Verification: `pnpm backend:test_only`.

Stop-safe: presence in the tree, not cache history, decides uniqueness.

### 26. Changing aliases updates shorthand cardinality

**Status:** planned
**Type:** Behavior

**Precondition:** Alias candidates make a shorthand unique or ambiguous.
**Trigger:** Authored aliases are added, removed, or changed.
**Postcondition:** Link rendering and graph/index consumers reflect the
new cardinality without editing the source note.

Test first: add/remove a title-colliding alias at the text-content
controller boundary. Do not duplicate slice 22's graph assertions.

Implementation: feed affected old/new alias lookup keys into slice 21's
owner.

Verification: `pnpm backend:test_only`.

Stop-safe: aliases participate in uniqueness over time.

### 27. Rename rewrite lengthens the Portable path when needed

**Status:** planned
**Type:** Behavior

**Precondition:** A resolved link points at a note that is being renamed,
and the new display name would be an ambiguous shorthand.
**Trigger:** Existing reference-preserving rename rewrite runs.
**Postcondition:** The rewritten path still identifies that note uniquely
(full normalized path), preserving display text, property selector, and
wiki/path-Markdown spelling.

Test first: extend existing rename E2E with a destination namesake; one
property or path-Markdown case at the controller boundary if not already
covered.

Implementation: call the authoring operation after the new title is known.
Do not rewrite a title string in isolation. Do not handle move in this
slice.

Verification: `pnpm backend:test_only`; focused wiki/path-markdown E2E as
touched.

Stop-safe: rename maintenance cannot rewrite a good link into an ambiguous
shorthand.

### 28. Move rewrite lengthens or qualifies when needed

**Status:** planned
**Type:** Behavior

**Precondition:** A resolved link points at a note that is being moved
across folder or notebook.
**Trigger:** Existing reference-preserving move rewrite runs.
**Postcondition:** The rewritten path is still unique, qualified when the
source scope differs.

Test first: extend existing move E2E with a namesake. Replace
`WikiLinkRewriteSupport`'s "lowest note id wins" co-move comment/behavior
when that path would pick by id.

Verification: focused link E2E; `pnpm backend:test_only`.

Stop-safe: move maintenance agrees with uniqueness.

### 29. Already-ambiguous markup is not rewritten

**Status:** planned
**Type:** Behavior

**Precondition:** Authored markup is already an ambiguous shorthand.
**Trigger:** A rename or move rewrite that would otherwise touch links
runs.
**Postcondition:** Donut does not guess a destination or rewrite that
shorthand.

Test first: one controller case; do not re-assert successful unique
rewrites.

Verification: `pnpm backend:test_only`.

Stop-safe: maintenance does not invent a destination for ambiguous source.

### 30. Pasting a note URL uses backend-authored unique paths

**Status:** planned
**Type:** Structure

Unlocks slice 31. Replace frontend note-identity reconstruction in
`convertPastedNotePropertyLinks` with the source-aware authoring operation
for **unique** same-notebook pastes so the stored Wiki link is unchanged
for that case. Keep one unresolved URL → ordinary Markdown.

Verification: existing paste specs still green; `pnpm frontend:test` for
the paste files (and backend if a new endpoint is added).

Stop-safe: paste shares the authoring seam; collision spelling is slice 31.

### 31. Pasting a colliding note URL stores the full Portable path

**Status:** planned
**Type:** Behavior

**Precondition:** The user pastes a `noteShow` or `noteProperty` URL whose
note collides in the source scope (same-notebook, root `/Title`, or
cross-notebook).
**Trigger:** Donut converts the internal URL to notebook markup.
**Postcondition:** The stored Wiki link uses the same shortest unambiguous
Portable path as insertion, with separate display text and one encoded
property selector when present.

Test first: extend mounted paste specs; do not re-assert unique paste.

Reconcile Proposed ADR 0005 paste wording; preserve Proposed status.

Verification: focused paste specs; whitespace/lint via pre-commit.

Stop-safe: paste, insertion, repair, and rewrite agree on colliding
spelling.

## Completion gates

- Leave ADR 0001/0004 stating the live rule; do not add an implementation-gap
  or plan pointer.
- Grep wiki-link resolution (not accidental-match) for leftover first-match
  characterizations (`lowest_note_id`, `getFirst()` on title/alias candidate
  lists used as the destination).
- Run backend verify, frontend tests, `wiki_link.feature` /
  `wiki_link_insert.feature` / `property_wiki_link.feature` /
  `path_markdown_link.feature`, then retire this plan directory (no seed to
  update).

## Slice wrap-up contract

One slice per commit, ~5 minute fuzzy budget, red-to-green,
`post-change-refactor`, update this plan, listed verification, then commit
and push. Unfinished E2E stays `@wip`. After the last slice, delete this
directory (`planning.mdc`).
