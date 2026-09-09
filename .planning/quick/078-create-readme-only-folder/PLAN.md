# Create one README-only folder locally

Source: [SEED-009 Story 19](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-19).
Status: complete.

## Goal and scope

A notebook owner adds one root-level folder containing only a valid nonblank
`README.md` to a clean bound checkout, commits it directly on accepted `main`,
and publishes. Donut creates one fresh Folder with that Readme and accepts the
exact authored commit atomically; no ordinary Note is required.

Included: exactly one new root folder, one added `Folder/README.md`, regular
file mode, valid `type: Readme`, matching accepted projection, and the existing
owner/ancestry/binding transaction. Excluded: notebook-root README changes,
nested placement, multiple folders, ordinary-note changes, folder moves or
renames, existing Readme edits, blank/untracked directories, attachments,
stale or divergent history, multiple unpublished commits, and bulk import.

## Execution context and current decisions

- CI observer: Codex cell `54`, stream session `10185`, receipt
  `/private/tmp/donut-ci-501/watch-Z3fb0x`, stream PID `57902`, coordinator
  `/root`, checkout `/Users/terryyin/git/doughnut`, repository/branch
  `nerds-odd-e/doughnut main`. Startup events for SHAs
  `34535e311c8f61764ca5b146a5f9261fc5a5ead8` and
  `e7463711a2dd6580a026b3da9ced4dba56ddfb5c` predate this execution and are
  outside its pushed history.
- Keep `NotebookController.publishNotebookGitProposal` as the stable boundary
  and `NotebookGitProposalPublisher.publish` as the existing SERIALIZABLE,
  REQUIRES_NEW transaction owner. No endpoint, CLI, API schema, migration, or
  transport change.
- Recognize only one added path matching one root segment plus `/README.md`,
  with no other changed file. Root `README.md` is the notebook Readme and stays
  excluded. A deeper path implies nested/new parents and stays excluded.
- Reuse the existing proposal path/mode checks, strict typed-Markdown gate,
  folder-name normalization/length/separator policy, and sibling collision
  rule. Require the container type to be `Readme`; do not treat README as an
  ordinary Note or generalize all README changes.
- Create one Folder with a fresh identity, root parent, authored Readme content,
  and existing timestamps. Rebuild the proposed Portable projection after the
  mutation and require an exact tree match before the existing bundle/binding
  write. Existing transaction rollback owns atomicity.
- [ADR 0001 — Ubiquitous language](../../../docs/adrs/0001-ubiquitous-language.md)
  supplies Folder and Readme terminology. [ADR 0004 — OKF-compatible notebook
  Markdown profile](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
  requires a folder Readme to map to `README.md` with `type: Readme` and states
  that empty folders exist in the tree only through tracked content. No
  Accepted ADR deviation is needed. ADR 0002 remains Proposed and non-binding.

## Outside-in proof

At the controller boundary, start with a matching Git-backed notebook and one
direct-child proposal adding only `Field Notes/README.md`. Publish it and
observe one root Folder named `Field Notes`, no new ordinary Note, the authored
Readme content, and accepted/downloaded Git head and tree equal to the proposed
commit. Existing mixed-change, root-README, unsafe-path, non-regular-mode,
invalid-Markdown, collision, drift, and idempotent-head tests remain green.

## Refinement assessment

Refinement performed after the initial slice plan. Both slices classify as
Ready: slice 1 has one internal classification gate and one unchanged-behavior
proof loop; slice 2 has one vertical publication outcome and one controller
proof loop. No slice was replaced, no story or architecture escalation is
needed, and there is no implementation-sizing exception. The backend-suite
runtime remains only an external-wait exception.

Resulting slice count: 2. Story resplit is not recommended. The plan is ready
for execution when separately authorized; no implementation, test run, commit,
or push has started.

## Ordered slices

### 1. Recognize the single root folder-creation shape
Type: Structure
Status: complete
Proof: Existing backend controller tests remain green and preserve their
current refusal messages while the new candidate is still refused. Run
`CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

Structure: Extend the existing folder-shape classification with one internal
candidate for exactly one added `Folder/README.md` and no other changed path.
Keep root README, deeper paths, mixed changes, and relocation classification
unchanged. Route the candidate to an interim refusal with the current observable
error until slice 2 immediately consumes it. Do not introduce a general folder
change hierarchy or public test-only surface.

Sizing hypothesis: about five minutes; one small classification branch and one
existing-suite proof loop.

### 2. Accept the README-only folder as the authored commit
Type: Behavior
Status: complete
Proof: A focused controller test observes the new root Folder, its Readme, no
ordinary Note, and the exact accepted/downloaded proposal head and tree. Then
run the full backend unit suite with
`CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

Behavior: A matching owner checkout publishes one direct-child commit adding
only a valid root-level folder README → Donut creates that root Folder and
Readme and advances accepted `main` to the exact authored commit atomically.

Implement the minimum acceptance path inside the existing publication
transaction. Reuse current folder construction/name/collision rules and the
proposal Markdown/blob readers; refresh folder projection before the exact
proposed-tree comparison and existing binding write. Replace slice 1's interim
refusal only for this exact candidate. Do not add nested placement, note
creation, batch support, a new endpoint, or special CLI handling.

Sizing hypothesis: about five minutes for one controller-first behavior loop;
backend suite runtime is an external-wait exception, not implementation scope.

## Contract-to-proof map

| Story promise | Owning proof |
| --- | --- |
| One locally authored README-only root folder is publishable | Slice 2 controller publication |
| Folder name, Readme content, and absence of ordinary Notes | Slice 2 folder/readme/note observations |
| Exact authored commit and Portable tree are accepted | Slice 2 proposed-vs-downloaded head/tree comparison |
| Atomic publication within existing safety boundaries | Slice 2 final projection plus existing transaction and rejection suites |
| All broader folder/tree shapes remain excluded | Slice 1 existing refusal suite and slice 2 full backend suite |

## Learnings

- Slice 1: the exact candidate fits cohesively beside relocation recognition in
  `NotebookGitProposalFolderShape`; routing it through the existing reserved
  README refusal preserves every observable until the behavior slice consumes
  it. Full backend proof passed with `pnpm backend:test_only`; the fresh
  refactor review found no worthwhile change.
- Slice 2: the existing folder construction and sibling-name validation rules
  can be reused after validating the authored Markdown is specifically a
  `Readme`. Reloading the folder projection after creation makes the existing
  exact-tree gate prove the authored proposal before the shared binding write.
  The controller-first test initially failed with the reserved folder-README
  refusal, then the full 2,329-test backend suite passed; the fresh refactor
  review found no worthwhile change.
