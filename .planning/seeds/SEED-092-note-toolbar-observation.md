# Confirm note toolbar click stability

<a id="story-1"></a>
### Confirm whether clicking note actions shifts the toolbar

**Identity:** SEED-092#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"unselected"}
```

**Goal:** Give the developer current observable evidence of whether clicking note-toolbar buttons shifts the toolbar or briefly shows a small gray control beside Delete.

**Scope:** Observe the deployed Donut web application using an existing browser session. Compare the toolbar before and during read-only actions at desktop and narrow widths. Report reproduction steps and any coverage limits. No diagnosis, repair, or permanent tests are authorized.

**Expected behavior:** Toolbar controls retain their position when an action opens its normal menu or dialog.

**Observation budget:** About five minutes of browser observation: prepare the note and baseline, try representative actions, then repeat any observed shift for confirmation.

**Observed outcome, 2026-10-29 (Asia/Singapore):** On deployed https://doughnut.odd-e.com/n24119 in the user's Safari on Mac, opening Export shifted the first toolbar control from x=2084 to x=2101 CSS pixels and Trash from x=2485 to x=2502, beginning about 61 ms after pointer-down. A separate repeat opening Questions for the note reproduced the same 17-pixel shift after about 62 ms. Measurements sampled the rendered controls with requestAnimationFrame via a temporary, one-use Web Inspector listener; the owned Safari tab was closed afterward. The gray flash itself was not captured. Chrome checks of Export, link search, Questions, and switching editor mode stayed stable at the observed desktop, tablet, and phone widths. This observation confirms the reported toolbar movement in Safari; it establishes neither a root cause nor a repair.
