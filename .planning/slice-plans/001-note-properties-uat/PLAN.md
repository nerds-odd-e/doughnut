# Identify note properties UX and design improvements through a one-hour manual UAT

**Identity:** SEED-061#story-1
**Source:** [story](../../seeds/SEED-061-note-properties-ux-uat.md#story-1), refined with the owner's
2026-09-30 decisions recorded there: an iPad is the primary device, a phone only has to be operable,
and an agent runs the UAT.

## Goal and scope

The owner receives one findings report, a `## UAT Findings` section in the seed. It ranks iPad
(portrait and landscape, touch) findings first, judges the phone only by whether every journey can be
completed, and gives design and architecture recommendations that reduce code and improve cohesion at
equal weight. Precedent: the book reading UAT (SEED-054#story-1).

Included: the story's viewing, editing, row panel, touch-fit and presentation scenarios; the design and
architecture assessment with line-count estimates checked against ADR 0004 and existing scenarios;
defects the automated tests do not cover, recorded separately.

Excluded: product code, automated tests, fixing anything found, creating backlog entries or seeds for
findings, and real-device testing (device-mode limits are listed as "needs a real iPad or phone").

Budget: 60 minutes of exploration across slices 2 and 3 (about 40 + 12, with an 8-minute reserve for
surprises and confirmation). Setup (slice 1), the code review (slice 4), and write-up (slice 5) are timed
and reported separately.

## Decisive premises

| Premise | Observation | Result |
| --- | --- | --- |
| A browser can be driven with device emulation | `ls node_modules/.pnpm \| grep playwright`; `ls ~/Library/Caches/ms-playwright` | Playwright 1.63.0 with Chromium headless shells installed |
| The properties code size the story quotes | `cat frontend/src/components/form/RichFrontmatter*.vue frontend/src/composables/useRichFrontmatterPropertyEditing.ts frontend/src/utils/noteContentFrontmatter*.ts frontend/src/utils/noteProperties.ts \| wc -l` | 13 `RichFrontmatter*` components and the listed files total 2,430 lines; the story's "about 4,000" also counts the other `noteContent*` utilities and tests, so slice 4 restates the count and its method |
| An existing scenario protects properties | `ls e2e_test/features/note_view/` | `note_frontmatter_image.feature`; component test `RichMarkdownEditor.frontmatter.spec.ts` named in the seed |
| The Development stack is needed for a signed-in app | `curl http://127.0.0.1:5175/` | 000 (not running now); `.agents/agent-map.md`: start it with `pnpm dev` from the primary checkout only, sign in `manual` / `password`; linked worktrees refuse it |
| No existing iPad-width proof | `grep -ril ipad frontend/tests e2e_test` | No matches, so no automated test covers tablet layout |

Unobserved until slice 1 (probe): the Development stack starts and serves the app from the primary
checkout, and Playwright's iPad device descriptors render the note page signed in.

## Proof

This story changes no product code. Proof is the findings report's evidence: for each defect, expected
versus observed, reproduction steps, viewport and note characteristics, and screenshots or measurements
kept under `$CLAUDE_JOB_DIR/tmp/uat/` (not committed). Suspected but unreproduced problems are recorded
with the conditions tried, never as confirmed defects. Every line-count estimate states how it was
obtained.

## Ordered slices

### 1. UAT environment is usable end to end
Type: Structure
Status: done
Size: about 15 minutes, outside the UAT budget.
Proof: the Development app started from the primary checkout serves the login page; a Playwright session
using an iPad descriptor signs in as `manual` and opens a UAT note. Notes exist with no properties, six
properties, and many properties, with long keys and values, a long alias list, a wiki-link value, an image
value, and a Wikidata ID. Record setup minutes. Explained empty change: nothing to commit besides this
plan's status.

Probe: if the stack or emulated iPad session cannot be established, stop dependent slices and revise this
plan.

Enables slices 2 and 3.

### 2. iPad findings are recorded
Type: Behavior
Status: done
Size: about 40 minutes of UAT budget.
Proof: `## UAT Findings` in the seed gains setup, note characteristics, and iPad findings (portrait about
820 px, landscape about 1180 px, touch), each with evidence.

Behavior: an iPad-emulated signed-in reader → view the notes in read-only and editable modes; add,
change, and remove properties, list values, image upload, relation type, the Wikidata dialog, validation
messages, the row panel and its remove button, with the on-screen keyboard open; judge tap targets,
overflow, density, hierarchy, empty states, and hover-only cues → defects and improvements are written.

### 3. Phone operability and defect findings are recorded
Type: Behavior
Status: done
Size: about 12 minutes of UAT budget plus the 8-minute reserve.
Proof: the same section gains phone findings (375 px, judged only by operability), a desktop sanity
pass, and a separate list of defects the automated tests do not cover, each with evidence.

Behavior: the notes from slice 1 → on a 375 px viewport view, add, change, remove, and save a property and
reach every control; repeat the core journeys on desktop → operability defects are written; cosmetic
phone observations are marked optional and lower priority.

### 4. Design and architecture assessment is recorded
Type: Behavior
Status: done
Size: about 25 minutes, outside the UAT budget.
Proof: the report's design section lists, for each candidate, where the properties concept is spread or
duplicated between read-only and editable paths, what could be merged or removed, the estimated lines and
files saved with the counting command, the check against ADR 0004, and the scenarios and component tests
that protect the behavior.

Behavior: the properties code and slices 2–3 observations → read the files, compare the two paths, count
lines → recommendations ordered with experience-improving and code-reducing changes first.

### 5. The owner can choose follow-up work from the report
Type: Behavior
Status: done
Size: about 10 minutes, outside the UAT budget.
Proof: the section ends with actual UAT time, scenarios not reached, needs-real-device cases, suggested
priorities, and proposed follow-up story outcomes separating fixes from new capabilities and code-reducing
changes from polish.

Behavior: findings from slices 2–4 → synthesize → the owner can select follow-up work without rereading
the session.

## Current decisions

- Development stack from the primary checkout (`pnpm dev`, account `manual`); UAT data in
  `doughnut_development` under notes named `UAT …`. The primary checkout is not edited.
- Playwright device emulation (iPad portrait and landscape, iPhone-width 375 px, desktop); real-touch
  differences are reported as needing a real device.
- Screenshots and raw measurements stay out of the repository.

## Learnings

- Slice 1 (about 4 minutes): Development stack runs from the primary checkout at http://127.0.0.1:5175/
  (login page returns 200). Notes are in notebook "UAT notebook" (id 22, account `manual`):
  `UAT no properties` /n13716, `UAT six properties` /n13717 (screenshot shows five rows, so slice 2 counts
  the real number), `UAT many properties` /n13718 (19 rows: 14-item aliases, `wikidata_id: Q42`, long key
  and value, wiki-link values, uploaded `example.png` image). Helper `uat.mjs` with presets `ipad-portrait`,
  `ipad-landscape`, `phone`, `desktop` is under `$CLAUDE_JOB_DIR/tmp/uat/`.
- Sign in through `/users/identify`; call the API from inside the page (`page.evaluate(fetch)`) because the
  secure session cookie is refused over http.
- First look, to confirm in slice 2: long keys and values are truncated; the floating yellow "T" button
  overlaps the rightmost row controls; adding `wikidata_id` may insert body text automatically.
- Slice 2 (about 20 of 40 UAT minutes): iPad portrait and landscape findings are in the seed's
  `## UAT Findings`. Read-only mode needs a second user, so notebook 22 was shared to the Bazaar and viewed
  as `old_learner`. The yellow "T" button is the Testability menu (testing environment only), not a defect.
  Unresolved for slice 3: Wikidata dialog Save showed no visible result for `Q42`; one Replace tap timeout
  did not reproduce. Not reached: Assimilate/Skip taps, list reorder, relation type selection flow, rename
  to an existing key. About 20 minutes remain in the exploration budget plus the reserve.
- Slice 3 (about 15 minutes; slices 2 and 3 together about 35 of the 60 UAT minutes): phone, desktop, and
  the unresolved slice-2 items are recorded in the seed. Read-only long-key rows lose their value on the
  phone as on the iPad; the key preset list covers the value field on the phone; typed `Q42` with an empty
  search list gives a silent Save; the Replace timeout stayed unreproduced. Assimilate from a row panel moves
  the page to another note (recorded as an owner question). Dev data side effects: scratch notes 13721-13725
  and one assimilation record.
- Slice 4 (about 20 minutes, code reading only): 13 `RichFrontmatter*.vue` = 1,608 lines plus 822 lines of
  composable and utilities = the 2,430 restated in the premises; the seed's "about 4,000" is not
  reproducible by any counting method and the section says so. Eight ranked candidates, no ADR conflict;
  one owner question (should a read-only viewer see row panel controls).
- Slice 5: the seed's `### Synthesis and follow-up (slice 5)` holds the actual times, coverage gaps, seven
  ordered priorities, follow-up candidates F1-F5 and N1-N3 (candidates only, no backlog entries), owner
  questions, and the development-database side effects.

## Execution complete

Product advice: Choose follow-up work from the seed's `### Synthesis and follow-up (slice 5)`. Start with the shared responsive row that also serves as the read-only view, then one key field with narrowing presets, then the draft-row add form; these improve the iPad experience and remove code. Answer the owner questions first: whether a read-only viewer sees Assimilate and Skip, and whether Assimilate from a row panel should move to another note. Selecting stories and reordering the backlog stay with the owner.
