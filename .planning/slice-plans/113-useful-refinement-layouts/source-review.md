# Source-only review

Fresh general-purpose reviewer `95d1fab0-c1ac-44dd-b6b1-89ffc6552e84`, with no conversation inheritance and no project files. The reviewer was told to use only the input below, change nothing, and number independently observable promises.

Input was the Goal, Scope, and Key examples of `SEED-073#generate-useful-refinement-layouts`, and nothing else:

**Goal.** A learner opening **Refine note** can select the parts of a structured note they want to work on. If generation fails, they can see what happened and retry from the same view.

**Scope.** Generate concise, selectable descriptions that follow meaningful source sections in order. For a dictionary note, keep numbered senses as top-level items, use the note's languages, and add children for distinct usages. Supporting examples and grammatical labels stay with the source sense. Use the existing question-aware selection when refinement starts from recall. Give generation enough output budget to complete and carry a useful failure reason to the view when it cannot. Extraction and removal edit content only after the learner chooses the corresponding action.

**Key examples.**

- A Japanese–Chinese dictionary note has five numbered senses, bilingual examples, and decorative markers. Opening refinement shows five concise sense labels in source order. Distinct usages within a sense can be selected individually.
- A learner opens refinement from a recall question. The layout identifies and preselects the relevant content.
- OpenAI runs out of output tokens. The view shows that generation could not finish within its limit, with the existing retry action. A successful retry replaces the failure message with the selectable layout. Cancelling an in-flight request still leaves the view available for a fresh attempt.

## Unnumbered context

The reviewer kept these as context and did not number them as promises:

- "Give generation enough output budget to complete"
- "decorative markers"

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| C1 | A learner who opens Refine note can select the parts of a structured note they want to work on, when a refinement layout is shown. Source: "A learner opening **Refine note** can select the parts of a structured note they want to work on." "Generate concise, selectable descriptions that follow meaningful source sections in order." | slice 3 |  |
| C2 | When generation fails or cannot complete, the same Refine note view shows a useful reason for what happened. Source: "If generation fails, they can see what happened and retry from the same view." "Give generation enough output budget to complete and carry a useful failure reason to the view when it cannot." | slice 4 |  |
| C3 | When generation fails, the learner can retry from that same view, which offers the existing retry action. Source: "If generation fails, they can see what happened and retry from the same view." "The view shows that generation could not finish within its limit, with the existing retry action." | slice 4 |  |
| C4 | The generated items are concise descriptions. Conditions: a refinement layout is generated for a structured note. Journey: a Japanese–Chinese dictionary note with five numbered senses is opened in refinement. Source: "Generate concise, selectable descriptions that follow meaningful source sections in order." "Opening refinement shows five concise sense labels in source order." | slice 2 |  |
| C5 | The descriptions follow the note's meaningful source sections. Conditions: a refinement layout is generated for a structured note. Journey: the note may also contain bilingual examples and decorative markers. Source: "Generate concise, selectable descriptions that follow meaningful source sections in order." "A Japanese–Chinese dictionary note has five numbered senses, bilingual examples, and decorative markers. Opening refinement shows five concise sense labels in source order." | slice 2 |  |
| C6 | The descriptions appear in source order. Conditions: a refinement layout is generated for a structured note. Journey: opening refinement on the five-sense dictionary note. Source: "Generate concise, selectable descriptions that follow meaningful source sections in order." "Opening refinement shows five concise sense labels in source order." | slice 2 |  |
| C7 | For a dictionary note, each numbered sense is a top-level item. Conditions: the learner opens refinement on a dictionary note. Journey: a note with five numbered senses shows five sense labels. Source: "For a dictionary note, keep numbered senses as top-level items, use the note's languages, and add children for distinct usages." "A Japanese–Chinese dictionary note has five numbered senses, bilingual examples, and decorative markers. Opening refinement shows five concise sense labels in source order." | slice 2 |  |
| C8 | For a dictionary note, the layout uses that note's languages. Conditions: a refinement layout is generated for a dictionary note. Source: "For a dictionary note, keep numbered senses as top-level items, use the note's languages, and add children for distinct usages." | slice 2 |  |
| C9 | For a dictionary note, distinct usages appear as children of their sense. Conditions: a dictionary note has distinct usages within a sense, and a refinement layout is generated. Source: "For a dictionary note, keep numbered senses as top-level items, use the note's languages, and add children for distinct usages." "Distinct usages within a sense can be selected individually." | slice 2 |  |
| C10 | Distinct usages within a sense can be selected individually. Conditions: a dictionary note has distinct usages within a sense, and the learner is viewing its refinement layout. Source: "Distinct usages within a sense can be selected individually." | slice 3 |  |
| C11 | Supporting examples stay with their source sense. Conditions: a dictionary note's sense has supporting examples, including bilingual examples, and a refinement layout is generated. Source: "Supporting examples and grammatical labels stay with the source sense." "A Japanese–Chinese dictionary note has five numbered senses, bilingual examples, and decorative markers." | slice 2 |  |
| C12 | Grammatical labels stay with their source sense. Conditions: a dictionary note's sense has grammatical labels, and a refinement layout is generated. Source: "Supporting examples and grammatical labels stay with the source sense." | slice 2 |  |
| C13 | When refinement starts from a recall question, the layout identifies the relevant content. Source: "Use the existing question-aware selection when refinement starts from recall." "A learner opens refinement from a recall question. The layout identifies and preselects the relevant content." | slice 2 |  |
| C14 | When refinement starts from a recall question, the layout preselects the relevant content. Source: "Use the existing question-aware selection when refinement starts from recall." "A learner opens refinement from a recall question. The layout identifies and preselects the relevant content." | slice 3 |  |
| C15 | When OpenAI runs out of output tokens, the view shows that generation could not finish within its limit. Source: "OpenAI runs out of output tokens. The view shows that generation could not finish within its limit, with the existing retry action." | slice 4 |  |
| C16 | A successful retry replaces the failure message with the selectable layout. Source: "A successful retry replaces the failure message with the selectable layout." | slice 4 |  |
| C17 | Cancelling an in-flight generation request leaves the view available for a fresh attempt. Source: "Cancelling an in-flight request still leaves the view available for a fresh attempt." | slice 4 |  |
| C18 | Extraction edits content only after the learner chooses extraction. Source: "Extraction and removal edit content only after the learner chooses the corresponding action." | slice 4 |  |
| C19 | Removal edits content only after the learner chooses removal. Source: "Extraction and removal edit content only after the learner chooses the corresponding action." | slice 4 |  |
