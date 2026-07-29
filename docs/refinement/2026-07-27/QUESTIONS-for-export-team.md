# Questions for the team building `/export`

**Historical record of the 2026-07-27 session.** `/sync --dry-run` shipped
unzipping the export in memory rather than into a scratch directory, so the
scratch-directory questions below were answered by dropping the idea. What the
command does now is specified by
`e2e_test/features/cli/cli_sync_dry_run.feature` and the CLI unit tests.

`/sync --dry-run` compares a workspace against the notebook as it stands. It
does that by exporting the notebook into a scratch directory on every run and
diffing the two directories, so its correctness rests on your export.

We have ordered these by how much they block us.

## Already agreed

- `/export ./a.path` is the user-facing command, and it writes to the directory
  it is given. We will point it at a scratch directory of our own.
- We create the scratch directory and delete it when the run ends, whether it
  succeeded or failed. The export does not need to manage it.

## Blocking — we cannot start without these

### 1. Please expose the export as a function we can call

`/export ./a.path` is the command a user types. We need the same work reachable
from code, so that `/sync --dry-run` can export into a scratch directory without
going through the interactive prompt — something along the lines of
`exportNotebook(notebook, targetDir)`, returning once the directory is complete
and throwing if it is not.

We are asking you to expose it rather than asking how to reach it: driving the
interactive command from inside another command, or spawning a child process,
would be painful for us and fragile in our end-to-end tests.

## Contract — these decide whether our diff is correct

### 2. Please send us a real sample of exported output

One notebook containing nested folders and a note with frontmatter, exactly as
your export writes it. A real sample answers questions 3 to 6 at once and is
worth more to us than a written description.

### 3. How does a note title become a filename?

Our examples assume `LeSS in Action/team.md`. We need your actual rule for
spaces, case, and characters that are not valid in a filename, such as `/`,
`:`, and emoji. We also need to know what happens when two notes in the same
folder share a title.

If your rule differs from the one that produced an existing workspace, every
note is reported as missing on one side and our diff is meaningless.

### 4. What does an exported file contain besides the note body?

The portable workspace notes say every exported note carries a stable Doughnut
identity in frontmatter. If frontmatter is written:

- Does any field change between two exports of an unchanged note, such as an
  export timestamp or a revision counter? If so, every note differs on every
  run and the preview is useless to us.
- Should we compare frontmatter at all, or strip it and compare only the body?

### 5. How is folder structure represented?

- Is nesting depth limited?
- Is an `index.md` written for a notebook or a folder? Should it appear in our
  diff?
- Are empty folders created?

### 6. Line endings, trailing newline, and encoding

LF or CRLF, whether files end with a newline, and whether a BOM is written. A
mismatch in any of these makes whole files differ.

## Timing and change

### 7. When will the export be usable?

We work trunk-based and every commit has to satisfy our Definition of Done, so
we cannot merge work that depends on something unavailable. Knowing your date
tells us whether to wait or to put a seam in front of your interface.

### 8. Is the interface still moving?

If you expect it to change, we would rather wrap it now than chase it later.

### 9. Same repository?

Doughnut is a mono-repo. Are you building inside it, so we can import you
directly?

## What we need from your failure behaviour

### 10. What happens when an export fails partway?

Our specification requires that a failed preview leaves no scratch directory
behind. We need failures to be reported in a way we can catch, and we need to
know whether partial output can be left on disk.

## Something you should know about how we intend to use it

### 11. We will run the export on every preview

You may be designing for a user who exports once. We would call it every time
someone previews, which makes two things matter more than they otherwise would:

- **Speed.** How long does a notebook of, say, 500 notes take? If it is tens of
  seconds, we will have to change our design.
- **Determinism.** Exporting an unchanged notebook twice must produce byte
  identical output. This overlaps with your own goal that a repeated pull
  produces no filesystem changes.

If either is a problem for you, tell us early — it is our design that would have
to move, not yours.
