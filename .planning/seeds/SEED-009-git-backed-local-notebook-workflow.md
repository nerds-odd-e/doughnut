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

<a id="story-47"></a>

### Moving a folder to another notebook respects the folder's one set of names

**Identity:** SEED-009#story-47
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/018-cross-notebook-folder-move-names/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"ee9b982d4ed7cd8b5274f0a33861c245e6d65c01d76059c03db9dab2b0f7d630","plan":"38ea62547b1137311f8a9302fe93b7efc1a135e98d959b35bd472f370b8db58e"}}
```

**Goal:** A notebook owner who moves a folder into another notebook gets the
same name protection as every other web placement, so the destination never
holds two entries whose names differ only by letter case, and a clash is
refused with a clear message instead of a server error.

This is the last web placement outside the one name owner; once it lands, the
North Star topic "One set of names per folder" can be retired. It stays ahead
of story-48 because it is small and closes that topic (owner, 2026-09-27).

**Scope:**

- Moving a folder to another notebook, with or without merge, is checked by
  the one name owner (`FolderSiblingNameValidation`) against the
  **destination** notebook before any row changes, giving the same outcomes
  and messages as a move within one notebook: a folder holding the name
  (ignoring case) is `FOLDER_NAME_CONFLICT` and offers merge; a note or file
  holding it is refused naming its path; a merge checks every entry at every
  level first.
- The move's exact-match, folder-only check (`mergeTargetOrRejectConflict`)
  is deleted.
- Git: each notebook's history records the move as an ordinary accepted
  change; nothing beyond normal Git behavior.
- Unchanged: moving a folder that holds files to another notebook stays
  refused (owner decision 2026-09-26); the move and merge UI and its error
  display.
- Deferred: the other users of the exact-match sibling check (folder
  creation's second check, the Git publish folder relocation), one "first
  free name" operation, and repair of any existing case-variant siblings
  (no production check requested).

**Key examples** (destination is another notebook):

1. Destination root holds folder `Shared` → move folder `shared` without
   merge → refused "A folder with this name already exists here."; nothing
   moves.
2. Destination root holds a file `shared` → move folder `shared` → refused
   naming `shared`; nothing moves.
3. Both `Shared` folders hold a note, `Intro` and `intro` → move with merge →
   refused naming `Shared/intro.md`, not a server error; nothing changes.
4. Moved `Shared/Deep` meets destination `Shared/deep` → move with merge →
   merged into `deep`, as today.
5. No clash → the move behaves as today.

<a id="story-48"></a>

### Server and CLI classify notebook paths the same way

**Identity:** SEED-009#story-48
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal:** A notebook owner publishing from a local checkout gets the same
answer from the CLI and the server about what each path is (note, folder
Readme, file, empty-folder marker, or Git metadata), so no publish fails or
misplaces content because the two sides disagree.

**Scope:** One Portable path classifier on the server that every tree walker
uses, settling a root `.keep` and `.md` letter case in that one place. The CLI
stops classifying paths and picks LFS uploads from the changed blobs that are
LFS pointers, leaving admission to the server.

