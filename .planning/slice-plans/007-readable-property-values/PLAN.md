# Read and edit complete property values comfortably

**Source:** [Read and edit complete property values comfortably in narrow windows](../../seeds/SEED-078-cohesive-note-workspace.md#story-4)

**Identity:** SEED-078#story-4

## Goal and scope

A learner or author reads complete property values and uses their existing
links, editors, and learning controls comfortably in narrow windows. Reflow
expanded content, preserve authored order and repeated list items, keep idle
rows quiet and active editing clear, and retain touch-sized actions. The
source's Goal, Scope, Key examples, and shared UI contract apply.

Whole-section disclosure, timestamp/body hierarchy, new property operations,
new editors, metadata representations, and learning policy are deferred. The
existing scalar/list value dialog remains the editing path it is today; this
plan does not redesign it. Existing access and validation constraints remain.

Preparation is planning only: no Take or execution is authorized. Observed
checkout: `/Users/terryyin/git/doughnut/.worktrees/read-and-edit-complete-property-values-comfortab`,
revision `4535beaeb57a2dd617c90f964ddf452a300fbd03`, 2026-10-20.
Execution starts from current trunk and reconciles shared changes normally.
There is no blocking supplier; SEED-064#story-7 is already completed.

## Existing solutions and architectural decisions

PFE choice: change the existing presentation, reusing its data and operations.

- `RichFrontmatterProperties` owns the property rows and editing composition;
  its coarse-pointer selectors already enlarge small inputs/buttons to 44px.
  `RichFrontmatterPropertyRow` currently constrains key/value space with grid
  columns. Keep key presets, draft insertion, specialized fields and events.
- `PropertyValueField` is the scalar contenteditable. Its `white-space: nowrap`
  and horizontal scrolling are the observed scalar bottleneck. Reflow its
  display without changing link serialization, paste, Enter/blur commit or
  soft-keyboard focus handling.
- `RichFrontmatterListPropertyValue` owns compact and read-only list display.
  `RichFrontmatterPropertyPanel` supplies the authored list to
  `AssimilationModes`, which already shows every occurrence and associates
  learning by value. Retain one detailed list, exact authored order, and its
  existing first-occurrence assimilation rule. Change sizing/wrapping, not
  identity or the authored list.
- `WikiLinkToken` and the scalar link handler already resolve note/property
  destinations and distinguish pending/dead links. Reuse them. The existing
  dialog owns scalar/list draft, reorder, save, cancel, and validation.
- Cross-product reading found persisted authored content and server-side
  property operations documented in `docs/note-content-saving.md`; the
  presentation change requires neither API nor storage changes. CLI/MCP and
  notebook codecs consume authored content, not the rendered row layout.
- Shared consumers: `NoteAssimilationModes` uses `AssimilationModes` for
  note-level controls; `RichMarkdownEditor` also renders notebook/folder
  readmes and draft properties. Preserve those purposes when changing shared
  CSS. Keep property presentation appropriately scoped instead of cloning
  learning rows or adding a window-size model.

Accepted ADR index and in-file statuses agree for 0000–0007; no supersession
or conflict was found. Relevant decisions:

- [ADR 0001 — Ubiquitous language](../../../docs/adrs/0001-ubiquitous-language.md):
  properties, authored values, and learning units retain their meanings.
  Its filename lacks the accepted suffix, but index and in-file status agree.
- [ADR 0004 — Notebook Markdown](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md):
  preserve author-owned keys, values, order, link spelling, and representation.
- [ADR 0005 — Web routes](../../../docs/adrs/0005-web-routes-accepted.md):
  reuse named note/property locations, focused-panel replace semantics, and
  current route-family/conversation behavior.
- [ADR 0007 — Environments and isolation](../../../docs/adrs/0007-environments-and-isolation-accepted.md):
  observations use disposable browser-test state, never Development data.

The existing [North Star](../../NORTH-STAR.md) requires no new topic or change
for this local presentation responsibility.

## Decisive premises and observations

All runtime observations used the named checkout, headless Chromium, real Vue
components/router, and mocked HTTP boundaries. No application stack, paid
service, or device usability session was used. Disposable styling and the
scratch spec were removed before preparation recording.

| Premise / consuming operation | Observation and result | Consequence |
| --- | --- | --- |
| Current fields hide long scalar text and expanded list content | Disposable `RichMarkdownEditor.propertyReflowProbe.spec.ts` mounted the real editor in a viewport minus 32px side space. At 320px the scalar had 1426px scroll width versus 24px client width; the expanded list panel also overflowed. | Reflow both the value and its enclosing row/panel. Merely increasing an input width is insufficient. |
| Presentation-only reflow can retain editing, repeated values and actual destinations | Temporary browser styles let scalar fields wrap, moved values across available row width, and bounded detailed learning rows. At 320/768/1280px full scalar and detailed-list values fit; inline editing emitted the changed Markdown; repeated plain values stayed ordered; clicking the long labeled wiki link reached `noteProperty(42, topic)`. Read-only navigation also passed at 320px. Five scratch cases passed. | Reuse the present editor, link and learning ownership. Final selectors, responsive arrangement, focus and touch interaction remain implementation acceptance, not claims about the prototype. |
| The geometry proof distinguishes the defect | `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyReflowProbe.spec.ts` passed the initial three width cases in 6.92s. With reflow disabled, the same command failed all three on scroll width (320/768/1280px client widths 24/452/964 versus 1426). Restoring reflow plus read-only and expanded-list cases passed five. | Maintained proof must measure width and visible text layout, not DOM text presence or height alone. |
| Current editor behavior can be exercised cheaply here | Command below passed 7 files / 37 tests in 5.43s: 5 probe cases and 32 existing cases. Inspected assertions reached note/property navigation, pending/dead events, duplicates and tracker destinations, dialog reorder/save/cancel, validation/recovery, renamed values, readme behavior, and read-only refusal. | Reuse these stable boundary scenarios and extend geometry at the mounted editor. Existing baseline results are not acceptance of future changes. |

Additional observations before assessment:

- With all scratch files removed, the focused browser command in Verification
  passed 26 files / 186 tests in 6.63s. This includes draft lifecycle, presets,
  scalar/list serialization, readme/relation/image-related controls, focus,
  and note-level assimilation consumers on the original product content.
- A second disposable run set Playwright provider `contextOptions.hasTouch`
  to `true` in the existing Vitest configuration. It observed real
  `(pointer: coarse)` and all six existing URL-row controls at least 44px high
  without injecting target-height styles. A separate case with temporary
  reflow kept entered scalar text and actual DOM focus through 768 → 320 →
  1280px resizing. Command:
  `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyReflowProbe.spec.ts -t 'coarse pointer|while resizing'`
  passed the two selected cases (three unrelated cases skipped) in 3.79s.
  The spec and configuration change were restored before recording. The
  existing browser provider therefore supplies the required pointer proof;
  execution can use a focused provider context without adding infrastructure.

Combined observation command:

```sh
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyReflowProbe.spec.ts tests/components/form/RichMarkdownEditor.propertyListWikiLinks.spec.ts tests/components/form/RichMarkdownEditor.listPropertyMemoryTracking.spec.ts tests/components/form/RichMarkdownEditor.propertyValueDialog.spec.ts tests/components/form/RichMarkdownEditor.propertyValidation.spec.ts tests/components/form/RichMarkdownEditor.readOnlyProperties.spec.ts tests/components/form/RichMarkdownEditor.propertyRowEditing.spec.ts
```

Consumer proof reading: `note_property_layout.feature` observes long keys,
control separation, touch heights, presets and page overflow. Its
`notePropertyLayoutMethods.expectEditablePropertyValueShowsAllText` checks
only vertical scroll; that does not prove horizontal visibility. The new
mounted geometry proof owns that missing signal. Key ellipsis is an existing
editing affordance, not a request to rewrite authored keys. The shared
`AssimilationModes.presentation.spec.ts` asserts grid classes as well as
behavior; if that presentation changes, replace obsolete structural assertions
with the retained layout outcome, preserving note-level controls.

## Outside-in proof ownership

All maintained new tests drive the mounted rich editor with production CSS,
real in-process collaborators, and only external API mocks. Do not inject
prototype CSS in acceptance tests. Parameterize representative narrow/tablet/
wide containers, not a business rule per viewport. Use range/client rectangles
and scroll extents to establish complete visible text and reachable actions;
asserting full `textContent` alone misses clipping.

| Source promise | Owner | Observable proof |
| --- | --- | --- |
| Complete prose, URL and long link values; legible row; wide/narrow reading | 1 | New `RichMarkdownEditor.propertyLayout.spec.ts`: scalar and read-only rows at 320/768/1280px, full wrapped value within row bounds, actual labeled link navigation. Include long key, unbroken text and URL shapes. |
| Inline scalar edit, authored keys/value/order, current specialized fields and read-only access | 1 | Existing row-editing, changes-only-the-edit, read-only, value-controls, Wikidata/image/relation tests through the rich editor; new wrapping scenario commits and observes Markdown. Existing read-only refusal remains a valid active access rule. |
| Every expanded list occurrence, compact/read-only placement, per-value destinations and learning identity | 2 | Extend mounted list/learning scenarios with long label, URL, repeated/empty items and unbroken text; observe visible ordered values, bounded controls, actual route/tracker destination or API request, collapse and Edit availability. |
| Pending/dead link recovery and keyboard navigation | 2 | Existing property-list and scalar wiki-link specs; retain pending navigation refusal and dead-link event, activate resolved expanded/read-only links by keyboard. |
| Quiet idle rows, visible edit/focus feedback, comfortable touch actions and ordinary compact pointer controls | 3 | Mounted real CSS: idle versus focused field presentation, keyboard focus visibility, reachable non-overlapping controls. Reuse coarse-pointer size rule; observe 44px touch sizing with browser pointer emulation (no matchMedia-only substitute), compact non-touch controls. |
| Draft survives width change; presets, validation, dialog save/cancel/reorder stay usable | 3 | Mounted editor: type then resize, retain draft and focus; use preset and inspect bounds, correct an invalid row, open dialog, reorder duplicate values/save and cancel a later edit. Existing draft-lifecycle and value-dialog specs supply unchanged semantics. |
| Note-level learning and readme consumers keep their operations | 2/3 | `AssimilationModes` suites plus rich-editor readme/draft cases; preserve named actions and destinations, change obsolete class coupling only when needed. |

The new geometry checks must fail on the corresponding original clipping
rule and pass on the actual implementation. One-time removal reading is
separate: search product, tests, docs and guidance for any retired local
classes/helpers/presentation names; delete exclusive dependents, keep shared
uses. Record actual searched names and classified results, without adding
absence tests or broad assertions that forbid a CSS utility everywhere.

## Ordered slices

### 1. Read and edit wrapping scalar properties
Type: Behavior
Status: planned
Proof: Slice 1 rows in the proof table; rich-editor suite below and frontend typechecks.

Behavior: A note reader/author opens long scalar and linked properties in a
narrow window, reads the entire value, follows a link or edits the scalar,
and gets the existing destination or committed Markdown. Read-only viewers
retain their readable values and access restriction.

Change existing row sizing and scalar/read-only presentation so labels leave
room for values and long text wraps. Preserve key presets, draft controls,
specialized fields and scalar interaction semantics. Add discriminating
mounted geometry proof and reconcile affected shared row consumers together.
Safe stop: scalar reading/editing improves; detailed-list layout remains for
slice 2, with existing operations intact.

Size hypothesis: 5–8 minutes including focused proof and local cleanup. Above
the 5-minute target because editable/read-only paths share one row-sizing
responsibility; splitting by component would fragment the outcome. Reassess
before 10 minutes, preserving the unfinished proof if the hypothesis fails.

### 2. Read expanded list values with their learning actions
Type: Behavior
Status: planned
Proof: Slice 2 rows in the proof table; rich-editor and shared assimilation suites, frontend typechecks.

Behavior: Expand a mixed list containing repeated and long values, read each
item in authored order, and follow or assimilate the intended value without
clipped text or overlapping controls; collapse restores usable compact links.

Reflow the existing list renderer and detailed learning rows within available
space. Retain authored occurrences and the existing learning identity rules.
Reconcile note-level consumers if shared styling changes; do not add another
expanded-list model. Prove actual link and learning activation alongside
geometry, then remove replaced exclusive styling.
Safe stop: list reading and operations work independently of the remaining
idle/focus presentation polish in slice 3.

Size hypothesis: 5–8 minutes. The shared detailed-list/learning boundary is
one proof loop; independent compact/full representations would add risk.
Scrutinized above target; stop and replan at 10 minutes unless only an
irreducible focused-check wait remains.

### 3. Edit properties with clear, reachable controls
Type: Behavior
Status: planned
Proof: Slice 3 rows in the proof table; rich-editor and shared assimilation suites, frontend typechecks.

Behavior: On a narrow touch window, start or resume a property edit, use its
controls and validation feedback, then save/cancel using existing semantics.
Widening or narrowing preserves entered text. Idle details remain quiet,
keyboard focus/edit state remains visible, and non-touch controls stay compact.

Reduce idle form borders through existing fields' focus styling. Keep the
existing coarse-pointer sizing owner, making any needed corrections there;
avoid per-component viewport flags. Verify presets do not cover the value or
specialized control, and keep the existing dialog operations reachable.
Update the maintained rich-property presentation documentation for the final
behavior, without duplicating workflow history.
Safe stop: this completes the source's presentation and interaction outcome.

Size hypothesis: 5–8 minutes including the focused interaction proof. Shared
focus/target styling and its editing feedback are one outcome. Use the observed Playwright context for pointer proof; retain ordinary
non-touch proof in the normal run.

## Verification and delivery

Focused browser command (use the new layout filename under the same prefix):

```sh
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor tests/components/recall/AssimilationModes
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit
```

The editor prefix covers shared draft, readme, image, relation, dialog,
serialization and focus consumers, so use it instead of selecting only new
cases. The shared assimilation suite covers note-level consumers outside that
prefix. Accept equivalent already-run checks on identical content. Both
typechecks are required by `.agents/skills/frontend/SKILL.md`; the browser run
alone is not frontend acceptance. No backend/API change is planned.

Do not infer real iPhone/iPad keyboard usability from headless Chromium.
Manual exploratory testing is not requested; invoke its skill only on an
explicit request or a later plan amendment that requires it. Hosted CI retains
its normal checks; it does not create an unrun local E2E acceptance promise.
If execution changes a callable contract or E2E-specific selector, inspect and
run its affected consumers under executable-proof rules before delivery.

Execution follows `AGENTS.md`: Jidoka, fresh dough-post-change-refactor agent,
API generation if needed, coordinator `./scripts/run.sh pnpm format:changed`
once, plan update, agent commit with check-only hook, and push under
`dough-execute-plan`. Do not implement or execute this sequence in preparation.

## Current decisions and review

One authored model, one list/learning rendering path, responsive presentation
owned with the existing components. Use document reflow as a presentation
analogy only; browser observations support the approach. Keep all source
promises together at acceptance and preserve each slice's stop-safe outcome.

The 40–60 minute story estimate includes refactoring, typechecking and delivery;
it is a hypothesis, not a measurement. Every leaf was reviewed for independent
outcomes, proof ownership and cumulative design. No preparatory Structure leaf
or further slice-plan refinement is needed. No remaining blocking concern was
identified; final interaction/design acceptance remains execution's work.
