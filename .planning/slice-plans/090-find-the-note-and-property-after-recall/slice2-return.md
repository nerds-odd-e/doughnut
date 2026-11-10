# Slice 2 implementation return

Implementation complete within approximately 8 minutes active; focused Cypress startup/run is the plan's test-runtime exception. All changes remain uncommitted. No refactor, selective formatter, standalone hook-owned lint, commit, push or done mark performed. Coordinator PLAN/source-review edits preserved.

## Outcome and owned paths

- `frontend/src/components/notes/NoteTitleWithLink.vue`: ordinary title always uses `noteShowLocation`; removed the orphaned focused-property prop/branch after caller inspection.
- `frontend/src/components/recall/FocusedPropertyIndicator.vue`: separate Reviewed property anchor uses `notePropertyLocation` for key-only recall, or `notePropertyItemLocation` with exact raw value and occurrence 1.
- `frontend/src/components/recall/NoteUnderQuestion.vue`: supplies note id and retains the ordinary title link. Existing adapter already carried raw value; no parallel route state/resolver introduced.
- `frontend/tests/components/recall/NoteUnderQuestion.spec.ts`: real-router distinct whole-note/property clicks replace retired focused-title tests.
- New `frontend/tests/components/recall/NoteUnderQuestion.sourceNavigation.spec.ts`: actual answered-result/tracker callers reach the real NoteShowPage for first duplicate, raw alias, reordered value, stale item and key-only focus; switching results changes path and both destinations.
- `frontend/tests/pages/MemoryTrackerPageView.spec.ts`: aligns the surviving property/value observations with Reviewed property wording.
- `e2e_test/features/recall/property_memory_tracker.feature`, `e2e_test/start/pageObjects/noteUnderQuestion.ts`, `e2e_test/step_definitions/assimilation_memory_tracker.ts`: title journey proves whole note; source journey checks the exact item href and real focused item independently of display text.

No API signature/component identity change; no generation trigger. All owned files below 250 lines before coordinator formatting (new source-navigation spec 100 lines; trace 208 lines).

## Proof and observation mapping

Q (terminal exit 0, 36 files / 158 tests passed, no unhandled errors, 18.46s):

```sh
./scripts/run.sh pnpm frontend:test tests/components/recall/Answered tests/components/recall/NoteUnderQuestion tests/components/conversation/ConversationComponent tests/pages/RecallPage tests/pages/MemoryTrackerPage tests/notes/NoteContextReader.spec.ts tests/pages/NoteShowPage.propertyItem.spec.ts
```

Log `/tmp/seed117-slice2-tests.log`. Q selected all new six caller-to-destination cases, the changed distinct-title/key test, retained five geometry/preview cases and affected existing result/tracker/conversation suites. After Q, only new source-navigation assertions were strengthened: exactly one focused row for matching values and zero selected rows for stale feedback. Unchanged Q observations remain valid; the strengthened six-case spec passed separately on final content (terminal exit 0, 1 file / 6 tests, 26.82s):

```sh
./scripts/run.sh pnpm frontend:test tests/components/recall/NoteUnderQuestion.sourceNavigation.spec.ts
```

Final log `/tmp/seed117-slice2-source-final.log`.

| Source ids | Trigger/setup → expected result → actual observer |
| --- | --- |
| C02,C07,C12,C13,G1 | Property recall → title opens whole note independently of separate property control → `NoteUnderQuestion.spec.ts` clicks both real anchors and observes `notePropertyLocation` versus `noteShowLocation`; E2E title scenario observes whole-note location. Slice1 notebook/folder navigation remains unchanged. |
| C03,C14,C18,C33,E09 | Key-only memory tracker → authored property and open panel → `sourceNavigation.spec.ts`, `follows a property-only tracker into the expanded authored key`, clicks tracker source anchor, observes named key route, visible real property panel and focused scalar content. |
| C04,C15,C16,C17,C23,C24,C32,Q01,Q02,Q05,E11,E12 | Result with raw value → occurrence1 source item, including duplicates, aliases and reordered current values → `follows the reviewed source into the note page: $label` parameterized cases. SDK supplies current note Markdown; product anchor drives production router, then real NoteShowPage renders it. Observes exact named raw-value/occurrence route, expected row index focused, exactly one selected row, selected raw value and visible row/panel/body. First duplicate fixture is `[[run]], [[run]], [[walk]]`; alias is complete `[[Verbs/run\|running]]`; reordered fixture puts reviewed `[[run]]` last. No occurrence identity is added. |
| C34,E10 | Recalled run missing from current walk/jog note → readable note/property plus explicit unresolved feedback, no other selection → `opens a readable current note with stale reviewed-item feedback` clicks actual result anchor and observes real warning text, body, visible panel and zero focused rows. |
| C36 | Switch first result to another source notebook/note/key/value → location and destinations update together → `switches result path, title and reviewed item destinations together` observes Second notebook, then clicks new source and title and checks their distinct named destinations. |

