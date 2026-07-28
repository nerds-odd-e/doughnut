# `/sync --dry-run` — Preview remote changes before updating a workspace

From the refinement session on 2026-07-27.
Implements user story 2 of `.planning/notes/2026-07-24-portable-notebook-workspace.md`.

## Goal

A notebook owner cannot currently see what a pull would change. They should be
able to preview the differences before any local file is written.

## Scope

In scope:

- `/sync --dry-run <workspace path>` inside the notebook context of the
  interactive CLI.
- A unified diff of note content per changed note.

Out of scope:

- Uploading a local workspace to Doughnut.
- The pull itself. Pull is a separate item; this preview reads a workspace that
  already exists.
- A web UI. This is CLI only.

## Which notebook is compared

The preview runs inside the notebook context that `/use` establishes, and
compares the workspace against that notebook.

```
/use Ben Notebook
/sync --dry-run ./BenNotebook
```

`/use` already resolves the notebook, so an unknown name, an ambiguous name, and
an expired session are all reported before the preview is reached. This item
does not repeat that handling.

## Assumptions

The preview is specified against a workspace whose notes correspond to notes in
the notebook. Within this item, remote notes are neither created, deleted, nor
renamed — only their content changes, and every note has a matching file.
A file with no matching note, and a note with no matching file, belong to later
items.

## How the comparison works

Each run exports the notebook fresh into a scratch directory under the system
temporary directory, then compares the workspace against that export.

```
/tmp/doughnut-sync-<random>/     scratch export, this run only
  less.md
  LeSS in Action/
    team.md

BenNotebook/                     the workspace, never written to
  less.md
  LeSS in Action/
    team.md
```

The scratch directory is removed when the run ends, whether it succeeded or
failed. Nothing about the comparison is remembered between runs, so the preview
needs no synchronization state of its own and no credentials are ever written
into the workspace.

Because the comparison is between the workspace as it stands and the notebook as
it stands, a difference is reported whichever side it came from. A note edited
locally is reported the same way as a note edited in Doughnut: the preview says
what a pull would change, not who changed it.

## Domain language

| Term | Meaning |
|---|---|
| workspace | A local directory holding one notebook as Markdown |
| scratch export | A fresh export of the notebook, made and discarded per run |
| changed note | A note whose workspace content differs from the scratch export |
| preview | The report of what a pull would do; it writes nothing |
| hunk | One region of change within a note, with its surrounding context |

## Diff format

A changed note is reported as a unified diff of its content.

- `-` lines are the workspace as it stands, `+` lines are the notebook as it
  stands. Reading a diff top to bottom is reading what a pull would do.
- Context lines are prefixed with spaces.
- Each hunk carries up to 3 unchanged lines before and after the change, as
  `git diff` does. Fewer are shown where the note begins or ends.
- Changes closer together than their context share one hunk; changes further
  apart produce separate hunks.
- Note content is compared and printed as raw text. Markdown is not rendered,
  so a change to markup itself remains visible and the `-`/`+` columns stay
  aligned.

## Examples

### 1. Preview one changed note

The key example from the whiteboard.

Given the notebook "Ben Notebook" holds one note "less" with content "Hello",
and the workspace `./BenNotebook` holds the same content,
when "less" is changed in Doughnut to "Hello world!"
and I run `/sync --dry-run ./BenNotebook`,
then the preview shows one changed note with a diff:

```
less.md
  - Hello
  + Hello world!

1 note would change.
```

### 2. Preview 2 changes

Given the notebook holds "less" with "Hello" and "scrum" with "Sprint",
and the workspace holds the same content,
when "less" is changed to "Hello world!" and "scrum" is changed to "Sprint review"
and I run the preview,
then both notes are reported:

```
less.md
  - Hello
  + Hello world!

scrum.md
  - Sprint
  + Sprint review

2 notes would change.
```

### 3. An unchanged note is not reported

Given the notebook holds "less" with "Hello" and "scrum" with "Sprint",
and the workspace holds the same content,
when only "less" is changed to "Hello world!"
and I run the preview,
then only "less" is reported, and "scrum" does not appear.

### 4. Change a note in a folder

The preview reports the workspace path, so the folder is visible.

Given the notebook holds "intro" at the root and "team" in the folder
"LeSS in Action",
and the workspace holds the same content,
when "team" is changed to "Sprint review"
and I run the preview,
then the path includes the folder:

```
LeSS in Action/team.md
  - Sprint
  + Sprint review

1 note would change.
```

### 5. Multiple folders

Given "team" is in "LeSS in Action" and "tech" is in "Engineering",
and the workspace holds the same content,
when both are changed
and I run the preview,
then both are reported, ordered by path:

```
Engineering/tech.md
  - Trunk
  + Trunk based

LeSS in Action/team.md
  - Sprint
  + Sprint review

2 notes would change.
```

### 6. One line changed in a note of many lines

Only the changed line and its surrounding context are reported.

Given the note "team" holds:

```
Sprint planning
Daily standup
Two week sprint
Retrospective
Demo
```

and the workspace holds the same content,
when the third line is changed in Doughnut to "Three week sprint"
and I run the preview,
then the diff carries the surrounding context:

```
team.md
    Sprint planning
    Daily standup
  - Two week sprint
  + Three week sprint
    Retrospective
    Demo

1 note would change.
```

### 7. A line is added

Given the note "team" holds "Sprint planning" and "Retrospective",
and the workspace holds the same content,
when "Daily standup" is inserted between them in Doughnut
and I run the preview,
then the diff shows an addition with no matching removal:

```
team.md
    Sprint planning
  + Daily standup
    Retrospective

1 note would change.
```

### 8. A line is removed

