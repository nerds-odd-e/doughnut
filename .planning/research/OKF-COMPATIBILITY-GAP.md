# Doughnut ↔ OKF v0.2 gap (toward ADR 0004)

**Status:** Live spec is **OKF v0.2**. **P4** is closed (dual-spelling + no conversion). Frontmatter links are the same dual-spelling as the body (wiki default; not OKF path scalars). Filename = display name is shipped on catalog ZIP. Remaining codec work is **P9**, plus **accept ADR 0004** (human). This tracker is not a second profile. Status stays Proposed.

**Updated:** 2026-08-19

**Feeds:** Proposed [ADR 0004](../../docs/adrs/0004-okf-compatible-notebook-markdown.md)

**Does not:** approve the ADR (humans own announce → discuss → approve)

Product profile lives in ADR 0004 Decision. This tracker is code vs [OKF v0.2](https://github.com/GoogleCloudPlatform/knowledge-catalog/blob/main/okf/SPEC.md) vs that profile.

## Current code vs OKF / ADR 0004

Portable output today is **one-way catalog ZIP** (`GET /api/notebooks/{notebook}/export`). There is **no** Markdown import, CLI OKF lint, or Git accept path. ADR 0002 Level 1 (git-native notebooks) is deferred in [PROJECT.md](../PROJECT.md).

`NotebookZipBuilder` / `ExportReadmeMarkdown`:

- One notebook → a directory of `.md` files; folders are subdirectories. Non-blank notebook/folder readme → that directory’s `README.md` with export-only `type: Readme`. Blank omits the file. Empty folders with no readme and no notes are omitted (ZIP/Git-like).
- Stored notes carry `type: Note` or `type: Relationship` on `note.content` (`NoteConceptType.ensureStoredType`, production backfill `V300000270`). Export copies the leading YAML block.
- Catalog ZIP entry path is the folder trail plus `{title}.md` (folder dirs = folder names). Download name is `{notebook name}.zip`. No collision suffixes, no Untitled fallback, no codec `title:` wrap. Author-owned `title:` is copied as stored.
- Export does not inject `# {title}` or Doughnut note ids. Author YAML (including `image:`) and author headings are copied as stored (no injected identity or `okf_version`).
- Wiki links (`[[…]]`) are copied as stored (no rewrite to path Markdown; that is the profile, not a remaining codec job).
- No listing `index.md`, no `log.md`, no root `okf_version` (missing listing is conformant).
- Note titles `readme` / `readme.md` are hard-reserved. Note titles `index` / `index.md` / `log` / `log.md` are allowed; note create/edit and notebook health warn (non-blocking). Folder and notebook names do not warn. Filename-as-title: a note titled `index` writes `index.md`. Locked in ADR 0004; not remaining work.

Titles live in a column (max 150). Doughnut-authored inter-note links stay wiki in **body and frontmatter**; path Markdown `[display](/folder/File.md)` is the same link as `[[folder/File|display]]`. Relationship `source` / `target` are wiki links (wiki default), not OKF path scalars. Identity is folder path + display name (the catalog ZIP uses that path). Collision suffixes (`Recipe (2).md`) are not identity. `tags` / `aliases` / `cssclasses` are Obsidian-style passthrough; `aliases` must be a plain YAML list. `image:` is authored frontmatter; binaries in the tree are ADR 0002 Level 2.

## ADR 0004 profile vs codec

**P4** is closed: dual-spelling + no conversion against Proposed [ADR 0004](../../docs/adrs/0004-okf-compatible-notebook-markdown.md) Decision. Wiki and path Markdown are the same link (resolve, live/dead display, title rename rewrite, folder-rename prefix rewrite). ZIP copies stored spelling (wiki in Doughnut ZIPs is a profile exception, not remaining rewrite work). Create-note / point-at-existing still search wiki tokens only — optional product polish, not remaining codec work.

| ID | ADR 0004 rule | Code today |
|----|---------------|------------|
| **P9** | Accept / CLI lint reject trees that break OKF or this profile | No import, no lint command, no Git accept. ZIP is download-only (`notebook_export.feature`). Concept `index.md` / `log.md` warn only (see ADR 0004). |

Frontmatter `source` / `target` / `overlaps` are **wiki links**, not OKF §6.2 path-valued fields. Wiki default; path Markdown accepted; no conversion; no backfill. Product dual-spelling for YAML scalars, reduce-on-delete, whole overlaps items, and editor flush is closed — not remaining codec conversion.

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
| **D3** | Filename-as-title matches OKF “derive title from filename” and Obsidian inline titles. OKF still *recommends* a `title` field; ADR 0004 filename = display name, so the codec does not insert `title:`. Author-owned `title:` is preserved. |

Humans still own accept / reject / supersede of ADR 0004 (`docs/adrs/README.md`). Do not silently ship a listing generator, or label a ZIP OKF-conformant while listings / `log.md` / `okf_version` are omitted, or when a concept occupies `index.md` / `log.md`.

## Deferred elsewhere

- **T1** — ADR 0002 Level 1 (Git objects in MySQL, identity lineage, one accept boundary). Format work here can still make ZIP export (and a future working tree) match the profile; Git authority is a separate milestone. Images and other binaries in the Git tree are ADR 0002 Level 2.

## References

- [ADR 0004](../../docs/adrs/0004-okf-compatible-notebook-markdown.md) (Proposed)
- [ADR 0002](../../docs/adrs/0002-git-native-notebooks-backed-by-mysql.md) — Git-native notebooks; OKF working tree
- [ADR 0001](../../docs/adrs/0001-ubiquitous-language.md) — portable Markdown profile pointer (OKF is not a glossary noun)
- Seed: [SEED-003](../seeds/SEED-003-close-okf-v0-2-compatibility-gaps.md)
- Code: `NotebookExportService`, `NotebookZipBuilder`, `ExportReadmeMarkdown`, `ReservedReadmeTitles`
- [Open Knowledge Format v0.2](https://github.com/GoogleCloudPlatform/knowledge-catalog/blob/main/okf/SPEC.md)
