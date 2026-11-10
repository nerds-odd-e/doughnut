# Publish explicit CI test counts

**Identity:** SEED-112#publish-explicit-ci-test-counts
**Source:** [See Doughnut test cases and test files in Intent](../../seeds/SEED-112-publish-explicit-ci-test-counts.md#publish-explicit-ci-test-counts)

## Goal and scope

Doughnut contributors see CI proof in the units the runners report: individual
cases and aggregate file outcomes, each retaining its own outcomes, file
breakdown, recorded execution times, untimed results, and missing parts.
Use the existing producer jobs, converter, composite actions and published
`ci-proof-summary-attempt-<n>` artifact. Keep its attempt/revision identity and
retention. Load failures before any cases are file outcomes, as decided under
the developer's 2026-11-10 delegation. A successful case report does not imply
an additional successful-file result.

Excluded: new proved suites, application changes, Intent UI or reader changes,
and extracting cases from Bash file reports. The installed Open Dough 0.3.94
already meets the source's minimum version; no guidance update is needed.

## Existing solution and decisions

- PFE: `.github/workflows/ci.yml`, `.github/lint_and_generated_types`,
  `.github/ci_proof_fragments`, `.github/ci_proof_summary`, and
  `scripts/ci/ci-proof-outcomes.mjs` already own the complete publishing path.
  Reuse them. `node-test-report.mjs` owns interpretation of Node runner events.
  Change those owners, rather than adding a second collector or reporter.
- Copy all seven collector modules unchanged from the installed
  `dough-test-optimization/scripts/` under its CI proof recipe: summary,
  options, contract, evidence, figures, fragments and collect. Extend the
  existing Biome exclusion to cover options and evidence too.
- Each fragment invocation has one declared `case` or `file` unit. Partition
  the same runner report by that unit in the existing converter. Case and
  file invocations must upload distinct artifact identities even when their
  job and part numbers coincide. Keep fresh staging for each invocation.
- Node reports preserve whether an event is a case or a file-load failure.
  Vitest reports already distinguish failed files with no assertions. Keep
  classification in the report interpreter, with one conversion path shared
  by both units; do not infer successful files by grouping cases.
- A valid report with zero results in a declared unit produces a zero-result
  fragment. A missing report leaves that part missing in each declared unit.
  The collector's existing rules own missing/ambiguous parts and identity.
- Expected units and independently numbered parts:

  | Entry | Unit | Parts and producers |
  | --- | --- | --- |
  | `e2e_test/features` | case | 6 matrix parts |
  | `frontend/tests` | case | 2 shards |
  | `frontend/tests` | file | the same 2 shards |
  | `backend/src/test/java` | case | 1 part |
  | `scripts` | case | 12 Node commands, in `lint:all` order |
  | `scripts` | file | the same Node parts 1–12, Bash part 13 |

- Preserve Node leaf/skip/todo outcomes, parent failures for their own reason,
  selection of the running file, exclusion of nested runner invocations, and
  normal console output and exit status. Preserve Cucumber's final-attempt
  rule and JUnit's outer-file attribution.
- [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md#usage): let
  conversion errors fail loudly. [ADR 0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
  keeps observations disposable and worktree-owned. No application services
  or persistent data are needed. The inspected North Star has no direction
  requiring a new architectural choice for this publisher adoption.

## Outside-in proof

`scripts/ci/ci-proof-outcomes-fixtures.mjs` runs the actual Bash conversion
and collection steps read from the composite actions. Extend that boundary
and the workflow tests; use real runner reports and real collector modules.

| Promise | Proof owner and signal |
| --- | --- |
| Mixed Scripts and Infrastructure units | Slice 2: a Node report with two cases plus a Bash file yields distinct case/file evidence, independent times and file breakdowns, valid under `summaryProblems`. |
| File-load failure remains visible | Slices 2 and 3: real Node import failure and a Vitest failed-file report each yield one failed file with available timing; accompanying real cases keep their case outcomes. |
| Empty and missing streams differ | Slices 2 and 3: an existing case-only report has a present zero-count file part; a missing report has missing parts in its declared units, while other parts remain present. |
| Other suites keep their actual case results | Slice 3: existing JUnit, Vitest and Cucumber action-level fixtures retain outcomes, file attribution, timing and untimed checks in case evidence. |
| Workflow wiring matches the reports | Slices 2 and 3: workflow tests match declarations by `(entry, unit)`, count each producer/matrix part once, check unit-specific artifact naming, failure conditions, `needs` and `fail-fast: false`. |
| Released reader can consume the published attempt | Slice 3 and execution CI finish: validate the final summary with the released contract and inspect the matching CI artifact's repository, revision, run and attempt; its evidence units and part counts match the table. |

Legacy-build comparison remains the released Intent reader's responsibility;
do not recreate that reader in this project. The published artifact is this
story's integration boundary with it.

## Decisive premises and retained observations

All observations below are from this owned checkout at
`5053776ea9e5e2ff4255f029630863561c2d725a` on 2026-11-10. Scratch collector
copies and temporary fixtures were restored or removed before seed writes.
The fetched `17f4ee9024bafd3c7a695d7d0e09f956670733d3` changes only SEED-119
and its design note, so these observations still cover the affected paths.

| Premise / consuming operation | Observation and result | Remainder |
| --- | --- | --- |
| The existing local proof route executes the action shells and report interpreters | `CURSOR_DEV=true nix develop -c pnpm test:ci-proof`: 25/25 pass, including Bash/Node mixed publication, missing reports, JUnit, Vitest and Cucumber. | Extend the assertions for the promised units during execution. |
| The released seven-module collector works when copied into the project's CI directory | Retained scratch `CURSOR_DEV=true nix develop -c python3 -` copied the installed 0.3.94 modules, then ran `pnpm test:ci-proof`: 22/25 pass. Three failures are summary-shape consumers in Bash and mixed Node publication tests; conversion/timing and workflow tests pass. | Slice 1 updates those assertions to the nested evidence schema. |
| One entry can publish independent units with missing parts and retained time | In that same copied-project profile, `node scripts/ci/ci-proof-summary.mjs fragment --outcomes <report> --times <times> --by-file --entry scripts --part 1 --attempt 1 --revision deadbeef --out <fragments>` ran twice: `--unit case --of 2` and `--unit file --of 1`. Then `summarize --expect scripts=2:case --expect scripts=1:file --repository nerds-odd-e/doughnut --run-id 42 --attempt 1 --revision deadbeef` produced schema 2 with case passed=1/failed=1/time=30 ms, missing case part 2, and file passed=1/time=500 ms. | None for collector support; project producer wiring is planned work. |
| Node default test isolation distinguishes a load failure from ordinary case results | Disposable fixtures ran `node --test --test-reporter=<event reporter> <broken.test.mjs> <cases.test.mjs>` through `CURSOR_DEV=true nix develop -c python3 -`. This uses the wrapper's default isolation and interpreter; the observation reporter only serializes its events. The import failure emitted a file-named `test:fail`, no `entryFile`, and a process `exitCode`; passing/failing case events carried `entryFile` and case names. Exit status was 1. | Slice 2 proves classification through the product wrapper, including its existing nested/imported cases. |

## Ordered slices

### 1. Adopt the released proof contract

Type: Structure
Status: planned
Proof: `CURSOR_DEV=true nix develop -c pnpm test:ci-proof` stays green with
the same existing outcomes, timings, missing parts and file breakdowns in
the released summary shape.

Copy the seven modules and extend their existing Biome exclusion. Adapt the
three summary consumers identified above. Keep producer behavior unchanged;
unit-less producers remain readable through the released legacy evidence
until the next Behavior enables explicit units. This structure directly
enables slice 2. Update the copied-collector documentation to the actual
release and seven-module set.

Sizing: 5–7 minutes including focused proof and cleanup. The work is a
byte-for-byte copy and three bounded assertion adaptations; no new parser.

### 2. Publish Scripts and Infrastructure cases and files

Type: Behavior
Status: planned
Proof: extend the action-level mixed publication fixture and real Node
runner fixtures, plus workflow assertions; run
`CURSOR_DEV=true nix develop -c pnpm test:ci-proof`.

Behavior: given the existing Node command reports and Bash file report,
publishing the attempt yields `scripts=12:case scripts=13:file` with the
part mapping above, separate timing/breakdowns, and file-load failures in
file evidence. A valid Node report with no file failures publishes a present
zero-count file fragment; a missing Node report leaves its declared parts
missing.

Add the action's unit input and pass it to the collector. Extend the converter
and Node reporter to carry/select the actual unit. Wire both streams for
Node and the file stream for Bash; include the unit in fragment artifact
names. Keep each unit's part declaration explicit and update the unit-aware
workflow checks. Move the mixed-action publication scenario out of the
240-line Node runner test file into a cohesive unit-publication test file
and register it in `test:ci-proof`; preserve its existing proof. Update the
scripts section of `docs/ci-proof-summary.md` with this behavior.

Sizing: 7–9 minutes. Scrutinized above the five-minute target: one scripts
publication outcome and one focused proof loop; collector migration is
already isolated in slice 1. The Node event distinction has been observed.
Other suites temporarily retain legacy evidence; slice 3 replaces it.

### 3. Publish the other suites in their reported units

Type: Behavior
Status: planned
Proof: extend the existing report-to-action fixtures and workflow tests,
run `CURSOR_DEV=true nix develop -c pnpm test:ci-proof`, then retain the
matching final CI artifact observation under execution's normal CI ownership.

Behavior: given JUnit, Vitest and Cucumber reports, their published proof
contains the actual case results in the declared case parts. A Vitest file
that failed before cases is one file failure with available timing, in its
shard's separate file stream. Empty and missing file streams retain the
meaning established in slice 2.

Wire all remaining explicit units and expectations in location order. Extend
Vitest interpretation using its reported failed-file/no-assertions shape;
preserve case timings, Cucumber final-attempt status and JUnit attribution.
Finish `docs/ci-proof-summary.md` with the full mapping, empty/missing parts,
unit-specific artifact identities, and recorded-time meaning. Validate a
final collected fixture using `summaryProblems`, then check the hosted
artifact from the exact executed revision and attempt when its CI finishes.

Sizing: 5–8 minutes for edits, focused proof and cleanup; hosted CI wait is
an external-wait exception. One case-publication rule covers these suites;
the Vitest file stream uses the same unit partition as slice 2.

## Delivery and readiness review

- All expected edited files are currently at most 250 lines. The closest
  are `biome.json` (248), the Node runner test file (240), and `ci.yml` (235).
  Keep the Biome exclusion on its existing line; split tests along runner
  versus publication responsibilities as above. If growth requires a seam
  elsewhere, keep each file cohesive under the existing refactor rule.
- At each slice boundary follow AGENTS.md: Jidoka, fresh
  dough-post-change-refactor agent, API generation only if needed,
  coordinator `./scripts/run.sh pnpm format:changed` once, plan update,
  agent commit with the independent check-only hook, then push. This plan
  changes no API. Use the consumer trace and run affected consumers under
  the existing executable-proof procedure. Broad application suites are
  not local gates for report/adaptor-only changes.
- Stop and refine an attempt at the ten-minute hard limit unless the named
  focused verification or external-wait exception applies; preserve learned
  evidence. Do not hide overruns by renaming a slice.
- Cumulative design: one runner interpretation, one unit selector, one
  released collector. Case and file streams have distinct meanings, rather
  than separate special-case collectors for individual suites.
- Slice-plan refinement was not needed: the structure immediately enables
  the two bounded publication behaviors, with mapped proof and explicit
  interim legacy evidence replaced by slice 3.
- This review identified no remaining slice-specific concern or pre-Take
  human decision. Record the assessment through the backlog recorder;
  this preparation supplies no execution authority.
