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

<a id="story-2"></a>

### Apply the shared dependency-readiness rule to redundant install-prefixed root scripts

- **Identity:** SEED-039#story-2
- **Plan:** none yet; ready for slice planning.
- **Goal:** Existing repository callers that currently reinstall dependencies
  unconditionally on every invocation (rather than reusing the one
  fingerprint-gated readiness owner `scripts/dev_setup.sh:setup_pnpm_deps`,
  delivered by SEED-039#story-1) reuse that same one rule, so a package
  command never pays for a redundant reinstall it doesn't need.
- **Scope:** Caller map recorded during SEED-039#story-1's execution
  (2026-09-21, at commit `725c0d61f4` on the now-deleted
  `worktree-claude+260921-ai-worktree-readiness` branch, before its plan
  history was removed — recover the full map from that commit if needed):
  root `package.json` has ~19 scripts prefixed with `pnpm --frozen-lockfile
  --silent recursive install &&` (`mcp-server:bundle/test/format/lint`,
  `test-fixtures:format/lint`, `cli`, `cli:bundle/format/lint/test`,
  `frontend:build/format/lint/test:ui/test/test:watch/sut/dev/storybook`,
  `test`, `dev`) — same install flavor as `setup_pnpm_deps`; 3 scripts
  (`generateTypeScript`, `cy:format`, `cy:lint`) prefixed with the
  non-recursive `pnpm --frozen-lockfile --silent install &&` — a different
  install flavor, not a straight substitution; `lint:all`/`format:all` also
  call several of the scripts above, so their prefix compounds within one
  invocation. Preserve each caller's actual task and lifecycle contract; do
  not add installation to unrelated Git/read-only operations; remove
  superseded prefix code only once all its callers use the selected owner.
- **Key examples:** SEED-039#story-1's readiness-gate postconditions
  (unchanged/mismatched/valid-change/interrupted-install) are already proven
  and reusable as-is — this story is about *where* that gate gets invoked,
  not re-proving its own correctness. A root script run after an unrelated
  dependency change installs once, not twice (its own prefix plus a prior
  Nix-hook install). `lint:all`/`format:all` no longer carry a separately
  compounding reinstall from each sub-script they call.
- **Evaluation:** One representative normal command per distinct changed
  caller contract still works exactly as before, plus the existing
  readiness-gate proof (reused, not re-derived). No new fingerprint,
  package-store, or runtime owner.
- **Deferred promises:** A second fingerprint/cache mechanism; forcing an
  unrelated caller with a genuinely different lifecycle contract into this
  rule merely for uniformity.
- **Value / learning:** Removes up to ~25 redundant reinstall passes across
  common package-script invocations, under one consistent readiness rule
  instead of a parallel ad hoc one per caller.
- **Effort hypothesis:** S–M; suggested decomposition from the investigation:
  (a) prove `setup_pnpm_deps` (or a thin wrapper) is safely invokable as a
  plain `pnpm`-script prefix outside the interactive Nix shell hook, for both
  install flavors; (b) apply it to the ~19 recursive-flavor leaf scripts;
  (c) resolve `lint:all`/`format:all`'s compounded prefix once leaf scripts
  no longer need their own; (d) decide the 3 non-recursive scripts'
  different contract separately. Not binding — replan at slice planning.
- **Depends on:** SEED-039#story-1 (delivered) — reuses its
  `setup_pnpm_deps` fingerprint gate as the one readiness owner.
- **Safe stopping point:** Any subset of callers fixed consistently with the
  rule is useful progress; do not force a caller with a genuinely different
  contract into a partial/inconsistent rule merely to finish this story.

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

SEED-039#story-1 was taken and delivered 2026-09-21 (fresh-worktree tooling
and dependency readiness across Codex, Cursor, and Claude Code, with a proven
redundancy-avoidance and parallel-isolation guarantee); its lasting behavior
now lives in `scripts/worktree_setup.sh`, `scripts/dev_setup.sh`, and
`.agents/agent-map.md`'s "Worktree setup" section, not in this seed.
SEED-039#story-2 is queued next in the product backlog, ready for slice
planning whenever taken.

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
