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
| C09 | Keep Vitest JSON as the input to the existing project conversion. | slice 2 |  |
| C10 | Keep Cucumber messages as the input to the existing project conversion. | slice 2 |  |
| C11 | Keep project conversion feeding timed proof fragments. | slice 2 |  |
| C12 | Keep the collector summary consuming those fragments. | slice 2 |  |
| C13 | Keep `e2e_test/features` as a six-part entry. | slice 2 |  |
| C14 | Keep `frontend/tests` as a two-part entry. | slice 2 |  |
| C15 | Keep the entries in `intent.locations` order. | slice 2 |  |
| C16 | Refresh the project's copied CI proof collector modules from the installed CI proof recipe. | slice 1 | observed: Five scripts/ci collector modules compared to the installed recipe sources with cmp. |
| C17 | Copy those modules unchanged, preserving the recipe's collector behavior. | slice 1 | observed: All five cmp comparisons returned equal. Biome files.includes excludes each verbatim copy. |
| C18 | The refreshed `fragment` accepts `--times`. | slice 1 | observed: Timed fragment CLI with outcomes.tsv and times.tsv produced fragment durationMs 500 in the slice 1 fixture. |
| C19 | The refreshed `fragment` accepts `--by-file`. | slice 1 | observed: The same fragment CLI with --by-file emitted one file with one pass, one fail and 500 ms. |
| C20 | The refreshed `summarize` retains `durationMs`. | slice 1 | observed: The timed summarize CLI fixture retained 500 ms in the present part and entry. |
| C21 | The refreshed `summarize` retains the file breakdown. | slice 1 | observed: The timed summary retained the file figures and marked the present part breakdown true. |
| C22 | The summary schema version stays `1`. | slice 1 | observed: The slice 1 fixture asserted schemaVersion 1 in timed and counts-only fragments and summaries. |
| C23 | `.github/ci_proof_fragments` passes `--times` when writing each part's fragment. | slice 2 |  |
| C24 | `.github/ci_proof_fragments` passes `--by-file` when writing each part's fragment. | slice 2 |  |
| C25 | Extend `scripts/ci/ci-proof-outcomes.mjs` to emit timing lines alongside converted outcomes. | slice 2 |  |
| C26 | Extend the converter's tests for the timing behavior. | slice 2 |  |
| C27 | Each timing line is `<seconds><TAB><repo-relative path>`. | slice 2 |  |
| C28 | Timing lines use the same labels as the existing outcomes. | slice 2 |  |
| C29 | Vitest emits one timing line per assertion that records `duration`. | slice 2 |  |
| C30 | Vitest assertion duration is converted from report milliseconds to seconds for `--times`. | slice 2 |  |
| C31 | A Vitest file that fails to load and has no assertions produces one failed outcome for that path. | slice 2 |  |
| C32 | That Vitest load failure emits no timing line. | slice 2 |  |
| C33 | Its fragment file breakdown marks the check untimed. | slice 2 |  |
| C34 | Cucumber emits one timing line per scenario with recorded timing on its final attempt. | slice 2 |  |
| C35 | Cucumber timing sums that final attempt's recorded step `testStepResult.duration` values. | slice 2 |  |
| C36 | Cucumber status rules stay as they are today. | slice 2 |  |
| C37 | A missing shard remains visible as missing. | slice 2 |  |
| C38 | A missing shard is excluded from counts. | slice 2 |  |
| C39 | Present shards still contribute counts when another shard is missing. | slice 2 |  |
| C40 | Present shards still contribute their available `durationMs` when another shard is missing. | slice 2 |  |
| C41 | Present shards still contribute their file breakdown when another shard is missing. | slice 2 |  |
| C42 | Failed checks still count as failed. | slice 2 |  |
| C43 | Recorded times from failed checks contribute to entry `durationMs`. | slice 2 |  |
| C44 | Recorded times from failed checks contribute to file `durationMs`. | slice 2 |  |
| C45 | A broken conversion fails its producer step. | slice 2 |  |
| C46 | `docs/ci-proof-summary.md` describes the timing behavior CI publishes. | slice 2 |  |
| C47 | `docs/ci-proof-summary.md` describes the file breakdown CI publishes. | slice 2 |  |
| R01 | Publish only durations recorded by the runner. | slice 2 |  |
| R02 | Checks with no recorded duration stay untimed. |  | G1 |
| R03 | Untimed checks still count. | slice 2 |  |
| R04 | Published labels contain no machine-absolute paths. | slice 2 |  |
| R05 | Published labels stay repository-relative inside their entry. | slice 2 |  |
| J01 | Both Vitest shards report assertions with durations, including a failed test. After collector upload, runner reports, summary and Intent agree for the entry and example file. | slice 3 |  |
| J02 | All six Cucumber shards report final-attempt durations, including a failed scenario. After collector upload, runner messages, summary and Intent agree for the entry and example feature. | slice 3 |  |
| J03 | Summarize five of six Cucumber fragments. Show the gap and include the present counts, durations and breakdown. | slice 2 |  |
| J04 | Fragment and summarize a Vitest or Cucumber part with failed timed checks. Keep their failures and their entry and file times. | slice 2 |  |
| J05 | Convert and fragment a Vitest file with failed status and no assertions. Keep one failed check, emit no timing line and mark it untimed. | slice 2 |  |
| X01 | Proof entries for `scripts` tests. | slice 2 |  |
| X02 | Timings for `scripts` tests. | slice 2 |  |
| X03 | Proof entries for `infra` tests. | slice 2 |  |
| X04 | Timings for `infra` tests. | slice 2 |  |
| X05 | Proof entries for backend tests. | slice 2 |  |
| X06 | Timings for backend tests. | slice 2 |  |
| X07 | Wall-clock job time. | slice 2 |  |
| X08 | Setup time. | slice 2 |  |
| X09 | Browser-boot time. | slice 2 |  |
| X10 | Shard-queue time. | slice 2 |  |
| X11 | Changes to Intent/dashboard product code. | slice 2 |  |
| X12 | Publishing timings through a CI adapter path. | slice 2 |  |
