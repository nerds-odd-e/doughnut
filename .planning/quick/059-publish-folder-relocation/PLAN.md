# Publish one folder relocation while retaining identities

## Source and status

Source: [SEED-009 Story 7](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-7).
Status: in progress; slices 1–5 done.

## Goal and scope

The notebook owner moves one folder through the existing local Git publication
flow and sees the same folder and descendant identities at the new location.

Accept one direct-child, single-parent commit from a clean bound owner checkout
on main, without an unfinished Git operation and with matching accepted projection.
The source has its own README in accepted history; every active descendant folder
is represented by tracked content. Move the whole subtree to a different existing,
represented parent or notebook root, keeping the folder name, relative paths,
regular modes, and exact bytes. No other changes. Preserve all identities, private
associations, tracker state, and authored references. Accept head and projection
atomically; retry is unchanged success. Clean clone/pull receives the result;
a later separately published edit updates the same descendant note.

Excluded: source without README, unrepresented empty descendants, folder renaming,
multiple moves, descendant renames/edits, reference rewriting, new parents, merging,
cross-notebook moves, attachments, README authoring, web structural synchronization,
drift repair, structural rebase, and multiple unpublished commits. Existing note
operations retain their meanings, including a last-note move leaving its folder.

## Execution context and current decisions

- `NotebookGitProposalPublisher.publish` already owns a SERIALIZABLE,
  REQUIRES_NEW acceptance transaction and binding lock. Retain that ownership,
  owner authorization, ancestry checks, typed Markdown validation, drift checks,
  bundle storage, and idempotent retry. No new endpoint, schema, or transport.
- Publisher inspects files, then `requireExactOrEmpty`. Exact `FolderRelocation`
  is checked with `requireRepresentedFolderRelocation` (source prefix and dest
  parent via `requireRepresentedFolderPath`) before note classification.
  Missing/unrepresented dest parents name `{destPrefix}/README.md` with the
  existing unrepresented-parent wording and create nothing. Root dest parent
  is null/valid. Exact represented-parent candidates still reserved-README
  until leaf 6. Placement then uses `NotebookGitProposalFolderPlacement`:
  collision (`FOLDER_NAME_CONFLICT`) and self/descendant 400, both contextualized
  as `Cannot move folder to path "{destPrefix}/README.md": …`. Invisible empty
  same-name destinations collide. No merge.
- Use source/destination prefixes and complete relative-path/blob correspondence,
  not independent equal-blob pairing. Require exactly one eligible folder mapping;
  identical note content elsewhere must not affect it. Nested README files belong
  to that subtree; they are not independent folder moves.
- Resolve both folder paths against accepted state. Reuse
  `FolderMoveDestinationRules` and `FolderSiblingNameValidation`; validate before
  mutation. Include invisible existing destination containers in collision checks.
  Folder has no soft-delete field: the seed's reserved-destination wording means
  existing placement rules, not a new folder tombstone or restoration policy.
- Reparent the existing source Folder. Do not call the web relocation orchestration
  that rewrites inbound links. Notes and nested folders keep their associations;
  deleted notes are not restored or processed.
- `NotebookGitStateLoader` captures immutable `ExportFolderRow` values before
  mutation. The proposed-tree comparison must use rows reflecting the new parent;
  do not compare against stale rows or drop the final exact projection check.
