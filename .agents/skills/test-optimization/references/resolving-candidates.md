# Resolving Test-optimization Candidates

This workflow runs only in `--resolve` mode. Triage every entry under
**Candidates** in `.planning/test-optimization-blacklist.md` without profiling,
selecting a top 10%, or optimizing tests.

For each Candidate:

1. Read the test, sibling scenarios, and any referenced unit tests to identify
   its unique protection.
2. Decide whether unit tests or a mocked E2E scenario can provide the same
   coverage, behavioral protection, and external user-value clarity. Genuine
   multi-step UI/PTY journeys often cannot be replaced without losing clarity.
3. Distinguish inherent cost (real page load, render, PTY startup, or frontend
   session state) from avoidable cost (live network, redundant setup, or
   duplicated coverage).

Resolve each Candidate exactly once:

| Option | When | Action |
|--------|------|--------|
| **Tag** | The cost is inherent and no cheaper test preserves its protection and user-value clarity. | Add `@skipOptimizationDueToKnownNecessarySlowness` to the narrowest slow Scenario, Outline, or Feature. |
| **Plan** | A cheaper test can preserve the same protection and clarity. | Use **slice-planning** to add replacement/removal work to one `.planning/quick/NNN-slug/` plan. |
| **Ask** | The choice requires a product, network, or value trade-off. | Ask the developer, then apply the choice. |

Create zero or one replacement plan for the whole pass. Although tagging
normally requires developer Jidoka, explicit `--resolve` invocation authorizes
an obvious Tag decision; ask whenever it is not obvious.

Delete every tagged or planned Candidate entry rather than retaining a
"Resolved" archive. Keep the Candidates header and `_(none)_` when empty.
