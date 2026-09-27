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
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal:** A notebook owner who moves a folder into another notebook gets the
same name protection as every other web placement, so the destination never
holds two entries whose names differ only by letter case, and a clash is
refused with a clear message instead of a server error.

**Scope:** Route the cross-notebook folder move, including its merge, through
the one name owner (`FolderSiblingNameValidation`) and its every-destination
check, as same-notebook moves already do. Retire the exact-match, folder-only
sibling check it still uses. Moving a folder that holds files to another
notebook stays refused.

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

