---
id: SEED-039
status: dormant
planted: 2026-09-21
planted_during: owner request to prioritize cross-agent worktree setup acceleration
trigger_when: now; first product-backlog priority
scope: medium
---

# SEED-039: Fast, isolated worktree setup across AI coding environments

## Why This Matters

Developers using Codex, Cursor, and Cloud Code need fresh Git worktrees to
become ready for useful Donut development quickly. Today the repository safely
reuses machine-level package caches, but a new worktree still needs its own
dependency layout, and setup and package commands can repeat installation
checks. Slow or inconsistent preparation reduces the value of parallel AI work,
while sharing mutable dependency or runtime directories would let concurrent
worktrees interfere.

The desired effect is one repository-owned setup outcome that each supported
environment can invoke: a fresh worktree reuses safe immutable caches, creates
only the isolated state it needs, and becomes ready for representative Donut
checks without manual dependency copying. Repeated setup in an unchanged
worktree should be a cheap no-op, and concurrent worktrees must remain stable.

## Alternatives and Decision

Doing nothing retains the current safe behavior, including pnpm's shared store,
Gradle's user cache, Nix's store, and Donut's isolated worktree databases, but it
leaves repeated setup work and environment-specific entrypoints unassessed.
Manually preparing each worktree is the strongest smaller alternative; it is
insufficient because every supported AI environment would pay the same operator
cost and could drift onto a different setup sequence.

Do not share or copy a mutable `node_modules` directory between worktrees.
Instead, first measure fresh and repeated setup through Codex, Cursor, and Cloud
Code, then optimize the common repository-owned dependency-readiness path while
preserving each worktree's own dependency view. The highest-learning question is
which cold-worktree costs remain after the existing pnpm, Gradle, and Nix caches
are warm; do not add cache machinery that measurements do not justify.

## Architectural Constraints

- [ADR 0007](../../docs/adrs/0007-environments-and-isolation-accepted.md)
  requires stable worktree identities and isolation of mutable application,
  database, process, and port state. Shared infrastructure is acceptable only
  when mutable state remains isolated.
- Dependency and tool versions remain controlled by committed manifests and
  lockfiles. A faster setup must not silently accept stale or mismatched
  dependencies.
- The common behavior belongs to the repository; Codex-, Cursor-, and Cloud
  Code-specific hooks may invoke it but must not become competing authorities.

## Story Decomposition

<a id="story-1"></a>

### Start isolated AI worktrees quickly and reliably

- **Identity:** SEED-039#story-1
- **For / why:** Developers using Codex, Cursor, and Cloud Code can begin useful
  work in a fresh Donut worktree without manually copying dependencies or
  waiting for redundant setup, while concurrent worktrees remain independent.
- **Evaluation:** With machine-level dependency caches warm, create fresh linked
  worktrees through each supported environment and invoke its normal setup path.
  Each worktree reaches representative frontend and backend checks using the
  committed dependency graph. Repeating setup without dependency changes takes
  the verified no-op path. Concurrent worktrees neither mutate one another's
  dependency view nor share test databases, ports, or build outputs. Record the
  comparable before/after setup boundaries and timings rather than claiming an
  unmeasured speedup.
- **Value / learning:** Makes the near-future parallel-AI-work direction cheaper
  and more predictable, while identifying whether remaining cost comes from Nix
  entry, pnpm link creation, lifecycle/native builds, or another owned step.
- **Effort hypothesis:** M, low confidence until the three environment
  entrypoints and cold-worktree baseline are observed.
- **Depends on:** none.
- **Safe stopping point:** All three environments use one proven repository
  setup outcome, unchanged worktrees skip unnecessary preparation, and failures
  remain visible without weakening worktree isolation. Further optimization can
  be deferred if the measured result is already adequate.

## Ordering and Scope Reduction

The owner placed this story first in the product backlog because fast, reliable
worktree creation directly enables the current parallel-AI-work direction. Keep
the first delivery to the shared setup outcome and its three supported callers.
Drop speculative cache formats, copied dependency trees, remote caches, and
unmeasured build acceleration before weakening isolation or broadening the
story.

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

Now, as the first queued product-backlog item. This seed does not authorize
implementation or allocate an executable plan.

## Breadcrumbs

- Owner direction, 2026-09-21: prioritize worktree dependency setup
  acceleration and scope it across Codex, Cursor, and Cloud Code.
- Current repository evidence: the Nix shell hook already fingerprints pnpm
  inputs, package scripts still run frozen recursive installation checks, pnpm
  uses a shared content-addressable store, Gradle enables shared caching, and
  linked worktrees receive isolated test databases.