- [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
  constrains README ownership, metadata-free trees, typed Markdown, and authored
  links. [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md) permits
  propagation and contextual errors. No Accepted ADR deviation is needed;
  ADR 0002 remains Proposed.
- Existing committed-transaction evidence in
  `NotebookGitProposalRenameRollbackControllerTest` and
  `NotebookGitPublicationAtomicTestSupport` covers the transaction/failure-injection
  pattern. Reuse it for folder-parent rollback. No uncertain DDL, new transaction
  ownership, or separate storage experiment is introduced.

## Refinement assessment

Slice 1 extracted raw inspection; no completed evidence was replaced.
Original leaves 1, 3, 4, 7, and 8 are Ready, with their dependency/proof references
updated below. Original 2 is Refine (recognition, placement, and acceptance beats);
5 is Refine (rollback and retry); 6 is Refine (publication and receipt). None
requires story escalation. The selected scope and exclusions are unchanged.

Interim refusal is useful feedback: before acceptance is enabled, unsupported
folder proposals receive the applicable path/reason and never mutate remote
state. Do not add a validation command, endpoint, feature flag, or protocol.
The remaining generic refusal for otherwise eligible moves is removed in leaf 6.
All final safety checks are wired before that leaf enables publication.

## Ordered slices

Sizing for every leaf: approximately five minutes for the named cohesive change,
its one proof loop, and local cleanup. Existing fixtures and APIs below are the
basis for that hypothesis. Backend suite/E2E runtime may dominate elapsed time;
record actual runtime separately, never use it to excuse implementation overruns.
Each leaf includes its own tests; existing preservation behavior may need only
additional boundary evidence, not gratuitous production changes.

### 1. Separate raw proposal inspection from note eligibility
Type: Structure
Status: done
Proof: Existing tree-shape controller coverage, including README refusal and
ordinary note operations, remains green with unchanged observable behavior.
`CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed (~50s).

Structure: Extract the existing raw safe regular-file walk with paths and blob
IDs, preserving its current caller and errors. Its immediate consumer is leaf 2's
folder-shape refusal. No new general change framework or public test-only exports.
Sizing basis: mechanical extraction of the existing walk, no new acceptance logic.

### 2. Explain why a proposed folder move is not an exact subtree relocation
Type: Behavior
Status: done
Proof: Through publishNotebookGitProposal, a table of partial, mixed, multiple,
renamed, or edited directory proposals receives a path-specific shape refusal;
the binding and projection remain unchanged.
`CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed (~53s).
`NotebookGitProposalFolderRelocationShapeControllerTest` names the breaking
path; exact nested-README candidates still get reserved-README.

Behavior: A proposal changes a folder README but is not one complete unchanged
same-name prefix relocation → publish → explain the unsupported shape. Derive
candidate prefixes from removed/added README paths and require a unique complete
relative-path/blob correspondence, including nested READMEs and unchanged files
left at the source. Retain existing path/mode checks. Valid candidates still reach
the existing unsupported-folder refusal; this leaf enables no mutation. Proposals
without a moved README retain existing note-operation classification.
Sizing basis: one pure correspondence rule consumed at the controller boundary;
use full subtree sets, not combinations of independent equal-blob note pairs.

### 3. Reject a destination parent absent from accepted history
Type: Behavior
Status: done
Proof: Controller proposals to missing and unrepresented parents name the final
Portable path and leave folders and the binding unchanged.
`CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed.
`NotebookGitProposalFolderRelocationDestinationControllerTest` asserts the
canonical missing-parent reason and folder-id stability; unrepresented is the
path delta; represented parents still reserved-README.

Behavior: An exact candidate names an unavailable destination parent → publish →
reject without creating it. Resolve the source Folder and destination parent by
full accepted paths, reusing represented-parent lookup. Root is a valid parent;
README-only parents and ancestors represented by descendant content resolve.
Otherwise eligible candidates remain refused pending leaf 6.
Sizing basis: adapt existing full-path resolution, with one refusal proof loop.

### 4. Reject a folder destination that collides or creates a cycle
Type: Behavior
Status: done
Proof: Controller data variants for an existing same-name destination (including
an invisible empty container) and self/descendant destination retain the original
hierarchy and report the placement reason at the requested path.
`CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed.
`NotebookGitProposalFolderRelocationPlacementControllerTest` covers collision
(canonical), invisible empty (delta), self, and descendant.

Behavior: An exact candidate resolves a parent but violates existing placement
rules → publish → reject without merging or overwriting. Use
FolderSiblingNameValidation and FolderMoveDestinationRules. No new tombstone,
case-normalization, or folder-name policy. Earlier shape refusal may already reject
some self-overlapping trees; those need not reach this validator to be safe.
Sizing basis: wire existing placement rules and contextual errors in one loop.

### 5. Reject a source containing an unrepresented empty descendant
Type: Behavior
Status: done
Proof: Add an empty descendant to an otherwise exact source subtree → controller
publication names that excluded folder; all parents/IDs and accepted head remain.
`CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed.
`NotebookGitProposalFolderRelocationEmptyDescendantControllerTest` names
`Topics/Empty/`; deeper-content descendants still reserved-README.

Behavior: A represented source has an active descendant folder with no tracked
accepted content anywhere below it → publish → refuse the whole move. Compare
existing descendant folder paths with accepted file prefixes, including path
separators. A descendant represented only through deeper content is eligible.
Otherwise eligible candidates still reach the interim refusal.
Sizing basis: one set-membership rule over existing export rows and tree paths.

### 6. Accept the validated folder move by reparenting the same Folder
Type: Behavior
Status: planned
Proof: A controller publication with source README and nested note accepts the
exact proposed tree and retains source/descendant IDs. Parameterized eligible
placements cover root, nested parent, README-only source, and a represented
ancestor without its own README. Assert canonical tree/identity shape once.

Behavior: A candidate passes leaves 2–5 and existing owner/ancestry/Markdown/drift
checks → publish → reparent the existing source and accept that exact authored
commit in the existing transaction. Replace the interim refusal here. Refresh
ExportFolderRow values for the proposed-tree comparison before updating binding;
do not alter descendant entities, contents, or references. Retain all note paths
through the existing classifier, including a last-note move leaving its folder.
Sizing basis: the preceding refusal leaves already perform all eligibility work;
this change is one parent assignment plus the fresh projection check. Existing
transaction/bundle acceptance stays intact. Re-run their refusal proofs here.

### 7. Retain identity-bound data through folder publication
Type: Behavior
Status: planned
Proof: Reuse NotebookGitProposalRelocationPrivateAssociationControllerTest's
fixtures for a learned descendant and inactive tracker; reload tracker schedule,
history, question/conversation associations and an unchanged identical outside
note after publishing. Assert preservation deltas only.

Behavior: An eligible source contains learned notes → publish its move → those
same identities retain their private associations and activation state; an
unchanged identical note keeps its own data. Deleted notes are not restored.
Sizing basis: existing private-association fixture, same publication entry point;
no new copying or mutation should be necessary.

### 8. Preserve authored references after the folder changes location
Type: Behavior
Status: planned
Proof: Reuse the relocation referrer controller pattern with body/property links
inside and outside the source; observe authored bytes unchanged and an old exact
path unresolved using ordinary current-state resolution.

Behavior: Notes refer to the old source path → publish the folder move → links
stay authored even when that path stops resolving. No rewrite or index-repair
policy is introduced.
Sizing basis: one existing reference-resolution proof pattern and one fixture.

### 9. Roll back a folder move when late acceptance fails
Type: Behavior
Status: planned
Proof: Existing late binding-save injection, committed fixture, and fresh read
transaction show original parent, descendant IDs, head, and bundle after failure.

Behavior: An eligible move fails after parent mutation → publication fails →
the entire accepted state remains at its old location. Reset failure injection
in existing cleanup. No catch/retry infrastructure or transaction changes.
Sizing basis: adapt NotebookGitProposalRenameRollbackControllerTest's existing
failure case; one failure and one readback, no resubmission in this leaf.

### 10. Retry an accepted folder proposal without another mutation
Type: Behavior
Status: planned
Proof: Publish an eligible proposal, then submit that accepted head again;
observe unchanged parent, IDs, binding head/bundle/timestamp.

Behavior: A folder proposal is already accepted with matching projection →
retry publication → unchanged success. Retain existing drift rejection on retry.
Sizing basis: reuse the existing accepted-head branch and fixture; one retry loop.

### 11. Publish a local directory move through the installed CLI
Type: Behavior
Status: planned
Proof: Extend the active cli_notebook_clone.feature: installed clone, ordinary
Git directory move/commit, installed publish, then show the same Donut folder/note
at the accepted new path. Use the existing Recipes README fixture.

Behavior: The owner commits an eligible local directory move → installed publish
→ Donut displays the accepted location. Add only a directory move step/task if
existing real-Git rename support cannot handle directories. No receipt in this
scenario. Keep multi-beat proof @wip until green; never commit red acceptance.
Sizing basis: existing installed CLI feature, fixtures, publish, and UI assertions;
one filesystem operation is the only anticipated harness addition.

### 12. Receive the accepted folder location in a clean checkout
Type: Behavior
Status: planned
Proof: In the same active feature, prepare a clean checkout at the old accepted
head; after the move is accepted, installed pull exposes the exact new tree and
retains the old head as an ancestor. Also check fresh clone sees the accepted tree
through existing clone coverage where it already observes arbitrary tree paths.

Behavior: A folder move is accepted elsewhere → pull a clean eligible checkout →
receive its new paths and retained history without publishing. Keep structural
rebase with unpublished content refused under existing interval/readiness rules.
Sizing basis: existing accepted-move fixture plus one ordinary fast-forward loop.

### 13. Edit a descendant after receiving its accepted move
Type: Behavior
Status: planned
Proof: One installed-CLI scenario starts after the accepted move is received,
authors/publishes one separate content commit at the new path, and observes the
same Donut note ID with the changed content.

Behavior: The move is accepted and received → edit and separately publish one
moved note → update that same identity. Do not batch move/edit or expand rebase.
Sizing basis: reuse preceding folder setup and existing single-note edit steps.

### 14. Explain the supported folder-move boundary in CLI guidance
Type: Behavior
Status: planned
Proof: Drive CLI run to observe clone guidance. Keep the installed-guidance
assertion consistent in cli_notebook_clone.feature without adding a new scenario.

Behavior: An owner reads clone guidance → they learn the accepted README,
whole-subtree, existing-parent, unchanged-link and publish-before-edit boundaries.
Replace the blanket unsupported-folder statement in nonInteractiveCli.ts; retain
all other exclusions. Path-specific refusal originates in leaves 2–5 and uses
existing submission/error rendering, including retaining the rejected local work.
Sizing basis: one existing message and its assertions, no new command or document.

## Contract-to-proof map

| Promise | Owning leaves and observations |
| --- | --- |
| Unique exact prefix mapping, same name, modes, bytes, no other changes | 2 refusal and 6 accepted-tree proof; nested READMEs form one subtree |
| Root/nested destination; README-only source; descendant-only representation | 3/5 eligibility and 6 accepted placement variants |
| Missing/unrepresented parent, collision, cycle, invisible descendants refused | 3–5 path-specific controller refusals with unchanged projection/binding |
| Source and descendant identities/hierarchy preserved; no recreation | 6 persisted IDs and exact tree; 7 private-association readback |
| Private data, tracker activation/schedules, unchanged identical copies | 7 persisted association deltas |
| Authored README/note bytes and references unchanged; ordinary resolution | 6 exact tree and 8 body/property reference observations |
| Atomic acceptance and unchanged retry | 9 fresh-transaction rollback; 10 accepted retry |
| Installed publication; receipt and history; subsequent same-note edit | 11, 12, 13 respectively |
| Scope/error guidance and rejected local work retained | 2–5 errors; 14 CLI copy; existing submission-rejection checkout assertions retained in 11 |
| Owner, clean main, no Git operation, direct child, stale/drift rejection | 6 retains shared gates; 11 existing ancestry/readiness/binding/concurrency/projection-drift regressions |
| Existing add/edit/delete/note relocation; last-note move keeps container | 1/6 tree-shape and relocation-container controller regressions |
| Structural divergence stays refused | 12 existing accepted-interval and pull/rebased-rejection coverage |
| No schema, metadata, API, link, or deleted-folder policy expansion | 6 exact-tree proof and scoped review; 14 unchanged commands |

Original ownership is preserved: old 1 → 1; old 2 → 2–6; old 3 → 7;
old 4 → 8; old 5 → 9/10; old 6 → 11/12; old 7 → 13; old 8 → 14.
The old failure/resubmit/retry scenario's recovery claim is covered by rollback
in 9, ordinary acceptance in 6, and accepted retry in 10; no separate recovery
feature is promised. No completed evidence exists to migrate.

## Verification and delivery

- Backend changes: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
  (all backend unit tests, per backend rule). Existing relevant controller suites
  named above supply regression proof; new tests use the same stable boundary.
- CLI changes: `CURSOR_DEV=true nix develop -c pnpm cli:test`.
- Installed workflow: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_notebook_clone.feature`.
- No manual or mutation testing requested. No tests run during this planning turn.
- Execute via execute-plan when authorized: Jidoka, fresh post-change-refactor
  agent, API generation only if unexpectedly required, one coordinator formatting
  pass, update plan, commit, push. Preserve concurrent unrelated working changes.
- At five minutes scrutinize the leaf; at ten minutes finer-decompose unless an
  explicitly recorded test-runtime exception applies. Story-boundary discoveries
  return to its seed; do not expand this story to make implementation easier.
- Once fully delivered, reduce source refinement to Goal/Scope and remove spent
  plan history according to planning.mdc.

## Readiness and learnings

Slices 1–5 done. Remaining leaves are target-sized hypotheses, not time guarantees.
No sizing exception is pre-approved; record actual test/external wait runtime
separately. If integration in leaf 6 fails to converge, refine this same plan
rather than bypass a safety gate or expand the story.

Leaf 5 learning: empty descendants compared as `ExportFolderRow` paths (trailing
slash) vs accepted file prefixes. Error:
`Descendant folder "{path}" is not represented in accepted Portable content; every active descendant must have tracked content before the folder can be moved.`
Publisher `requireEligibleFolderRelocation` owns represented + empty-descendant +
placement; leaf 6 should reparent after these refusals instead of falling through
to reserved-README.

CI observer delivered a 2026-09-05 E2E failure on `1d846feb` (`cli_notebook_clone`).
That SHA is not this execution's push; later `main` CI including origin `63dbba7f74`
succeeded. Disposition: superseded historical failure, no repair.

Refinement learning: useful path-specific rejection allows eligibility work to
land safely before acceptance; the old plan unnecessarily bundled these outcomes.
Rollback/retry and publication/receipt are independently observable proof loops
and now have separate leaves. No selected-story decision changed.
