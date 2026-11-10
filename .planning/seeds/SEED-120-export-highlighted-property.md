---
id: SEED-120
status: active
planted: 2026-11-10
scope: story
---

# Export a note from the highlighted property

<a id="export-a-note-from-the-highlighted-property"></a>

### Export a note from the highlighted property

**Identity:** SEED-120#export-a-note-from-the-highlighted-property
```json dough-story-dependencies
{"schemaVersion":1,"identity":"SEED-120#export-a-note-from-the-highlighted-property","dependencies":[{"supplier":{"identity":"SEED-119#highlight-and-link-to-a-specific-property-item","href":"seeds/SEED-119-highlight-specific-property-item.md#highlight-and-link-to-a-specific-property-item"},"implementation":"Item-level property focus and direct note URLs for list items.","rationale":"A property key alone leaves the export ambiguous for list properties. The export needs the same exact item focus the learner sees on the note.","condition":"A note exposes the exact highlighted list item as its focus when selected directly or opened through an item URL.","state":"satisfied","resolution":{"revision":"34be5d98666c88c89d0d7e17040270251e97876c","path":".planning/seeds/SEED-119-highlight-specific-property-item.md#highlight-and-link-to-a-specific-property-item","summary":"The delivered route-owned focus identifies the exact raw list value and one-based occurrence after direct row selection or an item URL. Mounted selection/location tests and the actual inbound/reload browser journey prove the visible item focus, including the second duplicate; export consumption remains this consumer story's own scope.\nCondition satisfied: A note exposes the exact highlighted list item as its focus when selected directly or opened through an item URL.\nAccepted integration: 2a868b7dc1d47f6da5400793670ea7f1bf07ac8e on origin/refs/heads/main."}}]}
```
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/089-export-a-note-from-the-highlighted-property/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"b95ed59de098c5bf825580b01e388b82341e4e610ee22a9fab234d17c2b312a1","plan":"0361281a7f66c6f4db329e7508f571e0c5cc8f77626fe1e5e16548e8cfb9753d"}}
```

**Goal.** A learner can export a note from the perspective of a highlighted
property or list item, so they can share that context and its quiz questions
with an AI for analysis.

**Scope.** When the learner opens **Export Note Data** with a property or
list item highlighted, build the Markdown using that focus. Share the
property-focus Markdown code already used by question generation.
[FocusContextMarkdownAugmenter](../../backend/src/main/java/com/odde/donut/services/focusContext/FocusContextMarkdownAugmenter.java)

Reuse Export Note Data's **Include recent multiple-choice questions** option.
When selected, include up to 10 distinct recent MCQs attributed through saved
recall-prompt/tracker links to the highlighted property or raw list value,
including unanswered prompts. Match the exact authored property key; an item
also matches its exact raw value. Do not infer attribution from question prose
or `testedFocus`. Filter before selecting the newest ten by MCQ creation time,
then ID, descending. Include choices, Donut's answer key, and only the current
learner's latest own selected answer where available (answer time, then ID).

Identify the exact highlighted occurrence in the Markdown. Equal values share
the existing property/value question history; describe that sharing when
occurrence identity could otherwise mislead. A list property opened without an
item selection focuses the whole property and includes attributable questions
across its values. No highlight keeps whole-note export as usual, including
questions without recall links. With the checkbox cleared, export focused
Markdown without the question appendix. Retain the surrounding focus context
and relevant wiki-link targets, rather than exporting an isolated snippet.

**Key examples.**

- With a scalar property highlighted, the Markdown identifies that property
  as the focus. Checking the option adds only that property's recent MCQs.
- A note has `[[walk]]` and `[[run]]` under `example of`. With `[[run]]`
  highlighted, the Markdown focuses on `[[run]]`. Checking the option adds
  its recent MCQs, even if questions on `[[walk]]` or the whole note are newer.
- Given `example of: ["[[walk]]", "[[run]]", "[[run]]"]` with the second
  `[[run]]` occurrence highlighted → open Export Note Data → identify that
  occurrence, retaining the surrounding context. With questions included,
  equal `[[run]]` values share the same saved history; an unanswered matching
  prompt is eligible and repeated prompts do not repeat its MCQ.
- Given more than ten matching questions and newer questions on other values
  → include questions → export the ten newest matching distinct MCQs, with
  the current learner's latest own selection when present.
- Given no attributable questions for the highlighted focus → include
  questions → export the focused Markdown without unrelated substitutions.
- Given a list property opened by its property-only URL → open export →
  describe the whole property and include attributable questions across its
  values when requested.
- Given a stale property/item selector → open export → show that the focus
  cannot be resolved and provide no success-looking Markdown for another
  subject. Close export, choose an existing focus or clear the highlight,
  and reopen to recover through the existing note controls.

**Investigation — 2026-11-10, revision
`05a4e226df34f06c88fb7f50b78767b4a39ab14a`.** These are source readings in
the established preparation worktree, not runtime observations:

- [RecallPrompt](../../backend/src/main/java/com/odde/donut/entities/RecallPrompt.java)
  holds both an MCQ and a memory tracker. The tracker stores the note, exact
  property key, and raw list value; a scalar uses an empty value. A prompt
  exists before an answer, so unanswered MCQs with these links can be
  attributed. See [RecallPromptService](../../backend/src/main/java/com/odde/donut/services/RecallPromptService.java)
  and [MemoryTracker](../../backend/src/main/java/com/odde/donut/entities/MemoryTracker.java).
- [Mcq](../../backend/src/main/java/com/odde/donut/entities/Mcq.java) has no
  structured property key, item value, or occurrence. Its `testedFocus` comes
  from AI output through
  [GeneratedQuestionPostProcessor](../../backend/src/main/java/com/odde/donut/services/ai/GeneratedQuestionPostProcessor.java);
  it is not a reliable substitute for saved attribution. A question without
  a matching recall link cannot be attributed by that route.
- Learning identity is property key plus value. Equal list values share
  history, as the deletion explanation in
  [usePropertyMemoryTrackerGuard](../../frontend/src/composables/usePropertyMemoryTrackerGuard.ts)
  also states. The visible highlight is more precise:
  [useFocusedNoteProperty](../../frontend/src/composables/useFocusedNoteProperty.ts)
  and [noteShowLocation](../../frontend/src/routes/noteShowLocation.ts) carry
  the raw value and one-based occurrence. The supplier's dependency receipt
  above preserves its delivered evidence; its spent seed has been removed.
- [NoteExportController](../../backend/src/main/java/com/odde/donut/controllers/NoteExportController.java)
  currently selects the newest ten MCQs for the note before rendering the
  appendix. The export form sends no property focus. The existing answer
  lookup selects only the current learner's latest answer, ordered by answer
  time and then answer ID; guests receive no personal selections. MCQ
  content itself is shared. Existing controller test sources cover these
  rules, including unprompted and contested questions in whole-note export.
- Question generation already uses
  [FocusContextMarkdownAugmenter](../../backend/src/main/java/com/odde/donut/services/focusContext/FocusContextMarkdownAugmenter.java)
  to add the property instruction to the surrounding focus context and
  preserve relevant wiki-link targets. It does not replace the context with
  an isolated property snippet. Its current property block has no occurrence
  descriptor, and its empty-value lookup reads scalar frontmatter only.
- A property-only list URL is possible: it opens the property panel without
  selecting a row. Stale keys or item selectors already produce an explicit
  unresolved-focus message on the note. See
  [RichFrontmatterPropertyNotFound](../../frontend/src/components/form/RichFrontmatterPropertyNotFound.vue).

**Alternatives and recommendation.** Compare these against sharing the
highlighted perspective together with relevant quiz evidence:

| Candidate | Learner interaction and system consequence | Assessment |
| --- | --- | --- |
| Keep whole-note export and annotate/filter it manually | Learner must identify the intended property and sort unrelated questions; existing export needs no change | Simpler, but does not deliver the story's focused question selection |
| Carry the current highlight into the existing export | Learner opens the same action and uses the existing checkbox; the server shares the focus augmenter and selects distinct MCQs through saved recall attribution before taking ten | Selected following the developer's answers below; preserves one focus selection and one appendix owner |
| Add a separate export focus picker and durable question-focus metadata | Learner chooses focus again; the system adds selection state and a new attribution lifecycle, with existing unlinked records still requiring a provenance decision | Deferred proposal; additional work is unnecessary for the recommended interpretation |

The borrowed mechanism is a report's selection manifest: the highlighted
subject corresponds to the report selection, surrounding note context supplies
its explanation, and the filtered MCQs form its evidence appendix. The export
should state the subject rather than discard the surrounding context. The
consequential limit is provenance: a manifest cannot recover an occurrence or
an original question focus that the saved evidence never recorded. This is a
design analogy, not runtime evidence.

**Challenge.** The strongest doubt is whether “exact list item” promises an
occurrence-specific quiz history. The UI can distinguish two identical values,
but the learning model cannot. An export could satisfy the highlighted-value
wording while misleading an external AI about whose history the appendix
represents. The selected interpretation preserves the exact highlighted
occurrence in the focus description and identifies the question history as
shared by equal values. The developer accepted this interpretation; an
occurrence-specific learning identity is deferred.

**Scope boundaries.** Retain the existing token-budget control, unchecked
question default, Markdown preview/copy/download, complete appendix outside
the note-body budget, read authorization, and current-learner answer privacy.
The no-highlight path keeps whole-note selection, including MCQs without
recall links. This story does not add a focus picker, generate new
questions, infer historical focus from prose, backfill question metadata, or
change learning identity. These are deferred delivery promises, not guards
against naturally supported behavior. Read authorization and current-learner
answer isolation are required restrictions protecting access and privacy.
Unresolved-focus feedback protects correctness: the exported perspective must
match the subject the learner selected.

**Architecture.** This is an on-demand AI-context report. Under
[ADR 0002 — Git-native Portable notebook tree synchronization](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md),
learning data remains private server-side data, outside the Portable notebook
tree; this story does not change notebook serialization. Consume the existing
route-owned focus under
[ADR 0005 — Web routes](../../docs/adrs/0005-web-routes-accepted.md),
without a second URL grammar or shadow focus state. Reuse the existing
property-focus Markdown owner and appendix renderer. Expected unresolved-focus
feedback has a correctness purpose; other unexpected failures retain the
handling of [ADR 0006 — Failure handling](../../docs/adrs/0006-failure-handling-accepted.md).
These Accepted records agree with the ADR index; no relevant supersession or
conflict was found in the reviewed paths. No ADR change is proposed.

**Decisions — 2026-11-10.** The developer answered “yes. yes.” to the two
questions: preserve the exact highlighted occurrence while sharing quiz
history by property/value (including unanswered recall-linked MCQs, excluding
unattributable MCQs from the focused appendix), and export the whole list
property when no item is selected. No goal or scope decision remains open.

**Runtime investigation.** The preparation's isolated MySQL 8.4.11 probe
confirmed distinct newest-ten selection through unanswered recall links,
whole-property selection across values, and exact key/value comparison. The
headless note/editor/export checks passed; disabling item consumption caused
the item-location tests to fail, and restoring it returned them to green.
Inputs, commands, results, limits, and future proof ownership are in the
[slice plan](../slice-plans/089-export-a-note-from-the-highlighted-property/PLAN.md).
The disposable observation edits were reverted. No execution is authorized
by this preparation.
