# Stop the MainMenu resume specs from failing intermittently in CI

**Identity:** SEED-039#mainmenu-mock-flake

## Source

[Stop the MainMenu resume specs from failing intermittently in CI](../../seeds/SEED-039-faster-ci-feedback.md#mainmenu-mock-flake),
refined on 2026-10-03. The story records the observed failure
([run 37108823838](https://github.com/nerds-odd-e/doughnut/actions/runs/37108823838),
frontend shard 2/2) and the owner's decision, made the same day after slice 1, to remove the recall-state mocks.

## Goal and scope

Contributors can rely on the frontend unit-test job: the twelve spec files that
mock `@/composables/useRecallData` (and `useResumeRecall`) use the real recall
state instead, so the module-mock mechanism that failed in
[run 37108823838](https://github.com/nerds-odd-e/doughnut/actions/runs/37108823838)
is no longer in their path.

- **Included:** drive the real `useRecallData` through its setters; observe
  through the rendered component and the router; one shared reset of recall
  state between tests; delete fakes that only these mocks used
  (`createUseRecallDataMock` and its helpers once no caller remains).
- **Excluded:** other module mocks in these files (for example
  `useGoToNextAssimilation`); raising retry, shard changes, skips; a Vitest
  upstream report; a general review of frontend mocking; CI-time savings.

## Outside-in proof

- **Focused check:**
  `env -u NODE_ENV CI=true CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/toolbars tests/pages/RecallPage tests/components/recall/AssimilationPanel`
- **No mock left:** `grep -rnE 'vi.mock\("@/composables/(useRecallData|useResumeRecall)"' frontend/tests`
  prints nothing at the end of slice 4.
- **Hosted CI:** both `Frontend Unit Tests` shards pass after publication.

| Promise | Slice | Proof |
| --- | --- | --- |
| MainMenu specs use real recall state; Resume behavior kept | 2 | three MainMenu files pass; no recall mocks in them |
| RecallPage specs use real recall state; fake deleted | 3 | six RecallPage files pass; `createUseRecallDataMock` gone |
| AssimilationPanel specs use real recall state | 4 | three files pass; grep above prints nothing |
| Failure not hidden | 2–4 | diff leaves `retry`, shard count, and skip markers unchanged |

## Slices

### 1. The failed automock is produced on demand and explained
Type: Behavior
Status: done (2026-10-03): not reproduced. Every attempt is under *Learnings*.
The owner then chose to remove the mocks instead of finding the cause (see the
story's owner decision); slices 2–4 replace the earlier fix and Vitest-report
slices.

### 2. MainMenu specs run on the real recall state
Type: Structure
Status: done (2026-10-03). Shared reset: `resetRecallData()` in
`frontend/tests/helpers/recallDataTestSupport.ts`, also used by
`useRecallData.spec.ts`. The Resume click asserts the `recall` route and that
`.menu-wrapper` keeps `is-collapsed`. `createUseRecallDataMock` left
`mainMenuMocks.ts`; RecallPage specs use their own copy in
`tests/pages/recallPageTestSupport.ts`. Focused set: 40 files, 232 tests pass;
`vue-tsc --noEmit` passes.
Proof: `MainMenu.spec.ts`, `MainMenu.recall.spec.ts`, and
`MainMenu.resume.spec.ts` pass under the focused command; none of them mocks
`useRecallData` or `useResumeRecall`.

- Add one shared test helper that resets the real recall state through its
  setters, called before each test in the specs that touch it (the pattern in
  `tests/composables/useRecallData.spec.ts`'s `resetRecallDataState`; move it
  to shared test support rather than copying it).
- Set paused / remaining / current index through `setIsRecallPaused`,
  `setToRepeat`, `setCurrentIndex`. Keep the faked `getMenuData` consistent
  with that state (`MainMenu` copies `toRepeat` from menu data only when it is
  empty).
- The Resume click test asserts the router is on `recall` (and the collapsed
  menu did not expand) instead of a `resumeRecall` spy.
- Remove `useRecallData` setup from `setupMainMenuTests`.

### 3. RecallPage specs run on the real recall state
Type: Structure
Status: done (2026-10-03). `createUseRecallDataMock` is deleted;
`givenRecallQueue(...)` in `recallPageTestSupport.ts` sets the real queue. With
real shared state, pages left mounted reacted to the next test's state, so
`recallPageTestSupport.ts` now uses `enableAutoUnmount(afterEach)`. Fake-call
checks became state checks (for example `diligentMode.value`). RecallPage: 6
files, 37 tests pass; `vue-tsc --noEmit` passes.
Proof: the six `RecallPage*.spec.ts` files pass under the focused command; none
mocks `useRecallData`; `createUseRecallDataMock` in
`tests/pages/recallPageTestSupport.ts` and any helper only it used are deleted.

Where a spec asserted a call on a fake setter, assert the resulting state or
rendering instead. Use slice 2's reset helper.

### 4. AssimilationPanel specs run on the real recall state
Type: Structure

Use `resetRecallData()`. If a mounted panel reacts to a later test's state,
unmount it automatically after each test, as `recallPageTestSupport.ts` does.
Status: planned
Proof: the three `AssimilationPanel*.spec.ts` files pass; the no-mock grep
prints nothing; the full focused check passes.

## Current decisions

- Owner decision (2026-10-03): mock only external dependencies. Recall state
  and the resume action are internal, so their mocks are removed rather than
  repaired. This replaces "no fix without a reproduction".
- Run frontend tests with `env -u NODE_ENV`; the owner's shell sets
  `NODE_ENV=production`.
- Local gates follow the `unit-testing` and `frontend` skills and the
  execution wrap-up in `CLAUDE.md`. Changes touch test files only.

## Learnings

Slice 1 (2026-10-03, at `9e55e8eba3`): **not reproduced.** Every attempt below
used CI's settings (`CI=true`: files one at a time, one retry) and passed. The
provider copy was instrumented temporarily and restored; it matches the saved
original.

How the mocker behaves, from logging in the provider's `createMocker` plus a
context `request` listener:

- Files run one at a time, each in a new iframe. The previous file's `clear`
  finishes before the next file's `register` starts, because the tester
  awaits `clear` inside `onAfterRunFiles` before it answers `execute`. Seen in
  every run (for example `CLEAR done 11:25:18.773`, next `REGISTER start
  11:25:18.836`). A `clear` that overlaps the next `register` cannot happen in
  this flow, which rules out the shared-map race named in the premises.
- Imports wait for every `register`: the browser runner's `wrapModule` waits
  for the mocker's queue. The request for `useRecallData.ts` came after all
  `REGISTER done` lines in every run. It was answered with a 302 to
  `?mock=automock`, and that redirect then returned 200.
- Only the mock routes exist, so Playwright turns request interception (and
  the cache-disable flag) off after each file's `clear` and on again at the
  next file's first `register`.
- **What makes `MainMenu.resume.spec.ts` different:** it imports
  `useRecallData` itself, as its first import, so it asks for the automocked
  module about 0-30 ms after the routes are registered. That held in all ten
  runs under CPU contention as well. `MainMenu.spec.ts` asks for the module
  only through `mainMenuTestSupport.ts`, about 94 ms after its `register`.
  If a request sent just after interception is turned back on can miss the
  route, only a file shaped like the resume spec is exposed. This is a
  hypothesis: no run showed such a miss.
- The CI log of run 37108823838 shows no unhandled error or rejection. A
  failed `register` would continue the import (`prepare().finally`) and leave
  an unhandled rejection, so the log argues against a failed `register`.

Attempts, in order (all `env -u NODE_ENV CI=true CURSOR_DEV=true nix develop -c …`):

| # | Condition | Result | Rules out |
| --- | --- | --- | --- |
| 1 | Instrumented provider; `pnpm -C frontend test` on MainMenu.spec, pdfBookViewerGeometryResample, MainMenu.resume | pass; order of register, request, and clear recorded as above | The route premise holds: 302 to `?mock=automock` for each request |
| 2 | The same files forced into CI's order with a scratch config whose sequencer keeps the command-line order | pass | File order alone |
| 2b | DonutApp.searchHistoryMigration (loads the real `useRecallData.ts` with no routes), then pdf, then resume | pass; the real load went to the network, and the later request was still caught by the route | A cached real module from an earlier unmocked file |
| 2c | All 159 files of CI's shard 2/2 in the exact order from the CI log | pass | CI's full order on this machine (7 frontend files have changed since `afc9f1163d`) |
| 3a | 1500 ms delay before `route()` in `register` | pass | A slow `route()`: imports wait for it |
| 3b | 1500 ms delay before `unroute()` in `clear` | pass; the next `register` waited for `CLEAR done` | Cleanup of the previous file overlapping the next `register` |
| 3c | `register` returns without awaiting `route()` | pass; the handler is added on the client side immediately and the request was still routed | A request that arrives before Playwright's client sees the route |
| 4 | `pnpm frontend:test --shard=2/2`, 10 runs with 32 busy-loop processes on 16 cores (load average 29-50) | 10/10 pass (resume 35-56 ms) | Plain CPU contention on macOS |

Not tried, because it is outside the bounded attempts or needs the owner:
hosted-runner reruns; Linux/CI Chromium; a gap inside Chromium between
`Fetch.enable` being acknowledged and the renderer using the intercepting
loader. Slices 2 and 3 do not start without a reproduction.

Standalone reproduction attempt (owner's instruction, 2026-10-03, at
`ba6581f1b5`): **not reproduced.** The suite uses no Donut code: Vitest 5.0.3
browser mode with Playwright and Chromium, headless,
`fileParallelism: false`, `retry: 1`, one instance, and a sequencer that runs
files by name. Its modules are a target module, a second module, and a chain
of 120 modules ("heavy").

Spec shapes:

- **A:** bare `vi.mock` of the target and the second module. The mocked
  target is the first import, and `vi.mocked(fn).mockReturnValue` runs in
  `beforeEach`.
- **B:** loads the real target and the heavy chain unmocked, like
  `DonutApp.searchHistoryMigration`.
- **C:** mocked target reached through a helper, like `MainMenu.spec.ts`.
- **M:** A plus a factory mock, like the resume spec's
  `AiReplyEventSource`.
- **P:** plain, no mocks.

The suite ran inside the frontend directory and was deleted afterwards. The
generator lives outside the repo.

| Variant | Files × runs | Condition | Mocked-first executions | Failures |
| --- | --- | --- | --- | --- |
| v1 | 60 (B,P,A repeating) × 15 | idle | 300 A | 0 |
| v2 | 60 (B,A,C,A,P) × 15 | idle | 360 A + 180 C | 0 |
| v3 | 60 (B,P,A) × 15 | 32 busy loops on 16 cores | 300 A | 0 |
| v4 | 150 (B,P,A) × 6 | busy loops + Donut's `orchestratorGcReporter` (5 s) | 300 A | 0 |
| v5 | 60 (B,P,A) × 10 | busy loops + the same reporter every 30 ms (110 GCs counted in one run) | 200 A | 0 |
| v6 | 60 (B,P,M) × 15 | busy loops + GC every 30 ms | 300 M | 0 |

In total, 1,760 mocked-first file runs and 180 mocked-via-helper runs, with no
failed automock. The B → P → A order switches interception off and on before
every mocked-first file. On this macOS machine with the bundled Chromium, a
mocked module requested first, right after routes are registered, is not
missed often enough to show up even under heavy CPU load and frequent forced
GC. The remaining place to look is the CI runner itself (Linux, its Chromium,
its load), which is a hosted-CI decision for the owner.

Linux container attempt (owner's instruction, 2026-10-03, at `196e38cf5a`):
**not reproduced.** The image `doughnut-mockflake-ci:arm64` is Ubuntu 24.04
(CI uses `ubuntu-24.04`) with Node v26.10.0 and pnpm 11.28.3, the same
versions as the CI log, plus `playwright@1.63.0 install-deps chromium`. It
ran under colima's default profile. The tracked tree went in through
`git archive HEAD`, and `pnpm --frozen-lockfile recursive install` ran
inside the container. `pnpm frontend:test` then installed Chrome Headless
Shell 153.0.8010.12 (Playwright chromium v1243), the same version CI
downloaded.

- **Architecture differs from CI.** An amd64 image under emulation crashed
  Chromium at launch (SIGSEGV), so the runs used native arm64 Linux and
  Playwright's linux-arm64 build of the same Chromium version. CI is x86_64.

| Run | Container CPUs | Load | Runs | Shard duration | Result |
| --- | --- | --- | --- | --- | --- |
| `CI=true pnpm frontend:test --shard=2/2` | 4 | idle | 12 | 47–53 s | 12/12 pass |
| same | 2 | idle | 6 | 81–90 s (CI: 87.7 s) | 6/6 pass |
| same | 2 | 4 busy loops | 5 | 270–287 s | 5/5 pass |
| standalone suite, B,P,A × 60 files | 2 | idle | 15 (300 mocked-first files) | — | 0 failures |
| standalone suite, B,P,M × 60 files | 2 | 4 busy loops + GC every 30 ms | 15 (300 mocked-first files) | — | 0 failures |

In total, 23 shard runs (207 runs of `MainMenu.resume.spec.ts`) and 600
standalone mocked-first file runs, with no failed automock. The 2-CPU idle
runs take as long as the CI shard did. The same Chromium version on Linux,
the same Node and pnpm, and hosted-runner speed or heavier load do not
produce the failure here. What remains untested is x86_64 Chromium on a
hosted runner, which only hosted CI reruns can cover.

History check (2026-10-03): the resume spec, `mainMenuTestSupport.ts`,
`useRecallData.ts` and `useResumeRecall.ts` last changed in August, and the CI
settings in `vitest.config.ts` are unchanged. The one nearby change is
`3624e353a8` on 2026-10-02, which moved `vitest`, `@vitest/browser-playwright`
and `@vitest/ui` from 5.0.2 to 5.0.3, the day before the only known failure.
Comparing the published 5.0.2 and 5.0.3 packages (`vitest`, `@vitest/browser`,
`@vitest/browser-playwright`, `@vitest/mocker`) and the upstream commits
`v5.0.2...v5.0.3` found nothing that changes when or how a mock is applied:
- the mocker now checks a path boundary when stripping the root (`1c3888bce`).
  That changes only paths in a sibling folder whose name starts with the root's
  name. It is deterministic, so it would fail every time;
- `prepare()` passes the queue iterator to `Promise.all` without spreading it.
  It behaves the same;
- the Playwright provider changes only the error type for a page crash;
- the browser server now starts listening at the first browser launch rather
  than at startup (`7d8ed3e9b`), which is once per run, not per file;
- the module-cache fix (`38f98855f`) is in Node-side module fetching, which
  browser mode does not use for spec modules;
- the remaining changes are build-tool output.

The update is not a supported cause.
