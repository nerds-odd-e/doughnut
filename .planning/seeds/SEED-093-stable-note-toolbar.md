# Stable note toolbar in Safari

<a id="story-1"></a>
### Toggle note edit modes without moving the toolbar

**Identity:** SEED-093#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planless"}
```

**Goal:** Fix the Safari on Mac toolbar jump and gray flash when the developer switches between rich-content and Markdown editing.

**Expected behavior:** The toolbar keeps its position through both directions of the edit-mode toggle, with stable scrolling and no transient gray scrollbar beside Delete. Preserve access to overflowing toolbar actions.

**Observed behavior and evidence:** The developer's 2026-10-29 recording, `/Users/terryyin/Desktop/Screen Recording 2026-10-29 at 12.09.43 PM.mov`, captures a gray scrollbar-like thumb immediately right of Delete at 1.150–1.217 seconds and 1.983–2.117 seconds. The toolbar shifts left and back during the edit-mode toggle. Frames 032, 033, and 037 extracted in this chat preserve the before, transient, and settled states. Earlier deployed Safari measurements of Export and Questions dialogs also showed a 17-CSS-pixel toolbar shift, but the recording's edit toggle is the required acceptance journey.

**Scope and authority:** The developer explicitly requests the repair and Safari verification. Diagnose, add the smallest failing regression proof, repair the responsible layout behavior, run related automated proof and the shared refactor pass, then deliver and verify the edit-mode toggle in Safari. No production deployment is requested. Bounded planless repair, no replanning, ten-minute implementation attempt.

**Uncertainty:** Identify which scrolling or layout surface changes during toggling; prove the exact edit-toggle journey in Safari against the repaired candidate.
