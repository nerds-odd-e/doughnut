---
status: resolved
trigger: "G-01-3 — LoadingModal long-message content is clipped at a narrow viewport"
created: 2026-07-21T06:52:37Z
updated: 2026-07-21T07:45:00Z
resolved_by: 01-03
---

## Current Focus

hypothesis: Confirmed root cause; diagnose-only investigation complete.
test: Complete.
expecting: N/A.
next_action: Parent/orchestrator can plan a LoadingModal-scoped overflow-safe centering fix and a 320x568 browser regression test.

## Symptoms

expected: The spinner, message, and control remain usable without clipping or horizontal overflow, and existing typography/layout are unchanged.
actual: Automated UAT probe reported that the real LoadingModal passes at 1280x720, but at 320x568 the long-message stack begins 103.5px above the viewport and the spinner and Cancel control are completely off-screen.
errors: Browser-rendered assertion failed because `content.getBoundingClientRect().top` was `-103.5`; narrow screenshot showed only the middle of the message.
reproduction: Test 3 in `.planning/phases/01-shared-cancellation-contract/01-UAT.md`. Browser-render `LoadingModal` with the same 20x repeated `longMessage` from `frontend/tests/components/commons/LoadingModal.spec.ts` at 320x568 and check content/message/button bounds.
started: Discovered during automated UAT on 2026-07-21.

## Eliminated

- hypothesis: The cancellation state/action binding places or removes the wrong elements.
  evidence: The real DOM contains exactly the expected spinner, long message, and identity-bound Cancel; their rectangles follow the declared flex stack exactly, and changing only CSS alignment/overflow restores reachability without touching state.
  timestamp: 2026-07-21T07:05:11Z
- hypothesis: Horizontal overflow or missing word breaking is the direct cause of G-01-3.
  evidence: At 320px the overlay and content are both exactly 320px wide and the spaced message wraps to 675px height; the failed elements are outside the vertical bounds. The removed `max-width`/`overflow-wrap` rules do not create a vertical scrolling path.
  timestamp: 2026-07-21T07:05:24Z
- hypothesis: The new 40px Cancel control alone creates the viewport failure.
  evidence: Without Cancel, the measured 28px spinner, 16px gap, and 675px message total 719px, already exceeding the 568px viewport by 151px; Cancel plus its gap worsens but does not originate the failure mechanism.
  timestamp: 2026-07-21T07:05:42Z

## Evidence

- timestamp: 2026-07-21T06:55:22Z
  checked: `.planning/debug/knowledge-base.md`
  found: The knowledge base does not exist, so there is no known-pattern candidate.
  implication: Form hypotheses from current source and rendered behavior.
- timestamp: 2026-07-21T06:56:03Z
  checked: `LoadingModal.vue`, `Overlay.vue`, the UAT report, UI specification, and current component test
  found: `Overlay` is a fixed `width: 100%; height: 100%` flex box with `align-items: center; justify-content: center`; `.loading-modal-content` is an unconstrained vertical flex stack; the long-message test checks text alignment, max width, wrapping mode, and button existence but no viewport or element bounds.
  implication: The production layout has no vertical overflow/scroll strategy, and the current test cannot detect off-screen stack endpoints.
- timestamp: 2026-07-21T06:56:44Z
  checked: Common bug pattern checklist
  found: No null, state, async, import, type, environment, data-shape, regex, error-handling, or closure pattern matches; the symptom is a CSS viewport boundary/overflow case absent from that checklist.
  implication: Test the open-ended layout-boundary hypothesis directly in a real browser.
- timestamp: 2026-07-21T07:00:18Z
  checked: Real Chromium render of `LoadingModal` at 320x568 with the exact 20x long message and Cancel control
  found: Overlay rect is 320x568 at top 0. Content is 775px tall at top -103.5 and bottom 671.5; `(775 - 568) / 2` equals 103.5 exactly. Spinner is entirely above (`-103.5..-75.5`), message spans `-59.5..615.5`, and Cancel is entirely below (`631.5..671.5`). Computed overlay styles are `align-items: center`, `justify-content: center`, `overflow-x/y: visible`.
  implication: The hypothesis is directly confirmed: flex centering splits the 207px vertical excess evenly outside the fixed-height viewport, and there is no declared scrolling strategy.
- timestamp: 2026-07-21T07:02:06Z
  checked: Chromium counterfactual probe at 320x568
  found: With only `align-items: flex-start`, content top becomes 0 but `overflow-y: visible` keeps `scrollTop` at 0 and Cancel remains below the viewport. With only `overflow-y: auto`, Cancel becomes scroll-reachable but the already-negative spinner remains unreachable. With `align-items: safe center` and `overflow-y: auto`, the oversized content starts at 0 and Cancel is reachable at the scroll end.
  implication: Both unsafe centering and absence of a scroll container are causal; changing message typography is not required to restore usability.
- timestamp: 2026-07-21T07:04:01Z
  checked: Source history and `Overlay` call sites
  found: `Overlay`'s fixed-height centered-flex behavior predates this phase; adding Cancel contributes 56px (40px control plus 16px gap), but the 675px message already makes the 320x568 stack too tall. Commit `328b0d003f` removed message `max-width`, `overflow-wrap`, and `text-align` rules to preserve legacy typography; those horizontal/text rules do not provide a vertical overflow path. `LoadingModal` is the only product source consumer of `Overlay`.
  implication: This is a latent vertical overflow defect exposed by the long-message-plus-control UAT, not a cancellation callback/state bug or a horizontal wrapping regression. A narrow overlay/layout change can address it without altering typography.
- timestamp: 2026-07-21T07:05:03Z
  checked: Committed `frontend/tests/components/commons/LoadingModal.spec.ts` in Chromium
  found: All 11 tests pass unchanged, including `preserves the existing message layout with the optional action`.
  implication: The regression suite has a viewport-coverage gap: it asserts computed typography/layout declarations and DOM presence at the default viewport but never checks element bounds or reachability at 320x568.
- timestamp: 2026-07-21T07:06:18Z
  checked: Temporary-probe cleanup
  found: The temporary browser spec and its two generated screenshots were removed after measurements and counterfactual testing.
  implication: Diagnosis leaves no product/test probe artifacts behind.

## Resolution

root_cause: `Overlay.vue` gives the LoadingModal root a fixed 100%-height flex box with unsafe vertical `align-items: center` and default `overflow-y: visible`. At 320x568 the exact long-message stack is 775px tall, so centering places its top at -103.5px and bottom at 671.5px. Negative-start overflow cannot be scrolled into view, and the fixed overlay provides no scrolling path to the bottom; therefore the spinner and Cancel are unreachable. The committed long-message test checks declarations/presence only, so it misses viewport bounds.
fix: Not applied in diagnose-only mode. Use overflow-aware safe centering and vertical scrolling on the LoadingModal overlay surface while preserving the existing message typography and 16px stack gaps; add a browser-rendered 320x568 bounds/reachability regression test using the exact long message.
verification: Reproduced exactly in real Chromium (`content.top = -103.5`, height 775, spinner fully above, Cancel fully below). Counterfactual probe proved start alignment alone and scrolling alone are each insufficient, while `safe center` plus `overflow-y: auto` makes both endpoints reachable. Existing committed component suite remains green (11/11), confirming the coverage gap.
files_changed: []
