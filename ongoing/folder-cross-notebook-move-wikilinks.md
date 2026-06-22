# Move a folder to another notebook with wiki links kept correct

## Goal

Let a user move an entire folder (its notes + subfolders) into a **different**
notebook, and keep both **incoming** and **outgoing** wiki links resolving to the
same notes they did before. Along the way, complete the same link correctness for
the existing single-note cross-notebook moves, which the folder move builds on.

## Confirmed requirements (decided with developer)

- **Scope:** implement folder-across-notebook move **and** fix the underlying
  single-note cross-notebook link handling it depends on.
- **Outgoing links to notes that stay behind** (target keeps living in the old
  notebook): qualify them to the **old** notebook so they keep resolving, e.g.
  `[[X]]` → `[[OldNb:X|X]]`. Already-qualified links (`[[C:Y]]`) are left alone.
- **Links between notes that move together** (both inside the moved subtree):
  keep them **relative/unqualified** so they keep resolving inside the destination
  notebook. No rewrite needed for these.
- **Name conflicts at the destination:** reuse the existing same-notebook folder
  move conflict/merge rules (409 by default, merge on confirm) plus the existing
  soft-deleted-title placement rules.

## Current state (from code analysis)

- `NotebookController.moveFolder` → `FolderRelocationService.moveFolder` is
  **same-notebook only** (validates parent in same notebook; conflict/merge keyed
  on `notebook.getId()`). No cross-notebook folder move exists.
- Single-note moves:
  - `move-to-notebook-root/{sourceNote}/{targetNotebook}` rewrites **inbound**
    referrers (`WikiTitleCacheService.rewriteInboundWikiLinksForNotebookMove`) but
    does **not** touch the moved note's **outgoing** links — a correctness bug.
  - `moveNoteToFolder` cross-notebook updates neither inbound nor outgoing links —
    a gap.
- Outgoing links are **never** rewritten on any move today.
- Link rewrite machinery to reuse: `WikiLinkMarkdown` (`newInnerForKeepNotebookMove`,
  `replaceWikiLinksMatchingTrimmedInner`, `splitInner`), `WikiLinkTargetReference`
  (qualified vs unqualified parsing), `WikiTitleCacheService.rewriteInboundWikiLinks`
  + `refreshForNote` (also refreshes the note property index).
- Frontend folder move dialog (`FolderSearchForm.vue`) is scoped to one
  `notebookId`; choosing a **destination notebook** is new UI.

## Central concept: the moved set S

A move relocates a set **S** of notes from old notebook A to new notebook B.
- Single-note move: `S = { note }`.
- Folder move: `S =` all notes in the moved folder subtree.

"Correct" links after the move:
1. **Inbound** referrer R → target T∈S:
   - R∉S: re-qualify R's link to B (so it still resolves).
   - R∈S: leave unchanged (both moved; relative link still resolves in B).
2. **Outgoing** note N∈S → target T:
   - T∉S (stayed in A): qualify N's **unqualified** link to A.
   - T∈S (co-moved): leave relative/unqualified (resolves in B).
   - Already-qualified outgoing links: never changed (they pin their notebook).

For a single-note move, S has one element, so no co-moved targets/referrers exist —
the degenerate, simplest case. Folder move generalizes S to many notes; Phases 8
and 9 add set-awareness to the link rewrites.

---

## Phases

Sizing rule for this plan: each phase should be small enough for one focused
commit, targeted tests, push, and deploy gate before the next phase. If a phase
starts to require both a new E2E story and several unrelated backend/UI changes,
split it again using the E2E-led sub-phase pattern from the planning skill.

### Phase 1 — Outgoing wiki link qualifier exists (Structure) — done

Prepare the immediate next behavior without changing any move behavior.

- Precondition: markdown contains unqualified wiki links (`[[X]]`, `[[X|label]]`)
  and already-qualified links (`[[C:Y]]`, `[[C:Y|label]]`).
- Trigger: call a pure link-inner transformation with source notebook name A.
- Postcondition: unqualified inners become qualified to A while preserving visible
  text; already-qualified inners are unchanged.
