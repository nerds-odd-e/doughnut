# Question-generation instructions

Authors guide question generation with frontmatter in notebook readmes, folder
readmes, and notes. The request's custom instruction message holds one labelled
block per source, in this order: notebook readme, folders from outermost to
innermost, then the focus note. Instructions add up; none overrides another,
and identical text appears once (the outermost wins). Blank values add nothing.

## General instruction

`question_generation_instruction` (scalar) on a notebook readme, folder readme,
or note applies to every question about notes under it.

## Per-property-type instruction

A notebook readme may hold a one-level map from property type to instruction:

```yaml
---
question_generation_instruction: Ask in Japanese.
question_generation_instructions_by_property:
  a part of: >-
    Ask which group the word belongs to using a sentence, not the reading.
---
```

An entry is added, labelled `Instruction from notebook "<name>" for property
"<type>":`, right after that notebook's general instruction, only when the
question is about a matching property type in that notebook:

- a property question (scalar property or one list item): its key without the
  numeric suffix (`a part of 2` → `a part of`);
- a question focused on a relationship note: the property key its `relation`
  reduces to (`a-part-of` → `a part of`, `an-example-of` → `example of`), the
  same rule used when a relationship note is reduced to a source property.

Property types compare ignoring case. Any other question, including a
whole-note question, gets no per-type entry. Only notebook readmes hold this
map; the web edits a readme holding it in Markdown mode, and the map travels
with the readme through Git synchronization like other author YAML.

Generation, batch generation, and evaluation share this assembly
(`QuestionGenerationRequestBuilder`, `NoteRealmService.questionGenerationInstructionBlocks`).
The learning-session request carries only the notebook's general instruction.
