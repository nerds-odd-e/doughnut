# SEED-094: Recall question regeneration

<a id="story-1"></a>

### 1. Regenerate a recall question successfully

**Identity:** SEED-094#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planless"}
```

**Goal:** Resolve the reported failure when a learner regenerates a recall question.

**Scope:** Diagnose the supplied stack trace and attempt one bounded repair through the recall-prompt controller. Reproduce the discrepancy before changing production code, retain regression coverage, and verify related backend behavior. Use the bug-fixing workflow's planless ten-minute repair attempt with no replanning.

**Known expectations:** A learner can regenerate an existing supported recall question and receive the replacement question.

**Observed behavior:** The supplied report shows `POST /api/recall-prompts/143043/regenerate` throwing `NullPointerException` in `RegenerateQuestionMessage.generatedMcqView`, line 41, while constructing an `ArrayList` from a null collection.

**Evidence and uncertainty:** Report supplied on 2026-10-30; production revision, stored question payload, and original event time are unknown. Establish the triggering question shape and whether the defect remains present before repair. No production account or data access is required.
