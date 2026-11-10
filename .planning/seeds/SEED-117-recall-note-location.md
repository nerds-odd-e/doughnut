---
id: SEED-117
status: active
planted: 2026-11-10
scope: story
---

# Find the note and property after recall

<a id="find-the-note-and-property-after-recall"></a>

### Find the note and property after recall

**Identity:** SEED-117#find-the-note-and-property-after-recall
```json dough-story-dependencies
{"schemaVersion":1,"identity":"SEED-117#find-the-note-and-property-after-recall","dependencies":[{"supplier":{"identity":"SEED-119#highlight-and-link-to-a-specific-property-item","href":"seeds/SEED-119-highlight-specific-property-item.md#highlight-and-link-to-a-specific-property-item"},"implementation":"Item-level property focus and direct note URLs for list items.","rationale":"The reviewed property link needs a note destination that can resolve and highlight the exact reviewed list item.","condition":"An item-level note URL opens the note and highlights the list item it identifies.","state":"satisfied","resolution":{"revision":"34be5d98666c88c89d0d7e17040270251e97876c","path":".planning/seeds/SEED-119-highlight-specific-property-item.md#highlight-and-link-to-a-specific-property-item","summary":"The delivered named notePropertyItem destination opens the note, expands the property and highlights the exact raw value/occurrence. Route and NoteShowPage tests plus the actual inbound second-duplicate and browser-reload journey prove this prerequisite; recall-result links and presentation remain this consumer story's own scope.\nCondition satisfied: An item-level note URL opens the note and highlights the list item it identifies.\nAccepted integration: 2a868b7dc1d47f6da5400793670ea7f1bf07ac8e on origin/refs/heads/main."}}]}
```
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/090-find-the-note-and-property-after-recall/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"3f355df58a5331d6989771b3aa90f3ea5d20b2124f89e5a85c5e5e82a49c4ce1","plan":"b7ccc2d5d7c6c2643b4e366ef04cd156f7ae4a81c6e7bdd88cb85797047d3d9b"}}
```

**Goal.** Learners reviewing a recall result can tell where the reviewed note
lives and open the note, the reviewed property, or the exact list item they
reviewed.

**Problem.** The breadcrumb can clip against the left edge, and the compact
view omits the notebook name. The focused property appears as plain text,
including wiki-link syntax for linked values. The note title can already open
a focused property, but it gives learners no distinct link for that destination.

**Scope.** Use the selected **Full path** design in multiple-choice and spelling
results, memory tracker details, and recall-question conversations. Show the
notebook, ancestor folders from root to parent, and note title as one readable
breadcrumb. Each segment opens its own destination. The trail wraps inside the
available width, including long individual names, instead of requiring a
horizontal scroll to discover the location. A root-level note still shows its
notebook and title.

The note-title link opens the full note. A separate **Reviewed property** link
opens that note at the authored property key. When recall identifies a list
value, this property link uses the delivered item destination with the exact
reviewed raw value and first matching occurrence; property-only recall uses
property-level focus. Equal list values share learning identity, so recall
does not claim to distinguish which equal row was reviewed.

Render a reviewed plain value as readable text. Render a resolved wiki value
with its ordinary display label and destination, using the source note's
viewer-scoped resolution. This value link opens the referenced note/property;
the Reviewed property link opens the containing note's reviewed value. Preserve
the authored value for item addressing even when its display label differs.
Unresolved references follow the existing visible unresolved-link behavior;
they must not invent a destination (ADRs 0004 and 0005).

Keep the read-only note preview in spelling results, including its existing
content and interactions. The full-path header remains available above it.
Other contexts retain their existing result, tracker and conversation actions.
Existing note/item destinations own highlighting, expansion and explicit
unresolved feedback when the current content no longer contains the property
or value; this story consumes that behavior.

**Scope limits.** No full-note preview is promised for the other three
contexts. Editing in the result, recall scheduling, question generation,
Portable item-link syntax, historical note snapshots and durable list-row
identity are deferred. These limits add no rejection of naturally supported
values or destinations. Existing notebook/note read authorization continues to
govern the linked pages and reference resolution.

**Key examples.**

- In a narrow recall conversation, a deep path wraps so the learner can read
  and follow the notebook, folders, and note title. The same rule applies to
  a long individual name and to a notebook-root note.
- When a question reviewed `[[run]]` in an `example of` list that also
  contains `[[walk]]`, the note title opens the full note and `example of`
  opens the note with `[[run]]` highlighted. The displayed value is a `run`
  link to the referenced note.
- When the reviewed value is `[[Verbs/run|running]]`, display `running` as
  the reference link, while the property link still addresses the exact raw
  `[[Verbs/run|running]]` item in the containing note. A plain `run` value is
  readable text. A saved unresolved wiki value remains visibly unresolved.
- A whole-note spelling result shows the notebook path and note preview.
  A property tracker without a tracked list item opens the authored property.
  Switching to another result updates its path and destinations together.
- A reviewed value is edited or removed before its link is followed → the
  shared item destination shows the readable note/property and unresolved-item
  feedback, rather than selecting a different value. This reuses the supplier's
  destination contract.
- Recall identifies `[[run]]` in `["[[run]]", "[[run]]", "[[walk]]"]` →
  follow the property link → the first `[[run]]` is highlighted. Reordering
  distinct values still addresses the same reviewed raw value.

**UI reference.** Open the [interactive mockup](../notes/note-under-question.html)
in a browser. **Full path**, the first design, is the approved option. Use
**Preview in** to switch contexts and **Long folder path** to see a deep trail.

**Exploration, borrowing and challenge.** The approved design remains the
recommendation; exploration does not reopen that decision. Three mechanisms
were compared against locating the source and reaching the reviewed material:

| Candidate | Learner effect and system consequence | Assessment |
| --- | --- | --- |
| Existing title link followed by manual inspection | Smallest change, but the title conflates whole-note and property destinations, and manual inspection cannot reliably identify the reviewed list value. | Insufficient for the recorded goal. |
| Full path with separate source and reference destinations | The learner can navigate notebook/folders/note, return directly to the reviewed property/item, or follow the value's reference. Reuses named routes and viewer-scoped reference resolution; adds no learning identity. | Recommended; follows the approved design. |
| Full read-only note context everywhere | Shows the source in place, but puts complete note content into every result and tracker/conversation view, with larger loading and presentation responsibilities. | Useful alternate mechanism, but a broader presentation promise than this increment. Keep the existing spelling preview. |

The second candidate borrows a compiler diagnostic's source locator: the file
path corresponds to notebook/folders/note, the source span to property/item,
and following a referenced symbol to opening the linked value's destination.
The correspondence separates **where this came from** from **what it refers
to**. Its limit matters: recall stores text identity, not a unique source span
for each equal list occurrence. The analogy supplies no missing row identity.

The strongest consequential doubt is that an implementation can satisfy
"highlight the exact item" for distinct values while silently claiming an
occurrence that recall never recorded. Terry accepted first-match navigation
on 2026-11-10, bounding exactness to the reviewed value rather than an identity
for each equal row. The separate property and value links otherwise achieve
the recorded purpose without changing the approved design.

**Architecture and reuse.** The existing note route family already supplies
whole-note, authored-property and exact-value/occurrence destinations. Reuse
those named-location helpers and their resolved/unresolved focus behavior.
Resolve wiki values in the source note's context through the existing
viewer-scoped note read/reference owners; a display label is not a destination.
The existing read-only note context supplies notebook metadata and wiki-link
resolution absent from the compact recalled-note payload. Reuse that read
boundary rather than introducing another Portable-path resolver or selection
store. The choice of loading/composition remains implementation work; account
for its effect on every enclosing result and spelling preview.

[ADR 0001 — Ubiquitous language](../../docs/adrs/0001-ubiquitous-language.md)
defines notebook/folder/property and property learning by key/value.
[ADR 0004 — OKF-compatible notebook Markdown](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
keeps authored wiki destinations and display labels distinct and resolves
shorthand in the source notebook.
[ADR 0005 — Web routes](../../docs/adrs/0005-web-routes-accepted.md)
requires named locations, separates Portable paths from server note IDs, and
keeps unresolved destinations explicit. No conflict or ADR exception was found.
No new North Star direction, schema or learning-identity change is proposed.

**Investigation evidence.** Readings and headless Chromium observations in this
preparation workspace at `c2958ecb8e5ab5dd879ee339242649fdc03a7328`,
2026-11-10, Asia/Singapore:

- `NoteUnderQuestion.vue` passes the focused key into the title link and its
  default slot replaces the breadcrumb in spelling. `BasicBreadcrumb.vue`
  supplies folder segments but no notebook name. `FocusedPropertyIndicator.vue`
  interpolates the raw value as text. The four contexts converge on this shared
  component; recall conversations use `AnsweredQuestionComponent`.
- `RecalledNote.from`, `MemoryTracker.propertyFocus` and `PropertyFocus` carry
  the exact list value, but no occurrence. Scalar focus becomes a key without
  a list value. `NoteRealmService` supplies viewer-scoped wiki links and the
  note's notebook context; the compact recalled-note payload contains neither
  the notebook name nor those resolved links.
- A disposable mounted `NoteUnderQuestion` spec used production CSS, a 280px
  host, three long folder names and a long title. The containment assertion
  failed: card `[0, 280]`, breadcrumb `[10, 848.578125]`. This reproduces an
  over-wide trail, not the original conversation's precise left-clipping
  geometry. The scratch spec was deleted; no product change was retained.
- Command: `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/seed117LayoutProbe.spec.ts tests/components/recall/NoteUnderQuestion.spec.ts tests/pages/NoteShowPage.propertyItem.spec.ts tests/routes/notePropertyItem.spec.ts`.
  At 17:39, the diagnostic layout case failed as above; the three existing
  files passed 13 tests. The note-page test loaded an inbound `run` item href
  and observed the selected visible row and body. The route tests cover encoded
  values and occurrences. This is baseline/supplier evidence, not proof of the
  proposed recall links or their future enclosing layouts.
- The satisfied prerequisite's removed seed is available at revision
  `34be5d98666c88c89d0d7e17040270251e97876c`; its contract addresses exact raw
  text plus a one-based equal-value occurrence, with explicit stale-item
  feedback. Its recorded acceptance is retained in the dependency agreement
  above; no missing live seed was invented or restored.
- Browser access to the interactive mockup was attempted but the browser host
  was unavailable. The Full path source was inspected, including its notebook,
  folder and note links, separate Reviewed property/value links and spelling
  excerpt. No visual mockup observation is claimed.

**Decision — duplicate values.** On 2026-11-10 Terry accepted the recommended
first equal occurrence, matching delivered assimilation and shared key/value
learning identity. Occurrence-specific recall identity remains deferred. No
human-owned decision remains open for this preparation.
