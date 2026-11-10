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

## Execution context

- Established start resumed 2026-11-10 by Nao-chan; source kind planned, story-branch mode.
- Owned execution checkout: `/Users/terryyin/git/doughnut/.worktrees/highlight-and-link-to-a-specific-property-item`; branch `codex/highlight-and-link-to-a-specific-property-item`. Existing dashboard-created linked worktree; no second claim.
- Remote `origin`, trunk `main`; increments target `refs/heads/codex/highlight-and-link-to-a-specific-property-item` in `nerds-odd-e/doughnut`.
- Starting revision `99ba3b9ac61db0bd3bc8ac58fe4d4c7ceed493e7`; accepted claim and initial candidate `add1cf80f5ef58b668ea31fe6b1aa6b183b4fd57`. Claim trunk CI is unobserved.
- Checkout setup: `./scripts/run.sh bash scripts/worktree_setup.sh` passed against current lockfile; applicable command `./scripts/run.sh pnpm -C frontend exec vue-tsc --version` passed (6.0.3).
- Observer: GitHub Actions selector `ci.yml`, verified active workflow `donut CI`, push covers all branches. Coordinator `nao-seed119`, directory `/tmp/dough-ci-501/watch-HpTDpK`, PID 69392, session 83266, yielded cell `g824:9`; runtime and launch rooted in this checkout. Target is the remote execution branch.
- Replanning remains authorized by the selected planned workflow; target about 5 minutes, hard scrutiny beyond 10 active minutes, with focused-check and external-stack waits excepted as stated above.
- Delivery hook is the common Git `pre-commit`, invoking check-only `./scripts/run.sh pnpm lint:changed`; coordinator fresh delivery owns selective formatting once. No API generation for existing endpoints; component inventory regeneration only on component addition/deletion.

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

## Resumed recovery context

- Developer on 2026-11-10 explicitly requested fixing the recovery failure and retrying. Fresh implementation and independent refactor corrected exact-content false-none classification and added explicit operator-reviewed resolution, preserving ordinary no-apply refusal. Root inspected the complete saved diff against resolved planning content: all eight-leaf draft edits survive with accepted G4/repair evidence and context relocation. `resolve --record /var/folders/65/16p4k5qj42qg7l46k2j0nhj40000gn/T/dough-ci-repair-stash-UxPGxe/record.json --reviewed-oid 266306cfa8064e45118b76c6581083a63bbcb2fd` returned resolved/reviewed; subsequent helper drop returned exact OID266306cfa8064e45118b76c6581083a63bbcb2fd, stash@{0}. Unrelated entries remain preserved. No second apply or manual record edit occurred.
- Recovery regression: `./scripts/run.sh pnpm test:ci-repair-stash`3/3 real disposable Git CLI cases, including both merged contents, legacy review, refusal gates and unrelated stash preservation. `./scripts/run.sh pnpm test:ci-proof`25/25 reads actual command/YAML/report inventory and preserves14-part completeness. Independent refactor extracted coherent resolution validation and restoration guidance; current CI summary docs and producer inventory follow the new retained regression. All affected files under250, whitespace passes; traces `/tmp/seed119-stash-recovery-consumers.md` and `/tmp/seed119-refactor-stash-recovery-consumers.md`. App proof unchanged.
- Previous observer's stopped receipt and PID exit permit rearm: current GitHub ci.yml execution-branch observer coordinatornao-seed119, directory `/tmp/dough-ci-501/watch-WW45zC`, PID6914, session96533, cell `g916:5`. c371659ac7121d1e83128c616fa1c0b391ac0867 registered. Re-delivered fa51/run42762324814/1 is the same known scalar failure already repaired/proved in c371; no nested repair. Source freshness unchanged at adc9f4ed41ac06af34c037851c8dce7682461b04; new SEED121 preparation only, no app/named-sibling overlap.
