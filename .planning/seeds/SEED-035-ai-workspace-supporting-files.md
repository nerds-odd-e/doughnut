---
id: SEED-035
status: dormant
planted: 2026-09-20
planted_during: owner shift toward parallel AI IDE work alongside Web Donut
trigger_when: selecting work for the shared local and web notebook direction
scope: large
---

# SEED-035: Keep AI working context and learning material together

## Why This Matters

Notebook owners use local AI IDEs alongside Web Donut. The desired outcome is
continuity of notebook content: Markdown notes and guidance follow Donut's
existing rules; images and other non-Markdown attachments travel with the notes.

The owner clarified that there is no special Markdown category for IDE guidance.
`AGENTS.md`, `SKILL.md`, and other Markdown use ordinary note/Readme behavior.
Invalid Markdown remains a publication error. Existing web-save normalization,
reserved Readme behavior, and valid unknown concept types remain unchanged.
AI guidance can use ordinary note refinement without special treatment.
Private learning history stays server-side and remains associated with notes.

The benefit is less manual copying between local authoring and web use. No time-saving or learning-improvement
measurements were supplied.
The priority remains a value hypothesis, not proof that all users need the same
workflow. Compliant Markdown guidance already has the ordinary note path;
non-Markdown acceptance is the remaining file-support gap selected here.

## Alternatives and Decision

- **Defer:** existing Markdown workflows continue, but non-Markdown material
  still cannot accompany the notebook through publication.
- **Keep files outside the notebook or in another repository:** adequate for
  machine-wide preferences or occasional use, but owners must assemble and
  maintain the supporting context separately.
- **Add a special Markdown attachment classifier:** rejected by owner direction.
  Purpose, IDE name, and file history do not exempt Markdown from current rules.
- **Recommended:** retain the cohesive Markdown behavior and add non-Markdown
  attachment continuity.
  Then deliver web retrieval and visual-authoring journeys through one file model.

The strongest workaround is a separate repository plus manual image downloads.
The first story earns priority only through the independent value of carrying
those files with the notebook, not because later stories need infrastructure.

## Architectural Constraints

