# Plan: Dual-spelling leftover bugs and test pins

**Status:** in progress

**Goal:** After 014 (dead path-Markdown retarget, co-move matching, first pass of overlapping tests), fix the leftover user-visible retarget bug and finish pinning dual-spelling at one stable boundary. Do not reopen conversion, P9, or frontmatter dual-spelling.

Inspection of commits `97c91ba7e3`, `d3f1271066`, `94ffdf077d`.

## Locked for this plan

- No conversion of stored `[[…]]` ↔ `[…](…)`.
- Do not unify Java `isConceptPathHref` with TS `hrefLooksLikeConceptNotePath` (cross-subsystem).
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
- Frontmatter dual-spelling (wiki default, path Markdown accepted, no conversion).

## Discoveries

- `SearchForm.onDeadWikiLinkToNote` used `String.replace` (first match only). Slice 1 switched to `replaceAll`; mounted SearchForm pins two identical path-Markdown tokens. Wiki tokens share that path.
- `isPathMarkdownWikiTitle` now delegates to `hrefLooksLikeConceptNotePath` (same frontend rule as retarget; rejects `/n42` and `//…`). Java `isConceptPathHref` stays separate.
- Path-Markdown retarget suffix matrix (`.md` / no `.md`) is pinned twice: `SearchDialog.deadWikiLink.spec.ts` `it.each` and `path_markdown_link.feature` outline.
- 014 slice 3 deleted path-prefix / folder-rename duplicates in `WikiLinkMarkdownTest` but left wiki title and qualify `newInner*` cases that still only repeat `TextContentControllerUpdateNoteTitleInboundWikiReferencesTests` and `RelationControllerTests` outgoing qualify.

## Slices

### 1. Pointing a dead link at a note rewrites every matching token

- **Type:** Behavior
- **Status:** done

**Pre:** two identical unresolved `[label](/Folder/Missing.md)`. **Trigger:** point-at-existing → `Title` in `ChosenFolder`. **Post:** two `[label](/ChosenFolder/Title.md)` spans; no leftover dead token; no `[[…]]`.

Pinned on mounted SearchForm (`SearchDialog.deadWikiLink.spec.ts`); `onDeadWikiLinkToNote` uses `replaceAll`. Single-token canonical shape remains the existing `it.each`.

### 2. Path-Markdown WikiTitle detection uses the concept-path helper

- **Type:** Structure
- **Status:** done

`isPathMarkdownWikiTitle` delegates to `hrefLooksLikeConceptNotePath`. Display/upgrade and retarget share one frontend rule.

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
