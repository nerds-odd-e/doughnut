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

## UAT Findings

UAT on 2026-09-28 against commit `ee8859e4fc` (E2E profile, local stack from the
story worktree, account `old_learner`). Setup (stack start plus data seeding)
took about 15 minutes outside the budget. Exploration took about 16 minutes of
the one-hour budget (07:51–08:07). It stopped once every reachable scenario
from the story's starting set, plus the sort and sidebar-collapse follow-ups,
had been exercised. The remaining gaps below need other browsers, real
hardware, or data this stack cannot provide cheaply, so more time would not
have closed them. Screenshots were taken during the session but are not
stored in the repository; the measurements below are the retained evidence.

### Setup and scenarios

- **Data:**
  - *UAT Deep Tree*: folders 7 levels deep, with sibling folders above and below
    each level (21 folders, 52 notes).
  - *UAT Long List*: a 150-note folder between six small folders (171 notes).
  - *UAT Wide*: 40 folders of 10 notes each.
  - *UAT Huge*: one 1,000-note folder.
  - *UAT Titles*: long note and folder titles at depth 4.
- **Cache states:**
  - Uncached: a full page load, which empties the in-memory listing and note
    caches.
  - Cached: in-app navigation within the same notebook (search, sidebar click,
    back/forward).
  - Partially cached: one folder's listing cached, then a jump into a folder
    not yet loaded.
- **Viewports:** 1440×900, 1280×560, and 390×844 (drawer), in light and dark themes.
- **Scenarios:**
  - direct and in-app jumps to deep, far, and first/last notes
  - expand and collapse
  - wheel scrolling
  - the path hint link
  - back/forward
  - creating a note
  - keyboard focus
  - long titles
  - re-sorting the tree
  - hiding and reopening the sidebar
- **Instruments:** screenshots plus measured scroll offsets, row and hint
  bounding boxes, layout-shift entries, long tasks, and API requests.
- **Invalid first attempt:** the first attempt drove the owner's Chrome while
  its window was hidden (`visibilityState: hidden`). There the reveal never ran
  because IntersectionObserver callbacks are paused. That was a test artifact,
  not a defect. All results below come from visible headless Chromium.

### Defects

1. **The revealed note is hidden under the sticky ancestor-path hint (High).**
   This reproduces the reported glitch.
   - *Expected:* after the sidebar brings the visited note into view, the whole
     row is visible and readable.
   - *Observed:* the note is scrolled to the top of the tree, and the sticky
     "Ancestor folders scrolled out of view" hint (a chevron-up plus folder name,
     which looks like a collapsed folder row) is drawn over it.
     - 1440×900, Long List, Note 120: the row spans 140–172px and the hint
       128–157px, so about the top half of the row is covered.
     - 1280×560, Deep Tree, Deep Target Note: the hint wraps to two lines
       ("Level 1 · … · Level 7", 128–177px) and covers the row completely.
   - *Reproduction:* open `/n/<id>` for any note far enough down to need
     scrolling, or reach it in-app. Also: scroll manually until a note's row
     sits half under the hint, open another note, then press Back. The row
     stays half covered (136–168px under a 128–157px hint), because a row
     that is partly visible is not scrolled again.
   - *Conditions:* identical for uncached, cached, and partially cached data,
     back navigation, a newly created note, dark and light themes, and the
     390px drawer.
   - *Impact:* on every far jump, users cannot read which note is selected at
     the moment the sidebar is supposed to show it. At depth in a short window
     the selection is invisible.
   - *Hypothesis (from reading the code, not verified):* the hint's sticky
     anchor has zero height, so the hint overlays rows instead of taking space.
     The reveal uses `scrollIntoView` with the default `block: 'start'`, and the
     scrollport's 0.75rem `scroll-padding-top` is smaller than the hint's height.
2. **Rows behind the hint cannot be clicked (Medium).**
   - *Expected:* a row that can be seen can be clicked.
   - *Observed:* clicking where a half-visible row shows under the hint does
     nothing. There is no navigation and no scroll.
   - *Reproduction:* in *UAT Long List*, scroll the tree so a note row sits
     half under the "Many Notes" hint, then click the visible half.
   - *Impact:* the user sees a row they cannot select until they scroll.
3. **The left-rail count badge overlaps the Assimilate icon (Low; outside the
   sidebar tree).**
   - *Expected:* the badge sits clear of the icon below it.
   - *Observed:* the "15/623" (later "15/1.6k") badge covers the top of the
     Assimilate icon at every desktop size tried.
   - *Reproduction:* log in as `old_learner` with notes due, then view any
     note page at 1280px or 1440px width.
   - *Impact:* cosmetic. It makes the rail look unfinished, but the icon stays
     recognisable and clickable.
