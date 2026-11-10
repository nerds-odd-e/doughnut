# Slice 3 implementation return

Uncommitted implementation complete, approximately 7 minutes active including the bounded wire-null repair; Cypress provisioning/runtime is the plan's exception. No refactor, selective formatting, standalone hook-owned lint, commit, push or done mark. Coordinator PLAN/source-review changes preserved. All owned commands completed; no live command remains.

## Outcome and owned paths

- `frontend/src/components/recall/FocusedPropertyIndicator.vue`: ordinary `WikiLinkToken` now renders the raw reviewed value with supplied source-realm `wikiLinks` and persisted Markdown. The separate source anchor still addresses the raw value/occurrence1.
- `frontend/src/components/recall/NoteUnderQuestion.vue`: passes existing viewer-scoped context and places breadcrumb, Reviewed property/value line, then spelling slot in the approved order.
- `frontend/src/components/recall/recalledNoteUnderQuestionProps.ts`: normalizes real API `propertyValue:null` to the existing absent-value representation, so scalar/key-only recall uses its authored-key route and never supplies a null token.
- New `frontend/tests/components/recall/NoteUnderQuestion.reviewedValue.spec.ts`: seven real-token/router cases: plain text, resolved run, raw alias, property selector, saved unresolved reference, tracker alias/source identity and key-only wire-null recall.
- `frontend/tests/pages/MemoryTrackerPageView.spec.ts`: deleted the dedicated raw-interpolation assertion. Its surviving tracked-value identity is now observed through the tracker-alias source route above, independently of display text.
- `e2e_test/features/recall/property_memory_tracker.feature`, `e2e_test/start/pageObjects/noteUnderQuestion.ts`, `e2e_test/step_definitions/assimilation_memory_tracker.ts`: existing list journey follows readable run to its note, returns to the recall answer, then follows the raw source item and observes focus/highlighting.

All files were below250 lines before coordinator formatting (new spec92, main trace225, supplemental trace169). No API signature/component identity/schema change; no generation trigger. No new resolver, selection state or preview mode.

## Final proof

Q, terminal exit0, **37 files / 164 tests passed**, no unhandled errors,19.13s:

```sh
./scripts/run.sh pnpm frontend:test tests/components/recall/Answered tests/components/recall/NoteUnderQuestion tests/components/conversation/ConversationComponent tests/pages/RecallPage tests/pages/MemoryTrackerPage tests/notes/NoteContextReader.spec.ts tests/pages/NoteShowPage.propertyItem.spec.ts
```

Log `/tmp/seed117-slice3-tests-final.log`. Actual selection includes all seven new reviewed-value cases, the five real-CSS location cases, six caller-to-NoteShowPage source-focus cases, and affected existing result/spelling/tracker/conversation actions.

Both T commands terminal exit0 on final content:

```sh
./scripts/run.sh pnpm -C frontend exec vue-tsc --noEmit
./scripts/run.sh pnpm -C frontend exec tsc --noEmit
```

Logs `/tmp/seed117-slice3-vue-tsc-final.log`, `/tmp/seed117-slice3-tsc-final.log`.

Cypress, terminal exit0, **5/5 scenarios passed**,21s feature runtime, owning wrapper cleanup completed:

```sh
./scripts/run.sh pnpm cy:run --spec e2e_test/features/recall/property_memory_tracker.feature
```

Final log `/tmp/seed117-slice3-cypress-final.log`. Private DB/OpenAI mock/stack at `http://127.0.0.1:58728`; no paid calls. The linked-value journey observes real backend viewer resolution, actual `run` navigation, return to the answer, exact source-item href and visible first-item focus. Whole-note title and tracker regressions remain green.

Final `scripts/check_diff_whitespace.sh` passed.

## Source observations and scope inspection

