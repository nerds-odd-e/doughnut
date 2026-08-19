# 0004 — OKF-compatible notebook Markdown profile

**Status:** Accepted  
**Date:** 2026-08-05  
**Decision makers:** Terry Yin  
**Consulted:** None 

## Context

Doughnut exports and syncs notebooks as Markdown trees. Portable knowledge
should follow the
[Open Knowledge Format (OKF) v0.2](https://github.com/GoogleCloudPlatform/knowledge-catalog/blob/main/okf/SPEC.md)
so local copies work with OKF tooling, Obsidian-style editing, and (with
[ADR 0002](./0002-git-native-notebooks-backed-by-mysql.md)) Git-native sync.

OKF standardizes a small structural floor (concepts as files, path as ID,
required `type`). Filename-as-title, wiki-link spelling, and what Doughnut
keeps out of the tree are Doughnut producer choices on top of that floor —
the profile this ADR records.

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
  canonicalize `readme` → `Readme`; any other non-empty `type` is left
  as authored. Preserve author YAML. `type: Readme` is export-only;
  stored readme columns are the authored text.
- `readme` / `readme.md` are the only hard-reserved note titles.
- Doughnut does not generate OKF `index.md` listings; omitting them is
  conformant. Doughnut does not emit `okf_version`; OKF allows that
  declaration only on a bundle-root listing.
- Concept titles that would become `index.md` or `log.md` (`index`,
  `index.md`, `log`, `log.md`, case-insensitive) are allowed. The product
  warns on create/rename (PathNameEditor, notebook health); save succeeds.
  Filename-as-title applies: an insisted title is exported as that
  basename. That tree is a profile exception to OKF §3.1 / §11.
- Empty folders exist in the tree only via tracked content (typically
  `README.md` when a readme is present).

### Titles, filenames, body

- Filename is the display name: the concept basename without `.md` is the
  ordinary note title (Obsidian inline-title / OKF “derive title from
  filename”). The title column (max 150) is the product source of truth.
  Filename length follows the title column. Sibling uniqueness implies
  unique files in a directory.
- Author-owned `title` in YAML is preserved as authored. The codec does
  not insert `title:` to compensate for a basename that is not the
  display name. Preserve other author YAML. Stored notes use the title
  column.
- Author H1s in the body are ordinary body content. Ordinary save
  preserves author headings, including a leading heading that matches
  the title.
- Public identity in the tree is the path,  **folder path + display name**.
  Doughnut note ID is server-side.

### Links and attachments

- Doughnut-authored inter-note links are wiki `[[target]]` /
  `[[target|display]]`. Product insert writes wiki. Unqualified `[[Title]]`
  resolves by title (lowest note id when titles collide across folders).
  `Notebook:Title` is a valid wiki target.
- These rules apply to the **body and to YAML frontmatter values** (scalars
  and one-level list items), including relationship `source` / `target` and
  `overlaps` items. Doughnut-authored frontmatter is wiki. Path Markdown
  in those values is the same link. No conversion. A bare YAML path
  (`source: /folder/File.md`) is not a link. OKF §6.2 path-valued fields
  (`resource`, `sources[].resource`, …) are a different key family;
  Doughnut relationship endpoints are not those fields.
- Path Markdown `[display](/folder/File.md)` is the same link as
  `[[folder/File|display]]`. Leading `/` on Markdown hrefs is bundle-relative
  (notebook root). Wiki path form has no leading `/`.
- `.md` on a **path-shaped** target is optional and ignored (`/folder/File` =
  `/folder/File.md`; `[[folder/File.md]]` = `[[folder/File]]`). Do not strip
  `.md` from unqualified wiki titles (`[[File.md]]` may be a title).
- No active conversion of stored `[[…]]` ↔ `[…](…)`, including save/paste
  round-trip of path Markdown. ZIP export copies stored spelling. 
- Both spellings share one resolved-link cache `(note, target_note,
  link_text)`. No style column. No second cache.

### Validation

- Durable writes (CLI push, web commit — ADR 0002 acceptance) and lint
  reject trees that break OKF requirements or this profile
  (missing/invalid `type`, reserved-name misuse of `readme` /
  `readme.md`, unsafe paths, etc.).
- Missing listing `index.md` is conformant. Concept files named
  `index.md` / `log.md` warn; durable write and lint succeed.
- Recommendations (e.g. OKF `tags` shape) may warn; durable write and
  lint succeed.

## Consequences

- Export, lint, import, and durable write share one codec contract.
- Title changes are filename changes; identity preservation across
  renames is ADR 0002 lineage.
- Filename is the display name on the portable tree. Author-owned `title:`
  is authored YAML; the codec does not wrap `title:` to compensate for a
  basename that is not the display name. Stored notes use the title
  column.
- Inter-note links are dual-spelling in body and frontmatter: Doughnut
  writes wiki; path Markdown is the authored spelling. ZIP does not rewrite wiki
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

- Strict validation rejects some free-form trees until fixed.
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