Given the note "team" holds "Sprint planning", "Daily standup" and
"Retrospective",
and the workspace holds the same content,
when "Daily standup" is removed in Doughnut
and I run the preview,
then the diff shows a removal with no matching addition:

```
team.md
    Sprint planning
  - Daily standup
    Retrospective

1 note would change.
```

### 9. Two changes far apart in one note

Changes separated by more than their context are reported as separate hunks,
each headed by the line it starts at.

Given the note "team" holds:

```
Sprint planning
Two week sprint
Daily standup
Backlog refinement
Story mapping
Estimation
Definition of done
Working agreement
Retrospective
Demo
```

and the workspace holds the same content,
when line 2 becomes "Three week sprint" and line 9 becomes
"Retrospective and demo" in Doughnut
and I run the preview,
then the diff carries two hunks, because "Estimation" on line 6 lies beyond the
context of both changes:

```
team.md
  @@ line 1 @@
    Sprint planning
  - Two week sprint
  + Three week sprint
    Daily standup
    Backlog refinement
    Story mapping
  @@ line 7 @@
    Definition of done
    Working agreement
  - Retrospective
  + Retrospective and demo
    Demo

1 note would change.
```

### 10. A change at the first line

Context is shown only where it exists.

Given the note "team" holds "Sprint planning", "Daily standup" and
"Retrospective",
and the workspace holds the same content,
when the first line is changed in Doughnut to "Sprint planning meeting"
and I run the preview,
then no context precedes the change:

```
team.md
  - Sprint planning
  + Sprint planning meeting
    Daily standup
    Retrospective

1 note would change.
```

### 11. A change at the last line

Given the same note,
when the last line is changed in Doughnut to "Retrospective and demo"
and I run the preview,
then no context follows the change:

```
team.md
    Sprint planning
    Daily standup
  - Retrospective
  + Retrospective and demo

1 note would change.
```

### 12. A blank line is part of the content

Blank lines are compared and reported like any other line.

Given the note "team" holds "Sprint planning", a blank line, and
"Retrospective",
and the workspace holds the same content,
when "Daily standup" replaces the blank line in Doughnut
and I run the preview,
then the blank line is reported as removed:

```
team.md
    Sprint planning
  -
  + Daily standup
    Retrospective

1 note would change.
```

### 13. The content is emptied

Given the note "less" holds "Hello",
and the workspace holds the same content,
when its content is emptied in Doughnut
and I run the preview,
then the whole content is reported as removed:

```
less.md
  - Hello

1 note would change.
```

### 14. Markdown markup is compared as text

Given the note "less" holds "**Put** to sleep is _sedation_",
and the workspace holds the same content,
when the emphasis is changed in Doughnut to "**Put** to sleep is **sedation**"
and I run the preview,
then the markup appears as raw text, so the change is visible:

```
less.md
  - **Put** to sleep is _sedation_
  + **Put** to sleep is **sedation**

1 note would change.
```

### 15. A note edited locally is reported too

The comparison is between the two sides as they stand, so a local edit is a
difference like any other.

Given the notebook holds "less" with "Hello",
and the workspace holds the same content,
when I edit `less.md` in the workspace to "Hello from Obsidian"
and nothing is changed in Doughnut
and I run the preview,
then the local edit is reported as what a pull would overwrite:

```
less.md
  - Hello from Obsidian
  + Hello

1 note would change.
```

### 16. No difference to report

Given a workspace matching the notebook,
when neither side is changed
and I run the preview,
then it reports `No changes to pull.`

### 17. The workspace is not written to

Given a workspace matching the notebook,
when "less" is changed in Doughnut to "Hello world!"
and I run the preview,
then `less.md` in the workspace still holds "Hello".

### 18. The scratch export does not survive the run

Given a workspace matching the notebook and a note changed in Doughnut,
when I run the preview,
then the scratch directory it exported into no longer exists.

### 19. Running the preview twice reports the same difference

Because each run exports afresh and keeps nothing, a second run sees what the
first one saw.

Given a workspace matching the notebook,
when "less" is changed in Doughnut to "Hello world!"
and I run the preview twice,
then both runs report the same single changed note.

### 20. The workspace path does not exist

Given the notebook context is "Ben Notebook",
when I run `/sync --dry-run ./NoSuchWorkspace`
then it reports `No directory at ./NoSuchWorkspace.`
and no diff is shown.

### 21. The notebook was deleted while the context was open

`/use` resolves the notebook when the context opens. It can be deleted in
Doughnut afterwards, and the export then has nothing to read.

Given the notebook context is "Ben Notebook",
when "Ben Notebook" is deleted in Doughnut
and I run the preview,
then it reports `Ben Notebook no longer exists in Doughnut.`
and no diff is shown.

### 22. The session expired before the export

Given the notebook context is "Ben Notebook",
when the access token is no longer valid
and I run the preview,
then it reports that the session expired, as other commands do,
and no diff is shown.

### 23. A failed export leaves no scratch directory behind

Given the notebook context is "Ben Notebook",
when the export fails partway
and I run the preview,
then the failure is reported
and the scratch directory it had started writing into no longer exists.

## Deferred

**A file with no matching note, and a note with no matching file.** The
whiteboard listed "Exception: note doesn't exist", which reads either way: a
`.md` the user added in Obsidian, or a note whose local file was deleted. Both
are one-sided existence rather than a difference in content, and deciding what a
pull should do with them is the subject of user stories 7 and 10. This item
assumes both sides hold the same set of notes.

**Telling a local edit apart from a remote one.** Example 15 reports a locally
edited note the same way as a remotely edited one, because comparing the two
sides as they stand cannot distinguish them. Doing so needs a record of the last
synchronized state, which this design deliberately does not keep. It belongs
with push and conflict handling in user stories 5 and 6.
