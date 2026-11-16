---
id: SEED-130
status: dormant
planted: 2026-11-16
planted_during: owner-directed CI backlog prioritization
trigger_when: the team takes up end-to-end CI coverage and shard balance
scope: medium
---

# SEED-130: Keep end-to-end CI coverage complete and workloads balanced

## Why This Matters

Donut contributors need every eligible end-to-end spec to run in CI and want
faster feedback from the last shard to finish. All six shards currently depend
on explicit folder patterns. A spec outside those patterns can be missed, and
folder counts alone cannot tell maintainers how to balance the workload.

## Alternatives and Decision

Keep the existing six slots: five explicit folder-pattern groups and their
remainder. Make rebalancing repeatable with a private Donut agent skill.

| Candidate | Coverage and feedback trade-off |
| --- | --- |
| Edit six folder lists by hand | Smallest immediate edit, but a new directory can still escape CI; it does not meet the coverage goal. |
| Compute the remainder of five explicit groups | Preserves the selected six-slot model and automatically admits new directories; timing still needs evidence. Recommended. |
| Assign files dynamically by historical duration | Can adapt more often, but changes the selected folder-group model and makes execution depend on timing data; defer. |

Borrow the load-balancing rule used to assign jobs to identical machines:
consider heavy folder workloads first and place them in lighter groups, then
compare the resulting maximum across all six. Folders are jobs, the six CI
slots are machines, and summed recorded file durations estimate load. This is
a candidate-selection aid, not a runtime scheduler. The correspondence breaks
at browser/setup cost, retries, external calls, runner variation, and new files
without timings; the estimated maximum does not prove the last job ends sooner.

## Story Decomposition

<a id="rebalance-ci-e2e-shards"></a>

### Keep all E2E specs covered and rebalance the six CI shards

**Identity:** SEED-130#rebalance-ci-e2e-shards
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/001-complete-and-balance-ci-e2e-shards/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"fb1b00c47b7539e1bfcdcc2d03daac4abfb5403462381d594962e53b4f5bc28b","plan":"00706da06416f1bc73fd607fbbe46e2782c1215996dd6907f9f34e00f4fc3fa6"}}
```

#### Goal

Donut contributors receive complete eligible E2E feedback. Maintainers can
repeat an evidence-based rebalance of the six shards to reduce the last
shard's elapsed time as the suite changes, without silently losing coverage.

#### Scope

- Keep five explicit folder-pattern groups and replace the sixth group with
  exactly the remaining discoverable feature files. Across the six selections,
  every eligible file belongs once. Preserve the runner's file exclusions and
  CI scenario/tag filters; a discovered file containing only excluded scenarios
  need not execute. Keep six matrix jobs and six expected case-proof parts.
- Add a private project-local Donut skill for on-demand rebalancing. On every
  invocation, read recent comparable completed CI proof, preferring the
  controlling dashboard's **Intent → CI status** when known and sufficient,
  with GitHub Actions artifacts and job timing as the fallback or supplement.
  Reuse the existing proof format. Keep downloaded evidence in temporary
  storage outside the checkout, with no repository cache or raw-proof files.
- Use the skill for the initial balance. Name repository, runs, attempts,
  revisions, their actual selections and runner conditions. Reconcile files,
  counts and durations against all six groups before proposing moves. Compare
  current and proposed per-group recorded-time estimates; keep unknown costs
  visible. Check elapsed job times separately and distinguish forecasts from
  measured results. Apply only timing moves supported by comparable evidence.
- If proof is unavailable, incomplete, incompatible, or leaves decisive timing
  costs unknown, report the gap and leave timing-based assignments unchanged.
  The coverage guarantee and working skill remain independently useful.

**Scope limits:** No seventh job, automatic runtime scheduling, test/scenario
rewrites, new profiling run, shared Open Dough distribution change, dashboard
feature, new proof schema, or numerical speedup guarantee. An initial skill
invocation that cannot support a move reports that outcome; it is not a claim
of faster CI. Normal delivery CI supplies the post-change observation.

#### Key examples

1. An eligible file exists under `learning_session/`, or a contributor adds an
   eligible file in a new directory. CI resolves its six selections → that
   file runs through the remainder once, without a folder-list edit.
2. A file belongs to an explicit group. CI resolves the remainder → the
   explicit group owns it once. Snapshot files and scenarios excluded by the
   existing CI tags keep their existing treatment.
3. Comparable successful attempts include all six parts and usable file times.
   A maintainer invokes the skill → it compares all six current and candidate
   loads, preserves the partition, and applies a supported folder reassignment;
   a later comparable CI result determines the measured elapsed-time claim.
4. An attempt re-ran only failed jobs, a revision changed selection, or a new
   covered file has no timing. Skill invocation → it names the missing or
   incompatible evidence, avoids treating unknown time as zero, and leaves
   timing assignments alone when the gap prevents a supported decision.

#### Architecture

Give the five group definitions and computed complement one selector owner;
CI and its coverage proof consume that owner. Reuse the existing Cypress file
discovery rules and retain Cucumber's CI tag filtering. Preserve the owned
`e2e-runner` lifetime and primary/isolated checkout boundaries under
[ADR 0007 — Environments and isolation](../../docs/adrs/0007-environments-and-isolation-accepted.md).
Missing proof is an explicit operational outcome, consistent with
[ADR 0006 — Failure handling](../../docs/adrs/0006-failure-handling-accepted.md).
No Accepted ADR conflict or new North Star direction was found.

#### Investigation and strongest doubt

At `3095fd3c6ac7c6489a874687449b5a76f915dfd5` on 2026-11-16,
the current matrix selected 97 of 98 feature files, with no overlaps. The
unselected `learning_session/commissioned_learning_session.feature` survives
the installed preprocessor's CI tag filter. A temporary local probe through
that preprocessor and the existing runner selection prefix covered it and an
added directory exactly once while preserving tag/snapshot exclusions; it
started no application or paid service. Probe edits were removed.

Dashboard proof for successful runs `43588251390/1` at `2b95a250` and
`43573637599/1` at `f4b8dccb` reports 93 files, 379 cases, all six parts and
no untimed cases. The relevant E2E code/configuration is unchanged between
those revisions and the preparation revision. The latest selected run,
`43726030546/1` at `3b200a8f`, failed overall and is diagnostic evidence only.

The strongest doubt is that balancing summed scenario durations could satisfy
the proposal while leaving the contributor waiting just as long. In the two
successful runs, shard 6 was last but elapsed job time varied from 355 to
484 seconds; Cucumber step time omits part of the runner cost. The newly
covered learning-session file is also missing from those timing observations.
Keep that gap explicit, compare like-for-like elapsed times, and make no
speedup claim from estimates alone.

**Effort hypothesis:** M; the positive-file selection prefix is observed, but
hosted timing evidence must be refreshed during execution.
**Depends on:** No other queued story.
**Safe stopping point:** Complete coverage plus the repeatable skill, with a
reported no-move result if current evidence does not support timing changes.

**Plan:** [Complete and balance the six CI E2E shards](../slice-plans/001-complete-and-balance-ci-e2e-shards/PLAN.md)

## Breadcrumbs

- [Current GitHub Actions workflow](../../.github/workflows/ci.yml) defines
  the E2E matrix and the expected six proof parts.
- [CI proof summary](../../docs/ci-proof-summary.md) describes the dashboard,
  the GitHub artifact, timing units, and how missing parts are reported.
