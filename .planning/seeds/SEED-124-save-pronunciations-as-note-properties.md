---
id: SEED-124
status: active
planted: 2026-11-11
scope: story
---

# Save pronunciations as note properties

<a id="save-pronunciations-as-note-properties"></a>

### Save pronunciations as note properties

**Identity:** SEED-124#save-pronunciations-as-note-properties
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal.** A learner can record how a note's title is pronounced in a named
property and keep using Spelling review without seeing the reading in the
prompt. Existing production notes gain the same representation while keeping
their learning history.

**Scope.** Offer `pronunciation` as an optional choice in **Add property**.
Selecting it adds an ordinary YAML note property. Keep it unselected by
default. When Spelling review asks for the note title, use the property to
conceal its reading, including matching occurrences in the prompt body.
The expected answer is still the note title.

**Production migration.** Run a one-time conversion of existing title
readings marked with `/…/` in production, using 日本語学習 as the reference
notebook. Preview the proposed changes, review ambiguous readings and conflicts
with existing properties, and preserve surrounding prose and readings that
belong to other words. Publish the edits in place through normal notebook Git
publication and verify the converted notes and their learning history in
production. Prefer pure SQL if it can safely prepare the changes for that
publication path. Use temporary Java tooling if needed. Once production is
verified, delete the migration code and everything used only by it, including
tests, fixtures, helpers, and configuration, from subsequent repository
revisions. Keep the tests that prove the ongoing property and review behavior.

**Key examples.**

- A learner opens **Add property** and can choose `pronunciation` to save
  `ただい` on the note **多大**.
- In Spelling review for **多大**, the prompt conceals `ただい` when that reading
  appears in the note body. The learner still recalls **多大**.
- In production, `「多大　/ただい/」とは…` becomes `「多大」とは…` with
  `pronunciation: ただい`. The inline `甚大（じんだい）` remains with its
  explanation, and the existing note retains its learning history.

**Architecture.** Use the existing YAML frontmatter and accepted notebook
publication boundary. See [ADR 0004 — OKF-compatible notebook Markdown
profile](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
and [ADR 0002 — Git-native Portable notebook tree
synchronization](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md).
