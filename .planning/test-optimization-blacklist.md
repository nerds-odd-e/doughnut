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
- `frontend/tests/pages/FolderPage.moveDestination.spec.ts` — sends destinationNotebookId and navigates after cross-notebook root move — ~14.9ms measured focused baseline — the single mounted-page journey uniquely protects destination selection wiring, the root-specific payload without a parent folder, and cross-notebook navigation; a lower-level mutation test would lose the UI-to-request connection — 2026-09-03
- `frontend/tests/components/form/RichMarkdownEditor.propertyAssimilation.spec.ts` — skips the property from its own property panel after confirming — ~16.3ms baseline — the one concise mounted-editor journey uniquely protects property-key routing through the panel, confirmation, and skip request; narrower composable coverage would lose the panel wiring — 2026-09-03
- `frontend/tests/wiki-link-or-relationship/SearchDialog.actions.spec.ts` — calls moveNoteToNotebookRootInNotebook with notebook id after confirm — ~15.0ms baseline — already uses fake timers and one concise mounted SearchForm journey; it uniquely protects notebook-hit ID routing through confirmation into the move API, which the lower-level slot-rendering and stored-API tests do not cover together — 2026-09-03
- `frontend/tests/wiki-link-or-relationship/InsertWikiLink.spec.ts` — does not call the inserter when Add a new relationship note is clicked — ~13.4ms baseline — replacing the debounced search journey with direct child selection did not improve measured runtime; the mounted SearchForm branch uniquely protects the relationship transition without accidental wiki-link insertion — 2026-09-03
- `frontend/tests/commons/Modal.spec.ts` — focuses autofocus target and prefers text controls in a marked autofocus container — ~13.1ms focused baseline — both mounted modal lifecycles uniquely protect the native `autofocus` selector and the marked-container text-control preference; reusing one parent with keyed modal remounts regressed the focused duration to ~16.9ms — 2026-09-03
