# Portable note title without generated H1

**Date:** 2026-08-18

**Trigger:** Close OKF tracker **P1** + **P2** after locking persist-vs-wrap in Proposed [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown.md).

**Goal:** Catalog ZIP note files express the display title as filename, or as `title:` frontmatter when the filename cannot round-trip, and never as a generated `# title` H1.

ADR 0004 stays **Proposed**. Humans still own accept. Do not reopen **D3** (filename-as-title; `title:` only when the path cannot round-trip).

## Out of scope

- **P3** / **P5** — human collision suffixes / no DB id in filenames (id suffix may remain; `title:` still wraps because that filename is not the exact title)
- **P4** wiki → path Markdown; **P8** attachments; **P9** accept/lint
- Persist or backfill `title:` on stored notes
- Stripping author H1s on ordinary save
- Accepting ADR 0004 wholesale
