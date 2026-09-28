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

<a id="story-4"></a>

### Stop the SUT start timeout test depending on runner speed

**Identity:** SEED-039#story-4
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/008-sut-start-timeout-race/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"9f1b2dee4f36def21e3556f62349bac4e3e6931aa30ed1c6a26728b8bf56d4e7","plan":"6da9c47f929ff6ee76763b6643661384a1594a41f943f55c20a824d6c8f33d9b"}}
```

**Goal**

Contributors get CI failures only for real defects, never because a runner
was slow. The isolated SUT start tests stop racing a wall-clock deadline
against the stand-in's startup, and the suite gets smaller rather than more
elaborate.

- **Evidence:** in `scripts/sut-isolated-start-release.test.mjs`, "isolated
  start timeout stops the owned process tree and releases the claim" gives the
  stand-in a 1.5s start timeout and must see its owned pids recorded, and
  still alive, before that deadline fires. Its sibling with a 100ms timeout
  failed that race in CI run [36373090056](https://github.com/nerds-odd-e/doughnut/actions/runs/36373090056)
  (fixed by `32c68269cd`). A scan of every failed CI run since 2026-09-08 found
  no failure of the 1.5s test (four observed passes, 1.56–1.65s), so this is a
  cheap removal of a known race pattern, not a response to observed flakiness.
- **Why this shape:** a start deadline and a cancellation end the same way.
  `waitForSutHealthy` returns `{ ok: false, exitCode: 1 }` for both, and
  `runSutStart` then runs the one shared `lifetime.shutdown()`. The
  deadline-specific behavior is already proven deterministically with a mock
  child in `sut-start-health-wait.test.mjs` ("returns ok=false when timeout
  expires") and `sut-start.test.mjs`. The isolated timeout test therefore adds
  only its teardown observations, which do not depend on what ended the start.
- **Effort hypothesis:** S (under 30 minutes), high confidence.
- **Depends on:** nothing.
- **Safe stopping point:** the change is complete on its own.
- **Plan:** [008-sut-start-timeout-race](../slice-plans/008-sut-start-timeout-race/PLAN.md)

**Scope**

- Required: the isolated timeout test is removed. Its teardown observations
  that no other isolated start test makes move into "isolated start
  cancellation stops the owned process tree", which already cancels only after
  the owned tree is recorded: a foreign process and a foreign TCP listener
  survive, the owner lock and control endpoint are removed, the live-owner
  check fails, and the SUT healthcheck reports not ok afterwards.
- Required: no isolated start test sets a start deadline that can fire before
  the stand-in records its pids. The remaining tests still expect the stand-in
  to start within the shared pid wait, which is ordinary fixture startup.
- Excluded: raising the timeout (keeps the race and slows the lint job), and
  any test-only clock or deadline hook in product code (adds complexity for an
  unobserved failure). Product code under `scripts/sut-*.mjs` is unchanged.
- Deferred: auditing other script tests for timing races; that belongs to the
  test-optimization story (story 2).

**Key examples**

- A contributor pushes while the lint job's runner is slow to start Node
  processes → the isolated start tests pass, because none starts a deadline
  racing the stand-in's startup.
- The cancellation test starts the stand-in, waits for its owned pids, cancels
  → the start returns 1, the owned leader and grandchild are gone, the foreign
  process and listener still run, the owner lock and endpoint are removed, and
  the healthcheck is not ok.
- A regression makes an expired start deadline skip teardown → the
  deadline-path unit tests still show a timeout returns exit code 1, and the
  one shared shutdown after any non-zero exit code is proven by the
  cancellation test.

## Ordering and Scope Reduction

Story 4 is independent of the others. Follow story 2, then story 3. Test optimization removes shared cost before shard
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
