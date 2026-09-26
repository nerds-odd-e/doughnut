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
and merge, a note moved within its notebook carrying its picture, and note and
folder moves to another notebook reaching both notebooks' Git are delivered.
Story 10 carries files along in a move to another notebook, which is still
refused for folders with files. Story 25 makes link rewrites in other notebooks reach their Git. The
[product backlog](../PRODUCT-BACKLOG.md) owns global order.
No executable plan or implementation is authorized by this seed.

<a id="story-25"></a>

### Link rewrites in other notebooks reach their Git
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/047-link-rewrites-in-other-notebooks-reach-git/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"2fab3287c26552715e342bdf5419c6bb59175d7cd3bd4b12e93cf30e0924f59b","plan":"92dc2b800dace40547120bb202cbf6c8adf798082f8a4fe41f24d5efe8c335c2"}}
```

- **Identity:** SEED-035#story-25
- **Goal:** An owner who renames, moves or trashes a note or folder on the web
  can keep publishing, from a local checkout, every other notebook of theirs
  whose notes link to it. Today those web actions change the linking notes in
  the database only. The linking notebook gets no accepted commit, so its next
  local publish fails the accepted-tree check ("…differs from accepted main;
  refresh the checkout before publishing"), and only a history reset recovers
  it ([synchronization contract](../../docs/notebook-git-synchronization.md#domain-operation-ownership):
  "cross-notebook referrer rewrites remain outside this owner"). It rarely
  happens: links between notebooks are uncommon. The owner wants it for
  completeness (2026-09-26), so every web change reaches Git.
- **Scope:**
  - Every web action that changes notes in another notebook the user can edit
    appends one accepted commit to that notebook, in the same transaction as
    the action's own commit. It goes through the existing multi-notebook
    accepted-change owner, as relationship reduction and moves to another notebook do. The
    actions are note rename, note and folder moves within a notebook, folder
    rename, folder dissolve, trashing a note with "remove from properties",
    and a third notebook in a move to another notebook. This is one rule for which notebooks get a commit,
    not a fix per action.
  - Donut no longer changes notes in notebooks the user cannot edit, such as
    a subscribed notebook owned by someone else. Their links keep the old
    text, as a link that owner wrote would (owner decision 2026-09-26). Today
    the rewrite edits them without checking edit rights. Notebooks the user
    owns through a circle are editable and included.
  - Link rewriting itself is unchanged: which links change and how their text
    changes stay as delivered.
- **Excluded:**
  - Notebooks already out of step from earlier rewrites are not repaired;
    `reset-git-history` remains the recovery.
  - Notebook rename: it rewrites no links today, so no notebook goes out of
    step; `[[OldName:…]]` links breaking is a separate problem.
  - Repairing drift automatically on publish or on a later web change. The
    North Star wants a missed change to fail loudly.
- **Key examples:**
  1. The owner's notebook `Science` has `Force`; their notebook `Engineering`
     has `Bridge` saying `[[Science:Force]]`. The owner renames `Force` to
     `Load` → after pull, `Engineering`'s `Bridge` says `[[Science:Load]]`,
     and a local edit to `Engineering` publishes normally.
  2. `Bridge` says `[[Science:physics/Force]]`. The owner renames folder
     `physics` to `mechanics`, or dissolves it → after pull, `Bridge` in
     `Engineering` points to the new path.
  3. `Bridge` has property `uses: "[[Science:Force]]"`. The owner trashes
     `Force` choosing "remove from properties" → after pull, `Bridge` in
     `Engineering` no longer has that link.
  4. The owner moves `Force` to notebook `Physics` → after pull, `Bridge` in
     `Engineering` says `[[Physics:Force]]`, beside the move's commits to
     `Science` and `Physics`.
  5. Another user's notebook `Shared`, which the owner subscribes to, has a
     note saying `[[Science:Force]]`. The owner renames `Force` → `Shared` is
     unchanged and gets no commit; the owner's own linking notebooks are
     rewritten as in example 1.
- **Effort hypothesis:** M, medium confidence. The multi-notebook owner and
  the change capture for moves to another notebook exist. The work is choosing the
  notebook set before locking (the linking notebooks the user can edit) and
  the edit-rights filter on referrers.
- **Depends on:** nothing outstanding (moves to another notebook reach Git).
- **Safe stopping point:** if never delivered, renaming, moving or trashing a
  note linked from another notebook keeps blocking that notebook's local
  publishing.

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
- **Candidate scope (not refined):** replaces the refusal; moves to another
  notebook already reach both notebooks' Git. Stored bytes are notebook-scoped
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
  files between two checkouts), so this is a convenience ranked after story 25.
- **Effort hypothesis:** M, low confidence.
- **Depends on:** nothing outstanding.
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
is proven. Web deletion, dissolve/merge, a note move keeping its picture
within the notebook and moves to another notebook reaching Git are delivered;
then link rewrites in other notebooks reaching Git
(story 25, the same correctness fix for renames and moves) and the rarer
cross-notebook move of files (story 10, a convenience whose absence loses nothing).

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
  split it: a separate story made those moves reach Git, story 10 keeps carrying
  files, both queued after folder dissolve and merge. A moved folder carries its nested
  subfolders; no multi-folder move. A note move leaving its picture behind is
  in scope of this refinement: the file moves with the note
  (option A), rather than pointing `image:` back (the web does not resolve
  `..`) or accepting the broken picture.
- Owner decisions, 2026-09-26 (refinement of moves to another notebook
  reaching Git): keep its priority, because moving to another notebook is a
  required feature although rarely used. Split link rewrites in third notebooks
  into story 25, which covers every operation and is queued right after it. A picture note moved to another
  notebook moves with a broken picture, as today, and is not refused.
- Owner decisions, 2026-09-26 (story 25 refinement): keep it for
  completeness although it rarely happens, and do not measure it; execute
  it after moves to another notebook reach Git. Stop changing notes in notebooks the user cannot edit
  instead of committing to someone else's notebook.
- [Near-future direction](../PRODUCT-BACKLOG.md#near-future-direction).
- [SEED-009](SEED-009-git-backed-local-notebook-workflow.md): prior local/web note
  workflow. This seed owns non-Markdown attachment continuity.
