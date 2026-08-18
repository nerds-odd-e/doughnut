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

Portable notebooks should be OKF v0.2 trees plus the Doughnut profile in Proposed [ADR 0004](../../docs/adrs/0004-okf-compatible-notebook-markdown.md). Catalog ZIP export is not yet that profile (generated H1, wiki links, id-suffixed collisions). Closing the gap is what makes ZIP / a future Git working tree openable by OKF and Obsidian-style tools without Doughnut IDs in the files.

## When to Surface

**Trigger:** accepting ADR 0004; changing notebook ZIP export; adding Markdown import, CLI lint, or Git accept (ADR 0002 Level 1).

Also surface when adding `title` frontmatter or rewriting wiki links to path Markdown. Do not reopen `index` / `log` note titles as a hard reserve.

## Scope Estimate

**Large** — remaining work is profile accept plus a lossless codec (and later accept/lint):

1. Humans accept Proposed [ADR 0004](../../docs/adrs/0004-okf-compatible-notebook-markdown.md). See `docs/adrs/README.md`.
2. Remaining codec IDs: **P1**–**P5**, **P8**–**P9**. Tracker: [OKF-COMPATIBILITY-GAP.md](../research/OKF-COMPATIBILITY-GAP.md). **P1**/**P2** Decision wrap is locked; executing: [`.planning/quick/012-portable-note-title-without-generated-h1/`](../quick/012-portable-note-title-without-generated-h1/).

## Breadcrumbs

- `.planning/research/OKF-COMPATIBILITY-GAP.md` — pointer + gap ID list
- `.planning/quick/012-portable-note-title-without-generated-h1/` — close **P1**/**P2** (no generated H1; `title:` wrap when the filename cannot round-trip)
- `docs/adrs/0004-okf-compatible-notebook-markdown.md` — Proposed Doughnut OKF profile (the OKF document; not a glossary noun in ADR 0001)
- `docs/adrs/0002-git-native-notebooks-backed-by-mysql.md` — Git working tree; Level 1 deferred (**T1**)
- `backend/src/main/java/com/odde/doughnut/services/notebookExport/`
- https://github.com/GoogleCloudPlatform/knowledge-catalog/blob/main/okf/SPEC.md — live spec is **Version 0.2** (verified 2026-08-17)
