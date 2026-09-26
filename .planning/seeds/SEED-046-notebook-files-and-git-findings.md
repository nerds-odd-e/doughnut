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

### 6. Pull, publish and clone read notebook history without per-object queries, and a fresh clone starts clean

**Identity:** SEED-046#story-8
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/008-history-download-and-clean-clone/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"ad173d6e995ccaa48f13977ff9b0c52ad49ee318114d831d9496397aef096b21","plan":"96aa3a8b6555f672c3ab640ae57b14d8eac9676334708a7da0ac7a444bd4060e"}}
```

- **Goal:** Owners who work on a large notebook from a local checkout, often
  with an AI IDE, can pull, publish and clone without waiting seconds on work
  unrelated to their change, and a freshly cloned checkout is clean so pull and
  publish work on it straight away. Today every pull, publish and clone waits
  about 5 s for the server to read the notebook's Git history one object per
  database query, and a fresh clone of a notebook holding imported notes with
  Windows (CRLF) line endings starts with "modified" files, so pull refuses.
- **Scope:**
  - **Required — history download:** downloading a notebook's accepted
    history (the bundle that clone, pull and publish all fetch) reads the
    stored Git objects in bulk rather than one database query per object, so
    its server time no longer grows with thousands of round trips.
  - **Required — LF note content:** note Markdown that Donut stores and writes
    into accepted Git history uses LF line endings, matching the `*.md text`
    rule in Donut's own `.gitattributes`, whatever the content's source (web,
    API, MCP, import). Web and Git show the same content.
  - **Required — existing notebooks:** notes that already hold CRLF are
    normalized once; each affected notebook gets one accepted commit by the
    Donut System identity changing only those line endings, so a fresh clone
    of it is clean. Earlier history is not rewritten.
  - **Preserved:** attachment bytes are never normalized; the downloaded
    history always reflects the latest accepted commit (no stale copy);
    clone, pull and publish messages and behavior are otherwise unchanged.
  - **Excluded:** letting a no-op pull or publish skip the download by asking
    only for the accepted head (owner decision 2026-09-26: revisit after bulk
    reads land); sending only the commits a checkout lacks; caches or stored
    prebuilt packs; Git's own checkout time in clone (about 1 s for 11k files);
    the LFS batch endpoint reading full attachment content to check existence
    (not measured to matter here); lone CR characters; repairing existing
    checkouts that already show the old CRLF files as modified (pull still
    refuses there; the owner can clone again).
- **Key examples:**
  1. The Development "Japanese learning" notebook (id 1, 14,748 Git objects,
     7.7 MB stored): downloading its history no longer takes about 5 s — the
     server makes one bulk read instead of tens of thousands of queries, and
     clone's time is then mostly the CLI's startup and Git's own checkout.
  2. A no-op pull of that notebook reports "Notebook unchanged" without the
     5 s wait; a publish of a one-note change no longer waits 5 s before
     sending.
  3. After a web edit, a clone or pull receives that edit (no stale history).
  4. The same notebook's 19 notes stored with CRLF (for example
     `文法/敬語/尊敬語/ござる.md`): after the one-time normalization, a fresh clone
     shows a clean `git status`, and `notebook pull` right after clone says
     "Notebook unchanged". The notebook's history gains one Donut System
     commit touching only those 19 files.
  5. Saving a note through the API with `line one\r\nline two` stores and
     commits `line one\nline two`.
  6. Boundary: an attachment whose bytes contain CRLF is published and
     downloaded byte for byte.
- **Known facts (2026-09-26, measured on the Development stack):** clone of
  notebook 1 took 7.0 s: CLI startup 0.4 s, bundle download 4.8–5.3 s (three
  runs, 3.3 MB, repacked on every request), `git clone` 1.1 s, other git steps
  0.3 s. A no-op pull took 5.8–6.7 s (the earlier 0.3–0.5 s reading was wrong
  or predates pull downloading the bundle). Pull
  (`notebookAcceptedHistory.ts`) and publish (`notebookPublishAncestry.ts`,
  only to learn the accepted head) download the same full bundle as clone.
  Of 140 backend stack samples of JGit's delta workers during a download,
  135 waited on `JdbcNotebookObjectDatabase.find` (one primary-key `SELECT`
  per object open); compression was negligible. All 14,748 stored objects of
  notebook 1 are reachable. CRLF: 22 notes in the Development database (19 in
  notebook 1, 3 in notebook 3), all imported on 2026-09-18, hold CRLF pairs;
  no folder or notebook readme does. Git reports only the "racily clean" ones
  as modified right after checkout (2 of 19 here); the others turn modified
  once touched. Production data is assumed to hold the same legacy content.
- **Value / learning:** Removes the dominant cost of every pull and publish
  on large notebooks and the first-use failure after clone.
- **Effort hypothesis:** M–L — medium confidence (bulk read S–M; line-ending
  rule and one-time normalization M).
- **Depends on:** none.
- **Safe stopping point:** after the bulk read alone; the line-ending work
  stands on its own.

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

Story 4b fixes a web problem: its whole-file rewrites hurt parallel local and
web work.
Story 6 slows every pull and publish on large notebooks and breaks pull
right after cloning some imported notebooks (re-measured 2026-09-26), so it
stays ahead of story 7. Story 7 is polish. Drop first: 7.

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
