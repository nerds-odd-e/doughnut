# Plan: Dual-spelling inter-note links (wiki + path Markdown)

**Status:** in progress

**Goal:** Doughnut keeps writing `[[…]]`. Path Markdown `[display](/folder/File.md)` (`.md` optional) is the same link as `[[folder/File|display]]`. No conversion either way. One resolver, one cache, one rewrite, one rich-display path.

Humans already chose the locks below. This plan only sequences them. Do not accept ADR 0004 in this plan.

**Commit grain:** one git commit per slice. Do not bundle slices. Each unit should be ~5 minutes including its targeted tests (`planning.mdc`).

## Locked for this plan

- Doughnut-authored inter-note links stay wiki `[[target]]` / `[[target|display]]`. Product insert stays wiki.
- `[display](/folder/File.md)` ≡ `[[folder/File|display]]`. Leading `/` on Markdown hrefs is bundle-relative (notebook root). Wiki path form has no leading `/`.
- `.md` on a **path-shaped** target is optional and ignored (`/folder/File` = `/folder/File.md`; `[[folder/File.md]]` = `[[folder/File]]`). Do **not** strip `.md` from unqualified wiki titles (`[[File.md]]` can still mean a title).
- Identity is **folder path + title** (`uk_note_notebook_folder_title`), not ZIP collision basenames (`Recipe (2).md`).
- **No active conversion** of stored `[[…]]` ↔ `[…](…)`, including save/paste round-trip of path Markdown.
- Unqualified `[[Title]]` is unchanged (still lowest note id when titles collide across folders). `Notebook:Title` is unchanged.
- Cache table stays `(note, target_note, link_text)`. No style column. No second cache.

ADR and glossary are updated **incrementally**: a Structure slice locks only what the **immediate next** Behavior will implement, except slice 1 which records the dual-spelling rule so later slices cannot “fix” P4 by converting.

## Cohesion (hard)

Do not grow a parallel link stack. Execute-plan must keep **one** of each:

| Piece | Role |
|-------|------|
| Authored token | Exact stored spelling (`[[inner]]` inner, or `[display](href)`). Cache `link_text` is this token (wiki: inner as today; Markdown: the Markdown link, so `splitInner` is not assumed). |
| Target token | Path or title used to resolve (`folder/File`, `Notebook:Title`, `Title`). Path-shaped: strip leading `/` and optional trailing `.md`. |
| `WikiLinkResolver` | Only resolver. Path-shaped targets walk folder names then title in that folder; otherwise existing title/alias/`Notebook:Title`. |
| `note_wiki_title_cache` | Resolved rows only. Format-agnostic: it stores authored `link_text` + `target_note_id`. |
| Rich display | One `replaceWikiLinksInHtml` (or equivalent) applies `wikiTitles` to both leftover `[[…]]` and path hrefs so they look/navigate as wiki links. |
| Turndown | Serializes by **href shape**: note-show `/n…` → wiki; concept path href → same Markdown spelling. Do not fold path hrefs into `[[…]]`. |
| Rewrite (rename/move) | Parse occurrence → change target path/title → write back **the same spelling**. |

Frontend `WikiTitle` stays `{ linkText, targetToken, displayText, noteId }`. `linkText` is the authored token used to find the span; `targetToken`/`displayText` come from the shared parse, not a second split on the client.

## Cache confirmation

The cache **row** does not care about wiki vs Markdown: it is referrer + target note + `link_text`.

Today the **pipeline** does care: extraction is `[[…]]` only; `wikiTitlesForViewer` always `WikiLinkMarkdown.splitInner(linkText)`; the editor replaces `[[${linkText}]]`. Path Markdown is not cached and renders as a generic `<a href>`.

This plan keeps the table as-is and makes extract/parse/display/rewrite share one target-token model so the cache stays style-blind.

## Out of this plan

P9 accept/lint, ZIP rewrite of wiki to Markdown, Flyway conversion, collision-filename mapping, relative `./`, `README.md` as a note target, `Notebook:folder/File`, changing insert-as-wiki to path Markdown, `/n…` paste rules except where they would convert path Markdown.

## Slices

### 1. Lock dual-spelling links in ADR 0004

- **Type:** Structure
- **Status:** done

Locked in Proposed [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown.md) Decision and [ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md) **Wiki link** row. P4 points here and is not closed. ADR 0004 remains Proposed.

### 2. Folder-path wiki link opens the note in that folder

- **Type:** Behavior
- **Status:** done

`[[Folder/Title]]` resolves via one `WikiLinkResolver` + `PathShapedTarget`. Unqualified `[[Title]]` still lowest id. Sanitizer keeps `/` in path-shaped wiki targets. E2E in `wiki_link.feature`.

