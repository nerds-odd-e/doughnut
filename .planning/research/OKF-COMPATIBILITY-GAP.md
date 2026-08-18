# Doughnut ↔ OKF v0.2 gap (toward ADR 0004)

**Status:** Live spec is **OKF v0.2**. Catalog ZIP export is **not** an OKF-conformant bundle. Remaining work is a lossless codec + accept/lint that match Proposed ADR 0004, plus **accept ADR 0004** (human). This tracker is not a second profile.

**Updated:** 2026-08-18

**Feeds:** Proposed [ADR 0004](../../docs/adrs/0004-okf-compatible-notebook-markdown.md)

**Does not:** approve the ADR (humans own announce → discuss → approve)

Product profile lives in ADR 0004 Decision. This tracker is code vs [OKF v0.2](https://github.com/GoogleCloudPlatform/knowledge-catalog/blob/main/okf/SPEC.md) vs that profile.

**Baseline (A1, closed):** On 2026-08-17 the linked `main` spec identifies itself as **Version 0.2** (self-contained; §13 records the v0.1 → v0.2 delta). The SEED-003 “0.1 — Draft” breadcrumb is stale.

## Current code vs OKF / ADR 0004

Portable output today is **one-way catalog ZIP** (`GET /api/notebooks/{notebook}/export`). There is **no** Markdown import, CLI OKF lint, or Git accept path. ADR 0002 Level 1 (git-native notebooks) is deferred in [PROJECT.md](../PROJECT.md).

`NotebookZipBuilder` / `ExportReadmeMarkdown` / `ExportNoteMarkdown` / `NotebookExportFilenames`:

- One notebook → a directory of `.md` files; folders are subdirectories. Non-blank notebook/folder readme → that directory’s `README.md` with export-only `type: Readme`. Blank omits the file. Empty folders with no readme and no notes are omitted (ZIP/Git-like).
- Concept filename = sanitized note title + `.md`. Collision suffix is `" (" + noteId + ")"` (database key), not a human sequence. Sanitize replaces `\/:*?"<>|` and control chars with spaces; blank → `Untitled`. No `title:` frontmatter when the filename is not the exact title.
- Every note file gets a generated `# {title}` H1. Author YAML, if any, is copied verbatim (no injected `type`, identity, or `okf_version`).
- Wiki links (`[[…]]`) and attachment paths (`/attachments/images/{id}/…`) are left unchanged.
- No `log.md`. No `index.md` listing (missing is conformant). No root `okf_version`.
- Note titles `readme` / `readme.md` are reserved. Titles `index` / `index.md` / `log` / `log.md` are allowed; PathNameEditor and notebook health warn (non-blocking). A note titled `index` still writes `index.md`.

In the product (not the ZIP), titles live in a column (max 150). Inter-note links are title/alias wiki links. Stored notes carry `type: Note` or `type: Relationship` (relationship notes also have `relation` / `source` / `target`). `tags` / `aliases` / `cssclasses` are Obsidian-style passthrough; `aliases` must be a plain YAML list. Images are MySQL blobs addressed by numeric id.

## Blocking for an OKF-conformant tree (§11)

A bundle is conformant only if every non-reserved `.md` has parseable YAML with a non-empty `type`, and every present `index.md` / `log.md` follows §8 / §9.

**C1** is closed: persist (`NoteConceptType.ensureStoredType`) and production backfill (`V300000270`) store `type: Note` / `type: Relationship` on `note.content`. Export copies the leading YAML block, so concept files carry that `type`.

**C2** is closed: ZIP writes container Readme as `README.md` (`type: Readme`), not as `index.md`. Missing listing is conformant. See **D1**.

| ID | Gap | Code today |
|----|-----|------------|
| **C3** | Concept files MUST NOT use reserved basenames `index.md` / `log.md` | Titles `index` / `log` (and `.md` variants) are allowed. PathNameEditor and health warn; save still succeeds. A note titled `index` still writes `index.md`. |

Missing `index.md` / `log.md` is **not** a §11 failure. Optional families (`sources`, `generated`, `verified`, `status`, `stale_after`, Attested Computation) are **not** required; consumers must not reject their absence.

## ADR 0004 profile vs codec

| ID | ADR 0004 rule | Code today |
|----|---------------|------------|
| **P1** | Do not inject a generated `# title` H1 | Export always injects `# {title}`. |
| **P2** | When the filename cannot round-trip the title, set `title` in frontmatter | Sanitize/collision change the path with **no** `title:` key. |
| **P3** | Collision suffixes are human (`Recipe (2).md`), never DB keys | Suffix is the note id. Tests that use id `2` look sequential by coincidence. |
| **P4** | Canonical inter-note links are path-based Markdown (`/path.md` or relative) | Export does not rewrite `[[wiki]]`. In-app canonical form remains wiki links. |
| **P5** | No Doughnut note id / UUID / sync manifest in the tree | Collision filenames leak the note id (**P3**). Frontmatter is otherwise identity-free. |
| **P6** | Preserve author-owned and unknown frontmatter | Export copies the leading YAML block; it does not add `type` or drop unknown keys. |
| **P7** | Root `okf_version` only on a generated listing `index.md`; missing listing is conformant | **Omit listing; warn on index/log.** No `index.md` / `okf_version` generated. |
| **P8** | Attachments: portable absolute URLs until Git binaries (ADR 0002 Level 2) | Paths stay `/attachments/images/{id}/…` (host-relative, id in the path). |
| **P9** | Accept / CLI lint reject trees that break OKF or this profile | No import, no lint command, no Git accept. ZIP is download-only (`notebook_export.feature`). |
| **P10** | Reserved basenames: `index.md`, `log.md` | **Omit listing; warn on index/log.** Product still hard-reserves `readme` / `readme.md`. Concept titles `index` / `log` warn and still save. |

Lossless round-trip (ADR 0004 Decision) is not implemented: there is no inverse of the ZIP codec.

## Optional OKF (warn-only per ADR 0004)

ADR 0004: recommendations (e.g. `tags` shape) may warn without blocking.

| ID | Topic | Notes |
|----|--------|--------|
| **O1** | `tags` as a YAML list of short strings | Passthrough; no export/lint shape check. |
| **O2** | Provenance / trust / lifecycle (`sources`, `generated`, `verified`, `status`, `stale_after`) | v0.2 optional families. Not produced. Absence is conformant. |
| **O3** | `type: Attested Computation` and computation keys | Out of Doughnut product scope unless a later profile says otherwise. |
| **O4** | `log.md` (§9) | Optional; not emitted. Fine for §11. |

Do not treat **O2**–**O4** as codec blockers.

## Profile vs spec (human, before accepting ADR 0004)

These are Decision tensions, not missing lines of export code.

**D1** is closed: Proposed ADR 0004 Decision maps folder/notebook **Readme** to `README.md` / `type: Readme` (not OKF `index.md`). `index.md` remains the listing file and is not emitted. Status stays Proposed.

**D2** is closed: ADR 0004 Decision names relationship notes as concepts (`type: Relationship`); ordinary notes use `type: Note`. Persist, compose, and backfill match that spelling.

| ID | Tension |
|----|---------|
| **D3** | Filename-as-title matches OKF “derive title from filename” and Obsidian inline titles. OKF still *recommends* a `title` field; ADR 0004 uses `title` only when the path cannot round-trip. |

Humans still own accept / reject / supersede of ADR 0004 (`docs/adrs/README.md`). Do not silently ship a listing generator, or label a ZIP OKF-conformant while listings / `log.md` / `okf_version` are omitted.

## Deferred elsewhere

- **T1** — ADR 0002 Level 1 (Git objects in MySQL, identity lineage, one accept boundary). Format work here can still make ZIP export (and a future working tree) match the profile; Git authority is a separate milestone.

## References

- [ADR 0004](../../docs/adrs/0004-okf-compatible-notebook-markdown.md) (Proposed)
- [ADR 0002](../../docs/adrs/0002-git-native-notebooks-backed-by-mysql.md) — Git-native notebooks; OKF working tree
- [ADR 0001](../../docs/adrs/0001-ubiquitous-language.md) — portable Markdown profile pointer (OKF is not a glossary noun)
- Seed: [SEED-003](../seeds/SEED-003-close-okf-v0-2-compatibility-gaps.md)
- Code: `NotebookExportService`, `NotebookZipBuilder`, `ExportReadmeMarkdown`, `ExportNoteMarkdown`, `NotebookExportFilenames`, `ReservedReadmeTitles`
- [Open Knowledge Format v0.2](https://github.com/GoogleCloudPlatform/knowledge-catalog/blob/main/okf/SPEC.md)
