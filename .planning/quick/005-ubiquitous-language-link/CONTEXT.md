# CONTEXT — Ubiquitous language: bare “link”

## Why

ADR `docs/adrs/0001-ubiquitous-language.md` (Proposed) disambiguation rule:

> Use **wiki link**, **relationship**, or **Wikidata association** — never bare **link** or **wiki** when the kind matters.

Bare **link** currently names wiki links, relationships, and (in one toolbar) both. This plan aligns product copy first, then tests, then identifiers.

## In scope

- User-visible strings that mean wiki link and/or relationship
- Matching E2E / unit assertions and page-object names driven by those strings
- Stop-safe Structure renames of frontend modules/CSS markers that encode bare `link` for those concepts

## Out of scope

- Accepting the ADR (human process)
- Bare **wiki** (Wikidata) cleanup as a separate slice
- HTTP / router / DaisyUI / Quill URL / invitation / Wikipedia URL senses of “link”
- Breaking OpenAPI / MCP renames (`outgoingLinks`, `linkText`) unless a later Jidoka-approved phase is added
- Splitting the toolbar into two buttons (wiki link vs relationship)

## Recommended microcopy (Jidoka if disagreeing)

| Surface | Current | Proposed |
|---------|---------|----------|
| Note toolbar aria/title | `Link` | `Wiki link or relationship` (+ shortcut in title) |
| Search hit CTA | `Add link` | `Use this note` |
| Choice header | `Link to:` | `Target:` |
| Dead-link retarget primary | `Link "…" to this note` | `Point wiki link "…" at this note` |
| Dead-link modal title | `Dead link:` | `Dead wiki link:` |
| Dead-link secondary | `Link to an existing note` | `Point at an existing note` |
| Accidental match CTA | `Build a link` | `Add wiki link or relationship` |
| Delete confirmation | `Leave all references as dead link` | `Leave all references as dead wiki links` |
| Path name warning | `Links will not work…` | `Wiki links will not work…` |
| Notebook rename confirm | `links from other notebooks…` | `wiki links from other notebooks…` |
| Assimilation type (E2E) | `link` | `relationship` |

Already glossary-correct (leave alone): `Insert as a wiki link`, `Add wiki link as a new property`, `Add a new relationship note`, health title `Dead wiki links`.

## Inventory source

Prior audit of bare-`link` violations (UI → E2E → identifiers). Re-scan touched files when executing each phase.
