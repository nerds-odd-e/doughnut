# Test optimization blacklist

**Candidates** are proposals from optimization runs (hard-to-improve after a
serious attempt). Permanent exclusion from profiling is done by tagging the
Scenario or Feature with `@skipOptimizationDueToKnownNecessarySlowness` — that
is a developer decision after review, not an automatic move from this list.

Profile E2E with:

```bash
--env tags='not @ignore and not @skipOptimizationDueToKnownNecessarySlowness'
```

## Candidates

<!-- file path — test/scenario name — duration — why hard — proposed YYYY-MM-DD -->

_(none)_
