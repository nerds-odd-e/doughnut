# Isolated source review

Reviewer launch: `/root/source_only_review`, with `fork_turns: none`.
The sole input was `/tmp/seed119-original-source.txt`, extracted verbatim from
the original story's Goal through Scope boundaries plus Architecture and reuse.
No plan, product code, proof, investigation, or previous review was provided or
read during the isolated review.

The original review return in this session contained the 82 numbered commitments
retained below. It preceded this record-keeping request and the subsequent read
of `PLAN.md` to add the source-review link. Slice assignments were supplied
afterward by the coordinator; the observation or obligation cells start empty.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| 1 | A learner can focus a particular value within the existing note page, using tint and a leading edge on the selected value row only. | slice 3 | |
| 2 | The highlight is the only added visible selection cue. | slice 3 | |
| 3 | Opening a list property’s chevron chooses its first item destination in displayed order: `[walk, run, jog]` initially highlights `walk`. | slice 3 | |
| 4 | Selecting empty space in another item row moves the highlight and updates the URL: selecting `run` makes its URL current. | slice 3 | |
| 5 | Keyboard users can select an item. | slice 3 | |
| 6 | Value links retain their interaction, including opening their referenced note. | slice 3 | |
| 7 | Assimilation and tracker actions retain their interactions; a tracker action opens its tracker instead of selecting its row. | slice 3 | |
| 8 | Existing chevrons and property edit controls remain available. | slice 3 | |
| 9 | Scalar properties use the same tint and leading edge on their single value area; opening `topic: movement` highlights the area containing `movement`. | slice 1 | observed: frontend/tests/components/form/RichMarkdownEditor.propertyLocation.spec.ts: focused editable value uses expectFocusedPropertyValue computed tint, leading edge and positive geometry |
| 10 | Scalar values retain their specialized edit controls and learning actions within the highlighted value area. | slice 1 | observed: RichMarkdownEditor.propertyLocation.spec.ts: actual scalar edit-dialog click and specialized image control containment; propertyMemoryTracking.spec.ts: assimilation/skip actions. Original source keeps scalar action panel ordinary. |
| 11 | Scalar values are not rendered a second time. | slice 1 | observed: RichMarkdownEditor.propertyLocation.spec.ts: one existing scalar value field; production RichFrontmatterPropertyRow retains its single renderer |
| 12 | Property keys, chevrons, and scalar action panels retain their ordinary presentation. | slice 1 | observed: Reading RichFrontmatterPropertyRow.vue and RichFrontmatterPropertyPanel.vue: value-only class binding, retained ordinary key/chevron/panel markup; full frontend controls/layout suite passes |
| 13 | Read-only scalar/context rendering applies the same value-focus treatment. | slice 1 | observed: RichMarkdownEditor.propertyLocation.spec.ts read-only value frame; RecallPage.justReview.spec.ts tracked-property context frame; both computed tint/edge and geometry |
| 14 | Controls remain usable on narrow and touch screens. | slice 3 | |
| 15 | The interactive mockup supplies the row treatment; the recorded decisions govern deletion and scalar focus. | slice 1 | observed: Reading propertyValueFocus.scss against source UI decision/mockup: tint and leading edge on existing value frame |
| 16 | An item destination identifies its note, exact authored property key, exact parsed item text, and occurrence among equal values. | slice 2 | |
| 17 | The named route is `notePropertyItem` within the note route family, with path `/n:noteId/p/:propertyKey/i/:occurrence/:itemText`. | slice 2 | |
| 18 | Occurrence is positive and one-based. | slice 2 | |
| 19 | The route segment is `itemText = "~" + exact parsed value`, allowing empty and dot-only values. | slice 2 | |
| 20 | The route helper owns the single prefix. | slice 2 | |
| 21 | Callers supply decoded authored keys and values; Vue Router handles serialization. | slice 2 | |
| 22 | Hrefs are compiled from named locations; an example destination is `/n42/p/example%20of/i/1/~run`. | slice 2 | |
| 23 | Opening or reloading an item URL expands the property and highlights the addressed occurrence, including the second `run` in `[run, run, walk]`. | slice 2 | |
| 24 | Distinct values retain their destinations through reordering and unrelated item removal; reordering distinct values leaves a saved `run` URL identifying `run`. | slice 2 | |
| 25 | Changed or missing text, or a missing occurrence, produces explicit unresolved-item feedback while leaving the readable note and property visible; this includes editing or removing the saved `run` text. | slice 2 | |
| 26 | An unresolved item destination does not substitute another value. | slice 2 | |
| 27 | A missing property uses the existing unresolved-property behavior. | slice 2 | |
| 28 | Identical occurrences have no durable identity through edits among equal rows. | slice 2 | |
| 29 | Assimilation knows a value and opens its first matching occurrence; receiving `run` as the next assimilation item opens its property with `run` highlighted. | slice 4 | |
| 30 | The shared item destination and current focus are available to other surfaces. | slice 2 | |
| 31 | An existing `noteProperty` list destination opens the property without selecting an item. | slice 2 | |
| 32 | That property destination represents cleared focus after deletion, without parallel local selection state. | slice 2 | |
| 33 | Expanding an empty list opens the property. | slice 3 | |
| 34 | A scalar `noteProperty` destination highlights its scalar value. | slice 1 | observed: RichMarkdownEditor.propertyLocation.spec.ts mounted noteProperty route highlights existing scalar frame |
| 35 | Panel and row changes replace history. | slice 2 | |
| 36 | Inbound links push history. | slice 2 | |
| 37 | The conversation query survives navigation within the note route family. | slice 2 | |
| 38 | Rename follows the authored key while preserving a still-resolvable item selector. | slice 2 | |
| 39 | Each expanded list item has a minus button at the start of its row. | slice 5 | |
| 40 | The list’s whole-property minus and everything used only by that interface are deleted. | slice 5 | |
| 41 | Expanded lists offer item removal while ordinary Markdown and value editing remain available. | slice 5 | |
| 42 | Scalar removal remains available. | slice 5 | |
| 43 | Item minus removes one displayed occurrence. | slice 5 | |
| 44 | Removing the final item removes the entire property entry and returns to the whole-note location; `[run]` follows this journey after confirmation when history exists. | slice 6 | |
| 45 | Removing an item from a surviving property preserves sibling values and their order; removing `run` preserves `walk` and `jog`. | slice 5 | |
| 46 | Removing the highlighted item clears item focus while leaving the surviving property open. | slice 5 | |
| 47 | Removing another row retains focus on the surviving selected occurrence and adjusts its occurrence number when necessary; removing the first `run` from `[run, run, walk]` keeps the selected second `run` highlighted as occurrence 1. | slice 5 | |
| 48 | Before deleting matching learning history, the learner receives a warning and can Cancel. | slice 5 | |
| 49 | Cancel preserves content, learning history, focus, and URL. | slice 5 | |
| 50 | Confirm removes the selected content and this learner’s matching trackers through existing tracker-deletion semantics. | slice 5 | |
| 51 | For an item in a surviving property, history matching uses the exact property key and value. | slice 5 | |
| 52 | When removing the property entry, history deletion uses the existing whole-property behavior. | slice 6 | |
| 53 | Equal values share learning history, and the item warning explains that deletion affects any remaining equal occurrence too. | slice 5 | |
| 54 | Sibling values retain their histories; confirming removal of `run` preserves the histories of `walk` and `jog`. | slice 5 | |
| 55 | Removal can proceed directly when there is no matching learning history. | slice 5 | |
| 56 | Existing authorization continues to govern content editing. | slice 5 | |
| 57 | Existing authorization continues to govern each learner’s tracker deletion. | slice 5 | |
| 58 | Assimilation eligibility does not limit which authored rows can be focused. | slice 3 | |
| 59 | Assimilation eligibility does not limit which authored rows can be removed. | slice 5 | |
| 60 | Ordinary list editing continues to support authored empty lists; final-item minus removes the property entry. | slice 6 | |
| 61 | This story supplies destinations and the focus contract for SEED-117 recall navigation; recall presentation remains in that story. | slice 2 | |
| 62 | This story supplies destinations and the focus contract for SEED-120 focused export; export implementation remains in that story. | slice 2 | |
| 63 | Portable wiki-link item syntax is deferred. | slice 2 | |
| 64 | Persistent row IDs are deferred. | slice 2 | |
| 65 | The implementation extends existing route-owned focus and shared named-location/href owners, without a second selection store. | slice 2 | |
| 66 | Authored list rendering is reused. | slice 2 | |
| 67 | Ordinary content saving is reused. | slice 5 | |
| 68 | The tracker guard is extended to distinguish item focus from whole-property removal. | slice 5 | |
| 69 | Existing tracker DELETE is reused. | slice 5 | |
| 70 | Existing warning and cancellation UI is reused. | slice 5 | |
| 71 | History deletion and content saving remain separate existing operations. | slice 5 | |
| 72 | This story introduces no atomic publication protocol. | slice 5 | |
| 73 | Scalar and list values share presentation responsibility while retaining their different editing and learning semantics. | slice 2 | |
| 74 | The whole-property-box highlight implementation and its orphaned dependencies are deleted. | slice 1 | observed: Separate acceptance reading: retired exact wrapper visual expression has zero matches in frontend/backend/cli/e2e_test/packages/docs/.agents; replacement value-frame proof |
| 75 | Route focus, expansion, scrolling, and missing-property handling remain required responsibilities. | slice 1 | observed: RichMarkdownEditor.propertyLocation.spec.ts route focus, real expanded panel, scrollIntoView signal and decoded missing-key notice; complete note/context continuation |
| 76 | Affected proof is replaced with proof of value highlighting and retained interactions. | slice 1 | observed: Replaced wrapper-class assertions with expectFocusedPropertyValue; edit-dialog click plus existing real learning/link/reify/specialized suites passed full frontend 399 files/2470 tests |
| 77 | A separate acceptance sweep is performed. | slice 1 | observed: One-time removal reading from slice1 return: exact retired visual expression zero matches; data-property-focused classified retained expansion/location responsibility; no permanent absence proof |
| 78 | Removal is limited to obsolete presentation and list whole-property action branches; still-used focus machinery remains. | slice 5 | |
| 79 | ADR 0001’s authored-property vocabulary and private key/value learning identity remain governing constraints. | slice 5 | |
| 80 | ADR 0004 keeps Markdown authoritative and Portable references separate from SPA locations. | slice 2 | |
| 81 | ADR 0005 governs the named route family, authored key, history navigation, and shared chrome. | slice 2 | |
| 82 | The selected approach requires no ADR change or exception, schema change, or new North Star direction. | slice 2 | |

