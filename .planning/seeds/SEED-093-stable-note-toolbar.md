# Stable note toolbar in Safari

<a id="story-1"></a>
### Toggle note edit modes without moving the toolbar

**Identity:** SEED-093#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planless","assessment":"ready","reasons":[],"basis":{"document":"5ad8eb55e38991e42273073c6c9c352cf9b4dea57fb1c9e6e1c6f6f48e256640"}}
```

**Goal:** Fix the Safari on Mac toolbar jump and gray flash when the developer switches between rich-content and Markdown editing.

**Expected behavior:** The toolbar keeps its position through both directions of the edit-mode toggle, with stable scrolling and no transient gray scrollbar beside Delete. Preserve access to overflowing toolbar actions.

**Observed behavior and evidence:** The developer's 2026-10-29 recording, `/Users/terryyin/Desktop/Screen Recording 2026-10-29 at 12.09.43 PM.mov`, captures a gray scrollbar-like thumb immediately right of Delete at 1.150–1.217 seconds and 1.983–2.117 seconds. The toolbar shifts left and back during the edit-mode toggle. Frames 032, 033, and 037 extracted in this chat preserve the before, transient, and settled states. Earlier deployed Safari measurements of Export and Questions dialogs also showed a 17-CSS-pixel toolbar shift, but the recording's edit toggle is the required acceptance journey.

**Scope and authority:** The developer explicitly requests the repair and Safari verification. Diagnose, add the smallest failing regression proof, repair the responsible layout behavior, run related automated proof and the shared refactor pass, then deliver and verify the edit-mode toggle in Safari. No production deployment is requested. Bounded planless repair, no replanning, ten-minute implementation attempt.

**Uncertainty:** Identify which scrolling or layout surface changes during toggling; prove the exact edit-toggle journey in Safari against the repaired candidate.

**Completed outcome and proof (2026-10-29):** Candidate `ba0f3568cb4ae19bd01ad699cd79fc38e2857f9b`, based on `ab01867c292cfc6aef45f3c2965311490e24784d`, adds vertical clipping to the shared horizontal command scrollport. Native Safari on Mac reproduced the recording's edit toggle before repair: content briefly measured 33px inside a 32px port, enabling a vertical scrollbar and shifting commands by 17 CSS pixels. After repair, six alternating real 150ms presses retained wiki/Trash positions 2101/2502 and client width 2263 while the same 33px transient had computed overflowY hidden. Two further toggles on a longer note also stayed fixed. The listeners were bounded and removed; the owned local Safari tab and held stack were closed. These observations verify the local repaired candidate, not a production deployment.

The routed workspace regression failed with the original class in headless WebKit (twelve vertically scrollable 33/32px states), then passed with the repair (5 tests in its geometry file). After independent test-only refactoring, the canonical geometry file passed 5 tests. `./scripts/run.sh pnpm frontend:test` passed 397 files / 2452 tests; `./scripts/run.sh pnpm -C frontend exec vue-tsc --noEmit` and `./scripts/run.sh pnpm -C frontend exec tsc --noEmit` passed; `./scripts/run.sh pnpm cy:run --spec e2e_test/features/note_creation_and_update/note_edit.feature` passed all 14 scenarios including both edit-mode changes. Source-only review and fresh post-change refactoring completed; the shared scrollport consumer trace retained existing navigation, overflow, and Recall proof. No API generation trigger changed. Detailed launches, inspected setup/assertion locations, consumer dispositions, and exact Safari measurements remain in this execution conversation.
