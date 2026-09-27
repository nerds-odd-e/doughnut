# Server and CLI classify notebook paths the same way

## Source

- Story: [SEED-009#story-48](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-48)
- **Identity:** SEED-009#story-48
- Found by the closing review of the local AI notebook effort (SEED-048#story-1),
  report at `ca15b7d7c1:.planning/slice-plans/016-review-and-close-local-ai-notebook-effort/report.html#arch-format`.
- Direction: [North Star — One format boundary](../../NORTH-STAR.md#one-format-boundary)
  (one classification owner maps the tree; invalid Markdown is rejected, never
  treated as an attachment); ADR 0004 §Validation; clear refusals over
  misleading conflicts (ADR 0006).

## Goal and scope

A notebook owner editing a checkout with an AI IDE or Obsidian gets one answer
about what each path is, from one owner on the server. A leftover empty-folder
marker or a `.md` extension in another letter case is refused with a message
naming the path and what to do. The CLI keeps no copy of the rules.

Included: one `PortablePathKind` classifier in `services/notebookGit` that every
tree walker uses; refusal of a `.keep` beside other content or with content;
refusal of added or changed `*.MD`-style paths; the CLI picks LFS uploads by
parsing changed blobs as pointers; docs updated to match.

Excluded (see the seed): dot-file policy, case-only name clashes on publish,
web picture names that look like Markdown, one-pass Markdown validation,
frontmatter edit owner, pull's final-newline rule.

## Starting facts (checked 2026-09-27 at `ecda05ba05`)

- Loose predicates: `NotebookGitProposalTreeShape.java:142-159`
  (`isEmptyFolderMarker` = `endsWith("/.keep")`, `isAttachment`,
  `carriesPortableContent`), used by LFS admission
  (`NotebookGitAttachmentSizeAdmission`), attachment projection
  (`NotebookGitProposalAcceptance.projectAttachments`) and initial publication
  (`NotebookGitProposalPublisher:113-120`).
- Inline copies: `NotebookGitProposalNoteCorrespondence.java:125` and
  `NotebookGitProposalMarkdownFormat.java:61` (`endsWith(".md")`);
  `NotebookGitProposalFolderMaterialization.java:90` (`endsWith("/README.md")`);
  `NotebookGitProposalTreeInspection.java:111-113`,
  `NotebookGitProposalFolderShape.java:257-261`,
  `NotebookGitProposalDocumentApplication.java:49-58` (`"README.md"` role);
  test helper `NotebookGitControllerTestBase.isAttachment` (`:109-115`).
- Metadata definition stays `NotebookGitAttributes.isMetadataPath`.
- CLI copy: `cli/src/commands/notebook/notebookPublishLfsSelection.ts:6-13`
  skips `.md`, any `.keep` (root included) and `.gitattributes`, then refuses a
  non-pointer blob itself.
- **Probe (2026-09-27, throwaway test, deleted):** empty folder `Chemistry`
  accepted as `Chemistry/.keep`; proposal tree `Chemistry/.keep` +
  `Chemistry/Atoms.md` → `409 CONFLICT "The notebook's current Portable content
  differs from accepted main; refresh the checkout before publishing."` from
  `NotebookGitProjection.requireMatchingAcceptedTree` via
  `NotebookGitProposalAcceptance.requireMatchingProposedTree`. The same proposal
  without `.keep` is accepted. Note: `proposalBundleBytes(binding, files)`
  builds the tree from the given files only, so a test must list the leftover
  `.keep` explicitly.

## Outside-in proof

Backend: `NotebookController.publishNotebookGitProposal` controller tests
(`NotebookGitControllerTestBase`). CLI: vitest publish tests with the stubbed
server (`notebookPublish.lfs*.test.ts`). The classifier's pure contract:
`NotebookGitAttachmentClassificationTest`, renamed to fit the classifier.

| Seed example | Slice |
| --- | --- |
| 1. leftover `Chemistry/.keep` beside `Chemistry/Atoms.md` → 400 naming `Chemistry/.keep`, asks to delete it; binding unchanged | 2 |
| 1b. non-empty `Empty/.keep` alone → same refusal | 2 |
| 2. added `Physics/Forces.MD` → 400 asking to rename to `.md`; unchanged accepted `x.MD` file still publishes | 3 |
| 3. empty root `.keep` → root attachment, no CLI error | 1 (classifier contract), 4 (CLI) |
| 4. raw `diagram.png` → CLI submits, server refuses with its LFS message | 4 |
| 5. `Empty/.keep` alone still creates folder `Empty` | existing `NotebookGitProposalInitialMixedTreeControllerTest` |

## Slices

### 1. One path-kind owner for markers, Markdown, files and metadata

Type: Structure
Status: done
Accepted proof: `pnpm backend:test:worktree --tests '*NotebookGit*'` (459 pass) and
`--tests '*PortablePathKindTest'` (12 pass; the first pattern does not select it).
`PortablePathKind.carriesPortableContent()` replaced the tree-shape predicate.
Proof: existing `services/notebookGit` unit tests and `NotebookGitProposal*`,
`NotebookGitAttachment*` controller tests stay green; the classification unit
test covers each kind, including root `.keep` → attachment and
`sub/.keep` → marker.

Change: add `PortablePathKind` with `static PortablePathKind of(String path)`
returning `MARKDOWN`, `EMPTY_FOLDER_MARKER`, `ATTACHMENT` or `METADATA`
(metadata by `NotebookGitAttributes.isMetadataPath`). Replace
`NotebookGitProposalTreeShape`'s predicates and the `.md` checks in
`NoteCorrespondence` and `MarkdownFormat`; the test helper's `isAttachment`
calls the classifier. No behavior changes.
Enables: slice 2 puts the marker rule in this one owner.

### 2. A leftover empty-folder marker is refused by name

Type: Behavior
Status: planned
Proof: new controller test for examples 1 and 1b fails first (409 drift
conflict for 1), then passes with
`assertProposalRejectedWithoutMutatingBinding(..., BAD_REQUEST)` and a reason
containing the path and "delete"; example 5's existing test stays green.

Behavior: a proposal whose tree holds a `.keep` below the root beside other
entries of its folder, or with content → publish → 400 naming that `.keep`
and asking to delete it; nothing changes.

Change: the check runs on the proposed tree before any projection drift check,
on both the ordinary and folder-relocation paths of
`NotebookGitProposalPublisher`. The classifier owns what a valid marker is.
Update `docs/notebook-git-attachments.md` (the `.keep` paragraph near line 80).

### 3. A `.md` extension in another letter case is refused

Type: Behavior
Status: planned
Proof: new cases `Forces.MD`, `Physics/Notes.Md` and `README.MD` in
`NotebookGitProposalAdditionValidationControllerTest.invalidAdditions`, each
failing first (accepted as a file today) and then refused with a reason
containing the path and "rename"; a new case where an unchanged accepted file
`x.MD` stays and another note changes still publishes.

Behavior: a publish that adds or changes a path whose extension is `.md` in
another letter case → 400 naming the path and asking to rename it to `.md`;
unchanged accepted content is not judged.

Change: the classifier recognises the mis-cased Markdown extension; admission
refuses it for changed paths only. Update the classification section of
`docs/notebook-git-attachments.md`.

### 4. The CLI uploads what is a pointer and leaves admission to the server

Type: Behavior
Status: planned
Proof: in `notebookPublish.lfsFailure.test.ts` the raw-attachment test now
expects no `git lfs push` for the raw blob, one bundle POST, and the stubbed
server refusal shown (example 4); a new test commits an empty root `.keep` and
a `notes/.keep` and a pointer file and expects only the pointer's object pushed
and the bundle submitted (example 3). Existing pointer-size "corrupt" and upload
failure tests stay green.

Behavior: publish with changed files → CLI pushes the LFS objects of exactly the
changed blobs that parse as pointers, then submits; it raises no path-based
refusal of its own.

Change: delete `isAttachment` from `notebookPublishLfsSelection.ts`; keep empty
and pointer parsing, drop the non-pointer throw. Update
`docs/notebook-git-lfs.md` (lines 86-88: the server, not the CLI, refuses raw
attachments).

### 5. The Readme role comes from the same owner

Type: Structure
Status: planned
Proof: existing Readme tests stay green
(`NotebookGitProposalInitialNotebookReadmeControllerTest`,
`NotebookGitProposalFolderReadmeEditControllerTest`,
`NotebookGitProposalAdditionValidationControllerTest` `readme.md` reserved case,
`NotebookGitProposalInitialContainerTreeControllerTest`).

Change: the classifier answers whether a Markdown path is a notebook Readme,
a folder Readme or a note (exact `README.md`, as today); `TreeInspection`,
`FolderShape.isReadme`, `DocumentApplication` and `FolderMaterialization` use
it. No remaining inline `.md`, `README.md` or `.keep` string checks in
`services/notebookGit` (grep).
Justification: the story's scope and its source finding (one classification
owner); no later Behavior depends on it, so it closes the plan and may be
stopped before without losing any promise.

## Current decisions

- Owner decisions 2026-09-27: leftover marker → clear refusal (not silent
  removal); `*.MD` → refusal (not accepted as a note); the CLI drops its
  pre-upload raw check.
- A root `.keep` stays an ordinary attachment; no special case anywhere.
- The marker rule judges the whole proposed tree (accepted trees never hold
  such a `.keep`); the letter-case rule judges changed paths only.

## Learnings

- `pnpm backend:test:worktree` takes one `--tests` pattern per run; `*NotebookGit*`
  misses `services.notebookGit.PortablePathKindTest`.
