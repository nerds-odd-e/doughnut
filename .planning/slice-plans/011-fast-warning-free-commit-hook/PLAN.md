# Fast, warning-free commit feedback

## Source

- Story: [Commit with trustworthy checks averaging under five seconds](../../seeds/SEED-070-fast-warning-free-commit-hook.md#fast-warning-free-commit-hook)
- Identity: SEED-070#fast-warning-free-commit-hook
- Preparation workspace: `/Users/terryyin/git/doughnut/.worktrees/commit-with-trustworthy-checks-averaging-under-f`
- Branch: `codex/commit-with-trustworthy-checks-averaging-under-f`
- Preparing agent: Yua-chan; established assignment revision `3a79e41cde21eab66c4a9c0f7cd2dda4fb95590b`.
- Publication target: `origin/main`; integration checkout: `/Users/terryyin/git/doughnut`.
- Authority: the current request is planning only. This plan does not Take,
  execute, commit, publish, or release the preparation assignment.

## Goal and scope

Contributors get healthy local commit-hook feedback averaging strictly below
five seconds on the owner's prepared Mac, outside Nix, without tooling
warnings. Preserve affected-component lint/typecheck coverage, frontend checks
against staged source, visible failures that block commits, and source/index
preservation on success and failure.

Follow the story's fixed sample: five distinct checked inputs in each of
frontend, backend, CLI, MCP, shared fixtures, root scripts/E2E, OpenAPI, and
mixed frontend/backend. Frontend samples include Vue and TypeScript. Report
all 40 timings, the overall arithmetic mean, and class means/ranges. No-op,
inside-Nix, and fresh-check-cache observations are separate diagnostics;
their correctness and healthy-run warning requirements still apply.

Keep workspace-package reads from the working tree as they work today.
Extending staged isolation to other components or workspace dependencies,
changing lint/typecheck coverage, CI/product test-suite optimization, and a
background checker service are excluded. No per-class timing ceiling is added.

## Existing solutions and architectural constraints

PFE search covered `scripts/`, package commands, frontend configuration,
application/E2E callers, CI, agent guidance, and development documentation:

- Keep `scripts/git-hooks/pre-commit` as the outer gate and
  `scripts/quality_changed.sh` as the affected-component/check owner. Its
  format mode shares selection and must retain its working-tree behavior.
- Change the existing dependency-readiness owner in `scripts/dev_setup.sh`,
  rather than adding a second installation fingerprint. `setup_pnpm_deps`
  already installs once and reuses a completed-install fingerprint. Its
  callers include `worktree_setup.sh` and `nix_shell_hook.sh`; their tests are
  consumers of any extraction or changed freshness rule. The current key
  covers the root manifest, lockfile and workspace configuration, but not
  individual workspace manifests; its presence check only covers root
  `node_modules`. It needs assessment before replacing pnpm's verification.
- Reuse TypeScript's existing incremental implementation: root
  `tsconfig.json` enables `incremental`, and a real frontend check writes
  `dist/frontend/tsconfig.tsbuildinfo`. Today's new temporary path and deletion
  discard that reusable state. Re-run the compiler for every selected
  frontend check; do not create a separate cache of successful exit codes.
- Nix has a native recorded build-environment profile. Use that mechanism as
  the environment-reuse candidate rather than saving the contributor's whole
  environment. A non-Git snapshot of the current flake definition avoids
  treating unrelated source edits as dirty environment-definition input.
  [Nix profile documentation](https://nix.dev/manual/nix/2.34/command-ref/new-cli/nix3-develop#examples)
  describes recording and reusing such environments. This remains a measured
  hypothesis, subject to the early probe below.
- pnpm's automatic verification can attempt installation before `run`/`exec`,
  and its default can continue with a warning after installation fails.
  [pnpm's verification documentation](https://pnpm.io/settings/build#verifydepsbeforerun)
  supports separating that automatic step from an explicitly verified
  installation. Any override must be scoped to an already-validated borrowed
  dependency context, not applied globally.

The ADR index and relevant in-file statuses agree:
[ADR 0006, Usage](../../../docs/adrs/0006-failure-handling-accepted.md#usage)
supports propagating actionable failures; do not catch a failed gate merely
to continue. [ADR 0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
requires proven resource ownership: these observations use owned disposable
Git/file state and must not start or mutate Development/Production services.
The existing North Star topics concern notebooks, attachments and audio; none
governs this local tooling change. No new North Star topic or ADR is warranted.

## Outside-in proof

| Promise | Owner | Observable proof |
| --- | --- | --- |
| Reproducible representative comparison | 1–2 | Driver exercises installed hooks on 40 actual staged changes; baseline preserves every time, warning, revision and environment condition before changes. |
| Healthy environment entry has no dirty-tree/tooling warnings | 3–4, then 8 | Real installed hook outside Nix with ordinary staged changes and unchanged flake inputs; environment versions match the current definition. Changed definition rebuilds instead of trusting old state. |
| Dependency freshness survives removal of redundant installation attempts | 5–6 | Real public preparation/check entry points with a matching installation, changed relevant manifests/settings, and missing installed package artifacts; fresh installation reused, stale/incomplete installation repaired or failed visibly. |
| Reported unsafe-modules/install warning has a cause-based remedy | 5–6, then 8 | Reproduce its real pnpm condition in owned disposable state before changing the path; rerun that condition and inspect actual diagnostics and check completion. An already-passing case is not remedy proof. |
| A staged frontend error blocks despite an unstaged correction | 7 | Real installed hook and real Biome/compiler: staged error, valid working-tree replacement → nonzero exit and relevant diagnostic. |
| Invalid unrelated unstaged/untracked frontend source cannot spoil a valid staged check | 7 | Real installed hook: valid index, invalid unrelated working-tree/untracked source → successful staged check. |
| Reused state still finds a newly staged lint/type error | 7 | Success → different staged error → failure → staged repair → success; re-run actual gates throughout. |
| Removed/renamed source and changed configuration/dependency inputs are checked correctly | 7 | Successive real staged snapshots, including a removed imported file and an intentional configuration/dependency change; result agrees with a fresh check. |
| Other selected gates still block, and mixed commits check both components | 6–7, then 8 | Dispatcher boundary tests for routing plus real selected-tool failures and mixed frontend/backend hook runs; no mocked tool establishes real gate correctness. |
| Source files and real index stay unchanged on success/failure | 4, 6–8 | Compare status, staged binary diff/index entries, working-tree diff and crafted untracked-file contents before/after hook execution in an owned disposable repository. |
| Overall mean is strictly below five seconds, with all healthy samples warning-free | 8 | Same 40-case corpus as baseline, installed hook invocation-to-exit timing includes environment entry, dependency work, checks and cleanup; report individual times and aggregate. |
| Inside-Nix and fresh-cache outcomes retain correctness and clean diagnostics | 8 | Separate class diagnostics, with the reset caches named and tooling/dependencies already installed. |

Existing fast boundary commands, run through Nix:

```sh
CURSOR_DEV=true nix develop -c bash scripts/test/pre-commit.test
CURSOR_DEV=true nix develop -c bash scripts/test/quality_changed.test
CURSOR_DEV=true nix develop -c bash scripts/test/dev_setup.sh.test
CURSOR_DEV=true nix develop -c bash scripts/test/worktree_setup.sh.test
CURSOR_DEV=true nix develop -c bash scripts/test/run.sh.test
CURSOR_DEV=true nix develop -c bash scripts/test/nix_shell_hook.sh.test
```

Extend the existing Bach suites at their real command boundaries, substituting
only external executables where appropriate. `run.sh.test` currently simulates
the runner's conditionals; replace that simulation with invocation of the real
runner when changing its behavior. Real installed-hook acceptance complements
these fast tests. No application/browser/backend unit suite is a local gate for
changes confined to this tooling; run one if actual implementation touches its
behavior, and load that area's skill then.

The planned driver is `scripts/profiling/profile-commit-hook.sh`, with a Bach
test under `scripts/test/`. Its initial commands are:

```sh
bash scripts/profiling/profile-commit-hook.sh baseline <local-evidence-directory>
bash scripts/profiling/profile-commit-hook.sh acceptance <local-evidence-directory>
```

These are new commands owned by slice 1, not commands already observed to work.
The driver must support the host Bash for outside-Nix entry and time the actual
installed hook, not just the quality dispatcher. Run the driver outside Nix for
that sample; ordinary repo checks continue using the required Nix prefix. The
driver owns disposable fixtures, package installations and cache resets. Never
use another checkout's mutable installation for a probe that can repair it.
Raw logs and timings stay local; retain compact, literal-command evidence and
consequential decisions in this plan.

## Decisive premises and observations

Observed on 2026-10-07 at product revision `3a79e41cde`; only this story's
preparation prose was changed. Host: Apple M4 Max, 48 GiB, macOS 26.6.2
(`25G83`). Nix environment reported Node `v26.10.0`, pnpm `11.28.5`.
Background load was not controlled, so these are probes, not acceptance data.

| Premise | Consumed by | Literal observation and result |
| --- | --- | --- |
| Installed hook follows the tracked outer entry point | All slices | `cmp scripts/git-hooks/pre-commit "$(git rev-parse --git-path hooks/pre-commit)"` passed; read hook → `run.sh pnpm lint:changed` → `lint_changed.sh` → `quality_changed.sh`. |
| Full hook on a genuine frontend staged change is slow here | 2, 7 | Disposable `GIT_INDEX_FILE`: `git read-tree HEAD`, then `git hash-object -w --stdin` and `git update-index --add --cacheinfo` add valid `frontend/src/commitHookTimingProbe.ts`; `/usr/bin/time -p env GIT_INDEX_FILE=<index> <installed-hook>` passed in 8.95 s. No-staged-change invocation passed in 0.90 s. Real status and binary diffs were unchanged. |
| Dirty-tree warning reaches an ordinary healthy hook | 4 | Both full-hook observations above emitted Nix's dirty-tree warning before the runner. The source of the warning is the flake evaluation, not Biome/typechecking. |
| Historical pnpm warning reproduces in this prepared worktree | 5's probe, then 6 | False for the frontend invocation above: real pnpm, Biome and compiler passed without the unsafe-modules/install warnings. Preserve the seed's older primary-checkout evidence; reproduce the relevant installation/snapshot condition in disposable state before claiming a fix. |
| Native incremental checking speeds changed input and still finds an error | 7 | `CURSOR_DEV=true nix develop -c python -` exported the tracked index to a temporary stable directory, borrowed root/frontend dependencies, wrote a probe TS file, and invoked the real `frontend/node_modules/.bin/vue-tsc --noEmit` there. First valid run: 15.86 s; changed valid boolean: 4.37 s; changed `string = true`: exit 2, TS2322, 4.29 s. Build info appeared at `dist/frontend/tsconfig.tsbuildinfo`. Temporary snapshot removed; real repository unchanged. This settles the compiler mechanism, not production snapshot synchronization. |
| A non-Git flake-definition snapshot avoids dirty-source diagnostics | 3–4 | Copy current `flake.nix`/`flake.lock` into a physical temporary path, then `CURSOR_DEV=true nix develop path:<snapshot> --no-write-lock-file -c bash -c 'node --version; pnpm --version'`: matching versions, no warning. A raw macOS `/var` symlink path failed, so resolve a physical path. Entry times varied (3.39, 9.51, 13.80 s); fresh path evaluation is not an established speed solution. |
| A native recorded environment can be consumed again | 3–4 | Same definition snapshot with `nix develop path:<snapshot> --no-write-lock-file --profile <profile> -c bash -c 'node --version; pnpm --version'`, followed by `nix develop <profile> -c ...`: matching versions throughout. Population 5.28 s; first reuse 47.11 s fetched the global Nix registry; second reuse 1.82 s. All temporary state cleaned. Slice 3 must bound that bootstrap/registry behavior before choosing this route. |
| Existing boundary fixtures exercise staged selection, preservation and dependency preparation | 4, 6–7 proof | Ran `CURSOR_DEV=true nix develop -c bash -c 'for check in scripts/test/pre-commit.test scripts/test/quality_changed.test scripts/test/dev_setup.sh.test scripts/test/worktree_setup.sh.test; do bash "$check" || exit; done'`: 15 tests passed. Inspected setups/assertions: hook tests replace runner; dispatcher tests replace pnpm; dependency/worktree tests invoke real preparation with external-tool substitutes. They do not prove real compiler failures or historical warning removal. |
| Shared dependency-owner consumers have been found | 6 | `rg -n 'setup_pnpm_deps|quality_changed|pre-commit|lint:changed' scripts .github e2e_test .agents/agent-map.md`: preparation/shell-hook callers and the suites named above. No E2E feature invokes these commit/dependency-preparation entry points. |

The full 40-case distribution, recorded environment invalidation/bootstrap,
and stale-install warning condition require state-changing disposable fixtures
and are early probes in slices 2, 3 and 5 respectively. Do not substitute the
small observations above for their completion. A false probe result stops its
dependent slices and requires changing this plan before broader implementation.

## Current decisions and cumulative design

- One rule: reuse only state owned by the operation and current for its inputs.
  Keep environment readiness, installed dependencies and compiler incremental
  state with their existing owners; these are different inputs/lifecycles, not
  three copies of the same success cache.
- Use current environment definitions, not a committed revision that ignores
  intended edits. A cache miss must propagate failures and publish reusable
  state only after preparation succeeds. Keep mutable state per worktree and
  avoid overlapping writers to a reusable snapshot. Never cache CLI credentials
  or the caller's arbitrary environment.
- A stable frontend view must exactly mirror the current tracked index, removing
  stale source paths while keeping only deliberate compiler bookkeeping. Keep
  generated/configuration inputs the index already supplied and the existing
  workspace-package dependency boundary. Concurrent invocation must not read a
  partially refreshed view; do not add a background service to solve this.
- The dependency owner must account for relevant workspace manifests/settings,
  tool version and installation completeness before a scoped bypass of pnpm's
  automatic snapshot verification is safe. Preserve check-only behavior: any
  preparation that would modify tracked source must fail with a useful repair
  instruction rather than rewriting it during the hook.
- No warning filter, blanket quieting, removed gate, changed sample weighting,
  or selective slow-run exclusion is an optimization. Fix the context that
  creates an inappropriate installation/evaluation attempt.
- If the measured workload already reaches the target after earlier slices,
  reassess the remaining optimization before adding state it does not need.
  Preserve all proof obligations and the original sample; do not claim the story
  complete until slice 8's integrated proof passes.

## Ordered slices

### 1. Repeatable installed-hook workload
Type: Structure
Status: planned
Proof: focused Bach driver test verifies actual staged/checked input variation,
outer-hook invocation, preservation of all sample results, and owned cleanup;
no performance threshold is asserted with substituted tools.

Structure: a small driver prepares disposable Git/package state and the fixed
40-case corpus, records full-hook timings/diagnostics and input identities, and
compares source/index state around calls. Compose existing public worktree
preparation rather than reimplementing package setup. Keep the driver and its
test together. This immediately enables slice 2's baseline without changing
the gate. Do not build a general benchmarking framework or automatic optimizer.
Sizing hypothesis: 5–8 minutes including focused proof. Keep setup narrow; if
the driver itself exceeds 10 minutes, finer-decompose before continuing.

### 2. Establish the current representative hook baseline
Type: Behavior
Status: planned
Proof: `bash scripts/profiling/profile-commit-hook.sh baseline <local-evidence-directory>`;
40 healthy cases, exact environment/inputs and diagnostic output retained.

Behavior: on the prepared Mac, invoke the installed original hook for the
fixed corpus → the contributor can see a reproducible baseline, class costs
and diagnostics. Preserve original inputs/results before optimizing; do not
substitute the small planning probes for this comparison. Prepare negative
correctness examples before the slices changing their paths.
Sizing: ~5 minutes of setup/analysis. The required 40 real checks may exceed
10 minutes wall time; this is a focused-measurement exception because reducing
the corpus changes the promised evaluation. Driver implementation is owned by
slice 1, not hidden in this exception.

### 3. Prove current-definition environment reuse
Type: Behavior
Status: planned
Proof: owned disposable current/changed flake definitions, native-profile
population/reuse, matching Node/pnpm/Java tools, bootstrap/registry provenance,
visible failure, and the caller's working-directory/argument behavior.

Behavior: the current environment definition is reused, then changed → the
probe demonstrates the exact environment each invocation consumes, the actual
entry costs and invalidation, with provisioned inputs and clean diagnostics.
Explain the observed first-profile registry fetch rather than assuming every
reuse is fast or pinned. This state-changing probe is the prerequisite for
slice 4; failure stops that dependent change and returns the approach to this
plan. It does not install a candidate runner in a shared checkout.
Sizing: ~5 minutes, one environment-fidelity probe; slow external Nix bootstrap
is an external-wait exception, not an allowance for open-ended investigation.

### 4. Enter the current development environment without dirty-source warnings
Type: Behavior
Status: planned
Proof: real runner/installed-hook calls outside and inside Nix, changed
environment-definition inputs, visible preparation/child-command failure,
source/index preservation, and the affected runner/shell-hook boundary suites.

Behavior: ordinary source edits are staged, environment definition is unchanged
→ use the validated current Nix build environment without reevaluating a dirty
product-source tree or unrelated bootstrap work → execute the same checks with
the current pinned tools. A changed definition rebuilds before checking.
Implement the smallest native-profile reuse that slice 3 supports, in the
existing runner ownership; preserve no-Nix and already-inside-Nix paths and
the caller's working directory/arguments. Keep the general interactive shell
on its existing path unless this result needs a shared change.
Sizing: 5–8 minutes; scrutinized as one environment-entry proof loop, with
probe results already available. Interim hook still has dependency/typecheck
costs, addressed by slices 6 and 7.

### 5. Explain the borrowed-snapshot installation warning
Type: Behavior
Status: planned
Proof: reproduce the real pnpm unsafe-modules/install diagnostic in owned
stale/incomplete installation and borrowed-source fixture state; record the
trigger, attempted operation, dependency state and actual check outcome.

Behavior: the historical installation/snapshot condition is reconstructed →
the contributor can see which verification/installation attempt creates the
warning and which freshness/completeness facts a replacement must retain.
The healthy prepared-worktree observation did not reproduce it. If the
historical condition remains unexplained, stop slice 6 and return the missing
premise; do not claim a remedy from an already-passing run. This probe may repair
only its own installation and does not alter another checkout's modules.
Sizing: ~5 minutes, one diagnostic reproduction loop; if it cannot be bounded
within 10 minutes, retain evidence and reassess instead of continuing guesses.

### 6. Validate one installation before checking borrowed staged source
Type: Behavior
Status: planned
Proof: public preparation/quality boundaries, matching/stale/incomplete
installation cases, the reproduced pnpm condition from slice 5 with real tools,
all selected-component routing, visible failure and source/index preservation;
run dependency, worktree, shell-hook and dispatcher consumer suites.

Behavior: a selected component needs workspace tools → the existing dependency
owner establishes one usable current installation → component checks reuse it;
the borrowed frontend copy does not attempt to reinstall the checkout's modules
from a different project root. Change/modularize `setup_pnpm_deps` as necessary,
then compose it into the quality path. If automatic snapshot verification is
disabled, scope that override to this verified borrowed context. Remove redundant
installs only through this owner, preserving the normal check commands and
failure semantics. Align touched package wrappers and existing caller guidance.
Sizing: 5–8 minutes, one dependency-readiness proof loop; the real warning
condition and required freshness inputs must already be established in slice 5.
Frontend performance remains provisional until slice 7.

### 7. Check successive staged frontend versions with reusable compiler state
Type: Behavior
Status: planned
Proof: real installed-hook sequence with valid → newly staged lint/type error
→ repair, opposed staged/unstaged versions and invalid untracked source,
removed/renamed inputs and changed configuration/dependency inputs; compare
reused-state outcomes to fresh checks and verify source/index preservation.
Run the full affected hook/dispatcher boundary suites, not helper-only tests.

Behavior: the frontend index changes between commits → refresh one owned,
complete staged view and run Biome plus the real incremental compiler → the
current staged version decides success/failure while usable compiler work is
retained. Eliminate stale source paths and partial-refresh exposure in this
same change. Keep private bookkeeping outside source/the real index and scoped
to the worktree. Update lint guidance that currently describes disposable copies
and cold typechecking; keep the scope of workspace dependency reads accurate.
Sizing: 5–8 minutes after the observed compiler mechanism and preceding probes;
one staged-check correctness/reuse loop. If safe snapshot synchronization cannot
fit the hard limit, stop and refine this leaf before continuing.

### 8. Demonstrate fast, clean ordinary commits across the fixed mix
Type: Behavior
Status: planned
Proof: `bash scripts/profiling/profile-commit-hook.sh acceptance <local-evidence-directory>`
on the same 40 input cases/environment as baseline, all warning-free and correct,
overall mean strictly below five seconds; separate no-op, inside-Nix and named
fresh-cache class diagnostics. Retain failures/slow samples and every timing.

Behavior: a contributor performs the representative ordinary staged changes
→ all existing affected gates run, failure examples remain blocking, files/index
are preserved, and healthy feedback meets the complete story's timing/diagnostic
contract. Record comparison, literal commands and proof boundaries in this plan.
If the target fails, retain the real result and refine the remaining bottleneck;
do not mark this slice done or silently relax scope. No additional optimization
is prescribed until that observation warrants it.
Sizing: ~5 minutes of setup/analysis; the fixed repeated acceptance and cold
diagnostic checks have the same focused-measurement exception as slice 2.

## Plan refinement

The initial six-leaf draft's slice 2 combined the representative baseline,
Nix lifecycle probe and historical pnpm reproduction. Split those independent
proof loops into slices 2, 3 and 5 and remap their dependent implementation and
all promise ownership. Retain the other cohesive boundaries. Eight leaves
remain; no story resplit is recommended. Measurements in 2/8 and external Nix
bootstrap in 3 have explicit exceptions; implementation/setup do not.

The simplest alternative is eliminating redundant setup while keeping cold
frontend checks. Use slice 2's distribution to assess that alternative before
adding compiler state; reassess any remaining optimization that the full target
already makes unnecessary. Preserve the same final proof mappings when revising
remaining leaves. The current design remains supported by distinct existing
owners, with unobserved state-changing premises bounded by early probes.
No further slice-specific boundary, cumulative-design or proof concern was
identified in this planning review. This is preparation judgment, not proof
of achieved speed or warning removal and not execution authority.

## Delivery and sizing gates

Repository instructions target ~5-minute leaves including proof and cleanup;
scrutinize >5 minutes, and finer-decompose >10 minutes except the explicit
focused-measurement exceptions above. Preserve overrun evidence; renaming a
leaf or retrying does not erase it. Stop-safe states keep all existing gates
working, with temporary costs explicitly replaced by later slices.

When execution is separately authorized, follow the repository's required
delivery sequence: Jidoka → fresh `dough-post-change-refactor` agent → API
generation if actually needed → coordinator's single
`./scripts/run.sh pnpm format:changed` pass → plan update → commit with the
independent check-only hook → push and owned asynchronous CI repair. The
planner/implementer/refactorer must not add standalone `lint:changed` as routine
wrap-up proof. Commit-hook invocations above are deliberate behavior/measurement
proof. Changing the shared installed hook must not replace the shared installation
for other sessions merely to run a candidate: install it in owned fixture state.
Keep spent source/plan history for retrospective and story wrap-up.

## Learnings

Planning observations above narrow the candidates. No execution slice is done;
no acceptance mean or historical warning remedy has been established.
