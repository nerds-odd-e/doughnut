---
id: SEED-038
status: dormant
planted: 2026-09-21
planted_during: owner-requested triage of Donut retrospective findings
trigger_when: prioritizing reliable CI observation for Donut execution worktrees
scope: small
---

# SEED-038: Reliable CI-observer startup in Donut worktrees

## Why This Matters

Developers relying on autonomous Donut delivery need working CI observation from
its first push. Donut’s local skill-path setup produces two recurring startup
failures: a missing runtime before shell setup and a silent no-op afterwards.
The [finding review](../../DonutRetrospectiveFindings.md#reliable-ci-observer-startup-in-donut-execution-worktrees)
records four occurrences of each and reproduces both at `8766adefee`.

## Alternatives and Decision

Queue two separately evaluable startup outcomes under one problem. The existing
workaround is to rediscover and invoke the tracked `.agents/skills` runtime;
copying local installations into worktrees also worked in some occurrences.
Repeated operator recovery has delayed or lost observation. Prefer a coherent
project-owned setup and invocation contract; refinement does not prescribe
symlinks, physical copies, or a patch to published Open Dough scripts.

## Story Decomposition

<a id="story-1"></a>

### Start CI observation from a fresh Donut worktree

- **Identity:** SEED-038#story-1
- **Finding:** [DD-074](../../DonutRetrospectiveFindings.md#dd-074).
- **Goal:** A developer’s execution can initialize CI observation in a fresh
  Donut worktree before its first push, without discovering an absent runtime
  through failure or copying a skill installation from another checkout.
- **Scope:** Make Donut’s supported startup path and project guidance usable
  before an interactive Nix shell has populated gitignored skill paths.
  Preserve execution-checkout identity and the published observer protocol.
  This story does not promise the post-shell path outcome owned by story 2.
- **Key examples / evaluation:** A fresh checkout of tracked files, with the
  required Node runtime available and no generated `.claude/skills`, follows
  Donut’s startup guidance and obtains a `CI_OBSERVER` probe receipt bound to
  that checkout. No manual directory copy or alternate-checkout runtime is
  needed. Missing external CI credentials remain an explicitly reported
  limitation, not misdiagnosed as a missing local runtime.
- **Value:** Avoid an entire execution proceeding without observation; one
  retained occurrence left ten of eleven pushes unobserved.
- **Effort hypothesis:** S (30–60 minutes), medium confidence; assumes a
  project-local setup/guidance correction suffices without upstream changes.
- **Depends on:** None.
- **Safe stopping point:** Fresh-worktree startup is reliable independently of
  later shell setup; story 2 retains ownership of that separate failure.

<a id="story-2"></a>

### Start CI observation reliably after Donut shell setup

- **Identity:** SEED-038#story-2
- **Finding:** [DD-065](../../DonutRetrospectiveFindings.md#dd-065).
- **Goal:** After Donut’s ordinary shell setup, a developer’s execution can
  initialize CI observation through the supported project invocation and
  receive its real receipt instead of a successful-looking silent no-op.
- **Scope:** Reconcile the project’s generated skill layout with runtime
  invocation. Cover the common `setup_claude_skills` behavior used by Nix and
  cloud setup. Preserve checkout binding and shared skill updateability;
  determine the smallest coherent project-owned correction during planning.
  Do not introduce a second observer or monitoring protocol.
- **Key examples / evaluation:** Starting from tracked files, run the actual
  project skill setup, then the documented observer probe. It prints
  `CI_OBSERVER` for that execution checkout; exit 0 with empty stdout does not
  satisfy acceptance. Repeating ordinary setup leaves that invocation working.
  A local startup check verifies the normal launch returns its receipt without
  depending on a live GitHub run. Fresh pre-shell availability belongs to story 1.
- **Value:** Remove repeated false-unavailable conclusions and delayed arming;
  two retained occurrences pushed before the observer was started.
- **Effort hypothesis:** S (30–60 minutes), medium confidence; any required
  published-skill change must be resolved without an unmaintainable local fork.
- **Depends on:** None; if story 1 changes the common path, reuse that solution.
- **Safe stopping point:** Post-setup invocation works independently; do not
  claim fresh-worktree availability unless story 1’s example also passes.

## Ordering and Scope Reduction

Both groups have four retained occurrences. Put story 1 first because its
recorded impact includes ten unobserved pushes, then story 2 for delayed arming
and silent false success. The owner explicitly prioritized the top two findings
above the existing queue; these delivery-reliability stories leave the notebook
product direction unchanged. If one coherent correction proves both outcomes,
reuse that proof and close the second story without redundant implementation.

## Open Decisions

No unresolved beneficiary or outcome blocks queueing. Implementation and proof
commands remain for slice planning; this seed authorizes neither execution nor
changes to shared published skills.

## When to Surface

Before another Donut execution relies on CI-observer startup in a new worktree.

## Breadcrumbs

- [Donut retrospective findings](../../DonutRetrospectiveFindings.md).
- [Project skill setup](../../scripts/shell_setup.sh).
- [Installed runtime setup](../../.agents/skills/dough-execute-plan/references/runtime-setup.md).
- Triage at `8766adefee`: both failures reproduced, no matching active backlog
  story and no established prior resolution found. Historical observer-shutdown
  improvements are a different outcome, not proof these startup problems closed.
