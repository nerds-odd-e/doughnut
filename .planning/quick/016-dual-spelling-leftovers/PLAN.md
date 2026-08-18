# Plan: Dual-spelling leftover bugs and test pins

**Status:** planned

**Goal:** After 014 (dead path-Markdown retarget, co-move matching, first pass of overlapping tests), fix the leftover user-visible retarget bug and finish pinning dual-spelling at one stable boundary. Do not reopen conversion, P9, or frontmatter (015).

Inspection of commits `97c91ba7e3`, `d3f1271066`, `94ffdf077d`. Do not execute until asked.

## Locked for this plan

- No conversion of stored `[[…]]` ↔ `[…](…)`.
- Do not unify Java `isConceptPathHref` with TS `hrefLooksLikeConceptNotePath` (cross-subsystem; 015 also locked this out).
- Frontend path-Markdown detection for a `WikiTitle` uses the existing concept-path helper, not a second `startsWith("/")` rule.
- Redundant tests: keep one stable boundary (controller, mounted `SearchForm` / `NoteTextContent`, or E2E). Delete algorithm/HTML tests that only re-assert that outcome.
- ADR 0004 stays Proposed. Do not accept it here.

## Out of this plan

- Create-note from a dead path-Markdown link.
- Inbound path Markdown following a **target** note into another notebook (known gap from 014).
- Extra `loadNoteRealm` on path retarget — search hits have no folder trail; nested paths need `ancestorFolders`.
- `NoteTextContent.wikiLinks` live/dead path display vs E2E open+persist (E2E unique: follow the link and round-trip markdown).
- Editor Turndown rows and “dead path Markdown upgrades to live” in `replaceWikiLinksInHtml` / `quillHtmlToMarkdown` (014 kept these).
- Pre-existing wiki retarget overlap (`wiki_link.feature` vs SearchForm wiki case).
- 015 frontmatter dual-spelling.

## Discoveries

- `SearchForm.onDeadWikiLinkToNote` still uses `String.replace`, which updates only the first matching token. Two identical `[label](/Missing.md)` (or two `[[Ghost]]`) leave the second dead. Backend inbound rewrite replaces every matching span.
- `isPathMarkdownWikiTitle` is `targetToken.startsWith("/")`. Retarget uses `hrefLooksLikeConceptNotePath` (rejects `/n42` and `//…`). Display/upgrade can classify a note-show or protocol-relative token as path Markdown while retarget does not.
- Path-Markdown retarget suffix matrix (`.md` / no `.md`) is pinned twice: `SearchDialog.deadWikiLink.spec.ts` `it.each` and `path_markdown_link.feature` outline.
- 014 slice 3 deleted path-prefix / folder-rename duplicates in `WikiLinkMarkdownTest` but left wiki title and qualify `newInner*` cases that still only repeat `TextContentControllerUpdateNoteTitleInboundWikiReferencesTests` and `RelationControllerTests` outgoing qualify.

## Slices

### 1. Pointing a dead link at a note rewrites every matching token

- **Type:** Behavior
- **Status:** planned

**Pre:** a note body has two identical unresolved `[label](/Folder/Missing.md)` (dead wiki-link UI). **Trigger:** point-at-existing chooses a note titled `Title` in `ChosenFolder`. **Post:** stored body contains two `[label](/ChosenFolder/Title.md)` spans; no leftover `[label](/Folder/Missing.md)`; no `[[…]]` introduced.

Extend the mounted `SearchForm` dead-wiki helper (same replace path as wiki). Canonical shape stays the existing single-token path-Markdown case; this slice asserts only that both spans update. Do not add a second E2E outline.

### 2. Path-Markdown WikiTitle detection uses the concept-path helper

- **Type:** Structure
- **Status:** planned

No user-facing change. `isPathMarkdownWikiTitle` delegates to `hrefLooksLikeConceptNotePath` so display/upgrade and retarget share one frontend rule. Existing mounted, HTML-upgrade, and E2E tests still pass.

### 3. Path-Markdown retarget suffix is pinned once

- **Type:** Structure
- **Status:** planned

No user-facing change. Keep the mounted `SearchForm` `it.each` (`.md` / no `.md`) as the suffix matrix. Collapse `path_markdown_link.feature` retarget outline to **one** example (keep `.md`). Existing controller, mounted, and remaining E2E still pass.

### 4. WikiLinkMarkdown rewrite tests pin only unique algorithm edges

- **Type:** Structure
- **Status:** planned

No user-facing change. Delete `WikiLinkMarkdownTest` cases that only repeat controller title-rename / outgoing-qualify outcomes:

- `newInnerForUpdateVisibleText_plainLink`
- `newInnerForUpdateVisibleText_keepsDisplaySegment`
- `newInnerForUpdateVisibleText_keepsNotebookQualifier`
- `newInnerForKeepVisibleText_plainLinkAddsDisplay`
- `newInnerForKeepVisibleText_preservesCustomDisplay`
- `newInnerForQualifyUnqualifiedOutgoingLink_plainLinkAddsSourceNotebookAndDisplay`
- `newInnerForQualifyUnqualifiedOutgoingLink_keepsAlreadyQualifiedPlainLink`

Keep unique edges: extract/parse; nested same-name folder vs title; unqualified title unchanged on folder rename; KEEP_VISIBLE path-Markdown label; empty-pipe / blank inner; qualify custom display and already-qualified-with-display; whitespace-in-`[[ ]]` replace.

Existing controller tests still pass.
