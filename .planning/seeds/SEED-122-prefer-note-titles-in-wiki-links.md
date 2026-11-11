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
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal.** A notebook reader can follow a shorthand wiki link to the note
whose title it uniquely matches, even when other notes share that name as
an alias.

**Scope.** Use title matches when present and alias matches otherwise. Apply
this rule to shorthand wiki links in note bodies and frontmatter, using the
link's notebook scope and the reader's access. Property links require the
selected note to contain the referenced property.
[ADR 0004 — OKF-compatible notebook Markdown profile](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md#links-and-attachments)

**Key examples.**

- In a notebook with a note titled `color` and other notes with the alias
  `color`, `[[color]]` opens the note titled `color`.
- If `colour` is the only note with the alias `color` and no note has that
  title, `[[color]]` opens `colour`.
- If two notes share the title `color`, or two notes share the alias `color`
  and no title matches, `[[color]]` is ambiguous.
- If neither a title nor an alias matches, `[[color]]` is unresolved.
