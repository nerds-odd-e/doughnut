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

<a id="story-1"></a>

### 1. Publishing a small change stays fast however much a notebook holds

**Identity:** SEED-046#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/002-publish-cost-follows-change/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"33fbe806b732dd4ba706a79badb8ebbcbf158301373d0df9ed0e3f70f864ce82","plan":"70c0ab06ae088a2d0e2c87e47239c4445cbaefbc114c8343c961c0307232f5e5"}}
```

- **Goal:** Owners who publish from a local checkout of a notebook with files,
  often from an AI IDE, get a publish whose cost follows the size of the
  change, not the length of accepted history or the number and size of files
  the change does not touch. Local publishing stays usable as work history
  grows, which the near-future direction of parallel local and web work
  depends on.
- **Scope:**
  - **Required:** neither the CLI nor the server repeats work over accepted
    history or over unchanged files when publishing. Evidence is publish time
    and the number of stored-file content reads per publish, both on the local
    Development stack.
  - **Approach constraint (standing owner expectation):** profile first, then
    remove the measured dominant cost through a simpler, more cohesive design
    with less code; no caches, background precomputation, or special modes. If
    only a more complex fast path is found, surface it as a decision.
  - **Preserved:** every current admission and refusal rule: the size limit
    for new files, Book source protection, Markdown validation, forward-only
    history, and size and digest verification of newly published file content.
    A notebook whose current files include an accepted over-limit Book source
    keeps publishing.
  - **Not promised either way (owner decision 2026-09-26, not the center of
    the problem):** how an over-limit file accepted earlier is recognized, and
    whether an over-limit file added and removed again within unpublished
    commits must be uploaded.
  - **Excluded:** real GCS and production measurement (owner decision
    2026-09-26: the slowness reproduces without it; content reads are counted
    instead); network transfer time of new file bytes; clone (story 6); pull
    (story 2); CLI messages (story 3).
  - **Deferred unless profiling shows it dominant after the above:** sending
    the whole Git history as a bundle in each direction; per-file overhead
    when publishing many new files at once.
- **Key examples** (local Development stack, owner's 5 s limit):
  1. A notebook with about 100 MB of files (12 files) and about 40 accepted
     commits; the owner publishes a one-line note edit → it takes about as
     long as the same edit in a notebook without files (0.5–0.8 s), not
     7–12 s, and no stored file content is read.
  2. A notebook with about 1,036 files, 300 MB and 47 commits; a one-line
     note edit → the same as example 1 (111.7 s today).
  3. The notebook from example 1; the owner adds 12 new files totalling
     100 MB → publish takes about as long as a first publish of those files to
     a fresh notebook (1.4–1.7 s), not 8–26 s, and only the 12 new files'
     content is read.
  4. The notebook from example 1; the owner changes one 8.5 MB file → only
     that file's new content is read (5.0–8.2 s today).
  5. Boundary: a new file over the size limit is still refused with today's
     size-limit message; a note edit in a notebook holding a 60 MB Book source
     still publishes.
- **Known facts (2026-09-26):**

  | Notebook | Publish | Time |
  | --- | --- | --- |
  | No files, 5–30+ commits | one-line edit | 0.5–0.8 s, flat |
  | 100 MB, fresh | 100 MB first publish | 1.4–1.7 s |
  | 100 MB unchanged files, ~5 commits | one-line edit | 1.45–1.56 s |
  | same, after 30 more note-only commits (one publish) | 30 commits | 7.4 s |
  | same, ~38 commits | one-line edit | 6.9–11.9 s |
  | same, ~40 commits | +100 MB (12 files) | 7.98 s, later 25.6 s |
  | 100 MB, ~25 commits incl. 5 versions of one 8.5 MB file | changed 8.5 MB file | 5.0–8.2 s |
  | 500 files × 200 KB (98 MB), fresh | first publish | 13.15 s |
  | same, +500 files | +100 MB | 48.9 s |
  | ~1,000 files / 200 MB, ~6 commits | add one note, then one-line edits | 69.6 s, 96.9 s, 94.3 s |
  | ~1,036 files / 300 MB, ~47 commits | one-line edit | 111.7 s |

  - Most of the time is CLI CPU: 94.3 s run = user 35 s + sys 40 s; 111.7 s
    run = user 49 s + sys 44 s; 6.5 s run = user 1.5 s + sys 1.6 s.
  - Backend garbage collection did not run during slow publishes (checked with
    `jstat`), so the in-memory byte store is not the cause.
  - Code reading during refinement (not yet profiled): on every publish the
    CLI walks every accepted commit and starts one `git cat-file` process per
    file per commit to collect earlier-accepted file digests
    (`attachmentPayloadDigestsInHistory` in
    `cli/src/commands/notebook/notebookPublishLfsSelection.ts`); the server
    walks the same history and then reads and re-hashes the stored content of
    every file present in each new commit, changed or not
    (`NotebookGitAttachmentSizeAdmission.admit`). Both history walks exist
    only for the over-limit rule that this story does not promise to keep.
  - For contrast, within the limit: clone 0.9–2.1 s (two early 100 MB runs of
    5.4–5.7 s were not reproducible); pull 0.5–2.9 s; web note save 0.12–0.20 s;
    web folder rename, move and trash about 0.1 s; dissolving a 500-file folder
    1.8 s; attaching a 60 MB Book 0.9 s.
- **Value / learning:** Removes the largest barrier to working on notebooks
  with files locally; tests the assumption that publish repeats work over
  accepted history and unchanged files that it does not need.
- **Effort hypothesis:** M — medium confidence; the likely cause is located,
  profiling still has to confirm it is dominant.
- **Depends on:** none.
- **Safe stopping point:** Publish keeps every current admission and refusal
  rule listed under Preserved.

<a id="story-3"></a>

### 3. Notebook CLI commands say briefly what happened and what to do next

**Identity:** SEED-046#story-3
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/003-clone-and-publish-name-next-step/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"945f97ea998695477866e2c909bc58802b0c3941612ab0e6cbb9a527c3ec9f82","plan":"a20d0e1ce4709ecc91fdd532758eec78ee2fbd01157ca3ec819193032238f5ee"}}
```

