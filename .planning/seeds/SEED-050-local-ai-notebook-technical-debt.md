---
id: SEED-050
status: dormant
planted: 2026-09-27
planted_during: owner filtering of the local AI notebook closing review's not-now findings
trigger_when: selected from the product backlog
scope: large
---

# SEED-050: Pay down the local AI notebook effort's technical debt

## Why This Matters

The closing review of the local AI notebook effort (local AI IDEs, images and
other files in notebooks) left findings that were not urgent on their own.
The owner kept those that grow into technical debt if left: rules written in
more than one place, which each new feature copies again, and failures that
pass silently.

All stories were re-evaluated against the code at `d9abdcc2eb` on 2026-09-27
and cut to what still holds and pays off (owner decisions the same day). Each
must leave the design smaller or more cohesive with no performance loss.
Claims the review got wrong were dropped rather than planned: `fk_note_folder`
is already `ON DELETE RESTRICT` (since `V300000329`); local publish does not
apply a document twice; the "five first-free-name versions" are one algorithm
with small legitimate predicates; a Book's source file is always a plain
filename at the notebook root; and the web editor and the server make
different frontmatter edits, each already with one owner.

## Story Decomposition

<a id="story-9"></a>

### Folder dissolve and merge enter subfolders by the one entry rule

**Identity:** SEED-050#story-9
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/027-dissolve-enters-folders-by-the-one-rule/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"05b6de91db7a458c562ad487a19f5326c8e6054dd6a33a9d9246e84e7bb749cd","plan":"876b08ab2e545d9cafab89deb82366c270763849229aa48bdfd47033c3436009"}}
```

**Goal:** Correction of SEED-050#story-8 (provenance
`da1e269ba5:.planning/slice-plans/026-one-folder-entry-rule/PLAN.md`, commits
649e162dd4 and cba24a706e). Developers who next change folder naming find
"enter the folder holding this name (ignoring case), else refuse a note or
file holding it" only in `FolderSiblingNameValidation.folderToEnter`;
the dissolve/merge placement check no longer carries its own copy.

**Scope:**

- Required: `FolderContentsPlacementCheck.firstFolderMeetingAFolder` gets each
  subfolder's existing destination folder from `folderToEnter`; net fewer lines.
- Preserved: dissolve and merge-move refusals naming the note or file path,
  case-variant folder merging, and every message and error type.
- Excluded: the folder-only `requireNoConflictingSibling` family; the trash
  path's `findOrCreateFolder`.

**Plan:** [027-dissolve-enters-folders-by-the-one-rule](../slice-plans/027-dissolve-enters-folders-by-the-one-rule/PLAN.md)

**Effort hypothesis:** S — high confidence.

**Depends on:** none (SEED-050#story-8 is delivered).
