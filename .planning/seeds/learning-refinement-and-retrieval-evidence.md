# Learning, refinement and retrieval: dated evidence

Extracted verbatim on 2026-11-13 SGT from
`.planning/seeds/SEED-073-learning-refinement-and-retrieval.md` at source revision
`e5b8aca1c605f59775f8cc607ea5f08095a9348e`. The original dates, analyzed/deployed
revisions, inputs, private aggregate/numeric evidence, actions, results and
limitations below remain as recorded. This extraction changes no observation.

## Learning and retrieval assessment

The 2026-10-12 assessment read local main at `e851a7682d`, including code, tests, and Accepted ADRs. It did not run the app or measure model quality:

- Answered MCQs already open refinement with the question and tested focus,
  but that refinement input omits the learner's selected answer.
- Refinement already decomposes content and previews extraction; contesting a
  question and conversing about a recall prompt are existing actions.
- Chat already proposes title/content updates for acceptance. The recall-chat
  proposal handoff needs verification: frontend acceptance reads the direct
  note subject, while a recall conversation has a recall-prompt subject.
- A shared bounded focus context already serves question generation, chat,
  automation, exports, and the graph API. Task information does not guide
  candidate selection. Related-note bodies are truncated to short prefixes.
- Search matches titles and aliases. MCP lists those matches as candidates
  with notebook and folder, reads a chosen note in full with its notebook's
  accepted revision, and offers graph context; it does not search note bodies.
- Git preserves accepted notebook content; recall history preserves learner
  events. Neither alone records the diagnosis, proposal decision, and learning
  follow-up as one resolved difficulty.

These are starting observations, not promises that the current UI works in every cited journey. Verify the selected story's decisive premises when refining it.

<a id="recall-question-integrity-observations"></a>

## Recall question integrity: refinement observations

Observed on 2026-11-16 SGT while refining
[Validate recall question integrity](SEED-073-learning-refinement-and-retrieval.md#validate-recall-question-integrity),
from a worktree at revision `3f2f1b937821edcb2f57cb982942a06e055f4732`.

**Inputs.** The local MCP export `recall-answers-100d.json`
(`list_recall_answers`, 2026-08-05T09:03Z to 2026-11-13T09:03Z): 11,091
attempts, of which 8,489 are multiple-choice and 2,602 are spelling. Row
numbers are one-based in that export. Generation times came from read-only
production reads with the owner's CLI token (`POST /api/notes/search`,
`GET /api/notes/{id}/note-info`,
`GET /api/memory-trackers/{id}/recall-prompts`,
`GET /api/user/recall-history-export`, `GET /api/recalls/recalling`,
`GET /api/settings/current-model-version`). No model was called and nothing
was written to Production. Periods in the rows that cite the recall history
export use Singapore time.

**Code reading at that revision.**

- A generated question names one correct answer and three distractors.
  `GeneratedMcq.isValid()` rejects a blank field, a distractor count other
  than three, and choice text that repeats after trimming.
  `GeneratedQuestionPostProcessor` shuffles the choices and takes the key
  from the answer named as correct. Commits `1716fda6d3` and `f0d6859a2c`
  introduced both on 2026-08-22 (+0800). Before them the model returned a
  list of choices and the position of the correct one.
- Two routes generate questions and both apply that validity check: on
  demand (`McqService.generateAFeasibleQuestion`) and the hourly batch
  (`QuestionGenerationBatchRowImportService`).
- The verifier (`QuestionEvaluation`, called through
  `NoteQuestionGenerationService.evaluateQuestion`) receives the question
  without its key, selects every choice it judges correct, and upholds the
  question only when it selects exactly the stored key and finds the
  question feasible. It is called when a learner contests a question, and by
  the on-demand route `question.regeneration.times` times. That setting is 0
  in `application-prod.yml` and 1 in the E2E and unit-test profiles. The
  production start script
  (`infra/gcp/scripts/mig-zulu25-openai-app-instance-startup.sh`) passes no
  override. The batch route never calls the verifier.
- `RecallPromptService.generateAQuestion` returns the tracker's newest
  unanswered, uncontested stored question before it generates one. There is
  no age limit and the stored question is not checked again.
- After an answer, private recall help can propose excluding the attempt and
  the learner confirms it (ADR 0003, excluded attempt).

**Archive observations.**