## Premises and observations

Observations used the owned workspace at `6282f1a14646b4d3213b14947e4bab8127021193`
on 2026-11-10. New interactions remain future slice proof.

| Premise / consuming operation | Observation and result | Remainder / owner |
| --- | --- | --- |
| Proposed raw key/value selector survives named routing and browser normalization | 10:32 Singapore: disposable Chromium spec used production metadata assembly, serialized raw named params, native `new URL`, then fresh router navigation; 9 cases passed, including empty/dot-only/reserved/Unicode/duplicate/long text and query/family | Actual page reload/fallback belongs to slice 2; scratch was deleted |
| Existing authoring preserves duplicate order and supports empty-list emission | Parser/editor command below: 2 files, 20 tests passed | Server save belongs to slice 5; final-entry policy to slice 6 |
| Existing focus, learning, list rendering and shared context boundaries execute here | Affected baseline command below: 7 files, 41 tests passed at 10:33 Singapore | Old presentation assertions are replaced in slice 1; baseline does not prove new behavior |
| Existing assimilation navigation boundary executes here | Route probe command below also ran route-family 14 tests and assimilation 5 tests; all passed (28 total with probe) | Item targeting is new slice 4 behavior |
| Copied item href reaches the route intact rather than being suppressed as a Portable path | 10:45 Singapore: real metadata registration, mounted QuillEditor ordinary-anchor clicks in editable/read-only modes, real classifier/handler/router; complete href/key/value/occurrence/query retained. Scratch plus conversion/paste suites: 42 tests passed | Actual item lookup/reload belongs to slice 2; scratch edits reverted |
| Isolated E2E spec admission and mock selection accept the intended scope | 10:39 Singapore: ran `selectedCypressSpecs` and `assertSupportedIsolatedCypressSpecs` from production selection module with the exact three-feature `--spec` below; accepted all, private OpenAI mock required, no Wikidata mock | Stopped before state-changing database/stack launch; bounded early probe at slice 2 |

