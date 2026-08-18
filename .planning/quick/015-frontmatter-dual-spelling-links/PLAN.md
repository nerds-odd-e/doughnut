# Plan: Frontmatter dual-spelling wiki links

**Status:** in progress

**Goal:** YAML frontmatter inter-note links use the same dual-spelling as the body (wiki default, path Markdown accepted, no conversion). One token API so the next wiki-link change does not add another regex. Do not convert stored wiki to paths. Do not treat a bare YAML path as a link.

Inspection after locking Proposed [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown.md) (frontmatter = body). Do not execute until asked.

## Locked for this plan

- Wiki is Doughnut-authored (`formatRelationshipNoteMarkdown`, overlap insert, product insert).
- Path Markdown `[display](/folder/File.md)` in a YAML scalar or list item is the same link. No `[[…]]` ↔ `[…](…)` conversion. No backfill.
- A bare YAML path (`source: /folder/File.md`) is not a link.
- Frontend markdown/YAML consumers go through `authoredLinkMarkup` (TS mirror of `WikiLinkMarkdown`; extracted from `wikiLinkMarkup`). Do not add a third occurrence regex. `wikiLinkMarkup` stays click/DOM helpers.
- Backend overlaps whole-item check uses `WikiLinkMarkdown` (extend `isWellFormedWholeLinkToken`; do not add a second parser).
- `replaceWikiLinksInHtml` stays the HTML body path. Do not merge HTML and markdown token walks here. Shared live/dead path-anchor HTML is `wikiLinkAnchorHtml` in `wikiLinkMarkup`.
- ADR 0004 stays Proposed. Do not accept it here.

## Out of this plan

- 014 body bugs (dead path-Markdown retarget, cross-notebook co-move, test overlap).
- Unifying `isConceptPathHref` (Java) with `hrefLooksLikeConceptNotePath` (TS).
- P9, collision basenames, accepting ADR 0004.
- Create-note / point-at-existing search still wiki-token-only (optional polish, not this plan).

## Discoveries

- Backend resolve, cache, rename rewrite, and reduce-on-delete already scan frontmatter via `NoteContentMarkdown.authoredTokensInOccurrenceOrder` (wiki + path Markdown). Stored wiki `source: "[[Sedition]]"` is already the default form.
- Wiki-only leftovers remaining:
  - `hasNewWikiLinkTexts` only sees `[[…]]`, so a new path-Markdown link does not flush the editor.

## Slices

### 1. Authored-link occurrences live in wikiLinkMarkup

- **Type:** Structure
- **Status:** done

`authoredLinkOccurrences` / `splitAuthoredToken` live in `frontend/src/utils/authoredLinkMarkup.ts` (file-size split from `wikiLinkMarkup`). Wiki `token` is the inner; path `token` is the full `[display](/href)`; `start`/`end` span the original substring. `propertyValuePlainToDisplayHtml` walks those occurrences; wiki still live/dead HTML. Path scalars are live/dead as of slice 2.

**Learning:** later frontend slices import from `authoredLinkMarkup`, not a new regex. Product commit landed early as `a66ef8721a` (mixed with unrelated `.planning/quick/016-dual-spelling-leftovers/`); wrap-up is the extract + this plan update.

### 2. Path Markdown in a frontmatter scalar is a live or dead link

- **Type:** Behavior
- **Status:** done

YAML `source: "[Moon](/Moon.md)"` renders as a live or dead path wiki-link (path href, plain label, `data-note-id` when resolved; no `wiki-bracket` spans; no conversion to `[[Moon]]`). Driven through `propertyValuePlainToDisplayHtml` and mounted `RichMarkdownEditor` source row. Live/dead path-anchor HTML is `wikiLinkAnchorHtml`. Slice-2 leftover E2E (`add_relationship.feature` path-Markdown source) passed; `@wip` dropped.

### 3. Reducing a relationship whose source is path Markdown

- **Type:** Behavior
- **Status:** done

Deleting a relationship whose YAML `source` is a resolvable path-Markdown token offers “Reduce to a property of the source” and writes the target onto the source note. `qualifyRelationNoteForReduceOnDelete` uses the first `authoredLinkOccurrences` token. Wrap-up moved `wikiTitleFromAuthoredToken` / `noteIdForAuthoredToken` into `authoredLinkMarkup`.

**Learning:** later slices resolve authored tokens through `noteIdForAuthoredToken`, not a private lookup. E2E: `relationship_edit_and_remove.feature`.

### 4. An overlaps list item written as path Markdown is the same wiki link

- **Type:** Behavior
- **Status:** done

A whole overlaps list item `[Title](/Folder/Title.md)` is a live or dead wiki-equivalent link and counts as an authored overlap (no conversion). `parseWholeWikiLinkItem` / `isWellFormedWholeWikiLinkItem` live in `authoredLinkMarkup`; backend `WikiLinkMarkdown.isWellFormedWholeLinkToken` accepts the same whole path token. `WikiLinkToken` resolves via `noteIdForAuthoredToken`. Deleted leftover `wholeWikiLinkItem.ts`.

**Learning:** whole-item parse is part of the frontend token API (`authoredLinkMarkup`), not a second module. Slice 5 still needs `hasNewWikiLinkTexts` to walk `authoredLinkOccurrences`.

### 5. A newly typed path-Markdown link flushes like a wiki link

- **Type:** Behavior
- **Status:** planned

**Pre:** the note content editor is open. **Trigger:** the next content introduces `[label](/Folder/Title.md)` (not only `[[Title]]`). **Post:** the editor flushes immediately, same as a new wiki token.

`hasNewWikiLinkTexts` uses `authoredLinkOccurrences` from `authoredLinkMarkup`. Extend `noteContentWikiLinks.spec.ts`. Existing wiki flush cases still pass.

## Coordination

Body path-Markdown retarget and co-move matching are on main. This plan owns markdown/YAML token consumers.
