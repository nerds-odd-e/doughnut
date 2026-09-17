---
name: dough-manual-testing
description: >-
  Plans bounded manual or exploratory observation of requested product
  behavior when the developer explicitly requests manual testing, exploratory
  testing, or an active plan slice requires it. Covers web, CLI, API, desktop,
  or combined surfaces. Does not run proactively or replace required automated
  tests.
---

# Observe requested behavior

Use only on explicit developer request or plan-slice manual testing.
No diagnosis, product repair, or unrequested tooling.

## Mission

Resolve scope and time budget from the request: a feature, story set, recent
deliveries, or change range. Recover current promises, examples, and
constraints from this project's records or Git history, including deleted
stories. Apply later decisions before older expectations; implementation,
narration, and existing tests are not the oracle.

Identify in-scope externally observable surfaces, including non-web. Resolve
URLs, accounts, startup, secrets, and tools from this project's guidance when
needed; do not guess credentials.

If oracle, budget, or needed environment or access is missing, name it and
stop; do not invent them.

## Plan

Before acting, list coverage areas and journeys, risks or questions, and
proportional split of preparation, breadth, selective depth, and
surprise/confirmation reserve. Weight by importance and risk. Include each
promised surface. Completing this plan is not acceptance.

## Prepare

For a standalone session that needs a project checkout, first settle which
checkout to use. If a story, an active plan slice, an explicit caller
selection, or another workflow already has an established checkout, use it
and create no nested temporary workspace. Otherwise, before any
checkout-bound setup begins, create one temporary Git branch and a paired
worktree from the verified current revision (for example, `git worktree add
<path> -b <branch> <verified-revision>`), and record that branch, worktree
path, and starting revision as the session's workspace identity. Use and
reuse that same workspace for all preparation, exploration, reporting, and
any resume within the session; never create a second one alongside it. On
resume after interruption, verify the recorded branch, worktree, and
starting revision still match actual Git state before reusing it; if they do
not match, stop and report rather than assuming reuse is safe. The
originating checkout stays unchanged throughout. This workspace covers only
the Git checkout; it does not isolate shared accounts, services, databases,
or other external test state, which remain governed by their own existing
rules.

Choose the cheapest reliable route: existing setup, whole or partial automated
journey, or temporary harness or test, including a setup-only feature
scenario using existing steps. Skip setup when current state serves. Confirm
state, session, and required services remain available for external
observation; a finished batch run may not. Preserve isolation, cleanup,
compatible-state reuse, and removal of owned temporary artifacts. Missing
reuse is a possible improvement, not authority for permanent test/runner
changes. If no supported route leaves a usable starting state, name it and
stop.

## Explore

Cover planned areas in breadth first with this project's tools. Spend depth on
surprises and high-risk questions; reallocate remaining time, keeping
confirmation reserve. Reuse sufficient automated evidence; do not replay
deterministic checks or proven setup. If a needed tool or environment is
unavailable, name it and stop.

## Report

When planned coverage completes with no actionable findings or material
uncertainty, report exactly `Good.` Otherwise report only discrepancies
(expected versus actual plus evidence), unresolved expectations (not false
fails), improvements (out-of-scope ideas, not failed acceptance), and
material coverage gaps. Never report `Good.` when blocked or incomplete.
Omit narration, speculation, and completion markers. Do not start repair,
root-cause, or permanent test changes.

On normal completion of a session-created temporary workspace, remove
session-owned temporary artifacts first, then remove the clean worktree and
its branch (`git worktree remove`, then delete the branch). If cleanup would
be unsafe — dirty state, unresolved evidence, or ambiguous ownership — do not
force it; report that and retain the exact workspace identity (branch,
worktree path, starting revision) so it can be resumed or deliberately
disposed of later.
