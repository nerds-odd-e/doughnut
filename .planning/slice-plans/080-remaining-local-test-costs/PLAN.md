# Assess remaining local setup cost without weakening proof

**Identity:** SEED-098#remaining-test-costs
**Source:** [unexecuted optimization follow-up](../../seeds/SEED-098-remaining-local-test-costs.md#remaining-test-costs)
**Source review:** [source-review.md](source-review.md)
**Preparation:** written by resolve-only triage SEED-097#resolve-candidates
(`3ca2684040c9c89995bd9bd107d2760c330305a7`); refined for planned execution
on 2026-11-02 at `22b377bc242ae9da744ae287040bd98adf590e4b`. No slice had been
executed at preparation; execution needed its own instruction.

## Goal and scope

Contributors wait less for trustworthy ordinary local test feedback by removing
evidenced repeated setup, or receive a truthful no-gain finding. This plan owns
`e2e-batch-cold-start` and `frontend-sibling-spec-setup` until their implemented
parts close and any unimplemented remainder returns to the candidate record.

Preserve real owned-stack startup, frontend HMR, browser-file isolation, normal
CI/test selection, existing profile-only exclusions and Cucumber proof recording.
Do not replace a local invocation with a persistent shared stack, clear global
caches, change workers/shards/retries, skip scenarios or mock in-process code.
No numeric speed target or current baseline is supplied. Passing tests or
fewer files alone cannot justify keeping a change.

## Evidence and family analysis

Resolve reading: `23eb70051fddbc51df81f5086589b56b780ac666`, 2026-11-02.
Historical evidence remains dated, not a current timing claim:

- `744541489cee5b4a24ae7277cab65668fad452e3:.planning/slice-plans/001-cheaper-local-test-feedback/PLAN.md`:
  2026-11-01 first LFS batch ~4m18s versus warm runs under a minute;
  later warm polling experiment retained 500ms. There is no stage-attributed
  cold baseline. Do not attribute that gap to the polling interval.
- Current `sut-owned-lifetime.mjs` provisions/checks isolated E2E data and ports
  before service launch. `sut-services.mjs` runs `backend:sut:ci` and Vite;
  the backend command invokes Gradle `bootRunE2E --build-cache --no-daemon`.
  Readiness is a real health predicate. Provisioning, build, application boot,
  frontend first load and browser launch have distinct owners; the historical
  observation does not identify which is removable.
- `fe990eff58:.planning/quick/041-faster-frontend-unit-tests/PLAN.md`:
  historical compatible merges saved ~70–250ms per removed file. Per-file
  isolation stays necessary; `--no-isolate` failed 216/339 files and was slower.
- BookReadingPage's remaining specs protect loading/layout, position/restore,
  notebook switching and marking; they share helpers, but include scoped fake
  timers and substantial distinct journeys. The merged visibility file is 245
  lines. A blanket concatenation is not the proposed replacement.
- The bounded AI-reply family has two cheap, source-compatible opportunities:
  `models/aiReplyState.content.spec.ts` and `.toolCalls.spec.ts` use the same
  fresh `createMockAiReplyContext` with no module mocks or fake timers (9 cases).
  `components/conversation/AiResponse.streaming.spec.ts` and
  `.unknownToolCall.spec.ts` use the same EventSource mock and
  `useAiResponseMount` reset contract (7 cases). Keep their separate contexts
  for different event preconditions, within one spec per responsibility.
  The exclusion-proposal spec's answered-question/confirmation setup (7
  cases) stays distinct. Protocol callback proof and mounted DOM proof remain separate.

PFE reuses `aiReplyStateTestSupport`, `aiResponseTestSupport`, the ordinary
runner, and owned-stack lifecycle. No new fixture framework or telemetry system.
ADRs 0006 (failure meaning) and 0007 (environment isolation) govern any later
startup change. Existing North Star domain direction is unaffected.

## Baseline prerequisite and outside-in proof

Preparation ran one focused observation (below) and no profile or experiment.
Before each experiment, record literal command, revision, macOS/runtime,
worker/filter mode, host load, caches, wall time and executed cases. Raw logs
stay outside Git. Obtain comparable ordinary-command before/after observations;
plain Vitest JSON durations alone cannot prove local feedback improvement.

