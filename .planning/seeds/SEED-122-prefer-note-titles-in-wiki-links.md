---
id: SEED-122
status: active
planted: 2026-11-11
scope: story
---

# Prefer note titles in wiki links

<a id="prefer-note-titles-in-wiki-links"></a>

### Prefer note titles in wiki links

**Identity:** SEED-122#prefer-note-titles-in-wiki-links
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/001-prefer-note-titles-in-wiki-links/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"bd90cbf5584c16e637e14dc9c1ec931d2ac14c81a43d79699696ad45fa40fb20","plan":"3a6d2e48afd81e51f5cb2f9c08abe338a0e663d107ad1c00748a03460394c424"}}
```

**Goal.** A notebook reader can follow a shorthand wiki link to the note
whose title it uniquely matches, even when other notes share that name as
an alias, so aliases do not obstruct a direct reference by title.

**Scope.** Within the link's notebook scope, consider the reader's readable,
non-trashed notes. If any titles match, resolve among those notes; otherwise
resolve among recognized alias matches. One matching note resolves, multiple
matching notes are ambiguous, and none is unresolved. Keep the existing title
comparison and alias normalization rules.

Apply the same selection to body links and supported YAML frontmatter values,
including scalar and one-level list values, notebook-qualified shorthand,
display labels, and property selectors. Select the note before checking its
property: a property link resolves only if the selected note contains the exact
referenced property key. Ambiguity does not select an arbitrary note; access
filtering protects private content; property validation prevents a link from
claiming a destination that lacks its target. These restrictions follow
[ADR 0004 — OKF-compatible notebook Markdown profile](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md#links-and-attachments).

Resolution uses current notebook state and viewer access, preserving the authored
destination and label. This increment adds no matching or ranking policy for
search or spelling recall, new disambiguation interaction, or new path syntax.

**Key examples.**

- In a notebook with a note titled `color` and other notes with the alias
  `color`, `[[color]]` opens the note titled `color`. The same selection applies
  to `[[color|Read this]]`, a YAML value containing that link, and
  `[[Notebook:color]]` in the named notebook scope.
- If `colour` is the only note with the alias `color` and no note has that
  title, `[[color]]` opens `colour`.
- If two notes share the title `color`, or two notes share the alias `color`
  and no title matches, `[[color]]` is ambiguous.
- An unreadable or trashed note titled `color` does not prevent the reader's
  unique available alias match from resolving.
- If neither a title nor an alias matches, `[[color]]` is unresolved.
- With a unique title `color` and an alias match on `colour`,
  `[[color#prop:topic]]` opens the `topic` property on `color` when present.
  If only `colour` has `topic`, the link is unresolved. Two title matches
  remain ambiguous even if only one contains `topic`.
- If `[[color]]` initially resolves through an alias and a note titled `color`
  is then added, reopening the source resolves to the title without resaving
  the source. Its inbound-reference view follows the current destination.

**Architecture.** Keep one backend selection rule behind the existing wiki-link
resolver, shared by live reference consumers and shortest-path authoring. A
unique title can therefore be authored as shorthand despite alias collisions.
Keep note selection ahead of property validation and retain authored references
as the source of truth; a stored reference index must not pin an alias destination.
Spelling-recall accidental matches have a different purpose and retain their
existing all-match behavior. This follows ADR 0004 above and
[ADR 0005 — Web routes](../../docs/adrs/0005-web-routes-accepted.md#notebook-links-as-web-destinations):
resolved note and property IDs feed the existing named routes. No API, schema,
route, or ADR change is needed.
