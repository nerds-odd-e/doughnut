# Source review — Bound CI system package installation

**Launch:** fresh general-purpose reviewer, no conversation inheritance; model inherit.
**Input:** selected story Goal, Scope, key examples and exclusions only (SEED-129#bound-ci-system-package-installation). No plan, product, or proof supplied.
**Instruction:** use only those inputs; read no other project files; change nothing; return numbered commitments with qualifications, examples, and exclusions.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| 1 | A Donut developer gets a timely, diagnosable CI setup failure when a system package mirror is unreachable. | slice 3 | observed: scripts/test/ci-system-packages-mysql.sh.test download/APT failure: phase=mysql + budget + retained cause; both actions wired |
| 2 | Jobs whose mirrors work install their required packages and continue to their existing checks. | slice 3 | observed: healthy mysql+canvas fixtures complete; mysql_binaries retains version checks and MYSQL_BASEDIR; push CI for callers |
| 3 | Each system-package phase in the existing Node/Cypress and MySQL binary setup actions is bounded to ten minutes from phase start. | slice 3 | observed: scripts/ci-system-packages.sh run_bounded_phase 600s wraps canvas and mysql purposes; both actions call the script |
| 4 | After the ten-minute phase deadline, at most ten additional seconds are allowed to terminate outstanding package work. | slice 3 | observed: KILL_AFTER_SECONDS=10 on run_bounded_phase; TERM-ignore cleanup proved slice 1/2 fixtures |
| 5 | Transient package downloads may be retried at most two times, within the shared phase deadline. | slice 3 | observed: mysql curl --retry ${ACQUIRE_RETRIES}=2 and APT Acquire::Retries=2; transient fixtures |
| 6 | Retry exhaustion or deadline expiry fails setup and fails that job. | slice 3 | observed: failure fixtures return nonzero from phase (composite step fails) |
| 7 | On that failure, the job preserves the underlying package-manager/download output. | slice 3 | observed: scripts/test/ci-system-packages-mysql.sh.test retains original curl/apt stderr on failure |
| 8 | On that failure, the job identifies the failing phase and its time budget. | slice 2 | observed: scripts/test/ci-system-packages.sh.test deadline exhaustion: stderr phase=canvas budget=…s |
| 9 | For a job that failed setup this way, tests for that job do not start. | slice 2 | observed: .github/setup_nodejs_with_cache/action.yml: canvas step precedes Install dependencies; nonzero canvas exit fails the composite step so later job steps do not start (GHA default); slice-1 probe Jobs API showed Downstream skipped after package failure |
| 10a | Bounded package work includes APT metadata refresh for the canvas build packages. | slice 2 | observed: scripts/test/ci-system-packages.sh.test slow-success stand-in log: apt-get update with APT::Update::Error-Mode=any |
| 10b | Bounded package work includes APT installation of the canvas build packages. | slice 2 | observed: scripts/test/ci-system-packages.sh.test slow-success stand-in log: apt-get install of five canvas packages |
| 11a | Bounded package work includes compatibility `.deb` downloads for MySQL's system libraries. | slice 3 | observed: purpose_mysql downloads libncurses5/libtinfo5; scripts/test/ci-system-packages-mysql.sh.test download-failure |
| 11b | Bounded package work includes APT installation of those MySQL system libraries. | slice 3 | observed: purpose_mysql APT install; scripts/test/ci-system-packages-mysql.sh.test healthy/APT-failure |
| 12.1 | Reuse the existing setup actions (with these bounds) for lint callers. | slice 3 | observed: lint_environment uses mysql_binaries + setup_nodejs; both call bounded script; CI after publish |
| 12.2 | Reuse the existing setup actions (with these bounds) for frontend callers. | slice 3 | observed: frontend jobs use setup_nodejs canvas policy; CI after publish |
| 12.3 | Reuse the existing setup actions (with these bounds) for other tests callers. | slice 3 | observed: Other Unit Tests use setup_nodejs canvas policy; CI after publish |
| 12.4 | Reuse the existing setup actions (with these bounds) for E2E callers. | slice 3 | observed: E2E uses setup_nodejs canvas policy; CI after publish |
| 12.5 | Reuse the existing setup actions (with these bounds) for application packaging callers. | slice 3 | observed: package_application_artifacts uses setup_nodejs; CI after publish |
| 12.6 | Reuse the existing setup actions (with these bounds) for CLI release setup callers. | slice 3 | observed: cli-release.yml uses setup_nodejs for setup; release upload excluded per X9 |
| 13a | Keep the required packages. | slice 3 | observed: CANVAS_PACKAGES and MySQL package/deb versions match prior action lists |
| 13b | Keep supported runner behavior. | slice 3 | observed: mysql_binaries keeps archive/symlink/version/MYSQL_BASEDIR; Node non-Linux unchanged |
| 13c | Keep downstream checks. | slice 3 | observed: downstream checks remain after package phase in actions; CI proves continuation |
| 14 | Slow successful acquisition within the deadline remains successful setup. | slice 2 | observed: scripts/test/ci-system-packages.sh.test delayed success under real timeout budget completes |
| 15 | Recovery from a transient download failure within the retry allowance and deadline remains successful setup. | slice 2 | observed: scripts/test/ci-system-packages.sh.test fail-once acquire then success under Acquire::Retries |
| X1 | Does not change Open Dough's CI observation. |  | X1 |
| X2 | Does not select new mirrors. |  | X2 |
| X3 | Does not add cross-job retry coordination. |  | X3 |
| X4 | Does not change Node / pnpm / Cypress setup policy. |  | X4 |
| X5 | Does not change MySQL archive setup policy (beyond the system-library package phase above). |  | X5 |
| X6 | Does not change Docker setup policy. |  | X6 |
| X7 | Does not change Java setup policy. |  | X7 |
| X8 | Does not change Cloud VM setup policy. |  | X8 |
| X9 | Existing downstream release work is preserved; proving setup does not require publishing a release. |  | X9 |
