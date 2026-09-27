---
id: SEED-009
status: active
planted: 2026-09-04
planted_during: ADR 0002 v1 discussion
trigger_when: when selecting the next Git-backed notebook workflow story from the product backlog
scope: small
---

# SEED-009: Refine a Donut notebook locally with Obsidian and AI-enabled IDEs

## Why This Matters

Notebook owners move between local refinement in Obsidian or an AI IDE and
Web Donut without manual copying, losing work, or separating notes from their
learning history. Accepted history is forward and linear; the local side
rebases unpublished work before publishing
([ADR 0002](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)).
The stories below are the follow-ups the effort's closing review judged urgent.

## Story Decomposition

<a id="story-48"></a>

### Server and CLI classify notebook paths the same way

**Identity:** SEED-009#story-48
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/019-one-path-classifier/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"28c686327ed08c6644de8fffca990da0b9c1b954879687758308a84529d6bb50","plan":"d56b547d9527bfc93efe6d8224ed00317f88088640e03ffad02ff4586e4aa82e"}}
```

**Goal:** A notebook owner who edits a checkout with an AI IDE or Obsidian
gets one answer about what each path is (note, folder Readme, file,
empty-folder marker, or Git metadata), from the one owner on the server. A
leftover empty-folder marker or a `.md` extension in another letter case is
refused with a message naming the path and what to do, instead of a
misleading "refresh the checkout" conflict or a silent demotion to a file.
The CLI no longer keeps its own copy of the rules that can drift.

**Scope:**

- One Portable path classifier on the server that every tree walker uses in
  place of its own `.md`, `README.md` and `.keep` string checks.
- An empty-folder marker is an empty `.keep` alone in a folder below the
  notebook root. A proposal whose tree holds a `.keep` beside other content,
  or a non-empty one, is refused with a message naming it and asking to
  delete it. Accepted trees never hold such a `.keep`, so this judges only
  what the owner changed.
- A path added or changed by a publish whose extension is `.md` in another
  letter case (such as `Note.MD`, `README.MD`) is refused with a message
  asking to rename it to `.md`. Accepted content is never judged again: an
  unchanged file already accepted under such a name stays a file.
- A root `.keep` is an ordinary file, as today; no special case.
- The CLI stops classifying paths. It uploads to LFS exactly the changed files
  that are LFS pointers. A raw (non-pointer) file is refused only by the
  server, with its existing message, after the LFS upload (owner decision
  2026-09-27).

**Excluded:**

- Dot-files such as `.gitignore` or `.obsidian/` stay accepted as files; web
  upload refuses dot names. That is a separate product decision.
- Names that differ only by letter case within a folder on publish belong to
  one set of names per folder, not to path classification.
- A web picture upload named like Markdown (`x.md`) stays unchecked; it is
  unlikely and belongs to web placement.
- Validating proposed Markdown in one pass, one owner for frontmatter edits,
  and pull's final-newline conflict rule.

**Key examples:**

1. A web-created empty folder `Chemistry/` checks out as `Chemistry/.keep`.
   An AI IDE adds `Chemistry/Atoms.md` and leaves `.keep`. Publishing is
   refused: `Chemistry/.keep` must be deleted because the folder is no longer
   empty. (Today this ends in a 409 "refresh the checkout", confirmed by a
   probe on 2026-09-27.) After deleting it, the publish succeeds.
2. A checkout adds `Physics/Forces.MD`. Publishing is refused, asking to
   rename it to `Forces.md`. After renaming, it publishes as a note.
3. A checkout adds an empty root `.keep`. It publishes as a root file named
   `.keep`, and the CLI raises no error of its own.
4. A checkout adds a raw `diagram.png` without LFS. The CLI uploads the other
   LFS objects, and the server refuses with "Attachment "diagram.png" must be
   a Git LFS pointer or empty file."
5. Adding `Empty/.keep` alone still creates the empty folder `Empty`.
