---
id: SEED-061
status: dormant
planted: 2026-09-30
planted_during: owner request for a budgeted note properties UX UAT, following the sidebar (SEED-042) and book reading (SEED-054) UATs
trigger_when: selecting note properties (front matter) UX, mobile, or simplification work from observed user experience
scope: small
---

# SEED-061: Find how note properties can be more compact, modern, mobile friendly, and simpler to maintain

## Why This Matters

Every note can carry properties (the front matter block at the top of its
content): built-in keys such as image, relation type, aliases, and Wikidata ID,
plus free keys. Users read and edit them beside the note content, and each
property row can open a panel of assimilation actions.

The properties area is large. The frontend has about fourteen `RichFrontmatter*`
components, several `noteContent*` and `noteProperties` utilities, and property
composables, about 4,000 lines in all. By observation of the code (not yet of
real use), each editable row uses a fixed three-column grid (toggle, key input
at least 8 rem wide, value), the row action panel is indented by a fixed amount,
and only two responsive breakpoints exist across all property components. These
are hypotheses about how the area behaves on a phone; the UAT must confirm or
refute them.

## Alternatives and Decision

Fixing isolated layout complaints would leave the structure as it is. Redesigning
the properties area without watching real use would choose changes before knowing
which ones matter. Following the approach used for the sidebar (SEED-042) and
book reading (SEED-054), the owner chose a one-hour manual UAT of what already
exists, to gather concrete findings before selecting stories.

The owner has two goals for the findings, and both are equally important:

1. **Experience:** a more compact, modern-looking, mobile friendly properties UI.
2. **Design and architecture:** every recommended change should also make the
   design more cohesive and reduce the amount of code. A recommendation that
   improves the look but adds code or scatters the concept further ranks lower.

## Story Decomposition

Effort bands: S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours, including delivery.
The manual UAT has an explicit one-hour budget; setup and findings write-up should
be reported separately rather than silently consuming or extending that budget.
This seed captures the assessment story and authorizes no UAT run or fixes now.

<a id="story-1"></a>

### Identify note properties UX and design improvements through a one-hour manual UAT

**Identity:** SEED-061#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal**

The product owner receives an evidence-backed list of defects to fix and
improvements to consider, so that note properties become more compact, modern
looking, and friendly on a phone, and so that each recommended change also makes
the design and architecture of the properties area more cohesive with fewer lines
of code.

**Scope**

- Start the assessment with a one-hour budgeted manual UAT of the note properties
  that exist today, in both the editable view and the read-only view of a note.
- Run the same journeys at a phone width (for example 375 px), a tablet width, and
  a desktop width. Use the real browser's device mode, and note where a real
  touch device would differ.
- Viewing: notes with no properties, a few properties, and many properties; long
  keys, long values, long lists (aliases), wiki-link values, image values,
  Wikidata ID values.
- Editing: adding a property (key presets, key typing, value entry), changing a
  key or value, removing a property, list values, image upload, relation type
  selection, the Wikidata dialog, validation messages, and saving while the
  keyboard is open on a phone-size viewport.
- The row panel: the toggle that opens a row's assimilation actions, its focus
  highlight, remove button, and how much vertical and horizontal space a row and
  its panel use.
- Touch and small-screen fit: tap target sizes, horizontal overflow, key column
  width, dialogs and preset lists that leave the screen, and how the properties
  area competes with the note content for space.
- Modern presentation: visual density, spacing, hierarchy between key and value,
  empty states, hover-only cues that a touch user cannot see, and consistency with
  the rest of the note page.
- Design and architecture assessment (required, given equal weight with the
  observed experience): from the running product and the code, identify where the
  properties concept is spread over many files, where the read-only and editable
  paths repeat each other, where the same decision is made in more than one place,
  and which parts could be removed or merged without losing supported behavior.
  For each candidate, estimate the effect on lines of code and cohesion, and check
  it against the accepted architectural decisions (for example ADR 0004 on note
  Markdown) and against existing end-to-end scenarios that protect the behavior.
- Check for defects that appear only when the properties area is used in ways the
  automated tests do not cover, and record them separately from the design
  observations.
- These scenarios are a starting set, not an exhaustive limit on exploration
  within the budget.

**Key examples**

