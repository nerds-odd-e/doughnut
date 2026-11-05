# Source review for SEED-103#report-gherkin-syntax-errors

On 2026-11-05, the coordinator launched `/root/source_review` with no
conversation inheritance and supplied only the selected story's Goal,
agreed Direction, Scope and Key examples. No human amendments applied. The
reviewer was asked to read no files and derive commitments without assigning
ownership or judging implementation. The returned commitments follow.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| C01 | Parse the same set of feature files the Cypress preprocessor parses. | slice 1 | observed: validateCucumberFeatures uses the locked getSpecs pattern, projectRoot, exclusions and feature extension; focused Cypress fixture observes both invalid files outside --spec, and the real corpus scan covered 95 files. |
| C02 | Perform that check before adding the preprocessor. | slice 1 | observed: cucumberProof.mjs calls validateCucumberFeatures before recorder creation or adding the preprocessor. |
| C03 | A feature file that fails Gherkin parsing triggers the run failure. | slice 1 | observed: The focused Cypress regression's stray-text feature after a valid step reports a parse error and exits 1. |
| C04 | A feature file that parses and yields zero scenarios before tag filtering also triggers the run failure. | slice 1 | observed: The focused Cypress regression's Scenario with empty Examples reports no scenarios; validation tests for pickles before any tag expression. |
| C05 | Either trigger stops the run before any spec runs. | slice 1 | observed: The focused Cypress regression requires exit 1 without a Running line despite a runnable selected feature. |
| C06 | Either trigger makes the Cypress runner exit unsuccessfully. | slice 1 | observed: The focused Cypress regression asserts the Cypress process exit code is 1 for the invalid project. |
| C07 | The failure output names every feature file that meets either trigger. | slice 1 | observed: The focused Cypress regression requires both project-relative paths from the same failed startup; validateCucumberFeatures accumulates the failures for all matching specs. |
| C08 | For each file that fails to parse, the output gives its Gherkin error. | slice 1 | observed: The focused Cypress regression checks features/broken.feature, Gherkin position (4:5) and stray text. |
| C09 | For each file that parses and yields zero scenarios, the output says it has no scenarios. | slice 1 | observed: The focused Cypress regression checks features/empty.feature and no scenarios. |
| C10 | Tag filtering retains its current behavior. | slice 1 | observed: The fixture's unrestricted follow-up with filterSpecs and @selected and not @ignore exits 0 and runs only normal.feature; the existing tagged Cypress proof cases also passed. |
| C11 | Files excluded solely by tag filtering continue to be skipped silently. | slice 1 | observed: The fixture's follow-up requires ignored.feature to be silent even though it has @selected as well as @ignore. |
| C12 | A broken feature file also stops a focused local run that selects a different, valid spec. | slice 1 | observed: The failed run supplies only features/normal.feature via --spec while both different invalid features are diagnosed before execution. |
| C13 | The same check and failure behavior applies to Cloud VM runs. | slice 1 | observed: AGENTS.md Cloud VM cy:run uses the same e2e-runner and ci.ts common configuration, where addCucumberPreprocessorWithProof invokes the proven startup check independently of host setup. |
| C14 | The same check and failure behavior applies to CI runs. | slice 1 | observed: The CI workflow's matrix step runs scripts/e2e-runner.mjs with --spec and the same ci.ts common setup; the focused Cypress regression exercises the shared adapter. |
| C15 | In the CI journey, each E2E shard’s runner exits non-zero. | slice 1 | observed: scripts/e2e-runner-batch-failure.cases.mjs tests preservation of a nonzero Cypress result. scripts/e2e-runner.mjs exits with that result on every matrix shard. |
| C16 | Each affected E2E job is shown as failed, so the broken feature file cannot appear green on the way to `main`. | slice 1 | observed: The CI workflow runs the wrapper directly without failure suppression; its result is the Cypress result as established by the existing nonzero test and the invalid-feature fixture. |
| C17 | The CI log shows the same failure message as the local journey. | slice 1 | observed: The invalid-feature fixture observes the diagnostic in actual Cypress output; runE2eBatch inherits Cypress stdio in the CI workflow, retaining the same message. |
| C18 | A broken shard writes no report. | slice 1 | observed: The failed Cypress fixture enables NDJSON reporting and confirms existing.ndjson was never written. |
| C19 | The CI proof summary shows the broken shard’s portion as missing. |  | G1 |

## Examples and qualifications

- Example 1 selects a valid spec beside a feature with stray text after a
  step. The failure names the broken file and gives the parser error and line.
- Example 2 has a feature whose only `Scenario:` has an empty `Examples:`
  block. The general trigger is zero scenarios before tag filtering.
- Example 3 keeps an all-`@ignore` file silently skipped and the valid spec
  running.
- Example 4 carries the parse error into every CI E2E shard, with the same
  diagnostic and a failed job.
- Naming the file in the CI proof summary belongs to SEED-102. Diagnosing a
  scenario that yields nothing inside a runnable file is also deferred.
