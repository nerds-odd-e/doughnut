# Bound CI system package installation

**Identity:** SEED-129#bound-ci-system-package-installation
**Source:** [selected story](../../seeds/SEED-129-bounded-ci-package-setup.md#bound-ci-system-package-installation)
**Source review:** [source-review.md](source-review.md)

## Outcome and boundaries

Developers see a failed, diagnosable setup within 600 seconds plus at most 10
seconds of termination time when a system-package mirror cannot supply its
packages. Healthy, slow, and transiently interrupted acquisitions within that
budget still reach the existing downstream checks. Apply one policy to the
canvas and MySQL system-library phases through their existing composite actions.

The canvas deadline covers update and install together. The MySQL deadline
covers both compatibility `.deb` downloads and the system-library installation
together. Each download permits at most two retries within its phase deadline;
do not restart a whole installation to retry acquisition. Preserve package lists,
MySQL archive/version setup, non-Linux Node behavior, and downstream checks.
The story's exclusions apply; do not run release publication to prove setup.

## Architecture and reuse

PFE: `.github/setup_nodejs_with_cache/action.yml` already owns Linux canvas
dependencies; `.github/mysql_binaries/action.yml` owns MySQL's compatibility
libraries. Preserve those action interfaces and place their common bounded
package policy in `scripts/ci-system-packages.sh`, called with `canvas` or
`mysql`. Package-purpose data belongs with that entry point; MySQL's symlink,
archive extraction, version checks and `MYSQL_BASEDIR` remain with its action.
No existing system-package deadline owner was found in `.github/`, `scripts/`,
or the Cloud VM setup. The deployment timeout and SUT lifetime owners govern
different operations; changing or importing them would couple unrelated work.

Use the runner's GNU timeout around the privileged package phase, including a
10-second forced-termination allowance. Use noninteractive sudo and a strict
Bash child; the caller must retain a live parent to report timeout or command
failure with phase/budget and original stderr/stdout. Use APT's acquisition
retry cap and HTTP/HTTPS timeout options, and strict update failure handling
(`APT::Update::Error-Mode=any`) so stale indexes cannot turn a mirror failure
into apparent success. Bound the compatibility curl downloads inside the same
deadline. Use native download retries, not another job-level retry loop.

Relevant Accepted decisions: [0006, Failure handling](../../../docs/adrs/0006-failure-handling-accepted.md#usage)
supports enriching the failure while retaining its cause; [0007, Environments
and isolation](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
keeps probe mutations on disposable CI resources. The ADR index and records
agree; no relevant conflict or supersession was found. Existing North Star
topics do not govern this local CI policy; no new direction is needed.
Expected edited action files have 45 and 22 lines; the script test runner has
60 lines. No oversized existing file needs reorganization for this approach.

Tool references, checked 2026-11-13: [APT acquisition retries](https://manpages.ubuntu.com/manpages/noble/man5/apt.conf.5.html#the_acquire_group),
[strict APT update failures](https://manpages.ubuntu.com/manpages/stonking/man8/apt-get.8.html),
[GNU timeout](https://www.gnu.org/s/coreutils/manual/html_node/timeout-invocation.html),
and [curl transfer/retry limits](https://curl.se/docs/manpage.html).
Documentation guides the probe; it does not establish runner behavior.

## Decisive premises and observations

Preparation checkout: `/Users/terryyin/git/doughnut/.worktrees/bound-ci-system-package-installation`,
revision `1541679fa263047b95759fcab62e495d8f4e1a41`, 2026-11-13.

| Premise / consuming operation | Observation, result and limits | Owner |
| --- | --- | --- |
| An actual system-package outage prevents tests and needs a shorter bound | [DD-485](../../../DonutRetrospectiveFindings.md#dd-485) retains the 2026-11-11 SEED-124 execution at `e9f109c3c25b0355a897161f8a8180fdfb22a1a5`: eleven failed jobs per initial run, about 3,046 seconds in apt-get, mirror resolution failure, no tests. `git diff 4059ac2353 HEAD -- .github/setup_nodejs_with_cache/action.yml .github/mysql_binaries/action.yml` returned no differences for the retained action boundaries. This is retained reproduction evidence, not an observation of the repair. | Slices 2–3 |
| A 600-second package budget leaves useful successful-install headroom | The literal API query below read the actual successful attempt. The slowest named E2E Node setup ran 17:00:21Z–17:08:13Z on 2026-11-11 (472 seconds including Node/pnpm/dependencies); lint including both actions ran 16:59:21Z–17:05:44Z (383 seconds). These aggregate times support the choice, not a guarantee for future cold runners. | Slice 1 validates current runner |
| The proof can run against the consuming Linux profile | `CURSOR_DEV=true nix develop -c gh variable get RUNNER --repo nerds-odd-e/doughnut` returned `ubuntu-latest`. The attempt query above successfully retrieved terminal job/step outcomes through the same API route used for observation. This proves the read route only; a new workflow run and Linux package mutation remain unobserved. | Slice 1 early probe |
| APT options, root process termination, actual package installation, and job skipping work together | The local tool query below found Node 26.13.0, Bash 5.3.9 and GNU timeout 9.11, but no Docker daemon. Darwin is not a proxy for privileged Linux process cleanup or APT. Their consuming operation is deliberately unobserved until the disposable, state-changing CI probe below. | Slice 1 early probe; stop dependents if false |
| Local shell fixture execution is usable | `CURSOR_DEV=true nix develop -c bash scripts/test/check-mig-rollout.sh.test` passed its actual external-command fixture and failure propagation. This establishes the shell-test route, not proof of package policy. `test-entry-rules.mjs --project <preparation checkout> --source scripts/test/ci-system-packages.sh.test` resolved MakeMe=false and pageObjects=false. | Slices 2–3 add discriminating proof |

Literal successful-attempt and local tool queries (read-only):

```sh
CURSOR_DEV=true nix develop -c gh api repos/nerds-odd-e/doughnut/actions/runs/43053073660/attempts/3/jobs --paginate --jq '.jobs[] | {name, conclusion, runner_name, steps: [.steps[] | select(.name | test("canvas|MySQL|Cypress|dependencies|Lint"; "i")) | {name,conclusion,started_at,completed_at}]}'
CURSOR_DEV=true nix develop -c bash -c 'node --version; bash --version | head -1; timeout --version | head -1; docker info --format "{{.OSType}} {{.Architecture}}"'
```

No package, APT source, application service, or shared checkout was mutated for
these observations. Before slice 1 launches, inspect the actual runner's image,
tools, CPU/memory and competing workload; use one serial disposable workload.
Local Mac observations establish no remote headroom. Retain runner image,
commands, elapsed times, diagnostics and terminal receipts with execution proof.

## Outside-in proof ownership

| Promise / source example | Owning slice | Observable proof |
| --- | --- | --- |
| An unreachable canvas mirror fails setup within budget with source diagnostic; tests skip (example 1) | 2 | Execute the real canvas action's package entry on Linux against an isolated `.invalid` mirror with empty package indexes. Read nonzero package phase, phase/budget plus original DNS diagnostic, elapsed time ≤610 seconds and the enclosing job's skipped test step. |
| A stalled acquisition ends with its outstanding privileged work within the deadline (example 2) | 1 establishes mechanism; 2 maintains it; 3 covers MySQL | Run the real timeout with a short test budget against a hanging external acquisition, including a child ignoring TERM. Observe terminal nonzero status and the owned child's exit by the kill allowance. Use the unmodified 600/10 defaults in at least one CI action-boundary check. |
| Required canvas packages install and the job reaches checks, including slow success and one transient recovery (examples 3–4) | 2 | Shell boundary regression with delayed success and fail-once external downloader; Linux healthy action plus successful frontend/other tests, E2E, lint and packaging CI outcomes. The delay is below the deadline; tests exercise the real deadline, not a fake timer. |
| Both MySQL compatibility downloads and APT installation share one bound, diagnosis and recovery policy (examples 1–4) | 3 | Exercise failure in a compatibility download and an APT acquisition, then a healthy combined phase. Observe whole-phase timing, retry exhaustion or recovery, required packages, and unchanged `mysqld --version`, `mysql --version`, `MYSQL_BASEDIR` and lint/generated-types continuation. |
| Every caller inherits the policy and preserves its continuation | 2–3 | Trace action use through `lint_environment` → `lint_and_generated_types`, frontend shards, Other Unit Tests, E2E shards, `package_application_artifacts`, and `cli-release.yml`. CI proves normal push callers. For CLI release, exercise only setup/build on a disposable branch or fixture; retain the tag trigger and upload boundary without executing upload. |

## Ordered slices

### 1. Validate bounded package failure on the actual Linux runner
Type: Behavior
Status: done
Proof: accepted — probe run `43410926122` on SHA `510f94debc` (Ubuntu 24.04.5 /
apt 2.8.3 / timeout 9.4): bad-mirror fails in 3s with DNS diagnostic + skipped
downstream; stalled `status=137` at 25s; transient `attempt_count=2`; healthy
elapsed 11s ≪ 600s. Main `ci.yml` exact success on same SHA (`43410926092`).
Receipts: [slice-1-probe-evidence.md](slice-1-probe-evidence.md). Approach usable.

Behavior: given an execution-owned branch on the configured Linux runner, run
isolated bad-mirror, stalled-transfer, transient-recovery and healthy-package
cases with the proposed 600/10-second policy and two acquisition retries →
observe reliable failure/continuation and decide whether the approach is usable.
Use a temporary branch-scoped `.github/workflows/ci-system-packages-probe.yml`;
record the resolved runner image/APT version and literal commands. Fail the
probe job on the bad mirror, then read its skipped next step from the Jobs API;
use separate jobs for success cases. Keep fresh APT sources/indexes and any
package install confined to the disposable runner or container. Do not simulate
mirror failure by replacing apt-get: real APT must consume the invalid source.

This is the permitted early probe because a state-changing Linux CI run is
required and no local Linux engine is available. On unsupported options,
incomplete cleanup, unavailable credentials/workflow, or healthy setup outside
the budget, stop dependent slices and replan against that evidence. A budget
change must remain aligned with the seed. No human checkpoint is assumed for
ordinary authorized CI branch publication. Target ~5 minutes of authoring and
analysis; the single real package/run wait is an external-wait exception.

### 2. Give canvas package setup one bounded failure policy
Type: Behavior
Status: done
Proof: focused shell ok (deadline + bypass, slow success, transient recovery);
action wires `scripts/ci-system-packages.sh canvas`. Push CI exact success on
SHA `2b7d4ebd` (run `43414560990`) for caller inheritance.

Behavior: a Linux Node/Cypress caller requests its existing canvas packages →
update and install share one 600-second budget with bounded acquisition retries
→ healthy setup reaches dependencies/checks, while failed setup emits the phase,
budget and original cause and stops the job before tests.

Add the canvas entry and common policy in `scripts/ci-system-packages.sh`, and
call it from the existing Linux-only step. Keep Node, cache, pnpm and non-Linux
behavior with their current owners. Add shell fixtures under
`scripts/test/ci-system-packages.sh.test`, following the script/Bach guidance
while actually executing the production Bash boundary with real timeout and
only external apt/curl/sudo stand-ins where needed. Include deadline exhaustion,
slow success and recovery after one transient acquisition failure. Check that
the regression fails with the guard bypassed; restore it before accepting proof.

Target 5–8 minutes including focused tests and cleanup. Scrutiny: one new
policy/entry, one caller and one proof loop form a cohesive outcome; no separate
structure slice is needed. CI observation is the external-wait exception.
MySQL keeps its existing behavior until slice 3.

### 3. Give MySQL system libraries the same bounded setup outcome
Type: Behavior
Status: done
Proof: shell fixtures ok; mysql action wired; probe deleted (sweep clean under
`scripts/`/`.github/`). Push CI exact success SHA `281dd138` (run `43418396664`).

Behavior: the MySQL setup action requests its compatibility libraries → the
two `.deb` downloads and APT install consume one shared phase deadline/retry
policy → healthy setup reaches existing MySQL version checks and lint; a mirror
or download failure produces the diagnosis and fails the job within the bound.

Extend the same package entry with MySQL's package-purpose data, using the
validated native curl/apt acquisition options. Leave archive download/extraction,
symlink, versions and environment export in their existing action. Extend the
same boundary proof for compatibility-download failure and for APT failure;
prove healthy combined execution and retry recovery. Reuse already accepted
common deadline/cleanup proof where unchanged. Transfer sufficient Linux
regressions into the discovered shell suite, then delete the temporary probe
workflow and anything used only by it. Record that deletion's one-time reading
separately; retain the surviving policy proof, without an absence assertion.

Target 5–8 minutes plus external CI wait. Two download commands and the existing
install share the already validated owner, keeping the cumulative design to one
policy. The ordinary test command discovers the added `.test`; no new test
shard or CI proof part is needed.

## Verification and delivery

Execution follows AGENTS.md's required gates: Jidoka, a fresh independent
post-change-refactor agent, API generation only when applicable, coordinator
`./scripts/run.sh pnpm format:changed` once, plan evidence, agent commit with
the check-only lint hook, push and asynchronous CI repair. Use Nix for repo
commands. No backend/frontend implementation or API regeneration is expected.

At each boundary, update the consumer trace with the changed package entry and
both action paths, then dispose every listed caller by run, sufficient retained
proof, or an effect-based exclusion. Run focused shell proof; before final
acceptance run `CURSOR_DEV=true nix develop -c bash scripts/test/run_all_script_tests.sh`
because it is the affected script suite required by the script skill. Hosted
CI owns its existing broad checks after publication; it is not a requirement
to run backend/frontend/E2E suites locally for this infrastructure-only change.
Do not report a green shell fixture as actual Linux APT or job-skipping proof.

## Plan review

Slice-plan refinement was not needed: the early infrastructure probe owns the
unavailable observation, followed by two bounded caller outcomes using one
common policy. No remaining slice-specific concern was identified beyond the
explicit probe, whose negative result stops dependent execution. No disputed
constraint or pre-Take human decision remains. The focused 5–8 minute estimates
include edits/tests/cleanup; the >5 minute cases were scrutinized above. At
10 minutes stop and decompose unless the named external-wait exception applies.

## Story obligations

### X1. Open Dough CI observation unchanged
Reported: slice 1 — "source review exclusion"
Story clause: "this story does not change Open Dough's CI observation"
Disposition: excluded "this story does not change Open Dough's CI observation"
Commitment: X1
### X2. Does not select new mirrors
Reported: slice 1 — "source review exclusion"
Story clause: "select new mirrors"
Disposition: excluded "select new mirrors"
Commitment: X2
### X3. No cross-job retry coordination
Reported: slice 1 — "source review exclusion"
Story clause: "add cross-job retry coordination"
Disposition: excluded "add cross-job retry coordination"
Commitment: X3
### X4. Node/pnpm/Cypress policy unchanged
Reported: slice 1 — "source review exclusion"
Story clause: "change Node/pnpm/Cypress"
Disposition: excluded "change Node/pnpm/Cypress"
Commitment: X4
### X5. MySQL archive policy unchanged beyond system libraries
Reported: slice 1 — "source review exclusion"
Story clause: "MySQL archive"
Disposition: excluded "MySQL archive"
Commitment: X5
### X6. Docker setup policy unchanged
Reported: slice 1 — "source review exclusion"
Story clause: "Docker"
Disposition: excluded "Docker"
Commitment: X6
### X7. Java setup policy unchanged
Reported: slice 1 — "source review exclusion"
Story clause: "Java"
Disposition: excluded "Java"
Commitment: X7
### X8. Cloud VM setup policy unchanged
Reported: slice 1 — "source review exclusion"
Story clause: "Cloud VM setup policies"
Disposition: excluded "Cloud VM setup policies"
Commitment: X8
### X9. Release publication not required to prove setup
Reported: slice 1 — "source review exclusion"
Story clause: "Existing downstream release work is preserved; proving setup does not require publishing a release."
Disposition: excluded "Existing downstream release work is preserved; proving setup does not require publishing a release."
Commitment: X9

## Execution complete

Product advice: No backlog change from this review. Treat Donut's DD-485 system-package wait as addressed by this story's shared bounded policy; wrap-up may close the Taken SEED-129 entry and assimilate that Donut finding disposition. Leave the shared Open Dough CI-observation facet of DD-485 alone (X1). No follow-up story or correction plan. Process review deferred into wrap-up (intentCoverage).
