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

### 4. A web edit changes only what the user edited

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
- **Value / learning:** Finds the per-entry cost in clone. Publishing now
  checks only the files the published commits change; clone was not part of
  that work.
- **Effort hypothesis:** S–M — low confidence.
- **Depends on:** none.
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

Story 3 carries the owner's explicit clone-message request. Stories 4
and 5 fix visible web problems; the web edit story comes first (owner decision
2026-09-26) because whole-file rewrites hurt parallel local and web work, while
picture upload is a web-only path the near-future direction does not name. Story 7 is
polish. Drop first: 7, then 6.

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
