# Consolidate initial container publication proof

Status: planned — retrospective correction; not executed.
Source: [SEED-017 Story 9](../../seeds/SEED-017-cohesive-design-corrections.md#story-9)
and the completed [plan 097](../097-cohesive-initial-notebook-publication/PLAN.md).

## Evidence and bounded outcome

At `f76fa62632`, `NotebookGitProposalInitialNestedFolderReadmeControllerTest`
repeats the controller publication, implied parent hierarchy, omitted Readmes,
stored child content and exact accepted head/tree proof now provided by
`NotebookGitProposalInitialContainerTreeControllerTest`. Its explicit assertion
that container publication creates no Note rows must survive consolidation.
Plan 097 added the broader proof in `4b2508fe7c` while retaining the older test.
Both use the same real controller/database/bundle boundary; the older example
adds another transaction and bundle round trip and a separate assertion owner.
No measured runtime saving is claimed.

Give developers one owner for container-only initial publication proof while
preserving its observable contract. This is test consolidation, with no product
behavior, environment, API, schema or backlog changes. Leave mixed-concept,
root-only, rejection, retry, reference and installed CLI journey tests intact.
The small cleanup supports maintainable verification; it does not supersede
the developer-selected queue or broaden the leading isolation direction.

Historical product boundary: base `583b5214b9`, final `52bb03e6e8`.
Reviewed execution commits, in slice order:
`0d545dcc9b` (root loop), `d44d20c176` (root roles),
`0d24be2fb8` (ancestry), `4b2508fe7c` (container trees),
`05ff2e8e5f` (shared application), `5e14e60b23` (general admission),
`960b02d6d3` (references), `3ec08ba31c` (content rollback),
`6ce697bc8f` (ancestry rollback), `3ab4655901` (retry),
`52bb03e6e8` (installed CLI). Planning and concurrent guidance changes are
provenance only. Product/test files are unchanged between that boundary and
`f76fa62632`.

## 1. Give container-only publication one behavioral proof owner

Type: Structure
Status: planned
Proof: the retained container-tree controller test proves exact accepted
head/tree/files/content, unique parent-before-child Folder paths, omitted
ancestor/notebook Readmes, and zero live Notes for container-only trees.

In `NotebookGitProposalInitialContainerTreeControllerTest`, explicitly assert
zero live Note rows after publishing a container-only tree. Preserve the
existing sibling/deep and optional notebook Readme coverage. Transfer the
minimal `Parent/Child` case to the same data-driven scenario if needed to retain
the singleton boundary; do not leave a separate copy of its orchestration.
Once the coverage mapping above holds, remove
`NotebookGitProposalInitialNestedFolderReadmeControllerTest`. Keep the installed
CLI initial-tree round trip as the integration proof; no E2E removal or rerun is
required for this controller-test-only correction.

Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only`, as backend rules
require the full backend unit suite. Use only the owning Unit Test environment.
Follow required execution wrap-up: fresh independent post-change refactor,
coordinator `./scripts/run.sh pnpm format:changed` once, plan evidence update,
commit with check-only hook, push and applicable asynchronous CI observation.
No API generation trigger exists.

Sizing: approximately 5 minutes active work plus required backend-suite runtime,
high confidence. The slice directly owns this bounded retrospective correction
and preserves behavior at the existing controller boundary. If active work
exceeds 10 minutes, stop and reassess under repository decomposition guidance;
required suite runtime is the stated exception.

Relevant constraints: ADR 0004 retains Readmes as container content, distinct
from Note rows; ADR 0007 requires owned disposable test data. Neither changes.
No slice-specific concern was found in this planning assessment; execution is
not authorized by this retrospective.
