# 0004 — OKF-compatible notebook Markdown profile

**Status:** Accepted  
**Date:** 2026-09-01
**Decision makers:** Terry Yin  
**Consulted:** None 

## Context

Donut exports and syncs notebooks as a **Portable notebook tree**. That tree
should follow the
[Open Knowledge Format (OKF) v0.2](https://github.com/GoogleCloudPlatform/knowledge-catalog/blob/main/okf/SPEC.md)
so local copies work with OKF tooling, Obsidian-style editing, and future
two-way synchronization. Synchronization, identity, and merge behavior are
outside this format decision; Proposed
[ADR 0002](./0002-revisioned-portable-notebook-synchronization.md)
addresses them.

OKF standardizes a small structural floor (concepts as files, path as ID,
required `type`). Filename-as-title, wiki-link spelling, and what Donut
keeps out of the tree are Donut producer choices on top of that floor —
the profile this ADR records.

## Decision

Donut’s **Portable notebook tree** conforms to OKF v0.2 plus this
profile. Codec round-trips must be lossless for these rules.

### Bundle and concepts

- One notebook ↔ one OKF bundle (directory of `.md` files).
- OKF calls a concept's bundle path without `.md` its **Concept ID**. For a
  Donut note, this is the normalized note portion of its **Portable path**.
- Stored note markdown carries `type` and valid YAML frontmatter.
  Ordinary notes use `type: Note`. Relationship notes are concepts in
  the bundle (`type: Relationship`). OKF unknown types are allowed.
  Preserve author-owned and unknown frontmatter keys on persist and
  round-trip.
- Container **Readme** is a notebook/folder column, not a note row.
  Non-blank readme maps to that directory’s `README.md`, a bundle concept
  with `type: Readme`. Blank readme omits the file.
- When exporting a **Portable notebook tree**, the codec inserts
  `type: Readme` if missing; canonicalizes `readme` → `Readme`; and leaves any
  other non-empty `type` as authored. Preserve author YAML. `type: Readme` is
  export-only; stored readme columns are the authored text.
- `readme` / `readme.md` are the only hard-reserved note titles.
- Donut does not generate OKF `index.md` listings; omitting them is
  conformant. Donut does not emit `okf_version`; OKF allows that
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
- A note's normalized **Portable path** is its **folder path + display name**.
  It addresses the note within that Portable notebook tree revision and
  changes when the note is renamed or moved. Donut note ID is server-side.

### Links and attachments

- Portable inter-note links use wiki `[[portable-path]]` /
  `[[portable-path|display-text]]`. Product insert writes wiki. Unqualified
  `[[Title-or-Alias]]` is a shorthand Portable path whose resolution scope is
  the source notebook's Portable notebook tree. `Notebook:Title-or-Alias`
  qualifies that scope with another notebook. Candidate matching includes note
  display names and recognized aliases. A shorthand Portable path resolves only
  when it identifies one destination under that scope. With no match it is
  unresolved; with multiple matches it is ambiguous and therefore unresolved,
  and Donut asks for a longer path. The authored destination remains unchanged.
  Donut-authored wiki bundle-root path form has no leading `/`, except the
  exact-root fallback `/Title` when that display name is an ambiguous
  shorthand. The reader accepts a leading `/` on a path-shaped wiki
  destination (`[[/Title]]`) as the same bundle-root spelling.
- Wiki-link rules apply to the **body and to YAML frontmatter values** (scalars
  and one-level list items), including relationship `source` / `target` and
  `overlaps` items. Donut-authored frontmatter uses wiki links. A bare YAML path
  (`source: /folder/File.md`) is not a link. OKF §6.2 path-valued fields
  (`resource`, `sources[].resource`, …) are a different key family; Donut
  relationship endpoints are not those fields.
- `.md` on a path-shaped **Portable path** is optional and ignored
  (`[[folder/File.md]]` = `[[folder/File]]`). Do not strip `.md` from
  unqualified wiki titles
  (`[[File.md]]` may be a title).
- A **Portable path** may select a **property**: note path plus the reserved
  `#prop:` separator and one non-empty encoded property-key component.
  The component is the exact authored YAML key encoded from UTF-8 bytes:
  RFC 3986 unreserved characters (`A-Z a-z 0-9 - . _ ~`) stay literal;
  every other byte is `%HH`. Product output uses uppercase hex; readers
  accept either hex case. Decode exactly once; an invalid escape or invalid
  UTF-8 makes the property target unresolved. Compare the resulting key
  case-sensitively. Example: `[[Moon#prop:a%20part%20of]]`. Product insert
  writes wiki. A literal `#prop:` is reserved for this property separator, not
  a heading id; other fragments are not property links. Bare YAML paths (with
  or without a fragment) are not links. A note title containing the literal
  substring `#prop:` cannot be the sole unqualified destination of a wiki link;
  the parser splits on the first marker. Accepted trade-off: title authors
  avoid `#prop:` in titles; this profile does not add escaping for it.
- Markdown links `[display-text](href)` use ordinary URL semantics. ADR 0005
  defines when a Donut URL also contributes a semantic note reference.
- Wiki Portable-path references and semantic Donut note-URL references defined
  by ADR 0005 share one code domain type, `AuthoredNoteReference`. Stored note
  Markdown is authoritative. One source-owned derived index mirrors every
  distinct authored reference, including wiki references that are currently
  unresolved or ambiguous; a resolved destination is not a second source of
  truth.
- Resolve an `AuthoredNoteReference` from its authored target, source scope,
  current notebook state, and current viewer. A semantic Donut note URL carries
  its authoritative destination note ID. A wiki Portable path does not persist
  an authoritative destination. Its optional property selector is part of the
  authored reference and resolves only while the current destination contains
  that exact property key.

### Validation

- Durable tree imports/synchronization writes and lint reject trees that break
  OKF requirements or this profile
  (missing/invalid `type`, reserved-name misuse of `readme` /
  `readme.md`, unsafe paths, etc.).
- Missing listing `index.md` is conformant. Concept files named
  `index.md` / `log.md` warn; durable write and lint succeed.
- Recommendations (e.g. OKF `tags` shape) may warn; durable write and
  lint succeed.

## Consequences

- Export, lint, import, and durable write share one codec contract.
- Title changes are filename changes; identity preservation across
  renames is outside this format decision.
- Filename is the display name on the **Portable notebook tree**. Author-owned
  `title:` is authored YAML; the codec does not wrap `title:` to compensate for
  a basename that is not the display name. Stored notes use the title column.
- Portable inter-note and property links use wiki syntax. Markdown URL handling
  follows ADR 0005. Wiki in Donut-authored YAML is the same profile exception
  as wiki in the body.
- Obsidian and OKF consumers can open a Donut Portable notebook tree. A note is
  addressed in the files by its normalized Portable path. A user-insisted
  concept `index.md` / `log.md` still has that path, but OKF tools may treat it
  as a listing/log or reject it. Tools that do not resolve wiki links will not
  follow Donut-authored `[[…]]`.

## Pros

- Portable, diffable, tool-friendly notebooks.
- Separates format (this ADR) from synchronization, identity, and merge
  behavior.

## Cons

- Strict validation rejects some free-form trees until fixed.
- Wiki in Donut-authored trees is a profile exception to OKF path-link
  preference.

## Related

- Links:
  - [ADR 0001 — Ubiquitous language](./0001-ubiquitous-language.md)
    (**Portable notebook tree**, **Portable path**, **Wiki link**, **Property**)
  - [ADR 0002 — Portable notebook synchronization](./0002-revisioned-portable-notebook-synchronization.md)
  - [ADR 0005 — Web routes](./0005-web-routes-accepted.md) (compile to `noteShow` / `noteProperty`)
  - [Open Knowledge Format v0.2](https://github.com/GoogleCloudPlatform/knowledge-catalog/blob/main/okf/SPEC.md)
  - [Obsidian inline titles](https://obsidian.md/help/settings)
