---
id: SEED-067
status: dormant
planted: 2026-10-03
planted_during: owner review of project retrospective findings, queueing the most frequent Donut-specific finding group
trigger_when: refining a queued story that corrects a local app-stack finding in DonutRetrospectiveFindings.md
scope: medium
---

# SEED-067: Dependable local app stacks for observing and proving changes

## Why This Matters

Developers and coding agents working on Donut need a running app to observe and
prove a change. The open project findings show the local stacks failing to
start or restarting mid-run because of backend build output that something else
left or rewrote in the same checkout. Each costs a failed start or run and a
diagnosis. Priority follows the findings' frequency; see
[Donut retrospective findings](../../DonutRetrospectiveFindings.md#open-findings).

## Story Decomposition

<a id="stacks-survive-other-builds"></a>
### Start and keep local app stacks on current backend code

**Identity:** SEED-067#stacks-survive-other-builds
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/005-stacks-survive-other-builds/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"6919fd9adf4cc741d8fba93d46002dc79a7132781c457b1902d49844d036dbe9","plan":"31e3445cca8f3fc632fc706723d0b63abb7c8fb8895f17b777b17fddea080785"}}
```

#### Goal

A developer or coding agent can start the Development stack, or keep a batch
E2E stack running, on current backend code while other backend builds touch
the same checkout's `backend/build`. No failed starts from leftover classes,
no batch-run restarts caused by someone else's build, and no hand-deleting
build output. This removes the most frequent Donut-specific retrospective
finding group, so observing and proving a change stops costing a failed run
and a diagnosis.

#### Scope

- **Batch E2E (`pnpm cy:run`):** another backend Gradle run in the same
  checkout with unchanged source and `HEAD` does not restart or break a
  running batch stack. The run proceeds. A test run that compiles changed
  source or a new commit is a deliberate change to the code under test and is
  out of scope.
- **Development start (`pnpm dev`):** a fresh start after backend source files
  were deleted runs only current code. No class whose source is gone is
  loaded.
- **Cause finding is part of the work for the Development start.** DD-159's
  log has rotated away. Refinement observed that `pnpm dev` still runs two
  Gradle compilers on one output at once (`backend:watch` and `bootRunDev`),
  the pattern DD-103 removed from E2E batches. That is a lead, not a
  confirmed cause. Execution reproduces or explains the stale classes before
  fixing them.
- **Batch mechanism observed during refinement:** batch `cy:run` launches
  `bootRunE2E` with Spring DevTools on the classpath, watching `build/classes`
  and `build/resources`. Batch mode has no watcher, so only another build can
  trigger those restarts. Slice planning found the writer: linked-worktree
  backend tests pass `--rerun-tasks`, which rewrites the whole main output on
  every run.
- **Out (reload is the intended behavior):** interactive `cy:open` and a
  running Development stack keep reloading when backend output changes,
  including a brief failed restart partway through a compile that recovers on
  its own (seen in the default checkout on 2026-09-29 at 09:19).
- **Out:** separate stacks in different checkouts (each already has its own
  `backend/build`), stale `dev.pid` handling (one occurrence on an unlanded
  branch, a different cause), and dev-log rotation.

#### Key examples

- DD-200 → Given a batch `pnpm cy:run` whose stack is up, when
  `pnpm backend:test` runs in the same checkout, then the E2E backend does not
  restart, Flyway still finds its migrations, and the features run with no
  Bad Gateway.
- DD-159 → Given `backend/build/classes` compiled from a revision that had a
  service (with a class that injects it), when that source is deleted (by
  edit, checkout, or pull) and `pnpm dev` starts afresh, then the backend
  starts healthy without anyone deleting `backend/build`.
- Boundary → Given an interactive `cy:open` stack, when backend source is
  edited, it still reloads as it does today.

#### Notes

- Plan: [005](../slice-plans/005-stacks-survive-other-builds/PLAN.md).
- Findings: DD-159, DD-200 in
  [Donut retrospective findings](../../DonutRetrospectiveFindings.md#open-findings);
  resolved DD-103 (`d86864023c`) must keep holding.
- **Effort hypothesis:** M, low confidence. The Development start depends on
  what the cause finding shows.
- **Safe stopping point:** the batch E2E case and the Development start case
  are independent. Either can land alone.
