# Find the note and property after recall

**Identity:** SEED-117#find-the-note-and-property-after-recall
**Source:** [Refined story](../../seeds/SEED-117-recall-note-location.md#find-the-note-and-property-after-recall)
**Source review:** [Source-only review](source-review.md)
**Preparation:** Eimi-chan; established workspace and branch
`/Users/terryyin/git/doughnut/.worktrees/find-the-note-and-property-after-recall`,
`codex/find-the-note-and-property-after-recall`; origin → main;
starting/published preparation revision `c2958ecb8e5ab5dd879ee339242649fdc03a7328`.
Execution authorized by the established start in this session.

## Outcome and boundaries

Learners can read the notebook/folder/note location in all four existing recall
contexts and independently open the full note, reviewed source property/item,
or the value's resolved reference. Follow the selected Full path design and
preserve the spelling note preview and existing surrounding actions.
First matching occurrence is Terry's accepted duplicate-value policy
(2026-11-10); item addressing retains the exact raw value even for a display alias.

No recall identity, schedule, schema, Portable-link syntax, historical snapshot,
editing-in-result, or additional full-note preview promise is added. Existing
read authorization and unresolved-reference behavior remain applicable.
The satisfied SEED-119 dependency supplies item destinations and focus behavior;
this story owns the links that reach those destinations.

## Design and reuse

- Extend the shared recall header rather than implementing four presentations.
  Use the existing viewer-scoped note read/store for notebook metadata and
  `wikiLinks`; compact `RecalledNote` already supplies reviewed key/raw value.
  Keep valid compact note/property links available while context loads. Use
  existing busy/error handling rather than swallowing read failures.
- Use existing breadcrumb notebook/folder locations and exact named
  `noteShowLocation`, `notePropertyLocation`, `notePropertyItemLocation` helpers.
  Apply wrapping at the recall presentation boundary; ordinary notebook chrome
  need not change. The header must survive the spelling preview slot.
- Reuse `WikiLinkToken` and source-note resolution for reviewed references.
  Display labels never replace raw item identity. Plain text stays text;
  unresolved references retain their existing non-navigating presentation.
- All slices evolve one model: source location + optional reviewed key/value +
  viewer-scoped note context. No parallel route state or reference resolver.
  Reuse `NoteContextReader` for spelling; avoid adding reader/editor modes just
  to satisfy an incidental legacy text-count assertion.
- [ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md),
  [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
  and [ADR 0005](../../../docs/adrs/0005-web-routes-accepted.md) constrain domain
  identity, authored reference semantics and named destinations. No conflict or
  exception. NORTH-STAR's notebook-tree direction is followed without amendment.

## Outside-in proof ownership

| Promise/example | Owner | Observable acceptance |
| --- | --- | --- |
| Notebook → folders → title is readable and navigable, including root, deep path and unbroken long names | Full location | Mounted real-CSS header at 280px and narrow recall conversation; text ranges remain inside card; links reach named notebook/folder locations. Check all four enclosing contexts. |
| Spelling keeps note context and gains the same full path | Full location | Answered spelling result shows header plus actual preview body/properties and existing context interactions; whole-note result is covered. |
| Title opens full note, property-only link focuses its key | Reviewed source | Click distinct anchors through production router; observe noteShow vs noteProperty and the destination property panel. |
| Reviewed list value reaches its first exact occurrence, including aliases/duplicates | Reviewed source | From a recall result click the property anchor into real NoteShowPage with SDK responses mocked only at HTTP; first matching row is visibly focused. Switch result and verify new destinations. |
| Edited/missing value retains explicit unresolved feedback | Reviewed source | Follow a recalled raw-value href into current note data where that value is missing; note/property remains readable with unresolved-item feedback. Reuse supplier behavior, prove this caller reaches it. |
| Plain value is text; wiki display label follows its resolved note/property | Reviewed value | Mounted shared header with real WikiLinkToken and source realm: plain value; aliased wiki value; reference-property selector; saved unresolved wiki value. Click resolved link and inspect current named route. |
| Existing result, tracker and conversation actions survive | Each affected slice | Run affected mounted suites below; retain preview, refinement, history, exclusions and answer feedback observations. |

## Ordered slices

### 1. Read the full location in every recall context
Type: Behavior
Status: done
Proof: first two proof rows; affected mounted suites, plus headless text-range
geometry in the shared header and narrow ConversationComponent subject pane.

Behavior: a learner opens a multiple-choice result, spelling result, memory
tracker or recall-question conversation → notebook, ancestor folders and note
title are shown in order and wrap inside the card; notebook/folder links open
their own locations. Spelling retains its read-only preview below the header.

Reuse the existing note read for missing metadata. Give newly reading tests
matching `showNote` responses, including direct header, tracker defaults and
result-switching cases; update Storybook's note context fixture too. Do not
stub the shared store or renderer. Adapt the legacy spelling single-title
text-count observation to the approved header plus retained preview: preserve
the actual preview content assertion, without inventing a title-suppression rule.

Sizing hypothesis: about 8 minutes including focused proof and cleanup. The
five-minute target is exceeded because one shared location outcome has four
enclosing contexts and a preview slot; these are one responsibility/proof loop.
Stop and refine if implementation itself reaches ten minutes. Interim: title
and focused-property navigation retain their current behavior until slice 2.

### 2. Open the reviewed source independently of the whole note
Type: Behavior
Status: done
Proof: source-destination rows above; real-router clicks and note-page selection,
duplicate first match, exact raw alias, stale value and result switching.

Behavior: a result identifies a property/value → the note title opens noteShow;
the separate Reviewed property anchor opens noteProperty, or notePropertyItem
with raw value and occurrence 1. A changed/missing value uses the destination's
existing explicit unresolved feedback instead of retargeting.

Carry raw value through the shared props adapter to the source anchor. Remove
the now-orphaned focused-property title-link prop/branch after checking all
NoteTitleWithLink callers; retain their ordinary note-title links. Replace
old tests of title-to-property behavior with the distinct-destination proof.
Update `property_memory_tracker.feature`, its thin steps and page object so
following a title proves the full note, while following the reviewed property
proves property/item focus. Its tracked-value assertion should observe item
identity through the property destination, independently of display text.

Sizing hypothesis: about 8 minutes for the common route rule and mounted proof.
Focused Cypress stack startup/run is an explicit test-runtime exception to the
ten-minute bound; it does not extend the implementation timebox. Interim:
reviewed value text remains raw until slice 3, while destinations are usable.

### 3. Read and follow the reviewed value
Type: Behavior
Status: planned
Proof: reviewed-value row; existing source-navigation proof stays green, and
the focused Cypress feature asserts the readable linked value's destination.

Behavior: a reviewed value is plain text, a resolved wiki reference with or
without a display alias/property selector, or an unresolved saved reference →
show its ordinary readable form; following a resolved reference opens its
destination independently of the containing note's property/item anchor.

Feed existing WikiLinkToken the raw value, viewer-scoped wikiLinks and saved
Markdown. Final presentation follows Full path's Reviewed property/value line.
Remove obsolete raw-interpolation presentation and its dedicated assertions;
retain the valid tracked-value identity and source-destination proof. Extend
the existing E2E list-recall example with the readable reference-link journey.

Sizing hypothesis: about 5 minutes plus the same focused Cypress test-runtime
exception. No separate Structure slice is needed: rendering and its inputs
belong with this behavior. Refine on an implementation overrun.

## Verification and delivery

Use project `unit-testing`, `frontend`, matching frontend component/testing
skills, and `e2e-authoring` when changing its consumers. Local commands:

```sh
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/Answered tests/components/recall/NoteUnderQuestion tests/components/conversation/ConversationComponent tests/pages/RecallPage tests/pages/MemoryTrackerPage tests/notes/NoteContextReader.spec.ts tests/pages/NoteShowPage.propertyItem.spec.ts
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/recall/property_memory_tracker.feature
```

Add new geometry/navigation spec paths to the focused command. This selection
covers changed read preconditions even where enclosing tests do not name the
header. Typechecks are the frontend skill's required local proof. Cypress
owns the real recall-to-note HTTP/browser integration and updated legacy
consumer; run after its changes, and reuse sufficient accepted unchanged proof.
Observe host load before starting that stack; use its owned isolated wrapper.
Also retain accidental-match reveal/resolve coverage through the affected
AnsweredSpellingQuestion specs; the current E2E consumer is
`accidental_match_scheduling.feature`. If its observed boundary changes, include
that feature in the same focused batch. No paid service or manual developer
observation is required.

Follow Execute Plan's required Jidoka → fresh post-change-refactor agent → API
generation if a signature actually changes → coordinator format:changed once
→ plan update → commit/check-only hook → push. This plan expects no API change.
If components are added/deleted, regenerate tracked component declarations with
the frontend skill's Vite build command. No code or tests are delivered here.

Expected edited production files are 11–98 lines; tests/support inspected are
79–205 lines; the E2E feature, steps and page object are 143, 78 and 25 lines.
All are below the 250-line rule. Keep new proof in cohesive files; reassess any
newly implicated oversized file before editing it.

Acceptance reading, separate from tests: inspect all NoteTitleWithLink callers
and search the retired focused-title binding and raw indicator interpolation;
remove their orphaned code/tests/fixtures while retaining genuine property
focus elsewhere. No permanent absence assertion or checker is requested.

## Premises and preparation observations

All observations are local, headless, unpaid, at the starting revision on
2026-11-10 Asia/Singapore. Only preparation on SEED-120 advanced origin/main;
the code paths observed did not change. All scratch edits were reverted before
writing this plan; observations establish feasibility/consumers, not completion.

| Premise and consuming operation | Observation/result | Limit |
| --- | --- | --- |
| The recall trail overflows at narrow width | Story's 17:39 Chromium diagnostic: breadcrumb right 848.578125 within card right 280; containment failed. | Reproduces over-wide shared trail, not exact original left-clipping geometry. |
| Recall can reuse existing note metadata/reference read | Scratch shared-header `getNoteRealmRefAndLoadWhenNeeded` read rendered a unique notebook name; real WikiLinkToken showed `running` for `[[Verbs/run\|running]]`, clicking reached noteShow(77). | HTTP response supplied by mockSdkService; no new server contract is presumed. |
| Existing breadcrumb can wrap deep and long names locally | Same scratch header with local wrapping CSS, 280px host, three folders including an 85-character unbroken name: every anchor text range stayed within the card. | Final four-context layout proof remains slice 1. |
| New read changes caller setup | Scratch run of the nine-file command below: 44 tests passed; deliberately corrupted item parsing failed the note-page selection test, and 33 unhandled read errors exposed missing header/tracker HTTP fixtures. Restored parsing and added showNote fixtures; expanded ten-file run passed 54 tests with no unhandled errors. | These fixture adaptations belong to slice 1; initial run was deliberately non-green, not accepted proof. |
| Existing item destination selects the reviewed value and its proof discriminates | Baseline note-page test passed at 17:39; replacing parsed item text with a missing value caused its selected-row assertion to fail at 17:50; restored parser passed in the 17:51 run. | Slice 2 must add caller-to-destination clicks, duplicate and stale cases. |
| The proposed focused Cypress input is admitted in this worktree profile | Ran the runner's actual `resolveSpecs` prefix with `--spec e2e_test/features/recall/property_memory_tracker.feature` and isolated=true: returned that exact feature with private OpenAI mock required and no Wikidata mock. | Stopped before state-changing stack provisioning; execution owns the actual recall-to-note journey, not preparation. |

Literal commands/results:

- 17:50: `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/NoteUnderQuestion.spec.ts tests/components/recall/AnsweredQuestionComponent.spec.ts tests/components/recall/AnsweredSpellingQuestion.spec.ts tests/pages/MemoryTrackerPageView.spec.ts tests/pages/MemoryTrackerPageView.recallPrompts.spec.ts tests/components/conversation/ConversationComponent.refinement.spec.ts tests/components/conversation/ConversationComponent.exclusion.spec.ts tests/notes/NoteContextReader.spec.ts tests/pages/NoteShowPage.propertyItem.spec.ts` — mutant selection failure and fixture errors described above, 6.94s.
- 17:51: `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/NoteUnderQuestion.spec.ts tests/components/recall/AnsweredQuestionComponent.spec.ts tests/components/recall/AnsweredSpellingQuestion.spec.ts tests/pages/MemoryTrackerPageView.spec.ts tests/pages/MemoryTrackerPageView.recallPrompts.spec.ts tests/components/conversation/ConversationComponent.refinement.spec.ts tests/components/conversation/ConversationComponent.exclusion.spec.ts tests/notes/NoteContextReader.spec.ts tests/pages/NoteShowPage.propertyItem.spec.ts tests/components/form/RichMarkdownEditor.propertyListWikiLinks.spec.ts`, with parser restored and HTTP fixtures supplied — 10 files / 54 tests passed, 39.66s. Setup/assertions inspected in the named specs reach note context, tracker actions/history, spelling, conversation refinement/exclusion, wiki rendering and destination focus.
- 17:53: `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/seed117ReadAndWrapProbe.spec.ts` — 2 scratch cases passed, 17.20s; consuming DOM notebook label/reference click and text-range geometry described above. Scratch file and product edits were deleted/reverted afterwards.
- Runner admission prefix: `CURSOR_DEV=true nix develop -c node --input-type=module -e 'import { resolveSpecs } from "./scripts/e2e-invocation-selection.mjs"; console.log(JSON.stringify(resolveSpecs(["--spec", "e2e_test/features/recall/property_memory_tracker.feature"], process.cwd(), true)));'` — exact selection admitted as described above, without starting services.

Consumer trace: [located consumers and dispositions](consumer-trace.md).
Execution regenerates it from its actual diff. Its E2E journey is
`e2e_test/features/recall/property_memory_tracker.feature`; title/property/raw
text observers must be aligned without losing tracked-value/source-focus proof.

## Preparation concern review

Slice-plan refinement was not needed: three cohesive Behavior outcomes reuse
one source-context model, each owns its proof and names its safe interim state.
No remaining slice-boundary, cumulative-design, proof-ownership, sizing or
decisive-premise concern was identified. No pre-Take human decision remains.
The readiness recorder owns the assessment; this review grants no execution.

## Execution context

Hibiki-chan; story-branch; workspace and branch as above. origin/main is the authorized integration target; execution increments publish to origin/codex/find-the-note-and-property-after-recall. Starting revision 9db0154c71473340882bc15f6c86fd9989815cf2; accepted claim/published base d17580ea6cee7f8eb3d348728721fdb9633e2f95. No second claim. Replanning permitted under existing planned execution authority. Setup `./scripts/run.sh bash scripts/worktree_setup.sh` passed against locked metadata; `./scripts/run.sh pnpm -C frontend exec vue-tsc --noEmit` passed. Hook is check-only scripts/git-hooks/pre-commit; coordinator formatter is `./scripts/run.sh pnpm format:changed`.

CI: GitHub nerds-odd-e/doughnut, execution branch codex/find-the-note-and-property-after-recall, workflow ci.yml verified active donut CI with push ** trigger. Coordinator hibiki-recall, observer /tmp/dough-ci-501/watch-aOJ9AU, PID 3366, session 34778, yielded cell g1054:10; runtime in this exact checkout. Trunk claim pendingCi: unobserved.

## Story obligations

### G1. Distinct title destination awaits reviewed-source slice
Reported: slice 1 — "Existing title-to-focused-property navigation remains for slice 2"
Story clause: "The note-title link opens the full note."
Disposition: proved by slice 2: NoteUnderQuestion.spec.ts distinct real-router title/key clicks, sourceNavigation.spec.ts and property_memory_tracker.feature title/item journey; slice2-return.md

### G2. Readable reviewed values await reference presentation
Reported: slice 1 — "raw reviewed-value presentation remains for slice 3"
Story clause: "Render a resolved wiki value with its ordinary display label and destination, using the source note's viewer-scoped resolution."
Disposition: interim until slices 3

## Accepted execution evidence

Slice 1: [return and promise observations](slice1-return.md), [actual consumer trace](consumer-trace-slice1.md). Coordinator inspected the header, all five location tests, preview setup/assertions and reached E2E helpers. Q passed 35 files/155 tests; both typechecks passed. Independent refactor1 returned REFACTOR COMPLETE with no product edits; supplemental helper-to-feature trace was records-only, proof unchanged. Early and delivery freshness unchanged at d17580ea6cee; no incoming named-sibling change. Guarded delivery accepted 8b9e70d9fb0f6ec7f11b30595278c29c6709f2c4 on the execution branch from suffix base d17580ea6cee7f8eb3d348728721fdb9633e2f95; observer registration reused the retained stream.

Slice 2: [return, actual commands and source comparison](slice2-return.md), [consumer trace](consumer-trace-slice2.md). Coordinator inspected real-router source/title clicks, real NoteShowPage duplicate/alias/reordered/stale/key-only focus and result switching, plus integrated feature changes. Q passed 36 files/158 tests; strengthened final six cases, both typechecks and Cypress 5/5 passed. The retired focused-title contract/label sweep is the one-time reading in that return. G1 is proved; G2 remains with slice 3. Independent refactor2 returned REFACTOR COMPLETE without edits; proof unchanged. Early/delivery freshness unchanged at d17580ea6cee, no incoming named-sibling change.
