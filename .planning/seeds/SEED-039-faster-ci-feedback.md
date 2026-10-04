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

<a id="internal-mocks-to-real-modules"></a>

### Replace frontend unit-test mocks of internal code with the real modules

**Identity:** SEED-039#internal-mocks-to-real-modules
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/009-frontend-specs-run-real-internal-modules/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"f05317652e859b2d7049cbf0368c0ec920539e3f840fb0a2540043addb0f843a","plan":"9a22df70017a532f959818112b7ddeba0627d875bac16cc327df41d05c55c501"}}
```

- **For / why:** Contributors trust a frontend unit test when it exercises the
  real code it depends on. A module mock of internal code tests the mock,
  hides real interactions, and adds a module-mocking mechanism that can fail
  on its own. That mechanism produced an unexplained CI failure (story
  SEED-039#mainmenu-mock-flake, recoverable at
  `263eb09a03:.planning/seeds/SEED-039-faster-ci-feedback.md`). The project's `unit-testing`
  skill already says to mock only external dependencies.
- **Effort hypothesis:** L or larger, low confidence: 52 spec and support
  files in six groups, planned as 13 slices by spec family.
- **Depends on:** None.
- **Safe stopping point:** any group of mocked modules replaced, with its
  specs passing.

**Goal:** Donut contributors can trust a passing or failing frontend unit
test, because it runs the real in-process code and replaces only what lies
outside the browser page under test. This serves the seed's aim of trustworthy
CI feedback. It does not promise shorter CI time.

**Scope:**

Required: in `frontend/tests`, every `vi.mock` of in-process code is removed
and the spec runs the real module. Each test keeps the behavior it checked.
Inventory on 2026-10-04 (64 files use `vi.mock`; 52 of them mock in-process
code):

| Group | Mocked module | Files | Real replacement |
| --- | --- | --- | --- |
| Routing | `vue-router` | 28 | A real router on the mounted component; navigation is read from the router's current location |
| Popups | `@/components/commons/Popups/usePopups` | 20 | The real popup stack; the spec reads the pending popup and answers it |
| Next assimilation | `@/composables/useGoToNextAssimilation` | 7 | The real composable, with `AssimilationController.next` given through `mockSdkService` |
| Toasts | `vue-toastification`, `@/composables/useToast` | 7 | The real toast library; the spec reads the toast message on the page |
| Time zone | `@/managedApi/window/timezoneParam` | 1 | The real function, with the browser time zone set by the existing `mockBrowserTimeZone` helper |
| Sign-in redirect | `@/managedApi/window/loginOrRegisterAndHaltThisThread` | 1 | The real function, observed at `browserLocation` |

With the popups group, `frontend/src/components/commons/Popups/__mocks__/usePopups.ts`
is deleted, along with test-support code that only served a removed mock.

Required: the frontend testing skill
(`.agents/skills/frontend-testing/SKILL.md`) names every allowed mock, so the
judgment is made once and not per spec.

Allowed mocks that stay (14 files):

- `@/managedApi/AiReplyEventSource`: the streaming part of the backend HTTP
  API, the same boundary `mockSdkService` covers for ordinary calls.
- `@/models/audio/audioRecorder`, `@/models/audio/recorderWorklet`,
  `@/models/wakeLocker`: microphone, audio worklet, and screen wake lock,
  which headless Chromium does not provide.
- `file-saver`: starts a browser download.
- `pdfjs-dist` in the gesture-zoom support file: the PDF engine and its
  worker, whose page geometry the spec must control.

Not committed in this delivery:

- Moving the allowed mocks from the wrapper module down to the browser API
  (for example faking `getUserMedia` or the streaming `fetch`).
- Replacing `vi.spyOn` on a real object, such as `vi.spyOn(router, "push")`.
- A lint rule or count limit on `vi.mock`.
- Changes to production code, except where a real module cannot be used in a
  spec without one.

Boundary assumption: an in-process library that renders or navigates inside
the page (`vue-router`, `vue-toastification`) counts as internal. The existing
rule supports this: the `unit-testing` skill limits mocks to third-party APIs
and network services, and prefers real in-process collaborators and real
browser rendering.

**Key examples:**

- Navigation. `NoteNewForm.submit.spec.ts` mocks `vue-router` to capture
  `push`. → The form is mounted with a real router and submitted. → The
  router's current location is the new note's show location.
- Route as a precondition. The `RecallPage` specs mock `useRouter` to report
  the route name `recall`. → The page is mounted with a real router placed at
  the recall location. → The specs pass without the mock.
- Popup that the spec answers. `RecallPage.answering.spec.ts` replaces
  `confirm` through `vi.mocked(usePopups)`. → The spec triggers the action,
  finds the confirm with its message on the real popup stack, and answers it.
  → The result of that answer shows on the page.
- Popup that the spec only silenced. The `NoteEditableContent` specs call
  `vi.mock` on `usePopups` with no assertion on it. → The line is removed. →
  The specs pass, and each test starts with an empty popup stack.
- Internal composable. The `MainMenu` specs automock
  `useGoToNextAssimilation`. → `AssimilationController.next` returns a next
  note through `mockSdkService` and the user chooses the assimilation entry.
  → The real router is at that note's show location.
- Toast. `NoteMoreOptionsForm.spec.ts` mocks `vue-toastification` to capture
  `error`. → The component runs with the real toast library. → The message
  text is visible on the page.
- Allowed mock. The `NoteAudioTools` specs keep their mocks of the audio
  recorder, recorder worklet, and wake locker, and the frontend testing skill
  names them.
- Finished state. A reviewer lists `vi.mock` in `frontend/tests` and finds
  only the allowed modules above.

**Architecture:**

- One owner for the boundary. The `unit-testing` skill leaves the list of
  allowed mocks to each package. The frontend testing skill becomes that list
  for the frontend; today it names only `mockSdkService`.
- Shared state moves into the tests. The real popup stack is module-level
  state with a document key listener, and a real router with web history
  changes the browser URL of the test page. Tests therefore depend on a clean
  start: shared test support gives each test an empty popup stack and a known
  route. This cleanup lives in one helper, not in each spec.
- Routing follows ADR 0005 (web routes) and the frontend testing skill's
  routing rule: test routers use production `routes` or
  `dummyRouteRecordsFromMetadata`, and navigation is asserted by named
  location. Removing the `useRoute` and `useRouter` stubs brings these 28
  files under that rule. No Accepted ADR conflicts with this story.
- Toasts need the toast plugin installed on the mounted test app to appear on
  the page; production installs it in `main.ts`. Toasts stay on the page
  between tests, so the clean start also clears them.
- Speed. Real routers and real toasts add work to 52 files in a suite this
  seed wants fast. Delivery reports `pnpm frontend:test` wall time before and
  after; a material slowdown is brought back for an owner decision.

<a id="specs-share-production-router"></a>

### Start every frontend spec's real router from one shared helper

**Identity:** SEED-039#specs-share-production-router
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** Contributors can trust and read a frontend spec's routing:
  every real router starts from a known page in one way. Today about 25 test
  files build their own production router, and plain
  `RenderingHelper.withRouter()` starts wherever the previous test in the same
  file left the browser URL, so a test can depend on test order. Found by the
  retrospective of SEED-039#internal-mocks-to-real-modules, which added the
  shared `productionRouterAt` helper.
- **Scope:** Test code only. Move the specs and support files under
  `frontend/tests` that call `createRouter(` over the production routes onto
  `productionRouterAt` (or `withRouter`), and give `withRouter()` without an
  argument a known start. Specs that test the route table itself
  (`tests/routes/*.spec.ts`) or need a router of their own keep it with a
  reason. No production code changes.
- **Evaluation:** `grep -rln 'createRouter(' frontend/tests` lists only
  `RenderingHelper.ts` and the files kept with a reason; a test that mounts
  with `withRouter()` sees the same starting route whatever ran before it; the
  whole frontend suite passes with unchanged test names and no weaker
  assertions.
- **Value / learning:** One way to start a real router in specs, and no
  order-dependent routing state.
- **Effort hypothesis:** S to M, medium confidence: mostly mechanical edits to
  about 25 files.
- **Depends on:** None.
- **Safe stopping point:** any group of files moved onto the shared helper,
  with their specs passing.

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