The selection observation used the E2E spec argument below as
`argv: ['--spec', selection]`, this `checkoutRoot`, and `parentSpecArgs: []`,
under `./scripts/run.sh node --input-type=module`; all three features selected.

Bounded early probe, before slice 2 edits: run existing `note_property.feature`
through the isolated wrapper in the execution checkout. It must load the note
and open a named property URL through that checkout's application origin.
Stack/database startup is state-changing, so it is deferred to execution.
Failure stops dependent E2E and route integration; diagnose/replan before
extending the implementation. This probe grants no paid/real-service authority.

Commands referred to by the retained observations above remain in the
[plan's Commands and acceptance](PLAN.md#commands-and-acceptance).

## Slice 1 acceptance

- Mounted `noteProperty` editable/read-only/specialized scalar, complete-note and just-review triggers now observe the existing value frame's tint, 3px leading edge and positive/inset geometry through `expectFocusedPropertyValue`. The editor also opens its real value dialog; full consumer suites exercise retained links, reify, edit, removal, learning and touch controls. Source commitments 9–13, 15, 34 and 74–77 are compared individually in the table above.
- The plan's seven-file affected baseline passed 41 tests; both listed `vue-tsc --noEmit` and `tsc --noEmit` commands passed. `./scripts/run.sh pnpm frontend:test` passed 399 files/2470 tests (77.07s), including the final edit-dialog and single existing scalar-field observation. All use real mounted rendering and mocked SDK HTTP boundaries. `scripts/check_diff_whitespace.sh` passed.
- Consumer trace: `node .agents/skills/dough-execute-plan/scripts/consumer-trace.mjs --checkout /Users/terryyin/git/doughnut/.worktrees/highlight-and-link-to-a-specific-property-item --base add1cf80f5ef58b668ea31fe6b1aa6b183b4fd57 --term property-value-frame --term data-property-value-focused --term bg-primary/10 --term rich-note-property-row`; complete located-consumer dispositions retained at `/tmp/seed119-slice1-consumer-trace.md`. Frontend consumers ran; incidental planning/unrelated-class matches excluded; E2E selector contracts inspected with equivalent mounted control/layout proof, real app journey owned by slice 2.
- Separate removal reading: exact retired expression `rounded bg-primary/10 ring-1 ring-primary/30` has no matches across `frontend backend cli e2e_test packages docs .agents`. Its wrapper-class assertions were replaced; `data-property-focused` remains for expansion/location. Actual diff exposes no exclusive helper/dependency to retain. No permanent absence assertion was added.
- Freshness unchanged at trunk `fca96ef9073f5c0094fa1fe72c406fbe67fa55a7`, base claim `add1cf80f5ef58b668ea31fe6b1aa6b183b4fd57`; intervening trunk only requeued unrelated SEED-112, no product change from named siblings SEED-117/120. Full frontend proof launched before coordinator settlement (sequencing deviation); unchanged source and absent product changes preserve its result.
- Implementation used about 6–7 active minutes plus proof wait; same cohesive scalar-frame change, no accumulating correction or scope change. Host prelaunch load 5.96/10.66/10.85 with 65% free memory, no coordinator-owned competing proof; sequence later heavy checks and reassess contention before launches.
- Independent refactor `/root/refactor1` completed: unified scalar-frame attributes in `RichFrontmatterPropertyRow.vue`, simplified wrapper classes and scoped the shared SCSS with `@use`; row is 242 lines. No components/API generation. Root inspected actual changed bindings and unchanged observation locations; prior full-suite learning/reify/removal/link proof stays applicable.
- Refactor proof passed 6 files/33 tests: `./scripts/run.sh pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyLocation.spec.ts tests/components/form/RichMarkdownEditor.readOnlyProperties.spec.ts tests/components/form/RichMarkdownEditor.propertyEditingLayout.spec.ts tests/components/form/RichMarkdownEditor.propertyTouchTargets.touch.spec.ts tests/notes/NoteTextContent.properties.spec.ts tests/pages/RecallPage.justReview.spec.ts`; both plan typechecks and `scripts/check_diff_whitespace.sh` passed. Reobserved actual scoped tint/edge/geometry, scroll/controls, read-only, draft layout and coarse-pointer targets. Trace `/tmp/seed119-refactor1-consumers.md` disposes every located consumer; no uncovered behavior.
