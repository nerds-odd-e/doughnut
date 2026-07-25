---
date: "2026-07-24 18:39"
promoted: false
---

# Portable Markdown Workspace and GitHub Publishing

## User stories

### 1. Pull a notebook into a usable Markdown workspace

As a notebook owner, I want to pull a Doughnut notebook into a chosen local directory so that I can read and use my knowledge with ordinary Markdown tools, including Obsidian.

Acceptance examples:

- Notes and folders reproduce the notebook hierarchy with deterministic paths.
- Every exported note has stable Doughnut identity in frontmatter.
- Notebook and folder indexes use `index.md` where needed.
- Internal references are emitted as usable ordinary Markdown links.
- Attachments remain remote but their references remain usable.
- A failed pull reports what happened and does not present a partial workspace as successfully synchronized.

### 2. Preview a pull without changing files

As a notebook owner, I want to preview what a pull would create, update, leave unchanged, or reject so that I can detect naming and mapping problems before writing to my workspace.

Acceptance examples:

- The preview reports exact target paths and actions.
- Reserved filenames, duplicate paths, and invalid mappings are reported clearly.
- Running the preview does not mutate Doughnut, the workspace, or sync metadata.

### 3. Pull only remote changes

As a notebook owner, I want later pulls to update only notes changed in Doughnut so that synchronization is fast and does not disturb untouched local files.

Acceptance examples:

- Unchanged files retain their content and modification time.
- New, changed, renamed, and moved remote notes produce the expected local changes.
- Running pull twice with no intervening changes produces no filesystem changes.
- Sync metadata is updated only after a successful operation.

### 4. Check whether a workspace follows the portable knowledge contract

As a workspace owner, I want a compatibility check so that I know whether the workspace can be consumed safely by OKF-oriented, Obsidian, and ordinary Markdown tools.

Acceptance examples:

- The check identifies malformed frontmatter, duplicate identities, broken local links, missing indexes, and unsupported path mappings.
- Unknown frontmatter properties are accepted and preserved.
- Findings name the affected file and provide an actionable explanation.
- A valid workspace produces a clear successful result.

### 5. Record a successful pull in local Git history

As a workspace owner, I want a successful synchronization recorded in Git so that I can inspect and recover earlier versions of my notebook.

Acceptance examples:

- Doughnut can initialize or use the Git repository containing the workspace.
- A successful pull with changes creates one understandable commit.
- A no-change pull creates no commit.
- Credentials, transient files, and private sync state are not added to the commit.
- If Git commit fails, the workspace remains inspectable and the sync is not falsely reported as fully successful.

### 6. Publish a snapshot to an existing GitHub repository

As a workspace owner, I want to publish synchronized commits to an existing GitHub repository so that I can back up or share the notebook using familiar access controls.

Acceptance examples:

- The user selects or configures an existing GitHub Git remote.
- Publishing uses the user's existing Git credential helper or SSH configuration.
- A successful publish reports the remote and branch.
- Authentication rejection, non-fast-forward rejection, and network failure leave local commits intact and produce actionable messages.
- Doughnut does not create repositories, store access tokens, force-push, or merge divergent branches.

### 7. Preview local edits and conflicts before pushing

As a notebook owner, I want to preview local changes and conflicts before updating Doughnut so that I can understand the consequences of a push.

Acceptance examples:

- The preview distinguishes unchanged, locally changed, remotely changed, and divergent notes.
- It reports exact create and update actions.
- Divergent edits are conflicts, not last-write-wins updates.
- The preview does not mutate Doughnut, local files, or sync metadata.

### 8. Push edits to existing notes safely

As a notebook owner, I want to push local content and metadata edits to their corresponding Doughnut notes so that work done in Obsidian or another editor is available in Doughnut.

Acceptance examples:

- The body and supported frontmatter fields of an identified note can be updated.
- The update succeeds only when the Doughnut note still matches the version last synchronized.
- A concurrent remote edit produces a conflict and neither version is silently overwritten.
- A successful push refreshes the local representation and sync metadata.
- Repeating the push without further changes has no effect.

### 9. Create a Doughnut note from a local Markdown file

As a workspace user, I want a new local Markdown file to become a Doughnut note so that capture can begin in whichever tool I am using.

Acceptance examples:

- A valid new file creates one note in the intended notebook location.
- Doughnut's stable identity is written back to the file only after creation succeeds.
- Retrying after a partial failure does not create a duplicate note.
- Invalid or ambiguous target locations are rejected with an actionable explanation.

### 10. Rename a note through its local filename

As a workspace user, I want an intentional local filename change to rename the corresponding Doughnut note so that names remain consistent across tools.

Acceptance examples:

- A rename preserves stable identity and updates the Doughnut title.
- References managed by the workspace remain valid after the rename.
- Case-only renames and path collisions behave predictably on supported filesystems.
- A concurrent remote rename is reported as a conflict.

### 11. Move a note between folders

As a workspace user, I want moving a Markdown file between folders to move the corresponding Doughnut note so that both tools show the same organization.

Acceptance examples:

- A local move changes the note's parent while preserving stable identity.
- Moving a subtree has explicit, deterministic behavior.
- Cycles, invalid parents, and concurrent remote moves are rejected safely.
- Related links remain valid or are updated according to the workspace contract.

### 12. Reconcile deletions safely

As a notebook owner, I want to review and confirm deletions detected on either side so that obsolete notes can be removed without accidental data loss.

Acceptance examples:

- Local and remote deletions are distinguished from rename, move, and temporary absence.
- The preview identifies exactly what will be deleted and where.
- Deletion requires explicit confirmation or an explicit configured policy.
- The operation is recoverable through Doughnut's trash or Git history.
- A conflict prevents automatic deletion.
