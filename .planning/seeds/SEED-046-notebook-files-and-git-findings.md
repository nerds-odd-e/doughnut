---
id: SEED-046
status: dormant
planted: 2026-09-26
planted_during: owner review of the 2026-09-26 manual test of notebook files and Git integration
trigger_when: now; the owner asked for every finding to be queued
scope: large
---

# SEED-046: Notebook files and Git work stay fast and understandable

## Why This Matters

Owners who keep notebooks with files and work on them from local checkouts,
often with AI IDEs, alongside Web Donut are the beneficiaries. A two-hour
manual test on 2026-09-26 found that the delivered file and Git behavior is
correct and safe in most places, but publishing slows down with history once a
notebook holds files, pull forces a fresh clone after unrelated file changes,
CLI and upload messages are hard to act on, and web edits rewrite formatting.

The owner decided on 2026-09-26 that every finding deserves attention, that
the publish performance gap must be fixed, and that the clone success message
must become much shorter and better formatted.

Measurements come from the local Development stack (`pnpm dev`), whose file
bytes live in backend memory rather than GCS, so GCS transfer time is
excluded. Test files were incompressible random bytes (`head -c … /dev/urandom`);
"100 MB of files" means twelve 8,912,896-byte files.

## Alternatives and Decision

Doing nothing leaves publish unusable for notebooks with files and history,
which directly blocks the near-future direction of parallel work from local
checkouts. Raising the limit or telling owners to keep files elsewhere would
contradict the accepted "one store for file bytes" direction. Fixing each
symptom where it appeared would add code without removing the causes. The
recommended direction groups findings by the owner-visible journey that fixes
them together, puts the measured performance gap first, and keeps each story
independently deliverable.

Performance work follows the owner's standing expectation: remove the obvious
slowness so the work publish does scales with the size of the change, through
a simpler, more cohesive design with less code — not caches, background
precomputation, or special modes. If only a more complex fast path is found,
surface it as a decision.

Owner decisions on 2026-09-26 that removed candidates:

- Replacing a note's picture on the web keeps the previous file, because it is
  part of the notebook and its Git history
  ([attachments](../../docs/notebook-git-attachments.md)).
- AI guidance files keep following the ordinary note rules: `AGENTS.md`,
  `SKILL.md` and similar files must carry frontmatter with `type` to publish.
  Publishing them without it is dropped, not parked.

## Story Decomposition

S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours. These are hypotheses
including delivery, not commitments.

<a id="story-14"></a>

### 4c. A rich body edit keeps the file's final newline

**Identity:** SEED-046#story-14
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/009-rich-edit-keeps-final-newline/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"cfea83d963044589dc89f1b251186048edc80697d4b03c1d0e9c1d3ab54a4d43","plan":"527bbe661a62a51c98e0100032a502a7e7f7f9511f1d088f82dc9cef6e87b22a"}}
```

- **Goal:** Correction of story 4b (SEED-046#story-5; provenance
  `3d3a4d9b94:.planning/slice-plans/006-web-edit-changes-only-edit/PLAN.md`). Owners who
  edit a note in the rich editor see the diff limited to the lines they
  edited; today the file's last line also changes because its final newline
  is dropped.
- **Scope:**
  - **Required:** a rich body edit keeps the authored body's trailing
    newline run (`\n` or `\r\n`, as written); a body authored without one
    stays without one.
  - **Preserved:** the in-place web edits described in
    [note content saving](../../docs/note-content-saving.md) (common Markdown
    forms, frontmatter kept as written, property edits in place); Markdown mode.
  - **Excluded:** blank-line runs inside the body (story 4b exclusion).
- **Key examples:**
  1. `First\n\nLast\n`; type `My ` at the start in rich mode → the emitted
     Markdown is `My First\n\nLast\n` (today `My First\n\nLast`).
  2. A file with frontmatter whose body ends `# Demo2\n`; a one-word body edit →
     the saved file keeps `\n` after the edited line.
  3. Boundary: `Body` with no final newline; a one-word edit → still no
     final newline.
- **Known facts (2026-09-27, retrospective of story 4b):** Turndown output
  never ends with a newline, and neither `composeNoteContentInPlace` nor the
  server adds one back; plan 006's example 1 test pins the lost newline
  (`note.replace("# Demo2\n", "# My Demo2")`).
- **Effort hypothesis:** S — high confidence.
- **Depends on:** none (story 4b is delivered).

<a id="story-9"></a>

### 7. File pages and file responses read cleanly

**Identity:** SEED-046#story-9
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** A file page shows raw byte counts, and file, note, and upload
  responses carry an internal field that API and CLI consumers should not see.
- **Evaluation:** The file page shows a readable size; those responses no
  longer contain the internal field.
- **Known facts (2026-09-26):** a 204,800-byte file's page says
  "204800 bytes". `hibernateLazyInitializer: {}` appears in the file page
  response (`GET /api/notebooks/{notebook}/attachments/{attachment}`) and the
  picture upload response (`POST /api/notes/{note}/images`), on folder and
  notebook objects.
- **Value / learning:** Small polish.
- **Effort hypothesis:** S — medium confidence.
- **Depends on:** none.
- **Safe stopping point:** Each change stands alone.

<a id="story-13"></a>

### 8. Retire the one-time note line-ending normalization

**Identity:** SEED-046#story-13
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"unselected"}
```

- **For / why:** Maintainers. Once production has normalized its CRLF notes,
  the startup normalization is spent code that runs a query on every start.
- **Goal:** Remove `NoteLineEndingNormalizationOnStartup`, the
  `NoteLineEndingNormalization` service and its test, and their mention in
  [note-content saving](../../docs/note-content-saving.md), as the legacy
  picture move was retired (`0d57de8778`).
- **Scope:** Required: those removals only. Preserved: note Markdown stays LF on
  every write (`AuthoredNoteDocument.fromContent`) with its save test.
  Excluded: any negative test or note about the removed code.
- **Start condition (gate):** a release containing the normalization has been
  deployed and started in production, and a read-only production query finds
  no note content containing CRLF (`content LIKE '%\r\n%'` returns 0). If the
  query finds notes, read the startup log for the failing notebooks instead of
  removing the code.
- **Key examples:**
  1. After removal, the backend starts with no line-ending startup work, and
     the save test for CRLF-to-LF content still passes.
- **Value / learning:** Keeps one-off data repair out of the permanent code.
- **Effort hypothesis:** S — high confidence.
- **Depends on:** SEED-046#story-8 released and run in production.
- **Safe stopping point:** the single removal commit.

## Ordering and Scope Reduction

Story 8 is a short cleanup that can start only once production has run the
line-ending normalization; it goes first so it is not forgotten.
Story 4b fixes a web problem: its whole-file rewrites hurt parallel local and
web work. Story 7 is polish. Drop first: 7.

Story numbers are local order; identities keep their original anchors.

## Open Decisions

None that change selection or order.

## When to Surface

Now; all stories are queued ahead of SEED-033#story-2.

## Breadcrumbs

- Manual test session 2026-09-26 (SEED-045#story-1, history recoverable at
  7499f4454a).
- [Git synchronization](../../docs/notebook-git-synchronization.md),
  [Git LFS attachment storage](../../docs/notebook-git-lfs.md),
  [attachments](../../docs/notebook-git-attachments.md),
  [ADR 0004](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md).
