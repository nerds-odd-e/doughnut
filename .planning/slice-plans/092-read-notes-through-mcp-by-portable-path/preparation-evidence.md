# Preparation evidence

## Decisive premises and preparation observations

Observed on 2026-11-12 in the named preparation worktree at
`b481551f17d71f637213f7c7d706b282889eefcc`, using its isolated test database
`doughnut_wt_0f536327332d443d8e6724b873e6c22a_test`. Each scratch edit was restored
byte-for-byte before planning writes. Production Git diff is empty.

| Premise/consumer | Consuming operation and observed form/profile | Result and limit |
| --- | --- | --- |
| Backend proof route can execute current behavior | `CURSOR_DEV=true nix develop -c pnpm backend:test_only`, original source, isolated `test` profile | Green: 3,118 tests, zero failures/errors, two skipped; Gradle 1m38s. This is the retained unchanged baseline, not future path proof. |
| Address changes reach real MCP and hosted callers; shared matching has discriminating proof | Same full-suite command with `McpToolArguments` lookup temporarily changed from `noteId` to `portablePath`, title matching temporarily forced to alias fallback, and formatter title temporarily set to `preparation-probe` | 154 expected failures among 3,118 tests. Read/graph endpoint, hosted access/renewal and title-precedence/property tests failed at their real results. Combined mutations do not isolate formatter coverage. |
| Shared graph strings have maintained consumers | Same full-suite command with only both `FocusContextWikiUri` title expressions changed to `preparation-probe` | Three failures: outgoing depth/path, depth-two traversal, and seeded sibling-selection tests. Restore matched the earlier green source exactly; reuse that baseline. This does not prove all generated addresses are navigable: 6 adds round trips. |
| Both named E2E specs are admitted in this checkout | Nix Node module executed `selectedCypressSpecs` with the exact two-spec argument in [proof routes](proof-routes.md), then `assertSupportedIsolatedCypressSpecs` | Both selected; neither requires private OpenAI/Wikidata mocks. This observes runner selection only, stopping before starting/resetting an E2E stack. Actual journeys are owned by execution, not claimed run here. |

Local host decision: macOS load averages 3.97/6.86/10.97, reclaimable inactive
memory available; unrelated processes were left alone. Run one owned backend
invocation at a time and never alongside Cypress in the same checkout. Before
another expensive launch, refresh machine/load evidence if contention changes.
The measured 1m38s–1m57s full-suite wait is an explicit focused-test timing
exception; it does not excuse oversized active edits. No decisive current-state
premise remains unobserved; new capability assertions remain implementation work.