- Work:
  - Add a pure helper in `WikiLinkMarkdown` (or a small sibling) for "qualify
    unqualified outgoing link inner with this notebook".
  - Keep the helper independent of move services so edge cases stay black-box and
    easy to test.
- Tests: focused `WikiLinkMarkdown`/algorithm unit tests for plain links,
  display text, already-qualified links, whitespace/blank handling.
- Verification: targeted unit test plus existing wiki-link unit tests.
- Done: added `WikiLinkMarkdown.newInnerForQualifyUnqualifiedOutgoingLink`,
  which qualifies unqualified inners to the source notebook while preserving
  visible text, leaves already-qualified inners unchanged, and leaves blank
  inners unchanged. Phase 2 can use this helper when rewriting a moved note's
  outgoing links.

### Phase 2 — Note root move preserves outgoing links (Behavior) — done

The root-move path already fixes inbound links; add the missing outgoing half.

- Precondition: note N in notebook A links out via `[[X]]` and `[[C:Y]]`.
- Trigger: move N to notebook B's root.
- Postcondition: N's `[[X]]` becomes `[[A:X|X]]` and still opens the original A
  target; `[[C:Y]]` is unchanged; existing inbound rewriting still works.
- Work:
  - Add a `WikiTitleCacheService` method that rewrites a moved note's own outgoing
    links using the Phase 1 helper and refreshes its cache/property index.
  - Wire it into `RelationController.moveNoteToNotebookRootInNotebook` only when
    the notebook actually changes.
- Tests: controller coverage in `RelationControllerTests`; E2E scenario in
  `note_topology/link.feature` for "move note across notebooks, then outgoing link
  still opens old-notebook target".
- Verification: targeted backend test and the touched Cypress feature.
- Done: `WikiTitleCacheService.rewriteOutgoingWikiLinksForNotebookMove` now
  qualifies a moved note's unqualified outgoing links back to the source
  notebook and refreshes its cache/property index. The root notebook move calls it
  only for cross-notebook moves after the existing inbound rewrite, so existing
  inbound rewriting remains covered. Phase 3 can reuse the same method for
  note-to-folder cross-notebook moves.

### Phase 3 — Note-to-folder cross-notebook move preserves links (Behavior) — done

Close the other single-note move gap before building folder moves on top of it.

- Precondition: note N in notebook A is referenced by R via `[[N]]` and links out
  via `[[X]]`; folder F lives in notebook B.
- Trigger: move N into F.
- Postcondition: R's link re-qualifies to B; N's `[[X]]` qualifies to A;
  same-notebook moves remain unchanged.
- Work:
  - In `RelationController.moveNoteToFolder`, capture source notebook before the
    move and detect a cross-notebook target folder.
  - Reuse the root-move inbound rewrite plus the Phase 2 outgoing rewrite.
  - Tighten the outgoing rewrite so no-op moves do not create content/timestamp
    churn for notes with no outgoing rewrites.
  - Prefer one shared cross-notebook rewrite helper for the root and folder move
    controller paths if that can be done without widening the phase.
- Tests: controller coverage in `RelationControllerTests`; add/extend the
  capability scenario in `note_topology/link.feature`. Add focused coverage for
  no-op outgoing rewrites preserving empty/null content if production behavior is
  changed for that review finding.
- Milestone: **all single-note cross-notebook moves are link-correct.**
- Done: extracted `WikiLinkRewriteService` with
  `rewriteWikiLinksForCrossNotebookMove` shared by root and folder cross-notebook
  moves; outgoing rewrite skips persist when content is unchanged and preserves
  `null` content. `RelationController.moveNoteToFolder` rewrites inbound and
  outgoing links when the target folder is in another notebook. Controller coverage
  in `RelationControllerMoveNoteToFolderTests`; E2E in `note_topology/link.feature`.
  Phase 4 can build folder cross-notebook moves on top of this link-correct
  single-note foundation.

### Phase 4 — Folder subtree can move to another notebook root through the API (Behavior) — done

