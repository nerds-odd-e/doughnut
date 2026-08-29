# Compiled href for wiki / path-Markdown anchors

**Status:** in progress (slice 1 done).
**Type:** ad-hoc plan (`.planning/quick/`)
**Depends on:** Proposed [ADR 0005](../../../docs/adrs/0005-web-routes.md) wiki-destination clause (compiled `href`; concept path is not a Vue location). ADR wording for this policy is already in that draft — this plan is the product change.

## Goal

Live path-Markdown anchors use the same compiled note-show `href` as live `[[wiki]]`. Unresolved path-Markdown uses `href="#"`. Stored tokens stay ADR 0004 path Markdown. Stop after any slice.

## Inspection

Live `[[wiki]]` and live **body** path Markdown compile `href` via `noteShowHref`. Property-field path Markdown still uses the concept path as `href`. Unresolved wiki uses `#`; unresolved path Markdown still keeps the concept path. `handleRichContentAnchorClick` still `navigateInApp(href)` for leftover non-http strings. Quill left-click `preventDefault`s; user-visible hole is middle-click / copy-link / open-in-new-tab, plus leftover click that misses `data-note-id`.

`WikiLinkToken.vue` already uses named `noteShowLocation` / `href="#"`. Backend token parse is unchanged.

`path_markdown_link.feature` live outline asserts `expectNoteShowHref` via `the wiki link … should open`.

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

### 1. Live path-Markdown in note body has a note-show href — Behavior `[x]`

**Pre:** Note body contains a resolved path-Markdown link (wikiTitles hit).
**Trigger:** View the note as rich content.
**Post:** The live anchor’s `href` is `noteShowHref(id)`. Stored markdown is still the path-Markdown token. Following the link still opens the target note.

**Done:** `upgradePathMarkdownAnchors` emits `noteShowHref(w.noteId)` and still matches marked concept-path `<a href>` plus leftover live/dead tags whose `href` is the concept path. Serialize still uses `data-wiki-title`.

**Learning:** leftover in-session live tags with a concept-path `href` needed an extra replace (shared `livePathMarkdownAttrs`). Slice 3 still must match `href="#"` after unresolved wrapping.

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

- Requiring `.md` on path-Markdown (0004 tightening)
- Changing compact `/n:id` or SPA prefixes
- Teaching `hrefLooksLikeConceptNotePath` a `routeMetadata` denylist
- ADR 0004; accepting ADR 0005 (human)
- Plan 011 named-route honesty follow-up / E2E visit paths
- `WikiLinkToken.vue` (already named `:to` / `href="#"`)
- Backend `WikiLinkMarkdown` token parse
