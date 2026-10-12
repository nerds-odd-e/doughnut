# Real-model practice follow-up observation

**Identity:** SEED-073#story-1

Supporting evidence for refined slices 1 and 3 in [PLAN.md](PLAN.md); this file grants no acceptance or execution status.

Observed 2026-10-12 at `5d6307f63d716bd99b275bd2df88f3cca1f5499b` in the established execution workspace `/Users/terryyin/git/doughnut/.worktrees/resolve-a-confusing-recall-answer-with-an-explan`, branch `codex/resolve-a-confusing-recall-answer-with-an-explan`, Story Branch mode. Publisher `dashboard-territory.local-doughnut`, agent Ai-chan; authorized target `origin/main`, starting revision `05b3651f2d9fb7e6f16a3e34447be602db4cc3b3`.

The owner explicitly authorized this follow-up, capped at two additional AI replies. The earlier run consumed six; these two exhaust the total eight-reply cap. Both consuming exports named model `gpt-4.1-mini`. No further paid call is authorized. The [original failed practice result](REAL-MODEL-OBSERVATION.md#rubric-and-retained-limits) and [six-reply transcript](REAL-MODEL-TRANSCRIPT.md) remain intact.

## Route and starting context

Used the existing paid held route and verified owned allocation: database `doughnut_e2e_wt_15962f3b994147d4a1cb17ae36fb3fe7`, backend 59805, Vite 59806, origin `http://127.0.0.1:59807`. The held app reached readiness; seeded `old_learner` authenticated. The same two fixed [notes and MCQs](REAL-MODEL-OBSERVATION.md#inputs) were recreated through the existing isolated fixture route. Fixed MCQ-to-tracker bindings were made only after inspecting this allocation's live schema. The actual answer controller established fresh answered-attempt baselines with choice index 1 and thinking time 1000ms. Conversation 1 supplied the correlation practice subject; the ambiguous fixture was not sent to the model again.

The four exact prior c1/c2 messages from [REAL-MODEL-TRANSCRIPT.md](REAL-MODEL-TRANSCRIPT.md) were inserted as starting context only. Their insertion establishes no new explanation, continuation, or message-persistence behavior. Only the new c3 request/question and c4 learner answer/feedback below went through actual `/send`, `/export`, `/ai-reply` SSE and subsequent `/messages` controllers. These new turns are the persistence observation.

Inspected raw evidence is `/tmp/seed073-practice-followup-4o0cijt3`: `observation.json`, `guidance.txt`, `commands.txt`, `c3-export-response.txt`, `c4-export-response.txt`, both `*-ai-reply-response.txt`, both `*-messages-response.txt`, `before-practice-state.txt`, `after-practice-state.txt`, `seed-history.sql` and `cleanup.txt`. Temporary files are diagnostic locations; durable inputs, guidance, complete new transcript, commands, state and limits are retained here and in the linked original records.

## Literal observation commands

These are the commands already used, not instructions to repeat paid calls.

```sh
CURSOR_DEV=true nix develop -c backend/gradlew -p backend classes

CURSOR_DEV=true nix develop -c pnpm e2e:hold --paid-openai

CURSOR_DEV=true nix develop -c mysql -h 127.0.0.1 -P 3309 -u root doughnut_e2e_wt_15962f3b994147d4a1cb17ae36fb3fe7 -e 'SHOW COLUMNS FROM recall_prompt; SHOW COLUMNS FROM memory_tracker; SHOW COLUMNS FROM conversation_message;' > /tmp/seed073-practice-followup-4o0cijt3/live-schema.txt

CURSOR_DEV=true nix develop -c python3 /tmp/seed073-practice-followup-4o0cijt3/probe.py

CURSOR_DEV=true nix develop -c python3 /tmp/seed073-practice-followup-4o0cijt3/seed-history.py

CURSOR_DEV=true nix develop -c mysql -h 127.0.0.1 -P 3309 -u root doughnut_e2e_wt_15962f3b994147d4a1cb17ae36fb3fe7 -B -e 'SELECT * FROM memory_tracker WHERE id IN (1,2); SELECT * FROM recall_log WHERE memory_tracker_id IN (1,2); SELECT * FROM answer WHERE id IN (1,2);' > /tmp/seed073-practice-followup-4o0cijt3/before-practice-state.txt

python3 /tmp/seed073-practice-followup-4o0cijt3/turns.py c3

python3 /tmp/seed073-practice-followup-4o0cijt3/turns.py c4

CURSOR_DEV=true nix develop -c mysql -h 127.0.0.1 -P 3309 -u root doughnut_e2e_wt_15962f3b994147d4a1cb17ae36fb3fe7 -B -e 'SELECT * FROM memory_tracker WHERE id IN (1,2); SELECT * FROM recall_log WHERE memory_tracker_id IN (1,2); SELECT * FROM answer WHERE id IN (1,2);' > /tmp/seed073-practice-followup-4o0cijt3/after-practice-state.txt
```

## Exact revised guidance

The only difference from the original candidate is the paragraph beginning “Ask conversational practice questions”. The explanation, correction and ambiguity instructions remain byte-identical; those earlier five responses were not rerun under this revision. Existing `NoteContentCompletion`, `TitleReplacement`, `GeneratedMcq`, and `QuestionEvaluation` tools and the existing note-update directive remained available. Both exports included the original attempted question, selected answer, incorrect result, tested focus/rationale, accessible note context and conversation history.

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

Ask conversational practice questions as ordinary assistant text, not as tool
calls. Do not use GeneratedMcq or question-generation tools for this practice:
those structured payloads carry an answer and rationale before the learner responds.

Practice is an ordinary conversation exchange. Describe what the learner's response
demonstrates on this occasion without claiming lasting mastery. Conversational
agreement is not a formal contest verdict and does not change the original Grade,
answer, memory tracker, or recall schedule.

Keep using the ordinary conversation and available tools. Discussing a question or
source problem alone is not a request to update a note.
```

## Complete new transcript

Exact prior c1/c2 four-message history was seeded as starting data only; it is not new persistence proof. These two new turns came through the actual send/export/AI-SSE/messages controllers.

### c3

Learner: Give me one different, related practice question. Wait for my answer before explaining it.

Assistant: Sure! Here's a related practice question for you:

A study finds that the number of people carrying umbrellas and the number of traffic accidents both increase during rainy days. What does this observation establish?

1. Carrying umbrellas causes more traffic accidents.
2. The two quantities are correlated; this alone does not prove one causes the other.
3. Rain causes people to carry umbrellas and also causes more traffic accidents.
4. Traffic accidents make people carry umbrellas.

Please pick the best answer.

### c4

Learner: I choose 2. The study establishes that umbrella carrying and traffic accidents vary together, but co-movement alone does not show that umbrellas cause accidents. Rain is a plausible common cause: it can lead people to carry umbrellas and make driving harder. That makes choice 3 a possible explanation, but the observation as stated does not establish those causal claims either. We would need evidence that addresses alternative explanations before inferring causation.


Assistant: Your choice and reasoning are well thought out and correct.

Choosing 2 acknowledges the key principle that correlation alone does not prove causation. You also correctly note that rain is a plausible common cause driving both umbrella use and traffic accidents, but the original observation does not by itself establish that causal chain. Additional evidence would be needed to confirm rain’s causal role.

Choice 3 is indeed a plausible explanation, but the question asks what the observation alone establishes. The answer is that the quantities are correlated, but causation requires more than just observing their co-movement.

Your response shows a clear understanding of the distinction between correlation and causation and the need for evidence beyond mere observation to make causal claims. Great job! Would you like to try another practice question or discuss this topic further?

## Persisted turns and Grade baseline

The c3 GET returned six messages: four seeded messages, learner request id 5 and new ordinary assistant question id 6 (`2026-10-12T06:43:36.652Z`). The c4 GET returned eight, adding actual learner answer id 7 and assistant feedback id 8 (`2026-10-12T06:43:57.728Z`). SSE text matches the corresponding stored question and feedback. Export before c4 consumed the persisted question and actual learner response. The new question, response and feedback survived subsequent real controller reads; mounted close/reopen is still slice 10 proof.

The following is the actual before-practice snapshot (Nix wrapper line retained). The after-practice snapshot was byte-identical; this comparison covers the fresh answered fixtures within this follow-up, not continuity across the held-runner fixture reset.

```text
<<running within nix env>>
id	user_id	stability	last_recalled_at	assimilated_at	next_recall_at	removed_from_tracking	note_id	property_key	property_value	type	difficulty
1	1	5	2026-10-12 06:42:54	2026-10-12 15:42:54	2026-10-12 11:42:54	0	1			UNDERSTANDING	6.4133
2	1	5	2026-10-12 06:42:55	2026-10-12 15:42:54	2026-10-12 11:42:55	0	2			UNDERSTANDING	6.4133
id	memory_tracker_id	recorded_at	elapsed_hours	product_outcome	answer_id	tutor_feedback	stability_before	difficulty_before	retrievability
1	1	2026-10-12 15:42:53.930	0	AGAIN	1	NULL	0	NULL	NULL
2	2	2026-10-12 15:42:54.642	0	AGAIN	2	NULL	0	NULL	NULL
id	result	created_at	updated_at	choice_index	thinking_time_ms	spelling_answer	outcome	away_ms	away_count	detour_ms	detour_count	idle_ms
1	0	2026-10-12 15:42:54	2026-10-12 15:42:53	1	1000	NULL	NULL	NULL	NULL	NULL	NULL	NULL
2	0	2026-10-12 15:42:55	2026-10-12 15:42:54	1	1000	NULL	NULL	NULL	NULL	NULL	NULL	NULL
```

## Rubric and limits

- Changed example: passed. Umbrella carrying and traffic accidents during rain test the same correlation/causation distinction through an ordinary assistant question.
- Answer withholding: passed at SSE and persisted-message boundaries. c3 supplies alternatives and asks for the best answer without selecting a key or giving the explanation before the learner responds.
- Feedback: passed. c4 evaluates actual choice 2, addresses rain as a plausible common cause and choice 3 as a hypothesis exceeding the observation, and explains the need for causal evidence.
- Retention and Grade: passed at HTTP/storage boundaries for the three new turns; actual Answer, RecallLog and MemoryTracker fields match before/after. Feedback describes this response without certifying lasting mastery.
- Earlier explanation/ambiguity observations retain their original guidance/revision and illustrative-case limitations. The practice-only directive leaves their reasoning contract unchanged; compatibility is bounded, not evidence of another run.
- Browser rendering, accessible interaction, privacy and mounted return/reopen were not observed. Current slices 5–10 retain their production and mounted proof; this evidence does not substitute for them.
- Two new replies on one illustrative fixed case establish neither representative-user success nor learning efficacy. All eight authorized replies are consumed. A materially changed future candidate requires separate authorization and relevant quality proof.

Follow-up active setup/observation was about 3–4 minutes, with external startup/model waits separate. Together with the original roughly seven active minutes, original slice 1 crossed the ten-minute hard limit. [PLAN.md](PLAN.md#current-decisions-and-learnings) owns the consequent split; no timing exception erases that overrun. Owned held runner 63322 reached terminal exit 0 after Ctrl-C and released its allocation; the exact temporary Java guidance patch was restored after shutdown. No permanent product change was made.