Deliver the smallest backend/integrator slice of the folder capability. Do not
expose it in the UI yet.

- Precondition: folder F in notebook A has notes and subfolders; notebook B has no
  conflicting root folder named F; no boundary-crossing wiki links are part of the
  scenario.
- Trigger: call the folder move API with destination notebook B and no parent
  folder.
- Postcondition: F, all descendant folders, and all notes in the subtree now belong
  to B at B's root; intra-subtree wiki links still resolve because they remain
  unqualified inside the new notebook.
- Work:
  - Extend `FolderMoveRequest` with an optional destination notebook id for root
    moves; keep existing same-notebook callers working when it is absent.
  - Authorize both the source notebook and target notebook.
  - Add a cross-notebook branch in `FolderRelocationService` that reassigns the
    folder subtree and contained notes to the destination notebook.
  - Regenerate the TypeScript API client.
- Tests: `NotebookFolderManagementControllerTest` for the API behavior; focused
  assertion that descendants and notes changed notebook.
- Verification: targeted backend test plus API generation check.
- Interim: boundary-crossing links are not promised by this backend-only slice.
- Done: `FolderMoveRequest.destinationNotebookId` enables cross-notebook root moves;
  `NotebookController.moveFolder` authorizes source and destination notebooks;
  `FolderRelocationService.moveFolderToAnotherNotebookRoot` reassigns the folder
  subtree and contained notes to the destination notebook. Controller coverage in
  `NotebookFolderManagementControllerTest`. Phase 5 can generalize the destination
  to a target parent folder in another notebook.

### Phase 5 — Folder subtree can move into a folder in another notebook (Behavior) — done

Generalize the backend destination from notebook root to a target parent folder.

- Precondition: folder F is in notebook A; destination parent P is in notebook B;
  P has no conflicting child named F.
- Trigger: call the folder move API with P as the new parent.
- Postcondition: F becomes a child of P; F's entire subtree and notes belong to B;
  moving into itself or its descendants is still rejected.
- Work:
  - Let `newParentFolderId` point at a folder in the destination notebook when a
    destination notebook is specified or can be inferred.
  - Keep existing "parent folder not in notebook" behavior for legacy
    same-notebook requests where no destination notebook is supplied.
  - Preserve `FolderMoveDestinationRules.requireNotMovingIntoSelfOrDescendant`.
- Tests: `NotebookFolderManagementControllerTest` for cross-notebook target
  parent, unauthorized target parent, and self/descendant rejection.
- Verification: targeted backend test.
- Done: `moveFolderToAnotherNotebook` accepts `newParentFolderId` in the destination
  notebook when `destinationNotebookId` is set; self/descendant validation runs
  before parent-in-notebook checks. Legacy same-notebook moves without
  `destinationNotebookId` still reject a parent in another notebook. Controller
  coverage in `NotebookFolderManagementControllerTest`. Phase 6 can add destination
  conflict reporting for cross-notebook moves.

### Phase 6 — Cross-notebook folder move reports destination conflicts (Behavior) — done

Make the no-merge conflict behavior match existing same-notebook folder moves.

- Precondition: destination root or parent folder already has a child folder with
  the same name as F, or moving notes would violate a soft-deleted title placement
  rule.
- Trigger: call the cross-notebook folder move API without merge confirmation.
- Postcondition: the move returns 409 and leaves the source subtree unchanged.
- Work:
  - Thread the destination notebook and destination parent through existing sibling
    conflict checks.
  - Apply `noteTitlePlacementRules` against the destination notebook/folder before
    committing note placement changes.
- Tests: `NotebookFolderManagementControllerTest` conflict cases for root,
  target-folder, and soft-deleted-title placement.
- Verification: targeted backend test.
- Done: `moveFolderToAnotherNotebook` validates destination sibling folder names via
  `folderSiblingNameValidation` and soft-deleted note title placement via
  `requireNoSoftDeletedTitlesInSubtree` before any mutations. Controller coverage in
  `NotebookFolderManagementControllerTest`. Phase 7 can add merge-on-confirm for
  cross-notebook moves.

