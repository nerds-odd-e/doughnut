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

## Eight-leaf refinement

Retained verbatim from the plan concern review after Behavior7 publication
`8ab86f53f02c4cdb243ff0a2b34567d3a7f6c8a8`, 2026-11-10.

## Concern review
Original URL slice 2's overrun and parked work remain in the evidence. Before
remaining removal work, a read of the 248-line editing owner exposed necessary
structural preparation: extract its existing removal command without splitting
warning from mutation. Add Structure 6 immediately before Behavior 7; final-entry
policy becomes 8. Completed 1–5 and all accepted proof remain unchanged. Eight
leaves use one focus/history model; no scope, PFE, ADR or North Star change.
Ready after reassessment; no further story split or overrun reset is implied.

## Retained architectural reuse

- PFE: reuse `PropertyFocus` and assimilation key/value; recreated index IDs
  cannot supply persistent identity. Add no competing focus or row-ID system.
- Follow [ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md),
  [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
  and [ADR 0005](../../../docs/adrs/0005-web-routes-accepted.md), with no conflict.
  Ordinary saving retains North Star's accepted-change owner; no new direction.

## Slice 1 acceptance

- Mounted `noteProperty` editable/read-only/specialized scalar, complete-note and just-review triggers now observe the existing value frame's tint, 3px leading edge and positive/inset geometry through `expectFocusedPropertyValue`. The editor also opens its real value dialog; full consumer suites exercise retained links, reify, edit, removal, learning and touch controls. Source commitments 9–13, 15, 34 and 74–77 are compared individually in the table above.
- The plan's seven-file affected baseline passed 41 tests; both listed `vue-tsc --noEmit` and `tsc --noEmit` commands passed. `./scripts/run.sh pnpm frontend:test` passed 399 files/2470 tests (77.07s), including the final edit-dialog and single existing scalar-field observation. All use real mounted rendering and mocked SDK HTTP boundaries. `scripts/check_diff_whitespace.sh` passed.
- Consumer trace: `node .agents/skills/dough-execute-plan/scripts/consumer-trace.mjs --checkout /Users/terryyin/git/doughnut/.worktrees/highlight-and-link-to-a-specific-property-item --base add1cf80f5ef58b668ea31fe6b1aa6b183b4fd57 --term property-value-frame --term data-property-value-focused --term bg-primary/10 --term rich-note-property-row`; complete located-consumer dispositions retained at `/tmp/seed119-slice1-consumer-trace.md`. Frontend consumers ran; incidental planning/unrelated-class matches excluded; E2E selector contracts inspected with equivalent mounted control/layout proof, real app journey owned by slice 2.
- Separate removal reading: exact retired expression `rounded bg-primary/10 ring-1 ring-primary/30` has no matches across `frontend backend cli e2e_test packages docs .agents`. Its wrapper-class assertions were replaced; `data-property-focused` remains for expansion/location. Actual diff exposes no exclusive helper/dependency to retain. No permanent absence assertion was added.
- Freshness unchanged at trunk `fca96ef9073f5c0094fa1fe72c406fbe67fa55a7`, base claim `add1cf80f5ef58b668ea31fe6b1aa6b183b4fd57`; intervening trunk only requeued unrelated SEED-112, no product change from named siblings SEED-117/120. Full frontend proof launched before coordinator settlement (sequencing deviation); unchanged source and absent product changes preserve its result.
- Implementation used about 6–7 active minutes plus proof wait; same cohesive scalar-frame change, no accumulating correction or scope change. Host prelaunch load 5.96/10.66/10.85 with 65% free memory, no coordinator-owned competing proof; sequence later heavy checks and reassess contention before launches.
- Independent refactor `/root/refactor1` completed: unified scalar-frame attributes in `RichFrontmatterPropertyRow.vue`, simplified wrapper classes and scoped the shared SCSS with `@use`; row is 242 lines. No components/API generation. Root inspected actual changed bindings and unchanged observation locations; prior full-suite learning/reify/removal/link proof stays applicable.
- Refactor proof passed 6 files/33 tests: `./scripts/run.sh pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyLocation.spec.ts tests/components/form/RichMarkdownEditor.readOnlyProperties.spec.ts tests/components/form/RichMarkdownEditor.propertyEditingLayout.spec.ts tests/components/form/RichMarkdownEditor.propertyTouchTargets.touch.spec.ts tests/notes/NoteTextContent.properties.spec.ts tests/pages/RecallPage.justReview.spec.ts`; both plan typechecks and `scripts/check_diff_whitespace.sh` passed. Reobserved actual scoped tint/edge/geometry, scroll/controls, read-only, draft layout and coarse-pointer targets. Trace `/tmp/seed119-refactor1-consumers.md` disposes every located consumer; no uncovered behavior.
- Slice 1 delivery accepted `3349feafb0163ea7c0a72d54562b5638a9fa1b52` on `refs/heads/codex/highlight-and-link-to-a-specific-property-item`, suffix base `add1cf80f5ef58b668ea31fe6b1aa6b183b4fd57`; guarded fresh formatting and check-only commit hook passed, tree clean. Managed delivery reused observer `/tmp/dough-ci-501/watch-HpTDpK`. This accepted SHA is the next increment's previously published base.

## Slice 2 prerequisite

- Before implementation edits, `./scripts/run.sh pnpm cy:run --spec e2e_test/features/note_topology/note_property.feature` passed 10/10 scenarios (21s browser time after isolated startup), at this checkout's application origin `http://127.0.0.1:57180`. The existing named property URL journey loaded its note, shared chrome, query, read-only view and rename. Runner-owned isolated services terminated through the normal wrapper. This fulfills the bounded state-changing prerequisite; item lookup/reload remains new slice proof.

## Oversized URL slice and refinement

- Original slice 2 used about 10–12 active minutes plus explicit check/stack waits. The sizing assumption that selector preparation, routing, rendering and proof would fit one 5–8 minute leaf failed. All 14 tracked and 4 untracked attempt-owned product/test paths were safely parked; no staged/unowned work was changed. Published slice 1 and its proof remain intact.
- Park: `/var/folders/65/16p4k5qj42qg7l46k2j0nhj40000gn/T/seed119-slice2-parked-3eixqvni/manifest.json`, base `3349feafb0163ea7c0a72d54562b5638a9fa1b52`; adjacent `tracked.patch` and `files/` preserve exact bytes and per-file SHA-256. Restore only each refined slice's subset; keep matching completed proof, never restore over unrelated edits.
- Completed original-attempt proof: five focused route/editor/page suites passed 41 tests; the item-location file then passed 9 including G1's positive scalar-frame observation. The nullable classifier route-name type mismatch was corrected before parking, but final typechecks are pending. Post-change inbound/reload E2E is pending. Trace `/tmp/seed119-slice2-consumers.md` records consumers and these gaps.
- Required broad command `./scripts/run.sh pnpm frontend:test --maxWorkers=2` failed after 275.37s, 255/402 files and 1664 passing tests, with `VITEST_BROWSER_CONNECTION_CLOSED` / RPC `createTesters` closure while starting unchanged `ConversationComponent.exclusion.spec.ts`; no assertion failure. This verdict remains incomplete. Source `frontend/orchestratorGcReporter.ts` describes this exact orchestrator accumulation failure class; `frontend/vitest.config.ts` enables its GC mitigation, sequential files and sharding for CI. No affirmative OS crash evidence was found.
- Bounded proof recovery for Behavior 3: use the existing CI-mode GC reporter, retries explicitly off, and two complete disjoint full-suite shards, sequenced with E2E and current host headroom checks. This changes the known runner conditions rather than repeating the failed run. If the same failure recurs, diagnose/stop; no retry-until-green or waived verdict. Host at 11:53: load 20.18/23.26/18.21, 56% free memory, no coordinator-owned competing proof; unrelated load remains with its owners.
- Cumulative design review: the exact selector is one domain algorithm, immediately consumed by the route-to-row Behavior. Structure 2 adds no live route/rendering change; Behavior 3 restores compatible work and owns G1/G2 and all URL proof. Later interactions, assimilation, occurrence removal and final-entry policy remain cohesive and unchanged. No story-boundary escalation or resplit threshold is reached; this is the first oversized slice.

## Refined execution continuation

Refinement was records-only, safely parked and refined, with the hard limit applied; seven slices remain Ready after actual reassessment of the unchanged story and revised plan. Accepted publication `9d9a5562ab1ebae56a5531a33551279a8532b612` on the remote execution branch, suffix base `3349feafb0163ea7c0a72d54562b5638a9fa1b52`, is the next increment base. Formatting no-op/check-only hook passed; managed observer remains `/tmp/dough-ci-501/watch-HpTDpK`. No second Take, preparation assignment or workspace was created.

## Structure 2 acceptance

- Added only the stable `PropertyItemFocus` / `propertyItemIndex` contract in the existing value owner; actual diff leaves prior exports and all public rendering/routes unchanged. Its immediate next consumer is Behavior 3; the inverse helper remains parked until selection needs it.
- `./scripts/run.sh pnpm frontend:test tests/utils/notePropertyItemFocus.spec.ts tests/components/form/RichMarkdownEditor.propertyLocation.spec.ts` passed 2 files/19 tests: nine supplied-array/selector cases observe exact indices for reordering, duplicate occurrences, blank/spaced strings and unresolved selectors; real mounted editor assertions retain scalar/read-only/specialized frame geometry, scroll, value-dialog edit, rename and removal. Both plan typecheck commands and `scripts/check_diff_whitespace.sh` passed on the same content.
- Consumer trace `/tmp/seed119-occurrence-identity-consumers.md` from the installed script against `9d9a5562ab1ebae56a5531a33551279a8532b612` supplies `PropertyItemFocus` / `propertyItemIndex` terms and every consumer's run/excluded disposition. Unchanged exports' consumers do not gain a new runtime effect. G1/G2/G3 remain with Behavior 3, including the failed broad verdict.
- About 3 active minutes plus checks; freshness unchanged at pinned trunk `fca96ef9073f5c0094fa1fe72c406fbe67fa55a7`, with the same unrelated queue-only incoming change and no named sibling product impact.
- Independent `/root/refactor_identity` completed: none—already clean; no edits or invalidated proof, so no test rerun. Shared resolution is justified by the immediately following focus/rename Behavior; utility/test files are 146/18 lines.
- Delivery-time freshness advanced to pinned trunk `dca7d78be57181ccdd154e130d9c386c3e82955b`: only SEED-112's Take/profile landed since the earlier pin. No product/sibling overlap; accepted proof remains applicable.
- Structure 2 delivery accepted `a02b00da566b28f42f795cbe53d32bbe7d95245f` on the remote execution branch, suffix base `9d9a5562ab1ebae56a5531a33551279a8532b612`; formatting/check-only hook and observer reuse passed. It is the next increment's published base. Behavior 3 restores other parked paths while retaining this helper and its test.

## Behavior 3 proof continuation

- Restored 17 compatible parked paths after manifest/hash/base checks, preserving delivered utility/test and coordinator records. G2's real mounted transition from a resolved list URL to a scalar replacement failed for the intended reason in editable/read-only cases (one substituted selected value); the existing frame predicate was corrected and the same cases passed, retaining readable scalar, explicit feedback and URL. Five focused suites then passed 44 tests and both typechecks passed; row 245 lines.
- First post-change `./scripts/run.sh pnpm cy:run --spec e2e_test/features/note_topology/note_property.feature` failed 10/11: the new exact-duplicate focus check passed, then the sidebar assertion expected one note while Background plus scenario author two (`Vitamins`, `Activities`). Correct the expected table to both notes, retaining shared chrome/count proof; rerun the feature under the corrected expectation. Reload and full-shard verdicts remain pending in G3.
- Corrected browser feature then passed 11/11 (22s), including actual reload and both sidebar notes; failed/corrected logs `/tmp/seed119-behavior3-e2e.log` and `/tmp/seed119-behavior3-e2e-corrected.log`. Full CI GC/retry0 shard 1 passed 202 files/1239 tests (129.80s); shard 2 failed eight `PropertyAssimilationModes.spec.ts` cases while 200 files/1252 tests passed (137.30s). This is a separate repository regression: the existing props-driven learning renderer gained a router requirement (`route.meta` TypeError), not an orchestrator recurrence; earlier conversation failure file passed. G3 retains the strict failed verdict, log `/tmp/seed119-behavior3-frontend-shard2.log`.
- Bounded correction keeps the learning renderer route-agnostic: existing route-aware panel/read-only owners resolve the index through the same shared focus getter and pass an optional result prop. Standalone mode tests, mounted focus consumers and both typechecks must pass, followed by both strict shards on corrected content. No assertion is weakened and no second focus store or product scope is introduced.
- Final correction-focused command `./scripts/run.sh pnpm frontend:test tests/components/recall/PropertyAssimilationModes.spec.ts tests/components/form/RichMarkdownEditor.propertyItemLocation.spec.ts tests/components/form/RichMarkdownEditor.readOnlyProperties.spec.ts tests/pages/NoteShowPage.propertyItem.spec.ts tests/components/form/RichMarkdownEditor.propertyLocation.spec.ts` passed 5 files/33 tests, including all eight unchanged standalone renderer cases and actual parent-wired focus/page/scalar-replacement observations. Both plan typechecks passed on final production content.
- Final corrected-wiring `./scripts/run.sh pnpm cy:run --spec e2e_test/features/note_topology/note_property.feature` passed 11/11, refreshing actual inbound second-duplicate/reload/sidebar proof (35s browser time). Final `CI=true ./scripts/run.sh pnpm frontend:test --shard=1/2 --maxWorkers=2 --retry=0` passed 202 files/1239 tests (149.13s); `CI=true ./scripts/run.sh pnpm frontend:test --shard=2/2 --maxWorkers=2 --retry=0` passed 201 files/1260 tests (192.27s). Combined complete strict verdict: 403 files/2499 tests, no assertion retries. Root inspected both terminal log tails at `/tmp/seed119-behavior3-frontend-final-shard{1,2}.log`; failed attempts remain distinguished above, not ignored.
- Final consumer record `/tmp/seed119-restored-item-consumers.md` has every consumer disposed and concrete setup/assertions. Root inspected the actual route/helper/focus/rename/read-only/scalar transition code and tests, plus classifier→unchanged ordinary-anchor handler→`router.push` flow. All affected frontend consumers ran in the final shards; real browser integration ran; incidental token matches and unchanged separate backend/Portable contracts are excluded with reasons. No unresolved consumer remains.
- G1/G2/G3 now have proved dispositions with their original reporting provenance retained. Behavior 3 used about 8–9 active minutes plus declared check/stack waits; no scope extension. Latest source freshness is unchanged at `dca7d78be57181ccdd154e130d9c386c3e82955b`; same named-sibling no-product-impact judgment applies. No component/API generation trigger. All owned subprocesses/runner services are terminal; whitespace passed.
- Independent `/root/refactor_item_urls` completed: used Vue iteration index directly and found the rename target once in their existing owners. Root inspected these two local equivalences; public focus prop, authored order/keys/attributes/actions, selector/query and server navigation boundaries are unchanged. Invalidated component/rename observations were refreshed by `./scripts/run.sh pnpm frontend:test tests/components/recall/PropertyAssimilationModes.spec.ts tests/components/form/RichMarkdownEditor.propertyItemLocation.spec.ts tests/pages/NoteShowPage.propertyItem.spec.ts` (3 files/20 tests), both typechecks and whitespace; all passed. Full strict shards and E2E are retained for unchanged boundaries, with every refactor consumer disposed in `/tmp/seed119-refactor-item-consumers.md`. No generation or gap remains.
- Delivery freshness pin `b490903d12f4b18866985218795fbfe54fbb27fb` adds SEED-112's CI proof-count publication/closure. Root inspected attributable workflow/package/Biome changes: reporting units and script proof only; app sources, route behavior, dependencies and frontend test configuration are unchanged. Named SEED-117/120 did not land. Local proof remains applicable; eventual trunk integration must observe its real updated CI workflow.
- Guarded formatting stopped before commit/publication on Biome's mechanical `useTemplate` finding in the round-trip spec. Root replaced pathname string concatenation with its equivalent template literal; URL inputs/results and accepted proof are unchanged. Formatting preparation is retried because that repair invalidated the failed preparation, with the same owned paths and published base.
- Behavior 3 publication accepted `6f5213de99f21270e450a3941fedc8b5409f131d` on the remote execution branch, suffix base `a02b00da566b28f42f795cbe53d32bbe7d95245f`. Retried guarded formatting/check-only hook passed; managed observer reused. Tree was clean at handoff; this SHA is the next increment base.

## Behavior 4 acceptance

- Actual chevron, physical background hit and native Enter now choose the shared raw text/occurrence destination, replacing history/query. Tests observe first/second equal values, editable/read-only blank ineligible items and empty-list property-only expansion. Transparent sibling button, native focus indicator and anchor/button exclusion preserve one visible selection treatment and independent learning/link/edit controls; no local selection state.
- `./scripts/run.sh pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyItemSelection.spec.ts tests/components/form/RichMarkdownEditor.propertyItemSelection.touch.spec.ts tests/components/form/RichMarkdownEditor.listPropertyLayout.spec.ts tests/components/form/RichMarkdownEditor.listPropertyMemoryTracking.spec.ts tests/components/recall/PropertyAssimilationModes.spec.ts tests/components/form/RichMarkdownEditor.propertyTouchTargets.touch.spec.ts` passed 6 files/30 tests. Additional `./scripts/run.sh pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyLocation.spec.ts tests/components/form/RichMarkdownEditor.propertyItemLocation.spec.ts` passed 21; `./scripts/run.sh pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyItemSelection.touch.spec.ts tests/components/form/RichMarkdownEditor.propertyListWikiLinks.spec.ts` passed 10. Source rows above identify actual native keyboard, mouse, geometry and retained controls at their mounted boundaries.
- Both listed frontend typecheck commands and whitespace passed. `./scripts/run.sh pnpm cy:run --spec e2e_test/features/note_topology/note_property.feature,e2e_test/features/assimilation/assimilation_property_order.feature,e2e_test/features/recall/property_memory_tracker.feature` passed 3 features/15 scenarios (11/1/3), 57s browser time at57180 with runner teardown. No E2E expectation was changed.
- Initial coarse deferred-load observation changed row height56→68px; reserving the action container's44px height corrected it and the real pointer hit test passes. Initial fixture typing used unsupported `Promise.withResolvers`; equivalent controlled Promise repaired typing without a library/production change. Failed observations remain distinguished from final pass.
- Trace `/tmp/seed119-selection-consumers.md` disposes every consumer with concrete setup/assertion locations, including noteRichPropertyMethods/layout/assimilation helpers. Off-note expansion and injected-context capability boundary were inspected unchanged. About9 active minutes plus checks; no new component/API generator trigger or remaining gap.
- Freshness unchanged at `e3dc390258fa6c26d59b8706254be3ced61d34f1`, no named sibling product overlap. Incoming checker1.0/lockfile changes are outside this branch's current locked setup; closure must refresh and validate the integrated dependency graph. Prelaunch12:53 load5.81/6.84/8.41,62%free; one owned batch, all subprocesses terminal.
- Independent `/root/refactor_selection` completed: first-item chevrons call the same selection action, and new mock cleanup moved to `afterEach`. Root inspected preserved scalar/empty/closing/off-note branches and unchanged public destinations/query semantics. `./scripts/run.sh pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyItemSelection.spec.ts tests/components/form/RichMarkdownEditor.propertyLocation.spec.ts tests/components/form/RichMarkdownEditor.propertyItemLocation.spec.ts` passed 3 files/25 tests after refactor; both typechecks and whitespace passed. Touch/link/learning/E2E boundaries remain unchanged and reused; every refactor consumer is disposed in `/tmp/seed119-refactor-selection-consumers.md`. No generation/gap.
- Delivery freshness pin `55ae0d07ab6a02ca0d30529c3d9337ef0ad43364` additionally bumps pnpm12.18→12.19 in manifests/Nix/lockfile. Root inspected these attributable toolchain changes; no app/named-sibling behavior changed in this execution checkout. Keep current locked proof for branch delivery; refresh Nix/locked dependencies and validate the combined toolchain at final integration.
- Guarded formatting stopped before commit/publication because Biome permits Vue `:deep` only inside an SFC, while the touch selector was in external SCSS. Independent refactor continuation moved the whole coarse-pointer block unchanged into the same component's inline scoped style, leaving base SCSS external. Root inspected identical selectors/heights; `./scripts/run.sh pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyItemSelection.touch.spec.ts tests/components/form/RichMarkdownEditor.listPropertyLayout.spec.ts tests/components/recall/PropertyAssimilationModes.spec.ts` passed 3 files/18 tests plus whitespace, retaining actual44px/loading hit targets. Script/types/public boundaries unchanged, prior type/E2E proof reused; trace `/tmp/seed119-refactor-selection-style-consumers.md`. Retry formatting is required by this repair; no older HEAD is delivered.
- Behavior 4 publication accepted `dd9bb5f16310a48ba9de45f124000b28142f9f3e` on the remote execution branch, suffix base `6f5213de99f21270e450a3941fedc8b5409f131d`. Repaired formatting/check-only hook passed; observer reused and tree clean. Next increment base is this accepted SHA.

## Continued observer after the cache counterexample

Previous execution completion88061832511d83ae3b7cc0197c827bfd57d145de received exact success run42774144407/1 with confirmed shutdown. The reopened original outcome uses a fresh execution-branch observer, same coordinatornao-seed119: `/tmp/dough-ci-501/watch-nNP8bS`, PID18086, session30625, cellg916:145. No prior observer remains active; no landing or cleanup occurred. The nine-leaf readiness review retains the original story outcome and the isolated wrong-tracker reproduction.
