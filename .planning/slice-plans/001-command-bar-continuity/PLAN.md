# Command-bar continuity

**Identity:** SEED-080#story-1
**Source:** [Recover active navigation and finish command-bar interactions](../../seeds/SEED-080-command-bar-reconciliation.md#story-1)

## Goal and scope

A signed-in author recognizes the current activity, resumes recall directly,
and searches through the common dialog from a comfortable responsive command
menu. Preserve the source's activity precedence, counts, navigation destinations,
signed-out behavior, adaptive note commands, and shared search capabilities.
The owner accepted visible property-learning presentation as Assimilate context
on 2026-10-20, regardless of entry path.

Execution was authorized on 2026-10-20 through `dough-execute-plan`. Sidebar
toolbar relocation, breadcrumbs, note-reading layout, and off-route assimilation
priority remain deferred. No API, persistence, or scheduling-policy change is
needed. Counts and recall eligibility retain their existing domain rules.

## Architecture and reuse

- `useNavigationItems` owns identity, precedence and counts; `NavigationItem` and
  `navigationActionItems` distinguish context from direct actions. Recover first-active
  selection from `HorizontalMenu.vue` at `e4227c46a0fb8fcc2185769bb0ab9974588288aa`
  using today's popup/header and the existing classifier.
- Call `useResumeRecall` for intent/spelling keyboard preparation consumed by `RecallPage`,
  and `useGoToNextAssimilation` for next-unit loading, counts, messages and destinations.
- Extend `useAssimilationView` and its note/property presentation consumers to expose
  current visible activity to navigation. Reuse eligibility/disclosure state: a property
  route, `v-show`-hidden panel or saved target alone is insufficient. No parallel
  sequence-origin flag or duplicate menu property parsing.
- Reuse `SearchForNoteAndFolder`, `Modal`, `primeSoftKeyboard`, the focus directive and
  `note-search` shortcut. One dialog/opening operation serves both menu targets and the
  shortcut. Relationship `SearchForm` needs a note and is not the global-search entry.
- Keep note actions, state and measurements with `NoteToolbar`/`NoteToolbarMoreOptions`;
  adjust allocation/alignment in `GlobalBar` and the existing command row.

Applicable Accepted decisions:
[ADR 0001 — domain vocabulary](../../../docs/adrs/0001-ubiquitous-language.md#assimilation),
[ADR 0005 — named locations and note route family](../../../docs/adrs/0005-web-routes-accepted.md),
and [ADR 0006 — failure handling](../../../docs/adrs/0006-failure-handling-accepted.md#usage).
No conflict or exception. [North Star](../../NORTH-STAR.md) needs no change.
PFE found suitable owners above; the missing integration is visible assimilation
context reaching the existing navigation model.

## Proof and premises

The [proof and consumer trace](PROOF.md) own observations and the slice-2 removal
reading; [evidence](EVIDENCE.md) retains dated baselines and revised native input
acceptance. Both belong to this executable plan.

## Ordered slices

### 1. Open focused global search in the common dialog
Type: Behavior
Status: done
Proof: Shortcut/dialog/search cases in the proof table; native input is unverified.

Behavior: Signed in on any supported page → press Ctrl/Cmd+F → the common
global search dialog opens focused, offers the existing search journey, and
closes through its normal controls or result navigation.

Use one opening operation and retain synchronous touch focus preparation for
the menu entries delivered next. The owner waived native input observation as an
acceptance dependency. Interim: menu search keeps its current
presentation until slice 2; record that interim obligation during execution,
owned by slice 2. Target 5–8 minutes of local work including focused checks;
owner-device access/wait is an explicit external-wait exception.

### 2. Open that search dialog from the popup menu
Type: Behavior
Status: done
Proof: Both launcher targets, keyboard/touch access, dialog result journeys,
shared-search regression checks, `search_note.feature`, and separate removal reading.

Behavior: Open the menu → activate either the empty search-shaped button or
its search button → the popup closes and the same focused search dialog opens.
Complete the consumer migration and transitive deletion described above in this
slice. The apparent field is not an editable textbox. Target 5–8 minutes;
scrutinized because proof migration belongs to the same entry change. Slow
isolated Cypress startup is an external-wait exception, not permission to
leave its observation unrun.

### 3. Recognize the current activity and resume recall directly
Type: Behavior
Status: done
Proof: Compact context, fallback, signed-out/menu tests and real recall continuity,
including `browse_answer_and_notes_while_recalling.feature`.

Behavior: Browse with the menu closed → see the existing active icon/count;
activate ordinary context to open navigation, or eligible green Resume to
return directly to the preserved current recall question. The explicit menu
opener works independently in either state.

Keep exact eligibility in `useNavigationItems`: paused cursor plus loaded
trackers, or progress beyond the first item off recall with remaining work.
Do not replace the paused-cursor branch with a remaining-count test. Preserve
the normal destinations, fallback, counts and diligent styling. Target 5–8
minutes; direct action and menu opening form one control contract. Existing
recall Cypress runtime may need the same external-wait exception.

### 4. Identify the currently presented assimilation activity
Type: Behavior
Status: done
Proof: Routed note/property presentation and next-unit observations in the table.

Behavior: Open note assimilation or a property-learning panel by either entry
path → see Assimilate and its existing badge/direct next action; leave/hide
that presentation → see the appropriate ordinary context, with eligible Resume
still taking precedence.

Extend the existing shared presentation concept and its consumers as one
change. Actual property learning eligibility, detail disclosure, note identity,
and page lifetime determine current context. Include read-only/missing/
non-learning browsing and a next-unit transition from note to property. Target
5–10 minutes; scrutinized because two presentations share one domain outcome.
If exposing that common concept needs more than the hard limit, stop and
refine this slice around the newly evidenced responsibility before continuing.

### 5. Align and distinguish the command groups
Type: Behavior
Status: planned
Proof: Workspace header geometry, overflow and busy/open-operation retention,
plus bounded phone/iPad/desktop visual observation.

Behavior: Open/resize a note while an action is active → commands align to
the right of their allocated area, retain order and measured overflow, and
subtle navigation/group/content boundaries remain visible.

Keep the existing action state and measurements, including pinned busy/open
controls and ellipsis. Measure with the expanded activity controls now present.
Target 4–6 minutes; no sidebar-toolbar relocation or breadcrumb redesign.

### 6. Keep a spacious menu reachable on narrow and short screens
Type: Behavior
Status: planned
Proof: Panel geometry/scroll/keyboard reachability and bounded visual observation
of spacing, footer separation and group boundaries with final controls.

Behavior: Open the popup and expand Account on phone/iPad/desktop or a short
viewport → destinations, search launchers and the footer have comfortable
separation and remain reachable through the panel's scroll area.

Extend layout checks using the final search launchers and account contents;
keep the footer's extra vertical gap. Target 4–6 minutes. This slice owns
final combined visual acceptance, not unrelated layout work.

## Verification and delivery

Follow `AGENTS.md`, `unit-testing`, `frontend`, the matching frontend rules,
and `e2e-authoring` for changed Cypress consumers. Use the whole affected
toolbar suite (the observed run took 7 seconds), not a hand-picked subset
that hides shared helper failures:

```sh
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/toolbars
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/layouts/NotebookWorkspaceHeader.spec.ts tests/notes/NoteToolbar.overflow.spec.ts tests/notes/NoteToolbar.voiceInputOverflow.spec.ts
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/search tests/wiki-link-or-relationship tests/notes/NoteUnresolvedWikiLinkModal.spec.ts
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_view/search_note.feature
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/recall/browse_answer_and_notes_while_recalling.feature
```

Include context/touch specs and affected page consumers from [PROOF.md](PROOF.md).
Reuse valid proof for unchanged boundaries. Require both frontend typechecks,
including generated types; component additions/deletions require the Vite build
to regenerate `frontend/components.d.ts`. Regenerate APIs only for a contract
change. No full backend or paid-service run is required.

The active slice requests bounded manual visual observation using
`dough-manual-testing` at the listed widths. Native iPad keyboard behavior stays
unverified by owner decision. Save setup, screenshots/results and limitations through
the normal execution evidence, never claim unobserved OS behavior.

Each slice requires Jidoka, a fresh `dough-post-change-refactor` agent,
coordinator-only `./scripts/run.sh pnpm format:changed` once, plan update, agent commit
with check-only hook, push and owned CI follow-up under `dough-execute-plan`.
These are delivery gates; local proof precedes publication.

The sequence retains one navigation model, assimilation-presentation concept,
search dialog and existing command ownership. Each slice is independently useful;
the search interim is explicitly assigned. Target roughly five minutes,
scrutinize longer work, and stop/redecompose at ten minutes. The stated external
test/device wait exceptions do not permit growing implementation.

## Current decisions and learnings

Slices 1–4 accepted: search, compact activity/Resume, and visible note/property
assimilation. Routed presentation proof and 214 affected tests pass with typechecks;
prior search/recall journeys remain valid. Independent refactoring completed.
Native input is unverified under the owner's decision; see [evidence](EVIDENCE.md).

CI repair: compact activity preserves pinned recording/open controls at 320px;
157 tests and typechecks pass after reproducing the failure. See [CI evidence](EVIDENCE-CI-HEADER.md).
Slice 5 remains planned; its candidate is safely stashed until repair publication.

- The owner's property-context choice is settled; no sequence-origin policy.
- Actual visible learning presentation, not property route or mounted DOM,
  is the shared context. Details disclosure uses `v-show`.
- Shortcut proof observes the actual dialog; Resume continuity observes the real
  question/cursor journey. Routes and intent alone do not prove continuity.
- Search deletion uses a separate one-time reading from retained behavior proof.
- Preparation found no unresolved slice-boundary/cumulative-design concern;
  no further refinement pass is needed. Native input remains unverified and
  does not block execution.

## Story obligations

### G1. Native iPad keyboard observation
Reported: slice 1 — "Native OS keyboard behavior remains unverified."
Story clause: "Touch activation supports entering text with the software keyboard."
Disposition: owner changed "Native iPad software-keyboard observation is not an acceptance prerequisite"

### G2. Search presentation interim
Reported: slice 1 — "Embedded menu search remains as slice 2’s interim obligation."
Story clause: "Remove the embedded-menu search presentation and transitively unused presentation-only code; shared search capabilities remain reusable."
Disposition: proved by slice 2: DonutMenu.search.spec.ts dialog journeys, DonutMenu.search.touch.spec.ts focus sequence, search_note.feature six passing scenarios, and PROOF.md separate removal reading

### G3. Recall question assertion consumer
Reported: slice 3 — "The changed shared method now requires @currentRecallQuestion and @currentRecallProgress, which would break that journey."
Story clause: "Preserve the other existing navigation destinations, fallback, and signed-out entry behavior."
Disposition: proved by slice 3: recallPage.ts retains expectCurrentQuestion for daily_probe.ts and uses expectRememberedQuestion only for continuity; both Cypress features passed all five scenarios

## Execution context

- Identity: `SEED-080#story-1`; publisher `dashboard-territory.local-doughnut`;
  assigned agent Maria-chan; established start reused.
- Execution/originating checkout: `/Users/terryyin/git/doughnut/.worktrees/recover-active-navigation-and-finish-command-bar`;
  owned linked worktree, branch `codex/recover-active-navigation-and-finish-command-bar`.
  Story Branch Mode publishes increments to the same branch on `origin`;
  final integration target is `origin/main`. Default checkout is
  `/Users/terryyin/git/doughnut` and is not this execution's edit location.
- Starting revision `99588521d53b64053b8cb7d1f7e43c1452480f42`;
  accepted claim / initial published base and candidate
  `c02920b649348640cdb8dab5b6d91ca1bd04e6c9`.
  Remote branch confirmed; claim CI on trunk is unobserved.
- Checkout setup `./scripts/run.sh bash scripts/worktree_setup.sh` succeeded;
  its locked workspace installation was current. Applicable command
  `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/toolbars/DonutMenu.searchShortcut.spec.ts`
  passed 6 baseline cases. Hook: check-only `./scripts/run.sh pnpm lint:changed`.
- Replanning retains existing authority and the stated budgets/exceptions.
- CI: GitHub Actions `nerds-odd-e/doughnut`, verified `ci.yml` / `donut CI`;
  target branch `codex/recover-active-navigation-and-finish-command-bar`.
  Codex coordinator `maria-command-bar`, live observer `/tmp/dough-ci-501/watch-5lMwJV`,
  PID `7412`, PTY `86137`, yielded cell `51`; prior observer stopped cleanly.