| Protection | Proof owner |
| --- | --- |
| Fresh owned stack, correct readiness/failure/cleanup and truthful cold attribution | Slice 1: ordinary LFS batch plus existing SUT lifecycle/ownership tests if its implementation changes |
| Content/done and all tool-call assembly, rejection, unknown, multiple-index and reset behavior | Slice 2: all 9 existing protocol cases through `createAiReplyStates` |
| Rendered streaming content/status/errors and unknown-tool display, skip/cancel/refusal | Slice 3: all 7 existing mounted cases; exclusion confirmations remain in their own spec |
| Same confidence with shorter ordinary local feedback | Every experiment measures its whole ordinary command; slice 3 owns final full frontend re-profile including moved proof cost |

## Ordered slices

### 1. Attribute a fresh E2E batch's extra startup cost
Type: Behavior
Status: done
Outcome: no measured removable stage; `e2e-batch-cold-start` stays pending.

Result (2026-11-02, `6c045ccfd5`, macOS 26.6.2, 16 cores, Node 26.13.0, Java
25.0.3, Gradle 9.8.2, Cypress 16.2.1; load 3.4→6.0 across runs, unrelated
cursor-agent present). Same ordinary command, sequential, 10/10 scenarios
green each, no leftover processes/ports, one proof recording per run:

| Run | State before | Wall | SUT healthy | Cypress `(Run Starting)` |
| --- | --- | --- | --- | --- |
| 1 (first in checkout) | no `backend/build`, `backend/.gradle`, Vite cache, worktree DB; `~/.gradle` build cache warm | 54.95s | 16.46s | 25.22s |
| 2 | warm | 41.89s | 12.12s | 13.93s |
| 3 | warm | 43.25s | 12.28s | 14.34s |

Attribution of the ~12.4s first-run gap (one first sample; stamped from
stdout, `sut.log` and Spring log timestamps):
- Cypress healthy→run-start window +6.8s (8.8s vs ~2s): unattributed; existing
  logs print nothing inside it. Missing evidence: a stage breakdown (config,
  `setupNodeEvents`, browser launch) on a genuine first run in another freshly
  prepared worktree versus a warm run.
- Backend readiness +4.2s: Gradle start to task graph +3.2s (first
  configuration plus `compileJava` FROM-CACHE versus UP-TO-DATE, not separable
  without Gradle timestamps); Flyway first migration of the empty isolated
  schema ~0.7s (inherent to per-worktree databases, ADR 0007); Spring boot
  unchanged (6.0–6.5s); remainder within 500ms poll granularity, not
  attributed to polling.
- Mountebank (+2.8s) and Vite (+1.9s) finished before the backend, so they
  are not on the critical path. `optimizeDeps.force` reoptimizes on every run,
  so it is not a first-run cost.
- The historical ~4m18s did not recur because `compileJava` came from the
  build cache. Missing evidence: a first run with a build-cache miss, to show
  whether real compilation is the inherent cost.

No startup edit was made or proposed. A missing stage baseline is not an
accepted inherent cost, so the candidate returns pending with the missing
evidence above. Raw logs stayed outside Git.
Proof: `./scripts/run.sh pnpm cy:run --spec e2e_test/features/cli/cli_notebook_lfs.feature`
through its normal owner, recording case count, total wall and stage evidence.

Behavior: the first ordinary application invocation in the owned checkout →
record provisioning, Gradle build/cache state, application readiness, Vite first
load and Cypress launch where the existing logs expose them → distinguish that
run from repeated warm invocations without treating unlike cache states as a
speedup. Preserve actual scenario proof and invocation cleanup.

Reuse existing logs/timestamps; any temporary observation stays outside Git.
Do not delete a developer cache or call a run cold solely because it ran first.
Record actual cache/build preconditions, host contention and attribution limits.
One natural first invocation plus two same-command warm observations is the
initial bounded investigation; an inconclusive cause is not an accepted cost.

Decision before a startup edit: identify one measured removable stage and a
smallest change preserving that owner's contract, then refine this same plan
with its concrete replacement and consumer proof. Otherwise keep the startup
candidate pending with its missing evidence. Do not dispatch a speculative
startup optimization or declare the first/warm gap solved. A specific behavior
shown inherent may be accepted only under the candidate acceptance rule.

