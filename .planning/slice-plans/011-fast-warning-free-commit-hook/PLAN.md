# Fast, warning-free commit feedback

## Source

- Story: [Commit with trustworthy checks averaging under five seconds](../../seeds/SEED-070-fast-warning-free-commit-hook.md#fast-warning-free-commit-hook)
- Identity: SEED-070#fast-warning-free-commit-hook
- Preparation workspace: `/Users/terryyin/git/doughnut/.worktrees/commit-with-trustworthy-checks-averaging-under-f`
- Branch: `claude/commit-with-trustworthy-checks-averaging-under-f`
- Preparing agent: Kirara-chan; established assignment revision `a9cfacda3477ddb106d842dccc62f17a73d90923`.
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
all 40 timings, the overall arithmetic mean, and class means/ranges. The
documentation-only no-component commit, one inside-Nix invocation per class,
and one fresh-check-cache invocation per class are separate diagnostics;
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
Status: done
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

Accepted proof: `CURSOR_DEV=true nix develop -c bash scripts/test/profile-commit-hook.test`
(1/1; `test-profiles-every-corpus-case-through-the-installed-hook-and-cleans-up`
runs the driver with host `/bin/bash` against a `git clone --shared` of HEAD,
real `worktree_setup.sh`, hook and runner, substituting only `nix`/`pnpm`; one
exact-match assertion over 8×5 corpus, 40 distinct inputs including `.ts`/`.vue`,
57 hook calls in the fixture whose checked input equals each recorded input,
kept failing/warning run, recorded preservation violation, summary contents
and fixture cleanup). `run_all_script_tests.sh`: 21/21. Implemented in
`scripts/profiling/profile-commit-hook.sh` and its fixed corpus
`scripts/profiling/commit-hook-corpus.sh`. Elapsed ~10.5 min implementation
plus ~1.5 min refactor: a marginal hard-limit overrun on one coherent driver;
not refined because the slice converged with complete proof.

### 2. Establish the current representative hook baseline
Type: Behavior
Status: done
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

Accepted baseline (2026-10-07, revision `f53bee8004`, Apple M4 Max 48 GiB,
macOS 26.6.2 `25G83`, host bash 3.2.57, git 2.50.1, Nix 2.30.2; fixture Node
v26.10.0, pnpm 11.28.5, OpenJDK 25.0.3; load 5.27→8.10, uncontrolled):
`bash scripts/profiling/profile-commit-hook.sh baseline <job-tmp>/commit-hook-baseline`
outside Nix. All 57 runs exit 0 with real tools; 0 preservation differences.
No corpus fix was needed.

| Class | Corpus timings (s) | Mean | Fresh-cache | Inside Nix |
| --- | --- | --- | --- | --- |
| frontend | 9.648 10.085 10.684 10.335 9.844 | 10.119 | 14.462 | 8.291 |
| backend | 3.595 3.973 3.968 3.811 3.757 | 3.821 | 12.170 | 1.270 |
| cli | 3.880 4.019 4.399 4.207 4.593 | 4.220 | 7.276 | 1.484 |
| mcp-server | 3.776 4.962 5.139 4.174 4.629 | 4.536 | 4.796 | 1.529 |
| test-fixtures | 3.652 3.967 4.123 4.154 3.958 | 3.971 | 5.943 | 1.394 |
| root | 3.653 4.827 4.168 4.319 4.385 | 4.270 | 4.874 | 1.618 |
| openapi | 3.172 3.515 3.847 3.731 3.963 | 3.646 | 3.823 | 0.961 |
| mixed | 10.664 11.019 11.292 11.307 11.367 | 11.130 | 12.665 | 9.166 |

Overall corpus mean **5.714 s**. No-component (`docs/nix.md`): 3.653 s.
Fresh-cache reset `dist backend/build backend/.gradle`; the backend
fresh-cache run also started a new Gradle daemon. Warnings: Nix's
`warning: Git tree '<fixture>/checkout' is dirty` in every one of the 49
outside-Nix hook runs and nothing else; inside-Nix hook output had none.
`ERR_PNPM_UNSAFE_MODULES_DIR` and the install warning did not occur
anywhere. Gradle's "Consider enabling configuration cache" is informational.
Outside minus inside-Nix suggests ~2.5–3.0 s of Nix entry/runner startup per
run; frontend costs ~8.3 s even inside Nix (Biome ~0.1 s, the rest vue-tsc
plus pnpm/staged-copy/install overhead); inside Nix, non-frontend checks
(1.0–1.6 s) include a redundant frozen-lockfile pnpm install.

Negative examples for slices 6–8 (run in the driver fixture through the
hook dispatch): (1) staged `debugger;` in `frontend/src/colors.ts`
(`noDebugger: error`) → nonzero, Biome names the file; (2) staged
`export const commitHookTypeProbe: string = true` in
`frontend/src/utils/reservedReadmeTitles.ts` with unstaged `= "ok"` → nonzero,
TS2322; (3) valid staged frontend-3 plus untracked
`frontend/src/profileInvalidUntracked.ts` (`const n: number = "x"; debugger;`)
and an invalid unstaged edit to `frontend/src/composables/modalTopAnchor.ts` →
exit 0, untracked/unstaged state unchanged; (4) other gates: misformatted Java
in `DonutApplication.java` (spotless), type error in `cli/src/terminalColumns.ts`,
`debugger;` in `mcp-server/src/helpers.ts`, invalid `$ref` in
`open_api_docs.yaml`, and mixed valid frontend + backend violation → nonzero.
Slice 7 sequences reuse (1) and (2) as valid → error → repaired.

### 3. Prove current-definition environment reuse
Type: Behavior
Status: done
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

Accepted probe (2026-10-07, Nix 2.30.2, owned temp snapshots/profiles, no
repo change): the definition is exactly `flake.nix` + `flake.lock` (the
shellHook sources `./scripts/nix_shell_hook.sh` at runtime from the working
directory, so hook-script edits need no rebuild).

- Populate: `CURSOR_DEV=true nix develop "path:$SNAP" --no-write-lock-file --profile "$PROF" -c true`
  → 2.56 s new, 2.62 s changed definition (prints a `building …donut-env.drv`
  progress line), no warnings. A broken definition fails visibly (rc 1,
  `undefined variable`) and writes no profile.
- Consume: `CURSOR_DEV=true nix develop "$PROF" --inputs-from "path:$SNAP" --option flake-registry "" -c "$@"`
  → 0.439–0.447 s over five runs, empty stderr, no downloads; child exit
  status (7) propagates; caller cwd and arguments with spaces/quotes/globs
  preserved. Clean-tree plain `nix develop -c true` is 0.67–0.69 s for
  reference.
- Registry fetch cause: `nix develop <profile>` evaluates
  `flake:nixpkgs#bashInteractive` for the interactive bash and, with no flake,
  resolves `nixpkgs` through the global registry (`--debug`: lookup →
  `nixpkgs-unstable/nixexprs.tar.zst`, 38 MB), refetching per `tarball-ttl`
  (1 h) — unpinned and occasionally 20–47 s. `--inputs-from path:<snapshot>`
  pins it to the locked input; `--option flake-registry ""` avoids the global
  registry. Registries disabled alone fall back to host bash 3.2, which fails
  (syntax error, rc 2). Do not add `--offline`: a GC'd bashInteractive must
  remain re-substitutable.
- Fidelity: tool versions and `env | sort` identical to plain `nix develop`
  (node v26.10.0, pnpm 11.28.5, openjdk 25.0.3, bash 5.3.9); the recorded
  shellHook runs; under `CURSOR_DEV=true` it only sets shell/env vars and the
  skills link (no install or services).
- Invalidation key: sha256 of `flake.nix`+`flake.lock`; changed definition →
  new env store path; unchanged → same. `--profile` registers an auto GC root
  (do not rename the profile directory afterwards: it dangles the root).
  Missing profile fails visibly (rc 1).
- Concurrency: three simultaneous populates of one profile and four
  simultaneous consumes all succeeded, no `database is busy`; Nix locks and
  swaps the profile link atomically, so the link is the ready marker.
- Gaps: dirty-tree plain-entry cost not separately measured (baseline
  stands); first consume after a nixpkgs bump downloaded the pinned tarball
  once (~5.5 s); post-GC behavior reasoned, not run.

### 4. Enter the current development environment without dirty-source warnings
Type: Behavior
Status: done
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
Design from slice 3: in `scripts/run.sh`'s "Nix present, not inside Nix"
branch, key per-worktree state by the definition hash, populate a profile
from a physical snapshot of `flake.nix`/`flake.lock` when absent (the profile
link is the ready marker; key-named directories avoid overwrites), then
consume it with `--inputs-from path:<snapshot> --option flake-registry ""`.
Sizing: 5–8 minutes; scrutinized as one environment-entry proof loop, with
probe results already available. Interim hook still has dependency/typecheck
costs, addressed by slices 6 and 7.

Accepted proof: `scripts/run.sh` keys a per-worktree profile under
`git rev-parse --git-path donut-nix-env/<git hash of flake.nix+flake.lock>`,
removes stale sibling keys, snapshots the definition by temp file + rename,
populates on miss and consumes with `--inputs-from`/empty registry.
`CURSOR_DEV=true nix develop -c bash scripts/test/run.sh.test` 6/6 (real
runner copy, only `nix` substituted: no-Nix, inside-Nix, populate once then
reuse with exact args/snapshot/cwd/args, changed definition, visible populate
failure running nothing, child exit 7); pre-commit 2/2; nix_shell_hook 2/2;
`run_all_script_tests.sh` 21/21. Real outside-Nix disposable worktree: no
dirty-tree warning; backend hook 6.08 s populate → 2.03/1.76 s reuse
(baseline 3.82); frontend 9.19/8.42 s (baseline 10.12); staged `debugger;`
in `mcp-server/src/helpers.ts` → exit 1 with Biome diagnostic; source/index
hashes unchanged; changed definition rebuilt (3.42 s) then hit (0.52 s);
inside Nix runs directly. CI workflows do not use Nix (no-Nix path).
Remaining: a populate run prints `<<running within nix env>>` twice (once to
stderr); flipping between two definitions rebuilds each time (~2.6 s, cached).

### 5. Explain the borrowed-snapshot installation warning
Type: Behavior
Status: done
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

Accepted probe (real pnpm 11.28.5, owned disposable worktrees/copies, no repo
change; another checkout's modules never touched):

- Trigger: pnpm's automatic `verifyDepsBeforeRun` install for
  `pnpm -C <copy>/frontend lint` in the borrowed copy. pnpm sets
  `pnpm_config_verify_deps_before_run=false` for a script's children
  (`createExtraEnv`), so the real hook (`run.sh pnpm lint:changed` →
  `quality_changed.sh`) never verifies; the seed's historical command ran
  `./scripts/run.sh bash scripts/quality_changed.sh lint` directly, which
  verifies. Reproduced exactly that way (and with the copy outside the
  checkout's store location): `ERR_PNPM_UNSAFE_MODULES_DIR … Refusing to remove
  the modules directory at "<wt>/node_modules" …` then
  `WARN The install that runs before scripts failed …`; Biome and vue-tsc
  still ran, exit 0.
- Mechanism: the copy's absolute project paths never match
  `node_modules/.pnpm-workspace-state-v1.json`, so verification always
  installs; when the copy's store location (derived from its path) differs
  from `.modules.yaml`, the purge is refused (the warning). When the stores
  match, the "successful" verification silently rewrites the borrowed
  workspace state and relinks `frontend/node_modules/donut-test-fixtures` into
  the temporary copy, breaking the checkout's install after cleanup (later
  TS2307). Verification in a borrowed copy is never meaningful.
- Facts pnpm checks (a replacement must keep): workspace-state settings
  (mostly `pnpm-workspace.yaml`), configDependencies, project set
  (names/versions), a modules dir per project with dependencies,
  patches/pnpmfiles, manifest/lockfile changes against `node_modules/.pnpm/lock.yaml`,
  and at install time layout version, store/virtual-store location and pnpm
  version.
- `setup_pnpm_deps` fingerprint gaps: workspace manifests (e.g.
  `frontend/package.json`), pnpm/Node version, store location, completeness.
  The frozen install covers the manifest gap by failing, but `--silent`
  hides `ERR_PNPM_OUTDATED_LOCKFILE`. A deleted direct-dependency link is
  noticed by neither (fails later as TS2307).
- Redundant installs in the hook path: `quality_changed.sh`
  `lint_frontend_index`'s `pnpm --frozen-lockfile --silent recursive install`,
  and the cli/mcp-server/test-fixtures `*:lint` and root `cy:lint` scripts'
  own installs, ~0.2–0.3 s each per selected component.

### 6. Validate one installation before checking borrowed staged source
Type: Behavior
Status: done
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
From slice 5: scope `pnpm_config_verify_deps_before_run=false` (or the
equivalent flag) explicitly to the borrowed-copy commands so every entry path
(including direct `quality_changed.sh`) is safe; make the dependency owner's
freshness cover workspace manifests and the pnpm/Node version so its single
verified install can replace the per-component installs; keep install
failures actionable (no `--silent` hiding `ERR_PNPM_OUTDATED_LOCKFILE`).
Proof re-runs the slice-5 direct command and shows no warning and an
unchanged borrowed installation.
Sizing: 5–8 minutes, one dependency-readiness proof loop; the real warning
condition and required freshness inputs must already be established in slice 5.
Frontend performance remains provisional until slice 7.

Accepted proof: `setup_pnpm_deps` fingerprint now covers lockfile, root
manifest, `pnpm-workspace.yaml`, every workspace project manifest and the
resolved `node`/`pnpm` paths; completeness requires `.modules.yaml` and a
`node_modules` per project with dependencies; the frozen install is no longer
silent and the fingerprint is written only after success.
`quality_changed.sh` lint mode runs the owner once when a workspace-tool
component is selected (not for backend-only), routes to the check commands
directly (public `*:lint`/`cy:lint` scripts unchanged for manual use), and
scopes `pnpm_config_verify_deps_before_run=false` to the borrowed-copy
command. The hook also scopes it to its outer `pnpm lint:changed`: in real
proof, pnpm's automatic verification otherwise ran a non-frozen install that
rewrote `pnpm-lock.yaml` for a manifest change missing from the lockfile and
the hook passed (pre-existing check-only breach). Suites:
`dev_setup.sh.test` 11/11 (manifest/pnpm/incomplete → reinstall, visible
failure not recorded), `quality_changed.test` 6/6 (exact order, owner once,
`verify deps: false` in the copy, backend-only needs no install),
`pre-commit.test` 2/2, `worktree_setup.sh.test`, `nix_shell_hook.sh.test`,
`profile-commit-hook.test`, `run_all_script_tests.sh` 21/21. Real outside-Nix
disposable worktrees: slice-5 direct command → no ERR/WARN and
installation state/links unchanged (old script reproduced both the warning
and the silent relink); one hook run per class, warning-free: frontend 7.58,
backend 1.62, cli 1.23, mcp-server 1.18, test-fixtures 1.32, root 1.35,
openapi 1.44, mixed 7.96 s (single uncontrolled runs, not acceptance data);
negatives block (`noDebugger`, `spotlessJavaCheck`, `ERR_PNPM_OUTDATED_LOCKFILE`
with lockfile unchanged); source/index/lockfile hashes preserved. Elapsed
~12 min implementation + ~3 min refactor: hard-limit overrun, recorded; the
slice converged with complete proof, so it was not refined. Remaining gaps: a
deleted link inside a project's `node_modules` is not detected; an in-place
tool upgrade at the same path outside Nix is not detected; the root
`postinstall: syncpack fix` can still run when a stale install is repaired
during the hook (pre-existing).

### 7. Prove staged frontend checks decide the result
Type: Behavior
Status: planned
Proof: real installed-hook sequence in the driver's disposable fixture with
valid → newly staged lint/type error → repair (slice 2 negative examples 1–2),
opposed staged/unstaged versions and invalid untracked/unstaged source
(example 3), and a removed/renamed imported frontend file and a changed
configuration/dependency input; verify source/index preservation. Run the full
affected hook/dispatcher boundary suites.

Behavior: the frontend index changes between commits → the existing complete
staged copy is checked by Biome and the real compiler → the current staged
version decides success/failure. Add a test only where an existing boundary
suite lacks the observation.

Reassessment (after slice 6): one warning-free run per class now averages
~2.96 s (frontend 7.58, mixed 7.96, others 1.2–1.6 s), well below the 5 s
target, so reusable compiler state is not added: it would introduce stale-view
and partial-refresh risks the target does not need. Slice 8 measures the full
corpus; if its mean misses the target, return to this plan with that evidence
before adding compiler state. The correctness promises this slice owned stay.
Sizing: ~5 minutes; a proof loop over the existing path.

### 8. Demonstrate fast, clean ordinary commits across the fixed mix
Type: Behavior
Status: planned
Proof: `bash scripts/profiling/profile-commit-hook.sh acceptance <local-evidence-directory>`
on the same 40 input cases/environment as baseline, all warning-free and correct,
overall mean strictly below five seconds; separate documentation-only
no-component, one-per-class inside-Nix and one-per-class named fresh-cache
diagnostics. Retain failures/slow samples and every timing.

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

Planning observations above narrow the candidates. No acceptance mean or
historical warning remedy has been established.

- Driver (slice 1): the fixture is a disposable linked worktree detached at
  the execution checkout's **committed HEAD**, so hook/runner changes must be
  committed before the driver measures them. It invokes
  `git -c core.hooksPath=<fixture>/scripts/git-hooks hook run pre-commit`
  (Git's dispatch of the fixture's tracked hook; the shared installed hook is
  never replaced). Hook stdout arrives in `hook.stderr`. The `warnings`
  column is a plain `warn|ERR_PNPM` line count: read flagged logs (Nix's
  dirty-tree warning is expected in the baseline). Fresh-cache resets only
  `CHECK_CACHE_PATHS` (`dist backend/build backend/.gradle`); slice 7 adds its
  compiler-state location. Java/Redocly/vue-tsc validity of corpus edits is
  unproven with real tools until slice 2; an unhealthy case is fixed in the
  corpus before the baseline is recorded (slice 2: all healthy). A full run
  takes ~7 min (57 hook runs). The `warnings` column scans hook output only,
  not the driver's own `nix-entry.log` for inside-Nix runs.
- Simplest-alternative assessment (slice 2): inside-Nix class costs average
  ~3.2 s across the eight classes, so removing environment-entry cost alone
  leaves ~3.2 s plus whatever the reused entry costs (planning probe: 1.82 s
  second reuse). That is near the 5 s line, with frontend/mixed (~8–9 s inside
  Nix) dominating. Reassess after slices 4 and 6 whether slice 7's compiler
  state is still needed before building it.

## Execution

- Mode: Story Branch; checkout
  `/Users/terryyin/git/doughnut/.worktrees/commit-with-trustworthy-checks-averaging-under-f`,
  branch `claude/commit-with-trustworthy-checks-averaging-under-f`, remote
  `origin`, target `main`; agent Mana-chan; publisher
  `dashboard-territory.local-doughnut`.
- Claim published on `origin/main` at `018dd21603f2b2014d6c292268dd7ffcc9dfb33a`
  (starting revision `075510bd7347b2cde5dd45467c28409e0070d936`).
