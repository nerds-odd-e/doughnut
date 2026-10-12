# Execution observation: real-model gate

**Identity:** SEED-073#story-1

Supporting evidence for the canonical [PLAN.md](PLAN.md); G1 and all execution statuses remain owned there.

Observed on 2026-10-12 at `5d6307f63d716bd99b275bd2df88f3cca1f5499b` in the established execution checkout, branch `codex/resolve-a-confusing-recall-answer-with-an-explan`, mode Story Branch. The accepted claim is that revision on `origin/main`; publisher is `dashboard-territory.local-doughnut`, agent Ai-chan, and starting revision is `05b3651f2d9fb7e6f16a3e34447be602db4cc3b3`.

The owner authorized the bounded paid probe in the execution conversation. Six AI replies were consumed, capped at eight. Model observed in every conversation export: `gpt-4.1-mini`. No separate paid question-generation endpoint was called. At this first-run boundary, original slice 1 failed and original dependent slices 2–8 stayed stopped. This historical result is not completed implementation. The later [two-reply follow-up](PRACTICE-FOLLOWUP-OBSERVATION.md) and refined ownership/status remain with [PLAN.md](PLAN.md).

### Route and setup

Checkout preparation and project command passed: `./scripts/run.sh bash scripts/worktree_setup.sh` and `./scripts/run.sh pnpm exec tsc --version`. The isolated selection helper reported `isolated: true`.

The paid held route's owned allocation was `doughnut_e2e_wt_15962f3b994147d4a1cb17ae36fb3fe7`, backend 59805, Vite 59806, browser origin `http://127.0.0.1:59807`. The runner reported ready after seeding. Authentication used its seeded `old_learner` account.

The first hold failed before readiness or paid calls with a Gradle cache-pack header-size mismatch. `backend:sut` concurrently starts the classes watcher and cached `bootRunE2E`; concurrent output mutation is a plausible cause, not a demonstrated root cause. A targeted sequential classes precompile passed, then a second hold became healthy. This establishes route recovery, not a permanent fix for the startup failure.

Fixtures used `POST /api/testability/inject_notes`, `POST /api/mcqs/{noteId}`, and `POST /api/assimilation`. After verifying the held allocation and live schema, only the two fixed MCQ-to-tracker bindings were inserted into that isolated E2E database. Each actual attempt was answered through `POST /api/recall-prompts/{promptId}/answer` with `{"choiceIndex":1,"thinkingTimeMs":1000}`. Note, MCQ, tracker, prompt and conversation IDs were respectively 1 and 2 for the two cases.

Each turn used ordinary `/api/conversation/{id}/send` text, `/export`, `/ai-reply` SSE, then `/messages`. The exported consuming request contained accessible note context, original choices/selected answer/incorrect result/focus/rationale, the exact candidate guidance below, the existing note-update directive, and persisted conversation messages. Existing assistant tools, including `GeneratedMcq`, remained available.

Raw inspectable observations and harnesses are in `/tmp/seed073-real-probe-03ywcgl4`; key files are `observation.json`, `c3-export-response.txt`, `c3-ai-reply-response.txt`, `c3-messages-response.txt`, and the two grade/tracker snapshots. The fixtures and candidate prompt are retained below, with the complete transcript in [REAL-MODEL-TRANSCRIPT.md](REAL-MODEL-TRANSCRIPT.md), because temporary files are not a durable resume input.

### Literal observation commands

The sequential precompile, healthy held route, fixture harness, six conversation turns and two actual state snapshots used these commands. They are retained observations, not instructions to repeat paid calls or a new accepted quality result.

```sh
CURSOR_DEV=true nix develop -c backend/gradlew -p backend classes
CURSOR_DEV=true nix develop -c pnpm e2e:hold --paid-openai
CURSOR_DEV=true nix develop -c python3 /tmp/seed073-real-probe-03ywcgl4/probe.py
python3 /tmp/seed073-real-probe-03ywcgl4/turns.py c1 c2 c3
python3 /tmp/seed073-real-probe-03ywcgl4/turns.py a1 a2 a3
CURSOR_DEV=true nix develop -c mysql -h 127.0.0.1 -P 3309 -u root doughnut_e2e_wt_15962f3b994147d4a1cb17ae36fb3fe7 -B -e 'SELECT * FROM memory_tracker WHERE id IN (1,2); SELECT * FROM recall_log WHERE memory_tracker_id IN (1,2); SELECT * FROM answer WHERE id IN (1,2);' > /tmp/seed073-real-probe-03ywcgl4/after-answer-state.txt
CURSOR_DEV=true nix develop -c mysql -h 127.0.0.1 -P 3309 -u root doughnut_e2e_wt_15962f3b994147d4a1cb17ae36fb3fe7 -B -e 'SELECT * FROM memory_tracker WHERE id IN (1,2); SELECT * FROM recall_log WHERE memory_tracker_id IN (1,2); SELECT * FROM answer WHERE id IN (1,2);' > /tmp/seed073-real-probe-03ywcgl4/after-probe-state.txt
```

### Inputs

Notes:

