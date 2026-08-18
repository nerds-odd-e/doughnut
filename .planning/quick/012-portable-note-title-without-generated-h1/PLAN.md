# Plan: Portable note title without generated H1

**Goal:** Catalog ZIP notes keep the display title without a generated `# title` H1.

**Status:** in progress (slice 1 done; slice 2 next)

Decision lives in Proposed [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown.md) (Titles, filenames, body). Tracker: [OKF-COMPATIBILITY-GAP.md](../../research/OKF-COMPATIBILITY-GAP.md) **P1** / **P2**.

## Design

- Title column is the stored source of truth. `title:` is **codec wrap** on the portable tree (same split as Readme `type`). No Flyway. No persist/backfill.
- Insert `title: {display title}` only when the concept filename (basename without `.md`) is not the exact title **and** the leading YAML has no `title` key. Leave author `title` unchanged.
- Sanitize: replace `\/:*?"<>|` and controls with spaces; blank → `Untitled`. No extra truncation.
- Generated H1 is interim until slice 2. Slice 1 is stop-safe: collision/sanitized files gain `title:` while H1 still present.
- Collision filenames may still use the note id (**P3** remains). That path is not a round-trip, so wrap still sets `title:`.

Do not reopen persist-vs-wrap, **D3**, or **P3** in this plan.

## Slices

### 1. Non-round-trip export filename gets title frontmatter — Behavior — done

Codec wrap via `NoteLeadingFrontmatter.ensureTitleKey` (same seam as Readme `type`). Generated H1 still present.

**Learning:** E2E cannot persist two notes with the identical title (`uk_note_notebook_folder_title`). Scenario uses `Recipe` + `Recipe*` so sanitize collides; ZIP unit tests still cover identical-title collision.

### 2. Export does not inject a title H1 — Behavior — planned

**Pre:** Slice 1 is done (`title:` wrap when the filename cannot round-trip).  
**Trigger:** Catalog ZIP export.  
**Post:** Note files have no generated `# {title}` H1. Filename or `title:` carries the display title. Author headings in the body are unchanged. Stored content is unchanged.

Tests:

- E2E `notebook_export.feature` — exported note file is stored markdown (type fence + body), not an injected heading.
- Update ZIP/service tests that pin `# Pasta` / `# {title}`.

## Out of scope

- **P3** / **P5** collision suffix shape and id leak
- **P4** / **P8** / **P9**
- Persist/backfill `title:`; ordinary-save H1 strip
- Accepting ADR 0004
