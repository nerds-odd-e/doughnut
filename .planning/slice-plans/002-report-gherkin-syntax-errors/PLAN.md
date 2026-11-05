# Report Gherkin syntax errors in Cypress CI

## Source

**Source:** [Report Gherkin syntax errors in Cypress CI](../../seeds/SEED-103-report-gherkin-syntax-errors.md#report-gherkin-syntax-errors)

**Identity:** SEED-103#report-gherkin-syntax-errors

**Source review:** [Independent review of the story](source-review.md)

## Goal and scope

A Cypress run stops before any spec runs, naming each feature file that fails
to parse or yields no scenarios. Files dropped only by tags stay silent. The
story owns the scope, its deferred promises, and the four key examples. This
plan adds no promise.

**Exclusions:** the CI proof summary (SEED-102); scenario-level loss inside an
otherwise runnable file; any change to `filterSpecs`, tags, or the CI workflow.

## Current decisions

- **Home:** a small module in `e2e_test/config/` that checks the feature files,
  called first in `addCucumberPreprocessorWithProof`
  (`e2e_test/config/cucumberProof.mjs`). Every Donut Cypress run, and the
  runner fixture tests, goes through that function. The check throws one
  `Error` listing every bad file, so Cypress prints it and exits 1.
- **Same files as the preprocessor:** glob `config.specPattern` from
  `config.projectRoot`, minus `config.excludeSpecPattern` and `node_modules`,
  the way the preprocessor's `getSpecs` does. Only `.feature` files are
  checked. Paths in the message are relative to `config.projectRoot`.
- **Same parser as the preprocessor:** `@cucumber/gherkin` `generateMessages`
  and `@cucumber/messages`, loaded through the preprocessor's own `require`, as
  `cucumberProofTestProject.mjs` already does. A `parseError` envelope gives
  the Gherkin message (with line and column). No `pickle` envelopes means "no
  scenarios". Tags are not evaluated.
- **Mode:** Story Branch Mode.
- **CI wiring:** the new test runs in CI through `lint:all`. Either include it
  in an existing `test:*` script's file list or add a `test:` script to the
  `lint:all` chain.

## Decisive premises

| Premise | Consuming operation | Observation (form, profile, revision) | Result | Remainder |
| --- | --- | --- | --- | --- |
| Today a malformed or scenario-less feature vanishes, and the run passes | Slice 1 test (discrimination) | Scratch Cypress project in `$CLAUDE_JOB_DIR/tmp` (outside the checkout) using this workspace's `addCucumberPreprocessorWithProof`, `expose` `filterSpecs: true`, `omitFiltered: true`, tags `not @ignore`; `node node_modules/cypress/bin/cypress run --project <tmp> --config-file <tmp>/cypress.config.cjs --browser electron` at `268fd645fd` (2026-11-05) | `Specs: 1 found (valid.feature)`, exit 0; broken, empty-`Examples:`, and `@ignore` files absent | none |
| An error thrown in `setupNodeEvents` before the preprocessor is added stops the run with the message and a non-zero exit | Slice 1 behavior | Same project, same command, with a scratch `throw new Error(...)` at the top of `setupNodeEvents` | Cypress printed "The error was thrown while executing your e2e.setupNodeEvents() function" and the message, ran no spec, and exited 1 | The real check is called inside `addCucumberPreprocessorWithProof`, which is in the same hook before any `on` registration, so the placement cannot change this |
| `config.specPattern` in `setupNodeEvents` is the full pattern, even with `--spec` | Slice 1 file list | Same project with `--spec "features/valid.feature,features/broken.feature"` and with a newline-separated glob list, logging `config.specPattern` | Full `features/**/*.feature` both times | none |
| The preprocessor filters exactly the `specPattern` glob | Slice 1 file list | Read `dist/helpers/specs.js` `getSpecs` and `dist/add-cucumber-preprocessor-plugin.js` (28.0.1) | `globSync(specPattern, {cwd: projectRoot, ignore: excludeSpecPattern + node_modules})`, then parse and keep the files with a pickle matching the tags | none |
| All current features pass the check, so no shard breaks on landing | Slice 1 acceptance | Node script with the preprocessor's Gherkin 42.0.1 over `find e2e_test/features -name "*.feature"` (2026-11-05) | 95 files, 0 parse errors, 0 without scenarios; 4 CLI files dropped only by CI tags | Confirmed by the story branch's CI run |
| A non-zero runner exit turns the E2E job red | Example 4 | `scripts/e2e-runner.mjs` ends with `process.exit(code)` of Cypress's exit outcome; the CI step runs it directly | Existing behavior that every failing scenario already uses | none |

## Outside-in proof

| Promise (story example) | Owner | Proof |
| --- | --- | --- |
| Example 1: parse error stops the run, naming the file and Gherkin error | Slice 1 | New `node --test` test: real `cypress run` on a temp project built like `cucumberProofRunnerFixture.mjs` (Donut adapter, `filterSpecs`, tags `not @ignore`), with valid, stray-line, empty-`Examples:`, and `@ignore` files → exit non-zero, output names both bad files (the stray-line one with its `(line:column)` message), no `Running:` line |
| Example 2: no scenarios stops the run | Slice 1 | Same run: output names the empty-`Examples:` file as having no scenarios |
| Example 3: tag exclusion unchanged | Slice 1 | Same test, second run without the two bad files → exit 0, only the valid spec runs, no message about the `@ignore` file; `pnpm test:cucumber-proof` stays green |
| Example 4: CI shard red | Slice 1 | Premise row (runner exit propagation); the story branch's green CI run shows the check passes on all 95 files |

## Slices

### 1. A Cypress run refuses feature files that cannot produce scenarios

Type: Behavior
Status: done
Proof: the new runner test (examples 1–3), with its first assertion red before the check exists; `pnpm test:cucumber-proof`; the story branch's CI run green.

Behavior: a checkout with a feature file that fails to parse or yields no
scenarios → any `cypress run` through Donut's preprocessor setup → no spec
runs, the run exits non-zero, and the output names each such file and why;
with only tag-excluded files, the run proceeds as today.

Work: the check module and its call at the top of
`addCucumberPreprocessorWithProof`; the runner test with its temp project; the
test's CI wiring in `lint:all`.

Sizing: about 5 minutes of code plus two fixture Cypress runs (about 30
seconds each).

## Execution complete

Product advice: no change. SEED-102 already owns the downstream CI proof
summary. The startup check and runner regression address this story without
adding a dependency or changing the configured tag expression.

## Concern evidence

Slice-plan refinement was not needed: there is one slice, it owns one outcome,
and its proof uses one existing harness pattern. This review found no remaining
slice-specific concern.

## Learnings

### Accepted slice proof

- Before the validator, the new Cypress runner assertion failed because the
  focused valid spec ran and Cypress exited 0 with both invalid features
  present. With the validator, `cucumberProofRunnerValidation.cases.mjs` proves
  exit 1, both relative filenames and reasons, Gherkin position `(4:5)`, no
  running spec and no report. The follow-up proves only the valid spec runs
  and the `@ignore` feature stays silent.
- `./scripts/run.sh pnpm test:cucumber-proof` passed 29 tests before the
  independent fixture extraction. The extraction preserved serial runner
  tests and moved the same process launch and transcript normalization to
  `cucumberProofRunnerFixture.mjs`. After extraction, `./scripts/run.sh node
  --test e2e_test/config/cucumberProofRunner.test.mjs` passed all four tests,
  including the final regression and ordinary, focused, tagged, dry and
  reported runner compatibility.
- The validator accepted all 95 repository features. The existing runner
  test `nonzero test result: cleans owned work and returns the nonzero result`
  passed via `./scripts/run.sh node --test --test-name-pattern='nonzero test
  result: cleans owned work and returns the nonzero result'
  scripts/e2e-runner-batch-failure.cases.mjs`. CI calls that wrapper directly
  on each shard with inherited Cypress output. The actual story-branch
  application shards remain subject to the execution completion CI gate.
- `test:cucumber-proof` already includes the runner test via its glob and
  `lint:all` already runs that command. The implementation needed no CI wiring
  edit. The fixture launcher canonicalizes the project root so Cypress's
  focused absolute spec and its own project root agree on macOS.

### Execution context

- Established start for Nao-chan (`dashboard-territory.local-doughnut`), Story
  Branch Mode. Workspace `.worktrees/report-gherkin-syntax-errors-in-cypress-ci`
  under the default checkout, branch `codex/report-gherkin-syntax-errors-in-cypress-ci`,
  remote `origin`, trunk `main`. Started from `a9e46f6cefa24893b1b9282d1c820079b6c2550c`.
  Claim and initial story branch published at `78cfbc552565f362049da91b506fa8cb2b377bbe`.
- Codex observer for `nerds-odd-e/doughnut` story branch, workflow `ci.yml`
  (`donut CI`), coordinator `nao-seed-103`, mailbox
  `/tmp/dough-ci-501/watch-LFCf2J`, PID `7147`, session `87487`, cell `g8:6`.


## Story obligations

### G1. CI proof summary belongs to SEED-102
Reported: slice 1 — "The source review identifies the CI proof-summary display as a separate consumer that does not ship with this story."
Story clause: "Deferred: naming the broken file in the CI proof summary (SEED-102)."
Disposition: excluded "Deferred: naming the broken file in the CI proof summary (SEED-102)."
Commitment: C19
