# DearDough Process Findings

## DD-001 — Implementation subagent ends its turn "waiting" on its own background test instead of blocking for the result

An implementation subagent launched its own long-running test command
asynchronously and then ended its turn reporting that it was "waiting" or
"pausing" for the background result, instead of blocking until the command
finished and reporting the actual pass/fail outcome, despite explicit
instructions to do so before reporting.

### Occurrences

- Execution: SEED-017 Story 1 / quick-099-receive-compatible-accepted-history / 5ed11cd8e0
  - Tool: Claude Code
  - Open Dough release: 0.3.8
  - Evidence: slice 4 implementation agent's first report ("I've started the
    vitest run and am waiting for its completion notification before
    evaluating results. Pausing here.") and, after being resent the same
    request, its second report ("Pausing for the background run to
    complete."), before the coordinator ran the focused test command directly
    to obtain the actual result (95/95 passed).
  - Observed effect: two extra coordinator round-trips (one resend, one
    direct verification run) before slice 4's wrap-up could proceed.

## DD-002 — Concurrent Vitest processes against the same worktree cause spurious test timeouts under resource contention

Running more than one Vitest process against the same CLI test suite at the
same time (coordinator and/or subagents, or a formatting command running
concurrently with a test run) caused real, passing tests to fail with a
5000ms timeout under load. Each occurrence was confirmed environmental by a
clean solo re-run passing all tests.

### Occurrences

- Execution: SEED-017 Story 1 / quick-099-receive-compatible-accepted-history / 5ed11cd8e0
  - Tool: Claude Code
  - Open Dough release: 0.3.8
  - Evidence: during slice 4 verification, `notebookPull.structuralHistory.suite.ts`'s
    "names the structural path for remote same-path-reversed-rename and
    leaves the checkout unchanged" test timed out while a second vitest
    process ran concurrently; a clean solo re-run of the same suite passed
    95/95. Recurred after slice 4's `format:changed` step, where 5 tests
    failed during a run that overlapped with another process, and a clean
    solo re-run passed 95/95.
  - Observed effect: two rounds of failure triage (checking for concurrent
    processes, re-running solo) before trusting the test suite's result.

## DD-003 — Discovery scaled to the incident size before defining an evidence stopping rule

A profiling investigation treated 10,000 additions as a mandatory initial scale
and continued expensive measurements without first asking whether a smaller
capture could choose the next optimization. Slice refinement subdivided execution
work but did not challenge the measurement's decision value until the user did.
Recommendation: define the question, smallest informative fixture and stop condition
before capture; expand only when the current evidence cannot resolve that question.

### Occurrences

- Execution: SEED-018 story 2 / quick-106-profile-large-notebook-publication / 00c9735cd1
  - Tool: Codex
  - Open Dough release: unknown
  - Evidence: PLAN 106 original workload and refinements; first/second large HTTP timings
    4,987,017 ms (including sleep) / 3,772,321 ms; user efficiency review;
    3651153736 removes further large rejection and completes evidence handoff.
  - Observed effect: Two long valid runs and growing orchestration preceded the user-directed
    reduction. Final handoff used existing profiles plus small rejection proof.
  - Inference: A smaller adaptive workload would likely have reduced discovery cost;
    its exact necessary size and elapsed saving were not measured.

## DD-004 — Reusing a browser assertion per file hides bulk verification cost

The profiler reused a correct single-file Cypress helper for every added file.
This retained correctness intent but made verification dominate the post-response
wait. Recommendation: keep the public publish/pull journey and verify the received
filesystem in bulk at the Node task boundary, with mismatch proof before replacing
the existing assertions. Reuse must assess operating cost as well as semantics.

### Occurrences

- Execution: SEED-018 story 2 / quick-106-profile-large-notebook-publication / 00c9735cd1
  - Tool: Codex
  - Open Dough release: unknown
  - Evidence: 453603a428 and current notebookPublicationProfile.ts:expectReceived;
    second capture 2026-09-11T12-37-08.116Z, receiver-driver-observation.json
    and independent-verification.json; user safe-stop request.
  - Observed effect: Cypress was still doing per-file checks more than 20 minutes after
    the HTTP response. Independent full 10,000-file verification took 2.5 seconds;
    the slow path remains in the delivered harness despite the documented workaround.
  - Inference: Browser-command overhead is avoidable here; the recorded 2.5 seconds
    is one local observation, not a universal performance guarantee.

## DD-005 — Unchanged long-job updates consume agent work without advancing a decision

The coordinator repeatedly slept, polled and emitted near-identical updates while
an agent owned the measured request. Recommendation: long-job coordination should
surface completion, failure or required intervention; progress cadence should
allow quiet waiting when nothing actionable changes. Instruction authors should
provide that exception rather than require continuous low-information narration.

### Occurrences

- Execution: SEED-018 story 2 / quick-106-profile-large-notebook-publication / 00c9735cd1
  - Tool: Codex
  - Open Dough release: unknown
  - Evidence: This execution conversation's repeated minute-scale waiting messages
    during both long publications; execution instructions required commentary
    at least every 60 seconds; user explicitly reported excessive token cost.
  - Observed effect: Many updates restated that no response had arrived. Approximate elapsed
    reports also needed correction to actual host time; no new optimization
    decision followed most updates.
  - Inference: The cadence requirement contributed to unnecessary narration, while
    coordinator polling choices added overhead. Exact token counts are unavailable;
    no numerical token saving is claimed.
