# 0004 — OKF-compatible notebook Markdown profile

**Status:** Proposed  
**Date:** 2026-08-05  
**Decision makers:** Terry Yin  
**Consulted:** None

## Context

Doughnut exports and syncs notebooks as Markdown trees. Portable knowledge
should follow the
[Open Knowledge Format (OKF) v0.2](https://github.com/GoogleCloudPlatform/knowledge-catalog/blob/main/okf/SPEC.md)
so local copies work with OKF tooling, Obsidian-style editing, and (with
[ADR 0002](./0002-git-native-notebooks-backed-by-mysql.md)) Git-native sync.

OKF defines structure (concept files, reserved `index.md` / `log.md`, path as
concept ID, required `type` frontmatter). It does not define Doughnut title
round-trips, collision filenames, wiki-link policy, or what stays out of the
tree. Those choices need a Doughnut profile.

## Decision

Doughnut’s canonical portable notebook tree conforms to OKF v0.2 plus this
profile. Codec round-trips must be lossless for these rules.

### Bundle and concepts

- One notebook ↔ one OKF bundle (directory of `.md` files).
- Public concept ID = path without `.md` (OKF).
- Stored note markdown carries `type` and valid YAML frontmatter.
  Ordinary notes use `type: Note`. Relationship notes are concepts in
  the bundle (`type: Relationship`). OKF unknown types are allowed.
  Preserve author-owned and unknown frontmatter keys on persist and
  round-trip.
- Container **Readme** is a notebook/folder column, not a note row.
  Non-blank readme maps to that directory’s `README.md`, a bundle concept
  with `type: Readme`. Blank readme omits the file.
- Codec wrap (export / portable tree): insert `type: Readme` if missing;
  canonicalize `readme` → `Readme`; leave any other non-empty `type`.
  Preserve author YAML. Do not persist `type: Readme` on stored readme
  columns. Do not backfill. ZIP has no stored artifacts to migrate.
- `readme` / `readme.md` stay hard-reserved note titles.
- `index.md` is only the OKF directory listing. Doughnut does not emit it
  until listing generation exists; missing is conformant. Root
  `okf_version` appears only when that listing file exists.
- Concept titles that would become `index.md` or `log.md` (`index`,
  `index.md`, `log`, `log.md`, case-insensitive) are allowed. The product
  warns on create/rename (PathNameEditor, same non-blocking channel as
  wiki-link-char warnings) and does not reject.
- Empty folders exist in the tree only via tracked content (typically
  `README.md` when a readme is present); Git has no empty dirs.

### Titles, filenames, body

- Filename is the ordinary note title (Obsidian inline-title / OKF “derive
  title from filename”).
- When the exact Doughnut title cannot round-trip through a filesystem-safe
  name (sanitize, truncate, collision suffix), set `title` in frontmatter as
  the display title. Collision suffixes belong to the path, not the title
  (e.g. `Recipe.md`, `Recipe (2).md`).
- Do not inject a generated `# title` H1. Author H1s are ordinary body content.
- No Doughnut note ID, UUID, or opaque stable identity in frontmatter, body,
  filename, or a committed sync manifest (identity lineage is server-side;
  see ADR 0002).

### Links and attachments

- Prefer standard Markdown path links compatible with OKF and Obsidian.
- Wiki-style `[[…]]` may appear in author content; export/lint may rewrite or
  report them per product rules, but the accepted canonical form for
  inter-note links is path-based Markdown.
- Git-managed images/binaries are out of scope here (ADR 0002 Level 2). Until
  then, attachments may use portable absolute URLs outside the bundle.

### Validation

- Accept (push / durable write) and CLI lint reject trees that break OKF
  requirements or this profile (missing/invalid `type`, reserved-name misuse
  of `readme` / `readme.md`, unsafe paths, etc.).
- Missing `index.md` is conformant until Doughnut generates listings.
  Concept files named `index.md` / `log.md` warn; they do not fail accept.
- Recommendations (e.g. OKF `tags` shape) may warn without blocking.

## Consequences

- Export, lint, import, and Git accept share one codec contract.
- Title changes are usually filename changes; identity preservation across
  renames is ADR 0002 lineage, not Markdown metadata.
- Obsidian and OKF consumers can open a Doughnut notebook tree without Doughnut
  IDs in the files.

## Pros

- Portable, diffable, tool-friendly notebooks.
- Separates format (this ADR) from Git authority/transport (ADR 0002).

## Cons

- Filenames that cannot express the exact title need `title` frontmatter.
- Strict accept rules reject some free-form trees until fixed.

## Related

- Supersedes: (none)
- Superseded by: (none)
- Links:
  - [ADR 0002 — Git-native notebooks](./0002-git-native-notebooks-backed-by-mysql.md)
  - [Open Knowledge Format v0.2](https://github.com/GoogleCloudPlatform/knowledge-catalog/blob/main/okf/SPEC.md)
  - [Obsidian inline titles](https://obsidian.md/help/settings)
  - Tracker (code vs spec vs profile, not a second profile): [`.planning/research/OKF-COMPATIBILITY-GAP.md`](../../.planning/research/OKF-COMPATIBILITY-GAP.md)