| Commitments | Trigger/setup → expected result → actual observation |
| --- | --- |
| C19,E06 | Raw plain run → readable text → `shows a plain reviewed value as readable text` asserts the rendered value text and text-only presentation. |
| C20,C21,C22,Q03,E05 | Source Markdown plus viewer-scoped wikiLinks maps → ordinary display and independent note/property destination → `follows the readable $raw reference independently of its reviewed source` asserts run/running/past labels, exact raw source-item route, then reference noteShow(77) or noteProperty(77,tense). Alias tracker case independently proves adapter-to-raw-source identity. |
| C25,C26,E07 | Saved unresolved `[[Missing\|unknown]]`, no resolved map → existing visible dead-link presentation and non-navigation → `keeps a saved unresolved reviewed reference visibly unresolved` observes unknown label, dead-wiki-link class, href#, and unchanged root after clicking. |
| E04 | Reviewed run source with sibling plus three distinct destinations → new real-token run case and current Cypress list journey prove reference navigation/raw source independence; retained `NoteUnderQuestion.spec.ts` whole-note/source clicks and `sourceNavigation.spec.ts` real run/walk source highlight prove their destination results. All selected in current Q; Cypress title and source/reference journeys5/5pass. |
| G2 | Resolved value previously interpolated raw → ordinary token display/destination → production token delegation plus the cases above and final integrated journey settle the interim. |
| G3,C18,E09 | Real wire scalar focus with `propertyValue:null` → readable retained header and key-only destination → new `opens the authored key for key-only recall from a nullable wire response` mounts actual AnsweredQuestionComponent from makeMe serialized to a wire-null fixture, clicks its rendered source control and asserts named noteProperty. Current geometry suites and Cypress scalar title/source rendering stay green. |
| C35 | Existing read authorization → same authorized read owner → inspected `noteStore.getNoteRealmRefAndLoadWhenNeeded` → `loadNoteRequest` → `NoteController.showNote`, which retains `assertReadAuthorization` then viewer-scoped `noteRealmService.build`; no endpoint/resolver change. Mounted HTTP fixtures prove frontend consumption, not server authorization itself. |
| Q04,X01,X02 | Naturally supported values and retained preview scope → inspected unchanged token parser/resolver and four header callers; spelling retains NoteContextReader, other recall contexts gain no new preview/editor. No new value-rejection branch. Existing preview/actions remain covered by Q. |
| X03-X08 | Scheduling/generation/Portable syntax/snapshot/row-identity exclusions → inspected actual owned product diff: only shared frontend presentation/adapter changes, named route helpers reused unchanged; no server, schema, identity or generation changes. Existing occurrence1 policy retained. These are scope readings, not invented absence tests. |

All fixtures use makeMe; only backend HTTP is mocked. Production adapter, store, token, router and destination components run for real. Fixture maps supply source reference data, never navigation or focus outcomes. New null regression models the observed wire representation because generated optional TS types omit runtime JSON null.

## Causal repair and proof history

Initial Q37files163tests and both T passed before the null fixture existed. First Cypress finished3pass/2fail: scalar recall response had `recalledNote.propertyValue:null`; SUT log showed `WikiLinkToken token=null` and `.trim()` rejection. The new key-only mounted regression failed before the repair with Vue invalid-token warning (`/tmp/seed117-slice3-null-before.log`); the actual Cypress/SUT traces establish the same null cause. No failure was waived. The adapter normalization then passed final Q164tests, both T and causal Cypress5/5. Initial Cypress log retained at `/tmp/seed117-slice3-cypress.log`.

Prelaunch decisions on this Mac16CPUs: initial bounded Q/T at load4.18/12.17/14 and63%free; first Cypress after Q/T terminal at load3.99/8.38/12.02 and63%free. After failure/cleanup and the repair's Q/T terminal, reconsidered before the single causal rerun: load6.04/7.95/11.17,61%free, no other coordinator-owned heavy work. Other Taken worktrees may share the host; their processes were untouched. Selected one focused isolated stack at a time and owned every command to terminal. No further deferred required proof.

## Consumer trace and sweep

[Actual-diff trace](consumer-trace-slice3.md) and [resupplied wikiLinks contract term](consumer-trace-slice3-wikiLinks.md) were generated with the installed script; every consumer has a run/reused/excluded disposition. Helper exports/step phrases reach actual feature consumers. Existing accidental-match helper/feature observations use alert/source containment/resolve, not raw-value text; equivalent current mounted suites passed and that E2E boundary remains unchanged.

One-time acceptance reading over `frontend/src`, `frontend/tests`, `e2e_test` for the retired raw interpolation, dedicated raw-value expectation and its old test name returned no matches. Retained item identity and source selection remain positively proved by the replacement tests. No permanent absence assertion/checker added.

G2 and G3 are ready to close from the current observations above. No missing final-slice behavior or proof, no remaining implementation command, and no new loss reported.
