# Publish one initial note inside the new README-backed folder

Source: [SEED-016 Story 2](../../seeds/SEED-016-initial-notebook-and-folder-readmes.md#story-2).
Status: planned; not executed.

## Goal and scope

From a notebook with no folders or live notes and an empty accepted Portable
tree, accept one direct-child commit adding exactly:

```text
README.md
New Folder/README.md
New Folder/First note.md
```

Both Readmes are valid nonblank `type: Readme` Markdown. The ordinary note is
valid `type: Note` Markdown with a valid filename-derived title. Store all
authored content, create one fresh root Folder and one fresh ordinary Note
inside it, and accept the exact commit atomically.

Exclude every fourth path, root notes, additional notes or folders, nested
folders, any existing notebook content including empty folders, relationships,
attachments, multiple unpublished commits, stale/divergent history, and bulk
import.

## Current decisions

- Keep `NotebookController.publishNotebookGitProposal` as the stable boundary
  and `NotebookGitProposalPublisher.publish` as the existing SERIALIZABLE,
  REQUIRES_NEW transaction owner. No endpoint, CLI, API schema, migration, or
  transport change.
- Extend only the delivered initial notebook-and-folder creation route. Preserve
  the sole-folder and two-README initial cases.
- Reuse the existing ordinary-note addition rules for typed authored content,
  filename-derived title, note creation, persistence, and contextual errors.
  Do not copy those rules into folder acceptance.
- Recognize only the exact three-path tree, with the ordinary note directly
  inside the one new root Folder. No general initial-tree importer or batch
  policy.
- Require the locked notebook to contain no folders or live notes before any
  mutation. Rebuild the final projection and require exact proposed-tree
  equality before the existing binding write; transaction rollback owns
  atomicity.
- Accepted ADR 0001 supplies Notebook, Folder, Note, and Readme terminology.
  Accepted ADR 0004 defines the three Portable paths, filename-derived note
  title, `type: Readme`, and `type: Note`. No ADR change is needed.

## Outside-in proof

At the controller boundary, start with an empty matching Git-backed notebook.
Publish one direct-child proposal containing only the three named paths.
Observe the authored notebook and Folder Readmes, one root Folder named
`New Folder`, one ordinary Note titled `First note` inside it with the authored
content, and accepted/downloaded head and tree exactly equal to the proposal.
The two delivered smaller shapes remain green.

## Ordered slices

### 1. Share ordinary-note addition with initial-tree acceptance
Type: Structure
Status: planned
Proof: Existing ordinary-note addition and initial README publication
controller tests remain green. Run
`CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

Structure: Extract the existing ordinary-note addition behavior from the
near-limit `NotebookGitProposalPublisher` into one cohesive backend acceptance
seam reusable by the immediate next Behavior. Preserve its authored-document,
filename-title, represented-destination, fresh-identity, persistence, and error
semantics exactly. Keep orchestration and the publication transaction in the
publisher; do not create a general import framework or expose a test-only API.

Sizing hypothesis: about five minutes for one behavior-preserving extraction
and one existing-suite proof loop.

### 2. Accept the exact three-path initial notebook tree
Type: Behavior
Status: planned
Proof: A controller test observes both Readmes, the fresh root Folder, the one
fresh ordinary Note inside it, all authored content, and exact
accepted/downloaded proposal head and tree. Then run
`CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

Behavior: An owner of an empty matching notebook publishes one direct-child
commit adding only the notebook Readme, one root Folder Readme, and one ordinary
Note directly inside that Folder → Donut creates the container and note and
atomically accepts the exact authored commit.

Add one exact creation candidate for those three paths. Reuse the current
initial-Readme eligibility and validation, create the Folder before resolving
the note's destination against the refreshed Folder projection, apply the
shared note-addition seam, then require exact final projection before the
shared binding write. Preserve all wider refusals and both delivered smaller
creation shapes.

Sizing hypothesis: about five to ten minutes for one controller-first proof
loop; full backend-suite runtime is an external-wait exception.

## Contract-to-proof map

| Story promise | Owning proof |
| --- | --- |
| The exact initial three-path commit is publishable | Slice 2 controller publication |
| Both Readmes and the ordinary Note preserve authored content | Slice 2 entity observations |
| The Note has a fresh identity inside the fresh root Folder | Slice 2 Folder/Note relationship observations |
| The authored commit is accepted atomically | Slice 2 downloaded head/tree plus existing transaction |
| Smaller delivered shapes stay supported and broader trees stay excluded | Slice 1 existing suite and Slice 2 full backend suite |

## Planning assessment

Both slices are cohesive, own one proof loop, and have plausible target-sized
hypotheses. Slice 1 is immediately justified by Slice 2 and prevents duplicated
note-addition rules while keeping the 242-line publisher below the 250-line
limit. Slice 2 owns one externally observable outcome. No unexplained
hard-limit path remains; backend-suite runtime is the stated external-wait
exception.

Resulting slice count: 2. Refinement is not needed. The plan is ready for direct
execution when separately authorized.

## Learnings

None yet; implementation and tests have not started.
