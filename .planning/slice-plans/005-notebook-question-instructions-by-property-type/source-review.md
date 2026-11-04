# Source review: SEED-007#story-3

Launch: a fresh general-purpose reviewer (no conversation inheritance) on
2026-11-04, given only the story's Goal, Scope, and Key examples text verbatim
from the seed section, told to read no project files and change nothing, and to
return numbered individual commitments. Returned ids are kept as returned; the
reviewer flagged a typo in its own E7 notebook name, corrected here to
`日本語学習`. Reviewer note: E3 does not say whether the general notebook
instruction is also present (commitment 12 implies it is).

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| 1 | A notebook author can write one question-generation instruction per property type (e.g. `a part of`) in the notebook readme. | slice 1 | observed: NoteQuestionGenerationRequestTests.InstructionsByPropertyType notebookWithMap readme map entry → propertyOfMatchingTypeCarriesTheEntryAfterTheGeneralInstruction |
| 2 | Donut sends that instruction only when it generates a question about a matching property, or about a relationship note of the equivalent relation, in that notebook. | slice 2 | |
| 3 | Guidance that fits one kind of question uses no context budget on other questions and cannot mislead them. This is the goal's rationale. | slice 2 | |
| 4 | The existing readme `question_generation_instruction` keeps going into every question of the notebook. | slice 1 | observed: NoteQuestionGenerationRequestTests.InstructionsByPropertyType wholeNoteQuestionHasOnlyTheGeneralInstruction / propertyOfOtherType… / noteInNotebookWithoutTheMap… assert GENERAL_INSTRUCTION present |
| 5 | Authoring: the instructions are a nested map under one notebook readme frontmatter key, from property type to instruction text. | slice 1 | observed: NoteQuestionGenerationRequestTests.InstructionsByPropertyType notebookWithMap nested map in readme frontmatter; Frontmatter.getStringMap |
| 5.1 | The key is `question_generation_instructions_by_property`, unless planning finds a naming conflict. | slice 1 | observed: FrontmatterQuestionGenerationInstruction.BY_PROPERTY_KEY = question_generation_instructions_by_property; NoteQuestionGenerationRequestTests.InstructionsByPropertyType notebookWithMap |
| 5.2 | The map sits beside the existing `question_generation_instruction` key in the same frontmatter, as in the YAML sample. | slice 1 | observed: NoteQuestionGenerationRequestTests.InstructionsByPropertyType notebookWithMap: map beside question_generation_instruction in one frontmatter |
| 6 | When this key is present, the web edits the readme's metadata in Markdown mode ("Nested metadata is edited in Markdown."). The owner accepted this. | slice 1 | observed: reading (owner-accepted, unchanged by this story): frontend RichMarkdownEditor nested_metadata Markdown-only path |
| 7 | The readme body stays as it is today. | slice 1 | observed: slice 1 diff changes no readme body handling; instructions read from frontmatter only |
| 8 | The map travels with the readme through Git synchronization like other author YAML. | slice 1 | observed: reading (unchanged by this story): NotebookGitProposalMarkdownFormat.assertValidTypedMarkdown checks only top-level map and type, so nested author YAML passes Git publication |
| 9 | Matching for a property question: the question matches when its property key names a map entry. | slice 1 | observed: NoteQuestionGenerationRequestTests.InstructionsByPropertyType propertyOfMatchingTypeCarriesTheEntryAfterTheGeneralInstruction |
| 9.1 | The numeric suffix is removed before comparing (`a part of 2` → `a part of`). | slice 1 | observed: NoteQuestionGenerationRequestTests.InstructionsByPropertyType propertyOfMatchingType… case 'a part of 2' → 'a part of' entry |
| 9.2 | Case is ignored. | slice 1 | observed: NoteQuestionGenerationRequestTests.InstructionsByPropertyType mapKeyMatchesIgnoringCase (map key 'A Part Of' matches focus 'a part of') |
| 9.3 | This applies to a scalar property. | slice 1 | observed: NoteQuestionGenerationRequestTests.InstructionsByPropertyType propertyOfMatchingType… case PropertyFocus('a part of 2','') |
| 9.4 | This applies to one list item of a list property. | slice 1 | observed: NoteQuestionGenerationRequestTests.InstructionsByPropertyType propertyOfMatchingType… case PropertyFocus('a part of','[[X]]') |
| 10 | Matching for a question focused on a relationship note uses the existing relation-to-property-key rule used to reduce a relationship note to a source property (`relation: a-part-of` → `a part of`). | slice 2 | |
| 10.1 | That rule includes the `an example of` → `example of` family. | slice 2 | |
| 11 | Which notebook's map applies: the notebook of the note being asked about. | slice 2 | |
| 11.1 | For a property question, that is the notebook of the note that owns the property. | slice 1 | observed: NoteRealmService.questionGenerationInstructionBlocks reads focus.getNotebook() readme; NoteQuestionGenerationRequestTests.InstructionsByPropertyType noteInNotebookWithoutTheMapHasOnlyTheGeneralInstruction |
| 11.2 | For a relationship-note question, that is the notebook of the relationship note itself. | slice 2 | |
| 12 | Combination: a matching instruction is added to the existing notebook, folder, and focus-note instructions. | slice 1 | observed: NoteRealmService.questionGenerationInstructionBlocks appends the per-type block to notebook/folder/focus-note blocks; NoteQuestionGenerationRequestTests.InstructionsByPropertyType asserts general + labelled entry together |
| 12.1 | It carries a label naming the notebook and the property type. | slice 1 | observed: NoteQuestionGenerationRequestTests.InstructionsByPropertyType PART_OF_LABEL 'Instruction from notebook "Anatomy" for property "a part of":' |
| 12.2 | Nothing is overridden. | slice 1 | observed: NoteQuestionGenerationRequestTests.InstructionsByPropertyType propertyOfMatchingType… asserts GENERAL_INSTRUCTION still present before the entry; NoteRealmScopedMetadataTest ordering/dedupe 15 pass |
| 12.3 | A blank entry adds nothing, the same as a blank instruction today. | slice 1 | observed: NoteQuestionGenerationRequestTests.InstructionsByPropertyType blankEntryAddsNothing |
| 13 | Required restriction: a non-matching question gets no per-type instruction. | slice 2 | |
| D1 | Deferred: per-type instructions in folder readmes. | | O1 |
| D2 | Deferred: per-type instructions on single notes. | | O1 |
| D3 | Deferred: per-type guidance in the learning-session request, which carries only the notebook instruction today. | | O1 |
| D4 | Deferred: showing a property-focused request through the question-generation export, which is whole-note only. | | O1 |
| D5 | Deferred: property focus in contest evaluation, which gets no property focus today. | | O1 |
| D6 | Deferred: rich-mode editing of the nested map. | | O1 |
| E1 | `日本語学習` has an `a part of` entry. Recall question for a word's `a part of` property → the request carries the `a part of` instruction and the general notebook instruction. | slice 1 | observed: NoteQuestionGenerationRequestTests.InstructionsByPropertyType propertyOfMatchingType… case PropertyFocus('a part of','[[X]]') general + entry |
| E2 | Same setup. Recall question for the word's `a part of 2` property → the request carries the `a part of` instruction and the general notebook instruction. | slice 1 | observed: NoteQuestionGenerationRequestTests.InstructionsByPropertyType propertyOfMatchingType… case PropertyFocus('a part of 2','') general + entry |
| E3 | Same notebook. Recall question focused on a relationship note with `relation: a-part-of` → the request carries the `a part of` instruction. | slice 2 | |
| E4 | Same notebook. Whole-note question about the same word → no `a part of` instruction; the general notebook instruction is still included. | slice 1 | observed: NoteQuestionGenerationRequestTests.InstructionsByPropertyType wholeNoteQuestionHasOnlyTheGeneralInstruction |
| E5 | Same notebook. Question about a `similar to` property → no `a part of` instruction; the general notebook instruction is still included. | slice 1 | observed: NoteQuestionGenerationRequestTests.InstructionsByPropertyType propertyOfOtherTypeHasOnlyTheGeneralInstruction |
| E6 | Same notebook. Question focused on a relationship note with `relation: similar-to` → no `a part of` instruction; the general notebook instruction is still included. | slice 2 | |
| E7 | Another notebook with no map entry. Question about an `a part of` property → no `a part of` instruction from `日本語学習`. | slice 1 | observed: NoteQuestionGenerationRequestTests.InstructionsByPropertyType noteInNotebookWithoutTheMapHasOnlyTheGeneralInstruction |
