---
id: SEED-107
status: active
planted: 2026-11-08
scope: story
---

# Show backend unit-test results in Intent

<a id="publish-backend-unit-test-proof"></a>

### Show backend unit-test results in Intent

**Identity:** SEED-107#publish-backend-unit-test-proof
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Beneficiary** A Doughnut developer checking what a CI attempt proved for the backend.

**Goal** The Open Dough dashboard’s Intent CI status shows the backend unit-test
results alongside the frontend and end-to-end results for the same CI attempt.

**Scope** Register `backend/src/test/java` in `intent.locations` in
`.planning/open-dough.json`. Use the JUnit results from the existing
`Backend-unit-tests` job to publish one backend proof fragment. Include that
fragment in the same `ci-proof-summary-attempt-<n>` artifact that Intent already
reads. Follow the [CI proof recipe](../../.agents/skills/dough-test-optimization/references/ci-proof-summary.md).

**Acceptance** For a completed CI attempt, the proof artifact and Intent show
backend passed, failed, and other counts that agree with the JUnit report. A job
that reports failed tests still contributes those results. If the job produces
no test report, Intent shows the backend part as missing. The same artifact
continues to carry the frontend and end-to-end proof.
