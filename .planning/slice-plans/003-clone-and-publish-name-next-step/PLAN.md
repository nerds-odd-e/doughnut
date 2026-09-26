# Clone and publish say briefly what happened and what to do next

## Source

- Story: [SEED-046#story-3](../../seeds/SEED-046-notebook-files-and-git-findings.md#story-3)
- **Identity:** SEED-046#story-3

## Goal and scope

The clone success message becomes a few lines — what was cloned where, then
the next commands with the actual directory — and lists no publish or pull
rules. Publish refuses a checkout that is behind the notebook with one short
message naming the recovery (pull, then publish again), whether the CLI's own
check or the server's race check finds it.

Assumption: executed after story 2 lands. Story 2 rewrites
`notebookPullNextSteps` (appended to the clone message today) and pull help,
and deletes the pull refusals behind the other findings. Excluded: pull help
text, pull refusal wording, the Book-source size wording, the missing-content
clone message, and other CLI wording (seed). Publish rule refusals, and the
dirty-checkout, size-limit and invalid-Markdown messages, stay unchanged.

## Outside-in proof

Entry point: `run(['notebook', …])` CLI suites under `cli/tests/` (real Git,
stubbed HTTP); the server race through its existing controller tests.

| Key example | Proof |
| --- | --- |
| 1 clone success: a few lines, next commands with the directory, no rules | slice 1, `notebookClone.test.ts` |
| 2 local main behind after a web save → pull, then publish again | slice 2, `notebookPublish.ancestry.suite.ts` |
| 3 another publish wins between the check and the submission → same recovery | slice 3, backend concurrency controller tests; CLI submission suites echo it |
| 4 unpublished merge → sent to pull, whose refusal names the next step | slice 2, existing merge-tip case in `notebookPublish.ancestryTip.suite.ts` with the new wording |
| 5 a publish rule refusal keeps naming its rule and paths | unchanged existing suites |

Commands: `CURSOR_DEV=true nix develop -c pnpm cli:test`;
`CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests '*NotebookGit*ControllerTest'`.

## Slices

### 1. Clone success names where the notebook went and the next commands

Type: Behavior
Status: planned
Proof: the first clone test in `cli/tests/notebookClone.test.ts` asserts the
printed lines (notebook id and destination; commit, `donut notebook publish
<dir>`, `donut notebook pull <dir>`) and that no rule sentence appears; green
in `pnpm cli:test`.

Behavior: `donut notebook clone 7 <dir>` succeeds → a few short lines: cloned
notebook 7 into `<dir>`; edit and commit there with any Git tool; publish
with `donut notebook publish <dir>`; receive newer changes with
`donut notebook pull <dir>`.

Change: replace the clone `console.log` in `cli/src/nonInteractiveCli.ts`; stop
appending `notebookPullNextSteps` there (pull usage keeps it).

### 2. Publish of a checkout behind the notebook names pull, then publish again

Type: Behavior
Status: planned
Proof: `notebookPublish.ancestry.suite.ts` and `notebookPublish.ancestryTip.suite.ts`
assert the new wording with the directory; green in `pnpm cli:test`.

Behavior: local main is not a linear run of commits on the accepted head
(behind, a merge tip, or unrelated history) → `donut notebook publish <dir>`
→ "Local main is not based on the notebook's latest accepted history. Run
`donut notebook pull <dir>`, then publish again." Nothing is posted.

Change: `ANCESTRY_ERROR` in `notebookPublishAncestry.ts` becomes a function of
the directory, passed from the publish command.

### 3. Losing a publish race names pull, then publish again

Type: Behavior
Status: planned
Proof: the four backend controller tests asserting "expectedHead no longer
matches" and `STALE_HEAD_MESSAGE` in `cli/tests/notebookPublish.testHelpers.ts`
use the new wording; backend command above and `pnpm cli:test` green.

Behavior: another publish or a web save is accepted after the CLI checked the
accepted head → the server refuses the submission → the CLI prints "The
notebook changed since this publish started. Run `donut notebook pull`, then
publish again."

Change: the message in `NotebookGitProposalPublisher` (the git-bundle endpoint
serves only CLI publish). The directory is not named because the server does
not know it.

## Current decisions

- Owner, 2026-09-26: the clone message lists no rules; refusals explain them.
- Planning, 2026-09-26: HTTP 409 also carries unrelated publish refusals, so
  the CLI does not rewrite messages by status; the server race message itself
  names the recovery.
