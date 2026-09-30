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
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/001-note-properties-uat/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"174eae1ecc198e62d4b44bf24ad68dc169591e4c5f86ee1a3193d4f583628445","plan":"ee93161540648f0bb220c688adcb566860dbb5f11a159984ecf29bba83fd2dd0"}}
```

**Goal**

The product owner receives an evidence-backed list of defects to fix and
improvements to consider, so that note properties are comfortable on an iPad (the device the owner uses
most often), stay operable on a phone, and so that each recommended change also makes
the design and architecture of the properties area more cohesive with fewer lines
of code.

**Scope**

- Start the assessment with a one-hour budgeted manual UAT of the note properties
  that exist today, in both the editable view and the read-only view of a note.
- Device priority: an iPad-class tablet is the primary device, so run every
  journey there first, in both portrait (about 820 px wide) and landscape (about
  1180 px wide), with touch input and the on-screen keyboard. Then repeat the core
  journeys at a phone width (for example 375 px) and a desktop width. Use the real
  browser's device mode, and note where a real touch device would differ.
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
  area competes with the note content for space. On an iPad, judge comfort and
  density; on a phone, judge only whether every journey can be completed.
- Presentation: on an iPad, visual density, spacing, hierarchy between key and
  value, empty states, hover-only cues that a touch user cannot see, and
  consistency with the rest of the note page. Phone appearance is not judged.
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

- Open a note with six properties on an iPad-sized viewport in portrait and
  landscape → observe readability, how much of the screen the properties use before
  the note content begins, and whether the row buttons can be tapped without mistakes.
- Add a property on an iPad-sized viewport with the on-screen keyboard open →
  observe whether the key presets, value input, and validation message stay visible.
- Open a note with six properties and add, change, and remove one on a 375 px wide
  viewport → observe only whether each action can be completed (no unreachable
  control, no content lost off screen); do not record looks as defects.
- Compare the read-only list and the editable list of the same note → observe what
  differs visually, and record which parts of the code exist twice to produce the
  differences.
- Take one recommended change (for example, a single responsive row layout that
  replaces the fixed three-column grid) → estimate the lines removed, the files
  merged, and the behavior that must stay covered by tests.

**Acceptance criteria**

- Every journey was run on an iPad-sized viewport in portrait and landscape with
  touch input; iPad findings rank above phone and desktop findings.
- The phone-width journeys are judged only by operability: view, add, change, and
  remove a property, save, and reach every control. Appearance is not an
  acceptance concern on a phone.
- A phone finding is a defect only when a user cannot complete a journey; cosmetic
  phone observations are recorded as optional, at lower priority than iPad findings.
- The report meets the output list below.

**Output and evaluation**

Produce one findings report containing:

- Scenarios exercised, viewport sizes (iPad portrait and landscape first), note
  characteristics, and actual UAT time;
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

- **For / why:** Users on an iPad can read and edit note properties comfortably and
  users on a phone can operate them; the owner can judge which changes to make first, with the
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
later product decision. If time is short, keep the iPad journeys, the phone operability journeys, and the
design and architecture section, and reduce coverage of rare value types first.

## Open Decisions

- Which findings warrant changes, and in what order? Decide from the UAT report
  rather than selecting them in advance.
- Decided at refinement: an agent runs the assessment in an exploration workspace
  against the local dev stack with the seeded `old_learner` account, driving a real
  browser in device mode; the owner reviews the report. Device mode cannot show real
  touch behavior, so those cases are listed as needing a real iPad or phone.

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

## UAT Findings

Screenshots and scripts are under `$CLAUDE_JOB_DIR/tmp/uat/` (not committed). File names below are
relative to that folder. "Observation" means measured or seen; "hypothesis" means not confirmed.

### Setup

- **Environment:** Development stack served from the primary checkout at http://127.0.0.1:5175/; headless
  Chromium (Playwright 1.63.0) with touch. Viewports: iPad portrait 820x1180 and iPad landscape 1180x820
  (2x scale, `isMobile`, `hasTouch`, taps only). Signed in as `manual`.
- **Read-only mode:** the app shows read-only properties only when the viewer is signed out or the notebook
  is read-only. To see it, the notebook "UAT notebook" (id 22) was shared to the Bazaar with
  `POST /api/notebooks/22/share`, and the notes were opened as `old_learner`. (This state change stays in the
  development database; only UAT notes are affected.)
- **Notes:** `UAT no properties` /n13716 (no front matter in the source), `UAT six properties` /n13717
  (exactly 5 rows: type, aliases (2 items), url, note_level, example of), `UAT many properties` /n13718
  (19 rows: 14-item aliases, `wikidata_id: Q42`, a 76-character key with a 200-character value, a
  76-character key without spaces, wiki-link values, uploaded `example.png`). Scratch notes edited
  destructively: `UAT edit portrait` /n13719, `UAT edit landscape` /n13720.
- **On-screen keyboard emulation and limits:** after focusing an input the viewport height was cut by 40%
  (portrait 1180 to 708, landscape 820 to 492) and `page.keyboard` typed. This shrinks the layout viewport,
  as some Android browsers do. iPadOS Safari keeps the layout viewport and shrinks only the visual viewport,
  so fixed elements, auto-scroll and the reveal of a focused field differ (see "needs a real iPad").
- **Testing button:** the yellow "T" button seen in slice 1 is the Testability menu. The code shows it only
  when `environment === 'testing'` (`frontend/src/components/DonutApp.vue`, `TestMenu.vue`). It covers the
  right-hand row controls on the Development stack (it blocked a tap in landscape), but a normal user does
  not see it. It is NOT recorded as a defect. Later taps hid it with a style rule.
- **Exploration minutes:** see the end of this section.

### Scenarios exercised (iPad portrait and landscape unless noted)

- View: no / five / nineteen rows, editable and read-only; measured how far down the note body starts.
- Row panel: chevron toggle, focus highlight, remove button, Assimilate / Skip buttons (portrait: five-row
  note; landscape: long-key row of the many note). Assimilate and Skip were only measured, not tapped.
- Add: Add property, key typing with the keyboard emulated, preset list, value entry, commit by tapping
  outside; add on a 19-row note (does the form come into view).
- Validation: duplicate scalar key, duplicate list-capable key (`url`), key only, value only, out-of-range
  `note_level`, empty list item in the list dialog.
- Change: value of a text row (`mood`), out-of-range `note_level`, key rename; reload to prove saving.
- Remove: a row through the row panel.
- List values: value dialog with Text / List modes, keyboard open (landscape only).
- Image: add `image` with the file chooser (new file name, and duplicate file name), the Replace button.
- Relation type: the `relation` key (control size only).
- Wikidata: insert form "Set...", dialog, keyboard open, Save with `Q42`.
- Not on phone or desktop (slice 3).

### iPad defects

Ordered by user impact.

1. **Read-only: a long key squeezes or hides the value.**
   - Expected: key and value both readable, the row wrapping inside the note width.
   - Observed (read-only, `old_learner`, note 13718): the row with the 76-character key with spaces is 460 px
     high in portrait, because the value column is 0 px wide (`dt` 484 px, `dd` 0 px); the value text is
     not visible. In landscape the value is 255 px wide and the row 100 px high; the URL inside it runs
     under the right edge. The key `a_rather_long_property_key_that_keeps_going...` (no spaces) is 571 px wide;
     its value `short` is 0 px wide in portrait (not visible) and 183 px in landscape. The note content area
     then scrolls sideways (scroll width 1037 against 500 px in portrait, 1052 against 770 in landscape).
   - Reproduce: sign in as a second user, open /n13718 in the Bazaar notebook, portrait 820 px; scroll to the
     rows 6 and 7. Script `a5.mjs`, `a4.mjs`.
   - Impact: values silently disappear for notes with long keys; a whole screen of empty space in portrait.
     Long keys are rare, so severity is medium. (Hypothesis: the read-only list is a definition list with
     natural-width columns and no maximum width for the key.)
   - Evidence: `a3-ipad-portrait-ro.png`, `a5-ipad-portrait-ro-long.png`, `a5-ipad-landscape-ro-long.png`.
2. **Validation message is far from the edited row and can be off screen.**
   - Expected: the message is visible near the field that was rejected.
   - Observed: the message is always placed below the last row, above the Add form. On the 19-row note an
     invalid `note_level` (value 7) reverted the field and showed "note_level must be an integer from 1 to 6."
     at y 984 (portrait) and y 970 (landscape) while the edited row was at y 380 and y 374. Portrait: on
     screen (viewport 1180). Landscape: off screen (viewport 820). The message is small (`text-xs`, 16 px).
   - Reproduce: /n13718, tap the `note_level` value, select all, type 7, tap another key field (`q1.mjs`).
     On the 5-row scratch note the message is visible (`d3-portrait-dupscalar.png`).
   - Impact: the value silently jumps back and the user may not see why. Medium for long lists.
   - Evidence: measurement in `q1.mjs` output (no screenshot of the off-screen state).
3. **Key preset list: does not narrow while typing, is wider than its own panel, and runs off the screen with
   the keyboard.**
   - Expected: presets narrow to the typed text; the list stays inside its panel and the visible screen.
   - Observed: after typing `mood` all 7 presets are still listed. Options are 250 px wide inside a 158 px
     panel; `question_generation_instruction` runs outside the white panel (and, in portrait, over the note
     body). In landscape with the keyboard emulated the list starts at y 388 and ends at y 622 in a 492 px
     viewport: 3 options visible, the rest reachable only by scrolling the page (the list scrolls into view
     with `scrollIntoView`; the page itself does not scroll to it).
   - Reproduce: Add property, tap Property key, type any text, shrink the viewport by 40%.
   - Impact: medium (typing a custom key still works; preset choice is harder). Also a `url 2` preset appears
     when `url` exists, which is not explained on screen.
   - Evidence: `c1-portrait-kb-typed.png`, `c1-landscape-kb-typed.png`.
4. **Long keys and values are cut off without an ellipsis.**
   - Expected: the user can read or reach the whole text.
   - Observed (editable): key input 158 px and value input show the first characters only ("a rather long
     property k", "This is a very long property value that should wrap or overflow depending on how th"). The
     full text is in the `title` attribute (a hover tooltip a touch user does not get) or reachable only by
     entering the field and scrolling the text, or by the value dialog for list and URL rows.
   - Impact: low to medium; the long-key case is rare, but no cue tells the user text is cut.
   - Evidence: `a1-ipad-portrait-many.png`, `n1-landscape-panel-longkey.png`.
5. **Dialog close button is small.** The "x" of the value and Wikidata dialogs is 26x26 px. Cancel, Save and
   Close are 40 px high. Impact: low (Cancel/Close alternatives exist).

Suspected, not reproduced or not resolved:

- **Wikidata dialog Save does nothing visible.** Insert form, `wikidata_id`, Set..., tap the ID field, type
  `Q42`, Tab, tap Save on `UAT edit portrait` in portrait: the request `GET /api/wikidata/entity-data/Q42`
  returned 200 (network log in `w1.mjs`), but the dialog stayed open for 9 s, no row was added, and no
  message appeared. Text under the input read "No Wikidata entries found for 'UAT edit portrait'" (the search
  by note title). It is unknown whether Save needs a selected search result, or whether the development
  wikidata data for Q42 differs from real Wikidata. Not confirmed as a defect. Needs a check with a real
  entry from the search list, or the e2e mock (`@usingMockedWikidataService`). Evidence: `w1-after-save.png`,
  `f1-portrait-after-wikidata.png`.
- **Replace button tap timed out once.** In one run (image row of `UAT edit portrait` right after adding
  it, tap of "Replace..."), Playwright waited 30 s without a file chooser. Repeating it on a fresh page load
  gave the chooser immediately (`h1.mjs`). Conditions tried: the same viewport, testing button hidden in
  both runs. Not reproduced.

### iPad UX improvements

Each entry: observed friction, benefit, proportionate recommendation. Observations unless marked.

1. **Tap targets are 32 px high, below the 44 px guide.**
   - Observed: chevron 42x32, edit icon 42x32, external-link icon 42x32, row remove 42x32, Assimilate 87x32 and
     Skip 52x32, key and value inputs 32 px high, key preset options 32 px high, dialog list controls 42x32,
     Replace 84x32, Choose image 120x32, Add property 124x32, the Wikidata ID button in a row 35x26. Rows are
     40 px apart (32 px + 8 px gap), so neighbouring controls are 8 px apart. Toolbar buttons are 50x32.
   - Benefit: fewer mistaken taps, in particular the row remove next to the chevron and Assimilate/Skip.
   - Recommendation: on touch widths use a minimum control height of 44 px for the chevron, edit and remove
     controls, and the row inputs (one shared size rule, not per-component changes).
2. **Density.** Distance from the top of the screen to the start of the note body (editable / read-only):
   no properties 264 px (a single `type: Note` row) / 240 px; five rows 424 / 340 px portrait, 418 / 334
   landscape; nineteen rows 992 px portrait (84% of the screen) and 978 landscape (more than the 820 px
   screen, so the body is not visible without scrolling), 1120 / 754 read-only. Editable rows use 40 px per
   row against 20 px read-only. Hypothesis: a collapsed summary (first few rows, "show all") or read-only
   until tapped would keep the body in view. Recommendation for the owner to weigh; do not add a new mode if
   the same result comes from removing the fixed row height.
3. **Key and value look the same in edit mode.** Both are bordered inputs of the same height and weight; only
   position tells them apart. In read-only mode the key is lighter than the value, which reads better.
   Hypothesis: a lighter, label-like key would improve the hierarchy.
4. **Read-only and editable lists differ in ways the user notices (same note, 13718).** Editable: aligned
   columns, bordered inputs, wiki-link values in link style, edit icons. Read-only: values start at different
   x positions (ragged), a single wiki-link value (`related`, `example of`) shows as plain "[[UAT six
   properties]]" while list wiki-links (`overlaps`) are teal links, the image row shows only the file name
   (no open icon), no images are shown. Recommendation: one row layout with an editable / read-only switch
   for the value part, so both views share spacing and link styling.
5. **Empty state.** `UAT no properties` has no front matter but shows a `type: Note` row in both modes; there
   is no empty-state message and no hint what properties are. The "Add property" button remains. Whether the
   `type` row is saved in the note or is only shown was not checked.
6. **Add form is separate from the list.** The Property key / Property value labels appear below the last
   row and are not aligned with the row columns (key 158 px in both, but the value field starts 8 px later
   and extends to the right edge). There is no Add or Save button: the row is added when the value field
   loses focus. Key only, or value only, then tapping outside, does nothing and gives no message; the form
   stays open (tested: `orphan` key, `lonely value`). On the 19-row note the form opens at the bottom and the
   page scrolls to it (focus was on the key field, in view in both orientations), which is good.
   Recommendation: a visible "Add" control (44 px) that also explains why nothing was added.
7. **Adding an existing list key changes the row silently.** Adding key `url` with a value when `url` exists
   appends the value and turns the single URL into a list (`https://example.com/e ,dup`), without a message.
   This is intended by the code (list-capable keys append), but nothing shows it on screen. A short
   confirmation ("Added to url") would help. Non-list keys show "Duplicate property keys are not allowed."
   (`d3-portrait-dupscalar.png`).
