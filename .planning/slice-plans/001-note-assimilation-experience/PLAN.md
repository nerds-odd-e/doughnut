# Choose how to assimilate a note and when to move on

**Identity:** SEED-113#assimilate-notes-with-a-clear-default-and-result
**Source:** [refined story](../../seeds/SEED-113-note-assimilation-experience.md#assimilate-notes-with-a-clear-default-and-result)
**Source review:** [source-review.md](source-review.md)

## Goal and scope

A learner sees the note before choosing its default Understanding action or an
independent optional mode, sees what Understanding added to recall, and decides
when to advance. Implement the seed's Visible options layout at wide available
widths and Read first layout with bottom actions at compact widths. Preserve
the note's content, property controls, optional tracker actions and statuses.

Terry accepted note-level skip on 2026-11-09: exclude only the note's Understanding
entry; properties and optional modes remain eligible. Confirmation explains
that scope. Restore stays on the note; direct Understanding remains possible.

Property-row design/advancement, sequence ordering, scheduling, quotas, session
commissioning and restoring a temporary result after browser reload are outside
this increment's delivery commitments. These limits add no product refusals.
Implementation is separately authorized through Execute Plan, not this preparation.

## Current decisions and existing solutions

- Use **Next item** after successful Understanding. It requests the next unit
  only on the learner's advance and uses existing named note/property locations.
  A property on the same note is a valid next destination. On exhaustion retain
  the current note page, display **No more notes to assimilate**, and dismiss
  assimilation presentation. The daily-goal toast does not end a nonempty sequence.
- Keep one note-scoped transient success state through optional-mode tracker
  refreshes; reset it when changing notes. Tracker availability and next dates
  remain derived from shared recall information. A previously active tracker
  shows its ordinary status rather than claiming success in this visit.
- Change the existing action chain (`useAssimilateUnit` /
  `useMemoryTrackerActionHandlers` / `useMemoryTrackerActions`) where necessary,
  preserving property auto-advance. Reuse `useGoToNextAssimilation`, skip and
  restoration owners; add no duplicate backend sequence, next-item cache, schema,
  persistence or public API responsibility.
- PFE: `NoteShow` / `NoteTextContent` own the reading content and editable title.
  Since slice 1, `NoteShow` renders `AssimilationPanel` in `NoteTextContent`'s
  `#after-title` slot and owns its remount key after optional additions;
  `NoteToolbar` no longer hosts it. The note-level markup lives in
  `NoteAssimilationModes`; property rows use the property-only
  `PropertyAssimilationModes`; both share `AssimilationModeAction` for the
  loading/tracker-link/action switch. Keep property presentation unchanged.
- PFE: `AutoCollapseDropdown` and its portal/collapse support already solve
  anchored disclosure and nested-modal/outside clicks. Reuse them for compact
  Optional modes, adding local keyboard/focus handling where needed. `PopButton`
  opens a modal and blurs its trigger, so it does not supply this popover behavior.
  Book reading bars offer bottom/safe-area styling examples; their Book-specific
  responsibilities do not justify a new shared abstraction.
- The seed's Architecture cites Accepted ADRs 0001 (assimilation vocabulary),
  0003 (New tracker scheduling), 0005 (named note/property destinations), and 0006
  (existing failure handling). No exception is needed. The
  [North Star](../../NORTH-STAR.md) adds no new direction for this local interaction.
- Choose the responsive threshold from available reading/panel space, not a device
  name. Verify iPad portrait (768px), phone (375px), and a wide workspace (1280px),
  including a wide viewport narrowed by notebook chrome. Preserve the seed's
  icons, descriptions, action wording and success checkmark.

## Outside-in proof ownership

| Final-state promise | Owner | Observable proof at a stable boundary |
| --- | --- | --- |
| Title first; Understanding is the default; distinct optional actions/descriptions/icons | 1 | Mounted real notebook workspace with production CSS: title geometry precedes actions; Understanding primary emphasis and optional actions are identifiable. |
| Each optional mode is independent; tracker link/date; title or alias verified with note hidden | 1 | Mounted panel action/status tests and spelling E2E observe tracker outcome, verification/cancel and same note. |
| Successful Understanding visibly waits; count/recall refresh occurs once; optional additions preserve result; changing notes resets visit success | 2 | Mounted workspace: save → checkmark/message with title/current route → Commissioned or verified Spelling → same result with updated tracker information → open another note with pre-existing Understanding → its ordinary tracker link/date. |
| Deliberate next reaches note or same-note property; exhaustion and daily-goal feedback | 2 | Mounted workspace/router and walkthrough E2E: result → Next item → correct named destination, or exhaustion feedback/current note. |
| Property assimilation retains its own advancement | 2 | Existing mounted property control posts its property key and reaches the next named property location. |
| Skip explanation; confirm advances only note-level skip; restore/direct Understanding and optional modes remain usable | 3 | Confirmation DOM and real walkthrough with a note containing a pending property; revisit, optional add, restore or direct Understanding; sequence offers the eligible property/unit. |
| Compact optional modes one tap away with current actions/statuses | 4 | Real workspace at compact width: open, act, dismiss and regain trigger; Spelling verification covers the note while the popup is active. |
| Compact title/properties/body precede actions; bottom actions stay accessible | 5 | Browser geometry at phone/iPad widths and narrowed workspace: scroll long content to its last line, operate bottom actions, open popover within viewport; successful result/Next item remain usable. |

## Premises and observations

All observations below used this owned worktree at
`96edea6b5919f11b874cb3a8780cd8c99534bc6d` on 2026-11-09. Product source was
restored exactly before preparation writes. These are baseline observations,
not acceptance of the new UI.

| Premise and consuming operation | Form/profile and result | Unverified remainder |
| --- | --- | --- |
| Existing note actions create trackers, stay for optional modes, and advance Understanding through the real action chain | `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/AssimilationPanel.spec.ts tests/components/recall/AssimilationPanel.trackers.spec.ts tests/composables/useGoToNextAssimilation.spec.ts` in headless Chromium: 20 passed. Scratch suppression of note-level auto-advance made the two route assertions fail, other 18 passed; reverted. | New success/advance interaction needs the mapped slice 2 proof. Existing assertions are consumers to replace with the promised positive result. |
| Existing property control advances; next-unit consumer accepts property and empty results | `CURSOR_DEV=true nix develop -c env -u NODE_ENV pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyMemoryTracking.spec.ts tests/composables/useGoToNextAssimilation.spec.ts tests/components/recall/AssimilationPanel.spec.ts`: 17 passed (8.22s). Scratch suppression of property advancement failed only the mounted property test (expected note 43/property level; remained note 42/property topic), other 16 passed; restored blob `4c08b3b2190d67b6229cac87485ed06eff734ada`. | New panel → explicit next → same-note property and panel exhaustion need slice 2 proof. |
| Optional addition really remounts the enclosing panel on the same note | Disposable `tests/layouts/assimilationOptionalRemountProbe.spec.ts`, run with `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/layouts/assimilationOptionalRemountProbe.spec.ts`, 2026-11-09T04:23:33Z: one Chromium test passed (8.82s). Mounted routed workspace at 1280; Commissioned success disconnected the old panel and replaced it with tracker status on the same named note route. Only backend HTTP mocked. Scratch deleted. | Slice 2 must observe result survival at this enclosing boundary, not an isolated panel alone. |
| Focused real journeys run through the isolated E2E stack and persist existing tracker/skip outcomes | The exact six-feature command under Verification below exited 0: 6 files, 23 scenarios passed, spec time 1m58s. Walkthrough 9, page types 1, spelling 6 (including alias), property order 1, editing 2, mocked note refinement 4. Cypress 16.2.1/headless Electron 146, Nix Node 26.13.0, isolated E2E backend and private OpenAI mock. | The route and current behavior are observed; new presentation/result/advance assertions belong to their implementation slices. |

The E2E baseline used allocation `wt_13e81ca92be2431987bf46f873299db5`, test
database `doughnut_e2e_wt_13e81ca92be2431987bf46f873299db5`, and backend/Vite/browser
ports 65146/65147/65148. Before launch the local host had 16 logical CPUs,
48 GiB RAM, falling load 14.38 and 72% free memory; no other E2E/Gradle owner was
running. A sequential six-feature batch was selected, leaving unrelated MySQL
processes alone. Runner, SUT and Gradle exited; owned ports stopped listening and
runtime locks were removed. No paid-service option was selected. This observation
does not establish headroom on another host; inspect it before later heavy runs.

Backend tracker independence and skip grain are unchanged domain contracts,
traced in the seed to `MemoryTrackerAssimilation`, `AssimilationSequenceSkip`
and property selection. Frontend mock-based observations above do not establish
backend persistence; the focused walkthrough owns the real persisted journey.
Layout fit and comprehension are design hypotheses; the future geometry and
visible action/result checks establish the promised affordances, not a measured
learning benefit.

## Ordered slices

### 1. Read the note and choose a clear default on a wide screen
Type: Behavior
Status: done
Proof: The first two proof rows, through mounted notebook workspace/panel tests;
run the recall-panel tests and affected NoteToolbar/NoteTextContent tests.

Behavior: A learner opens note-level assimilation at wide available width →
the title appears before Understanding and the two described optional modes →
Understanding has **Assimilate understanding**, Spelling has **Verify spelling**,
and Commissioned has **Add to sessions**, with their icons and existing tracker
links/dates. Optional actions keep the same note and spelling verification hides
the note until verification/cancel finishes. Keep existing editable note content
and property controls intact.

Change placement/presentation through the existing reading and action owners.
Rewrite the panel's inherited assertions about retired UI as positive proof of
this hierarchy; delete assertions/fixtures dedicated only to removed controls.
Interim behavior: Understanding still advances automatically until slice 2;
compact presentation remains the existing placement until slices 4–5.
Sizing: target 5 minutes, scrutinized 5–8 minutes for enclosing title placement
and focused proof; do not begin a >10-minute implementation without finer cuts.

### 2. See the Understanding result and deliberately continue
Type: Behavior
Status: done
Proof: Proof rows 3–5. Mounted workspace covers optional reload/remount and
same-note property; panel tests preserve count/refresh; walkthrough covers the
persisted result/advance journey and exhaustion.

Behavior: Understanding succeeds → current title/note and checkmarked
**Understanding added to recall** appear with **Next item** → optional additions
update their tracker status while the result remains → only explicit Next item
requests the next unit. Preserve property-row auto-advance, daily count and
recall refresh. Empty next response keeps the note and uses existing exhaustion
feedback/dismissal; reaching the daily target still permits the next unit.

Own the result at a same-note lifetime surviving optional refresh, not merely in
the panel keyed by NoteShow. Do not infer this visit's success from any
pre-existing tracker. The mounted proof continues by opening a different note
with pre-existing Understanding and observing its ordinary tracker link/date,
establishing the result belongs to the successful visit. Keep mutations and
navigation in their existing owners.
Align walkthrough/table helpers and their actual consumer scenarios in this
slice: keep “assimilate” and “advance” separate actions so helpers cannot hide
the intermediate result. Note creation shortcuts need only their original
tracker-creation outcome. Replace slice 1's automatic-advance interim behavior.
Sizing: target 5 minutes, scrutinized 5–8 minutes plus recorded runner wait;
consumer alignment belongs here, not an unowned follow-up.

### 3. Explain and restore note-level sequence participation
Type: Behavior
Status: planned
Proof: Proof row 6. Mounted confirmation and walkthrough use a note with a
property and independent optional modes; include cancel, confirm and restoration.

Behavior: A pending note-level Understanding entry is offered → Skip from
sequence… explains Understanding stays out until restored, while properties
and optional modes remain available → confirmation advances. On revisit,
Return to sequence restores its Understanding eligibility and stays on the
note; direct Understanding remains usable and optional additions preserve skip.

Reuse existing skip storage/actions and popup confirmation. Keep explanatory
copy appropriate to the calling unit; changing a shared message must not make a
property-row confirmation claim it is skipping note Understanding.
Sizing: target 5 minutes; one participation/restore proof loop, existing backend
rule unchanged.

### 4. Reach optional modes from compact disclosure
Type: Behavior
Status: planned
Proof: Proof row 7. Mounted workspace at compact width opens Optional modes,
operates both modes/statuses, closes with keyboard/outside interaction and
returns focus; spelling verification remains the opaque top interaction.

Behavior: Available panel width is compact → learner opens **Optional modes**
once → the same Spelling and Commissioned descriptions, actions and tracker
states are accessible in a popover. Verification/cancel or adding a mode keeps
the note and any current Understanding result.

Use existing anchored-dropdown support, avoiding duplicated mode state or a new
overlay stack. Scope keyboard handling to this disclosure and reuse Modal for
the existing verification. Interim: compact controls may still sit above reading
content; slice 5 replaces that placement.
Sizing: target 5 minutes; bounded to disclosure/overlay behavior, not bottom docking.

### 5. Read first with accessible bottom actions
Type: Behavior
Status: planned
Proof: Proof row 8 plus the slice 4 disclosure interaction in its final position.
Browser geometry uses production CSS at 375px, 768px and narrowed workspace,
with long content and a long title; run focused layout tests and final checks.

Behavior: At compact available width, the learner sees title, properties and
content before actions → scrolling keeps **Assimilate understanding**, **Skip
from sequence…** and **Optional modes** accessible in a bottom action bar →
content's final line, labels and popup remain reachable and unclipped. After
Understanding, the same bar presents the result/Next item and optional access.

Integrate the current note surface and scroll owner without rendering a second
editable note or retaining two independent action states. Account for safe-area
spacing and notebook chrome. Select the threshold from measured available space;
exercise width changes as well as both static presentations. Replace slice 4's
above-content compact placement. Keep wide layout from slice 1 green.
Sizing: target 5 minutes, scrutinized 5–8 minutes for scroll/placement integration.

## Story obligations

### O1. Understanding still advances automatically
Reported: slice 1 — "Understanding still advances automatically (slice 2)."
Story clause: "The sequence waits at the result until the learner chooses to advance."
Disposition: proved by slice 2: frontend/tests/components/recall/AssimilationPanel.spec.ts 'waits on the note at the Understanding result until Next item requests the next unit' (stays at the note until clickNextItem); NotebookWorkspaceUnderstandingResult.spec.ts result tests; assimilation_walkthrough.feature 'Assimilating waits at its result until Next item…'

### O4. The result stays for the rest of that note visit after Next item opens a same-note property
Reported: slice 2 — "When Next item goes to a property on the same note, the panel closes but the result stays set for that note. Reopening the panel during that visit shows the result and Next item again."
Story clause: "After Understanding succeeds, keep the current note and title visible in a temporary result state."
Disposition: no user cost "decide when to advance after seeing what was added to recall": inspected useMemoryTrackerActionHandlers (set only on this visit's Understanding success, reset on noteId change), NoteAssimilationModes (renders the result for that note) and AssimilationPanel (Next item calls the unchanged useGoToNextAssimilation). Reopening the note-level panel on the same note page shows what this visit added and Next item still requests the next unit in existing order; opening another note resets it (NotebookWorkspaceUnderstandingResult.spec.ts reset observations).

### O2. Skip from sequence… keeps the existing skip confirmation
Reported: slice 1 — "\"Skip from sequence…\" still opens the old confirmation text (slice 3)."
Story clause: "Skip from sequence… opens a confirmation explaining that only this note's Understanding entry stays out of the sequence until restored"
Disposition: interim until slices 3

### O3. Compact widths stack the wide layout after the title
Reported: slice 1 — "At compact widths the panel now sits after the title with stacked columns, not above the title as before. Slices 4–5 replace this"
Story clause: "Around iPad portrait width and below, use Read first."
Disposition: interim until slices 4, 5

## Execution notes

- Execution: story-branch on `claude/choose-how-to-assimilate-a-note-and-when-to-move`
  in this worktree; published claim `8f91daebcd` on `origin/main`.
- Local E2E from a non-interactive shell needs `OPENAI_API_TOKEN` set to any
  non-empty placeholder; the backend OpenAI client reads only that variable,
  and without it mocked-OpenAI features fail with `This request requires apiKey`.
  Nix commands also need `NIX_CONFIG="extra-experimental-features = nix-command flakes"`.
- Slice 1 diagnosis: `e2e_test/features/learning_session/commissioned_learning_session.feature`
  "Recording the tutor's report closes the session and reschedules each tracker"
  fails locally only at its last step (`199 hours between last and next recall`,
  observed 240). Cause: the Grade interval fuzz is seeded by memory tracker id
  (`MemoryTrackerRecallIntervalFuzz.intervalHoursForGrade`), and the reused
  worktree E2E database allocates different ids than CI's fresh database. It
  failed identically on an untouched checkout of `8f91daebcd`; main CI is green.
  Every earlier step, including those using this slice's page objects, passed.
  Disposition: outside this story's changed boundary; branch CI observes the
  scenario. Remaining defect: the scenario asserts an id-seeded fuzzed value.

- Slice 2 learning: note-level Understanding success now emits `reloadNeeded`,
  so `NoteShow` reloads the realm and remounts the panel (as optional additions
  already did) and focus falls back to the page. Slice 5's bottom bar should
  keep the result and Next item reachable after that remount.
- Slice 2 test support: `frontend/tests/helpers/toastTestSupport.ts` now
  re-appends the toast container before each test, because the workspace spec
  empties `document.body` between tests.

## Verification and delivery

Use Nix for repo commands. Focused mounted tests are the primary local feedback;
all frontend acceptance also requires `pnpm -C frontend exec vue-tsc --noEmit`
and `pnpm -C frontend exec tsc --noEmit`, per the frontend skill. Run
`CURSOR_DEV=true nix develop -c pnpm frontend:test` on the final implementation.
If components are added/deleted, regenerate `frontend/components.d.ts` with
`CURSOR_DEV=true nix develop -c pnpm -C frontend exec vite build --emptyOutDir`.
Backend/API generation is conditional on an actual API change, not planned here.

Focused real-journey command (private mock, no paid OpenAI):

```bash
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/assimilation/assimilation_walkthrough.feature,e2e_test/features/assimilation/assimilation_page_types.feature,e2e_test/features/assimilation/assimilate_with_remembering_spelling.feature,e2e_test/features/assimilation/assimilation_property_order.feature,e2e_test/features/assimilation/edit_when_assimilating.feature,e2e_test/features/assimilation/note_refinement.feature
```

Run affected scenarios at their owning slice; this bounded batch covers the
changed action/placement consumer family. Before broader launches, inspect actual
runner load/memory and known competing work. Test runner and isolated stack
startup are a stated external-wait exception to the 10-minute leaf limit;
implementation/cleanup exceeding that limit still requires finer decomposition.
Do not launch a full E2E suite or add paid-service proof.

Consumer trace: the new hierarchy reaches NoteShow/NoteToolbar, mounted workspace,
panel/loading/tracker tests and Storybook. The shared action chain also reaches
RichFrontmatterPropertyPanel and its mounted editor tests. E2E
`assimilationFlow.assimilateOnPanel` and table-shaped `assimilateOneNote` feed
walkthrough/page-type features; `noteMoreOptionsForm.assimilateNote` serves
tracker-creation setup in recall/daily-probe journeys. Preserve those purposes
rather than globally adding an implicit Next action. At each actual product diff,
run the installed consumer-trace script with the old labels, changed action
contracts, helper names and step phrases; inspect each consumer's assertions
and record run/reused/excluded dispositions. Extend proof to affected consumers
outside this family when the actual trace reaches them.

Removal reading: once at acceptance, search the whole product for any dedicated
symbols/selectors actually retired by the implementation, classify shared names
that remain in use, and delete orphan-only dependents. This is a reading separate
from proof; add no absence test, guard or historical UI explanation. Do not
search for generic retained property actions as if they had been removed.

Apply the repository execution wrap-up for each leaf: Jidoka → fresh independent
post-change refactor agent → API generation if needed → coordinator's single
`./scripts/run.sh pnpm format:changed` → update this plan → agent commit with
check-only hook → push. Keep the active plan through retrospective/Story Wrap Up.
Wrap Up owns removal of the spent mockup and planning history.

## Plan review

The common model is one note's tracker data, one transient success result and
existing sequence operations, rendered in two responsive presentations. Five
Behavior leaves keep the progression, participation, disclosure and scroll proof
loops evaluable; no preparatory Structure or speculative shared framework is
needed. Compact disclosure and docking are separate because their overlay and
scroll integration have different failure/recovery boundaries. The named interim
placements/advance behavior have explicit replacing slices.

Independent boundary review retained all five slices; an extra slice-plan
refinement pass was not needed because each leaf has one coherent proof loop
and the compact overlay/scroll integration is already separated. The review's
result-lifetime proof mapping was tightened in slice 2 without adding scope or
a new boundary. The focused full-stack baseline also passed. No remaining
slice-specific design, sizing, proof-mapping or decisive-premise concern was
identified in the reviewed plan; the new behavior still requires its owning
slices' implementation and acceptance proof.