- **Goal:** Owners working on a notebook from a local checkout read the clone
  result, and the refusal they get when their checkout is behind the notebook,
  at a glance, and the next step it names works. Each publish rule is
  explained by the refusal that applies it, not listed in advance.
- **Scope:**
  - **Clone success:** a few short lines: which notebook was cloned where,
    then the next commands — edit and commit with any Git tool,
    `donut notebook publish <dir>`, `donut notebook pull <dir>` — each with
    the actual directory. It lists no publish or pull rules (owner decision
    2026-09-26); publish refusals already name the rule and paths they apply.
  - **Checkout behind the notebook:** publish gives one short message naming
    the recovery — run `donut notebook pull <dir>`, then publish again —
    whether its own check finds local main behind the accepted history or the
    server finds another publish or web save got in first. Today these print
    "only a contiguous single-parent commit range…" and "expectedHead no
    longer matches the notebook's current accepted head." with no next step.
  - **Assumption:** written against the code after story 2, which deletes the
    pull refusals behind the other findings and rewrites the pull help text.
  - **Excluded (owner decision 2026-09-26):** pull help text (story 2 owns
    it); which path a pull refusal names (story 2 deletes that refusal); the
    size-limit wording for a changed Book source (story 1 reworks that check;
    requeue only if it survives story 1); the clone message when the server
    has lost a file's content (rare data loss no local step fixes; clone
    already exits 1 and removes the partial checkout); any other CLI wording.
  - **Preserved:** the messages that already read well — dirty checkout,
    size limit, invalid Markdown — and every publish and pull refusal's
    decision.
- **Key examples:**
  1. `donut notebook clone 7 notes` succeeds → the output says notebook 7 was
     cloned into `notes`, then lists editing and committing,
     `donut notebook publish notes` and `donut notebook pull notes`; no rule
     list, a few lines instead of about 300 words.
  2. The owner commits a note edit in `notes`; meanwhile a web save is
     accepted; `donut notebook publish notes` → local main is not based on
     the notebook's latest accepted history; run `donut notebook pull notes`,
     then publish again. Doing so publishes the edit.
  3. Another checkout publishes after `notes` checked the accepted history but
     before its submission arrives → the server's refusal names the same
     recovery: run `donut notebook pull`, then publish again.
  4. Boundary: publish of a local merge commit still sends the owner to pull,
     whose existing refusal names the actual next step (recreate the work as
     ordinary commits); this story does not reword it.
  5. Boundary: a publish that breaks a publish rule (for example an unmatched
     note deletion mixed with additions) is refused with today's message
     naming the rule and paths — the guidance the clone message no longer
     lists in advance.