All mounted setup uses `makeMe` and only HTTP `mockSdkService`/`mockNotebookGetForNoteRealm`. Production router, adapter, store, header, property renderer and NoteShowPage are real. The note-page mount occurs after following the caller anchor on that same router. Setup supplies the current source data, never the selected outcome.

Both frontend typecheck commands were run to terminal after final assertion content:

```sh
./scripts/run.sh pnpm -C frontend exec vue-tsc --noEmit
./scripts/run.sh pnpm -C frontend exec tsc --noEmit
```

Both commands terminal exit 0 on final content. Logs `/tmp/seed117-slice2-vue-tsc-final.log`, `/tmp/seed117-slice2-tsc-final.log`.

Cypress (terminal exit 0, 5 / 5 scenarios passed, 1m30s feature runtime):

```sh
./scripts/run.sh pnpm cy:run --spec e2e_test/features/recall/property_memory_tracker.feature
```

Log `/tmp/seed117-slice2-cypress.log`. The isolated owning wrapper provisioned its private DB/OpenAI mock and stack at `http://127.0.0.1:58728`, then cleaned up. Changed title scenario clicks the result breadcrumb and asserts whole-note named location. Changed list scenario answers a real mocked question, checks Reviewed property href compiled with exact raw `[[run]]`/occurrence1, follows it, then observes focused/open example-of panel and visible highlighted first run item. Remaining three scenarios retain tracker isolation and property removal regression journeys. No paid service used; no manual observation required.

## Consumers, sweep and remaining state

[Actual-diff generated/disposed consumer trace](consumer-trace-slice2.md) includes retired contract terms, helper exports, step phrases and actual accidental-match feature/helper consumers. Each listed consumer is run, reused or excluded with an effect reason. Ordinary NoteTitleWithLink callers supply only noteTopology; their whole-note contract is identical. Q retains accidental-match reveal/resolve and preview purpose; its actual E2E helper checks source containment and alert/dialog, so that feature's observed boundary did not change and no extra Cypress feature is claimed.

One-time reading: inspected all NoteTitleWithLink callers, including multiline SearchResultListItem/UndoConfirmationForm calls. Product searches over `frontend/src`, `frontend/tests`, `e2e_test` for `{ noteTopology, focusedPropertyKey }`, the retired title prop inside NoteTitleWithLink, `expectNoteUnderQuestionFocusedProperty`, its retired step phrase and `Focused property:` returned no matches. This is an acceptance reading, not a permanent assertion/checker. Genuine focus keys and destination behavior remain.

No missing slice2 behavior or proof obligation. G1 is ready to close with the distinct destination proof above. G2 remains the planned slice3 raw-value presentation interim; no new loss. Source preview/actions and note authorization endpoints unchanged.

Host decision: this Mac has 16 CPUs. Before the single Cypress launch at 18:43, load11.59/8.31/10.70 and 51% system-wide free memory, no other coordinator-owned heavy work; Q and typechecks had finished. Selected one isolated feature/stack sequentially, with me owning terminal completion. Other Taken worktrees may share the host; no control over their processes was assumed. Final narrow proof/typecheck pass followed Cypress cleanup. Later observation showed load43.10/25.01/17.26 from competing work; no failed command or unchanged overloaded retry occurred. Reconsider headroom before any further heavy proof; current product needs no further Cypress run for this slice.

Final `scripts/check_diff_whitespace.sh` passed (terminal exit0). All owned verification sessions completed; no live command remains.