### 2. Share AI reply protocol spec setup
Type: Structure
Status: done
Outcome: no demonstrated gain; merge undone, both original specs restored unchanged.

Result (2026-11-02, `0d343fdc2a`, macOS 26.6.2, Node 26.13.0, Vitest 5.0.5
browser chromium headless, default workers/isolation; host load not quiet:
1-minute average rose ~4.4→16 from unrelated sessions). The merged
`aiReplyState.spec.ts` (207 lines, all 9 cases verbatim, one fresh
`createMockAiReplyContext` per case) passed with both typechecks.

- Focused command, alternating three pairs: before 4.96/5.10/5.15s wall
  (Vitest 3.35/3.45/3.50s, 5 files) versus after 5.08/4.98/5.32s (Vitest
  3.41/3.46/3.67s, 4 files), 23/23 tests each. Means: after ~60–80ms slower,
  within the 0.2–0.35s spread.
- Full command: before 77.87s and 86.55s, after 84.56s and 86.68s (397 vs
  396 files, 2454 tests each). Load swings of ~8s are far larger than the
  historical 70–250ms per-file saving; the closest-load pair differs by 0.13s
  with after slower.
- Limits: three focused pairs cannot resolve a saving under ~100ms. One
  unmeasured explanation is that in parallel browser mode a small file's setup
  overlaps other workers instead of extending the critical path.

The removed-name sweep was not needed because the merge was not kept.
Proof: normal focused AI-reply command before/after, all 9 protocol cases;
same-content frontend typechecks and ordinary full-suite comparison.

Directly owned weakness: two browser pages/module graphs initialize the same
protocol boundary and fresh context. Merge content/done and tool-call specs
into `models/aiReplyState.spec.ts`; keep every case, assertion and context reset.
Deduplicate imports/setup, preserve the existing support seam, and stay <=250
lines after formatting. No production or boundary change is proposed.

Hypothesis: removing one file avoids one setup/import payment (historical
70–250ms; current saving unknown). Before/after alternate three focused runs:
`./scripts/run.sh env -u NODE_ENV pnpm frontend:test tests/models/aiReplyState tests/components/conversation/AiResponse`.
Record actual cases (currently 23 across the selected family), timers, filter
and files. Run `./scripts/run.sh pnpm -C frontend exec vue-tsc --noEmit` and
`./scripts/run.sh pnpm -C frontend exec tsc --noEmit` on the same content. Compare full
`./scripts/run.sh env -u NODE_ENV pnpm frontend:test` before/after under quiet,
comparable ordinary local conditions; include the changed family's proof cost.
Keep only demonstrated ordinary-command improvement with all protection green;
otherwise undo only this experiment and record no demonstrated gain.
After retaining the merge, sweep the removed spec filenames across the
repository once and classify/repair remaining consumers. Record this as an
acceptance reading, separately from the surviving behavioral proof above.

### 3. Share mounted streamed-reply spec setup and re-profile
Type: Structure
Status: done
Outcome: merge kept; the focused ordinary command is consistently faster; the full suite cannot resolve the change.

Result (2026-11-02, `4ae0329ed8`, same environment as slice 2; host load
4.5–20 from unrelated sessions). `AiResponse.reply.spec.ts` (132 lines) holds
the 7 mounted cases verbatim, with one shared `@/managedApi/AiReplyEventSource`
mock. Its two describes keep their own `useAiResponseMount` and setup:
`submitAiReply` for streaming, and the `unknown_tool` call for unknown tools.
`AiResponse.exclusionProposal.spec.ts` is unchanged. Both typechecks passed.

- Focused command, six alternating pairs, 23/23 tests each: after was faster
  in every pair (Vitest −0.18 to −0.31s, wall mean ~−0.22s). In the calm-load
  pairs (load 5.0–5.9) the Vitest ranges do not overlap: before 3.39–3.46s,
  after 3.21–3.28s.
- Full command, three alternating pairs, 2454 tests each (397 → 396 files):
  before 85.03/91.70/84.82s, after 84.10/94.35/87.37s. The ~10s
  load-following spread cannot resolve a ~0.2s change in either direction.
