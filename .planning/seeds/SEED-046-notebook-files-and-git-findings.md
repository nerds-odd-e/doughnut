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

<a id="story-10"></a>

### 4a. The rich editor never silently loses content

**Identity:** SEED-046#story-10
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/005-rich-editor-keeps-content/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"21dc6b21669ba30f2dc4ef45679acc28bf1ea4dd0943bae247d451497ab4b4a4","plan":"1f688f88051c815e45e8c61f8979268e0f3ae0fea0355391f1bc070dff8f831a"}}
```

- **Goal:** Owners whose notes carry Markdown the web's rich editor cannot
  represent — often notes written locally or by an AI IDE — never lose part of
  a note by editing it on the web. Today a one-word rich edit deletes raw HTML,
  task checkboxes, line breaks and more, and saves the loss into the accepted
  history without warning. Safe web editing of locally authored notes is a
  precondition for parallel local and web work.
- **Scope:**
  - **Required:** when the rich editor opens a note whose body it cannot
    carry through an edit unchanged in meaning, it shows the note read-only
    with a warning that tells the owner to switch to Markdown mode to edit it,
    as it already does for frontmatter it cannot parse. "Unchanged in
    meaning" is judged by how the body renders: the body the editor would save
    must render the same as the authored body, so one check covers every
    construct, including ones not listed below.
  - **Required:** a body whose round trip changes only style — heading or
    bullet style, emphasis marker, list numbering, blank-line count, reference
    links written inline — stays editable in rich mode. Style churn belongs to
    story 4b.
  - **Preserved:** Markdown mode edits the text exactly as typed; the existing
    read-only warning for frontmatter the rich editor cannot parse; how the
    rich editor displays a note.
  - **Excluded:** making the rich editor support more constructs (each would
    be its own story); frontmatter (story 4b keeps it verbatim outside the
    edited property); restoring content already lost by earlier web edits (it
    is in the notebook's Git history); automatically switching the page to
    Markdown mode; content pasted into the rich editor during an edit.
- **Key examples** (a one-word edit in rich mode unless stated; measured on
  2026-09-26 by driving the real editor):
  1. A note whose body holds `<details><summary>S</summary>Inner</details>`
     → the rich editor is read-only and warns to use Markdown mode (today the
     whole body except the typed word is saved away).
  2. A task list `- [ ] todo` / `- [x] done` → read-only with the warning
     (today the checkboxes disappear).
  3. `line one␠␠` / `line two` (a hard line break), or `**a** *b*` (two
     formatted words separated by a space) → read-only with the warning (today
     the words run together: `line oneline two`, `**ab**`).
  4. Two fenced code blocks, one tagged `ts` → read-only with the warning
     (today they merge into one untagged block).
  5. Boundary: `# Heading`, `- item` bullets and a paragraph with a link → rich
     editing works as today; the edit is saved (in today's style until story 4b).
  6. Boundary: the note from example 1 in Markdown mode → edits save exactly
     as typed.
- **Known facts (2026-09-26):** also lost or changed in meaning today: nested
  and multi-line block quotes, footnotes, `$$` math blocks, `3)` ordered
  lists, nested ordered lists (indent drops from 3 to 2 spaces), intraword
  `2*3*4`. Tables, wiki links, images, quoted YAML values and plain paragraphs
  survive. The body goes Markdown → HTML (`marked`) → Quill → HTML → Markdown
  (Turndown). Frontmatter the rich editor cannot parse already makes it
  read-only with "Switch to Markdown mode to fix the frontmatter."
- **Value / learning:** Stops silent content loss for every construct at once
  instead of patching the converters construct by construct; shows how many
  real notes the rich editor cannot carry.
- **Effort hypothesis:** S — medium confidence.
- **Depends on:** none.
- **Safe stopping point:** Notes the rich editor can carry edit exactly as
  today.

<a id="story-5"></a>

### 4b. A web edit changes only what the user edited

**Identity:** SEED-046#story-5
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/006-web-edit-changes-only-edit/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"f658172ca79107b7e3666796491a3346f8ac5762b8cef65630a6a3fb75b57b5b","plan":"8bc0a3b8e34b39f6fa6e98dee0e6a17894804f59e1e1de6d84a03c4c51fedffe"}}
```

- **Goal:** Owners who edit a notebook both locally (often with an AI IDE) and
  on the web see a web edit change only what they edited, so pulled diffs stay
  readable and local rebases do not conflict with reformatting. Today one
  web-edited word reformats the whole note file.
- **Scope:**
  - **Required — body style:** the rich editor writes the common Markdown
    forms: `#` headings, `-` bullets, `*` emphasis (with `**` strong) and
    `---` rules. So a body using those forms changes only where it was edited.
  - **Required — body edit:** a rich edit of the body leaves the frontmatter
    text exactly as it was, including comments, key order, quoting, flow lists
    and the blank lines between the frontmatter and the body.
  - **Required — property edit on the web:** changing, adding, renaming or
    removing one property in the rich editor's property panel changes only
    that property's lines. A new property goes at the end of the frontmatter.
    The panel lists properties in the file's order, not sorted.
  - **Required — server property edits:** setting a note's picture (upload,
    and its update when the picture file moves), setting its image mask,
    adding a link property when a relationship note is reduced to a property,
    and removing links to a trashed note from other notes' properties each
    change only the affected property's lines.
  - **Preserved:** `type: note` → `type: Note` on save (ADR 0004; one line,
    once); author-owned and unknown keys (ADR 0004); Markdown mode saves text
    as typed; a property left empty by link removal is removed as today; a
    note without frontmatter gets a new block formatted as today.
  - **Excluded:** reproducing every other body style (`+`/`*` bullets,
    `_` emphasis, setext headings, list numbering, blank-line runs); keeping
    the style of a property's own value once it is changed (a changed flow list
    may become a block list); content the rich editor loses (story 4a); other
    server code that writes whole files (export, new notes, readmes).
