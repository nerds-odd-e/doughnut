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

**Slice 3 update (2026-09-30, phone 375 px, dev stack; scripts `s11.mjs`, `s12.mjs`, `s10.mjs`).**

- Wikidata, real search result: on a scratch note titled `Tokyo` the dialog showed a search list (from
  `GET /api/wikidata/search?search=Tokyo`, 200). Tapping "Tokyo - capital and largest city of Japan" filled the
  ID field with `Q1490`; Save called `GET /api/wikidata/entity-data/Q1490` and `PATCH .../content`, the dialog
  closed, and the row `wikidata_id: Q1490` was stored. No message was shown. Note content afterwards was only the
  added property line; **no descriptive body text was inserted** for this entry (observation; whether it should be
  for a city entry was not checked).
- Wikidata, typed `Q42`, search list present (note `Kyoto`): `entity-data/Q42` 200; the dialog stayed open and
  now showed "Suggested Title: Douglas Adams | Replace title | Add as alias | Save | Close". So Save asks a
  follow-up question when the entry label differs from the note title. This explains part of slice 2 (the dialog
  stays open), and it is what the frontend tests cover. The follow-up choice was not taken.
- Wikidata, typed `Q42`, search list empty ("No Wikidata entries found for 'UAT wd noresult xq'"): `entity-data/Q42`
  200, the dialog stayed open, and neither the suggested-title question nor any message was shown; no row was
  added. This reproduces slice 2 exactly (twice now, on iPad portrait and on the phone). Observation: Save gives
  no feedback in this state. Hypothesis (not checked in code): the title question is only drawn with the result list.
  Status: reproduced as a silent Save; whether it is a defect depends on whether a typed ID without matching title
  results is meant to be supported.
