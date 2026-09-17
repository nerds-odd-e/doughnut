# Reduce a relationship into a source note in another notebook

Status: in progress
Source: [SEED-025 story 1](../../seeds/SEED-025-cross-notebook-relationship-reduction.md#story-1),
refined 2026-09-17. Owner decisions: Option B (one accepted commit per
touched notebook), priority kept. Execution authorized 2026-09-17
(`/dough-execute-plan 138`).

## Execution identity

- Mode: Story Branch Mode; replanning permission: default (plan refinement
  allowed under learning escalation).
- Originating/integration checkout: `/Users/terryyin/git/doughnut`, branch
  `main`; claim commit `6281a7d0cd` (local, not pushed separately).
- Execution checkout:
  `/Users/terryyin/git/doughnut/.claude/worktrees/138-cross-notebook-relationship-reduction`,
  branch `138-cross-notebook-relationship-reduction`.
- Push destination: `origin` (`nerds-odd-e/doughnut`), branch
  `138-cross-notebook-relationship-reduction`.
- CI observer: GitHub Actions `ci.yml` ("donut CI"), mailbox
  `/tmp/dough-ci-501/watch-u5BoT7`, runtime
  `.agents/skills/dough-execute-plan/scripts/ci-mailbox.mjs` in the execution
  checkout.

## Goal and scope

A note owner whose relationship note lives in notebook A and whose source
note lives in notebook B reduces the relationship. Afterwards Donut, A's
accepted Git history, and B's accepted Git history all show the same result:
B's source note carries the property (authored so it resolves from B), A no
longer has the relationship file, and neither notebook has drifted from its
accepted tree. Today B silently drifts and its synchronization stops for good
(verified in the seed).

Included:

- The added property value is the relationship's `target` re-authored from
  the source note's notebook, using the same rule a note's outgoing links get
  when the note moves between notebooks.
- The accepted-change owner covers the set of notebooks a web action touches:
  lock in ascending notebook-id order, run the complete domain operation once,
  append one accepted commit per locked notebook whose tree changed, all in
  one transaction.
- Reduction locks the relationship note's notebook and the resolved source
  note's notebook (one lock when equal), commits both, and refuses with no
  change if the source is found in a notebook outside the locked set.
- Existing per-notebook pre-existing-drift policy is preserved: a notebook
  whose tree already differed before the action gets the database change and
  no commit.

Excluded (considered, not built or verified here):

- Cross-notebook note move, rename referrer rewrites, and trash
  "remove from properties" across notebooks (family follow-ups, seed).
- Drift visibility or recovery.
- Frontend and API changes: the endpoint, request, and response are unchanged,
  so no client regeneration.
- An E2E scenario. The web-visible outcome (property on the source) is already
  covered by `relationship_edit_and_remove.feature`; the cross-notebook
  difference is Git history, which the controller-plus-bundle boundary
  observes directly and cheaply.
- Fault injection for commit failure. Atomicity rests on the existing
  `@Transactional(SERIALIZABLE, rollbackFor = Exception.class)` boundary that
  now spans both bindings; the plan proves refusal paths leave both heads
  unchanged instead.

Assumptions:

- Every production notebook has a Git binding; the owner still tolerates an
  unbound notebook by skipping its commit (existing `Optional` path).
- Cross-notebook wiki resolution of `source` already works via qualified
  Portable paths (`[[Astronomy:Moon]]`); the test builder
  `RelationshipNoteMarkdown.forEndpoints` qualifies cross-notebook endpoints.

## Architecture

Follows NORTH-STAR "One complete accepted web change", revised 2026-09-17 to
state the multi-notebook rule (see that topic). One consistency owner,
`AcceptedWebChangeService`, gains a notebook-set form; the single-notebook
form delegates to it. No second owner, no controller-level Git coordination,
no per-story mode.

PFE results:

- Re-authoring the target link: reuse the outgoing-link rule inside
  `WikiLinkRewriteSupport.applyOutgoingNotebookMoveRewrite` (ambiguous in the
  old scope → keep; otherwise qualify an unqualified link with the old
  notebook name). Modularize that per-link decision into one static used by
  both callers; do not add a second qualifier.
- Source resolution before locking: reuse `NoteReferenceHandling`'s
  parse-and-resolve (currently private inside
  `reduceRelationNoteToSourceProperty`); expose it so `RelationReduceService`
  can learn the source notebook before locking, and the reduction re-resolves
  under the lock as today.
- Locking: `NotebookGitBindingRepository.findByNotebookIdForUpdate` per
  notebook, called in ascending id order.

Accepted ADRs touched: 0004 (Portable markdown, unchanged), 0006 (fail
loudly on the moved-source race). Proposed ADR 0002 unchanged.

## Key examples (from the seed)

1. Cross-notebook reduction commits to both notebooks.
2. Source notebook not editable → refused, neither head advances.
3. Source notebook already drifted → A commits, B gets the database change
   and no commit.
4. Same-notebook reduction unchanged (existing proof).
5. Learning tracker follows the property across notebooks.

## Outside-in proof

| Promise | Owning slice | Proof |
| --- | --- | --- |
| Property value resolves from the source note's notebook | 1 | `RelationControllerReduceToSourcePropertyTests`: relation in "Space topics", source "Moon" in "Astronomy", target "Earth" in "Space topics" → Moon contains `[[Space topics:Earth|Earth]]` |
| Existing single-notebook accepted changes unchanged | 2, 3 | `*NotebookGitWeb*` controller tests and `RelationController*Tests` green after each |
| One accepted commit in each notebook, relationship file gone, no `_trash/`, tracker on Moon | 4 | `NotebookGitWebRelationReduceControllerTest`: both downloaded bundles advanced by exactly one commit whose parent is the previous head; tracker is a property tracker on Moon |
| Refusal leaves both heads unchanged | 5 | same class: source notebook not editable → 400, both heads equal to before, relation note present |
| Pre-existing drift keeps per-notebook policy | 6 | same class: Moon edited without snapshot → A advances, B head unchanged, Moon has property in DB |
| Moved-source race refused | 4 | code review only: no automated proof (needs a concurrent move); recorded as unproved guard |

## Ordered slices

### 1. Reduced property value is authored from the source note's notebook

Type: Behavior
Status: done
Accepted proof: `RelationControllerReduceToSourcePropertyTests.authorsTheTargetFromTheSourceNotebookWhenTheSourceLivesInAnotherNotebook`
(6/6 green after refactor); move caller unchanged
(`RelationControllerTests`, `NotebookFolderCrossNotebookMove*`,
`RelationControllerMoveNoteToFolderTests` green). Shared owner:
`WikiLinkRewriteSupport.markdownLeavingNotebook` / `outgoingLinkLeavingNotebook`.
Proof: `CURSOR_DEV=true nix develop -c ./backend/gradlew -p backend test --tests '*RelationControllerReduceToSourcePropertyTests*' -Dspring.profiles.active=test --build-cache`
green with the new cross-notebook example.

Behavior: relationship note in owned notebook "Space topics" with
`source: "[[Astronomy:Moon]]"` and `target: "[[Earth]]"` (Earth in "Space
topics"), Moon in owned notebook "Astronomy" → reduce → Moon's frontmatter
gains `a part of: '[[Space topics:Earth|Earth]]'`; the relationship note is gone.
Same-notebook reduction still writes the target verbatim.

Implementation: in `NoteReferenceHandling.reduceRelationNoteToSourceProperty`,
when the source notebook differs from the relationship note's notebook,
rewrite each wiki token of the target scalar with the outgoing-link rule
extracted from `WikiLinkRewriteSupport.applyOutgoingNotebookMoveRewrite`
into one static (ambiguous in the old scope → keep; otherwise qualify an
unqualified link with the old notebook name; old scope = relationship note's
notebook name). Both callers use that static. Nothing else moves in this
slice.

### 2. Accepted web change operates on a locked notebook set

Type: Structure
Status: done
Accepted proof: `*NotebookGitWeb*`, `*RelationController*`,
`*TextContentController*`, `*NotebookFolder*` green (235 tests) after
refactor; covers Git-bound (one-entry) and plain (empty) `LockedNotebooks`.
Proof: `CURSOR_DEV=true nix develop -c ./backend/gradlew -p backend test --tests '*NotebookGitWeb*' --tests '*RelationController*' -Dspring.profiles.active=test --build-cache`
green; every existing caller still passes one notebook id.

Internal change: introduce `LockedNotebooks` in `services/notebookGit`: the
states of the notebooks a web action locked, keyed by notebook id, with
`Optional<LockedNotebookState> state(notebookId)` and
`Optional<Note> liveNote(noteId)` (a note found in any locked snapshot). The
`CompleteOperation` receives `LockedNotebooks` instead of one
`Optional<LockedNotebookState>`. `WebNoteEditService.
resolveNoteWithinLockedNotebooksOrRepository` becomes "locked snapshot, else
repository" through `liveNote`; `edit`'s notebook-id check is unchanged.
`apply(Integer, …)` still locks exactly one binding. Enables slice 3.

### 3. Accepted web change locks several bindings and commits each changed notebook

Type: Structure
Status: planned
Proof: same command as slice 2 green; `apply(Set<Integer>, …)` exists and
`apply(Integer, …)` delegates to it with a one-element set.

Internal change: `AcceptedWebChangeService.apply(Set<Integer> notebookIds, …)`
locks each bound notebook in ascending id order (`findByNotebookIdForUpdate`
per id), opens each accepted bundle and verifies its head, records per
notebook whether the tree matched before the change, runs the operation once,
flushes, and for every notebook that matched before and differs now appends
one accepted commit with the operation's message. Bundles are closed after
the loop (one small try/finally over the opened list). Unbound ids are
skipped. Single-notebook callers observe no change. Enables slice 4.

### 4. Cross-notebook reduction appends one accepted commit to each notebook

Type: Behavior
Status: planned
Proof: `CURSOR_DEV=true nix develop -c ./backend/gradlew -p backend test --tests '*NotebookGitWebRelationReduceControllerTest*' -Dspring.profiles.active=test --build-cache`
green with the new both-notebook test.

Behavior: Git-backed "Space topics" (relation note, Earth) and Git-backed
"Astronomy" (Moon), both snapshotted, a learner tracker on the relation note
→ reduce → Astronomy's accepted head advanced by one commit (parent =
previous head) whose `Moon.md` contains `[[Space topics:Earth|Earth]]`; Space
topics' head advanced by one commit with the relationship file absent and
nothing under `_trash/`; the learner's tracker is now a property tracker on
Moon (existing rule, observed in the same result).

Implementation: expose `NoteReferenceHandling`'s parse-and-resolve of the
relationship source as `NoteService.resolveRelationshipSource(relationNote,
viewer)` (reduction keeps re-resolving under the lock as today).
`RelationReduceService` resolves the source before locking, calls
`apply(Set.of(relationNotebookId, sourceNotebookId), …)`, runs the existing
reduction and permanent removal inside, then checks the reduced source's
notebook id is in the locked set, otherwise throws `409 CONFLICT` ("The
source note moved to another notebook; retry."). Commit message for both
notebooks: `Reduce relationship note: <title>`. Test support: a titled
variant of `createGitBackedNotebook` so the two notebooks have distinct
names.

### 5. Refused cross-notebook reduction leaves both notebooks unchanged

Type: Behavior
Status: planned
Proof: same test class green with the refusal test.

Behavior: "Astronomy" is owned by another user and not editable by the
viewer → reduce → 400 with the existing message, both accepted heads equal to
their values before, the relationship note still present. No production
change expected; the slice exists to observe the rejection at the Git
boundary.

### 6. Pre-existing drift in the source notebook keeps its per-notebook policy

Type: Behavior
Status: planned
Proof: same test class green with the drift test.

Behavior: "Astronomy" drifted before the action (Moon's content changed
without a snapshot) → reduce → Space topics' head advanced by one commit,
Astronomy's head unchanged, Moon in the database has the property. No
production change expected; the slice guards slice 3's per-notebook
pre-match against silently committing a drifted notebook.

## Current decisions

- Lock order: ascending notebook id; the touched set is known before locking
  and verified under it (Option B, owner 2026-09-17).
- Drift policy is per notebook and unchanged (seed boundary assumption,
  owner-confirmed).
- No frontend, API, or E2E change.
- The moved-source race guard has no automated proof; it is a fail-loudly
  guard under ADR 0006 and is reported as unproved.

## Learnings

- Cross-notebook wiki links use `Notebook:Title` (a slash is a folder path in
  the same notebook), and the qualify rule keeps the visible text, so the
  reduced value is `'[[Space topics:Earth|Earth]]'`. The plan examples were
  corrected; the seed's key example 1 still shows slash notation (fix at
  wrap-up).
- `LockedNotebooks` snapshots hold every note of the notebook (trash is a
  folder), so "snapshot, else repository" only reaches a note outside the
  lock when it lives in another notebook. `edit` refuses that by notebook id;
  reduction could only hit it via a concurrent move of the relationship note,
  so slice 4's moved-note guard must check the relationship note's notebook
  as well as the source's.
