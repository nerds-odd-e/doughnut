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
- **Status:** planned

**Pre:** two notes share a title in different folders of the same notebook. **Trigger:** save body `[[Folder/Title]]` (display optional). **Post:** rendered wiki link opens the note in `Folder`, not the other namesake. Unqualified `[[Title]]` still lowest id.

E2E: extend `e2e_test/features/note_topology/wiki_link.feature`. Resolver grows path-shaped targets; do not add a second resolver. Do not fullwidth-slash path-shaped wiki targets (AI sanitizer included).

### 3. Renaming a note keeps folder-path wiki prefix

- **Type:** Behavior
- **Status:** planned

**Pre:** a note contains `[[Folder/Old]]` (or piped display) pointing at `Old` in `Folder`. **Trigger:** rename `Old` → `New` (keep or update visible text, existing product choice). **Post:** stored spelling stays wiki with prefix `Folder/`; link still opens the renamed note. Must not collapse to `[[New]]`.

### 4. Nested folder-path wiki link opens the nested note

- **Type:** Behavior
- **Status:** planned

**Pre:** note titled `Title` lives in `Parent/Child`. **Trigger:** save `[[Parent/Child/Title]]`. **Post:** wiki link opens that note. Same path walker as slice 2 (generalize the segments; do not fork).

### 5. Path Markdown link opens like a wiki link; `.md` optional; spelling kept

- **Type:** Behavior
- **Status:** planned

**Pre:** target note in `Folder` titled `Title`. **Trigger:** save `[label](/Folder/Title.md)` or `[label](/Folder/Title)` (one scenario outline). **Post:** rich view is a live wiki-style link to that note; markdown source still contains the authored Markdown (with or without `.md` as written). Cache row uses that authored token; resolver sees the same target token as `[[Folder/Title|label]]`.

Extract Markdown path links in the **same** occurrence list as wiki inners (one extract → one `resolveWikiLinksForCache`). Turndown must not convert these hrefs to `[[…]]`.

Root `[label](/Title.md)` (note at notebook root) is the same behavior with an empty folder path — include as an Examples row if cheap; otherwise the next slice. Prefer Examples in this slice.

### 6. Renaming a note rewrites path-Markdown hrefs and keeps Markdown

- **Type:** Behavior
- **Status:** planned

**Pre:** body has `[label](/Folder/Old.md)` (and a sibling without `.md` if still authored). **Trigger:** rename `Old` → `New`. **Post:** href path last segment is `New`; link remains Markdown (not rewritten to `[[…]]`); still opens the note. Reuse slice 3 rewrite; only the write-back spelling differs.

### 7. Unresolved path Markdown shows as a dead wiki link

- **Type:** Behavior
- **Status:** planned

**Pre:** no note at that folder/title. **Trigger:** save `[label](/Folder/Missing.md)`. **Post:** dead wiki-link treatment (same as unresolved `[[…]]`); markdown source unchanged. Point-at-existing / create-note may follow existing dead-wiki flows **without** converting the stored span to wiki unless that flow already replaces the authored token in place.

### 8. Folder rename updates path prefixes in both spellings

- **Type:** Behavior
- **Status:** planned

**Pre:** a note contains both `[[OldFolder/Title]]` and `[label](/OldFolder/Title.md)` to the same target. **Trigger:** rename folder `OldFolder` → `NewFolder`. **Post:** both spans still open the note; wiki stays wiki; Markdown stays Markdown; path prefix is `NewFolder`. One rewrite over parsed path segments.

### 9. Close P4 dual-spelling in the tracker

- **Type:** Structure
- **Status:** planned

[OKF-COMPATIBILITY-GAP.md](../../research/OKF-COMPATIBILITY-GAP.md) / [SEED-003](../../seeds/SEED-003-close-okf-v0-2-compatibility-gaps.md): P4 closed as dual-spelling + no conversion; remaining P9 / YAML path-valued `source`/`target` / collision basenames called out. Spent “export should rewrite wiki” wording gone. No product code.

## Discoveries

- Slice 1: glossary **Wiki link** row names both spellings and points at ADR 0004 rather than restating `.md` / leading-`/` mechanics. Gap tracker/seed no longer treat ZIP wiki rewrite as remaining P4 work; product dual-spelling still open until slice 9.
- Wiki qualification today is only `Notebook:Title`. `/` in wiki **titles** is forbidden (fullwidth on persist). `/` in wiki **targets** is not a folder path; `[[folder/File]]` does not resolve. Same title in two folders: lowest note id (`WikiTitleCacheTitleResolutionTest`).
- Cache is style-blind at rest; extract/display/rewrite are wiki-inner-specific. Path Markdown is not extracted (`NoteContentMarkdown.wikiLinkInnersInOccurrenceOrder`).
- `quillHtmlToMarkdown` turns doughnut-wiki-link and `/n…` hrefs into `[[label]]`. Path `.md` hrefs must not take that path, or “no conversion” fails on save.
- ZIP collision `Recipe (2).md` is not `[[Recipe (2)]]`. Out of this plan.
