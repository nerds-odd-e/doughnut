# Highlight and link to a specific property item

**Source:** [selected story](../../seeds/SEED-119-highlight-specific-property-item.md#highlight-and-link-to-a-specific-property-item)
**Source review:** [source review](source-review.md)
**Identity:** SEED-119#highlight-and-link-to-a-specific-property-item
**Execution context:** [established context](proof-context.md#execution-context)
**Proof context:** [premises and observations](proof-context.md#premises-and-observations)
**Trace and seams:** [contract](proof-context.md#consumer-trace-and-structural-seams)

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
- [Retained PFE and ADR decisions](proof-context.md#retained-architectural-reuse) remain unchanged.

## Proof ownership

| Final promise | Owning slice | Observable proof |
| --- | --- | --- |
| Scalar/specialized/read-only/context value focus and retained controls | 1 | Mounted editor, complete note and just-review context; value-frame geometry/tint and real controls |
| Exact URL, reload, duplicates, reorder, stale item, authored key, query, shared note chrome | 3 | Route round-trip plus mounted page and real inbound/reload E2E |
| Chevron first item; row/background and keyboard selection; shared focus for consumers | 4 | Mounted editor actions change route and selected value; complete authored rows remain usable |
| Value links and learning actions retain their destination; narrow/touch controls | 4 | Real browser click/keyboard/geometry tests including tracker-loading state |
| Assimilation's exact value opens first matching occurrence | 5 | Production-router assimilation boundary and notebook workspace continuation |
| Warn/cancel preserves content/history/focus; confirm removes exact item/history and preserves distinct siblings | 7 | Mounted note save boundary plus real persisted reload/tracker E2E |
| Selected removal clears focus; other removal preserves selected occurrence | 7 | Mounted editor route/content, including earlier equal occurrence removal |
| Final removal deletes entry and its key's history, then goes to note | 8 | Mounted note save boundary and real persisted final-entry save/reload journey |

## Ordered slices

Interim: slice 7 may save the final removal as `[]`; slice 8 deletes the entry. Ordinary authoring remains usable.

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
Accepted implementation and proof: [retained evidence](source-review.md#structure-2-acceptance).

### 3. Return to an exact list occurrence from its URL
Type: Behavior
Status: done
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
Status: done
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
Status: done
Accepted implementation and proof: [retained evidence](source-review.md#behavior-5-acceptance).

### 6. Keep removal coordination in one editing owner
Type: Structure
Status: done
Accepted implementation and proof: [retained evidence](source-review.md#structure-6-acceptance).

### 7. Remove an occurrence with informed history deletion
Type: Behavior
Status: done
Behavior: Minus with matching history → warning/cancel; cancel preserves the
saved values/history/focus, confirm removes one occurrence and targeted history.
Distinct siblings retain order/history; selected removal clears item selection,
other removal retains the selected row and recalculates its text/occurrence
selector, including removal of an earlier equal row. Equal values share history;
explain that loss in the warning. Replace list whole-property minus with item
minus and delete its exclusive dependencies; preserve scalar whole-entry removal.
Keep warning, deletion, content update and focus transition in one operation;
extend guard matching instead of copying the whole-key deletion loop. A failed
tracker-info load or DELETE must stop content/focus mutation with existing error
feedback; unknown history is not an empty result. Preserve shared loading.
Proof: Mounted note/editor with real popup stack and mocked SDK boundary;
assert warning text, cancellation state, accepted target tracker IDs and saved
Markdown. Cover shared duplicate history, earlier-equal-row removal and scalar
removal. Real E2E cancels/confirms assimilated-item removal, reloads persisted
values and observes surviving sibling learning data.
Sizing: 5–8 minutes including focused proof/cleanup; retain warning and mutation
together. At 10 active minutes only one focused E2E Cancel-query correction,
terminal proof and trace remain; apply the bounded focused-check exception.
Final-entry policy and its separate proof loop move to slice 8.

### 8. Remove the final item as a property entry
Type: Behavior
Status: done
Behavior: Final-item minus → warn/cancel using the existing whole-key history
guard; confirm removes the key and its matching history, then replaces location
with the whole note. Reuse slice 7's operation, changing the empty-result rule;
ordinary value editing still supports authored empty lists.
Proof: Mounted save/route boundary distinguishes Cancel from confirmed key
removal, including existing history for the key. Real E2E removes the final
item, reloads the saved note, and observes whole-note navigation and the retained
other properties. This replaces slice 7's interim empty-list result.
Sizing: about 5 minutes plus focused-check/stack wait; existing whole-key guard
and named note navigation supply the special final-entry rule.

### 9. Keep removal history bound to the edited note
Type: Behavior
Status: done
Behavior: Cancel item removal, then reuse the editor for another note → its
history guard fetches current matching trackers for each action. Share only
pending reads for the same note; completed or prior-note data cannot satisfy
a later removal or rename after history changes.
Preserve exact key/value and whole-key matching, cancellation and shared errors.
Proof: Mounted NoteTextContent switches notes after Cancel; current tracker22
is deleted while prior-note tracker11 remains; new same-note history after
Cancel is warned/deleted too. Run reached guard/edit/save
consumers and both types; renew complete retryOFF frontend shards after this
cache-contract correction. Keep the accepted18E2E boundaries when unchanged.
Sizing: about3–5 active minutes; one cache-identity correction, no new API/state
store or generic synchronization protocol. Focused-check waits are excepted.

## Commands and acceptance
[Required commands and acceptance readings](proof-context.md#commands-and-acceptance)
remain the proof/delivery contract; use their focused forms for each slice.

## Concern review

[Retained refinement rationale](proof-context.md#eight-leaf-refinement) plus
G8 supports nine leaves; matching-history completion remains open in slice9.

## Story obligations

### G1. Resolved list item marker (first reported in original slice 2)
Reported: slice 2 — "Coordinator inspection found the scalar action row index -1 matched unresolved selectedItemIndex -1, marking a learning action as a focused item."
Story clause: "Scalar and list values share the presentation responsibility while retaining their different editing and learning semantics."
Disposition: proved by slice 3: RichMarkdownEditor.propertyItemLocation.spec.ts single existing scalar frame and editable/read-only exact duplicate; unchanged standalone PropertyAssimilationModes 8 cases plus complete final 403-file/2499-test retry0 shards pass with optional parent-resolved index
Commitment: 73
### G2. Stale list destination after scalar replacement (first reported in original slice 2)
Reported: slice 2 — "Slice 2 inspection found that replacing an addressed list property with a scalar shows unresolved-item feedback but still highlights the scalar through property-level focus."
Story clause: "It does not substitute another value."
Disposition: proved by slice 3: RichMarkdownEditor.propertyItemLocation.spec.ts keeps a scalar replacement unresolved, editable/read-only; red reproduced one substituted selection, corrected shared-frame predicate gives explicit feedback/readable scalar/zero selections/unchanged URL; final focused, E2E and full shards pass
Commitment: 26
### G3. Complete URL journey proof (first reported in original slice 2)
Reported: slice 2 — "The full frontend run failed on a closed browser connection after 1664 passing tests; final typechecks and post-change inbound/reload E2E remain pending."
Story clause: "Opening or reloading that URL expands the property and highlights that occurrence."
Disposition: proved by slice 3: final note_property.feature 11/11 observes actual inbound second duplicate plus browser reload and both sidebar notes; both typechecks and final disjoint CI GC retry0 shards pass 403 files/2499 tests on corrected wiring, including prior 8 standalone failures; /tmp/seed119-restored-item-consumers.md proof chronology
Commitment: 23


### G4. Scalar assimilation uses its property destination
Reported: slice 5 — "CI run 42762324814 attempt 1 showed the scalar Skip continuation opening /n6/p/topic/i/1/~ instead of /n6/p/topic because the empty-string scalar sentinel was classified as a list item."
Story clause: "A scalar `noteProperty` destination highlights its scalar value."
Disposition: proved by slice 5: scalar empty-sentinel next-unit red reproduced on fa51; corrected production predicate and positive real-router/workspace cases pass in six suites/37 tests; actual assimilation_walkthrough Skip and property_order continuation pass 10/10 E2E, both typechecks; manual blank selection is unchanged and raw nonempty values remain exact
Commitment: 34


### G5. Persisted item removal Cancel journey
Reported: slice 7 — "The initial Behavior 7 browser batch passed three scenarios but its new removal journey could not find Cancel because the query selected the loading dialog instead of the confirmation dialog; corrected collection query and terminal persisted-reload proof are pending."
Story clause: "Cancel keeps content, learning history, focus and URL intact."
Disposition: proved by slice 7: actual confirmation-root Cancel succeeds unforced; canonical mounted save boundary preserves DELETE/save/URL/selected run, then Confirm deletes11 and saves ordered siblings; final property_memory_tracker.feature 4/4 passes actual Cancel/Confirm and persisted reload walk/jog with retained walk history after correcting dialog and expanded-row fixture queries
Commitment: 49


### G6. Ordinary content-save continuation proof
Reported: slice 7 — "CI run 42770072237 attempt 1 at 8ab failed the autosave-after-trash-failure case: expected three updateNoteContent calls, observed two; focused reproduction found missing getNoteInfo data when the edit removes relation frontmatter and the history guard correctly refuses unknown history."
Story clause: "Reuse authored list rendering and ordinary content saving"
Disposition: proved by slice 7: deterministic red on 8ab; real mounted NoteShowPage uses known-empty-history HTTP fixture for frontmatter removal and retains three-save/one-trash expectations plus exact third Second edit payload; five current autosave/trash suites17 tests and both typechecks pass, no production or timing workaround
Commitment: 67

### G7. Final-entry browser journey proof
Reported: slice 8 — "The initial final integrated browser batch passed 17 of 18 scenarios; the new final-item journey used an item-warning helper that expected learning history wording while the correctly reused whole-key guard displayed its existing property memory-tracker warning."
Story clause: "When that is the final item, remove the entire property entry and return to the whole-note location."
Disposition: proved by slice 8: explicit final-item helper observes existing whole-key warning with unforced confirmation; final integrated batch18/18 reaches whole note, reloads retained source and deleted entry; quoted authored-key mounted case proves Cancel, targets11/12/13, exact save and noteShow/query; both combined types and final disjoint retryOFF frontend shards407files2514tests pass
Commitment: 44


### G8. History cache follows the edited note
Reported: slice 9 — "After the initially completed review, an isolated mounted editor check canceled removal on note1, reused the editor for note99 and confirmed removal; expected current-note tracker22 but the guard deleted cached prior-note tracker11. The matching-history outcome is therefore unfinished despite the earlier completion marker."
Story clause: "Confirm removes the selected content and this learner's matching trackers through existing tracker-deletion semantics."
Disposition: proved by slice 9: three real mounted regressions delete current22 with exact note99 save, withhold delayed prior-note history and warn/delete newly added same-note33 after Cancel; both defects reproduced red, pending-only read lifetime corrected;45 focused tests, both types and complete retryOFF408files2517tests pass; unchanged18native E2E boundaries retained
Commitment: 50

## Execution complete

Product advice: no change; original matching-history outcome is proved after the note/action read-lifetime correction.
