---
id: SEED-070
status: dormant
planted: 2026-10-07
planted_during: owner-requested commit-hook timing investigation and backlog prioritization
trigger_when: selecting the owner's highest-priority commit-hook optimization
scope: medium
---

# SEED-070: Get fast, trustworthy commit feedback

## Why This Matters

Donut contributors wait for the pre-commit hook on every commit. The owner
reported that it feels slow, asked for confirmation, and then requested an
average runtime below five seconds and removal of the observed warnings.
Faster, clean feedback should retain the confidence supplied by the checks.

## Story Decomposition

<a id="fast-warning-free-commit-hook"></a>
### Commit with trustworthy checks averaging under five seconds

**Identity:** SEED-070#fast-warning-free-commit-hook
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal:** Donut contributors get ordinary local commit-hook feedback in under
five seconds on average, with no tooling warnings during healthy runs.

**Scope:** Optimize the existing pre-commit path, including environment
startup, dependency verification, staged-source preparation, and affected
component checks as needed. Preserve the existing lint and typecheck gates,
frontend checks against staged content, failure exit status, and the hook's
check-only behavior: it leaves source files and the real Git index unchanged.
Fix the causes of the observed warnings rather than hiding diagnostics;
actionable failures must remain visible. CI-suite and product test-suite
optimization are outside this item.

**Evaluation:**

- Measure wall-clock time from hook invocation to exit, including Nix entry
  when the contributor invokes a commit outside the development shell.
- Demonstrate an arithmetic mean below five seconds across repeated,
  representative ordinary local commits. Include frontend changes, other
  affected components, and mixed-component changes; report the sample mix,
  individual timings, environment, and mean. Also report the no-staged-change
  baseline and behavior when already inside Nix. The exact sampling protocol
  remains for refinement; a fast no-op alone does not satisfy the target.
- Healthy runs produce neither `ERR_PNPM_UNSAFE_MODULES_DIR` nor the ensuing
  dependency-install warning, and no replacement tooling warnings.
- Show that relevant lint/type errors still block commits and that unrelated
  unstaged/untracked frontend edits cannot make staged frontend checks pass
  or fail. Verify that checks leave the working tree and index unchanged.

**Current evidence (local investigation, 2026-10-07):**

Measured in the primary macOS checkout at `4a4900df07`, on `main`, with an
initially clean working tree and index. These are individual observations,
not averages and not proof of a regression relative to an older revision.

| Observation | Wall time | Result |
| --- | --- | --- |
| Installed hook, no staged changes: `/usr/bin/time -p .git/hooks/pre-commit` | 5.48 s | Passed; no components selected |
| Equivalent frontend-check path: `./scripts/run.sh bash -x scripts/quality_changed.sh lint` | 11.21 s | Passed, with pnpm warnings |

The frontend observation used a disposable copy of the Git index with
`frontend/index.html` removed from that copy to select the frontend component.
The real index and files were untouched; this measured the frontend gate
without creating a commit. It was not a timed ordinary source-change commit.
The actual hook and this path share the runner and quality dispatcher; the
observation bypassed the outer hook/`pnpm lint:changed` launcher and added shell
tracing, so it is an approximate frontend-commit baseline.

Approximate frontend-path breakdown from timestamped command/output events:

| Phase | Elapsed |
| --- | --- |
| Nix entry and dispatcher setup | 3.01 s |
| Explicit frozen-lockfile recursive dependency install | 0.32 s |
| Temporary index copy and dependency symlinks | 0.55 s |
| Frontend script startup, including automatic dependency verification | 0.52 s |
| Biome startup/check | 0.46 s; Biome reported 997 files in 105 ms |
| Remaining frontend check, dominated by `vue-tsc --noEmit` | 6.20 s |
| Dispatcher completion and temporary-copy cleanup | 0.15 s |

Boundaries come from trace/output timestamps rather than separate CPU profiles.
Nix overhead varied between the two observations; do not add 5.48 s to the
frontend total, which already includes Nix entry.

