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
concept ID, required `type` frontmatter). Doughnut filename = display name,
wiki-link policy, and what stays out of the tree are a producer profile.

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

- Filename is the display name: the concept basename without `.md` is the
  ordinary note title (Obsidian inline-title / OKF “derive title from
  filename”). The title column (max 150) is the product source of truth.
  Filename length follows the title column. Sibling uniqueness already
  implies unique files in a directory.
- Portable path is folder path + display name. Collision suffixes
  (`Recipe (2).md`) are not part of the profile.
- Author-owned `title` in YAML is preserved unchanged. The codec does not
  insert `title:` to compensate for a basename that is not the display
  name. Preserve other author YAML. Stored notes keep the title column.
- Author H1s in the body are ordinary body content. Ordinary save keeps
  author headings, including a leading heading that matches the title.
  AI extract may drop a repeated title heading so the stored body is the
  note content.
- Public identity in the tree is the path. Doughnut note ID, UUID, and sync
  manifests stay server-side (ADR 0002).

### Links and attachments

- Doughnut-authored inter-note links stay wiki `[[target]]` /
  `[[target|display]]`. Product insert stays wiki. Unqualified `[[Title]]` is
  unchanged (lowest note id when titles collide across folders).
  `Notebook:Title` is unchanged.
- These rules apply to the **body and to YAML frontmatter values** (scalars
  and one-level list items), including relationship `source` / `target` and
  `overlaps` items. Doughnut-authored frontmatter stays wiki. Path Markdown
  in those values is the same link. No conversion. A bare YAML path
  (`source: /folder/File.md`) is not a link. OKF §6.2 path-valued fields
  (`resource`, `sources[].resource`, …) are a different key family;
  Doughnut relationship endpoints are not those fields.
- Path Markdown `[display](/folder/File.md)` is the same link as
  `[[folder/File|display]]`. Leading `/` on Markdown hrefs is bundle-relative
  (notebook root). Wiki path form has no leading `/`.
- `.md` on a **path-shaped** target is optional and ignored (`/folder/File` =
  `/folder/File.md`; `[[folder/File.md]]` = `[[folder/File]]`). Do not strip
  `.md` from unqualified wiki titles (`[[File.md]]` can still mean a title).
- Identity is **folder path + display name**, not ZIP collision basenames
  (`Recipe (2).md`). Those suffixes are gone from the profile, not mapped.
- No active conversion of stored `[[…]]` ↔ `[…](…)`, including save/paste
  round-trip of path Markdown. ZIP export copies stored spelling. Wiki in
  Doughnut ZIPs is a profile exception to OKF path-link preference, not a
  rewrite job.
- Both spellings share one resolved-link cache `(note, target_note,
  link_text)`. No style column. No second cache.
- Git-managed images/binaries are out of scope here (ADR 0002 Level 2). Until
  then, stored `image:` is authored frontmatter (today a host-relative
  attachment path). Export copies it. That is not a codec wrap.

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
- Title changes are filename changes; identity preservation across
  renames is ADR 0002 lineage.
- Filename is the display name on the portable tree. Author-owned `title:`
  stays as authored YAML; the codec does not wrap `title:` to compensate
  for a basename that is not the display name. Stored notes keep the title
  column.
- Inter-note links are dual-spelling in body and frontmatter: Doughnut
  writes wiki; path Markdown stays as authored. ZIP does not rewrite wiki
  to path Markdown. Wiki in Doughnut-authored YAML is the same profile
  exception as wiki in the body.
- Obsidian and OKF consumers can open a Doughnut notebook tree. Public
  identity in the files is the path, except a user-insisted concept
  `index.md` / `log.md`, which OKF tools may treat as a listing/log or reject.
  Tools that do not resolve wiki links will not follow Doughnut-authored
  `[[…]]` until they support both spellings.

## Pros

- Portable, diffable, tool-friendly notebooks.
- Separates format (this ADR) from Git authority/transport (ADR 0002).

## Cons

- Unique sibling display names are the files; uniqueness is not encoded
  with collision suffixes. The title column stays the stored source of
  truth.
- Strict accept rules reject some free-form trees until fixed.
- Wiki in Doughnut-authored trees is a profile exception to OKF path-link
  preference.

## Related

- Supersedes: (none)
- Superseded by: (none)
- Links:
  - [ADR 0001 — Ubiquitous language](./0001-ubiquitous-language.md) (**Wiki link**)
  - [ADR 0002 — Git-native notebooks](./0002-git-native-notebooks-backed-by-mysql.md)
  - [Open Knowledge Format v0.2](https://github.com/GoogleCloudPlatform/knowledge-catalog/blob/main/okf/SPEC.md)
  - [Obsidian inline titles](https://obsidian.md/help/settings)
  - Tracker (code vs spec vs profile, not a second profile): [`.planning/research/OKF-COMPATIBILITY-GAP.md`](../../.planning/research/OKF-COMPATIBILITY-GAP.md)
