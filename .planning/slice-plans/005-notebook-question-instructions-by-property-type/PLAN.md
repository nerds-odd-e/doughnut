# Plan: Notebook question instructions by property type

**Source:** [SEED-007#story-3](../../seeds/SEED-007-per-concept-prompt-splitting-single-note-qg.md#story-3)
**Source review:** [source-review.md](source-review.md)
**Identity:** SEED-007#story-3

## Goal and scope

A notebook readme's `question_generation_instructions_by_property` map gives
one instruction per property type. Donut adds the matching entry to the custom
instruction message only when the question is about a property of that type, or
about a relationship note whose relation is the equivalent type, in the
notebook of the note asked about. Goal, scope, required restriction, deferred
promises, and key examples are in the source story and are not repeated.

**Exclusions (from the story):** per-type maps in folder readmes or note
frontmatter; the learning-session request; the question-generation export;
property focus in contest evaluation; rich-mode editing of the map.

**Assumptions:** the owner accepted that a readme holding the map is edited in
Markdown mode on the web; nothing in the frontend changes.

## Current decisions

- **One property-type rule.** A property type is the property key without its
  numeric suffix (`PropertyKeyNaming.propertyKeyBaseAndSuffix`), trimmed, put
  through `PropertyKeyNaming.canonicalExampleOfFamilyKey`, and compared
  ignoring case. The same rule applies to map keys, to a `PropertyFocus` key,
  and to the property key derived from a relationship note's `relation`
  (`RelationshipNoteComposition.propertyKey`). There is no second matcher for
  relationships.
- **Reading the map.** `Frontmatter` gains a reader for a one-level map of
  scalar strings under a key, looked up ignoring case; non-scalar entries and
  blank values are skipped. `getString` and the property readers stay
  unchanged.
- **Where the block is added.** `NoteRealmService.questionGenerationInstructionBlocks`
  takes the question's property type (null for a whole-note question that is not
  about a relationship note). It adds the notebook's matching entry right after
  the notebook's general instruction, labelled
  `Instruction from notebook "<name>" for property "<type>":`. The existing
  rule that identical text appears once also applies. The token budget
  subtraction in `QuestionGenerationRequestBuilder` already covers the whole
  instruction message, so it needs no change.
- **Which property type a request carries.** In
  `QuestionGenerationRequestBuilder.openAiResponseRequestForQuestionGeneration`:
  the `PropertyFocus` key when present; otherwise the relation-derived key when
  the focus note is a relationship note; otherwise none. Generation, batch
  generation, and evaluation all go through this method, so each follows the
  focus it already receives.
- **Relationship reading is shared.** Slice 2 moves "property key of a
  relationship note" (parse `relation`, `RelationshipNoteComposition.propertyKey`,
  `canonicalExampleOfFamilyKey`) out of `NoteReferenceHandling`'s private
  reduce path into one place that reduce and instruction matching both call.
  Reduce behavior stays the same.
- **Proof command.** In this worktree:
  `CURSOR_DEV=true nix develop -c pnpm backend:test:worktree --tests '<one pattern>'`
  (one `--tests` per run; two flags print usage and fail). Observed 2026-11-04.

## Decisive premises

| Premise | Consuming operation | Observation | Result |
| --- | --- | --- | --- |
| A property question's `PropertyFocus` reaches `openAiResponseRequestForQuestionGeneration` through `NoteQuestionGenerationService.buildQuestionGenerationRequest(note, msg, propertyFocus)`, and recall (`RecallPromptService`) and batch (`QuestionGenerationBatchJsonlRenderer`) pass `MemoryTracker.propertyFocus()`. | Slice 1 derives the property type there. | `QuestionGenerationRequestBuilderTests` run in this worktree (3 tests, 0 failures, 2026-11-04T06:37:10Z) asserts the property-focus block in the request built through that service method; call sites read 2026-11-04. | Settled. |
| Readme `question_generation_instruction` blocks reach the first user message in notebook → folder → note order. | Slice 1 inserts the per-type block after the notebook block. | `NoteQuestionGenerationRequestTests` run in this worktree (9 tests, 0 failures, 2026-11-04T06:36:39Z), including `nestedLabeledQuestionInstructionsAppearInFirstUserMessageInOrder`. | Settled. |
| `Frontmatter.parse` keeps a nested map in its data, which `getString` returns as empty. | Slice 1 adds the map reader. | `Frontmatter.getString` / `holdsUnsupportedValue` read 2026-11-04 (a key holding a map is "unsupported", so the map is retained). | Settled by reading; slice 1's test is the discriminating proof. |
| A relationship note's `relation` is readable from its content, and relation → property key is `RelationshipNoteComposition.propertyKey` plus `canonicalExampleOfFamilyKey`. | Slice 2. | `NoteReferenceHandling.reduceRelationNoteToSourceProperty` and `parseRelationshipFrontmatter` read 2026-11-04; reduce tests `RelationControllerReduceToSourcePropertyTests`, `RelationControllerReduceIntoExistingKeyTests`, `NotebookGitWebRelationReduceControllerTest` exist. | Settled for the rule; slice 2 runs the reduce tests after the move. |
| A readme holding a nested map saves on the web and passes Git publication. | Authoring (story scope, no slice changes it). | `RichMarkdownEditor` `nested_metadata` path and `NotebookGitProposalMarkdownFormat.assertValidTypedMarkdown` (top-level map and non-blank `type` only) read 2026-11-04. | Accepted as today's behavior by reading; not run. Not blocking: no slice depends on it, and the owner accepted Markdown-mode editing. |

