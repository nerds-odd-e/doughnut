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

### Diagnose automatic mobile keyboard presentation

**Identity:** SEED-086#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal:** Establish the likely cause of the reported keyboard regression with
code/history evidence, platform evidence, and clearly stated reproduction gaps.

**Scope:** Investigate shared focus handling and the search and spelling
journeys; compare relevant recent changes with WebKit behavior. This instruction
authorizes diagnosis only; product repair requires later authorization.

**Expected:** Showing a search or spelling-answer input automatically presents
the soft keyboard, as the owner observed before the regression.

**Actual:** The owner reports that automatic keyboard presentation recently
stopped after product changes or an iOS 27 upgrade.

**Known gaps:** Exact device/browser/standalone mode, OS build, last working
date, and which initial/subsequent transitions fail are not yet supplied.

**Evaluation:** Trace both journeys through shared focus code and dated history;
separate DOM focus evidence from real iOS software-keyboard presentation.

**Effort hypothesis:** S, with device reproduction availability uncertain.
