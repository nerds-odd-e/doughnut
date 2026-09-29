---
id: SEED-060
status: dormant
planted: 2026-09-29
planted_during: owner request to queue the highest-priority open project retrospective findings
trigger_when: preparing real-book manual acceptance or starting the Development stack from the default checkout
scope: small
---

# SEED-060: Dependable local run environment for manual and development runs

## Why This Matters

Two open project retrospective findings in
[DonutRetrospectiveFindings.md](../../DonutRetrospectiveFindings.md) show
Donut's own local run tooling costing time before any product observation
starts:

- **DD-161**: real-book manual acceptance of book reading needed about 28
  minutes, mostly repairing the MinerU environment and writing a temporary
  script to keep a disposable stack running. The queued SEED-059 book-reading
  stories will need the same kind of acceptance again.
- **DD-159**: `pnpm dev` from the default checkout failed to start because
  `backend/build/classes` still held a class whose source had been removed.
  This recurs whenever backend code is deleted and the Development stack is
  started from an older build.

The correction for both lands in Donut's scripts and documentation, not in
shared Open Dough guidance.

## Story Decomposition

Two independent stories, ordered by the findings' impact. This seed captures
backlog work, not an execution plan.

<a id="story-1"></a>

### Run real-book manual acceptance without rebuilding the environment

**Identity:** SEED-060#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal**

A developer or agent doing manual acceptance of book reading can attach a real
PDF through the CLI to a disposable running app, using documented repo
commands, without repairing the MinerU install or writing their own
stack-holding script.

**Scope**

- MinerU is installed at a pinned version known to work with
  `cli/python/mineru_book_outline.py` (DD-161 used `mineru[pipeline]==3.4.5`;
  unpinned installs now get MinerU 4.x, which has no `pipeline` extra). The
  install hints in `cli/python/mineru_book_outline.py`,
  `cli/src/commands/mineruOutline/mineruOutlineSpawn.ts` and
  `e2e_test/fixtures/book_reading/regenerate_mineru_output_for_refactoring.sh`
  name the same version.
- `.venv-mineru` does not depend on a Nix store path that garbage collection
  can remove, or the setup rebuilds it when it does.
- One repo command starts a disposable E2E stack and keeps it running for
  manual use without Cypress, replacing the temporary `hold-stack.mjs` around
  `runE2eInteractive`.

**Key example**

From a checkout whose `.venv-mineru` is missing or broken, following the
documented steps, attach *Attention Is All You Need* through the CLI `/attach`
to a held disposable stack and open it in the browser, with no ad hoc script.

**Source finding:** DD-161.

<a id="story-2"></a>

### The Development stack starts after backend code is removed

**Identity:** SEED-060#story-2
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal**

A developer who pulls or checks out a revision that removes a backend class
can start the Development stack with `pnpm dev` without deleting build
output by hand.

**Scope**

- Starting the Development stack does not load compiled classes whose sources
  no longer exist.
- The cause (the `backend:watch` compile beside `bootRunDev`, or something
  else) is found during refinement; DD-159 did not investigate it.

**Key example**

With `backend/build/classes` built at a revision that still has
`NotebookGitCutoverService`, check out a revision without it and run
`pnpm dev`: the application starts, instead of failing with
`No qualifying bean of type NotebookGitCutoverService`.

**Source finding:** DD-159.
