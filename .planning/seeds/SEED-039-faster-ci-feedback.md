---
id: SEED-039
status: dormant
planted: 2026-09-23
planted_during: owner-directed decomposition following analysis of five successful GitHub Actions CI runs
trigger_when: selecting work to shorten CI feedback after the September 23 timing analysis
scope: large
---

# SEED-039: Get trustworthy CI feedback sooner

## Why This Matters

For Donut contributors waiting for CI, successful checks should finish materially
sooner while preserving behavioral coverage, reliability, and reproducibility.
The five latest successful runs inspected on 2026-09-23 averaged 8m12s across
branches. E2E finished last in all five; other checks finished 2m09s–2m59s earlier.

| Run | Workflow elapsed | Last job |
| --- | --- | --- |
| [21346](https://github.com/nerds-odd-e/doughnut/actions/runs/35808762047) | 8m03s | CLI-containing E2E shard |
| [21345](https://github.com/nerds-odd-e/doughnut/actions/runs/35805143511) | 7m14s | Note-topology E2E shard |
| [21344](https://github.com/nerds-odd-e/doughnut/actions/runs/35802219595) | 8m16s | CLI-containing E2E shard |
| [21343](https://github.com/nerds-odd-e/doughnut/actions/runs/35800888132) | 9m52s | Note-topology E2E shard, started 2m47s late |
| [21342](https://github.com/nerds-odd-e/doughnut/actions/runs/35800881836) | 7m35s | CLI-containing E2E shard |

The CLI-containing E2E invocation consistently took 4m35s–4m52s;
in the latest run the other invocations took 2m24s–2m39s. Invocation duration
includes application/browser startup and orchestration, not only test bodies.
These observations motivate experiments; they are not promised savings or a
substitute for fresh, comparable baselines.

## Alternatives and Decision

The remaining sequence is to optimize related tests globally, then redistribute
E2E shards using the resulting measurements. Immediate shard rebalancing would
redistribute current waste and rely on timings that optimization will change.
It remains deferred to story 3. Optimizing packaging or unit tests alone would
not have shortened completion in the initial sample.

Dependency preparation follows the [maintained CI policy](../../docs/development-setup.md#ci-dependency-preparation).
Use fresh baselines for test optimization. [Run 35818732541](https://github.com/nerds-odd-e/doughnut/actions/runs/35818732541)
on revision `cd57b491fa57e768997b5fc1b2e7d5eaaf5ab939` passed all six E2E shards
in a 6m56s workflow; the CLI-containing shard finished last. This single run is
starting evidence, not a controlled test-optimization result.

## Story Decomposition

S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours, including delivery. These are
rough hypotheses; measurement may require refinement or resplitting. This seed
authorizes no implementation, profiling run, or executable slice plan.

<a id="candidate-resolution"></a>

### Shorten local test feedback by reducing repeated setup

**Identity:** SEED-039#candidate-resolution
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/001-cheaper-local-test-feedback/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"63cd74846696eb57494ea8f6ec703d5e100595e403c4c294d6cb0e29902e5012","plan":"1f5a54bdcef6802cb6c2e7e0f0ddb3491be170001269b7aab4602df6a2387dec"}}
```

**Goal:** Give contributors shorter trustworthy local test feedback by removing
three small, avoidable setup costs: waiting longer than needed to notice that
an owned E2E stack is ready, running Git LFS hooks on test-owned fixture
commits, and preparing the same mounted BookReadingPage twice in two
reading-panel spec files. Each cost is small; the story is worth it only for
the changes whose ordinary command measurably improves.

- **Scope:** The three transferred obligations in
  [the candidate record](../TEST-OPTIMIZATION-CANDIDATES.md):
  `e2e-batch-startup` (readiness polling cadence only),
  `cli-git-lfs-hook-installation` (test-owned fixture commits only), and
  `frontend-sibling-spec-setup` (the reading-panel visibility and target specs
  only). Required restrictions, each protecting existing proof: real health
  checks, deadline, cancellation and failure cleanup stay; the E2E runner keeps
  owning its fresh stack; real LFS filters, transfer, pointer history and
  hydration stay real; mounted-page behavior and per-file browser isolation stay;
  existing profile exclusions and owner-selected local HMR stay.
- **Scope limits:** No other startup cost (backend boot, Gradle check, Cypress
  launch, database provisioning), no other sibling-spec families, no shard or
  worker changes. Costs found there go back to the candidate record.
- **Key examples:**
  - A fresh owned stack becomes healthy between two polls → the runner notices
    within a shorter gap than today's 3s → Cypress starts only after a real
    healthy check; a stack that never gets healthy still times out with its
    log tail, and Ctrl-C still cleans up.
  - An E2E step commits a changed attachment in a CLI checkout → the commit
    still stores a real LFS pointer and the installed CLI still publishes and
    hydrates the exact bytes, but the commit no longer runs the LFS
    `post-commit`/`post-checkout` hooks.
  - The eight visibility cases and three target cases → run from one spec
    file with one setup → every case still observes its distinct visible
    behavior, and the file stays within the 250-line limit.
  - A change whose ordinary command shows no comparable improvement (including
    added proof cost) → is undone and recorded as no gain.
- **Architecture:** No responsibility moves between owners.
  [ADR 0007](../../docs/adrs/0007-environments-and-isolation-accepted.md): the
  E2E runner keeps owning its disposable stack.
  [ADR 0002](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md):
  fixtures keep standard LFS filters, transfer, and history. Readiness cadence
  is one shared default in `scripts/sut-start-health-wait.mjs`, also used by
  `dev-start.mjs`, `sut-owned-restart.mjs` and `sut-owned-lifetime.mjs`; change
  it there for every consumer, not as an E2E-only value. A hook bypass applies
  only to test-owned fixture commit commands (`-c core.hooksPath=…` on that
  command), never to a setting saved in a checkout the installed CLI later reads.
- **Execution boundary:** The
  [experiment plan](../slice-plans/001-cheaper-local-test-feedback/PLAN.md)
  belongs to this story. Its premises were re-observed on 2026-11-01 at
  `ee6ecf2549`; each slice still takes its own comparable before/after
  measurement as part of the experiment. Landing preparation starts no execution.

<a id="story-2"></a>

### Run related E2E tests systematically faster through cohesive test design

**Identity:** SEED-039#story-2
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** Contributors get substantially faster trustworthy local test
  feedback and shorter CI E2E execution by removing recurring test costs.
- **Scope:** Conduct a measured test-optimization round using the CLI-containing
  shard as the initial evidence, while considering related tests and shared
  responsibilities globally across files, shards, and test layers. Include fast
  siblings, fixtures, setup/teardown, synchronization, shared helpers, and affected
  consumers where they explain or share the cost. Global consideration does not
  authorize unrelated product work or an unbounded rewrite of every test.
- **Required skill handoff:** Invoke
  [dough-test-optimization](../../.agents/skills/dough-test-optimization/SKILL.md)
  in its normal measure-and-improve mode when this story is executed, with this
  instruction:

  > Use the CI findings as a starting hypothesis, not a file or shard boundary.
  > Consider improvements globally across related tests and their shared costs.
  > Seek a simple, cohesive, clean solution that makes the tests systematically
  > much faster. Challenge repeated setup, redundant coverage, and fragmented
  > test/support design together rather than accumulating isolated micro-fixes
  > or special cases for the slowest tests. Preserve behavioral coverage and
  > confidence. Establish comparable baselines, experiment, verify, re-measure,
  > and retain only demonstrated improvements. Keep E2E shard assignments and
  > parallelism unchanged during this story so redistribution cannot masquerade
  > as test optimization. Report remaining per-feature costs for story 3.

- **Evaluation:** The ordinary local command for the selected related scope
  and the unchanged CI E2E grouping demonstrate repeatable wall-time improvement
  under comparable conditions. Report before/after commands, revision, cache and
  runner conditions, executed cases, setup versus execution costs, support-code
  simplifications, and surviving proof for any removed or relocated test cases.
  Include replacement tests' cost. Faster isolated cases, fewer tests/lines,
  skips, weaker assertions, or retries do not establish success.
- **Value / learning:** Determine which common test responsibilities account
  for repeated cost and whether a cleaner design removes it systematically.
- **Effort hypothesis:** L, low confidence until profiling establishes the
  related families and removable cost. Refine or resplit if the investigation
  reveals multiple independently useful outcomes beyond one bounded round.
- **Depends on:** Establish a fresh baseline using the current dependency setup;
  no particular cache design is a technical prerequisite.
- **Safe stopping point:** Verified test improvements stand on their own with
  the existing shards. Preserve coverage and record inconclusive experiments
  or remaining candidates honestly rather than claiming an unmeasured gain.

<a id="story-3"></a>

### Finish E2E sooner with shards balanced against optimized test timings

**Identity:** SEED-039#story-3
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** Contributors wait less for an E2E straggler after test costs
  have been reduced.
- **Scope:** Use story 2's resulting per-feature and shared-startup measurements
  to redistribute E2E features across shards. Prefer a simple allocation using
  the existing six shards; reassess that assumption from evidence before any
  runner-capacity change. Distinguish execution imbalance from job-start delays.
  Do not prescribe a new grouping from the pre-optimization sample.
- **Evaluation:** Comparable complete CI runs show a shorter last-finishing
  E2E path and reduced execution imbalance after redistribution, accounting for
  startup/cache overhead and queue delays. Every required feature remains
  selected exactly once, with unchanged test behavior and coverage.
- **Value / learning:** Convert the optimized workload into shorter workflow
  elapsed time without adding test complexity or losing verification.
- **Effort hypothesis:** S, medium confidence once story 2's measurements exist;
  assumes feature reassignment among the existing shards is sufficient.
- **Depends on:** Story 2's completed optimization and fresh measurements on
  the retained implementation and current dependency setup.
- **Safe stopping point:** A verified redistribution is independently usable.
  If the resulting timings no longer justify rebalancing, bring that evidence
  back for an owner decision rather than inventing work or silently cancelling it.

## Ordering and Scope Reduction

Follow story 2, then story 3. Test optimization removes shared cost before shard
rebalancing redistributes the residual workload. If scope must shrink, defer
story 3 first. Global backlog priority has not been selected, so these remain
ordered candidates in this seed.

## Open Decisions

Stories 2 and 3 remain candidates; story 3's grouping must await story 2's result.

## When to Surface

Select story 2 for the next CI feedback improvement, then assess story 3 using
the optimized suite.

## Breadcrumbs

- Owner instruction: invoke test optimization with global, simple, cohesive
  improvement as the goal; only then rebalance E2E shards from its measurements.
- [CLI shard test evidence](https://github.com/nerds-odd-e/doughnut/actions/runs/35808762047/job/107015324770).
