---
id: SEED-084
status: dormant
planted: 2026-10-23
planted_during: failure report 862 investigation
trigger_when: recall prompt generation receives incomplete structured AI output
scope: small
---

# SEED-084: Resolve recall-prompt failure report 862

## Story Decomposition

<a id="story-1"></a>

### Handle incomplete AI output during recall-prompt generation

**Identity:** SEED-084#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planless","assessment":"ready","reasons":[],"basis":{"document":"c7da7cbf20c3e1e179c3747b99afb8da55526ee0f55c721fdc42e6627da0c614"}}
```

**Goal:** Resolve the reported recall-prompt failure with an evidenced diagnosis
and a bounded repair when confirmed.

**Scope:** Investigate failure report 862, then repair the confirmed discrepancy
through the shared bug-fixing workflow. Preserve existing successful question
generation and the product's intended outcome when AI cannot generate a question.

**Known expectations:** A learner requesting a recall prompt receives a usable
question or the established generation-failure outcome.

**Observed:** On 2026-10-13, GET /api/memory-trackers/11911/recall-prompt failed
with OpenAIInvalidDataException while parsing JSON ending midway through the
second Japanese distractor. The stack reaches OpenAiApiHandler's structured
response parsing through NoteQuestionGenerationService.

**Uncertainty:** The report omits response status and incomplete-details metadata;
the cause of truncation and the current failure handling require investigation.

**Evaluation:** Reproduce the confirmed discrepancy through the existing stable
API boundary before repair; verify the intended observable result and related
backend behavior.

**Bounded repair instruction:** Through the controller boundary, reproduce a
truncated structured response marked incomplete by OpenAI, then return the
existing generation-failure outcome before attempting to parse that output.
Preserve completed-response generation. The report lacks provider metadata, so
do not claim which incomplete reason caused the historical incident. Use the
bug-fixing workflow's ten-minute planless attempt with no replanning.

**Key examples:** Incomplete AI output with truncated Japanese distractors
produces the existing HTTP 503 generation-failure response. A later successful
request generates a usable recall prompt without changing the learning state.

**Source:** Owner-supplied failure report 285 / GitHub issue 862, pasted 2026-10-23.