- **Depends on:** story 2 (it removes refusals and rewrites the pull help
  text); story 1 only for the excluded Book-source wording.
- **Effort hypothesis:** S — medium confidence.
- **Safe stopping point:** Each improved message stands alone.

<a id="story-5"></a>

### 4b. A web edit changes only what the user edited

**Identity:** SEED-046#story-5
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** Owners who also edit locally see a one-word web edit reformat
  the whole file, so diffs become noisy and local rebases conflict more. The
  churn is one-time per note plus whatever local tools keep writing in another
  style (a second rich edit of an already reformatted note changes only the
  edited word, checked 2026-09-26).
- **Evaluation:** After a one-word web edit, pulling shows a diff of that word
  only, for a note whose frontmatter and body use common Markdown and YAML
  styles.
- **Owner decisions (2026-09-26):**
  - The narrow goal is accepted: common local styles survive and a web edit
    touches only what it edited; the rich editor need not reproduce every
    authored body style byte for byte.
  - In: `#` headings and `-` bullets in the rich editor's output; the blank
    line after the frontmatter kept; a body edit leaves the frontmatter text
    untouched; a property edit changes only that property's line, both in the
    web editor and in the server's rewrites for setting a picture and for
    adding or removing a wiki link property.
  - Kept: `type: note` → `type: Note` (ADR 0004; one line, once).
  - Content the rich editor cannot keep is out of scope: the rich editor
    already opens such a note read-only.
- **Known facts (2026-09-26):** a published file

  ```markdown
  ---
  name: demo2
  description: Second skill
  type: note
  tags: [x]
  ---

  # Demo2
  ```

  came back after appending one word in the web editor as

  ```markdown
  ---
  description: Second skill
  name: demo2
  tags:
    - x
  type: Note
  ---
  Demo2 webui
  ===========
  ```

  - The rich editor re-sorts keys (`noteContentPropertyRows.ts`), re-dumps
    the whole frontmatter with the `yaml` package even for a body-only edit
    (dropping YAML comments and turning flow lists into block lists), and
    Turndown defaults to setext headings and `*` bullets. A frontmatter with
    nested metadata is instead kept verbatim.
  - `type: Note` comes from the server's in-place, one-line rewrite
    (`NoteLeadingFrontmatter.ensureTypeKey`).
  - The server re-dumps the whole frontmatter through SnakeYAML
    (`Frontmatter.fenced`) when setting a picture or adding or removing a wiki
    link property; the body stays verbatim. Inbound link rewriting on rename
    already splices only the changed scalar.
- **Value / learning:** Tests whether web edits can touch only the edited
  frontmatter line while the body keeps the common local styles.
- **Effort hypothesis:** M — low confidence.
- **Depends on:** none; it lets more notes stay editable in the rich editor.
- **Safe stopping point:** Author-owned and unknown frontmatter keys stay
  preserved, as ADR 0004 requires.

<a id="story-4"></a>

### 5. Uploading a picture on the web explains refusals and shows the new file

