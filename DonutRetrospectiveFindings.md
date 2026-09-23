# Donut Retrospective Findings

Findings specific to Donut’s product, repository tooling, or local conventions.
Findings about shared OpenDough skills, supporting guidelines, and skill scripts
remain in [DearDough.md](DearDough.md), including those observed in this project.
Existing finding identifiers and occurrence evidence are preserved.

## Review and grouping — 2026-09-23

Ownership follows the failing responsibility, rather than the project where
an occurrence happened. The published Open Dough v0.3.31
[finding registry](https://github.com/terryyin/open-dough/blob/v0.3.31/docs/maintainer/finding-names.md)
maps all 13 ODF entries retained in DearDough.md. Their groups are measurement
scope (ODF-030), CI observation/runtime (ODF-034, ODF-069, ODF-085), delegation
and proof (ODF-042, ODF-051, ODF-059, ODF-082), and workspace/claim ownership
(ODF-035, ODF-081, ODF-083, ODF-084, ODF-086). DD-097 and DD-098 concern the
published [delegation](https://github.com/terryyin/open-dough/blob/v0.3.31/src/skills/dough-execute-plan/references/delegation.md)
and [refactor delivery](https://github.com/terryyin/open-dough/blob/v0.3.31/src/skills/dough-execute-plan/references/wrap-up.md)
rules. They remain shared process feedback, not Donut product stories.

The project log was empty at review start. DD-073's frontend-proof gap was
fixed by `7b1d80b4e8`; the required typecheck still appears in the
[frontend proof rule](.agents/skills/frontend/SKILL.md#frontend-proof), so its
removed finding stays removed. DD-065's symlink CLI-entry defect belongs to
the shared runtime, and DD-074 is now ODF-085; do not revive the withdrawn
CI-startup stories as project defects. The old SEED-038 startup seed was
withdrawn in `2381cf9e60`, not completed; that number now names unrelated
release work in history.

### E2E startup reliability

| Priority | Finding | Retained frequency | Impact | Backlog story |
| --- | --- | --- | --- | --- |
| 1 | [DD-103](#dd-103) | One execution, three failed launch attempts | About 25 minutes lost; local E2E proof and profiling blocked until a manual build workaround | [Start local E2E reliably with an existing backend build](.planning/seeds/SEED-040-reliable-local-e2e-startup.md#story-1) |

DD-103 is the only supported unresolved project finding recovered by this
review. Frequency counts executions separately from retries; no second
independent project problem is established to justify a second priority story.
The second review retains first priority as a bounded delivery-reliability
repair: three consecutive launches could not produce E2E proof, the workaround
cost about 25 minutes, and the affected ordinary command validates work across
the product backlog. This is significant contributor impact, not a production
outage. The evidence does not establish failure on every run or broad frequency
across contributors. The story must reproduce the failure and stay bounded;
an inconclusive or substantially larger investigation warrants reprioritization
against note-save latency and attachment work. Unrelated backlog order is
preserved.

<a id="dd-103"></a>

## DD-103 — The E2E runner races two Gradle processes on a checkout with a populated build cache

`pnpm cy:run` starts `backend:watch` (`gradlew -t classes`) and `backend:sut:ci` (`bootRunE2E --build-cache`) in parallel. On a linked worktree whose backend had already been built, both processes rewrote `backend/build/classes/java/main`: `:compileJava` failed with "Unable to delete directory ... New files were found", then the application failed with "Could not find or load main class com.odde.donut.DonutApplication". Three consecutive launches failed the same way; pre-compiling from an emptied classes directory (`rm -rf backend/build/classes/java/main && gradlew classes --rerun-tasks`) before the launch avoided it.

### Current evidence and prior correction

Recovered unchanged from `b0b385a184:DonutRetrospectiveFindings.md`.
`a2660d71f6` deleted the finding while closing SEED-034#story-3; that commit
changed planning/findings only and supplied no runner fix. Story closure is
not evidence this defect was resolved.

The earlier correction `8b9fc7d30a` (2026-09-12) avoids competing Gradle builds
only for the built CI target. The September 22 local occurrence is later
evidence of the same failure mechanism on an uncovered target, not proof that
the built-target fix regressed. At review revision `9c4c712f70`,
[sutServiceArgs](scripts/sut-services.mjs) still selects `backend:sut` for
non-built targets, and [package.json](package.json) runs watch and bootRun in
parallel. The built-target assertions in
[e2e-runner.test.mjs](scripts/e2e-runner.test.mjs) explicitly explain why that
target omits watch. This is source confirmation of the surviving mechanism,
not a new runtime reproduction or another countable occurrence.

No queued/taken story covers this startup failure. SEED-039's test-cost and
shard-balancing candidates concern speed, not reliable startup. The linked
SEED-040 story owns the remaining local-launch outcome; the historical
SEED-034 note-save story remains closed.

The second ownership review confirms that both the launch selection and the
parallel Gradle commands live in Donut-owned files, outside the installed
Open Dough payload. Fixing this requires no Open Dough skill change. The
September 12 CI-only correction is also direct evidence that the repository
owns this compilation responsibility.

### Occurrences

- Execution: quick/010-change-proportional-note-save / 91bf241404 (slice 9 measurement)
  - Timestamp: 2026-09-22T06:02:00Z, 2026-09-22T06:09:00Z, 2026-09-22T06:17:00Z (+08:00 local 14:02, 14:09, 14:17)
  - Tool: Claude Code
  - Model: claude-fable-5-1
  - Open Dough release: modified; revision 2ef78c3990; base 0.3.27
  - Evidence: job scratch logs `baseline-run-attempt2-gradle-race.log`, `baseline-run-attempt3-gradle-race.log`, `baseline-run-attempt4-missing-classes.log`; `scripts/e2e-runner.test.mjs` around line 2114 notes that only the built target avoids the double-Gradle launch.
  - Observed effect: about 25 minutes lost before the baseline measurement could run; no product impact.
  - Inference: repository tooling defect, not a process finding; a backlog or tooling fix (build once, then watch, or let the SUT reuse the watcher's classes) would remove the workaround. Numbered from the shared DD sequence in DearDough.md to keep one sequence across both logs.
