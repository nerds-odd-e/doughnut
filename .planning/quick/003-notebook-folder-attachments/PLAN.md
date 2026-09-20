# Keep attachments inside notebook folders through local and web changes

Status: **awaiting story refinement — not ready for slice-plan refinement or execution**.
Work item: **SEED-035#story-9**.
Source: [mapped story](../../seeds/SEED-035-ai-workspace-supporting-files.md#story-9).
Depends on [root attachment continuity](../002-notebook-attachment-continuity/PLAN.md)
(SEED-035#story-6). No implementation or completed evidence is carried over.

## Mapped outcome and boundaries

Owners organize supporting files inside notebook folders, publish local changes,
continue existing web folder work, and recover the complete folder tree locally.
Attachment-only folders count as content. Existing move, rename, trash, recovery,
dissolve/merge and permanent folder deletion account for their files, without
silent loss or overwrite. An attachment remains independent of a referring note.

This plan receives the nested-file and containment promises from the original
15-slice plan. It does not add new web file controls, image presentation/conversion,
reference rewriting, AI behavior or cross-notebook publication integration.
The original scope is retained across these two stories; the parent is not marked
done. Root publication, exact byte storage and transport are implemented once by
story 6 and reused here, not rebuilt as folder-specific services.

## Resume requirement

Run `dough-story-refinement` on story 9 to settle its goal, scope and key examples,
especially merge collisions, empty-folder outcomes and existing folder operations.
Realign this plan to that understanding before `dough-slice-plan-refinement` or
execution. The following are **provisional mapped inputs**, not dispatchable leaves;
old slice estimates and readiness do not carry across the split.

## Architecture carried forward

Follow the existing [North Star](../../NORTH-STAR.md),
[domain vocabulary](../../../docs/adrs/0001-ubiquitous-language.md),
[publication decision](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md),
[format decision](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md),
and [synchronization contract](../../../docs/notebook-git-synchronization.md).

Extend the same Attachment projection with folder containment, following the
project's root/folder ownership convention. Remove story 6's interim nested-file
admission restriction when this entire containment outcome is safe. Do not introduce
separate root and folder attachment types, content stores or publication paths.

Existing `FolderSubtree` owns rehome/merge/deletion; folder materialization and
`NotebookGitProposalAcceptance` own the tip projection. Use those owners and the
shared snapshot instead of Git overlays or per-endpoint file-copy patches.
Keep complete raw subtree evidence for folder relocation; exclude attachments
only from ordinary-note identity correspondence. Unchanged note identities and
learning data remain protected. Root files stay outside unrelated folder mutations.

## Provisional mapped inputs

### 1. Publish files into attachment-only folders

Mapped from original slices 2, 3, 5 and the nested independence/atomicity promises
of 8 and 14. Add folder placement to the same projection, reuse materialization,
and count attachments for `.keep`. Publish a binary file into a new nested folder,
web-edit or remove its referring note, and recover unchanged file bytes. No invented
Readmes. Observe final projection and accepted tree; preserve atomic invalid-tip
refusal and distinguish accepted-state drift from proposed-state equality.
Needs subdivision during refinement: containment Structure and its first Behavior.

### 2. Publish nested file changes with existing note/folder correspondence

Mapped from original slices 6 and 7. Nested file edit/move/removal and accumulated
ranges use final-state attachment reconciliation. Include a supported exact
folder relocation with notes and files: the files reach their intended paths,
while notes retain private identities. Identical attachment bytes do not create
ambiguous note correspondence. A last-file removal must follow agreed canonical
empty-folder behavior. Observe actual downloaded history and projected identities.

### 3. Carry files through existing web folder placement

Mapped from original slice 9. Rename, move, trash and recover a containing folder;
file bytes follow its location. Exercise an attachment-only folder. Reuse existing
placement operations and accepted transaction; inspect final Git paths/bytes and
retain current note/referrer assertions. Split operations if they need different
implementation mechanisms rather than assuming one five-minute change.

### 4. Rehome files during dissolve and merge without overwriting

Mapped from original slice 10. Rehome direct files and recursively merged contents
through `FolderSubtree`. Existing one-content-value-per-path and no-silent-overwrite
constraints remain. The original plan proposed atomic refusal on destination
filename collision, not suffixing or deduplication; confirm the precise examples
during refinement. Observe both successful rehome and unchanged state on refusal.
Recursive merge was low-confidence before the split and remains so here.

### 5. Remove contained files during permanent folder deletion

Mapped from original slice 11. Permanent removal of a trashed folder removes its
contained files from the new accepted tree; original history remains readable.
Use complete FK-closure fixtures and the existing accepted operation. Prove
rollback for the changed dependent projection, not just a returned success.

### 6. Include nested files in existing complete-snapshot outputs

Mapped from original slices 12 and 13. Extend root-story evidence to nested ZIP
entries and existing authorized history reset. Both consume the same snapshot,
but they are distinct public observations and must become separate leaves during
refinement if implementation is required. Populate preconditions by publication;
do not seed the resulting projection and call that admission proof.

### 7. Recover the organized notebook through the installed CLI

Mapped from original slice 15. Publish nested text/binary files with native Git,
perform a representative web folder operation, and pull a second clean checkout.
Observe exact paths, bytes, clean worktree and ancestry. Root-story byte fixtures
and transport are reused; no new rebase algorithm or mocked acceptance.

## Evidence, priority and safety

The [original-to-new mapping](../002-notebook-attachment-continuity/PLAN.md#redistribution-of-the-original-15-slices)
accounts for all 15 original slices. This plan owns only their nested extensions;
the root story owns shared mechanics and its final-state proof. No product tests
were run and no slices are done.

Queue after the owner's second-priority guidance-folder assimilation item and
before nested web browsing. It earns this position by letting users retain their
directory organization and safely use existing folder operations. It does not
inherit immediate second place from the original story.

Do not enable nested-file acceptance in an independently delivered leaf that can
lose files through currently exposed folder operations. Refinement must resolve
that safe activation boundary and realistic sizing; the hold cannot be removed
by copying the original plan's readiness. Root continuity remains usable if this
story is deferred.

Verification will reuse full backend suites, `backend:verify` plus ERD regeneration
for schema, and the existing installed-CLI E2E spec from the first plan. Exact
leaf commands and proof ownership are assigned after story refinement. Follow
the existing execution lifecycle only after separate execution authorization.
