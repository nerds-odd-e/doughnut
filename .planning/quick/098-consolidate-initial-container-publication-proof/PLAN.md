# Consolidate initial container publication proof

Status: done — retrospective correction delivered.
Source: [SEED-017 Story 10](../../seeds/SEED-017-cohesive-design-corrections.md#story-10).

## Evidence and bounded outcome

`NotebookGitProposalInitialNestedFolderReadmeControllerTest`
repeats the controller publication, implied parent hierarchy, omitted Readmes,
stored child content and exact accepted head/tree proof now provided by
`NotebookGitProposalInitialContainerTreeControllerTest`. Its explicit assertion
that container publication creates no Note rows must survive consolidation.
Both use the same real controller/database/bundle boundary; the older example
adds another transaction and bundle round trip and a separate assertion owner.
No measured runtime saving is claimed.

Give developers one owner for container-only initial publication proof while
preserving its observable contract. This is test consolidation, with no product
behavior, environment, API, schema or backlog changes. Leave mixed-concept,
root-only, rejection, retry, reference and installed CLI journey tests intact.
The small cleanup supports maintainable verification and is first in the queue.
It does not broaden the leading isolation direction.

## 1. Give container-only publication one behavioral proof owner

Type: Structure
Status: done
Proof: the retained container-tree controller test proves exact accepted
head/tree/files/content, unique parent-before-child Folder paths, omitted
ancestor/notebook Readmes, and zero live Notes for container-only trees.

In `NotebookGitProposalInitialContainerTreeControllerTest`, explicitly assert
zero live Note rows after publishing a container-only tree. Preserve the
existing sibling/deep and optional notebook Readme coverage. Add the minimal
`Parent/Child` input to the same data-driven scenario so it proves that one
authored child Readme creates exactly one implied parent with no Readme. Do not
leave a separate copy of its orchestration.
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
The cumulative design remains one container-tree model: authored Readmes define
container content, missing ancestors are implied once, and container-only input
creates no Notes. The singleton, sibling/deep and optional-notebook-Readme
examples exercise that rule rather than introducing separate handlers.

Refinement assessment: ready for direct execution. This one Structure slice has
one controller-level proof loop, a plausible approximately-five-minute active
path, and no unexplained work beyond the stated full-suite runtime exception.
Execution was separately authorized and completed below.

## Execution evidence

- Added the `Parent/Child` singleton to the retained data-driven controller
  proof and asserted zero live Notes for every container-only tree, then removed
  `NotebookGitProposalInitialNestedFolderReadmeControllerTest`.
- `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed with
  `BUILD SUCCESSFUL in 1m`.
- A fresh post-change refactor review found the consolidated proof already
  cohesive and made no further edits; `scripts/check_diff_whitespace.sh` passed.
- Coordinator selective formatting passed once with
  `./scripts/run.sh pnpm format:changed`; no generated API trigger changed.
- CI observer: `nerds-odd-e/doughnut`, branch `main`, workflow `ci.yml`
  (`donut CI`), key
  `ci-watch-execution:nerds-odd-e/doughnut:main:plan-098-root`, session `27360`,
  mailbox `/tmp/dough-ci-501/watch-SOGTfp`, PID `15058`.
