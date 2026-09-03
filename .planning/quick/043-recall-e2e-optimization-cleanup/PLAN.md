# Recall E2E optimization cleanup

**Status:** done

**Shipped:** Follow-up cleanup on `.planning/quick/042-recall-e2e-test-optimization/` (post-hoc review found no bugs, only leftover dead code and one readability gap). Removed 3 E2E step definitions in `recall.ts` orphaned by plan-042 slices 2 and 3, removed the dead `I should see {int} due for assimilation` step and its single-caller `expectCount()` page-object method (orphaned by plan-042 slice 6), and added an explanatory comment to `property_memory_tracker.feature` documenting why its backdating step is necessary (not a bug) for the due-count assertion to stay unambiguous.

**Commits:** 3 slices, one commit each, on `main` (`cfe42f1c97`, `41e51d801a`, `7e0deccd92`).