### Phase 7 — Cross-notebook folder move can merge on confirmation (Behavior) — done

Complete parity with same-notebook merge semantics for the backend API.

- Precondition: destination has an existing same-name folder; request has
  `merge=true`.
- Trigger: call the cross-notebook folder move API.
- Postcondition: source children and notes merge into the destination folder using
  existing recursive merge rules; all moved descendants and notes belong to the
  destination notebook; the source folder row is removed.
- Work:
  - Update `mergeFolderInto` or its caller so cross-notebook merges reassign
    notebooks consistently for moved folders and notes.
  - Preserve existing same-notebook merge behavior.
- Tests: `NotebookFolderManagementControllerTest` recursive merge case with a
  cross-notebook source and destination.
- Verification: targeted backend test.
- Done: `moveFolderToAnotherNotebook` handles `merge=true` with soft-deleted-title
  validation before merge; `mergeFolderInto` reassigns notebooks via
  `reassignFolderSubtreeToNotebook` when source and target differ; same-notebook
  merge unchanged. Controller coverage in
  `NotebookFolderManagementControllerTest`. Phase 8 can add inbound boundary link
  rewrites for folder moves.

### Phase 8 — Folder move rewrites inbound boundary links (Behavior) — done

Start moved-set-aware link correctness with inbound links into the moved subtree.

- Precondition: outside note R links to inside note T via `[[T]]`; another note
  inside F also links to T via `[[T]]`.
- Trigger: move folder F from notebook A to notebook B through the backend API.
- Postcondition: R's link re-qualifies to B; the inside referrer's link stays
  unqualified because both notes moved together.
- Work:
  - Collect S = all notes in the moved subtree before or during the folder move.
  - Extend inbound rewrite logic with an excluded-referrer set, immediately used
    by folder moves.
  - Reuse the existing single-note inbound rewrite unchanged for note moves.
- Tests: controller coverage for outside referrer rewritten and inside referrer
  skipped. Do not add Cypress here unless the UI has already been exposed; the
  backend controller is the external API surface for this phase.
- Verification: targeted backend test.
- Done: `WikiLinkRewriteService.rewriteInboundWikiLinksForFolderNotebookMove`
  rewrites inbound links for every note in moved set S with co-moved referrers
  excluded via `excludedReferrerIds`. `FolderRelocationService` collects subtree
  note IDs before cross-notebook moves (including merge) and calls the rewrite
  after reassignment. Single-note moves still use the unchanged
  `rewriteInboundWikiLinksForNotebookMove` entry point. Controller coverage in
  `NotebookFolderManagementControllerTest`. Phase 9 can add outgoing boundary
  link rewrites for folder moves.

### Phase 9 — Folder move rewrites outgoing boundary links (Behavior) — done

Complete moved-set-aware link correctness for links leaving the moved subtree.

- Precondition: inside note N links via `[[Outside]]` to a note that stays in A and
  via `[[Peer]]` to a co-moved note; it also has an already-qualified link.
- Trigger: move folder F from A to B through the backend API.
- Postcondition: N's `[[Outside]]` qualifies to A; `[[Peer]]` remains
  unqualified; already-qualified links are unchanged.
- Work:
  - Extend the outgoing rewrite from Phase 2 so it can skip unqualified links whose
    resolved target is in S.
  - Refresh caches/property indexes for every rewritten moved note.
- Tests: controller coverage for outside target, co-moved target, and
  already-qualified link. Do not add Cypress here unless the UI has already been
  exposed; cover the user path in the later UI phases.
- Verification: targeted backend test.
- Milestone: **backend folder cross-notebook move is fully link-correct.**
- Done: `WikiLinkRewriteService.rewriteOutgoingWikiLinksForFolderNotebookMove`
  qualifies unqualified outgoing links to the source notebook while skipping
  co-moved targets via `coMovedTargetResolvesFrom`. `FolderRelocationService`
  calls `rewriteWikiLinksForFolderMove` for both inbound and outgoing rewrites
  after cross-notebook moves (including merge). Controller coverage in
  `NotebookFolderManagementControllerTest`. Phase 10 can expose cross-notebook
  folder move in the UI.

