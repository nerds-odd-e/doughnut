# Independent source review

Reviewer `/root/source_review` was launched with no inherited conversation
and received only the selected seed's beneficiary, goal, required scope,
rejection constraints, deferred scope and five key examples. Its launch input
and numbered return are in this execution conversation. Failed checks and
specific files in the journeys illustrate their promises.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| C01 | Intent shows cumulative recorded Vitest execution time as `durationMs` for the judged attempt. | slice 3 |  |
| C02 | Intent shows the Vitest per-file breakdown for that attempt. | slice 3 |  |
| C03 | Intent shows cumulative recorded Cucumber execution time as `durationMs` for the judged attempt. | slice 3 |  |
| C04 | Intent shows the Cucumber per-file breakdown for that attempt. | slice 3 |  |
| C05 | Published entry timing totals match the corresponding runner reports. | slice 3 |  |
| C06 | Published file counts and timings match the corresponding runner reports. | slice 3 |  |
| C07 | Intent entry timing totals match the downloaded `ci-proof-summary-attempt-<n>` artifact for the same attempt. | slice 3 |  |
| C08 | Intent file breakdowns match the downloaded artifact for the same attempt. | slice 3 |  |
| C09 | Keep Vitest JSON as the input to the existing project conversion. | slice 2 | observed: Vitest producer YAML still reads CI_PROOF_VITEST_REPORT. The real action test supplies its Vitest JSON and asserts its fragment. |
| C10 | Keep Cucumber messages as the input to the existing project conversion. | slice 2 | observed: Cucumber producer YAML still reads CI_PROOF_CUCUMBER_MESSAGES. The real action test supplies its NDJSON messages and asserts its fragment. |
| C11 | Keep project conversion feeding timed proof fragments. | slice 2 | observed: The two real fragment-action tests run the shell and assert written timing rows and timed fragments. |
| C12 | Keep the collector summary consuming those fragments. | slice 2 | observed: The probe.mjs real summary shell consumes the producer artifacts and returns both timed entries. |
| C13 | Keep `e2e_test/features` as a six-part entry. | slice 2 | observed: ci-workflow.test.mjs matrix proof and probe.mjs part assertions cover five present Cucumber shards and a sixth missing one. |
| C14 | Keep `frontend/tests` as a two-part entry. | slice 2 | observed: ci-workflow.test.mjs matrix proof and probe.mjs Vitest summary use both declared shards. |
| C15 | Keep the entries in `intent.locations` order. | slice 2 | observed: ci-workflow.test.mjs checks expected paths in the same order as intent.locations, and the probe asserts the summary order. |
| C16 | Refresh the project's copied CI proof collector modules from the installed CI proof recipe. | slice 1 | observed: Five scripts/ci collector modules compared to the installed recipe sources with cmp. |
| C17 | Copy those modules unchanged, preserving the recipe's collector behavior. | slice 1 | observed: All five cmp comparisons returned equal. Biome files.includes excludes each verbatim copy. |
| C18 | The refreshed `fragment` accepts `--times`. | slice 1 | observed: Timed fragment CLI with outcomes.tsv and times.tsv produced fragment durationMs 500 in the slice 1 fixture. |
| C19 | The refreshed `fragment` accepts `--by-file`. | slice 1 | observed: The same fragment CLI with --by-file emitted one file with one pass, one fail and 500 ms. |
| C20 | The refreshed `summarize` retains `durationMs`. | slice 1 | observed: The timed summarize CLI fixture retained 500 ms in the present part and entry. |
| C21 | The refreshed `summarize` retains the file breakdown. | slice 1 | observed: The timed summary retained the file figures and marked the present part breakdown true. |
| C22 | The summary schema version stays `1`. | slice 1 | observed: The slice 1 fixture asserted schemaVersion 1 in timed and counts-only fragments and summaries. |
| C23 | `.github/ci_proof_fragments` passes `--times` when writing each part's fragment. | slice 2 | observed: Actual action shell writes ci-proof-times.tsv and passes it via --times. Both producer tests assert resulting fragment durationMs. |
| C24 | `.github/ci_proof_fragments` passes `--by-file` when writing each part's fragment. | slice 2 | observed: Actual action shell passes --by-file. Both producer tests assert complete per-file fragment figures. |
| C25 | Extend `scripts/ci/ci-proof-outcomes.mjs` to emit timing lines alongside converted outcomes. | slice 2 | observed: ci-proof-outcomes.mjs CLI writes an optional times file while retaining outcome stdout, verified by the real action tests and probe. |
| C26 | Extend the converter's tests for the timing behavior. | slice 2 | observed: ci-proof-outcomes.test.mjs asserts timing through both actual runner-conversion and fragment paths. |
| C27 | Each timing line is `<seconds><TAB><repo-relative path>`. | slice 2 | observed: Both real action tests assert exact seconds TAB repository-relative label rows. |
| C28 | Timing lines use the same labels as the existing outcomes. | slice 2 | observed: Vitest/Cucumber outcomeLines tests assert existing repository labels. Real action tests assert the matching timing labels. |
| C29 | Vitest emits one timing line per assertion that records `duration`. | slice 2 | observed: Vitest real action fixture expects exactly two timing rows, including zero, and counts the third untimed assertion. |
| C30 | Vitest assertion duration is converted from report milliseconds to seconds for `--times`. | slice 2 | observed: Vitest real action fixture records 125 ms and asserts a 0.125 second row plus 125 ms in the fragment. |
| C31 | A Vitest file that fails to load and has no assertions produces one failed outcome for that path. | slice 2 | observed: Vitest load failure outcomeLines test and real action fixture assert one failed outcome for the broken file. |
| C32 | That Vitest load failure emits no timing line. | slice 2 | observed: Real Vitest action test expects only the timed assertions’ rows while the broken file still contributes one outcome. |
| C33 | Its fragment file breakdown marks the check untimed. | slice 2 | observed: Real Vitest action test asserts the broken file’s single failed, untimed check. |
| C34 | Cucumber emits one timing line per scenario with recorded timing on its final attempt. | slice 2 | observed: Cucumber real action fixture asserts one row for the final retry and none for its wholly untimed scenario. |
| C35 | Cucumber timing sums that final attempt's recorded step `testStepResult.duration` values. | slice 2 | observed: Cucumber real action fixture asserts 1.375 seconds from two final-attempt step durations, excluding the 8-second earlier attempt. |
| C36 | Cucumber status rules stay as they are today. | slice 2 | observed: Existing Cucumber outcomeLines tests cover final retries, all skipped, and undefined, ambiguous or pending steps. |
| C37 | A missing shard remains visible as missing. | slice 2 | observed: probe.mjs asserts missing status for part 6 and a corresponding summarize log. |
| C38 | A missing shard is excluded from counts. | slice 2 | observed: probe.mjs asserts 4 passed and 1 failed across the five present parts, with the sixth absent. |
| C39 | Present shards still contribute counts when another shard is missing. | slice 2 | observed: probe.mjs asserts those same present counts in the timed Cucumber summary despite the absent sixth fragment. |
| C40 | Present shards still contribute their available `durationMs` when another shard is missing. | slice 2 | observed: probe.mjs asserts 1875 ms for the Cucumber entry from five present shards. |
| C41 | Present shards still contribute their file breakdown when another shard is missing. | slice 2 | observed: probe.mjs asserts five per-file rows and the failed timed example under that partial Cucumber entry. |
| C42 | Failed checks still count as failed. | slice 2 | observed: The real Vitest and Cucumber action tests assert failed file counts alongside durations. |
| C43 | Recorded times from failed checks contribute to entry `durationMs`. | slice 2 | observed: probe.mjs includes the failed Cucumber retry in the 1875 ms summary and failed Vitest assertion in its 375 ms summary. |
| C44 | Recorded times from failed checks contribute to file `durationMs`. | slice 2 | observed: probe.mjs asserts 1375 ms for the failed Cucumber file and 125 ms in the Vitest mixed file. |
| C45 | A broken conversion fails its producer step. | slice 2 | observed: probe.mjs runs the producer shell with malformed JSON and asserts failure before it writes a fragment. |
| C46 | `docs/ci-proof-summary.md` describes the timing behavior CI publishes. | slice 2 | observed: docs/ci-proof-summary.md Recorded execution time explains assertion timing, final retries, recorded failures and untimed checks. |
| C47 | `docs/ci-proof-summary.md` describes the file breakdown CI publishes. | slice 2 | observed: docs/ci-proof-summary.md How it is made explains --by-file and the tab shows file-level counts and recorded time. |
| R01 | Publish only durations recorded by the runner. | slice 2 | observed: Real action timing fixtures assert only recorded timing rows and retain zero as measured. Cucumber omits wholly untimed scenarios. |
| R02 | Checks with no recorded duration stay untimed. |  | G1 |
| R03 | Untimed checks still count. | slice 2 | observed: Real action tests assert skipped mixed-file and load-failed untimed counts. The Cucumber fixture counts its untimed scenario. |
| R04 | Published labels contain no machine-absolute paths. | slice 2 | observed: Both real action fixtures assert exact relative file labels. The Vitest fixture supplies an absolute runner filename. |
| R05 | Published labels stay repository-relative inside their entry. | slice 2 | observed: Real action tests assert files only under each expected entry. Collector byte copy retains isPathIn validation. |
| J01 | Both Vitest shards report assertions with durations, including a failed test. After collector upload, runner reports, summary and Intent agree for the entry and example file. | slice 3 |  |
| J02 | All six Cucumber shards report final-attempt durations, including a failed scenario. After collector upload, runner messages, summary and Intent agree for the entry and example feature. | slice 3 |  |
| J03 | Summarize five of six Cucumber fragments. Show the gap and include the present counts, durations and breakdown. | slice 2 | observed: probe.mjs passes five Cucumber fragments to the real collector and asserts five counts, 1875 ms, five file rows and missing part 6. |
| J04 | Fragment and summarize a Vitest or Cucumber part with failed timed checks. Keep their failures and their entry and file times. | slice 2 | observed: Both real action timing tests and probe.mjs keep failure counts and their durations in each file and its summary entry. |
| J05 | Convert and fragment a Vitest file with failed status and no assertions. Keep one failed check, emit no timing line and mark it untimed. | slice 2 | observed: Real Vitest action fixture asserts a failed, untimed load-failed file and excludes its row from the exact times file assertion. |
| X01 | Proof entries for `scripts` tests. | slice 2 | observed: The unchanged CI workflow expectations and probe.mjs declare only the Cucumber and Vitest entries. |
| X02 | Timings for `scripts` tests. | slice 2 | observed: The unchanged CI workflow supplies reports for only the Cucumber and Vitest producers. |
| X03 | Proof entries for `infra` tests. | slice 2 | observed: The unchanged CI workflow expectations and probe.mjs declare only the Cucumber and Vitest entries. |
| X04 | Timings for `infra` tests. | slice 2 | observed: The unchanged CI workflow supplies reports for only the Cucumber and Vitest producers. |
| X05 | Proof entries for backend tests. | slice 2 | observed: The unchanged CI workflow expectations and probe.mjs declare only the Cucumber and Vitest entries. |
| X06 | Timings for backend tests. | slice 2 | observed: The unchanged backend job has no CI proof fragment producer. |
| X07 | Wall-clock job time. | slice 2 | observed: ci-proof-outcomes.mjs derives duration from assertion and final-attempt step results. |
| X08 | Setup time. | slice 2 | observed: ci-proof-outcomes.mjs reads assertion and scenario-step timings. The documented figure is test execution time. |
| X09 | Browser-boot time. | slice 2 | observed: ci-proof-outcomes.mjs reads assertion and scenario-step timings. Browser setup is outside those input fields. |
| X10 | Shard-queue time. | slice 2 | observed: ci-proof-outcomes.mjs reads assertion and scenario-step timings. The producer receives no queue timestamps. |
| X11 | Changes to Intent/dashboard product code. | slice 2 | observed: The delivery changes Doughnut CI scripts, action, tests and documentation. Open Dough source changes only the collector and its regression. |
| X12 | Publishing timings through a CI adapter path. | slice 2 | observed: The composite action and workflow upload the existing judged GitHub Actions artifact. |
