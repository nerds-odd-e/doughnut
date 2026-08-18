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
concept ID, required `type` frontmatter). Doughnut title round-trips, collision
filenames, wiki-link policy, and what stays out of the tree are a producer
profile.

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
  Preserve author YAML. `type: Readme` is export-only; stored readme
  columns stay as authored.
- `readme` / `readme.md` stay the only hard-reserved note titles.
- `index.md` is only the OKF directory listing. Doughnut emits a listing
  only once listing generation exists; missing is conformant. Root
  `okf_version` appears only when that listing file exists.
- Concept titles that would become `index.md` or `log.md` (`index`,
  `index.md`, `log`, `log.md`, case-insensitive) are allowed. The product
  warns on create/rename (PathNameEditor, notebook health); save succeeds.
  Filename-as-title still applies: an insisted title is exported as that
  basename. That tree is a profile exception to OKF §3.1 / §11.
- Empty folders exist in the tree only via tracked content (typically
  `README.md` when a readme is present).

### Titles, filenames, body

- Filename is the ordinary note title (Obsidian inline-title / OKF “derive
  title from filename”). The title column (max 150) is the product source of
  truth. Export sanitize replaces `\/:*?"<>|` and control characters with
  spaces; a blank result becomes `Untitled`. Filename length follows the
  title column.
- When the exact Doughnut title cannot round-trip through that filesystem-safe
  name (sanitize, Untitled fallback, collision suffix), set `title` in
  frontmatter as the display title. Collision suffixes belong to the path,
  not the title (e.g. `Recipe.md`, `Recipe (2).md`).
- Codec wrap (export / portable tree): when the concept filename (basename
  without `.md`) differs from the exact title and the leading YAML omits
  `title`, insert `title: {display title}`. Leave an author-owned `title` key
  unchanged. Preserve other author YAML. This wrap is export-only; stored
  notes keep the title column.
- Title in the portable file is the filename, or `title:` when the filename
  cannot round-trip. Author H1s in the body are ordinary body content.
  Ordinary save keeps author headings, including a leading heading that
  matches the title. AI extract may drop a repeated title heading so the
  stored body is the note content.
- Public identity in the tree is the path. Doughnut note ID, UUID, and sync
  manifests stay server-side (ADR 0002).

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
- Missing listing `index.md` is conformant until Doughnut generates
  listings. Concept files named `index.md` / `log.md` warn; accept and lint
  still succeed.
- Recommendations (e.g. OKF `tags` shape) may warn; accept and lint still
  succeed.

## Consequences

- Export, lint, import, and Git accept share one codec contract.
- Title changes are usually filename changes; identity preservation across
  renames is ADR 0002 lineage.
- `title:` on the portable tree is codec wrap when the filename cannot
  round-trip; stored notes keep the title column.
- Obsidian and OKF consumers can open a Doughnut notebook tree. Public
  identity in the files is the path, except a user-insisted concept
  `index.md` / `log.md`, which OKF tools may treat as a listing/log or reject.

## Pros

- Portable, diffable, tool-friendly notebooks.
- Separates format (this ADR) from Git authority/transport (ADR 0002).

## Cons

- Filenames that cannot express the exact title need `title` frontmatter on
  the portable tree; the title column stays the stored source of truth
  (same persist-vs-wrap split as Readme `type`).
- Strict accept rules reject some free-form trees until fixed.

## Related

- Supersedes: (none)
- Superseded by: (none)
- Links:
  - [ADR 0002 — Git-native notebooks](./0002-git-native-notebooks-backed-by-mysql.md)
  - [Open Knowledge Format v0.2](https://github.com/GoogleCloudPlatform/knowledge-catalog/blob/main/okf/SPEC.md)
  - [Obsidian inline titles](https://obsidian.md/help/settings)
  - Tracker (code vs spec vs profile, not a second profile): [`.planning/research/OKF-COMPATIBILITY-GAP.md`](../../.planning/research/OKF-COMPATIBILITY-GAP.md)
