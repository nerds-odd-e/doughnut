# Focus on note content while revealing details when needed

**Source:** [Focus on note content while revealing details when needed](../../seeds/SEED-078-cohesive-note-workspace.md#story-5)

**Identity:** SEED-078#story-5

## Goal and scope

A reader opening a note reaches the authored title and body first and reveals
the note's properties and dates when needed. One details disclosure between
the title and the body is compact on every note opening, reveals the existing
property section followed by the created and updated dates, and is opened by
property-targeted arrivals and actions. Dates are unambiguous and quiet with a
theme-aware freshness cue; the reading surface gets the agreed spacing,
margins, and wide-window measure. The source's Goal, Scope, Key examples, UI
sketch, trade-off defaults, and Architecture apply.

Deferred: remembering the disclosure across notes or visits, relative-time
wording, a disclosure for the readme page editors, further property row and
editor restyling, new metadata or settings. Existing property validation and
access rules are the only rejection constraints.

Preparation is planning only: no Take or execution is authorized. Observed
checkout: this worktree on 2026-10-20, first at `aeb5452823` (seed refinement
commit, before story 4 landed) and again after reconciliation onto trunk
`deb065d8dc`, which contains story 4's completed readable property rows.
Execution starts from current trunk and reconciles shared changes normally.
There is no blocking supplier.

## Existing solutions and architectural decisions

PFE choice: change the existing note reading surface and property section;
add one small disclosure responsibility, no new representation of properties
or dates.

- `NoteTextContent` already is the note reading surface (title, then the
  `Note content` region holding the editor) and receives the note topology
  with `createdAt`/`updatedAt`. It is used by `NoteShow` and by the
  conversation context pane `NoteContextReader`. It becomes the single
  details-disclosure owner.
- `RichFrontmatterProperties` (inside `RichMarkdownEditor`) owns the rows,
  draft, snapshots, validation message, `Add property`, and
  `addWikiLinkAsProperty`; `useRichFrontmatterPropertyEditing` owns
  `showSection`/`headingVisible`. `RichMarkdownEditor.composeBodyMarkdown`
  reads the mounted section's rows and falls back to none, and
  `composeNoteContentInPlace` with no rows rewrites the note without its
  frontmatter (`frontend/src/utils/noteContentInPlaceEdit.ts:36`). The
  collapsed section therefore stays mounted and hidden.
- Provide/inject is the established way the surface reaches the section:
  `richFrontmatterProvide.ts` (readme context) and
  `useFocusedNoteProperty.ts` (`provideFocusedPropertyKey`). Reuse the
  pattern for the disclosure state; a host without a provider (the readme
  editors through `ScopedReadmeEditor`) keeps today's revealed section.
- `useFocusedNoteProperty` resolves the focused key from the `noteProperty`
  route or a provided key and scrolls the row into view; the stale-key
  notice `RichFrontmatterPropertyNotFound` renders outside the section.
  Reveal on a focused key reuses that composable's knowledge.
- The expanded-panel chevron in `RichFrontmatterPropertyRow` (button with
  `aria-expanded`) and the existing coarse-pointer 44px selector are the
  product's disclosure and touch conventions; follow them for the new
  control. Touch proof uses a `*.touch.spec.ts` file, which the
  `chromium-touch` instance in `frontend/vitest.config.ts` runs with real
  coarse-pointer emulation, and geometry checks follow
  `.agents/skills/frontend-testing/SKILL.md` "Layout and Pointer Tests".
  `NotebookHealthFindings` uses a daisy collapse for a different purpose;
  not reused.
- Date formatting: `AssimilationModes.formatNextRecallAt` already uses
  `toLocaleDateString(undefined, {...})` with a spelled month. Use the same
  API with `year: "numeric"`; no date library.
- Freshness: `NoteRecentUpdateIndicator` computes a fixed-RGB age colour and
  is used only by `NoteShow`. Its age-to-emphasis meaning moves into the
  compact line with theme tokens; the component and the bottom dates block
  are deleted.
- Body typography: `rich-content-prose` is Tailwind prose with `max-w-none`
  inside `NotebookSidebarLayout`'s `container mx-auto px-4` main column;
  no text measure exists today. Bound the measure on the note reading
  surface, not on the layout shared with other pages.
- Maintained documentation: `docs/note-content-saving.md` "Rich property
  editing" describes the property panel journeys; the details disclosure is
  documented beside it.

Accepted ADR index and in-file statuses agree for 0000–0007; no supersession
or conflict was found. Relevant decisions:

- [ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md): properties and
  authored values keep their meaning; the disclosure is presentation.
- [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md):
  authored content, including a title's date prefix, is unchanged.
- [ADR 0005](../../../docs/adrs/0005-web-routes-accepted.md): a property
  location reveals and focuses its row with replace semantics; a stale key
  keeps its explicit not-found state; the disclosure itself adds no route.
- [ADR 0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md):
  observations use disposable browser-test state.

The [North Star](../../NORTH-STAR.md) needs no new topic for this local
presentation responsibility.

## Decisive premises and observations

| Premise / consuming operation | Observation and result | Consequence |
| --- | --- | --- |
| A hidden-but-mounted section keeps body composition intact; an unmounted one drops the frontmatter | Traced `RichMarkdownEditor.composeBodyMarkdown` → `frontmatterPropertiesRef.value?.getPropertyRows() ?? []` → `composeNoteContentInPlace`, whose `parsed.ok && rows.length === 0` branch composes from no rows (`noteContentInPlaceEdit.ts:36`). | Slice 1 hides rows without unmounting and owns a discriminating proof: a body edit while rows are hidden keeps the frontmatter. |
| The focused frontend browser command runs here and the affected suites are green before any change | Ran in this worktree at `aeb5452823` on 2026-10-20: `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteShow.spec.ts tests/notes/NoteContextReader.spec.ts tests/components/form/RichMarkdownEditor.frontmatter.spec.ts tests/components/form/RichMarkdownEditor.propertyLocation.spec.ts tests/notes/NoteEditableContent.spec.ts tests/pages/RecallPage.justReview.spec.ts tests/pages/NoteShowPage.imageUpload.spec.ts` passed 7 files / 52 tests in 13.96s (Chromium); rerun on the reconciled tree at trunk `deb065d8dc` (with story 4's rows) passed 7 files / 52 tests in 4.20s. No product change was made. | Slice proof uses the same command family and files; this baseline is not acceptance of later changes. |
| Consumers of the freshness colour | `e2e_test/start/pageObjects/noteRecentUpdate.ts` reads `.note-recent-update-indicator` CSS `color` and compares the first channel; `frontend/tests/notes/NoteShow.spec.ts` pins exact RGB per age. No other consumer. | Slice 3 replaces both observations with the retained ordering (newer note more prominent) on the new line. |
| End-to-end property journeys reach rows right after visiting a note | Twelve features use property steps; ten steps visit a note (not a property location) first. Page objects reaching rows: `noteRichPropertyMethods.ts` (18 region scopes), `notePropertyLayoutMethods.ts` (6), `notePropertyLocationMethods.ts` (5, route-driven), `noteRichPropertyAssimilationMethods.ts` (1), `assimilationPage/shared.ts`, `step_definitions/note_property.ts`. | Slice 2 adds one reveal helper in `notePageContentRegion.ts` and calls it from the methods that reach rows on a note location; route-driven methods rely on the route reveal. |
| Unit consumers that reach rows through the note surface | `NoteContextReader.spec.ts` asserts `wrapper.text()` contains property values (a whole-container text check that hidden rows would still satisfy); `NoteShowPage.imageUpload.spec.ts` adds an image property through the page; `RecallPage.justReview.spec.ts` reaches a property through `notePropertyLocation`. Specs mounting `RichMarkdownEditor` or `NoteEditableContent` directly have no provider and stay revealed. | Slice 2 reconciles the first two to reveal and observe visible content; the third is covered by the route reveal. |
| No code parses dates from titles | Searched `frontend/src` for date construction and title handling: only the freshness indicator and `titlePatternRender.ts` (readme title patterns) construct dates. | The title promise needs only the retained title proof. |

## Outside-in proof ownership

New tests mount `NoteTextContent` (or `NoteShow` where the page matters)
with production CSS, real in-process collaborators, router, and mocked API
only. Visibility means the element is rendered and not hidden, observed with
the browser's layout (`checkVisibility`/client rectangles), not DOM text.

| Source promise | Owner | Observable proof |
| --- | --- | --- |
| Collapsed rows stay mounted; body edit keeps frontmatter; hosts without an owner stay revealed | 1 | New `NoteTextContent.detailsDisclosure.spec.ts` first case: with the section hidden, a body edit emits Markdown that still contains the authored frontmatter. Existing `RichMarkdownEditor.frontmatter.spec.ts` stays green for the readme/direct host. |
| Compact line on opening (keys in order, no-properties wording, updated date); reveal shows rows, Add property, read-only display; collapse restores; no save on toggle | 2 | Same spec: writable and read-only notes, keys visible and rows not visible on mount; after reveal rows visible; after collapse hidden; the content-save spy was not called. Toggle has `aria-expanded` and works by keyboard. A `NoteTextContent.detailsDisclosure.touch.spec.ts` case observes the control at least 44px high under the coarse-pointer instance. |
| Property location reveals and focuses the row; stale key shows not-found without revealing | 2 | Existing `RichMarkdownEditor.propertyLocation.spec.ts` and `RecallPage.justReview.spec.ts` extended to assert the focused row is visible; new case: stale key → notice visible, rows hidden. E2E `note_property.feature` keeps passing through the route reveal. |
| Existing property journeys from a note location | 2 | Reconciled page objects; `note_property_layout.feature`, `note_edit.feature`, `property_wiki_link.feature`, `reify_property.feature`, `property_memory_tracker.feature`, `note_frontmatter_image.feature` run in hosted CI after publication; locally the reconciled unit consumers (`NoteContextReader.spec.ts`, `NoteShowPage.imageUpload.spec.ts`). |
| Unambiguous dates; created only when revealed; freshness ordering theme-aware | 3 | Spec cases: updated date text contains the spelled month and four-digit year and no `/`; created date absent while compact and present when revealed; two notes (hour-old, year-old) yield a strictly more prominent updated line for the recent one, observed through a numeric freshness attribute or computed opacity, in light and dark theme. E2E `noteRecentUpdate.ts` reconciled to that observation; `NoteShow.spec.ts` RGB cases deleted. |
| Draft and validation survive collapse; compact cue while unfinished; insert-as-property reveals | 4 | Spec cases: open draft with key only → Add → message; collapse → cue text visible; reveal → draft value and message intact. Inserting a wiki link as a property on a compact note reveals and shows its row. |
| Title exactly as authored, inline title/body editing unchanged | 2 | Spec case with `2026-05-15 Trip notes`: heading text equals the title; existing `NoteTextContent.titleEdit.spec.ts` and `NoteEditableContent.spec.ts` stay green. |
| Hierarchy, phone margins, line spacing, wide measure, no sideways scroll, muted theme-aware details | 5 | Spec cases at 375px and 1440px containers: body line box width bounded (≤ the chosen measure) at 1440px; surface `scrollWidth` ≤ `clientWidth` at 375px; details line uses muted token. Document the disclosure in `docs/note-content-saving.md`. |

One-time removal reading (slice 5): search product, tests, docs, and agent
guidance for `NoteRecentUpdateIndicator`, `note-recent-update-indicator`,
`toLocalDateString`, and the bottom `Created:`/`Last updated:` block; delete
exclusive dependents, keep shared uses, record the searched names and
classified results in this plan. No absence tests.

## Ordered slices

### 1. Property rows hide under a shared details state while staying mounted
Type: Structure
Status: planned
Proof: Slice 1 rows in the proof table; rich-editor suite below and frontend typechecks.

Internal change: `NoteTextContent` provides a details-disclosure state
(revealed by default in this slice); `RichFrontmatterProperties` injects it
and hides its heading, rows, draft, and Add property when collapsed while
keeping them mounted. The not-found notice stays outside the hidden region.
Hosts without a provider behave as today. External behavior is unchanged.
Enables slice 2 (compact by default).

Size hypothesis: 4–5 minutes including the discriminating body-edit proof.

### 2. Open a note on its compact details line and reveal the properties
Type: Behavior
Status: planned
Proof: Slice 2 rows in the proof table; rich-editor, notes, and pages suites below; reconciled page objects.

Behavior: A reader opens a writable or read-only note → the title, then one
quiet line with the property keys in authored order (or the no-properties
wording), then the body; rows are hidden. Activating the line by pointer,
touch, or keyboard reveals the existing section; activating again collapses
it; neither issues a save. Visiting a property location reveals the section
and focuses that row; a stale key shows its not-found notice without
revealing. The authored title is shown exactly.

Switch the default to compact, add the compact line and control, reveal on a
focused property key, reconcile `NoteContextReader.spec.ts` and
`NoteShowPage.imageUpload.spec.ts`, and add the end-to-end reveal helper to
the page objects that reach rows from a note location. Safe stop: reading is
compact; dates still sit below the body until slice 3.

Size hypothesis: 6–8 minutes. Above target because the route reveal must land
with the default change to keep `note_property.feature` green; splitting it
would deliver a CI-breaking state. Reassess at 10 minutes.

### 3. Read quiet, unambiguous dates in the details
Type: Behavior
Status: planned
Proof: Slice 3 rows in the proof table; notes suite below; reconciled `noteRecentUpdate.ts`.

Behavior: The compact line ends with the updated date in a spelled-month,
four-digit-year form; revealing shows the created and updated dates after the
rows; a recently updated note's line is more prominent than an old note's in
both themes. The bottom created/updated block is gone.

Move the dates into the disclosure, delete `NoteRecentUpdateIndicator` and
the `NoteShow` block, express freshness with theme tokens and a numeric
attribute the end-to-end page object can compare, replace the RGB unit cases.
Safe stop: dates and freshness are delivered.

Size hypothesis: 4–6 minutes.

### 4. Keep an unfinished property edit discoverable across the disclosure
Type: Behavior
Status: planned
Proof: Slice 4 rows in the proof table; rich-editor and notes suites below.

Behavior: With a draft or validation message in the section, collapsing shows
an unfinished-edit cue on the compact line; revealing shows the draft and
message intact. Inserting a wiki link as a property from the note tools on a
compact note reveals the section with the new row.

The section reports its unfinished state through the same provided object
and requests reveal on insertion. Safe stop: editing feedback is complete.

Size hypothesis: 4–5 minutes.

### 5. Read the note in a quiet, bounded column
Type: Behavior
Status: planned
Proof: Slice 5 rows in the proof table; notes suite below; documentation and the removal reading.

Behavior: At phone width the surface has comfortable side margins, roughly
17px body text with 1.5–1.6 line spacing, and no sideways scroll; in a wide
window body lines stay within a readable measure shared by title, details
line, and body; details use muted theme-aware contrast.

Apply the measure and spacing on the note reading surface only, document the
disclosure beside the property panel journeys, and record the one-time
removal reading. Safe stop: completes the source's outcome.

Size hypothesis: 4–6 minutes.

## Verification and delivery

Focused browser command (add the new spec under the notes prefix):

```sh
env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes tests/components/form/RichMarkdownEditor tests/pages/NoteShowPage.imageUpload.spec.ts tests/pages/RecallPage.justReview.spec.ts
env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit
```

`tests/notes` covers the note surface, title editing, content saving, and
the context reader; the editor prefix covers draft, readme, image, relation,
dialog, and property-location consumers. Both typechecks are required by
`.agents/skills/frontend/SKILL.md`. No backend or API change is planned.

End-to-end features are not a local gate; hosted CI runs them after
publication, and the slices that change selectors or page objects (2 and 3)
inspect their consumers under executable-proof rules before delivery. Do not
infer real device usability from headless Chromium; manual testing is not
requested.

Execution follows `AGENTS.md`: Jidoka, fresh dough-post-change-refactor agent,
coordinator `./scripts/run.sh pnpm format:changed` once, plan update, agent
commit with the check-only hook, and push under `dough-execute-plan`. Do not
implement or execute this sequence in preparation.

## Current decisions and review

One disclosure owner on the note reading surface, one property section that
stays mounted, dates from the note topology, no new route, API, storage, or
metadata. Trade-off defaults from the source stand: compact on every opening,
freshness retained in theme-aware form, details under the title.

The 45–60 minute story hypothesis includes refactoring, typechecking, and
delivery. Every leaf was reviewed for independent outcomes, proof ownership,
and cumulative design: slice 1 is the only Structure leaf and sits directly
before the Behavior it enables. No slice-plan refinement is needed; no
remaining blocking concern was identified.
