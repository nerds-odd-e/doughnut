# Remaining consumer runtime baselines

## Provenance and authority

Observed on 2026-10-12 at published revision
`5c6243073aa710c6d39db0decc44fc0dfa3ba017` in
`/Users/terryyin/git/doughnut/.worktrees/resolve-a-confusing-recall-answer-with-an-explan`.
The coordinator authorized bounded unpaid preparation observations and accepted
the four inspected results. Production source was unchanged. Concurrent planning
writes are outside these product observations. No model call occurred.

These observations settle the runtime premises for planned leaves 12–14 in
[PLAN.md](PLAN.md). They are baselines, not accepted correction/delivery proof.
Completed slice proof and the paid MCQ protocol remain separately owned.

## Mounted frontend consumers

Literal command:

```sh
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/Seed073ConsumerBaseline.spec.ts
```

Real Chromium mounted the production RecallPage/router and MemoryTrackerPageView,
actual answer selection, contextual conversation content, note store and nested
refinement dialog. Only backend HTTP was mocked with `mockSdkService`; in-process
components were real. On 2026-10-12 at 09:33:34 UTC (18:33:34 JST), the terminal
result was exit 1: three tests, three expected surviving-behavior assertion
failures, 5.62 seconds. Exact source, output and screenshots remain at
`/tmp/seed073-consumer-baseline-20261012T093246Z`.

### Read acknowledgement

The actual answer selected choice index 1 from Association/Causation for
“What does correlation establish?” in prompt 73. Conversation 19 fetched and
rendered assistant message 75: “A common cause can explain the observed
association.” The shared unread store began with that same HTTP-returned message.
The expected read call failed with this literal diagnostic:

```json
{"conversationId":19,"markReadCalls":[],"globalUnreadIds":[75]}
```

This observes the contextual frontend fetch/store consumer. Backend unread SQL,
server persistence, successful read acknowledgement, AI-completion acknowledgement,
failed-fetch handling, Message Center and direct NoteConversation behavior were
not newly observed. Slice 12 still owns those positive correction observations.

### Nested dismissal

From that same answer → help parent, the learner opened Refine note and typed
“A common cause can explain both observations.” A bubbling document keydown
Escape reached the repository dismissal handler. The surviving-parent assertion
failed with the literal before/after diagnostic:

```json
{"before":{"openDialogs":2,"typed":"A common cause can explain both observations."},"after":{"openDialogs":0,"helpConnected":false,"remainingHelpOpen":false,"remainingReviewOpen":false}}
```

The real browser DOM and document handler were observed; native OS key delivery
was not separately tested. Child-only dismissal, eventual recall/focus return and
ordinary extraction through actual UI/API remain slice 13's delivery proof.

### Spelling refinement

The real MemoryTrackerPageView rendered answered SPELLING history prompt 82,
wrong spelling `Seddition` for note `Sedition`, and a matching AnsweredQuestion
without an MCQ. Get private help → Refine note showed the clarification editor,
with zero generation requests before review. The learner typed
“The word has one d before the i.” and continued. The actual single HTTP call was:

```json
{"path":{"note":47},"body":{"clarification":"The word has one d before the i."},"signal":{}}
```

The signal was an AbortSignal; the empty JSON above is its diagnostic rendering.
The mocked empty layout rendered. The expected ordinary refinement request failed.
This proves frontend request timing/body, not backend interpretation or model
quality. Slice 14 still owns the positive ordinary spelling flow and retained
reviewed MCQ/practice action proof.

### Failed initial setup and restoration

`frontend.initial-log-fixture-failure.log` retains the earlier 18:32:54 JST run:
the repository's console.log guard threw before some required actions. It is
failed observation setup, not product-defect proof. The second run used assertion
diagnostics and reached all three consuming sequences. After terminal completion,
only the owned temporary `frontend/tests/pages/Seed073ConsumerBaseline.spec.ts`
was removed. No delivered frontend change remained; full-suite/typecheck acceptance
was not claimed from this preparation run.

## Actual spelling conversation export

Literal command:

```sh
CURSOR_DEV=true nix develop -c pnpm backend:test_only
```

An owned note titled `Millennium`, content “A millennium is a period of one
thousand years.”, had a real spelling MemoryTracker/RecallPrompt created by
`makeMe`. Actual `RecallPromptController.answerSpelling` submitted `Millenium`
and recorded incorrect. Actual start, learner send and export controllers ran.
The learner's message was “Why was my spelling marked incorrect?”

The temporary controller test ran at 2026-10-12T09:34:04.436Z and passed. The full
suite finished at 09:34:30 UTC with exit 0: 2,778 tests, zero failures/errors,
two skipped and 570 XML reports, including this one temporary test. Literal
artifacts remain at `/tmp/seed073-consumer-backend-baseline-8_jdjuwe`.

The exported input was exactly:

```json
[{"content":"Why was my spelling marked incorrect?","role":"user","type":"message"}]
```

The model was `gpt-4.1-mini`; its four ordinary tools were NoteContentCompletion,
TitleReplacement, GeneratedMcq and QuestionEvaluation. This was request export,
with no model endpoint invocation. Recorded `questionDetails` was `{}`.

This contradicts the earlier source-only inference that the newly installed
MCQ guide reaches an MCQ-null spelling subject. Source reading explains the
observed request: `Conversation.getSubjectNote` returns a recall note only for
a non-null MCQ; `ConversationAiRequestBuilder.addNoteContext` returns immediately
when that subject note is null. The ordinary spelling export therefore contains
only user history. Preserve that existing request; no backend guide bug or new
spelling source-context requirement is inferred.

Limits: direct controller calls bypass request binding/web serialization.
Jackson NON_NULL omitted captured null mcq/subject-note/answerOutcome fields;
`source.java` preserves their capture sites. No model quality, browser reopen or
same-attempt retention was observed here. Broader spelling source/model quality
is outside the selected MCQ goal, with its disposition in G10. This limit does
not waive spelling's ordinary refinement compatibility proof in slice 14.

Only the owned temporary
`backend/src/test/java/com/odde/donut/controllers/Seed073SpellingConsumerRuntimeObservationTest.java`
was removed after its terminal result. No product/source, CI, formatting, lint,
commit, push or paid-service operation was performed by these probes.

## Exact artifact identity

The following SHA-256 values bind the retained raw sources/results:

| Artifact | SHA-256 |
| --- | --- |
| frontend source | `31a9ad4a51d30dfb9453620d5fd464a6c5488442c9b0d7d4173c69968719f615` |
| frontend completed baseline log | `5c9804df466182a70da7a9a67e06cfdd4b8cb63ff4fc97123f94ada803c20c0c` |
| frontend failed setup log | `4adaa4529af631c27bc0b38e52f8fd90b609095b4ca641bc0100bb0197d38354` |
| backend source.java | `900f0d74fc7557bdf4c2f501bb7141663b9f1e99e127ee8c3f3e6b4d15d3f057` |
| backend controller-export.json | `f27e4f074d5a9a4057f011924c082dec4f533dcf2b4af82dd48b07bcb10902be` |
| backend fixture-actuals.json | `a78d679fb381f3d93d26baf7049ffe383519c6c2a831d33d64ee7d8ecdc89707` |
| backend provenance.json | `2226caa7e14b8d6195e10cbf7d3ec76fbb17c9302660b30df9b98ac5e0457644` |
| backend suite-summary.json | `5521fc63106a323013295172c953745e79c69c151ee4176075421069a9bdb3ec` |
