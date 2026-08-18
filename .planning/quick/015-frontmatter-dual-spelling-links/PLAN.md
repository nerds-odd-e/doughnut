# Plan: Frontmatter dual-spelling wiki links

**Status:** planned

**Goal:** YAML frontmatter inter-note links use the same dual-spelling as the body (wiki default, path Markdown accepted, no conversion). One token API so the next wiki-link change does not add another regex. Do not convert stored wiki to paths. Do not treat a bare YAML path as a link.

Inspection after locking Proposed [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown.md) (frontmatter = body). Do not execute until asked.

## Locked for this plan

- Wiki is Doughnut-authored (`formatRelationshipNoteMarkdown`, overlap insert, product insert).
- Path Markdown `[display](/folder/File.md)` in a YAML scalar or list item is the same link. No `[[…]]` ↔ `[…](…)` conversion. No backfill.
- A bare YAML path (`source: /folder/File.md`) is not a link.
- Frontend markdown/YAML consumers go through `wikiLinkMarkup` (TS mirror of `WikiLinkMarkdown`). Do not add a third occurrence regex.
- Backend overlaps whole-item check uses `WikiLinkMarkdown` (extend `isWellFormedWholeLinkToken`; do not add a second parser).
- `replaceWikiLinksInHtml` stays the HTML body path ([014](../014-dual-spelling-link-followup/PLAN.md)). Do not merge HTML and markdown token walks here.
- ADR 0004 stays Proposed. Do not accept it here.

## Out of this plan

- 014 body bugs (dead path-Markdown retarget, cross-notebook co-move, test overlap).
- Unifying `isConceptPathHref` (Java) with `hrefLooksLikeConceptNotePath` (TS).
- P9, collision basenames, accepting ADR 0004.
- Create-note / point-at-existing search still wiki-token-only (optional polish, not this plan).

## Discoveries

- Backend resolve, cache, rename rewrite, and reduce-on-delete already scan frontmatter via `NoteContentMarkdown.authoredTokensInOccurrenceOrder` (wiki + path Markdown). Stored wiki `source: "[[Sedition]]"` is already the default form.
- Wiki-only leftovers are product parsers, not missing data:
  - `propertyValuePlainToDisplayHtml` only linkifies `[[…]]` (relationship `source` / `target` scalars).
  - `qualifyRelationNoteForReduceOnDelete` uses a private wiki regex; backend would already resolve a path-Markdown source.
  - `parseWholeWikiLinkItem` / `WikiLinkToken` / overlaps validation / `WikiLinkMarkdown.isWellFormedWholeLinkToken` require a whole `[[…]]` item.
  - `hasNewWikiLinkTexts` only sees `[[…]]`, so a new path-Markdown link does not flush the editor.

## Slices

### 1. Authored-link occurrences live in wikiLinkMarkup

- **Type:** Structure
- **Status:** planned

Add `authoredLinkOccurrences` (wiki + path Markdown, document order) on `wikiLinkMarkup`, mirroring `WikiLinkMarkdown.authoredTokensInOccurrenceOrder` / `splitAuthoredToken`. Switch `propertyValuePlainToDisplayHtml` wiki matching to those wiki occurrences. Path occurrences exist but are still rendered as escaped text this slice. Existing `propertyValueField.spec.ts` wiki cases pass. No user-facing change.

### 2. Path Markdown in a frontmatter scalar is a live or dead link

- **Type:** Behavior
- **Status:** planned

**Pre:** a relationship note’s YAML has `source: "[Moon](/Moon.md)"` (and a wiki `target`), stored spelling unchanged. **Trigger:** open the note (rich property row). **Post:** the source value shows the same live wiki-link UI as a body path-Markdown link to Moon (dead UI if unresolved); markdown source still contains `[Moon](/Moon.md)`, not `[[Moon]]`.

Drive `propertyValuePlainToDisplayHtml` / mounted `PropertyValueField` with path-Markdown scalars; E2E on `e2e_test/features/relationships/add_relationship.feature` (edit YAML or seed content; do not change Doughnut-authored create, which stays wiki). `serializePropertyValueFieldRoot` already round-trips path Markdown via `wikiAnchorToMarkdownToken`.

### 3. Reducing a relationship whose source is path Markdown

- **Type:** Behavior
- **Status:** planned

**Pre:** relationship YAML `source` is a resolvable path-Markdown token (same as slice 2). **Trigger:** delete the relationship note. **Post:** “Reduce to a property of the source” is offered; confirming writes the target onto the source note as today.

`qualifyRelationNoteForReduceOnDelete` uses the first authored token from slice 1 (delete the private `[[…]]` regex). Extend `relationNoteReduceOnDelete.spec.ts`; reuse existing delete E2E reduce flow with path-Markdown source content.

### 4. An overlaps list item written as path Markdown is the same wiki link

- **Type:** Behavior
- **Status:** planned

**Pre:** `overlaps` contains `[Title](/Folder/Title.md)` as a whole list item. **Trigger:** view the property / accidental-match overlap check. **Post:** the item is a live or dead wiki-equivalent link; it counts as an authored overlap (same as `[[Folder/Title]]`).

`parseWholeWikiLinkItem` / `isWellFormedWholeLinkToken` accept a whole path-Markdown token. `WikiLinkToken` and overlaps validation/index use that. No conversion of stored items.

### 5. A newly typed path-Markdown link flushes like a wiki link

- **Type:** Behavior
- **Status:** planned

**Pre:** the note content editor is open. **Trigger:** the next content introduces `[label](/Folder/Title.md)` (not only `[[Title]]`). **Post:** the editor flushes immediately, same as a new wiki token.

`hasNewWikiLinkTexts` uses authored occurrences from slice 1. Extend `noteContentWikiLinks.spec.ts`. Existing wiki flush cases still pass.

## Coordination

If [014](../014-dual-spelling-link-followup/PLAN.md) is in progress, do not edit `wikiLinkMarkup.ts` in parallel. 014 owns HTML body retarget; this plan owns markdown/YAML token consumers.
