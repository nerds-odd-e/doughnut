---
id: SEED-020
status: dormant
planted: 2026-09-17
planted_during: Trash feature architecture review
trigger_when: when correcting recoverable-trash terminology across maintained product artifacts
scope: S
---

# SEED-020: Use trash and deletion vocabulary consistently

## Why This Matters

Donut maintainers and contributors currently encounter recoverable trash behavior
described as trash, deletion, and soft deletion across the API, production code,
and tests. The Accepted ubiquitous language distinguishes recoverable
location-based trash from permanent entity deletion, but maintained names do not
yet follow it consistently.

## Alternatives and Decision

Rename only the two misleading production comments as the smallest correction,
but that would leave the same obsolete concept in DTOs, generated API names,
frontend types, component filenames, and tests. Align the complete maintained
surface in one bounded correction while preserving behavior. Historical migration
files keep historically accurate terminology.

## Story Decomposition

<a id="story-1"></a>

### 1. Use trash vocabulary for recoverable note removal

- **For / why:** Donut maintainers and contributors can use one current domain
  vocabulary without mistaking recoverable trash for permanent deletion.
- **Evaluation:** Accepted ADR 0001 defines Trash and permanent deletion distinctly;
  OpenAPI, production code, frontend code, generated-client consumers, and tests
  use trash terminology for the recoverable `_trash` journey. Delete terminology
  remains only where an entity is permanently removed or historical material
  accurately describes the former model.
- **Scope:** Rename misleading soft-delete comments, test language, DTOs, API-shaped
  types, and trash UI/code artifacts. Regenerate the frontend API client when
  OpenAPI type names change. Preserve routes, request fields, user-visible behavior,
  data, and the distinction between trash and Git-published permanent deletion.
- **Key example:** The note Trash endpoint still offers the same reference-handling
  choices and moves the same note beneath `_trash`, while its request type and all
  maintained callers describe that operation as trash rather than delete.
- **Effort hypothesis:** S (30–60 minutes), moderate confidence; generated-client
  propagation is mechanical but crosses backend and frontend compile boundaries.
- **Depends on:** No unfinished product prerequisite.
- **Safe stopping point:** Current behavior is unchanged, all maintained names agree
  with the Accepted glossary, and backend/frontend suites prove the renamed boundary.

## Ordering and Scope Reduction

This correction is first in the product backlog by owner direction. Keep the
cross-stack naming boundary together; a partial rename would retain competing
domain terms.

## Open Decisions

None.

## When to Surface

Immediately, from the product backlog.

## Breadcrumbs

- [ADR 0001 — Ubiquitous language](../../docs/adrs/0001-ubiquitous-language.md)
- [ADR 0004 — Trash](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md#trash)
