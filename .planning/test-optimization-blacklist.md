# Test optimization blacklist

**Candidates** are proposals from optimization runs (hard-to-improve after a
serious attempt). Permanent exclusion from profiling is done by tagging the
Scenario or Feature with `@skipOptimizationDueToKnownNecessarySlowness` — that
is a developer decision after review, not an automatic move from this list.

Profile E2E with:

```bash
--expose tags='not @ignore and not @skipOptimizationDueToKnownNecessarySlowness'
```

## Candidates

<!-- file path — test/scenario name — duration — why hard — proposed YYYY-MM-DD -->

- `frontend/tests/pages/BookReadingPage.snap.budgets.spec.ts` — marking READ clears snap reminder: block no longer snaps when re-visited — ~17.6ms baseline — full PDF mount + snap budget lifecycle + mark-as-read; no cheaper remount preserves behavior — 2026-08-20
- `frontend/tests/pages/BookReadingPage.snap.budgets.spec.ts` — different unread blocks get independent snap budgets — dual-block PDF geometry with multi-crossing budgets; inherent viewer cost — 2026-08-20
- `frontend/tests/pages/BookReadingPage.snap.spec.ts` — remaining cross-page / disposition / animating / no-bbox snap cases — each needs a distinct PDF mount scenario; further merge would lose unique geometry coverage — 2026-08-20
- `frontend/tests/pages/RecallPage.spelling.spec.ts` — focuses the spelling answer input when resuming recall — ~14.8ms baseline — already fake-timers + RAF; remaining cost is recall page mount — 2026-08-20
- `frontend/tests/pages/RecallPageOverlap.spec.ts` — stays on the same tracker, skips threshold, and remounts spelling on Try again — ~16.5ms baseline — single lean mount; overlap retry needs full Quiz remount — 2026-08-20
- `frontend/tests/pages/FolderPage.moveDestination.spec.ts` — retries cross-notebook folder move with merge after 409 conflict — ~33.8ms baseline — the unique cross-notebook parent selection, conflict confirmation, retry payload, and destination navigation require the mounted page journey; generic conflict retry is covered separately, but replacing this combination with a narrower test would lose its cross-notebook behavioral protection — 2026-09-03
- `frontend/tests/components/form/RichMarkdownEditor.propertyAssimilation.spec.ts` — skips the property from its own property panel after confirming — ~16.3ms baseline — the one concise mounted-editor journey uniquely protects property-key routing through the panel, confirmation, and skip request; narrower composable coverage would lose the panel wiring — 2026-09-03
- `frontend/tests/wiki-link-or-relationship/SearchDialog.actions.spec.ts` — calls moveNoteToNotebookRootInNotebook with notebook id after confirm — ~15.0ms baseline — already uses fake timers and one concise mounted SearchForm journey; it uniquely protects notebook-hit ID routing through confirmation into the move API, which the lower-level slot-rendering and stored-API tests do not cover together — 2026-09-03