## Outside-in proof

| Story example | Owning slice | Proof |
| --- | --- | --- |
| 1. `a part of` / `a part of 2` property question carries the entry and the general instruction | 1 | `NoteQuestionGenerationRequestTests` (new cases) |
| 2. Relationship note `relation: a-part-of` carries the entry | 2 | `NoteQuestionGenerationRequestTests` (new case) |
| 3. Whole-note question, `similar to` property, `relation: similar-to` → no entry, general instruction still there | 1 (whole-note, other property), 2 (other relation) | same class |
| 4. Another notebook without an entry → no entry | 1 | same class |

## Slices

### 1. Property questions carry the notebook's instruction for their property type

Type: Behavior
Status: done
Proof: new cases in `NoteQuestionGenerationRequestTests`, built through
`service.buildQuestionGenerationRequest(note, null, propertyFocus)`. The
notebook readme has a general instruction and an `a part of` map entry:
- a `PropertyFocus("a part of 2", "")` request's first user message has the
  labelled `a part of` entry after the notebook's general instruction;
- a list-item focus `PropertyFocus("a part of", "[[X]]")` has it too;
- a whole-note request and a `PropertyFocus("similar to", "")` request do not
  have it, and still have the general instruction;
- a note in another notebook whose readme has no map does not have it, for the
  same `a part of` focus.

The new cases fail before the change. The existing classes
`NoteQuestionGenerationRequestTests` and `QuestionGenerationRequestBuilderTests`
stay green.

Behavior: notebook readme with `question_generation_instructions_by_property`
→ a question is generated for a property of a note in that notebook → the
custom instruction message includes the entry whose property type matches,
and nothing for non-matching questions.

Accepted proof (2026-11-04): nested `NoteQuestionGenerationRequestTests.InstructionsByPropertyType`
(7 cases, including case-insensitive map key and blank entry) plus
`QuestionGenerationRequestBuilderTests`, `NoteRealmScopedMetadataTest`,
`QuestionGenerationBatchJsonlRendererTest`, all passing. The single rule is
`PropertyKeyNaming.propertyType`; the request carries it from
`QuestionGenerationRequestBuilder.openAiResponseRequestForQuestionGeneration`
(null without a property focus). Slice 2 only adds the relationship-note
fallback at that line, through the same `propertyType`.

### 2. Relationship-note questions carry the instruction for their relation

Type: Behavior
Status: done
Proof: new cases in `NoteQuestionGenerationRequestTests` with relationship
notes (`type: Relationship`, `source`, `target` wiki links) in the same
notebook: `relation: a-part-of` with no property focus → the `a part of` entry
is present; `relation: similar-to` → it is absent; `relation: an-example-of`
with an `example of` map entry → that entry is present. The new cases fail
before the change. The reduce tests
`RelationControllerReduceToSourcePropertyTests`,
`RelationControllerReduceIntoExistingKeyTests`, and
`NotebookGitWebRelationReduceControllerTest` stay green after the shared
relation-to-property-key rule moves.

Behavior: notebook map entry → a question is generated focused on a
relationship note in that notebook → the custom instruction message includes
the entry for the relation's property type.

Accepted proof (2026-11-04): four relationship cases in
`NoteQuestionGenerationRequestTests.InstructionsByPropertyType` (two matching
cases failed first), plus `QuestionGenerationRequestBuilderTests`,
`RelationControllerReduce*`, `NotebookGitWebRelationReduceControllerTest`, and
`QuestionGenerationBatchJsonlRendererTest`, all passing. The shared reader is
`RelationshipNoteComposition.parse` with `Relationship.propertyKey()`; the
builder's `questionPropertyType` picks the focus key, else the relation's key,
through `PropertyKeyNaming.propertyType`.

## Execution complete

Product advice: no change. Both slices delivered the story's promises with
request-level proof; the deferred promises (folder or note maps, the
learning-session request, export, contest evaluation, rich-mode editing) stay
deferred in the story. Revisit them only if authors ask for per-type guidance
outside the notebook readme.

## Story obligations

### O1. Deferred per-type instruction surfaces
Reported: slice 1 — "The source review lists the story's deferred promises D1–D6 as commitments."
Story clause: "Deferred promises (not rejections)"
Disposition: excluded "Deferred promises (not rejections)"
Commitment: D1, D2, D3, D4, D5, D6

## Concern evidence

Slice-plan refinement was not needed: two Behavior slices, each one outcome
with one proof loop at the existing request-assembly entry point, both on the
same property-type rule and no new structure beyond the shared relation
reader that slice 2 needs.

This review found no remaining slice-specific concern.
