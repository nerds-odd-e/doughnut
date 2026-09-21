# Reliable preparation for AI worktrees

Status: planned; no implementation or execution proof yet.
Source: [SEED-039#story-1](../../seeds/SEED-039-fast-isolated-worktree-setup.md#story-1).
Work item identity: SEED-039#story-1.

## Goal and boundaries

On an already-provisioned local development machine, a developer using Codex,
Cursor or Claude Code can prepare a fresh Donut worktree without manual copying
or repair, then run frontend and backend checks. Repeating preparation avoids
redundant installation while changed or incomplete inputs cannot falsely pass
as ready. Concurrent worktrees retain their own mutable state.

Keep all three hosts, allowing different supported entry points. A documented
repository command is acceptable when automatic setup would require replacing
a host's worktree lifecycle. Setup does not eagerly build the application or
start every service: existing test/runtime commands own their prerequisites.

This is an owner-prioritized investment in development tools. The notebook
content direction in [NORTH-STAR](../../NORTH-STAR.md) remains unchanged; its
tree, codec and accepted-change topics need no new architectural topic here.

Exclude new-machine/cloud provisioning, worktree managers, branch-policy and
retirement redesign, arbitrary local-file/secret copying, remote caches, general
build/test acceleration and shared Open Dough runtime redesign. No speedup ratio
or absolute setup time has been agreed. Further caching is conditional below.

## Preparation identity

- Draft checkout: `/Users/terryyin/git/doughnut/.worktrees/005-ai-worktree-readiness-planning`.
- Draft branch: `codex/plan-ai-worktree-readiness`.
- Starting revision: `aa8154b272f5ba62c396b43df9b391f61adfa5be`.
- Originating/integration checkout: `/Users/terryyin/git/doughnut`, branch `main`.
- Resolved publication target if later retained: `origin/main`,
  `git@github.com:nerds-odd-e/doughnut.git`.
- This is preparation ownership, not an execution identity or Taken claim.
  Draft remains local for review. Execution must establish its own identity.

## Execution identity

- Mode: Story Branch Mode (default; no `--trunk`).
- Taken claim: committed and published on `main` at `b5d4225d49`
  ("Take SEED-039#story-1 for execution").
- Execution checkout: `/Users/terryyin/git/doughnut/.worktrees/005-ai-worktree-readiness`.
- Execution branch: `worktree-claude+260921-ai-worktree-readiness`, created
  from published `main` (`b5d4225d49`) and pushed to
  `origin/worktree-claude+260921-ai-worktree-readiness` after slice 1.
- Integration checkout/branch: `/Users/terryyin/git/doughnut`, `main`.
- Authorized publication target: `origin/main`,
  `git@github.com:nerds-odd-e/doughnut.git` (fast-forward via the integration
  checkout at story wrap-up; this execution branch itself is pushed to
  `origin`, not merged into `main`, until wrap-up).
- CI observer: repo `nerds-odd-e/doughnut`, branch
  `worktree-claude+260921-ai-worktree-readiness`, workflow `ci.yml`
  ("donut CI"), directory `/tmp/dough-ci-501/watch-YlPJi2`. Registered SHA:
  `86b2fadc4ebb4eecfc63769088cd117f7ed22b67` (slice 1 delivery).
- Replanning permission: not explicitly granted or denied by the invoking
  instruction (`/dough-execute-plan 005`); existing planning authority
  preserved (ordinary refinement/replanning stays available if a slice
  overruns).

## Existing solutions and proof gaps

Inspected at the starting revision; recheck changed callers before execution.

| Responsibility | Existing owner/evidence | Decision |
| --- | --- | --- |
| Toolchain entry | `scripts/run.sh`, `scripts/nix_shell_hook.sh`, `flake.nix` | Reuse Nix and the quiet agent path; no new environment manager. |
| Dependency readiness | `scripts/dev_setup.sh` fingerprint; root package scripts explicitly install; pnpm 11.27.0 pinned in root manifest and flake | Prefer native verification if the isolated gate proves the required behavior. Otherwise improve the existing readiness owner, not a parallel cache ledger. |
| Skill visibility | `scripts/shell_setup.sh:setup_claude_skills`, tracked `.agents/skills` | Reuse same-checkout relative links; runtime scripts use tracked paths where appropriate. Do not copy another checkout's installed skills. |
| Host preparation | `.cursor/worktrees.json` currently invokes normal Nix then `pnpm install`; no tracked Codex environment setup or Claude startup preparation | Thin host entry points invoke the repository behavior; prove actual invocation. |
| Runtime/data isolation | `scripts/backend-test-worktree*`, `scripts/worktree-identity.mjs`, SUT/E2E owners | Reuse existing allocation, admission and retirement. Do not allocate from a second setup path. |
| Script proof | Bach tests under `scripts/test/`; Node command-boundary fixtures in `scripts/*worktree*.test.mjs` | Extend real command execution. The current Nix-hook tests simulate output and most daemon tests simulate implementation; neither proves real preparation. |
| Install lifecycle | root `postinstall: syncpack fix`; `scripts/workspace-syncpack-lifecycle.test.mjs`; frontend `pretest` prepares stories/browser | Preserve necessary lifecycle behavior. Separate dependency setup from browser/test preparation in timing and claims. |

The fingerprint omits workspace manifests and toolchain versions. Normal
`setup_pnpm_and_biome` also restarts Biome using a process-wide match. Do not
invoke that whole path from isolated preparation. Narrow extraction is warranted
to expose dependency readiness without global service/daemon side effects.

Retrospective sources: [ODF-035](../../../DearDough.md#odf-035--enterworktrees-default-base-ref-and-branch-name-sanitization-conflict-with-this-projects-worktreebranch-convention),
[ODF-084](../../../DearDough.md#odf-084--a-concurrent-session-deleted-an-active-story-branch-execution-worktree-and-branch-while-a-delegated-subagent-was-mid-slice),
and [ODF-085](../../../DearDough.md#odf-085--the-documented-claudeskills-runtime-path-did-not-exist-at-all-in-a-freshly-created-execution-worktree).
They demonstrate repair costs, not measured install slowness. Their shared
ownership remains unchanged; this story owns Donut's supported preparation.

## Decisions and external evidence

Follow Accepted [ADR 0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
for mutable-state ownership and [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md#usage)
for visible failure and no speculative recovery. The ADR index and record
statuses agree (0000–0007 Accepted); these two constrain this change. No exception
or new ADR is proposed.

Official documentation researched during refinement:

- [Codex local environments](https://learn.chatgpt.com/docs/environments/local-environment):
  setup on app-created worktrees, configuration under `.codex`.
- [Cursor worktrees](https://cursor.com/docs/configuration/worktrees):
  `.cursor/worktrees.json` setup; recommends package-manager installs rather
  than linking another worktree's dependencies.
- [Claude worktrees](https://code.claude.com/docs/en/worktrees) and
  [WorktreeCreate](https://code.claude.com/docs/en/hooks#worktreecreate): normal
  project preparation is supported; this hook replaces creation entirely.
  A session-start hook alone must not be assumed to cover later EnterWorktree.
- [pnpm verification/build settings](https://pnpm.io/settings/build#verifydepsbeforerun):
  evaluate native readiness checking on the pinned version before adopting it.
- [pnpm install](https://pnpm.io/cli/install): frozen lockfile and prefer-offline
  are candidates; strict offline is not a requirement. No lockfile rewriting to
  make setup succeed.
- [pnpm virtual store](https://pnpm.io/settings/node-modules#virtualstoretype):
  managed graph-addressed reuse is distinct from sharing another checkout's
  mutable directory; assess only at the optional gate, including direct-node
  resolution and local workspace links.
- [Gradle cache](https://docs.gradle.org/current/userguide/dependency_caching.html#sec:cache_locking):
  existing local sharing supports communicating Gradle processes; no new cache
  service or container-sharing commitment.

Documentation establishes candidates, not proof of installed-host integration.
Preserve manifests, lockfile, required lifecycle/native builds and same-checkout
workspace resolution. Do not choose `--ignore-scripts` as a performance shortcut.

## Before-change observation gate

Obtain this during authorized execution, before edits invalidate the baseline.
Use owned disposable linked worktrees, never the shared development checkout.
Record revision, OS/architecture, Nix/Node/pnpm and host versions, commands,
cache conditions, elapsed times and failures in this plan. Do not confuse
absence of a setup hook with slow dependency install.

Recorded 2026-09-21 in owned worktree
`.worktrees/005-ai-worktree-readiness` (branch
`worktree-claude+260921-ai-worktree-readiness`) at revision `b5d4225d49`,
machine cache warm. Host: Darwin 25.6.0 arm64 (macOS, Apple Silicon); Nix
2.30.2; Node v24.5.0 (host shell; the Nix shell provides its own pinned
Node); pnpm pinned `11.27.1` (`packageManager` in `package.json`; the plan's
`11.27.0` reference is stale — 11.27.1 is what is actually pinned and used
below).

1. Fresh-worktree setup: `git worktree add` alone leaves `node_modules` and
   `.claude/skills` absent (0 entries), confirming ODF-085 — Claude has no
   session-start or `WorktreeCreate` hook that prepares a new worktree, and
   `.claude/settings.json` in this checkout declares no `Worktree*` hook at
   all. `.agents/skills/*` (the tracked source) is present and unaffected —
   only the generated `.claude/skills/*` symlinks are missing. Codex has no
   tracked environment file (`.codex/hooks.json` only wires the product
   backlog guard, not setup). Cursor's `.cursor/worktrees.json` already runs
   `nix develop -c pnpm install` — plain (non-`CURSOR_DEV`, non-frozen)
   install with no `setup_claude_skills` call and no readiness fingerprint —
   on worktree creation; not re-observed live here since that requires the
   installed Cursor app itself. Missing setup recorded as the gap for Codex
   and Claude; a documented repository command is the fallback per the plan's
   boundaries.
2. Dependency baseline, `CURSOR_DEV=true nix develop -c pnpm --frozen-lockfile
   recursive install` in the fresh owned worktree: first run 7.6s wall (1201
   packages resolved, 0 downloaded — reused from the local content-addressable
   store — plus `postinstall: syncpack fix`); immediate repeat 1.1s wall
   (pnpm's own already-up-to-date check, "Done in 273ms" pnpm-reported).
   Nix shell entry alone (`CURSOR_DEV=true nix develop -c true`), separately:
   4.7s cold-of-turn, 2.2s repeated. Dependency install is not the dominant
   cost once caches are warm; Nix entry and install are both low-single-digit
   seconds here. No profiling framework used; two samples were unambiguous.
3. Native-readiness assessment, same owned worktree, pinned pnpm 11.27.1
   (`pnpm exec biome --version`, no explicit `verify-deps-before-run` config
   present — `pnpm config get verify-deps-before-run` reports `undefined`,
   i.e. pnpm's built-in default applies):
   - Unchanged state: 2.3s wall, `Version: 2.5.14`, no reinstall output. Pass.
   - Missing install (`node_modules` moved aside): `pnpm exec` transparently
     ran a full install before printing the version (implicit
     verify-before-run). Required lifecycle work (`postinstall: syncpack fix`)
     ran. Pass for "still usable"; not itself evidence for the mismatch
     postcondition below.
   - Workspace manifest inconsistent with the committed lockfile (added
     `left-pad` to root `package.json`'s `dependencies` only, lockfile
     untouched): `pnpm exec biome --version` silently ran an install that
     resolved and added `left-pad`, **rewriting `pnpm-lock.yaml`** to match,
     then printed the version with no visible warning of the prior mismatch.
     **Fails** the "mismatch stays visible without rewriting inputs"
     postcondition — bare `pnpm exec`'s implicit verification silently heals
     divergent inputs instead of surfacing them.
   - The same mismatch fixture against the existing readiness owner's actual
     command, `pnpm --frozen-lockfile recursive install` (no `pnpm exec`):
     fails loudly with `ERR_PNPM_OUTDATED_LOCKFILE` / "specifiers in the
     lockfile don't match specifiers in package.json: 1 dependencies were
     added: left-pad", exit 1, and leaves `pnpm-lock.yaml` unchanged. Pass.
   - Valid paired input change (`package.json` plus a matching
     `pnpm install --no-frozen-lockfile --lockfile-only` update):
     `pnpm --frozen-lockfile recursive install` then succeeds normally
     (1s, `postinstall` ran). Pass.
   - All fixtures reverted (`git checkout -- package.json pnpm-lock.yaml`)
     and `node_modules` restored to the committed lockfile afterward; worktree
     left clean at `b5d4225d49`.

   **Decision:** bare `pnpm exec`'s native verify-before-run does not satisfy
   the required mismatch-visibility contract (it rewrites the lockfile
   instead of failing), so slices 3–4 do not adopt it as the dependency
   readiness owner. The existing owner — `pnpm --frozen-lockfile recursive
   install`, as `scripts/dev_setup.sh:setup_pnpm_and_biome` already runs —
   already satisfies all four postconditions and is what slice 3 composes
   with slice 1's entry point; slice 4's redundancy rule reuses the existing
   fingerprint gate around that same command rather than a native setting.

Preparation baseline and actual check completion are separate observations.
The full backend suite runtime may exceed a slice budget; do not call it setup
time. An unavailable host blocks only that host's acceptance, not independent
repository work. Report missing host proof explicitly.

## Proof commands

Run from the selected owned checkout. New filenames below are planned artifacts,
not existing tests. Tests execute real repository entry points with controlled
data; use subprocess substitutes only for external tools in focused script tests.
Installed pnpm, native-module readiness and host invocation need real observations.

| Key | Literal command | Signal |
| --- | --- | --- |
| P | `CURSOR_DEV=true nix develop -c bash scripts/test/worktree_setup.sh.test` | Planned Bach command-boundary proof for actual preparation. |
| S | `CURSOR_DEV=true nix develop -c bash scripts/test/dev_setup.sh.test` | Existing setup behavior preserved; extend real invocation where touched. |
| N | `CURSOR_DEV=true nix develop -c bash scripts/test/nix_shell_hook.sh.test` | Extend to invoke real hook; quiet success and visible setup failure. |
| L | `CURSOR_DEV=true nix develop -c node --test scripts/workspace-syncpack-lifecycle.test.mjs` | Existing lifecycle ownership remains valid; not a substitute for an actual install. |
| F | `CURSOR_DEV=true nix develop -c pnpm frontend:test` | Real frontend browser-mode unit suite after supported preparation. |
| T | `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit` | Frontend typecheck on the same content; reuse equivalent existing proof. |
| B | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` | Full backend suite on the worktree's own allocated test database. |
| I | `CURSOR_DEV=true nix develop -c pnpm test:backend-test-worktree` | Existing command routing, identity and admission regressions. |
| E | `CURSOR_DEV=true nix develop -c pnpm test:browser-worktree-isolation` | Preserve existing E2E ownership; run if its callers/allocation are implicated. |

Use `./scripts/run.sh bash scripts/worktree_setup.sh` as the planned public setup
command, a small wrapper over existing owners, not a second environment manager.
If extraction reveals a suitable existing public command, use it instead and
update this plan's command and host callers together.

## Ordered slices

### 1. Make repository tooling available in a fresh worktree
Type: Behavior. Status: done.

Fresh checkout lacking generated skill links → public preparation → tracked
repository tooling and Claude discovery links are usable from this checkout.
Compose existing `setup_claude_skills` with the small entry point; repeat safely.
Use tracked runtime entry paths where Donut controls their invocation. Do not
fork Open Dough's observer implementation or copy a local skill installation.

Proof: P with real temporary checkout and actual script path resolution. The
resolved source stays in this worktree; representative tracked scripts load.
Do not start a live CI observer merely to demonstrate tool discovery.

Delivered: new `scripts/worktree_setup.sh` (resolves its own repo root via
`BASH_SOURCE`, sources `scripts/shell_setup.sh`, calls `setup_claude_skills`;
no dependency install or service/daemon startup, `scripts/nix_shell_hook.sh`
unchanged) and `scripts/test/worktree_setup.sh.test` (real disposable
`git worktree add`, unmocked script execution run twice, symlink/SKILL.md
resolution asserted inside that same temp worktree, `trap cleanup EXIT` so
every exit path removes the disposable worktree/branch). Proof P passing;
full `scripts/test/run_all_script_tests.sh` 20/20.

### 2. Separate dependency preparation from interactive services
Type: Structure. Status: done.

Expose the existing dependency preparation independently from Biome restart and
service startup. Keep normal interactive setup calling the appropriate existing
pieces. Remove no callers yet. This immediately enables slice 3 to add dependency
readiness to the public entry point. No second fingerprint, package store or
runtime owner.

Proof: S/N/L as touched, including real invocation with external commands
observed. Existing interactive behavior and quiet command output remain intact.
The extracted preparation does not start/stop services or a foreign daemon.

Delivered: `scripts/dev_setup.sh:setup_pnpm_deps` extracted from
`setup_pnpm_and_biome` (byte-identical fingerprint-gated `pnpm
--frozen-lockfile recursive install` body); `setup_pnpm_and_biome` now calls
`setup_pnpm_deps` plus the unchanged NixOS Biome-patch step and
`restart_biome_daemon`. `scripts/nix_shell_hook.sh`'s one call site
unchanged. New real-invocation test in `scripts/test/dev_setup.sh.test`
proves `setup_pnpm_deps` alone installs once, writes the fingerprint, skips
on repeat, and never touches Biome. S 5/5, N 2/2, L 2/2, full suite 20/20.

### 3. Prepare the committed dependency graph for useful checks
Type: Behavior. Status: done.

Fresh worktree with warmed machine cache → public preparation → dependencies
required by F/B work, with unchanged manifests and lockfile. Compose the readiness
owner selected at the gate with slice 1's entry point. Preserve lifecycle/native
build requirements. Runtime commands retain lazy service/database provisioning.

Proof: P/L plus one real install and F/T/B through normal commands. Verify the
allocated test identity belongs to this checkout. Use the existing native
dependency consumers rather than testing a new cache implementation in isolation.

Delivered: `scripts/worktree_setup.sh` now also sources `scripts/dev_setup.sh`
and calls `setup_pnpm_deps` alongside `setup_claude_skills` (plus
`setup_logging`, required once `setup_pnpm_deps`'s `log()` calls are
composed in — without it `log` silently resolved to macOS's unrelated
`/usr/bin/log` and aborted under `set -e`). No service/Biome-daemon startup
added. `scripts/test/worktree_setup.sh.test` extended to assert real
dependency usability (`node_modules/.bin/biome` executable, fingerprint
written) after a real `pnpm --frozen-lockfile recursive install` against the
disposable worktree's committed, consistent manifests, with symlink and
dependency postconditions asserted separately. P/L pass; F 1938/1938; T
clean; B `BUILD SUCCESSFUL`, allocated worktree-scoped test DB identity
confirmed to belong to this checkout via its own `.worktree.local.json`.

### 4. Avoid redundant preparation without accepting stale state
Type: Behavior. Status: done (narrowed at execution — see below).

Prepared worktree → repeat setup and ordinary package command → no redundant
install/lifecycle pass. Relevant changed or incomplete state → same entry point →
valid readiness or a visible failure, never a false success. A corrected input
after a failed attempt can be prepared normally. This is one readiness rule,
not separate caches or host-specific bypasses.

Apply it consistently to existing install-prefixed root scripts and normal Nix
setup. Preserve each caller's actual task and lifecycle contract, including API
generation, CLI/MCP commands, formatting and nested scripts; do not add installation
to unrelated Git/read-only operations. Remove superseded fingerprint/prefix code
only when all its callers use the selected owner.

Proof: P at the command boundary with unchanged, mismatched workspace manifest,
valid paired dependency change and interrupted/partial install data. Reuse the
real pnpm gate, supplemented for the chosen owner. Observe actual install and
lifecycle invocation, not just equal output or elapsed time. Preserve propagation
of failures; test readiness bookkeeping/retry outcomes, not every external error.
Run N/L, one representative normal command for each distinct changed caller
contract, and F/T when frontend invocation changes. Reuse sufficient B/I evidence
unless backend routing changes. Record the inspected caller map here at execution.

**Narrowed at execution, per this slice's own "input diversity is a sizing
concern" warning.** Delivered: the readiness-gate proof half only. Added two
real command-boundary tests to `scripts/test/dev_setup.sh.test` proving
`donut_needs_pnpm_install`/`setup_pnpm_deps` correctly forces a real reinstall
(observed via the fake pnpm's call log, not just exit status) rather than a
silent false success on (a) an interrupted/partial install — `node_modules`
present, no fingerprint file, since the fingerprint is only written after a
*completed* install (`pnpm ... && donut_workspace_deps_fingerprint >file`) —
and (b) a stale/mismatched fingerprint, also confirming it gets corrected
afterward. No production logic changed: the existing `&&`-gated write and
`donut_needs_pnpm_install`'s existing guard clauses were already correct for
both cases; this closed a real proof gap, not a defect. The unchanged/valid
manifest-change fixtures from the before-change gate hit the same
fingerprint-mismatch code path as (b) and were judged not to need a third
near-duplicate test. S 7/7, P/N/L pass, full suite 20/20.

**Deferred to slice 9** (added below): applying the same one readiness rule
to the ~25 redundant-install-prefixed root `package.json` scripts and to
`.cursor/worktrees.json`. Investigation at execution found this set too large
and heterogeneous (two distinct install flavors — plain `install` vs.
`recursive install` — plus aggregate scripts like `lint:all`/`format:all`
that call sub-scripts which each also carry the prefix) to fix safely inside
this slice's original ~5-minute leaf without inventing per-script exceptions.
The full caller map is recorded under slice 9.

### 5. Prepare Cursor-created worktrees through the common behavior
Type: Behavior. Status: config delivered; host-integration proof unavailable
(no live Cursor app in this execution environment — see below).

Create a fresh worktree through Cursor → its configured setup invokes public
preparation → repository tooling/dependencies are ready without duplicate Nix
hook installation or global daemon restart. Update the existing worktrees config.

Proof: actual Cursor setup log and resulting checkout; record host version,
command, cwd, exit outcome and dependency use. P/N cover repository behavior;
invoking the JSON's command manually alone does not prove this host integration.
Document this supported path in existing development guidance in the same slice.

Delivered: `.cursor/worktrees.json`'s `setup-worktree` now runs
`CURSOR_DEV=true nix develop -c bash scripts/worktree_setup.sh` (quiet mode —
without it, `nix develop`'s own shellHook restarts the Biome daemon and starts
MySQL/Redis before the wrapped command even runs, which the refactor pass
caught by empirical comparison and which would have directly contradicted
this slice's own "without... global daemon restart" promise). Documented in
`.agents/agent-map.md`'s new "Worktree setup" section. Manual command run
(`CURSOR_DEV=true nix develop -c bash scripts/worktree_setup.sh`, the exact
JSON literal) exits 0, quiet, no daemon/service side effects; P/N/full suite
pass (regression, unaffected by a config/doc-only change). **Genuine
unclosed gap, honestly reported per the plan's own boundary ("An unavailable
host blocks only that host's acceptance, not independent repository work"):**
this execution environment (Claude Code, no installed Cursor app) cannot
produce actual Cursor setup-log proof — host version, real invocation cwd,
Cursor's own recorded exit outcome. That proof needs a session with real
Cursor access.

### 6. Prepare Codex-created worktrees through the common behavior
Type: Behavior. Status: planned. Target: ~5 minutes plus host observation.

Create a fresh worktree through the installed Codex app's configured local
environment → preparation completes in the worktree before useful commands.
Use the host-supported environment configuration; do not invent its file schema
or assume a generic create-worktree tool runs environment setup.

Proof: actual app setup invocation with cwd, version and outcome, then use the
prepared dependencies/tooling. Record the supported path in development guidance.
If the installed app has no compatible automatic entry point, explicitly document
and prove the same public command in a Codex worktree as the supported path.

### 7. Prepare Claude Code worktrees without replacing their lifecycle
Type: Behavior. Status: planned. Target: ~5 minutes plus host observation.

Enter a fresh worktree with Claude Code → supported preparation → repository
tooling and dependencies are available before use. Cover both an initially
launched worktree and a worktree entered during a session in the documented
workflow. Verify installed hook semantics before choosing automatic invocation.
Default fallback: run the public command in the new checkout, as documented.

Do not introduce WorktreeCreate/WorktreeRemove replacements for this purpose.
Do not infer the target checkout from an origin-root environment variable when
Claude's current worktree differs. Fix Donut-owned path assumptions needed for
the supported path; shared observer lifecycle redesign remains deferred.

Proof: real Claude session observations for the two entry situations; resolve
tooling to the intended checkout and use prepared dependencies. Record host
version, selected path, command and outcome. P covers common preparation, not
whether the host invokes it. Document any explicit command requirement honestly.

### 8. Preserve parallel work while establishing the final result
Type: Behavior. Status: planned. Target: ~5 minutes active work plus concurrent checks.

Two owned worktrees, with distinguishable dependency inputs/runtime identities →
prepare and run normal checks concurrently → each retains its dependencies,
build outputs and test data while peer processes remain alive. Extend existing
command-boundary isolation proof only where preparation changes an obligation;
do not add a new database/port allocator or registry.

Proof: P/I and E when implicated; real F/B in the two prepared worktrees, with
distinct test-database identities, local workspace resolution/build paths and
peer-owned process continuity. Existing allocator tests own port separation;
preparation must not start a shared replacement stack. A setup-only dry run does
not prove this result. The real check run also supplies final F/T/B acceptance
for each host path by reusing common-command evidence plus the host invocation
observations; do not require six identical full backend suite runs.

Repeat the comparable fresh/repeated setup measurements from the gate against
the final behavior, excluding test runtime. Report measured setup time/spread,
observed eliminated work, remaining cost and comparison limits. Keep temporary
measurement material only while needed; no permanent benchmark subsystem.

### 9. Apply the readiness rule to redundant install-prefixed root scripts
Type: Structure. Status: planned. Target: split into smaller leaves at
execution; do not attempt as one ~5-minute slice (see caller map below).

Deferred from slice 4 at execution 2026-09-21. Same one readiness rule
(slice 4's now-proven `donut_needs_pnpm_install`/`setup_pnpm_deps` fingerprint
gate), applied to existing callers that currently reinstall unconditionally
on every invocation instead of reusing it — not a second mechanism. Preserve
each caller's actual task and lifecycle contract; do not add installation to
unrelated Git/read-only operations; remove superseded prefix code only once
all its callers use the selected owner.

**Recorded caller map** (root `package.json` `scripts`, plus
`.cursor/worktrees.json`; nested `cli/`, `mcp-server/`, `frontend/`
`package.json` had no redundant-install callers of their own):

| Callers | Install prefix | Notes |
| --- | --- | --- |
| `mcp-server:bundle/test/format/lint`, `test-fixtures:format/lint`, `cli`, `cli:bundle/format/lint/test`, `frontend:build/format/lint/test:ui/test/test:watch/sut/dev/storybook`, `test`, `dev` | `pnpm --frozen-lockfile --silent recursive install &&` | ~19 entries; recursive-install flavor, same as `setup_pnpm_deps`. |
| `generateTypeScript`, `cy:format`, `cy:lint` | `pnpm --frozen-lockfile --silent install &&` (non-recursive) | Different install flavor from `setup_pnpm_deps` (which is recursive) — a naive substitution would change these three scripts' actual contract, not just skip redundant work. |
| `lint:all`, `format:all` | same prefix, and each also calls several of the scripts above, which redundantly reapply the prefix again within one invocation | Compounded redundancy; fixing this coherently likely means changing the prefix pattern in one place shared by all callers, not per-script edits. |
| `.cursor/worktrees.json` (`setup-worktree`) | `nix develop -c pnpm install` (plain, non-`CURSOR_DEV`, no fingerprint) | Real redundant-install caller, but this is slice 5's territory (Cursor host wiring), not this slice — resolve there instead of here. |

Suggested split (not binding — replan at execution): (a) prove
`setup_pnpm_deps` (or a thin wrapper) is safely invokable as a plain
`pnpm`-script prefix outside the interactive Nix shell hook, for both the
recursive and non-recursive install needs identified above; (b) apply it to
the ~19 recursive-flavor leaf scripts; (c) resolve `lint:all`/`format:all`'s
compounded prefix once the leaf scripts no longer need their own; (d) decide
`generateTypeScript`/`cy:format`/`cy:lint`'s non-recursive case separately,
since it is a different contract, not merely a smaller version of the same
one.

Proof: P at the command boundary as slice 4 already established (unchanged,
mismatched, valid paired change, interrupted/partial install all already
covered by slice 4's `scripts/test/dev_setup.sh.test` proof — reuse it, do
not re-derive). N/L regression. One representative normal command for each
distinct changed caller contract touched. F/T when frontend invocation
changes. Reuse slice 3's B/I evidence unless backend routing changes.

## Conditional optimization and stopping

After the required behavior and evidence above, stop if setup is reliable and
remaining cost does not justify more work. No arbitrary speed threshold decides
success. If a substantial measured cost remains, name that cost and propose at
most one bounded experiment (for example pnpm virtual-store reuse or prefer-offline).
Record exact comparison/compatibility proof and add a sized Behavior/Structure
leaf before changing production configuration. An unavailable or inconclusive
baseline does not automatically activate optimization. A novel cache service,
package-manager migration, or lifecycle redesign returns to scope discussion.

## Promise ownership and safe stops

| Promise | Owner |
| --- | --- |
| Fresh tracked tooling and skill discovery | 1; actual Claude path in 7 |
| Correct committed graph and representative checks | 3, final reuse in 8 |
| Cheap repeats, relevant changes and retry after failure (readiness-gate proof) | 4 |
| Redundant-install cleanup across existing root-script callers | 9 (deferred from 4) |
| Cursor / Codex / Claude supported invocation | 5 / 6 / 7 respectively |
| Independent mutable state and no peer disruption | 2's boundary; 8's final observation; existing I/E allocation proof |
| Comparable setup observations; bounded acceleration | Before-change gate, 8 and conditional decision |

Safe stops: after 1 usable tracked tooling; after 2 preserved existing behavior; after
3–4 a usable manual preparation path; after each host its complete supported
journey. Remaining host/parallel proof stays unfinished. Every delivery retains
working existing entry points. No story completion based solely on speed or a
setup command returning zero. Slice 9 (root-script redundant-install cleanup)
is itself a further safe stop within the "usable manual preparation path"
already delivered by 3–4: its own promise is cleanliness/consistency, not new
capability, so the story is not blocked on it.

All leaves target about five minutes including focused proof. Scrutinize longer
leaves; above ten minutes split the affected leaf before continuing unless the
recorded reason is external host access or required install/full-suite runtime.
Do not hide active implementation time behind that exception. Input/caller
diversity in 4 required a smaller behavior leaf once the gate selected the
owner — resolved at execution by narrowing 4 to its proof half and deferring
the caller-map work to 9, per this note's own anticipation.

## Delivery and remaining concerns

On explicit execution authorization follow `dough-execute-plan`: Taken claim at
actual start, owned execution checkout, Jidoka, fresh independent
`dough-post-change-refactor` agent, API generation only if triggered, coordinator
`./scripts/run.sh pnpm format:changed` once, update this plan, commit with the
check-only hook, push and asynchronous CI repair. Implementers/refactorers do
not run format:changed or standalone lint:changed. This planning session runs
none of that delivery flow. Do not add a broad test suite per tiny doc/config edit.

Remaining slice-specific concerns:

- 3–4: native verification versus frozen-lockfile/lifecycle contracts was
  resolved at the before-change gate (bare `pnpm exec` verify-before-run
  rewrites the lockfile on mismatch instead of failing; the existing
  frozen-lockfile fingerprint gate remains the one readiness owner). Resolved,
  no longer an open concern.
- 9: many root script callers have distinct purposes (two install flavors,
  compounded aggregate-script prefixes). The recorded caller map and scope of
  redundant-prefix removal can exceed one leaf; preserve their contracts and
  split rather than introduce per-package exceptions — see slice 9's own
  suggested split.
- 5–7: actual installed host access and hook timing are unproved. Missing access
  leaves host acceptance open; command/config inspection is not equivalent proof.
  Claude's mid-session entry is especially sensitive to original-root paths.
- 8: installed packages, browser prerequisite caching and shared-machine load
  affect timing. Report limitations and reuse matching evidence; no claimed
  speedup from unlike workloads or simulated infrastructure.

The cumulative design uses one dependency-readiness owner, existing tracked
tooling and existing runtime isolation. No new cache ledger, lifecycle manager
or host-specific dependency policy is intended. This assessment identifies
concerns; it does not certify execution readiness or authorize implementation.
