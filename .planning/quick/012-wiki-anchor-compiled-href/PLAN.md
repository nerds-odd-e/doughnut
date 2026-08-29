# Compiled href for wiki / path-Markdown anchors

**Status:** planned, not started.
**Type:** ad-hoc plan (`.planning/quick/`)
**Do not execute until the developer approves.**
**Depends on:** Proposed [ADR 0005](../../../docs/adrs/0005-web-routes.md) wiki-destination clause (compiled `href`; concept path is not a Vue location). ADR wording for this policy is already in that draft — this plan is the product change.

## Goal

Live path-Markdown anchors use the same compiled note-show `href` as live `[[wiki]]`. Unresolved path-Markdown uses `href="#"`. Stored tokens stay ADR 0004 path Markdown. Stop after any slice.

## Inspection

Today live `[[wiki]]` compiles `href` via `noteShowHref`. Live path Markdown keeps the bundle-relative path as `href` (`/Folder/Title.md`). Unresolved wiki uses `#`; unresolved path Markdown keeps the concept path. `handleRichContentAnchorClick` still `navigateInApp(href)` for leftover non-http strings, so a concept path can be fed to the router. Quill left-click `preventDefault`s, so the user-visible hole is middle-click / copy-link / open-in-new-tab, plus any leftover click that misses `data-note-id`.

`WikiLinkToken.vue` already uses named `noteShowLocation` / `href="#"`. Backend token parse is unchanged.

Existing E2E `path_markdown_link.feature` follows live path Markdown but uses `following the wiki link`, which does **not** assert `href`. Wiki spelling already has `the wiki link … should open the note titled …` (`expectNoteShowHref`).

## Design decisions

- **No 0004 change.** Authored `[label](/folder/File.md)` stays. Serialize keeps using `data-wiki-title` (`wikiAnchorToMarkdownToken`), never the compiled `href`.
- **One compiled `href` language in the DOM.** Live: `noteShowHref(id)`. Unresolved (dead / pending): `#`. Concept path only on `data-wiki-title` and in markdown.
- **Do not classify by SPA-route denylist.** `hrefLooksLikeConceptNotePath` stays a token/inbound classifier (not note-show), not a `routeMetadata` mirror.
- **Incoming HTML still has concept-path `href`s** from marked (`<a href="/Folder/Title.md">label</a>`) and from in-session Quill written before this change. `upgradePathMarkdownAnchors` must still match those inputs, then emit compiled `href`. After unresolved uses `#`, dead→live must also match `href="#"`.
- **Surfaces in order:** note body (common), then property-field / relationship source (same rule, narrower precondition), then unresolved `href`, then click leftover.
- **Not 011.** `.planning/quick/011-named-spa-route-honesty-follow-up/` is test-router / E2E visit honesty. Do not fold this in.

## Slices

Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

Verify with `CURSOR_DEV=true nix develop -c pnpm frontend:test` on the specs named in the slice. Slice 1 also runs `path_markdown_link.feature` (`cypress run --spec`). No full E2E suite.

### 1. Live path-Markdown in note body has a note-show href — Behavior `[ ]`

**Pre:** Note body contains a resolved path-Markdown link (wikiTitles hit).
**Trigger:** View the note as rich content.
**Post:** The live anchor’s `href` is `noteShowHref(id)` (same shape as live `[[wiki]]`). Stored markdown is still the path-Markdown token. Following the link still opens the target note.

Tests first: `NoteTextContent.wikiLinks.spec.ts` (live path Markdown `href` is `/Folder/Title.md` today); `replaceWikiLinksInHtml.spec.ts` (dead→live path Markdown expected `href`). E2E: in `e2e_test/features/note_topology/path_markdown_link.feature`, the live outline uses `following the wiki link` — switch that Then to `the wiki link "<display>" should open the note titled "<target_title>"` so `expectNoteShowHref` runs (existing step; will fail until production changes). `quillHtmlToMarkdown.spec.ts`: keep inbound fixtures that still have a concept-path `href`; add or adjust that a live tag with `noteShowHref` + `data-wiki-title` still serializes to `[label](/Folder/Title.md)`.

Production: `upgradePathMarkdownAnchors` live `href` is `noteShowHref(w.noteId)`. Still match marked `<a href="{concept}">` and existing live/dead tags whose `href` is still the concept path. Do not change unresolved wrapping yet.

### 2. Live path-Markdown in a property field has a note-show href — Behavior `[ ]`

**Pre:** A YAML property scalar (e.g. relationship `source`) is resolved path Markdown.
**Trigger:** Render the property value field.
**Post:** Live anchor `href` is `noteShowHref(id)`. Serialize still `[Moon](/Moon.md)` (or the authored token). No wiki brackets on path Markdown.

Tests: `propertyValueField.spec.ts` live `/Moon.md` → `noteShowHref(42)` (round-trip markdown unchanged); `RichMarkdownEditor.propertyWikiLinks.spec.ts` relationship source live `href`.

Production: `propertyValuePlainToDisplayHtml` path-Markdown live branch uses `noteShowHref(noteId)` instead of `target`. Unresolved in this function still uses the concept-path `href` until slice 3.

### 3. Unresolved path-Markdown href is `#` — Behavior `[ ]`

**Pre:** Path Markdown in body or property field does not resolve (dead or pending).
**Trigger:** Render rich content / property field.
**Post:** Unresolved anchor `href` is `#` (same as unresolved `[[wiki]]`). `data-wiki-title` is still the concept path. Serialize still path Markdown. Dead click still opens the dead-wiki flow, not a route.

Tests: `NoteTextContent.wikiLinks.spec.ts` unresolved path Markdown (add `href="#"`); `propertyValueField.spec.ts` unresolved `href="/Moon.md"` → `href="#"` (keep `not.toContain("/n")` for unresolved); `replaceWikiLinksInHtml` / `quillHtmlToMarkdown` unresolved path Markdown round-trip after wrap. Dead→live still upgrades when wikiTitles arrive (`upgradePathMarkdownAnchors` must match dead/pending tags with `href="#"` as well as leftover concept-path `href`s).

Production: `markUnresolvedWikiLinks` and property-field unresolved path Markdown pass `href: "#"`.

### 4. Concept-path and `#` hrefs are not routed — Behavior `[ ]`

**Pre:** A rich-content anchor is not a dead/pending wiki handler hit and not a live `data-note-id` wiki link.
**Trigger:** Click (the existing capture `preventDefault` + `handleRichContentAnchorClick`).
**Post:** `href="#"` and concept-path `href`s do not call `navigateInApp`. `http(s)` still opens a tab. Live wiki with `data-note-id` still uses `noteShowLocation`.

Test: `wikiLinkMarkup.spec.ts` — extend `handleRichContentAnchorClick` (concept path / `#` must not navigate; keep the existing path-Markdown + `data-note-id` case). Production: leftover branch in `handleRichContentAnchorClick` skips `#` and `hrefLooksLikeConceptNotePath`. Do not push a raw concept path. Optional: leftover note-show path strings become `noteShowLocation` instead of `navigateInApp(href)` if that stays a small change in this slice; if not, leave string push for real note-show hrefs only.

## Out of scope

- Executing this plan until the developer approves
- Requiring `.md` on path-Markdown (0004 tightening)
- Changing compact `/n:id` or SPA prefixes
- Teaching `hrefLooksLikeConceptNotePath` a `routeMetadata` denylist
- ADR 0004; accepting ADR 0005 (human)
- Plan 011 named-route honesty follow-up / E2E visit paths
- `WikiLinkToken.vue` (already named `:to` / `href="#"`)
- Backend `WikiLinkMarkdown` token parse
