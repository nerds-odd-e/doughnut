# Note details preparation evidence

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