8. **Remove has no confirmation or undo.** The panel's minus icon (42x32, under the chevron) removes the
   row at once. The label "Understanding" and its Assimilate / Skip buttons share the panel with the remove
   icon, so remove is visually tied to assimilation actions (`b1-ipad-portrait-panel.png`). A remove button
   with a text label or a short undo would reduce risk of mistakes with 32 px targets.
9. **Panel size.** An opened panel adds 36 px below the row (row 32 px becomes 68 px) and uses the row's whole
   width; the highlight is a light purple background with a border, clearly visible. Only one panel appears
   to be open at a time in the runs done (flags showed row 5 only after a tap in row 6; not analysed
   further). No hover-only styling exists in the properties components (no `hover:` classes found in
   `RichFrontmatter*.vue`); the only hover cue is the `title` tooltip on truncated values (defect 4).
10. **Image upload error message is short-lived.** Uploading a file whose name already exists in the
    notebook gives HTTP 409 and the message "Cannot upload example.png: it already exists; rename the image
    and upload it again". It was visible at 0.3 s and 1 s and gone at 2.5 s. The form keeps an empty value
    field. Recommendation: keep the message until the next action. A new file name uploaded without problems
    (`photo-b.png`, then the row was added and the file appeared in the sidebar).
11. **List dialog.** Text / List tabs (40 px), list item input 32 px, move up / down / remove 42x32, Add item
    79x32, Cancel 80x40 and Save 66x40. With the keyboard emulated (landscape 492 px) every dialog button
    stayed in the viewport. An empty item shows "List items cannot be empty." and keeps the dialog open.
    Reasonable, apart from the target sizes.