```json
{
  "externalIdentifier": "old_learner",
  "notebookName": "Recall help probe",
  "noteTestData": [
    {
      "Title": "Correlation and causation",
      "Content": "Correlation means quantities vary together. Observational co-movement alone does not establish causation. Ice-cream sales and drowning incidents may both increase because summer heat affects both. Temporal order alone is insufficient; causal claims need evidence that addresses alternative explanations."
    },
    {
      "Title": "Even numbers",
      "Content": "An integer is even if it is divisible by 2 without a remainder. Both 2 and 4 satisfy this definition."
    }
  ]
}
```

Correlation MCQ and deliberately ambiguous MCQ:

```json
[
  {
    "questionStem": "An observational study finds that ice-cream sales and drowning incidents both rise during summer. What does this establish?",
    "responseChoices": [
      "The quantities are correlated; this alone does not establish that one causes the other.",
      "Buying ice cream causes drowning."
    ],
    "correctAnswerIndex": 0,
    "testedFocus": "Distinguish observational correlation from evidence of causation.",
    "validationRationale": "Co-movement can reflect a common cause such as summer heat; the observational relationship alone does not establish causation."
  },
  {
    "questionStem": "Which number is even?",
    "responseChoices": [
      "2",
      "4"
    ],
    "correctAnswerIndex": 0,
    "testedFocus": "Recognize an even integer.",
    "validationRationale": "2 is divisible by 2 without a remainder."
  }
]
```

### Exact candidate guidance

The following was temporarily added as a developer message in `ConversationAiRequestBuilder.addNoteContext` for a recall subject. It was reverted after observation and is not installed in production.

```text
This conversation helps the learner understand an answered recall question.

Use the original question, choices, selected answer, marked result, tested focus,
and rationale as the record of that attempt. Current accessible notes are source
context, and may differ from the attempt's original context.

Address the learner's actual reasoning and the distinction behind the selected
choice. Reasoning is optional: when the learner asks for an explanation, explain
without requiring them to describe their thought process first. Any interpretation
of why they answered that way is tentative; accept corrections and adapt the
explanation.

Examine objections to the wording, choices, answer key, or source evidence on
their merits. A marked result is not proof that the question is sound or that the
learner forgot. Acknowledge ambiguity, defensible alternative answers, and missing
evidence when present, and state what remains uncertain.

When requested, give one related practice question that changes the example while
checking the relevant distinction. Do not reveal its answer or explanation before
the learner responds. Then give feedback on that response and explain the reasoning.

Practice is an ordinary conversation exchange. Describe what the learner's response
demonstrates on this occasion without claiming lasting mastery. Conversational
agreement is not a formal contest verdict and does not change the original Grade,
answer, memory tracker, or recall schedule.

Keep using the ordinary conversation and available tools. Discussing a question or
source problem alone is not a request to update a note.
```

### Transcript

The complete six-reply transcript and exact returned tool arguments are retained in [REAL-MODEL-TRANSCRIPT.md](REAL-MODEL-TRANSCRIPT.md).

### Rubric and retained limits

- Correlation explanation: passed. c1 addresses the learner's actual selected causal inference, the plausible common cause and missing causal evidence.
- Corrected interpretation: passed. c2 engages the temporal-order objection and explains why sequence alone is insufficient.
- Changed practice example: present only in the c3 `GeneratedMcq` tool payload. It uses winter, hot-chocolate sales and flu rather than the original example.
- Ordinary practice question and withholding: failed at the returned response and persistence boundaries. c3 streamed a `GeneratedMcq` call including its explanatory `validationRationale` before any learner answer. `/messages` retained the c3 learner request after c2's assistant reply, with no new assistant question. The exact user-facing rendering of tool data was not observed; this finding does not claim an observed browser display.
- Practice-response feedback: unobserved. c4 was skipped because an ordinary question did not exist to answer. Mocked responses cannot fill that gap.
- Ambiguity: passed. a1/a2 recognize both 2 and 4 as even and examine the objection without insisting the learner forgot.
- Uncertainty and Grade: a3 distinguishes the flawed question/key from the learner's reasoning. Actual Answer, RecallLog and tracker snapshots before and after all turns match. This is not full practice-loop proof.
- This was one illustrative HTTP observation, not representative-user, efficacy, or mounted-UI evidence. Mounted interaction was assigned to original slices 5–7 (refined slices 7–9).

Active probe work was about seven minutes, exceeding the five-minute target because setup/fixture work required more than anticipated, but under the 10-minute hard limit. Startup/model waits were recorded separately. Both holds reached terminal shutdown; the successful hold ended with exit 0, its lock released and owned app processes stopped. The exact probe patch was restored. No permanent product code was changed.

### Consequence and next candidate

The assumption that the proposed guidance plus all existing tools naturally produces ordinary persisted practice messages is false for this observed case. The request owner exposed `GeneratedMcq`, and the model selected that tool. Keep the existing ordinary-message design and the story's outcome; do not build a practice-card or grading subsystem around this failed premise.

A concrete next candidate is to add the following after the practice paragraph:

```text
Ask conversational practice questions as ordinary assistant text, not as tool
calls. Do not use GeneratedMcq or question-generation tools for this practice:
those structured payloads carry an answer and rationale before the learner responds.
```

At this first-run boundary the candidate was unvalidated and original slices 2–8 remained stopped. It preserves note-update tools for explicit updates and changes no story scope or Grade policy. The later [authorized two-reply observation](PRACTICE-FOLLOWUP-OBSERVATION.md) preserves the exact revised guidance, actual question/response/feedback and remaining limits; this does not rewrite the failed first-run result.
