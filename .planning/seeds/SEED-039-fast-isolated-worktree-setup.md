---
id: SEED-039
status: dormant
planted: 2026-09-21
planted_during: owner request to prioritize cross-agent worktree setup acceleration
trigger_when: now; first product-backlog priority
scope: small
---

# SEED-039: Fast, isolated worktree setup across AI coding environments

## Why This Matters

The owner’s remaining concern is developer waiting time and AI token cost.
The completed worktree setup remains available. The current root
`frontend:test` script still invokes a frozen recursive install before Vitest,
even after successful preparation; the agent map names this as the normal
frontend testing entry point.

This establishes repeated work, not its wall-clock cost or token savings.
Keep only a small, observable reduction in this feedback loop. Do not justify
a wider tooling project from the number of install-prefixed scripts.

## Alternatives and Decision

Reuse the existing dependency-readiness owner for this one command if it
preserves dependency validation and removes meaningful repeated work.
Leaving the command unchanged is preferable to a readiness redesign whose
cost exceeds the demonstrated benefit. No performance percentage or token
saving is promised without evidence.

## Architectural Constraints

- Reuse `scripts/dev_setup.sh:setup_pnpm_deps`; introduce no second readiness
  or cache authority.
- Preserve frozen-lockfile failure behavior and task exit status. A package
  manifest change must not silently bypass validation merely because the
  root fingerprint is unchanged.
- Preserve worktree isolation under
  [ADR 0007](../../docs/adrs/0007-environments-and-isolation-accepted.md).
  Do not share mutable dependency trees or alter runtime provisioning.

## Story Decomposition

<a id="story-2"></a>

### Apply the shared dependency-readiness rule to redundant install-prefixed root scripts

- **Identity:** SEED-039#story-2
- **Plan:** [Frontend test dependency readiness](../quick/006-frontend-test-readiness/PLAN.md)
  — two planned slices; implementation has not started.
- **Goal:** Developers and coding agents reach frontend test feedback without
  paying for an unnecessary installation on an already-prepared checkout.
  Reduce waiting and avoidable setup interaction; exact token savings remain
  unmeasured.
- **Scope:** Only the root `frontend:test` entry point and the minimum shared
  readiness integration necessary to preserve its existing dependency and
  task contract. Keep test selection, arguments, and exit status intact.
  This is the sole caller promised despite the retained broader story title.
- **Key examples:**
  - Prepared checkout with unchanged dependencies → run the normal focused
    frontend test command → the selected test runs without a pnpm install.
  - Dependencies require preparation → run the same command → prepare once,
    then run the test; a failed frozen install prevents the test from starting.
  - An incompatible workspace package-manifest change → the command still
    detects the dependency mismatch rather than accepting a stale ready state.
- **Evaluation:** Reuse existing readiness failure/recovery tests where they
  cover the promise. Record one comparable before/after focused frontend test
  invocation on a warm checkout, with elapsed time and whether installation
  ran. Inspect only the missing caller-boundary proof. Count removed setup
  interactions/output if observed; do not equate output bytes or install
  counts with measured AI tokens. No profiling campaign or full platform matrix.
- **Value / why now:** One bounded reduction in a documented feedback loop
  used to deliver the near-future direction. First position remains an explicit
  tool-cost investment, not a prerequisite for attachment-folder continuity.
  If the initial comparison shows negligible cost, or preserving correctness
  needs a larger redesign, stop and reassess rather than expanding this story.
- **Dropped scope:** Other root callers, aggregate lint/format command cleanup,
  non-recursive install normalization, cache/store experiments, cross-IDE
  setup changes, and general worktree lifecycle work. These are removed,
  not automatically queued as future stories.
- **Codex advice considered:** UI-generated repository-owned local environment
  setup would automate the existing manual command but is a separate outcome.
  Drop it from this narrowed delivery, including its UI launch/evidence work;
  retain the existing manual setup and `.codex/hooks.json`. Do not create a
  replacement story without a new prioritization decision.