4. **Console error at phone width (Low).**
   - *Expected:* no console errors.
   - *Observed:* at 390px, the console logs `<path> attribute d: Expected
     number, "… h --120 …"`, which suggests a malformed SVG path in the mobile
     layout.
   - *Reproduction:* open any note page with a 390×844 viewport.
   - *Impact:* not established. No visual breakage was traced to it, but some
     mobile shape is probably not drawn as intended.
5. **Search sends a burst of requests (Low; outside the sidebar tree).**
   - *Expected:* recent notes load once when search opens.
   - *Observed:* typing one query into note search sent `GET /api/notes/recent`
     15 times before the single `POST /api/notes/search`.
   - *Reproduction:* open note search and type "Quinces Note 2" at normal
     speed while watching network requests.
   - *Impact:* wasted server load and bandwidth. No visible slowness was
     observed locally.
6. **Re-sorting loses the selected note (Medium).**
   - *Expected:* after the sort order changes, the selected note stays in view.
   - *Observed:* with Note 120 selected and revealed, choosing *Sort sidebar →
     Title (Z–A)* reorders the list but keeps the old scroll offset. The
     selected row ends up about 2,700px above the visible area.
   - *Reproduction:* open `/n/<Note 120 id>` in *UAT Long List*, then apply
     Title (Z–A).
   - *Impact:* users lose their place right after reorganising the view.

### What worked

- The path to the target note expands correctly, even 7 levels deep.
- Selection highlighting follows the route.
- Cached in-app navigation within a folder re-renders no rows and causes no
  layout shift.
- A partially cached jump fetches only the missing folder listing.
- Creating a note adds exactly one row and scrolls to it (though defect 1 still
  applies to that row).
- Rapid back/forward leaves the sidebar stable.
- Long titles wrap cleanly at depth.
- The phone drawer closes after a row is tapped.
- Hiding and reopening the sidebar keeps its scroll position and selection.
- Clicking the hint's folder link opens that folder's page with its row in view.

### Improvements to consider

- **Keyboard tree navigation.**
  - *Friction:* the tree declares `role="tree"`, but ArrowDown does not move
    focus. Only Tab moves row by row, so passing a 150-note folder takes 150
    Tab presses.
  - *Benefit:* the tree works as expected for keyboard and assistive-technology
    users.
  - *Recommendation:* medium priority.
- **Calmer reveal for far jumps.**
  - *Friction:* reaching note 800 of the 1,000-note folder smooth-scrolls about
    25,600px in roughly 1.5 seconds, a fast blur through the list.
  - *Benefit:* the note appears without a long animation.
  - *Recommendation:* use an instant jump beyond a distance threshold, together
    with the fix for defect 1. Low–medium priority.
- **Tree build-up on an uncached deep load.**
  - *Friction:* the Deep Tree fills in over 4–5 visible layout shifts (about
    0.08 in total) as each ancestor listing arrives. It is stable afterward.
  - *Benefit:* a deep link shows its final tree at once instead of settling in
    steps.
  - *Recommendation:* low priority. Batch ancestor listings only if users notice
    the build-up.
- **Folders left expanded.**
  - *Friction:* folders expanded earlier (for example, the 150-note folder)
    stay open after navigating elsewhere, so the tree keeps growing and later
    targets move further down.
  - *Benefit:* the tree stays short enough to scan.
  - *Recommendation:* consider a "collapse others" or "collapse all" action. Low
    priority; this is an observation, not user-validated.

### Progressive loading

The evidence argues against it for now.

- Expanding the 150-note folder took about 6ms, with no long tasks.
- Expanding the 1,000-note folder took about 140ms, with one long task of about
  60ms.
- Wheel-scrolling through 1,000 rows produced no long tasks.
- A direct load put the active row in the DOM in about 250ms.
- The observed friction (defect 1 and the long reveal animation) is about scroll
  positioning, not rendering cost.

Missing evidence that could change this: folders with several thousand notes,
slower devices, and throttled networks.

### Coverage gaps

- The smoothness a person perceives on real hardware and with touch scrolling;
  headless measurements stand in for it.
- Firefox and Safari.
- Throttled network and slow-backend timing.
- Attachments in the tree. The seeding route used creates notes and folders
  only.
- Sort orders other than Title (Z–A), and whether the chosen order persists
  across sessions.
- Resizing the sidebar. Hiding and reopening it preserved the scroll position
  and selection.

### Suggested priorities and follow-up story candidates

1. **Keep the revealed note fully visible and clickable below the path hint**
   (defects 1–2): high priority and small in size.
2. **Keep the selected note in view after re-sorting** (defect 6): medium
   priority. It could share the reveal fix from candidate 1.
3. **Arrow-key navigation in the sidebar tree:** medium priority.
4. **Instant reveal for far jumps:** low–medium priority; could be folded into
   candidate 1 if cheap.
5. **Left-rail badge overlap and the mobile SVG console error** (defects 3–4):
   low-priority bug fixes.
6. **Recent-notes request burst in search** (defect 5): low priority.

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
