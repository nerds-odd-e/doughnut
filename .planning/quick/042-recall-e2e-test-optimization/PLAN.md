# Recall E2E test optimization

**Status:** done

**Shipped:** Cut `e2e_test/features/recall/` from 59 scenarios to 12 (11 tag-eligible + 1 `@skipOptimizationDueToKnownNecessarySlowness`), plus one scenario moved to `e2e_test/features/assimilation/assimilation_page_types.feature` (assimilation-page-types, not recall). Suite wall time dropped from ~5:39 to 1:16 (~78%). Removed redundant/overlapping E2E scenarios (accidental-match reveal chrome, FSRS-number pinning, daily-probe variants, property-tracker assimilation-queue scenarios, dead recall-stats/frequent-failure-warning coverage) whose business value was already covered by backend/frontend unit and controller tests per ADR 0003. Added two small tests that were genuinely missing: a frontend unit test that an empty `dailyProbe` series hides the Recall Stats trend, and a backend controller test that a property-level sequence skip does not create a memory tracker. Removed the large amount of now-dead E2E step definitions, page objects, and mock-service files this left behind.

**Commits:** 6 slices, one commit each, on `main` (`5b198b4d99`, `faf7fb2cd6`, `e16d32d82a`, `5c5318c5b5`, `c73c41a546`, `dddbec79ed`).

**Note:** No blacklist candidates emerged — all remaining scenarios run in 5.7s–9.5s. `.planning/test-optimization-blacklist.md` untouched.
