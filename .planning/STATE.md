# State

## Current

- **Active plan:** `.planning/quick/001-cancelable-followups/PLAN.md`
- **Phase:** 3 — Structure (planned) — Prune dead/redundant cancel tests
- **Next:** Execute Phase 3

## Progress

| Plan | Phase | Status |
| --- | --- | --- |
| 001-cancelable-followups | 1 Nested remove / cancelable layout | done |
| 001-cancelable-followups | 2 Overlay align-items fallback | done |
| 001-cancelable-followups | 3 Prune dead/redundant cancel tests | planned |

## Notes

Ad-hoc follow-up from progressive-cancellation review. No roadmap milestone;
quick plan only. Out of scope: empty-vs-cancel copy, NoteRefinement split,
IdentityBoundCancelButton rewrite.

**Phase 1 learning:** Nested post-remove layout reload must use thin-bar
`loadRefinementLayout({ blockUi: false })` inside `runWithBlockingApiLoading`;
template `@click` needs `() => loadRefinementLayout()` after parameterization.

**Phase 2 learning:** Dual `align-items` (`center` then `safe center`) is the
intentional CSS cascade fallback; not duplication to collapse.
