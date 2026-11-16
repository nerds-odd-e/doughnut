# Source review

Launch/input: Fresh source-only agent `/root/source_review`; the full Goal, Scope, Key examples and Exclusions for `SEED-130#rebalance-ci-e2e-shards` were provided in the launch. No file reads. This retains the source-only review with slice allocation; it records no observations.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| C01 | Contributors receive complete eligible E2E feedback. | slice 3 | |
| C02 | Maintainers can repeat an evidence-based rebalance as the suite changes. | slice 3 | |
| C03 | Rebalancing aims to reduce the last shard’s elapsed time. | slice 3 | |
| C04 | Rebalancing must not silently lose coverage. | slice 3 | |
| C05 | Retain five explicit folder-pattern groups. | slice 1 | observed: scripts/ci/e2e-shards.test.mjs: actual CLI checkout/fixture partition, installed Cucumber tags and workflow-wrapper/proof agreement; validator and runner suites passed (slice 1 evidence) |
| C06 | Replace the sixth group with exactly the remaining discoverable feature files. | slice 1 | observed: scripts/ci/e2e-shards.test.mjs: actual CLI checkout/fixture partition, installed Cucumber tags and workflow-wrapper/proof agreement; validator and runner suites passed (slice 1 evidence) |
| C07 | Across the six selections, each eligible file belongs exactly once. | slice 1 | observed: scripts/ci/e2e-shards.test.mjs: actual CLI checkout/fixture partition, installed Cucumber tags and workflow-wrapper/proof agreement; validator and runner suites passed (slice 1 evidence) |
| C08 | Preserve existing runner file exclusions. | slice 1 | observed: scripts/ci/e2e-shards.test.mjs: actual CLI checkout/fixture partition, installed Cucumber tags and workflow-wrapper/proof agreement; validator and runner suites passed (slice 1 evidence) |
| C09 | Preserve existing CI scenario filters. | slice 1 | observed: scripts/ci/e2e-shards.test.mjs: actual CLI checkout/fixture partition, installed Cucumber tags and workflow-wrapper/proof agreement; validator and runner suites passed (slice 1 evidence) |
| C10 | Preserve existing CI tag filters. | slice 1 | observed: scripts/ci/e2e-shards.test.mjs: actual CLI checkout/fixture partition, installed Cucumber tags and workflow-wrapper/proof agreement; validator and runner suites passed (slice 1 evidence) |
| C11 | Retain six matrix jobs. | slice 1 | observed: scripts/ci/e2e-shards.test.mjs: actual CLI checkout/fixture partition, installed Cucumber tags and workflow-wrapper/proof agreement; validator and runner suites passed (slice 1 evidence) |
| C12 | Retain six expected case-proof parts. | slice 1 | observed: scripts/ci/e2e-shards.test.mjs: actual CLI checkout/fixture partition, installed Cucumber tags and workflow-wrapper/proof agreement; validator and runner suites passed (slice 1 evidence) |
| C13 | Add a private, project-local Donut skill for on-demand rebalancing. | slice 2 | |
| C14 | On every skill invocation, read recent comparable completed CI proof. | slice 2 | |
| C15 | Prefer the controlling dashboard’s Intent → CI status when known and sufficient. | slice 2 | |
| C16 | Use GitHub Actions artifacts as fallback or supplementary evidence. | slice 2 | |
| C17 | Use job timing as fallback or supplementary evidence. | slice 2 | |
| C18 | Reuse the existing proof format. | slice 2 | |
| C19 | Keep downloaded evidence in temporary storage outside the checkout. | slice 2 | |
| C20 | Use the skill for the initial balance. | slice 3 | |
| C21 | Name the repository represented by the evidence. | slice 2 | |
| C22 | Name the runs represented by the evidence. | slice 2 | |
| C23 | Name the attempts represented by the evidence. | slice 2 | |
| C24 | Name the revisions represented by the evidence. | slice 2 | |
| C25 | Identify those runs’ actual selections. | slice 2 | |
| C26 | Identify those runs’ runner conditions. | slice 2 | |
| C27 | Reconcile files against all six groups before proposing moves. | slice 2 | |
| C28 | Reconcile counts against all six groups before proposing moves. | slice 2 | |
| C29 | Reconcile durations against all six groups before proposing moves. | slice 2 | |
| C30 | Compare current and proposed per-group recorded-time estimates. | slice 3 | |
| C31 | Keep unknown costs visible. | slice 3 | |
| C32 | Check elapsed job times separately. | slice 3 | |
| C33 | Distinguish forecasts from measured results. | slice 3 | |
| C34 | Apply only timing moves supported by comparable evidence. | slice 3 | |
| C35 | Report an evidence gap when proof is unavailable, incomplete, incompatible, or leaves decisive timing costs unknown. | slice 3 | |
| C36 | Under those evidence-gap conditions, leave timing-based assignments unchanged. | slice 3 | |
| C37 | Deliver the coverage guarantee independently of whether an initial timing move is supported. | slice 1 | observed: scripts/ci/e2e-shards.test.mjs: actual CLI checkout/fixture partition, installed Cucumber tags and workflow-wrapper/proof agreement; validator and runner suites passed (slice 1 evidence) |
| C38 | Deliver a working skill independently of whether an initial timing move is supported. | slice 2 | |
| C39 | An initial invocation that cannot support a move reports that outcome. | slice 3 | |
| C40 | Use normal delivery CI for the post-change observation. | slice 3 | |
| Q01 | — C01, C07: Coverage concerns eligible files; existing runner exclusions remain operative. | slice 1 | observed: scripts/ci/e2e-shards.test.mjs: actual CLI checkout/fixture partition, installed Cucumber tags and workflow-wrapper/proof agreement; validator and runner suites passed (slice 1 evidence) |
| Q02 | — C06: The sixth selection is the discoverable remainder after the five explicit groups. | slice 1 | observed: scripts/ci/e2e-shards.test.mjs: actual CLI checkout/fixture partition, installed Cucumber tags and workflow-wrapper/proof agreement; validator and runner suites passed (slice 1 evidence) |
| Q03 | — C09–C10: A discovered file containing only excluded scenarios need not execute. | slice 1 | observed: scripts/ci/e2e-shards.test.mjs: actual CLI checkout/fixture partition, installed Cucumber tags and workflow-wrapper/proof agreement; validator and runner suites passed (slice 1 evidence) |
| Q04 | — C14: Evidence must be recent, comparable, and completed. | slice 2 | |
| Q05 | — C15: Dashboard preference applies when its controlling status is known and sufficient. | slice 2 | |
| Q06 | — C16–C17: GitHub Actions artifacts and job timing may serve as fallback **or** supplement. | slice 2 | |
| Q07 | — C27–C29: Reconciliation precedes proposing moves. | slice 2 | |
| Q08 | — C30: Compared loads are recorded-time estimates, not automatically measured elapsed-time outcomes. | slice 3 | |
| Q09 | — C34–C36: Timing changes depend on sufficient comparable evidence; the stated gap conditions constrain timing assignments. | slice 3 | |
| Q10 | — C03, C39: An invocation with no supported move does not establish faster CI. | slice 3 | |
| Q11 | — C33, C40: A later comparable CI result supports a measured elapsed-time claim. | slice 3 | |
| Q12 | The successful-attempt and failed-job-rerun examples illustrate evidence situations; they do not establish blanket acceptance or rejection rules for every such attempt. | slice 3 | |
| J01a | An eligible file already exists under `learning_session/`. | slice 1 | observed: scripts/ci/e2e-shards.test.mjs: actual CLI checkout/fixture partition, installed Cucumber tags and workflow-wrapper/proof agreement; validator and runner suites passed (slice 1 evidence) |
| J01b | Alternatively, a contributor adds an eligible file in a new directory. | slice 1 | observed: scripts/ci/e2e-shards.test.mjs: actual CLI checkout/fixture partition, installed Cucumber tags and workflow-wrapper/proof agreement; validator and runner suites passed (slice 1 evidence) |
| J01c | When CI resolves its six selections in either example, the file runs through the remainder once. |  | G1 |
| J01d | That coverage requires no folder-list edit. | slice 1 | observed: scripts/ci/e2e-shards.test.mjs: actual CLI checkout/fixture partition, installed Cucumber tags and workflow-wrapper/proof agreement; validator and runner suites passed (slice 1 evidence) |
| J02a | A file belongs to an explicit group. | slice 1 | observed: scripts/ci/e2e-shards.test.mjs: actual CLI checkout/fixture partition, installed Cucumber tags and workflow-wrapper/proof agreement; validator and runner suites passed (slice 1 evidence) |
| J02b | When CI resolves the remainder, that explicit group owns the file once. | slice 1 | observed: scripts/ci/e2e-shards.test.mjs: actual CLI checkout/fixture partition, installed Cucumber tags and workflow-wrapper/proof agreement; validator and runner suites passed (slice 1 evidence) |
| J02c | Snapshot files retain their existing treatment. | slice 1 | observed: scripts/ci/e2e-shards.test.mjs: actual CLI checkout/fixture partition, installed Cucumber tags and workflow-wrapper/proof agreement; validator and runner suites passed (slice 1 evidence) |
| J02d | Scenarios excluded by existing CI tags retain their existing treatment. | slice 1 | observed: scripts/ci/e2e-shards.test.mjs: actual CLI checkout/fixture partition, installed Cucumber tags and workflow-wrapper/proof agreement; validator and runner suites passed (slice 1 evidence) |
| J03a | Comparable successful attempts contain all six parts and usable file times. | slice 3 | |
| J03b | A maintainer invokes the skill and compares all six current and candidate loads. | slice 3 | |
| J03c | The invocation preserves the partition. | slice 3 | |
| J03d | The invocation applies a supported folder reassignment. | slice 3 | |
| J03e | A later comparable CI result determines the measured elapsed-time claim. | slice 3 | |
| J04a | An attempt re-ran only failed jobs. | slice 3 | |
| J04b | Alternatively, a revision changed selection. | slice 3 | |
| J04c | Alternatively, a newly covered file has no timing. | slice 3 | |
| J04d | In these illustrative situations, the invocation names missing or incompatible evidence. | slice 3 | |
| J04e | The invocation avoids treating unknown time as zero. | slice 3 | |
| J04f | It leaves timing assignments unchanged when the gap prevents a supported decision. | slice 3 | |
| X01 | No repository evidence cache. | slice 2 | |
| X02 | No raw-proof files in the repository. | slice 2 | |
| X03 | No seventh job. | slice 3 | |
| X04 | No automatic runtime scheduling. | slice 2 | |
| X05 | No test rewrites. | slice 3 | |
| X06 | No scenario rewrites. | slice 3 | |
| X07 | No new profiling run. | slice 3 | |
| X08 | No shared Open Dough distribution change. | slice 2 | |
| X09 | No dashboard feature. | slice 2 | |
| X10 | No new proof schema. | slice 2 | |
| X11 | No numerical speedup guarantee. | slice 3 | |
| X12 | An unsupported initial move outcome is not a claim of faster CI. | slice 3 | |
