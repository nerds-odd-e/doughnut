# Portable path review fix-up

**Status:** in progress — slice 1 done

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

**Status:** planned
**Type:** Structure

Rename the transient in-editor DOM attributes `data-wiki-title` and
`data-wiki-display` to `data-portable-path` and `data-display-text`,
completing the DOM-attribute rename slice 3 of the original plan specified
but missed. These attributes are regenerated from Markdown on every render
(never persisted in HTML form), so this is a pure rename with no
data-migration concern. This unblocks the immediate next behavior: existing
rich-editor round-trip and dead/live wiki-link rendering tests passing
unchanged under the new attribute names.

- Rename both attributes and every read/write site in
  `frontend/src/utils/wikiLinkMarkup.ts` and
  `frontend/src/components/form/replaceWikiLinksInHtml.ts`.
- Update `frontend/src/components/notes/WikiLinkToken.vue` if it reads either
  attribute directly.
- Update the ~10 spec files asserting on these attributes:
  `wikiLinkMarkup.spec.ts`, `propertyValueField.spec.ts`,
  `replaceWikiLinksInHtml.spec.ts`, `quillHtmlToMarkdown.spec.ts`,
  `RichMarkdownEditor*.spec.ts`, `NoteTextContent.wikiLinks.spec.ts`,
  `QuillEditor.spec.ts` (confirm exact list via
  `grep -rl "data-wiki-title\|data-wiki-display" frontend/src frontend/tests`
  before starting — the review's list may not be exhaustive).
- No behavior change: same live/dead rendering, same round-trip to Markdown,
  same click/navigation behavior — only the attribute names change.

Verification:

- Run `CURSOR_DEV=true nix develop -c pnpm frontend:test`.
- Run the focused wiki-link E2E feature:
  `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/note_topology/wiki_link.feature`.

Stop-safe outcome: no DOM attribute in the rich-editor wiki-link rendering
path still spells "wiki-title" or "wiki-display"; rendering and round-trip
behavior is unchanged.

### 3. No camelCase "concept path" survivors remain

**Status:** planned
**Type:** Structure

Rename the camelCase survivors of "concept path" vocabulary that the
original plan's literal-phrase completion-gate search missed, plus one
adjacent stale javadoc noticed in the same review. This unblocks the
immediate next behavior: existing path-Markdown href classification and
dead-link/leftover-markup detection tests passing unchanged under the new
names.

- Backend: rename `WikiLinkMarkdown.isConceptPathHref` to
  `isPortablePathHref` (private method, update its 3 call sites in the same
  file).
- Frontend: rename `routes/noteShowLocation.ts`'s
  `hrefLooksLikeConceptNotePath` to `hrefLooksLikePortablePath`, and
  `frontend/src/utils/authoredLinkMarkup.ts`'s
  `authoredHrefLooksLikeConceptNotePath` to
  `authoredHrefLooksLikePortablePath`. Update call sites in
  `wikiLinkMarkup.ts`, `authoredLinkMarkup.ts`, `replaceWikiLinksInHtml.ts`,
  `SearchForm.vue`, and the 3 test files that reference these functions —
  including rewording the test-description strings in
  `replaceWikiLinksInHtml.spec.ts` that literally read "leftover ... with
  concept-path href".
- Fix the stale javadoc in
  `backend/src/main/java/com/odde/donut/controllers/dto/WikiLink.java`: the
  `displayText` field's javadoc says "...or same as target when absent" —
  reword to "...or same as the link's Portable path when absent."
- No behavior change: same href classification, same leftover-markup
  detection — only names and one doc comment change.

Verification:

- Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.
- Run `CURSOR_DEV=true nix develop -c pnpm frontend:test`.
- Confirm zero remaining case-insensitive "concept" hits:
  `grep -rin "concept" backend/src frontend/src frontend/tests` (expect no
  output, or only genuinely unrelated matches).

Stop-safe outcome: no production identifier, test description, or doc
comment in the wiki-link/portable-path code paths still spells "concept
path" in any casing.

### 4. `PortablePath.resolve` has direct unit coverage

**Status:** planned
**Type:** Behavior

**Precondition:** `PortablePath.resolve(focusNotebookName)` already
implements the focus-notebook fallback merged from
`WikiLinkTargetReference.forToken` during the original plan (qualified
tokens use their own qualifier; unqualified tokens fall back to a supplied
focus notebook; unqualified tokens with no focus notebook resolve to
nothing) — this behavior is currently exercised only indirectly through
full-Spring-context integration tests.
**Trigger:** A "small test"-style unit test calls `resolve` directly with a
qualified token, an unqualified token with a focus notebook, and an
unqualified token with no focus notebook.
**Postcondition:** Each case returns the same effective notebook-name result
the old `WikiLinkTargetReference.forToken` produced, now pinned by a fast,
isolated unit test independent of Spring context startup.

Test work:

- Add three cases to `PortablePathTest.java` (or a focused nested class if
  that fits the file's existing structure) covering: qualified token
  (qualifier used as-is, focus notebook ignored), unqualified token with a
  focus notebook present (fallback applied), unqualified token with no focus
  notebook (empty/no-resolution result).
- Do not change `PortablePath.resolve`'s implementation — this slice adds
  coverage for existing, already-verified-correct behavior only.

Verification:

- Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

Stop-safe outcome: `PortablePath.resolve`'s three documented behaviors are
each pinned by a direct, fast unit test.

## Slice wrap-up contract

For every executed slice: keep existing behavior green, run
`post-change-refactor`, update this plan with concise learnings, run the
listed verification, then commit and push before the next slice. After the
final slice, clean up spent planning history according to `planning.mdc`.
