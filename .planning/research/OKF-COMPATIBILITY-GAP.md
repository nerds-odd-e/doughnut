# Doughnut ↔ OKF v0.2 gap (toward ADR 0004)

**Status:** Live spec is **OKF v0.2**. Catalog ZIP export does not yet match Proposed ADR 0004 (generated H1, wiki links, id-suffixed collisions). **P1**/**P2** persist-vs-wrap is locked in the ADR Decision; remaining for those IDs is codec. Other remaining work is the lossless codec + accept/lint, plus **accept ADR 0004** (human). This tracker is not a second profile. Status stays Proposed.

**Updated:** 2026-08-18

**Feeds:** Proposed [ADR 0004](../../docs/adrs/0004-okf-compatible-notebook-markdown.md)

**Does not:** approve the ADR (humans own announce → discuss → approve)

Product profile lives in ADR 0004 Decision. This tracker is code vs [OKF v0.2](https://github.com/GoogleCloudPlatform/knowledge-catalog/blob/main/okf/SPEC.md) vs that profile.

## Current code vs OKF / ADR 0004

Portable output today is **one-way catalog ZIP** (`GET /api/notebooks/{notebook}/export`). There is **no** Markdown import, CLI OKF lint, or Git accept path. ADR 0002 Level 1 (git-native notebooks) is deferred in [PROJECT.md](../PROJECT.md).

`NotebookZipBuilder` / `ExportReadmeMarkdown` / `ExportNoteMarkdown` / `NotebookExportFilenames`:

- One notebook → a directory of `.md` files; folders are subdirectories. Non-blank notebook/folder readme → that directory’s `README.md` with export-only `type: Readme`. Blank omits the file. Empty folders with no readme and no notes are omitted (ZIP/Git-like).
- Stored notes carry `type: Note` or `type: Relationship` on `note.content` (`NoteConceptType.ensureStoredType`, production backfill `V300000270`). Export copies the leading YAML block.
- Concept filename = sanitized note title + `.md`. Collision suffix is `" (" + noteId + ")"` (database key), not a human sequence. Sanitize replaces `\/:*?"<>|` and control chars with spaces; blank → `Untitled`. No `title:` frontmatter when the filename is not the exact title.
- Every note file gets a generated `# {title}` H1. Author YAML, if any, is copied verbatim (no injected identity or `okf_version`).
- Wiki links (`[[…]]`) and attachment paths (`/attachments/images/{id}/…`) are left unchanged.
- No listing `index.md`, no `log.md`, no root `okf_version` (missing listing is conformant).
- Note titles `readme` / `readme.md` are hard-reserved. Note titles `index` / `index.md` / `log` / `log.md` are allowed; note create/edit and notebook health warn (non-blocking). Folder and notebook names do not warn. Filename-as-title: a note titled `index` writes `index.md`. Locked in ADR 0004; not remaining work.

In the product (not the ZIP), titles live in a column (max 150). Inter-note links are title/alias wiki links. Relationship notes also have `relation` / `source` / `target`. `tags` / `aliases` / `cssclasses` are Obsidian-style passthrough; `aliases` must be a plain YAML list. Images are MySQL blobs addressed by numeric id.

## ADR 0004 profile vs codec

| ID | ADR 0004 rule | Code today |
|----|---------------|------------|
| **P1** | Title in the portable file is the filename, or `title:` when needed. Author H1s stay body; ordinary save keeps them | Export always injects `# {title}`. |
| **P2** | When the filename cannot round-trip the title, set `title` in frontmatter (export-only wrap; leave author `title` if present). Filename length follows the title column | Sanitize/collision change the path with **no** `title:` key. |
| **P3** | Collision suffixes are human (`Recipe (2).md`), never DB keys | Suffix is the note id. Tests that use id `2` look sequential by coincidence. |
| **P4** | Canonical inter-note links are path-based Markdown (`/path.md` or relative) | Export does not rewrite `[[wiki]]`. In-app canonical form remains wiki links. |
| **P5** | No Doughnut note id / UUID / sync manifest in the tree | Collision filenames leak the note id (**P3**). Frontmatter is otherwise identity-free. |
| **P8** | Attachments: portable absolute URLs until Git binaries (ADR 0002 Level 2) | Paths stay `/attachments/images/{id}/…` (host-relative, id in the path). |
| **P9** | Accept / CLI lint reject trees that break OKF or this profile | No import, no lint command, no Git accept. ZIP is download-only (`notebook_export.feature`). Concept `index.md` / `log.md` warn only (see ADR 0004). |

Lossless round-trip (ADR 0004 Decision) is not implemented: there is no inverse of the ZIP codec.

## Optional OKF (warn-only per ADR 0004)

ADR 0004: recommendations (e.g. `tags` shape) may warn without blocking.

| ID | Topic | Notes |
|----|--------|--------|
| **O1** | `tags` as a YAML list of short strings | Passthrough; no export/lint shape check. |
| **O2** | Provenance / trust / lifecycle (`sources`, `generated`, `verified`, `status`, `stale_after`) | v0.2 optional families. Not produced. Absence is conformant. |
| **O3** | `type: Attested Computation` and computation keys | Out of Doughnut product scope unless a later profile says otherwise. |
| **O4** | `log.md` (§9) | Optional; not emitted. Fine when absent. |

Do not treat **O2**–**O4** as codec blockers.

## Profile vs spec (human, before accepting ADR 0004)

These are Decision tensions, not missing lines of export code.

| ID | Tension |
|----|---------|
| **D3** | Filename-as-title matches OKF “derive title from filename” and Obsidian inline titles. OKF still *recommends* a `title` field; ADR 0004 uses `title` only when the path cannot round-trip. |

Humans still own accept / reject / supersede of ADR 0004 (`docs/adrs/README.md`). Do not silently ship a listing generator, or label a ZIP OKF-conformant while listings / `log.md` / `okf_version` are omitted, or when a concept occupies `index.md` / `log.md`.

## Deferred elsewhere

- **T1** — ADR 0002 Level 1 (Git objects in MySQL, identity lineage, one accept boundary). Format work here can still make ZIP export (and a future working tree) match the profile; Git authority is a separate milestone.

## References

- [ADR 0004](../../docs/adrs/0004-okf-compatible-notebook-markdown.md) (Proposed)
- [ADR 0002](../../docs/adrs/0002-git-native-notebooks-backed-by-mysql.md) — Git-native notebooks; OKF working tree
- [ADR 0001](../../docs/adrs/0001-ubiquitous-language.md) — portable Markdown profile pointer (OKF is not a glossary noun)
- Seed: [SEED-003](../seeds/SEED-003-close-okf-v0-2-compatibility-gaps.md)
- Plan (P1/P2 codec): [`.planning/quick/012-portable-note-title-without-generated-h1/`](../quick/012-portable-note-title-without-generated-h1/)
- Code: `NotebookExportService`, `NotebookZipBuilder`, `ExportReadmeMarkdown`, `ExportNoteMarkdown`, `NotebookExportFilenames`, `ReservedReadmeTitles`
- [Open Knowledge Format v0.2](https://github.com/GoogleCloudPlatform/knowledge-catalog/blob/main/okf/SPEC.md)
