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

Developers using Codex, Cursor, and Claude Code encounter worktree startup and
recovery friction. Retrospective findings demonstrate wrong-base recovery,
worktree recreation, and missing generated skill paths; they do not establish
dependency installation as the dominant cost or quantify a potential speedup.
Current setup also differs between normal Nix entry, agent-mode entry, and
package commands, which can repeat installation.

The desired effect is predictable preparation on an already-provisioned machine:
fresh worktrees become usable without manual copying or repair, repeated setup
avoids unnecessary installation, and concurrent work remains independent. This
improves development of Donut; its contribution to the near-future notebook
workflow is indirect. The owner retains first priority as a bounded investment
in the tools used to deliver that direction.

## Alternatives and Decision

Reuse host-native setup configuration and the existing repository/package-manager
behavior before adding another setup authority. One documented command run in a
fresh checkout is an acceptable supported entry point where automatic integration
would require taking over worktree creation. Manual copying and repair are not.
Keep all three tools in scope without requiring identical hook mechanisms.

Use pnpm's existing shared package store and Gradle/Nix caches. Assess native pnpm
dependency verification before extending the handwritten fingerprint. Do not
copy or link another worktree's mutable dependency tree. pnpm's managed global
virtual store is a different candidate, to consider only in one bounded experiment
if measured fresh-layout cost warrants it; it is not a promised adoption.
Do not replace Claude Code's worktree lifecycle merely to install dependencies.

## Architectural Constraints

- [ADR 0007](../../docs/adrs/0007-environments-and-isolation-accepted.md)
  requires stable worktree identities and isolation of mutable application,
  database, process, and port state. Shared infrastructure is acceptable only
  when mutable state remains isolated.
- Dependency and tool versions remain controlled by committed manifests and
  lockfiles. A faster setup must not silently accept stale or mismatched
  dependencies.
- The common behavior belongs to the repository; Codex-, Cursor-, and Claude
  Code-specific hooks may invoke it but must not become competing authorities.

## Story Decomposition

<a id="story-1"></a>

### Start isolated AI worktrees quickly and reliably

- **Identity:** SEED-039#story-1
- **Plan:** [AI worktree readiness](../quick/005-ai-worktree-readiness/PLAN.md).
- **Goal:** Developers using Codex, Cursor, or Claude Code can prepare a fresh
  Donut worktree on an already-provisioned machine, access repository tooling,
  and run frontend and backend checks without manual dependency copying or repair.
- **Scope:** One coherent preparation behavior; supported entry points for all
  three hosts; tracked tooling and necessary generated skill links; correct
  dependencies after relevant input changes; cheap unchanged preparation;
  failed/incomplete preparation never reports readiness. Preserve existing
  database, port, process, and build-output isolation. Service allocation remains
  with existing runtime/test commands, not eager startup of every service.
- **Key examples:** Fresh worktree with warm machine caches → supported setup →
  repository tools and frontend/backend checks work using committed dependencies.
  Unchanged worktree → repeat setup and a normal package command → no redundant
  installation/lifecycle pass. Changed workspace manifest/lockfile or missing
  install state → prepare → install the valid graph or report the mismatch without
  silently rewriting committed inputs. Failed preparation → fix its cause and
  retry → actual readiness. Two concurrent worktrees → prepare and check both →
  each keeps its dependency view, runtime ownership and disposable data.
- **Evaluation:** Record before/after fresh and repeated preparation using the
  same revision inputs, machine/cache conditions and stated timing boundaries;
  separate setup from compilation/test execution. Record actual host versions
  and invocation paths. A shell command alone does not prove a host invokes it.
  Existing caching may already be adequate; no absolute latency or speedup ratio
  is promised. At most one bounded optimization experiment follows a measured
  substantial remaining preparation cost.
- **Deferred promises:** New-machine or cloud provisioning; a worktree manager;
  branch-policy or cleanup redesign; arbitrary local-file/credential copying;
  remote caches; general compilation, test or browser-download optimization;
  fixing every shared retrospective finding; notebook product changes.
- **Value / learning:** Remove demonstrated repair work and unnecessary setup
  from repeated development cycles; determine the actual remaining cost before
  investing in acceleration.
- **Effort hypothesis:** M, low confidence until host entry points and native
  pnpm readiness behavior are observed; reassess if these demand lifecycle work.
- **Depends on:** none.
- **Safe stopping point:** All three hosts have proven supported preparation,
  repeated installation is avoided, failures are visible and isolation is
  preserved. Stop without additional cache machinery when remaining timings
  do not justify it. Partial host coverage is useful progress, not story completion.

## Ordering and Scope Reduction

Keep the existing first position as the owner's explicit tool-sharpening choice,
not a prerequisite for notebook attachments or assimilation. Deliver reliable
preparation and remove redundant work first. Drop speculative acceleration before
weakening isolation or broadening into lifecycle/platform management.

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

Now, as the first queued product-backlog item. The linked plan is planning-only;
the story has not been taken and implementation is not authorized by refinement.

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