- Final re-profile in the kept state: 87.37s wall, Vitest 85.33s, 396 files /
  2454 tests passed, load 14.5→12.9 (not quiet).
- Kept on the measured focused-command improvement, with no demonstrated
  full-suite gain or regression. Removed-name sweep: `git grep` for the two
  removed spec names matches only this plan's dated evidence, so there is no
  consumer to repair.

Candidate return: `e2e-batch-cold-start` returns pending with slice 1's
missing evidence. `frontend-sibling-spec-setup` returns pending for the
unpicked families. This plan removed only the mounted streaming/unknown-tool
pair, and the protocol pair is recorded as no demonstrated gain.
Proof: all 7 mounted streaming/unknown-tool cases plus the independent exclusion
proposal cases, both frontend typechecks, focused and full ordinary commands.

Directly owned weakness: duplicate EventSource mock registration and mount/reset
setup. Merge the streaming and unknown-tool specs into `AiResponse.reply.spec.ts`
with shared imports/module mock; preserve distinct nested setup and per-case
DOM outcomes. Keep the exclusion-proposal harness distinct and preserve every
case; do not replace mounted proof with protocol spies. Expected saving is one
file's setup, unmeasured here. Use slice 2's alternating focused command and
typecheck gates, and include this change in final ordinary full-suite timing.
After retaining the merge, apply slice 2's separate filename-sweep reading to
the removed streaming and unknown-tool spec names; add no absence test.

Decide/reassess: retain only supported whole-command improvement, undo a noisy,
regressing or ineffective experiment. Re-run the final ordinary local command
under comparable conditions; a failed re-profile is incomplete measurement.
Record each keep/rejection, before/after wall and cases, and measurement limits.
Remove a transferred entry only for work actually done. Return all unpicked
sibling families or unresolved cold cost to pending under their original slugs;
the two AI-reply merge assessments do not claim every sibling opportunity solved.

## Settled premises

Observed 2026-11-02 in this preparation's workspace at `22b377bc24`:

- Focused proof route and case counts:
  `./scripts/run.sh env -u NODE_ENV pnpm frontend:test tests/models/aiReplyState tests/components/conversation/AiResponse`
  passed 5 files / 23 tests (content 2, toolCalls 7, streaming 3,
  unknownToolCall 4, exclusionProposal 7); Vitest 4.33s, wall 8.2s including
  install check. One untimed-conditions observation, not a baseline.
- Shared setup: both protocol specs import `createAiReplyStates` and
  `aiReplyStateTestSupport` with a plain `beforeEach`, no `vi.mock` or fake
  timers; both mounted specs register the identical
  `@/managedApi/AiReplyEventSource` tracking mock and the same
  `beforeEachMount`/`afterEachReset` contract, no fake timers. Merged sizes
  stay under 250 lines (217 and 144 before import deduplication).
- Filename consumers: `git grep` for the four spec names finds only this plan,
  so the removals have no other consumer to repair.
- Slice 1's stage attribution depends on what the owned-stack log exposes; that
  is its own bounded probe, and an inconclusive result keeps the startup
  candidate pending without blocking slices 2–3.

## Current decisions

- Preparation collected no baseline timings; each experiment measures its own
  comparable before/after.
- Each experiment uses hypothesize → comparable baseline → smallest change →
  verify and measure → keep/undo → reassess. No loss of existing behavior proof.
- Code/edit leaves target ~5 minutes, scrutinize >5 and stop/refine >10 active
  minutes. Repeated ordinary commands and service startup are explicit external
  verification-time exceptions, not permission to enlarge implementation scope.
- Keep the prior owner's HMR/isolation/exclusion decisions. Preserve the
  installed ordinary Cucumber proof recorder in any changed profile.
- [Failing and flaky tests](/Users/terryyin/git/open-dough/AGENTS.md#failing-and-flaky-tests)
  applies with Donut guidance to later execution/delegation/CI repairs.
- Future delivery uses Execute Plan: independent refactor, generation if
  triggered, coordinator format once, plan update, normal hooks, push and CI.

## Concerns and readiness

Refinement is not needed: each spec experiment has one compatible setup and
one proof loop, and the cold path is an early probe slice with a decision
before any startup implementation. Missing current timings are produced by
the slices themselves. No remaining slice-specific concern or human trade-off
was identified.