- Replace button: four repeated taps on the phone (fresh page loads; once right after the load with only 100 ms
  wait; the first right after adding the image) gave the file chooser every time in 53 to 66 ms. The timeout of
  slice 2 was **not reproduced**; it stays "suspected, not reproduced". Conditions tried: iPad portrait (slice 2,
  once fails, once ok), phone 4 of 4 ok, testability button hidden in all. One unexplained observation: right
  after choosing the first image on the phone a tap on the "Properties" heading was blocked once ("a div
  intercepts pointer events") and worked on the next run; a busy overlay during the upload is a hypothesis.

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
- Not run on the iPad: the Assimilate and Skip buttons in the panel, list reorder, the relation type selection flow,
  portrait list dialog with the keyboard, rename to an existing key (one attempt renamed to `urlmood` by
  mistake and was not repeated), Wikidata Save with a real search result, read-only view of a note in
  landscape beyond the height measurements, dark mode, very narrow split view. Slice 3 ran the last five items on
  the phone (see the phone findings); they were not repeated on the iPad.
- Slice 3 gaps: real phone browser behaviour (keyboard shrinking only the visual viewport, auto-scroll of the
  focused field, the Wikidata dialog's Save and Close at the bottom edge, momentum scrolling in the dialogs);
  landscape phone; phone width in dark mode; the Wikidata follow-up choice (Replace title / Add as alias) and
  what it inserts; whether descriptive text is inserted for other entry types; desktop image upload,
  relation type, Wikidata and read-only long keys; tapping the clipped wiki-link value on the phone (only its
  box was measured); list dialog on the phone in landscape; the 44 px target rule on a real finger.
- The 40-minute budget was not used up (see minutes); the remaining time went to the checks above and to
  clarifying the read-only mode.

### Phone operability findings (slice 3)

Phone = 375x812, 3x scale, `isMobile`, touch taps only. Keyboard emulated by cutting the viewport height by 40%
(812 to 487) after focusing an input. Same limits as on the iPad apply (see Setup). Notes: `UAT six properties`
/n13717 (five rows), `UAT many properties` /n13718 (19 rows), scratch note `UAT edit phone` /n13721, read-only
view as `old_learner`. Scripts: `s1.mjs` (control positions), `s3.mjs`, `s4.mjs`, `s5.mjs`, `s6.mjs`, `s7.mjs`,
`s10.mjs`, `s11.mjs`, `s12.mjs`, `s13.mjs`, `s14.mjs`, `s15.mjs`, `s16.mjs`, `s17.mjs`, `s19.mjs`.

**Operability defects (a journey or control cannot be completed or reached)**

1. **Read-only: the value of a long-key row is lost (worse than on the iPad).**
   - Expected: the value of every property can be read.
   - Observed (read-only, `old_learner`, /n13718, 375 px): row `a rather long property key ...` has `dt` 327 px
     and `dd` 0 px; row `a_rather_long_property_key...` has `dt` 571 px and `dd` 0 px. The values (a
     200-character text, and `short`) have no width. The note area then scrolls sideways (scroll width 880
     against 343). After scrolling that area fully to the right (scroll left 537) the value was still 0 px wide,
     so panning does not reach it. A tall empty gap follows the first long key in the page (the screenshot
     shows about 1000 px of blank space before the next row).
   - Reproduce: sign in as a second user, open /n13718 in the Bazaar notebook, viewport 375 px, scroll to the
     rows after `note_level` (`s13.mjs`, `s14.mjs`).
   - Impact: values silently disappear for notes with long keys (rare); the user cannot read them at all on a
     phone. Same cause as iPad defect 1. Hypothesis (from the measured `grid-cols-[auto_minmax(0,...)]` class):
     the key column is `auto` wide with no maximum.
   - Evidence: `s13-phone-ro-longkey.png`, `s13-phone-ro-*.png`.
2. **Editable: the key preset list covers the value field and the Choose image button while the key has focus.**
   - Expected: after typing a key the user can tap the value field (or Choose image for key `image`).
   - Observed (insert form, 375 px): the form stacks key above value, and the preset list (6 options, 32 px
     each, y 459 to 661) is drawn over the value field and over the Choose image button. `elementFromPoint`
     at the value field returns a preset option; Playwright tapping the value field timed out because "preset
     option intercepts pointer events"; the same for the Choose image button. Typing more letters does not narrow
     or close the list. Tab from the key moves focus to the first preset option, not to the value.
   - Ways out that worked: tap a blank area (the "Properties" heading) so the key loses focus and the list
     closes, then tap the value or Choose image; or tap a preset (focus then moves to the value). Neither is
     shown on screen. A user who types a custom key and taps the value field sees nothing happen.
   - Reproduce: Add property, tap Property key, type `colour`, tap Property value (`s5.mjs`, `s6.mjs`, `s10.mjs`).
   - Impact: medium. The journey can be completed by a user who finds the workaround; the failing tap gives no
     feedback. With the keyboard emulated the value field is at y 489 to 521 in a 487 px viewport, so the page has
     to scroll before the value can be seen (the real browser scrolls a focused field into view; not verified).
   - Evidence: `s5-phone-preset-overlap.png`, `s4-kb-presets.png`, `s10-image-insert.png`.
3. **Editable: values are cut to about 50 px, so most plain text values cannot be read in one view.**
   - Expected: a value is readable, or an obvious way to see all of it.
   - Observed: the key input is 158 px, the toggle 42 px, the trailing icon 42 px, so the value cell is about
     60 px (`Note`, `abc`, `2` fit). The `url` row's value input is about 30 px and the edit icon is drawn on top
     of it (only `ht` is visible). A wiki-link value (`[[UAT no ...`) is cut, and its link element is 261 to 382
     px wide, that is 23 px past the row and 7 px past the screen edge. Aliases and URL rows can be read through
     the value dialog (edit icon). For plain text, the only way to see all is to enter the field and move the
     cursor (the full text is in `title`, hover only).
   - Impact: low to medium: nothing is unreachable (edit works), but long text values need cursor movement.
   - Evidence: `s2-phone-five.png`, `s16-relation-insert.png`.

No other control was unreachable. All of these were reached and tapped or measured on the phone: chevron
(16 to 58 px), key input (74 to 232), value edit icon (317 to 359), open-URL icon, row remove (48 to 90 in
the panel), Assimilate (204 to 291) and Skip (290 to 342, overlapping Assimilate by 1 px), list dialog (Text /
List, move up / down, remove, Add item, Cancel, Save, close), image Choose and Replace, relation type button
and its dialog, Wikidata Set... button, dialog, Save, Close, validation message.

**Optional, lower priority (cosmetic; not defects)**

- The Relation Type button is 248 to 377 px wide: 18 px past the row and 2 px past the screen edge (still tappable).
- Assimilate and Skip touch (1 px overlap); every control is 32 px high, below the 44 px guide.
- Row remove/panel layout: the panel repeats the iPad layout; the "Understanding" label is not repeated per row.
- Tapping the row panel's Assimilate ran `POST /api/assimilation` and then moved the page to a different note
  (`Perf100`, next in the assimilation sequence), leaving the note being edited. This may be the intended
  assimilation flow; question for the owner, not a defect. Skip asks "Leave this note out of the assimilation
  sequence?" (a note-wide wording inside a single property panel).
- The Wikidata dialog's Save and Close are at y 482 to 522 in the 487 px keyboard viewport (partly below the
  edge); the taps worked because the automation scrolls first. Needs a real phone.

**Journeys completed OK on the phone (with the keyboard emulated where typing)**

- View: five and nineteen rows, no horizontal page overflow in editable mode (scroll width 375 of 375).
- Add: `colour` = `blue` (tap a blank area to commit), persisted (stored content and reload). Add via preset
  tap works. Key only or value only: form stays open (as on iPad).
- Change: `mood` value changed and saved (`abc`, stored); rename to an existing key (`mood` to `note_level`)
  showed "Duplicate property keys are not allowed." and kept both rows; note the exact key was typed.
- Remove: `colour` removed through the panel.
- List dialog: `aliases` opened, items reordered with move down (`one, two` to `two, one`), Save, stored.
- Image: add with a new file name (`photo-ph-c.png`), Replace (`photo-ph-d.png`), and four repeats of the
  Replace tap gave the file chooser every time (53 to 66 ms).
- Relation: key `relation` with a value creates a row with a Relation Type button; the dialog lists the types
  (list scrolls inside the dialog); choosing "a part of" stored `relation: a-part-of` and survived reload.
- Validation: `note_level` 9 reverted with "note_level must be an integer from 1 to 6." visible at y 441 of 812.
- Wikidata dialog: opens fully in the viewport, search list selectable, Save adds `wikidata_id` (see slice 3 update).
- Read-only mode: rows are readable except the two long-key rows (defect 1).

### Desktop sanity pass

Desktop 1440x900, mouse, headless Chromium; scratch note `UAT edit desktop` /n13725 (`s18.mjs`). Only
differences and defects are marked.

- Same behaviour as iPad: presets do not narrow while typing (6 shown after `colour`); validation message
  appears below the last row (y 450); duplicate-key message; list dialog opens; remove through the panel;
  Assimilate 87x32 / Skip 52x32 / remove 42x32; value edit saved (`mood` to `calm`, stored).
- Difference: the key preset list does not cover the value field on desktop (side by side layout), so typing
  a key and clicking the value works (phone defect 2 does not occur).
- Difference: rows are 1030 px wide with a wide value column; no cut values in this note; no horizontal
  overflow (1440 of 1440); the note body starts at y 418 with five rows.
- No desktop-only defect found. Not run on desktop: image upload, relation type, Wikidata dialog, read-only
  view, long-key rows.

### Defects the automated tests do not cover (slice 3 extends)

Coverage was checked with `ls e2e_test/features/note_view e2e_test/features` and
`grep -ril "frontmatter\|property\|properties" e2e_test/features frontend/tests`. Existing scenarios and specs
found: `e2e_test/features/note_view/note_frontmatter_image.feature` (header image, upload becomes a file),
`e2e_test/features/note_topology/note_property.feature` and `property_wiki_link.feature` (property location and
panel), `e2e_test/features/wikidata/*.feature`, `frontend/tests/components/form/RichMarkdownEditor.*.spec.ts`
(row editing, list properties, value dialog), `frontend/tests/utils/noteContentPropertyRows*.spec.ts`,
`frontend/tests/notes/WikidataAssociationDialog*.spec.ts`, `frontend/tests/utils/noteContentPropertyKeyPresets.spec.ts`.
Only the file names were read, not each test; "no test found" means no name or grep hit for that case.
The only phone-width feature found is `e2e_test/features/book_reading/phone_reading.feature` (book reading, not
properties). Component tests run in jsdom, which has no layout, so no width or overlap case can be covered there.

| Defect (slice) | Covered by an automated test? |
|---|---|
| Read-only long key hides the value (iPad 1, phone 1) | No test found. Needs a layout check in a real browser; jsdom cannot see widths. `property_wiki_link.feature` "Visiting a read-only property location focuses the property value" covers a short key only. |
| Validation message far from the row / off screen on long lists (iPad 2) | Message text covered (`RichMarkdownEditor.propertyRowEditing.spec.ts` "rejects duplicate keys ...", `noteContentPropertyRows.validate.spec.ts`). Position and visibility: no test found. |
| Key preset list does not narrow, wider than its panel (iPad 3) | Preset content covered by `noteContentPropertyKeyPresets.spec.ts`. Narrowing while typing and width: no test found. |
| Preset list covers the value field and Choose image on a phone (phone 2) | No test found (no phone-width property scenario exists). |
| Values cut with no cue, value cell about 50 px on a phone (iPad 4, phone 3) | No test found. |
| Wikidata Save with a typed ID and an empty search list does nothing visible (slice 3 update) | Suggested-title step is covered (`frontend/tests/notes/wikidataAssociationDialogTestSupport.ts`, "Replace title"); "No Wikidata entries" is covered by `WikidataAssociationDialog.search.spec.ts`. The combination typed ID plus empty list: no test found. |
| Silent append to an existing list key (iPad improvement 7) | The append itself is covered (`noteContentPropertyRows.append.spec.ts`); feedback to the user: nothing to test today. |
| Transient upload error message (iPad improvement 10) | Successful upload covered by `note_frontmatter_image.feature`; the 409 message duration: no test found. |
| Row panel Assimilate moves to another note (observation) | Navigation to the next note covered (`frontend/tests/composables/useGoToNextAssimilation.spec.ts`); from a property panel: not checked. |
| Key-only or value-only add leaves the form open with no message (iPad improvement 6) | No test found for the missing message. |

### Design and architecture assessment (slice 4)

Method: read-only review of the code on `main` (this worktree is the same). "Observed" means read in code
(file:line); "hypothesis" means not confirmed by running it. Every line count is from `wc -l`; every saving is
an estimate (existing lines removed minus a stated guess of the replacement lines). Defect and improvement
numbers refer to the lists above ("iPad D2" = iPad defect 2, "iPad I6" = iPad improvement 6, "phone D1" =
phone operability defect 1).

#### Size of the properties concept

Commands (repository root):

```
cat frontend/src/components/form/RichFrontmatter*.vue | wc -l                       # 1,608  (13 files)
cat frontend/src/composables/useRichFrontmatterPropertyEditing.ts \
    frontend/src/utils/noteContentFrontmatter*.ts frontend/src/utils/noteProperties.ts | wc -l   # 822
  -> 1,608 + 822 = 2,430   (the plan's count)
cat frontend/src/utils/noteContent*.ts | wc -l                                       # 1,058 (10 files)
cat <13 components> useRichFrontmatterPropertyEditing.ts noteContent*.ts noteProperties.ts | wc -l   # 3,047 (all frontend property code, no double count)
cat PropertyValueField.vue propertyValueField.ts useFocusedNoteProperty.ts useNotePropertyPanelLocation.ts \
    usePropertyRowClientIds.ts authored{Aliases,Overlaps,NoteLevel}Validation.ts | wc -l   # 532 (related helpers)
cat frontend/tests/components/form/RichMarkdownEditor.*.spec.ts propertiesTestDom.ts propertyValueDialogTestDom.ts \
    richMarkdownEditorTestHarness.ts frontend/tests/utils/noteContent*.spec.ts | wc -l   # 3,508 (frontend tests)
cat backend/src/main/java/com/odde/donut/algorithms/{Frontmatter*,NoteLeadingFrontmatter}.java | wc -l   # 917 (9 files)
```

Reconciling: the plan's 2,430 is the 13 components plus the composable, the two `noteContentFrontmatter*`
utilities and `noteProperties.ts`. The seed's "about 4,000" cannot be reproduced: all frontend property code
is 3,047 lines, with related helpers 3,579, and the frontend tests alone are 3,508. So "about 4,000" is not
a count of code that could be removed; use the per-candidate estimates below.

Component roles (lines):

| File | Lines | Role |
| --- | ---: | --- |
| `RichFrontmatterProperties.vue` | 244 | Section owner: parses, holds rows and the single validation message, switches read-only or editable list, insert form, Wikidata dialog |
| `RichFrontmatterEditablePropertyList.vue` | 60 | Loop over editable rows, ids, forwards 7 events |
| `RichFrontmatterEditablePropertyRow.vue` | 246 | One editable row: 3-column grid, chevron, key input plus presets, value by key kind, panel |
| `RichFrontmatterScalarPropertyValue.vue` | 134 | Text or list value of one editable row, edit-dialog button, URL link |
| `RichFrontmatterReadOnlyList.vue` | 81 | The whole read-only path: own grid, own value-by-key-kind chain |
| `RichFrontmatterInsertForm.vue` | 171 | The add form: own key input plus presets, own value-by-key-kind chain |
| `RichFrontmatterPropertyValueDialog.vue` | 264 | Text or list edit dialog |
| `RichFrontmatterImagePropertyValue.vue` | 120 | Image URL input, upload, Choose or Replace (used by row and insert form) |
| `RichFrontmatterListPropertyValue.vue` | 83 | List value display (wiki links only for `overlaps`, URLs for `url`) |
| `RichFrontmatterPropertyKeyPresets.vue` | 62 | Preset list (absolutely positioned) |
| `RichFrontmatterPropertyExternalLink.vue` | 61 | Open URL or Wikidata icon; has a `compact` variant for read-only |
| `RichFrontmatterPropertyPanel.vue` | 59 | Row panel: remove, Assimilate, Skip (`pl-8`) |
| `RichFrontmatterPropertyNotFound.vue` | 23 | Unresolved-property warning |
| `useRichFrontmatterPropertyEditing.ts` | 249 | Insert, commit, remove, rename, relation, validation calls |
| `noteContentPropertyKeys.ts` / `Rows.ts` / `KeyPresets.ts` | 232 / 200 / 62 | Key rules, row functions and validation, preset keys |
| `noteContentFrontmatter.ts` / `Parse.ts` / `noteProperties.ts` | 202 / 239 / 132 | Re-export barrel plus compose, YAML parse, value type |

#### Where the concept is spread or duplicated

Observations (file:line):

1. **The read-only path is a second implementation of the row, not a mode of it.** `RichFrontmatterReadOnlyList.vue`
   has its own grid (line 6, `grid-cols-[auto_minmax(0,1fr)]`) and its own chain choosing the value display
   (lines 19-54: list, relation, Wikidata, URL, else plain text). The editable row has a second grid
   (`EditablePropertyRow.vue:14`, three columns) and a third chain (lines 54-127: text, relation, image, Wikidata).
   `InsertForm.vue:43-105` is a fourth chain (Wikidata, image, URL, text). The decision "what kind of value is
   this key" is made in four places, in different orders. A `compact` flag exists only so the read-only path can
   draw the external link smaller (`ExternalLink.vue:11,29-35`; used at `ReadOnlyList.vue:25,40,51`,
   `ListPropertyValue.vue:39`). Connects to: iPad I4 (ragged columns, plain `[[...]]` for a single wiki-link value,
   image row shows only a file name), iPad D1, phone D1.
2. **Wiki-link rendering has two mechanisms.** Editable scalars use `PropertyValueField.vue` (contenteditable, wiki
   links drawn by `propertyValuePlainToDisplayHtml`); read-only scalars print `{{ row.value.value }}` as plain text
   (`ReadOnlyList.vue:54`), which is why `related` shows "[[UAT six properties]]" as text (iPad I4). Lists show wiki
   links only when the key is exactly `overlaps` (`ListPropertyValue.vue:75-78`). `PropertyValueField` already has a
   `readonly` prop (`PropertyValueField.vue:39,92`) that no property component passes (no `:readonly` in
   `RichFrontmatter*.vue`). ADR 0004 (line 127) says wiki-link rules apply to frontmatter scalars and one-level list
   items, so the split is an implementation accident, not a rule.
3. **The list-capable key rule is written twice.** `isListCapablePropertyKey` (`noteContentPropertyKeys.ts:214`) is
   used only by `tryCommitInsert` (`useRichFrontmatterPropertyEditing.ts:87`). The value dialog uses
   `!isScalarOnlyStructuralPropertyKey(key)` (`ScalarPropertyValue.vue:108-110`). The text-capable rule is also
   spelled twice in one row: `isTextCapablePropertyRow(...)` at `EditablePropertyRow.vue:55`, then the other kinds
   are re-tested one by one (lines 67, 76, 91). Hypothesis: they agree today; nothing forces it.
4. **Key names are listed in three places.** The preset lists (`noteContentPropertyKeyPresets.ts:13-26`), the `switch`
   in `propertyKeyMatchesPresetFamily` (`noteContentPropertyKeys.ts:110-136`), and single-key files
   `authoredAliasesValidation.ts`, `authoredOverlapsValidation.ts`, `authoredNoteLevelValidation.ts` (each has its
   own key test). A new preset means editing at least the list and the switch.
5. **The key field with presets is written twice, word for word.** `presetPanelOpen`, `onKeyPresetWrapperFocusOut`
   and `onPresetSelected` are identical in `EditablePropertyRow.vue:192,234-244` and `InsertForm.vue:148-170`.
   `KeyPresets.vue:24-30` computes the list from the whole preset set and never sees the typed text, so the list
   cannot narrow (iPad D3). The list is `absolute ... top-full w-full` inside the key cell (`KeyPresets.vue:42`) and
   options are `font-mono` buttons without wrap: hypothesis for why they overflow the 158 px panel (the classes are
   observed, the cause is not proven). In the phone stack layout it is drawn over the value field (phone D2).
6. **Validation has one message slot at the wrong level.** `validationMessage` is one `ref` in
   `RichFrontmatterProperties.vue:153`, rendered at lines 48-56 after the list, before the insert form. The composable
   receives `setValidationMessage` and cannot say which row failed
   (`useRichFrontmatterPropertyEditing.ts:88,102,139,185,199`). That is why the message sits at y 984 for a row at
   y 380 (iPad D2). The text "Duplicate property keys are not allowed." exists twice
   (`useRichFrontmatterPropertyEditing.ts:88` and `noteContentPropertyRows.ts:106`).
7. **The add form is a separate mini-row.** It has its own layout (`flex flex-wrap ... sm:w-auto`,
   `InsertForm.vue:5-8,39`), labels the rows do not have, and commits only through the value field's blur
   (`Properties.vue:79`); `tryCommitInsert` returns silently when key or value is empty
   (`useRichFrontmatterPropertyEditing.ts:83`). That is iPad I6. An existing precedent: `addWikiLinkAsProperty`
   (lines 192-217) already adds an ordinary row with an empty key and focuses its key input, so "a new property
   is a row" already exists for one path.
8. **Image handling has extra props for two callers.** `ImagePropertyValue.vue` takes five test-id props
   (lines 62-70) so the row and the insert form can pass different ids (`EditablePropertyRow.vue:81-84`,
   `InsertForm.vue:67-70`). The read-only path shows an image value as plain text (`ReadOnlyList.vue:54`; iPad I4).
9. **Fixed layout code.** Editable row: `grid-cols-[auto_minmax(8rem,auto)_minmax(0,1fr)]` plus `min-w-[8rem]` on the
   cell and the input (`EditablePropertyRow.vue:14,28,36`): chevron, key of at least 8 rem, value gets the rest.
   The row has no breakpoint; the only breakpoint in the concept is `sm:` in the add form (`InsertForm.vue:8,39`).
   Read-only: an `auto` key column with no upper bound (`ReadOnlyList.vue:6`), the likely code cause of iPad D1 and
   phone D1 (measured `dt` 484 or 571 px, `dd` 0 px; matches the grid, still a hypothesis until a layout test shows
   it). Panel indent `pl-8` is a fixed 32 px, not tied to the chevron width (`PropertyPanel.vue:3`). Every button and
   input is `daisy-*-sm` (32 px): 21 lines in 10 files (`grep -c "daisy-btn-sm\|daisy-input-sm\|daisy-btn-xs"`),
   which is iPad I1; there is no single place to set the touch size.
10. **Value area of about 60 px on a phone (phone D3)** follows from item 9: 42 px chevron + 158 px key + 42 px
    trailing button + gaps leave about 60 px at 375 px (as measured); hypothesis that a wrapping layout fixes it.

#### Candidates, best first

Order rule: candidates that improve the experience and reduce code come first; those that add code come after
and are marked. "Lines removed" is `wc -l` of the cited ranges; "lines added" is my guess of the replacement.

**1. Responsive row layout in place of the fixed grid (the seed's key example).** Improves: iPad D1 and phone D1
(value lost with a long key), iPad D4 and phone D3 (cut values), iPad I2 (density: a narrow row may wrap to two
lines instead of forcing a fixed column), and it is the base for 44 px targets (I1).
Change: one row rule, key cell `minmax(6rem, 40%)` with `break-words` (or `flex-wrap` so the value drops under
the key below a width), shared by read-only and editable. The class strings at `ReadOnlyList.vue:6`,
`EditablePropertyRow.vue:14,28,36` and `PropertyPanel.vue:3` become one shared rule; `min-w-[8rem]` on the input goes.
Lines: about 0 to -5 (class edits in 4 files; estimate from counted lines). Files: 0. Alone it does not reduce
code; with candidate 2 the layout lives once.
ADR: compatible (ADR 0004 governs stored markdown, not layout; there is no accepted ADR on component layout).
Protection: none for layout (jsdom has no layout). Gap to close first: a Cypress scenario at 820 and 375 px with a
long-key note, asserting the value is visible and there is no horizontal scroll; about +25 lines of feature and
step code (estimate). `note_property.feature` "Visiting a read-only property location focuses the property
value" covers a short key only.

**2. One row for read-only and editable (a `readonly` mode of the same row).** Improves: iPad I4 (same spacing,
link style, image display); with candidate 1 the layout is written once (iPad D1, phone D1).
Change: delete `RichFrontmatterReadOnlyList.vue` (81 lines); the row takes `readonly` and swaps input parts for
display parts (key as text, `PropertyValueField` with its existing `readonly`, same list and URL components);
chevron, edit and remove are not drawn. The `compact` flag in `ExternalLink` (about 8 lines) and
`ListPropertyValue` (about 4) goes.
Lines: remove 81 + about 12 (compact) + about 35 (duplicated chain, `ReadOnlyList.vue:19-54`) = about 130; add about
55 for readonly branches; net about -60 to -75 (estimate). Files: -1.
ADR: compatible, one owner question. ADR 0005: the property panel is part of the `noteProperty` route family; the
read-only path today has no chevron and only focuses the row. Keep that: read-only draws no panel controls, so no
product change. Adding the panel (for example Assimilate) to read-only would be new behavior, not this candidate.
Protection: `RichMarkdownEditor.frontmatter.spec.ts` (191), `.propertyWikiLinks.spec.ts` (168),
`.listProperties.spec.ts` (192), `.propertyLocation.spec.ts` (250), `note_property.feature` (read-only focus),
`property_wiki_link.feature` (wiki links in values). Gaps before the change: read-only tests for a single wiki-link
value, an image value and a Wikidata value (only spec names were checked for read-only cases, not each assertion),
plus candidate 1's layout test.

**3. One key field with presets, and presets that narrow while typing.** Improves: iPad D3 (does not narrow, wider
than its panel), phone D2 (list covers the value field; with narrowing, a custom key leaves no matches and the list
closes, so the value can be tapped).
Change: a `RichFrontmatterKeyField.vue` owning the input, `presetPanelOpen`, focus-out and preset selection
(item 5), used by the row and the insert form; the preset function takes the typed text and filters (about 6 lines
in `noteContentPropertyKeyPresets.ts`).
Lines: remove about 88 (`EditablePropertyRow.vue:27-52` 26, `:192,229-244` about 22, `InsertForm.vue:11-35` 25,
`:148,155-170` about 15); add about 55 (component) + 6 (filter); net about -25 (estimate). Files: +1.
ADR: compatible (ADR 0004 preserves unknown keys; a filtered list does not stop a custom key).
Protection: `noteContentPropertyKeyPresets.spec.ts` (98), `RichMarkdownEditor.propertyEntry.spec.ts` (242),
`.propertyRowEditing.spec.ts` (233). Gap: a test that the list narrows to typed text (pure function, cheap) and
candidate 1's viewport test extended to check the list stays inside the viewport.

**4. The add form becomes a draft row of the same component.** Improves: iPad I6 (aligned columns; a visible Add
button on that row that also explains a missing key or value), phone D2 (same key field as candidate 3), one look.
Precedent: `addWikiLinkAsProperty` (item 7).
Change: delete `RichFrontmatterInsertForm.vue` (171 lines) and its pass-through in `Properties.vue:66-83` (18 lines);
a draft row (not emitted by `filterForEmit`) uses the row component; `tryCommitInsert`
(`useRichFrontmatterPropertyEditing.ts:80-107`) stays. Image test-id props shrink (item 8).
Lines: remove about 190; add about 40 (draft flag, Add button, messages); net about -120 to -150 (estimate;
largest single saving and highest risk). Files: -1.
ADR: compatible. ADR 0006 supports messages for business outcomes ("Add a key" or "Add a value" instead of doing
nothing); no new `catch`.
Protection: `RichMarkdownEditor.propertyEntry.spec.ts` (242) and page object
`e2e_test/start/pageObjects/noteRichPropertyMethods.ts` (190) use the insert form's ids `rich-note-property-key`
and `rich-note-property-value`; keep the ids or change page objects in the same commit. Gap: a test for key only
and value only (no test found, slice 3 table), written first. Do after candidates 2 and 3.

**5. One value-kind decision.** Improves nothing directly; it stops the four chains (item 1) from drifting, which
is why iPad I4 exists. Change: one function `propertyValueKind(key)` (text, list-capable text, relation, image,
Wikidata, URL) in `noteContentPropertyKeys.ts`, used by the row, the insert form and `tryCommitInsert`, replacing
`isTextCapablePropertyRow`, `isListCapablePropertyKey` and the dialog's `!isScalarOnlyStructuralPropertyKey`
(item 3). Lines: remove about 25, add about 15; net about -10 (estimate). Files: 0. ADR: compatible. Protection:
`noteContentPropertyKeys.spec.ts` (142). Best done inside candidates 2 and 4; small reduction, so it ranks below them.

*The next three add code or scatter the concept. They improve the experience, and are listed after the
code-reducing ones.*

**6. Validation message next to its row (adds code).** Improves: iPad D2. Change: the composable reports the failed
row index with the message; the row draws it below itself. Lines: about +20, -3 (moved `<p>`); net about +17.
Files: 0. Cheaper option, about +4 lines: keep one message but scroll it into view when it appears; it fixes "off
screen" but not "far away". ADR: compatible (ADR 0006 supports business-outcome messages). Protection: message text
is covered (`propertyRowEditing.spec.ts`, `noteContentPropertyRows.validate.spec.ts`); position is not. Gap: viewport
check with an invalid `note_level` on a long note.

**7. 44 px touch targets on touch widths (adds a little code, conflicts with density).** Improves: iPad I1. Change:
one rule on the section (for example `@media (pointer: coarse)` raising `.daisy-btn-sm` and `.daisy-input-sm` to 44 px
inside the properties section) instead of editing 21 class strings. Lines: about +8 CSS. It raises each row from 32
to 44 px, so a 19-row note grows by about 230 px (19 x 12 px, estimate) and worsens iPad I2 unless candidate 1 lets
rows fit better. ADR: compatible. Protection: none; finger feel needs a real iPad.

**8. Feedback for silent outcomes (adds code, scatters the concept).** Covers iPad I7 (silent append to a list key),
iPad I10 (upload error gone after 2 s) and the typed-ID Wikidata Save with an empty search list. Each lives in a
different place (composable, `ImagePropertyValue.vue`, `WikidataAssociationDialog`), each about +10 to +20 lines
(estimate), and nothing merges. Do after the structure work if the owner wants them. Protection: append is covered
(`noteContentPropertyRows.append.spec.ts`), the message is not; the Wikidata combination is not (slice 3 table).

#### Not recommended

- **A collapsed "show all" summary for many properties (iPad I2).** New mode and state, so more code; candidate 1 may
  give the same gain. Owner's call after candidate 1.
- **Merging all `noteContent*` utilities into one file.** Moves lines, saves none (1,058 today), and loses small,
  well-tested units.
- **Sharing one YAML parser between frontend (`noteContentFrontmatterParse.ts`, 239 lines) and backend
  (`Frontmatter*.java`, 917 lines).** Two languages; parity risk is real but not a UX issue, and it would need a
  generated contract. Out of scope for this story.
- **Replacing the contenteditable `PropertyValueField` with a plain `<input>`.** It draws wiki links inside values
  (ADR 0004 line 127); removing it loses links in editable mode and does not shrink the row.
- **Changing the value dialog (264 lines) or the panel's Assimilate and Skip actions.** No finding needs it; the
  26 px close button is in the shared Modal and is a separate, general item.
- **A new confirmation dialog for remove (iPad I8).** Adds code; a text label on the remove button or an undo
  message is proportionate polish, not structure work.
- **Hiding the `type: Note` row when there are no other properties (iPad I5).** Whether the row is stored is
  unchecked; a display rule could disagree with ADR 0004 (stored notes carry `type`). Ask the owner first.

#### ADR check

Read `docs/adrs/README.md` and the accepted ADRs 0004, 0005 and 0006; 0002 and 0007 (Git synchronization,
environments) do not touch the properties UI. No ADR covers frontend structure or layout, so the layout and
component changes are not constrained by one.

- ADR 0004: "Preserve author-owned and unknown frontmatter keys on persist and round-trip." All candidates change
  display and entry components only; candidate 4 must keep `filterForEmit` and `notePropertiesFromPropertyRows`
  behavior so an unsaved draft row never reaches stored markdown. "Wiki-link rules apply to the body and to YAML
  frontmatter values (scalars and one-level list items)": candidate 2 aligns read-only display with it. Compatible.
- ADR 0005: "Opening or closing the property panel replaces within the note family ... it must not silently look
  like `noteShow`." Candidates 2 and 4 keep `useNotePropertyPanelLocation` and `useFocusedNoteProperty` unchanged.
  Compatible. Owner decision only if read-only should gain the panel (not proposed).
- ADR 0006: "Handle an exception when a business requirement needs a specific outcome, or when wrapping improves the
  failure message." Candidates 6 and 8 add visible messages for business outcomes and no `catch`. Compatible.

No ADR conflict found. One question for the owner: should a read-only viewer ever see row panel controls (Assimilate,
Skip)? The answer does not block candidates 1 to 5.

#### What protects the behavior today

- Component tests (jsdom, no layout): `frontend/tests/components/form/RichMarkdownEditor.frontmatter.spec.ts` (191),
  `.propertyEntry.spec.ts` (242), `.propertyRowEditing.spec.ts` (233), `.listProperties.spec.ts` (192),
  `.propertyValueDialog.spec.ts` (173), `.propertyWikiLinks.spec.ts` (168), `.propertyLocation.spec.ts` (250),
  `.propertyMemoryTracking.spec.ts` (246), `.changesOnlyTheEdit.spec.ts` (152).
- Utility tests: `frontend/tests/utils/noteContentPropertyKeys.spec.ts`, `noteContentPropertyKeyPresets.spec.ts`,
  `noteContentPropertyRows.spec.ts` (plus `.append` and `.validate`), `noteContentFrontmatter*.spec.ts`.
- End to end: `e2e_test/features/note_view/note_frontmatter_image.feature` (header image, upload becomes a file,
  image property URL), `note_topology/note_property.feature` and `property_wiki_link.feature` (location, panel,
  read-only focus, wiki links in values), `recall/property_memory_tracker.feature`, `note_edit.feature`.
- Not protected: layout, width, overlap and viewport behavior (no phone or tablet property scenario; the plan's
  `grep -ril ipad frontend/tests e2e_test` found nothing), the position of the validation message, and the key-only
  or value-only add. Closing these gaps first (one viewport feature with a long-key note at 820 and 375 px; a
  key-only add test) is the cheapest way to make candidates 1 to 4 safe.

Time spent on slice 4: about 20 minutes (13 components, the composable, key, row and preset utilities, ADRs 0004 to
0006, counting; no application run).

### Synthesis and follow-up (slice 5)

Sources: slices 2 to 4 above. Numbers: "iPad D1" is iPad defect 1, "iPad I6" is iPad improvement 6, "phone D2" is
phone operability defect 2, "C3" is design candidate 3 (slice 4). All line effects are slice 4 estimates.

#### Summary for the owner

- **What was exercised:** the note properties area (view, add, change, remove, list dialog, image, relation type,
  Wikidata dialog, validation, row panel) in editable and read-only mode, on an iPad (portrait 820 px and landscape
  1180 px, touch, keyboard emulated), then on a phone (375 px) and a desktop (1440 px). The code was reviewed for
  duplication and size.
- **iPad, headline:** every journey can be completed and saving works. The main defect is read-only: a long key
  leaves the value 0 px wide, so the value is not visible (iPad D1). Next: the validation message can sit far below
  the edited row and off screen on long lists (D2), the key preset list does not narrow and is wider than its panel
  (D3), and long text is cut with no cue (D4). Controls are 32 px high (guide: 44 px), and 19 rows push the note
  body to 84% of the screen height (I1, I2).
- **Phone, operability:** all journeys can be completed. Three obstacles: the read-only value of a long key is
  unreachable even by panning (phone D1), the preset list covers the value field and Choose image until the key loses
  focus (phone D2), and plain text values show about 50 px (phone D3).
- **Design finding:** the read-only list, the editable row and the add form each decide separately how to show a key
  (four value-kind chains, three grids). One row with a read-only mode, one key field, and the add form as a draft row
  would remove about 200 to 250 lines (estimate) and fix the most visible defects with them. The size the story
  quoted ("about 4,000" lines) is not reproducible; all frontend property code is 3,047 lines.
- **Not confirmed:** a Wikidata Save that shows nothing (reproduced only for a typed ID with an empty search list)
  and one Replace timeout (not reproduced).

#### Actual UAT time

| Part | Minutes |
| --- | ---: |
| Setup (slice 1) | about 4 |
| iPad exploration (slice 2, 14:07 to 14:27) | about 20 |
| Phone and desktop exploration, Wikidata and Replace checks (slice 3, 14:29 to 14:44) | about 15 |
| Exploration total | about 35 of the 60 minute budget |
| Design and code review (slice 4) | about 20 |
| Write-up (slice 5) | not timed here |

The exploration budget was not used up; the remaining time went to checks and to clarifying the read-only mode.

#### Scenarios not reached / needs a real iPad or phone

Consolidated from the coverage lists above; nothing is added.

- **Needs a real device:** iPadOS Safari behaviour with the software keyboard (visual viewport, auto-scroll to the
  focused field, whether presets and the message stay visible), floating keyboard and shortcut bar, hardware keyboard
  and trackpad, Apple Pencil, split view widths, safe-area insets, Safari zoom on focus of small inputs, the native
  file picker and camera upload, long-press and text selection in 32 px inputs, momentum scrolling in the preset list
  and dialogs. On a phone: the keyboard shrinking only the visual viewport, the Wikidata dialog's Save and Close at
  the bottom edge, and whether the 44 px rule works for a real finger.
- **Not run on the iPad:** tapping Assimilate and Skip (measured only), list reorder, the relation type selection,
  the portrait list dialog with the keyboard, rename to an existing key, Wikidata Save with a real search result,
  read-only landscape beyond height measurements, dark mode, very narrow split view. (Slice 3 ran list reorder,
  relation type, duplicate rename and Wikidata with a search result on the phone only.)
- **Not run on the phone or desktop:** landscape phone, phone in dark mode, the Wikidata follow-up choice (Replace
  title, Add as alias) and what it inserts, descriptive text for other entry types, tapping the clipped wiki-link
  value, list dialog on a phone in landscape; on desktop: image upload, relation type, Wikidata, read-only view and
  long keys.
- **Not checked in code or running product:** whether the `type: Note` row is stored or only shown (iPad I5); the
  relation type control flow on the iPad (I12).

#### Suggested priorities

One ordered list. iPad findings come first, and changes that improve the experience and reduce code come before
those that add code. Slice 4's ranking is kept; the validation message (iPad D2, medium severity on the iPad) is moved
up within the code-adding items, as the first of them.

1. **Shared responsive row, and read-only as a mode of the same row** (C1 + C2; iPad D1, D4, I4; phone D1, D3;
   base for I1 and I2). [fix existing behavior] [reduces code]
2. **One key field with presets that narrow while typing** (C3; iPad D3; phone D2). [fix existing behavior]
   [reduces code]
3. **Add form becomes a draft row with a visible Add button and reasons** (C4 with C5; iPad I6; phone D2 rest).
   [new capability] [reduces code]
4. **Validation message next to its row** (C6; iPad D2). [fix existing behavior] [adds code / polish]
5. **44 px touch targets on touch widths** (C7; iPad I1; iPad D5 partly). [fix existing behavior]
   [adds code / polish]
6. **Feedback for silent outcomes** (C8; iPad I7, I10; the typed-ID Wikidata Save). [new capability]
   [adds code / polish]
7. **Remove control with a text label or undo message** (iPad I8). [new capability] [adds code / polish]

Not proposed (slice 4, "Not recommended"): a collapsed "show all" mode (iPad I2), merging `noteContent*` utilities,
one YAML parser for both languages, replacing the contenteditable value field, changing the value dialog or panel
actions, hiding the `type: Note` row (iPad I5).

#### Proposed follow-up story outcomes

Candidates for the owner to select. No backlog entry was created. Effort bands from this seed: S = 30 to 60
minutes, M = 1 to 2 hours, L = 2 to 4 hours, including delivery.

**Fixes to existing behavior, changes that reduce code**

- **F1. Long keys no longer hide values, on iPad and phone.** A note with a very long key shows its value at 820 and
  375 px without sideways scrolling, in one shared row rule. Covers iPad D1, D4; phone D1, D3. Effort M. Line effect
  (estimate): about 0 to -5. Protecting test first: a Cypress scenario at 820 and 375 px with a long-key note,
  read-only and editable, asserting the value is visible and there is no horizontal scroll (about +25 lines of
  feature and step code, estimate).
- **F2. Read-only properties look and link like the editable ones.** One row component with a read-only mode; the
  read-only list file is deleted, single wiki-link values become links, image values show as images. Covers iPad
  I4. Effort M to L (needs F1). Line effect (estimate): about -60 to -75, one file fewer. Protecting tests first:
  read-only component tests for a single wiki-link value, an image value and a Wikidata value (only spec names were
  checked, not assertions), plus F1's layout scenario.
- **F3. Typing a key narrows the presets, and the list stays inside its panel and off the value field.** Covers iPad
  D3; phone D2. Effort M. Line effect (estimate): about -25, one file more. Protecting tests first: a unit test that
  the preset function filters by typed text (cheap), and a viewport check that the list stays on screen and does not
  cover the value field at 375 px.

**Fixes to existing behavior, polish (adds code)**

- **F4. The validation message appears under the row that was rejected.** Covers iPad D2. Effort S. Line effect
  (estimate): about +17, or about +4 for the cheaper "scroll the message into view". Protecting test first: a
  viewport check with an invalid `note_level` on a long note (message text is already covered).
- **F5. Touch controls are 44 px high on touch widths.** One rule for the section instead of 21 class edits. Covers
  iPad I1. Effort S. Line effect (estimate): about +8 CSS; a 19-row note grows by about 230 px (estimate), so it is
  best after F1. Protecting test: none possible in jsdom; check on a real iPad.

**New capabilities, one reduces code**

- **N1. The owner adds a property through a row that has a visible Add button and says why nothing was added.**
  Add form deleted; key-only or value-only shows a reason. Covers iPad I6; rest of phone D2. Effort L. Line effect
  (estimate): about -120 to -150, the largest saving and the highest risk. Protecting tests first: key-only and
  value-only add tests (none exist); keep the ids `rich-note-property-key` and `rich-note-property-value` or change
  the page object `noteRichPropertyMethods.ts` in the same commit. Do after F2 and F3.
- **N2. The user sees a message when the product changes something silently.** Adds a note when a value is appended
  to a list key ("Added to url"), keeps the upload error until the next action, and shows a result when a typed
  Wikidata ID cannot be used. Covers iPad I7, I10; the Wikidata silent Save. Effort M. Line effect (estimate): about
  +10 to +20 in each of three places. Protecting tests first: for the append and the upload message, none exist; the
  Wikidata combination (typed ID plus empty list) has none. Confirm the Wikidata case is a defect first (open
  question 4).
- **N3. Remove is clearly labelled or can be undone.** Covers iPad I8. Effort S. Line effect (estimate): small
  addition. Protecting test first: a component test for the label or the undo.

#### Open questions for the owner

1. **Read-only panel controls.** Should a read-only viewer ever see the row panel controls (Assimilate, Skip)? Today
   read-only has no chevron. The answer does not block F1 to F3; F2 keeps today's behavior.
2. **Assimilate from a row panel.** Tapping Assimilate ran `POST /api/assimilation` and then moved the page to another
   note (the next one in the sequence). Is that intended when the button is inside a single property's panel? It was
   observed only on the phone.
3. **Skip wording.** The panel's Skip asks "Leave this note out of the assimilation sequence?", a wording about the
   whole note inside a single property's panel. Should it change?
4. **Wikidata Save with a typed ID and no search results** showed nothing (twice, iPad portrait and phone). Is a
   typed ID without matching title results meant to be supported? With results, Save asks for Replace title or Add as
   alias and works.
5. **Empty state.** `UAT no properties` shows a `type: Note` row. Is that row stored in the note or only shown? The
   answer decides whether an empty-state message or hiding the row is possible under ADR 0004.
6. **Density.** After F1, is a collapsed "show all" summary still wanted for notes with many properties (19 rows use
   84% of an iPad portrait screen)? Slice 4 does not recommend it before F1.
7. **Not reproduced, listed honestly:** (a) a Replace tap timed out once for 30 s on the iPad right after adding the
   image; four taps on the phone and one repeat on the iPad gave the file chooser at once (53 to 66 ms on the phone),
   so it stays "suspected, not reproduced"; (b) a tap on the "Properties" heading was blocked once on the phone right
   after choosing an image (cause unknown, a busy overlay during the upload is a guess); (c) the Wikidata dialog
   "stays open" on the iPad is partly explained by the suggested-title question, but the empty-list case remains
   unexplained; (d) rename to an existing key on the iPad (an earlier attempt renamed by mistake) was checked only on
   the phone. The yellow "T" Testing button covers controls on the Development stack only and is not a defect.

#### Side effects left in the development database

Remove these if wanted; only UAT items are affected.

- **Notebook 22 "UAT notebook" is shared to the Bazaar** (`POST /api/notebooks/22/share`). It was needed for the
  read-only view.
- **Scratch notes 13719 to 13725** were edited destructively: 13719 `UAT edit portrait`, 13720 `UAT edit landscape`,
  13721 `UAT edit phone`, 13722 `Tokyo`, 13723 `Kyoto`, 13724 `UAT wd noresult xq`, 13725 `UAT edit desktop`. Uploaded files (`photo-b.png`, `photo-ph-c.png`, `photo-ph-d.png`) sit in the
  notebook.
- **One assimilation record** was created by tapping Assimilate in a row panel on the phone (`POST /api/assimilation`);
  the page then moved to a note titled `Perf100`, which is not a UAT note. Its assimilation state changed.
- Notes 13716 to 13718 (`UAT no properties`, `UAT six properties`, `UAT many properties`) are the fixed test notes;
  the edit runs used the scratch notes instead.

### Actual exploration minutes (slice 2)

About 20 minutes of exploration by the wall clock (14:07 to 14:27, 2026-09-30), inside the 40-minute budget
for this slice.

### Actual exploration minutes (slice 3)

About 15 minutes by the wall clock for slice 3 (14:29 to 14:44, 2026-09-30), including phone and desktop runs, the
Wikidata and Replace checks, the coverage check and writing this section; the planned budget was 12 minutes plus
an 8-minute reserve. Slices 2 and 3 together: about 35 minutes of exploration.