- Open a note with six properties on a 375 px wide viewport → observe whether every
  key and value is readable without sideways scrolling, how much of the screen the
  properties use before the note content begins, and whether the row buttons can be
  tapped without mistakes.
- Add a property on a phone-size viewport with the on-screen keyboard open → observe
  whether the key presets, value input, and validation message stay visible.
- Compare the read-only list and the editable list of the same note → observe what
  differs visually, and record which parts of the code exist twice to produce the
  differences.
- Take one recommended change (for example, a single responsive row layout that
  replaces the fixed three-column grid) → estimate the lines removed, the files
  merged, and the behavior that must stay covered by tests.

**Output and evaluation**

Produce one findings report containing:

- Scenarios exercised, viewport sizes, note characteristics, and actual UAT time;
  identify important scenarios not reached within the hour.
- For each defect: expected versus observed behavior, reproduction steps, viewport
  conditions, user impact, and evidence where practical.
- For each UX improvement: the observed friction, intended user benefit, and a
  proportionate recommendation. Distinguish observations from hypotheses.
- **Design and architecture recommendations (emphasized section, not an appendix):**
  each recommended story states the experience it improves, the structural change
  it makes (what is merged, removed, or made to live in one place), the expected
  reduction in lines of code and files, and the tests or scenarios that protect
  the behavior during the change. Recommendations are ordered so that changes that
  both improve the experience and reduce code come first.
- Suggested priorities and proposed follow-up story outcomes, separating fixes to
  existing behavior from new capabilities, and separating changes that reduce code
  from changes that only add polish.

The owner can evaluate success by reviewing reproducible findings and actionable
recommendations. An unreproduced suspected problem must be recorded honestly with
the conditions tried; it must not become a confirmed defect by assumption. A line
count estimate must say how it was obtained.

- **For / why:** Users on phones and small windows can read and edit note
  properties comfortably; the owner can judge which changes to make first, with the
  code getting smaller and more cohesive rather than larger.
- **Value / learning:** Learn where the properties area fails on small screens or
  feels heavy, and where its structure can be simplified without losing behavior.
- **Effort hypothesis:** M, medium confidence: one hour of UAT plus bounded setup
  (notes with varied properties, several viewport sizes) and synthesis including
  the code review for the design section. The hour limits exploration, not a
  promise of exhaustive coverage.
- **Depends on:** A usable application and representative notes; no dependency on
  other queued stories is established.
- **Safe stopping point:** The findings report is useful independently of later
  changes. Record coverage gaps and inconclusive observations at the budget boundary.
- **Execution handoff:** Use
  [dough-manual-testing](../../.agents/skills/dough-manual-testing/SKILL.md)
  when this assessment is selected for execution.

## Ordering and Scope Reduction

At the one-hour boundary, stop exploration and synthesize what was observed.
Preserve untested scenarios as coverage gaps rather than extending the UAT or
claiming complete coverage. Implementation fixes, redesign, and automatic creation
of follow-up backlog entries are deferred; the output supplies candidates for a
later product decision. If time is short, keep the phone-width journeys and the
design and architecture section, and reduce coverage of rare value types first.

## Open Decisions

- Which findings warrant changes, and in what order? Decide from the UAT report
  rather than selecting them in advance.
- Whether the assessment is run by an agent or by the owner, and against which
  stack, is decided at refinement, as it was for the book reading UAT.

## When to Surface

When the owner selects note properties, mobile, or simplification work from the
product backlog.

## Breadcrumbs

- Properties UI: `frontend/src/components/form/RichFrontmatter*.vue`,
  `frontend/src/composables/useRichFrontmatterPropertyEditing.ts`,
  `frontend/src/utils/noteContentFrontmatter*.ts`, `frontend/src/utils/noteProperties.ts`.
- Backend front matter handling: `backend/src/main/java/com/odde/donut/algorithms/Frontmatter*.java`.
- Existing scenarios: `e2e_test/features/note_view/note_frontmatter_image.feature`;
  component tests `frontend/tests/components/form/RichMarkdownEditor.frontmatter.spec.ts`.
- Earlier UATs for shape: `43ce1a45d7` (sidebar, SEED-042) and `4f2f230505`
  (book reading, SEED-054).