### Phase 10 — UI can move a folder to another notebook root (Behavior) — done

Expose the safe root-destination workflow after backend link correctness exists.

- Precondition: user is viewing folder F in notebook A and has access to notebook B.
- Trigger: open "Move folder", choose notebook B, leave destination folder empty,
  confirm.
- Postcondition: F appears at B's root; the user is navigated/refreshed to the
  moved folder state; link-correctness behavior from Phases 8-9 is observable from
  the UI.
- Work:
  - Add destination notebook selection to the folder move dialog while preserving
    the existing same-notebook default.
  - Send the generated API request shape from `FolderPage.vue`.
  - Keep `FolderSearchForm.vue`/`FolderSelector.vue` names and tests capability-
    based, not phase-based.
- Tests: frontend unit tests in `FolderPage.spec.ts`; Cypress scenario in
  `folder_organization/folder_organization.feature`.
- Verification: targeted frontend test and touched Cypress feature.
- Done: `FolderPage.vue` loads destination notebooks from `myNotebooks`, defaults
  to the source notebook, hides the folder picker for cross-notebook root moves,
  sends `destinationNotebookId` on submit, and navigates to the moved folder in
  the destination notebook. Unit coverage in `FolderPage.spec.ts`; E2E in
  `folder_organization.feature` verifies inbound/outgoing boundary wiki links after
  a cross-notebook root move. Phase 11 can add non-root destination folders and
  merge confirmation for cross-notebook moves.

### Phase 11 — UI can move a folder into another notebook's folder and merge conflicts (Behavior) — done

Finish the cross-notebook folder UI by supporting non-root destinations and
existing conflict confirmation.

- Precondition: user chooses notebook B, selects destination parent P, and the
  destination may or may not already have a same-name folder.
- Trigger: confirm the move; if a 409 conflict appears, confirm merge.
- Postcondition: F moves under P, or merges into the existing same-name folder
  when confirmed; inline conflict behavior remains consistent with same-notebook
  moves.
- Work:
  - Let the folder picker/search scope switch to the selected destination notebook.
  - Reuse the existing 409/merge confirmation flow with the cross-notebook request.
  - Preserve same-notebook move behavior and tests.
- Tests: `FolderPage.spec.ts` for request shape and conflict retry; Cypress
  scenario in `folder_organization/folder_organization.feature`.
- Verification: targeted frontend test and touched Cypress feature.
- Milestone: **folder cross-notebook move is user-facing and fully link-correct.**
- Done: `FolderPage.vue` scopes `FolderSelector` to the destination notebook for
  cross-notebook moves (with `:key` remount on notebook change), sends
  `newParentFolderId` with `destinationNotebookId`, and reuses the existing 409
  merge confirmation. Unit coverage in `FolderPage.spec.ts`; E2E in
  `folder_organization.feature` for non-root destination and cross-notebook merge.

**Plan complete.** Folder cross-notebook move is fully link-correct end-to-end.

## Test ownership (named by capability, not phase)

- Wiki link behavior: `note_topology/link.feature`,
  `WikiLinkMarkdown`/algorithm unit tests, `RelationControllerTests`.
- Folder organization behavior: `folder_organization/folder_organization.feature`,
  `NotebookFolderManagementControllerTest`.

## Suggested focused commands

- Backend controller/unit: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
- Frontend single file: `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/FolderPage.spec.ts`
- E2E touched folder feature: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/folder_organization/folder_organization.feature`
- E2E touched wiki-link feature: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/note_topology/link.feature`
- API regeneration after controller/DTO changes: `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`
- Diff whitespace: `scripts/check_diff_whitespace.sh`

## Open follow-ups (not in scope unless raised)

- Bulk performance for very large folder subtrees (per-note rewrite loop).
- Undo support for cross-notebook folder moves.
