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

<a id="specs-share-production-router"></a>

### Start every frontend spec's real router from one shared helper

**Identity:** SEED-039#specs-share-production-router
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/002-specs-share-production-router/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"ba78782eb99ddb8daed0401c1421f54ed7e35cf98d0ccf49d3e04873b430a6c6","plan":"2b2b448dc4193e631fd68be93eefa7fd9fbbccbe2c9bfcc8ee32cca7ce66d9a5"}}
```

- **For / why:** Contributors can trust and read a frontend spec's routing:
  every real router starts from a known page in one way. Today 11 test
  files build their own production router, and plain
  `RenderingHelper.withRouter()` starts wherever the previous test in the same
  file left the browser URL, so a test can depend on test order. Found by the
  retrospective of the story that added the shared `productionRouterAt` helper
  (`8cb73fa479:.planning/seeds/SEED-039-faster-ci-feedback.md#internal-mocks-to-real-modules`).
- **Effort hypothesis:** S to M, medium confidence: mostly mechanical edits to
  11 files, plus one shared reset.
- **Depends on:** None.
- **Safe stopping point:** any group of files moved onto the shared helper,
  with their specs passing.

**Goal:** Donut contributors can read and trust the routing in a frontend
spec: a router over the production routes is always started by the shared
helper, at a start the spec can see, and no test's starting route depends on
the test that ran before it. This serves the seed's aim of trustworthy
feedback. It does not promise shorter test time.

**Scope:**

Required: in `frontend/tests`, the files that build their own router over the
production `routes` get it from `productionRouterAt(location)`, or from plain
`withRouter()` when the start does not matter to the spec. Inventory on
2026-10-04 (26 files call `createRouter(`; 11 are in scope):

| Today | Files |
| --- | --- |
| Production routes, browser history | `components/form/quillEditorTestHarness.ts`, `components/recent/RecentlyRecalledNotes.spec.ts`, `notes/FolderSelector.spec.ts`, `notes/NoteMoreOptionsForm.spec.ts`, `notes/NoteUnresolvedWikiLinkModal.spec.ts`, `pages/NotebookCatalogReadBook.spec.ts`, `pages/folderPageTestSupport.ts`, `pages/noteShowPageTestSupport.ts` |
| Production routes, memory history | `pages/MessageCenterPage.spec.ts`, `pages/settings/RecentSettingsTab.spec.ts`, `toolbars/mainMenuTestSupport.ts` |

Required: a router from `withRouter()` without an argument starts at the root
location (`{ name: "root" }`) in every test, whatever ran before. Any router
with browser history that a test creates gets the same start, because the
reset lives in shared test support, in one place, like the popup stack reset.

Required: test names stay, and no assertion is weakened. A spec that relied on
a leftover URL states its start explicitly.

Required: the frontend testing skill
(`.agents/skills/frontend-testing/SKILL.md`) says that a production-routes
router in a spec comes from `productionRouterAt` or `withRouter()`.

Stay as they are (15 files), under the routing rule that skill already has:

- `tests/routes/routes.spec.ts` and `tests/routes/noteRouteFamily.spec.ts`:
  they test the route table itself.
- Routers over `dummyRouteRecordsFromMetadata` (9 files, counting
  `routes.spec.ts` once more): the skill allows them for resolving named
  locations without page imports.
- Routers with a single stand-in route (`commons/modalTestSupport.ts`,
  `commons/Popups/popButtonTestSupport.ts`, `notes/mcqsTestSupport.ts`,
  `pages/bookReadingPageTestSupport.ts`).

Not committed in this delivery:

- Moving the dummy-route and stand-in routers onto a shared helper, or
  removing them.
- Removing `createRouter(` from every file but `RenderingHelper.ts`.
- A lint rule on `createRouter(` in tests.
- Production code changes.

**Key examples:**

- Own router replaced. `FolderSelector.spec.ts` builds
  `createRouter({ history: createWebHistory(), routes })`. → It uses
  `productionRouterAt` with the location it needs. → Its tests pass with the
  same names and assertions.
- Memory history replaced. `MessageCenterPage.spec.ts` builds a production
  router on memory history and pushes a location. → It uses
  `productionRouterAt` with that location. → The tests pass.
- Known start. One test in a file navigates its router to a note's show
  location. → The next test in the same file mounts with `withRouter()`. →
  Its current route is root.
- Kept router. `SidebarFolderItem.spec.ts` keeps its router over
  `dummyRouteRecordsFromMetadata`, and `routes.spec.ts` keeps its own routers.
- Finished state. A reviewer lists `createRouter(` in `frontend/tests`. → Only
  `RenderingHelper.ts` and the two `tests/routes` specs pass the production
  `routes`. → The whole frontend suite passes.

<a id="one-production-router-builder"></a>

### Build the test router over production routes in one place

**Identity:** SEED-039#one-production-router-builder
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/003-one-production-router-builder/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"3a6a0ff345c3e5b2bec3e0a0207b3eed135d3c30e10712ce4dd5457c1102aec3","plan":"d4148e4a8588e5185fcad49854b514a4489308cc41a21eae6140d94fac29cc53"}}
```

Correction from the retrospective of
[SEED-039#specs-share-production-router](#specs-share-production-router).
Plan: [003-one-production-router-builder](../slice-plans/003-one-production-router-builder/PLAN.md).

**Goal:** Donut contributors reading frontend test support find one way to
build a router over the production routes, and no NoteShowPage support that
nothing uses.

**Scope:** `frontend/tests/helpers/RenderingHelper.ts` builds the
production-routes router once, shared by `productionRouterAt` and the default
of `withRouter()`. The unused sidebar-layout render path in
`frontend/tests/pages/noteShowPageTestSupport.ts` and the fixture only it
uses are deleted. No test name or assertion changes; no production code.

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