12. **Relation type.** The `relation` row shows a 96x32 "Relation Type" control; only its size was
    measured, the selection flow was not run.

### Confirmed working (iPad)

- Text value edit saves after tapping outside (`mood` -> `sad`, survives reload); key rename saves.
- Adding a property with a new key and value works in both orientations and persists across reload.
- No horizontal page overflow in the editable view of the 19-row note (scroll width 770 of 770 in
  landscape; the page width equals the viewport in both orientations).
- Image add with a new file name; Replace opens the file chooser.
- Wikidata dialog opens and stays fully on screen with the keyboard emulated; its Save and Close are 40 px.
- Adding `wikidata_id` did not insert body text in these runs, but the Save did not finish (see the suspected
  item), so the question stays open. The end-to-end scenarios under `e2e_test/features/wikidata/` (for
  example `associate_wikidata_location_entries.feature`: "the note content ... should be `Location: ...`")
  show that inserting descriptive text after a Wikidata association is intended behaviour, so the body text
  "United Kingdom, 11 March 1952" in the many-properties note is consistent with that intent.

### Coverage gaps / needs a real iPad

- Real iPadOS Safari behaviour: the visual viewport with the software keyboard (auto-scroll to the focused
  field, whether the preset list and validation message stay visible), the floating keyboard and the
  shortcut bar, hardware keyboard and trackpad, Apple Pencil, Stage Manager / split view widths, safe-area
  insets, Safari's own zoom on focus of inputs smaller than 16 px text, the native file picker and camera
  upload, long-press and text selection in the 32 px inputs, momentum scrolling in the preset list.
- Not run: the Assimilate and Skip buttons in the panel, list reorder, the relation type selection flow,
  portrait list dialog with the keyboard, rename to an existing key (one attempt renamed to `urlmood` by
  mistake and was not repeated), Wikidata Save with a real search result, read-only view of a note in
  landscape beyond the height measurements, dark mode, very narrow split view.
- The 40-minute budget was not used up (see minutes); the remaining time went to the checks above and to
  clarifying the read-only mode.

### Phone operability findings (slice 3)

_To be filled in slice 3._

### Defects the automated tests do not cover (slice 3 extends)

_To be filled in slice 3. Candidates from the iPad run that have no test known to cover them: read-only long
key layout (defect 1), validation message placement on long lists (defect 2), preset list width and
filtering (defect 3), Wikidata Save without visible outcome (suspected), silent append to an existing list
key (improvement 7), transient upload error message (improvement 10)._

### Design and architecture assessment (slice 4)

_To be filled in slice 4._

### Synthesis and follow-up (slice 5)

_To be filled in slice 5._

### Actual exploration minutes (slice 2)

About 20 minutes of exploration by the wall clock (14:07 to 14:27, 2026-09-30), inside the 40-minute budget
for this slice.
