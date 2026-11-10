# Highlight and link to a specific property item

**Source:** [selected story](../../seeds/SEED-119-highlight-specific-property-item.md#highlight-and-link-to-a-specific-property-item)
**Identity:** SEED-119#highlight-and-link-to-a-specific-property-item

## Goal and scope

Focus scalar values and individual list rows with one visual treatment; return
to an exact list occurrence through a note URL; remove one occurrence and its
matching learning history after warning/cancellation. Final-item minus removes
the property entry. Preserve sibling order, value links, learning actions and
readable note navigation. The seed owns the agreed examples and boundaries.

Delete the whole-property-box visual and list whole-property minus transitively,
including exclusive tests/helpers/styles. Retain focus/expansion/scrolling and
scalar removal. Use replacement proof plus the separate acceptance reading.

Deferred: persistent row IDs, Portable wiki-item syntax, SEED-117 recall UI and
SEED-120 export. Ordinary authoring retains empty lists; final-item minus deletes.

## Current decisions and reuse

- Developer decisions on 2026-11-10 are recorded in the seed: exact occurrence
  focus, confirmed removal, final-entry deletion and value-only presentation.
- Extend `useFocusedNoteProperty` and named routing owners. One route-owned
  focus contains key and optional item `{ value, occurrence }`; retain the
  composable's existing off-note/injected-focus boundary where needed.
- Named `notePropertyItem` uses the seed's nested route, one-based occurrence,
  and `~`-prefixed raw item text. Helpers alone own the prefix. `noteProperty`
  on a list means expanded with no selected item; chevron chooses first item.
  Scalar `noteProperty` highlights its existing value area. Stale item focus
  is explicit; it never substitutes the first row. Preserve query and rename
  following. Value identity is exact parsed text, not normalized display text.
- Reuse list row rendering and specialized scalar editors. Give value frames
  the shared tint/leading edge. Keep action/link hit targets separate from row
  selection; selection must be keyboard reachable without nested buttons.
- Reuse `usePropertyMemoryTrackerGuard`, current-learner note info, existing
  tracker DELETE and ordinary content save. Item-in-surviving-property targets
  exact key/value; final entry targets the key's history as scalar deletion
  does. Equal occurrences share history; communicate that in the warning.
- Existing DELETE/content-save coordination is separate requests. Preserve its
  error propagation and loading behavior; cancellation precedes mutations.
  No new atomic API, schema or publication protocol is promised.
- PFE: reuse `PropertyFocus` and assimilation key/value; recreated index IDs
  cannot supply persistent identity. Add no competing focus or row-ID system.
- Follow [ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md),
  [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
  and [ADR 0005](../../../docs/adrs/0005-web-routes-accepted.md), with no conflict.
  Ordinary saving retains North Star's accepted-change owner; no new direction.

## Proof ownership

| Final promise | Owning slice | Observable proof |
| --- | --- | --- |
| Scalar/specialized/read-only/context value focus and retained controls | 1 | Mounted editor, complete note and just-review context; value-frame geometry/tint and real controls |
| Exact URL, reload, duplicates, reorder, stale item, authored key, query, shared note chrome | 2 | Route round-trip plus mounted page and real inbound/reload E2E |
| Chevron first item; row/background and keyboard selection; shared focus for consumers | 3 | Mounted editor actions change route and selected value; complete authored rows remain usable |
| Value links and learning actions retain their destination; narrow/touch controls | 3 | Real browser click/keyboard/geometry tests including tracker-loading state |
| Assimilation's exact value opens first matching occurrence | 4 | Production-router assimilation boundary and notebook workspace continuation |
| Warn/cancel preserves content/history/focus; confirm removes exact item/history and preserves distinct siblings | 5 | Mounted note save boundary plus real persisted reload/tracker E2E |
| Selected removal clears focus; other removal preserves selected occurrence | 5 | Mounted editor route/content, including earlier equal occurrence removal |
| Final removal deletes entry and its key's history, then goes to note | 6 | Mounted note save boundary and real persisted final-entry save/reload journey |

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

## Consumer trace and structural seams

Expected production edits include route metadata/assembly mapping, note location
helpers/classifier, focus/follow-location composables, property row/list/panel,
list learning rows, editing coordination, tracker guard and assimilation
navigation. Follow actual changed output into notebook chrome/conversation,
read-only `NoteContextReader`, complete-note rendering and just-review.

Preserve API clients and backend deletion semantics. Trace copied item hrefs
through classification/link handling, retaining selector and authored href.
The probe reaches QuillEditor, `wikiLinkClick` and HTML-to-Markdown conversion.
Backend `NoteIdUrl` recognizes exact note-show paths; item hrefs remain ordinary
Markdown. Portable wiki-property destinations stay property-level.

Expected production files are at most 250 lines at the base; near the ceiling:
property editing 248, rich editor 248, properties 240, property row 237, editable
content 242. Keep removal orchestration in a cohesive editing collaborator and
value-frame presentation together. Add focused item specs rather than growing
near-ceiling location/memory tests. Recheck sizes before editing.

During delivery, run the installed consumer-trace procedure for the actual
diff and give every consumer run/reused/excluded disposition. Include assertions,
steps and `noteRichPropertyMethods`, `notePropertyLayoutMethods` and assimilation
helpers. Preserve scalar remove/reify/edit and replace focus proof at the value boundary.

## Ordered slices

Accepted interim behavior: list chevrons can open key-only/no-selection until
slice 3, and assimilation can open key-only until slice 4. Slice 5 can save an
emptied list as `[]`; slice 6 replaces that with final-entry deletion. These
states keep ordinary authoring usable and add no rejection or disabled action.

### 1. Focus the value area consistently
Type: Behavior
Status: planned
Behavior: Open a scalar property (including specialized and read-only/context
views) → its existing value has shared tint/leading edge and remains operable.
Move the visual responsibility from the property wrapper to the value frame;
retain expansion, focus identity, refs, scrolling and missing-property behavior.
Proof: Mounted property-location and complete-note/context examples observe
value-frame styling/geometry and usable edit/link/learning controls. Run the
affected baseline; replace old presentation proof with these observables.
Sizing: about 5 minutes including checks/cleanup for one presentation rule;
no new scalar rendering or editing model.

### 2. Return to an exact list occurrence from its URL
Type: Behavior
Status: planned
Behavior: Open/reload a named item URL → expand and highlight exact text plus
occurrence; stale selector → explicit unresolved feedback with readable note.
Run the bounded early probe first. Extend route metadata and component mapping,
named helpers, focus/follow-location parsing, item-row frame and note-family
classifier together. Preserve query/chrome, exact authored key and rename.
Expose the same resolved item focus for later consumers. Value changes/reorder
must exercise one text/occurrence rule, not another selector representation.
Proof: Named round-trip and mounted page examples for reload-equivalent fresh
mount, distinct reorder, duplicate occurrence, stale text and preserved query;
real inbound URL/reload scenario in existing `note_property.feature`.
Sizing: 5–8 minutes plus isolated stack external wait; the coherent URL-to-row
journey stays together. The probe's startup wait is the stated timing exception.

### 3. Select a list item while reading or operating its row
Type: Behavior
Status: planned
Behavior: Open chevron → first authored item destination; choose another row by
background or keyboard → that row's location/highlight replaces it. Existing
value links and learning controls still execute their own actions.
Use the slice 2 focus/route rule. Empty/property-only destinations represent
expanded lists without selection. Keep all authored occurrences available,
including rows ineligible for assimilation; retain existing off-note expansion.
Proof: Mounted editor interactions, duplicate/blank/long representative data,
real link/tracker clicks and narrow/coarse-pointer geometry/loading stability.
Sizing: about 5 minutes; no separate local selection model on note routes.

### 4. Continue assimilation at the addressed value
Type: Behavior
Status: planned
Behavior: Next assimilation unit includes key/value → push its item destination
and highlight first exact occurrence; a property-only unit opens its property.
Reuse the shared named helper and current count/toast/dismiss behavior.
Proof: Extend `useGoToNextAssimilation.spec.ts` using the production router and
the notebook workspace continuation; exercise the actual returned value.
Sizing: about 5 minutes including the focused existing suite.

### 5. Remove an occurrence with informed history deletion
Type: Behavior
Status: planned
Behavior: Minus with matching history → warning/cancel; cancel preserves the
saved values/history/focus, confirm removes one occurrence and targeted history.
Distinct siblings retain order/history; selected removal clears item selection,
other removal retains the selected row and recalculates its text/occurrence
selector, including removal of an earlier equal row. Equal values share history;
explain that loss in the warning. Replace list whole-property minus with item
minus and delete its exclusive dependencies; preserve scalar whole-entry removal.
Keep warning, deletion, content update and focus transition in one operation;
extend guard matching instead of copying the whole-key deletion loop. Reflect
existing API failures through shared loading/error handling before continuing.
Proof: Mounted note/editor with real popup stack and mocked SDK boundary;
assert warning text, cancellation state, accepted target tracker IDs and saved
Markdown. Cover shared duplicate history, earlier-equal-row removal and scalar
removal. Real E2E cancels/confirms assimilated-item removal, reloads persisted
values and observes surviving sibling learning data.
Sizing: 5–8 minutes including focused proof/cleanup; retain warning and mutation
together. Final-entry policy and its separate proof loop move to slice 6.

### 6. Remove the final item as a property entry
Type: Behavior
Status: planned
Behavior: Final-item minus → warn/cancel using the existing whole-key history
guard; confirm removes the key and its matching history, then replaces location
with the whole note. Reuse slice 5's operation, changing the empty-result rule;
ordinary value editing still supports authored empty lists.
Proof: Mounted save/route boundary distinguishes Cancel from confirmed key
removal, including existing history for the key. Real E2E removes the final
item, reloads the saved note, and observes whole-note navigation and the retained
other properties. This replaces slice 5's interim empty-list result.
Sizing: about 5 minutes plus focused-check/stack wait; existing whole-key guard
and named note navigation supply the special final-entry rule.

## Commands and acceptance

```sh
# Retained parser/editor observation (20 tests)
./scripts/run.sh pnpm frontend:test tests/utils/noteContentFrontmatterList.spec.ts tests/components/form/RichMarkdownEditor.propertyValueDialog.spec.ts
# Retained affected baseline (41 tests)
./scripts/run.sh pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyLocation.spec.ts tests/components/form/RichMarkdownEditor.propertyMemoryTracking.spec.ts tests/components/form/RichMarkdownEditor.listPropertyLayout.spec.ts tests/components/form/RichMarkdownEditor.listPropertyMemoryTracking.spec.ts tests/composables/usePropertyMemoryTrackerGuard.listProperty.spec.ts tests/notes/NoteTextContent.properties.spec.ts tests/pages/RecallPage.justReview.spec.ts
# Retained scratch route observation (28 tests; probe file was deleted)
./scripts/run.sh pnpm frontend:test tests/routes/seed119ItemUrlProbe.spec.ts tests/routes/noteRouteFamily.spec.ts tests/composables/useGoToNextAssimilation.spec.ts
# Execution frontend acceptance and required typechecks
./scripts/run.sh pnpm frontend:test
./scripts/run.sh pnpm -C frontend exec vue-tsc --noEmit
./scripts/run.sh pnpm -C frontend exec tsc --noEmit
# Execution integrated proof; private OpenAI mock is runner-owned
./scripts/run.sh pnpm cy:run --spec e2e_test/features/note_topology/note_property.feature,e2e_test/features/assimilation/assimilation_property_order.feature,e2e_test/features/recall/property_memory_tracker.feature
```

Assess execution-host load/memory and owned competing work before starting the
stack; run these three affected features together, not the entire E2E suite.
No live OpenAI call is required. If adding/deleting a component, regenerate
`frontend/components.d.ts` with `pnpm -C frontend exec vite build --emptyOutDir`.
No API regeneration is needed for the selected existing-endpoint approach.

Acceptance reading, separately from tests: collect deleted names/selectors,
sweep product code, tests, fixtures, CSS, docs, CLI, MCP and generated consumers
for orphaned dependencies, and inspect property/value/action render ownership.
Classify still-used focus/expansion attributes separately from retired visual
responsibility. Record searches/results once; add no permanent absence test.

Delivery follows AGENTS.md: Jidoka → fresh refactor agent → API generation if
needed → coordinator `./scripts/run.sh pnpm format:changed` once → evidence →
commit/check-only hook → push. Slices target ~5 minutes including proof/cleanup;
scrutinize overruns and decompose before >10 minutes except stated check/wait
exceptions. Retrospective/wrap-up follow authorized execution.

## Concern review

Refinement ran: retain 1–4; split original 5 into occurrence removal (5) and
final-entry policy (6), isolating its proof loop while keeping warning/mutation
together. Six Behavior slices share one model; no consolidation/Escalate/resplit.
Only focused-check/external-stack waits justify >10 minutes. Slice 2's early
probe bounds its state-changing prerequisite; no other blocking concern remains.
The coordinator records readiness; this plan grants no Take/execution authority.
