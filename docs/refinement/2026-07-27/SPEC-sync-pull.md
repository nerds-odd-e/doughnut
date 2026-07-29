# `/sync <workspace>` — Pull remote note changes into an existing workspace

From the refinement session on 2026-07-27 (whiteboard IMG_9541).
Implements user story 3 of `.planning/notes/2026-07-24-portable-notebook-workspace.md` for the
narrow slice: **update only files that already exist locally and have a matching exported note**.

## Goal

A notebook owner who already has a local Markdown workspace wants remote edits in Doughnut
written into matching local files quickly, without creating new files or touching local-only files.

## Scope

In scope:

- `/sync <workspace path>` inside the notebook context established by `/use`.
- `/sync --dry-run <workspace path>` remains preview-only (see `SPEC-sync-dry-run.md`).
- For each `.md` file already in the workspace, if the notebook export contains the same path and
  the content differs, replace the local file with the exported content (including frontmatter).
- Unchanged intersecting files are not written (content and modification time preserved).
- A summary line in the assistant transcript (`No changes to pull.` or `N note(s) updated.`).

Out of scope:

- Creating local files for notes that exist only in Doughnut.
- Deleting or renaming local files based on remote changes.
- Uploading local edits to Doughnut.
- Sync metadata or incremental export APIs (may be added later if full export is too slow).
- Overwriting local-only files that have no path in the export.

## Which notebook is pulled

Same as dry-run: the active notebook from `/use`.

```
/use Ben Notebook
/sync ./BenNotebook
```

## How pull works

Each run exports the notebook fresh (zip from the backend), unzips it in memory, reads the
workspace from disk, and updates **only paths present in both** the workspace and the export.

```
BenNotebook/                 workspace on disk (read + selective write)
  less.md
  Less 2.md                  local-only; never read from export, never deleted

export zip (this run)        scratch in memory only
  less.md                    updated content when remote note changed
  scrum.md                   ignored if scrum.md is not in the workspace
```

Pull does not keep state between runs.

## Difference from `--dry-run`

| | `--dry-run` | `/sync` |
|---|-------------|---------|
| Writes workspace | Never | Updates intersecting changed files |
| Local-only file edited | Reported as a diff | **Not** overwritten |
| Remote-only note | Appears in diff if export has extra paths | **No** local file created |
| Local path with no export entry | Reported if content differs | **Not** changed |

## Assistant messages

- No intersecting file needs a write: `No changes to pull.`
- One file updated: `1 note updated.`
- N files updated: `N notes updated.`
- Missing workspace directory: `No directory at <path>.` (same as dry-run)

## Examples

### 1. Pull one changed note

Given the notebook "Ben Notebook" holds "less" with content "Hello",
and the workspace `./BenNotebook` holds the same export for "less",
when "less" is changed in Doughnut to "Hello world!"
and I run `/sync ./BenNotebook`,
then `less.md` in the workspace contains "Hello world!"
and the assistant reports `1 note updated.`

### 2. Extra local file is untouched

Given the workspace holds `less.md` matching the notebook and an extra `Less 2.md`,
when "less" is changed in Doughnut to "Hello world!"
and I run `/sync ./BenNotebook`,
then `less.md` is updated and `Less 2.md` is unchanged.

### 3. Remote note with no local file

Given the notebook holds "less" and "scrum",
and the workspace holds only `less.md` matching the notebook,
when "scrum" is changed in Doughnut
and I run `/sync ./BenNotebook`,
then `scrum.md` is not created and `less.md` is unchanged if the remote "less" did not change.

### 4. Already in sync

Given the workspace matches the notebook export,
when I run `/sync ./BenNotebook`,
then the assistant reports `No changes to pull.`
and no file contents or modification times change.

### 5. Performance

Given a notebook with 1000 notes and a workspace that contains all 1000 exported files,
when exactly one note is changed in Doughnut
and I run `/sync ./BenNotebook`,
then the command completes in under 5 seconds.

## Deferred

Rename, move, delete reconciliation, push, and conflict detection — other user stories in
`2026-07-24-portable-notebook-workspace.md`.
