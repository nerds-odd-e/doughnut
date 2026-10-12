# Accepted slice behavior and proof

Retained verbatim from PLAN.md at reviewed published revision `5c6243073aa710c6d39db0decc44fc0dfa3ba017` on 2026-10-12. The active [PLAN](PLAN.md) keeps statuses and owns remaining work. These observations retain their original boundaries and limitations; later joined-consumer findings do not invalidate their distinct proof.

<a id="slice-1"></a>

### 1. Prepare fixed answered attempts in the isolated route

Type: Structure
Status: done

Sizing: at most five minutes active fixture/setup work; external startup waits remain separate. Reuse the inspected setup rather than repeat it.

Change: Stage the exact proposed guidance temporarily, establish the existing isolated held-app route, and bind the two fixed MCQs to owned trackers. Answer both through the actual answer controller. This immediately enables slice 2; add no production fixture service or practice state.

Proof: The recorded route reaches readiness; exported conversation requests consume the original answered correlation and ambiguous-even-number fixtures. Preserve the actual Answer/RecallLog/tracker baseline and restore the temporary patch before delivery. [First-run setup](REAL-MODEL-OBSERVATION.md#route-and-setup) and [follow-up setup](PRACTICE-FOLLOWUP-OBSERVATION.md#route-and-starting-context) supply inspectable commands, allocation and inputs; coordinator judges reuse.

<a id="slice-2"></a>

### 2. Validate explanation, correction and ambiguity

Type: Behavior
Status: done

Sizing: at most five minutes active transcript/rubric work; model waits are separate.

Behavior: Given the fixed answered attempts and learner reasoning, the real conversation reply explains the causal distinction, adapts to corrected interpretation, and examines both defensible answers to the ambiguous even-number question.

Proof: The original c1/c2 and a1–a3 [transcript](REAL-MODEL-TRANSCRIPT.md) and [rubric](REAL-MODEL-OBSERVATION.md#rubric-and-retained-limits) retain their exact original guidance/revision. The follow-up changes only the practice directive, so these explanation/ambiguity observations remain compatible within that boundary; they were not rerun under revised guidance. No efficacy inference. Failed/unavailable quality stops dependent slices 3–11.

<a id="slice-3"></a>

### 3. Validate ordinary changed practice and response feedback

Type: Behavior
Status: done

Sizing: at most five minutes active practice protocol/rubric work; model waits are separate. Reuse the two already observed replies.

Behavior: After an explanation, a requested changed example is an ordinary assistant message, withholds the key/reasoning until the learner responds, then gives feedback on that actual response and retains the exchange without changing the original Grade.

Proof: [Follow-up evidence](PRACTICE-FOLLOWUP-OBSERVATION.md) records the exact revised guidance, actual c3 question → c4 learner answer → feedback, later controller message reads and byte-identical Answer/RecallLog/tracker snapshots. Its four seeded c1/c2 messages are starting context only. Coordinator accepted the consuming export, actual question/response/feedback controller reads and unchanged snapshots; G1 is proved here; mounted interaction remains with slices 8–10. Failed/unavailable quality stops production slices 4–11.

The isolated paid route is `CURSOR_DEV=true nix develop -c pnpm e2e:hold --paid-openai`, using the existing conversation flow and manual-testing skill. Both runs were explicitly authorized; six original plus two follow-up AI replies exhaust the eight-reply cap. No further paid call is authorized. Preserve all failed-run evidence and use compatible observations; do not repeat them. Temporary guidance is restored; slice 9 installs the exact base plus ordinary-text practice directive. A material later prompt change needs its relevant authorized real-service observation, without waiving the cap or quality premise.

<a id="slice-4"></a>

### 4. Name conversation access ownership coherently

Type: Structure
Status: done

Sizing: about five minutes plus measured required verification/API generation.

Change: Express ownership of conversation access independently of its subject through the existing Ownership association. Preserve the physical column and all existing audience values. Update the callers, builders and generated public contract together. This immediately enables slice 5; introduce no new visibility resource or schema.

Proof: Existing start/message/listing/menu/AI/export controller behavior remains green; regenerate the API if its public property changes and verify affected frontend types. Inspect consumers of the old field and partner labels, including generated types; use one existing access model rather than a parallel predicate. Accepted: `pnpm generateTypeScript`, full `pnpm backend:test_only` (2737 tests; 0 failures/errors, 2 skips), full headless `pnpm frontend:test` (338 files/2151 tests), both frontend `vue-tsc --noEmit`/`tsc --noEmit` and `pnpm openapi:lint`, all under `CURSOR_DEV=true nix develop -c`. Inspected the unchanged audience assertion in `ConversationStartControllerTest.StartConversationAboutRecallPrompt.setsRecallPromptAsSubject`, access/list JPQL, authorization getter and generated property. One-time consumer reading found no old camel/method consumers outside historical planning; the physical FK remains unchanged.

<a id="slice-5"></a>

### 5. Protect access to the learner's private attempt help

Type: Behavior
Status: done

Sizing: about five active minutes to restore scoped audience/entry changes and one confidentiality proof loop, plus the required backend suite. Reuse the parked prototype without restoring its label/unread-reply suffix.

Behavior: Given an answered prompt owned by the learner, starting help assigns their personal access ownership. The learner reads and replies; notebook authorship or membership grants neither direct access nor discovery. Existing shared conversations retain their audience.

Proof: Accepted actual creation, subject/messages, send, export, AI request, mark-read authorization, inbox listing and unread exclusion for learner, author and another circle member in `PrivateRecallConversationControllerTest` (25 cases). Another learner's prompt is refused; legacy shared reads/replies remain. Unread confidentiality uses the learner's stored non-null reasoning, so null-sender filtering cannot conceal a leak. Full `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed after independent refactor: 2762 tests, no failures/errors, 2 skips. Recall subject assignment is now independent of access assignment; the fixture builder explicitly retains legacy notebook audiences. Positive incoming-AI unread and truthful labels remain with slice 6; this actual prefix has fresh proof, unlike the [parked prototype](PRIVATE-ACCESS-PROTOTYPE-OBSERVATION.md).

<a id="slice-6"></a>

### 6. Recognize private help and incoming replies in the inbox

Type: Behavior
Status: done

Sizing: about five active minutes to restore the scoped inbox changes and one list/menu/read proof loop, plus the required backend suite.

Behavior: For the learner's private recall conversation, the inbox identifies its private audience. An actual stored incoming AI reply appears unread to its authorized learner until they mark it read; other notebook participants still cannot discover it. Existing shared/personal conversations keep truthful participant labels.

Proof: Accepted `PrivateRecallConversationControllerTest.learnerRequestsAndReadsAnAiReply`: actual streamed reply with only OpenAI mocked → persisted message → UserController unread menu → mark-read → empty menu. Author/member assistant discovery is refused; list observations distinguish private help and legacy circle/two-person labels. The common predicate includes null-sender AI replies with the same audience rule; existing human unread/read tests pass. Full `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed after independent test refactor: 2766 tests, no failures/errors, 2 skips, including all 29 private cases. G5/G6 are proved here. No second visibility or notification owner.

<a id="slice-7"></a>

### 7. Continue the same private attempt discussion

Type: Behavior
Status: done

Sizing: 5–8 minutes including repeat/overlapping-request handling and the required backend suite; reuse and creation are one identity-consistency rule.

Behavior: Starting help again for the same attempt finds the learner's compatible private conversation and retained messages. An existing shared thread is not misrepresented or repurposed. A different attempt has its own discussion.

Proof: Accepted six `RecallConversationContinuationControllerTest` cases: repeated start after flush/clear retains identity/messages, distinct attempts differ, oldest compatible personal thread is reused, and legacy circle/author/other-initiator threads retain their access. `RecallConversationConcurrentStartControllerTest` queues two actual controller requests behind a held prompt lock; release returns one identity and one stored conversation. The service owns locking before compatible lookup/creation; no registry. Full `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed after shared concurrency-support refactor: 2773 tests, no failures/errors, 2 skips. Earlier constructor compilation and detached-user fixture failures were corrected before this final proof; active implementation 8–9 minutes excludes suite waits.

<a id="slice-8"></a>

### 8. Open contextual help and return to the same recall

Type: Behavior
Status: done

Sizing: 5–8 minutes including mounted proof/typechecks; one shared entry journey.

Behavior: From an answered MCQ or a particular history item, the learner opens clearly labelled private help with the original attempt visible. Closing it returns focus and preserves the underlying recall/history position. Multiple history items each target their own attempt.

Proof: Accepted `RecallPage.privateHelp.spec.ts`: actual second-choice click and answer HTTP response → matching private-help Modal → original question/selected answer → dialog close or Escape → same answer/progress/queue/named route and invoker focus. `MemoryTrackerPageView.privateHelp.spec.ts` opens two distinct history attempts and preserves their cards/focus; real conversation/answer rendering and native Modal run with only HTTP mocked. Retained Message Center subjects pass. Full headless `CURSOR_DEV=true nix develop -c pnpm frontend:test` passed (340 files/2154 tests), with both frontend `vue-tsc --noEmit` and `tsc --noEmit`; independent refactor found no edits. G2 is proved by this same-parent journey; active implementation about eight minutes excludes verification waits.

<a id="slice-9"></a>

### 9. Explain the learner's reasoning and accept clarification

Type: Behavior
Status: done

Sizing: about five minutes of change plus backend/frontend verification.

Behavior: The learner can request an explanation immediately or provide why a choice seemed right. The reply addresses that reasoning and the original attempt; the learner can correct a tentative interpretation in a further turn.

Proof: Installed recall-only guidance at `ConversationAiRequestBuilder`; coordinator compared its Java text-block content byte-for-byte with the validated 1859-character protocol. `RecallConversationReasoningControllerTest` captures original stem/choices/key/selected index/result/focus/rationale separately from changed current note content, and persisted reasoning/reply/correction roles after DB reload. `ConversationInner.reasoning.spec.ts` drives neutral explanation, free reasoning and disagreement through HTTP/SSE completion, then remounts all six stored messages. Full Nix-prefixed backend suite passed (2775 tests, no failures/errors, 2 skips), headless frontend suite passed (341 files/2155 tests), and both frontend typechecks passed. Independent refactor found no edits. Formatting changed an explicit undefined mock return to void; the commit hook caught its type error. Returning the saved message, reformatting, and fresh full frontend/typechecks passed before commit. Teaching quality remains with slices 2/3; no further paid calls or material prompt change.

<a id="slice-10"></a>

### 10. Check the distinction without another Grade

Type: Behavior
Status: done

Sizing: about five minutes of change plus joined-flow verification.

Behavior: After an explanation, an explicit optional check asks one changed example about the same distinction. The learner responds before feedback, can discuss it further or leave, and can later reopen the question/response/ feedback exchange. Their original Grade, tracker and schedule stay authoritative.

Proof: Accepted `RecallPage.practice.spec.ts`: real choice/answer → explanation → exact optional check request → question → learner reasoning → feedback → close/reopen six HTTP stored messages, same selected answer/progress/queue and one answer submission. `RecallConversationPracticeControllerTest` drives the actual original answer and DB message loop, reloads six messages/roles, and compares unchanged Answer id/choice/outcome/correctness, RecallLog ids/Grades/answer ids and tracker stability/difficulty/last/next/count. Full Nix-prefixed backend suite passed (2776 tests, no failures/errors, 2 skips), headless frontend suite passed (342 files/2156 tests), both frontend typechecks passed; independent refactor found no edits. The slice 3 real model rubric owns quality/withholding; practice uses ordinary messages and preserves installed guidance.

<a id="slice-11"></a>

### 11. Carry a useful clarification into authorized refinement

Type: Behavior
Status: done

Sizing: 5–8 minutes; one contract change across its UI and AI consumer.

Behavior: When the learner wants to refine a note they may edit, the existing refinement receives the original selected answer and a clarification the learner explicitly chooses to carry, along with the existing question/focus context. Existing previews and author permissions govern the resulting content changes.

Proof: Accepted `ConversationComponent.refinement.spec.ts`: real parent shows a blank review before generation, learner revises text twice, confirmation sends exactly the final clarification and original selected choice/question/focus to refinement. Direct answered-question refinement remains immediate; readonly entry is refused. `AiControllerNoteRefinementTest` captures selected-answer text and reviewed clarification at the model boundary; actual subscriber reads through NoteController but cannot refine. Nix generation, full backend (2777 tests, no failures/errors, 2 skips), headless frontend (343 files/2158 tests), both frontend typechecks and OpenAPI lint passed. Independent refactor derives phase from confirmed text; its focused mounted rerun passed 5 tests and both typechecks. No private transcript is copied into the payload; existing permissions/previews govern content changes. No sharing or story 2 proposal work.

## Earlier execution refinements

On 2026-10-12, original slice 1 accumulated about 10–11 minutes active work: roughly seven for the first setup/observation and 3–4 for practice recovery. Fixture preparation, route recovery and two quality loops disproved its five-minute setup/rubric premise and crossed the ten-minute hard limit; external startup/model waits are separate. Attempt-owned Java was restored and both holds shut down before refinement. Split that leaf into setup Structure (1), explanation/ambiguity (2), and practice (3); retain original production leaves 2–8 as 4–10. The common ordinary-transcript/access design and source scope are unchanged; no new exception, responsibility, or story split is justified.

- Keep the L-sized story: accepted slices 1–4 delivered isolated setup, compatible model proof and coherent access naming. The remaining common transcript/audience model is understood. This separate access/inbox overrun justifies one bounded split; no numeric repeated-overrun threshold is supplied. Eleven leaves do not add scope or machinery.
- Slices 2 and 3 have accepted quality proof. Preserve it; any material later prompt change still needs relevant authorized real-service observation.
- Adding dedicated practice cards, mastery state, or author sharing changes this sizing premise. Return to story refinement and consider the vertical split "understand privately and resume" / "verify understanding with a follow-up" if needed; do not enlarge the plan silently.
- The later contest/repair candidates are unqueued and unplanned. Their unresolved verdict authority and historical reach do not hold this story's scope open.

On 2026-10-12, the later private-audience slice 5 accumulated 10–11 active minutes across eight access consumers, proof writing and the existing unread defect. The [parked complete prototype](PRIVATE-ACCESS-PROTOTYPE-OBSERVATION.md) passed 2763 backend tests (0 failures/errors, 2 skips), but its overrun prevents acceptance/delivery. The coordinator parked only six attempt-owned paths and restored clean published `98996efafdb7847e36602aa5e477fe70d8d33255` before this refinement. Split confidentiality into slice 5 and the cohesive inbox label/incoming-reply loop into slice 6; previous remaining slices 6–10 become 7–11. No privacy channel is deferred. G5/G6 honestly own the unread/label suffix; each restored prefix needs current proof.

The smallest authorized outcome remains private explanation/practice with later reading and recall return. Audience-only delivery without the inbox suffix would abandon its planned observation. Reuse the common incoming-message predicate and existing labels, rather than introducing per-help rules or a notification resource. The source, scope, queue and architectural direction are unchanged; the unrelated fixture and inbox overruns are preserved, not erased by splitting.


## Resumed consumer review

The automatic retrospective resumed only affected consumer conclusions on 2026-10-12 at `7c4702116cf568825c4b1b82012127d516e0e932`; the original review, process occurrences and architectural conclusions remain unchanged. Related published revisions added to its retained manifest:

- `fbd065c91599606112ca41496eb67edb42003119`: preparation, dated actual baselines and existing process findings.
- `b841084cdada9d23b8622f21f057d8dda3521e23`: shared read lifecycle across actual conversation viewers.
- `bba07df1a6bb3afb96de8f40ed1e5c677a12a58c`: nested dismissal and reached extraction/loading/E2E consumers.
- `7c4702116cf568825c4b1b82012127d516e0e932`: truthful MCQ/spelling UI context.

Read acknowledgement, child-only dismissal and ordinary spelling refinement are closed by the inspected surviving-behavior proofs in slices 12–14. Latest frontend proof is 345 files / 2,164 tests plus both typechecks; four existing refinement E2E scenarios passed. Unchanged backend/generator proof remains applicable. Mounted tests mock HTTP, Escape uses a browser DOM keyboard event, and model-quality observations remain illustrative with no lasting-mastery inference. No new correction, backlog change or duplicate process occurrence was warranted. The coordinator retains the exact publication target/observer and owns the final CI operation.
