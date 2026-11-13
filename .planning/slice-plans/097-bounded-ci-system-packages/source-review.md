# Source review — Bound CI system package installation

**Launch:** fresh general-purpose reviewer, no conversation inheritance; model inherit.
**Input:** selected story Goal, Scope, key examples and exclusions only (SEED-129#bound-ci-system-package-installation). No plan, product, or proof supplied.
**Instruction:** use only those inputs; read no other project files; change nothing; return numbered commitments with qualifications, examples, and exclusions.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| 1 | A Donut developer gets a timely, diagnosable CI setup failure when a system package mirror is unreachable. | slice 3 |  |
| 2 | Jobs whose mirrors work install their required packages and continue to their existing checks. | slice 3 |  |
| 3 | Each system-package phase in the existing Node/Cypress and MySQL binary setup actions is bounded to ten minutes from phase start. | slice 3 |  |
| 4 | After the ten-minute phase deadline, at most ten additional seconds are allowed to terminate outstanding package work. | slice 3 |  |
| 5 | Transient package downloads may be retried at most two times, within the shared phase deadline. | slice 3 |  |
| 6 | Retry exhaustion or deadline expiry fails setup and fails that job. | slice 3 |  |
| 7 | On that failure, the job preserves the underlying package-manager/download output. | slice 3 |  |
| 8 | On that failure, the job identifies the failing phase and its time budget. | slice 2 |  |
| 9 | For a job that failed setup this way, tests for that job do not start. | slice 2 |  |
| 10a | Bounded package work includes APT metadata refresh for the canvas build packages. | slice 2 |  |
| 10b | Bounded package work includes APT installation of the canvas build packages. | slice 2 |  |
| 11a | Bounded package work includes compatibility `.deb` downloads for MySQL's system libraries. | slice 3 |  |
| 11b | Bounded package work includes APT installation of those MySQL system libraries. | slice 3 |  |
| 12.1 | Reuse the existing setup actions (with these bounds) for lint callers. | slice 3 |  |
| 12.2 | Reuse the existing setup actions (with these bounds) for frontend callers. | slice 3 |  |
| 12.3 | Reuse the existing setup actions (with these bounds) for other tests callers. | slice 3 |  |
| 12.4 | Reuse the existing setup actions (with these bounds) for E2E callers. | slice 3 |  |
| 12.5 | Reuse the existing setup actions (with these bounds) for application packaging callers. | slice 3 |  |
| 12.6 | Reuse the existing setup actions (with these bounds) for CLI release setup callers. | slice 3 |  |
| 13a | Keep the required packages. | slice 3 |  |
| 13b | Keep supported runner behavior. | slice 3 |  |
| 13c | Keep downstream checks. | slice 3 |  |
| 14 | Slow successful acquisition within the deadline remains successful setup. | slice 2 |  |
| 15 | Recovery from a transient download failure within the retry allowance and deadline remains successful setup. | slice 2 |  |
| X1 | Does not change Open Dough's CI observation. |  | X1 |
| X2 | Does not select new mirrors. |  | X2 |
| X3 | Does not add cross-job retry coordination. |  | X3 |
| X4 | Does not change Node / pnpm / Cypress setup policy. |  | X4 |
| X5 | Does not change MySQL archive setup policy (beyond the system-library package phase above). |  | X5 |
| X6 | Does not change Docker setup policy. |  | X6 |
| X7 | Does not change Java setup policy. |  | X7 |
| X8 | Does not change Cloud VM setup policy. |  | X8 |
| X9 | Existing downstream release work is preserved; proving setup does not require publishing a release. |  | X9 |
