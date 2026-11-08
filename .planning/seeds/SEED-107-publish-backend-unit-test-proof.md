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
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/083-publish-backend-unit-test-proof/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"d6f6bbca06f14912c4155c1ed3eadddac67b3198cf13c0258e97aa132592e9a3","plan":"2652bf04da0708ced43a96469db0ed215338a40e46da93edacd70de74ce40b75"}}
```

**Goal** A Doughnut developer checking what a CI attempt proved for the backend
sees the Open Dough dashboard’s Intent CI status show backend unit-test results
alongside the frontend and end-to-end results for the same CI attempt.

**Scope — required behavior**

- Register `backend/src/test/java` in `intent.locations` in
  `.planning/open-dough.json` so Intent treats that path as a proof entry.
- From the existing `Backend-unit-tests` job, convert that run’s JUnit results
  into one backend proof fragment (one part) and keep it for the attempt.
- Include that fragment in the same `ci-proof-summary-attempt-<n>` artifact
  Intent already reads for the attempt.
- Follow the
  [CI proof recipe](../../.agents/skills/dough-test-optimization/references/ci-proof-summary.md)
  and the project’s existing producer → collector pattern documented in
  [docs/ci-proof-summary.md](../../docs/ci-proof-summary.md).

**Scope — limits**

- `scripts` and `infra` stay without CI proof in this story.
- Do not change how frontend or end-to-end proof is produced or judged beyond
  extending the shared summary to expect the backend entry.
- No Intent UI or dashboard product change: Intent already reads the summary
  artifact for configured locations.
- Do not add backend test sharding in this story; the existing single
  `Backend-unit-tests` job is one part.

**Key examples**

1. **Passing attempt.** Pre: a completed CI attempt ran `Backend-unit-tests`
   and wrote a JUnit report whose counts match the Gradle run. Trigger: the
   attempt’s proof summary is collected and Intent shows that attempt.
   Result: the proof artifact and Intent show a backend entry with passed,
   failed, and other counts that agree with the JUnit report; frontend and
   end-to-end entries remain present as today.
2. **Failed tests still publish.** Pre: `Backend-unit-tests` finishes with
   some failed tests and still produces a JUnit report. Trigger: fragment
   write and summary collection for that attempt. Result: the backend fragment
   is present and Intent shows the failed (and other) counts from that report,
   not a missing backend part.
3. **No report → missing.** Pre: the backend job produces no test report
   (for example the test step was skipped after a setup failure). Trigger:
   summary collection for that attempt. Result: Intent shows the backend part
   as missing; frontend and end-to-end proof for parts that reported are
   unaffected.
4. **Same artifact.** Pre: frontend and end-to-end producers already publish
   fragments for the attempt. Trigger: the `ci-proof` collector runs after the
   producers end. Result: one `ci-proof-summary-attempt-<n>` artifact carries
   frontend, end-to-end, and backend proof together.
