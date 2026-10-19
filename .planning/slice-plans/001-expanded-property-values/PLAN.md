# Expand a property into one navigable authored list

**Source:** [Expand a list property into a single list of clickable values](../../seeds/SEED-064-note-properties-fixes.md#story-7)

**Identity:** SEED-064#story-7

## Goal and scope

A learner expands a list property and reads one ordered detailed list, follows
its authored note labels, and uses the learning controls belonging to each value.
Collapse restores the compact links beside the key. Preserve the key, toggle,
list-wide editing and property operations, authored occurrences, plain/URL
values, and existing wiki resolution and recovery. Display also works in shared
editors without a saved note. The source seed owns the complete examples and
deferred promises; this plan changes presentation, not learning-unit policy.

Preparation checkout: `/Users/terryyin/git/doughnut/.worktrees/expand-a-list-property-into-a-single-list-of-cli`.
Baseline: `51dec4f7b1678c85aa5cff2448c97e54b427aec5`, observed 2026-10-20.
Execution uses this landed plan in its selected checkout; repeat an observation
only when that checkout's content invalidates its boundary.

## Existing solutions and decisions

- PFE: `RichFrontmatterListPropertyValue` already presents authored list items,
  `WikiLinkToken` owns authored labels, resolved note/property destinations and
  pending/dead recovery, and `RichFrontmatterPropertyExternalLink` owns URL
  actions. Reuse these responsibilities rather than adding a link resolver.
  `RichFrontmatterPropertyPanel` and `AssimilationModes` already own the
  property learning controls; preserve their note-level consumers too.
- Display authored `value.items` in their original order. Eligibility and
  tracker identity remain value-based; repeated occurrences refer to the same
  property-key/value unit. The filtered `assimilableListValues` projection must
  not become the source of the visible collection. Keep one key-wide Skip or
  Return operation and preserve tracker links, next-recall feedback and actions.
- Pass existing wiki resolution data, saved Markdown and recovery events to the
  active value renderer. Follow [ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md),
  [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md#links-and-attachments)
  and [ADR 0005](../../../docs/adrs/0005-web-routes-accepted.md). No ADR conflict
  was found. The current North Star topics require no change for this local
  presentation work.
- Keep compact rendering during slices 1 and 2 as an interim state replaced by
  slice 3. Do not introduce a separate model for each example or tracking policy
  for a presentation change. Add structure only if necessary for the immediate
  behavior; no preparatory framework is planned.

## Investigation and decisive premises

All scratch edits and the temporary spec were removed before preparation writes.
These are investigation results, not implementation acceptance.

| Premise and consuming operation | Observation in the baseline checkout | Result and consequence |
| --- | --- | --- |
| Expanding duplicates authored links as non-navigable learning labels | `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.story7Observation.spec.ts`; real editor harness, source note 99, target 42, tracker 12, expand `topics` | Exit 0, 2 tests, 2.84 s. Compact showed `lunar topic`; detail showed the raw property-target token. Its only anchor was View tracker. Reproduces the selected discrepancy. |
| Hiding compact rendering can lose authored content and shared-editor values | Same temporary fixture: property-target alias, two `practice` occurrences, blank, 256-character item, dead and pending links; hide only the compact list while retaining Edit, then `CURSOR_DEV=true nix develop -c pnpm frontend:test` | Exit 1, 69.24 s: 355 files/2214 tests passed and one existing navigation test failed. Detail retained only eligible distinct values; a no-note-ID expansion displayed no values. Slices 2 and 3 must separate display ownership from eligibility and saved-note availability. |
| Existing expanded navigation proof observes the promised detail | Full suite above; inspected `RichMarkdownEditor.propertyListWikiLinks.spec.ts` and `propertiesTestDom.ts` | False: the expanded-source test queries the compact list and fails when it is hidden. Tracking, dialog and generic assimilation tests stayed green despite lost values. Move surviving navigation proof to the detailed item and add positive complete-list proof. |
| Existing wiki rendering works at the expanded consuming label | `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.story7Observation.spec.ts -t 'consumes expanded'`; detail-scoped proof, then temporary renderer/context/event wiring through the panel | Baseline exit 1 (3.03 s), missing detailed link; wiring exit 0 (2.99 s), 1 passed/2 skipped. `lunar topic` reached named `noteProperty(42, 'a part of')`; pending retained source `noteProperty(99, 'topics')`; dead emitted `{portablePath:'Missing', displayText:'shown'}` through the editor. Settles reuse and proof discrimination for slice 1. |
| Selected E2E route is admitted in an isolated checkout | `CURSOR_DEV=true nix develop -c node --input-type=module -e 'import { assertSupportedIsolatedCypressSpecs } from "./scripts/isolated-cypress-spec-selection.mjs"; const specs = ["e2e_test/features/note_topology/property_wiki_link.feature", "e2e_test/features/recall/property_memory_tracker.feature", "e2e_test/features/note_topology/note_property_layout.feature"]; console.log(JSON.stringify(assertSupportedIsolatedCypressSpecs(specs)));'` | Exit 0; private OpenAI mock required, no Wikidata mock. Scenario execution changes the runner-owned isolated test data and was left to slice 1's early probe. No E2E pass is claimed by this preparation. |

The real editor harness mocks only backend HTTP. The scratch fixture supplied
wiki resolution/tracker data; it observed browser navigation and recovery, not
server resolution or tracker persistence. Existing E2E scenarios retain those
integration responsibilities.

## Outside-in proof ownership

| Final promise / retained consumer | Owner and observable proof |
| --- | --- |
| Expanded authored note/property labels navigate; pending/dead behavior and recovery survive | Slice 1: extend `RichMarkdownEditor.propertyListWikiLinks.spec.ts` at the detailed item; assert label, named destination, actual click and recovery. Include keyboard activation at this active surface. Repoint the existing expanded-source proof. |
| Existing tracker link/status and value-specific actions remain associated | Slices 1–2: `RichMarkdownEditor.listPropertyMemoryTracking.spec.ts` observes tracker destination/feedback and assimilation body plus the other value's action and key-wide Skip. Replace its raw-label expectation with the surviving human-readable value proof. Retain generic `AssimilationModes` consumers. |
| Complete authored mixed list and URL actions, including repeated/long values and editor without saved note | Slice 2: extend the mounted editor list specs with one authored-order fixture and the no-note-ID context; observe rendered item sequence, URL action and shared tracker identity. Reuse existing read-only, list-dialog and list-value tests. |
| Expand/collapse selects the active list while key, toggle and Edit remain usable | Slice 3: one mounted-editor transition opens detail, accesses the existing Edit dialog, collapses and follows a restored compact link. Assert the positive canonical detailed-item sequence once; deletion is checked separately below. |
| Saved-note navigation, per-value learning workflow and touch layout consumers | Slice 1 early probe, then affected slice boundaries: existing `property_wiki_link.feature`, `property_memory_tracker.feature` and `note_property_layout.feature`. Scope expanded navigation to its detailed property item; preserve its destination promise. Existing 820/375-width checks own responsive controls. |
| Maintained behavior description | Slice 3: reconcile the rich-property section of `docs/note-content-saving.md` with compact versus detailed placement, retaining link, URL and mixed-list semantics. |

## Ordered slices

### 1. Follow expanded property item labels

Type: Behavior
Status: done
Proof: Detailed-item label/navigation/recovery proof and retained learning
controls, using the owning specs above and the acceptance commands below.
Accepted 2026-10-20: early isolated E2E probe passed 26/26 before edits;
detailed-item click/keyboard/pending/dead tests in
`RichMarkdownEditor.propertyListWikiLinks.spec.ts`, retained
`RichMarkdownEditor.listPropertyMemoryTracking.spec.ts`; full frontend suite
355 files/2215 tests and both typechecks passed. Refactor reused the existing
`listPropertyValue` factory; its focused rerun (4 files/25 tests) and both
typechecks passed.

Behavior: Given an expanded list item with a resolved authored wiki label, follow
that label to its named note or property destination while the item's tracker
and learning controls remain associated. Pending/dead items keep their current
feedback and recovery. Reuse the existing item/link rendering and pass its
context through the panel; preserve other `AssimilationModes` users.

Early probe, before dependent edits: run the selected isolated E2E batch below
to reach the actual runner and existing saved-note journeys. The safe selection
prefix passed during preparation; test-data mutation is deferred to authorized
execution. A failure stops dependent work and requires fixing the proof route
or reassessing this plan, without claiming those journeys covered.

Sizing: 5–7 minutes including focused frontend verification and cleanup; the
bounded runner startup/scenario wait is a separate verification exception to
the ten-minute cap. Compact/detail duplication remains until slice 3.

### 2. Read the complete authored list in detail

Type: Behavior
Status: done
Proof: Authored item sequence and URL actions in the mounted editor, saved and
no-note-ID contexts, plus retained value-specific learning proof.
Accepted 2026-10-20: authored-order fixture (linked, URL, repeated, blank and
260-character values) in `RichMarkdownEditor.listPropertyMemoryTracking.spec.ts`
observes the detailed sequence, URL action, shared tracker identity for repeats
and one key-wide Skip in saved and no-note-ID contexts; full frontend suite
355 files/2216 tests and both typechecks passed on final content. Refactor
hoisted the authored/eligible row derivation in `AssimilationModes` and named
the panel's saved-note projection; focused rerun and typechecks passed.

Behavior: Given a list containing linked and plain values, including repeated
and long authored occurrences, expand it and read those occurrences in order
alongside the applicable existing learning details. The same display works
without a saved note. Preserve eligibility, value-based tracker association,
key-wide controls and the existing value dialog rather than deriving visible
content from eligible learning units.

Sizing: 7–9 minutes including focused verification and cleanup. This is the
shared collection/learning-projection rule, not separate handlers per fixture.
Scrutinize implementation breadth before starting; if code work exceeds ten
minutes, stop and refine the remaining work instead of extending the attempt.
Compact/detail duplication remains until slice 3.

### 3. Switch between compact and detailed property values

Type: Behavior
Status: planned
Proof: Expand → open Edit → collapse → compact-link navigation at the mounted
editor, retained layout/E2E consumers, maintained documentation, and the
separate acceptance reading below.

Behavior: Given a stored list property, expand it to use its complete detailed
list with the key, collapse control and Edit available; collapse restores the
compact links beside the key. Delete the redundant expanded-state presentation
and its exclusive dependencies, updating affected observing helpers and docs.
Read-only and draft consumers retain their existing interactions.

Sizing: 5–7 minutes including focused verification and cleanup. The only
permitted verification overrun is bounded isolated-runner/scenario waiting;
implementation work remains subject to the ten-minute refinement limit.

## Verification and delivery

At each affected slice boundary, run the full frontend suite: the scratch run
took 69.24 seconds and these shared renderers have consumers beyond one spec.
Require both typechecks on the same accepted content under the frontend skill:

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit
```

Use the runner-owned isolated E2E batch for the early probe and affected external
consumers; reuse passing unchanged boundaries and rerun when a later slice
changes what they observe. All selected AI scenarios use the runner's mock:

```bash
CURSOR_DEV=true nix develop -c pnpm cy:run --spec 'e2e_test/features/note_topology/property_wiki_link.feature,e2e_test/features/recall/property_memory_tracker.feature,e2e_test/features/note_topology/note_property_layout.feature'
```

Follow AGENTS.md execution delivery: Jidoka, a fresh
`dough-post-change-refactor` agent, API generation if signatures actually
change, coordinator `./scripts/run.sh pnpm format:changed` once, plan evidence
update, agent commit with its check-only hook, and authorized publication/CI
ownership under `dough-execute-plan`. The preparation itself authorizes no
implementation or Take. No broader local backend suite is required by this
frontend-only plan.

## Acceptance reading for deletion

After slice 3, read the active expanded row/panel and compact branch, then sweep
the whole product for identifiers, fixtures, helpers and documentation used
only by the deleted rendering. Use `rg` for the identifiers actually retired;
record the literal search and classified result here during execution. Delete
exclusive dependents transitively and reconcile current consumers. Preserve
dated source evidence with its provenance, including the owner's screenshot
description. This is a one-time reading, separate from surviving-behavior proof;
add no permanent absence assertion, guard or checker for the deleted rendering.