### 3. Renaming a note keeps folder-path wiki prefix

- **Type:** Behavior
- **Status:** done

Rename rewrites via `WikiLinkTargetReference.replaceNoteTitle` / `PathShapedTarget.withNoteTitle`. `[[Folder/Old]]` stays wiki with prefix `Folder/`; does not collapse to `[[New]]`. E2E in `wiki_link.feature`.

### 4. Nested folder-path wiki link opens the nested note

- **Type:** Behavior
- **Status:** done

Walker already handled nested `PathShapedTarget.folderNames`. Locked with unit test + E2E in `wiki_link.feature`. No production change.

### 5. Path Markdown link opens like a wiki link; `.md` optional; spelling kept

- **Type:** Behavior
- **Status:** done

`[label](/Folder/Title.md)` (`.md` optional; root `/Title.md` included) extracts with wiki via `authoredTokensInOccurrenceOrder`, caches the authored Markdown token, rich-displays as a live wiki-style link, Turndown keeps Markdown. `PathShapedTarget` allows empty folder when href has leading `/`.

### 6. Renaming a note rewrites path-Markdown hrefs and keeps Markdown

- **Type:** Behavior
- **Status:** done

Inbound `[label](/Folder/Old.md)` last segment becomes `New`; stays Markdown; preserves `/` and optional `.md`. Same walk as wiki; `PathMarkdownToken` parse is shared; `WikiLinkMarkdownRewrite` writes the Markdown token.

### 7. Unresolved path Markdown shows as a dead wiki link

- **Type:** Behavior
- **Status:** done

Unresolved `[label](/Folder/Missing.md)` uses the same dead wiki-link UI; stored Markdown unchanged. Create / point-at-existing still search wiki tokens only (no Markdown→wiki conversion). Path Markdown E2E lives in `path_markdown_link.feature`.

### 8. Folder rename updates path prefixes in both spellings

- **Type:** Behavior
- **Status:** planned

**Pre:** a note contains both `[[OldFolder/Title]]` and `[label](/OldFolder/Title.md)` to the same target. **Trigger:** rename folder `OldFolder` → `NewFolder`. **Post:** both spans still open the note; wiki stays wiki; Markdown stays Markdown; path prefix is `NewFolder`. One rewrite over parsed path segments (`PathShapedTarget` + `pathMarkdownOccurrences`). E2E: `wiki_link.feature` and/or `path_markdown_link.feature`.

### 9. Close P4 dual-spelling in the tracker

- **Type:** Structure
- **Status:** planned

[OKF-COMPATIBILITY-GAP.md](../../research/OKF-COMPATIBILITY-GAP.md) / [SEED-003](../../seeds/SEED-003-close-okf-v0-2-compatibility-gaps.md): P4 closed as dual-spelling + no conversion; remaining P9 / YAML path-valued `source`/`target` / collision basenames called out. Spent “export should rewrite wiki” wording gone. No product code.

## Discoveries

- Slice 1: glossary **Wiki link** row names both spellings and points at ADR 0004 rather than restating `.md` / leading-`/` mechanics. Gap tracker/seed no longer treat ZIP wiki rewrite as remaining P4 work; product dual-spelling still open until slice 9.
- Slice 2: `PathShapedTarget` already holds `folderNames` (list) + title. Sanitizer asks that parser. `/` in wiki **titles** stays forbidden (fullwidth on persist). Unqualified `[[Title]]` still lowest id.
- Slice 3: title rewrite lives on `WikiLinkTargetReference.replaceNoteTitle` (path-shaped via `PathShapedTarget.withNoteTitle`).
- Slice 4: nested `[[Parent/Child/Title]]` already resolved; this slice only locked it with tests.
- Slice 5: extract is `authoredTokensInOccurrenceOrder`. Cache `link_text` is the full Markdown token. Display uses `WikiTitle.targetToken` (leading `/` ⇒ path href). Turndown: `/n…` → wiki; concept path href → Markdown.
- Slice 6: `WikiLinkMarkdown.tryParsePathMarkdownToken` / `pathMarkdownOccurrences` is the only Markdown scan. `PathShapedTarget.withNoteTitle` keeps `/` and `.md`. Slice 8 should rewrite folder segments on that same parse, not a second scanner.
- Slice 7: leftover wiki tokens and leftover path hrefs share one dead-link pass. Create / point-at-existing still wiki-token-only (out of this plan). Path Markdown scenarios: `e2e_test/features/note_topology/path_markdown_link.feature`.
- ZIP collision `Recipe (2).md` is not `[[Recipe (2)]]`. Out of this plan.
