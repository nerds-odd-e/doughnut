# Source review — SEED-107#publish-backend-unit-test-proof

**Launch:** fresh general-purpose reviewer (no plan, product, or proof inputs);
model inherit; Agent ID `fe6786c5-bbde-4505-bf86-22d29e5e48c0`.

**Input:** selected story Goal, Scope (required behavior and limits), and Key
examples from
`.planning/seeds/SEED-107-publish-backend-unit-test-proof.md#publish-backend-unit-test-proof`
only.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| 1 | A Doughnut developer checking what a CI attempt proved for the backend sees the Open Dough dashboard’s Intent CI status show backend unit-test results alongside the frontend and end-to-end results for the same CI attempt. | slice 2 | |
| 2 | Register `backend/src/test/java` in `intent.locations` in `.planning/open-dough.json` so Intent treats that path as a proof entry. | slice 1 | observed: .planning/open-dough.json intent.locations path backend/src/test/java |
| 3 | From the existing `Backend-unit-tests` job, convert that run’s JUnit results into one backend proof fragment (one part) and keep it for the attempt. | slice 1 | observed: .github/workflows/ci.yml Backend-unit-tests ci_proof_fragments format junit matrix part [1] |
| 4 | Include that fragment in the same `ci-proof-summary-attempt-<n>` artifact Intent already reads for the attempt. | slice 1 | observed: ci.yml ci-proof expect includes backend/src/test/java=1; local summarize carries three entries |
| 5.1 | Follow the CI proof recipe documented in `docs/ci-proof-summary.md`. | slice 1 | observed: docs/ci-proof-summary.md backend producer row and junit rules |
| 5.2 | Follow the project’s existing producer → collector pattern documented in `docs/ci-proof-summary.md`. | slice 1 | observed: .github/ci_proof_fragments + ci_proof_summary wiring for Backend-unit-tests |
| 6 | `scripts` and `infra` stay without CI proof in this story. | | G1 |
| 7 | Do not change how frontend or end-to-end proof is produced or judged beyond extending the shared summary to expect the backend entry. | slice 1 | observed: FE/E2E fragment steps unchanged; only ci-proof expect extended |
| 8 | No Intent UI or dashboard product change: Intent already reads the summary artifact for configured locations. | | G2 |
| 9.1 | Do not add backend test sharding in this story. | slice 1 | observed: Backend-unit-tests matrix part [1] only |
| 9.2 | The existing single `Backend-unit-tests` job is one part. | slice 1 | observed: expect backend/src/test/java=1; ci-workflow.test.mjs |
| 10.1 | Backend entry counts (passed, failed, and other) agree with the JUnit report. | slice 1 | observed: ci-proof-outcomes-junit.test.mjs status/path/dir/times assertions |
| 10.2 | Frontend and end-to-end entries remain present as today. | slice 1 | observed: local summarize with FE+E2E+backend fixtures; expect still lists e2e and frontend |
| 11.1 | Backend fragment is present (failed tests still publish). | slice 1 | observed: fragment if outcome != skipped; failure/error→failed in junit converter tests |
| 11.2 | Intent shows the failed (and other) counts from that report. | slice 2 | |
| 11.3 | Backend part is not missing when a report exists. | slice 1 | observed: local summarize with backend fragment shows backend counts not missing |
| 12.1 | Intent shows the backend part as missing when there is no report. | slice 1 | observed: local summarize without backend fragment → part 1 of 1 missing (Intent reads that summary) |
| 12.2 | Frontend and end-to-end proof for parts that reported are unaffected. | slice 1 | observed: summarize without backend still counts FE/E2E parts |
| 13 | One `ci-proof-summary-attempt-<n>` artifact carries frontend, end-to-end, and backend proof together. | slice 1 | observed: expect three entries; local summarize one artifact with all three |
