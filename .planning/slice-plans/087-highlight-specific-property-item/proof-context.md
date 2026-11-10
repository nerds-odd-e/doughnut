# Proof context

These dated planning observations, literal commands and original concern review
are relocated verbatim from the active acceptance evidence; no observed action,
input, result, provenance or limitation is changed. Execution results remain in
[source-review.md](source-review.md).

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
[retained Commands and acceptance](#commands-and-acceptance).

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
