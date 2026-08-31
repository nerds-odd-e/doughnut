---
id: SEED-009
status: dormant
planted: 2026-08-31
planted_during: unknown
trigger_when: when planning or changing Portable path and wiki-link resolution
scope: unknown
---

# SEED-009: Make Portable path shorthand resolution reject ambiguity

## Why This Matters

ADR 0001 and ADR 0004 now say:

> A shorthand Portable path resolves only when it identifies one destination
> under the documented resolution scope. Otherwise it is
> unresolved/ambiguous, and Donut asks for a longer path.

The system is not yet honest to that rule. `WikiLinkResolver` currently orders
same-title candidates by private database note ID and selects the first readable
one. It also prefers any title matches before considering aliases. Consequently,
the meaning of portable Markdown can depend on database creation order rather
than solely on the Portable notebook tree and documented resolution scope.

## When to Surface

**Trigger:** when planning or changing Portable path resolution, wiki-link
resolution, generated link spelling, dead/ambiguous link UX, or notebook
import/lint behavior.

This seed will surface during `$gsd-new-milestone` when the milestone scope
matches.

## Scope Estimate

**Unknown** — plan the behavior as stop-safe slices before implementation. At
minimum, cover candidate collection within one notebook scope, ambiguity across
titles and aliases, notebook-qualified shorthand, property selectors, generated
shortest-unique paths, and user-visible guidance to supply a longer path.

## Breadcrumbs

- `docs/adrs/0001-ubiquitous-language.md` — canonical Portable path vocabulary and shorthand rule
- `docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md` — spelling, resolution scope, ambiguity behavior
- `docs/adrs/0005-web-routes.md` — compilation from stored Portable paths to SPA locations
- `backend/src/main/java/com/odde/donut/services/WikiLinkResolver.java` — currently selects the first readable candidate
- `backend/src/main/java/com/odde/donut/entities/repositories/NoteRepository.java` — title candidates ordered by note ID
- `backend/src/main/java/com/odde/donut/algorithms/WikiLinkTargetReference.java` — parses qualified, shorthand, and path-shaped destinations
- `backend/src/test/java/com/odde/donut/services/WikiTitleCacheTitleResolutionTest.java` — explicitly expects lowest-note-ID resolution
- `frontend/src/utils/buildWikiLinkText.ts` — currently generates title-only shorthand destinations
- `e2e_test/features/note_topology/wiki_link.feature` — resolved/dead wiki-link behavior and repair UX
- `e2e_test/features/note_topology/path_markdown_link.feature` — path-Markdown resolution and unresolved behavior

## Notes

Deferred intentionally. The later plan should distinguish an ambiguous Portable
path from a missing destination where that improves the repair UX, while both
remain unresolved for navigation and cache purposes. It should also make code
names and API fields honest to **Portable path**, **link destination**, and
**display text**, rather than preserving `targetToken` as domain vocabulary.
