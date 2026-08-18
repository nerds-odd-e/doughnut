# Doughnut ↔ OKF v0.2 gap (toward ADR 0004)

**Status:** Live spec is **OKF v0.2**. **P4** is closed (dual-spelling + no conversion). Frontmatter links are the same dual-spelling as the body (wiki default; not OKF path scalars). Remaining codec work is **P9** and collision basenames, plus **accept ADR 0004** (human). This tracker is not a second profile. Status stays Proposed.

**Updated:** 2026-08-18

**Feeds:** Proposed [ADR 0004](../../docs/adrs/0004-okf-compatible-notebook-markdown.md)

**Does not:** approve the ADR (humans own announce → discuss → approve)

Product profile lives in ADR 0004 Decision. This tracker is code vs [OKF v0.2](https://github.com/GoogleCloudPlatform/knowledge-catalog/blob/main/okf/SPEC.md) vs that profile.

## Current code vs OKF / ADR 0004

Portable output today is **one-way catalog ZIP** (`GET /api/notebooks/{notebook}/export`). There is **no** Markdown import, CLI OKF lint, or Git accept path. ADR 0002 Level 1 (git-native notebooks) is deferred in [PROJECT.md](../PROJECT.md).

`NotebookZipBuilder` / `ExportReadmeMarkdown` / `ExportNoteMarkdown` / `NotebookExportFilenames`:

- One notebook → a directory of `.md` files; folders are subdirectories. Non-blank notebook/folder readme → that directory’s `README.md` with export-only `type: Readme`. Blank omits the file. Empty folders with no readme and no notes are omitted (ZIP/Git-like).
- Stored notes carry `type: Note` or `type: Relationship` on `note.content` (`NoteConceptType.ensureStoredType`, production backfill `V300000270`). Export copies the leading YAML block.
- Concept filename = sanitized note title + `.md`. Collision suffix is a human sequence in export order (`Recipe.md`, `Recipe (2).md`, …), never a database key. Sanitize replaces `\/:*?"<>|` and control chars with spaces; blank → `Untitled`. When that basename is not the exact title and leading YAML has no `title` key, export wraps `title: {display title}` (`NoteLeadingFrontmatter.ensureTitleKey`). Author `title` is left unchanged. Stored notes keep the title column.
- Export does not inject `# {title}` or Doughnut note ids. Author YAML (including `image:`) and author headings are copied as stored (no injected identity or `okf_version`).
- Wiki links (`[[…]]`) are copied as stored (no rewrite to path Markdown; that is the profile, not a remaining codec job).
- No listing `index.md`, no `log.md`, no root `okf_version` (missing listing is conformant).
- Note titles `readme` / `readme.md` are hard-reserved. Note titles `index` / `index.md` / `log` / `log.md` are allowed; note create/edit and notebook health warn (non-blocking). Folder and notebook names do not warn. Filename-as-title: a note titled `index` writes `index.md`. Locked in ADR 0004; not remaining work.

In the product (not the ZIP), titles live in a column (max 150). Doughnut-authored inter-note links stay wiki in **body and frontmatter**; path Markdown `[display](/folder/File.md)` is the same link as `[[folder/File|display]]`. Relationship `source` / `target` are wiki links (wiki default), not OKF path scalars. Identity is folder path + title, not ZIP collision basenames (`Recipe (2).md`). `tags` / `aliases` / `cssclasses` are Obsidian-style passthrough; `aliases` must be a plain YAML list. `image:` is authored frontmatter; binaries in the tree are ADR 0002 Level 2.

## ADR 0004 profile vs codec

**P4** is closed: dual-spelling + no conversion against Proposed [ADR 0004](../../docs/adrs/0004-okf-compatible-notebook-markdown.md) Decision. Wiki and path Markdown are the same link (resolve, live/dead display, title rename rewrite, folder-rename prefix rewrite). ZIP copies stored spelling (wiki in Doughnut ZIPs is a profile exception, not remaining rewrite work). Create-note / point-at-existing still search wiki tokens only — optional product polish, not remaining codec work.

| ID | ADR 0004 rule | Code today |
|----|---------------|------------|
| **P9** | Accept / CLI lint reject trees that break OKF or this profile | No import, no lint command, no Git accept. ZIP is download-only (`notebook_export.feature`). Concept `index.md` / `log.md` warn only (see ADR 0004). |

Remaining besides **P9**: collision basenames (identity is folder path + title, not `Recipe (2).md`).

Frontmatter `source` / `target` / `overlaps` are **wiki links**, not OKF §6.2 path-valued fields. Wiki default; path Markdown accepted; no conversion; no backfill. Product holes (wiki-only property display, reduce-on-delete, overlaps whole-item, editor flush) are [015](../quick/015-frontmatter-dual-spelling-links/PLAN.md), not remaining codec conversion.

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

- **T1** — ADR 0002 Level 1 (Git objects in MySQL, identity lineage, one accept boundary). Format work here can still make ZIP export (and a future working tree) match the profile; Git authority is a separate milestone. Images and other binaries in the Git tree are ADR 0002 Level 2.

## References

- [ADR 0004](../../docs/adrs/0004-okf-compatible-notebook-markdown.md) (Proposed)
- [ADR 0002](../../docs/adrs/0002-git-native-notebooks-backed-by-mysql.md) — Git-native notebooks; OKF working tree
- [ADR 0001](../../docs/adrs/0001-ubiquitous-language.md) — portable Markdown profile pointer (OKF is not a glossary noun)
- Seed: [SEED-003](../seeds/SEED-003-close-okf-v0-2-compatibility-gaps.md)
- Code: `NotebookExportService`, `NotebookZipBuilder`, `ExportReadmeMarkdown`, `ExportNoteMarkdown`, `NotebookExportFilenames`, `ReservedReadmeTitles`
- [Open Knowledge Format v0.2](https://github.com/GoogleCloudPlatform/knowledge-catalog/blob/main/okf/SPEC.md)