- **Effort hypothesis:** Small only if existing readiness can be reused safely;
  the package-manifest validation boundary is the main uncertainty.
- **Depends on:** Delivered SEED-039#story-1 readiness behavior.
- **Safe stopping point:** This command delivers useful savings on its own;
  no other caller migration is needed to complete the narrowed promise.

## Ordering and Scope Reduction

Owner direction during refinement, 2026-09-21: prioritize time and AI token
cost, dramatically reduce scope, and drop minor or unsupported improvements.
Keep the current queue position for this bounded attempt. Do not spend more on
measurement or generalized cleanup than the small improvement warrants.
No new backlog items are created for the dropped work.

## Related Retrospective Findings

The [Donut project retrospective findings](../../DonutRetrospectiveFindings.md)
currently retain no project-owned finding for this concern after the 2026-09-21
ownership review. Do not invent a Donut-specific finding merely to justify this
story.

The shared Open Dough retrospective log contains directly relevant evidence:

- [ODF-035](../../DearDough.md#odf-035--enterworktrees-default-base-ref-and-branch-name-sanitization-conflict-with-this-projects-worktreebranch-convention)
  records repeated extra setup and recovery when a worktree tool chose a
  different base revision and branch name from the repository's convention.
  Cross-environment setup must verify the resulting checkout rather than assume
  every host creates worktrees identically.
- [ODF-084](../../DearDough.md#odf-084--a-concurrent-session-deleted-an-active-story-branch-execution-worktree-and-branch-while-a-delegated-subagent-was-mid-slice)
  records an active worktree being removed by a concurrent session, followed by
  worktree recreation and another frozen pnpm install. The story must preserve
  active-worktree ownership and cleanup isolation; faster dependency readiness
  alone does not make destructive concurrent cleanup safe.
- [ODF-085](../../DearDough.md#odf-085--the-documented-claudeskills-runtime-path-did-not-exist-at-all-in-a-freshly-created-execution-worktree)
  records a fresh worktree missing a shell-generated `.claude/skills` path,
  causing failed startup, unnecessary directory copies, drift risk, and in one
  execution lost CI observation. The common setup outcome must distinguish
  tracked repository tooling from generated per-environment paths and avoid
  copying a mutable local tool installation between worktrees.

These links supply evidence and constraints; they do not transfer ownership of
the shared findings into this product story or expand it into fixing every
worktree lifecycle issue recorded in `DearDough.md`.

## When to Surface

SEED-039#story-1 was taken and delivered 2026-09-21 (fresh-worktree tooling
and dependency readiness across Codex, Cursor, and Claude Code, with a proven
redundancy-avoidance and parallel-isolation guarantee); its lasting behavior
now lives in `scripts/worktree_setup.sh`, `scripts/dev_setup.sh`, and
`.agents/agent-map.md`'s "Worktree setup" section, not in this seed.
SEED-039#story-2 remains first in the product backlog with the narrowed
frontend-test outcome above; no execution is authorized by this refinement.

## Breadcrumbs

- Owner direction, 2026-09-21: prioritize worktree dependency setup
  acceleration and scope it across Codex, Cursor, and Claude Code (corrected by
  the owner; cloud infrastructure is not an additional required platform).
- Owner accepted the research-based scope and requested slice planning. Keep
  reliable setup and all three hosts; condition further optimization on evidence.
- Current repository evidence: the Nix shell hook already fingerprints pnpm
  inputs, package scripts still run frozen recursive installation checks, pnpm
  uses a shared content-addressable store, Gradle enables shared caching, and
  linked worktrees receive isolated test databases.
- SEED-039#story-1 execution, 2026-09-21: delivered slices 1-8 of its plan.
  Slice 4 was found oversized mid-execution and narrowed to its
  readiness-gate proof half; the deferred half became SEED-039#story-2 above,
  with its caller map preserved there. Owner chose to close story-1 at that
  point rather than continue into story-2 in the same execution.
