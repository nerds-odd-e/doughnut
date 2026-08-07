# Ubiquitous language: bare “link” — completed

**Status:** completed 2026-08-07  
**ADR:** Proposed `docs/adrs/0001-ubiquitous-language.md` disambiguation (wiki link / relationship / Wikidata association).

## Shipped outcomes (product)

- User-facing connect/dead-wiki-link/accidental-match/delete/rename copy uses glossary language (not bare “link”).
- E2E speaks wiki link / relationship; feature `wiki_link.feature`.
- Modules under `frontend/src/components/wiki-link-or-relationship/`.
- DOM markers: `dead-wiki-link`, `doughnut-wiki-link`, health/accidental-match testids; `wikiLinkDomMarkers.ts`.
- Filename follow-up: `svgs/relation_types/` (was `link_types`), `RelationChain`, `rich-content-wiki-links.scss`, wiki-link rewrite controller test; unused `CyclicLinkDetectedException` removed.

## Still out of scope

- Accepting ADR 0001; OpenAPI/`outgoingLinks`/`linkText`; bare “wiki” / Wikidata copy
- Navigational filenames (`NoteTitleWithLink`, `NotebookLink`, paste/URL “links”)