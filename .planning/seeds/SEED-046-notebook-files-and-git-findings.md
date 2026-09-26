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
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** Owners publishing from a local checkout of a notebook with
  files wait tens of seconds or minutes for a one-line note edit; the wait grows
  with every accepted commit.
- **Evaluation:** In a notebook holding about 100 MB of files with 40+
  accepted commits, publishing a one-line note edit, 100 MB of new files, or
  500 new small files each finishes within 5 s (the owner's limit); a
  one-line edit costs about the same as in a notebook without files.
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
  - The cost follows history length and the number and size of current files,
    not the size of the change.
  - For contrast, within the limit: clone 0.9–2.1 s (two early 100 MB runs of
    5.4–5.7 s were not reproducible); pull 0.5–2.9 s; web note save 0.12–0.20 s;
    web folder rename, move and trash about 0.1 s; dissolving a 500-file folder
    1.8 s; attaching a 60 MB Book 0.9 s.
- **Value / learning:** Removes the largest barrier to working on notebooks
  with files locally; tests the assumption that publish repeats work over
  accepted history and unchanged files that it does not need.
- **Effort hypothesis:** M–L — low confidence until profiling shows the
  dominant cost.
- **Depends on:** none.
- **Safe stopping point:** Publish keeps every current admission and refusal
  rule (size limit, Book source protection, Markdown validation, forward-only
  history).

<a id="story-2"></a>

### 2. Pull keeps unpublished local work over unrelated file and folder changes

**Identity:** SEED-046#story-2
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** An owner with unpublished local work must re-clone and move
  the work by hand whenever anyone deletes or adds a file, or renames or moves a
  folder, anywhere in the notebook — common once several people or tools work
  in parallel. The accepted synchronization direction expects local work to
  rebase over independent web changes
  ([Git synchronization](../../docs/notebook-git-synchronization.md)).
- **Evaluation:** Local unpublished commits that add or edit notes survive a
  pull over an accepted web file delete, a file published from another
  checkout, and a web folder rename elsewhere in the notebook; pull rebases
  them and they then publish. A change that really overlaps the local work
  still stops with a clear, working next step.
- **Known facts (2026-09-26):** each case below refused pull with "Local main
  cannot receive the accepted history because accepted history includes a
  structural change at …", then told the owner to clone fresh elsewhere:
  - a local note edit, then a web delete of an unrelated root file (`fake.png`);
  - a local note addition, then another checkout published a new root file
    (`d2.bin`);
  - a local commit adding a root file, then a web folder rename elsewhere
    (`Renamed/inner/doc.pdf`);
  - a local note and file addition, then another checkout published a note and
    a file.

  A local file addition over an accepted note-only addition rebased fine.
- **Value / learning:** Delivers the parallel local/web work the accepted
  synchronization direction describes; tests which accepted changes can be
  replayed under local work safely.
- **Effort hypothesis:** L — low confidence; split by change kind (file changes
  first, folder changes next) if refinement confirms two outcomes.
- **Depends on:** none.
- **Safe stopping point:** Anything not rebased is still refused with the local
  work preserved, as today.
- **Open for refinement:** what counts as overlapping (same path, same folder
  moved, a picture the local note refers to).

<a id="story-3"></a>

### 3. Notebook CLI commands say briefly what happened and what to do next

**Identity:** SEED-046#story-3
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** CLI users get a very long clone success message and refusals
  that use internal terms, name the wrong path, or send them in circles.
- **Evaluation:** The owner can read each message at a glance, and every next
  step it suggests works.
- **Known facts (2026-09-26):**
  - Clone success prints one paragraph of about 300 words listing every
    supported publish shape. The owner wants it much shorter and better
    formatted.
  - Losing a publish race prints "expectedHead no longer matches the notebook's
    current accepted head." with no next step.
  - After pull refuses, publish says "Run donut notebook pull to base your
    local commits on the accepted history", which refuses again.
  - When another checkout published a note and a file together, the pull
    refusal named the note (`FromA.md`) instead of the file that blocked it.
  - Changing a Book's 60 MB source file locally is refused with the size-limit
    message ("Remove or shrink it"), not with "remove the Book first", which a
    rename of the same file does get.
  - When the server no longer has a file's content, clone reports "Notebook
    attachments are incomplete" with each missing object, then says "Fix
    authorization or connectivity, then rerun". Clone does exit 1 and removes
    the partial directory, so the rerun works.
  - Messages that already read well: the dirty-checkout refusal, the size-limit
    refusal (path, size, limit), and the invalid-Markdown refusal.
- **Value / learning:** Owner-requested; cheap and immediately visible.
- **Effort hypothesis:** M — medium confidence.
- **Depends on:** none; story 2 removes some refusals, so refine this after it
  to avoid polishing messages that disappear.
- **Safe stopping point:** Each improved message stands alone.

<a id="story-4"></a>

### 4. Uploading a picture on the web explains refusals and shows the new file

**Identity:** SEED-046#story-4
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** Web users uploading a picture see "binding error" for a wrong
  type or an over-limit size, can upload a picture that will never display, and
  do not see the new file in the sidebar until they reload.
- **Evaluation:** An unsupported type or an over-limit file is refused with a
  message naming the allowed types or the size and limit; an upload the note
  could not display is refused; an accepted upload appears in the sidebar
  immediately.
- **Known facts (2026-09-26):**
  - "binding error" (HTTP 400, shown as the web toast) for: an SVG; a
    10,485,761-byte PNG; a PNG sent as `application/octet-stream`.
  - A 10,485,760-byte PNG is accepted.
  - PNG bytes named `notes.txt` are accepted and written as
    `image: notes.txt`, but the picture then fails with 415. SVG bytes named
    `fake.png` sent as `image/png` are accepted and served as `image/png`.
    Upload trusts the declared content type; display goes by the name.
  - After uploading `Blue.PNG` from the note page, the picture shows, but the
    sidebar lists it only after a page reload.
  - Name clashes (ignoring case, against files, notes and folders) and
    dot-names are already refused with clear messages.
- **Value / learning:** Makes the one web way to add files trustworthy.
- **Effort hypothesis:** S–M — medium confidence.
- **Depends on:** none.
- **Safe stopping point:** Accepted uploads keep today's single-commit
  behavior and keep the previous picture file.

<a id="story-5"></a>

### 5. A web edit changes only what the user edited

**Identity:** SEED-046#story-5
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** Owners who also edit locally see a one-word web edit rewrite
  the whole file, so diffs become noisy and local rebases conflict more.
- **Evaluation:** After a one-word web edit, pulling shows a diff of that word
  only, for a note whose frontmatter and body use common Markdown and YAML
  styles.
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

  Keys are re-sorted, the flow list becomes a block list, `note` becomes
  `Note`, the blank line after the frontmatter is dropped, and the heading
  changes style. Author keys themselves are preserved, as ADR 0004 requires. A
  body line holding `![](p.png)` kept its text.
- **Value / learning:** Tests whether the web editor can preserve authored text
  outside the edited part.
- **Effort hypothesis:** M–L — low confidence.
- **Depends on:** none.
- **Safe stopping point:** Author-owned and unknown frontmatter keys stay
  preserved, as ADR 0004 requires.
- **Open for refinement:** whether `type: note` → `Note` canonicalization
  should stay.

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
direction. Story 2 follows: it removes the forced re-clone that parallel work
hits most often. Story 3 carries the owner's explicit clone-message request;
it follows story 2 so it does not polish messages story 2 removes. Stories 4
and 5 fix visible web problems. Story 6 may be absorbed by story 1. Story 7 is
polish. Drop first: 7, then 6 (if story 1 absorbed it).

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
