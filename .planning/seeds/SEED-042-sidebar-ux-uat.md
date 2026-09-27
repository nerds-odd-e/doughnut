---
id: SEED-042
status: dormant
planted: 2026-09-28
planted_during: owner request for a budgeted sidebar UX assessment
trigger_when: selecting sidebar navigation improvements from observed user experience
scope: small
---

# SEED-042: Find what prevents smooth, modern, stable sidebar navigation

## Why This Matters

Donut users navigate their notebooks and notes through the sidebar's file/tree
browser. Its experience should remain smooth, modern-looking, and stable across
different tree depths, note counts, navigation paths, and frontend cache states.

The owner reports a specific glitch: when a visited note outside the visible
portion of a long list is automatically brought to the first visible position,
the collapsed folder above covers the top half of the note row. This is a reported
observation to reproduce and characterize, not a confirmed diagnosis.

## Alternatives and Decision

Fixing only the reported visibility glitch would miss the broader experience
across tree size, navigation, and cache state. A broad sidebar redesign without
observing those journeys would select improvements prematurely. The owner chose
a one-hour manual UAT to gather concrete findings before selecting fixes.

Progressive loading is a possibility to evaluate from that evidence, not a
prescribed solution or an implementation commitment.

## Story Decomposition

Effort bands: S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours, including delivery.
The manual UAT has an explicit one-hour budget; setup and findings write-up should
be reported separately rather than silently consuming or extending that budget.
This seed captures the assessment story and authorizes no UAT run or fixes now.

<a id="story-1"></a>

### Identify sidebar UX fixes and improvements through a one-hour manual UAT

**Identity:** SEED-042#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planless","assessment":"ready","reasons":[],"basis":{"document":"1b58c61210d04383077b1730691203ae920b3272dc4079d663635f4dc0ae6767"}}
```

**Goal**

The product owner receives an evidence-backed list of defects to fix and UX
improvements to consider, so subsequent work can make sidebar notebook and note
navigation smooth, modern-looking, and stable.

**Scope**

- Start the assessment with a one-hour budgeted manual UAT of the sidebar,
  including its file browser, note tree browser, and notebook tree browser.
- Explore varied folder/tree depths and varied note counts, including folders
  with many notes and long lists that exceed the sidebar viewport.
- Visit or jump directly to a note deep in the tree and observe how the sidebar
  reveals its location, expands relevant paths, updates selection, and scrolls.
- Compare uncached and cached frontend note and folder information. Include
  partially cached combinations where practical, and document the state used;
  do not infer cache state solely from how quickly the UI responds.
- Observe automatic visibility when the visited note initially lies outside the
  viewport. Check whether positioning it as the first visible item leaves the
  full row visible and readable, including the reported overlap from a collapsed
  folder above it.
- Assess smoothness, visual presentation, and stability during browsing,
  expansion/collapse, scrolling, and note navigation. These scenarios are a
  starting set, not an exhaustive limit on exploration within the budget.
- Evaluate whether gradual/progressive loading would improve the sidebar's
  experience. Record the observed problem it would address, relevant tradeoffs,
  and whether evidence supports it, argues against it, or remains inconclusive.
- Deliver findings covering both defects and improvement opportunities, with
  suggested priority and candidates for later product backlog items.

**Key examples**

- A notebook has deeply nested folders and many notes → jump to a deep note
  outside the visible sidebar area → observe whether the correct path and
  selected note become visible smoothly and remain stable.
- Repeat comparable navigation with cached, uncached, and partially cached note
  and folder data → compare the visible experience and record any differences.
- A long list positions a visited note at its first visible row beneath a
  collapsed folder → inspect whether the entire note row is visible or its upper
  half is obscured, and record reproduction conditions.

**Output and evaluation**

Produce one findings report containing:

- Scenarios exercised, notebook/tree characteristics, cache setup, and actual
  UAT time; identify important scenarios not reached within the hour.
- For each defect: expected versus observed behavior, reproduction steps,
  relevant tree/cache conditions, user impact, and evidence where practical.
- For each improvement: the observed friction, intended user benefit, and a
  proportionate recommendation. Distinguish observations from hypotheses.
- Suggested priorities, proposed follow-up story outcomes, and a reasoned
  progressive-loading recommendation or the missing evidence needed to decide.

**Refinement decisions**

- Run the UAT against the local app started from this story's own execution
  checkout, logged in as a seeded local account, in a browser that renders the
  real sidebar (screenshots serve as evidence). Representative notebooks are
  built as local test data; that setup time is reported separately from the hour.
- Deliver the findings report as a `## UAT Findings` section in this seed, so the
  owner reviews it in the same place as the story before choosing follow-ups.
- Execute as one planless slice: the UAT and its report. No product code,
  automated test, or permanent tooling change is part of this story.

The owner can evaluate success by reviewing reproducible findings and actionable
recommendations. An unreproduced reported glitch must be recorded honestly with
the conditions tried; it must not become a confirmed defect by assumption.

- **For / why:** Users benefit from subsequent improvements selected using real
  sidebar behavior; the owner can judge which fixes deserve priority.
- **Value / learning:** Learn which navigation, rendering, loading, and cache
  conditions cause friction, and whether progressive loading addresses it.
- **Effort hypothesis:** M, medium confidence: one hour of UAT plus bounded setup
  and synthesis. The hour limits exploration, not a promise of exhaustive coverage.
- **Depends on:** A usable application and representative notebook data; no
  dependency on the login or frontend-update stories is established.
- **Safe stopping point:** The findings report is useful independently of later
  fixes. Record coverage gaps and inconclusive observations at the budget boundary.
- **Execution handoff:** Use
  [dough-manual-testing](../../.agents/skills/dough-manual-testing/SKILL.md)
  when this assessment is selected for execution.

## Ordering and Scope Reduction

The owner selected this as the third and last item in the current product backlog.
At the one-hour boundary, stop exploration and synthesize what was observed.
Preserve untested scenarios as coverage gaps rather than extending the UAT or
claiming complete coverage. Implementation fixes, sidebar redesign, progressive
loading, and automatic creation of follow-up backlog entries are deferred; the
output supplies candidates for a later product decision.

## Open Decisions

- Which findings warrant fixes or improvements, and in what order? Decide from
  the UAT report rather than selecting them in advance.
- Does the evidence justify progressive loading, or a simpler improvement?

## When to Surface

Select after the two preceding queued stories, or when the owner reprioritizes
sidebar UX assessment.

## Breadcrumbs

- Owner request on 2026-09-28: one-hour manual UAT across tree depth, note counts,
  deep-note jumps, and frontend cache variations; investigate the partially
  obscured first visible note; evaluate progressive loading; deliver findings
  that may become future product backlog items.
- Desired product qualities: smooth, modern-looking, stable sidebar behavior.
- Continuing session instruction: write directly on main, without committing.