| Observation | Result |
| --- | --- |
| Choice text repeated after trimming | 2 of 8,489: rows 8518 and 8719, generated 2026-08-21T19:00Z and 2026-08-21T00:57Z. Row 8518 was answered on 2026-08-22T23:48Z and records the same selected and expected text as wrong. None among attempts answered from 2026-08-23. |
| Rationale opens by endorsing a choice other than the key | 2: rows 9229 and 9350, generated 2026-08-17T14:45Z and 2026-08-15T18:00Z. None among the 6,200 attempts with a rationale answered from 2026-08-23. |
| Key text absent from the rationale while another choice's text is present | 40 answered before 2026-08-23 and 144 from then, of which 23 were marked wrong. Not reviewed; a paraphrased key produces the same signal. |
| Row 3748 (location particle) | Generated 2025-02-06T23:29Z, answered 2026-10-06T14:08Z. Three choices, no rationale. |
| Attempts on a question without a rationale | 385 of 8,489 (4.5%): 152 in August, 106 in September, 90 in October, 37 on 2026-11-01 to 2026-11-13. Of these, 144 have two or three choices. All 8,104 questions with a rationale have four. |
| Eight four-choice questions without a rationale answered in November (seeded random sample) | Generated between 2023-08-27 and 2024-11-20. |
| Ten questions with a rationale answered 2026-11-08 to 2026-11-12 (seeded random sample) | Nine located. Eight were generated within seconds after a full hour, up to 6.3 days before the answer. One was generated 21 seconds before its answer. |
| Row 4917 (event occurrence, two defensible answers) | Generated 2026-09-20T18:00Z, answered 2026-09-22T13:30Z. |
| Row 9797 (sigh reading) | Generated 2026-08-11T18:00Z, answered 2026-08-13T22:26Z. |
| Contested state of rows 3748, 4917, 8518, 8719, 9229, 9350, 9797 | None was contested. |
| Repeated stems | 231 of 8,177 distinct stems occur more than once (543 attempts). |
| Wrong selection equals the key of another attempt with the same note and stem | 3: rows 8586, 9229, 9350, all answered before 2026-08-23. |
| Wrong selection equals a key credited on another attempt for the same note | 25 of 963 wrong multiple-choice attempts: 11 answered before 2026-08-23 and 14 from then. Not reviewed. |
| Recall history export, exported 2026-11-16T14:50+08:00 | 122,671 multiple-choice answers, each with the time its question was generated. All 8,489 multiple-choice rows of the MCP export match one answer by its second, none ambiguously. |
| Multiple-choice answers from 2026-08-23, by when the question was generated | 6,659 answers: 6,323 generated from 2026-08-22, 68 from 2026-05-16 to 2026-08-21, 69 from 2025-01-01 to 2026-05-15, and 199 before 2025. |
| Multiple-choice answers from 2026-11-01, by when the question was generated | 1,106 answers: 1,061 generated from 2026-08-22, 5 from 2026-05-16 to 2026-08-21, 11 from 2025-01-01 to 2026-05-15, and 29 before 2025. |
| Questions generated from 2026-08-22 and answered from 2026-08-05 | 6,361, of which 5,682 were generated within the first minute of an hour. |
| Excluded attempts, all time | 1, answered and excluded on 2026-10-23. |
| Sixty of the 14,301 non-spelling trackers (seeded random sample), read through `recall-prompts` | 585 prompts, 4 of them contested. 48 unanswered, uncontested stored questions: 37 generated before 2025, 9 from 2025-01-01 to 2026-05-15, and 2 from 2026-08-22. Fourteen have three choices and none repeats a choice. An unanswered question is returned without its key or rationale. The 60 sequential reads took 52.6 seconds. |
| Production model settings | The question generation model and the evaluation model are both `gpt-6.1-sol`. |

**Limitations.** Rows that cite the MCP export alone split by answer date;
its generation times were looked up for the seven named rows and the
eighteen sampled rows only. Both exports hold answered attempts only, so
questions a learner contested and had replaced, and stored questions not yet
answered, are absent from them. The stored-question figures come from 60
trackers and are not a count. Neither export records which model generated
a question. The text comparisons are
signals, not judgments that a question is wrong. The production setting was
read from the repository; the running instance was not inspected because the
local `gcloud` login had expired.
