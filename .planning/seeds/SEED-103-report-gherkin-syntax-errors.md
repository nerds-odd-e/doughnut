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
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal:** A Donut developer can see which selected `.feature` file failed to
parse. The Cypress run fails, CI shows the failure, and the CI proof summary
reports the broken file.

**Observed:** The CI proof plan's Learnings, recorded on 2026-11-05 in
`ee65a0ba22`, describe a scratch run with an `Examples` block appended to
`e2e_test/features/testability/cypress_retry.feature` without a scenario
outline. The scratch edit was reverted. The bug report states that Cypress
ran only the other selected spec and passed. The Cucumber messages report
contained no entry for the malformed file, leaving the summary without a
failure to report.

**Acceptance example:** Select two feature files, one valid and one with the
same Gherkin syntax error. Even if the valid spec passes, the Cypress run
exits unsuccessfully, the CI job fails with a diagnostic naming the malformed
file, and the CI proof summary records a failure for that file.

**Open question:** Where does the parse failure disappear between Cypress
spec selection and Cucumber reporting?
