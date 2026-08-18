---
id: SEED-003
status: sprouting
planted: 2026-08-10
planted_during: ad-hoc planning session 2026-08-10
trigger_when: when accepting Proposed ADR 0004, or when changing notebook Markdown export / import / lint
scope: large
---

# SEED-003: Analyze the gap with OKF v0.2 and close it

## Why This Matters

Portable notebooks should be OKF v0.2 trees plus the Doughnut profile in Proposed [ADR 0004](../../docs/adrs/0004-okf-compatible-notebook-markdown.md). Catalog ZIP export is not yet an OKF-conformant bundle (generated H1, wiki links, id-suffixed collisions). Closing the gap is what makes ZIP / a future Git working tree openable by OKF and Obsidian-style tools without Doughnut IDs in the files.

## When to Surface

**Trigger:** accepting ADR 0004; changing notebook ZIP export; adding Markdown import, CLI lint, or Git accept (ADR 0002 Level 1).

Also surface when adding `type` / `title` frontmatter, rewriting wiki links to path Markdown, or changing how concept titles occupy `index.md` / `log.md`.

## Scope Estimate

**Large** — remaining work is profile accept plus a lossless codec (and later accept/lint):

1. Humans accept Proposed [ADR 0004](../../docs/adrs/0004-okf-compatible-notebook-markdown.md). See `docs/adrs/README.md`.
2. Remaining blocking codec IDs: **C3**, **P1**–**P6**, **P8**–**P9**. **P7** / **P10** are interim (omit listing; warn on index/log). Tracker: [OKF-COMPATIBILITY-GAP.md](../research/OKF-COMPATIBILITY-GAP.md).

## Breadcrumbs

- `.planning/research/OKF-COMPATIBILITY-GAP.md` — pointer + gap ID list
- `docs/adrs/0004-okf-compatible-notebook-markdown.md` — Proposed Doughnut OKF profile (the OKF document; not a glossary noun in ADR 0001)
- `docs/adrs/0002-git-native-notebooks-backed-by-mysql.md` — Git working tree; Level 1 deferred (**T1**)
- `backend/src/main/java/com/odde/doughnut/services/notebookExport/`
- https://github.com/GoogleCloudPlatform/knowledge-catalog/blob/main/okf/SPEC.md — live spec is **Version 0.2** (verified 2026-08-17)

## Notes

Gap analysis written 2026-08-17. The capture-time “spec is 0.1 Draft” note is stale.

**C1** and **D2** are closed (2026-08-18): persist and production backfill (`V300000270`) store `type: Note` / `type: Relationship`; ADR 0004 Decision names relationship notes as concepts.

**D1** and **C2** are closed (2026-08-18): Readme → `README.md` / `type: Readme`.
