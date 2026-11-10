# Highlight and link to a specific property item

**Source:** [selected story](../../seeds/SEED-119-highlight-specific-property-item.md#highlight-and-link-to-a-specific-property-item)
**Source review:** [source review](source-review.md)
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
| Exact URL, reload, duplicates, reorder, stale item, authored key, query, shared note chrome | 3 | Route round-trip plus mounted page and real inbound/reload E2E |
| Chevron first item; row/background and keyboard selection; shared focus for consumers | 4 | Mounted editor actions change route and selected value; complete authored rows remain usable |
| Value links and learning actions retain their destination; narrow/touch controls | 4 | Real browser click/keyboard/geometry tests including tracker-loading state |
| Assimilation's exact value opens first matching occurrence | 5 | Production-router assimilation boundary and notebook workspace continuation |
| Warn/cancel preserves content/history/focus; confirm removes exact item/history and preserves distinct siblings | 6 | Mounted note save boundary plus real persisted reload/tracker E2E |
| Selected removal clears focus; other removal preserves selected occurrence | 6 | Mounted editor route/content, including earlier equal occurrence removal |
| Final removal deletes entry and its key's history, then goes to note | 7 | Mounted note save boundary and real persisted final-entry save/reload journey |

## Premises and observations

[Dated preparation observations and decisive premises](source-review.md#premises-and-observations)
are retained verbatim in the acceptance evidence. The bounded existing
`note_property.feature` probe passed before the parked URL implementation; reuse
that matching observation before restoring Behavior 3.

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
slice 4, and assimilation can open key-only until slice 5. Slice 6 can save an
emptied list as `[]`; slice 7 replaces that with final-entry deletion. These
states keep ordinary authoring usable and add no rejection or disabled action.

### 1. Focus the value area consistently
Type: Behavior
Status: done
Behavior: Open a scalar property (including specialized and read-only/context
views) → its existing value has shared tint/leading edge and remains operable.
Move the visual responsibility from the property wrapper to the value frame;
retain expansion, focus identity, refs, scrolling and missing-property behavior.
Proof: Mounted property-location and complete-note/context examples observe
value-frame styling/geometry and usable edit/link/learning controls. Run the
affected baseline; replace old presentation proof with these observables.
Sizing: about 5 minutes including checks/cleanup for one presentation rule;
no new scalar rendering or editing model.

### 2. Establish exact occurrence identity
Type: Structure
Status: done
Internal change: Add the shared exact-text/one-based-occurrence type and pure
index/selector conversion in the existing property-value owner. Preserve all
external rendering, routes and editing behavior; register no item URL yet.
Enables immediately following Behavior 3, which consumes this single identity.
Proof: Stable domain algorithm examples cover ordered distinct values, duplicate
occurrences, blank/exact whitespace text and an unresolved selector; existing
scalar property-location proof and both typechecks preserve external behavior.
Sizing: about 3–5 minutes using the parked implementation's compatible utility;
no routing, rendering, new selection state or generic identity framework.

### 3. Return to an exact list occurrence from its URL
Type: Behavior
Status: planned
Behavior: Open/reload a named item URL → expand and highlight exact text plus
occurrence; stale selector → explicit unresolved feedback with readable note.
Reuse the passed bounded early probe. Extend route metadata and component mapping,
named helpers, focus/follow-location parsing, item-row frame and note-family
classifier together. Preserve query/chrome, exact authored key and rename.
Treat a list changed to scalar as an unresolved item without selecting its new
value. Expose the same resolved item focus for later consumers. Value changes/reorder
must exercise one text/occurrence rule, not another selector representation.
Proof: Named round-trip and mounted page examples for reload-equivalent fresh
mount, distinct reorder, duplicate occurrence, stale text and preserved query;
real inbound URL/reload scenario in existing `note_property.feature`. G1/G2
and the failed broad-run proof are owned here; final typechecks must pass.
Sizing: about 5 active minutes restoring compatible parked work and correcting
one scalar-replacement focus predicate. Reuse matching focused proof; rerun
invalidated observations. Check/stack waits remain exceptions. Recover broad
proof through the existing CI GC reporter and bounded shards with retries off;
record every shard's terminal verdict, never repeat an unchanged failing run.

### 4. Select a list item while reading or operating its row
Type: Behavior
Status: planned
Behavior: Open chevron → first authored item destination; choose another row by
background or keyboard → that row's location/highlight replaces it. Existing
value links and learning controls still execute their own actions.
Use the slice 3 focus/route rule. Empty/property-only destinations represent
expanded lists without selection. Keep all authored occurrences available,
including rows ineligible for assimilation; retain existing off-note expansion.
Proof: Mounted editor interactions, duplicate/blank/long representative data,
real link/tracker clicks and narrow/coarse-pointer geometry/loading stability.
Sizing: about 5 minutes; no separate local selection model on note routes.

### 5. Continue assimilation at the addressed value
Type: Behavior
Status: planned
Behavior: Next assimilation unit includes key/value → push its item destination
and highlight first exact occurrence; a property-only unit opens its property.
Reuse the shared named helper and current count/toast/dismiss behavior.
Proof: Extend `useGoToNextAssimilation.spec.ts` using the production router and
the notebook workspace continuation; exercise the actual returned value.
Sizing: about 5 minutes including the focused existing suite.

### 6. Remove an occurrence with informed history deletion
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
together. Final-entry policy and its separate proof loop move to slice 7.

### 7. Remove the final item as a property entry
Type: Behavior
Status: planned
Behavior: Final-item minus → warn/cancel using the existing whole-key history
guard; confirm removes the key and its matching history, then replaces location
with the whole note. Reuse slice 6's operation, changing the empty-result rule;
ordinary value editing still supports authored empty lists.
Proof: Mounted save/route boundary distinguishes Cancel from confirmed key
removal, including existing history for the key. Real E2E removes the final
item, reloads the saved note, and observes whole-note navigation and the retained
other properties. This replaces slice 6's interim empty-list result.
Sizing: about 5 minutes plus focused-check/stack wait; existing whole-key guard
and named note navigation supply the special final-entry rule.

## Commands and acceptance
[Required commands and acceptance readings](source-review.md#commands-and-acceptance)
remain the proof/delivery contract; use their focused forms for each slice.

## Concern review
Original slice 2 overran at 10–12 active minutes plus check/stack waits; its
owned product changes are parked with hashes in the acceptance evidence.
Split hidden exact-occurrence preparation into Structure 2 immediately before
Behavior 3. Retain completed 1 and later selection/assimilation/removal outcomes
as 4–7. Seven slices use one exact-text/occurrence rule; no source scope, PFE,
ADR or North Star decision changes. Implementation and proof already completed
remain reusable only at matching boundaries. Ready after reassessment.

## Execution context

- Established start resumed 2026-11-10 by Nao-chan; source kind planned, story-branch mode.
- Owned execution checkout: `/Users/terryyin/git/doughnut/.worktrees/highlight-and-link-to-a-specific-property-item`; branch `codex/highlight-and-link-to-a-specific-property-item`. Existing dashboard-created linked worktree; no second claim.
- Remote `origin`, trunk `main`; increments target `refs/heads/codex/highlight-and-link-to-a-specific-property-item` in `nerds-odd-e/doughnut`.
- Starting revision `99ba3b9ac61db0bd3bc8ac58fe4d4c7ceed493e7`; accepted claim and initial candidate `add1cf80f5ef58b668ea31fe6b1aa6b183b4fd57`. Claim trunk CI is unobserved.
- Checkout setup: `./scripts/run.sh bash scripts/worktree_setup.sh` passed against current lockfile; applicable command `./scripts/run.sh pnpm -C frontend exec vue-tsc --version` passed (6.0.3).
- Observer: GitHub Actions selector `ci.yml`, verified active workflow `donut CI`, push covers all branches. Coordinator `nao-seed119`, directory `/tmp/dough-ci-501/watch-HpTDpK`, PID 69392, session 83266, yielded cell `g824:9`; runtime and launch rooted in this checkout. Target is the remote execution branch.
- Replanning remains authorized by the selected planned workflow; target about 5 minutes, hard scrutiny beyond 10 active minutes, with focused-check and external-stack waits excepted as stated above.
- Delivery hook is the common Git `pre-commit`, invoking check-only `./scripts/run.sh pnpm lint:changed`; coordinator fresh delivery owns selective formatting once. No API generation for existing endpoints; component inventory regeneration only on component addition/deletion.

## Story obligations

### G1. Resolved list item marker (first reported in original slice 2)
Reported: slice 2 — "Coordinator inspection found the scalar action row index -1 matched unresolved selectedItemIndex -1, marking a learning action as a focused item."
Story clause: "Scalar and list values share the presentation responsibility while retaining their different editing and learning semantics."
Disposition: receiving slice 3
Commitment: 73
### G2. Stale list destination after scalar replacement (first reported in original slice 2)
Reported: slice 2 — "Slice 2 inspection found that replacing an addressed list property with a scalar shows unresolved-item feedback but still highlights the scalar through property-level focus."
Story clause: "It does not substitute another value."
Disposition: receiving slice 3
Commitment: 26

### G3. Complete URL journey proof (first reported in original slice 2)
Reported: slice 2 — "The full frontend run failed on a closed browser connection after 1664 passing tests; final typechecks and post-change inbound/reload E2E remain pending."
Story clause: "Opening or reloading that URL expands the property and highlights that occurrence."
Disposition: receiving slice 3
Commitment: 23
