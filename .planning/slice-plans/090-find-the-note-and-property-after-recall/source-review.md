# Source-only review

Reviewer launch: collaboration source_review, fresh context (`fork_turns: none`), 2026-11-10. Input supplied only selected source Goal, Scope, examples, exclusions and Terry's first-occurrence amendment; no plan, implementation or proof. Reviewer read no files and changed nothing. Commitments below retain returned individual ids, qualifications and journeys. Actual launch and full return are in this execution conversation.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| C01 | Learners can tell where the reviewed note lives. | slice 1 | observed: frontend/tests/components/recall/NoteUnderQuestion.location.spec.ts: four real contexts at 280px, ordered anchors/text-range containment, root path, notebook/folder clicks, spelling body/header ordering; Q passed (slice1-return.md) |
| C02 | Learners can open the reviewed note. | slice 2 | |
| C03 | Learners can open the reviewed property. | slice 2 | |
| C04 | Learners can open the reviewed list item, subject to the equal-value qualification below. | slice 2 | |
| C05 | The selected Full path design appears in multiple-choice results, spelling results, memory tracker details, and recall-question conversations. | slice 1 | observed: frontend/tests/components/recall/NoteUnderQuestion.location.spec.ts: four real contexts at 280px, ordered anchors/text-range containment, root path, notebook/folder clicks, spelling body/header ordering; Q passed (slice1-return.md) |
| C06 | One readable breadcrumb shows notebook, ancestor folders from root to parent, and note title. | slice 1 | observed: frontend/tests/components/recall/NoteUnderQuestion.location.spec.ts: four real contexts at 280px, ordered anchors/text-range containment, root path, notebook/folder clicks, spelling body/header ordering; Q passed (slice1-return.md) |
| C07 | Every breadcrumb segment opens its destination. |  | G1 |
| C08 | The trail wraps within the available width. | slice 1 | observed: frontend/tests/components/recall/NoteUnderQuestion.location.spec.ts: four real contexts at 280px, ordered anchors/text-range containment, root path, notebook/folder clicks, spelling body/header ordering; Q passed (slice1-return.md) |
| C09 | Wrapping accommodates long names. | slice 1 | observed: frontend/tests/components/recall/NoteUnderQuestion.location.spec.ts: four real contexts at 280px, ordered anchors/text-range containment, root path, notebook/folder clicks, spelling body/header ordering; Q passed (slice1-return.md) |
| C10 | The trail needs no horizontal scrolling. | slice 1 | observed: frontend/tests/components/recall/NoteUnderQuestion.location.spec.ts: four real contexts at 280px, ordered anchors/text-range containment, root path, notebook/folder clicks, spelling body/header ordering; Q passed (slice1-return.md) |
| C11 | A root-level note shows notebook and note title. | slice 1 | observed: frontend/tests/components/recall/NoteUnderQuestion.location.spec.ts: four real contexts at 280px, ordered anchors/text-range containment, root path, notebook/folder clicks, spelling body/header ordering; Q passed (slice1-return.md) |
| C12 | The note-title link opens the full note. | slice 2 | |
| C13 | A separate Reviewed property link is available. | slice 2 | |
| C14 | Reviewed property addresses the authored key. | slice 2 | |
| C15 | List recall uses the delivered item destination. | slice 2 | |
| C16 | That destination uses the exact raw reviewed value. | slice 2 | |
| C17 | When equal values occur, the first matching occurrence is selected. | slice 2 | |
| C18 | Property-only recall focuses the key. | slice 2 | |
| C19 | A plain value renders as readable text. | slice 3 | |
| C20 | A resolved wiki value uses its ordinary display label. | slice 3 | |
| C21 | Wiki-value resolution uses the source note’s viewer-scoped resolution. | slice 3 | |
| C22 | A resolved value link opens the referenced note or property. | slice 3 | |
| C23 | Reviewed property opens the reviewed value in the containing note. | slice 2 | |
| C24 | Addressing preserves the raw authored value, including alias syntax. | slice 2 | |
| C25 | Unresolved references retain the existing visible unresolved behavior. | slice 3 | |
| C26 | An unresolved reference receives no invented destination. | slice 3 | |
| C27 | The existing spelling preview remains read-only. | slice 1 | observed: frontend/tests/notes/NoteContextReader.spec.ts: read-only content, images, inbound references and usable wiki links; enclosing RecallPage.spelling.spec.ts real preview content; Q 155 tests passed (slice1-return.md) |
| C28 | Its existing content is retained. | slice 1 | observed: frontend/tests/notes/NoteContextReader.spec.ts: read-only content, images, inbound references and usable wiki links; enclosing RecallPage.spelling.spec.ts real preview content; Q 155 tests passed (slice1-return.md) |
| C29 | Its existing interactions are retained. | slice 1 | observed: frontend/tests/notes/NoteContextReader.spec.ts: read-only content, images, inbound references and usable wiki links; enclosing RecallPage.spelling.spec.ts real preview content; Q 155 tests passed (slice1-return.md) |
| C30 | The header remains above the spelling preview. | slice 1 | observed: frontend/tests/components/recall/NoteUnderQuestion.location.spec.ts: four real contexts at 280px, ordered anchors/text-range containment, root path, notebook/folder clicks, spelling body/header ordering; Q passed (slice1-return.md) |
| C31 | Other contexts retain their result, tracker, and conversation actions. | slice 1 | observed: slice1-return.md: Q selected AnsweredQuestion, AnsweredSpellingQuestion, ConversationComponent and MemoryTrackerPage/View existing action/history/refinement/exclusion observers; 35 files155 tests passed |
| C32 | Existing destination behavior supplies highlighting. | slice 2 | |
| C33 | Existing destination behavior supplies expansion. | slice 2 | |
| C34 | Existing destination behavior supplies explicit unresolved feedback. | slice 2 | |
| C35 | Existing read authorization remains. | slice 3 | |
| C36 | Switching results updates the path and its destinations together. | slice 2 | |
| Q01 | Equal list values share learning identity; recall does not distinguish equal rows. | slice 2 | |
| Q02 | Exactness therefore means matching the raw reviewed value and choosing its first occurrence, rather than preserving an individual equal row. | slice 2 | |
| Q03 | Referenced destinations may be notes or properties; neither alternative is restricted to a particular example. | slice 3 | |
| Q04 | The supplied examples do not define an exclusive set of supported values or destinations. | slice 3 | |
| Q05 | Terry accepted first-equal-occurrence behavior on 2026-11-10. Occurrence-specific recall identity remains deferred. | slice 2 | |
| E01 | A deep path in a narrow recall conversation remains readable, wraps, and supports navigation. | slice 1 | observed: frontend/tests/components/recall/NoteUnderQuestion.location.spec.ts: four real contexts at 280px, ordered anchors/text-range containment, root path, notebook/folder clicks, spelling body/header ordering; Q passed (slice1-return.md) |
| E02 | A long path segment remains readable and navigable. | slice 1 | observed: frontend/tests/components/recall/NoteUnderQuestion.location.spec.ts: four real contexts at 280px, ordered anchors/text-range containment, root path, notebook/folder clicks, spelling body/header ordering; Q passed (slice1-return.md) |
| E03 | A root-level note still provides the notebook/title path. | slice 1 | observed: frontend/tests/components/recall/NoteUnderQuestion.location.spec.ts: four real contexts at 280px, ordered anchors/text-range containment, root path, notebook/folder clicks, spelling body/header ordering; Q passed (slice1-return.md) |
| E04 | While reviewing `[[run]]` in an `example of` list also containing `[[walk]]`, the title opens the whole note, `example of` opens the source with `[[run]]` highlighted, and the displayed `run` link opens the reference. | slice 3 | |
| E05 | `[[Verbs/run\|running]]` displays a `running` link while source addressing retains the complete raw value. | slice 3 | |
| E06 | Plain `run` appears as text. | slice 3 | |
| E07 | An unresolved saved wiki reference remains visibly unresolved. | slice 3 | |
| E08 | Whole-note spelling presents both the path and the retained preview. | slice 1 | observed: frontend/tests/components/recall/NoteUnderQuestion.location.spec.ts: four real contexts at 280px, ordered anchors/text-range containment, root path, notebook/folder clicks, spelling body/header ordering; Q passed (slice1-return.md) |
| E09 | A tracker entry without a list item opens the property key. | slice 2 | |
| E10 | An edited or removed reviewed value leads to a readable note/property with unresolved feedback, rather than selecting another value. | slice 2 | |
| E11 | `[[run]], [[run]], [[walk]]` selects the first `[[run]]`. | slice 2 | |
| E12 | Reordering distinct values still finds the reviewed value by raw text. | slice 2 | |
| X01 | Adding full previews to other contexts. | slice 3 | |
| X02 | Editing within recall results. | slice 3 | |
| X03 | Recall scheduling changes. | slice 3 | |
| X04 | Generation changes. | slice 3 | |
| X05 | Portable item-link syntax. | slice 3 | |
| X06 | Historical snapshots. | slice 3 | |
| X07 | Durable row identity. | slice 3 | |
| X08 | Occurrence-specific recall identity, as reaffirmed by the human amendment. | slice 3 | |
