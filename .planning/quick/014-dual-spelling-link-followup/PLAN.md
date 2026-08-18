# Plan: Dual-spelling follow-up (bugs, missed cohesion, redundant tests)

**Status:** in-progress

**Goal:** Path Markdown and folder-path wiki links that already resolve/display still mis-fire on dead-link retarget and cross-notebook co-move, and the dual-spelling work left overlapping tests that pin rewrite internals. Fix those; do not reopen conversion or P9.

Inspection of the 012 dual-spelling implementation. Do not execute until asked.

## Locked for this plan

- No conversion of stored `[[…]]` ↔ `[…](…)`.
- One `WikiLinkResolver`, one inbound rewrite walk, one `PathShapedTarget`.
- Point-at-existing on a dead path-Markdown span writes Markdown whose href is the chosen note’s folder path + title; keep the authored `.md` / no-`.md` suffix.
- Co-moved path-shaped matching uses the same folder-trail rule as resolve (`PathShapedTarget` + `FolderTrailSegments`), not `note.getTitle()` vs the raw target string.
- Notebook-qualify transforms skip path-Markdown tokens (leave them authored). They must not invent `Notebook:[label](/…)`.
- Redundant tests: keep the stable-boundary test (controller, mounted `NoteTextContent`, or E2E). Delete algorithm/HTML tests that only re-assert the same rewrite/display.

ADR 0004 stays Proposed. Do not accept it here.

## Out of this plan

- Create-note from a dead path-Markdown link (which folder/title is a product fork; today’s flow uses display text and wiki insert).
- Inbound path Markdown following a **target** note into another notebook without conversion (wiki can add `Notebook:`; path hrefs cannot). Leave as a known gap.
- Unifying `isConceptPathHref` (Java) with `hrefLooksLikeConceptNotePath` (TS) — cross-subsystem, needs an explicit authorize.
- P9, collision basenames, accepting ADR 0004. Frontmatter dual-spelling (wiki default) is [015](../015-frontmatter-dual-spelling-links/PLAN.md).

## Discoveries

- Dead path-Markdown retarget (slice 1): fixed — original token is path Markdown when the target is a path.
- `WikiLinkRewriteSupport.noteMatchesWikiLinkTarget` compares `note.getTitle()` to `ref.noteTitle()`. For `[[Folder/A]]` / `[x](/Folder/A.md)` that string is the path, not the title. Co-moved notes in a folder moved across notebooks get incorrectly qualified to the old notebook (path Markdown currently no-ops only because the corrupt `newInner` fails path-parse and wiki replace misses).
- `WikiLinkResolver.noteCandidates` still uses `contains("/")` instead of `PathShapedTarget.tryParse` (same dual heuristic the sanitizer already dropped).
- Overlap: `WikiLinkMarkdownTest` rewrite cases vs `TextContentControllerUpdateNoteTitleInboundWikiReferencesTests` / `NotebookFolderRenameWikiLinkRewriteControllerTest`. Frontend: `replaceWikiLinksInHtml` live/dead path Markdown vs `NoteTextContent.wikiLinks` vs `path_markdown_link.feature`.

## Slices

### 1. Pointing a dead path-Markdown link at a note keeps Markdown

- **Type:** Behavior
- **Status:** done

**Pre:** a note body has unresolved `[label](/Folder/Missing.md)` (dead wiki-link UI). **Trigger:** point-at-existing chooses a note titled `Title` in `ChosenFolder`. **Post:** stored body contains `[label](/ChosenFolder/Title.md)` (`.md` kept if the dead href had it; omitted if it did not); no `[[…]]` introduced; the live link opens `Title`.

Shipped: `markdownWikiTokenFromDeadWikiLinkPayload` now reconstructs the authored `[label](href)` token for path targets; replace writes `pathMarkdownTokenForNote` (folder trail from the chosen note’s `ancestorFolders` + title, `.md` suffix from the dead href). Wiki dead-link retarget unchanged. E2E outline in `path_markdown_link.feature` (`.md` and no-`.md`).

Learning: SearchForm loads the chosen note realm only for path retarget (folder names). Create-note from dead path Markdown remains out of plan.

### 2. Moving a folder across notebooks keeps path-shaped links to co-moved notes

- **Type:** Behavior
- **Status:** planned

**Pre:** folder `F` contains notes `A` and `B`; `B`’s body has both `[[F/A]]` and `[label](/F/A.md)`. **Trigger:** move folder `F` to another notebook. **Post:** both spans still open `A`; wiki stays wiki with prefix `F/`; Markdown stays Markdown with href `/F/A.md`; neither is qualified as `OldNb:…` or `OldNb:[label](/…)`.

Controller boundary: existing cross-notebook folder/note move tests (`RelationControllerTests` / folder-move controller). Co-moved match must use `PathShapedTarget` + folder trail (same as resolve). Path-Markdown tokens are skipped by qualify-outgoing / keep-notebook-move. Drop the `contains("/")` gate in `noteCandidates` in this slice if it is on the same matching path.

### 3. Dual-spelling tests pin the stable boundary once

- **Type:** Structure
- **Status:** planned

No user-facing change. Delete rewrite tests in `WikiLinkMarkdownTest` that only repeat controller title-rename / folder-rename outcomes (`keepsFolderPathPrefix`, path-Markdown last-segment rewrite, folder-prefix rewrite). Keep unique algorithm edges (nested same-name folder segment vs title; unqualified title unchanged; extract/parse; whitespace-in-`[[ ]]` replace).

Delete `replaceWikiLinksInHtml` live/unresolved path-Markdown cases that duplicate `NoteTextContent.wikiLinks.spec.ts` + `path_markdown_link.feature`. Keep editor-only “dead path Markdown upgrades to live” and Turndown table rows in `quillHtmlToMarkdown.spec.ts`.

Existing controller, mounted, and E2E tests still pass.
