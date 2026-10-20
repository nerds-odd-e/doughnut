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

## Existing solutions and preparation

Change the existing note reading surface and property section, with one shared
disclosure owner and mounted hidden rows. The dated source investigations,
consumer traces, and relevant Accepted ADRs are retained in
[preparation evidence](PREPARATION.md).

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
Status: done
Proof: Slice 1 rows in the proof table; rich-editor suite below and frontend typechecks.

Accepted: `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test
tests/notes/NoteTextContent.detailsDisclosure.spec.ts
tests/components/form/RichMarkdownEditor.frontmatter.spec.ts
tests/notes/NoteTextContent tests/notes/NoteContextReader.spec.ts
tests/notes/NoteShow.spec.ts` passed 7 files / 50 tests. Both required
frontend typechecks passed. The new real-surface test collapses the provided
state, edits Quill, and observes exact frontmatter plus body in the save.
Providerless editor tests retain revealed rows and composition. Independent
refactoring found no edits; coordinator selective formatting passed. Slice 2
replaces the temporary internal-state setup with the disclosure control.

Internal change: `NoteTextContent` provides a details-disclosure state
(revealed by default in this slice); `RichFrontmatterProperties` injects it
and hides its heading, rows, draft, and Add property when collapsed while
keeping them mounted. The not-found notice stays outside the hidden region.
Hosts without a provider behave as today. External behavior is unchanged.
Enables slice 2 (compact by default).

Size hypothesis: 4–5 minutes including the discriminating body-edit proof.

### 2. Open a note on its compact details line and reveal the properties
Type: Behavior
Status: done
Proof: Slice 2 rows in the proof table; rich-editor, notes, and pages suites below; reconciled page objects.

Accepted: the focused browser command below passed 103 files / 596 tests,
including the real coarse-pointer 44px observation, both typechecks passed,
and local Cypress passed 54 scenarios across the seven property, layout,
wiki-link, editing, reification, memory-tracker, and image features named in
preparation. The same public control now drives the hidden-body-save test.
Valid route arrivals, stale-key notice, collapse on a property route, and
same-key arrival after another note's reset are observed at the note surface.
Independent refactoring moved repeated test helpers without changing their
actions/assertions; accepted proof remains valid. Selective formatting passed.
Budget exception: one isolated E2E startup plus 2:56 of consumer journeys
extends verification beyond the 10-minute implementation checkpoint.

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

Slices that change selectors or page objects (2 and 3) trace their consumers
and obtain bounded local observations for affected journeys before delivery,
as required by execution proof acceptance; hosted CI also runs the full suite.
This execution correction replaces the preparation's CI-only assumption for
known affected consumers. Do not
infer real device usability from headless Chromium; manual testing is not
requested.

Execution follows `AGENTS.md`: Jidoka, fresh dough-post-change-refactor agent,
coordinator `./scripts/run.sh pnpm format:changed` once, plan update, agent
commit with the check-only hook, and push under `dough-execute-plan`. Do not
implement or execute this sequence in preparation.

## Current decisions and review

Execution 2026-10-20: established claim `cb6eb20ba6b29f0d096cdd0c27c17a31aec456a7`
on `origin/main`; publisher `dashboard-territory.local-doughnut`, agent
Kaoru-chan. Story Branch Mode uses the owned checkout
`/Users/terryyin/git/doughnut/.worktrees/focus-on-note-content-while-revealing-details-wh`
and branch `codex/focus-on-note-content-while-revealing-details-wh`; increments
publish to that origin branch. Starting revision:
`8542a6145aefa952f23200b90c97a544e3664e79`. Setup through
`./scripts/run.sh bash scripts/worktree_setup.sh` succeeded; the title-edit
browser suite passed 8 tests. Existing planning authority permits replanning.

CI observer: GitHub `nerds-odd-e/doughnut`, verified `ci.yml` (`donut CI`),
execution branch above; coordinator `kaoru-story5`, directory
`/tmp/dough-ci-501/watch-Sq1fN8`, PID `16516`, session `79406`, yielded cell `9`.
The startup claim on trunk is unobserved by this branch observer. Retain this
observer across slice delivery.

One disclosure owner on the note reading surface, one property section that
stays mounted, dates from the note topology, no new route, API, storage, or
metadata. Trade-off defaults from the source stand: compact on every opening,
freshness retained in theme-aware form, details under the title.

The 45–60 minute story hypothesis includes refactoring, typechecking, and
delivery. Every leaf was reviewed for independent outcomes, proof ownership,
and cumulative design: slice 1 is the only Structure leaf and sits directly
before the Behavior it enables. No slice-plan refinement is needed; no
remaining blocking concern was identified.
