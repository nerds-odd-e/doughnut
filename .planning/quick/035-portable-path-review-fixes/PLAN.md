# Portable path review fix-up

**Status:** done

## Goal

Fix concrete issues surfaced by a post-hoc deep review of the completed
"Portable path domain vocabulary alignment" plan
(`.planning/quick/032-portable-path-domain-model/`, executed as commits
`870d7886f3..809745f8d7` on `main`, since retired). That plan renamed the
wiki-link path model, public contract, and persistence index to Portable-path
/ Wiki-link vocabulary; this plan closes gaps the review found: a real naming
collision, an explicit slice-3 requirement that was dropped, and leftover
camelCase survivors of vocabulary the original plan meant to eliminate.

This plan is behavior-preserving. Every slice is a pure naming/test-coverage
fix — no semantic change to resolution, rendering, navigation, refresh, or
rewrite behavior.

## Scope boundary

- Does not touch `.planning/quick/034-portable-path-ambiguity-behavior/` —
  SEED-009's deferred ambiguity/resolution-state behavior stays untouched.
- Does not address the pre-existing ~90%-duplicated native queries in
  `ResolvedWikiLinkRepository` (`findInboundReferrersForTargetByIdAscLimited`
  / `...BySeedLimited`) — confirmed pre-existing, not aggravated by the
  original plan, out of scope here.
- Does not touch `wikiLinkMarkup.ts`'s file size (251 lines) — already
  deliberately reviewed and left unsplit during the original plan's
  post-change-refactor.
- Does not touch the whitespace-only `WikidataControllerTests.java` reformat
  from the original plan's slice 2 commit — confirmed inert
  (`git diff -w --exit-code` clean).

## Discoveries from the review

- `WikiLinkResolver.java:41` has a pre-existing local
  `public record ResolvedWikiLink(String linkText, Note targetNote) {}` that
  now collides in simple name with the original plan's new JPA entity
  `com.odde.donut.entities.ResolvedWikiLink`. `ResolvedWikiLinkRefresh.java`
  uses both under the bare name `ResolvedWikiLink` in the same method,
  which is confusing and violates the original plan's own "old and new
  names never coexist" principle. The record's own fields (`linkText`,
  `targetNote`) are themselves the leftover wiki-link-destination `target`
  vocabulary the plan set out to eliminate (not a relationship role).
- Slice 3 of the original plan explicitly said to rename "...concept-path
  helpers/props/fixtures/**DOM attributes**..." to Wiki-link/Portable-path
  nouns, but `data-wiki-title`/`data-wiki-display` were never renamed.
  Slice 3/4's implementers misclassified these as persistence-layer names
  deferred to slice 5, but they are rendering-layer DOM markers unrelated to
  slice 5's backend table rename, and slice 5/6 never touched them either.
  Confirmed these are transient in-editor DOM markers — regenerated from
  Markdown on every render via `replaceWikiLinksInHtml`, never persisted in
  HTML form — so renaming them carries no data-migration concern.
- The original plan's completion-gate search only matched the literal phrase
  "concept path" (with a space), missing camelCase survivors of the same
  vocabulary: backend `WikiLinkMarkdown.isConceptPathHref` and frontend
  `hrefLooksLikeConceptNotePath`/`authoredHrefLooksLikeConceptNotePath`,
  including test-description strings that literally read "leftover ... with
  concept-path href".
- `WikiLink.java`'s `displayText` javadoc still says "...or same as target
  when absent" — stale vocabulary from before the DTO rename.
- `PortablePath.resolve(String focusNotebookName)` — the method born from
  merging `WikiLinkTargetReference.forToken`'s focus-notebook-fallback logic
  into the Jidoka-decided eager-parse design — has zero direct unit tests in
  `PortablePathTest.java`, only indirect coverage through full-Spring-context
  integration tests. (Not a new gap: the deleted `WikiLinkTargetReferenceTest`
  never tested `forToken` directly either.) It is now the single most
  semantically important method on `PortablePath` and deserves direct
  "small test"-style coverage.

## Slices

### 1. `WikiLinkResolver`'s local result type has its own name

**Status:** done
**Type:** Structure

Resolver result is now `WikiLinkResolution` (`authoredLink` / `destinationNote`);
JPA entity stays `ResolvedWikiLink`. Cache rebuild in `ResolvedWikiLinkRefresh`
uses both under distinct names. Accessor call sites in
`WikiLinkResolverYamlAndBodyIntegrationTest` were updated too.

**Learning:** leftover `target` locals/methods in `WikiLinkResolver` (e.g.
`resolveAnyTargetToken`) are pre-existing and not this type — leave them;
not justified by slice 2 (frontend DOM attributes).

Stop-safe outcome: resolver result and persistence entity have distinct names.

### 2. Wiki-link DOM markers use Portable-path vocabulary

**Status:** done
**Type:** Structure

In-editor DOM attributes are `data-portable-path` / `data-display-text`.
Production strings live in `wikiLinkDomMarkers.ts`
(`WIKI_LINK_PORTABLE_PATH_ATTR` / `WIKI_LINK_DISPLAY_TEXT_ATTR`); specs still
assert the literal DOM names. `e2e_test/` had no hits. `wikiLinkMarkup.ts`
left unsplit (plan scope).

**Learning:** first wiki_link.feature run failed 4 scenarios because the SUT
backend on 9081 was serving "No static resource" for API routes (stale Java
after a DevTools port clash). TCP healthcheck still passed. `pnpm sut:restart`
restored real API mapping; feature then 14/14. Not caused by the rename.

Stop-safe outcome: no `wiki-title` / `wiki-display` DOM attributes remain.

### 3. No camelCase "concept path" survivors remain

**Status:** done
**Type:** Structure

`isPortablePathHref` / `hrefLooksLikePortablePath` /
`authoredHrefLooksLikePortablePath`; WikiLink `displayText` javadoc uses
Portable path. Test descriptions no longer say "concept-path href".

**Learning:** remaining case-insensitive "concept" hits are `NoteConceptType`
and AI "Main concept" / "related concepts" fixtures — unrelated to wiki-link
paths. Java and TS href classifiers stay in their own subsystems (collapsing
them would be cross-subsystem; slice 4 does not justify it).

Stop-safe outcome: no wiki-link/portable-path identifier or test description
still spells "concept path".

### 4. `PortablePath.resolve` has direct unit coverage

**Status:** done
**Type:** Behavior

Three cases in `PortablePathTest.ResolveFocusNotebookFallback`: qualified
token ignores focus; unqualified falls back to focus notebook; unqualified
with no focus resolves empty. Implementation unchanged.

Stop-safe outcome: `PortablePath.resolve`'s three documented behaviors are
each pinned by a direct unit test.

## Slice wrap-up contract

For every executed slice: keep existing behavior green, run
`post-change-refactor`, update this plan with concise learnings, run the
listed verification, then commit and push before the next slice. After the
final slice, clean up spent planning history according to `planning.mdc`.