- [ADR 0001](../../docs/adrs/0001-ubiquitous-language.md#notebook--note-structure)
  distinguishes Notes, container Readmes, and non-Markdown Attachments. Image
  presentation uses the same Attachment ownership and folder placement.
- [ADR 0004](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md#validation)
  applies the current Markdown format without IDE exceptions. Missing/invalid
  type or malformed Markdown frontmatter rejects publication; invalid content
  is not an attachment fallback. Import, export, publication, and lint share
  the format contract. AI-authored note results use the same contract.
- [ADR 0002](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
  makes the accepted tree authoritative for Portable content. Web operations
  preserve accepted files and apply complete content/reference changes through
  the same publication boundary. Learning identities remain private to Donut.
- [ADR 0005](../../docs/adrs/0005-web-routes-accepted.md) keeps authored content
  addresses distinct from private web identities. Attachment references do not
  become semantic note/property Wiki links.
- [ADR 0006](../../docs/adrs/0006-failure-handling-accepted.md) permits loud
  failures; do not conceal invalid output or lost files as successful acceptance.
- Accepted web changes derive their commit from the accepted head's tree and
  the projection rows the change touched (`NotebookGitTreeEncoder.derive`);
  today only the full assembly adds attachment entries. A web operation that
  inserts, renames or moves an attachment row must add that entry in the
  derivation (bytes for an insert, the accepted blob id at the previous path
  for a rename or move); the derived-tree oracle tests catch a missing entry.

The [North Star](../NORTH-STAR.md) owns the concise direction and the
[synchronization contract](../../docs/notebook-git-synchronization.md#attachments-in-the-portable-tree)
owns details. Ordinary local rebase reconciles unpublished work against the
latest accepted history; the remote never merges or rebases.

The accepted [Git LFS storage contract](../../docs/notebook-git-lfs.md), linked
from ADRs 0002 and 0004, specifies standard Git LFS, its client and protocol, and
immutable GCS payloads; accepted Git pointers select exact file versions. The
North Star's [one attachment content model](../NORTH-STAR.md#one-attachment-content-model)
governs every remaining story here: one byte store, every notebook on LFS, one
way in and one way out, roles (note picture, Book) that refer to Attachments,
and moving before retiring. Architecture acceptance does not mean
implementation is complete. Accepted history is preserved, so old binaries stay
in historical bundles.

## Story Decomposition

S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours, including delivery. Estimates
are hypotheses; refine/split work that exceeds L before execution planning.
Root-file and nested-file continuity, web file deletion, folder dissolve
and merge, and a note moved within its notebook carrying its picture are
delivered. Stories 24 and 10 own moves to another notebook, which the delivered behavior does not publish
(24) or refuses for folders with files (10). The [product backlog](../PRODUCT-BACKLOG.md) owns global order.
No executable plan or implementation is authorized by this seed.

<a id="story-24"></a>

### Moves to another notebook reach both notebooks' Git
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **Identity:** SEED-035#story-24
- **Goal:** An owner who moves a note or a folder to another notebook on the
  web finds the same result in both local checkouts after pull. Today such a
  move changes only the database: neither notebook's accepted history records
  it, so a pull of the source brings the note back and the destination never
  receives it
  ([synchronization contract](../../docs/notebook-git-synchronization.md#domain-operation-ownership):
  "Cross-notebook move and cross-notebook referrer rewrites remain outside this
  owner"). This silent divergence undercuts the near-future direction of
  working in local checkouts alongside the web.
- **Candidate scope (not refined):** the web note move and folder move to
  another notebook, including a folder's nested subfolders, run through the
  existing multi-notebook accepted-change owner (as relationship reduction
  does): one accepted commit per changed notebook, in one transaction. Folders
  containing files keep today's refusal (story 10). Moving several folders at
  once stays out (owner decision 2026-09-26).
- **Open decisions for refinement:** whether wiki-link rewrites in third
  notebooks belong here; whether a picture note moved to another notebook is
  refused until story 10 (the seed rule that operations which would rehome
  files refuse until their story delivers) or moves with a broken picture as
  today.
- **Effort hypothesis:** M, low confidence.
- **Depends on:** nothing outstanding (queued after folder dissolve and merge
  by owner decision 2026-09-26; that is delivered).
- **Safe stopping point:** if never delivered, web moves to another notebook
  keep diverging from local checkouts.

<a id="story-10"></a>

### Carry a folder's files along when it moves to another notebook
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **Identity:** SEED-035#story-10
- **Goal:** An owner reorganizing notebooks on the web can move a folder that
  contains supporting files, or a picture note, to another notebook, instead of
  first removing or relocating those files locally.
- **Evaluation:** Folder `refs/` holds a note and `paper.pdf`. The owner moves
  `refs` to another notebook on the web, then pulls both notebooks: the source no
  longer has `refs/`, the destination has `refs/paper.pdf` with the same bytes,
  and the note keeps its learning identity.
- **Candidate scope (not refined):** replaces the refusal once story 24 has
  made moves to another notebook reach Git. Stored bytes are notebook-scoped
  (`notebook/{id}/lfs/{sha}`, no cross-notebook deduplication), so each file's
  object is copied into the destination notebook's store before the change is
  accepted. A moved folder carries all nested subfolders; moving several
  folders at once stays out (owner decision 2026-09-26). The picture of a note
  moved to another notebook follows the within-notebook picture rules in the
  [attachment contract](../../docs/notebook-git-attachments.md).
- **Proposed exclusions:** a folder holding a Book's source file is refused
  (the Book belongs to the source notebook; [story 17 decision](#breadcrumbs));
  references from outside the moved folder break, as folder dissolve and
  merge accept;
  source objects are not deleted (garbage collection stays deferred). A merge
  into a same-named destination folder only if the delivered merge rules (every
  destination checked first) make it free; otherwise it stays refused.
- **Value:** the refusal loses nothing and a local workaround exists (copy the
  files between two checkouts), so this is a convenience ranked after story 24.
- **Effort hypothesis:** M, low confidence.
- **Depends on:** story 24.
- **Safe stopping point:** If never delivered, the refusal stays safe and clear.

## Ordering and Scope Reduction

The [product backlog](../PRODUCT-BACKLOG.md) owns global order. Root and nested
file continuity, the size boundary, LFS for new notebooks, and web browsing and
download are delivered.

The owner's end state (2026-09-24) is one attachment model for every file:
existing and new pictures and Book files as LFS notebook files, the legacy
stores removed, and every file present after clone or pull. The order that
avoids a chicken-and-egg problem is:

1. Story 14: every notebook converted to LFS, and no path creates a raw one;
   then story 20 moves the tests to LFS notebooks.
2. Story 4: new uploads become files, so the set of legacy pictures stops growing.
3. Story 5: move existing pictures. Its startup run in v1.3.26 moved nothing;
   the owner's manual trigger completed it (verified 2026-09-25).
4. Story 17: Book files.
5. The legacy picture storage, the separate Book storage, `attachment_blob`,
   the `/attachments/` address and the Book bucket are removed.

"Present after clone or pull" is not a story: it is how each of these stories
is proven. Web deletion, dissolve/merge and a note move keeping its picture within the
notebook are delivered; then moves to another notebook reaching Git
(story 24, a correctness fix) and the rarer cross-notebook move of files
(story 10, a convenience whose absence loses nothing).

The split is by usable outcome, not backend/frontend layers. Reject a
publish-now/preserve-on-web-later split: it would expose accepted files to
loss. Operations that would rehome files refuse until their story delivers.

## When to Surface

Now, under the revised near-future direction. No implementation is authorized.
Ready-made AI skills, arbitrary document previews, file editors, and new Git
integration need their own selected outcomes.

## Breadcrumbs

- Owner direction, 2026-09-20: AI IDEs alongside Web Donut; portable attachments
  and image files; web browsing, download, and deletion.
- Owner clarification: all Markdown retains the existing cohesive behavior,
  with no guidance exception or refinement changes. Remove the format-guarantee
  story.
- Owner direction, 2026-09-23: cap attachments at approximately 10 MB, keep
  attachment payloads out of MySQL and ordinary Git history, reduce bundle size,
  and store payloads in bucket-backed object storage. Git commits use a
  Git-LFS-style content-addressed pointer, never a raw GCS URL. The owner accepted
  standard Git LFS and the preserved-history transition, directing concise rules
  into existing ADRs and the Git LFS details into a regular document. Rollout
  belongs in the North Star and stories; implementation remains separate work.
- Owner direction, 2026-09-24: the end state is (1) existing pictures moved to
  LFS/GCS in the new structure, (2) new uploads on the same path, (3) the old
  implementation removed, and (4) files present on clone and pull. Convert
  existing notebooks by forward commit (option A); do not reset history, and do
  not move old-history bytes. Drop the story for skipping AI guidance folders
  during assimilation entirely: it is not important and kept drawing attention.
  Books become ordinary attachments, and their
  source file is the one exception to the 10 MiB limit. One cohesive
  architecture governs these stories (North Star).
- Owner decisions, 2026-09-25 (story 17 refinement): keep the priority; the
  Book refers to its file by path; removing a Book leaves the file; refuse a
  local publish that deletes, renames or changes a Book's file; three or four
  production Books, so moving them stays in the story.
- Owner decisions, 2026-09-25 (story 5 refinement): move a picture only into
  the notebook that owns it; leave other-notebook references unchanged; accept
  an ordinary rebase conflict for unpublished local frontmatter edits; keep
  learning state and last-updated time; split legacy removal into pictures
  and Books (both now done).
- Owner decisions, 2026-09-25 (after the production check of the picture
  move): the manual trigger completed the move; the production
  check is a read-only database query, not the startup log; the two notes
  with dead picture links are fixed manually, outside any story.
- Owner decisions, 2026-09-26 (story 10 refinement): story 10's premise was
  wrong — moves to another notebook never reach either notebook's Git — so
  split it: story 24 makes those moves reach Git, story 10 keeps carrying
  files, both queued after folder dissolve and merge. A moved folder carries its nested
  subfolders; no multi-folder move. A note move leaving its picture behind is
  in scope of this refinement: the file moves with the note
  (option A), rather than pointing `image:` back (the web does not resolve
  `..`) or accepting the broken picture.
- [Near-future direction](../PRODUCT-BACKLOG.md#near-future-direction).
- [SEED-009](SEED-009-git-backed-local-notebook-workflow.md): prior local/web note
  workflow. This seed owns non-Markdown attachment continuity.