- **Key examples:**
  1. The manual-test file above; append one word to the body in rich mode →
     the saved file differs only on the body line and on `type: note` →
     `type: Note`; the blank line and `# Demo2` stay.
  2. A note whose frontmatter has `# a comment`, `description: "Quoted: value"`
     and `tags: [x, y]`, keys unsorted; change `description` in the panel →
     only the `description` line changes.
  3. Same note; add property `source` → one line appended before the closing
     `---`; remove `tags` → only its line disappears.
  4. A body with `## Part`, `- item`, `*em*`, `**strong**` and a `---` rule;
     one-word edit → only the edited line changes.
  5. Upload a picture to the note from example 2 → only an `image:` line is
     added (or replaced); the comment and flow list stay.
  6. Reduce a relationship note to a property of its source note, whose
     frontmatter is as in example 2 → one property line appended.
  7. Move a note to trash choosing to remove its links from properties, when
     another note has `see also: "[[Target]] and [[Other]]"` → only that
     value changes.
  8. Boundary: a body with `+ item` bullets and `_em_`; one-word edit → those
     lines become `- item` and `*em*` (style outside the common forms is not
     kept).
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

  - The rich editor sorts property rows (`noteContentPropertyRows.ts`) and
    re-dumps the whole frontmatter with the `yaml` package even for a
    body-only edit (`composeNoteContentMarkdown`), dropping comments and
    turning flow lists into block lists. Turndown defaults to setext headings,
    `*` bullets and `_` emphasis. A frontmatter with nested metadata is
    already kept verbatim.
  - `type: Note` comes from the server's in-place, one-line rewrite
    (`NoteLeadingFrontmatter.ensureTypeKey`).
  - The server re-dumps the whole frontmatter through SnakeYAML
    (`Frontmatter.fenced`) in `NoteContentMarkdown.setLeadingFrontmatterProperty`
    (picture, image mask, link property added by reduce) and
    `removeWikiLinksFromLeadingFrontmatterProperties`. Inbound link rewriting
    on rename already splices only the changed scalar
    (`WikiLinkMarkdownDocumentRewrite`).
- **Owner decisions (2026-09-26):** the narrow goal above rather than
  reproducing every authored style; keep `type: Note`; include the server
  picture and link-property rewrites; content loss split out as story 4a.
  Refinement decided without further questions: the panel follows file order,
  and `*` emphasis and `---` rules count as common forms, per the evaluation
  "common Markdown and YAML styles".
- **Value / learning:** Tests whether every web and server frontmatter edit
  can share one in-place edit rule.
- **Effort hypothesis:** M — medium confidence.
- **Depends on:** none; it lets more notes pass story 4a's check.
- **Safe stopping point:** each of body style, body edit, panel edit and
  server edit stands alone.

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

<a id="story-11"></a>

### 8. Clone test states the clone output instead of what it no longer says

**Identity:** SEED-046#story-11
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/007-clone-test-states-output/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"4895a0836d89935a2a8427efcad8af5ff68c561540b01b54b61c653d2941012e","plan":"c58a45ad26fa340e8cbd0a5694d2dba225e8a2231b3027ed7e29e2d3f8599f0d"}}
```

- **Goal:** Developers reading the clone test see the exact clone message
  that owners get, with no assertion naming text that was deleted
  (retrospective correction of SEED-046#story-3).
- **Scope:** Only the first test in `cli/tests/notebookClone.test.ts`: replace
  its `toContain` / `not.toContain` checks on the log output with one exact
  assertion of the four-line message. No product change.
- **Plan:** [007-clone-test-states-output](../slice-plans/007-clone-test-states-output/PLAN.md)

## Ordering and Scope Reduction

Stories 4a and 4b fix web problems. Story 4a (silent content loss in the rich
editor) comes first: it damages locally authored notes permanently, and the
owner split it out of story 4b on 2026-09-26 and ranked it first. Story 4b's
whole-file rewrites come next because they hurt parallel local and web work.
Story 7 is polish. Drop first: 7, then 6.

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
