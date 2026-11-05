---
id: SEED-103
status: active
planted: 2026-11-05
planted_during: CI proof summary preparation
trigger_when: A selected Cypress feature file contains a Gherkin syntax error
scope: story
---

# SEED-103: Report Gherkin syntax errors in Cypress CI

<a id="report-gherkin-syntax-errors"></a>

### Report Gherkin syntax errors in Cypress CI

**Identity:** SEED-103#report-gherkin-syntax-errors
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/002-report-gherkin-syntax-errors/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"a124a58a8476cbf7b3b1264be6bbf2a3384b73c3b04ba2ecd37a594801d14495","plan":"c22d6b6019922ab6407b5fd2c60d116b574f512a1b4d173a9244f573dfd1809b"}}
```

**Goal:** A Donut developer whose checkout has a `.feature` file that Cypress
cannot turn into scenarios learns which file it is and why, because the
Cypress run fails instead of passing without it. CI then shows the run as
failed, so a broken feature file cannot reach `main` looking green.

**Observed:**

- The CI proof plan's Learnings (`ee65a0ba22`, 2026-11-05) record a scratch
  run with an `Examples` block appended to
  `e2e_test/features/testability/cypress_retry.feature`. Its exact text was
  not recorded. Cypress ran only the other selected spec and passed.
- Where the file disappears (`@badeball/cypress-cucumber-preprocessor`
  28.0.1, `dist/add-cucumber-preprocessor-plugin.js` and
  `dist/helpers/specs.js`): `package.json` sets `filterSpecs: true`, and
  `e2e_test/config/ci.ts` always sets a tag expression. In
  `setupNodeEvents`, the preprocessor parses every file matched by
  `config.specPattern`, which is all of `e2e_test/features`, not only the
  `--spec` selection. It keeps only the files with a scenario that matches the
  tags. A file that fails to parse, or that yields no scenarios, is dropped
  silently, the same way as an all-`@ignore` file.
- Scratch Cypress project outside the checkout, using Donut's
  `addCucumberPreprocessorWithProof`, Cypress 16.2.1 with Electron,
  2026-11-05, at `268fd645fd`. It had `filterSpecs: true` and tags
  `not @ignore`, plus four files: a valid file, a file with a stray text line
  after a step, a file with an empty `Examples:` under its `Scenario:`, and an
  `@ignore` file.
  - `cypress run` reported `1 found (valid.feature)`, ran it, and exited 0.
  - Throwing an error from `setupNodeEvents` before the preprocessor is added
    made Cypress print the error message, run no spec, and exit 1.
  - With `--spec` given, `config.specPattern` in `setupNodeEvents` is still
    the full pattern.
- The preprocessor's own Gherkin (42.0.1) gives these results:
  - A stray text line gives a parse error with line and column.
  - `Examples:` with no data rows under a `Scenario:` is valid Gherkin, but
    yields no scenarios.
  - `Examples:` directly under `Feature:` is read as description text.
- Today, all 95 feature files parse, and each yields at least one scenario.
  In CI, 4 files are dropped by tags alone, as intended: `cli_recall`,
  `cli_access_token`, `cli_gmail`, and `cli_interactive_mode` under
  `e2e_test/features/cli/`.
- `scripts/e2e-runner.mjs` exits with Cypress's exit code, and the CI E2E step
  runs it.

**Direction (agreed 2026-11-05):** Before the preprocessor is added, parse
the same files it parses. If any file fails to parse or yields no scenarios,
stop the run before any spec runs, with a message naming each such file and
why. Tag filtering stays as it is. ADR 0006 supports failing loudly with a
clearer message.

Other options considered:

- **Turn off `filterSpecs`.** The other specs would keep running, but the
  tag-only files would each start a browser with no tests.
- **Keep broken files in the spec list with our own filter.** This copies the
  library's filter logic into Donut.
- **Lint step only.** `cy:run` itself would still pass silently.
- **Fix upstream.** Donut does not control when it ships.
- **Borrowed mechanism, mail delivery.** A mail server drops spam silently but
  sends a bounce for mail it cannot read. The analogy stops at timing: the
  agreed check stops the whole run, so it does not deliver the rest.

**Scope:**

- Required: when a feature file fails to parse, the Cypress run stops before
  any spec runs and exits unsuccessfully. The message names the file and its
  Gherkin error.
- Required: when a feature file parses but yields no scenarios at all (before
  tag filtering), the run stops the same way. The message names the file and
  says that it has no scenarios.
- Required: files dropped only because of tags keep being dropped silently.
- Boundary: the check covers every feature file the preprocessor parses, so a
  broken file also stops a focused local run of another spec. This holds
  locally, on the Cloud VM, and in CI.
- Deferred: naming the broken file in the CI proof summary (SEED-102). With
  this story, the broken shard writes no report, so the summary will show
  that part as missing, not as passed.
- Deferred: a scenario that yields nothing while the rest of its file still
  runs, such as an empty `Examples:` block beside other scenarios.

**Key examples:**

1. **Parse error beside a valid file.** A feature file has a stray text line
   after a step, and the selected spec is valid → `cypress run` exits
   unsuccessfully without running any spec. The output names that file and
   the Gherkin error with its line.
2. **No scenarios.** A feature file whose only `Scenario:` has an empty
   `Examples:` block → the run stops the same way. The output names the file
   as having no scenarios.
3. **Tag exclusion still works.** All files parse, and some are all-`@ignore`
   → the run proceeds as today. The ignored files are skipped without a
   message.
4. **CI.** A push containing example 1's file → each E2E shard's runner exits
   non-zero and its job is red. The log shows the same message.
