---
id: SEED-007
status: dormant
planted: 2026-08-21
planted_during: focus-note retrieval exploration for OKF / LLM Wiki
updated: 2026-10-14
trigger_when: improving question-context quality under the learning and retrieval epic, or when QG quality plateaus
scope: medium
---

# SEED-007: Improve supporting context for single-note recall questions

## Current Direction

On 2026-10-12 the owner requested concrete backlog stories from the OpenKB
implementation assessment and reuse of existing seeds. This seed now owns the
question-context story within the
[learning/refinement and retrieval epic](SEED-073-learning-refinement-and-retrieval.md).
Per-concept splitting, summarization, task-aware candidate selection, and
passage selection are alternatives to evaluate, not chosen implementations.

The assessment read main at `e851a7682d` without running model-quality
comparisons. Question generation already uses shared focus context, property
focus, author instructions, and a focus-note grounding rule. Candidate selection
does not receive the intended task; related-note content is prefix-truncated.
Current code budgets 2,000 approximate combined content tokens, up to 1,500 for
the focus note and 200 per related note. The older research/design descriptions
below describe their dated investigation, not current budget measurements.

## Story Decomposition

Effort bands follow [SEED-039](SEED-039-faster-ci-feedback.md#story-decomposition):
S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours, including delivery.

<a id="story-1"></a>

### Generate focus-note questions with relevant supporting passages

**Identity:** SEED-007#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **Goal:** A learner gets an answerable question about the focus note
  whose distractors and qualifications use the relevant surrounding knowledge.
- **Scope:** Choose a small set of real weak or ambiguous questions. Establish
  the current result and whether each miss concerns candidate selection,
  excerpt selection, or generation. Compare the smallest task-aware context
  change against that baseline and retain only an evidenced improvement.
  Keep the focus note as the learning target and ordinary token/cost limits
  explicit. Do not require a summarizer, a new model, per-link calls, or embeddings.
- **Evaluation:** A related note contains the useful distinction beyond its
  initial excerpt. The generated question uses that distinction to avoid an
  ambiguous distractor while keeping the correct answer grounded in the focus
  note. Also include an irrelevant nearby note to detect unnecessary context.
  The owner compares answerability, ambiguity, target fidelity, latency, and
  cost on the same cases; model self-evaluation alone is insufficient.
- **Value / learning:** Improve an existing daily learning outcome and identify
  whether graph selection or evidence presentation is the limiting factor.
- **Effort hypothesis:** L, low confidence until cases are selected. Bound the
  first comparison; resplit before execution if evaluation and a useful retained
  improvement cannot fit. An inconclusive experiment is recorded honestly and
  does not establish that the improvement story is complete.
- **Depends on:** No new story; existing generation, graph retrieval, context
  exports, and contest behavior suffice. General search and query are not
  prerequisites.
- **Safe stopping point:** A measured improvement can stand alone in question
  generation. If no candidate helps, preserve the baseline and evidence for
  an owner decision rather than shipping unmeasured complexity.

#### Refinement draft — 2026-10-14

The owner requested retaining and landing this unfinished draft, with the
needed evaluation inputs to follow later. Refinement remains incomplete and
the approach unselected; no slice plan, experiment, or implementation is
authorized by this draft.

**Observed basis.** Read the following code at
`039066b2b051cc9b4ca0bedc94572e6788bc33e4`; no application, model-quality
comparison, or timing measurement was run:

- `FocusContextRelatedNoteMaterializer.materialize` prefix-truncates related
  note content to 200 approximate tokens. A qualification beyond that prefix
  cannot reach generation through that excerpt.
- `FocusContextRetrievalService.retrieve` uses graph relationships and sampled
  folder peers without an intended-task input. Focus content is capped at
  1,500 approximate tokens within a combined content budget of 2,000.
- `QuestionGenerationRequestBuilder` subtracts author-instruction and
  property-focus tokens from that content budget. These are approximate
  content limits, not a cap on the complete request or monetary cost. Its
  request assembly is shared by generation, batch generation, and evaluation.
- `McqController.export` exposes the generation request for an authorized
  note. `AiToolFactory` already requires focus-note grounding and standalone
  questions; the learner cannot see the supplied context.
- `ContestableQuestion.vue` already offers contest feedback and replacement
  after an accepted contest. The proposed improvement uses this existing
  learner interaction; no additional interaction has been selected.

These observations establish a plausible excerpt-selection gap, not that it
causes the owner's weak questions or that a different excerpt improves them.

**Alternatives and recommendation (proposed).** Diagnose the selected real
cases first; compare the cheapest change that addresses the observed miss.

| Candidate | Learner effect | System consequence and limit |
| --- | --- | --- |
| Existing context with adjusted generation instructions | Could improve question wording using evidence already supplied | Smallest change; cannot recover an omitted passage |
| Select relevant source passages from candidate notes | Could avoid ambiguous distractors without another learner step | Could fit the existing budget; a selected passage can omit essential context |
| Rank candidate notes for the question task | Could bring a missing useful note into context | Changes candidate selection; does not by itself repair prefix truncation |
| Summarize context or generate per linked concept | Could incorporate more surrounding knowledge | Adds model work, cost, and latency; can shift the target away from the focus note |

The provisional first candidate is source-passage selection when the useful
note is already selected but its useful distinction is omitted. This borrows
an editor's evidence brief: the focus note supplies the claim being tested,
related notes supply quoted qualifications, and a short brief selects evidence
for the question writer. The analogy breaks when the qualification depends
on broader context; selecting a short quotation can itself mislead. The
analogy supplies a candidate, not evidence of benefit.

**Key examples to ground in real cases (illustrative, not observations).**

- A focus note supports the correct answer and a selected related note has a
  useful distinction beyond its initial excerpt → generate a question → the
  distinction helps avoid an ambiguous distractor, while the learner can
  answer from knowledge of the focus note.
- The same case also has an irrelevant nearby note → generate a question →
  the question remains about the focus note and its choices remain
  unambiguous. The example does not require rejecting every irrelevant note
  at retrieval time.
- The useful distinction is already in the request → a weak question still
  occurs → classify the miss as generation-related before choosing a
  retrieval change. A relevant note missing from the request instead calls
  for investigation of candidate selection.

**Proposed comparison and deferred promises.** Start with the current model,
one generation call, and the existing context budget; the owner has not yet
selected these comparison limits. Compare baseline and candidate on the same
real cases, with the owner judging answerability, ambiguity, and focus-note
fidelity and reviewing measured latency and token/cost usage. Agree the
acceptance threshold and repetitions before the comparison. Additional model
calls, new models, embeddings, a new learner control, and general search
improvements are not delivery commitments. These are deferred promises, not
new product rejection rules. Existing note access rules and focus-note
grounding remain constraints.

**Architecture.** Keep the bounded shared focus-context concept from
[ADR 0001 — Ubiquitous language](../../docs/adrs/0001-ubiquitous-language.md#focus-context),
including the distinction between an MCQ and a recall prompt. A task-aware
selection responsibility must account for the current shared generation and
evaluation assembly and other context consumers; no separate context model
has been selected. Preserve visible failure handling under
[ADR 0006 — Failure handling](../../docs/adrs/0006-failure-handling-accepted.md),
and use isolated disposable environments for comparisons under
[ADR 0007 — Environments and isolation](../../docs/adrs/0007-environments-and-isolation-accepted.md).
No conflict was found for these proposed directions. ADR 0001's index and
in-file status both say Accepted despite its filename lacking the usual
`-accepted` suffix; no status change is implied.

**Inputs needed from the owner before completing refinement.**

1. Supply 2–3 real weak or ambiguous questions with their focus and supporting
   notes, or an accessible notebook/export location. Illustrative cases above
   cannot substitute for this evidence. Use the supplied cases to distinguish
   candidate-selection, excerpt-selection, and generation misses.
2. Confirm or revise the proposed call/model/context limits, acceptable added
   latency/cost, and human comparison acceptance criteria. The recommendation
   is to begin within existing call and context limits, but no answer is
   assumed from permission to land this draft.

Once those inputs settle the examples and comparison, finish refinement and
continue into slice planning. An inconclusive result remains evidence for an
owner decision, not completion of the improvement story.

## Ordering and Open Decisions

This follows the epic's two learning/refinement stories in global priority.
Select actual question cases, human evaluation criteria, and acceptable added
latency/cost at refinement. No experiment or implementation is authorized by
this decomposition record. Preserve the research below as hypotheses and
provenance; its reported external results do not establish benefit for Donut.

## Original Exploration — 2026-08-21

## Why This Matters

Research on concept-grouped retrieval for question generation surfaced Savaal (arXiv 2502.12477), which splits the LLM call **per concept**: each call receives one main idea plus its top-k retrieved passages, and generates N/M questions per idea in separate calls. Savaal reports 6.5× better depth-of-understanding on dissertations and 1.5× on papers vs direct-prompting.

In Donut, the wiki links of the focus note ARE concept anchors (each linked note is an atomic concept node — see `.planning/notes/graph-structural-retrieval-as-implicit-concept-grouping.md`). So the per-concept split translates to: **one question-generation call per linked-note-group**, where each call receives the focus note plus the related notes reached through that one link, rather than one call with all related notes batched together.

This is a different prompt structure from the current single `<focus_context>` envelope that batches all related notes. It is the most concrete structural change the research points to for deepening QG.

## When to Surface

**Trigger:** QG quality plateaus (questions feel shallow, repetitive, or fail to test discrimination between the focus note and its neighbors); or when redesigning the retrieval→prompt pipeline (`FocusContextMarkdownRenderer`, the question-generation prompt assembly, or the recall-prompt generation service).

Also surface if the companion summarization finding (`.planning/notes/qg-retrieval-summarization-highest-leverage.md`) is acted on and the question becomes "summarize-then-batch vs summarize-then-split-per-concept."

## Scope Estimate

**Medium** — the retrieval service already produces per-link-grouped candidates (BFS frontier tracks the parent that proposed each note). The work is in the prompt-assembly layer: group related notes by their proposing link, render one envelope per group, and make one generation call per group. Requires evaluating whether the depth gain (Savaal's evidence) shows up at single-note scale, where Savaal is untested.

Key risk: Savaal's per-concept win **scales with document length**. At single-note scale the gain may be small or absent. This is exactly the open empirical question — spike before committing.

## Breadcrumbs

- `backend/src/main/java/com/odde/donut/services/focusContext/FocusContextRetrievalService.java` — `Proposal` / `retrievalPath` already track the proposing parent
- `backend/src/main/java/com/odde/donut/services/focusContext/FocusContextMarkdownRenderer.java` — single-envelope renderer; would need a per-group variant
- `docs/focus-context/focus_context_retrieval_design.md` — current retrieval design
- `.planning/notes/graph-structural-retrieval-as-implicit-concept-grouping.md` — why wiki links are concept anchors
- `.planning/notes/qg-retrieval-summarization-highest-leverage.md` — companion finding; summarization may be a stronger lever than splitting
- Savaal: [arxiv.org/html/2502.12477v1](https://arxiv.org/html/2502.12477v1), [github.com/mit-nms/savaal](https://github.com/mit-nms/savaal)

## Notes

Captured while exploring focus-note retrieval structure. The seed is dormant until QG quality becomes a measured concern or the prompt pipeline is redesigned. Savaal's AI-judge was unaligned with human experts — any spike needs human evaluation, not auto-evaluation.
