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
