# Notebook history commits are proven where they happen

## Source

- Story: [SEED-050#story-10](../../seeds/SEED-050-local-ai-notebook-technical-debt.md#story-10)
- **Identity:** SEED-050#story-10
- Correction of SEED-050#story-5 (plan
  `4426190218:.planning/slice-plans/024-finished-transitions-leave-no-trace/PLAN.md`,
  commits 48d5a2e8f8, 433ca0be36, b2dd68f03e, 5afa318baf), from its execution
  retrospective on 2026-09-27.

## Goal and scope

A developer changing notebook creation or history reset learns from the
entry-point tests when the commit message or author changes, and the
synchronization doc says only what those commits do.

Included: assert "Create notebook" by Donut System <system@donut.local> in the
shared creation assertion; assert the reset commit's message and author;
drop the service test's repeated commit-count walk; drop the two leftover
cutover-era phrases in `docs/notebook-git-synchronization.md`.

Excluded: `docs/notebook-git-lfs.md` decision-record wording (owner-owned);
the binding-less branch in `resetHistory`; the CLI pause test.

## Starting facts (checked 2026-09-27 at `5afa318baf`)

- `NotebookGitHistoryServiceTest.java:35-92` calls `startHistory` directly and
  asserts message and author (`:76-78`) plus a parent-count and commit-count
  walk (`:74`, `:84-92`) that `NotebookGitBindingAssertions.assertInitialLfsRootCommitBinding`
  (`controllers/NotebookGitBindingAssertions.java:28`) already makes for real
  creation (`NotebookCrudControllerTest:46`, `CircleControllerTest:111`); that
  shared assertion checks neither message nor author.
- `resetHistoryReplacesTheExistingBindingWithTheNotebooksCurrentContent`
  (`NotebookGitHistoryServiceTest.java:97-140`) checks only the tree; no test
  asserts "Reset: restart Git history from the current notebook" or its
  author, which `docs/notebook-git-synchronization.md:48-50` promises.
- `docs/notebook-git-synchronization.md:45-46` "Repository creation needs no
  owner opt-in and does not wait for local acquisition." is implied by "from
  the moment it is created"; `:186` "may still read a complete tree" keeps a
  before/after flavour.

## Outside-in proof

| Key example | Slice |
| --- | --- |
| 1. create a notebook through the controller → its root commit reads "Create notebook" by Donut System | 1 |
| 2. reset a notebook's history → its root commit reads "Reset: restart Git history from the current notebook" by Donut System | 1 |
| docs state only current behavior | 2 |

## Slices

### 1. Creation and reset commits are asserted at their entry points

Type: Behavior (proof)
Status: planned
Proof: temporarily change `CREATION_COMMIT_MESSAGE`/`RESET_COMMIT_MESSAGE`
and see the new assertions fail, restore, then
`CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests 'com.odde.donut.controllers.NotebookCrudControllerTest' --tests 'com.odde.donut.controllers.CircleControllerTest' --tests 'com.odde.donut.controllers.NotebookGitHistoryResetControllerTest' --tests 'com.odde.donut.services.notebookGit.NotebookGitHistoryServiceTest'`.

Change: `assertInitialLfsRootCommitBinding` also asserts message and author
(literals); the reset test (service or `NotebookGitHistoryResetControllerTest`,
whichever drives the real reset) asserts the reset message and author; the
service creation test keeps only what the shared assertion does not cover.

### 2. The synchronization doc drops cutover-era phrases

Type: Structure (docs)
Status: planned
Proof: the two phrases are gone and the paragraphs still read whole.

Change: delete `:45-46`'s sentence; drop "still" at `:186` and rewrap.

## Current decisions

- `notebook-git-lfs.md`'s "Rewriting or resetting accepted history needs a
  separate decision" and its Compatibility bullet are decision-record text for
  the owner.

## Learnings

None yet.
