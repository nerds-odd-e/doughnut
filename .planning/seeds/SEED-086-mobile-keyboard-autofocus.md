---
id: SEED-086
status: dormant
planted: 2026-10-25
planted_during: owner-reported mobile keyboard regression investigation
trigger_when: search and spelling inputs fail to summon the soft keyboard
scope: small
---

# SEED-086: Restore automatic mobile keyboard presentation

## Why This Matters

The owner reports that search, spelling-answer, and other inputs previously
opened the soft keyboard when shown, but recently stopped. Recent product
changes and an upgrade to iOS 27 are both possible causes.

## Story Decomposition

<a id="story-1"></a>

### Restore automatic mobile keyboard presentation

**Identity:** SEED-086#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planless","assessment":"ready","reasons":[],"basis":{"document":"6a7992500547b438c11604ebee8d319ca7e7276c932b207467e4f5bced7f8ce0"}}
```

**Goal:** Opening main-menu search, opening link-target search directly from
the note toolbar, and resuming a paused spelling question focus the editable
input promptly and preserve the mobile keyboard handoff.

**Scope:** Repair the shared focus timing and native-dialog focus handoff for
the three reported journeys, with cohesive shared behavior and regression
coverage. The owner explicitly authorized this repair on 2026-10-25. Preserve
the working MCQ-to-spelling keyboard handoff. No operating-system detection,
retry timers, new per-launch workarounds, or unrelated menu redesign.

**Expected:** Showing a search or spelling-answer input automatically presents
the soft keyboard, as the owner observed before the regression.

**Actual:** The owner reports that automatic keyboard presentation recently
stopped after product changes or an iOS 27 upgrade.

**Known gaps:** Exact device/browser/standalone mode, OS build, last working
date, and which initial/subsequent transitions fail are not yet supplied.
The follow-up observations below supersede these initial report gaps.

**Evaluation:** Trace both journeys through shared focus code and dated history;
separate DOM focus evidence from real iOS software-keyboard presentation.

**Effort hypothesis:** S, with device reproduction availability uncertain.

**Authorized repair approach:** One bounded contextual bug-repair slice under
dough-bug-fixing's planless execution handoff, with `--no-replan` and its
ten-minute implementation limit. First confirm failing boundary proofs for
editable focus before the next animation frame; then keep shared focus within
Vue's DOM update and prepare the actual marked control for native dialog
autofocus before opening. Run focused regression tests, independent cohesion
refactoring, frontend unit tests, both frontend typechecks, selective formatting,
and ordinary delivery. Browser proof establishes focus continuity/timing;
real iOS software-keyboard presentation remains a reporter-validation limit.

**Key examples:** Both main-menu search launchers focus the search field as
their dialog opens; the direct note-toolbar link icon does the same; both
compact Resume and expanded-menu Resume restore a paused spelling field as
its view becomes visible; MCQ-to-spelling still primes and focuses the answer.

## Investigation evidence — 2026-10-25

**Checkout:** `/Users/terryyin/git/doughnut/.worktrees/ios-keyboard-investigation`,
branch `codex/ios-keyboard-investigation`, created for this investigation from
`c43b6f3cbaef0c71d52c86fd2f7c579ee12b59bb`. Admission commit
`537586c54f52ce8511c0f758ecb0a74cd048949f` owns the existing Taken story.
Originating/integration checkout: `/Users/terryyin/git/doughnut`. Remote target
for the admission was `origin/main`. No product repair is authorized or made.

**Reporter update:** It perhaps worked earlier this week. Both Safari and
Chrome on iOS 27.0.1 exhibit the issue. Whether the first/resumed spelling
question or consecutive questions fail, whether a caret is visible, and whether
direct tapping opens the keyboard remain unconfirmed.

**Code/history readings:**

- `frontend/src/utils/focusTarget.ts:24` primes the keyboard by focusing the
  persistent `SoftKeyboardPrimer` only on coarse-pointer devices. The primer is
  a clipped `sr-only` text input. This implementation dates to `21cffa1e52`
  (2026-05-29), and the primer component has not changed since introduction.
- Visible-input focus uses `nextTick` followed by `requestAnimationFrame`.
  This asynchronous handoff predates the reported week. The 2026-10-20
  `b94786fa6a` extraction of the shared directive preserves ordinary `v-focus`
  behavior and adds the explicit-false opt-out; current search and spelling
  use ordinary `v-focus`.
- Search moved through menu arrangements and back into the shared dialog on
  2026-10-20 (`ac60f83ff1`, `39503b5d1d`). `openIntoSearch` still primes
  synchronously before changing visibility. Native `Modal.showModal()` may
  initially focus the history summary, whose focus handler immediately blurs.
  The history blur dates to `130d1f139c` (2026-05-15); native modal presentation
  also predates this week. The old global-search pairing used the same primer,
  native Modal, and search content. This is an existing vulnerability, not
  evidence that the recent search rearrangement introduced it.
- Spelling submission changes (`a8b2d2d66c`, 2026-10-21) retain synchronous
  priming before the network request when the next tracker is spelling.
  The shared question-loading change (`8ee3143db6`, 2026-10-21) remounts the
  answer input and retains `v-focus`.
- The new Daily probe Start/Skip gate (`f338f52a9d`, 2026-10-20) has no primer
  call when Skip reveals ordinary recall. That is a separate plausible gap
  for entering spelling after Skip; it cannot explain general search failure
  or establish an iOS regression.

**Automated observations (headless Chromium, production CSS):**

- Worktree setup succeeded through `./scripts/run.sh bash
  scripts/worktree_setup.sh` against the locked dependency installation.
- The focused existing specs for DonutMenu touch search, SpellingQuestionDisplay,
  RecallPromptComponent, useRecallData, and PopButton passed: 5 files, 24 tests.
- A disposable touch-spec probe mounted the real menu, real Modal, shared
  directive, and primer. Observed focus trace:
  `primer focus → primer blur → history summary blur → BODY → search input focus`.
  The trace assertion and final visible-input focus passed. An initial probe
  run failed only because this repository forbids `console.log`; replacing
  logging with trace assertions passed. That harness failure was not a
  reproduction of the reported keyboard failure.
- A second disposable touch-spec probe mounted the real RecallPage with its
  real directive, two spelling trackers, and mocked external HTTP responses.
  The first answer received focus, submitting synchronously focused the
  primer, and the next answer received focus after loading. It passed.
- The temporary files were
  `frontend/tests/toolbars/KeyboardInvestigation.touch.spec.ts` and
  `frontend/tests/pages/KeyboardInvestigation.touch.spec.ts`; they are removed
  after this record is written. No production test or product code is retained.

**Platform evidence:** Apple's WebKit bug 195884 explains that software-keyboard
presentation without a hardware keyboard is gated on user interaction, and
that ordinary programmatic DOM focus does not guarantee keyboard presentation:
<https://bugs.webkit.org/show_bug.cgi?id=195884>.
The native embedding API exception in bug 243416 / WebKit PR 2907 does not
grant ordinary website JavaScript unrestricted keyboard presentation:
<https://github.com/WebKit/WebKit/pull/2907>.
Safari 27 release notes were reviewed; no explicit announcement matching
this reported regression was found:
<https://webkit.org/blog/18325/webkit-features-for-safari-27-0/>.

**Disposition / limits:** Unconfirmed cause. The shared workaround is present
and DOM focus still succeeds in the tested search and consecutive-spelling
journeys. An iOS 27.0.1 compatibility change affecting hidden-input priming or
asynchronous transfer is plausible, not reproduced or established. The search
focus interruption is verified on Chromium, not on the reporter's iPhone.
No iOS simulator is available (`xcrun simctl` unavailable), and desktop/touch
emulation cannot validate a real iOS software keyboard. No claim of repaired,
explained-no-change, or reporter-confirmed resolution is made. This admitted
story stays Taken with its additional evidence as a local unpublished draft.

**Decisive next observation:** On the affected iPhone, compare DOM activeElement
and actual keyboard state for direct input tapping, synchronous primer focus,
primer-to-input transfer, search-dialog opening, entering recall after Daily
probe Skip, and consecutive spelling questions. Compare a known earlier app
revision on the same iOS build, or the same app revision on the previous iOS,
before attributing the regression to either update. A repair needs separate
authorization and iOS software-keyboard verification.

## Follow-up investigation — 2026-10-25

**Reporter observations:** On iOS 27.0.1, spelling after an MCQ still opens
the keyboard. Resuming a spelling question through the main menu does not.
Main-menu search is reported without a blinking cursor (interpreted as no
input focus) and without the expected keyboard. Link-target search also fails
to open the keyboard when launched using the direct note-toolbar icon, not
the more-options menu. Tapping directly in the input opens the keyboard.
These observations supersede the unconfirmed journey/caret/direct-tap gaps
in the first report. The owner associates the failures mainly with the new
main menu. They do not establish which revision or OS upgrade introduced them.

**Revised interpretation:** The reporter's successful MCQ-to-spelling handoff
shows that hidden-input keyboard priming still works in at least one real iOS
journey. A blanket failure of that workaround is not supported. The affected
set consists of input launch/resume actions. Both search launchers use the
same Modal and SearchForNoteAndFolder. The direct toolbar icon calls
PopButton.onTriggerClick, which does prime the keyboard. The separate
more-options route bypasses priming via openDialog(), but the reporter's
direct-icon clarification rules that out as the explanation of this report.
Daily probe Skip is not part of the clarified reproduction either.

**Additional code/history readings:**

- Main-menu actions became native buttons in `29333c5875` (2026-10-20).
  The collapsed current-activity Resume button was introduced by `b3cd0c7e27`
  later that day. Both still invoke the real useResumeRecall synchronously.
- The existing RecallPage spelling-resume test sets shouldResumeRecall directly;
  it does not click through the main menu. New disposable probes exercised
  the real compact and dropdown Resume buttons instead.

**Additional automated observations:** On the same investigation checkout,
five disposable scenarios used real browser click sequences (`userEvent.click`),
the production focus directive, real components/router, production CSS, the
real primer, and only external backend HTTP mocks:

1. Pause a spelling question by viewing a previous answer, then use compact
   Resume: the spelling answer regains DOM focus.
2. The same pause followed by Resume inside the expanded main menu: the
   spelling answer regains DOM focus.
3. Main-menu search via the input-shaped Open search launcher: the primer
   receives focus and the visible search input receives final focus.
4. Main-menu search via the Search icon: same observable result.
5. Link-target search via the direct note-toolbar icon: the primer receives
   focus and the visible search input receives final focus.

All five passed in headless Chromium touch context and in Playwright 1.64.1's
desktop WebKit 27.2 with touch/mobile emulation. A sixth WebKit probe paused
scheduled animation-frame focus and confirmed the search interruption before
the final input focus:
`primer focus → primer blur → history summary blur → BODY`.
This is now observed in both engines, not inferred solely from Chromium.
The WebKit trace assertion and both final-search-focus checks passed.

Commands: ordinary `pnpm frontend:test` for the temporary Chromium files;
`pnpm -C frontend exec vitest run --config
vitest.keyboard-investigation.config.ts` for the temporary WebKit configuration,
each through `CURSOR_DEV=true nix develop -c`. WebKit was downloaded into the
ordinary Playwright browser cache. No lockfile or product source was changed.
Desktop WebKit 27.2 is neither the reporter's iOS 27.0.1 engine nor an iPhone
software-keyboard test. These passes do not contradict the reporter's device
observations or prove that the behavior works on iOS.

Disposable paths removed after retaining this evidence:
`frontend/tests/pages/KeyboardResumeInvestigation.touch.spec.ts`,
`frontend/tests/toolbars/KeyboardOpenInvestigation.touch.spec.ts`,
`frontend/tests/notes/KeyboardToolbarInvestigation.touch.spec.ts`, and
`frontend/vitest.keyboard-investigation.config.ts`.

**Remaining uncertainty:** Search has a verified interruption of editable
focus, making its shared dialog handoff a supported candidate. The cause of
failed resume and of the missing final search focus on the reporter's iPhone
is not reproduced. A real-device event trace should capture focusin/focusout,
the primer and target activeElement, native dialog opening, menu closure,
and the scheduled focus callback while performing those exact actions. Do
not attribute this solely to iOS or solely to the menu rewrite without that
observation. Repair remains outside the authorized investigation scope.
The same admitted story remains Taken; this follow-up evidence is retained
locally and unpublished with the first investigation record.
