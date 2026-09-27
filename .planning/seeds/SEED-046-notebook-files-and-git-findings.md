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

<a id="story-9"></a>

### 7. Responses carry no ORM internals and a lean folder trail

**Identity:** SEED-046#story-9
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/010-responses-carry-no-orm-internals/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"acb4a665131454faf46e42ae55471a838f9c4b66b0215581d14e4efa56658123","plan":"cbd378e18afbfa14ece652f6f41ffdab215313e84ebd13d8a5fa6c4d9940ae48"}}
```

- **Goal:** API consumers (the web app, CLI, MCP, and anyone reading the
  API) receive responses that describe the domain, not the persistence layer.
  Today a Hibernate proxy's internal `hibernateLazyInitializer` property leaks
  into JSON, and the folder trail beside every note, folder, and file page is a
  list of full folder entities. Fixing the cause once, instead of on the two
  endpoints where the manual test noticed it, keeps the API honest as more
  work flows between local checkouts and Web Donut.
- **Scope:**
  - **Required (cause):** JSON responses never contain Hibernate proxy
    internals, whichever path loads an entity. The web JSON mapper handles
    Hibernate proxies; values that serialize today keep serializing.
  - **Required (shape):** each folder-trail entry (`ancestorFolders` on the
    note, folder, and file page realms and on recalled notes) carries only the
    folder's `id` and `name`, outermost first, as today.
  - **Preserved:** breadcrumbs, folder pickers, trash detection, the sidebar
    tree, recall's folder path, and the CLI's folder path segments behave as
    before.
  - **Excluded:** the file page's readable size (owner chose cause and shape
    only on 2026-09-27); the realm's `notebook` object, which is a loaded
    entity in practice and is read in about 20 places; other entity-bearing
    responses whose associations are eager; removing the Jackson 2 mapper
    used for hand-written serialization.
- **Key examples:**
  1. A file in folder `outer/inner`; a fresh request for its file page →
     `ancestorFolders` is `[{id, name: "outer"}, {id, name: "inner"}]`, with no
     `hibernateLazyInitializer` anywhere in the body (today the nested folder
     carries `"hibernateLazyInitializer": {}`).
  2. A picture uploaded to a note in a nested folder → the returned note realm
     has the same clean two-entry trail.
  3. A folder whose readme is long; the page of a note inside it → the trail
     entry has no `readmeContent`, `createdAt`, or `updatedAt`.
  4. Boundary: a note at notebook root → `ancestorFolders` is empty.
- **Known facts (2026-09-27):** Spring Boot 4 serializes web responses with
  its Jackson 3 (`tools.jackson`) mapper; `ObjectMapperConfig` registers
  `Hibernate7Module` only on a Jackson 2 mapper that serves hand-written
  serialization, and its Javadoc wrongly says web JSON uses it. A Jackson 3
  `tools.jackson.datatype:jackson-datatype-hibernate7` exists (3.1.7+). The
  leak reproduced on the dev backend for nested folders on the file page;
  whether it appears depends on whether the session already loaded the folder.
  Controller tests call methods directly and miss serialization; a MockMvc
  test like `NotebookFolderPageXmlAcceptMvcTest` sees real proxies.
- **Effort hypothesis:** M — medium confidence.
- **Depends on:** none.
- **Safe stopping point:** after the cause fix; the trail shape stands alone.

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
- **Known facts (2026-09-27, before the release):** production held 257 notes
  with CRLF in 96 notebooks, so the release should add up to 96 `Normalize
  note line endings` commits. Query through the Cloud SQL Auth Proxy as `root`
  (`mysql_root_password`); the `doughnut` user only accepts the app's private
  network. After v1.3.28 started in production (2026-09-26 23:52 UTC) the same
  query returned 0 notes in 0 notebooks: the start condition is met.
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
web work. Story 7 removes persistence details from API responses at their cause;
it matters least to owners today. Drop first: 7.

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
