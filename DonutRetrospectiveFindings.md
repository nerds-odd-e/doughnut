# Donut Retrospective Findings

Findings specific to Donut’s product, repository tooling, or local conventions.
Findings about shared OpenDough skills, supporting guidelines, and skill scripts
remain in [DearDough.md](DearDough.md), including those observed in this project.
Existing finding identifiers and occurrence evidence are preserved.

No retained project-specific findings after the 2026-09-21 ownership review.
DD-065 and DD-074 are recorded in [DearDough.md](DearDough.md) as shared
Open Dough observer/runtime integration findings.

## DD-103 — The E2E runner races two Gradle processes on a checkout with a populated build cache

`pnpm cy:run` starts `backend:watch` (`gradlew -t classes`) and `backend:sut:ci` (`bootRunE2E --build-cache`) in parallel. On a linked worktree whose backend had already been built, both processes rewrote `backend/build/classes/java/main`: `:compileJava` failed with "Unable to delete directory ... New files were found", then the application failed with "Could not find or load main class com.odde.donut.DonutApplication". Three consecutive launches failed the same way; pre-compiling from an emptied classes directory (`rm -rf backend/build/classes/java/main && gradlew classes --rerun-tasks`) before the launch avoided it.

### Occurrences

- Execution: quick/010-change-proportional-note-save / 91bf241404 (slice 9 measurement)
  - Timestamp: 2026-09-22T06:02:00Z, 2026-09-22T06:09:00Z, 2026-09-22T06:17:00Z (+08:00 local 14:02, 14:09, 14:17)
  - Tool: Claude Code
  - Model: claude-fable-5-1
  - Open Dough release: modified; revision 2ef78c3990; base 0.3.27
  - Evidence: job scratch logs `baseline-run-attempt2-gradle-race.log`, `baseline-run-attempt3-gradle-race.log`, `baseline-run-attempt4-missing-classes.log`; `scripts/e2e-runner.test.mjs` around line 2114 notes that only the built target avoids the double-Gradle launch.
  - Observed effect: about 25 minutes lost before the baseline measurement could run; no product impact.
  - Inference: repository tooling defect, not a process finding; a backlog or tooling fix (build once, then watch, or let the SUT reuse the watcher's classes) would remove the workaround. Numbered from the shared DD sequence in DearDough.md to keep one sequence across both logs.