**Observed warnings:** When `pnpm -C <temporary-copy>/frontend lint` ran,
pnpm attempted automatic dependency verification/install before the script.
It emitted `ERR_PNPM_UNSAFE_MODULES_DIR`, refusing to remove
`/Users/terryyin/git/doughnut/node_modules` because its resolved target was
outside the temporary project root. It then warned that the install before
scripts failed, that `node_modules` might be out of sync with the lockfile,
and suggested `verifyDepsBeforeRun: false`. The lint/typecheck still ran and
the command exited successfully. The suggested setting is diagnostic evidence,
not an approved fix; dependency freshness and the check's safety need to be
understood before choosing an approach.

During this planning update, Nix also emitted `warning: Git tree
'/Users/terryyin/git/doughnut' is dirty` on entry with staged documentation
changes. Concurrent Nix invocations for the whitespace check and commit hook
produced an ignored SQLite evaluation-cache `database is busy` message; both
commands ultimately passed. This is evidence of a possible concurrent-cache
cost, not a measurement of an ordinary serial commit or an established cause
of the owner's reported delay. Include Nix entry diagnostics when evaluating
warning-free healthy commits.

**Code and history facts:**

- The installed `.git/hooks/pre-commit` matches the tracked
  [hook](../../scripts/git-hooks/pre-commit). It invokes
  `./scripts/run.sh pnpm lint:changed`.
- [The runner](../../scripts/run.sh) enters Nix when Nix exists and
  `IN_NIX_SHELL` is unset. No-op commits currently pay that cost too.
- [The dispatcher](../../scripts/quality_changed.sh) selects components from
  staged paths, then checks them sequentially. It selects whole components,
  rather than limiting each tool to the changed files.
- Its frontend path explicitly runs a frozen-lockfile recursive install,
  copies all tracked index files into a fresh temporary directory, symlinks
  root/frontend dependencies from the checkout, runs frontend lint, and
  deletes the copy. Workspace packages linked through `node_modules` still
  resolve from the working tree; the existing isolation promise is scoped
  to frontend source.
- [Frontend lint](../../frontend/package.json) is
  `biome check . && vue-tsc --noEmit`, so even one frontend path selects a
  full frontend typecheck. The temporary-copy path was introduced in
  `574d61b52c6` on 2026-09-26; the same lint/typecheck command existed before
  that change.
- The current package manager is `pnpm@11.28.5`. The warnings occurred with
  this version; their first occurrence and historical frequency are unknown.
- Backend lint uses Gradle `lint`, which depends on `spotlessCheck`.
  Backend, CLI, MCP, shared-fixture, root, OpenAPI, and mixed-component hook
  timings have not been measured. No comparative historical benchmark exists.
- [Linting guidance](../../.agents/skills/linting_formating/SKILL.md) records
  an earlier approximate cold frontend-typecheck cost of 15–20 seconds;
  the present measured typecheck phase was about six seconds.

**Approach considerations:** Measure the normal entry point and dominant
costs before choosing changes. Environment reuse, dependency-verification
reuse, staged snapshot preparation, and safe reuse of typecheck results are
investigation candidates, not prescribed implementation. Removing the
typecheck or broadly suppressing warnings would not meet the scope.

- **Value / learning:** Shorter contributor feedback while retaining a
  dependable commit gate; establish repeatable evidence of the improvement.
- **Effort hypothesis:** M (1–2 hours), low confidence; the target is below
  the currently observed typecheck phase alone, and broader commit timings
  remain unknown. Reassess after profiling the representative commit mix.
- **Depends on:** None identified.
- **Safe stopping point:** The optimized hook independently provides fast,
  warning-free checks while preserving commit-gate behavior.

## Ordering and Open Decisions

The owner explicitly placed this one story first in the product backlog on
2026-10-07. Existing story priorities and the near-future direction stay intact.
Refinement must establish the representative sample mix, repetition count,
machine/dependency assumptions, and treatment/reporting of cold runs. No
implementation approach or executable plan has been selected.

## Breadcrumbs

- Owner's timing-confirmation request and subsequent under-five-second,
  warning-removal, top-priority backlog instruction in this conversation.
- Owner authorized recording this backlog change directly on main and
  synchronizing with origin; optimization implementation is future work.
