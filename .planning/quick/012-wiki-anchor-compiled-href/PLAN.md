# Compiled href for wiki / path-Markdown anchors

**Status:** complete.
**Type:** ad-hoc plan (`.planning/quick/`)

Live path-Markdown anchors use `noteShowHref(id)`; unresolved use `href="#"`. Stored tokens stay ADR 0004 path Markdown (`data-wiki-title`). Leftover `#` and concept-path hrefs are not routed.

Product: `replaceWikiLinksInHtml.ts`, `propertyValueField.ts`, `handleRichContentAnchorClick` in `wikiLinkMarkup.ts`. Policy text: Proposed [ADR 0005](../../../docs/adrs/0005-web-routes.md) wiki-destination clause (not accepted by this plan).
