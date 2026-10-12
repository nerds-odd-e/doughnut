---
id: SEED-007
status: dormant
planted: 2026-08-21
planted_during: focus-note retrieval exploration for OKF / LLM Wiki
updated: 2026-10-12
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

- **For / why:** A learner gets an answerable question about the focus note
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
