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

The combined pnpm/Cypress cache was 3,597 MB. In the five CLI shards its
restoration took 56–106 seconds, followed by only 6–12 seconds of dependency
installation. In run 21345 the critical note-topology shard spent 128 seconds
restoring it. The CLI-containing E2E invocation consistently took 4m35s–4m52s;
in the latest run the other invocations took 2m24s–2m39s. Invocation duration
includes application/browser startup and orchestration, not only test bodies.
These observations motivate experiments; they are not promised savings or a
substitute for fresh, comparable baselines.

## Alternatives and Decision

The owner selected three ordered outcomes: reduce dependency-cache overhead,
optimize related tests globally, then redistribute E2E shards using the resulting
measurements. Immediate shard rebalancing is the strongest smaller alternative,
but would redistribute current waste and rely on timings that optimization
will change. It is explicitly deferred to story 3. Doing nothing retains the
observed delay; optimizing packaging or unit tests alone would not have shortened
completion in this sample. Cache layout and test implementation remain choices
to establish through measurement, not designs selected by this decomposition.

## Story Decomposition

S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours, including delivery. These are
rough hypotheses; measurement may require refinement or resplitting. This seed
authorizes no implementation, profiling run, or executable slice plan.

<a id="story-1"></a>

### Prepare CI dependencies faster with a smaller effective cache

**Identity:** SEED-039#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../quick/015-smaller-ci-dependency-caches/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"9f951423543b3055c657451b9a071484771cdf4e8c0afc61479078777248ff93","plan":"668551d1f2952e2c4147ab1aca441728e5b56e7583fd8a39c2e06b8e6d6e88ff"}}
```

- **Goal:** Contributors receive trustworthy CI results sooner through materially
  faster dependency preparation, measured across restore, install, and cache-save
  costs. The improvement must survive normal dependency updates, rather than
  depending on a one-off deletion of the current archive.
- **Scope:** Improve the cache lifecycle owned by the existing shared Node/pnpm
  setup action and preserve its current consumers: lint/type generation, frontend
  and other unit tests, deployment packaging, six E2E shards, and CLI release
  builds. Keep the locked workspace installation and required native/postinstall
  behavior. Compare cache contents and total setup costs before choosing the
  smallest effective change. Cold caches remain a supported normal condition;
  their initial population cost is measured separately from repeat cache hits.
- **Key examples:**
  - Given an unchanged lockfile and a populated cache, a fresh runner completes
    correct dependency setup measurably faster and the same checks still pass.
  - Given an empty or evicted cache, setup installs the committed dependencies
    and required Cypress binary, and the subsequent warm run reuses the result.
  - Given a dependency or Cypress version update, setup installs the newly
    requested versions without repeatedly carrying historical browser versions
    or an ever-growing inherited dependency archive into future runs.
  - Given the same action in a build-only consumer, dependency preparation still
    supports bundling; no release tag or live deployment is needed to prove it.
- **Evaluation:** Compare successful Linux CI runs with matching dependency graph,
  Node/pnpm/Cypress versions, runner class, and job selection. Report cache bytes,
  restore/install/save times, cold versus warm conditions, total job/workflow
  elapsed time, and queue delays separately. Require repeatable setup improvement
  beyond observed run-to-run variation; smaller bytes or a single fast run alone
  are insufficient. Assess downstream CI benefit without attributing unrelated
  test or scheduling variation to this change. Do not invent a numeric saving
  before the representative experiment.
- **Preserved constraints:** Keep coverage, dependency versions, shard assignments,
  test parallelism, release behavior, existing force-install compatibility, and
  persistent developer caches intact. Use isolated runner stores for experiments.
  Missing cache data is recoverable by normal installation; installation or
  verification failures remain visible.
- **Deferred promises:** Test optimization, E2E rebalancing, removing repeated
  installs from package scripts, selective workspace installs, skipping Cypress
  in particular jobs, new runner infrastructure, and global cache deletion.
  These are not prerequisites for this story's bounded cache improvement.
- **Value / learning:** Establish which cached payload causes the observed cost
  and prove that a bounded cache lifecycle lowers total preparation time rather
  than shifting the cost elsewhere.
- **Effort hypothesis:** M, medium confidence; assumes the existing setup action
  can be improved without introducing a new runner platform.
- **Depends on:** None.
- **Safe stopping point:** Measurably faster, correct dependency preparation
  remains valuable even if no tests or shards subsequently change.
- **Plan:** [Smaller CI dependency caches](../quick/015-smaller-ci-dependency-caches/PLAN.md).
- **Open questions:** None about the outcome or scope. Cache partition and
  performance remain implementation hypotheses with a measured decision gate.

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
- **Depends on:** Follow story 1 in the owner's sequence and establish a fresh
  baseline after its changes; no particular cache design is a technical prerequisite.
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
  the retained implementation, including the effects of story 1.
- **Safe stopping point:** A verified redistribution is independently usable.
  If the resulting timings no longer justify rebalancing, bring that evidence
  back for an owner decision rather than inventing work or silently cancelling it.

## Ordering and Scope Reduction

Follow the owner's sequence: story 1, then story 2, then story 3. Story 1 tests
the clearest infrastructure-overhead hypothesis. Story 2 removes shared test
cost before story 3 redistributes the residual workload. No rebalancing belongs
in either earlier story. If scope must shrink, defer story 3 first; stories 1
and 2 retain their own measurable value. Global backlog priority has not been
selected, so these remain ordered candidates in this seed.

## Open Decisions

No unresolved decision changes the requested three-story order. Story 1 is
refined with a comparative performance criterion and a measured implementation
gate. Stories 2 and 3 remain candidates; story 3's grouping must await story 2's
result.

## When to Surface

Select story 1 when taking up CI feedback acceleration. Continue to story 2
after measuring the cache change, then assess story 3 using the optimized suite.

## Breadcrumbs

- Owner request on 2026-09-23: reduce the oversized cache; invoke test
  optimization with global, simple, cohesive improvement as the goal; only then
  rebalance E2E shards based on the optimization result.
- [CLI shard cache and test evidence](https://github.com/nerds-odd-e/doughnut/actions/runs/35808762047/job/107015324770).
- [Critical note-topology cache evidence](https://github.com/nerds-odd-e/doughnut/actions/runs/35805143511/job/107004176637).
- SEED-038 was already allocated by unrelated work in the originating checkout;
  this decomposition uses the next seed identity.