**Identity:** SEED-046#story-4
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/004-picture-upload-explains-and-shows/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"9266ce9b5c53b99b64163f591b17fbef283052d1d553b65b34dc7263bc360049","plan":"aa465c2c7fbb617d08e6374034ba08db719771a8a215a68378e870ab74fd6c98"}}
```

- **Goal:** Web users adding a picture to a note learn from the refusal why it
  was refused, never end up with a picture that cannot display, and see the new
  file in the sidebar at once. It makes the one web way to add files
  trustworthy. Any web form refused by request validation also shows the reason
  instead of "binding error".
- **Scope:**
  - **Required (app-wide, owner decision 2026-09-26):** a request refused by
    field validation carries the field's own message as its message, so the web
    toast shows it. "binding error" disappears everywhere, not only for upload.
    Fields that already show their message next to the input keep doing so.
  - **Required:** upload accepts a picture by its file extension, from the one
    list display uses (png, jpg, jpeg, gif, webp, ignoring letter case), and
    ignores the content type the browser declares (owner decision 2026-09-26).
    Any other extension is refused with a message naming the allowed types, so
    every accepted upload can display.
  - **Required:** an accepted upload appears in the sidebar without a reload.
  - **Preserved:** the 10 MiB limit (10,485,760 bytes, inclusive) and its
    refusal naming the limit; one accepted change writing the file and
    `image:`; the previous picture file stays
    ([attachments](../../docs/notebook-git-attachments.md)); name-clash and
    dot-name refusals.
  - **Excluded:** checking that the bytes really are a picture (a mislabelled
    file only fails to display, it cannot run); SVG or HEIC support; limiting
    the file picker to the allowed types (owner decision 2026-09-26: the
    refusal is what is promised); a size check in the browser before sending;
    the 100 MB request limit and its error; `image:` values published from a
    local checkout (a bad one shows as a broken picture, like a broken link);
    Book upload, which has its own validation.
- **Key examples:**
  1. Upload a 10,485,761-byte `big.png` → refused; the toast says the file
     exceeds the 10,485,760-byte limit, not "binding error"; the note is
     unchanged.
  2. Upload `drawing.svg` → refused; the toast names png, jpg, jpeg, gif and
     webp.
  3. Upload PNG bytes named `notes.txt` → refused the same way; `image:` is
     unchanged (today accepted, then 415 on display).
  4. Upload `photo.png` that the browser sends as `application/octet-stream`
     → accepted and displays (today refused).
  5. Upload `Blue.PNG` from the note page → the picture shows and the sidebar
     lists `Blue.PNG` without a reload.
  6. A web form whose field fails request validation → the toast shows that
     field's message instead of "binding error".
  7. Boundary: a 10,485,760-byte PNG is accepted; SVG bytes named `fake.png`
     are accepted as today and simply do not display.
- **Architecture:** one owner answers "which file names are pictures" for both
  upload admission and picture display, replacing today's separate upload
  content-type list and display extension map.
- **Known facts (2026-09-26):** the validator already writes "Invalid file
  type … Allowed types are …" and "File size exceeds the limit: 10485760
  bytes."; the two request-validation handlers set the message to the literal
  "binding error" and put those texts only in the per-field errors, which the
  toast ignores. Nothing depends on the literal. The sidebar has one refresh
  call that other structural web actions use and upload does not.
- **Effort hypothesis:** S — medium confidence.
- **Depends on:** none.
- **Safe stopping point:** Accepted uploads keep today's single-commit
  behavior and keep the previous picture file.

<a id="story-8"></a>

### 6. Cloning a notebook with many notes stays within 5 seconds

**Identity:** SEED-046#story-8
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** A notebook with many notes clones in over 5 s even without any
  large files.
- **Evaluation:** Cloning that notebook finishes within 5 s.
- **Known facts (2026-09-26):** the Development database's "Japanese
  learning" notebook (id 1, 51 commits, 11,222 tracked files — notes and
  folder `.keep` markers, 5.4 MB `.git`) cloned in 5.18, 5.36 and 5.12 s. A
  no-op pull of it took 0.3–0.5 s.
- **Value / learning:** Tests whether per-entry cost remains after story 1.
- **Effort hypothesis:** S–M — low confidence; may already be fixed by story 1.
- **Depends on:** story 1, whose profiling may remove the same cost.
- **Safe stopping point:** Clone still fills in every current file.

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

## Ordering and Scope Reduction

Story 1 is first by owner decision and because it most limits the near-future
direction. Story 3 carries the owner's explicit clone-message request. Stories
4b and 5 fix web problems. Story 4b's
whole-file rewrites come first because they hurt parallel local and web work,
while picture upload (story 5) is a web-only path the near-future direction
does not name (owner decision 2026-09-26). Story 6 may be absorbed by story 1.
Story 7 is polish. Drop first: 7, then 6 (if story 1 absorbed it).

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
